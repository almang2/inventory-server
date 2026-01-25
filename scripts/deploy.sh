#!/usr/bin/env bash

set -e  # 에러 나면 즉시 종료

APP_DIR=/home/ec2-user/app
LOG_DIR=$APP_DIR/logs
DEPLOY_DIR=$APP_DIR/deploy

DEPLOY_LOG=$LOG_DIR/deploy.log
ACTIVE_COLOR_FILE=$DEPLOY_DIR/active-color

# Blue/Green 포트 (실제 구성에 맞게 수정 가능)
BLUE_PORT=8081
GREEN_PORT=8082

mkdir -p "$LOG_DIR"
mkdir -p "$APP_DIR/blue" "$APP_DIR/green" "$DEPLOY_DIR"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$DEPLOY_LOG"
}

log "==== 배포 시작 ===="

# CI에서 업로드한 JAR 존재 확인
if [ ! -f "$APP_DIR/app.jar" ]; then
  log "❌ ERROR: $APP_DIR/app.jar 파일이 없습니다. (GitHub Actions에서 JAR 업로드 실패 가능)"
  exit 1
fi

# active-color 파일 없으면 blue로 초기화
if [ ! -f "$ACTIVE_COLOR_FILE" ]; then
  echo "blue" > "$ACTIVE_COLOR_FILE"
  log "active-color 파일이 없어 blue로 초기화합니다."
fi

CURRENT_COLOR=$(cat "$ACTIVE_COLOR_FILE")
if [ "$CURRENT_COLOR" = "blue" ]; then
  IDLE_COLOR="green"
  IDLE_PORT=$GREEN_PORT
else
  IDLE_COLOR="blue"
  IDLE_PORT=$BLUE_PORT
fi

log "현재 활성 색상: $CURRENT_COLOR → 대기 색상: $IDLE_COLOR (port=$IDLE_PORT)"

TARGET_DIR="$APP_DIR/$IDLE_COLOR"
TARGET_JAR="$TARGET_DIR/app.jar"

log "대기 색상 디렉터리($TARGET_DIR)에 JAR 복사"
cp "$APP_DIR/app.jar" "$TARGET_JAR"
chmod +x "$TARGET_JAR"

SERVICE_NAME="app-$IDLE_COLOR.service"

log "systemd 서비스 재시작: $SERVICE_NAME"
sudo systemctl restart "$SERVICE_NAME"

# 헬스체크
log "헬스체크 시작: http://127.0.0.1:$IDLE_PORT/actuator/health"
SUCCESS="false"

for i in {1..10}; do
  sleep 5
  STATUS=$(curl -s "http://127.0.0.1:$IDLE_PORT/actuator/health" || echo "")

  if echo "$STATUS" | grep -q "\"status\":\"UP\""; then
    log "✅ 헬스체크 성공 (시도 $i / 10)"
    SUCCESS="true"
    break
  else
    log "헬스체크 대기 중... (시도 $i / 10) 응답: $STATUS"
  fi
done

if [ "$SUCCESS" != "true" ]; then
  log "❌ 헬스체크 실패! 배포 중단 (Nginx 전환 안 함)"

  log "=== systemd 로그 (마지막 50줄) ==="
  sudo journalctl -u "$SERVICE_NAME" -n 50 --no-pager | tee -a "$DEPLOY_LOG"

  exit 1
fi

# Nginx 전환 (upstream.conf 스위치)
log "Nginx upstream을 $IDLE_COLOR 기준으로 전환 (port=$IDLE_PORT)"

# 전환 직전 최종 확인(실패 시 즉시 중단)
curl -sf "http://127.0.0.1:$IDLE_PORT/actuator/health" >/dev/null

UPSTREAM_FILE="/etc/nginx/conf.d/upstream.conf"
BACKUP_FILE="/etc/nginx/conf.d/upstream.conf.bak.$(date +%Y%m%d%H%M%S)"

sudo cp "$UPSTREAM_FILE" "$BACKUP_FILE" 2>/dev/null || true

sudo tee "$UPSTREAM_FILE" >/dev/null <<EOF
upstream app_upstream {
    server 127.0.0.1:$IDLE_PORT;  # $IDLE_COLOR
}
EOF

# 설정 검증 후 reload (실패 시 롤백)
if sudo nginx -t 2>&1 | tee -a "$DEPLOY_LOG"; then
  sudo systemctl reload nginx 2>&1 | tee -a "$DEPLOY_LOG"
  log "Nginx reload 완료 (upstream → $IDLE_COLOR:$IDLE_PORT)"
else
  log "❌ nginx -t 실패! upstream 롤백 후 배포 중단"
  if [ -f "$BACKUP_FILE" ]; then
    sudo cp "$BACKUP_FILE" "$UPSTREAM_FILE"
    sudo nginx -t 2>&1 | tee -a "$DEPLOY_LOG" || true
    sudo systemctl reload nginx || true
    log "upstream 롤백 완료"
  fi
  exit 1
fi

# active-color 갱신
echo "$IDLE_COLOR" > "$ACTIVE_COLOR_FILE"
log "active-color를 $IDLE_COLOR 로 갱신"

log "==== 배포 완료 ===="
