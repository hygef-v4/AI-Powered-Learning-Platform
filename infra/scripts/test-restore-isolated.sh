#!/bin/sh
set -eu

SCRIPT=$(cd "$(dirname "$0")" && pwd)/restore-isolated.sh
TEST_WORK=$(mktemp -d)
trap 'rm -rf "$TEST_WORK"' EXIT
mkdir "$TEST_WORK/bin" "$TEST_WORK/tmp"
printf archive > "$TEST_WORK/archive.alapbk"
printf key > "$TEST_WORK/key.pem"
cat > "$TEST_WORK/bin/docker" <<'MOCK'
#!/bin/sh
case "$*" in
    *-Dloader.main=edu.aiplatform.backup.integration.BackupDecryptTool*) ;;
    *) exit 99 ;;
esac
for argument do
    case "$argument" in
        *:/work) work=${argument%:/work} ;;
    esac
done
test -f "$work/private-key.pem"
printf partial > "$work/dump.pgc"
exit 42
MOCK
chmod +x "$TEST_WORK/bin/docker"
cd "$TEST_WORK"
status=0
TMPDIR="$TEST_WORK/tmp" PATH="$TEST_WORK/bin:$PATH" sh "$SCRIPT" archive.alapbk key.pem backend:test > output 2>&1 || status=$?
test "$status" -eq 42
test -z "$(find "$TEST_WORK/tmp" -type f -print)"
test -f key.pem
test -f archive.alapbk
echo 'restore decrypt failure cleanup: passed'

cat > "$TEST_WORK/bin/docker" <<'MOCK'
#!/bin/sh
case "$*" in
    *-Dloader.main=edu.aiplatform.backup.integration.BackupDecryptTool*)
        for argument do
            case "$argument" in
                *:/work) work=${argument%:/work} ;;
            esac
        done
        test -f "$work/private-key.pem"
        printf dump > "$work/dump.pgc" ;;
    run\ -d*) printf 'test-restore-container\n' ;;
    *pg_isready*) test "$DATABASE_READY" = yes ;;
    *pg_restore*) printf 'restored\n' >> "$MOCK_CALLS" ;;
    exec\ *) printf 'query\n' >> "$MOCK_CALLS" ;;
    port\ *) printf '127.0.0.1:5433\n' ;;
    rm\ -f\ test-restore-container) printf 'removed\n' >> "$MOCK_CALLS" ;;
    *) exit 99 ;;
esac
MOCK
chmod +x "$TEST_WORK/bin/docker"
export MOCK_CALLS="$TEST_WORK/restore-calls"
: > "$MOCK_CALLS"
status=0
DATABASE_READY=no RESTORE_READY_TIMEOUT_SECONDS=1 TMPDIR="$TEST_WORK/tmp" PATH="$TEST_WORK/bin:$PATH" \
    timeout 6 sh "$SCRIPT" archive.alapbk key.pem backend:test > output 2>&1 || status=$?
test "$status" -eq 1
grep -q 'PostgreSQL' output
grep -qx removed "$MOCK_CALLS"
test -z "$(find "$TEST_WORK/tmp" -type f -print)"
echo 'restore readiness timeout cleanup: passed'

: > "$MOCK_CALLS"
DATABASE_READY=yes RESTORE_READY_TIMEOUT_SECONDS=1 TMPDIR="$TEST_WORK/tmp" PATH="$TEST_WORK/bin:$PATH" \
    sh "$SCRIPT" archive.alapbk key.pem backend:test > output 2>&1
grep -qx restored "$MOCK_CALLS"
! grep -qx removed "$MOCK_CALLS"
test -z "$(find "$TEST_WORK/tmp" -type f -print)"
echo 'successful restore keeps the database: passed'
