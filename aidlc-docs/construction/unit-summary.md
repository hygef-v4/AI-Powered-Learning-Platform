# Tổng hợp 15 unit — AS-BUILT 2026-10-30

Source-observed tại `794a405304bf02dd2593a9fa2c88662d7cd52845`; source present không đồng nghĩa full UC acceptance.

| Unit | Trách nhiệm hiện hành | UC | Packages |
|---|---|---|---|
| U01 | Account & Access | 01–12 | [identity](../../backend/src/main/java/edu/aiplatform/identity) |
| U02 | Audit | 13 | [audit](../../backend/src/main/java/edu/aiplatform/audit) |
| U03 | Job and Settings | 14–15 | [files](../../backend/src/main/java/edu/aiplatform/files), [jobs](../../backend/src/main/java/edu/aiplatform/jobs), [settings](../../backend/src/main/java/edu/aiplatform/settings), [backup](../../backend/src/main/java/edu/aiplatform/backup) |
| U04 | Subject Administration | 16–19 | [subjects](../../backend/src/main/java/edu/aiplatform/subjects) |
| U05 | Class & Enrollment | 20–30 | [academics](../../backend/src/main/java/edu/aiplatform/academics) |
| U06 | AI Connections | 31–33 | [ai](../../backend/src/main/java/edu/aiplatform/ai) |
| U07 | Assignment Authoring | 34–36 | [assignments](../../backend/src/main/java/edu/aiplatform/assignments) |
| U08 | Assignment Work | 37–41 | [assignmentwork](../../backend/src/main/java/edu/aiplatform/assignmentwork) |
| U09 | Assignment Grading | 42–45 | [grading](../../backend/src/main/java/edu/aiplatform/grading) |
| U10 | Quiz Authoring | 46–48 | [quizzes](../../backend/src/main/java/edu/aiplatform/quizzes) |
| U11 | Quiz Practice | 49–52 | [quizpractice](../../backend/src/main/java/edu/aiplatform/quizpractice) |
| U12 | Group Assignment | 53–57 | [groups](../../backend/src/main/java/edu/aiplatform/groups), [groupdocs](../../backend/src/main/java/edu/aiplatform/groupdocs) |
| U13 | Reference Materials | 58–62 | [materials](../../backend/src/main/java/edu/aiplatform/materials) |
| U14 | Announcements & Notifications | 63–65 | [announcements](../../backend/src/main/java/edu/aiplatform/announcements), [notifications](../../backend/src/main/java/edu/aiplatform/notifications) |
| U15 | Admin Dashboard | 66 | [admindashboard](../../backend/src/main/java/edu/aiplatform/admindashboard) |


## U01 Account & Access

UC 01–12; FR-001, FR-002, FR-015.

- [aidlc-docs/construction/u01-account-and-access/code/as-built-baseline.md](u01-account-and-access/code/as-built-baseline.md)
- [aidlc-docs/construction/u01-account-and-access/functional-design/business-rules.md](u01-account-and-access/functional-design/business-rules.md)
- [aidlc-docs/construction/u01-account-and-access/functional-design/domain-entities.md](u01-account-and-access/functional-design/domain-entities.md)

## U02 Audit

UC 13; FR-014.

- [aidlc-docs/construction/u02-audit-job-and-outbox/code/as-built-baseline.md](u02-audit-job-and-outbox/code/as-built-baseline.md)
- [aidlc-docs/construction/u02-audit-job-and-outbox/functional-design/business-rules.md](u02-audit-job-and-outbox/functional-design/business-rules.md)
- [aidlc-docs/construction/u02-audit-job-and-outbox/functional-design/domain-entities.md](u02-audit-job-and-outbox/functional-design/domain-entities.md)

## U03 Job and Settings

UC 14–15; FR-013, FR-033, FR-038.

- [aidlc-docs/construction/u03-job-and-settings/code/as-built-baseline.md](u03-job-and-settings/code/as-built-baseline.md)
- [aidlc-docs/construction/u03-job-and-settings/functional-design/business-rules.md](u03-job-and-settings/functional-design/business-rules.md)
- [aidlc-docs/construction/u03-job-and-settings/functional-design/domain-entities.md](u03-job-and-settings/functional-design/domain-entities.md)

## U04 Subject Administration

UC 16–19; FR-003.

- [aidlc-docs/construction/u04-subject-administration/code/as-built-baseline.md](u04-subject-administration/code/as-built-baseline.md)
- [aidlc-docs/construction/u04-subject-administration/functional-design/business-rules.md](u04-subject-administration/functional-design/business-rules.md)
- [aidlc-docs/construction/u04-subject-administration/functional-design/domain-entities.md](u04-subject-administration/functional-design/domain-entities.md)

## U05 Class & Enrollment

UC 20–30; FR-003, FR-005, FR-024.

- [aidlc-docs/construction/u05-class-enrollment/code/as-built-baseline.md](u05-class-enrollment/code/as-built-baseline.md)
- [aidlc-docs/construction/u05-class-enrollment/functional-design/business-rules.md](u05-class-enrollment/functional-design/business-rules.md)
- [aidlc-docs/construction/u05-class-enrollment/functional-design/domain-entities.md](u05-class-enrollment/functional-design/domain-entities.md)

## U06 AI Connections

UC 31–33; FR-012, FR-021, FR-034, FR-035, FR-036, FR-037.

- [aidlc-docs/construction/u06-ai-connections/code/as-built-baseline.md](u06-ai-connections/code/as-built-baseline.md)
- [aidlc-docs/construction/u06-ai-connections/functional-design/business-rules.md](u06-ai-connections/functional-design/business-rules.md)
- [aidlc-docs/construction/u06-ai-connections/functional-design/domain-entities.md](u06-ai-connections/functional-design/domain-entities.md)

## U07 Assignment Authoring

UC 34–36; FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

- [aidlc-docs/construction/u07-assignment-authoring/code/as-built-baseline.md](u07-assignment-authoring/code/as-built-baseline.md)
- [aidlc-docs/construction/u07-assignment-authoring/functional-design/business-rules.md](u07-assignment-authoring/functional-design/business-rules.md)
- [aidlc-docs/construction/u07-assignment-authoring/functional-design/domain-entities.md](u07-assignment-authoring/functional-design/domain-entities.md)

## U08 Assignment Work

UC 37–41; FR-018, FR-030, FR-039.

- [aidlc-docs/construction/u08-assignment-work/code/as-built-baseline.md](u08-assignment-work/code/as-built-baseline.md)
- [aidlc-docs/construction/u08-assignment-work/functional-design/business-rules.md](u08-assignment-work/functional-design/business-rules.md)
- [aidlc-docs/construction/u08-assignment-work/functional-design/domain-entities.md](u08-assignment-work/functional-design/domain-entities.md)

## U09 Assignment Grading

UC 42–45; FR-008, FR-009, FR-020, FR-024.

- [aidlc-docs/construction/u09-assignment-grading/code/as-built-baseline.md](u09-assignment-grading/code/as-built-baseline.md)
- [aidlc-docs/construction/u09-assignment-grading/functional-design/business-rules.md](u09-assignment-grading/functional-design/business-rules.md)
- [aidlc-docs/construction/u09-assignment-grading/functional-design/domain-entities.md](u09-assignment-grading/functional-design/domain-entities.md)

## U10 Quiz Authoring

UC 46–48; FR-006, FR-007, FR-016, FR-017, FR-028.

- [aidlc-docs/construction/u10-quiz-authoring/code/as-built-baseline.md](u10-quiz-authoring/code/as-built-baseline.md)
- [aidlc-docs/construction/u10-quiz-authoring/functional-design/business-rules.md](u10-quiz-authoring/functional-design/business-rules.md)
- [aidlc-docs/construction/u10-quiz-authoring/functional-design/domain-entities.md](u10-quiz-authoring/functional-design/domain-entities.md)

## U11 Quiz Practice

UC 49–52; FR-017, FR-018, FR-039.

- [aidlc-docs/construction/u11-quiz-practice/code/as-built-baseline.md](u11-quiz-practice/code/as-built-baseline.md)
- [aidlc-docs/construction/u11-quiz-practice/functional-design/business-rules.md](u11-quiz-practice/functional-design/business-rules.md)
- [aidlc-docs/construction/u11-quiz-practice/functional-design/domain-entities.md](u11-quiz-practice/functional-design/domain-entities.md)

## U12 Group Assignment

UC 53–57; FR-025, FR-026, FR-039.

- [aidlc-docs/construction/u12-group-assignment/code/as-built-baseline.md](u12-group-assignment/code/as-built-baseline.md)
- [aidlc-docs/construction/u12-group-assignment/functional-design/business-rules.md](u12-group-assignment/functional-design/business-rules.md)
- [aidlc-docs/construction/u12-group-assignment/functional-design/domain-entities.md](u12-group-assignment/functional-design/domain-entities.md)

## U13 Reference Materials

UC 58–62; FR-004, FR-005.

- [aidlc-docs/construction/u13-reference-materials/code/as-built-baseline.md](u13-reference-materials/code/as-built-baseline.md)
- [aidlc-docs/construction/u13-reference-materials/functional-design/business-rules.md](u13-reference-materials/functional-design/business-rules.md)
- [aidlc-docs/construction/u13-reference-materials/functional-design/domain-entities.md](u13-reference-materials/functional-design/domain-entities.md)

## U14 Announcements & Notifications

UC 63–65; FR-011, FR-019, FR-023.

- [aidlc-docs/construction/u14-announcements-and-notifications/code/as-built-baseline.md](u14-announcements-and-notifications/code/as-built-baseline.md)
- [aidlc-docs/construction/u14-announcements-and-notifications/functional-design/business-rules.md](u14-announcements-and-notifications/functional-design/business-rules.md)
- [aidlc-docs/construction/u14-announcements-and-notifications/functional-design/domain-entities.md](u14-announcements-and-notifications/functional-design/domain-entities.md)

## U15 Admin Dashboard

UC 66; FR-024, FR-038.

- [aidlc-docs/construction/u15-admin-dashboard/code/as-built-baseline.md](u15-admin-dashboard/code/as-built-baseline.md)
- [aidlc-docs/construction/u15-admin-dashboard/functional-design/business-rules.md](u15-admin-dashboard/functional-design/business-rules.md)
- [aidlc-docs/construction/u15-admin-dashboard/functional-design/domain-entities.md](u15-admin-dashboard/functional-design/domain-entities.md)
