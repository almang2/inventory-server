#!/usr/bin/env bash

set -euo pipefail

APP_DIR=/home/ec2-user/app
LOG_DIR=$APP_DIR/logs
DEPLOY_DIR=$APP_DIR/deploy

ROLLBACK_LOG=$LOG_DIR/rollback.log
ACTIVE_COLOR_FILE=$DEPLOY_DIR/active-color

# Blue/Green 포트 & systemd 서비스 이름
BLUE_PORT=8081
GREEN_PORT=8082

BLUE_SERVICE=app-blue.service
GREEN_SERVICE=app-green.service

# Nginx 설정
NGINX_ENABLED=/etc/nginx/sites-enabled/app.conf
BLUE_CONF=/etc/nginx/sites-available/app-blue.conf
GREEN_CONF=/etc/nginx/sites-available/app-green.conf

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ROLLBACK] $1" | tee -a "$ROLLBACK_LOG"
}

log "==== 롤백 시작 ===="

mkdir -p "$LOG_DIR" "$DEPLOY_DIR"

# 1) 활성화된 색상 파일 로드
if [ ! -f "$ACTIVE_COLOR_FILE" ]; then
  log "❌ active-color 파일이 없습니다. 롤백 불가."
  exit 1
fi

CURRENT_COLOR=$(head -n1 "$ACTIVE_COLOR_FILE" | tr -d '[:space:]')

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

# 2) 롤백 대상 서버 헬스체크
HEALTH_URL="http://127.0.0.1:${PREV_PORT}/actuator/health"
log "헬스체크 시도: $HEALTH_URL"

SUCCESS="false"

for i in {1..5}; do
  STATUS=$(curl -s --max-time 5 "$HEALTH_URL" || echo "")

  if echo "$STATUS" | grep -qi "\"status\"[[:space:]]*:[[:space:]]*\"UP\""; then
    log "✅ 헬스체크 성공 (시도 $i / 5)"
    SUCCESS="true"
    break
  else
    log "헬스체크 실패 (시도 $i / 5), 응답: $STATUS"
    sleep 5
  fi
done

# 3) 헬스체크 실패 시 서비스 재시작 후 재검증
if [ "$SUCCESS" != "true" ]; then
  log "⚠️ 서버가 DOWN처럼 보입니다. 서비스 재시작: $PREV_SERVICE"

  if ! sudo systemctl restart "$PREV_SERVICE"; then
    log "❌ systemctl restart 실패: $PREV_SERVICE"
    exit 1
  fi

  sleep 10
  STATUS=$(curl -s --max-time 5 "$HEALTH_URL" || echo "")

  if echo "$STATUS" | grep -qi "\"status\"[[:space:]]*:[[:space:]]*\"UP\""; then
    log "✅ 재시작 후 헬스체크 성공"
  else
    log "❌ 재시작 후에도 헬스체크 실패. 로그 출력:"
    JOURNAL=$(sudo journalctl -u "$PREV_SERVICE" -n 50 --no-pager || true)
    echo "$JOURNAL" | tee -a "$ROLLBACK_LOG"
    exit 1
  fi
fi

# 4) Nginx 전환
log "Nginx 설정을 $PREV_COLOR 기준으로 전환"

sudo ln -sf "$TARGET_CONF" "$NGINX_ENABLED"

sudo nginx -t
sudo systemctl reload nginx

log "Nginx reload 완료"

# 5) active-color 갱신
if ! echo "$PREV_COLOR" > "$ACTIVE_COLOR_FILE" 2>/dev/null; then
  log "❌ active-color 파일 쓰기 실패: $ACTIVE_COLOR_FILE (권한 문제)"
  exit 1
fi

log "active-color 갱신 완료 → $PREV_COLOR"

log "==== 롤백 완료 ===="
