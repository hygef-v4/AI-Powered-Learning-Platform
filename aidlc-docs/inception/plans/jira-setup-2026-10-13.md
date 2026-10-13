# Jira AI-DLC setup — 2026-10-13

User request: giúp tôi setup jira theo đề xuất, nma tôi chưa phân công hết người làm unit nào

## Applied configuration

Project: [SEP490-G21-Fall26 (KAN)](https://sep490-g21-fall26.atlassian.net/jira/software/projects/KAN/boards/2).
Scope source: current Inception/Construction revision 2026-10-13, 66 active UC, 50 MVP Stories plus 1 optional extension, 15 logical units.

Created 106 issues: 15 unit Epics plus 1 coordination Epic, 51 Stories, 30 unit review/integration-test Tasks, 4 open-decision Tasks, 4 acceptance-gate Tasks and 1 operating-guide Task.

All created issues verified To Do, Unassigned and without a due date. Existing owners, In Progress statuses and 2026-10-17 due dates for KAN-11–17 preserved; these seven documentation/report issues now use Task and belong to the coordination Epic. Historical KAN-4–10 retain Done and their owners/dates, with explicit baseline scope notes and labels.

[Coordination Epic](https://sep490-g21-fall26.atlassian.net/browse/KAN-33) · [Operating guide and pending ownership](https://sep490-g21-fall26.atlassian.net/browse/KAN-123).

## Unit containers

| Unit | Epic | Wave | Iteration labels | Requirements/design review | Independent integration tests | Owner |
|---|---|---|---|---|---|---|
| U01 Account & Access | [KAN-18](https://sep490-g21-fall26.atlassian.net/browse/KAN-18) | 1 | iteration-1 | [KAN-85](https://sep490-g21-fall26.atlassian.net/browse/KAN-85) | [KAN-86](https://sep490-g21-fall26.atlassian.net/browse/KAN-86) | Pending |
| U02 Audit | [KAN-19](https://sep490-g21-fall26.atlassian.net/browse/KAN-19) | 1 | iteration-1 | [KAN-87](https://sep490-g21-fall26.atlassian.net/browse/KAN-87) | [KAN-88](https://sep490-g21-fall26.atlassian.net/browse/KAN-88) | Pending |
| U03 File, Job, Event & Settings | [KAN-20](https://sep490-g21-fall26.atlassian.net/browse/KAN-20) | 1 | iteration-1, iteration-2 | [KAN-89](https://sep490-g21-fall26.atlassian.net/browse/KAN-89) | [KAN-90](https://sep490-g21-fall26.atlassian.net/browse/KAN-90) | Pending |
| U04 Subject, Class & Enrollment | [KAN-21](https://sep490-g21-fall26.atlassian.net/browse/KAN-21) | 1 | iteration-1, iteration-2 | [KAN-91](https://sep490-g21-fall26.atlassian.net/browse/KAN-91) | [KAN-92](https://sep490-g21-fall26.atlassian.net/browse/KAN-92) | Pending |
| U05 Reference Content & Announcements | [KAN-22](https://sep490-g21-fall26.atlassian.net/browse/KAN-22) | 2 | iteration-2 | [KAN-93](https://sep490-g21-fall26.atlassian.net/browse/KAN-93) | [KAN-94](https://sep490-g21-fall26.atlassian.net/browse/KAN-94) | Pending |
| U06 Rubrics & Subject Quiz Bank | [KAN-23](https://sep490-g21-fall26.atlassian.net/browse/KAN-23) | 2 | iteration-2 | [KAN-95](https://sep490-g21-fall26.atlassian.net/browse/KAN-95) | [KAN-96](https://sep490-g21-fall26.atlassian.net/browse/KAN-96) | Pending |
| U07 AI Usage & Teacher Quota | [KAN-24](https://sep490-g21-fall26.atlassian.net/browse/KAN-24) | 2 | iteration-2 | [KAN-97](https://sep490-g21-fall26.atlassian.net/browse/KAN-97) | [KAN-98](https://sep490-g21-fall26.atlassian.net/browse/KAN-98) | Pending |
| U08 Assignment/Quiz Lifecycle & Copy | [KAN-25](https://sep490-g21-fall26.atlassian.net/browse/KAN-25) | 2 | iteration-2 | [KAN-99](https://sep490-g21-fall26.atlassian.net/browse/KAN-99) | [KAN-100](https://sep490-g21-fall26.atlassian.net/browse/KAN-100) | Pending |
| U09 Assignment Documents & Quiz Authoring | [KAN-26](https://sep490-g21-fall26.atlassian.net/browse/KAN-26) | 3 | iteration-2 | [KAN-101](https://sep490-g21-fall26.atlassian.net/browse/KAN-101) | [KAN-102](https://sep490-g21-fall26.atlassian.net/browse/KAN-102) | Pending |
| U11 Individual Work & Quiz Submission | [KAN-27](https://sep490-g21-fall26.atlassian.net/browse/KAN-27) | 3 | iteration-2 | [KAN-103](https://sep490-g21-fall26.atlassian.net/browse/KAN-103) | [KAN-104](https://sep490-g21-fall26.atlassian.net/browse/KAN-104) | Pending |
| U12 Group & Allocation | [KAN-28](https://sep490-g21-fall26.atlassian.net/browse/KAN-28) | 2 | iteration-2 | [KAN-105](https://sep490-g21-fall26.atlassian.net/browse/KAN-105) | [KAN-106](https://sep490-g21-fall26.atlassian.net/browse/KAN-106) | Pending |
| U13 AI Connections & MCP | [KAN-29](https://sep490-g21-fall26.atlassian.net/browse/KAN-29) | 4 | iteration-2 | [KAN-107](https://sep490-g21-fall26.atlassian.net/browse/KAN-107) | [KAN-108](https://sep490-g21-fall26.atlassian.net/browse/KAN-108) | Pending |
| U14 Group Workspace & Submission | [KAN-30](https://sep490-g21-fall26.atlassian.net/browse/KAN-30) | 3 | iteration-2 | [KAN-109](https://sep490-g21-fall26.atlassian.net/browse/KAN-109) | [KAN-110](https://sep490-g21-fall26.atlassian.net/browse/KAN-110) | Pending |
| U15 Grading & Gradebook | [KAN-31](https://sep490-g21-fall26.atlassian.net/browse/KAN-31) | 3 | iteration-2 | [KAN-111](https://sep490-g21-fall26.atlassian.net/browse/KAN-111) | [KAN-112](https://sep490-g21-fall26.atlassian.net/browse/KAN-112) | Pending |
| U16 Reporting & Notification | [KAN-32](https://sep490-g21-fall26.atlassian.net/browse/KAN-32) | 4 | iteration-1, iteration-2 | [KAN-113](https://sep490-g21-fall26.atlassian.net/browse/KAN-113) | [KAN-114](https://sep490-g21-fall26.atlassian.net/browse/KAN-114) | Pending |

U04: Iteration 1 Admin subject management; Iteration 2 classes/enrollment. U16: Iteration 1 basic Admin Dashboard; Iteration 2 academic reporting/notifications. U03 backup foundations start in Iteration 1; cross-unit restore/demo evidence is completed when dependent data is available. Iteration labels on an Epic identify delivery participation, not a promise the entire Epic is in one iteration. U10 remains retired.

## Story mapping

| Source story | Jira | Unit | Scope |
|---|---|---|---|
| US-AIC-001 | [KAN-34](https://sep490-g21-fall26.atlassian.net/browse/KAN-34) | U13 | MVP |
| US-AIC-002 | [KAN-35](https://sep490-g21-fall26.atlassian.net/browse/KAN-35) | U13 | MVP |
| US-AIC-003 | [KAN-36](https://sep490-g21-fall26.atlassian.net/browse/KAN-36) | U07 | MVP |
| US-AIC-004 | [KAN-37](https://sep490-g21-fall26.atlassian.net/browse/KAN-37) | U07 | MVP |
| US-AIG-001 | [KAN-38](https://sep490-g21-fall26.atlassian.net/browse/KAN-38) | U13 | MVP |
| US-AIG-002 | [KAN-39](https://sep490-g21-fall26.atlassian.net/browse/KAN-39) | U13 | MVP |
| US-AIG-003 | [KAN-40](https://sep490-g21-fall26.atlassian.net/browse/KAN-40) | U13 | MVP |
| US-ASM-001 | [KAN-41](https://sep490-g21-fall26.atlassian.net/browse/KAN-41) | U08 | MVP |
| US-ASM-003 | [KAN-42](https://sep490-g21-fall26.atlassian.net/browse/KAN-42) | U11 | MVP |
| US-ASM-004 | [KAN-43](https://sep490-g21-fall26.atlassian.net/browse/KAN-43) | U09 | MVP |
| US-ASM-006 | [KAN-44](https://sep490-g21-fall26.atlassian.net/browse/KAN-44) | U09 | MVP |
| US-ASM-008 | [KAN-45](https://sep490-g21-fall26.atlassian.net/browse/KAN-45) | U08 | MVP |
| US-ASM-009 | [KAN-46](https://sep490-g21-fall26.atlassian.net/browse/KAN-46) | U08 | MVP |
| US-ASM-010 | [KAN-47](https://sep490-g21-fall26.atlassian.net/browse/KAN-47) | U08 | MVP |
| US-ASM-012 | [KAN-48](https://sep490-g21-fall26.atlassian.net/browse/KAN-48) | U11 | MVP |
| US-AUD-001 | [KAN-49](https://sep490-g21-fall26.atlassian.net/browse/KAN-49) | U02 | MVP |
| US-CAT-001 | [KAN-50](https://sep490-g21-fall26.atlassian.net/browse/KAN-50) | U04 | MVP |
| US-CAT-002 | [KAN-51](https://sep490-g21-fall26.atlassian.net/browse/KAN-51) | U04 | MVP |
| US-CAT-003 | [KAN-52](https://sep490-g21-fall26.atlassian.net/browse/KAN-52) | U04 | MVP |
| US-CNT-001 | [KAN-53](https://sep490-g21-fall26.atlassian.net/browse/KAN-53) | U05 | MVP |
| US-CNT-002 | [KAN-54](https://sep490-g21-fall26.atlassian.net/browse/KAN-54) | U05 | MVP |
| US-CNT-004 | [KAN-55](https://sep490-g21-fall26.atlassian.net/browse/KAN-55) | U05 | MVP |
| US-CNT-005 | [KAN-56](https://sep490-g21-fall26.atlassian.net/browse/KAN-56) | U05 | EXTENSION |
| US-GRD-001 | [KAN-57](https://sep490-g21-fall26.atlassian.net/browse/KAN-57) | U15 | MVP |
| US-GRD-002 | [KAN-58](https://sep490-g21-fall26.atlassian.net/browse/KAN-58) | U15 | MVP |
| US-GRD-003 | [KAN-59](https://sep490-g21-fall26.atlassian.net/browse/KAN-59) | U15 | MVP |
| US-GRD-004 | [KAN-60](https://sep490-g21-fall26.atlassian.net/browse/KAN-60) | U15 | MVP |
| US-GRD-005 | [KAN-61](https://sep490-g21-fall26.atlassian.net/browse/KAN-61) | U15 | MVP |
| US-GRP-001 | [KAN-62](https://sep490-g21-fall26.atlassian.net/browse/KAN-62) | U12 | MVP |
| US-GRP-002 | [KAN-63](https://sep490-g21-fall26.atlassian.net/browse/KAN-63) | U12 | MVP |
| US-GRP-003 | [KAN-64](https://sep490-g21-fall26.atlassian.net/browse/KAN-64) | U09 | MVP |
| US-GRP-004 | [KAN-65](https://sep490-g21-fall26.atlassian.net/browse/KAN-65) | U14 | MVP |
| US-GRP-005 | [KAN-66](https://sep490-g21-fall26.atlassian.net/browse/KAN-66) | U14 | MVP |
| US-GRP-006 | [KAN-67](https://sep490-g21-fall26.atlassian.net/browse/KAN-67) | U15 | MVP |
| US-IAM-001 | [KAN-68](https://sep490-g21-fall26.atlassian.net/browse/KAN-68) | U01 | MVP |
| US-IAM-002 | [KAN-69](https://sep490-g21-fall26.atlassian.net/browse/KAN-69) | U01 | MVP |
| US-IAM-003 | [KAN-70](https://sep490-g21-fall26.atlassian.net/browse/KAN-70) | U01 | MVP |
| US-IAM-004 | [KAN-71](https://sep490-g21-fall26.atlassian.net/browse/KAN-71) | U01 | MVP |
| US-IAM-005 | [KAN-72](https://sep490-g21-fall26.atlassian.net/browse/KAN-72) | U01 | MVP |
| US-IAM-006 | [KAN-73](https://sep490-g21-fall26.atlassian.net/browse/KAN-73) | U01 | MVP |
| US-IAM-007 | [KAN-74](https://sep490-g21-fall26.atlassian.net/browse/KAN-74) | U01 | MVP |
| US-LRN-001 | [KAN-75](https://sep490-g21-fall26.atlassian.net/browse/KAN-75) | U04 | MVP |
| US-NTF-001 | [KAN-76](https://sep490-g21-fall26.atlassian.net/browse/KAN-76) | U16 | MVP |
| US-QBK-001 | [KAN-77](https://sep490-g21-fall26.atlassian.net/browse/KAN-77) | U06 | MVP |
| US-QBK-002 | [KAN-78](https://sep490-g21-fall26.atlassian.net/browse/KAN-78) | U06 | MVP |
| US-RPT-001 | [KAN-79](https://sep490-g21-fall26.atlassian.net/browse/KAN-79) | U16 | MVP |
| US-RPT-002 | [KAN-80](https://sep490-g21-fall26.atlassian.net/browse/KAN-80) | U16 | MVP |
| US-RPT-003 | [KAN-81](https://sep490-g21-fall26.atlassian.net/browse/KAN-81) | U16 | MVP |
| US-SET-001 | [KAN-82](https://sep490-g21-fall26.atlassian.net/browse/KAN-82) | U03 | MVP |
| US-OPS-001 | [KAN-83](https://sep490-g21-fall26.atlassian.net/browse/KAN-83) | U03 | MVP |
| US-INT-001 | [KAN-84](https://sep490-g21-fall26.atlassian.net/browse/KAN-84) | U15 | MVP |

English Jira acceptance criteria summarize the source; the full source scenarios remain authoritative and must be reviewed before acceptance. Story UC labels include supporting integrations and do not reassign primary ownership. The existing source audit story's legacy payment example is excluded by the current no-payment contract.

## Dependencies and acceptance

Verified 70 native issue links: 24 provider-readiness Blocks links between unit Epics; 15 review-before-test-acceptance Blocks links; 8 decision relationships; 7 historical relationships; 16 gate relationships.

Epic Blocks links describe required provider behavior before integration, not a mandatory full-Epic completion barrier. Parallel development via neutral contracts remains possible. Waves are dependency groups; manual work does not wait for AI. Integration-test Tasks include case preparation before execution, actual outcomes, defect recording and independent retest.

- [D01: KAN-115](https://sep490-g21-fall26.atlassian.net/browse/KAN-115)
- [D02: KAN-116](https://sep490-g21-fall26.atlassian.net/browse/KAN-116)
- [D03: KAN-117](https://sep490-g21-fall26.atlassian.net/browse/KAN-117)
- [D04: KAN-118](https://sep490-g21-fall26.atlassian.net/browse/KAN-118)
- [G1: KAN-119](https://sep490-g21-fall26.atlassian.net/browse/KAN-119)
- [G2: KAN-120](https://sep490-g21-fall26.atlassian.net/browse/KAN-120)
- [G3: KAN-121](https://sep490-g21-fall26.atlassian.net/browse/KAN-121)
- [G4: KAN-122](https://sep490-g21-fall26.atlassian.net/browse/KAN-122)

No D01–D04 decisions, numeric quota/retention, people/effort or new delivery dates were invented. Optional extension is separated using scope-extension. No claim of current code completion or successful runtime tests is made.

## Operating policy and useful views

AI-DLC operating guide — current revision 2026-10-13

Scope: 66 active UC, 50 MVP Stories, 1 optional extension and 15 logical units. KAN-33 is the management container, not a sixteenth logical unit. Historical completed items remain separate.

Board workflow: To Do -> In Progress -> In Review -> Done.

Issue types:
- Epic: a logical unit; the coordination Epic holds cross-unit documents/decisions/gates.
- Story: a user-facing capability with stable US/UC/FR references and source acceptance scenarios.
- Task: requirements/design review, implementation support, documents and independent testing.
- Bug: a reproducible defect linked to the affected Story and unit.

Assignment policy:
- Newly created work stays To Do and Unassigned until the team agrees on ownership. No new deadlines or effort estimates are assumed.
- Before implementation, record three different people on each feature: requirement owner, code owner and independent test-case owner. Use assignee for the current task owner and record the other roles in the description.
- Developers write unit tests; independent testers prepare/execute integration cases and retest defects. Existing assignments and deadlines on KAN-11 through KAN-17 are preserved.
- Recommended active-unit limit from the team plan: at most five; this is team guidance, not an enforced board limit.

Labels:
- aidlc-current / aidlc-historical separate current work from previous baselines.
- scope-2026-10-13 identifies the revision; scope-mvp / scope-extension separates committed and optional work.
- unit-u01 ... unit-u16 (no U10), wave-1 ... wave-4, iteration-1 / iteration-2 support filtering.
- requirements-review, integration-test, decision-pending, acceptance-gate and documentation identify work purposes.

Wave and iteration policy:
- Wave 1: U03/U02 -> U01 -> U04.
- Wave 2: U05/U06/U07/U12/U08.
- Wave 3: U09/U11/U14/U15.
- Wave 4: U13/U16.
- Waves are dependency groups, not full-wave barriers. Blocks links between unit Epics mean required provider behavior must be ready for integration; they do NOT require every provider Story/Epic to be Done before parallel contract-based development can start.
- Iteration 1: foundation/auth, Admin accounts/subjects/Settings/audit, backup foundations and basic Admin Dashboard. U04 classes/enrollment and U16 academic integrations belong to Iteration 2.
- Iteration 2: classes/workspaces/submission/grading and AI/MCP integrations when providers are ready. Manual grading does not wait for AI; Lessons/RAG are optional.

Definition of Done:
- The full approved source acceptance scenarios are covered; Jira English descriptions are summaries, not replacements for the source specification.
- Record reviewed changes, build/commit references, developer unit tests and independent integration/retest evidence with actual outcomes.
- Resolve or explicitly accept relevant defects and record independent reviewer acceptance.
- Document-only completion does not certify runtime implementation; old approvals are not inherited by current deltas.
- These are documented team rules. Jira transition validators have not been configured by this connector; the workflow does not automatically enforce reviewer/evidence requirements.

Defect template:
- Summary: [UC/Unit] Observable problem.
- Environment/build and affected Story/UC.
- Preconditions and exact reproduction steps.
- Expected behavior vs actual behavior.
- Severity, impact and evidence.
- Fix reference, independent retest result and reviewer acceptance.

Pending decisions: D01 Quiz policy; D02 Group Practice; D03 Student group AI context; D04 quota limits/units. Keep documented temporary defaults; creating decision tasks does not approve policies.

Useful views (open links, then save as filters in Jira if desired):
- [Current MVP](https://sep490-g21-fall26.atlassian.net/issues/?jql=project%20%3D%20KAN%20AND%20labels%20%3D%20aidlc-current%20AND%20labels%20!%3D%20scope-extension%20ORDER%20BY%20Rank%20ASC)
- [Unassigned work](https://sep490-g21-fall26.atlassian.net/issues/?jql=project%20%3D%20KAN%20AND%20labels%20%3D%20aidlc-current%20AND%20assignee%20IS%20EMPTY%20AND%20statusCategory%20!%3D%20Done%20ORDER%20BY%20Rank%20ASC)
- [Wave 1](https://sep490-g21-fall26.atlassian.net/issues/?jql=project%20%3D%20KAN%20AND%20labels%20%3D%20aidlc-current%20AND%20labels%20%3D%20wave-1%20ORDER%20BY%20Rank%20ASC)
- [Iteration 1](https://sep490-g21-fall26.atlassian.net/issues/?jql=project%20%3D%20KAN%20AND%20labels%20%3D%20aidlc-current%20AND%20labels%20%3D%20iteration-1%20ORDER%20BY%20Rank%20ASC)
- [Integration tests](https://sep490-g21-fall26.atlassian.net/issues/?jql=project%20%3D%20KAN%20AND%20labels%20%3D%20aidlc-current%20AND%20labels%20%3D%20integration-test%20ORDER%20BY%20Rank%20ASC)
- [Open decisions](https://sep490-g21-fall26.atlassian.net/issues/?jql=project%20%3D%20KAN%20AND%20labels%20%3D%20decision-pending%20AND%20statusCategory%20!%3D%20Done%20ORDER%20BY%20Rank%20ASC)
- [Defects](https://sep490-g21-fall26.atlassian.net/issues/?jql=project%20%3D%20KAN%20AND%20issuetype%20%3D%20Bug%20ORDER%20BY%20priority%20DESC%2C%20updated%20DESC)
- [Optional extension](https://sep490-g21-fall26.atlassian.net/issues/?jql=project%20%3D%20KAN%20AND%20labels%20%3D%20scope-extension%20ORDER%20BY%20Rank%20ASC)

Source repository: https://github.com/hygef-v4/AI-Powered-Learning-Platform
Authoritative files: aidlc-docs/inception/README.md; construction/current-srs-contract.md; inception/user-stories/stories.md; inception/application-design/unit-of-work-dependency.md; inception/plans/execution-plan.md; inception/requirements/revision-decisions-2026-10-13.md. Paths after the first are relative to aidlc-docs/.

No payment/credit sales, Code Lab or U10 are introduced. No Student institutional quota; no automated official AI grading or activity-based penalties.

## Verification

Read all 120 KAN issues across two pages after writes. Verified all 106 created keys, all Story parents, all 70 directed links, Unassigned/To Do/no-date state for new issues, seven preserved current assignments/deadlines/statuses and seven historical Done states. No missing keys, incorrect parents or missing links found.

Saved original changed issue fields and creation/link ledger at .tmp/jira-setup-2026-10-13-snapshot.json. This is a restoration record, not an automatic delete/revert script.

No application code, AI-DLC stage approvals, diagrams or Google Reports changed. No commits/pushes performed. Workflow transition validators and a hard WIP limit were not configured: the connector exposes issue data/links but no workflow-validator operation. DoD and reviewer/evidence requirements are documented team rules. Project board columns and Bug type remain in place.

Enabled Security/Resiliency subset remains unchanged; runtime rule compliance is N/A for this management setup, not certified. PBT remains disabled.
