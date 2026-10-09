# U05 Content, Material & RAG - Code Generation Plan

**Bản tài liệu 2026-10-09**: UC 15, 30, 33, 34, 36, 54, 55 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U05. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story trong phạm vi**: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Catalog chỉ chứa story MVP.
- **Primary UC hiện hành**: UC 15, 30, 33, 34, 36, 54, 55 theo bản 73 UC. Supporting flows theo current-srs-contract.md.
- **Quyết định 2026-10-04**: chapter đổi thành module của môn (mọi lớp dùng chung); mỗi module có nút tải tệp/gắn link; học liệu của môn (`class_id` rỗng) hoặc của lớp.
- **Quyết định 2026-10-09**: Chủ nhiệm môn quản lý module và học liệu của môn trên Material List; giảng viên quản lý học liệu của lớp trên tab Materials của Teacher Class Detail; Material Detail dùng chung; quiz gắn với học liệu (U09 soạn, U11 cho làm); bỏ bình luận dưới thông báo; Class Announcements mở từ Class Dashboard, gộp thông báo các lớp mình học hoặc dạy; Admin không quản lý học liệu và thông báo.
- **Thiết kế nguồn**: `construction/u05-content-material-rag/` (functional-design, nfr-requirements, nfr-design, infrastructure-design), `construction/shared-infrastructure.md` và [mô hình dữ liệu của unit](../../u05-content-material-rag/functional-design/domain-entities.md).
- **Quyết định 2026-10-03**: học liệu chỉ tải lên rồi quét; không soạn markdown, không phiên bản, không phát hành.
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `JobPort`, `JobHandler`, `PendingSweeper` | U03 | Dùng thật |
| `ArtifactPort`, `FileUploader` | U03 | Dùng thật |
| `ClassAccessPort`, `SubjectScopePort`, `ClassScopePort` | U04 | Dùng thật; cần thêm `ClassAccessPort.listOpenClassesOf` (plan U04 §7); U05 thay `EmptyPublishedContentAdapter` của U04 bằng `PublishedContentService` |
| `EventPublisherPort` | U03 | Phát `class.announcement-posted` sau commit; U16 tiêu thụ |
| `AiUsagePort` | U13 (`C`) | Cần `quote`, `hold`, `begin`, `complete`, `release` (mục 8). Adapter giả chỉ local/test, không gọi Gemini/không trừ credit thật; trước bật Gemini phải cắm U13 thật: SettingsPort U03, trần chi phí, ai_suggestions và credit U07; không bảng ai_services |
| `EmbeddingPort`, `YoutubePort` | Gemini, YouTube | Adapter thật + adapter giả khi không có key |

### Dữ liệu U05 sở hữu

PostgreSQL `modules`, `lessons` (gồm `class_id`, `uploaded_by`, `extracted_text`, `summary`, `embedding vector(768)`), `announcements`; việc LESSON_SCAN trích chữ trên jobs.triggered; MATERIAL_SUMMARY sau nút View Material trên jobs.gemini, `YOUTUBE_CAPTION` trên `jobs.youtube` (U03 khai báo queue).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  content/
    api/                ContentController, LessonViewController,
                        AnnouncementController, DTO
    application/        ModuleService, LessonService, PublishedContentService,
                        RetrievalService, AnnouncementService
    domain/             Module, Lesson, ScanStatus, YoutubeUrlParser,
                        Announcement
    event/              AnnouncementPosted, LessonReplaced
    scan/               TextExtractor, CaptionFetcher, LessonSummarizer,
                        PassageSelector
    infrastructure/     JPA repository, LessonVectorRepository (JdbcTemplate),
                        GeminiEmbeddingAdapter, GeminiSummaryAdapter, YoutubeAdapter,
                        FakeEmbeddingAdapter, FakeSummaryAdapter, FakeYoutubeAdapter,
                        EnvAiUsageAdapter
    worker/             LessonScanHandler, LessonPendingSweeper
    port/               PublishedContentPort, RagRetrievalPort, ContentRefPort,
                        EmbeddingPort, SummaryPort, YoutubePort, AiUsagePort
/backend/src/main/resources/db/migration/content/
/infra/postgres/init/01-extensions.sql
/frontend/src/shared/content/
/frontend/src/app/manager/materials/
/frontend/src/app/classes/[id]/teaching/materials/
/frontend/src/app/classes/[id]/materials/
/frontend/src/app/classes/announcements/
/contracts/openapi/content.yaml
```

## 3. Các bước

### Nhóm A - Khung và hạ tầng

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - `pom.xml`: `tika-parsers-standard-package`, `com.pgvector:pgvector`, thư viện đọc phụ đề YouTube. Biến cấu hình U05 theo `logical-components.md` §3.
- [ ] **Bước 2** - Docker Compose (image `pgvector/pgvector:pg16` đã có từ khung): script `infra/postgres/init/01-extensions.sql` (`CREATE EXTENSION vector`), RAM worker 1,5 GB, truyền `GEMINI_API_KEY`, `YOUTUBE_API_KEY`; cấu hình runtime đọc SettingsPort U03 qua U13. Nginx CSP thêm `frame-src https://www.youtube-nocookie.com`.

### Nhóm B - Domain và logic

- [ ] **Bước 3** - Domain: `Module` (thuộc môn), `Lesson` (`FILE`/`YOUTUBE`, của môn hoặc của lớp) với `ScanStatus` và chuyển trạng thái; `YoutubeUrlParser` (chỉ một video, regex ID) (BR-U05-02, 10, 11, 14, 22, NFR-U05-21).
- [ ] **Bước 4** - Port và adapter giả: `EmbeddingPort`, `SummaryPort`, `YoutubePort`, `AiUsagePort`, `PublishedContentPort`, `RagRetrievalPort`, `ContentRefPort` (thêm `getLessonRef`) (P7).
- [ ] **Bước 5** - `ModuleService`: Chủ nhiệm môn tạo, đổi tên, đổi thứ tự, lưu trữ module của môn trên Material List (mọi lớp dùng chung, không sao chép); audit (F1, F2, BR-U05-01, 14, 50).
- [ ] **Bước 6** - `LessonService`: tải tệp (đổi `FileRef` qua `ArtifactPort.attach`) hoặc link video vào module; Chủ nhiệm môn tải trên Material List → `class_id` rỗng, giảng viên tải trên tab Materials → `class_id` của lớp; ghi `uploaded_by` (BR-U05-02, 12); upload không AI/credit; chỉ giữ credit khi nhận yêu cầu View Material; `PENDING` + gửi việc trong cùng transaction; sửa thông tin (tên, thứ tự), xóa (lưu trữ); không có thay tài liệu hay quét lại (F3, F5, F8, BR-U05-11, 21, 22, 30, 39).
- [ ] **Bước 7** - `TextExtractor` (Tika theo luồng, giới hạn ký tự, `NO_TEXT`), `CaptionFetcher` (vi → en → tự động, `NO_CAPTION`) (P2, BR-U05-32, 33).
- [ ] **Bước 8** - LessonScanHandler (TEXT) và MaterialSummaryHandler (AI) (cập nhật có điều kiện từ `PENDING`/`BUSY`, semaphore, extraction không AiUsagePort/credit tới EXTRACTED; MaterialSummaryHandler riêng begin/complete/release theo HOLD requester, một vector, ghi một transaction, `BUSY`/`FAILED`) và `LessonPendingSweeper` (gửi lại `PENDING` quá 5 phút và `BUSY` mỗi 30 phút, `BUSY` quá 24 giờ → `FAILED`) (F6, P1, P3, BR-U05-35…39).
- [ ] **Bước 9** - `PublishedContentService` và `LessonViewController`: `GET /lessons/{id}` và tải tệp cho R2 (học liệu của môn), R3/R4 (học liệu của lớp, học liệu của môn chỉ đọc), R5 (ghi danh `ACTIVE`, lớp `OPEN`, lesson `ACTIVE`) (F4, F9, BR-U05-02, 04, 23).
- [ ] **Bước 10** - `RetrievalService` (kiểm phạm vi, `AiUsagePort` cho `requesterId`, vector câu hỏi, k ≤ 10, `PassageSelector` ≤ 12 000 ký tự) (F6, P4, BR-U05-40…44).
- [ ] **Bước 11** - `AnnouncementService`: giảng viên lớp tạo (phát event U16 sau commit), sửa, xóa mềm thông báo theo `version`; feed `GET /me/announcements` gộp lớp `OPEN` actor học hoặc dạy, lọc theo lớp; kiểm quyền U04, lọc markdown (F11–F13, BR-U05-60…65).
- [ ] **Bước 12** - Unit test cho mọi `BR-U05-xx`: URL YouTube giả mạo hoặc playlist, markdown có script, PDF không chữ, video không phụ đề, vượt trần (tự thử lại), upload thiếu credit vẫn thành công, chưa gọi AI; thiếu credit lúc bấm summary giữ EXTRACTED, kết thúc quét trả credit còn giữ.
- [ ] **Bước 13** - Tóm tắt: `aidlc-docs/construction/u05-content-material-rag/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu và adapter ngoài

- [ ] **Bước 14** - Flyway `db/migration/content/V20260925_1200__create_modules_lessons_announcements.sql` theo `infrastructure-design.md` §4.
- [ ] **Bước 15** - JPA repository; `LessonVectorRepository` (lọc lesson trong phạm vi, `<=>`) (P4).
- [ ] **Bước 16** - `GeminiEmbeddingAdapter` (768 chiều, key trong header, timeout) và `YoutubeAdapter` (phụ đề ưu tiên vi → en → tự động) (P5).
- [ ] **Bước 17** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test Testcontainers (image pgvector, Redis, RabbitMQ) với adapter giả: upload → trích chữ → EXTRACTED (không AI/credit) → nút View Material → tóm tắt/embedding → INDEXED; `retrieve` không trả lesson lưu trữ/lớp khác/môn khác, phạm vi môn không trả học liệu của lớp; lớp khác không thấy học liệu riêng của lớp; quét chạy lại không trừ credit hai lần; thiếu credit khác `BUSY`. Adapter thật test bằng mock HTTP (429, 403).
- [ ] **Bước 18** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 19** - `/contracts/openapi/content.yaml` (endpoint theo `frontend-components.md`).
- [ ] **Bước 20** - Controller + DTO + validation.
- [ ] **Bước 21** - Test MockMvc: giảng viên không sửa được học liệu của môn, Chủ nhiệm môn không sửa được học liệu của lớp mình không dạy, học viên lớp khác không tải được tệp hoặc đọc thông báo, người học không đăng được thông báo, giảng viên không tạo được module, Admin bị từ chối, không có endpoint `retrieve` công khai.
- [ ] **Bước 22** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 23** - Màn Material List (`MaterialListPage`, `SubjectPicker`) và tab Materials (`MaterialsTab` gắn vào `TeacherClassPage` của U04) dùng chung `ModuleList`, `ModuleItem` (nút "Tải tệp", "Gắn link video"), `LessonRow`, `ScanStatusBadge` (poll, không có Quét lại).
- [ ] **Bước 24** - Popup `UploadLearningMaterialsDialog` mở từ nút của module (nhiều tệp hoặc một link video, module đã chọn sẵn, hiện mức credit cần giữ và số dư); màn Material Detail (`MaterialDetailPage`, `MaterialEditForm` chỉ sửa thông tin, `DeleteMaterialDialog`).
- [ ] **Bước 25** - Màn Learning Material (`LearningMaterialPage`: `MaterialViewer` với PDF xem trực tiếp, tải tệp, video `youtube-nocookie`; SummaryAction (nút cho Student theo R5) + SummaryBlock; chỗ gắn `LessonQuizList` của U11).
- [ ] **Bước 26** - Màn Class Announcements (`ClassAnnouncementsPage`, `ClassFilter`, `AnnouncementFeed`, `AnnouncementCard`, `AnnouncementForm` tạo/sửa, `DeleteAnnouncementDialog`) (UC 30, 36).
- [ ] **Bước 27** - Test frontend: markdown có script bị lọc, URL YouTube sai hoặc playlist bị chặn, upload không chặn vì credit; nút Tóm tắt tài liệu báo thiếu credit mà vẫn xem/tải được, poll dừng ở trạng thái cuối; badge `BUSY` báo hệ thống sẽ tự thử lại.
- [ ] **Bước 28** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 29** - Cập nhật `README.md`: tạo `GEMINI_API_KEY`, `YOUTUBE_API_KEY`; chạy local không có key; hạ `U05_SCAN_CONCURRENCY` khi VPS nhỏ; cách U13 dùng `RagRetrievalPort`.
- [ ] **Bước 30** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-CNT-001 (UC 34, 55) | 5, 6, 7, 8, 23, 24 |
| US-CNT-002 (UC 33, 54) | 5, 6, 23, 24 |
| US-CNT-005 (UC 34, 55) | 3, 6, 8, 16 |
| US-CNT-004 (UC 30, 36) | 11, 14, 19-21, 26 |
| UC 15 | 9, 25 |
| RAG cho U13 | 10, 15, 17 |


## 5. Ngoài phạm vi

- Soạn nội dung trực tiếp, phiên bản, phát hành học liệu, playlist YouTube (bỏ ngày 2026-10-03).
- Tìm kiếm học liệu theo yêu cầu người dùng (ngoài phạm vi dự án). Tóm tắt theo nút trên màn xem học liệu nay thuộc phạm vi (mục 7).
- Gọi LLM tạo đề/chấm (U13, U15).

## 6. Revision implementation scope - 2026-10-08
- [ ] AnnouncementService update/delete với version, updated/deleted actor/time, DELETED khỏi feed; DTO/PATCH/DELETE, form và audit trước/sau.
- [ ] Cột announcements version/updated/deleted fields/status (gộp vào migration Bước 14 vì chưa áp dụng). Test stale version 409, foreign class 404, không mất audit.
- [ ] Tạo mới notification một lần; update/delete không gửi class.announcement-posted.
- [ ] Learning Material viewer/download cho actor R2/R3/R4/R5; quản lý môn không sửa học liệu lớp không dạy.
- [ ] Material List, Material Detail, tab Materials, Class Announcements đúng nhãn.

## 7. Tóm tắt học liệu theo yêu cầu trên màn xem - 2026-10-09

Người dùng chốt: chỉ tóm tắt khi người xem bấm Tóm tắt tài liệu; người bấm trên View Material chịu credit; chỉ trừ credit khi gọi AI bên ngoài; là luồng phụ của xem học liệu (UC 15, 34, 55 theo bản 73 UC).

- [ ] **Bước T1** - Cột `lessons.summary` (văn bản ≤ 4 000 ký tự, cho phép rỗng) trong migration Bước 14.
- [ ] **Bước T2** - Port `SummaryPort` + `GeminiSummaryAdapter` (model lấy từ Settings, loại việc `MATERIAL_SUMMARY`) và adapter giả cho test/local.
- [ ] **Bước T3** - `LessonSummarizer`: lấy tối đa 200 000 ký tự đầu, chia đoạn 30 000 ký tự, tóm tắt từng đoạn rồi gộp ≤ 4 000 ký tự (BR-U05-47).
- [ ] **Bước T4** - MaterialSummaryHandler: chỉ chạy sau nút và HOLD requester, tóm tắt trước embedding qua `AiUsagePort.begin(MATERIAL_SUMMARY, …)` trên credit đã giữ; ghi `summary` ngay khi xong; lần tự thử lại có `summary` thì bỏ qua; embedding tính từ `summary` (BR-U05-35, 36, 45, 46).
- [ ] **Bước T5** - `RetrievalService` trả thêm `summary` của mỗi lesson (BR-U05-42).
- [ ] **Bước T6** - API `GET /api/v1/lessons/{id}` trả `summary` (mục 8).
- [ ] **Bước T7** - Frontend: `SummaryBlock` trên Learning Material và Material Detail, SummaryAction với quote/số dư trên View Material, upload không hiện yêu cầu giữ credit.
- [ ] **Bước T8** - Unit test: không chữ/không phụ đề không AI/HOLD; AI tắt trước nhận không nhận yêu cầu, tắt sau nhận thì BUSY; thiếu credit lúc bấm không nhận yêu cầu nhưng giữ học liệu; tự thử lại không tóm tắt lần hai; tài liệu dài được chia đoạn đúng.

## 8. Revision theo bản 73 UC - 2026-10-09

- [ ] Bỏ bình luận dưới thông báo: entity `AnnouncementComment`, bảng `announcement_comments`, API `GET/POST /announcements/{id}/comments`, `POST /announcement-comments/{id}:hide`, `CommentsDialog`, phần bình luận trong `AnnouncementCard`.
- [ ] Thông báo chỉ còn `VISIBLE`/`DELETED`: bỏ `hidden_reason` và API `POST /classes/{classId}/announcements/{announcementId}:hide`; thêm `PATCH`, `DELETE /api/v1/announcements/{id}` kèm `version`.
- [ ] Feed `GET /api/v1/me/announcements?classId=` thay `GET /api/v1/classes/{classId}/announcements` (gộp lớp `OPEN` actor học hoặc dạy).
- [ ] Học liệu: thêm `GET /api/v1/lessons/{id}?classId=`, đổi `POST /classes/{classId}/lessons/{lessonId}/download` thành `POST /api/v1/lessons/{id}/download?classId=`; cột `lessons.uploaded_by`, `uploaded_at`.
- [ ] Trích chữ một lần khi tải lên; AI chỉ sau nút và lỗi tạm tự retry: bỏ `POST /api/v1/lessons/{id}/scan` (Quét lại), bỏ trạng thái `NO_CREDIT`; sweeper gửi lại `BUSY` mỗi 30 phút, quá 24 giờ → `FAILED`.
- [ ] Upload/tạo lesson không kiểm credit; POST lessons/{id}/summary giữ credit qua AiUsagePort.hold, thiếu thì không nhận yêu cầu; GET lessons/{id}/summary-credit lấy quote và số dư requester. U13 thêm `quote`, `hold` (trả `holdId`), `begin(holdId, ...)`, `release(holdId)` vào `AiUsagePort`; U05 phục hồi holdId qua findHold và lưu summary_requested_by/at theo lesson (đã ghi ở plan U13).
- [ ] Sửa học liệu chỉ đổi thông tin (tên, thứ tự); `PATCH /lessons/{id}` không nhận tệp hay link; không có thay tài liệu.
- [ ] `x-roles` mới trong `content.yaml`: bỏ `ADMIN` khỏi mọi endpoint; `GET /classes/{classId}/modules` chỉ cho `TEACHER`, `SUBJECT_MANAGER` (R3/R4).
- [ ] `ContentRefPort` thêm `getLessonRef(lessonId)` (môn, lớp, module, tiêu đề, trạng thái) cho U09, U11; sửa chú thích người dùng thành U06, U08, U09, U11.
- [ ] Màn mới theo screen flow: Material List, Material Detail, tab Materials, Learning Material (trang, không còn popup), Class Announcements (mở từ Class Dashboard); module quản lý trên Material List thay vì Subject Detail.

## 9. Đồng bộ retry/settlement — 2026-10-09

- [ ] Migration Lesson thêm scan_expires_at cố định 24 giờ, scan_retry_at, scan_claim_id, scan_lease_until và scan_retry_count; indexes theo infrastructure-design. Không reset deadline theo scanned_at.
- [ ] LessonScanHandler claim PENDING/BUSY đến hạn bằng CAS, lease 5 phút gia hạn, mọi checkpoint/kết quả kiểm fencing claim; reset PENDING/backoff trước lỗi tạm; tối đa 5 retry/recovery.
- [ ] Sweeper phục hồi lease SCANNING hết hạn, xử lý BUSY đến hạn, terminal FAILED khi hết deadline/retry; worker cũ không ghi/settle muộn.
- [ ] AiUsagePort requestRef riêng HOLD/chunkIndex/MERGE/EMBEDDING; checkpoint complete idempotent trong ai_suggestions.result, dùng lại chunk/summary đã xong, không cộng lần hai; holdId tra bền vững theo lesson.
- [ ] Mọi terminal settle chỉ credit đã dùng thật và trả dư, chưa gọi AI thì trả toàn bộ; embedding FAILED giữ summary nhưng không đưa lesson vào RAG. SettingsPort là cấu hình thật; fake adapter không gọi Gemini.
- [ ] Kiểm các kịch bản BUSY hồi phục, timeout/reset/retry, worker chết sau claim, message trùng, fencing worker cũ, deadline không kéo dài, lỗi embedding sau summary và hold release idempotent. Integration chuyển tester riêng.

## Bổ sung sau recheck 2026-10-09 — contract checkpoint

- [ ] Khai báo HoldSnapshot, UsageStart, CallSnapshot, checkpoint và ticket theo functional domain U05; findHold theo lesson/requestRef; begin phân biệt RUN/REPLAY/BUSY/IN_PROGRESS/CLOSED.
- [ ] Worker chỉ gọi provider khi RUN; REPLAY dùng checkpoint qua DTO. complete/fail kiểm claim/lease trong cùng transaction, truyền scanClaimId; không đọc repository U13.
- [ ] Kiểm restart sau chunk/merge/embedding READY, checkpoint replay không charge thêm, claim cũ bị từ chối và complete cạnh tranh với scanner không ghi vào HOLD đã chốt. Các integration scenarios chuyển tester riêng.

## Revision: nút tóm tắt trên View Material — 2026-10-09

Các bước cũ giả định summary/credit lúc upload được thay bởi yêu cầu hiện hành dưới đây; dấu [x] implementation cũ giữ lịch sử, không xác nhận code mới.

- [ ] Upload MATERIAL và tạo lesson không gọi Gemini/quote/hold, không chặn vì thiếu credit; extraction worker lưu EXTRACTED, không summary/vector.
- [ ] Migration/DTO U05 thêm EXTRACTED, summary_requested_by/at; hai giai đoạn deadline và guard jobType; U03 nhận MATERIAL_SUMMARY trên jobs.gemini.
- [ ] View Material có SummaryAction cho Student/Teacher/Subject Manager theo quyền xem, kể cả Teacher xem học liệu môn chỉ đọc; Admin denied. GET lesson summary-credit/scan và POST summary (classId, Idempotency-Key).
- [ ] Nhận yêu cầu atomically khóa lesson + kiểm quyền xem/AI guard + HOLD của requester + metadata/deadline + enqueue. Student MATERIAL_SUMMARY hợp lệ; child EMBEDDING cùng HOLD, không cho embedding độc lập. Hai actor bấm chỉ một payer; kết quả dùng chung, không charge lại.
- [ ] Worker phục hồi/checkpoint/lease/fencing, một HOLD summary+embedding; AI deadline 24 giờ và HOLD fallback 25 giờ từ yêu cầu; lỗi settle thực dùng/trả dư, giữ summary khi embedding lỗi.
- [ ] Kiểm upload zero-credit/no-AI; 3 role hợp lệ, ngoài scope/Admin denied; hai actor bấm đồng thời/duplicate; late first request sau upload >24 giờ vẫn hợp lệ; thiếu credit/AI guard trước nhận giữ EXTRACTED; no text/caption; cache summary; retry/crash/embedding lỗi; Student không truy xuất RAG hoặc soạn/chấm Graded.

AiUsagePort.checkAvailability(task, actor, target) dùng AiGuard U13, trả allowed/reason trước nhận yêu cầu, không reserve; hold cũng kiểm guard để tránh khoảng trống giữa preflight và nhận. Worker begin kiểm lại guard; nếu bị chặn sau nhận thì BUSY/retry theo deadline. U13 không đọc bảng lesson: U05 truyền target/context đã kiểm, rồi U13 xác minh actor/task/target/HOLD.
