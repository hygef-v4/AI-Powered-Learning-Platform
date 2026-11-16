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
