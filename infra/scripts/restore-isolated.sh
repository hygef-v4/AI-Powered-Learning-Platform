#!/bin/sh
# Khôi phục archive backup vào PostgreSQL tạm trên máy người vận hành (runbook docs/runbooks/restore-isolated.md).
# Cách dùng: restore-isolated.sh <archive.alapbk> <private-key.pem> <image backend>
set -eu

if [ "$#" -ne 3 ]; then
    echo "Cách dùng: $0 <archive.alapbk> <private-key.pem> <image backend>" >&2
    exit 2
fi

ARCHIVE=$(cd "$(dirname "$1")" && pwd)/$(basename "$1")
KEY=$(cd "$(dirname "$2")" && pwd)/$(basename "$2")
IMAGE=$3
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
WORK=$(mktemp -d)
NAME="platform-restore-$STAMP"
EVIDENCE="restore-evidence-$STAMP.txt"
PASSWORD=$(head -c 24 /dev/urandom | od -An -tx1 | tr -d ' \n')
START=$(date -u +%s)

echo "Bắt đầu: $(date -u)" | tee "$EVIDENCE"
echo "Archive: $(basename "$ARCHIVE") ($(wc -c < "$ARCHIVE") byte)" | tee -a "$EVIDENCE"

cp "$ARCHIVE" "$WORK/archive.alapbk"
cp "$KEY" "$WORK/private-key.pem"
chmod 600 "$WORK/private-key.pem"

echo "1. Giải mã archive"
docker run --rm --user "$(id -u):$(id -g)" -v "$WORK:/work" --entrypoint java "$IMAGE" -cp /app/app.jar \
    -Dloader.main=edu.aiplatform.backup.infrastructure.BackupDecryptTool \
    org.springframework.boot.loader.launch.PropertiesLauncher \
    /work/archive.alapbk /work/private-key.pem /work/dump.pgc
rm -f "$WORK/private-key.pem"

echo "2. Tạo PostgreSQL 16 tạm: $NAME"
docker run -d --name "$NAME" -e POSTGRES_PASSWORD="$PASSWORD" -e POSTGRES_DB=platform \
    -v "$WORK:/work" -p 127.0.0.1::5432 postgres:16-alpine > /dev/null
until docker exec "$NAME" pg_isready -U postgres -d platform > /dev/null 2>&1; do sleep 1; done

echo "3. pg_restore"
docker exec "$NAME" pg_restore --no-owner --no-privileges -U postgres -d platform /work/dump.pgc

echo "4. Đếm số dòng các bảng"
docker exec "$NAME" psql -U postgres -d platform -c "ANALYZE;" > /dev/null
docker exec "$NAME" psql -U postgres -d platform -At -F ' | ' \
    -c "select relname, n_live_tup from pg_stat_user_tables order by relname" | tee -a "$EVIDENCE"
docker exec "$NAME" psql -U postgres -d platform -At \
    -c "select 'schema: ' || coalesce(max(version), 'none') from flyway_schema_history where success" | tee -a "$EVIDENCE"
rm -f "$WORK/dump.pgc" "$WORK/archive.alapbk"

END=$(date -u +%s)
echo "Kết thúc: $(date -u), thời gian khôi phục $((END - START)) giây" | tee -a "$EVIDENCE"
PORT=$(docker port "$NAME" 5432 | head -n 1 | sed 's/.*://')
echo
echo "Database tạm: psql postgresql://postgres:$PASSWORD@127.0.0.1:$PORT/platform"
echo "Kiểm theo danh sách trong runbook, ghi kết quả vào $EVIDENCE."
echo "Xong thì xóa: docker rm -f $NAME && rm -rf $WORK"
