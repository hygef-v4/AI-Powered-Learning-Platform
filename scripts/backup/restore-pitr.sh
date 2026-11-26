#!/usr/bin/env bash
# Khôi phục cơ sở dữ liệu từ base backup và WAL trên Google Drive vào một Postgres 16 cô lập (U03, 2026-11-26).
# Không đụng tới cơ sở dữ liệu đang chạy: chuyển ứng dụng sang bản khôi phục làm tay theo
# scripts/backup/README.md sau khi đã kiểm tra.
#
# Cách dùng (chạy ở thư mục gốc repo, máy có Docker):
#   scripts/backup/restore-pitr.sh [--target-time 2026-11-26T16:55:00+07:00] [--private-key private.pem]
#                                  [--env-file infra/.env] [--image <image backend>] [--out <thư mục>] [--port 55432]
set -euo pipefail

TARGET_TIME=""
PRIVATE_KEY=""
ENV_FILE="infra/.env"
IMAGE="platform-backend:local"
OUT="restore-$(date +%Y%m%d-%H%M%S)"
PORT="55432"
PG_IMAGE="postgres:16-alpine"

while [ $# -gt 0 ]; do
  case "$1" in
    --target-time) TARGET_TIME="$2"; shift 2 ;;
    --private-key) PRIVATE_KEY="$2"; shift 2 ;;
    --env-file) ENV_FILE="$2"; shift 2 ;;
    --image) IMAGE="$2"; shift 2 ;;
    --out) OUT="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    *) echo "Không hiểu tham số $1" >&2; exit 2 ;;
  esac
done

# Docker trên Windows (Git Bash) cần đường dẫn dạng Windows và không đổi đường dẫn trong tham số.
export MSYS_NO_PATHCONV=1
abs_path() {
  if pwd -W >/dev/null 2>&1; then (cd "$1" && pwd -W); else (cd "$1" && pwd); fi
}

mkdir -p "$OUT"
OUT_ABS="$(abs_path "$OUT")"
NAME="platform-restore-$(basename "$OUT")"

echo "1/4 Tải base backup và WAL từ Google Drive vào $OUT"
TOOL_ARGS=(/restore)
KEY_MOUNT=()
if [ -n "$TARGET_TIME" ]; then TOOL_ARGS+=(--target-time "$TARGET_TIME"); fi
if [ -n "$PRIVATE_KEY" ]; then
  KEY_DIR="$(abs_path "$(dirname "$PRIVATE_KEY")")"
  KEY_MOUNT=(-v "$KEY_DIR/$(basename "$PRIVATE_KEY"):/keys/private.pem:ro")
  TOOL_ARGS+=(--private-key /keys/private.pem)
fi
docker run --rm --env-file "$ENV_FILE" --user 0 -v "$OUT_ABS:/restore" "${KEY_MOUNT[@]}" --entrypoint java "$IMAGE" \
  -cp /app/app.jar -Dloader.main=edu.aiplatform.backup.integration.BackupRestoreTool \
  org.springframework.boot.loader.launch.PropertiesLauncher "${TOOL_ARGS[@]}"

echo "2/4 Giải nén base backup và cấu hình phát lại WAL"
RECOVERY_TARGET=""
if [ -n "$TARGET_TIME" ]; then RECOVERY_TARGET="recovery_target_time = '$TARGET_TIME'"; fi
docker run --rm -v "$OUT_ABS:/restore" --entrypoint sh "$PG_IMAGE" -c "
  set -e
  rm -rf /restore/pgdata && mkdir -p /restore/pgdata
  tar -xzf /restore/base.tar.gz -C /restore/pgdata
  touch /restore/pgdata/recovery.signal
  {
    echo \"restore_command = 'cp /restore/wal/%f %p'\"
    echo \"recovery_target_action = 'promote'\"
    echo \"$RECOVERY_TARGET\"
  } >> /restore/pgdata/postgresql.auto.conf
  chown -R postgres:postgres /restore/pgdata /restore/wal
  chmod 700 /restore/pgdata
"

echo "3/4 Khởi động Postgres cô lập $NAME ở 127.0.0.1:$PORT và phát lại WAL"
docker rm -f "$NAME" >/dev/null 2>&1 || true
docker run -d --name "$NAME" -p "127.0.0.1:$PORT:5432" -v "$OUT_ABS:/restore" \
  -e PGDATA=/restore/pgdata --entrypoint docker-entrypoint.sh "$PG_IMAGE" postgres >/dev/null
for _ in $(seq 1 120); do
  STATE="$(docker exec "$NAME" psql -U postgres -d platform -tAc 'select pg_is_in_recovery()' 2>/dev/null || true)"
  if [ "$STATE" = "f" ]; then break; fi
  sleep 2
done
if [ "$STATE" != "f" ]; then
  echo "Postgres chưa phát lại xong WAL sau 4 phút; xem: docker logs $NAME" >&2
  exit 1
fi

echo "4/4 Kiểm tra dữ liệu đã khôi phục"
docker exec "$NAME" psql -U postgres -d platform -c "
  select 'accounts' as bang, count(*) from accounts
  union all select 'subjects', count(*) from subjects
  union all select 'course_classes', count(*) from course_classes
  union all select 'audit_logs', count(*) from audit_logs
  union all select 'flyway_schema_history', count(*) from flyway_schema_history;"
docker exec "$NAME" psql -U postgres -d platform -tAc \
  "select 'Bản ghi audit mới nhất: ' || coalesce(max(occurred_at)::text, 'không có') from audit_logs"

echo "Xong. Postgres đã khôi phục chạy ở container $NAME (127.0.0.1:$PORT), dữ liệu trong $OUT/pgdata."
echo "Chuyển ứng dụng sang bản này: scripts/backup/README.md. Dọn: docker rm -f $NAME && rm -rf $OUT"
