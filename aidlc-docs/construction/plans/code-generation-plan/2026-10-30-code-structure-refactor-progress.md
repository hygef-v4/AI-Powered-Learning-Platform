# Refactor execution ledger

> **Đã thay thế theo yêu cầu mới nhất:** chỉ chuyển file/package và sửa tham chiếu. Các phần tách service/editor, thêm public exports, batch lookup, xóa Admin MCP và thêm test/guard trong bản rộng dưới đây đã được hoàn tác. Đây là hồ sơ đề xuất/thực thi cũ; [phạm vi và kiểm tra hiện tại](../../build-and-test/2026-10-30-package-moves-only-results.md).

Plan: [approved refactor plan](../../../inception/plans/2026-10-30-code-structure-refactor-plan.md).

Approval: “Giúp tôi refactor lại dự án theo kế hoạch” — 2026-10-30.

- [x] Baseline and package-independent architecture guard
- [x] Remove unreachable Admin MCP providers
- [x] Subjects pilot and verification
- [x] Migrate remaining backend/frontend features
- [x] Extract editor/draft/document/academics responsibilities
- [x] Stable frontend public imports and batched U05 reads
- [x] Synchronize current AI-DLC/Construction and final verification

Ruling: keep the existing `aidlc-docs` checkout and prior documentation edits; execute directly without subagents, commit, push or deploy.

Ruling: existing characterization tests protect unchanged behavior during moves/extractions; new guard and batch-read behavior get focused regressions first.

Evidence: baseline backend 1611 pass/0 skipped and required PostgreSQL guard passed. Guard RED reproduced the gap; guard/MCP GREEN 35 pass. Subjects pilot: backend 187 pass/0 skipped including PostgreSQL, frontend 85 pass/11 files, typecheck and production build pass.

Completed: all 15 backend units and 18 frontend features migrated; focused extractions, public frontend exports and batched U05 lookups verified. Final backend clean verify 1614 pass/0 skipped, required PostgreSQL guard pass; frontend 601 pass/94 files; lint/typecheck/production build pass.

Ruling: document validator/sanitizer stay package-private in assignments/service to preserve XML/helper access; DTO snapshot factories intentionally cross the internal service/dto boundary. No public unit contract changed.

Ruling: batch migrations share an explicit old/new manifest and get a full clean suite before logic extraction; mixed production classes are split only where their responsibilities justify it. Java tests stay beside package-private persistence helpers.

Not run: browser E2E, real providers, broker faults, load/security/restore acceptance. These do not become passing evidence from build/unit/integration results. No commit/push/deploy.
