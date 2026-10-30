# U13 Reference Materials — bàn giao code

**Current source snapshot 2026-10-30:** [as-built-baseline.md](as-built-baseline.md) — source-observed; các số test/ngữ cảnh checkout dưới đây là lịch sử, không phải kết quả chạy lại tại HEAD hiện tại.

## Ghi nhận trước baseline 2026-10-30 (lịch sử)

Ngày 2026-10-21; nhánh `reference-materials`. Người dùng chọn A trong file duyệt Part 1, sau đó yêu cầu giữ nguyên DTO U06 và tiếp tục code. Không commit/push, đổi Jira hoặc nhắn owner unit.

## Phạm vi đã triển khai

| UC | Hành vi và đường vào |
|---|---|
| 58 | `/manager/materials`: chọn môn được giao, danh sách đang dùng/lưu trữ, thứ tự, trạng thái trích chữ, chi tiết và lịch sử phiên bản |
| 59 | Tải tệp qua U03 hoặc thêm một video YouTube; sửa thông tin; thay tệp/link tạo phiên bản; archive giữ lịch sử; reorder có expected version |
| 60 | `TeacherClassTab` materials: học liệu của lớp và học liệu của môn; tài liệu môn trong context lớp chỉ đọc |
| 61 | Giảng viên được giao lớp thêm/sửa/thay phiên bản/archive/reorder tài liệu riêng của lớp; scope đã archive chỉ đọc |
| 62 | `StudentClassSection`, `/materials/[id]/view`: học liệu ACTIVE của môn/lớp OPEN đang ghi danh; token tài khoản 5 phút qua U03, PDF blob viewer và YouTube nocookie; giữ classId trên link |

US-CNT-001/002 có implementation. Không đánh dấu acceptance story hoàn tất trước kiểm thử độc lập. US-CNT-005 (summary, OCR, embedding, RAG) ngoài phạm vi.

Backend mới tại `backend/src/main/java/edu/aiplatform/materials/`: domain bất biến, JPA adapter, access policy, command/query services, job/sweeper/parser, nguồn tham khảo U06, MCP và bốn controller. Migration hai bảng tại `backend/src/main/resources/db/migration/materials/V20260925_1900__create_material_tables.sql`.

Frontend mới tại `frontend/src/features/materials/` và các route `frontend/src/app/manager/materials/`, `frontend/src/app/materials/`. Tái sử dụng FileUploader, shared UI, authenticated layout và registry hiện có. UI bám các artboard m13/m13b/m02/m02b/t20/s02/s05/s05b trong thư mục tham chiếu người dùng cung cấp. CSS mobile chỉ tác động khung khi có trang U13.

OpenAPI: [materials.yaml](../../../../docs/contracts/openapi/materials.yaml). File dùng JSON, là cú pháp hợp lệ của YAML 1.2; các local references đã kiểm tra. Error ngoài scope 404, stale/read-only 409, dependency failure 503, request invalid 400; HTTP authentication/CSRF do U01.

## Thay đổi liên unit

- U01: thêm bốn action U13 và rule tương ứng trong `Actions`/`AccessPolicy`; giữ nguyên rule trước đó. Actor chỉ từ principal, account hiện thời qua IAuthorization; không nhận role/actorId từ body.
- U03: không sửa source; tái sử dụng IArtifact/IJob/IJobHandler/IPendingSweeper và routing MATERIAL_TEXT_EXTRACT đã có. `backend/pom.xml` thêm hai parser module Tika cùng `tika.version` hiện có; không thêm OCR parser.
- U06: không sửa DTO, chữ ký interface hoặc stub. Bean U13 thực hiện IReferenceMaterial; test context xác nhận conditional stub nhường đúng một provider thật. DTO hiện tại thiếu versionNo/textStatus trong kết quả nguồn AI; giữ gate C04, không giả rằng provenance liên unit đã đầy đủ. MCP của U13 có versionNo thực trong kết quả.
- U05/U08/U11: đăng ký qua slot trong register.ts của U13; không sửa implementation các unit này. Giữ LearningMaterialSection cho U11.
- `frontend/next.config.ts`: thêm CSP/header cho `/manager/materials` và `/materials/*`, cho phép self/blob PDF và youtube-nocookie; không áp CSP mới lên route unit khác.

## Các quyết định cần người nhận biết

1. Bản nội dung `versionNo` bắt đầu 1; optimistic `version` bắt đầu 0. Metadata/reorder không tạo bản nội dung. Replace/archive/update dùng CAS; lỗi sau đó rollback transaction, SUCCESS audit cùng transaction và U03 enqueue sau commit theo contract.
2. JPA adapter detach snapshot của chính U13, không clear persistence context của unit khác. Reorder kiểm scope/ID trùng/version trước ghi và cập nhật theo UUID để tránh thứ tự khóa ngược giữa hai reorder.
3. Admin bị chặn; Subject Manager không được xem tài liệu riêng của lớp chỉ nhờ quyền chủ nhiệm môn. Staff còn quyền xem có thể đọc tài liệu archived; learner chỉ thấy ACTIVE trong lớp OPEN đang ghi danh. Download và MCP dùng cùng policy. Learner không tải phiên bản cũ hoặc xem lịch sử nội dung cũ.
4. Job chỉ chạy phiên bản PENDING còn hiện hành khi bắt đầu. Replace đang trích không chuyển kết quả sang phiên bản mới: kết quả và callback FAILED đều CAS đúng materialId/versionNo/PENDING. Sweeper chỉ lấy tối đa 100 pending hiện hành mỗi lượt, giữ idempotency key.
5. Parser PDF/DOCX/PPTX chạy trong JVM con, heap 256MB, metaspace 128MB, tối đa hai worker nội bộ, input 50MB, output 1.000.000 ký tự, deadline 120 giây và kill process khi hủy. Không OCR, không parse tài liệu đính kèm, không chạy macro. Chữ thưa dưới 50 ký tự/trang thành NO_TEXT. Nguồn/API lỗi được retry qua contract U03, hết retry chuyển FAILED.
6. PDF lấy binary qua token và tạo blob URL để không bị X-Frame-Options của endpoint tải chặn iframe. Blob được revoke khi thay nguồn/unmount; có lấy link mới và refresh trước khi token hết hạn. Nút tải dùng token U03 mới. Browser native PDF support vẫn là gate riêng bên dưới.
7. MCP đọc tham chiếu hiện có, kể cả tài liệu archived mà staff còn quyền; chọn nguồn cho task AI mới chỉ cho ACTIVE. Tool có tổng trần 32.000 ký tự, đánh dấu untrustedContent. Transaction REPEATABLE_READ giữ version/text cùng snapshot; expected ApiException của read services không làm outer MCP transaction rollback-only. Có test proxy transaction thật cho error 404.
8. extractText kiểm tất cả ID trước đọc chữ, tối đa 10 ID, maxChars là tổng ngân sách và không vượt 1.000.000. YouTube chỉ trả metadata trong text, không gọi API/caption AI.

## Bằng chứng kiểm tra

- Full backend `mvn -B verify`: 928 tổng, 851 pass, 77 skip, 0 failure/error; JAR packaged. Đây là lần trước hai chỉnh sửa review cuối về transaction/MCP archived reference.
- Focused U13 + ArchitectureTest cuối: 80 pass / 0 failure/error/skip (78 U13 + 2 architecture), executable JAR packaged; chi tiết trong [hướng dẫn kiểm tra](../../build-and-test/u13-verification.md). Không cộng focused vào full để tạo số pass giả. Bao gồm bean wiring, token sau quyền, HTTP validation, role/scope, CAS conflict, extraction race/idempotence và parser từ ba định dạng thật.
- Parser từ executable JAR đã chạy với PDF mẫu và kiểm được chữ/page count; không chỉ dựa vào test classpath. Smoke không khởi động Spring, không gọi Drive/AI.
- Full frontend: 321 pass / 61 files, gồm 29 ca U13; typecheck/lint không warning/error; production standalone build pass. HTTP smoke standalone: cả ba route U13 trả200 và có CSP/blob/YouTube/nosniff đúng. Không gọi API nghiệp vụ trong production smoke.
- Browser QA dùng fixture HTTP loopback riêng trong `.tmp/u13/`, không có fixture trong application source/build. Đã kiểm list, chặn playlist, create link, sửa title, replace giữ cả version1/version2, archive read-only; mobile 390px sau sửa có document scrollWidth bằng clientWidth (375px trừ scrollbar).
- YouTube ID mẫu không có video: player báo video unavailable như thiết kế. PDF token/binary/blob iframe đã nạp; in-app browser không hiển thị native PDF renderer trong vùng iframe, nên **không xác nhận render PDF thực tế qua browser**. JUnit PDF parser và component blob tests không thay cho acceptance này.

## Gate chưa hoàn tất

- Chưa chạy migration U13 trên PostgreSQL thật hoặc kiểm full startup với toàn bộ migration owner; checkout còn các prerequisite migration liên unit đã ghi ở state. Không tạo schema giả trong runtime để bỏ qua chúng.
- Chưa có kiểm độc lập HTTP/JWT/current-session/CSRF + DB + audit rollback + private Drive token hết hạn + RabbitMQ retry/sweeper; các suite skip không được coi là pass.
- Chưa kiểm reorder/CAS và scope lifecycle race trên PostgreSQL thật, provider timeout/resource release toàn stack, hoặc account bị đổi role/disable giữa request/token trên môi trường tích hợp.
- Native PDF rendering/download và learner/class/attachment slot workflows cần browser phổ thông với provider thật. UI fixture của chủ nhiệm môn không chứng minh auth/provider integration.
- DTO nguồn AI của U06 chưa mang provenance phiên bản theo quyết định người dùng. Thông tin tên người tạo ở history chưa được enrich qua contact provider; hiện dùng createdBy của contract dữ liệu U13.
- Chưa đo p95 list 300ms, token 200ms, extractText 1s hoặc parser 50MB/2 phút trên tải thật. Heap/deadline tests không phải phép đo NFR.
- Chờ người dùng review code; chưa chuyển unit hoặc Operations, chưa đánh toàn UC58–62 Done.

## Compliance

Local handoff on `reference-materials`: user requested separate commits on 2026-10-21, with no push. Code is split into domain/persistence, scoped management, bounded extraction, AI/MCP, REST/OpenAPI, frontend and SEC-004 correction; this documentation is committed separately. No application source changes during commit preparation; acceptance gates above remain open.

| Rule | Kết quả developer checks | Giới hạn |
|---|---|---|
| SECURITY-03 | Compliant trong U13: không log text/token/fileId; payload job chỉ IDs | Centralized logging triển khai dùng cấu hình chung, chưa chạy full stack |
| SECURITY-04 | SEC-004 configuration corrected: per-request script nonce, strict-dynamic, DENY, frame-ancestors none; required headers on HTML page routes via Next Proxy. Production HTTP nonce/script and browser hydration checks pass | Native PDF renderer and deployed Nginx/HTTPS acceptance pending. Documented style-src-attr unsafe-inline exception for existing React style attributes; script eval only in development |
| SECURITY-05 | Compliant: metadata/source/version/scope validation; URL single-video chặt | Kiểm nội dung file theo provider U03, full integration pending |
| SECURITY-08 | Compliant ở unit/HTTP consumer tests: deny default, role/scope, token sau kiểm quyền | JWT/session/DB/provider thật pending |
| SECURITY-09 | Compliant cho thay đổi U13: không mở anonymous route hoặc dùng remote font mới | Shared deployment hardening ngoài implementation U13 |
| SECURITY-12 | Compliant ở consumer: principal U01, không thêm credential | Password/login/MFA do U01, N/A cho U13 |
| SECURITY-15 | Compliant ở developer checks: safe errors, CAS, stream/process cleanup, retry | PostgreSQL/provider fault tests thật pending |
| RESILIENCY-04 | N/A: chưa deploy hoặc thay cơ chế rollback chung | Không suy ra production rollback từ migration code |
| RESILIENCY-06 | N/A cho service mới: chạy trong backend/worker hiện có | Shared health trên deployment chưa kiểm |
| RESILIENCY-10 | Compliant trong parser và boundary implementation | Deadline/resource tests toàn stack và NFR pending |
| Rule khác / PBT | N/A theo phạm vi rút gọn / PBT disabled | Giữ nguyên quyết định trong state |
