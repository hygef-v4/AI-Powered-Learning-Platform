# U07 Assignment Authoring — business-rules

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-22 theo code hiện tại (các yêu cầu của người dùng 2026-10-21, 2026-10-22; chi tiết từng vòng trong code-summary).

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Loại bài, phạm vi và quyền

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-01 | U07 soạn loại bài `ASSIGNMENT`; Quiz thuộc U10. Assignment có `submissionMode` (`INDIVIDUAL`, `GROUP`) và `gradingMode` (`GRADED`, `PRACTICE`; bài nhóm chỉ `GRADED`, D02). Sơ đồ Draw.io luôn dùng được ở mọi bài (yêu cầu 2026-10-21): cờ `diagramEnabled` còn trong dữ liệu nhưng luôn `true`, bài cũ lưu `false` cũng đọc là bật. Không có loại Essay, Diagram, Group, Code riêng. | FR-007, FR-017, US-ASM-001 |
| BR-U07-02 | Bài của lớp: giảng viên chính của lớp (R3/R4) soạn từ tab Evals. Bài của môn: Chủ nhiệm môn (R2) soạn từ Assignment List, chỉ `INDIVIDUAL`, giao cho mọi lớp `OPEN` của môn theo một lịch chung, kể cả lớp mở sau khi phát hành; giảng viên từng lớp chấm lớp mình. Giảng viên các lớp chỉ xem bài của môn. Admin không soạn, không xem. | FR-027, US-ASM-009 |
| BR-U07-03 | `GROUP` chỉ có ở bài của lớp và chỉ dùng được khi U12 đã cài `IGroupAssignmentSupport`; chưa có thì form không cho chọn, API từ chối. UC 53 của U12 dùng chính Assignment Form này với `GROUP`. | FR-026, UC 53 |
| BR-U07-04 | Lớp hoặc môn đã `ARCHIVED`: bài chỉ đọc (không tạo, sửa, phát hành). | BR-U05-16, BR-U04-07 |

## 2. Đề và khung tài liệu

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-10 | Đề gồm tiêu đề (≤ 200 ký tự), hướng dẫn chung (≤ 20.000 ký tự) và khung tài liệu kiểu Google Docs. Khung là danh sách block: heading (Tiêu đề, Phụ đề, Tiêu đề 1–6 như Word; thanh công cụ chỉ cho chọn Văn bản thường và Tiêu đề 1–4, đề nhập từ DOCX giữ kiểu khác), đoạn văn, danh sách (cấp 0–4), bảng, ảnh (U03 `DOCUMENT_IMAGE`), sơ đồ Draw.io. Đoạn và mục danh sách là các run, mỗi run có định dạng riêng (đậm, nghiêng, gạch chân, gạch ngang, mã, màu chữ, màu nền dạng hex); bôi đen rồi chỉnh thì chỉ đổi phần bôi đen, không bôi đen thì đổi cả đoạn. Heading, đoạn, danh sách có căn lề (trái, giữa, phải, đều). | FR-017, US-ASM-004 |
| BR-U07-11 | Phần là heading nhỏ nhất của mỗi nhánh trong cây Tiêu đề 1–6. Ví dụ H1 có nhánh H2.1 chứa H3.1, H3.2 và nhánh H2.2 không có H3 thì các phần là H3.1, H3.2, H2.2. Block trước heading đầu tiên, Tiêu đề, Phụ đề là phần chung; khung không có heading là một phần. Hệ thống tự tính; `partId` lấy theo id block heading nên giữ qua các lần sửa. | Người dùng chốt 2026-10-04 |
| BR-U07-12 | Sơ đồ lưu XML Draw.io đầy đủ kèm SVG xem trước đã làm sạch. XML qua parser an toàn (không DOCTYPE, XXE, XInclude; gốc `mxfile` hoặc `mxGraphModel`). Trần: ≤ 2.000 block, ≤ 100 sơ đồ, mỗi XML ≤ 2 MB, cả khung ≤ 20 MB. Chèn sơ đồ không chọn loại: loại sơ đồ do server suy từ hình vẽ mỗi lần lưu (lifeline: tuần tự; component, module, giao diện: thành phần; bảng, mũi tên ER, hoặc thoi nối elip không mũi tên: ER; actor: use case; nút đầu, cuối kèm thoi, fork hoặc làn: hoạt động; chỉ nút đầu, cuối: trạng thái; lớp UML: lớp; còn thoi: hoạt động; không nhận ra: `OTHER`; chưa vẽ thì giữ loại gửi lên). Khung sơ đồ rộng hết trang, bấm vào mở Draw.io ở cột phải. | US-ASM-004 S2 |
| BR-U07-13 | `requiredDiagrams` (tùy chọn): loại sơ đồ (`USE_CASE`, `CLASS`, `SEQUENCE`, `ACTIVITY`, `ER`, `COMPONENT`, `STATE`, `OTHER`) → số tối thiểu 1–20; U08, U12 kiểm lúc nộp theo loại suy từ hình vẽ; sơ đồ `OTHER` (sau khi đủ yêu cầu của chính `OTHER`) được tính bù cho loại còn thiếu. Popup Cài đặt không còn mục này (yêu cầu 2026-10-22); giá trị đã có giữ nguyên. | FR-017 |
| BR-U07-14 | Bỏ (yêu cầu 2026-10-21): sơ đồ luôn bật, không còn tắt sơ đồ. | - |
| BR-U07-15 | Khung khóa với người học: không sửa, xóa, di chuyển block của người soạn, không thêm tiêu đề. Người học chỉ viết trong các phần lá (từ tiêu đề lá tới tiêu đề kế; khung không có tiêu đề thì cả bài); phần chung và đoạn dưới tiêu đề cha không viết được. Trong phần: sơ đồ của người soạn thì người học vẽ thẳng vào (đổi XML, loại sơ đồ; mô tả giữ nguyên); bảng của người soạn thì người học điền ô, chèn hàng, cột ở bất kỳ đâu, ô thêm mang cờ `added` và chỉ hàng, cột gồm toàn ô có cờ mới xóa được; hàng tiêu đề và ô của đề giữ nguyên. U08, U12 kiểm qua `IDocumentModel.validateForSave`. | FR-017 |
| BR-U07-16 | `activityMonitoringEnabled` (nút gạt trong popup Cài đặt) mặc định tắt, đặt trước khi phát hành kèm lời báo cho người học; retention cố định 1 ngày, không còn phụ thuộc setting activity.retentionDays; muốn đổi sau phát hành phải tạo version mới. | UC 35, BR-U08-54 |
| BR-U07-17 | Tùy chọn gắn tối đa 10 tài liệu tham khảo (khi U13 có), chọn qua `IReferenceMaterial.listSelectable` của U06. Popup Cài đặt không còn mục chọn (yêu cầu 2026-10-22); giá trị đã có giữ nguyên và người học vẫn thấy trên Assignment Detail (U13 hiện qua slot của U08). | FR-004 |

## 3. Rubric (UC 36)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-20 | Mỗi phần có đúng một rubric, tự tạo trống khi phần xuất hiện, tự xóa khi phần mất (bài còn nháp). Rubric sửa thẳng trên thẻ bên lề của phần (kiểu comment Google Docs), thẻ bắt đầu ngang tiêu đề phần và dính trên màn hình suốt phần đó; tự lưu cùng trang. | US-QBK-001 S1 |
| BR-U07-21 | Rubric atomic: mỗi dòng một ý kiểm được (đạt hay không) kèm điểm; lưu mỗi dòng thành một tiêu chí có một mục (tên tiêu chí là 200 ký tự đầu). Hợp lệ: 1–20 tiêu chí mỗi phần, mỗi mục điểm > 0, tối đa 2 chữ số thập phân; tiêu chí loại `TEXT` hoặc `DIAGRAM`. Dòng cuối là dấu "+" để thêm mục, Enter ở một mục thêm mục ngay dưới; dòng chưa gõ gì không gửi lên. Tiêu chí nhiều mục (bản cũ, đề xuất AI) hiện tách dòng, sửa thì chuyển sang dạng atomic. Bài còn nháp thì rubric được trống hoặc chưa đủ. | UC 36 |
| BR-U07-22 | Rubric khóa cung cấp item/max points. U09 nhận điểm từng item từ0 tới max, tối đa2 chữ số thập phân; checkbox achieved/not vẫn là đường tương thích. GradingJson.score/effectiveItemScores tính tổng, không mặc định chỉ điểm0 hoặc full và không mô tả IRubric.score là implementation U09 đang gọi. | US-QBK-001 S1 |
| BR-U07-23 | Rubric thuộc đúng một bài. Phát hành thì mọi rubric của bài bị khóa; rubric đã khóa không sửa, không xóa. | US-QBK-001 S2 |
| BR-U07-24 | Phát hành khi còn rubric trống hoặc chưa hợp lệ thì từ chối; thẻ rubric của phần thiếu và tên bài trống được tô đỏ, đầu trang chỉ báo chung "Bài còn chỗ chưa đủ". | US-QBK-001 S2a |
| BR-U07-25 | Nhân bản, version mới, copy sang lớp khác: rubric được nhân bản thành rubric mới, chưa khóa, của bài đích; hai bài không dùng chung rubric. | US-QBK-001 S3 |

## 4. Vòng đời

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-30 | Trạng thái `DRAFT → SCHEDULED → OPEN → CLOSED`; `SCHEDULED` hoặc `OPEN` → `RETIRED`. Không còn bước Đã duyệt (yêu cầu 2026-10-22): `REVIEWED` chỉ còn ở bài cũ, coi như bản nháp (hiện là "Nháp", lọc Nháp lấy cả bài đó, sửa thì về `DRAFT`). Chỉ sửa nội dung khi bài còn nháp. | FR-007 |
| BR-U07-31 | Xóa: chỉ bài nháp (`DRAFT`, `REVIEWED` cũ), xóa hẳn kèm rubric và đề xuất AI; bài đã phát hành dùng Ngưng giao. | UC 35 |
| BR-U07-32 | Luật duyệt (tiêu đề, khung hợp lệ, mọi rubric hợp lệ, bài `GROUP` cần `IGroupAssignmentSupport`, bật ghi nhận hoạt động thì cần lời báo; retention cố định 1 ngày) kiểm ngay khi phát hành; thiếu thì `ASSIGNMENT_NOT_READY` kèm chỗ thiếu. Nút Xếp lịch kiểm sơ bộ tên bài và rubric trước khi mở lịch. Xem trước như sinh viên là tùy chọn. API `POST /assignments/{id}/review` còn nhưng giao diện không gọi. | US-ASM-001 |
| BR-U07-33 | Phát hành thẳng từ bài nháp: `closesAt` bắt buộc; ngày mở tùy chọn (tích "Hẹn giờ mở" mới chọn ngày giờ, không tích là mở ngay); `opensAt < closesAt`, hạn chưa qua, `maxAttempts` 1–10, `lateUntil` tùy chọn (tích "Cho nộp muộn"; `closesAt < lateUntil ≤ closesAt + 30 ngày`). Bài của lớp cần lớp `OPEN`; bài của môn cần môn `ACTIVE`; bài `GROUP` cần U12 báo nhóm sẵn sàng (lỗi thì chặn; sinh viên chưa có nhóm thì người phát hành phải xác nhận). Một transaction: ghi lịch, khóa rubric, chuyển `SCHEDULED` (mở ngay thì `OPEN` luôn). Giờ Việt Nam; chọn ngày là đủ, giờ có sẵn (mở 07:00, hạn 23:59). | FR-007 |
| BR-U07-34 | Mở, đóng tự động theo lịch, trễ ≤ 1 phút: tới `opensAt` thì `OPEN`, gọi `onOpened` và phát `assignment.opened`; tới `lateUntil` (hoặc `closesAt` nếu không có) thì `CLOSED`. Nộp sau `closesAt` mà trước `lateUntil` là nộp trễ. | FR-007 |
| BR-U07-35 | Sửa lịch (Đổi lịch): `SCHEDULED` sửa tự do theo BR-U07-33, bỏ tích "Hẹn giờ mở" là mở ngay (bộ hẹn giờ mở trong 1 phút); `OPEN` chỉ kéo dài `closesAt`, `lateUntil`, không rút ngắn, không đổi ngày mở và `maxAttempts` (form không hiện ngày mở); đang trong hạn nộp trễ thì chỉ cần hạn cuối nhận bài còn ở tương lai. Bài của môn chỉ Chủ nhiệm môn sửa lịch. | Thiết kế |
| BR-U07-36 | Ngưng giao: `SCHEDULED` hoặc `OPEN` → `RETIRED`, lý do bắt buộc. Người học không bắt đầu, không nộp thêm; bài đã nộp giữ nguyên; `onRetired` của U08, U12 (tự nộp bài đang làm) chạy trong cùng transaction. Bài của môn chỉ Chủ nhiệm môn ngưng giao. | US-ASM-008 |
| BR-U07-37 | Người học thấy bài `OPEN`, `CLOSED` của lớp `OPEN` mình đang ghi danh, gồm bài của lớp và bài của môn chứa lớp. Bài nháp, `SCHEDULED` ẩn; `RETIRED` ẩn trừ khi người học đã có kết quả ở bài đó (U08 quyết theo dữ liệu bài làm). | FR-007 |

## 5. Nhân bản, version và copy

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-40 | Nhân bản: bài `DRAFT` mới cùng lớp hoặc cùng môn, `origin = CLONE`, giữ nguyên tên bài như bài thường (không thêm "(bản sao)"); danh sách bài không ghi nhãn Nhân bản. | FR-028 |
| BR-U07-41 | Version mới: từ bài `CLOSED` hoặc `RETIRED`, tạo `DRAFT` có `versionNo` tăng 1, `sourceAssignmentId` là bài cũ, `origin = NEW_VERSION`. Mỗi bài nguồn tối đa một version còn nháp. Bài cũ giữ nguyên cho bài nộp cũ. | US-ASM-008 |
| BR-U07-42 | Copy giữa lớp: chỉ bài của lớp; giảng viên phải dạy cả lớp nguồn và lớp đích; tạo `DRAFT` ở lớp đích, `origin = CLASS_COPY`. Lớp ngoài quyền thì "không tìm thấy". | US-ASM-010 |
| BR-U07-43 | Ba cách trên đều sao `submissionMode`, `diagramEnabled`, `gradingMode`, `requiredDiagrams`, theo dõi hoạt động, tài liệu gắn kèm (copy giữa lớp bỏ tài liệu riêng của lớp nguồn), hướng dẫn, khung và rubric (mới). Không sao lịch, nhóm, đề xuất AI, kết nối AI, lượt làm, bài nộp, phản hồi, điểm; không đồng bộ hai chiều. | FR-028, US-ASM-010 S2 |

## 6. AI và MCP

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-50 | AI soạn bản nháp cho bài `DRAFT`: người soạn nhập yêu cầu, chọn tài liệu tham khảo được phép (khi U13 có), chọn nguồn AI trong popup của U06. AI đề xuất hướng dẫn, cây heading Tiêu đề 1–6 kèm đoạn hướng dẫn dưới mỗi heading và gợi ý rubric cho từng phần; không có sơ đồ. | FR-006, US-AIG-001, 002 |
| BR-U07-51 | Đề xuất là bản nháp có provenance (người yêu cầu, nguồn, provider, model khi biết, tài liệu và version). Người soạn xem trước; Áp dụng thì thay toàn bộ khung (cảnh báo khi khung đã có nội dung) và hướng dẫn nếu chọn, tính lại phần, điền gợi ý rubric vào rubric của phần để sửa tiếp; cần `version` của bài không đổi từ lúc xem. Bỏ thì bài không đổi. AI không tự duyệt hay phát hành. | FR-006 |
| BR-U07-52 | Prompt chỉ có yêu cầu, cài đặt bài và chữ của tài liệu đã chọn (cắt theo trần ký tự); không có dữ liệu người học. Không có AI hoặc hết quota vẫn soạn thủ công được. | FR-006 |
| BR-U07-53 | Tool MCP: `read_assignment_context` (giảng viên có quyền đọc bản nháp và rubric; người học chỉ nhận thông tin bài như Assignment Detail: hình thức, chế độ, lịch, sơ đồ cần có, không có đề và rubric) và `save_assignment_draft` (chỉ giảng viên có quyền, bài `DRAFT`, kèm `expectedVersion`). Tool ghi chỉ lưu thành đề xuất chờ người soạn áp dụng như BR-U07-51, không sửa thẳng bài. | FR-035 |

## 7. DOCX

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-60 | Xuất đề và xuất rubric là hai tệp DOCX riêng (yêu cầu 2026-10-22). Tệp đề: tiêu đề, hướng dẫn, khung (heading theo style Word, đoạn có căn lề và định dạng run, danh sách, bảng, ảnh); mỗi sơ đồ thành ảnh PNG kèm alt text "mô tả [drawio:assignmentId/blockId]", hyperlink tới trang xem sơ đồ (kiểm quyền) và khối `mxfile` trong PNG. Tệp rubric: tên bài, mỗi phần một bảng "Mục chấm \| Điểm" kèm tổng điểm; quyền như xuất đề, sinh viên không đọc được. | Thiết kế |
| BR-U07-61 | Nhập DOCX (≤ 20 MB) khi tạo bài: popup Tạo bài có một ô tệp DOCX tùy chọn; có tệp thì tạo bài rồi nhập luôn tệp làm khung trước khi mở trang soạn. Title, Subtitle, Heading 1–6 thành heading; đoạn (giữ định dạng run, màu, căn lề), danh sách, bảng, ảnh thành block của người soạn. Mỗi ảnh tìm sơ đồ lần lượt: mã trong alt text, hyperlink, khối `mxfile` của PNG hoặc thuộc tính `content` của SVG; không có thì giữ là ảnh. Sơ đồ theo mã chỉ lấy khi người nhập có quyền đọc bài nguồn. Không dùng AI. | US-ASM-004 |
| BR-U07-62 | Nội dung không hỗ trợ (textbox, SmartArt, công thức, header, footer) bị bỏ. Không còn bước xem trước nội dung tệp: nhập lỗi thì bài đã tạo vẫn còn, người soạn "Thử lại" (chỉ nhập lại tệp, không tạo thêm bài) hoặc "Mở bài". File DOCX chỉ xử lý trong bộ nhớ, không lưu. | Thiết kế |

## 8. Phân quyền, audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U07-70 | Mọi API kiểm quyền phía server; ngoài phạm vi trả `404` và ghi audit `ACCESS_DENIED`. Sửa bài, rubric, lịch phải kèm `version`; lệch thì `409`. | SEC-002 |
| BR-U07-71 | Audit: `ASSIGNMENT_CREATED`, `ASSIGNMENT_DELETED`, `ASSIGNMENT_PUBLISHED` (lớp hoặc môn, lịch), `ASSIGNMENT_SCHEDULE_CHANGED`, `ASSIGNMENT_RETIRED` (lý do), `ASSIGNMENT_CLONED`, `ASSIGNMENT_VERSION_CREATED`, `ASSIGNMENT_COPIED` (nguồn, đích), `RUBRIC_UPDATED`, `ASSIGNMENT_AI_DRAFT_APPLIED` (provenance), `ASSIGNMENT_DOCX_IMPORTED`; `ASSIGNMENT_REVIEWED` chỉ khi gọi API duyệt. Không ghi nội dung đề. | FR-014 |
| BR-U07-72 | U01, U04, U05 không trả lời thì từ chối (fail closed). AI, U12, U13 lỗi không chặn việc soạn thủ công. | BR-U01-93 |
