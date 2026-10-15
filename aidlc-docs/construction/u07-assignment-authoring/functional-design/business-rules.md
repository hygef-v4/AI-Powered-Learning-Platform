# U07 Assignment Authoring — business-rules

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U07 Assignment Authoring thuộc Iter 2, người code: Người 4; UC 34–36. Lấy phần Assignment của U08 cũ (vòng đời, phát hành), U09 cũ (tài liệu, khung, DOCX) và rubric của U06 cũ. Quiz và ngân hàng thuộc U10. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Loại bài, phạm vi và quyền

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-01 | U07 soạn loại bài `ASSIGNMENT`; Quiz thuộc U10. Assignment có `submissionMode` (`INDIVIDUAL`, `GROUP`) và `diagramEnabled` độc lập, đủ bốn tổ hợp; `gradingMode` là `GRADED` hoặc `PRACTICE`, bài nhóm chỉ `GRADED` (D02). Không có loại Essay, Diagram, Group, Code riêng. | FR-007, FR-017, US-ASM-001 |
| BR-U07-02 | Bài của lớp: giảng viên chính của lớp (R3/R4) soạn từ tab Evals. Bài của môn: Chủ nhiệm môn (R2) soạn từ Assignment List, chỉ `INDIVIDUAL`, giao cho mọi lớp `OPEN` của môn theo một lịch chung, kể cả lớp mở sau khi phát hành; giảng viên từng lớp chấm lớp mình. Giảng viên các lớp chỉ xem bài của môn. Admin không soạn, không xem. | FR-027, US-ASM-009 |
| BR-U07-03 | `GROUP` chỉ có ở bài của lớp và chỉ dùng được khi U12 đã cài `GroupAssignmentSupport`; chưa có thì form không cho chọn, API từ chối. UC 53 của U12 dùng chính Assignment Form này với `GROUP`. | FR-026, UC 53 |
| BR-U07-04 | Lớp hoặc môn đã `ARCHIVED`: bài chỉ đọc (không tạo, sửa, phát hành). | BR-U05-16, BR-U04-07 |

## 2. Đề và khung tài liệu

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-10 | Đề gồm tiêu đề (≤ 200 ký tự), hướng dẫn (≤ 20.000 ký tự, định dạng cơ bản) và khung tài liệu. Khung là danh sách block: heading (Tiêu đề, Phụ đề, Tiêu đề 1–6 như Word), đoạn văn, danh sách, bảng, ảnh (U03 `DOCUMENT_IMAGE`), sơ đồ Draw.io (chỉ khi `diagramEnabled`). | FR-017, US-ASM-004 |
| BR-U07-11 | Phần là heading nhỏ nhất của mỗi nhánh trong cây Tiêu đề 1–6. Ví dụ H1 có nhánh H2.1 chứa H3.1, H3.2 và nhánh H2.2 không có H3 thì các phần là H3.1, H3.2, H2.2. Block trước heading đầu tiên, Tiêu đề, Phụ đề là phần chung; khung không có heading là một phần. Hệ thống tự tính; `partId` lấy theo id block heading nên giữ qua các lần sửa. | Người dùng chốt 2026-10-04 |
| BR-U07-12 | Sơ đồ lưu XML Draw.io đầy đủ kèm SVG xem trước đã làm sạch. XML qua parser an toàn (không DOCTYPE, XXE, XInclude; gốc `mxfile` hoặc `mxGraphModel`). Trần: ≤ 2.000 block, ≤ 100 sơ đồ, mỗi XML ≤ 2 MB, cả khung ≤ 20 MB. Sơ đồ luôn hiện bằng ảnh xem trước; bấm vào mở Draw.io nhúng. | US-ASM-004 S2 |
| BR-U07-13 | `requiredDiagrams` (tùy chọn, chỉ khi bật sơ đồ): loại sơ đồ (`USE_CASE`, `CLASS`, `SEQUENCE`, `ACTIVITY`, `ER`, `COMPONENT`, `STATE`, `OTHER`) → số tối thiểu 1–20; U08, U12 kiểm lúc nộp. | FR-017 |
| BR-U07-14 | Tắt `diagramEnabled` khi khung còn sơ đồ, còn `requiredDiagrams` hoặc tiêu chí sơ đồ thì từ chối và liệt kê chỗ cần bỏ. | US-ASM-004 S1 |
| BR-U07-15 | Khung khóa với người học: không sửa, xóa, di chuyển block của người soạn; người học chèn block của mình ở bất kỳ vị trí nào. U08, U12 kiểm qua `DocumentModelPort`. | FR-017 |
| BR-U07-16 | `activityMonitoringEnabled` mặc định tắt, đặt trước khi phát hành kèm thông báo cho người học; chỉ bật được khi Admin đã đặt `activity.retentionDays` (đọc qua `SettingsPort`); muốn đổi sau phát hành phải tạo version mới. | UC 35, BR-U08-54 |
| BR-U07-17 | Tùy chọn gắn tối đa 10 tài liệu tham khảo (khi U13 có), chọn qua `ReferenceMaterialPort.listSelectable` của U06: bài của môn chỉ chọn tài liệu của môn; bài của lớp chọn tài liệu của môn và của lớp. Người học thấy trên Assignment Detail (U13 hiện qua slot của U08). | FR-004 |

## 3. Rubric (UC 36)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-20 | Mỗi phần có đúng một rubric, tự tạo trống khi phần xuất hiện, tự xóa khi phần mất (bài còn nháp). Không có nút tạo hay xóa rubric riêng; người soạn thêm, sửa, xóa tiêu chí và mục trong Rubric Detail; lưu là ghi đè. Vì luôn có ít nhất một phần nên bài nào cũng có rubric. | US-QBK-001 S1 |
| BR-U07-21 | Rubric hợp lệ: 1–20 tiêu chí, mỗi tiêu chí 1–20 mục checklist; mỗi mục điểm > 0, tối đa 2 chữ số thập phân. Tiêu chí loại `TEXT` hoặc `DIAGRAM`; `DIAGRAM` chỉ khi bật sơ đồ. Bài còn nháp thì rubric được trống hoặc chưa đủ. | UC 36 |
| BR-U07-22 | Điểm tối đa của phần là tổng điểm các mục (tự tính, không nhập tay); điểm tối đa của bài là tổng các phần. Khi chấm, mỗi mục đạt hoặc không; điểm là tổng mục đạt (U09 gọi `RubricPort.score`). | US-QBK-001 S1 |
| BR-U07-23 | Rubric thuộc đúng một bài. Phát hành thì mọi rubric của bài bị khóa; rubric đã khóa không sửa, không xóa. | US-QBK-001 S2 |
| BR-U07-24 | Duyệt hay phát hành khi còn rubric trống hoặc chưa hợp lệ thì từ chối và chỉ ra phần cần điền; Assignment Form cảnh báo trên phần có rubric trống. | US-QBK-001 S2a |
| BR-U07-25 | Nhân bản, version mới, copy sang lớp khác: rubric được nhân bản thành rubric mới, chưa khóa, của bài đích; hai bài không dùng chung rubric. | US-QBK-001 S3 |

## 4. Vòng đời

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-30 | Trạng thái `DRAFT → REVIEWED → SCHEDULED → OPEN → CLOSED`; `SCHEDULED` hoặc `OPEN` → `RETIRED`. Chỉ sửa nội dung khi `DRAFT`; sửa bài `REVIEWED` đưa về `DRAFT`. | FR-007 |
| BR-U07-31 | Xóa: chỉ bài `DRAFT` hoặc `REVIEWED` (chưa từng phát hành), xóa hẳn kèm rubric và đề xuất AI; bài đã phát hành dùng Ngưng giao. | UC 35 |
| BR-U07-32 | Duyệt: chính người có quyền tự duyệt sau khi xem trước. Kiểm tiêu đề, khung hợp lệ, mọi rubric hợp lệ, sơ đồ đúng cờ; bài `GROUP` cần có `GroupAssignmentSupport`. | US-ASM-001 |
| BR-U07-33 | Phát hành từ `REVIEWED`: `opensAt < closesAt`, `maxAttempts` 1–10, `lateUntil` tùy chọn (`closesAt < lateUntil ≤ closesAt + 30 ngày`). Bài của lớp cần lớp `OPEN`; bài của môn cần môn `ACTIVE`; bài `GROUP` cần U12 báo nhóm sẵn sàng (lỗi thì chặn; sinh viên chưa có nhóm thì người phát hành phải xác nhận). Một transaction: ghi lịch, khóa rubric, chuyển `SCHEDULED`. | FR-007 |
| BR-U07-34 | Mở, đóng tự động theo lịch, trễ ≤ 1 phút: tới `opensAt` thì `OPEN`, gọi `onOpened` và phát `assignment.opened`; tới `lateUntil` (hoặc `closesAt` nếu không có) thì `CLOSED`. Nộp sau `closesAt` mà trước `lateUntil` là nộp trễ. | FR-007 |
| BR-U07-35 | Sửa lịch: `SCHEDULED` sửa tự do theo BR-U07-33; `OPEN` chỉ kéo dài `closesAt`, `lateUntil`, không rút ngắn, không đổi `maxAttempts`. Bài của môn chỉ Chủ nhiệm môn sửa lịch. | Thiết kế |
| BR-U07-36 | Ngưng giao: `SCHEDULED` hoặc `OPEN` → `RETIRED`, lý do bắt buộc. Người học không bắt đầu, không nộp thêm; bài đã nộp giữ nguyên; `onRetired` của U08, U12 (tự nộp bài đang làm) chạy trong cùng transaction. Bài của môn chỉ Chủ nhiệm môn ngưng giao. | US-ASM-008 |
| BR-U07-37 | Người học thấy bài `OPEN`, `CLOSED` của lớp `OPEN` mình đang ghi danh, gồm bài của lớp và bài của môn chứa lớp. `DRAFT`, `REVIEWED`, `SCHEDULED` ẩn; `RETIRED` ẩn trừ khi người học đã có kết quả ở bài đó (U08 quyết theo dữ liệu bài làm). | FR-007 |

## 5. Nhân bản, version và copy

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-40 | Nhân bản: bài `DRAFT` mới cùng lớp hoặc cùng môn, `origin = CLONE`. | FR-028 |
| BR-U07-41 | Version mới: từ bài `CLOSED` hoặc `RETIRED`, tạo `DRAFT` có `versionNo` tăng 1, `sourceAssignmentId` là bài cũ, `origin = NEW_VERSION`. Mỗi bài nguồn tối đa một version đang `DRAFT` hoặc `REVIEWED`. Bài cũ giữ nguyên cho bài nộp cũ. | US-ASM-008 |
| BR-U07-42 | Copy giữa lớp: chỉ bài của lớp; giảng viên phải dạy cả lớp nguồn và lớp đích; tạo `DRAFT` ở lớp đích, `origin = CLASS_COPY`. Lớp ngoài quyền thì "không tìm thấy". | US-ASM-010 |
| BR-U07-43 | Ba cách trên đều sao `submissionMode`, `diagramEnabled`, `gradingMode`, `requiredDiagrams`, theo dõi hoạt động, tài liệu gắn kèm (copy giữa lớp bỏ tài liệu riêng của lớp nguồn), hướng dẫn, khung và rubric (mới). Không sao lịch, nhóm, đề xuất AI, kết nối AI, lượt làm, bài nộp, phản hồi, điểm; không đồng bộ hai chiều. | FR-028, US-ASM-010 S2 |

## 6. AI và MCP

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-50 | AI soạn bản nháp cho bài `DRAFT`: người soạn nhập yêu cầu, chọn tài liệu tham khảo được phép (khi U13 có), chọn nguồn AI trong popup của U06. AI đề xuất hướng dẫn, cây heading Tiêu đề 1–6 kèm đoạn hướng dẫn dưới mỗi heading và gợi ý rubric cho từng phần; không có sơ đồ. | FR-006, US-AIG-001, 002 |
| BR-U07-51 | Đề xuất là bản nháp có provenance (người yêu cầu, nguồn, provider, model khi biết, tài liệu và version). Người soạn xem trước; Áp dụng thì thay toàn bộ khung (cảnh báo khi khung đã có nội dung) và hướng dẫn nếu chọn, tính lại phần, điền gợi ý rubric vào rubric của phần để sửa tiếp; cần `version` của bài không đổi từ lúc xem. Bỏ thì bài không đổi. AI không tự duyệt hay phát hành. | FR-006 |
| BR-U07-52 | Prompt chỉ có yêu cầu, cài đặt bài và chữ của tài liệu đã chọn (cắt theo trần ký tự); không có dữ liệu người học. Không có AI hoặc hết quota vẫn soạn thủ công được. | FR-006 |
| BR-U07-53 | Tool MCP: `read_assignment_context` (giảng viên có quyền đọc bản nháp và rubric; người học đọc bài `OPEN`, `CLOSED` của lớp mình đúng như Assignment Detail) và `save_assignment_draft` (chỉ giảng viên có quyền, bài `DRAFT`, kèm `expectedVersion`). Tool ghi chỉ lưu thành đề xuất chờ người soạn áp dụng như BR-U07-51, không sửa thẳng bài. | FR-035 |

## 7. DOCX

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-60 | Xuất đề ra DOCX: tiêu đề, hướng dẫn, khung (heading theo style Word, đoạn, danh sách, bảng, ảnh), tùy chọn kèm rubric. Mỗi sơ đồ thành ảnh PNG kèm ba thứ: alt text "mô tả [drawio:assignmentId/blockId]", hyperlink trên ảnh tới trang xem sơ đồ (kiểm quyền), và khối `mxfile` trong PNG. | Thiết kế |
| BR-U07-61 | Nhập DOCX (≤ 20 MB) vào khung bài `DRAFT`: Title, Subtitle, Heading 1–6 thành heading; đoạn, danh sách, bảng, ảnh thành block của người soạn. Mỗi ảnh tìm sơ đồ lần lượt: mã trong alt text, hyperlink, khối `mxfile` của PNG hoặc thuộc tính `content` của SVG; không có thì giữ là ảnh. Sơ đồ theo mã chỉ lấy khi người nhập có quyền đọc bài nguồn. Bài tắt sơ đồ thì sơ đồ được nhập thành ảnh. Không dùng AI. | US-ASM-004 |
| BR-U07-62 | Nội dung không hỗ trợ (textbox, SmartArt, công thức, header, footer) bị bỏ và liệt kê trong báo cáo. Kết quả là bản xem trước; người soạn chọn thay khung hoặc thêm vào cuối rồi mới lưu. File DOCX chỉ xử lý trong bộ nhớ, không lưu. | Thiết kế |

## 8. Phân quyền, audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-70 | Mọi API kiểm quyền phía server; ngoài phạm vi trả `404` và ghi audit `ACCESS_DENIED`. Sửa bài, rubric, lịch phải kèm `version`; lệch thì `409`. | SEC-002 |
| BR-U07-71 | Audit: `ASSIGNMENT_CREATED`, `ASSIGNMENT_DELETED`, `ASSIGNMENT_REVIEWED`, `ASSIGNMENT_PUBLISHED` (lớp hoặc môn, lịch), `ASSIGNMENT_SCHEDULE_CHANGED`, `ASSIGNMENT_RETIRED` (lý do), `ASSIGNMENT_CLONED`, `ASSIGNMENT_VERSION_CREATED`, `ASSIGNMENT_COPIED` (nguồn, đích), `RUBRIC_UPDATED`, `ASSIGNMENT_AI_DRAFT_APPLIED` (provenance), `ASSIGNMENT_DOCX_IMPORTED`. Không ghi nội dung đề. | FR-014 |
| BR-U07-72 | U01, U04, U05 không trả lời thì từ chối (fail closed). AI, U12, U13 lỗi không chặn việc soạn thủ công. | BR-U01-93 |
