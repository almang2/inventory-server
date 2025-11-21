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

# Nginx 전환 (sites-enabled/app.conf 심볼릭 링크 교체)
log "Nginx 설정을 $IDLE_COLOR 기준으로 전환"

if [ "$IDLE_COLOR" = "blue" ]; then
  sudo ln -sf /etc/nginx/sites-available/app-blue.conf /etc/nginx/sites-enabled/app.conf
else
  sudo ln -sf /etc/nginx/sites-available/app-green.conf /etc/nginx/sites-enabled/app.conf
fi

sudo nginx -t
sudo systemctl reload nginx
log "Nginx reload 완료"

# active-color 갱신
echo "$IDLE_COLOR" > "$ACTIVE_COLOR_FILE"
log "active-color를 $IDLE_COLOR 로 갱신"

log "==== 배포 완료 ===="
