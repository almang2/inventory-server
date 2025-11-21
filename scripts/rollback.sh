#!/usr/bin/env bash

set -e

APP_DIR=/home/ec2-user/app
LOG_DIR=$APP_DIR/logs
DEPLOY_DIR=$APP_DIR/deploy

ROLLBACK_LOG=$LOG_DIR/rollback.log
ACTIVE_COLOR_FILE=$DEPLOY_DIR/active-color

# Blue/Green 포트 & systemd 서비스 이름 (deploy.sh와 동일해야 함)
BLUE_PORT=8081
GREEN_PORT=8082

BLUE_SERVICE=app-blue.service
GREEN_SERVICE=app-green.service

# Nginx 설정 (deploy.sh와 동일)
NGINX_ENABLED=/etc/nginx/sites-enabled/app.conf
BLUE_CONF=/etc/nginx/sites-available/app-blue.conf
GREEN_CONF=/etc/nginx/sites-available/app-green.conf

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ROLLBACK] $1" | tee -a "$ROLLBACK_LOG"
}

log "==== 롤백 시작 ===="

mkdir -p "$LOG_DIR" "$DEPLOY_DIR"

if [ ! -f "$ACTIVE_COLOR_FILE" ]; then
  log "❌ active-color 파일이 없습니다. 롤백 불가."
  exit 1
fi

CURRENT_COLOR=$(cat "$ACTIVE_COLOR_FILE")

if [ "$CURRENT_COLOR" = "blue" ]; then
  PREV_COLOR="green"
  PREV_PORT=$GREEN_PORT
  PREV_SERVICE=$GREEN_SERVICE
  TARGET_CONF="$GREEN_CONF"
else
  PREV_COLOR="blue"
  PREV_PORT=$BLUE_PORT
  PREV_SERVICE=$BLUE_SERVICE
  TARGET_CONF="$BLUE_CONF"
fi

log "현재 활성 색상: $CURRENT_COLOR → 롤백 대상 색상: $PREV_COLOR (port=$PREV_PORT, service=$PREV_SERVICE)"

# 1) 롤백 대상 인스턴스 헬스 체크 시도
HEALTH_URL="http://127.0.0.1:${PREV_PORT}/actuator/health"

log "롤백 대상 헬스체크 시도: $HEALTH_URL"

SUCCESS="false"
for i in {1..5}; do
  STATUS=$(curl -s "$HEALTH_URL" || echo "")

  if echo "$STATUS" | grep -q "\"status\":\"UP\""; then
    log "✅ 롤백 대상 인스턴스 헬스체크 성공 (시도 $i / 5)"
    SUCCESS="true"
    break
  else
    log "헬스체크 실패 (시도 $i / 5), 응답: $STATUS"
    sleep 5
  fi
done

# 2) 필요 시 서비스 재시작 후 한 번 더 체크
if [ "$SUCCESS" != "true" ]; then
  log "⚠️ 인스턴스 상태가 DOWN으로 보입니다. 서비스 재시작 시도: $PREV_SERVICE"
  sudo systemctl restart "$PREV_SERVICE"

  sleep 10
  STATUS=$(curl -s "$HEALTH_URL" || echo "")
  if echo "$STATUS" | grep -q "\"status\":\"UP\""; then
    log "✅ 재시작 후 헬스체크 성공"
    SUCCESS="true"
  else
    log "❌ 재시작 후에도 헬스체크 실패. 롤백 중단."
    sudo journalctl -u "$PREV_SERVICE" -n 50 --no-pager | tee -a "$ROLLBACK_LOG"
    exit 1
  fi
fi

# 3) Nginx 심볼릭 링크를 롤백 대상 색상으로 변경
log "Nginx 설정을 $PREV_COLOR 기준으로 전환: $TARGET_CONF → $NGINX_ENABLED"
sudo ln -sf "$TARGET_CONF" "$NGINX_ENABLED"

# 4) Nginx 설정 검증 및 reload
sudo nginx -t
sudo systemctl reload nginx
log "Nginx reload 완료"

# 5) active-color 갱신
echo "$PREV_COLOR" > "$ACTIVE_COLOR_FILE"
log "active-color를 $PREV_COLOR 로 갱신"

log "==== 롤백 완료 ===="
