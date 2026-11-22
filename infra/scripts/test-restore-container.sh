#!/bin/sh
# Linux + Docker: test the actual backend image and PostgreSQL with synthetic data only.
# Usage: test-restore-container.sh <locally built backend image>
set -eu
test "$#" -eq 1 || { echo 'Usage: test-restore-container.sh <backend-image>' >&2; exit 2; }
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
IMAGE=$1
PG_IMAGE=$(sed -n 's/^    -v .* \(postgres:[^ ]*\) >.*/\1/p' "$SCRIPT_DIR/restore-isolated.sh")
test -n "$PG_IMAGE"
WORK=$(mktemp -d)
SOURCE="nfr-restore-source-$(date -u +%s)-$$"
RESTORED=''
cleanup() {
    docker rm -fv "$SOURCE" >/dev/null 2>&1 || true
    if [ -n "$RESTORED" ]; then docker rm -fv "$RESTORED" >/dev/null 2>&1 || true; fi
    rm -f "$WORK/archive.alapbk" "$WORK/key.pem" "$WORK/output"
}
trap cleanup EXIT
trap 'exit 1' HUP INT TERM
PASSWORD=$(head -c 24 /dev/urandom | od -An -tx1 | tr -d ' \n')
docker run -d --name "$SOURCE" -e POSTGRES_PASSWORD="$PASSWORD" -e POSTGRES_DB=platform \
    -v "$WORK:/fixture" "$PG_IMAGE" >/dev/null
deadline=$(($(date -u +%s) + 60))
until docker exec "$SOURCE" pg_isready -U postgres -d platform >/dev/null 2>&1; do
    test "$(date -u +%s)" -lt "$deadline" || exit 1
    sleep 1
done
docker exec -i "$SOURCE" psql -v ON_ERROR_STOP=1 -U postgres -d platform >/dev/null <<'SQL'
create table flyway_schema_history(version text, success boolean);
insert into flyway_schema_history values ('fixture-1', true);
create table restore_fixture(id integer primary key, payload jsonb not null, score numeric(5,2));
insert into restore_fixture values(1, '{"text":"synthetic submission","role":"STUDENT"}', 8.25);
SQL
docker exec "$SOURCE" pg_dump -Fc -U postgres -d platform -f /fixture/archive.alapbk
printf 'unused for raw dump\n' > "$WORK/key.pem"
cd "$WORK"
sh "$SCRIPT_DIR/restore-isolated.sh" archive.alapbk key.pem "$IMAGE" > output
RESTORED=$(sed -n 's/.*docker rm -fv \([^ ]*\).*/\1/p' output)
case "$RESTORED" in platform-restore-*) ;; *) echo 'Missing restored container' >&2; exit 1 ;; esac
actual=$(docker exec "$RESTORED" psql -U postgres -d platform -At \
    -c "select payload->>'text' || ':' || score from restore_fixture where id=1")
test "$actual" = 'synthetic submission:8.25'
test "$(docker exec "$RESTORED" psql -U postgres -d platform -At -c 'select count(*) from restore_fixture')" = 1
grep 'Kết thúc:' output
echo 'PASS actual-image restore: schema, JSON payload, decimal score and row count'
