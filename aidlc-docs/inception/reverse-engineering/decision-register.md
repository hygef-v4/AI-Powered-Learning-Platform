# Decision register — AS-BUILT 2026-10-30, persistence decision 2026-11-06

## JDBC toàn dự án — quyết định 2026-11-06

Người dùng hủy hướng chuyển U05/U09 sang JPA và xác nhận: “Chuyển U05/U09 sang JDBC và sửa AI-DLC quy định toàn dự án dùng JDBC”. [Quy định persistence chung](../../construction/persistence-conventions.md) thay lựa chọn Spring Data JPA/ORM trong tech-stack và các kế hoạch persistence cũ cho code mới/migration. JDBC gồm JdbcTemplate, NamedParameterJdbcTemplate hoặc JdbcClient; native SQL qua EntityManager không đáp ứng quy định.

U05 đã JDBC; U09 chuyển hai adapter evaluation/proposal, giữ visibility JDBC. U04/U13/U14 hiện còn JPA, cần migration riêng; giữ hạ tầng JPA chung cho các owner đó trong giai đoạn chuyển tiếp, không tuyên bố toàn bộ code đã JDBC. Giữ schema/API/JSONB, locks/CAS/version, transaction/audit và public contracts. Các decision/evidence có ngày cũ bên dưới là lịch sử; không viết lại kết quả test cũ thành kết quả mới. [Kế hoạch và phạm vi](../../construction/plans/code-generation-plan/2026-11-06-u05-u09-jdbc.md), [bằng chứng mới](../../construction/build-and-test/2026-11-06-u05-u09-jdbc-results.md).

## Baseline lịch sử 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`. User approved documentation execution: “Giúp tôi triển khai theo plan”. Source-observed behavior is documented without approving new application behavior.

| Delta | Previous requirement | Current implementation | Disposition |
|---|---|---|---|
| D01–D04 scope decisions 15/10 | Quiz Practice, GROUP Graded, no Student AI for GROUP, educator/day plus global quota | Retain scope decisions; details traced per unit | Checklist feedback 29/10 does not automatically supersede these decisions |
| D05 council scope | Backup/activity/email/provenance | Source exists; runtime acceptance is separate | No NFR certification |
| D03 audit | Student MCP, no Admin MCP | MCP_SELF for Teacher/Subject Manager/Admin; Student personal API remains | Merge staff expansion 26/10 |
| D04 audit | Single institutional key/provider | Multiple keys/provider | Document AS-BUILT cardinality |
| D05 audit | MCP token shown once | Owner can reveal ACTIVE credential if ciphertext exists | Document encryption, audit and legacy-key limits |
| D06 audit | OPEN requires teacher | Domain/service/DB permit no teacher; create remains DRAFT | Document source behavior, not migration comment alone |
| D07 audit | Configurable 1–365 day retention | Activity retention is fixed at 1 day | No automatic penalty inferred |
| D08 audit | Leader request/reject reason required | Optional, maximum 1,000 characters | Update validation and flow |
| D09 audit | Module only an extension | Basic module is implemented in U13 | Keep advanced Lesson/RAG separate |
| D10 audit | Only edit DRAFT, retire OPEN with reason, no reopen | OPEN edit stays OPEN; retire non-RETIRED; reopen DRAFT/OPEN | Quiz REVIEWED removed 2026-11-01 (D-Q2 below); Assignment REVIEWED remains |
| D-Q1 2026-11-02 | FR-039: no automatic penalty | Still no automatic penalty. Quiz only: configurable suggested penalty computed at submit; the teacher applies an amount or dismisses it per attempt, with a reason; no signal means flag and no suggestion | User decision in the [question file](../requirements/2026-11-01-quiz-rework-questions.md), Question 2 option B, replacing the 2026-11-01 default of automatic deduction. Assignment unchanged |
| D-Q2 2026-11-01 | Quiz DRAFT → REVIEWED → OPEN, author approves own quiz | DRAFT → OPEN; same validation at publish | Feedback 29/10 implemented |
| D-Q3 2026-11-01 | Only Subject Manager writes to the bank | Teachers propose, manager approves or rejects; quiz-own questions need no approval | Feedback 29/10 implemented |
| D-Q4 2026-11-01 | Near-duplicate via `pg_trgm` proposed in the assessment | Trigram in Java; exact via SHA-256 | Role `migrator` has no CREATE on the database, so `create extension` could break existing environments |
| D-Q5 2026-11-01 | Quiz could attach reference materials | Attachment removed from API and UI; column `reference_material_ids` kept unused until checked on staging | AI source materials remain |
| D15 discovered during execution | FR-012/BR-U06-03 prohibit all API-key readback | AiConnectionService.revealInstitutional and AiAdminController permit Admin reveal for non-REVOKED institutional connections, including DISABLED; AI_CONNECTION_KEY_REVEALED audit | Document AS-BUILT exception; security design approval pending, no code/security change in this synchronization. Admin cannot thereby read personal keys |

## Execution rulings

- Execute directly in the existing checkout to preserve uncommitted plan/state/audit context; no worktree migration, commit or push.
- No production-code changes, so application TDD is not applicable. Verify document links/manifests, OpenAPI, diagram XML/renders and final diff.
- Preserve dated audit and test evidence; replace stale current descriptions rather than cover them with an ambiguous new note.
- Existing four OpenAPI files remain scoped specs. Controller inventory documents APIs without an OpenAPI spec instead of guessing their DTOs.

## User override and remediation — 2026-10-30

Previous D03 role findings describe the initial794a4053 audit. The user now explicitly removes Admin MCP and restores Student MCP for the same submitted-own PRACTICE feedback operation as personal APIs. No new AI Quiz-result explanation is introduced. Teacher/Manager tools stay scoped. Current spec: [remediation scope](../requirements/2026-10-30-test-remediation-student-mcp.md).

## Bug remediation — 2026-11-01

User request: “sửa toàn bộ 26 bug trên”, then “sửa doc toàn dự án cho khớp với các bug bạn vừa sửa”. Commit `1cd58c02` changes these documented behaviors; full list in [review-bugs remediation](../../construction/remediation/review-bugs-2026-11-01/README.md).

| Delta | Previous documentation | Current implementation | Disposition |
|---|---|---|---|
| R01 BR-U03-07 | Unattached uploads kept, no cleanup job | Unattached uploads tracked in Redis and deleted 10 minutes after FileRef expiry; attach needs Redis | Supersedes the earlier design decision |
| R02 D10 / BR-U10-13 | OPEN edit stays OPEN | OPEN edit stays OPEN only while no attempt exists; question edits after an attempt return 409 `QUIZ_HAS_ATTEMPTS` | Refines D10; new version required for question changes |
| R03 BR-U10-23 | Reopen RETIRED to OPEN when published | Rejected with 409 `QUIZ_OTHER_VERSION_OPEN` while another version of the chain is OPEN | Refines reopen |
| R04 BR-U06-22 | Redelivery with dispatch mark goes UNKNOWN | Stays RUNNING inside `requestTimeout` + 30 s, UNKNOWN after | Avoids losing an in-flight result |
| R05 BR-U11-12 | Enrollment checked when the attempt starts | Enrollment re-checked for get/save/submit/activity; results stay readable | BR-U11-20 unchanged |
