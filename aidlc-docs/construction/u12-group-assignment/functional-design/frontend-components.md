# U12 Group Assignment — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Gộp U12 cũ (nhóm và trưởng nhóm) và U14 cũ (tài liệu nhóm và nộp). Soạn đề, rubric dùng U07; chấm dùng U09. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 32 | Teacher Class Detail tab Sinh viên: chưa chia nhóm (`t02c`), đã chia nhóm (`t02`), sửa nhóm (`t02f`), tiến độ bài nhóm (`t02e`), popup Chia ngẫu nhiên (`t02d`), Nhả mục (`t02h`); số nhóm trên tab Thông tin lớp (`t18`) | `ClassGroupsPanel` và các popup | 57, 42 | Teacher, Subject Manager được giao dạy |
| 33 | Student Detail, phần nhóm (`t19`) | `StudentGroupInfo` | 57 | Như trên |
| 36 | Student Class Detail, phần Nhóm của tôi (`s02`); trang Nhóm của tôi (`s03`); popup Yêu cầu đổi trưởng nhóm (`s04`) | `MyGroupCard`, `MyGroupPage`, `LeaderChangeRequestDialog` | 54, 55 | Student |
| 26 | Assignment Form bài nhóm, hộp Kiểm tra sẵn sàng (`t15`) | `GroupAssignmentPanel` | 53 | Teacher, Subject Manager được giao dạy |
| 39 | Assignment Detail bài nhóm (`s12d`) | `OpenGroupWorkButton` | 56 | Student |
| 40 | Assignment Workspace bài nhóm: thành viên (`s18`), trưởng nhóm giao mục (`s18f`), trình soạn mục (`s18b`), popup Nộp tài liệu nhóm (`s18c`), thẻ Bản nộp (`s18d`), tự nộp khi hết hạn (`s18e`) | `GroupWorkspacePage` và các popup | 56 | Student; giảng viên chỉ xem |

## 2. Cây component

```
(TeacherClassTab "students" của U05)
  ClassGroupsPanel                           lớp chưa có nhóm: nút Chia ngẫu nhiên, Tạo nhóm, cột Nhóm của bảng sinh viên;
                                             lớp có nhóm: thẻ từng nhóm, nút Tiến độ bài nhóm, Sửa nhóm
    GroupEditor                              t02f: tạo, thêm, chuyển, gỡ, chọn trưởng nhóm; lưu cả bộ
    RandomSplitDialog                        t02d: sĩ số tối đa, bản xem trước
    LeaderRequestList                        duyệt, từ chối kèm lý do
    GroupAssignmentsCard                     bài nhóm đang mở, lối vào tiến độ
    GroupProgressTable                       t02e: tiến độ theo bài nhóm, nhả mục
    ReleaseSectionDialog                     t02h
(TeacherClassTab "info" của U05)  GroupCountCard          số nhóm, số sinh viên chưa có nhóm
(StudentDetailSection của U05)    StudentGroupInfo        nhóm của sinh viên
(StudentClassSection của U05)     MyGroupCard             thẻ Nhóm của tôi trên Student Class Detail
app/classes/[classId]/group/      MyGroupPage             s03 Nhóm của tôi
  LeaderChangeRequestDialog                  s04
(AssignmentFormSection của U07)   GroupAssignmentPanel    kiểm tra sẵn sàng
(GroupWorkEntry của U08)          OpenGroupWorkButton     nhóm, mục của bạn, tiến độ, nút Mở tài liệu nhóm
app/group-docs/[id]/              GroupWorkspacePage      #40 chế độ nhóm
  GroupWorkspaceTopBar                       tên bài · nhóm, vai trò của mình, trạng thái, hạn, Nộp tài liệu
  SectionList                                trạng thái, người giữ; Nhận, Giao, Nhả mục
  GroupDocumentView                          DocumentViewer (U07), nội dung đã Xong theo mục
  SectionEditorDialog                        s18b: trình soạn che kín trang, DocumentEditor (U07)
  AssignSectionDialog                        s18f
  SubmitGroupDialog                          s18c
  GroupSubmissionTab                         s18d: bản nộp, biên nhận, tác giả từng mục, DOCX
  AutoSubmitOverlay                          s18e
  useGroupDocStream                          SSE, tải lại khi kết nối lại
  (useActivityTracker của U08 khi bật theo dõi)
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `ClassGroupsPanel` | `GET /api/v1/classes/{classId}/groups` | Lớp chưa có nhóm: "n sinh viên · lớp chưa có nhóm", nút Chia ngẫu nhiên, Tạo nhóm, ghi chú hướng dẫn; bảng sinh viên của U05 có cột Nhóm ("Chưa có nhóm"). Lớp có nhóm: "k nhóm · n sinh viên · n chưa có nhóm", nút Tiến độ bài nhóm, Sửa nhóm; thẻ từng nhóm: tên, số thành viên, trưởng nhóm (biểu tượng vương miện), thành viên kèm email; phân trang nhóm |
| `RandomSplitDialog` | `POST .../groups:random-preview`, `PUT .../groups` | "Chia ngẫu nhiên": lớp · số sinh viên chưa có nhóm; Số thành viên tối đa mỗi nhóm (2–20); dòng tính trước số nhóm và cỡ nhóm; Xem trước; "Trưởng nhóm được chọn ngẫu nhiên; bạn đổi được trước khi lưu."; bảng Nhóm, Thành viên, Trưởng nhóm; Hủy, Lưu k nhóm |
| `GroupEditor` | `PUT .../groups` | Thanh "Đang sửa nhóm. Thay đổi chỉ áp dụng khi bạn bấm Lưu cho cả lớp.", Hủy, Lưu cho cả lớp; mỗi nhóm một cột (tên, số người, thành viên có nút chọn Trưởng nhóm); nhóm mới ghi "(mới)", "Chưa có trưởng nhóm"; cột "Chưa có nhóm (n)"; kéo thả, hoặc chọn nhóm qua nút ⇄; Thêm nhóm; lỗi hiện theo nhóm |
| `LeaderRequestList` | `GET .../leader-requests`, `:approve`, `:reject` | Thẻ "Yêu cầu đổi trưởng nhóm" (số chờ duyệt): nhóm · người gửi, Chờ duyệt, đề xuất trưởng nhóm mới, lý do; ô "Lý do từ chối (bắt buộc khi từ chối)"; Từ chối, Duyệt |
| `GroupAssignmentsCard` | `GET /api/v1/classes/{classId}/assignments` (U07, lọc bài nhóm đang mở) | "Bài nhóm đang mở: tên bài" kèm link Xem tiến độ từng nhóm |
| `GroupProgressTable` | `GET .../group-assignments/{assignmentId}/progress` | Quay lại danh sách nhóm; chọn bài nhóm (tên · hạn); "Mục đã Xong / tổng số mục"; cột Nhóm, Trưởng nhóm, Mục xong, Tiến độ, Trạng thái (Chưa bắt đầu, Đang làm, Đã nộp), Mục đang giữ (mục · người · số ngày giữ) kèm nút Nhả mục |
| `ReleaseSectionDialog` | `POST .../sections/{sectionId}:release` | "Nhả mục X?": nhóm · bài; người giữ và ngày bắt đầu giữ; cảnh báo "Bản nháp chưa Xong của người đang giữ sẽ bị bỏ"; sau khi nhả, mục về Xong nếu đã có nội dung đã Xong, không thì về Mở (BR-U12-36); người giữ nhận thông báo; Hủy, Nhả mục |
| `GroupCountCard` | `GET .../groups` | Thẻ số Nhóm và số sinh viên chưa có nhóm trên tab Thông tin lớp |
| `StudentGroupInfo` | `GET .../groups` | Tên nhóm của sinh viên trong popup Student Detail |
| `MyGroupCard` | `GET /api/v1/me/classes/{classId}/group` | "Nhóm của tôi" trên Student Class Detail: tên nhóm, số thành viên, trưởng nhóm, avatar thành viên, link "Mở Nhóm của tôi" |
| `MyGroupPage` | `GET /api/v1/me/classes/{classId}/group` | Nút quay lại; "Nhóm n"; đường dẫn "Lớp của tôi › lớp › Nhóm của tôi"; nút Yêu cầu đổi trưởng nhóm. "Thành viên (n)", "Nhóm cố định của lớp": avatar, tên, nhãn Trưởng nhóm, Bạn. "Yêu cầu đổi trưởng nhóm gần nhất": trạng thái (Chờ duyệt, Đã duyệt, Từ chối kèm lý do, Đã hủy), thời điểm gửi, đề xuất, lý do, nút Hủy yêu cầu khi đang chờ; dòng tóm tắt yêu cầu trước. "Bài nhóm": Bài nhóm (tên, Graded), Hạn nộp, Mục của bạn, Trạng thái (Đang làm · x / y mục xong, Đã nộp), Mở tài liệu hoặc Xem. Chỉ nhóm của mình |
| `LeaderChangeRequestDialog` | `POST /api/v1/groups/{groupId}/leader-requests`, `POST /api/v1/leader-requests/{id}:cancel` | Nhóm · lớp; "Giảng viên X sẽ duyệt hoặc từ chối. Mỗi nhóm chỉ có một yêu cầu chờ duyệt."; Trưởng nhóm hiện tại; Lý do* (10–1.000 ký tự, đếm ký tự); Đề xuất trưởng nhóm mới (tùy chọn); Hủy, Gửi yêu cầu |
| `GroupAssignmentPanel` | `GET .../groups` | Hộp "Kiểm tra sẵn sàng" trên Assignment Form bài nhóm: lỗi chặn phát hành (nhóm chưa có trưởng nhóm, nhóm không có thành viên), cảnh báo sinh viên chưa có nhóm; nút Mở tab Sinh viên |
| `OpenGroupWorkButton` | `GET /api/v1/assignments/{assignmentId}/my-group-doc` | Trên Assignment Detail bài nhóm: Nhóm (tên · trưởng nhóm), Mục của bạn, Tiến độ (x / y mục Xong), nút Mở tài liệu nhóm |
| `GroupWorkspacePage`, `GroupWorkspaceTopBar`, `SectionList`, `GroupDocumentView` | `GET /api/v1/group-docs/{id}`, `:claim`, `:assign`, `:release` | Thanh trên: Thoát, "tên bài · Nhóm n", "lớp · bạn là thành viên (hoặc trưởng nhóm) · trưởng nhóm X", nhãn Đang làm hoặc Đã nộp, "Còn n ngày · hạn …"; trưởng nhóm có Nộp tài liệu (Nộp lại sau khi đã nộp), thành viên thấy "Trưởng nhóm nộp bài"; hướng dẫn "Bạn chỉ sửa được mục mình đang giữ. Bấm vào mục để mở trình soạn toàn trang; nội dung tự lưu mỗi 10 giây." Cột "Các mục" (x / y Xong): tên mục, trạng thái (Mở, Đang làm, Xong), người giữ ("Bạn", tên, "Chưa ai nhận"); nút Nhận mục ở mục Mở hoặc Xong (BR-U12-33), Nhả mục ở mục mình giữ, Giao mục cho trưởng nhóm; "Trưởng nhóm nộp bài. Mục Xong mới được tính khi nộp." Phần tài liệu: từng mục kèm tác giả và nội dung đã Xong; mục mình giữ có "Bạn đang giữ" và nút Mở trình soạn; mục người khác giữ ghi "Chỉ người giữ mục xem được bản nháp." Cập nhật từ SSE |
| `AssignSectionDialog` | `:assign` | "Giao mục X": chọn một thành viên; mục đang do người khác giữ thì cảnh báo nhả trước; Hủy, Giao mục; "Thành viên cũng có thể tự nhận mục." |
| `SectionEditorDialog` | `PUT .../draft`, `:done` | Trình soạn che kín trang: tên mục, "Bạn đang giữ", "Tự lưu lúc … · mỗi 10 giây", Đóng, Xong; Hướng dẫn của giảng viên; Nội dung (`DocumentEditor`); khối sơ đồ ghi số sơ đồ bắt buộc, nút Mở trình vẽ Draw.io; `409` báo tải lại; Xong thì đóng popup |
| `SubmitGroupDialog` | `POST /api/v1/group-docs/{id}/submit` | Chỉ trưởng nhóm: "Nộp tài liệu nhóm?"; còn mục chưa Xong thì cảnh báo "Chỉ nội dung của mục Xong được đưa vào bản nộp", bảng Mục, Người giữ, Trạng thái; "Bạn có thể nộp lại trước hạn; mỗi lần nộp tạo một biên nhận mới."; Quay lại, Vẫn nộp |
| `GroupSubmissionTab` | `GET .../export.docx` | Thẻ "Bản nộp": Đã nộp, "Trưởng nhóm X nộp lúc …", Mã biên nhận; bảng Mục, Tác giả, Trạng thái lúc nộp; Tải DOCX; "Nộp lại được trước hạn …". Không hiện số lần nộp (chỉ lưu bản nộp cuối; mọi lần nộp nằm trong audit) |
| `AutoSubmitOverlay`, `useGroupDocStream` | `GET .../events` | Khi tự nộp lúc hết hạn: khóa trang "Đang nộp… Đã hết hạn nộp lúc …. Hệ thống đang tự nộp bản mới nhất của các mục; trang bị khóa trong lúc nộp." rồi chuyển sang thẻ Bản nộp |

Route `/group-docs/*` cho Student và giảng viên của lớp; `/classes/*` cho Student (backend kiểm quyền thật).
