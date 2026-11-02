# U08 Assignment Work — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U08 Assignment Work thuộc Iter 2, người code: Người 3; UC 37–41. Lấy phần Assignment cá nhân, phản hồi Practice và dữ liệu hoạt động của U11 cũ. Lượt Quiz thuộc U11 mới, bài nhóm thuộc U12. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.
> Cập nhật 2026-10-22 theo code hiện tại (các yêu cầu của người dùng 2026-10-21, 2026-10-22; chi tiết từng vòng trong code-summary).

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 38 | Student Assignments (`s19`) | `StudentAssignmentsPage` | 37 | Student |
| 36 | Student Class Detail, phần Bài tập sắp đến hạn (`s02`) | `ClassAssignmentsCard` | 37 | Student |
| 30 | Class Dashboard của Student, dòng bài trên thẻ lớp (`s01`) | `ClassCardAssignments` | 37 | Student |
| 39 | Assignment Detail: bài Graded đang làm (`s12`), bài Practice (`s12b`), bài Graded đã công bố điểm (`s12c`), bài nhóm (`s12d`) | `StudentAssignmentDetailPage` | 38 | Student |
| 40 | Assignment Workspace bài cá nhân (`s16`), popup Nộp bài (`s16b`), Biên nhận sau khi nộp (`s16c`) | `AssignmentWorkspacePage`, `SubmitDialog`, `ReceiptPage` | 39 | Student |
| 41 | Submission History: bài Practice (`s13`), bài Graded đã chấm (`s13b`), popup Nhận góp ý AI (`s13c`) | `SubmissionHistoryPage`, `PracticeFeedbackPanel` | 40, 41 | Student |

U08 đăng ký `NavEntry` "Bài tập" (Student). Chế độ nhóm của Assignment Workspace (UC 56) do U12 làm, dùng lại `DocumentEditor` của U07 và `useActivityTracker` của U08.

## 2. Cây component

```
(Student Class Detail, đăng ký qua StudentClassSection của U05)
  ClassAssignmentsCard                       "Bài tập sắp đến hạn" của lớp, lối vào danh sách đã lọc
(Class Dashboard, đăng ký qua ClassCardSection của U05)
  ClassCardAssignments                       dòng "n bài sắp đến hạn" trên thẻ lớp
app/my-assignments/                 StudentAssignmentsPage       #38 Student Assignments
  AssignmentFilters, StudentAssignmentTable
  (vùng StudentAssignmentResult cho U09)
app/my-assignments/[assignmentId]/  StudentAssignmentDetailPage  #39 Assignment Detail
  AssignmentInfoCard                          lớp, hình thức, chế độ, số phần, sơ đồ cần có, lịch, số lượt
  ActivityNotice                              thông báo theo dõi khi bật
  AttemptSummary, StartAttemptButton
  PracticeAiNote                              bài Practice: cách nhận góp ý AI
  (vùng SubmissionResultSection cho U09: kết quả đã công bố)
  (vùng GroupWorkEntry cho U12)
  (vùng AssignmentMaterialSection cho U13)
app/attempts/[attemptId]/           AssignmentWorkspacePage      #40 Assignment Workspace
  PageHeader                                  tên · lượt, trạng thái lưu, đếm ngược, Nộp bài
  DocumentEditor (U07, chế độ người học)      thanh công cụ có Nhập DOCX, Tải DOCX; không có rubric
  DocxImportDialog, SubmitDialog
app/attempts/[attemptId]/receipt/   ReceiptPage                  biên nhận sau khi nộp
app/my-assignments/[assignmentId]/history/  SubmissionHistoryPage  #41 Submission History
  AttemptList, AttemptViewer, DocxDownloadButton
  PracticeFeedbackPanel                       góp ý AI của bài Practice, AiSourcePicker (U06, chỉ nguồn cá nhân), AiTaskStatus (U06)
  (vùng SubmissionResultSection cho U09)
shared/activity/
  useActivityTracker                          dùng chung U08, U11, U12
  ActivityTimeline                            U09, U11 dùng
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `StudentAssignmentsPage` | `GET /api/v1/me/assignments` | "Bài tập"; lọc Lớp, Hình thức, Chế độ, trạng thái; cột Bài tập (tên, mã lớp, "Của môn"), Hình thức, Chế độ (hai cột riêng), Hạn nộp, Trạng thái (Chưa làm, Đang làm, Đã nộp n lượt; bài nhóm từ U12), Điểm (U09 qua `StudentAssignmentResult`; Practice "Không tính điểm"), thao tác Làm bài, Tiếp tục, Xem; sắp theo hạn |
| `ClassAssignmentsCard` | `GET /api/v1/me/assignments?classId=` | "Bài tập sắp đến hạn" trên Student Class Detail: tên, hình thức · chế độ, hạn, trạng thái; link "Xem tất cả bài của lớp" mở Student Assignments đã lọc lớp |
| `ClassCardAssignments` | `GET /api/v1/me/assignments` | Dòng trên thẻ lớp: "n bài sắp đến hạn", "n bài đang mở" hoặc "Chưa có bài" |
| `StudentAssignmentDetailPage` | `GET /api/v1/me/assignments/{assignmentId}` | Nút quay lại; tên kèm nhãn chế độ, hình thức; Thông tin bài: Lớp, Hình thức, Chế độ (Graded, hoặc Practice không tính điểm, có góp ý AI sau khi nộp; không ghi tổng điểm), Số phần cần làm, Sơ đồ cần có, Mở lúc, Hạn nộp, Nộp trễ, Số lượt (đã dùng / tối đa · còn); lời báo ghi nhận hoạt động khi bài bật theo dõi; học liệu gắn kèm (U13); phần Làm bài: lượt gần nhất, nút Bắt đầu làm bài hoặc "Tiếp tục lượt n" (kèm thời điểm tự lưu) hoặc lý do chưa làm được, link Xem lịch sử nộp bài; bài Practice có phần Góp ý AI; bài Graded đã công bố có phần Kết quả (U09); bài nhóm do U12 gắn. Không hiện đề và rubric |
| `StartAttemptButton` | `POST /api/v1/assignments/{assignmentId}/attempts` | Trả lượt đang làm nếu đã có |
| `AssignmentWorkspacePage` | `GET /api/v1/attempts/{attemptId}`, `PUT .../content` | Đầu trang: tên · Lượt n, trạng thái lưu, đếm ngược tới hạn, Nộp bài. Trang tài liệu: tên bài, hướng dẫn, nhãn hình thức, chế độ (không ghi tổng điểm); các phần theo cây (cùng tầng cùng màu, lá khung màu ruột trắng); chữ của giảng viên nhạt, không sửa được; người học viết trong ruột trắng của phần lá, không có chữ gợi ý; bấm sơ đồ của đề để vẽ (Draw.io ở cột phải); điền bảng của đề (đủ nút "+ −", hàng, cột, ✕ của đề mờ, chỉ xóa phần mình thêm); sơ đồ, ảnh, bảng của mình có ✕ ở góc. Thanh công cụ: định dạng theo vùng bôi đen, chèn bảng, ảnh, sơ đồ, Nhập DOCX (chèn vào phần đang chọn), Tải DOCX. Thông báo chỉ ghi trạng thái ("Bài đang mở ở nơi khác, bản đang sửa chưa lưu", "Mất kết nối, chưa lưu", "Đã nộp lúc … · chỉ xem", "Đã quá hạn") |
| `DocxImportDialog` | `POST .../docx:preview` | Xem trước rồi chèn vào cuối phần đang chọn |
| `SubmitDialog` | `POST .../submit` | "Nộp bài lượt n?": bảng Mục, Kiểm tra (kiểm sơ bộ: tổng số sơ đồ bắt buộc, có nội dung; rồi lỗi `validateForSubmit` của server theo phần; hoặc Hợp lệ); còn lỗi thì chỉ có Quay lại sửa; "Sau khi nộp, lượt này không sửa được. Bạn còn n lượt sau lượt này." |
| `ReceiptPage` | `GET /api/v1/attempts/{attemptId}` | "Đã nộp bài": tên · lượt x / y, Mã biên nhận, Thời gian nộp (giờ Việt Nam), Tình trạng (Đúng hạn hoặc Trễ), Nội dung (số mục, số sơ đồ); nút Tải DOCX, Xem lịch sử nộp bài, Về danh sách bài tập |
| `SubmissionHistoryPage`, `AttemptList`, `AttemptViewer`, `DocxDownloadButton` | `GET /api/v1/assignments/{assignmentId}/attempts/mine`, `GET /api/v1/attempts/{attemptId}`, `GET .../export.docx` | Đường dẫn "Bài tập › tên › Lịch sử nộp bài"; "Bài nộp · lượt n" (thời điểm nộp · mã biên nhận), Tải DOCX, nội dung chỉ đọc; "Các lượt nộp": mỗi lượt kèm thời điểm, bài Graded ghi "Được chấm" ở lượt nộp cuối và "Không chấm" ở lượt khác, bài Practice ghi đã có hoặc chưa có góp ý AI; bài Graded có phần Kết quả (U09) |
| `PracticeFeedbackPanel` | `POST`, `GET .../feedback` | Chỉ bài `PRACTICE`: nút Nhận góp ý AI mở popup chọn API cá nhân (chưa có khóa thì link sang Kết nối AI); góp ý đã có: nhãn "AI ước tính, không phải điểm chính thức", nguồn · thời điểm, nhận xét chung, điểm mạnh, cần cải thiện, theo từng phần (tên phần); không có ước tính theo rubric. Góp ý do agent MCP ghi qua `record_private_feedback` hiện cùng chỗ với nhãn Agent |
| `useActivityTracker` | `POST .../activity` (hoặc API của U11, U12) | Dùng trạng thái hiển thị của trang, không dùng blur; heartbeat 60 giây; gom lô, gửi lại khi lỗi |

Route `/my-assignments/*` và `/attempts/*` chỉ cho Student (dùng `RoleGuard` của U01). Backend vẫn kiểm quyền thật.
