# U07 Assignment Authoring — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U07 Assignment Authoring thuộc Iter 2, người code: Người 4; UC 34–36. Lấy phần Assignment của U08 cũ (vòng đời, phát hành), U09 cũ (tài liệu, khung, DOCX) và rubric của U06 cũ. Quiz và ngân hàng thuộc U10. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 25 | Assignment List (`t09`) | `SubjectAssignmentListPage` | 34, 35 | Subject Manager |
| 26 | Assignment Form: bước 1 Nội dung (`t13`), bước 3 Lịch & lượt nộp (`t13c`), bước 4 Xem trước và thao tác (`t13d`), bài nhóm (`t15`), popup Tạo nháp bằng AI (`t13b`), Nhập DOCX (`t13e`) | `AssignmentFormPage` | 35 | Người có quyền với bài; giảng viên lớp chỉ xem bài của môn |
| 27 | Rubric Detail, popup (`t14`) | `RubricDetailDialog` | 36 | Như Assignment Form |
| 32 | Teacher Class Detail tab Đánh giá, phần Bài tập (`t21`), popup Tạo bài tập (`t21b`) | `ClassAssignmentsSection`, `CreateAssignmentDialog` | 34, 35 | Teacher, Subject Manager được giao dạy |
| 17, 18 | Số bài tập của môn trên Manager Dashboard, My Subject (`m11`, `m01`) | `AssignmentSummary` | 34 | Subject Manager |

U07 đăng ký `NavEntry` "Bài tập" (Subject Manager) mở Assignment List.

## 2. Cây component

```
(TeacherClassTab "evals" của U05)
  ClassAssignmentsSection                     dòng bài của lớp và của môn trong bảng tab Đánh giá
    CreateAssignmentDialog                    t21b: tạo mới hoặc copy, cá nhân hay nhóm, chế độ, sơ đồ
    CopySourcePicker                          lớp mình dạy và bài để copy
    (vùng AssignmentRowAction cho U09)
(SubjectSummarySection của U05)
  AssignmentSummary                           số bài tập của môn theo trạng thái
app/manager/assignments/  SubjectAssignmentListPage   #25 Assignment List
app/assignments/[id]/     AssignmentFormPage          #26 Assignment Form
  AssignmentTopBar                            Thoát, tên, nhãn trạng thái, chế độ, hình thức, "Đã lưu", nút thao tác
  AssignmentStepper                           1 Nội dung · 2 Rubric · 3 Lịch & lượt nộp · 4 Xem trước
  RubricMissingAlert                          mục chưa có rubric
  PartList                                    khung tài liệu: các mục và điểm; Thêm mục, Nhập DOCX, Tạo nháp bằng AI
  PartEditor                                  hướng dẫn chung, tiêu đề mục, điểm mục, hướng dẫn cho sinh viên
    SkeletonEditor                            DocumentEditor chế độ người soạn
    RubricDetailDialog                        #27 popup Rubric Detail
  AssignmentSettingsPanel                     chế độ, hình thức, ghi nhận hoạt động, sơ đồ, học liệu gắn kèm
  ScheduleStep                                bước 3
  PreviewStep                                 bước 4: xem như sinh viên và các thao tác
  AiDraftDialog                               t13b: AiSourcePicker (U06), AiTaskStatus (U06), xem trước đề xuất
  DocxImportDialog                            t13e
  DocxExportButton, ReviewButton, PublishDialog, ScheduleDialog, RetireDialog, CloneButton, NewVersionButton, DeleteButton
  (vùng AssignmentFormSection cho U12: kiểm tra sẵn sàng của bài nhóm)
app/assignments/[id]/diagrams/[blockId]/   DiagramViewPage   trang xem sơ đồ từ link trong DOCX
shared/document/
  DocumentEditor                              trình soạn block dùng chung U07, U08, U12
  DiagramBlock                                ảnh xem trước, mở Draw.io nhúng
  DocumentViewer, RubricViewer
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `SubjectAssignmentListPage` | `GET /api/v1/subjects/{subjectId}/assignments` | "Bài tập của môn"; ghi chú "Bài cá nhân do Chủ nhiệm môn soạn cho cả môn, giao cho mọi lớp đang mở của môn theo một lịch chung; giảng viên lớp chỉ xem và chấm. Bài nhóm chỉ có ở bài của lớp." (BR-U07-02); nút chọn môn, Tạo bài tập; tab Tất cả, Graded, Practice; lọc trạng thái (Nháp, Đã duyệt, Đã lên lịch, Đang mở, Đã đóng, Ngưng giao); ô "Hiện bài Đã đóng / Ngưng giao" (mặc định tắt); ô tìm; cột Bài tập (tên, nhãn "Có sơ đồ Draw.io"), Hình thức, Chế độ, Trạng thái, Cập nhật; chân bảng ghi số bài đang ẩn và "Quiz của môn xem ở mục Quiz." |
| `ClassAssignmentsSection` | `GET /api/v1/classes/{classId}/assignments` | Trong bảng chung của tab Đánh giá (cùng phần Quiz của U10): nút Tạo bài tập; bộ lọc Tất cả / Bài tập / Quiz và lọc trạng thái; cột Bài (tên, "Bài tập · Cá nhân hoặc Nhóm · Graded hoặc Practice"), Phạm vi (Của môn, Của lớp), Trạng thái, Bài nộp (U09 gắn qua `AssignmentRowAction`); bài của môn chỉ xem; mặc định ẩn bài Đã đóng, Ngưng giao; ghi chú "Bài và quiz “Của môn” do Chủ nhiệm môn soạn, bạn chỉ xem và chấm. Bài nhóm chỉ tạo được ở lớp." |
| `CreateAssignmentDialog`, `CopySourcePicker` | `POST .../assignments`, `GET .../assignment-copy-sources`, `POST .../assignments:copy` | "Tạo bài tập" của lớp: Cách tạo (Tạo mới, hoặc Copy từ lớp khác: chọn bài ở lớp mình dạy), Hình thức (Cá nhân; Nhóm theo nhóm cố định của lớp, trưởng nhóm nộp, chỉ hiện khi U12 có), Chế độ (Graded, Practice; bài nhóm khóa Graded), Sơ đồ Draw.io (Không dùng, Có sơ đồ); Hủy, Tiếp tục mở Assignment Form |
| `AssignmentTopBar`, `AssignmentStepper` | `GET /api/v1/assignments/{id}` | Thoát, tên bài, nhãn trạng thái, chế độ, hình thức, "Đã lưu hh:mm"; nút Xuất DOCX, Lưu nháp, Duyệt (khi Nháp) hoặc Phát hành (khi Đã duyệt); bài nhóm hiện "Lớp · môn"; thanh bước |
| `RubricMissingAlert` | - | "Mục X chưa có rubric. Cần đủ rubric cho mọi mục trước khi duyệt." |
| `PartList`, `PartEditor`, `SkeletonEditor` | `PATCH /api/v1/assignments/{id}`, `PUT .../skeleton` | Danh sách mục (tên, điểm, hoặc "Chưa có rubric"); Thêm mục, Nhập DOCX, Tạo nháp bằng AI. Mục đang soạn: Hướng dẫn chung của bài, Tiêu đề mục*, Điểm của mục (theo rubric, chỉ đọc), Hướng dẫn cho sinh viên (Markdown), nút "Mở rubric của mục" (số tiêu chí · điểm). Lưu tự động khi ngừng gõ 2 giây và khi rời trang; `409` báo tải lại, giữ bản đang sửa |
| `AssignmentSettingsPanel` | `PATCH /api/v1/assignments/{id}`, `GET .../selectable-materials` | Chế độ Graded / Practice ("Graded vào sổ điểm; Practice cho sinh viên nhận góp ý AI"); Hình thức (bài của môn chỉ Cá nhân; bài nhóm theo nhóm cố định của lớp, trưởng nhóm nộp, thành viên tự nhận mục hoặc trưởng nhóm giao); Ghi nhận hoạt động (công tắc) kèm Lời báo cho sinh viên; Dùng sơ đồ Draw.io (công tắc) kèm số sơ đồ bắt buộc theo loại (sơ đồ lớp, sơ đồ tuần tự, loại khác); Học liệu gắn kèm (tối đa 10, khi U13 có). Tắt sơ đồ bị từ chối thì hiện chỗ cần bỏ |
| `RubricDetailDialog` | `GET`, `PUT .../parts/{partId}/rubric` | "Rubric · tên mục"; mỗi tiêu chí có tên, mô tả, loại (Chữ, Sơ đồ) và danh sách mục chấm (nội dung, điểm), Thêm mục chấm; Thêm tiêu chí; "Tiêu chí gồm nhiều mục chấm; giảng viên tick từng mục chấm khi chấm. Tổng điểm tự tính."; bài đã phát hành chỉ xem; Hủy, Lưu rubric |
| `ScheduleStep`, `PublishDialog`, `ScheduleDialog` | `/publish`, `PATCH .../schedule` | Ngày mở, Hạn nộp*, Hạn nộp trễ (tùy chọn; sau hạn nộp, không quá 30 ngày; bài nộp sau hạn được đánh dấu trễ), Số lượt nộp tối đa (1–10; Graded chấm lượt nộp cuối cùng, kể cả nộp trễ); chọn mở ngay khi phát hành hoặc lên lịch mở vào Ngày mở; giờ Việt Nam (UTC+7); thẻ Tóm tắt (Mở, Hạn, Hạn trễ, Lượt); "Sau khi phát hành, đổi lịch được ghi vào Nhật ký audit."; bài đang mở chỉ kéo dài hạn, không rút ngắn, không đổi số lượt (BR-U07-35); lỗi nhóm của U12 hiện trong dialog |
| `PreviewStep` | `GET /api/v1/assignments/{id}` | "Đang xem như sinh viên · Chỉ đọc": tên, chế độ, hạn nộp và số lượt, hướng dẫn chung, lời báo ghi nhận hoạt động, các mục (sơ đồ cần có, điểm), học liệu gắn kèm. Thao tác: Phát hành / Lên lịch, Nhân bản, Tạo version mới, Ngưng giao (bắt buộc lý do), Xóa (chỉ bản nháp) |
| `AiDraftDialog` | `POST .../ai-drafts`, `GET .../ai-drafts/{draftId}`, `:apply`, `:discard` | Yêu cầu*, Tài liệu tham khảo (chỉ học liệu đã trích chữ), Nguồn AI đang chọn kèm "Đổi nguồn" (`AiSourcePicker`, bên chịu chi phí, ước tính token); Đề xuất: các mục kèm rubric gợi ý; provenance; cảnh báo "Áp dụng sẽ thay toàn bộ khung tài liệu hiện tại (n mục)."; Bỏ, Áp dụng |
| `DocxImportDialog` | `POST .../skeleton:import-docx` | Ô tệp, Chọn tệp khác; xem trước các mục đọc được (Heading 1 thành mục; cột #, Mục, Độ dài) và cách khôi phục sơ đồ; Cách nhập: Thay khung hiện tại hoặc Thêm vào cuối; Hủy, Nhập |
| `DocxExportButton` | `GET .../docx` | Xuất đề ra DOCX |
| `RetireDialog`, `CloneButton`, `NewVersionButton`, `DeleteButton` | `/retire`, ... | Ngưng giao bắt buộc lý do; Xóa chỉ với bản nháp |
| `DiagramBlock` | - | Draw.io nhúng từ `embed.diagrams.net` qua iframe, kiểm origin của message; lưu XML và SVG |
| `AssignmentSummary` | `GET /api/v1/subjects/{subjectId}/assignments` | Số bài tập của môn: dòng tóm tắt trên thẻ môn (Manager Dashboard) và thẻ số liệu "Bài tập của môn" (n đang mở · n nháp) trên My Subject |

Route `/assignments/*` và `/manager/assignments` cho Teacher, Subject Manager (dùng `RoleGuard` của U01). Backend vẫn kiểm quyền thật.

| Slot U07 khai báo | Unit gắn | Dùng để |
|---|---|---|
| `AssignmentRowAction` | U09 | Cột Bài nộp trên dòng bài ở tab Đánh giá |
| `AssignmentFormSection` | U12 | Kiểm tra sẵn sàng nhóm trên Assignment Form của bài nhóm |
