# Components — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed, không phải runtime acceptance. 66 active UC, 50 MVP stories + 1 extension, 15 logical units.

| Unit | Trách nhiệm hiện hành | UC | Packages |
|---|---|---|---|
| U01 | Account & Access | 01–12 | [identity](../../../backend/src/main/java/edu/aiplatform/identity) |
| U02 | Audit | 13 | [audit](../../../backend/src/main/java/edu/aiplatform/audit) |
| U03 | Job and Settings | 14–15 | [files](../../../backend/src/main/java/edu/aiplatform/files), [jobs](../../../backend/src/main/java/edu/aiplatform/jobs), [settings](../../../backend/src/main/java/edu/aiplatform/settings), [backup](../../../backend/src/main/java/edu/aiplatform/backup) |
| U04 | Subject Administration | 16–19 | [subjects](../../../backend/src/main/java/edu/aiplatform/subjects) |
| U05 | Class & Enrollment | 20–30 | [academics](../../../backend/src/main/java/edu/aiplatform/academics) |
| U06 | AI Connections | 31–33 | [ai](../../../backend/src/main/java/edu/aiplatform/ai) |
| U07 | Assignment Authoring | 34–36 | [assignments](../../../backend/src/main/java/edu/aiplatform/assignments) |
| U08 | Assignment Work | 37–41 | [assignmentwork](../../../backend/src/main/java/edu/aiplatform/assignmentwork) |
| U09 | Assignment Grading | 42–45 | [grading](../../../backend/src/main/java/edu/aiplatform/grading) |
| U10 | Quiz Authoring | 46–48 | [quizzes](../../../backend/src/main/java/edu/aiplatform/quizzes) |
| U11 | Quiz Practice | 49–52 | [quizpractice](../../../backend/src/main/java/edu/aiplatform/quizpractice) |
| U12 | Group Assignment | 53–57 | [groups](../../../backend/src/main/java/edu/aiplatform/groups), [groupdocs](../../../backend/src/main/java/edu/aiplatform/groupdocs) |
| U13 | Reference Materials | 58–62 | [materials](../../../backend/src/main/java/edu/aiplatform/materials) |
| U14 | Announcements & Notifications | 63–65 | [announcements](../../../backend/src/main/java/edu/aiplatform/announcements), [notifications](../../../backend/src/main/java/edu/aiplatform/notifications) |
| U15 | Admin Dashboard | 66 | [admindashboard](../../../backend/src/main/java/edu/aiplatform/admindashboard) |

## Kiến trúc triển khai

Một backend Spring Boot và một worker dùng chung code/module; frontend Next.js; không phải 15 microservice. PostgreSQL/Flyway quản lý schema; Redis cho phiên/rate limit; RabbitMQ cho job/events; private Google Drive cho tệp/backup; SMTP Brevo demo/production và Mailpit local. Adapter availability phụ thuộc cấu hình/profile; có lớp fallback không chứng minh runtime đang dùng stub.

## Quyền và ranh giới

U01 kiểm actor/current role/session; U04/U05 cung cấp subject/class scope. U06 sở hữu AI quota/task/credential/MCP; U07 authoring, U08 attempts/activity, U09 grading/gradebook; U10 bank/Quiz, U11 Quiz attempts; U12 groups và groupdocs; U13 materials/modules, U14 announcements/notifications, U15 admin aggregates. Student dùng API cá nhân hoặc MCP cho UC41; Admin không kết nối MCP. Không wallet/payment/Code Lab/Judge0.

## Nguồn chi tiết

[Manifest code/schema/UI/tests](../reverse-engineering/current-code-baseline.md); [Public contract signatures](component-methods.md); [HTTP API inventory](../reverse-engineering/api-inventory.md); [Decision/delta register](../reverse-engineering/decision-register.md).
