# Unit of Work Story Map

## 1. Quy tắc ánh xạ

- Mỗi user story có đúng một unit chủ trì chịu trách nhiệm acceptance criteria và Definition of Done.
- Unit hỗ trợ được ghi riêng, không tạo ownership trùng.
- Phase 2 được giữ trong cùng bounded context với MVP để tránh tách sai ranh giới.
- Tổng nguồn chuẩn: 55 story, gồm 44 MVP và 11 Phase 2.

## 2. Tổng hợp coverage

| Unit | MVP | Phase 2 | Tổng |
|---|---:|---:|---:|
| U01 Platform Foundation and Identity | 8 | 0 | 8 |
| U02 Academic Administration | 3 | 1 | 4 |
| U03 Content, Learning and Banks | 7 | 3 | 10 |
| U04 Assessment Authoring and Publication | 4 | 1 | 5 |
| U05 Submission and Group Work | 8 | 0 | 8 |
| U06 Grading, AI and Code Execution | 9 | 3 | 12 |
| U07 Reporting and Notification | 2 | 3 | 5 |
| U08 Payment and Entitlement | 3 | 0 | 3 |
| **Tổng** | **44** | **11** | **55** |

## 3. U01 - Platform Foundation and Identity

| Story | Phase | Nội dung | Unit hỗ trợ chính |
|---|---|---|---|
| US-IAM-001 | MVP | Nhận và kích hoạt tài khoản trường cấp | U07 notification |
| US-IAM-002 | MVP | Đăng nhập và đăng xuất an toàn | - |
| US-IAM-003 | MVP | Khôi phục mật khẩu riêng tư | U07 notification |
| US-IAM-004 | MVP | Quản lý hồ sơ cá nhân | - |
| US-IAM-005 | MVP | Quản lý vai trò và phạm vi môn | U02 scope reference |
| US-IAM-006 | MVP | Đổi mật khẩu cá nhân | - |
| US-IAM-007 | MVP | Quản trị vòng đời tài khoản | - |
| US-AUD-001 | MVP | Tra cứu audit nghiệp vụ và bảo mật | Tất cả unit phát audit event |

## 4. U02 - Academic Administration

| Story | Phase | Nội dung | Unit hỗ trợ chính |
|---|---|---|---|
| US-CAT-001 | MVP | Quản lý cấu trúc môn và lớp | U01 authorization/audit |
| US-CAT-002 | MVP | Quản lý vòng đời lớp/khóa học | U01 authorization/audit |
| US-CAT-003 | MVP | Ghi danh người học | U01 identity, U07 notification |
| US-CAT-005 | Phase 2 | Tự ghi danh bằng mã mời | U01 rate limit/audit |

`US-CAT-004` không tồn tại trong catalog đã duyệt và không được đưa lại vào phạm vi.

## 5. U03 - Content, Learning and Banks

| Story | Phase | Nội dung | Unit hỗ trợ chính |
|---|---|---|---|
| US-CNT-001 | MVP | Quản lý kho học liệu và RAG cấp môn | U01 file/job, U06 worker adapter |
| US-CNT-002 | MVP | Quản lý nội dung riêng của lớp | U01 file, U02 class scope |
| US-CNT-003 | Phase 2 | Tìm kiếm và tóm tắt học liệu | U06 AI orchestration |
| US-CNT-004 | Phase 2 | Thông báo và hỏi đáp trong lớp | U07 notification |
| US-LRN-001 | MVP | Truy cập lớp đã ghi danh | U02 enrollment, U08 entitlement port |
| US-LRN-002 | MVP | Lưu tiến độ và tiếp tục học | U02 enrollment |
| US-LRN-003 | MVP | Theo dõi tiến độ lớp | U02 class scope |
| US-QBK-001 | MVP | Quản lý ngân hàng rubric | U01 audit |
| US-QBK-002 | MVP | Quản lý ngân hàng câu hỏi | U01 audit |
| US-QBK-003 | Phase 2 | Phân tích chất lượng câu hỏi | U07 reporting projection |

## 6. U04 - Assessment Authoring and Publication

| Story | Phase | Nội dung | Unit hỗ trợ chính |
|---|---|---|---|
| US-ASM-001 | MVP | Duyệt và xuất bản bài đánh giá của lớp | U02 scope, U03 banks |
| US-ASM-002 | MVP | Phát hành đề chung cho mọi lớp thuộc môn | U02 subject/classes |
| US-ASM-006 | MVP | Soạn và kiểm tra bài trắc nghiệm | U03 question bank |
| US-ASM-007 | MVP | Soạn bài viết luận | U03 rubric bank |
| US-ASM-008 | Phase 2 | Nhân bản, sửa phiên bản và ngừng giao bài | U01 audit |

## 7. U05 - Submission and Group Work

| Story | Phase | Nội dung | Unit hỗ trợ chính |
|---|---|---|---|
| US-GRP-001 | MVP | Chia lớp thành nhóm và chỉ định trưởng nhóm | U02 enrollment |
| US-GRP-002 | MVP | Yêu cầu thay đổi trưởng nhóm | U01 audit, U02 enrollment |
| US-GRP-003 | MVP | Tạo bài tập nhóm và phân chia phần cá nhân | U04 assessment |
| US-GRP-004 | MVP | Nộp và chấm phần cá nhân của bài nhóm | U06 grading/AI proposal |
| US-GRP-005 | MVP | Trưởng nhóm nộp tài liệu chung | U01 file, U02/U05 leader check |
| US-GRP-006 | MVP | Đối chiếu và chấm tay bài chung | U06 manual grading |
| US-ASM-003 | MVP | Làm và nộp bài | U01 file/idempotency, U04 publication |
| US-ASM-004 | MVP | Soạn và làm bài sơ đồ Draw.io | U04 authoring, U01 artifact, U06 compact AI copy |

## 8. U06 - Grading, AI and Code Execution

| Story | Phase | Nội dung | Unit hỗ trợ chính |
|---|---|---|---|
| US-AIG-001 | MVP | Tạo bản nháp bài tập cho lớp bằng AI | U03 content, U04 draft |
| US-AIG-002 | MVP | Tạo bản nháp đề chung cấp môn bằng AI | U02 scope, U03 RAG, U04 draft |
| US-AIG-003 | MVP | Cấu hình và giám sát sử dụng AI | U01 audit, U07 reporting |
| US-ASM-005 | MVP | Soạn và kiểm thử Code Lab | U04 authoring, worker sandbox adapter |
| US-GRD-001 | MVP | Nhận kết quả tự chấm câu hỏi xác định | U04 assessment, U05 submission |
| US-GRD-002 | MVP | Nhận đề xuất chấm câu trả lời mở từ AI | U03 rubric, U05 submission |
| US-GRD-003 | MVP | Duyệt, ghi đè và công bố điểm | U01 audit, U05 submission |
| US-GRD-004 | MVP | Xem sổ điểm theo quyền | U02 scope, U07 read projection |
| US-GRD-005 | MVP | Kiểm tra và chốt điểm hàng loạt | U01 audit, U05 submission |
| US-GRD-006 | Phase 2 | Yêu cầu gia hạn nộp bài | U04 publication, U05 submission |
| US-GRD-007 | Phase 2 | Khiếu nại và phúc khảo điểm | U01 audit, U05 submission |
| US-GRD-008 | Phase 2 | Kiểm tra tương đồng bài nộp | U05 submission, worker adapter |

`US-GRD-009` không tồn tại trong catalog đã duyệt; chức năng phân công chấm chéo không được đưa lại vào phạm vi.

## 9. U07 - Reporting and Notification

| Story | Phase | Nội dung | Unit hỗ trợ chính |
|---|---|---|---|
| US-RPT-001 | MVP | Theo dõi tiến độ nộp bài và nhắc nhở | U02 class, U05 submission |
| US-RPT-002 | Phase 2 | Dashboard kết quả cá nhân | U03 progress, U06 grades |
| US-RPT-003 | Phase 2 | Xuất bảng điểm | U06 grades, U01 file/job |
| US-RPT-004 | Phase 2 | Đối sánh điểm AI và điểm chốt | U06 proposals/final grades |
| US-NTF-001 | MVP | Nhận thông báo thiết yếu | Tất cả producer qua outbox |

## 10. U08 - Payment and Entitlement

| Story | Phase | Nội dung | Unit hỗ trợ chính |
|---|---|---|---|
| US-PAY-001 | MVP | Bắt đầu thanh toán an toàn | U01 identity/audit |
| US-PAY-002 | MVP | Nhận quyền sau xác nhận thanh toán | U01 idempotency/audit, U02 learner/product reference |
| US-PAY-003 | MVP | Đối soát trạng thái thanh toán | U01 job, worker reconciliation handler |

## 11. Cross-cutting acceptance ownership

| Concern | Owner chuẩn | Trách nhiệm của unit nghiệp vụ |
|---|---|---|
| Authentication/authorization | U01 | Truyền actor/resource context và gọi authorization contract |
| Audit | U01 | Phát đúng security/business event, không log secret/PII |
| File/artifact | U01 | Khai báo purpose, scope, checksum và retention phù hợp |
| Job/outbox | U01 | Định nghĩa idempotent handler, retry class và safe failure state |
| Worker runtime | `/worker` deployable | Handler vẫn do U03/U06/U07/U08 sở hữu về nghiệp vụ |
| Reporting | U07 | Producer phát versioned event; không cho projection ghi ngược transaction nguồn |

## 12. Coverage assertions

- Mọi ID trong `stories.md` xuất hiện đúng một lần ở cột Story của các bảng U01-U08.
- Không thêm lại `US-CAT-004` hoặc `US-GRD-009` đã bị loại khỏi phạm vi.
- Bài viết chỉ dùng loại chung “bài viết luận”; không có loại ngoại ngữ riêng.
- Bản Draw.io chuẩn là full XML; compact XML chỉ được tạo cho AI grading sau yêu cầu của giảng viên.
- Bài cá nhân của bài nhóm có thể nhận AI proposal nếu giảng viên chọn; bài DOCX chung luôn do giảng viên chấm tay.
