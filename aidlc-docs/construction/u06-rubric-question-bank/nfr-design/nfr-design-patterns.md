# U06 Rubric & Question Bank - NFR Design Patterns

**Bản tài liệu 2026-10-09**: UC 46, 56, 57 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-QBK-001, US-QBK-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Phiên bản câu hỏi bất biến
- Entity `Question` chỉ có phương thức sửa `definition` khi `status = DRAFT`; `activate()`, `retire()` đổi trạng thái.
- Repository không có `update` tự do; `newDraftFrom(activeId)` sao chép sang dòng mới (NFR-U06-11).
- Tạo bản nháp đồng thời: unique partial index 1 `DRAFT`/`lineage_id` trên `questions`; vi phạm → `409` "đã có bản nháp".

## P2 - Kiểm `definition`
- Jackson `@JsonTypeInfo(property = "questionType")` ánh xạ `McqDefinition`, `EssayDefinition`, `DocumentDefinition`, `CodeDefinition`; `rubrics.criteria` sang `RubricDefinition`.
- `DefinitionValidator` hai mức: `DRAFT` (định dạng, độ dài) và `ACTIVATE` (đầy đủ BR-U06-20…28); rubric kiểm BR-U06-30, 31 mỗi lần lưu.
- Lỗi trả danh sách `{field, message}` để frontend gắn vào trường.

## P3 - Nhập file theo luồng
1. `ImportReader` theo đuôi: `XlsxRowReader` (POI event API, `ZipSecureFile.setMinInflateRatio(0.01)`, `setMaxEntrySize(20 MB)`, đọc giá trị đã tính, không đánh giá công thức) hoặc `CsvRowReader` (Commons CSV, bỏ BOM).
2. Dừng khi > 500 dòng hoặc > 5 MB.
3. `RowMapper` theo loại (4 bộ cột, BR-U06-42) → `definition` → `DefinitionValidator` mức `ACTIVATE` (câu vẫn lưu `DRAFT`).
4. `TransactionTemplate` mỗi dòng (NFR-U06-13).

## P4 - Tính điểm rubric
- `RubricScorer.score(definition, checkedItemIds)` hàm thuần, dùng `BigDecimal` scale 2; ID lạ → lỗi (BR-U06-32, NFR-U06-12).

## P5 - Tìm kiếm
- Query "bản `ACTIVE` mới nhất mỗi `lineage_id`" bằng `DISTINCT ON (lineage_id) ORDER BY lineage_id, version DESC` có lọc; Question List gộp thêm câu chỉ có bản nháp; GIN `tags`; `ILIKE` tiêu đề (NFR-U06-01).
- Giảng viên (chọn câu trong Quiz Detail) luôn bị ép `status = ACTIVE`.

## P6 - Ẩn đáp án
- DTO tách: `QuestionManagerView` (đủ) cho API của U06; `QuestionStudentView` (bỏ `correctOptionIds`, `explanation`, `answerGuide`, test ẩn, `referenceFiles`) do U06 cung cấp qua `BankQueryPort.getStudentView(id)` để U11 dùng (NFR-U06-22).

## P7 - Khóa rubric
- `lockForAssignment(assignmentId)`: đọc mọi rubric của bài, rubric nào trống hoặc không hợp lệ (BR-U06-30) thì ném lỗi kèm danh sách `target_ref`; đủ thì `UPDATE rubrics SET locked_at = now() WHERE assignment_id = :id AND locked_at IS NULL`, chạy trong transaction phát hành của U08 (BR-U06-37).
- `update`, `delete`: câu lệnh có điều kiện `locked_at IS NULL`; không dòng nào đổi → `409` "rubric đã khóa" (NFR-U06-14).
