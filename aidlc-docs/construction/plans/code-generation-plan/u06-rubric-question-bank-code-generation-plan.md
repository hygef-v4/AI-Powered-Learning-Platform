# U06 Rubric & Question Bank - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U06. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story trong phạm vi**: US-QBK-001, US-QBK-002 (Scenario 1; Scenario 2, 3 thuộc U08/U11). Phân tích chất lượng câu hỏi không thuộc MVP.
- **Use case**: UC-QBK-01, UC-QBK-02.
- **Thiết kế nguồn**: `construction/u06-rubric-question-bank/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `ArtifactPort`, `FileUploader` | U03 | Dùng thật |
| `ClassAccessPort`, `SubjectScopePort` | U04 | Dùng thật |
| `DocumentModelPort` | U09 (`C`) | Chưa có U09: chỉ kiểm cấu trúc JSON; U09 thay |
| `ContentRefPort` | U05 (`C`) | Chưa có U05: adapter tạm chấp nhận mọi ID (chỉ lưu); U05 thay bằng bản thật |
| `AiDraftPort` | U13 (`C`) | Chưa có U13: ẩn nút "Nhờ AI tạo câu hỏi"; U13 thay bằng bản thật |

### Dữ liệu U06 sở hữu

PostgreSQL `bank_items`.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  questionbank/
    api/                BankController, ImportController, DTO (ManagerView)
    application/        BankItemService, BankScopeGuard, BankQueryService,
                        ImportService, RubricScorer
    domain/             BankItem, BankItemStatus, definition/ (Mcq, Essay, Document,
                        Code, Rubric), DefinitionValidator
    importer/           XlsxRowReader, CsvRowReader, RowMapper (4 loại)
    infrastructure/     BankItemRepository, PermissiveContentRefAdapter
    port/               BankQueryPort, RubricPort, ContentRefPort
/backend/src/main/resources/db/migration/u06/
/backend/src/main/resources/u06/import-templates/   4 file mẫu xlsx + csv
/frontend/src/app/teaching/bank/
/frontend/src/shared/question/        QuestionView (dùng chung với U11)
/contracts/openapi/u06-bank.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - `pom.xml`: Apache POI (`poi-ooxml`), Commons CSV. Biến cấu hình U06 theo `logical-components.md` §3.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain `BankItem` và trạng thái; record `definition` 5 loại với Jackson polymorphic (P1, P2).
- [ ] **Bước 3** - `DefinitionValidator` hai mức cho 4 loại câu và rubric (BR-U06-20…27, 30, 31).
- [ ] **Bước 4** - Port `BankQueryPort`, `RubricPort`, `ContentRefPort`; `PermissiveContentRefAdapter`.
- [ ] **Bước 5** - `BankScopeGuard` (BR-U06-01…04).
- [ ] **Bước 6** - `BankItemService`: tạo/sửa nháp, bản nháp mới từ `ACTIVE`, kích hoạt, ngưng, xóa nháp, nhân bản, audit (F1-F4, BR-U06-10…16, 50).
- [ ] **Bước 7** - `RubricScorer` (P4, BR-U06-32).
- [ ] **Bước 8** - `BankQueryService`: tìm kiếm bản `ACTIVE` mới nhất, lịch sử, xem trước, `getVersion`, `getLearnerView` (F5, F7, P5, P6).
- [ ] **Bước 9** - Nhập file: `XlsxRowReader`, `CsvRowReader`, `RowMapper` 4 loại, `ImportService` mỗi dòng một transaction (F6, BR-U06-40…43, P3).
- [ ] **Bước 10** - 4 file mẫu nhập (xlsx và csv) theo BR-U06-42.
- [ ] **Bước 11** - Unit test mọi `BR-U06-xx`: từng loại câu, rubric, điểm `BigDecimal`, bản `ACTIVE` không sửa được, file nhập lỗi/zip bomb/CSV sai mã hóa.
- [ ] **Bước 12** - Tóm tắt: `aidlc-docs/construction/u06-rubric-question-bank/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 13** - Flyway `V20260925_1300__u06_bank_items.sql` theo `infrastructure-design.md` §2.
- [ ] **Bước 14** - `BankItemRepository` với query `DISTINCT ON` và lọc tag GIN.
- [ ] **Bước 15** - Integration test Testcontainers: hai người tạo bản nháp cùng lúc → một `409`; tìm kiếm chỉ trả bản `ACTIVE` mới nhất; nhập 500 dòng ≤ 10 s.
- [ ] **Bước 16** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 17** - `/contracts/openapi/u06-bank.yaml` (endpoint theo `frontend-components.md`).
- [ ] **Bước 18** - Controller + DTO + validation; lỗi `{field, message}`.
- [ ] **Bước 19** - Test MockMvc: giảng viên không sửa ngân hàng cấp môn, không thấy ngân hàng lớp khác, người học không gọi được API U06.
- [ ] **Bước 20** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 21** - `BankPage`, `BankFilters`, `BankItemTable`, `VersionHistoryDrawer`, `CloneDialog`.
- [ ] **Bước 22** - `QuestionEditor` (4 loại + `ClassificationFields`) và `RubricEditor` (tổng điểm tự tính).
- [ ] **Bước 23** - `QuestionView` dùng chung và `PreviewDialog` (bật/tắt đáp án).
- [ ] **Bước 24** - `ImportDialog` (tải mẫu, kết quả từng dòng).
- [ ] **Bước 25** - Test frontend: MCQ 1 đáp án chặn chọn 2, tổng điểm rubric, bảng kết quả nhập.
- [ ] **Bước 26** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 27** - Cập nhật `README.md`: dùng file mẫu nhập, cách unit khác dùng `BankQueryPort`/`RubricPort`.
- [ ] **Bước 28** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-QBK-001 (UC-QBK-01) | 2, 3, 6, 7, 22 |
| US-QBK-002 S1 (UC-QBK-02) | 2, 3, 6, 8, 9, 10, 21-24 |
| Contract cho U08-U15 | 4, 7, 8, 23 |

## 5. Ngoài phạm vi

- US-QBK-002 S2, S3 thuộc U08/U11; phân tích chất lượng câu hỏi ngoài phạm vi dự án.
- Chạy test case Code Lab (U13), chấm theo rubric (U15).
