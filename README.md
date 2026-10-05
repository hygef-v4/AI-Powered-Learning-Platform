# AI-Powered-Learning-Platform

Nền tảng học tập cho một trường hoặc trung tâm đào tạo với bốn vai trò: Student, Teacher, Subject Manager và Administrator. Hệ thống quản lý môn/lớp, học liệu có RAG (kể cả nguồn YouTube), năm dạng bài (Text Essay, Multiple-Choice Quiz, Diagram Essay, Code Lab, Group Assignment) ở chế độ `GRADED` hoặc `PRACTICE`, chấm điểm có AI hỗ trợ và credit AI thanh toán qua PayOS.

Dự án đang ở giai đoạn sinh code theo quy trình AI-DLC. Kiến trúc: backend modular monolith Java 17 / Spring Boot 3.5 (`backend/`), frontend Next.js 16 + TypeScript (`frontend/`), PostgreSQL, Redis, RabbitMQ, Google Drive, Gemini và Judge0 chạy bằng Docker Compose (`infra/`). Hợp đồng giữa các unit và với frontend nằm ở `contracts/`.

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [docs/](docs/README.md) | Bảng 40 use case, đặc tả use case chính, ERD, screen flow, business flow, context diagram |
| [Requirements](aidlc-docs/inception/requirements/requirements.md) | Yêu cầu chức năng và phi chức năng |
| [User stories](aidlc-docs/inception/user-stories/stories.md) | 49 story MVP và ma trận story ↔ use case |
| [Application design](aidlc-docs/inception/application-design/unit-of-work.md) | 16 unit, phụ thuộc và story map |
| [Construction](aidlc-docs/construction/) | Thiết kế chức năng, NFR, hạ tầng và code plan từng unit |
| [Trạng thái AI-DLC](aidlc-docs/aidlc-state.md) | Tiến độ các stage và việc còn mở |
| [contracts/](contracts/) | OpenAPI từng unit, JSON schema message/event và mô hình tài liệu |

## Chạy local

```bash
cp infra/.env.example infra/.env.local
docker compose -f infra/docker-compose.yml -f infra/docker-compose.local.yml --env-file infra/.env.local up --build
```

- Ứng dụng: <http://localhost:8088> (Nginx), backend trực tiếp ở cổng 8080, Mailpit ở <http://localhost:8025>.
- Không điền `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY` thì tệp được lưu vào volume `files-local` (overlay local đặt `U03_REQUIRE_DRIVE=false`). Production đặt `U03_REQUIRE_DRIVE=true`: thiếu key thì upload/tải báo tạm thời không khả dụng, không lưu tệp vào container.
- Test: `mvn test` trong `backend/`, `npm test` trong `frontend/`. Unit chỉ viết unit test; integration test do tester riêng viết.

## Việc nền và worker (U03)

Container `worker` dùng cùng image với `backend`, chạy với `SPRING_PROFILES_ACTIVE=worker`, chỉ nằm trong mạng `internal`. Worker nghe 7 queue việc nền, thử lại lỗi tạm qua các queue `jobs.retry.*` (30 s, 1, 2, 4, 8 phút), mỗi phút chạy sweeper (gửi lại việc bị mất) và scanner (việc hẹn giờ).

Xem RabbitMQ trên VPS (management UI không public), mở SSH tunnel rồi vào <http://localhost:15672> bằng user `app`:

```bash
ssh -L 15672:localhost:15672 deploy@<vps>
```

Trên VPS cần cho container `rabbitmq` publish cổng 15672 ra `127.0.0.1` của máy chủ (hoặc dùng `docker compose exec rabbitmq rabbitmqctl ...`).

### Thêm một loại việc nền

1. Thêm hằng vào `jobs/port/JobTypes` và một dòng vào `jobs/domain/JobRouting`, chọn **một trong 7 queue có sẵn** theo BR-U03-63 (`jobs.triggered`, `jobs.email`, `jobs.gemini`, `jobs.youtube`, `jobs.code`, `jobs.drive`, `jobs.payos`). Không tạo queue mới.
2. Viết bean `@Component` cài `JobHandler`: `jobType()`, `queue()` khớp bảng định tuyến, `handle()` idempotent (dòng nghiệp vụ không còn ở trạng thái chờ thì bỏ qua), ném `RetryableJobException` khi lỗi tạm, `onFailed()` chuyển dòng nghiệp vụ sang trạng thái lỗi. Bean tự đăng ký với `JobHandlerRegistry`.
3. Trong transaction nghiệp vụ: ghi dòng ở trạng thái chờ rồi gọi `JobPort.enqueue(jobType, payload, idempotencyKey)`. Payload chỉ chứa ID; khóa nhạy cảm (`password`, `otp`, `token`, `secret`, `phone`) bị chặn. Message gửi sau commit, rollback thì không gửi.
4. Cài `PendingSweeper` (bean) trả các dòng còn chờ quá 5 phút để worker gửi lại.

### Việc hẹn giờ

Cài bean `ScheduledScanner`: `scan(now)` đọc mốc thời gian trên bảng của unit và cập nhật có điều kiện (ví dụ `UPDATE ... WHERE status = 'SCHEDULED' AND opens_at <= now`) để chạy lại không gây trùng. Worker gọi mỗi phút.

### Sự kiện thông báo

`EventPublisherPort.publish(event)` gửi lên exchange `platform.events` sau commit, chỉ cho thông báo U16; mất event được chấp nhận. Phản ứng nghiệp vụ bắt buộc giữa các unit đi qua port, không qua event.

## Lưu trữ tệp trên Google Drive (U03)

1. Google Cloud Console: tạo project, bật **Google Drive API**, tạo **service account** và tải **JSON key**.
2. Google Drive của trường: tạo **Shared Drive**, thêm email của service account với quyền **Content manager**. Lấy ID Shared Drive từ URL.
3. Mã hóa key thành một dòng base64 và đặt vào `.env` (không commit):

   ```bash
   base64 -w0 service-account.json
   ```

   `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY=<chuỗi base64>`, `GOOGLE_SHARED_DRIVE_ID=<id>`, `U03_FILEREF_SECRET=<chuỗi ngẫu nhiên ≥ 32 byte>`.
4. Backend tự tạo thư mục `AVATAR`, `MATERIAL`, `DOCUMENT_IMAGE` trong Shared Drive. Drive lỗi thì `/health` báo `DEGRADED` nhưng backend vẫn chạy; upload/tải báo "tạm thời không khả dụng".

### Unit khác dùng tệp như thế nào

- Frontend upload qua `POST /api/v1/files` (component `FileUploader`), nhận `fileRef` (hạn 1 giờ).
- Unit sở hữu gọi `ArtifactPort.attach(fileRef, actor, purpose)` trong transaction nghiệp vụ, lưu `fileId` trả về vào bảng của mình. `fileId` không bao giờ trả cho frontend.
- Khi người dùng muốn xem tệp: API của unit sở hữu **tự kiểm quyền**, rồi gọi `ArtifactPort.issueDownloadToken(fileId, accountId)` và trả `url` (token 5 phút, gắn với đúng tài khoản). Frontend dùng `FileLink` để lấy URL khi bấm.
- Worker đọc tệp bằng `ArtifactPort.open(fileId)`. U01 kiểm ảnh đại diện bằng `AvatarPort.validateAvatar`. U03 không xóa tệp đã upload thành công.
