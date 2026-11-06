#!/usr/bin/env bash
# Tự động deploy production theo kiểu kéo: VPS hỏi GitHub bản mới nhất trên main đã qua CI rồi tự cập nhật, lỗi thì tự
# quay về bản trước. GitHub Actions không SSH vào được VPS (nhà cung cấp chặn kết nối quốc tế), nên VPS phải tự kéo.
# Xem docs/runbooks/deploy-vps.md, mục 10.
#
#   platform-deploy check          # timer gọi: deploy nếu main có bản mới đã qua CI
#   platform-deploy deploy <sha>   # deploy một commit cụ thể (đã có image trên GHCR)
#   platform-deploy rollback [sha] # quay về bản gần nhất trước đó, hoặc về <sha>
#   platform-deploy status         # bản đang chạy, bản có thể quay về, danh sách bỏ qua, lịch sử
#   platform-deploy unskip <sha>   # cho phép timer deploy lại một bản đã bị bỏ qua
set -Eeuo pipefail

PLATFORM_DIR=${PLATFORM_DIR:-/opt/platform}
INFRA_DIR="$PLATFORM_DIR/infra"
ENV_FILE="$INFRA_DIR/.env"
STATE_DIR=${STATE_DIR:-/var/lib/platform-deploy}
BACKUP_DIR=${BACKUP_DIR:-/var/backups/platform}
LOCK_FILE=${LOCK_FILE:-/run/lock/platform-deploy.lock}
REPO=${DEPLOY_REPO:-hygef-v4/AI-Powered-Learning-Platform}
WORKFLOW=${DEPLOY_WORKFLOW:-ci.yml}
BRANCH=${DEPLOY_BRANCH:-main}
HEALTH_TIMEOUT=${HEALTH_TIMEOUT:-300}
HEALTH_INTERVAL=${HEALTH_INTERVAL:-10}
KEEP_DUMPS=${KEEP_DUMPS:-5}

DEPLOYED="$STATE_DIR/deployed" # các SHA đã deploy thành công, cũ trước mới sau
SKIP="$STATE_DIR/skip"         # SHA timer không tự deploy: đã lỗi hoặc đã bị rollback
HISTORY="$STATE_DIR/history.log"
LAST_PROBLEM=""
LAST_DUMP=""

log() { printf '%s\n' "$*" >&2; }
die() {
    log "LỖI: $*"
    exit 1
}

compose() { (cd "$INFRA_DIR" && docker compose --env-file .env "$@"); }
env_value() { sed -n "s/^$1=//p" "$ENV_FILE" | tail -n 1; }
valid_sha() { [[ $1 =~ ^[0-9a-f]{40}$ ]]; }
short() { printf '%s' "${1:0:12}"; }
record() { printf '%s %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*" >>"$HISTORY"; }
is_skipped() { [[ -f $SKIP ]] && grep -qxF "$1" "$SKIP"; }

add_skip() {
    is_skipped "$1" || printf '%s\n' "$1" >>"$SKIP"
}

lock() {
    exec 9>"$LOCK_FILE"
    flock -n 9 || die "đang có một lần deploy khác chạy"
}

init_state() {
    mkdir -p "$STATE_DIR"
    chmod 700 "$STATE_DIR"
    touch "$DEPLOYED" "$SKIP" "$HISTORY"
    # Lần chạy đầu: bản đang chạy (deploy tay theo runbook) là mốc để quay về.
    local current
    current=$(env_value IMAGE_TAG)
    if [[ ! -s $DEPLOYED ]] && valid_sha "$current"; then
        printf '%s\n' "$current" >>"$DEPLOYED"
    fi
}

# Đổi IMAGE_TAG trong .env; tệp tạm cùng thư mục, quyền 600 như tệp gốc.
set_image_tag() {
    local tmp
    tmp=$(mktemp "$ENV_FILE.XXXXXX")
    awk -v tag="$1" '/^IMAGE_TAG=/ { print "IMAGE_TAG=" tag; found = 1; next } { print }
        END { if (!found) print "IMAGE_TAG=" tag }' "$ENV_FILE" >"$tmp"
    chmod 600 "$tmp"
    mv "$tmp" "$ENV_FILE"
}

latest_success_sha() {
    curl -fsS -m 20 -H 'Accept: application/vnd.github+json' \
        "https://api.github.com/repos/$REPO/actions/workflows/$WORKFLOW/runs?branch=$BRANCH&event=push&status=success&per_page=1" |
        jq -r '.workflow_runs[0].head_sha // empty'
}

# Bản gần nhất đã chạy tốt, khác bản hiện tại và không bị bỏ qua.
previous_sha() {
    local sha
    while read -r sha; do
        if [[ $sha != "$1" ]] && ! is_skipped "$sha"; then
            printf '%s' "$sha"
            return 0
        fi
    done < <(tac "$DEPLOYED")
    return 0
}

pull_images() {
    local owner app
    owner=$(env_value GHCR_OWNER)
    for app in backend frontend; do
        docker pull -q "ghcr.io/${owner:-max2min}/platform-$app:$1" >/dev/null || return 1
    done
}

dump_database() {
    mkdir -p "$BACKUP_DIR"
    chmod 700 "$BACKUP_DIR"
    local file
    file="$BACKUP_DIR/predeploy-$(date -u +%Y%m%dT%H%M%SZ)-$(short "$1").dump"
    if ! compose exec -T postgres pg_dump -U postgres -d platform -Fc >"$file.part"; then
        rm -f "$file.part"
        return 1
    fi
    chmod 600 "$file.part"
    mv "$file.part" "$file"
    LAST_DUMP=$file
    # Giữ KEEP_DUMPS bản mới nhất.
    find "$BACKUP_DIR" -maxdepth 1 -name 'predeploy-*.dump' -printf '%T@ %p\n' | sort -rn |
        tail -n +"$((KEEP_DUMPS + 1))" | cut -d' ' -f2- | xargs -r rm -f
    log "Đã sao lưu database: $file"
}

switch_to() {
    LAST_PROBLEM=""
    git -C "$PLATFORM_DIR" checkout -q --detach "$1" &&
        set_image_tag "$1" &&
        compose up -d --remove-orphans
}

containers_ok() {
    local ids
    ids=$(compose ps -a -q)
    if [[ -z $ids ]]; then
        LAST_PROBLEM="không có container nào"
        return 1
    fi
    # shellcheck disable=SC2086 # tách id theo dòng là chủ ý
    LAST_PROBLEM=$(docker inspect --format \
        '{{.Name}} {{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' $ids |
        awk '$2 != "running" || ($3 != "healthy" && $3 != "none") { printf "%s%s %s/%s", sep, substr($1, 2), $2, $3; sep = ", " }')
    [[ -z $LAST_PROBLEM ]]
}

site_ok() {
    local domain
    domain=$(env_value DOMAIN)
    if ! curl -fsS -o /dev/null -m 10 --resolve "$domain:443:127.0.0.1" "https://$domain/"; then
        LAST_PROBLEM="https://$domain/ không trả 2xx"
        return 1
    fi
}

wait_healthy() {
    local deadline=$((SECONDS + HEALTH_TIMEOUT))
    while ((SECONDS < deadline)); do
        if containers_ok && site_ok; then
            return 0
        fi
        sleep "$HEALTH_INTERVAL"
    done
    return 1
}

# Đưa <sha> lên chạy; lỗi thì quay về bản đang chạy trước đó. $2 là lý do ghi lịch sử (auto, manual, rollback).
release() {
    local sha=$1 reason=$2 prev
    valid_sha "$sha" || die "SHA không hợp lệ: $sha"
    prev=$(env_value IMAGE_TAG)
    pull_images "$sha" || die "không kéo được image $(short "$sha") từ GHCR; chưa thay đổi gì"
    git -C "$PLATFORM_DIR" fetch -q origin || die "git fetch lỗi; chưa thay đổi gì"
    git -C "$PLATFORM_DIR" cat-file -e "$sha^{commit}" 2>/dev/null || die "không có commit $sha; chưa thay đổi gì"
    dump_database "$prev" || die "sao lưu database lỗi; chưa thay đổi gì"

    log "Deploy $(short "$sha") ($reason), bản trước $(short "$prev")"
    if switch_to "$sha" && wait_healthy; then
        printf '%s\n' "$sha" >>"$DEPLOYED"
        record "$reason $sha OK (trước: $prev)"
        log "Deploy $(short "$sha") xong, mọi dịch vụ khỏe"
        return 0
    fi

    local problem=${LAST_PROBLEM:-docker compose up lỗi}
    add_skip "$sha"
    record "$reason $sha LỖI: $problem"
    log "Bản $(short "$sha") không khỏe ($problem); quay về $(short "$prev")"
    if ! valid_sha "$prev"; then
        die "không có bản trước để quay về"
    fi
    if switch_to "$prev" && wait_healthy; then
        record "auto-rollback $prev OK"
        die "deploy $(short "$sha") lỗi, đã quay về $(short "$prev"); timer sẽ bỏ qua bản này"
    fi
    record "auto-rollback $prev LỖI: ${LAST_PROBLEM:-docker compose up lỗi}"
    die "quay về $(short "$prev") cũng lỗi (${LAST_PROBLEM:-docker compose up lỗi}). Có thể migration của bản mới không tương thích với bản cũ: khôi phục database từ $LAST_DUMP theo runbook mục 10"
}

cmd_check() {
    local latest current
    latest=$(latest_success_sha) || die "không hỏi được GitHub API"
    current=$(env_value IMAGE_TAG)
    if [[ -z $latest || $latest == "$current" ]] || is_skipped "$latest"; then
        return 0
    fi
    release "$latest" auto
}

cmd_rollback() {
    local current target
    current=$(env_value IMAGE_TAG)
    target=${1:-$(previous_sha "$current")}
    [[ -n $target ]] || die "không có bản nào trước $(short "$current") để quay về"
    [[ $target != "$current" ]] || die "$(short "$target") đang chạy rồi"
    add_skip "$current"
    record "rollback yêu cầu: $current -> $target"
    release "$target" rollback
}

cmd_status() {
    local current
    current=$(env_value IMAGE_TAG)
    printf 'Đang chạy:       %s\n' "$current"
    printf 'Quay về được:    %s\n' "$(previous_sha "$current")"
    printf 'Bỏ qua:          %s\n' "$(paste -sd ' ' "$SKIP")"
    printf 'Lịch sử gần nhất:\n'
    tail -n 10 "$HISTORY" | sed 's/^/  /'
    if command -v systemctl >/dev/null; then
        systemctl list-timers --all --no-pager platform-deploy.timer 2>/dev/null | head -n 2 || true
    fi
}

cmd_unskip() {
    valid_sha "${1:-}" || die "cần SHA đầy đủ 40 ký tự"
    local tmp
    tmp=$(mktemp)
    grep -vxF "$1" "$SKIP" >"$tmp" || true
    mv "$tmp" "$SKIP"
    record "unskip $1"
}

main() {
    [[ -f $ENV_FILE ]] || die "không thấy $ENV_FILE"
    local cmd=${1:-}
    shift || true
    init_state
    case $cmd in
    check) lock && cmd_check ;;
    deploy) lock && release "${1:-}" manual ;;
    rollback) lock && cmd_rollback "${1:-}" ;;
    status) cmd_status ;;
    unskip) lock && cmd_unskip "${1:-}" ;;
    *) die "dùng: platform-deploy check | deploy <sha> | rollback [sha] | status | unskip <sha>" ;;
    esac
}

main "$@"
