# U05 / U15 — Jira scope snapshot

Captured 2026-10-25 from live Atlassian connector, site https://sep490-g21-fall26.atlassian.net. Query: `text ~ "U05" OR text ~ "U15" ORDER BY key ASC`; response `isLast=true`. Filtered to current U05/U15 epics and UC20–30/UC66, not legacy unit numbering. This snapshot is requirements evidence, not test evidence. Jira was read only.

## [KAN-21: [U05] Class & Enrollment](https://sep490-g21-fall26.atlassian.net/browse/KAN-21)

Status: Done. Parent: Epic.

AI-DLC current unit U05 — Class & Enrollment. Iteration 1; this unit belongs entirely to this iteration. 11 primary use cases.

Use cases:

* [\[UC20\] View Enrolled Classes](https://sep490-g21-fall26.atlassian.net/browse/KAN-75)
* [\[UC21\] View Enrolled Class Detail](https://sep490-g21-fall26.atlassian.net/browse/KAN-126)
* [\[UC22\] View Assigned Class List](https://sep490-g21-fall26.atlassian.net/browse/KAN-51)
* [\[UC23\] View Assigned Class Detail](https://sep490-g21-fall26.atlassian.net/browse/KAN-135)
* [\[UC24\] View Managed Subject Classes](https://sep490-g21-fall26.atlassian.net/browse/KAN-137)
* [\[UC25\] View Managed Class Detail](https://sep490-g21-fall26.atlassian.net/browse/KAN-138)
* [\[UC26\] Create Class](https://sep490-g21-fall26.atlassian.net/browse/KAN-50)
* [\[UC27\] Assign Teacher To Class](https://sep490-g21-fall26.atlassian.net/browse/KAN-139)
* [\[UC28\] Add/Remove Class Students](https://sep490-g21-fall26.atlassian.net/browse/KAN-52)
* [\[UC29\] Edit Class Information](https://sep490-g21-fall26.atlassian.net/browse/KAN-140)
* [\[UC30\] View Managed Subject](https://sep490-g21-fall26.atlassian.net/browse/KAN-141)

Required provider contracts: [U01 Account & Access](https://sep490-g21-fall26.atlassian.net/browse/KAN-18); [U04 Subject Administration](https://sep490-g21-fall26.atlassian.net/browse/KAN-24). Native Blocks links represent provider-contract readiness for integration, not a requirement that the entire provider Epic must be Done before parallel development. Optional AI and event consumers follow the contract/event notes in unit-of-work.md.

Delivery and review checklist:

- [ ] Review current UC/FR permissions, business rules, lifecycle and public contracts.
- [ ] Record three distinct requirement, code and independent test-case owners once the team confirms identities. Requirement owner: pending; code owner: Khuat Quang Hung (confirmed by the user); test-case owner: pending. AI-DLC code slot: Người 5.
- [ ] Implement only owned behavior; use public ports for cross-unit access. Record reviewed code/commit references and meaningful developer unit tests.
- [ ] Prepare and execute independent integration cases, denied-access/state/concurrency/failure cases and defect retests; record actual results.
- [ ] Resolve or explicitly accept defects and record reviewer acceptance before Done. Documentation approval is not runtime evidence.

Track one Story per UC with review/test checklists; do not duplicate the source user stories as extra tickets. Source: docs/usecase_table.md; aidlc-docs/inception/application-design/unit-of-work.md and unit-of-work-story-map.md; aidlc-docs/inception/user-stories/stories.md. Current Construction designs remain in legacy folders u04-subject-class-enrollment; use the documented mapping. No implementation approval, new personnel, deadline, numerical quota or effort estimate is inferred.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Migration note: this Epic retains Jira key KAN-21 for history; its former title was '\[U04\] Subject, Class & Enrollment'. The canonical unit ID and current scope are those above.

## [KAN-32: [U15] Admin Dashboard](https://sep490-g21-fall26.atlassian.net/browse/KAN-32)

Status: To Do. Parent: Epic.

AI-DLC current unit U15 — Admin Dashboard. Iteration 3; this unit belongs entirely to this iteration. 1 primary use cases.

Use cases:

* [\[UC66\] View Admin Dashboard](https://sep490-g21-fall26.atlassian.net/browse/KAN-80)

Required provider contracts: [U01 Account & Access](https://sep490-g21-fall26.atlassian.net/browse/KAN-18); [U03 File, Job, Settings & Backup](https://sep490-g21-fall26.atlassian.net/browse/KAN-20); [U05 Class & Enrollment](https://sep490-g21-fall26.atlassian.net/browse/KAN-21). Native Blocks links represent provider-contract readiness for integration, not a requirement that the entire provider Epic must be Done before parallel development. Optional AI and event consumers follow the contract/event notes in unit-of-work.md.

Delivery and review checklist:

- [ ] Review current UC/FR permissions, business rules, lifecycle and public contracts.
- [ ] Record three distinct requirement, code and independent test-case owners once the team confirms identities. Requirement owner: pending; code owner: Khuat Quang Hung (confirmed by the user); test-case owner: pending. AI-DLC code slot: Người 5.
- [ ] Implement only owned behavior; use public ports for cross-unit access. Record reviewed code/commit references and meaningful developer unit tests.
- [ ] Prepare and execute independent integration cases, denied-access/state/concurrency/failure cases and defect retests; record actual results.
- [ ] Resolve or explicitly accept defects and record reviewer acceptance before Done. Documentation approval is not runtime evidence.

Track one Story per UC with review/test checklists; do not duplicate the source user stories as extra tickets. Source: docs/usecase_table.md; aidlc-docs/inception/application-design/unit-of-work.md and unit-of-work-story-map.md; aidlc-docs/inception/user-stories/stories.md. Current Construction designs remain in legacy folders u16-reporting-notification; use the documented mapping. No implementation approval, new personnel, deadline, numerical quota or effort estimate is inferred.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Migration note: this Epic retains Jira key KAN-32 for history; its former title was '\[U16\] Reporting & Notification'. The canonical unit ID and current scope are those above.

## [KAN-50: [UC26] Create Class](https://sep490-g21-fall26.atlassian.net/browse/KAN-50)

Status: Done. Parent: KAN-21.

Allows a subject manager to create a class within a subject they manage.

Unit: U05 Class & Enrollment. Iteration: 1. Feature: Subject Class Management.

Source of truth: docs/usecase_table.md (UC26); aidlc-docs/inception/requirements/use-cases.md; aidlc-docs/inception/application-design/unit-of-work.md. Construction retains legacy folder names: aidlc-docs/construction/u04-subject-class-enrollment. Read the unit mapping before implementation; legacy folder IDs do not determine current ownership.

Implementation and acceptance checklist:

- [ ] Review this UC's actor permissions, resource scope, preconditions and lifecycle rules against the current documents.
- [ ] Implement the permitted behavior and reject unauthorized or invalid state/version requests.
- [ ] Record developer unit-test results and independent integration/retest evidence for success, denied access and applicable failures.
- [ ] Record related defects and reviewer acceptance before Done; documentation alone is not runtime evidence.

Supporting specification scenarios: aidlc-docs/inception/user-stories/stories.md, US-CAT-001. These are source references, not additional Jira tracking tickets.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Ownership: requirement owner pending; code owner Khuat Quang Hung (AI-DLC Người 5, confirmed by the user); independent test-case owner pending. Requirement, code and test-case roles must be held by three different people. No assignee, dates or Story Points are inferred.

## [KAN-51: [UC22] View Assigned Class List](https://sep490-g21-fall26.atlassian.net/browse/KAN-51)

Status: Done. Parent: KAN-21.

Allows a teacher to view their assigned classes and select a class to manage.

Unit: U05 Class & Enrollment. Iteration: 1. Feature: Teacher Class Access.

Source of truth: docs/usecase_table.md (UC22); aidlc-docs/inception/requirements/use-cases.md; aidlc-docs/inception/application-design/unit-of-work.md. Construction retains legacy folder names: aidlc-docs/construction/u04-subject-class-enrollment. Read the unit mapping before implementation; legacy folder IDs do not determine current ownership.

Implementation and acceptance checklist:

- [ ] Review this UC's actor permissions, resource scope, preconditions and lifecycle rules against the current documents.
- [ ] Implement the permitted behavior and reject unauthorized or invalid state/version requests.
- [ ] Record developer unit-test results and independent integration/retest evidence for success, denied access and applicable failures.
- [ ] Record related defects and reviewer acceptance before Done; documentation alone is not runtime evidence.

Supporting specification scenarios: aidlc-docs/inception/user-stories/stories.md, US-CAT-002. These are source references, not additional Jira tracking tickets.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Ownership: requirement owner pending; code owner Khuat Quang Hung (AI-DLC Người 5, confirmed by the user); independent test-case owner pending. Requirement, code and test-case roles must be held by three different people. No assignee, dates or Story Points are inferred.

## [KAN-52: [UC28] Add/Remove Class Students](https://sep490-g21-fall26.atlassian.net/browse/KAN-52)

Status: Done. Parent: KAN-21.

Allows a subject manager to add students to or remove students from a managed class while preserving their learning history.

Unit: U05 Class & Enrollment. Iteration: 1. Feature: Class Enrollment Management.

Source of truth: docs/usecase_table.md (UC28); aidlc-docs/inception/requirements/use-cases.md; aidlc-docs/inception/application-design/unit-of-work.md. Construction retains legacy folder names: aidlc-docs/construction/u04-subject-class-enrollment. Read the unit mapping before implementation; legacy folder IDs do not determine current ownership.

Implementation and acceptance checklist:

- [ ] Review this UC's actor permissions, resource scope, preconditions and lifecycle rules against the current documents.
- [ ] Implement the permitted behavior and reject unauthorized or invalid state/version requests.
- [ ] Record developer unit-test results and independent integration/retest evidence for success, denied access and applicable failures.
- [ ] Record related defects and reviewer acceptance before Done; documentation alone is not runtime evidence.

Supporting specification scenarios: aidlc-docs/inception/user-stories/stories.md, US-CAT-003. These are source references, not additional Jira tracking tickets.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Ownership: requirement owner pending; code owner Khuat Quang Hung (AI-DLC Người 5, confirmed by the user); independent test-case owner pending. Requirement, code and test-case roles must be held by three different people. No assignee, dates or Story Points are inferred.

## [KAN-75: [UC20] View Enrolled Classes](https://sep490-g21-fall26.atlassian.net/browse/KAN-75)

Status: Done. Parent: KAN-21.

Allows a student to view their enrolled classes and select a class to access.

Unit: U05 Class & Enrollment. Iteration: 1. Feature: Student Class Access.

Source of truth: docs/usecase_table.md (UC20); aidlc-docs/inception/requirements/use-cases.md; aidlc-docs/inception/application-design/unit-of-work.md. Construction retains legacy folder names: aidlc-docs/construction/u04-subject-class-enrollment. Read the unit mapping before implementation; legacy folder IDs do not determine current ownership.

Implementation and acceptance checklist:

- [ ] Review this UC's actor permissions, resource scope, preconditions and lifecycle rules against the current documents.
- [ ] Implement the permitted behavior and reject unauthorized or invalid state/version requests.
- [ ] Record developer unit-test results and independent integration/retest evidence for success, denied access and applicable failures.
- [ ] Record related defects and reviewer acceptance before Done; documentation alone is not runtime evidence.

Supporting specification scenarios: aidlc-docs/inception/user-stories/stories.md, US-LRN-001. These are source references, not additional Jira tracking tickets.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Ownership: requirement owner pending; code owner Khuat Quang Hung (AI-DLC Người 5, confirmed by the user); independent test-case owner pending. Requirement, code and test-case roles must be held by three different people. No assignee, dates or Story Points are inferred.

## [KAN-80: [UC66] View Admin Dashboard](https://sep490-g21-fall26.atlassian.net/browse/KAN-80)

Status: In Progress. Parent: KAN-32.

Allows an administrator to view platform statistics and backup status or history, including the latest successful backup and reported errors.

Unit: U15 Admin Dashboard. Iteration: 3. Feature: System Overview.

Source of truth: docs/usecase_table.md (UC66); aidlc-docs/inception/requirements/use-cases.md; aidlc-docs/inception/application-design/unit-of-work.md. Construction retains legacy folder names: aidlc-docs/construction/u16-reporting-notification. Read the unit mapping before implementation; legacy folder IDs do not determine current ownership.

Implementation and acceptance checklist:

- [ ] Review this UC's actor permissions, resource scope, preconditions and lifecycle rules against the current documents.
- [ ] Implement the permitted behavior and reject unauthorized or invalid state/version requests.
- [ ] Record developer unit-test results and independent integration/retest evidence for success, denied access and applicable failures.
- [ ] Record related defects and reviewer acceptance before Done; documentation alone is not runtime evidence.

Supporting specification scenarios: aidlc-docs/inception/user-stories/stories.md, US-RPT-002; aidlc-docs/inception/user-stories/stories.md, US-OPS-001. These are source references, not additional Jira tracking tickets.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Ownership: requirement owner pending; code owner Khuat Quang Hung (AI-DLC Người 5, confirmed by the user); independent test-case owner pending. Requirement, code and test-case roles must be held by three different people. No assignee, dates or Story Points are inferred.

## [KAN-126: [UC21] View Enrolled Class Detail](https://sep490-g21-fall26.atlassian.net/browse/KAN-126)

Status: Done. Parent: KAN-21.

Allows a student to view an enrolled class's information and access its materials, assignments, announcements and group information.

Unit: U05 Class & Enrollment. Iteration: 1. Feature: Student Class Access.

Source of truth: docs/usecase_table.md (UC21); aidlc-docs/inception/requirements/use-cases.md; aidlc-docs/inception/application-design/unit-of-work.md. Construction retains legacy folder names: aidlc-docs/construction/u04-subject-class-enrollment. Read the unit mapping before implementation; legacy folder IDs do not determine current ownership.

Implementation and acceptance checklist:

- [ ] Review this UC's actor permissions, resource scope, preconditions and lifecycle rules against the current documents.
- [ ] Implement the permitted behavior and reject unauthorized or invalid state/version requests.
- [ ] Record developer unit-test results and independent integration/retest evidence for success, denied access and applicable failures.
- [ ] Record related defects and reviewer acceptance before Done; documentation alone is not runtime evidence.

Supporting specification scenarios: aidlc-docs/inception/user-stories/stories.md, US-LRN-001. These are source references, not additional Jira tracking tickets.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Ownership: requirement owner pending; code owner Khuat Quang Hung (AI-DLC Người 5, confirmed by the user); independent test-case owner pending. Requirement, code and test-case roles must be held by three different people. No assignee, dates or Story Points are inferred.

## [KAN-135: [UC23] View Assigned Class Detail](https://sep490-g21-fall26.atlassian.net/browse/KAN-135)

Status: Done. Parent: KAN-21.

Allows a teacher to view an assigned class's information and access its teaching functions.

Unit: U05 Class & Enrollment. Iteration: 1. Feature: Teacher Class Access.

Source of truth: docs/usecase_table.md (UC23); aidlc-docs/inception/requirements/use-cases.md; aidlc-docs/inception/application-design/unit-of-work.md. Construction retains legacy folder names: aidlc-docs/construction/u04-subject-class-enrollment. Read the unit mapping before implementation; legacy folder IDs do not determine current ownership.

Implementation and acceptance checklist:

- [ ] Review this UC's actor permissions, resource scope, preconditions and lifecycle rules against the current documents.
- [ ] Implement the permitted behavior and reject unauthorized or invalid state/version requests.
- [ ] Record developer unit-test results and independent integration/retest evidence for success, denied access and applicable failures.
- [ ] Record related defects and reviewer acceptance before Done; documentation alone is not runtime evidence.

Supporting specification scenarios: aidlc-docs/inception/user-stories/stories.md, US-CAT-002. These are source references, not additional Jira tracking tickets.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Ownership: requirement owner pending; code owner Khuat Quang Hung (AI-DLC Người 5, confirmed by the user); independent test-case owner pending. Requirement, code and test-case roles must be held by three different people. No assignee, dates or Story Points are inferred.

## [KAN-137: [UC24] View Managed Subject Classes](https://sep490-g21-fall26.atlassian.net/browse/KAN-137)

Status: Done. Parent: KAN-21.

Allows a subject manager to view classes belonging to their assigned subjects.

Unit: U05 Class & Enrollment. Iteration: 1. Feature: Subject Class Management.

Source of truth: docs/usecase_table.md (UC24); aidlc-docs/inception/requirements/use-cases.md; aidlc-docs/inception/application-design/unit-of-work.md. Construction retains legacy folder names: aidlc-docs/construction/u04-subject-class-enrollment. Read the unit mapping before implementation; legacy folder IDs do not determine current ownership.

Implementation and acceptance checklist:

- [ ] Review this UC's actor permissions, resource scope, preconditions and lifecycle rules against the current documents.
- [ ] Implement the permitted behavior and reject unauthorized or invalid state/version requests.
- [ ] Record developer unit-test results and independent integration/retest evidence for success, denied access and applicable failures.
- [ ] Record related defects and reviewer acceptance before Done; documentation alone is not runtime evidence.

Supporting specification scenarios: aidlc-docs/inception/user-stories/stories.md, US-CAT-001; aidlc-docs/inception/user-stories/stories.md, US-CAT-002. These are source references, not additional Jira tracking tickets.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Ownership: requirement owner pending; code owner Khuat Quang Hung (AI-DLC Người 5, confirmed by the user); independent test-case owner pending. Requirement, code and test-case roles must be held by three different people. No assignee, dates or Story Points are inferred.

## [KAN-138: [UC25] View Managed Class Detail](https://sep490-g21-fall26.atlassian.net/browse/KAN-138)

Status: Done. Parent: KAN-21.

Allows a subject manager to view a managed class's information, assigned teacher and enrolled students.

Unit: U05 Class & Enrollment. Iteration: 1. Feature: Subject Class Management.

Source of truth: docs/usecase_table.md (UC25); aidlc-docs/inception/requirements/use-cases.md; aidlc-docs/inception/application-design/unit-of-work.md. Construction retains legacy folder names: aidlc-docs/construction/u04-subject-class-enrollment. Read the unit mapping before implementation; legacy folder IDs do not determine current ownership.

Implementation and acceptance checklist:

- [ ] Review this UC's actor permissions, resource scope, preconditions and lifecycle rules against the current documents.
- [ ] Implement the permitted behavior and reject unauthorized or invalid state/version requests.
- [ ] Record developer unit-test results and independent integration/retest evidence for success, denied access and applicable failures.
- [ ] Record related defects and reviewer acceptance before Done; documentation alone is not runtime evidence.

Supporting specification scenarios: aidlc-docs/inception/user-stories/stories.md, US-CAT-001; aidlc-docs/inception/user-stories/stories.md, US-CAT-002. These are source references, not additional Jira tracking tickets.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Ownership: requirement owner pending; code owner Khuat Quang Hung (AI-DLC Người 5, confirmed by the user); independent test-case owner pending. Requirement, code and test-case roles must be held by three different people. No assignee, dates or Story Points are inferred.

## [KAN-139: [UC27] Assign Teacher To Class](https://sep490-g21-fall26.atlassian.net/browse/KAN-139)

Status: Done. Parent: KAN-21.

Allows a subject manager to assign or change the teacher responsible for a managed class.

Unit: U05 Class & Enrollment. Iteration: 1. Feature: Subject Class Management.

Source of truth: docs/usecase_table.md (UC27); aidlc-docs/inception/requirements/use-cases.md; aidlc-docs/inception/application-design/unit-of-work.md. Construction retains legacy folder names: aidlc-docs/construction/u04-subject-class-enrollment. Read the unit mapping before implementation; legacy folder IDs do not determine current ownership.

Implementation and acceptance checklist:

- [ ] Review this UC's actor permissions, resource scope, preconditions and lifecycle rules against the current documents.
- [ ] Implement the permitted behavior and reject unauthorized or invalid state/version requests.
- [ ] Record developer unit-test results and independent integration/retest evidence for success, denied access and applicable failures.
- [ ] Record related defects and reviewer acceptance before Done; documentation alone is not runtime evidence.

Supporting specification scenarios: aidlc-docs/inception/user-stories/stories.md, US-CAT-001. These are source references, not additional Jira tracking tickets.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Ownership: requirement owner pending; code owner Khuat Quang Hung (AI-DLC Người 5, confirmed by the user); independent test-case owner pending. Requirement, code and test-case roles must be held by three different people. No assignee, dates or Story Points are inferred.

## [KAN-140: [UC29] Edit Class Information](https://sep490-g21-fall26.atlassian.net/browse/KAN-140)

Status: Done. Parent: KAN-21.

Allows a subject manager to update class information, open or archive a class, and control anonymous grade-distribution visibility.

Unit: U05 Class & Enrollment. Iteration: 1. Feature: Subject Class Management.

Source of truth: docs/usecase_table.md (UC29); aidlc-docs/inception/requirements/use-cases.md; aidlc-docs/inception/application-design/unit-of-work.md. Construction retains legacy folder names: aidlc-docs/construction/u04-subject-class-enrollment. Read the unit mapping before implementation; legacy folder IDs do not determine current ownership.

Implementation and acceptance checklist:

- [ ] Review this UC's actor permissions, resource scope, preconditions and lifecycle rules against the current documents.
- [ ] Implement the permitted behavior and reject unauthorized or invalid state/version requests.
- [ ] Record developer unit-test results and independent integration/retest evidence for success, denied access and applicable failures.
- [ ] Record related defects and reviewer acceptance before Done; documentation alone is not runtime evidence.

Supporting specification scenarios: aidlc-docs/inception/user-stories/stories.md, US-CAT-002; aidlc-docs/inception/user-stories/stories.md, US-RPT-001. These are source references, not additional Jira tracking tickets.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Ownership: requirement owner pending; code owner Khuat Quang Hung (AI-DLC Người 5, confirmed by the user); independent test-case owner pending. Requirement, code and test-case roles must be held by three different people. No assignee, dates or Story Points are inferred.

## [KAN-141: [UC30] View Managed Subject](https://sep490-g21-fall26.atlassian.net/browse/KAN-141)

Status: Done. Parent: KAN-21.

Allows a subject manager to view their assigned subjects and access each subject's information and management functions.

Unit: U05 Class & Enrollment. Iteration: 1. Feature: Subject Management.

Source of truth: docs/usecase_table.md (UC30); aidlc-docs/inception/requirements/use-cases.md; aidlc-docs/inception/application-design/unit-of-work.md. Construction retains legacy folder names: aidlc-docs/construction/u04-subject-class-enrollment. Read the unit mapping before implementation; legacy folder IDs do not determine current ownership.

Implementation and acceptance checklist:

- [ ] Review this UC's actor permissions, resource scope, preconditions and lifecycle rules against the current documents.
- [ ] Implement the permitted behavior and reject unauthorized or invalid state/version requests.
- [ ] Record developer unit-test results and independent integration/retest evidence for success, denied access and applicable failures.
- [ ] Record related defects and reviewer acceptance before Done; documentation alone is not runtime evidence.

Supporting specification scenarios: aidlc-docs/inception/user-stories/stories.md, US-CAT-001. These are source references, not additional Jira tracking tickets.

Current scope (2026-10-14): 66 active UCs; 15 logical units in 3 iterations (5 units each). Assignment Individual/Group with optional Draw.io; standalone Quiz; educational YouTube links. No payment, Code Lab or draft feedback. Student feedback applies only to owned submitted Practice Assignments; AI grading proposals require assigned-educator review. U06 owns institutional quota, AI connections, protected credentials, provider adapters and MCP. U03 owns Settings and backup; U08 owns work/activity data; U09 reads activity for grading; U11/U12 write activity through U08; U14 owns announcements/notifications; U15 reads Admin Dashboard/backup status. API/MCP never expands current scope or permissions. Policies D01–D04 remain as recorded in the source decision log, without new approval.

Ownership: requirement owner pending; code owner Khuat Quang Hung (AI-DLC Người 5, confirmed by the user); independent test-case owner pending. Requirement, code and test-case roles must be held by three different people. No assignee, dates or Story Points are inferred.
