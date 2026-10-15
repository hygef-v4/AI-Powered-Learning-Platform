# Unit of Work - 15 unit logic

## 1. Phạm vi và quy tắc

Đây là 15 unit lập kế hoạch, được đối chiếu với **73 use case và 51 story** trong phạm vi MVP (`docs/use-cases-73.md`). Phần Learning Access trước đây đứng riêng đã được gộp vào U04 vì chỉ còn một story và không sở hữu bảng nào. Chúng là module logic trong một backend Spring Boot, không phải 15 service triển khai độc lập. Frontend Next.js và worker process dùng contract có version. Mỗi story có một primary unit; hai catalog hiện hành chỉ chứa phạm vi MVP.

- Mỗi unit sở hữu dữ liệu và quy tắc nghiệp vụ của mình; unit khác gọi public contract, không đọc bảng/repository trực tiếp. Hai bảng dùng chung (`accounts`, `assignments`) cho unit khác thêm cột của mình và ghi qua port của unit chủ (xem `component-dependency.md` mục 3a). Mô hình dữ liệu chi tiết nằm trong [thiết kế từng unit ở Construction](../../construction/); đã đồng bộ với SRS hiện hành ngày 2026-10-09; code/contracts còn theo baseline cũ. Tài liệu nhóm nằm ở bảng `group_documents` của U14.
- U01 kiểm quyền actor/object; U02 giữ audit; U03 giữ file/artifact, việc nền, worker và sự kiện thông báo (chuyển từ U02, 2026-10-04). U03 và U02 code trước, song song, qua authorization contract có version; U01 code sau. Tách unit không thay đổi một backend deployable.
- Bài đã phát hành bị khóa nội dung; muốn sửa thì ngưng giao hoặc đợi đóng rồi tạo version mới cùng stable key. Attempt giữ assignment/question/rubric snapshot cũ.
- AI tạo draft hoặc grade proposal cho bài `GRADED`; Teacher duyệt đề và quyết định điểm cuối. Với bài Text Essay/Diagram Essay `PRACTICE`, AI có thể trả kết quả luyện tập trực tiếp cho Student khi Student bấm chấm trên attempt đã nộp và đủ credit. RAG là nguồn hỗ trợ tùy chọn cho tạo đề.
- Không sao chép khóa học/lớp. Copy assignment/rubric theo phạm vi đã duyệt tạo identity độc lập có lineage.
- Không có learning path, trạng thái hoàn thành hay tiến độ từng bài học. Tiến độ nộp bài và trạng thái job vẫn có.
- Phần Learning Access của U04 cần nội dung của U05, trong khi U05 lại cần class scope của U04. Quan hệ này được đảo ngược qua port trung lập đặt ở tầng contract dùng chung: U04 phụ thuộc interface, U05 cung cấp implementation. Nhờ đó đồ thị unit không có chu trình cứng. Thanh toán (U07) chỉ cộng credit AI, không ảnh hưởng quyền vào lớp nên U04 không phụ thuộc U07.

## 2. Unit boundaries hiện hành

| Unit | Tên | Phạm vi/đóng góp |
|---|---|---|
| U01 | Account & Access | Role/authentication cho mọi unit; counts cho Admin Dashboard. |
| U02 | Audit | Audit append-only trong transaction mọi command nhạy cảm. |
| U03 | File, Job, Event & Settings | Tệp/worker/events dùng chung; lưu Settings và kiểm version/audit; U07/U13 khai báo các mục của mình. |
| U04 | Subject, Class, Enrollment & Learning Access | R2–R5 scope cho tài nguyên môn/lớp; Admin chỉ quản lý môn, danh sách lớp của môn chỉ đọc. |
| U05 | Content, Material & RAG | Tóm tắt/embedding và RAG cho AI; quiz/attempt do U08/U09/U11 giữ. |
| U06 | Rubric & Subject Question Bank | Rubric thuộc bài, tự tạo và khóa khi phát hành; câu ngân hàng SUBJECT và câu riêng ASSIGNMENT. |
| U07 | Payment & AI Credit | Ví của ba role; snapshot payment, verified webhook và grant định kỳ qua Settings. |
| U08 | Assessment Core, Publication & Copy | Vòng đời chung UC 35/42–45, bài của môn, copy/version; U09 cấu hình theo dạng. |
| U09 | Question Type Authoring | Cấu hình quiz và bốn dạng assignment, document model; U08 vòng đời, U06 rubric. |
| U11 | Attempt & Submission | Quiz Practice tách Student Assignments; lần làm bài của môn ghi classId. |
| U12 | Group & Allocation | Chia nhóm/leader hỗ trợ Teacher Class Detail và UC 45/27. |
| U13 | AI & Code Execution | AI draft, Practice grading UC 29, proposals UC 38, Judge0 UC 25/43, số liệu AI UC 58. |
| U14 | Group Document & Submission | Tài liệu nhóm chỉ ở lớp, GRADED; chỉ leader nộp. |
| U15 | Grading | Bài của môn chấm/công bố theo từng lớp; Practice/quiz không vào gradebook. |
| U16 | Reporting & Notification | Progress/distribution và export của UC 40; thông báo bài của môn tới mọi lớp OPEN. |

Primary UC/story theo [unit map](unit-of-work-story-map.md): 73 UC/51 story; U10 đã bỏ, ID khác giữ nguyên. Admin không phân công môn/lớp, không ví/AI; Subject Manager R2 cho môn và R4 chỉ khi được giao dạy lớp. Student vào Class Dashboard/Student Class Detail; không lesson progress.

## 3. Code organization

- `/frontend`: Next.js, mỗi unit một feature folder riêng dưới console tương ứng, dùng chung layout và nav. Khu vực giảng dạy tách thành Class Console, Assignment Console và Grading Console để nhiều unit không sửa chung một cây component.
- `/backend`: một Spring Boot modular monolith; package theo tên nghiệp vụ ngắn của từng unit (ví dụ `identity`, `audit`, `academics`), mỗi package có `api`/`application`/`domain`/`infrastructure` khi cần. Mã U01–U09, U11–U16 dùng để truy vết tài liệu và migration.
- `worker`: container riêng chạy cùng mã nguồn backend với profile `worker` (không có thư mục mã riêng); handler thuộc package của unit nghiệp vụ tương ứng, dùng versioned job contract và chỉ đọc dữ liệu qua service/port của unit sở hữu.
- `/contracts`: OpenAPI tách theo unit (`identity.yaml`, `authoring.yaml`...), event/job schema và compatibility tests; gộp thành một spec lúc build.
- Migration đánh số theo timestamp (`V20260924_1430__`), không dùng số tăng dần, để hai người không trùng số.
- `/infra`: cấu hình database, queue, storage, deployment/observability.
- `/aidlc-docs`: chỉ có tài liệu.

Nhóm 5 người mở tối đa năm unit đang triển khai cùng lúc theo dependency trực tiếp và slot trống. Mỗi unit có owner và reviewer; consumer chỉ tích hợp sau khi provider tương ứng đạt kiểm tra contract/behavior, không cần chờ toàn bộ wave đóng.

## 4. Bốn wave mở việc theo dependency

Wave là nhóm công việc và điểm kiểm tra tích hợp, không phải barrier bắt mọi unit trong một cột xong rồi mới mở cột tiếp theo. Unit được mở ngay khi **tất cả provider `H` của chính nó** đạt contract/behavior cần dùng và có người rảnh; không phải chờ unit không liên quan. Cạnh `C` cho phép phát triển song song với contract có version và fake adapter; cạnh `E` cần event/schema của owner và dữ liệu thật tại integration gate. Nhóm có tối đa năm unit đang được triển khai đồng thời trên toàn bộ các wave, kể cả khi hai wave chồng thời gian.

| Wave | Unit (số lượng) | Nhánh có thể mở song song và điều kiện nối tiếp | Điểm dừng tích hợp |
|---|---|---|---|
| 1 - nền | U03, U02, U01, U04 (4) | U03 (khung dự án, việc nền, tệp) và U02 (audit) chạy song song trước với adapter giả cho kiểm quyền; U01 mở sau khi cả hai xong; U04 sau U01 | Việc nền/worker, audit, identity, file/artifact, Settings version/audit, class/enrollment contract |
| 2 - nguồn và đề lõi | U05, U06, U07, U12, U08 (5) | Sau U04, U05/U06 chạy song song; U07 chỉ cần U01 (và U02, U03 đã có); U12 mở ngay sau U04 vì nhóm thuộc lớp; U08 mở khi U05 và U06 sẵn sàng. Phần Learning Access của U04 hoàn tất tại wave này khi implementation của U05 cắm vào port | Content/summary/credit holds, subject bank versions, rubric khóa, credit AI, nhóm của lớp, Learning access và đề thủ công/publication |
| 3 - biên soạn, thực hiện và chấm | U09, U11, U14, U15 (4) | U09 mở sau U08 và soạn cả năm loại bài (Text Essay, Quiz, Diagram Essay, Code Lab, Group); U11 và U14 sau U09 (U14 cần thêm U12); U15 sau U11/U14. Chạy code và AI của U13 dùng qua `C` với adapter giả | Năm loại bài, attempt/submission, tài liệu nhóm, chấm tay/final grade |
| 4 - mở rộng và kết quả | U13, U16 (2) | U13 sau U05/U06/U07, cắm implementation vào port của U08, U09, U11, U15; U16 sau U15 | AI/Code sandbox, notification và báo cáo |

Không có điều kiện “đóng toàn bộ wave N mới được bắt đầu wave N+1”. Ví dụ U03/U02 có thể bắt đầu cùng lúc; U13 thuộc wave 4 có thể bắt đầu khi U05/U06/U07 sẵn sàng, dù U09 ở wave 3 vẫn đang làm. U02 chỉ phát hành API đọc audit sau khi tích hợp kiểm quyền từ U01. Khi đủ năm người đang giữ unit, unit mới đủ dependency sẽ chờ slot trống. Đường phụ thuộc chi tiết và hình đồ thị phụ thuộc nằm trong `unit-of-work-dependency.md`.

## 5. Integration gates

| Gate | Kiểm tra bắt buộc |
|---|---|
| G1 | U03, U02, U01, U04: việc nền (gửi sau commit, retry, sweeper, scanner), deny-by-default auth, audit append-only, job idempotency/retry, upload checksum, kiểm loại file, abuse-file rejection, token tải file, subject/class scope |
| G2 | U05-U08, U12 và phần Learning Access của U04: nhóm của lớp (một trưởng nhóm, yêu cầu đổi trưởng nhóm), Content/File scope, QuestionVersion bất biến và rubric của bài khóa khi phát hành, verified payment event, Learning access không có lesson progress và đề thủ công/phát hành đúng scope |
| G3 | U09, U11, U14, U15: năm loại bài (XML Draw.io an toàn, cấu hình Code Lab, khung bài nhóm), attempt snapshot, nộp idempotent, tài liệu nhóm (khóa mục, realtime, bản nộp bất biến), chấm tay/final grade |
| G4 | U13, U16: AI/Code sandbox, chấm Practice theo credit, AI proposal không thành final grade và notification theo quyền |

Gate kiểm tra kết quả của từng nhánh khi nhánh đó sẵn sàng; gate tổng của wave dùng để xác nhận đủ phạm vi, không khóa việc mở unit ở wave sau nếu provider trực tiếp đã sẵn sàng.

## 6. Quy tắc security và resiliency xuyên unit

Theo phạm vi rút gọn (`requirements.md` mục 12-13):

- Mọi unit áp dụng deny-by-default, kiểm input, kiểm quyền theo đối tượng, log có cấu trúc không chứa secret và lỗi an toàn (SECURITY-03, 05, 08, 15).
- Secret chỉ nằm trong `.env` trên VPS, không commit (SECURITY-09).
- Gọi dịch vụ ngoài có timeout và retry hữu hạn; không dùng circuit breaker (RESILIENCY-10).
- Job handler idempotent; nguồn dữ liệu và kết quả quan trọng có checksum/version.
- Health check cho backend và worker (RESILIENCY-06); deploy/rollback bằng Docker Compose (RESILIENCY-04).
- Ngoài phạm vi: mã hóa at rest, TLS giữa các container, metrics/alerting, backup, RTO/RPO, multi-zone (xem `construction/shared-infrastructure.md`).

## 7. Definition of Done cho mỗi unit

- Story và acceptance criteria thuộc unit được truy vết tới thiết kế và test.
- Public API/event/job contracts được version hóa và có compatibility test.
- Unit/MockMvc/frontend tests theo plan và security negative tests chạy đạt; integration tests chuyển tester riêng theo quyết định 2026-10-05, không đánh dấu đạt khi chưa chạy.
- Không vi phạm data ownership hoặc dependency direction.
- Migration, observability, error handling và audit events liên quan được xác định.
- Contract/behavior của mọi provider trực tiếp đạt trước khi consumer tích hợp; gate tổng wave không chặn nhánh độc lập.

## 8. Extension compliance tại Units Generation

Bảng dưới đây được lập trước khi rút gọn phạm vi (2026-09-24). Hiện chỉ SECURITY-03, 04, 05, 08, 09, 12, 15 và RESILIENCY-04, 06, 10 còn áp dụng; mọi rule khác là N/A "ngoài phạm vi đồ án" ở mọi stage, kể cả các dòng ghi "thuộc stage sau".

### Security Baseline

| Rule | Trạng thái tại stage | Lý do |
|---|---|---|
| SECURITY-01 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-02 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-03 | Compliant | Structured logging/correlation là Foundation concern cho mọi deployable |
| SECURITY-04 | N/A | HTTP header implementation thuộc NFR Design/Code Generation |
| SECURITY-05 | Compliant | Input/file/XML/job schema validation là boundary bắt buộc |
| SECURITY-06 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-07 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-08 | Compliant | U01 sở hữu deny-by-default, role/scope/object authorization |
| SECURITY-09 | N/A | Runtime hardening thuộc NFR/Infrastructure Design |
| SECURITY-10 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-11 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-12 | Compliant | Identity/session/credential responsibility nằm riêng tại U01 |
| SECURITY-13 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-14 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| SECURITY-15 | Compliant | Fail-closed, safe error và dependency failure isolation là cross-unit rule |

Không có blocking Security finding ở Units Generation.

### Resiliency Baseline

| Rule | Trạng thái tại stage | Lý do |
|---|---|---|
| RESILIENCY-01 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-02 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-03 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-04 | N/A | Deployment/rollback selection thuộc Infrastructure Design |
| RESILIENCY-05 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-06 | Compliant | Health check riêng cho API và worker là boundary requirement |
| RESILIENCY-07 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-08 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-09 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-10 | Compliant | Timeout, retry hữu hạn và worker isolation đã được yêu cầu (không circuit breaker) |
| RESILIENCY-11 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-12 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-13 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-14 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |
| RESILIENCY-15 | N/A | Ngoài phạm vi đồ án (rút gọn 2026-09-24) |

Không có blocking Resiliency finding ở Units Generation.

## 9. Baseline 2026-10-09

73 UC/51 story/15 unit. U03 giữ Settings UC 70–71; U02 Audit UC 73. U08 giữ bài của môn, version/copy, US-ASM-001/008/009/010; U09 cấu hình UC 35/42–45. U10 đã bỏ; ngân hàng chỉ môn, không mã mời hoặc comments; Admin chỉ quản trị. Checkpoints 16 plan ngày 05/10 là phê duyệt baseline trước, không xác nhận revision được code. Các hình dependency cũ có U10 chỉ là historical; ma trận hiện hành là nguồn lập kế hoạch.
