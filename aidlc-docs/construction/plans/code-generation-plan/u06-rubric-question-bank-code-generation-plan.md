# U06 Rubric & Question Bank - Code Generation Plan

**Bản tài liệu 2026-10-09**: UC 46, 56, 57 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-QBK-001, US-QBK-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U06. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story trong phạm vi**: US-QBK-001 (rubric), US-QBK-002 (ngân hàng câu hỏi; Scenario 3 thuộc U08/U11). Phân tích chất lượng câu hỏi không thuộc MVP.
- **Primary UC hiện hành**: UC 46, 56, 57 theo bản 73 UC. Supporting flows theo current-srs-contract.md.
- **Quyết định 2026-10-09**: ngân hàng câu hỏi chỉ ở cấp môn, giữ câu của mọi dạng bài (trắc nghiệm, tự luận, tài liệu, code) cho quiz và bài tập; không có ngân hàng của lớp và nhân bản lớp↔môn; rubric thuộc từng bài, sửa trực tiếp khi bài còn nháp, khóa khi phát hành, không có phiên bản.
- **Thiết kế nguồn**: `construction/u06-rubric-question-bank/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `SubjectScopePort`, `ClassScopePort`, `ClassAccessPort` | U04 | Dùng thật |
| `ContentRefPort` | U05 (`C`) | Chưa có U05: adapter tạm chấp nhận mọi ID (chỉ lưu); U05 thay bằng bản thật |
| `DocumentModelPort` | U09 (`C`) | Chưa có U09: chỉ kiểm cấu trúc JSON; U09 thay |
| `ArtifactPort`, `FileUploader` | U03 | Dùng thật (ảnh trong khung tài liệu) |
| `AiDraftPort` | U13 (`C`) | Chưa có U13: ẩn nút "Nhờ AI soạn câu hỏi"; U13 thay bằng bản thật |

### Dữ liệu U06 sở hữu

PostgreSQL `questions` (câu hỏi mọi loại, có phiên bản), `rubrics` (rubric của từng bài, không có phiên bản).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  questionbank/
    api/                QuestionController, ImportController, DTO (ManagerView)
    application/        QuestionService, QuestionScopeGuard, QuestionQueryService,
                        ImportService, RubricService, RubricScorer
    domain/             Question, QuestionStatus, definition/ (Mcq, Essay, Document, Code),
                        Rubric, RubricDefinition, DefinitionValidator
    importer/           XlsxRowReader, CsvRowReader, RowMapper (4 loại)
    infrastructure/     QuestionRepository, RubricRepository, PermissiveContentRefAdapter
    port/               BankQueryPort, InlineQuestionPort, RubricPort, ContentRefPort
/backend/src/main/resources/db/migration/questionbank/
/backend/src/main/resources/questionbank/import-templates/   4 file mẫu xlsx + csv
/frontend/src/app/manager/questions/
/frontend/src/shared/question/        QuestionEditor, QuestionView, QuestionPicker (dùng chung với U08, U09, U11, U15)
/frontend/src/shared/rubric/          RubricEditor, RubricView (dùng chung với U09, U11, U15)
/contracts/openapi/question-bank.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - `pom.xml`: Apache POI (`poi-ooxml`), Commons CSV. Biến cấu hình U06 theo `logical-components.md` §3.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain `Question` và trạng thái, record `definition` 4 loại với Jackson polymorphic; `Rubric` (có `assignment_id`, `target_ref`, `locked_at`), record `RubricDefinition` (P1, P2).
- [ ] **Bước 3** - `DefinitionValidator` hai mức cho 4 loại câu và kiểm rubric (BR-U06-20…28, 30, 31); suy dạng bài dùng được từ `questionType`.
- [ ] **Bước 4** - Port cung cấp: `BankQueryPort` (`search`, `pickRandom` BR-U08-18, `getVersion`, `getStudentView`), `InlineQuestionPort` (U08), `RubricPort` (`create`, `update`, `delete`, `lockForAssignment`, `cloneForAssignment`, `getRubric`, `score`, BR-U06-32…36). Port khai báo: `ContentRefPort` (`PermissiveContentRefAdapter`).
- [ ] **Bước 5** - `QuestionScopeGuard` (BR-U06-01…04): R2 quản lý ngân hàng môn; giảng viên một lớp của môn chỉ đọc câu `ACTIVE`.
- [ ] **Bước 6** - `QuestionService`: tạo/sửa nháp, bản nháp mới từ `ACTIVE`, kích hoạt, ngưng dùng, xóa nháp chưa từng kích hoạt, audit (F3-F5, BR-U06-10…16, 50).
- [ ] **Bước 7** - `RubricService` và `RubricScorer`: tự tạo rubric trống theo câu/phần, sửa ghi đè, tự xóa khi chưa khóa, kiểm đủ rồi khóa theo bài (từ chối khi còn rubric trống), nhân bản cho bài đích, tính điểm (F8, P4, P7, BR-U06-30…37).
- [ ] **Bước 8** - `QuestionQueryService`: Question List (bản `ACTIVE` mới nhất, cờ có bản nháp, câu chỉ có bản nháp), lịch sử, xem trước, `getVersion`, `getStudentView` (F1, F2, F9, P5, P6).
- [ ] **Bước 9** - Nhập file: `XlsxRowReader`, `CsvRowReader`, `RowMapper` 4 loại, `ImportService` mỗi dòng một transaction (F6, BR-U06-40…42, P3).
- [ ] **Bước 10** - 4 file mẫu nhập (xlsx và csv) theo BR-U06-42.
- [ ] **Bước 11** - Unit test mọi `BR-U06-xx`: từng loại câu, rubric, điểm `BigDecimal`, bản `ACTIVE` không sửa được, rubric đã khóa không sửa/xóa được, khóa bị từ chối khi còn rubric trống, file nhập lỗi/zip bomb/CSV sai mã hóa.
- [ ] **Bước 12** - Tóm tắt: `aidlc-docs/construction/u06-rubric-question-bank/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 13** - Flyway `db/migration/questionbank/V20260925_1300__create_questions_rubrics.sql` theo `infrastructure-design.md` §2.
- [ ] **Bước 14** - `QuestionRepository` với query `DISTINCT ON` và lọc tag GIN; `RubricRepository` với cập nhật có điều kiện `locked_at IS NULL`.
- [ ] **Bước 15** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test Testcontainers: hai người tạo bản nháp cùng lúc → một `409`; tìm kiếm chỉ trả bản `ACTIVE` mới nhất; nhập 500 dòng ≤ 10 s; sửa rubric sau khi khóa → `409`.
- [ ] **Bước 16** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 17** - `/contracts/openapi/question-bank.yaml` (endpoint theo `frontend-components.md`); cần sửa theo mục 7.
- [ ] **Bước 18** - Controller + DTO + validation; lỗi `{field, message}`.
- [ ] **Bước 19** - Test MockMvc: giảng viên không sửa được ngân hàng môn và chỉ thấy câu `ACTIVE`, Chủ nhiệm môn không thấy ngân hàng môn khác, Admin và người học không gọi được API U06.
- [ ] **Bước 20** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 21** - Màn Question List (`QuestionListPage`, `SubjectPicker`, `QuestionFilters`, `QuestionTable`, `ImportDialog`, `AiQuestionDraftDialog`).
- [ ] **Bước 22** - Màn Question Detail (`QuestionDetailPage`, `QuestionEditor` 4 loại, `ClassificationFields`, `QuestionActions`, `VersionHistoryPanel`, `PreviewDialog`).
- [ ] **Bước 23** - Component dùng chung: `QuestionView`, `QuestionEditor`, `QuestionPicker` (U08 gắn vào Quiz Detail và Assignment Form), `RubricEditor`, `RubricView` (U09 gắn vào popup Rubric Detail).
- [ ] **Bước 24** - Test frontend: MCQ một đáp án chặn chọn 2, tổng điểm rubric, rubric đã khóa chỉ đọc, bảng kết quả nhập.
- [ ] **Bước 25** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 26** - Cập nhật `README.md`: dùng file mẫu nhập, cách unit khác dùng `BankQueryPort`/`RubricPort`.
- [ ] **Bước 27** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-QBK-001 (UC 46) | 2, 3, 4, 7, 23 |
| US-QBK-002 S1, S1a, S2, S4, S5 (UC 56, 57) | 2, 3, 5, 6, 8, 9, 10, 21, 22 |
| Contract cho U08-U15 | 4, 7, 8, 23 |

## 5. Ngoài phạm vi

- US-QBK-002 S3 thuộc U08/U11; phân tích chất lượng câu hỏi ngoài phạm vi dự án.
- Chạy test case Code Lab và lưu kết quả kiểm lời giải (U13); chấm theo rubric (U15).

## 6. Revision implementation scope - 2026-10-08
- [ ] Teacher chỉ dùng câu ACTIVE cấp môn trong selector soạn bài/quiz, không quản trị ngân hàng.
- [ ] Xóa câu: bản nháp chưa dùng xóa, bản từng ACTIVE/tham chiếu RETIRED; test giữ phiên bản đã ghim.

## 7. Revision theo bản 73 UC - 2026-10-09

- [ ] Bỏ ngân hàng của lớp: `scope_type = CLASS` của `questions`, nhân bản câu hỏi (`POST /bank/items/{id}/clone`, `CloneDialog`), `BankCopyPort`.
- [ ] Giữ 4 loại câu và bộ lọc theo dạng bài (người dùng chốt lại 2026-10-09: ngân hàng dùng cho mọi dạng bài); bỏ `QuestionVerificationPort` (U13 lưu kết quả kiểm lời giải theo `contentHash` của phiên bản câu `CODE`).
- [ ] Rubric không có phiên bản: bỏ `lineage_id`, `version`, `status` của `rubrics`, `RubricPort.revise`, `RubricOwnerPort`; thêm `assignment_id`, `target_ref`, `locked_at`, `RubricPort.update`, `delete`, `lockForAssignment` (kiểm đủ trước khi khóa); `create` tạo rubric trống; rubric bắt buộc, tự tạo theo câu Text Essay hoặc phần Diagram Essay/bài nhóm; Question Bank không còn rubric.
- [ ] Đổi API `question-bank.yaml`: `/api/v1/bank/items...` thành `/api/v1/subjects/{subjectId}/questions`, `/api/v1/questions/{id}` (`GET`, `PATCH`, `DELETE`), `/activate`, `/retire`, `/versions`, `/preview`; `/api/v1/question-import-template`, `/api/v1/subjects/{subjectId}/question-imports`; `x-roles` chỉ `SUBJECT_MANAGER` (giảng viên chỉ đọc bản `ACTIVE`), bỏ `ADMIN`.
- [ ] Đổi tên code: `BankItemService` → `QuestionService`, `BankQueryService` → `QuestionQueryService`; route frontend `app/teaching/bank/` → `app/manager/questions/`.
- [ ] Ảnh hưởng unit khác (sửa ở lượt của unit đó): U08 bỏ `RubricOwnerPort`, gọi `RubricPort.lockForAssignment` khi phát hành (từ chối phát hành khi còn rubric trống), thêm FK `rubrics.assignment_id`; U09 tự tạo rubric khi thêm câu Text Essay hoặc chia phần và tự xóa khi bỏ câu hoặc phần, API rubric chỉ còn xem và lưu; bỏ `BankCopyPort` (U10 đã xóa, copy giữa lớp ở U08 giữ tham chiếu câu ngân hàng môn); U13 tự lưu kết quả kiểm lời giải mẫu Code Lab theo `contentHash` của phiên bản câu `CODE` thay cho `QuestionVerificationPort`.
