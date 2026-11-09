# Material and quiz UI Implementation Plan

> For agentic workers: execute approved tasks with TDD and verification; preserve existing Excel changes. User approved the proposal with “ok giúp tôi sửa theo kế hoạch”.

**Goal:** Enabled materials, formatted descriptions, multi-select AI references and authoring within the question section.

**Architecture:** Existing material/quiz services and React forms remain the owners. Add optional structured formatted text alongside existing plain description/instructions so old clients retain text. Disabled is independent of archive/publish state. Touched material persistence uses JDBC per project conventions; preserve transaction/version behavior. No new dependencies.

**Spec:** [approved proposal](../../../inception/requirements/2026-11-09-material-quiz-ui-proposal.md).

## Contract and decisions

- Material exposes `enabled: boolean`, `descriptionDocument?: FormattedText | null`; old materials default enabled, old requests retain behavior. Create/update may carry optional enabled/document fields. Quiz description uses optional `instructionsDocument` alongside plain `instructions`.
- FormattedText is `{ blocks: [{ type: "PARAGRAPH" | "UL" | "OL", align: "LEFT" | "CENTER" | "RIGHT" | "JUSTIFY", indent: number, runs: [{ text: string, bold?: boolean, italic?: boolean, underline?: boolean }] }] }`. Reuse existing run tools; validate bounded block/run counts, alignment, indentation and plain length at server; render React nodes, not untrusted HTML. Preserve old plain description text.
- Accepted Disabled default: hide from students, reject student direct view/download and new AI reference use; staff can manage/re-enable. No destruction of history.
- Independent domains may be delegated under dispatching-parallel-agents: backend storage/contracts, shared formatted editor/material UI, and root quiz inline/source UI. Do not overlap files. Root integrates and verifies.
- No application restart, live migration, commit, push or deployment without separate instruction. Use existing dirty worktree and preserve unrelated approved Excel diff.

## Tasks and checks

- [x] 1. Read existing contracts, repository call sites, shared editor/quiz forms, rules and proposal. Establish shared format above; user approval covers implementing and testing this scope.
- [x] 2. Backend: migration for enabled/formatted fields, bounded structured descriptions and legacy contracts; material JDBC conversion, student list/direct/token issuance and redemption/reference gates. Disposable PostgreSQL migration/roundtrip, stale version and old signed token disable/re-enable tests included in 533-case clean run. Final whitespace preservation review fixes tracked in task 5.
- [x] 3. Shared compact editor/viewer and material/quiz descriptions, Enabled switch, legacy text and safe rendering; 58 scoped frontend tests passed before integration, server bounds mirrored.
- [x] 4. Manual/bank/AI authoring within question section, existing-item edit, searchable multi-select/chips/10-limit/disabled reasons, persistent source warnings, empty-source rejection and retry. Review regressions added for version/format retention, pending saves and navigation protection; targeted frontend tests passed.
- [x] 5. Backend clean533/0skip plus focused104/119/10 and required PostgreSQL guard passed, JAR packaged. Frontend836/124file, full lint, production build/typecheck passed; gutter correction82test/10file and build/typecheck passed; final copy-only labels55test/8file passed. User takes manual UI acceptance; no browser/mobile/provider success claimed.
- [x] 6. Updated current contracts/U13/U10 docs, audit/state and [final results](../../build-and-test/2026-11-09-material-quiz-ui-results.md). Source/test/OpenAPI diff check passed; raw audit preserves original input spaces. No live migration/restart/commit/deploy.

## Review focus

- Direct URLs and signed download issuance must respect disabled visibility.
- Default-enabled and null rich descriptions preserve existing records and callers.
- Sanitized structured formatting survives create/edit/view without losing Vietnamese text or cursor position.
- Switching question modes and errors must not silently discard unsaved edits; version conflicts remain visible.
- AI uses only enabled/readable/extracted references; no claim that a YouTube URL provides transcript; truncation must be visible, not silently omitted.

Security/Resiliency scopes retained; PBT disabled. Runtime/DB/provider acceptance must be reported separately from unit tests.

## Execution decisions and review

- Ruling: user approval applies to proposed Disabled default (hide students, retain staff access, exclude new AI source requests). Archive/publish lifecycle stays separate.
- Ruling: touched material persistence converted to JDBC per required code conventions, not expanded into a module-wide persistence migration. Obsolete two JPA row classes removed only after caller search; clean backend output verified.
- Ruling: bounded download owner-policy hook checks issued material tokens at redemption, so previously issued student links cannot bypass a later disable; no new token format.
- Read-only independent review found four Important edit-loss cases: settings save stripping rich descriptions, mode switching during pending save, failed AI retry resetting input, and browser traversal bypassing navigation protection. All received focused regression tests/fixes; reviewer re-read fixes without remaining Important findings in those paths.
- User steered: “cứ sửa trươc sdid sau tôi tự test ui”. Stop manual UI checks; preserve automatic tests/build and leave manual acceptance to user. Browser tab created for local inspection closed; CUA capture host unavailable, no screenshot-based acceptance claimed.
- shortcut: older browsers without Navigation API restore canceled Back/Forward with pushState and may drop the forward path; remove fallback when supported browsers all expose Navigation API. Current unsaved form is retained.
- User follow-up screenshots: fixed list marker gutter in shared editor/viewer; then replaced English visible status labels with “Bật/Tắt” at create/edit/detail/list. No manual UI test per steering; scoped checks in final report.
