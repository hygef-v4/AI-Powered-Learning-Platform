# Đối chiếu Report 4 với implementation hiện tại

Tài liệu: [Report 4 Software Design Specification](https://docs.google.com/document/d/1odPk65Ly9I3DHgz9gWH4rC_WjQHufI70/edit). Đã đọc toàn bộ nội dung trích xuất, 34 bảng và xem 16 hình thiết kế nhúng (không tính logo bìa). Không sửa file Report 4 trên Drive.

Kết luận: phần kiến trúc tổng quan đúng hướng modular monolith, nhưng database/package design còn từ mô hình cũ; một số sequence mô tả sai luồng hiện tại. Cần đồng bộ cả bảng và hình, không chỉ thay đoạn giới thiệu.

## D01 — Database Design và ERD còn các bảng ngoài phạm vi

**Vị trí:** I.3, ERD và các mục 3.6–3.24. Có `lessons`, `announcement_comments`, `credit_packages`, `payments`, `ai_services`, `ai_suggestions`, `assignment_questions`, `student_groups`, `group_members`, `attempts`, `rubrics` theo thiết kế cũ.

**Đối chiếu:** [migration inventory](../../../backend/src/main/resources/db/migration/README.md) và các migration thực tế. Implementation dùng `materials`/`material_versions`/`material_modules`/`material_module_quizzes`, `class_groups`/`class_group_members`, `assignment_attempts`, `assignment_rubrics`, `ai_connections`/`ai_tasks`/`ai_usage_records` và các bảng quota. Quiz có schema riêng: `quizzes`, `quiz_items`, `quiz_attempts`, quy tắc sinh đề và đề xuất câu. Còn thiếu các bảng hiện hành như `grading_proposals`, `grade_distribution_visibility`, `group_peer_reviews`, credential MCP, settings và backup runs trong mô tả tương ứng.

**Cần sửa:** dựng lại inventory từ migration, phân biệt bảng thực với lớp khái niệm. Basic module là hiện hành nhưng tên bảng là `material_modules`; không xóa module chỉ vì `modules` cũ sai. Không mang payment/credit/Code Lab/comment trở lại scope.

## D02 — Cột và constraint của cả bảng còn tồn tại cũng lệch

**Ví dụ xác nhận:**

- `accounts`: doc có `school_email VARCHAR(320)`, avatar, credential_version và credit balances; [migration](../../../backend/src/main/resources/db/migration/identity/V20260925_0930__create_accounts.sql) dùng email 254, không có các cột kể trên, `version BIGINT`.
- `assignments`: doc dùng `type/config`; [migration](../../../backend/src/main/resources/db/migration/assignments/V20260925_1300__create_assignment_tables.sql) có `scope`, `origin`, `version_no`, `submission_mode`, `skeleton`, `parts`, monitoring và rubric riêng theo part.
- `evaluations`: [schema](../../../backend/src/main/resources/db/migration/grading/V20260925_1500__create_grading_tables.sql) có `grading_document`, submission hash, class/assignment, trạng thái SUPERSEDED và proposal riêng; không theo bộ cột ai_score/ai_feedback/history của bảng trong doc.
- ERD ghi OPEN yêu cầu teacher; [migration](../../../backend/src/main/resources/db/migration/classes/V20261027_1100__allow_open_class_without_teacher.sql) đã bỏ constraint đó.
- Đổi trưởng nhóm dùng `requested_by`/`proposed_leader_id`, người đề xuất thay thế có thể null. Lý do optional ở input nhưng database vẫn có thể lưu chuỗi rỗng vào cột NOT NULL; không tự coi NOT NULL của reason là lỗi.

**Cần sửa:** kiểu, nullability, PK/FK/index/check theo trạng thái cuối sau tất cả migration, không chỉ migration đầu tiên.

## D03 — Package diagram và mô tả package dùng cấu trúc cũ

**Vị trí:** I.2 và hình package. Có Judge0/billing/templates/questionbank/aiexecution/reporting, rubric bank ở subject/class và code execution.

**Đối chiếu:** [backend package root](../../../backend/src/main/java/edu/aiplatform) và [frontend source](../../../frontend/src). Hiện có `ai`, `assignments`, `assignmentwork`, `quizzes`, `quizpractice`, `materials`, `announcements`, `notifications`, `admindashboard`, `backup`, `settings` cùng các package khác; frontend theo `app/features/shared`. Mười lăm logical units không đồng nghĩa mười lăm thư mục Java.

**Cần sửa:** ánh xạ package thực và logical unit, bỏ hạ tầng/feature đã loại. Diagram không được giữ Judge0 trong khi overview chỉ mô tả Quiz/Assignment.

## D04 — Durable outbox vừa được khẳng định vừa bị phủ định

**Vị trí:** I.1 đoạn transaction, bảng Repository/PostgreSQL/RabbitMQ khẳng định durable outbox; IV.3 lại ghi không cần bảng job/outbox.

**Đối chiếu:** [JobPublisher](../../../backend/src/main/java/edu/aiplatform/jobs/service/JobPublisher.java), [EventPublisher](../../../backend/src/main/java/edu/aiplatform/jobs/service/EventPublisher.java), [AfterCommitExecutor](../../../backend/src/main/java/edu/aiplatform/jobs/service/AfterCommitExecutor.java). Publisher gửi sau commit, không ghi bảng outbox của U03; owner giữ pending state và sweeper khôi phục các loại công việc hỗ trợ recovery. Event gửi lỗi có thể bị mất và chỉ ghi log.

**Cần sửa:** thống nhất after-commit + owner pending/sweeper, không hứa atomic durable outbox hoặc bảo đảm mọi event đều được khôi phục.

## D05 — AI source, key và retry/fallback còn theo quy tắc cũ

**Vị trí:** I.1 deployment paragraph; IV.2 AI/MCP và AI result handling. Doc yêu cầu explicit source, không tự chuyển personal khi hết quota và không retry provider sau dispatch.

**Đối chiếu:** [AiTaskService](../../../backend/src/main/java/edu/aiplatform/ai/service/AiTaskService.java), [AiTaskJobHandler](../../../backend/src/main/java/edu/aiplatform/ai/service/AiTaskJobHandler.java), [ConnectionOrder](../../../backend/src/main/java/edu/aiplatform/ai/service/ConnectionOrder.java), [AiConnectionService](../../../backend/src/main/java/edu/aiplatform/ai/service/AiConnectionService.java). Omitted source hỗ trợ automatic shared rồi personal; nhiều key/provider, thứ tự connection và bounded retry 429/503/529. Sau quyết toán REJECTED/CHARGED/UNKNOWN có thể xét key tiếp theo. UNKNOWN không tự hoàn quota. Personal fallback do chủ key chịu chi phí. Key/token reveal phải có guard, không đồng nghĩa thêm secret-read MCP tool.

## D06 — Sequence AI đang mô tả gọi provider đồng bộ

**Vị trí:** II.3.2 Practice Feedback và II.3.3 Direct API. Hình cho AI Task Service gọi provider rồi trả kết quả qua cùng chuỗi request UI.

**Đối chiếu:** [PracticeFeedbackService](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/PracticeFeedbackService.java), [GradingProposalService](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingProposalService.java), [AiTaskJobHandler](../../../backend/src/main/java/edu/aiplatform/ai/service/AiTaskJobHandler.java). Request tạo pending record/task, worker gọi provider, result handler áp dụng sau; cần vẽ response/task status và ranh giới bất đồng bộ. Không đổi luồng external MCP thành ALAP gọi model: phần agent tự gọi model trong II.3.4 vẫn đúng nguyên tắc.

## D07 — Luồng chấm thủ công bỏ qua Finalize; correction API không khớp

**Vị trí:** II.3.1 class diagram có `GradeWriter.amend`; II.3.5 sequence đi từ save manual tới Publish, không có Finalize.

**Đối chiếu:** [Evaluation](../../../backend/src/main/java/edu/aiplatform/grading/model/Evaluation.java) chỉ publish FINALIZED, khóa FINALIZED/PUBLISHED bằng GRADE_LOCKED; [GradingService](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingService.java) thực hiện các action hiện tại. Không có lớp GradeWriter tương ứng trong source. [GradingCommands](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingCommands.java) có partial item scores, structured feedback và review acknowledgement.

**Cần sửa:** Save draft → Finalize → Publish, guard/state/version và human review; tránh mô tả amend như đường sửa điểm đã chốt. Tách mức trừ Quiz khỏi khóa điểm Assignment.

## D08 — Thiết kế nhóm thiếu các bước cộng tác hiện hành

**Vị trí:** II.2.1–2.3 và I.3 group tables. Hình còn GroupDocumentController/Service tổng quát, chưa thể hiện phân công/chốt phân công, import, restore revision, peer review và schema tách section/revision.

**Đối chiếu:** [GroupDocController](../../../backend/src/main/java/edu/aiplatform/groupdocs/controller/GroupDocController.java), [GroupDocCollaborationController](../../../backend/src/main/java/edu/aiplatform/groupdocs/controller/GroupDocCollaborationController.java), [GroupSectionService](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupSectionService.java), [GroupPeerReviewService](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupPeerReviewService.java). Chốt/mở phân công là quyền leader; release bỏ draft khác với unlock phân công giữ draft. Holder nhập từng mục, leader nhập toàn tài liệu. Peer review mở sau submission và đóng 7 ngày sau effective deadline, không tự cộng vào điểm chính thức.

**Cần sửa:** cập nhật class responsibilities, state/role guard và sequence chính; thể hiện pending evaluations cùng transaction submission qua port, không chỉ UPDATE bản nộp.

## D09 — Email chỉ có SMTP

**Vị trí:** architecture figure, component Provider Adapters/Brevo và auth class diagram chỉ trỏ SmtpMailAdapter.

**Đối chiếu:** [SmtpMailAdapter](../../../backend/src/main/java/edu/aiplatform/identity/integration/SmtpMailAdapter.java), [BrevoApiMailAdapter](../../../backend/src/main/java/edu/aiplatform/identity/integration/BrevoApiMailAdapter.java), [application.yml](../../../backend/src/main/resources/application.yml). SMTP mặc định, Mailpit local/test, `brevo-api` HTTPS với BREVO_API_KEY khi SMTP bị chặn. Hình nên phụ thuộc mail port và hai adapter thay vì cố định SMTP.

## D10 — Backup component hứa lưu cả file riêng và manifest

**Vị trí:** I.1 bảng Drive Backup: “independently recoverable files and a manifest”. IV.4 chỉ ghi PostgreSQL, nên còn mâu thuẫn nội bộ.

**Đối chiếu:** [BackupJobHandler](../../../backend/src/main/java/edu/aiplatform/backup/service/BackupJobHandler.java). Job dump PostgreSQL, mã hóa archive, upload và kiểm size/checksum; không backup file học liệu trên Drive theo mô tả kia. Restore verification riêng trong môi trường cô lập; private key giữ ngoài VPS.

## Các điểm nên làm rõ, không gọi là lỗi chắc chắn

- Hình repository chỉ ghi JPA, trong khi nhiều repository thực dùng NamedParameterJdbcTemplate; dự án vẫn có dependency JPA. Nên mô tả đủ JDBC/JPA theo thành phần, không kết luận toàn bộ hệ thống không dùng JPA.
- IV.4 Docker Compose còn quá chung so với [runbook](../../../docs/runbooks/deploy-vps.md): CI build/push SHA images, VPS pull main đã qua CI, health gate và rollback. Cần bổ sung khi đồng bộ thiết kế triển khai; rollback image không đảm bảo đảo ngược migration.
- Authentication activate/login/logout/reset giữ đúng các ý chính session Redis, activation tự login và reset thu hồi session. Tên lớp/method chi tiết trong hình vẫn cần mapping nếu đây là code design vật lý, nhưng không kết luận activation tạo session là sai.
- NFR latency/load là mục tiêu, không biến thành số đo hay khẳng định đã pass. Không xác nhận môi trường VPS/provider đang hoạt động từ source review.

## Phạm vi và thứ tự xử lý

Ưu tiên I.2 package + I.3 database/ERD, rồi I.1/IV.3 outbox, AI/grading sequences, collaboration, transport và backup. Giữ template và change log lịch sử khi sửa; phải sửa đồng thời hình và mô tả nếu muốn tuyên bố Report 4 đồng bộ đầy đủ.

Review không thay đổi Report 4, code hay cấu hình; không chạy test/build hoặc kiểm live deployment. Đã xem các hình nhúng độc lập; chưa kiểm bố cục phân trang Word/PDF.
