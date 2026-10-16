# AI-Powered-Learning-Platform

Nền tảng học tập cho một trường hoặc trung tâm đào tạo với bốn vai trò: Student, Teacher, Subject Manager và Administrator. Phạm vi gồm 66 use case chia cho 15 unit: tài khoản và phân quyền, môn, lớp, ghi danh; Assignment cá nhân và nhóm (Draw.io tùy chọn) ở chế độ `GRADED` hoặc `PRACTICE`; Quiz trắc nghiệm luyện tập; chấm điểm có AI đề xuất; học liệu tệp và link YouTube; thông báo trong app và email. AI dùng API chung theo quota giảng viên, API cá nhân hoặc agent riêng qua MCP; không bán credit.

Dự án đang ở giai đoạn sinh code theo quy trình AI-DLC. Hiện có **khung code**: dự án chạy được, mỗi unit có package riêng và các interface xuyên unit, chưa có nghiệp vụ.

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

- Mỗi package của unit chia tầng `contract` (interface và kiểu dữ liệu cho unit khác), `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep`. Unit khác chỉ được import `contract`.
- Interface đặt tên dạng `Ixxx` (ví dụ `IAuthorization`, `IJobHandler`, `IAccountRepository`), không dùng đuôi `Port`. Class, record, enum đặt tên bình thường.
- Unit trước không phụ thuộc unit sau. Khi unit trước cần dữ liệu của unit sau, unit trước khai báo interface trong `contract` của mình và unit sau cài (ví dụ `ISubjectClasses` do U04 khai báo, U05 cài). Chưa có unit cài thì unit trước trả rỗng hoặc từ chối.
- Mỗi interface một class cài có bản tạm trong `contract/stub/` (ví dụ `IdentityContractStubs`): trả dữ liệu mẫu, danh sách rỗng, kiểm quyền trả `true`, ghi/gửi chỉ ghi log. Bản tạm chỉ bật khi chưa unit nào cài thật, lúc khởi động ghi cảnh báo; tắt hẳn bằng `platform.contract-stubs.enabled=false`. Interface nhiều unit cùng cài (`IJobHandler`, listener…) không cần bản tạm vì nơi gọi nhận danh sách.
- `backend/src/test/java/edu/aiplatform/ArchitectureTest.java` kiểm ba quy ước trên.
- Frontend làm tương tự bằng slot: unit trước khai báo slot trong `src/features/<package>/slots.ts`, unit sau đăng ký component vào slot và thêm import file đăng ký của mình vào `src/features/register.ts`.
- Migration Flyway đặt trong `backend/src/main/resources/db/migration/<thư mục của unit>/` (danh sách trong `README.md` ở thư mục đó).
- Người code viết unit test; integration test do tester viết.

## Chạy local

```bash
cp infra/.env.example infra/.env.local
docker compose -f infra/docker-compose.yml -f infra/docker-compose.local.yml --env-file infra/.env.local up --build
```

- Ứng dụng ở <http://localhost:8088> (Nginx), backend trực tiếp ở cổng 8080 (`/health`), Mailpit ở <http://localhost:8025>.
- Không điền `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY` thì tệp lưu ở volume `files-local`, backup database ở `backups-local`. Production đặt `U03_REQUIRE_DRIVE=true`: thiếu Drive thì upload, tải và backup báo tạm thời không khả dụng.
- Backup cần `U03_BACKUP_PUBLIC_KEY` và `BACKUP_DB_PASSWORD`; tạo khóa và khôi phục theo [runbook](docs/runbooks/restore-isolated.md).
- Test: `mvn test` trong `backend/`; `npm run lint`, `npm run typecheck`, `npm test` trong `frontend/`.
