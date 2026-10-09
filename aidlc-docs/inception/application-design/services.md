# Services and Orchestration

## 1. Service layer pattern

Mỗi module có application service làm transaction boundary. Controller nhận REST request, validate hình thức, tạo actor context rồi gọi service. Domain object thực thi invariants; repository/port xử lý persistence hoặc dịch vụ ngoài. Workflow liên module dùng port đồng bộ hoặc event phát sau commit (U03); job dài chạy trong `worker`.

## 2. Dịch vụ nghiệp vụ

Tên service dưới đây là tên logic của module; tên class cụ thể (ví dụ `UploadService`, `ArtifactService` của U03) nằm trong `nfr-design/logical-components.md` của từng unit.

| Service (unit) | Orchestration chính | Không được làm |
|---|---|---|
| AccountService, AuthorizationService (U01) | Kích hoạt OTP, phiên, khôi phục, role; quyết định quyền theo role + phạm vi U04 | Không cho admin đặt/xem mật khẩu hay OTP; không tin quyền do frontend gửi |
| AuditService (U02), JobService (U03) | Audit append-only ghi trong transaction; gửi sau commit vào 1 trong 7 queue, retry bằng queue TTL, `PendingSweeper` gửi lại dòng chờ, `ScheduledScanner` cho việc theo thời gian | Không cung cấp update/delete audit; không quyết định nghiệp vụ |
| SettingsService (U03) | Admin xem/sửa Settings, type/range/version/audit, cache 30 giây; seed chỉ khóa thiếu | Không lưu secrets trong Settings |
| FileArtifactService (U03) | Upload, kiểm loại/dung lượng file, lưu Drive, token tải | Không tự quyết ai được xem file; không dùng `acknowledgeAbuse` |
| AcademicService (U04) | Môn, lớp, phân công, ghi danh do Chủ nhiệm môn quản lý, lớp của người học | Không xóa lịch sử ghi danh; không kiểm thanh toán |
| ContentService, ClassCommunicationService (U05) | Module, học liệu tải lên của môn/lớp, ingest RAG, `retrieve`; tạo/sửa/xóa thông báo theo R3/R4 và sự kiện thông báo cho U16 | Không tự phiên âm; không xử lý ingest trong request; không cho lớp khác đọc/ghi |
| BankService (U06) | Phiên bản câu hỏi/rubric, nhập file, tính điểm rubric | Không sửa bản đã `ACTIVE` |
| PackageService, PaymentService, CreditService (U07) | Admin thêm/sửa gói và đọc lịch sử toàn nền tảng; PayOS/webhook/tự đối soát, ví credit, giữ/trừ/trả | Không tin browser redirect; không để số dư âm |
| AssessmentService (U08) | Bài, duyệt, phát hành, lịch, khóa, version mới, ngưng giao | Không sửa version đã phát hành; bài của môn một lịch chung mọi lớp OPEN, không nhóm |
| TypeConfigService, DocumentService (U09) | Cấu hình loại bài, mô hình tài liệu, DOCX | Không cho sửa block khóa của giảng viên |
| AssignmentCopier (U08) | Copy/version/clone bài, rubric theo bài; chỉ copy khi dạy cả hai lớp | Không copy lịch, lượt làm, bài nộp, điểm; không diff screen |
| AttemptService (U11) | Bắt đầu lượt, tự lưu, nộp, tự nộp | Không sửa bài đã nộp |
| GroupService (U12) | Nhóm của lớp, chia ngẫu nhiên, trưởng nhóm, yêu cầu đổi trưởng nhóm | Giảng viên không giao mục cho từng sinh viên (trưởng nhóm giao mục ở U14) |
| AiService, CodeRunService (U13) | Kiểm trần/credit, gọi Gemini, kiểm đầu ra; chạy Judge0 | Không phát hành đề, không chốt điểm; không chạy mã ngoài sandbox |
| GroupDocumentService (U14) | Tài liệu nhóm theo các phần của khung, giao phần của trưởng nhóm, khóa phần, Xong → realtime, nộp, tự nộp khi hết hạn | Không cho hai người sửa cùng một mục; không cho sinh viên sửa/xóa mục của giảng viên |
| GradingService (U15) | Tự chấm, chấm tay/AI, chốt, công bố, sổ điểm | Không để AI hay công thức quyết định điểm cuối |
| NotificationService, ReportingService (U16) | Thông báo, email có trần, nhắc hạn, tiến độ, phân bố điểm ẩn danh, thống kê quản trị và xuất bảng điểm | Không rollback nghiệp vụ khi gửi email lỗi; không công bố điểm nháp hoặc điểm AI đề xuất |

## 3. Orchestration quan trọng

### Bài tài liệu có sơ đồ và AI đề xuất chấm
1. Người học soạn tài liệu (U09 `DocumentEditor`), có thể nhập DOCX vào lượt DOCUMENT sau khi xem trước, vẽ sơ đồ trong iframe Draw.io; U11 tự lưu và kiểm tài liệu qua U09, không cho DOCX sửa khung giảng viên.
2. Nộp: U11 khóa nội dung. Chỉ bài `GRADED` gọi `SubmissionSubmittedPort` trong cùng transaction để U15 tạo dòng `evaluations` `PENDING`; Quiz/Code Lab `PRACTICE` dùng scorer đáp án/test; Text/Diagram Practice không gọi AI khi nộp, chỉ gửi yêu cầu UC 29 khi Student bấm chấm và đủ credit; kết quả giữ riêng `kind = PRACTICE`.
3. U15 tạo điểm `PENDING`; giảng viên chọn chấm tay hoặc "Nhờ AI đề xuất".
4. U13 kiểm trần và credit, lấy văn bản phẳng + XML rút gọn (U09), gọi Gemini, kiểm đầu ra, trả đề xuất.
5. Giảng viên dùng/sửa đề xuất, chốt, công bố (U15); U16 báo người học.

### Bài nhóm
1. Giảng viên chia nhóm trong danh sách sinh viên của lớp (U12), mỗi nhóm đúng một trưởng nhóm; mọi bài nhóm của lớp dùng chung các nhóm này.
2. Bài mở: U08 gọi `AssignmentLifecyclePort.onOpened` trong transaction; U14 gửi việc `GROUP_DOC_CREATE` dựng tài liệu cho mọi nhóm của lớp từ các phần của khung.
3. Trưởng nhóm giao phần; thành viên nhận hoặc làm phần được giao trong popup che kín trang, bấm Xong → ghép realtime vào Assignment Workspace (SSE qua `platform.realtime`).
4. Trưởng nhóm nộp bất kỳ lúc nào trước hạn; hết hạn thì tự nộp, gồm phần đang làm của thành viên; U15 chấm tài liệu chung như bài `DOCUMENT` (tay/AI), điểm đóng góp thành viên mặc định bằng điểm tài liệu chung, chấm tay từng người.

### Tạo và phát hành bài
1. Teacher/Subject Manager được giao lớp tạo bài lớp; Chủ nhiệm môn R2 tạo bài môn. U08 giữ vòng đời/câu riêng, U06 ngân hàng môn/rubric, U09 cấu hình và khung; U13 AI draft chỉ đề xuất.
2. Bài lớp: Text Essay, Code Lab, Diagram Essay hoặc Group Assignment; bài môn không Group Assignment. Duyệt trước phát hành; rubric tự tạo trống khi soạn, phải đầy đủ và khóa lúc phát hành. Bài môn giao mọi lớp OPEN (kể cả mở sau) với một lịch chung; giảng viên từng lớp chấm/công bố cho lớp mình.
3. Quiz riêng gắn học liệu: DRAFT → REVIEWED → OPEN ngay → RETIRED; không lịch đóng, gradebook hoặc Teacher grading queue. Student làm từ Learning Material/Quiz Practice.
4. Nội dung đã phát hành bất biến; version/clone/copy ở U08, không template/U10. Copy chỉ bài lớp và người copy phải dạy cả lớp nguồn/đích.

### Quét học liệu cho RAG
1. Chủ nhiệm môn tạo module trên Material List; học liệu môn tải từ đó, học liệu lớp tải từ tab Materials của Teacher Class Detail. API U05 kiểm scope và giữ credit trước tạo lesson trong cùng transaction; thiếu credit không tạo lesson. U03 upload byte là bước staging riêng, không đồng nghĩa đã tạo học liệu.
2. Lesson hiện ngay; worker claim bằng lease, trích chữ/caption, tóm tắt theo đoạn, lưu summary đã hoàn thành rồi embedding từ summary. Mỗi lời gọi qua AiUsagePort/Settings kiểm trần và ghi lượng sử dụng thật vào hold của uploader. Không OCR/phiên âm và không quét lại thủ công.
3. Lỗi tạm chuyển PENDING trước retry; BUSY tự gửi lại mỗi 30 phút tới scan_expires_at tuyệt đối (24 giờ từ khi tạo). Lease SCANNING hết hạn được sweeper phục hồi; fencing token ngăn worker cũ ghi kết quả. Job trùng không xử lý đồng thời cùng lesson.
4. INDEXED/NO_TEXT/NO_CAPTION/FAILED chốt hold: chỉ settle lời gọi AI thật, trả phần chưa dùng; chưa gọi AI thì trả toàn bộ. Embedding lỗi sau summary không xóa summary đã có. Hold scanner U13 ở 25 giờ là dự phòng, không thay giới hạn scan 24 giờ.

### Thanh toán mua credit AI
1. U07 cho tài khoản `ACTIVE` thuộc Student, Teacher hoặc Subject Manager (Admin không có ví) tạo giao dịch PayOS với idempotency key; trang quay về chỉ hiển thị. Sinh viên chỉ dùng credit cho AI chấm bài `PRACTICE` (UC 29).
2. Webhook có chữ ký hoặc `PaymentScanner` + việc `PAYOS_CHECK` → `UPDATE ... WHERE status <> 'PAID'` chuyển `PAID` và cộng số dư mua đúng một lần; sau commit phát `payment.paid`.


### Quản trị gói và lịch sử thanh toán (UC 68–69, 72)
1. U01 xác nhận Administrator; U07 PackageService kiểm thông tin/giá/credit và version, lưu thay đổi với audit.
2. Checkout dùng snapshot tại lúc tạo giao dịch. Thay gói chỉ ảnh hưởng giao dịch mới; webhook/đối soát cấp credit theo snapshot, không theo giá hiện hành.
3. U07 truy vấn lịch sử toàn nền tảng có bộ lọc/phân trang cho Admin; không có thao tác sửa giao dịch hoặc credit thủ công.

### Thông báo lớp và scope
1. U01/U04 kiểm Teacher R3 hoặc vai trò kế thừa R4 trước khi U05 tạo/sửa/xóa thông báo.
2. Sửa kiểm version và audit trước/sau; xóa loại khỏi feed nhưng giữ audit/tham chiếu. Chỉ tạo mới phát sự kiện in-app cho U16.
3. Các chức năng học liệu/bài của môn/ngân hàng môn kiểm R2; submissions/chấm/gradebook kiểm R3/R4. Admin quản trị cấu trúc không tự cấp quyền giảng dạy.

### Tra cứu Audit Log (UC 73)
U01 kiểm Administrator; U02 queryAudit lọc actor/action/object/result/time, phân trang và che dữ liệu nhạy cảm. API chỉ đọc, không sửa/xóa audit; thao tác nghiệp vụ vẫn ghi audit append-only.

## 4. Job policies

Không có bảng job: việc gửi sau commit lên một trong 7 queue (U03 BR-U03-63), trạng thái nằm ở dòng nghiệp vụ; `PendingSweeper` gửi lại dòng chờ quá 5 phút; việc theo thời gian chạy bằng `ScheduledScanner` mỗi phút.

| Việc (unit) | Queue / cơ chế | Retry | Idempotency |
|---|---|---|---|
| `OTP_DELIVERY` (U01) | `jobs.email` (ưu tiên cao) | Backoff U03, tối đa 5 lần | Theo tài khoản + mục đích + phút |
| `EMAIL_SEND` (U16) | `jobs.email` | Backoff U03, tối đa 5 lần | Theo `notifications.email_status`; trần 300/ngày |
| `DRIVE_CLEANUP` (U03) | `jobs.drive` | Backoff U03 | Theo `file_id` |
| `LESSON_SCAN` (U05) | `jobs.gemini` | Lỗi tạm retry sau reset PENDING; BUSY sweeper 30 phút, giới hạn tuyệt đối 24 giờ | Theo scan_status + lease/fencing + requestRef |
| `YOUTUBE_CAPTION` (U05) | `jobs.youtube` | Lỗi tạm retry | Theo lesson + lease/fencing |
| `PAYOS_CHECK` (U07) | `jobs.payos` | Backoff U03 | Theo `order_code` |
| `GROUP_DOC_CREATE` (U14) | `jobs.triggered` | Chạy lại không tạo trùng | Unique nhóm × bài |
| `AI_TASK` (U13) | `jobs.gemini` | Lỗi tạm tối đa 3 lần | Theo `ai_suggestions.id` |
| `CODE_RUN` (U13) | `jobs.code` | Lỗi sandbox retry; lỗi code của người học không retry | Theo lượt / câu hỏi |
| `AssignmentScheduleScanner` (U08), `AttemptDeadlineScanner` (U11), `GroupAutoSubmitScanner` (U14), `CreditReservationScanner`, `AiPendingSweeper` (U13), `PaymentScanner` (U07), `DeadlineReminderScanner`, `EmailDeferredScanner`, `NotificationRetentionScanner` (U16) | `ScheduledScanner` | Lần quét sau làm lại | Điều kiện trên dòng nghiệp vụ |

## 5. REST contract

- Base path `/api/v1`; OpenAPI mỗi unit trong `/contracts/openapi/`.
- Command quan trọng dùng `Idempotency-Key` hoặc `version` (khóa lạc quan).
- Lỗi dạng problem-details an toàn, có correlation ID, không lộ stack trace.
- Việc dài trả `202`; trạng thái đọc qua API của unit sở hữu (cột trạng thái của dòng nghiệp vụ, ví dụ `lessons.scan_status`), không có API trạng thái job chung (BR-U03-61).
- Realtime bằng SSE cho tài liệu nhóm (U14) và thông báo (U16).

### Contract checkpoint học liệu

U05 dùng AiUsagePort.findHold theo lesson + requestRef HOLD; begin trả RUN hoặc REPLAY checkpoint READY (BUSY/IN_PROGRESS/CLOSED không gọi provider). complete(ticket, tokens, cost, checkpoint) ghi kết quả và lượng dùng cùng transaction. U05 kiểm claim/lease trước ghi trong cùng transaction; U13 gắn scanClaimId vào ticket metadata và khóa HOLD trước cộng usage/chốt. Không gọi provider trong transaction và không cho U05 đọc repository U13. DTO/điều kiện cụ thể ở [contract U05](../../construction/u05-content-material-rag/functional-design/domain-entities.md).
