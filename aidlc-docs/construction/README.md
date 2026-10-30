# Construction — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; 66 UC, 15 units. Source nghiệp vụ đã có ở cả 15 unit; trạng thái source-observed không thay cho integration/E2E/NFR acceptance. Code-generation plans có ngày giữ bằng chứng phạm vi/phê duyệt trước đây, không còn đồng loạt là “chưa code”.

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

## Đọc tiếp

- [Tổng hợp unit](unit-summary.md): folder thiết kế, source và evidence.
- [Baseline](../inception/reverse-engineering/current-code-baseline.md), [traceability](../inception/reverse-engineering/doc-code-traceability.md).
- [Build và test](build-and-test/build-and-test-summary.md): evidence có ngày, không cộng dồn test.

Folder unit cũ được giữ để không phá liên kết; ownership theo bảng trên. Các folder chỉ dùng cho scratch/remediation không trở thành unit mới.
