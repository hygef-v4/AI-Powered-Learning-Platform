# Unit of Work — 2026-10-14

> Revision 2026-10-14 theo yêu cầu người dùng: 15 unit chia lại thành 3 iteration, mỗi iteration 5 unit, mỗi unit nằm trọn một iteration; unit chia theo dữ liệu sở hữu để liên kết trong cao và liên kết ngoài thấp. Danh mục 66 UC, 50 MVP story + 1 extension giữ nguyên ID. Đây là revision tài liệu; thiết kế Construction vẫn nằm trong các folder unit cũ, chưa tách/gộp theo unit mới.

66 active UC; 50 MVP stories + 1 extension; 15 logical units; 3 iteration × 5 unit

| Iter | Unit | Tên | Primary UC | LOC (nghìn, ước lượng) | Code |
|---|---|---|---|---|---|
| 1 | U01 | Account & Access | 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12 | 4 | Người 1 |
| 1 | U02 | Audit | 13 | 1 | Người 2 |
| 1 | U03 | Job and Settings | 14, 15 | 3 | Người 3 |
| 1 | U04 | Subject Administration | 16, 17, 18, 19 | 2 | Người 4 |
| 1 | U05 | Class & Enrollment | 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30 | 4 | Người 5 |
| 2 | U06 | AI Connections | 31, 32, 33 | 4 | Người 5 |
| 2 | U07 | Assignment Authoring | 34, 35, 36 | 5 | Người 4 |
| 2 | U08 | Assignment Work | 37, 38, 39, 40, 41 | 4 | Người 3 |
| 2 | U09 | Assignment Grading | 42, 43, 44, 45 | 3 | Người 2 |
| 2 | U10 | Quiz Authoring | 46, 47, 48 | 3 | Người 1 |
| 3 | U11 | Quiz Practice | 49, 50, 51, 52 | 2 | Người 1 |
| 3 | U12 | Group Assignment | 53, 54, 55, 56, 57 | 5 | Người 2 |
| 3 | U13 | Reference Materials | 58, 59, 60, 61, 62 | 2 | Người 4 |
| 3 | U14 | Announcements & Notifications | 63, 64, 65 | 2 | Người 3 |
| 3 | U15 | Admin Dashboard | 66 | 1 | Người 5 |

## Iteration

| Iter | Phạm vi | Units |
|---|---|---|
| 1 | Khung bắt buộc: tài khoản, phân quyền, audit, file/job/settings/backup, quản trị môn, lớp và ghi danh | U01–U05 |
| 2 | AI và nghiệp vụ chính: kết nối AI (API chung, API cá nhân, MCP), soạn/làm/chấm Assignment cá nhân, soạn Quiz và ngân hàng câu hỏi | U06–U10 |
| 3 | Phần còn lại: làm Quiz, Assignment nhóm, học liệu, thông báo lớp và thông báo hệ thống, Admin Dashboard | U11–U15 |

Mỗi unit chỉ phụ thuộc unit ở iteration trước hoặc cùng iteration; không có vòng phụ thuộc. Sơ đồ: [unit-of-work-dependency.drawio](unit-of-work-dependency.drawio) (chỉ vẽ phụ thuộc trực tiếp).

## Boundaries

- 15 module logic trong một Spring Boot backend và worker, không phải 15 microservice.
- Mỗi unit sở hữu bảng dữ liệu, API và màn hình của mình; unit khác chỉ gọi qua public port, không đọc bảng/repository của nhau.
- U06 là cổng AI duy nhất: API chung theo quota giảng viên, API cá nhân, MCP, adapter Gemini/OpenAI/Claude, lịch sử dùng. UC 41 (U08) và UC 43 (U09) gọi U06 qua port.
- Ghi nhận hoạt động làm bài (FR-039) thuộc U08; U09 đọc để hiện cho giảng viên; U11 và U12 ghi sự kiện qua port của U08.
- Backup (FR-038) thuộc U03; U15 đọc trạng thái.
- Sự kiện bài mới, công bố điểm, thông báo quan trọng được phát từ iter 2 nhưng chỉ được gửi tới người dùng khi U14 hoàn tất ở iter 3.
- Assignment và Quiz không phụ thuộc học liệu (U13).

## Phụ thuộc

| Unit | Phụ thuộc bắt buộc | Contract / sự kiện |
|---|---|---|
| U01 | U02, U03 | Ghi audit qua U02; OTP và email qua job của U03 |
| U02 | Không | Mọi unit ghi audit qua AuditPort |
| U03 | Không | U06 khai báo mục cài đặt API chung và quota; U15 đọc trạng thái backup |
| U04 | U01 | U05 đọc phạm vi môn |
| U05 | U01, U04 | Các unit iter 2 và 3 kiểm phạm vi lớp qua ClassScopePort |
| U06 | U01, U03 | Port gọi AI cho U07–U10; MCP nhận tool do U07–U10 đăng ký, nên U06 không phụ thuộc ngược |
| U07 | U03, U05 | Gọi U06 để AI soạn đề; phát sự kiện bài mới/mở cho U14 |
| U08 | U03, U05, U06, U07 | Cung cấp bài nộp và ActivityQueryPort cho U09; ActivityWritePort cho U11, U12 |
| U09 | U05, U06, U07, U08 | Phát sự kiện công bố điểm cho U14 |
| U10 | U04, U05 | Gọi U06 để AI soạn câu hỏi; U11 đọc quiz đã phát hành |
| U11 | U05, U10 | Ghi hoạt động làm quiz qua ActivityWritePort của U08 |
| U12 | U05, U07, U08, U09 | Dùng khung tài liệu U07, workspace U08, chấm U09 |
| U13 | U03, U05 | Không unit nào bắt buộc phụ thuộc học liệu |
| U14 | U01, U03, U05 | Nhận sự kiện từ U07–U12; email qua job của U03 |
| U15 | U01, U03, U05 | Đọc số đếm từ U01, U04, U05 và trạng thái backup từ U03 |

## Phân công code

Mỗi người code một unit mỗi iteration. LOC là ước lượng code production (backend và frontend, chưa tính test), dùng để cân tải; cần đối chiếu LOC thực tế sau iter 1.

| Người | Iter 1 | Iter 2 | Iter 3 | LOC (nghìn) |
|---|---|---|---|---|
| Người 1 | U01 Account & Access | U10 Quiz Authoring | U11 Quiz Practice | 9 |
| Người 2 | U02 Audit | U09 Assignment Grading | U12 Group Assignment | 9 |
| Người 3 | U03 Job and Settings | U08 Assignment Work | U14 Announcements & Notifications | 9 |
| Người 4 | U04 Subject Administration | U07 Assignment Authoring | U13 Reference Materials | 9 |
| Người 5 | U05 Class & Enrollment | U06 AI Connections | U15 Admin Dashboard | 9 |

Theo quy ước nhóm, mỗi unit có ba người khác nhau viết requirement, viết test case và code để review chéo; phân công requirement/test chưa ghi trong tài liệu này.

## Đối chiếu unit cũ và folder Construction

| Unit mới | Lấy từ unit cũ | Folder Construction hiện có |
|---|---|---|
| U01 Account & Access | U01 | [u01-account-and-access](../../construction/u01-account-and-access/functional-design/business-rules.md) |
| U02 Audit | U02 | [u02-audit-job-and-outbox](../../construction/u02-audit-job-and-outbox/functional-design/business-rules.md) |
| U03 Job and Settings | U03 (+ backup) | [u03-job-and-settings](../../construction/u03-job-and-settings/functional-design/business-rules.md) |
| U04 Subject Administration | U04 (phần môn) | [u04-subject-administration](../../construction/u04-subject-administration/functional-design/business-rules.md) (folder mới 2026-10-15) |
| U05 Class & Enrollment | U04 (phần lớp, ghi danh) | [u05-class-enrollment](../../construction/u05-class-enrollment/functional-design/business-rules.md) (folder mới 2026-10-15) |
| U06 AI Connections | U07, U13 | [u06-ai-connections](../../construction/u06-ai-connections/functional-design/business-rules.md) (folder mới 2026-10-15) |
| U07 Assignment Authoring | U08, U09 (Assignment), U06 (rubric) | [u07-assignment-authoring](../../construction/u07-assignment-authoring/functional-design/business-rules.md) (folder mới 2026-10-15) |
| U08 Assignment Work | U11 (Assignment) | [u08-assignment-work](../../construction/u08-assignment-work/functional-design/business-rules.md) (folder mới 2026-10-15) |
| U09 Assignment Grading | U15, U16 (xuất sổ điểm) | [u09-assignment-grading](../../construction/u09-assignment-grading/functional-design/business-rules.md) (folder mới 2026-10-15) |
| U10 Quiz Authoring | U06 (ngân hàng), U08, U09 (Quiz) | [u10-quiz-authoring](../../construction/u10-quiz-authoring/functional-design/business-rules.md) (folder mới 2026-10-15) |
| U11 Quiz Practice | U11 (Quiz), U15 (chấm Quiz) | [u11-quiz-practice](../../construction/u11-quiz-practice/functional-design/business-rules.md) (folder mới 2026-10-15) |
| U12 Group Assignment | U12, U14, U09 (soạn bài nhóm) | [u12-group-assignment](../../construction/u12-group-assignment/functional-design/business-rules.md) (folder mới 2026-10-15) |
| U13 Reference Materials | U05 (học liệu) | [u13-reference-materials](../../construction/u13-reference-materials/functional-design/business-rules.md) (folder mới 2026-10-15) |
| U14 Announcements & Notifications | U05 (thông báo lớp), U16 (thông báo) | [u14-announcements-and-notifications](../../construction/u14-announcements-and-notifications/functional-design/business-rules.md) (folder mới 2026-10-15) |
| U15 Admin Dashboard | U16 (Admin Dashboard) | [u15-admin-dashboard](../../construction/u15-admin-dashboard/functional-design/business-rules.md) (folder mới 2026-10-15) |

Folder Construction giữ tên cũ để không phá liên kết; thiết kế chi tiết của unit mới đọc tại các folder tương ứng ở bảng trên cho tới khi được tách/gộp lại.

## Integration gates

| Gate | Evidence |
|---|---|
| G1 — cuối iter 1 | Kích hoạt, đăng nhập, khôi phục mật khẩu; role và scope; quản trị tài khoản, môn; lớp và ghi danh; Settings; file/job; backup có kiểm thử restore; audit ghi và tra cứu; các trường hợp bị từ chối quyền |
| G2 — cuối iter 2 | 3 provider API; API chung theo quota, API cá nhân, MCP; thu hồi key; Assignment cá nhân có/không Draw.io, duyệt, phát hành, version, copy, rubric; tự lưu, nộp, lịch sử nộp, phản hồi AI cho bài Practice, ghi nhận hoạt động; chấm tay, AI đề xuất điểm, công bố, sổ điểm và xuất file; soạn Quiz và ngân hàng câu hỏi |
| G3 — cuối iter 3 | Làm Quiz và chấm tự động; Assignment nhóm (chia nhóm, trưởng nhóm, nhận phần, nộp chung, điểm đóng góp); học liệu file và link YouTube; thông báo lớp, thông báo trong app và email; Admin Dashboard kèm trạng thái backup |

## DoD và trạng thái

Requirement, test case độc lập, code và bằng chứng kiểm thử liên kết với nhau; sửa tài liệu không thay cho kiểm thử runtime. D01–D04 đã chốt 2026-10-15: Quiz chỉ Practice, nhóm chỉ Graded, bài nhóm không có AI cho Student, quota theo ngày và trần hệ thống.

## UC consolidation — 66 active UC

UC 14 View Settings và UC 15 Edit Setting là primary U03, gồm UI cấu hình API chung và quota; U06 giữ dữ liệu quota, kết nối và credentials. UC 66 gộp UC 80 (ID cũ) (U15 dashboard, U03 backup). UC 42 gộp UC 81 (ID cũ) (U09 xem, U08 dữ liệu hoạt động). UC 35 soạn Assignment cá nhân (U07), UC 53 soạn Assignment nhóm (U12); Draw.io tùy chọn trong UC 39/56. UC 26, 76, 78, 79, 80, 81, 82 (ID cũ) retired; ID không dùng lại.
