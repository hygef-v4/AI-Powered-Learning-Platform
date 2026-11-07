# U09 Assignment Grading — AS-BUILT, persistence cập nhật 2026-11-06

**Structure cleanup 2026-11-06:** concrete JDBC repositories are `EvaluationRepository` and `GradingProposalRepository`; interfaces and SQL remain unchanged. Frontend `formatErrorMessage` lives in `model/format.ts`, with all consumers and its existing test relocated accordingly. [Scope and verification](../../plans/code-generation-plan/2026-11-06-u09-structure-cleanup.md).

**JDBC delta 2026-11-06:** Evaluation/GradingProposal adapters chuyển từ JPA sang Spring JDBC; visibility giữ JDBC. Hai mapping type là `EvaluationRow`/`GradingProposalRow`, hai Spring Data interface đã bỏ. Giữ contract/schema/JSONB, explicit CAS/version và row/advisory transaction locks. [Persistence hiện hành](jdbc-persistence.md); các snapshot/evidence có ngày bên dưới giữ phạm vi lịch sử.

**Merge delta 2026-11-06:** grading uses staging's automatic AI source selection when `AiRequest.source` is omitted, for both single and batch requests. The HTTP DTO no longer requires source, while requestRef and the nested batch request remain mandatory. Explicit PERSONAL requests still work; provider/source consistency, teacher authorization, grade locks and rubric validation remain enforced. Frontend sends through `buildAiRequest`/`requestProposals` without a source-picker dialog. The grading OpenAPI schema is synchronized. [Merge verification and regression](../../build-and-test/2026-11-06-merge-staging-u05-u09-u15.md).

**Convention delta 2026-11-05:** concrete actions use names such as `loadEvaluation`, `saveEvaluation`, `saveMemberGrade`, `loadStudentGrades`, `requestProposals` and `calculateEffectiveItemScores`. Service/repository/validator dependencies identify their roles; original wildcard imports are retained per the user's follow-up preference. Frontend API, helper and callback names are updated with consumers, spies and dynamic mocks. UI `finalize`/`publish` values and hook return keys remain compatible. `Evaluation.hasOfficialGrade` avoids introducing a Jackson bean property; `requireEditableGrade` remains a throwing guard. Existing accessors, repository/interface overrides, JSON/record fields, MCP tools, authorization and grading/resubmission behavior are preserved. [Naming verification and exceptions](../../build-and-test/2026-11-05-u05-u09-u15-conventions-results.md).

**Follow-up delta 2026-11-05:** one-student grade lookup now delegates once to the service and reads class metadata once, selecting only the target contact/row. Backend rubric arithmetic/copy helpers moved unchanged to `model/RubricScoring`; JSON parsing remains in `GradingJson`. Frontend submitted documents derive shared block types and adapt image flags in `model/submissionDocument`, replacing unchecked workspace casting. Public API, grading/authorization/resubmission rules and database schema are unchanged. [Current verification and limits](../../build-and-test/2026-11-05-u05-u09-u15-followup-results.md).

**Cleanup delta 2026-11-04:** optional evaluation navigation filters preserve the originating submission queue; direct/MCP calls remain compatible. Gradebook uses batched contacts and request-local indexes, and export loads sources once. Frontend scoring helpers are in `model/rubricScores.ts`; conflict messages use business error codes. Grade locks, resubmission policy and public unit contracts are unchanged. [Evidence and limits](../../build-and-test/2026-11-04-u05-u09-u15-cleanup-results.md).

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership unchanged.

**Rule delta 2026-11-02 (latest decision):** FINALIZED/PUBLISHED rejects manual/AI/member grade edits with GRADE_LOCKED, even with a reason; FINALIZED publication remains allowed. Resubmission invalidates the previous grade: individual evaluations become SUPERSEDED including published ones; group root/member evaluations are reused and reset to PENDING. Existing audit/history remains; no group revision snapshot or schema/index migration is added. [Decision](../../../inception/requirements/2026-11-02-locked-grades.md).

**Contract delta 2026-11-01:** grading owns `grade_distribution_visibility`, keyed by assignment and class, with `enabled` (default false) and optimistic `version`; migration `V20261101_0900__assignment_grade_distribution_visibility.sql` does not import old class flags. GET/PUT `/api/v1/classes/{classId}/assignments/{assignmentId}/grade-distribution-visibility` returns `{enabled,version,canEdit}`; PUT accepts `{enabled,version}`. Only the current assigned teacher can configure GRADED assignments; class-management role alone cannot. SUBJECT settings are independent per class; unpublished SUBJECT DRAFT/REVIEWED returns `404`. Archived class/retired assignment gives `canEdit=false`, PUT `409`. Content readOnly is independent. Student histogram still requires active enrollment, at least 20 usable published scores, suppression below 5 and no identities/individual scores; Quiz/Practice excluded. Verification for this delta belongs to the current change, not the older reports below.

grading có submission review, AI proposal và structured feedback với evidence/recommendations, manual/finalize/publication/history/gradebook. Official publish là thao tác educator có scope và version; AI không tự công bố. Các reports structured feedback/result delivery/gradebook 25–26/10 là evidence lịch sử, không phải run mới ở HEAD.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [grading/controller](../../../../backend/src/main/java/edu/aiplatform/grading/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [grading/service](../../../../backend/src/main/java/edu/aiplatform/grading/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [grading/repository](../../../../backend/src/main/java/edu/aiplatform/grading/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [grading/model](../../../../backend/src/main/java/edu/aiplatform/grading/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [grading/dto](../../../../backend/src/main/java/edu/aiplatform/grading/dto) | Kiểu dữ liệu request/response riêng. |
| [grading/contract](../../../../backend/src/main/java/edu/aiplatform/grading/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [grading](../../../../frontend/src/features/grading) | [api](../../../../frontend/src/features/grading/api) | [model](../../../../frontend/src/features/grading/model) | [pages](../../../../frontend/src/features/grading/pages) | [components](../../../../frontend/src/features/grading/components) | [hooks](../../../../frontend/src/features/grading/hooks) | — | [tests](../../../../frontend/tests/features/grading) |

Tests: [grading](../../../../backend/src/test/java/edu/aiplatform/grading).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/2026-10-25-u09-feedback-results.md](../../build-and-test/2026-10-25-u09-feedback-results.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u09-gradebook-class-layout-implementation-report.md](../../build-and-test/2026-10-25-u09-gradebook-class-layout-implementation-report.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u09-gradebook-class-layout-manual-test-guide.md](../../build-and-test/2026-10-25-u09-gradebook-class-layout-manual-test-guide.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u09-ui-bugfix-results.md](../../build-and-test/2026-10-25-u09-ui-bugfix-results.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u09-ui-polish-results.md](../../build-and-test/2026-10-25-u09-ui-polish-results.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u09-ai-result-delivery-report.md](../../build-and-test/2026-10-26-u09-ai-result-delivery-report.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u09-feedback-evidence-fix.md](../../build-and-test/2026-10-26-u09-feedback-evidence-fix.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u09-structured-feedback-manual-test-guide.md](../../build-and-test/2026-10-26-u09-structured-feedback-manual-test-guide.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u09-structured-feedback-report.md](../../build-and-test/2026-10-26-u09-structured-feedback-report.md)
