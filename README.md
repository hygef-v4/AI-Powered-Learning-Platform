# AI-Powered-Learning-Platform

Nền tảng học tập cho một trường hoặc trung tâm đào tạo với bốn vai trò: Student, Teacher, Subject Manager và Administrator. Phạm vi gồm 66 use case chia cho 15 unit: tài khoản và phân quyền, môn, lớp, ghi danh; Assignment cá nhân ở chế độ `GRADED` hoặc `PRACTICE`, Assignment nhóm chỉ `GRADED` (Draw.io tùy chọn); Quiz trắc nghiệm luyện tập; chấm điểm có AI đề xuất; học liệu tệp và link YouTube; thông báo trong app và email. AI dùng API chung theo quota giảng viên, API cá nhân hoặc agent riêng qua MCP; không bán credit.

Dự án đã có implementation của 15 unit theo quy trình AI-DLC. Backend và frontend chia theo nghiệp vụ, có test tự động và hạ tầng chạy local. Bằng chứng kiểm thử và các phần nghiệm thu runtime/provider còn lại được ghi trong [AI-DLC state](aidlc-docs/aidlc-state.md).

## Cấu trúc

| Thư mục | Nội dung |
|---|---|
| `backend/` | Java 17, Spring Boot 3.5, modular monolith; một image chạy `backend` và `worker` (profile `worker`) |
| `frontend/` | Next.js 16, React 19, TypeScript, Tailwind, Vitest |
| `infra/` | Docker Compose (Nginx, PostgreSQL 16, Redis, RabbitMQ, Mailpit khi chạy local), cấu hình Nginx, script role PostgreSQL |
| `aidlc-docs/` | Tài liệu AI-DLC: [requirements](aidlc-docs/inception/requirements/requirements.md), [stories](aidlc-docs/inception/user-stories/stories.md), [unit of work](aidlc-docs/inception/application-design/unit-of-work.md), [thiết kế từng unit](aidlc-docs/construction/README.md), [tổng hợp 15 unit](aidlc-docs/construction/unit-summary.md) |
| `docs/` | [Bảng use case](docs/usecase_table.md), sơ đồ Draw.io |

## Unit và package

| Unit | Package backend (`edu.aiplatform.*`) | Thư mục frontend (`src/features/*`) |
|---|---|---|
| U01 Account & Access | `identity` | `identity` |
| U02 Audit | `audit` | `audit` |
| U03 Job and Settings | `files`, `jobs`, `settings`, `backup` | `files`, `settings` |
| U04 Subject Administration | `subjects` | `subjects` |
| U05 Class & Enrollment | `academics` | `academics` |
| U06 AI Connections | `ai` | `ai` |
| U07 Assignment Authoring | `assignments` | `assignments` |
| U08 Assignment Work | `assignmentwork` | `assignmentwork` |
| U09 Assignment Grading | `grading` | `grading` |
| U10 Quiz Authoring | `quizzes` | `quizzes` |
| U11 Quiz Practice | `quizpractice` | `quizpractice` |
| U12 Group Assignment | `groups`, `groupdocs` | `groups`, `groupdocs` |
| U13 Reference Materials | `materials` | `materials` |
| U14 Announcements & Notifications | `announcements`, `notifications` | `announcements`, `notifications` |
| U15 Admin Dashboard | `admindashboard` | `admindashboard` |

## Quy ước

- Trong mỗi package nghiệp vụ, `controller` nhận HTTP, `service` xử lý use case, `repository` chứa SQL/JPA và persistence mapping, `model` chứa đối tượng/quy tắc nghiệp vụ, `dto` chứa kiểu request/response riêng. Cấu hình và adapter ngoài nằm ở `config`/`integration` khi cần. Unit khác chỉ import public `contract`; không thêm tầng rỗng hay interface/Impl cho mọi service.
- Interface đặt tên dạng `Ixxx` (ví dụ `IAuthorization`, `IJobHandler`, `IAccountRepository`), không dùng đuôi `Port`. Class, record, enum đặt tên bình thường.
- Unit gọi thẳng `contract` của unit khác, không theo thứ tự số unit (ví dụ U01 dùng `IAudit` của U02, `IJob` của U03). Khi unit cần dữ liệu mà unit sở hữu chưa có thì có thể khai báo interface trong `contract` của mình cho unit kia cài (ví dụ `ISubjectClasses` do U04 khai báo, U05 cài).
- Các fallback còn giữ trong `contract/stub/` chỉ bật khi thiếu implementation thật và ghi cảnh báo; tắt bằng `platform.contract-stubs.enabled=false`. Không xem dữ liệu mẫu hoặc quyền giả của fallback là bằng chứng nghiệp vụ đã chạy đúng.
- `backend/src/test/java/edu/aiplatform/ArchitectureTest.java` giữ các kiểm tra hiện có; tên package tầng được cập nhật theo vị trí mới.
- Trong `frontend/src/features/<nghiệp vụ>`, `api` gọi backend, `model` chứa types/validation/format, `pages` ghép màn hình, `components` chứa UI và `hooks` quản lý state/vòng đời. CSS riêng của feature nằm trong `styles/`. Test và fixture riêng cho kiểm thử nằm trong `frontend/tests/`, giữ cây tương ứng với `src/`; setup ở `tests/setup.ts`. Route Next.js vẫn ở `src/app`; giữ subgroup như `identity/otp` hoặc `ai/admin` khi hữu ích.
- Các slot được đăng ký ở `register.ts` và nạp tập trung qua `src/features/register.ts`. HTTP client/CSRF/refresh dùng chung ở `shared/api`, UI dùng chung ở `shared/ui`; CSS dùng chung/toàn ứng dụng ở `shared/styles`, được import từ layout hoặc component cần dùng. Import của caller và test được cập nhật theo vị trí file mới.
- Migration Flyway đặt trong `backend/src/main/resources/db/migration/<thư mục của unit>/` (danh sách trong `README.md` ở thư mục đó).
- Người code viết unit test; integration test do tester viết.

Đợt sắp xếp này chỉ chuyển file và cập nhật package/import/tham chiếu; giữ nguyên nội dung xử lý và các file gốc. [Kết quả kiểm tra](aidlc-docs/construction/build-and-test/2026-10-30-package-moves-only-results.md).

## Chạy local

```bash
cp infra/.env.example infra/.env.local
docker compose -f infra/docker-compose.yml -f infra/docker-compose.local.yml --env-file infra/.env.local up --build
```

- Ứng dụng ở <http://localhost:8088> (Nginx), backend trực tiếp ở cổng 8080 (`/health`), Mailpit ở <http://localhost:8025>.
- Không điền `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY` thì tệp lưu ở volume `files-local`, backup database ở `backups-local`. Production đặt `U03_REQUIRE_DRIVE=true`: thiếu Drive thì upload, tải và backup báo tạm thời không khả dụng.
- Backup cần `U03_BACKUP_PUBLIC_KEY` và `BACKUP_DB_PASSWORD`; tạo khóa và khôi phục theo [runbook](docs/runbooks/restore-isolated.md).
- Test: `mvn test` trong `backend/`; `npm run lint`, `npm run typecheck`, `npm test` trong `frontend/`.
