# U07 Assignment Authoring — code summary

Code viết ngày 2026-10-20 theo [plan](../../plans/code-generation-plan/u07-assignment-authoring-code-generation-plan.md), nhánh `assignment`, làm cùng U08. Integration test (PostgreSQL thật, chạy đồng thời, Word và Google Docs) do tester viết.

## Business logic

| Luồng | Class chính |
|---|---|
| F1 Bài của lớp (UC 34) | `AssignmentAuthoringService.listForClass`: bài của lớp và bài của môn đã phát hành (chỉ đọc); mặc định ẩn `CLOSED`, `RETIRED` và trả số bài đang ẩn, số bài theo trạng thái, `groupSupported` |
| F2 Bài của môn (UC 34) | `listForSubject`: chỉ Chủ nhiệm môn |
| F3 Tạo bài (UC 35) | `createForClass`, `createForSubject`: bài `DRAFT`, version 1, một phần `DOCUMENT` kèm rubric trống. Bài nhóm cần U12 (`GROUP_NOT_SUPPORTED`), bài của môn chỉ cá nhân, nhóm chỉ Graded; lớp hoặc môn lưu trữ thì chỉ đọc |
| F4 Soạn đề, khung (UC 35) | `updateDetails`: tiêu đề, hướng dẫn, chế độ, bật/tắt sơ đồ (tắt bị chặn khi còn sơ đồ, số sơ đồ bắt buộc, tiêu chí sơ đồ), số sơ đồ bắt buộc 1–20, theo dõi hoạt động (cần `activity.retentionDays`), học liệu ≤ 10 qua `IReferenceMaterial`. `saveSkeleton`: làm sạch, kiểm (`DocumentModelService`), gắn ảnh mới qua U03, tính phần (`PartCalculator`), đồng bộ rubric theo phần (P1); bài đã duyệt về nháp |
| F5 Rubric (UC 36) | `rubric`, `updateRubric`; `RubricService.normalize`: ≤ 20 tiêu chí × ≤ 20 mục, điểm > 0 tối đa 2 chữ số thập phân, tiêu chí sơ đồ chỉ khi bật sơ đồ; nháp được thiếu; tổng tự tính; rubric đã khóa không sửa |
| F6 AI soạn bản nháp | `AssignmentAiDraftService`, `AssignmentDraftResultHandler` (`ASSIGNMENT_DRAFT`): đề xuất nằm riêng trong `assignment_ai_drafts`; tạo đề xuất trước, gửi task ngoài transaction, kết quả tìm theo `targetRef`; Áp dụng (cần `version`) thay khung, tính phần, điền gợi ý rubric; Bỏ thì bài không đổi (P5) |
| F7 DOCX | `DocxService` (cài `IDocx`), `DocxExporter`, `DocxImporter`: style Word (Title, Subtitle, Heading 1–6), danh sách đánh số, bảng, ảnh; mỗi sơ đồ là ảnh PNG có chunk `tEXt mxfile`, alt text `[drawio:assignmentId/blockId]` và hyperlink tới trang xem sơ đồ. Nhập tìm sơ đồ theo mã (người nhập phải đọc được bài nguồn), hyperlink, chunk PNG, rồi thuộc tính `content` của SVG; chặn file nén bất thường (≤ 2.000 mục, giải nén ≤ 100 MB); liệt kê nội dung bị bỏ |
| F8 Duyệt | `review`: tiêu đề, khung hợp lệ, tiêu đề mục không trống, mọi rubric đủ, sơ đồ đúng cờ, nhóm có U12, theo dõi hoạt động có lời báo và thời hạn giữ dữ liệu |
| F9–F11 Phát hành, lịch, ngưng giao | `AssignmentPublicationService`: phát hành một transaction (lịch, khóa rubric, `SCHEDULED`, mở ngay nếu tới giờ); sửa lịch (`OPEN` chỉ kéo dài hạn); ngưng giao bắt buộc lý do, gọi `onRetired` trong cùng transaction. `AssignmentScheduleScanner` (worker, mỗi bài một transaction) mở và đóng theo lịch; `AssignmentLifecycle` gọi listener rồi phát `assignment.opened` sau commit |
| F12, F13 Nhân bản, version, copy | `AssignmentCopyService`: sao nội dung, cài đặt, rubric mới chưa khóa; không sao lịch; một version đang soạn mỗi bài nguồn; copy giữa lớp cần dạy cả hai lớp, bỏ học liệu ngoài phạm vi lớp đích |
| F14 Xóa | `delete`: chỉ bài chưa phát hành; rubric, đề xuất AI xóa theo khóa ngoại |
| F15 MCP | `AssignmentMcpTools`: `read_assignment_context`, `save_assignment_draft` (lưu đề xuất, không sửa bài) |
| F16 Contract cho unit khác | `AssignmentQueryService` (`IAssignmentQuery`, gồm `submissionWindow`, `listClosingBetween`), `RubricService` (`IRubric`), `DocumentModelService` (`IDocumentModel`) |

Domain: `Assignment`, `AssignmentOrigin`, `AssignmentRubric`, `AssignmentAiDraft`, `PartCalculator`. Repository `AssignmentRepository`, `RubricRepository`, `AiDraftRepository` (`JdbcClient`, jsonb): mọi lần ghi bài tăng `version`, kể cả scanner, nên lệch phiên bản luôn trả 409. Tài liệu: `DocumentModelService`, `DiagramXml` và `SecureXml` (cấm DOCTYPE, entity ngoài, XInclude; giải trang nén có trần), `SvgSanitizer`, `PngChunks` (PNG mức byte, không dùng AWT). Contract tiện ích `DocumentFiles`: frontend không thấy `fileId` của ảnh, ảnh mở theo id block.

Kiểm quyền: action mới trong `Actions`/`AccessPolicy` của U01: `ASSIGNMENT_CLASS_MANAGE` (Teacher, Subject Manager; phạm vi lớp, giảng viên chính) và `ASSIGNMENT_SUBJECT_MANAGE` (Subject Manager; phạm vi môn). Giảng viên các lớp của môn chỉ đọc bài của môn đã phát hành. Ngoài phạm vi trả 404 kèm audit `ACCESS_DENIED`; U01, U04, U05 lỗi trả 503. Audit thành công ghi cùng transaction: `ASSIGNMENT_CREATED`, `ASSIGNMENT_DELETED`, `ASSIGNMENT_REVIEWED`, `ASSIGNMENT_PUBLISHED`, `ASSIGNMENT_SCHEDULE_CHANGED`, `ASSIGNMENT_RETIRED`, `ASSIGNMENT_CLONED`, `ASSIGNMENT_VERSION_CREATED`, `ASSIGNMENT_COPIED`, `RUBRIC_UPDATED`, `ASSIGNMENT_AI_DRAFT_APPLIED`, `ASSIGNMENT_DOCX_IMPORTED`; không ghi nội dung đề.

## API

Gốc `/api/v1`.

| Endpoint | UC |
|---|---|
| `GET`, `POST /classes/{classId}/assignments`; `GET /classes/{classId}/assignment-copy-sources`; `POST /classes/{classId}/assignments:copy` | 34, 35 |
| `GET`, `POST /subjects/{subjectId}/assignments` | 34, 35 |
| `GET`, `PATCH`, `DELETE /assignments/{id}`; `GET /assignments/{id}/selectable-materials` | 35 |
| `PUT /assignments/{id}/skeleton`; `POST .../skeleton:import-docx`; `GET .../docx?includeRubric=`; `GET .../diagrams/{blockId}`; `GET .../blocks/{blockId}/image` (302 tới link tải 5 phút của U03) | 35 |
| `GET`, `PUT /assignments/{id}/parts/{partId}/rubric` | 36 |
| `POST /assignments/{id}/ai-drafts`; `GET .../ai-drafts/{draftId}`; `POST .../ai-drafts/{draftId}:apply`, `:discard` | 35 |
| `POST /assignments/{id}/review`, `/publish`, `/retire`, `/clone`, `/versions`; `PATCH .../schedule` | 34, 35 |

Lỗi kiểm trả Problem Detail kèm `issues` (mã, block, thông điệp); `version` lệch trả 409.

## Migration

`db/migration/assignments/V20260925_1300__create_assignment_tables.sql`: `assignments` (ràng buộc phạm vi, nhóm chỉ Graded, lịch, hạn trễ ≤ 30 ngày, lượt 1–10, bài đã phát hành phải có lịch; index theo lớp, môn, lịch; unique một version đang soạn mỗi bài nguồn), `assignment_rubrics` (unique `(assignment_id, part_id)`), `assignment_ai_drafts` (unique `task_id`, `(assignment_id, request_ref)`).

## Cấu hình

`platform.assignments.public-url` (`APP_PUBLIC_URL`, mặc định `http://localhost:8088`) cho link trang xem sơ đồ trong DOCX. Thư viện: Apache POI `poi-ooxml` 5.4.1 với `poi-ooxml-full` (bỏ `poi-ooxml-lite` vì thiếu class schema để tạo style, numbering).

## Frontend

- Route: `/manager/assignments` (#25 Assignment List, Chủ nhiệm môn), `/assignments/[id]` (#26 Assignment Form, Teacher và Subject Manager), `/assignments/[id]/diagrams/[blockId]` (trang xem sơ đồ).
- `features/assignments`: `ClassAssignmentsSection` (gắn `TeacherClassTab` tab `evals`), `SubjectAssignmentListPage`, `AssignmentSummary` (gắn `SubjectSummarySection`), `CreateAssignmentDialog` (tạo mới hoặc copy lớp khác), `AssignmentFormPage` (một trang kiểu Google Docs: sửa khung và rubric cùng lúc, rubric của mỗi mục lá nằm ở lề phải ngang tiêu đề; tự lưu sau 1,5 giây và Ctrl+S, các lệnh ghi nối hàng để dùng đúng version; 409 báo tải lại và giữ bản đang sửa; dòng bước Soạn đề và rubric → Duyệt → Lịch phát hành, duyệt xong mở popup lịch; Cài đặt, Lịch, Ngưng giao, Xóa là popup; Xuất DOCX, Nhân bản, Version mới trong menu Khác; Xem như sinh viên), `RubricEditor` (sửa rubric trong thẻ bên lề), `AssignmentSettingsPanel`, `ScheduleForm` (giờ Việt Nam), `AiDraftDialog`, `AssignmentDiagramPage`; host slot `AssignmentRowAction`, `AssignmentFormSection`.
- `shared/document` dùng chung U08, U09, U12: `DocumentEditor` (trang giấy, thanh công cụ dính: kiểu đoạn, B/I/U/mã, Chèn; thao tác block ở lề trái khi rê chuột; Enter tách đoạn, Backspace xóa đoạn trống, Tab đổi cấp danh sách; chế độ người học khóa khối của giảng viên; ghi chú theo block), `CommentRail` (cột ghi chú kiểu comment Google Docs: thẻ đứng ngang block, cuộn cùng tài liệu, chồng nhau thì dồn, thẻ đang chọn đứng đúng ngang block; màn hẹp hơn 1280px thì thẻ nằm ngay dưới block), `RubricNote` (thẻ rubric của mục), `DiagramEditor` (Draw.io nhúng từ `embed.diagrams.net`, kiểm origin và iframe của message, xuất `xmlsvg`), `DocumentViewer`, `RubricViewer`, `DocxImportDialog`, `DiagramViewPage`.

## Unit test

Backend: tính phần, kiểm khung và bài làm, XML có DOCTYPE, SVG có script, rút gọn sơ đồ nén; tạo bài, quyền, nhóm, lớp lưu trữ; lưu khung tính lại phần và đồng bộ rubric; 409; tắt sơ đồ khi đang dùng; theo dõi hoạt động cần thời hạn; duyệt khi rubric trống; quy tắc rubric và tổng điểm; rubric khóa; xóa; giảng viên lớp đọc bài của môn; phát hành, lịch sai, mở ngay gọi listener và phát sự kiện, lớp chưa mở, sửa lịch bài đang mở, ngưng giao, scanner chạy lại không mở hai lần; nhân bản, version, copy ngoài quyền; DOCX xuất rồi nhập lại theo mã, theo chunk PNG, giữ là ảnh khi tắt sơ đồ; đề xuất AI (kết quả cho bài đã phát hành bị từ chối, áp dụng lệch version, áp dụng thay khung và rubric); tool MCP; cửa sổ nộp. Frontend: trình soạn (khóa khối giảng viên, chèn khối, giữ định dạng, Enter tách đoạn, Backspace xóa đoạn trống, ghi chú dưới block khi màn hẹp), đổi kiểu block, tính phần và mục chứa block giống backend, rubric đủ và tổng điểm, thẻ rubric, sửa rubric và chuẩn hóa điểm gửi lên, trình xem và rubric, giải SVG data URI, form lịch, đổi giờ Việt Nam, gộp ảnh sau khi lưu.

## Chạy thử trên Docker (2026-10-20)

Tạo bài, soạn khung có sơ đồ (SVG xem trước được làm sạch), duyệt khi rubric trống trả 409, điền rubric, duyệt, phát hành mở ngay, sửa rubric sau phát hành trả 409; scanner mở bài lên lịch trong vòng 1 phút; xuất DOCX rồi nhập lại khôi phục sơ đồ theo mã; tải ảnh lên, lưu khung gắn ảnh, mở ảnh qua link 302; bài của môn; version mới (lần hai trả 409); AI dùng bản tạm của U06 trả `FAILED`. Màn Đánh giá của lớp, Assignment Form, Assignment List, số bài trên Manager Dashboard hiển thị đúng.

## Khác với thiết kế

- Ảnh PNG của sơ đồ trong DOCX là hình đại diện dựng sẵn, không vẽ lại sơ đồ (image backend Alpine không có thư viện vẽ SVG và font); alt text, hyperlink, chunk `mxfile` vẫn đủ để khôi phục.
- Trình soạn là trình soạn khối tự viết thay cho TipTap; đậm, nghiêng, gạch chân, mã áp cho cả đoạn.
- Theo yêu cầu người dùng (2026-10-21, làm theo bản demo `chi-cuongg/demo_do_an`): #26 không chia 4 bước và #27 Rubric Detail không còn là popup; khung và rubric sửa trên một trang, rubric ở lề phải theo từng mục lá; lịch phát hành chỉ mở sau khi duyệt. Phần của tài liệu được tính ngay ở frontend (giống `PartCalculator`) để thẻ rubric hiện ngay khi gõ tiêu đề; rubric của mục mới lưu sau khi khung được lưu.
- Popup AI dùng ô chọn nguồn đơn giản và tự hỏi trạng thái đề xuất mỗi 2 giây vì `AiSourcePicker`, `AiTaskStatus` của U06 chưa có.
- Chưa có `contracts/openapi/assignments.yaml` (giống U01–U05).
- Contract đổi so với thiết kế: `IDocx.importPreview` thêm `actor`; thêm `IDiagramLinker.viewUrlFor`, `IGroupAssignmentSupport.UNAVAILABLE`, `IDocumentModel.sanitize`, lớp `DocumentFiles` (đã cập nhật unit-summary và domain-entities).

## Sửa ngoài unit khi chạy thử

- `backend/Dockerfile`: tạo sẵn `/app/data/files`, `/app/data/backups` thuộc user `app`; trước đó volume mount vào thư mục do root tạo nên U03 không ghi được tệp local (`STORAGE_UNAVAILABLE`).
- `shared/config/SecurityConfig`: cho qua lượt dispatch ASYNC; tải tệp của U03 dùng `StreamingResponseBody`, lượt ASYNC không có Authentication nên response bị cắt giữa chừng.

## Thay đổi theo yêu cầu 2026-10-21 (trang kiểu Google Docs)

Các thay đổi dưới đây (cả vòng 2026-10-21 và các vòng 2026-10-22) đã đưa vào functional-design ngày 2026-10-22 (business-rules, business-logic-model, domain-entities, frontend-components); các bảng dưới giữ lại để truy vết.

Người dùng yêu cầu làm theo cách này dù khác thiết kế; các BR liên quan trong functional-design chưa sửa.

| Thay đổi | Khác thiết kế | Code |
|---|---|---|
| Sơ đồ Draw.io luôn dùng được; tạo bài chỉ chọn hình thức và chế độ, không chọn có sơ đồ hay không; popup Cài đặt bỏ công tắc sơ đồ | Bỏ BR-U07-14 (tắt sơ đồ); cờ `diagramEnabled` của BR-U07-01 luôn `true` | `Assignment` (constructor ép `true`, bài cũ lưu `false` cũng coi là bật), `updateDetails` bỏ qua cờ gửi lên; `CreateAssignmentDialog`, `AssignmentSettingsPanel` |
| Tạo bài "Từ tệp DOCX": tạo bài nháp rồi trang soạn mở sẵn hộp nhập DOCX (`?import=docx`); đề trống có nút Nhập tệp DOCX, Soạn bằng AI | Thêm cách tạo | `CreateAssignmentDialog`, `AssignmentFormPage`, `DocumentEditor.emptyActions` |
| Rubric atomic: mỗi dòng một ý kiểm được (đạt hay không) kèm điểm; lưu mỗi dòng thành một tiêu chí có một mục chấm (tên tiêu chí là 200 ký tự đầu); tiêu chí nhiều mục (bản cũ, đề xuất AI) hiện tách dòng, sửa lần đầu thì chuyển sang dạng atomic | Giao diện BR-U07-21 chỉ còn mục chấm; mô hình dữ liệu và luật kiểm giữ nguyên | `RubricEditor`, `RubricNote`, `RubricViewer`, `atomicItems`, `criteriaOf`; DOCX kèm rubric là bảng 2 cột Mục chấm, Điểm (`DocxExporter`) |
| Thẻ rubric bên phải bắt đầu ngang tiêu đề của phần, dính dưới thanh công cụ khi cuộn qua đầu phần, chỉ bị đẩy đi khi tới đầu phần kế tiếp | Thay cách xếp thẻ cũ (thẻ cuộn cùng block, thẻ đang chọn đứng đúng ngang block) | `CommentRail`: mỗi thẻ trong một dải từ phần đó tới thẻ kế, thẻ `sticky` |
| Luồng soạn: Soạn đề và rubric → Xem trước (trang sinh viên thấy khi làm bài, không có rubric, chỗ sinh viên viết là khung nét đứt) → Tiếp tục: đặt lịch phát hành | Nút Duyệt thành bước Xem trước; "Tiếp tục" là lệnh duyệt `DRAFT → REVIEWED` (BR-U07-32) nên luật duyệt giữ nguyên; bỏ nút "Xem như sinh viên" | `AssignmentFormPage` (`openPreview`, `continueToSchedule`, `FlowSteps`) |
| Người học chỉ viết trong các phần lá: block của người học phải nằm từ tiêu đề lá tới tiêu đề kế; phần chung (trước tiêu đề đầu tiên) và đoạn dưới tiêu đề cha không viết được; khung không có tiêu đề thì cả bài là một phần | Thay câu "người học chèn block của mình ở bất kỳ vị trí nào" của BR-U07-15 | `DocumentModelService.validateForSave` (lỗi `STUDENT_BLOCK_OUTSIDE_PART`), `blocksInParts`; `DocumentEditor` chế độ người học |
| Sơ đồ của người soạn nằm trong phần: người học vẽ thẳng vào (đổi XML, ảnh xem trước), không đổi loại, mô tả, không xóa; sơ đồ ở phần chung vẫn khóa. Khi nộp: ô sơ đồ trống của đề phải được vẽ (`DIAGRAM_EMPTY`); sơ đồ đã vẽ khác bản của đề tính là bài làm và tính vào số sơ đồ bắt buộc | Nới BR-U07-15 cho sơ đồ | `DocumentModelService.validateForSave`, `validateForSubmit` |
| Tool MCP `read_assignment_context` với người học chỉ trả thông tin bài (hình thức, chế độ, lịch, sơ đồ cần có), không có đề, rubric | Thay phần người học của BR-U07-53 | `AssignmentMcpTools` |

Sửa lỗi tìm thấy ở bước Review: bài đang mở đã qua hạn nộp chính (đang trong hạn nộp trễ) không gia hạn được vì code đòi `closesAt` ở tương lai; nay chỉ đòi hạn cuối nhận bài còn ở tương lai (`AssignmentPublicationService.changeSchedule`).

Không thêm migration: cột `diagram_enabled` giữ nguyên, đọc lên luôn là `true` và được ghi `true` ở lần lưu kế tiếp. Migration số `V20261021_…` sẽ làm các migration `V20260925_1500…2000` chưa có của unit sau thành "cũ hơn bản đã chạy", Flyway mặc định từ chối.

Unit test thêm: người học viết ngoài phần, vẽ vào sơ đồ của đề trong và ngoài phần, sơ đồ không tắt được, gia hạn trong hạn nộp trễ, tool MCP với người học không có đề; frontend: rubric atomic, thẻ dính, chỉ viết trong phần lá, nút Vẽ, tạo bài từ DOCX. Chưa chạy thử trên Docker sau thay đổi này (Docker Desktop trên máy báo cần cập nhật WSL).

## Thay đổi theo yêu cầu 2026-10-22 (vòng sửa giao diện)

Thay các dòng luồng soạn, tạo bài "Từ tệp DOCX" và sơ đồ trong phần của bảng trên; các BR trong functional-design chưa sửa.

| Thay đổi | Khác thiết kế | Code |
|---|---|---|
| Trang soạn có nút "Xem trước" (tùy chọn) và nút "Xếp lịch": lưu, bài còn nháp thì duyệt (`DRAFT → REVIEWED`, BR-U07-32) rồi mở lịch; duyệt không qua thì về trang soạn kèm lỗi. Trang xem trước có "Quay lại" và "Xếp lịch" | Bỏ bước bắt buộc Xem trước → Tiếp tục; luật duyệt giữ nguyên | `AssignmentFormPage.openSchedule` |
| Lịch một trang: ngày mở tùy chọn (tích "Hẹn giờ mở"), không tích là mở ngay; bài đã lên lịch mà bỏ tích thì mở ngay (gửi `opensAt` lúc bấm, bộ hẹn giờ mở trong 1 phút theo BR-U07-34) | Thêm mở ngay khi sửa lịch bài đã lên lịch | `ScheduleForm` |
| Xuất đề và xuất rubric là hai tệp DOCX riêng: `GET /api/v1/assignments/{id}/docx` (bỏ tham số `includeRubric`), `GET /api/v1/assignments/{id}/rubric/docx` (tên bài, mỗi phần một bảng Mục chấm, Điểm; quyền như xuất đề, sinh viên không đọc được). Menu "Khác": Xuất DOCX, Xuất rubric | BR-U07-60 tách rubric ra tệp riêng | `DocxService.exportRubric`, `DocxExporter.exportRubric`, `AssignmentController.exportRubric` |
| Popup tạo bài: một ô tệp DOCX tùy chọn, chọn xong chỉ hiện tên tệp; tạo xong nhập luôn tệp vào khung (`importDocx` rồi `saveSkeleton` với `fromDocx`) rồi mới mở trang soạn, không còn hộp xem trước nội dung tệp; nhập lỗi thì "Thử lại" chỉ nhập lại tệp (không tạo thêm bài) hoặc "Mở bài" đã tạo; copy từ lớp khác là nút nhỏ ở chân popup; bỏ mọi dòng hướng dẫn, gợi ý | Thay cách tạo "Từ tệp DOCX" (`?import=docx` và hộp nhập trên trang soạn) | `CreateAssignmentDialog`, `AssignmentFormPage`, `ClassAssignmentsSection`, `SubjectAssignmentListPage` |
| Chèn sơ đồ: một nút, chèn khung trống và mở Draw.io ngay, không chọn loại hay mẫu. Loại sơ đồ do server suy từ hình vẽ mỗi lần lưu: lifeline là tuần tự; component, module, giao diện là thành phần; bảng, mũi tên ER, hoặc thoi và elip nối không mũi tên (Chen) là ER; actor là use case; nút đầu, cuối kèm thoi, thanh fork hoặc làn là hoạt động, chỉ có nút đầu, cuối là trạng thái; lớp UML (swimlane xếp ô) là lớp; còn thoi là hoạt động; không nhận ra là `OTHER`; chưa vẽ thì giữ loại gửi lên. Khi nộp, sơ đồ `OTHER` (sau khi đủ yêu cầu của chính `OTHER`) được tính bù cho loại còn thiếu | BR-U07-12, 13: loại sơ đồ không còn do người soạn chọn; luật đủ số sơ đồ theo loại nới cho sơ đồ không nhận ra loại | `DiagramXml.detectType`, `DocumentModelService.clean`, `validateForSubmit`; `DocumentEditor.insertDiagram` |
| Draw.io mở ở cột phải kéo giãn được, đẩy trang sang trái, không che trang (như bản demo); đang vẽ thì ẩn thẻ rubric bên lề. Khung sơ đồ rộng hết trang, cao tối thiểu 240px, viền đứt, bấm vào là vẽ | Giao diện | `DiagramEditor`, `globals.css` (`html.drawio-panel-open`), `BlockView` |
| Người học điền bảng của đề nằm trong phần: sửa ô, thêm hàng, cột; không bớt hàng, cột của đề, không đổi hàng tiêu đề, không xóa, không dời bảng; bảng ở phần chung vẫn khóa; bảng đã điền khác đề tính là bài làm khi nộp | Nới BR-U07-15 cho bảng (như sơ đồ) | `DocumentModelService.sameContent`, `filledTable`, `validateForSubmit`; `DocumentEditor` (`fillable`), `TableEditor` (`canShrink`) |
| Cây phần: phần cùng tầng cùng màu, tầng sâu đậm hơn; lá thì khung mang màu tầng, ruột trắng là chỗ viết. Bỏ chữ gợi ý ở mỗi dòng (đoạn, mục danh sách, tiêu đề, chú thích ảnh, sơ đồ, phần trống, dòng rubric, ô điểm); giữ chữ gợi ý ở ô tên bài | Giao diện | `DocumentEditor` (`SECTION_BG`, `emptyLeaf`), `RubricEditor` |
| Bảng: ô vuông + − nằm ngoài bảng (của cột ở trên đầu, của hàng bên phải), hiện ở cột, hàng đang trỏ tới; bỏ nút Xóa hàng, Xóa cột trên thanh công cụ (còn nút Hàng tiêu đề); ô chọn cỡ bảng là lưới cột cố định 18px | Giao diện | `TableEditor`, `Square`, `TablePicker` |
| Danh sách bài: Hình thức và Chế độ là hai cột riêng; bỏ nhãn "Có sơ đồ Draw.io" (sơ đồ luôn bật) | Giao diện | `AssignmentTable`, `StudentAssignmentsPage` (U08) |

Sửa lỗi:
- Đổi lịch bài đang mở bị từ chối `SCHEDULE_OPEN_LOCKED` khi bài mở ngay lúc phát hành: form gửi lại ngày mở đã cắt giây (ô chỉ tới phút) nên khác ngày mở trên server; nay bài đang mở không gửi ngày mở (`ScheduleForm`).
- Ô chọn cỡ bảng hiện thành dải chữ nhật: lưới dùng cột co giãn trong ô bật ra hẹp bằng nút, các ô 16px chồng lên nhau; nay cột cố định 18px.
- Thứ tự khóa định dạng của chữ khi làm sạch (`RUN_FLAGS`, `RUN_COLORS`) dùng `List` thay `Set.of`: thứ tự lặp của `Set.of` đổi theo mỗi lần chạy JVM, JSON lưu ra đổi thứ tự khóa và test so chuỗi lúc qua lúc trượt.

Unit test thêm: xuất rubric thành tệp riêng, suy loại sơ đồ từ hình vẽ, sơ đồ `OTHER` bù loại còn thiếu, người học điền bảng của đề trong và ngoài phần; frontend: một nút chèn sơ đồ mở cột vẽ, màu theo tầng, ô + − ngoài bảng, người học điền bảng, ô chọn cỡ bảng, không có chữ gợi ý, tạo bài kèm tệp DOCX (cả khi nhập lỗi), đổi lịch bài đang mở và bài đã lên lịch.

## Thay đổi theo yêu cầu 2026-10-22 (vòng sửa thứ hai)

| Thay đổi | Khác thiết kế | Code |
|---|---|---|
| Nút trên trang soạn theo thứ tự Khác, Xem trước hoặc Quay lại, Xếp lịch hoặc Đổi lịch; Đổi lịch cũng là nút chính (xanh) | Giao diện | `AssignmentFormPage` |
| Xếp lịch mà bài còn thiếu: báo một câu chung chung, chỗ thiếu đánh dấu đỏ ngay trong bài (block, thẻ rubric, ô tên bài); chỉ lỗi không gắn được vào đâu (cài đặt, nhóm) mới ghi kèm | Giao diện | `AssignmentFormPage.missingText` |
| Đổi lịch bài đang mở không hiện ô "Mở lúc"; ngày mở chỉ hiện khi tích "Hẹn giờ mở" | Giao diện | `ScheduleForm` |
| Nhân bản giữ nguyên tên bài (bỏ hậu tố " (bản sao)"), danh sách bài không ghi "Nhân bản"; dữ liệu vẫn `origin = CLONE` | BR-U07-40 giữ nguyên, bỏ hậu tố tên | `AssignmentCopyService`, `AssignmentTable` |
| Định dạng chữ theo vùng bôi đen (đậm, nghiêng, gạch chân, gạch ngang, màu chữ, màu nền); không bôi đen thì áp cho cả đoạn. Đoạn văn và mục danh sách sửa trong ô contentEditable, mỗi run một span; Enter tách đoạn giữ định dạng hai nửa; dán chỉ lấy chữ thuần. Mô hình run và backend không đổi (đã hỗ trợ nhiều run mỗi đoạn) | Thay "định dạng áp cho cả đoạn" của vòng trước | `RichText`, `runs.ts`, `DocumentEditor` |
| Bỏ nút mã trên thanh công cụ; kiểu đoạn chỉ còn Văn bản thường và Tiêu đề 1–4 (đề nhập từ DOCX có Title, Tiêu đề 5–6 vẫn hiện đúng kiểu của nó) | Giao diện | `DocumentEditor` |
| Bỏ menu "⋮" (lên, xuống, xóa) ở từng block; sơ đồ, ảnh, bảng có nút ✕ nổi ở góc trên trái để xóa; đoạn chữ xóa bằng Backspace khi trống | Không còn đổi chỗ block bằng menu | `DocumentEditor` (`CornerDelete`) |
| Khung sơ đồ mới chèn trải hết bề ngang trang như bản demo (trước đó bị co thành khối hẹp vì khung ngoài căn giữa); mở Draw.io thì thu thanh menu bên trái của ứng dụng | Sửa lỗi giao diện; AppShell thêm `data-app-sidebar` | `DocumentEditor.BlockEditor`, `globals.css`, `AppShell` |
| Bảng: ô "+ −" nổi đè lên chỗ xung quanh, không chiếm hàng, cột riêng; "−" khi bảng còn một hàng hoặc một cột là xóa cả bảng; bỏ nút Hàng tiêu đề trên thanh công cụ | Giao diện | `TableEditor` |
| Rubric: dòng cuối là dấu "+" để thêm mục, Enter ở một mục thêm mục ngay dưới; bỏ cách "gõ vào dòng trống là thêm mục"; dòng thêm mà chưa gõ gì không gửi lên server | Giao diện | `RubricEditor`, `cleanCriteria` |

Unit test thêm: nhân bản giữ tên; frontend: định dạng phần bôi đen, ô chữ đọc lại run (cả thẻ trình duyệt tự chèn), tách đoạn giữ định dạng, nút xóa ở góc, không còn menu ba chấm, bảng nút nổi và xóa bảng, rubric dấu "+", báo thiếu chung chung, đổi lịch bài đang mở.

## Thay đổi theo yêu cầu 2026-10-22 (vòng sửa thứ ba)

| Thay đổi | Khác thiết kế | Code |
|---|---|---|
| Bảng: nút "+ −" của hàng chuyển sang bên trái hàng; nút ✕ xóa bảng ở góc trên trái, cùng hàng với nút của cột | Giao diện | `TableEditor` |
| Người học điền bảng của đề: hàng, cột nào cũng có "+" (chèn được cả giữa bảng) và "−"; ô người học thêm mang cờ `added`, "−" chỉ dùng được với hàng, cột gồm toàn ô có cờ, hàng, cột của đề thì "−" bị khóa. Server giữ cờ khi lưu và kiểm: các hàng còn ô của đề không ít hơn đề, mỗi hàng đó còn đủ số ô của đề (đánh dấu ô của đề là ô thêm để xóa thì bị từ chối) | Thay cách "chỉ thêm sau phần của đề" của vòng trước; thêm cờ `added` vào ô bảng (`TableCell`) | `DocumentModelService` (`clean`, `filledTable`, kiểm cờ), `TableEditor` (`guard`) |
| Popup Cài đặt bài giống popup tạo bài: thẻ chọn Hình thức (chọn khi tạo, chỉ xem), Chế độ, Ghi nhận hoạt động (Tắt, Bật) kèm lời báo; bỏ mục sơ đồ bắt buộc theo loại và học liệu gắn kèm khỏi popup (giá trị đã có giữ nguyên khi lưu) | Không còn chỗ đặt BR-U07-13 (số sơ đồ bắt buộc) và học liệu gắn kèm trên giao diện | `AssignmentSettingsPanel`, `Choice` (dùng chung với popup tạo bài) |
| Thông báo trên màn hình chỉ ghi trạng thái, không ghi hành động: "Bài còn chỗ chưa đủ", "Chưa lưu: …", "Đã phát hành", "Đã lên lịch", "Đã lưu lịch", "Đã ngưng giao", "Bài chưa duyệt", "Bài đã thay đổi ở nơi khác, bản đang sửa chưa lưu"; lỗi form lịch: "Thiếu hạn nộp", "Hạn nộp không sau lúc mở", "Hạn nộp muộn quá 30 ngày sau hạn nộp", "Số lượt ngoài 1–10"… | Giao diện | `AssignmentFormPage`, `ScheduleForm`, `CreateAssignmentDialog` |
| Đầu trang: màn hẹp thì hàng nút (Khác, Xem trước, Xếp lịch) xuống dưới tên bài thay vì ép tên bài thành cột hẹp; dấu "+" của rubric ra giữa thẻ | Giao diện; `PageHeader` dùng chung đổi cho mọi trang | `PageHeader`, `RubricEditor` |

Unit test thêm: cờ ô thêm được giữ khi lưu, chèn hàng, cột giữa bảng của đề, đánh dấu ô của đề để xóa bị từ chối; frontend: nút bảng bên trái, ✕ cùng hàng, người học có đủ nút và chỉ xóa hàng, cột mình thêm, thông báo chỉ ghi trạng thái.

## Thay đổi theo yêu cầu 2026-10-22 (vòng sửa thứ tư)

| Thay đổi | Khác thiết kế | Code |
|---|---|---|
| Bỏ trạng thái Đã duyệt: "Xếp lịch" mở lịch ngay từ bản nháp (trang tự kiểm trước tên bài, rubric các phần); phát hành nhận bài `DRAFT` (bài `REVIEWED` cũ vẫn được), luật duyệt của BR-U07-32 kiểm ngay khi phát hành, thiếu thì trả `ASSIGNMENT_NOT_READY` kèm chỗ thiếu; quyền `canPublish` có cho mọi bài còn sửa được. Bài `REVIEWED` cũ hiện là "Nháp", lọc "Nháp" lấy cả bài đó. API duyệt (`POST /assignments/{id}/review`) còn nhưng giao diện không gọi | Bỏ bước DRAFT → REVIEWED của BR-U07-32 khỏi luồng (luật kiểm giữ nguyên, chuyển sang lúc phát hành) | `AssignmentPublicationService.publish`, `AssignmentSupport` (quyền), `AssignmentFormPage` (`openSchedule`, `localMissing`), `types.ts`, `AssignmentFilters` |
| Ghi nhận hoạt động trong popup Cài đặt lại là nút gạt như bản cũ | Giao diện | `AssignmentSettingsPanel` |
| Nút ✕ không được phép vẫn hiện nhưng mờ, như "−" của hàng, cột của đề: bảng người học điền ("Bảng của đề"), sơ đồ, ảnh của đề trong phần ("Sơ đồ của đề", "Ảnh của đề") | Giao diện | `DocumentEditor` (`CornerDelete`, `TableEditor`) |

Unit test thêm: phát hành thẳng từ bản nháp (đủ thì lên lịch, thiếu thì `ASSIGNMENT_NOT_READY`, bài đang mở thì từ chối); frontend: ✕ mờ ở bảng, sơ đồ của đề, nhãn và bộ lọc Nháp.

## Còn lại

- Trang lớp của U05 chưa đọc `?tab=evals`; nút quay lại từ Assignment Form về trang lớp, chưa mở thẳng tab Đánh giá.
- Bước 14 Review: đối chiếu UC 34–36, story, BR-U07 với code, người dùng duyệt.
