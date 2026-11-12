# U07 Assignment Authoring — frontend-components

## Nhập đề từ ảnh/PDF — 2026-11-11

Thiết kế, chưa có mã. Quy tắc: BR-U07-87…101.

### `ExamImportDialog` (mới, `features/assignments/components`)

- **Props**: `assignmentId`, `version`, `hasContent`, `initialFiles?`, `autoStart?`, `onApplied(detail, previous)`, `onClose`.
- **Trạng thái**: `PICK`, `RUNNING`, `PREVIEW`, `ERROR`.

| Bước | Giao diện |
|---|---|
| `PICK` | Vùng chọn tệp (`accept=".png,.jpg,.jpeg,.webp,.pdf"`, nhiều tệp). Danh sách tệp có ảnh nhỏ, nút lên/xuống, kéo thả để đổi thứ tự, nút gỡ. Kiểm ngay theo policy `ASSIGNMENT_IMPORT` (`getPolicies`, `checkFile`): loại tệp, dung lượng, tối đa 5 ảnh hoặc 1 PDF, không trộn. Nút "Chuyển thành đề" chỉ bật khi danh sách hợp lệ. |
| `RUNNING` | Popup chờ dùng chung `AiProgress` ("Đang đọc đề…"), có nút "Hủy" gọi `:discard`. Hỏi trạng thái bản nháp như `AiDraftDialog`. |
| `PREVIEW` | Xem trước chỉ đọc bằng trình hiển thị tài liệu hiện có. Đầu màn: số heading, bảng, hình, số chỗ không đọc rõ. Cảnh báo dạng `Alert` có danh sách. Ô "Dùng '{title}' làm tên bài" (mặc định tắt, chỉ hiện khi có `title`). Nút: khung trống thì "Ghi vào đề"; khung có nội dung thì "Thay toàn bộ" và "Chèn vào cuối"; "Bỏ" gọi `:discard`. |
| `ERROR` | Thông báo lỗi tiếng Việt theo mã (BR-U07-88, 100, U06), nút "Thử lại" (requestRef mới) và "Đóng". |

Trạng thái có `aria-live="polite"`; mọi nút dùng được bằng bàn phím; ở 375px danh sách tệp xếp một cột.

### Trang soạn bài (`AssignmentFormPage`)

- Menu thêm (cạnh "Xuất DOCX") có mục "Nhập đề từ tệp" (icon `upload_file`). Chỉ hiện khi sửa được bài và bài chưa phát hành. Mục này lưu thay đổi đang dở (như `saveForAi`) rồi mở `ExamImportDialog`.
- Tệp `.docx` chọn trong dialog đi đường nhập DOCX cũ (`importDocx`), cùng màn xem trước, không gọi AI.
- Ghi xong: đưa bản trước vào lịch sử trình soạn (cơ chế "đề AI ghi thẳng lên trang"), toast "Đã nhập đề. Nhấn Ctrl+Z để quay lại bản trước."
- Có import đang chờ từ popup tạo bài thì tự mở dialog với `initialFiles` và `autoStart`.

### Popup tạo bài (`CreateAssignmentDialog`)

- Lựa chọn "Đề mới", ô "Mẫu đề" (`TemplateFileField`) nhận `.docx,.png,.jpg,.jpeg,.webp,.pdf`; ảnh chọn được tối đa 5. Gợi ý: "DOCX, ảnh hoặc PDF đề cũ".
- DOCX: giữ luồng cũ (nhập và lưu ngay).
- Ảnh/PDF: tạo bài, đặt tệp vào `pendingImports` (bộ nhớ module, theo `assignmentId`), rồi `onCreated`. Trang soạn bài mở `ExamImportDialog` tự chạy. Lỗi gửi tệp: bài vẫn được giữ, báo "Đã tạo bài, chưa nhập tệp: …".

### API client (`features/assignments/api`)

- `startImport(id, files, { requestRef, source?, provider? })`: multipart `POST /assignments/{id}/imports`.
- `applyAiDraft(id, draftId, { version, placement, replaceTitle })`: thêm `placement`.
- Dùng lại `getAiDraft`, `discardAiDraft`.
- Kiểu `AiDraft` thêm `mode`. Đề xuất `IMPORT`: `document`, `warnings`, `counts`, `title`.

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.
> Cập nhật 2026-10-22 theo code hiện tại (các yêu cầu của người dùng 2026-10-21, 2026-10-22; chi tiết từng vòng trong code-summary).

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## Popup tạo bài, trình soạn câu hỏi, lịch — 2026-11-10

Nguồn: `211927b0`, `4ff7a0da`, `6a787e3f` (PR #60). Phần này thay mô tả trái ngược của `CreateAssignmentDialog`, `AiDraftDialog`, `DocumentEditor`, `ScheduleForm` và `SubjectAssignmentListPage` ở các bảng bên dưới.

- `CreateAssignmentDialog`: tên bài bắt buộc; "Cách tạo" gồm Đề mới (tệp DOCX mẫu), Đề có sẵn (`SearchPicker` tìm theo tên; đề mẫu của môn đứng đầu, rồi bài các lớp mình dạy) và Tạo từ AI (Ngôn ngữ, Mẫu đề, Tài liệu tham khảo, Yêu cầu, tiến độ AI ngay trong popup); mỗi lựa chọn có một câu gợi ý. API thêm `GET .../assignment-materials`, `GET .../assignments/materials`, `GET .../ai-drafts/templates`; copy gửi kèm `title`. Bỏ `templateStore` (ghi nhớ mẫu đề).
- `SubjectAssignmentListPage`: tiêu đề "Đề mẫu bài tập", tạo đề mẫu; không có phát hành.
- `AssignmentFormPage`: bỏ nút Soạn bằng AI trên thanh công cụ; `AiDraftDialog` chỉ mở khi duyệt đề xuất có sẵn; `AiReviseChat` giữ nguyên.
- `DocumentEditor`: mục là câu hỏi, đánh số `I.`/`I.1.`/`I.1.a.`/`I.1.a.aa.` (`SectionNumber`, bốn tầng); thanh "+ Thêm câu hỏi" và "+ Thêm ý" luôn hiện; nút "+ Câu hỏi" trên thanh công cụ thêm vào cuối khi không có con trỏ; nút Cả trang / Từng phần luôn hiện kể cả khi chưa có câu hỏi; placeholder ngắn cho mô tả, tên câu hỏi, dòng rubric và ô trả lời của sinh viên.
- `AssignmentSettingsPanel`: gợi ý một dòng và info tip; công tắc phân bố điểm (U09 `GradeDistributionSettings`) không còn ghi "ẩn danh".
- `ScheduleForm`: ô "Số lượt làm" chỉ hiện với bài Luyện tập; bài có chấm gửi `maxAttempts = null`.

Tests nguồn: `AiDraftDialog.test.tsx`, `assignments.test.tsx`, `outline.test.tsx`, `authoringFlow.test.tsx`, `document.test.tsx`.

## 1. Màn hình

Theo design G21, đổi theo các yêu cầu 2026-10-21, 2026-10-22: Assignment Form thành một trang tài liệu kiểu Google Docs, không còn thanh bước, popup Rubric Detail và hộp nhập DOCX riêng.

| # | Màn | Component | UC | Role |
|---|---|---|---|---|
| 25 | Assignment List | `SubjectAssignmentListPage` | 34, 35 | Subject Manager |
| 26 | Assignment Form: trang soạn kiểu Google Docs, xem trước, popup Xếp lịch, Cài đặt bài, Soạn bằng AI | `AssignmentFormPage` | 35, 36 | Người có quyền với bài; giảng viên lớp chỉ xem bài của môn |
| 32 | Teacher Class Detail tab Đánh giá, phần Bài tập; popup Tạo bài tập | `ClassAssignmentsSection`, `CreateAssignmentDialog` | 34, 35 | Teacher, Subject Manager được giao dạy |
| 17, 18 | Số bài tập của môn trên Manager Dashboard, My Subject | `AssignmentSummary` | 34 | Subject Manager |

U07 đăng ký `NavEntry` "Bài tập" (Subject Manager) mở Assignment List.

## 2. Cây component

```
(TeacherClassTab "evals" của U05)
  ClassAssignmentsSection                     bảng bài của lớp và của môn, lọc, nút Tạo bài tập
    AssignmentFilters, AssignmentTable
    CreateAssignmentDialog                    ô tệp DOCX, Hình thức, Chế độ (Choice); nút Copy từ lớp khác
    (vùng AssignmentRowAction cho U09)
(SubjectSummarySection của U05)
  AssignmentSummary                           số bài tập của môn theo trạng thái
app/manager/assignments/  SubjectAssignmentListPage   #25 Assignment List
app/assignments/[id]/     AssignmentFormPage          #26 Assignment Form
  PageHeader                                  tên, trạng thái, đã lưu; Khác, Xem trước/Quay lại, Xếp lịch/Đổi lịch
  DocumentEditor (chế độ người soạn)          trang giấy, thanh công cụ, cây phần
    CommentRail + RubricNote + RubricEditor   thẻ rubric atomic bên lề từng phần
    DiagramEditor                             Draw.io ở cột phải
  AssignmentSettingsPanel (popup Cài đặt)     Hình thức, Chế độ (Choice), nút gạt Ghi nhận hoạt động
  ScheduleForm (popup Xếp lịch, Đổi lịch)
  AiDraftDialog                               AiSourcePicker (U06), AiTaskStatus (U06), xem trước đề xuất
  AiReviseChat                                khung chat "Sửa đề bằng AI" trên thẻ rubric Toàn bài (bài DRAFT sửa được)
  RetireDialog, popup Xóa bản nháp
  (vùng AssignmentFormSection cho U12: kiểm tra sẵn sàng của bài nhóm)
  (vùng AssignmentSettingsSection / assignmentSettingsSections cho U09: cài đặt phân bố theo bài–lớp)
app/assignments/[id]/diagrams/[blockId]/   DiagramViewPage   trang xem sơ đồ từ link trong DOCX
shared/document/
  DocumentEditor, RichText, runs              trình soạn dùng chung U07, U08, U12; định dạng theo vùng bôi đen
  DiagramEditor                               Draw.io nhúng ở cột phải kéo giãn được
  DocumentViewer, CommentRail, RubricNote, RubricViewer, DocxImportDialog
shared/ui/PageHeader                          màn hẹp thì hàng nút xuống dưới tiêu đề
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `SubjectAssignmentListPage` | `GET /api/v1/subjects/{subjectId}/assignments` | "Bài tập của môn"; chọn môn, Tạo bài tập; tab Tất cả, Graded, Practice; lọc trạng thái (Nháp, Đã lên lịch, Đang mở, Đã đóng, Ngưng giao; Nháp gồm cả bài `REVIEWED` cũ); ô "Hiện bài Đã đóng / Ngưng giao"; ô tìm; bảng `AssignmentTable` |
| `ClassAssignmentsSection` | `GET /api/v1/classes/{classId}/assignments` | Trong tab Đánh giá: nút Tạo bài tập, lọc, bảng `AssignmentTable`; bài của môn chỉ xem; mặc định ẩn bài Đã đóng, Ngưng giao |
| `AssignmentTable` | - | Cột Bài tập (tên; version > 1, nguồn copy hoặc version mới; bài nhân bản không ghi nhãn), Hình thức, Chế độ (hai cột riêng), Phạm vi, Trạng thái, Hạn nộp, Bài nộp (U09) |
| `CreateAssignmentDialog` | `POST .../assignments`, `POST .../skeleton:import-docx`, `PUT .../skeleton`, `GET .../assignment-copy-sources`, `POST .../assignments:copy` | Ô tệp DOCX tùy chọn (chọn xong chỉ hiện tên tệp, ✕ bỏ tệp); thẻ chọn Hình thức (Cá nhân, Nhóm khi U12 có), Chế độ (Graded, Practice; nhóm khóa Graded); nút nhỏ "Copy từ lớp khác" (bài của lớp) đổi sang danh sách bài để copy; Hủy, Tạo. Có tệp thì tạo bài rồi nhập luôn làm khung; lỗi thì "Đã tạo bài, chưa nhập tệp", nút Thử lại (chỉ nhập lại tệp), Mở bài. Không có dòng hướng dẫn |
| `AssignmentFormPage` | `GET`, `PATCH /api/v1/assignments/{id}`, `PUT .../skeleton` | Đầu trang: tên, nhãn trạng thái, "Đã lưu lúc hh:mm"; nút Khác (Xuất DOCX, Xuất rubric, Nhân bản, Tạo version mới, Ngưng giao, Xóa bản nháp), Xem trước hoặc Quay lại, Xếp lịch (bài nháp) hoặc Đổi lịch (xanh). Trang giấy: tên bài (chữ gợi ý "Tên bài tập"), hướng dẫn chung, nhãn hình thức, chế độ, tổng điểm, rồi khung. Tự lưu 1,5 giây, Ctrl+S lưu ngay; `409` báo "Bài đã thay đổi ở nơi khác, bản đang sửa chưa lưu" kèm Tải lại. Thông báo chỉ ghi trạng thái ("Bài còn chỗ chưa đủ", "Chưa lưu: …", "Đã phát hành", "Đã lên lịch", "Đã lưu lịch", "Đã ngưng giao"); chỗ thiếu tô đỏ ngay trong bài |
| `DocumentEditor` (người soạn) | - | Thanh công cụ dính: hoàn tác, làm lại, kiểu đoạn (Văn bản thường, Tiêu đề 1–4), B, I, U, S, màu chữ, màu nền (bôi đen thì chỉ đổi phần bôi đen), căn lề, danh sách chấm, số, thụt lề, chèn bảng (lưới 10 × 8 ô 18px), chèn ảnh, chèn sơ đồ (một nút, mở Draw.io ngay); Soạn bằng AI, Cài đặt bài. Cây phần: cùng tầng cùng màu, lá khung màu ruột trắng; không có chữ gợi ý ở dòng trống. Sơ đồ, ảnh có ✕ ở góc trên trái. Bảng: "+ −" nổi trên đầu cột và bên trái hàng, ✕ ở góc cùng hàng với nút của cột; "−" khi còn một hàng hoặc một cột là xóa bảng |
| `RubricNote`, `RubricEditor`, `CommentRail` | `GET`, `PUT .../parts/{partId}/rubric` | Thẻ rubric của phần bắt đầu ngang tiêu đề phần, dính khi cuộn qua đầu phần; tên phần, tổng điểm; mỗi dòng một mục chấm (nội dung, điểm), ✕ xóa; dấu "+" ở giữa dòng cuối thêm mục, Enter thêm mục dưới; bài đã phát hành chỉ xem (Đã khóa) |
| `DiagramEditor` | - | Draw.io từ `embed.diagrams.net` trong cột phải kéo giãn được (mặc định 60% bề ngang), đẩy trang sang trái, thu menu bên trái của ứng dụng, ẩn thẻ rubric; kiểm origin và iframe của message; lưu XML và SVG (`xmlsvg`) |
| `AssignmentSettingsPanel` (popup Cài đặt) | `PATCH /api/v1/assignments/{id}` | Thẻ chọn Hình thức (chỉ xem), Chế độ (Graded, Practice; nhóm khóa Graded); nút gạt Ghi nhận hoạt động (chưa có thời hạn giữ dữ liệu thì khóa) kèm Lời báo cho sinh viên; bài nhóm có phần của U12. Không còn mục sơ đồ bắt buộc, học liệu gắn kèm |
| `ScheduleForm` | `POST .../publish`, `PATCH .../schedule` | Một trang: tích "Hẹn giờ mở" mới chọn ngày, giờ mở (không tích là mở ngay); Hạn nộp (ngày, giờ; mặc định 23:59); tích "Cho nộp muộn" mới chọn hạn nộp muộn (≤ 30 ngày sau hạn); Số lượt nộp 1–10; nút Phát hành ngay, Xếp lịch hoặc Lưu lịch. Bài đang mở không hiện ngày mở, không đổi số lượt. Lỗi: "Thiếu hạn nộp", "Hạn nộp không sau lúc mở", "Hạn nộp muộn quá 30 ngày sau hạn nộp", "Số lượt ngoài 1–10"; cảnh báo nhóm của U12 kèm ô "Vẫn phát hành" |
| Xem trước | `GET /api/v1/assignments/{id}` | Đề như người học thấy khi làm bài, chỉ đọc, không có rubric; Quay lại, Xếp lịch |
| `AiDraftDialog` | `POST .../ai-drafts`, `GET .../ai-drafts/{draftId}`, `:apply`, `:discard` | Yêu cầu*, tài liệu tham khảo, nguồn AI (`AiSourcePicker`); đề xuất các mục kèm rubric gợi ý; Bỏ, Áp dụng |
| `AiReviseChat` | `POST .../ai-drafts` (`mode: REVISE`), `GET .../ai-drafts/{draftId}`, `:apply`, `:discard` | Ô "Yêu cầu sửa đề" (≤ 4.000 ký tự, Enter gửi, Shift+Enter xuống dòng); lưu thay đổi đang chờ, hỏi kết quả mỗi 2 giây, tối đa 3 phút; tự áp dụng rồi tải lại đề, Ctrl+Z quay lại khung đề trước; hiện tối đa 3 lưu ý; báo khi AI không đổi gì, bản sửa không hợp lệ hoặc đề đã đổi (`AI_DRAFT_STALE`), khi đó đề không đổi |
| `RetireDialog`, Xóa bản nháp | `/retire`, `DELETE` | Ngưng giao bắt buộc lý do; Xóa chỉ với bản nháp |
| `AssignmentSummary` | `GET /api/v1/subjects/{subjectId}/assignments` | Số bài tập của môn (đang mở, nháp gồm cả bài `REVIEWED` cũ) |

Route `/assignments/*` và `/manager/assignments` cho Teacher, Subject Manager (dùng `RoleGuard` của U01). Backend vẫn kiểm quyền thật.

| Slot U07 khai báo | Unit gắn | Dùng để |
|---|---|---|
| `AssignmentRowAction` | U09 | Cột Bài nộp trên dòng bài ở tab Đánh giá |
| `AssignmentFormSection` | U12 | Kiểm tra sẵn sàng nhóm trên Assignment Form của bài nhóm |
| `AssignmentSettingsSection` (`assignmentSettingsSections`) | U09 | Cài đặt phân bố cho bài GRADED khi có classId. Quyền `canEdit` của U09 độc lập với readOnly của nội dung bài, kể cả bài SUBJECT chỉ đọc |
