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
| `docs/` | [Bảng use case](docs/usecase_table.md), sơ đồ Draw.io, [hướng dẫn](docs/guides/) (kết nối MCP, khóa AI, người dùng) và [runbook](docs/runbooks/) (deploy, khôi phục, mã hóa khóa AI) |
| `scripts/` | Kiểm tra MCP, [E2E](scripts/e2e/README.md), đánh giá chấm AI |

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

- Code mới và code được refactor trong toàn bộ backend/frontend tuân theo [Quy ước mã nguồn chung](aidlc-docs/construction/code-conventions.md): dependency service có hậu tố `Service`, method hành động bắt đầu bằng động từ + đối tượng, ưu tiên khoảng hai từ; có ngoại lệ cho framework và contract hiện hữu.
- Trong mỗi package nghiệp vụ, `controller` nhận HTTP, `service` xử lý use case, `repository` chứa SQL/JPA và persistence mapping, `model` chứa đối tượng/quy tắc nghiệp vụ, `dto` chứa kiểu request/response riêng. Cấu hình và adapter ngoài nằm ở `config`/`integration` khi cần. Unit khác chỉ import public `contract`; không thêm tầng rỗng hay interface/Impl cho mọi service.
- Interface đặt tên dạng `Ixxx` (ví dụ `IAuthorization`, `IJobHandler`, `IAccountRepository`), không dùng đuôi `Port`. Class, record, enum đặt tên bình thường.
- Unit gọi thẳng `contract` của unit khác, không theo thứ tự số unit (ví dụ U01 dùng `IAudit` của U02, `IJob` của U03). Khi unit cần dữ liệu mà unit sở hữu chưa có thì có thể khai báo interface trong `contract` của mình cho unit kia cài (ví dụ `ISubjectClasses` do U04 khai báo, U05 cài).
- Các fallback còn giữ trong `contract/stub/` chỉ bật khi thiếu implementation thật và ghi cảnh báo; tắt bằng `platform.contract-stubs.enabled=false`. Không xem dữ liệu mẫu hoặc quyền giả của fallback là bằng chứng nghiệp vụ đã chạy đúng.
- `backend/src/test/java/edu/aiplatform/ArchitectureTest.java` giữ các kiểm tra hiện có; tên package tầng được cập nhật theo vị trí mới.
- Trong `frontend/src/features/<nghiệp vụ>`, `api` gọi backend, `model` chứa types/validation/format, `pages` ghép màn hình, `components` chứa UI và `hooks` quản lý state/vòng đời. CSS riêng của feature nằm trong `styles/`. Test và fixture riêng cho kiểm thử nằm trong `frontend/tests/`, giữ cây tương ứng với `src/`; setup ở `tests/setup.ts`. Route Next.js vẫn ở `src/app`; giữ subgroup như `identity/otp` hoặc `ai/admin` khi hữu ích.
- Các slot được đăng ký ở `register.ts` và nạp tập trung qua `src/features/register.ts`. HTTP client/CSRF/refresh dùng chung ở `shared/api`, UI dùng chung ở `shared/ui`; CSS dùng chung/toàn ứng dụng ở `shared/styles`, được import từ layout hoặc component cần dùng. Import của caller và test được cập nhật theo vị trí file mới.
- Migration Flyway đặt trong `backend/src/main/resources/db/migration/<thư mục của unit>/` (danh sách trong `README.md` ở thư mục đó).
- Người code viết unit test; integration test do tester viết.

## Chạy local

Cần Docker, JDK 17, Maven 3.9 và Node.js 20.9 trở lên.

### Toàn bộ bằng Docker

```bash
cp infra/.env.example infra/.env.local
docker compose -f infra/docker-compose.yml -f infra/docker-compose.local.yml --env-file infra/.env.local up --build
```

- Ứng dụng ở <http://localhost:8088> (Nginx), backend trực tiếp ở cổng 8080 (`/health`), Mailpit ở <http://localhost:8025>.
- Không điền cấu hình Drive (service account với Shared Drive, hoặc ba biến `GOOGLE_DRIVE_OAUTH_*` của Drive cá nhân) thì tệp lưu ở volume `files-local` và không có backup database: backup chỉ lưu trên Google Drive. Cách cấu hình: [runbook deploy](docs/runbooks/deploy-vps.md) mục 6.
- Backup database chỉ lưu trên Google Drive: base backup (`pg_basebackup`) mỗi ngày theo giờ trong Cài đặt và WAL mỗi 5 phút, khôi phục được về thời điểm bất kỳ trong 7 ngày. Cần `BACKUP_DB_PASSWORD` (role `backup_replicator`). `U03_BACKUP_PUBLIC_KEY` là tùy chọn: có thì base backup và WAL được mã hóa. Tạo khóa và khôi phục tự động: [scripts/backup/README.md](scripts/backup/README.md).
- Giao diện có tiếng Anh (mặc định) và tiếng Việt; chuỗi nằm trong `frontend/src/i18n/messages/`.

### Backend và frontend chạy trực tiếp khi phát triển

Giữ PostgreSQL, Redis, RabbitMQ và Mailpit trong Docker, dừng container `backend` và `worker` rồi chạy:

- Backend: `mvn spring-boot:run` trong `backend/` với profile `local,worker` (API cùng worker, cổng 8080), biến môi trường lấy từ `infra/.env.local` và các host dịch vụ trỏ về `localhost`.
- Frontend: `npm install` rồi `npm run dev` trong `frontend/`, mở <http://localhost:3000>. Next chuyển `/api/v1/*` và `/mcp` sang `BACKEND_URL` (mặc định `http://localhost:8080`).

## Kiểm thử

- Backend: `mvn test` trong `backend/`.
- Frontend: `npm run lint`, `npm run typecheck`, `npm test` trong `frontend/`.
- E2E trên stack local: [scripts/e2e/README.md](scripts/e2e/README.md).
- MCP: `scripts/check-mcp.ps1` kiểm tra kết nối; `scripts/mcp-smoke.py` chạy thử luồng tạo đề và chấm thật ([hướng dẫn](docs/guides/mcp-clients.md)).

## Agent qua MCP

Giáo viên và sinh viên có thể dùng agent riêng (Codex, Claude Code, Antigravity, OpenCode…) qua endpoint `/mcp` với token tạo trong ứng dụng. Agent chỉ đọc dữ liệu được phép và tạo bản nháp hoặc đề xuất; không nộp bài chính thức và không công bố điểm. Cách kết nối: [docs/guides/mcp-clients.md](docs/guides/mcp-clients.md). Quy trình soạn đề và chấm: [docs/guides/mcp-agent-workflows.md](docs/guides/mcp-agent-workflows.md).

## Triển khai

Staging dùng chung cho nhóm kiểm thử trên VPS: [docs/runbooks/deploy-staging.md](docs/runbooks/deploy-staging.md). Production: [docs/runbooks/deploy-vps.md](docs/runbooks/deploy-vps.md).
