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

## Còn lại

- Trang lớp của U05 chưa đọc `?tab=evals`; nút quay lại từ Assignment Form về trang lớp, chưa mở thẳng tab Đánh giá.
- Bước 14 Review: đối chiếu UC 34–36, story, BR-U07 với code, người dùng duyệt.
