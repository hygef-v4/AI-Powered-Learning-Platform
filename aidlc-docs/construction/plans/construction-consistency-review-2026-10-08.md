# Construction consistency review - SRS and Inception

Review date: 2026-10-08. Request: "check construction doc theo doc mới, inception docs".

## Sources and scope

- [Current UC and screen catalog](../../../docs/use-cases-and-screens.md): 70 UC; screen labels from the latest supplied `screen-flow (1).drawio`.
- [Requirements](../../inception/requirements/requirements.md): FR-001/002/003/004/005/009/014/016/023/024/027/031/032.
- [Stories](../../inception/user-stories/stories.md): 51 stories, including US-PAY-004/005; [primary ownership](../../inception/application-design/unit-of-work-story-map.md): 16 units.
- Scanned the Construction Markdown inventory, current UC/story references, role rules, frontend mappings, code-plan scope and local Markdown links. Inspected the relevant functional rules/flows and affected NFR, infrastructure and code-plan references. Original question/answer files and generated-code summaries are historical evidence, not the authoritative new requirements.
- This is a documentation review, not verification of implemented behavior. No Construction design, plan step, application code or prior approval was changed by this review.

## Findings

### R01 - P1: Teacher is still allowed to edit class structure

**Evidence:** [U04 business rules, BR-U04-13](../u04-subject-class-enrollment/functional-design/business-rules.md:19) includes the assigned teacher among class managers and gives all managers permission to edit name/description/term and class state.

**Current requirement:** FR-003 limits class information/lifecycle changes to Administrator or the assigned Subject Manager; Teacher read access does not grant UC 49. Roster/invite/group support actions must be evaluated separately from structural updates.

**Required synchronization:** Separate structure-management, teaching and roster permissions in U04 rules, flow, API descriptions, frontend actions and code-plan acceptance scenarios. Avoid a single class-manager check granting all actions.

### R02 - P1: Administrator inheritance and assignment eligibility are incomplete

**Evidence:** [U01 BR-U01-60](../u01-account-and-access/functional-design/business-rules.md:76) only states Subject Manager inheritance from Teacher. [U04 BR-U04-03/12](../u04-subject-class-enrollment/functional-design/business-rules.md:9) restricts subject managers to SUBJECT_MANAGER and teachers to TEACHER/SUBJECT_MANAGER; both use a not-DISABLED condition rather than ACTIVE. [U08 BR-U08-01](../u08-assessment-core-publication/functional-design/business-rules.md:7) says ADMIN only reads assignments.

**Current requirement:** FR-002/003 allows ACTIVE Administrator to be assigned as subject manager/class teacher. Assigned Subject Manager/Administrator can perform Teacher actions under R4; subject-resource actions require R2. Administrator's structural Full permissions do not grant unrestricted access to teaching resources.

**Required synchronization:** U01 authorization, U04 assignment validation and U05/U06/U08/U09/U10/U12/U13/U14/U15/U16 permission descriptions must consistently use the relevant assigned scope. Explicitly cover Admin with and without assignments; inheritance does not grant Student functions.

### R03 - P1: Subject management is still treated as teaching access

**Evidence:**

- [U06 BR-U06-02](../u06-rubric-question-bank/functional-design/business-rules.md:8) lets the subject manager edit class-bank questions without being the class teacher.
- [U05 BR-U05-02](../u05-content-material-rag/functional-design/business-rules.md:8) lets the subject manager edit/archive every material in the subject, including class-scoped materials.
- [U15 BR-U15-02](../u15-grading/functional-design/business-rules.md:8) allows subject managers to read the gradebook and forbids ADMIN entirely; BR-U15-52 similarly grants subject-wide grade-history access.
- [U16 BR-U16-31/43](../u16-reporting-notification/functional-design/business-rules.md:37) gives subject managers progress/export access based on managing the subject.

**Current requirement:** R2 is for subject resources. R3/R4 is for class teaching resources, class banks, submissions, grading and UC 37 gradebook/export. Subject management alone is insufficient; assigned Administrator must not be categorically refused.

**Required synchronization:** Narrow subject-manager access where no class-teacher assignment exists, and allow assigned Administrator. Keep Teacher's read/use of permitted ACTIVE subject questions in an assignment selector separate from standalone Subject Question Bank management. U06 already has class/subject scopes; this does not require new bank tables.

### R04 - P1: Admin package management is absent and explicitly prohibited

**Evidence:** [U07 BR-U07-02](../u07-payment-credit/functional-design/business-rules.md:8) says packages are fixed and have no create/edit API or screen. Its F7, domain fields, NFR logical-component compliance and [code plan step 5](./code-generation-plan/u07-payment-credit-code-generation-plan.md:67) seed packages from U07_PACKAGES. Step 23 explicitly excludes package administration. No references to US-PAY-004, FR-031 or Credit Package Setting were found in the existing Construction documents.

**Current requirement:** UC 67/68 and US-PAY-004 require Admin view/add/edit on Credit Package Setting: positive price/credit validation, version checking and audit before/after. Existing pending/paid payments retain their snapshots. Monthly free-credit grants remain deployment configuration.

**Required synchronization:** Add the Admin service/query/API, frontend screen/actions, version/audit fields and relevant validation scenarios across U07 functional/NFR/infrastructure design and code plan. Seeding may remain an initial-data mechanism, but must not overwrite subsequent Admin changes. Do not add package deletion, refunds or manual credit adjustment.

### R05 - P1: Global payment history conflicts with the own-wallet rule

**Evidence:** [U07 BR-U07-52](../u07-payment-credit/functional-design/business-rules.md:56) says no one sees another account's transactions. [Frontend API mapping](../u07-payment-credit/functional-design/frontend-components.md:17) only lists /me/payments. NFR-U07-33 also describes every credit/history API as own-wallet access. US-PAY-005 and FR-032 are absent from the existing Construction design/code plan.

**Current requirement:** UC 69 gives Administrator a read-only platform Payment History, with account/package/time/status filters and pagination. Own-wallet endpoints and Payment Result UC 10 remain restricted to their owner.

**Required synchronization:** Add a separately authorized Admin history query/screen, safe returned fields and denial scenarios. Amend broad prohibitions while retaining owner checks for personal endpoints. This feature does not permit payment editing, manual reconciliation or balance changes.

### R06 - P1: Announcements cannot be updated after creation

**Evidence:** [U05 BR-U05-63](../u05-content-material-rag/functional-design/business-rules.md:59) forbids editing both announcements and comments after sending. Its frontend only documents list/create/hide announcement actions; no announcement update/delete flow is specified.

**Current requirement:** FR-023 and UC 31 require create/update/delete announcements. Updates check version and audit before/after without re-emitting a new-announcement notification. Removal is soft, preserving references/audit. Comment editing remains prohibited.

**Required synchronization:** Split announcement lifecycle from comment rules, specify update/removal semantics, API/form actions, concurrency and audit. Keep the two-comment preview, comments popup and notification-on-create behavior.

### R07 - P2: Removed avatar updates remain in U01/U03 and code plans

**Evidence:** [U01 BR-U01-50/52](../u01-account-and-access/functional-design/business-rules.md:66), profile flow/domain/frontend and NFR designs still include avatarFileId, AvatarUploader and AvatarPort. U03 still defines AVATAR upload/validation; [U01 code-plan step 14](./code-generation-plan/u01-account-and-access-code-generation-plan.md:89) and U03's completed steps include this integration.

**Current requirement:** UC 07 / FR-001 / US-IAM-004 permits display name and phone updates, without avatar updates.

**Required synchronization:** Remove the active profile avatar-update flow, DTO/API fields, frontend action and U01 dependency rationale. Review U03's unused support and generated implementation separately; completed steps/code summaries are historical facts and must not simply be rewritten as if no code existed. Removing existing database fields is not automatically required by removing a user action.

### R08 - P2: Active UC traceability still uses the previous catalog

**Evidence:** 46 non-question/non-plan/non-generated-code design files contain numeric UC references; 15 of the 16 code plans contain them. Many references are to the former 40-UC catalog. Examples: audit UC 39 (now UC 70), statistics UC 18 (now UC 57), payments UC 37 (now UC 08-10/67-69), templates UC 21 (now UC 53-54), and five authoring types UC 23-27 (now UC 39-43). U13's old direct UC ownership also conflicts with its current supporting role.

**Required synchronization:** Rebuild active references by function using the ownership table below. Do not perform a global numeric replacement: old aggregate UCs split into multiple new UCs, and the same number now names a different function. Keep old numbers in dated questions, audit and approval history; mark their baseline when retained in completed plans.

| Unit | Current primary UC IDs |
|---|---|
| U01 | 01-07, 58-62 |
| U02 | 70 |
| U03 | None; shared infrastructure |
| U04 | 12-13, 27-28, 45-50, 63-66 |
| U05 | 14, 26, 29-31, 51-52 |
| U06 | 32-33, 44, 55-56 |
| U07 | 08-10, 67-69 |
| U08 | 38 |
| U09 | 39-43 |
| U10 | 53-54 |
| U11 | 17-22, 24-25 |
| U12 | 15-16 |
| U13 | None; AI/code execution support |
| U14 | 23 |
| U15 | 34-37 |
| U16 | 11, 57 |

### R09 - P2: Screen labels and entry mappings do not match the latest diagram

**Evidence:** Current frontend documents use Teacher Menu/Class List/Student Menu in U04, AI Credits in U07, Upload/View Learning Materials and Announcements in U05, generic student Assignment List/Assignment Overview/Submitted Assignment in U11, Grading Queue in U15 and Template List in U10.

**Required synchronization:**

- U04: My Classes for Student, Assigned Classes for Teacher and Subject Classes for subject administration.
- U05: Uploaded Learning Materials, Learning Material and Class Announcements.
- U06: distinguish Class Question Bank and Subject Question Bank entries, reusing the existing scope-aware BankPage/Question Editor where suitable.
- U07: Credit Packages plus new Credit Package Setting and Admin Payment History.
- U10: Subject Template; Template Editor already exists in design and should map to UC 54.
- U11: the five Student assignment lists (Quiz, Codelab, Text Essay, Diagram Essay, Group Essay Assignments), Assignment Detail, Submission History and the corresponding workspaces. Map the Student Practice AI request to Assignment Detail.
- U15: Student Submissions and Grading Workspace; map gradebook/export to Class Detail for UC 37. U16 statistics should map to Statistic.

These are screen-entry/mapping corrections, not automatic instructions to rename every internal class/route or discard working shared components. Internal symbols such as MyClassesPage, TemplateListPage or GradingQueuePage can remain if their user-facing label, access and entry are documented correctly. The diagram alone does not authorize deletion of supporting UI flows.

### R10 - P2: Local design-source links are broken

**Evidence:** The active design/plan scan found 21 broken Markdown links across 21 files: 19 to `docs/database.md`, 2 to `docs/screen-flow.md`. Neither file exists in this checkout. This is an existing documentation defect, separate from the new screen-label changes.

**Required synchronization:** Screen references should use the current UC/screen catalog and latest existing Drawio file. Database references need an existing, verified source or clear per-unit model references; do not invent a global database document or hardcode an old table count. Also review inline mentions of retired catalogs such as docs/use-case-table.md.

## Behaviors already aligned or retained

- U02 already specifies Administrator-only, read-only audit filtering by actor/action/object/result/time. UC 70 requires traceability/name synchronization, not a second audit implementation.
- U10 already specifies template list/editor, create/edit and lifecycle-safe delete; the new diagram adds mappings, not an entirely new template engine. R2/Admin eligibility still needs synchronization.
- U11/U13/U15 already separate private Student Practice AI results from Teacher AI proposals; submission does not automatically run Practice Essay AI grading. Map these to UC 25 and UC 35 separately.
- Existing credit reservation/idempotent settlement, payment snapshots, webhook verification, provider failures and private Practice results remain relevant. Monthly grants remain configuration; automatic PayOS reconciliation remains a supporting job.
- Five assignment types, group submission by leader, rubric pinning/version history, AI as a proposal, deterministic Quiz/Code Lab scoring and no Practice results in official gradebooks remain applicable.
- Gemini adapter/model/API details are provider-specific implementation references, not automatically terminology errors. Use AI/AI Grading Proposals in business-facing descriptions; preserve real provider names in technical configuration.
- Existing module content, roster/invite/group operations, comments, AI drafts, copy/version/review/publish and notification preferences remain supporting flows unless explicitly superseded. Their lack of a separate new UC does not remove them.

## Recommended synchronization order

1. U01/U04 authorization and assignment eligibility, then propagate R2/R3/R4 through affected units.
2. U07 package administration/global history and U05 announcement lifecycle.
3. U01/U03 avatar scope; retain historical generated-code evidence and identify any separate implementation cleanup.
4. UC/story ownership and frontend screen mappings across functional/NFR/infrastructure design, then code plans/validation scenarios and source links.

Prior approvals refer to the previous baseline and do not establish that the new requirements are implemented. Keep the approved/completed history while adding explicit revision scope. No stage advanced during this review.

## Verification limits and extension status

The review compared local documentation with the current SRS-derived Inception/catalog; it did not test application code, migrations or live integrations. Enabled Security authorization/audit constraints inform findings R01-R06; existing documentation does not meet the revised permission baseline until synchronized. Existing enabled Resiliency constraints are retained; no new runtime resilience conclusion is asserted. Runtime-only checks are N/A, and disabled Property-Based Testing was skipped.

## Resolution - documentation revision 2026-10-08

The findings above describe the pre-revision snapshot. Their old line numbers locate historical evidence; current files have moved. All ten groups are resolved in the documentation revision, with implementation work still unchecked. No runtime behavior or new approval is asserted.

| Finding | Updated documentation evidence |
|---|---|
| R01 | [U04 rules](../u04-subject-class-enrollment/functional-design/business-rules.md): structure versus roster/teaching actions |
| R02 | [Shared scope contract](../current-srs-contract.md), [U01](../u01-account-and-access/functional-design/business-rules.md), [U04](../u04-subject-class-enrollment/functional-design/business-rules.md): ACTIVE eligible inherited roles and assignments |
| R03 | [U05](../u05-content-material-rag/functional-design/business-rules.md), [U06](../u06-rubric-question-bank/functional-design/business-rules.md), [U15](../u15-grading/functional-design/business-rules.md), [U16](../u16-reporting-notification/functional-design/business-rules.md): class-teacher scope for teaching resources |
| R04–R05 | [U07 functional flows](../u07-payment-credit/functional-design/business-logic-model.md), domain/frontend/NFR/infrastructure and code plan: Admin package add/edit and read-only global history |
| R06 | [U05 functional flows](../u05-content-material-rag/functional-design/business-logic-model.md), domain/frontend/NFR/infrastructure and code plan: versioned announcement update/soft-delete; immutable comments |
| R07 | [U01 profile](../u01-account-and-access/functional-design/business-rules.md), [U03 scope](../u03-file-and-artifact/functional-design/business-rules.md): no active avatar update; [U03 code-plan follow-up](./code-generation-plan/u03-file-and-artifact-code-generation-plan.md) preserves completed code history |
| R08 | [Current ownership](../current-srs-contract.md): 70 UC/51 stories mapped once across 16 units; current design/code-plan headers and function-specific references revised |
| R09 | Per-unit frontend mappings and [current catalog](../../../docs/use-cases-and-screens.md): latest labels/entries; Student Practice AI starts at Assignment Detail |
| R10 | Missing global database/screen links replaced by existing per-unit domain models and current catalog; local-link validation recorded in [sync plan](./construction-sync-2026-10-08.md) |

Existing contracts/ports and generated application code were not changed. Follow each code plan's unchecked revision tasks before treating the changed functionality as implemented. Historical questions, original approvals and completed U03 steps remain preserved.
