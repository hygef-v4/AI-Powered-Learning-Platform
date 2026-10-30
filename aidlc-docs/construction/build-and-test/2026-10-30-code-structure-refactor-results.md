# Code structure refactor — results 2026-10-30

> **Đã thay thế theo yêu cầu mới nhất:** chỉ chuyển file/package và sửa tham chiếu. Các phần tách service/editor, thêm public exports, batch lookup, xóa Admin MCP và thêm test/guard trong bản rộng dưới đây đã được hoàn tác. Đây là hồ sơ đề xuất/thực thi cũ; [phạm vi và kiểm tra hiện tại](2026-10-30-package-moves-only-results.md).

Implemented on branch `aidlc-docs`, parent HEAD `5a76e428`, in the working tree. User approval: “Giúp tôi refactor lại dự án theo kế hoạch”. No commit, push, deployment or application database migration.

## Result

- Backend keeps the 15 business units and public `contract` packages. Internals now use `controller`, `service`, `repository`, `model`, `dto`, with `config`/`integration` where needed. Concrete JDBC repositories stay concrete; existing interfaces/adapters stay intact.
- Frontend keeps its 18 business features and App Router routes. API, model, pages, components and hooks are grouped within each feature; meaningful identity/AI subgroups remain. Cross-feature consumers use small public exports or existing slots.
- Removed the two unreachable Admin-only MCP implementations and their obsolete positive test. Catalog tests retain a synthetic forbidden Admin provider to prove the deny policy still works. Public `McpInput.admin` remains unchanged; normal Admin web APIs remain available.
- Architecture guard now checks every unit-internal package, independent of layer names. Regression fixtures check cross-unit rejection and same-unit collaboration for controller/service/repository/model/dto.
- U05 is split into `AcademicsQueryService`, `ClassService` and `EnrollmentService`. `AcademicsAccess` owns current-role/scope checks and locks; `AcademicsOperations` owns transaction/error/audit policy. Existing lock order, rechecks and per-row `REQUIRES_NEW` behavior are retained.
- Document editing separates pure block transformations, selection/caret, controls/toolbar primitives and block editors. Assignment AI drafting separates source upload/cleanup from draft request/poll/apply state and dialog rendering.
- `DocumentModelService` keeps `IDocumentModel`; validation and sanitization now have separate package-private collaborators. These helpers stay in the same package to preserve access to existing document/XML helpers.
- Class lists batch teacher contacts and reuse repeated subjects only within the request. Student lists batch contacts. Current-role checks remain separate; ordering, missing-contact errors, single-item reads and DTO shape remain protected.
- README and current Inception/Construction inventories, logical components and test instructions reflect the new paths. Historical acceptance reports remain dated evidence.

## Readability checkpoints

| Previous file | Before | Current entry |
|---|---:|---:|
| `DocumentEditor.tsx` | 1,124 lines | 579 lines, with focused editor modules |
| Assignment `AiDraftDialog.tsx` | 689 lines | 393 lines, with two lifecycle hooks |
| `DocumentModelService.java` | 711 lines | 132 lines, with validator/sanitizer |
| `AcademicsService.java` | 397 lines | Query 172 / class 127 / enrollment 113 lines, plus focused access/operation policy |

These numbers describe navigation and responsibility, not net code reduction. No new dependency, generic CRUD layer, base service or service interface/Impl pair was added.

## Verification

| Check | Fresh result |
|---|---|
| Backend baseline before production edits | 1,611 passed, 0 failures/errors/skips |
| Architecture guard regression | RED on the old selector; GREEN after selecting all non-contract unit internals |
| Subjects pilot | Backend 187 passed/0 skips including PostgreSQL; frontend 85 passed/11 files; typecheck and production build passed |
| Full suite after structural moves | Backend 1,611 passed/0 skips; frontend 599 passed/94 files |
| Editor/draft characterization | 40 passed; includes source-upload abort and fresh-version apply, run before and after draft extraction |
| U05 batch regressions | Three assertion failures before batching; then GREEN, together with DTO/auth/transaction tests |
| Focused final extraction/batch suite | 261 passed, 0 failures/errors/skips |
| Final backend `mvn clean verify` | **1,614 passed, 0 failures/errors/skips; BUILD SUCCESS** |
| Required PostgreSQL guard | **PASS**: SubjectRepositoryIntegrationTest 31, NotificationDeliveryIntegrationTest 2; no skips |
| Final frontend Vitest | **601 passed across 94 files**; exit 0 |
| Final TypeScript | **Passed**; exit 0 |
| Final ESLint | **Passed**, no warnings/errors; exit 0 |
| Final production build | **Passed**; exit 0 |
| Current documentation validation | **45 Markdown files** checked; no broken local links or unbalanced fences |
| Compatibility/diff review | **46 controller annotation sets unchanged**; public contract/schema/config/dependency files untouched; `git diff --check` passed |

Logs are under `.tmp/2026-10-30-refactor-*.log`; final logs use `final-{backend,db-guard,frontend,typecheck,lint,build}`. Dependency versions/lockfiles, Flyway migrations, external contract files and infrastructure configuration were not changed. Required-test suite names in `scripts/assert-required-backend-tests.ps1` follow their new packages.

Tests ran with JDK 21 targeting Java 17, Node 25 and disposable PostgreSQL/Redis containers. CI uses Java 17/Node 22. A frontend run with unrestricted worker concurrency terminated in the native runtime; the retained complete verification uses `--maxWorkers=2`. Existing React `act` warnings remain in jsdom test output; they are not test failures.

## Scope limits

Automated tests, compilation and build support behavior preservation; they do not certify all 66 UC or production performance. Browser E2E, real AI/Drive/SMTP/broker fault injection, load tests, security certification and restore acceptance were not run. No application database or real provider was used. Security/resiliency decisions remain in scope; property-based testing stays disabled.

Applicable security controls (logging, headers, validation, authorization, credential handling and safe errors) are preserved through this refactor and existing scoped tests. Deployment/rollback and health-check redesign are N/A because configuration is unchanged. Timeout/isolation guards are retained; these statements are not runtime control certification.

## References

- [Approved plan](../../inception/plans/2026-10-30-code-structure-refactor-plan.md)
- [Execution ledger](../plans/code-generation-plan/2026-10-30-code-structure-refactor-progress.md)
- [Current source/test inventory](../../inception/reverse-engineering/current-code-baseline.md)
