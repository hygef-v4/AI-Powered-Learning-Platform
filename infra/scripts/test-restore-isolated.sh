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
set -eu
printf '%s\n' "$*" >> "$TEST_WORK/docker-calls"
case "$1" in
    run)
        case "$*" in
            *-Dloader.main=*)
                for argument do
                    case "$argument" in *:/work) work=${argument%:/work} ;; esac
                done
                printf dump > "$work/dump.pgc"
                ;;
        esac
        ;;
    inspect)
        if [ "$SCENARIO" = exited ]; then printf 'exited\n'; else printf 'running\n'; fi
        ;;
    exec)
        case "$*" in
            *pg_isready*) [ "$SCENARIO" = success ] ;;
            *pg_restore*) test "$SCENARIO" = success ;;
        esac
        ;;
    port) printf '127.0.0.1:54321\n' ;;
    rm) ;;
    *) exit 99 ;;
esac
MOCK
export TEST_WORK
for SCENARIO in exited timeout success; do
    export SCENARIO
    : > "$TEST_WORK/docker-calls"
    status=0
    TMPDIR="$TEST_WORK/tmp" PATH="$TEST_WORK/bin:$PATH" RESTORE_READY_TIMEOUT_SECONDS=1 \
        sh "$SCRIPT" archive.alapbk key.pem backend:test > output 2>&1 || status=$?
    test -z "$(find "$TEST_WORK/tmp" -type f -print)"
    if [ "$SCENARIO" = success ]; then
        test "$status" -eq 0
        grep -q pg_restore "$TEST_WORK/docker-calls"
        if grep -q '^rm -fv platform-restore-' "$TEST_WORK/docker-calls"; then exit 1; fi
    else
        test "$status" -eq 1
        grep -q '^rm -fv platform-restore-' "$TEST_WORK/docker-calls"
        if grep -q pg_restore "$TEST_WORK/docker-calls"; then exit 1; fi
    fi
    echo "restore $SCENARIO: passed"
done
