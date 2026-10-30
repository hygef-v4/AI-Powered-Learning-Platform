# U13 Reference Materials — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Bản kế hoạch ban đầu ngày 15/10; triển khai theo approval A ngày 21/10/2026.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Thiết kế: [business-logic-model](../../u13-reference-materials/functional-design/business-logic-model.md), [business-rules](../../u13-reference-materials/functional-design/business-rules.md), [domain-entities](../../u13-reference-materials/functional-design/domain-entities.md), [frontend-components](../../u13-reference-materials/functional-design/frontend-components.md), [NFR](../../u13-reference-materials/nfr-requirements/nfr-requirements.md), [NFR design](../../u13-reference-materials/nfr-design/nfr-design-patterns.md), [infrastructure](../../u13-reference-materials/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, bổ sung kiểm tra khả năng triển khai ngày 2026-10-21; **Part 1 đã duyệt (Answer a); Part 2 đã triển khai và kiểm tra developer, chờ người dùng review**. Khi bắt đầu phiên, unit chỉ có khung tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại), chưa có nghiệp vụ/migration/test riêng. Kết quả hiện tại: [code-summary](../../u13-reference-materials/code/code-summary.md). Đây là nguồn thực thi duy nhất của Code Generation U13; các chi tiết bên dưới cụ thể hóa mười bước, không thay phạm vi UC 58–62.

## Code có sẵn trước triển khai ngày 2026-10-21

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/materials/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../materials/contract/` | Trống: unit không cung cấp interface mới, chỉ cài interface của unit trước |
| `frontend/src/features/materials/slots.ts` | Slot đã khai báo: `learningMaterialSections` |
| `frontend/src/features/materials/register.ts` | Đã đăng ký `manager-materials` trỏ `/manager/materials`; sửa tại chỗ, không đăng ký trùng |
| `backend/.../jobs/contract/JobTypes.java`, `jobs/application/JobRouting.java` | Đã có `MATERIAL_TEXT_EXTRACT` và định tuyến `TRIGGERED`; bước 5 tái sử dụng, không thêm lần nữa |

## Kiểm tra đầu vào — 2026-10-21

- [x] Đọc `CLAUDE.md`, rule Code Generation, state, thiết kế functional/NFR/infrastructure U13 và truy vết FR-004/005, US-CNT-001/002.
- [x] Xác nhận workspace brownfield Spring Boot/Java 17 + Next.js/React, nhánh hiện tại `reference-materials`, working tree sạch trước phiên này.
- [x] Kiểm contract thật U01/U02/U03/U04/U05/U06 và các registry frontend, thay vì dựa vào tên trong thiết kế.
- [x] Xác nhận UI cục bộ ở `E:/FPTUni/sem9_fa26/ui-reference`: m13, m13b, m02, m02b, t20, s02, s05, s05b; dùng HTML/CSS đó cùng shared UI hiện có khi triển khai.
- [x] Giữ extension đã chốt: Security/Resiliency theo phạm vi rút gọn trong state; PBT tắt, không hỏi bật lại.
- [x] Người dùng duyệt toàn bộ kế hoạch và các quyết định tương thích dưới đây; câu hỏi tại [u13-implementation-approval-questions.md](u13-implementation-approval-questions.md).

## Quyết định triển khai đề xuất

### C01 — Phân quyền và ranh giới unit

U13 chỉ gọi package `contract` của unit khác. Bổ sung action U13 cùng dòng tương ứng ở `identity/contract/Actions.java` và `identity/application/AccessPolicy.java`, theo quy ước U01; không dùng action quản trị môn chỉ dành cho Admin. Actor lấy từ principal phía server. Mọi nhánh kiểm account/session hiện thời qua `IAuthorization`, sau đó kiểm đúng scope; role từ request không được dùng.

- Tài liệu môn: Subject Manager quản lý môn được giao; teacher chỉ đọc nếu đang dạy lớp thuộc môn. Dùng `IClassScope.listTaughtClasses` rồi `IClassAccess.getClassRef` để không bỏ sót lớp đã archive do dùng nhầm `listOpenClassesOf`.
- Tài liệu lớp: chỉ teacherId được phân công quản lý. Subject Manager chỉ quản lý lớp nếu đồng thời được giao dạy lớp đó; quyền chủ nhiệm môn tự nó không cho xem học liệu riêng của lớp.
- Student cần lớp `OPEN`, ghi danh đang hiệu lực, tài liệu `ACTIVE` và subjectId/classId khớp. Kiểm lại các điều kiện đó ở list, detail/view, download và MCP; không chỉ ẩn nút frontend.
- Admin không có quyền đọc hoặc quản lý. Ngoài phạm vi trả 404 và audit `ACCESS_DENIED`; provider lỗi thì từ chối, không trả danh sách rỗng hoặc token giả.
- Môn/lớp archive chỉ đọc với người còn quyền; quyền sửa kiểm trạng thái và optimistic version trong transaction. SUCCESS audit cùng transaction, denied audit qua cơ chế transaction riêng của U02.

### C02 — Dữ liệu, tệp và transaction

U13 sở hữu hai bảng trong infrastructure design. ID, kiểu FK, grants và Flyway locations phải đối chiếu migration thật trước khi viết SQL. Không tự tạo bảng U07/U08/U12 còn thiếu để làm full startup qua. Migration toàn hệ thống và kiểm thử provider thật là gate riêng nếu prerequisite chưa sẵn sàng.

`IArtifact.attach(FileRef, Actor, MATERIAL)` xác nhận ownership/purpose và lấy metadata tin cậy; bỏ qua metadata tệp do client khai. Upload tái sử dụng `frontend/src/features/files/components/FileUploader.tsx` và `/api/v1/files`, không dựng upload service khác. Download cấp qua `issueDownloadToken` sau authorize; response U13 không lộ storage fileId. Token hết hạn 5 phút do U03 chịu trách nhiệm; U13 UI cho lấy token mới.

Update metadata, replace version, archive và reorder phải nguyên tử, có điều kiện theo `version`; tranh chấp trả 409 và không ghi đè. Reorder chỉ gồm ID thuộc đúng scope, không trùng, không kéo tài liệu môn vào thứ tự của lớp; payload có version của các hàng bị thay đổi, transaction rollback toàn bộ khi một hàng xung đột. Lịch sử phiên bản giữ nguyên nội dung cũ.

### C03 — Job trích chữ

Tái sử dụng `JobTypes.MATERIAL_TEXT_EXTRACT`, queue `TRIGGERED`, `IJob`, `IJobHandler`, `IPendingSweeper`. Job mang materialId/versionNo; idempotency key theo cặp này; enqueue theo transaction contract U03. Handler kiểm PENDING và version trước đọc, không xử lý phiên bản đã bị thay trước lúc job bắt đầu. Nếu replace diễn ra trong lúc đang trích, kết quả chỉ được ghi có điều kiện vào đúng hàng phiên bản cũ, không sửa currentVersionNo hoặc chữ của phiên bản mới. Callback hết retry chỉ chuyển đúng PENDING sang FAILED, không ghi đè kết quả đã thành công.

Thêm parser Tika cho PDF/DOCX/PPTX với phiên bản đồng bộ `tika.version` trong `backend/pom.xml`; đọc stream qua U03, luôn đóng stream, không OCR/macro/gọi AI. Giới hạn 1.000.000 ký tự, thời gian tối đa 2 phút và tài nguyên parser; trung bình dưới 50 ký tự/trang thành NO_TEXT. Phải có kiểm chứng giới hạn và hủy việc thực tế; không coi chỉ đặt `timeout()` là đủ. Trích chữ lỗi không chặn xem/tải tệp.

### C04 — Contract AI hiện tại thiếu metadata phiên bản

`IReferenceMaterial` hiện trả `SelectableMaterial(id,title,hasText)` và `MaterialText(id,title,text,truncated)`: không có versionNo/textStatus/URL như mô tả thiết kế. Đề xuất cài đúng chữ ký hiện tại, không âm thầm đổi DTO chung hoặc giả rằng provenance đủ phiên bản. U13 detail/MCP có metadata phiên bản thực; việc thêm trường vào DTO U06 và kiểm consumer U07/U10 được ghi thành gate bàn giao trước khi xác nhận full F7/provenance.

`extractText` kiểm lại từng ID, tối đa 10 tài liệu; maxChars là tổng ngân sách kết quả, có trần server và báo truncated chính xác. Không trả một phần nội dung nếu có ID trái quyền. YouTube trả tiêu đề/mô tả/URL chuẩn hóa, không gọi YouTube API. Tài liệu archived không còn được chọn làm nguồn mới. `ReferenceMaterialService` thay bean rỗng theo cơ chế conditional bean hiện có; cần test context để tránh hai bean.

MCP `read_authorized_material` dùng Actor từ U06, cùng policy với web, giới hạn đầu ra, đánh dấu nội dung là dữ liệu không tin cậy; không thực thi chỉ dẫn trong tài liệu.

### C05 — UI và các trạng thái bắt buộc

Giữ style HTML/CSS cục bộ: nền sáng, thẻ/bảng, font Be Vietnam Pro hiện có, nhãn và dialog tiếng Việt; dùng shared layout, Button/Dialog/Table/Icon, không copy app shell hoặc tải Google Fonts mới. Hành vi theo BR-U13 khi lời trong mock khác nghiệp vụ (archive ẩn với student; teacher còn quyền có thể đọc).

Route `/manager/materials`, `/materials/[id]`, `/materials/[id]/view`; truyền classId cho learner và không làm mất context khi đi qua các link. Dialog thêm tệp/link, sửa metadata, thay phiên bản, archive; list active/archive, filter theo scope và reorder có phản hồi pending/lỗi. PDF dùng viewer browser qua token; video chỉ iframe `youtube-nocookie.com` từ videoId đã validate. Link chỉ nhận một video `youtube.com/watch?v=...` hoặc `youtu.be/...`; từ chối playlist, domain giả, URL nguy hiểm.

Đăng ký đủ SubjectSummarySection, TeacherClassTab (chỉ tab materials), StudentClassSection, AssignmentMaterialSection, QuizMaterialSection; giữ LearningMaterialSection cho U11. Component dùng API thật; fixtures chỉ trong test hoặc preview development được chặn production. Có loading/empty/404/401/503/retry/409, ngăn double submit và stale response khi đổi môn/lớp. Dialog hỗ trợ bàn phím/focus, nút có accessible name và data-testid ổn định; kiểm desktop và mobile.

## Chi tiết thực thi mười bước

Các path backend tương đối với `backend/src/main/java/edu/aiplatform/`; frontend tương đối workspace. Mỗi bước chỉ đánh dấu khi có implementation và developer checks phù hợp, cập nhật ngay ở checklist chính.

| Bước | Đầu ra cụ thể và kiểm tra | Truy vết |
|---|---|---|
| 1 | SQL tại `backend/src/main/resources/db/migration/materials/V20260925_1900__create_material_tables.sql`; constraints scope/kind/status/size/version, FK, indexes, grants; kiểm Flyway discovery và ghi prerequisite toàn chuỗi | BR-11/12/13, P1 |
| 2 | `materials/domain/Material.java`, `MaterialVersion.java`, enum, `IMaterialRepository`; repository JPA + adapter trong `materials/infrastructure/`; domain tests cho invariants, URL, archive, version | UC58–62, BR-10–13 |
| 3 | `materials/application/MaterialService.java`, command DTO/validation, success audit; create/update/replace/archive/reorder; optimistic conflict, rollback và enqueue tests | UC59/61, BR-40/41 |
| 4 | `materials/application/MaterialAccessPolicy.java`, `MaterialQueryService.java`, safe read DTO; action registry U01 và role/scope matrix; token sau quyền; list của lớp gộp subject/class đúng thứ tự | UC58/60/62, BR-01–03/20/23/42 |
| 5 | `materials/application/MaterialTextExtractJobHandler.java`, sweeper và extractor trong infrastructure; parser dependencies; idempotence/stale/race/NO_TEXT/FAILED/resource-limit tests | F6, BR-30, P2 |
| 6 | `materials/application/ReferenceMaterialService.java`, `MaterialMcpTools.java`; current contract, bean wiring, quyền/trần/YouTube/tool tests; ghi rõ gate version metadata U06 | F7, BR-31/32 |
| 7 | `materials/web/SubjectMaterialController.java`, `ClassMaterialController.java`, `MaterialController.java`, `MyMaterialController.java`, validated request/response và scoped errors; `docs/contracts/openapi/materials.yaml` theo vị trí contract repo hiện có; HTTP principal/validation/404/409 tests cho toàn endpoint | Domain entities §4, UC58–62 |
| 8 | `frontend/src/features/materials/` types/api/validation/components/CSS module và sửa register.ts; các route ở `frontend/src/app/`; sáu component/slot nêu trên, upload component U03, viewer và dialogs theo UI cục bộ | Toàn màn U13, BR-20–23 |
| 9 | `backend/src/test/java/edu/aiplatform/materials/`, `frontend/src/features/materials/*.test.*`; chạy focused rồi full suite, ArchUnit, lint/typecheck/build. Tạo `aidlc-docs/construction/build-and-test/u13-verification.md` với lệnh tái hiện, kết quả thật và matrix dành tester | NFR-U13-40, DoD |
| 10 | Review UC/story/BR với code; `aidlc-docs/construction/u13-reference-materials/code/code-summary.md` ghi paths, test evidence và gate. Cập nhật state; chờ người dùng review trước chuyển unit/stage | AI-DLC Part 2, DoD |

Không thực hiện commit/push/merge, cập nhật Jira hay gửi tin cho owner unit trong kế hoạch này. Không giao agent phụ; thực thi trong chat hiện tại.

## Verification và điều kiện bàn giao

- Backend: targeted domain/service/HTTP/job/MCP tests và ArchitectureTest, sau đó Maven verify. Ghi số pass/fail/skip; test bỏ qua không được tính pass. Không sửa test unit khác chỉ để làm suite xanh.
- Frontend: Vitest focused và full suite, lint, typecheck, production build theo scripts hiện có. Browser QA danh sách → tạo → sửa → thay phiên bản → archive, learner PDF/video, 409/retry và mobile; nếu browser/provider không sẵn sàng thì ghi chưa kiểm thay vì đánh hoàn thành.
- Test bảo mật phải chứng minh không gọi attach/open/token/repository mutation khi bị từ chối; learner lớp khác cùng môn không đọc tài liệu riêng của lớp; Admin bị chặn mọi endpoint/tool.
- Full integration do tester độc lập: migration thật, JWT/current account/CSRF, audit rollback, private Drive token hết hạn, Rabbit retry/sweeper, replace đang trích, các slot consumer thật và provenance U06. Không đồng nhất developer unit tests với acceptance này.
- NFR p95 300ms list, 200ms token, 1s extractText và parser 50MB/2 phút cần phép đo riêng, không suy ra từ unit tests/build.

## Compliance của bản kế hoạch

| Rule | Đánh giá ở Part 1 | Cách kiểm khi sinh code |
|---|---|---|
| SECURITY-03 | Compliant: kế hoạch không chứa secret/nội dung tài liệu; cấm log chúng | Kiểm logging/audit DTO, không lộ fileId/token/chữ |
| SECURITY-04 | Compliant ở mức kế hoạch: dùng CSP/shared headers hiện có | Kiểm iframe PDF và youtube-nocookie với cấu hình thật |
| SECURITY-05 | Compliant ở mức kế hoạch: validation URL/tệp/payload/version | Negative tests, metadata lấy từ U03 |
| SECURITY-08 | Compliant ở mức kế hoạch: quyền server/scoped 404 | Role/scope matrix web, download, AI, MCP |
| SECURITY-09 | Compliant ở mức kế hoạch: giữ cấu hình bảo mật chung, không thêm app shell hoặc remote font mới | Kiểm config/CSP, không mở anonymous route hoặc bật debug |
| SECURITY-12 | Compliant ở mức consumer: Actor từ principal, kiểm phiên qua U01, không thêm credential | Kiểm anonymous/expired session; password/login/MFA do U01 sở hữu, N/A cho implementation U13 |
| SECURITY-15 | Compliant ở mức kế hoạch: fail closed | Provider deny/error/null tests |
| RESILIENCY-04 | N/A: chưa triển khai/publish; không chọn quy trình rollback mới | Gate triển khai theo quy trình chung |
| RESILIENCY-06 | N/A cho thay đổi tài liệu này; runtime health chưa kiểm | Giữ health chung và kiểm worker trên stack thật |
| RESILIENCY-10 | Compliant ở mức kế hoạch: parser có giới hạn, dependency failure có hành vi rõ | Timeout/resource-release/retry tests và gate thật |
| Các rule còn lại | N/A theo phạm vi rút gọn đã duyệt trong state | Không mở rộng đồ án |
| Property-Based Testing | Disabled theo state | Dùng JUnit/Vitest theo kế hoạch |

## Checklist

- [x] **Bước 1 — Migration:** `db/migration/materials/V20260925_1900__create_material_tables.sql` theo infrastructure-design.
- [x] **Bước 2 — Domain:** `Material`, `MaterialVersion` và enum; repository chỉ dùng trong package.
- [x] **Bước 3 — Quản lý:** `MaterialService` (F2, F4, P1).
- [x] **Bước 4 — Xem:** `MaterialQueryService`, `MaterialAccessPolicy` (F1, F3, F5, P3).
- [x] **Bước 5 — Trích chữ:** `MaterialTextExtractJobHandler`, `MaterialTextExtractor`, dùng lại `MATERIAL_TEXT_EXTRACT`/routing đã có của U03 (F6, P2).
- [x] **Bước 6 — Nguồn cho AI:** `ReferenceMaterialService` (cài `IReferenceMaterial` của U06, thay adapter rỗng), `MaterialMcpTools` (tool `read_authorized_material`) (F7).
- [x] **Bước 7 — API:** controller theo domain-entities mục 4; `docs/contracts/openapi/materials.yaml`; ngoài phạm vi trả `404`.
- [x] **Bước 8 — Frontend:** `SubjectMaterialListPage` (đăng ký `NavEntry`), `MaterialSummary` (`SubjectSummarySection`), `MaterialDetailPage`, `ClassMaterialsTab` (`TeacherClassTab`), `ClassMaterialsCard` (`StudentClassSection`), `LearningMaterialPage` với slot `LearningMaterialSection`; `MaterialLinkList` cài slot `AssignmentMaterialSection` (U08), `QuizMaterialSection` (U11).
- [x] **Bước 9 — Unit test (người code viết):** quyền theo phạm vi (Chủ nhiệm môn, giảng viên, người học, Admin); link YouTube hợp lệ và không hợp lệ (playlist, domain khác); thay phiên bản giữ phiên bản cũ; lưu trữ ẩn với người học; trích chữ `EXTRACTED`, `NO_TEXT`; `extractText` kiểm lại quyền và cắt theo trần; tool MCP từ chối ngoài quyền; component frontend. Integration test do tester viết.
- [ ] **Bước 10 — Review:** đối chiếu UC 58–62, story, BR-U13 với code; người dùng duyệt trước khi chuyển unit.

### Bằng chứng và checkpoint bàn giao

#### SEC-004 correction authorized 2026-10-21

- User requested security configuration alignment with AI-DLC. Replace scoped static headers in `frontend/next.config.ts` with per-request CSP in `frontend/src/proxy.ts`; use request rendering in the root layout so Next attaches the actual nonce to scripts. Apply required headers to all HTML page routes per SECURITY-04, preserving API/static asset handling.
- Script CSP uses fresh 256-bit nonce and strict-dynamic; no production unsafe-inline/eval. Deny framing of application pages, while retaining PDF blob, YouTube and approved Draw.io child frames. Style attributes retain an explicit documented exception for existing React dynamic styles; eval/WebSocket exceptions apply only to local development.
- [x] Implement request nonce/header correction and focused regression tests.
- [x] Verify production HTML nonce/header consistency (including 404 and repeat requests), full frontend323pass/62files, typecheck/lint/build and browser hydration/U13 navigation/YouTube. Native PDF renderer acceptance remains open; see handoff.

- [x] Developer đối chiếu UC/story/BR, sửa expected-error transaction của MCP và phân biệt đọc archived reference với chọn nguồn AI mới.
- [x] Backend full verify928 total/851pass/77skip; final focused80pass và JAR packaged. Frontend321pass, typecheck/lint/build pass; standalone route/header smoke pass.
- [x] Kiểm browser fixture manager flow và mobile; ghi rõ native PDF renderer chưa xác nhận, không coi blob/component test là PDF browser acceptance.
- [x] Lập [code-summary](../../u13-reference-materials/code/code-summary.md) và [u13-verification](../../build-and-test/u13-verification.md), giữ gate provider/DB/tester/NFR/U06 metadata.
- [ ] Người dùng review code và duyệt chuyển bước. Các gate integration/acceptance trong handoff vẫn độc lập với developer checks ở bước1–9; global stage và story acceptance chưa được đánh complete.

## Integration test cases (tester viết)

- Tải tệp qua token; token hết hạn bị từ chối.
- Thay phiên bản khi job trích chữ phiên bản cũ đang chạy: kết quả gắn đúng phiên bản.
- U07, U10 dùng `extractText` với tài liệu ngoài quyền: bị từ chối.
- Phân quyền 4 role trên mọi endpoint, gồm người học lớp khác cùng môn mở tài liệu của lớp.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
