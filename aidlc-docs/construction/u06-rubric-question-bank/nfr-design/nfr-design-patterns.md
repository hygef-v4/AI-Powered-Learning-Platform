# U06 Rubric & Question Bank - NFR Design Patterns

## P1 - Phiên bản bất biến
- Entity `BankItem` chỉ có phương thức sửa `definition` khi `status = DRAFT`; `activate()`, `retire()` đổi trạng thái.
- Repository không có `update` tự do; `newDraftFrom(activeId)` sao chép sang dòng mới (NFR-U06-11).
- Tạo bản nháp đồng thời: unique partial index 1 `DRAFT`/`stableKey`; vi phạm → `409` "đã có bản nháp".

## P2 - `definition` đa hình
- Jackson `@JsonTypeInfo(property = "questionType")` ánh xạ `McqDefinition`, `EssayDefinition`, `DrawioDefinition`, `CodeDefinition`, `RubricDefinition`.
- `DefinitionValidator` theo loại, hai mức: `DRAFT` (định dạng, độ dài) và `ACTIVATE` (đầy đủ BR-U06-20…31).
- Lỗi trả danh sách `{field, message}` để frontend gắn vào trường.

## P3 - Nhập file theo luồng
1. `ImportReader` theo đuôi: `XlsxRowReader` (POI event API, `ZipSecureFile.setMinInflateRatio(0.01)`, `setMaxEntrySize(20 MB)`, đọc giá trị đã tính, không đánh giá công thức) hoặc `CsvRowReader` (Commons CSV, bỏ BOM).
2. Dừng khi > 500 dòng hoặc > 5 MB.
3. `RowMapper` theo loại (4 bộ cột, BR-U06-42) → `definition` → `DefinitionValidator` mức `ACTIVATE` (câu vẫn lưu `DRAFT`).
4. `TransactionTemplate` mỗi dòng; `DRAWIO` gọi `ArtifactPort.store` cho XML mẫu trước khi lưu (NFR-U06-13, 23).

## P4 - Tính điểm rubric
- `RubricScorer.score(definition, checkedItemIds)` hàm thuần, dùng `BigDecimal` scale 2; ID lạ → lỗi (BR-U06-32, NFR-U06-12).

## P5 - Tìm kiếm
- Query "bản `ACTIVE` mới nhất mỗi `stableKey`" bằng `DISTINCT ON (stable_key) ORDER BY stable_key, version_no DESC` có lọc; GIN `tags`; `ILIKE` tiêu đề (NFR-U06-01).

## P6 - Ẩn đáp án
- DTO tách: `BankItemManagerView` (đủ) cho API của U06; `QuestionLearnerView` (bỏ `correctOptionIds`, `answerGuide`, test ẩn) do U06 cung cấp qua `BankQueryPort.getLearnerView(id)` để U11 dùng (NFR-U06-22).
