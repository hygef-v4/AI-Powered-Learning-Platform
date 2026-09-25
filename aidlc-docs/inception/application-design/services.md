# Services and Orchestration

## 1. Service layer pattern

Mỗi module có application service làm transaction boundary. Controller nhận REST request, validate hình thức, tạo actor context rồi gọi service. Domain object thực thi invariants; repository/port xử lý persistence hoặc dịch vụ ngoài. Workflow liên module dùng port đồng bộ hoặc event phát sau commit (U02); job dài chạy trong `worker`.

## 2. Dịch vụ nghiệp vụ

Tên service dưới đây là tên logic của module; tên class cụ thể (ví dụ `UploadService`, `ArtifactService` của U03) nằm trong `nfr-design/logical-components.md` của từng unit.

| Service (unit) | Orchestration chính | Không được làm |
|---|---|---|
| AccountService, AuthorizationService (U01) | Kích hoạt OTP, phiên, khôi phục, role; quyết định quyền theo role + phạm vi U04 | Không cho admin đặt/xem mật khẩu hay OTP; không tin quyền do frontend gửi |
| AuditService, JobService (U02) | Audit append-only; enqueue trong transaction, gửi sau commit, retry theo DB, sweeper | Không cung cấp update/delete audit; không quyết định nghiệp vụ |
| FileArtifactService (U03) | Upload, kiểm loại/dung lượng file, lưu Drive, token tải | Không tự quyết ai được xem file; không dùng `acknowledgeAbuse` |
| AcademicService (U04) | Môn, lớp, phân công, ghi danh, mã mời, lớp của người học | Không xóa lịch sử ghi danh; không kiểm thanh toán |
| ContentService, ClassCommunicationService (U05) | Chương/bài/phiên bản, liên kết bài cấp môn, ingest RAG, `retrieve`; thông báo/hỏi đáp lớp và sự kiện U16 | Không tự phiên âm; không xử lý ingest trong request; không cho lớp khác đọc/ghi |
| BankService (U06) | Phiên bản câu hỏi/rubric, nhập file, tính điểm rubric | Không sửa bản đã `ACTIVE` |
| PaymentService, CreditService (U07) | PayOS, webhook, đối soát, ví credit, giữ/trừ/trả | Không tin browser redirect; không để số dư âm |
| AssessmentService (U08) | Bài, duyệt, phát hành, lịch, khóa, version mới, ngưng giao | Không sửa version đã phát hành; không có đề chung |
| TypeConfigService, DocumentService (U09) | Cấu hình loại bài, mô hình tài liệu, DOCX | Không cho sửa block khóa của giảng viên |
| TemplateService, CopyService, SimulationService (U10) | Template, copy, diff, chính sách thi thử | Không copy lịch, lượt làm, bài nộp, điểm |
| AttemptService (U11) | Bắt đầu lượt, tự lưu, nộp, tự nộp | Không sửa bài đã nộp |
| GroupService (U12) | Bộ nhóm, trưởng nhóm, yêu cầu đổi trưởng nhóm | Không phân công phần (thành viên tự nhận mục ở U14) |
| AiService, CodeRunService (U13) | Kiểm trần/credit, gọi Gemini, kiểm đầu ra; chạy Judge0 | Không phát hành đề, không chốt điểm; không chạy mã ngoài sandbox |
| GroupDocumentService (U14) | Tài liệu nhóm, khóa mục, Xong → realtime, nộp | Không cho hai người sửa cùng một mục |
| GradingService (U15) | Tự chấm, chấm tay/AI, chốt, công bố, sổ điểm | Không để AI hay công thức quyết định điểm cuối |
| NotificationService, ReportingService (U16) | Thông báo, email có trần, nhắc hạn, tiến độ, dashboard cá nhân và xuất bảng điểm | Không rollback nghiệp vụ khi gửi email lỗi; không công bố điểm nháp hoặc điểm AI đề xuất |

## 3. Orchestration quan trọng

### Bài tài liệu có sơ đồ và AI đề xuất chấm
1. Người học soạn tài liệu (U09 `DocumentEditor`), có thể nhập DOCX vào lượt DOCUMENT sau khi xem trước, vẽ sơ đồ trong iframe Draw.io; U11 tự lưu và kiểm tài liệu qua U09, không cho DOCX sửa khung giảng viên.
2. Nộp: U11 khóa nội dung, phát `u11.submission.submitted`.
3. U15 tạo điểm `PENDING`; giảng viên chọn chấm tay hoặc "Nhờ AI đề xuất".
4. U13 kiểm trần và credit, lấy văn bản phẳng + XML rút gọn (U09), gọi Gemini, kiểm đầu ra, trả đề xuất.
5. Giảng viên dùng/sửa đề xuất, chốt, công bố (U15); U16 báo người học.

### Bài nhóm
1. U12 tạo bộ nhóm cho bài nhóm, đúng một trưởng nhóm.
2. Bài mở: U14 dựng tài liệu nhóm từ khung (mục việc).
3. Thành viên nhận mục, làm ở trang riêng, bấm Xong → ghép realtime (SSE qua `platform.realtime`).
4. Trưởng nhóm nộp (hoặc tự nộp khi hết hạn); U15 chấm tay tài liệu chung, chấm phần đóng góp từng thành viên (tay/AI), nhập điểm cuối từng người.

### Tạo và phát hành bài
1. Giảng viên tạo bài trong lớp mình dạy (U08), lấy câu từ ngân hàng (U06) hoặc câu riêng, hoặc AI đề xuất (U13, dùng RAG U05).
2. Cấu hình loại bài (U09); duyệt; phát hành cho một lớp với lịch, nộp trễ, số lượt, hoặc dạng thi thử (U10).
3. Phát hành khóa nội dung; muốn đổi thì ngưng giao/đợi đóng rồi tạo version mới.

### Nạp nguồn RAG
1. Giảng viên/Chủ nhiệm môn thêm file hoặc YouTube vào bài (U05).
2. Worker trích chữ (không OCR) hoặc lấy caption có sẵn (không tự phiên âm), cắt đoạn; kiểm trần hệ thống và giữ credit của người tạo nguồn học liệu qua U07, rồi gọi Gemini embedding, ghi pgvector và quyết toán credit. Hết trần hệ thống báo "Hệ thống đang bận"; không trừ credit cho lô bị từ chối.

### Thanh toán mua credit AI
1. U07 tạo giao dịch PayOS với idempotency key; trang quay về chỉ hiển thị.
2. Webhook có chữ ký hoặc đối soát → `PAID` và cộng credit đúng một lần.

## 4. Job policies

| Job | Retry | Idempotency |
|---|---|---|
| OTP email (U01), email thông báo (U16) | Backoff U02, tối đa 5 lần | Theo job / outbox ID; email thông báo có trần 300/ngày |
| Dọn file Drive còn sót khi upload lỗi (U03) | Backoff U02 | Theo `providerFileId` |
| Ingest RAG, giải playlist (U05) | Lỗi tạm retry; lỗi vĩnh viễn/`BUSY` không | Theo `contentKey` |
| Đối soát PayOS, trả phần giữ quá hạn (U07) | Theo lịch | Theo `orderCode` / reservation |
| Mở/đóng bài (U08) | Chạy lại bỏ qua nếu lịch đổi | Theo `expectedAt` |
| Tự nộp (U11, U14) | Chạy lại không nộp trùng | Theo lượt / tài liệu nhóm |
| AI, chạy code (U13) | Lỗi tạm tối đa 3 lần; lỗi code không retry | Theo proposal / run ID |
| Nhắc hạn (U16) | Bỏ qua nếu hạn đổi | Theo publication |

## 5. REST contract

- Base path `/api/v1`; OpenAPI mỗi unit trong `/contracts/openapi/`.
- Command quan trọng dùng `Idempotency-Key` hoặc `version` (khóa lạc quan).
- Lỗi dạng problem-details an toàn, có correlation ID, không lộ stack trace.
- Job dài trả `202` + job ID; trạng thái qua `GET /api/v1/jobs/{id}`.
- Realtime bằng SSE cho tài liệu nhóm (U14) và thông báo (U16).
