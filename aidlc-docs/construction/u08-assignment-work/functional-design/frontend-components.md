# U08 Assignment Work — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U08 Assignment Work thuộc Iter 2, người code: Người 3; UC 37–41. Lấy phần Assignment cá nhân, phản hồi Practice và dữ liệu hoạt động của U11 cũ. Lượt Quiz thuộc U11 mới, bài nhóm thuộc U12. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

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
  InstructionsView, DocumentViewer (U07), RubricViewer (U07)
  AssignmentInfoCard                          hạn nộp, nộp trễ, số lượt, điểm
  ActivityNotice                              thông báo theo dõi khi bật
  AttemptSummary, StartAttemptButton
  PracticeAiNote                              bài Practice: cách nhận góp ý AI
  (vùng SubmissionResultSection cho U09: kết quả đã công bố)
  (vùng GroupWorkEntry cho U12)
  (vùng AssignmentMaterialSection cho U13)
app/attempts/[attemptId]/           AssignmentWorkspacePage      #40 Assignment Workspace
  WorkspaceTopBar                             Thoát, tên · lượt, tự lưu, đếm ngược, Nhập DOCX, Nộp bài
  AssignmentSidePanel                         đề bài, rubric, hình thức, lần làm, công cụ vẽ
  DocumentEditor (U07, chế độ người học), AutosaveStatus
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
| `StudentAssignmentsPage` | `GET /api/v1/me/assignments` | "Bài tập", "Bài tập của các lớp bạn đang học. Quiz luyện tập ở mục Luyện tập Quiz."; tab Sắp đến hạn, Đã nộp, Tất cả; lọc Lớp, Hình thức, Chế độ; cột Bài tập (tên, mã môn), Hình thức · chế độ, Hạn nộp, Trạng thái (Chưa làm, Đang làm, Đã nộp n lượt; bài nhóm từ U12: Nhóm đang làm · x/y, Đã nộp), Điểm (U09 qua `StudentAssignmentResult`; Practice "Không tính điểm"), thao tác Làm bài, Tiếp tục, Mở tài liệu, Xem; sắp theo hạn |
| `ClassAssignmentsCard` | `GET /api/v1/me/assignments?classId=` | "Bài tập sắp đến hạn" trên Student Class Detail: tên, hình thức · chế độ, hạn, trạng thái; link "Xem tất cả bài của lớp" mở Student Assignments đã lọc lớp |
| `ClassCardAssignments` | `GET /api/v1/me/assignments` | Dòng trên thẻ lớp: "n bài sắp đến hạn", "n bài đang mở" hoặc "Chưa có bài" |
| `StudentAssignmentDetailPage` | `GET /api/v1/me/assignments/{assignmentId}` | Nút quay lại; tên kèm nhãn chế độ, hình thức; đường dẫn "Bài tập › tên"; Đề bài; Học liệu gắn kèm (U13); Rubric: bảng Tiêu chí, Mục chấm, Điểm và tổng điểm; thẻ Thông tin: Hạn nộp, Nộp trễ (đến thời điểm, hoặc không cho phép), Số lượt (đã dùng / tối đa), Điểm (Chưa công bố; Practice "không tính điểm"); nút Bắt đầu lượt mới, "Tiếp tục lượt n" kèm "Bạn có một lượt làm dở, tự lưu lúc …", hoặc Đã hết lượt; link Xem lịch sử nộp bài; thông báo ghi nhận hoạt động trước khi bắt đầu khi bài bật theo dõi. Bài Practice có phần "Góp ý AI": sau khi nộp nhận góp ý bằng API cá nhân (hoặc agent MCP tự gọi công cụ). Bài Graded đã công bố có phần Kết quả (U09). Bài nhóm: Nhóm, Mục của bạn, Tiến độ, nút Mở tài liệu nhóm (U12) |
| `StartAttemptButton` | `POST /api/v1/assignments/{assignmentId}/attempts` | Trả lượt đang làm nếu đã có |
| `AssignmentWorkspacePage`, `AutosaveStatus` | `GET /api/v1/attempts/{attemptId}`, `PUT .../content` | Thanh trên: Thoát, tên · Lượt n, "Tự động lưu 10 giây · Đã lưu hh:mm", đếm ngược tới hạn, Nhập DOCX, Nộp bài. Cột trái: Đề bài, Rubric (tổng điểm), Hình thức, Lần làm, Công cụ vẽ. Nội dung: khối của giảng viên chỉ đọc ("Không thể chỉnh sửa"), khối sơ đồ (Mở trình vẽ Draw.io, ảnh xem trước), khối chữ. Lưu 10 giây sau lần sửa cuối và khi rời trang; `409` báo tải lại, giữ bản đang sửa trong trình duyệt |
| `DocxImportDialog` | `POST .../docx:preview` | Xem trước rồi thêm vào cuối |
| `SubmitDialog` | `POST .../submit` | "Nộp bài lượt n?": bảng Mục, Kiểm tra (lỗi `validateForSubmit` theo mục, ví dụ sơ đồ có thay đổi chưa lưu, thiếu sơ đồ bắt buộc; hoặc Hợp lệ); còn lỗi thì chỉ có Quay lại sửa; "Sau khi nộp, lượt này không sửa được. Bạn còn n lượt sau lượt này." |
| `ReceiptPage` | `GET /api/v1/attempts/{attemptId}` | "Đã nộp bài": tên · lượt x / y, Mã biên nhận, Thời gian nộp (giờ Việt Nam), Tình trạng (Đúng hạn hoặc Trễ), Nội dung (số mục, số sơ đồ); nút Tải DOCX, Xem lịch sử nộp bài, Về danh sách bài tập |
| `SubmissionHistoryPage`, `AttemptList`, `AttemptViewer`, `DocxDownloadButton` | `GET /api/v1/assignments/{assignmentId}/attempts/mine`, `GET /api/v1/attempts/{attemptId}`, `GET .../export.docx` | Đường dẫn "Bài tập › tên › Lịch sử nộp bài"; "Bài nộp · lượt n" (thời điểm nộp · mã biên nhận), Tải DOCX, nội dung chỉ đọc; "Các lượt nộp": mỗi lượt kèm thời điểm, bài Graded ghi "Được chấm" ở lượt nộp cuối và "Không chấm" ở lượt khác, bài Practice ghi đã có hoặc chưa có góp ý AI; bài Graded có phần Kết quả (U09) |
| `PracticeFeedbackPanel` | `POST`, `GET .../feedback` | Chỉ bài `PRACTICE`: "Góp ý AI dùng API cá nhân của bạn; sinh viên không dùng API trường."; nút Nhận góp ý AI mở popup chọn API cá nhân (nhà cung cấp, 4 ký tự cuối, "Bạn chịu chi phí", ước tính; chưa có khóa thì link sang Kết nối AI); góp ý đã có: nhãn "AI ước tính, không phải điểm chính thức", nguồn · thời điểm, điểm mạnh, cần cải thiện, ước tính; Nhận góp ý lại. Góp ý do agent MCP ghi qua `record_private_feedback` hiện cùng chỗ với nhãn Agent |
| `useActivityTracker` | `POST .../activity` (hoặc API của U11, U12) | Dùng trạng thái hiển thị của trang, không dùng blur; heartbeat 60 giây; gom lô, gửi lại khi lỗi |

Route `/my-assignments/*` và `/attempts/*` chỉ cho Student (dùng `RoleGuard` của U01). Backend vẫn kiểm quyền thật.
