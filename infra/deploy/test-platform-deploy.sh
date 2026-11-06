#!/usr/bin/env bash
# Test platform-deploy.sh bằng docker, git, curl giả; không đụng Docker hay GitHub thật.
#   docker run --rm -v "$PWD:/repo" -w /repo ubuntu:24.04 \
#     bash -c 'apt-get update -qq && apt-get install -qq -y jq >/dev/null && infra/deploy/test-platform-deploy.sh'
set -Eeuo pipefail

SCRIPT="$(cd "$(dirname "$0")" && pwd)/platform-deploy.sh"
A=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
B=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
C=cccccccccccccccccccccccccccccccccccccccc
FAILURES=0

setup() {
    ROOT=$(mktemp -d)
    export PLATFORM_DIR="$ROOT/platform" STATE_DIR="$ROOT/state" BACKUP_DIR="$ROOT/backups"
    export LOCK_FILE="$ROOT/lock" HEALTH_TIMEOUT=1 HEALTH_INTERVAL=1 KEEP_DUMPS=2
    export CALLS="$ROOT/calls" BAD_TAGS="" FAIL_PULL="" LATEST="$A" API_FAIL=""
    mkdir -p "$PLATFORM_DIR/infra" "$ROOT/bin"
    printf 'DOMAIN=example.test\nGHCR_OWNER=hygef-v4\nIMAGE_TAG=%s\nSECRET=x\n' "$A" >"$PLATFORM_DIR/infra/.env"
    chmod 600 "$PLATFORM_DIR/infra/.env"
    : >"$CALLS"

    cat >"$ROOT/bin/docker" <<'STUB'
#!/usr/bin/env bash
tag=$(sed -n 's/^IMAGE_TAG=//p' "$PLATFORM_DIR/infra/.env")
case "$*" in
pull*) [[ -n $FAIL_PULL && $* == *"$FAIL_PULL"* ]] && exit 1; echo "pull ${*: -1}" >>"$CALLS" ;;
*" ps -a -q") echo c1; echo c2 ;;
*" up -d"*) echo "up $tag" >>"$CALLS"; [[ " $BAD_TAGS " == *" up:$tag "* ]] && exit 1; exit 0 ;;
*pg_dump*) echo "dump $tag" >>"$CALLS"; echo DATA ;;
inspect*)
    echo "/certbot running none"
    if [[ " $BAD_TAGS " == *" $tag "* ]]; then echo "/backend running unhealthy"; else echo "/backend running healthy"; fi ;;
*) echo "docker giả không hiểu: $*" >&2; exit 2 ;;
esac
STUB
    cat >"$ROOT/bin/git" <<'STUB'
#!/usr/bin/env bash
case "$*" in
*fetch*) exit 0 ;;
*cat-file*) [[ $* != *dddd* ]] ;;
*checkout*) echo "checkout ${*: -1}" >>"$CALLS" ;;
*) exit 2 ;;
esac
STUB
    cat >"$ROOT/bin/curl" <<'STUB'
#!/usr/bin/env bash
if [[ $* == *api.github.com* ]]; then
    [[ -z $API_FAIL ]] || exit 22
    printf '{"workflow_runs":[{"head_sha":"%s"}]}' "$LATEST"
fi
STUB
    cat >"$ROOT/bin/systemctl" <<'STUB'
#!/usr/bin/env bash
exit 0
STUB
    chmod +x "$ROOT/bin/"*
    export PATH="$ROOT/bin:$PATH"
}

run() { "$SCRIPT" "$@" >"$ROOT/out" 2>&1; }
tag() { sed -n 's/^IMAGE_TAG=//p' "$PLATFORM_DIR/infra/.env"; }

check() {
    local name=$1
    shift
    if "$@"; then
        printf 'ok    %s\n' "$name"
    else
        printf 'FAIL  %s\n' "$name"
        sed 's/^/      /' "$ROOT/out" "$CALLS" 2>/dev/null || true
        FAILURES=$((FAILURES + 1))
    fi
}

t_check_noop() {
    setup
    run check && [[ $(tag) == "$A" ]] && [[ ! -s $CALLS ]] && grep -qxF "$A" "$STATE_DIR/deployed"
}

t_check_deploys_new() {
    setup
    LATEST=$B run check || return 1
    [[ $(tag) == "$B" ]] && grep -q "dump $A" "$CALLS" && grep -q "checkout $B" "$CALLS" &&
        [[ $(tail -n 1 "$STATE_DIR/deployed") == "$B" ]] && ls "$BACKUP_DIR"/predeploy-*-aaaaaaaaaaaa.dump >/dev/null &&
        [[ $(stat -c %a "$PLATFORM_DIR/infra/.env") == 600 ]] && grep -q '^SECRET=x$' "$PLATFORM_DIR/infra/.env"
}

t_unhealthy_rolls_back_and_skips() {
    setup
    BAD_TAGS="$B" LATEST=$B run check && return 1
    [[ $(tag) == "$A" ]] && grep -qxF "$B" "$STATE_DIR/skip" && grep -q "đã quay về" "$ROOT/out" || return 1
    # Lần hỏi sau không deploy lại bản lỗi.
    : >"$CALLS"
    LATEST=$B run check && [[ ! -s $CALLS ]]
}

t_compose_up_failure_rolls_back() {
    setup
    BAD_TAGS="up:$B" LATEST=$B run check && return 1
    [[ $(tag) == "$A" ]] && grep -qxF "$B" "$STATE_DIR/skip"
}

t_pull_failure_changes_nothing() {
    setup
    FAIL_PULL=$B LATEST=$B run check && return 1
    [[ $(tag) == "$A" ]] && ! grep -q "up \|dump " "$CALLS" && [[ ! -s $STATE_DIR/skip ]]
}

t_missing_commit_changes_nothing() {
    setup
    run deploy dddddddddddddddddddddddddddddddddddddddd && return 1
    [[ $(tag) == "$A" ]] && ! grep -q "up " "$CALLS"
}

t_api_failure_changes_nothing() {
    setup
    API_FAIL=1 run check && return 1
    [[ $(tag) == "$A" ]] && [[ ! -s $CALLS ]]
}

t_manual_rollback_twice() {
    setup
    LATEST=$B run check && LATEST=$C run check || return 1
    [[ $(tag) == "$C" ]] || return 1
    run rollback && [[ $(tag) == "$B" ]] && grep -qxF "$C" "$STATE_DIR/skip" || return 1
    # Timer không đẩy bản đã rollback lên lại.
    LATEST=$C run check && [[ $(tag) == "$B" ]] || return 1
    run rollback && [[ $(tag) == "$A" ]] || return 1
    run rollback && return 1
    grep -q "không có bản nào trước" "$ROOT/out"
}

t_rollback_to_sha_and_unskip() {
    setup
    LATEST=$B run check && LATEST=$C run check || return 1
    run rollback "$A" && [[ $(tag) == "$A" ]] && grep -qxF "$C" "$STATE_DIR/skip" || return 1
    run unskip "$C" && ! grep -qxF "$C" "$STATE_DIR/skip" && LATEST=$C run check && [[ $(tag) == "$C" ]]
}

t_keeps_latest_dumps() {
    setup
    LATEST=$B run check && sleep 1 && LATEST=$C run check && sleep 1 && run deploy "$A" || return 1
    [[ $(find "$BACKUP_DIR" -name 'predeploy-*.dump' | wc -l) == 2 ]] && ! ls "$BACKUP_DIR"/*aaaaaaaaaaaa.dump >/dev/null 2>&1
}

t_status_and_bad_input() {
    setup
    LATEST=$B run check && run status && grep -q "Đang chạy:       $B" "$ROOT/out" &&
        grep -q "Quay về được:    $A" "$ROOT/out" || return 1
    ! run deploy not-a-sha && ! run unknown
}

for t in t_check_noop t_check_deploys_new t_unhealthy_rolls_back_and_skips t_compose_up_failure_rolls_back \
    t_pull_failure_changes_nothing t_missing_commit_changes_nothing t_api_failure_changes_nothing \
    t_manual_rollback_twice t_rollback_to_sha_and_unskip t_keeps_latest_dumps t_status_and_bad_input; do
    check "$t" "$t"
done
((FAILURES == 0)) || {
    echo "$FAILURES test lỗi"
    exit 1
}
echo "Tất cả test qua"
