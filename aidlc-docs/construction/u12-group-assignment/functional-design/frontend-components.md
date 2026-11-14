# U12 Group Assignment — frontend-components

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006, US-GRP-007, US-GRP-008, US-GRP-009, US-GRP-010. **Requirements:** FR-025, FR-026, FR-018, FR-039; SG-01…09 (2026-11-11).

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
  useGroupDocStream                          SSE, tải lại khi kết nối lại; đóng hẳn (401) thì làm mới phiên và nối lại có backoff
  (useActivityTracker của U08 khi bật theo dõi)
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `ClassGroupsPanel` | `GET /api/v1/classes/{classId}/groups` | Lớp chưa có nhóm: "n sinh viên · lớp chưa có nhóm", nút Chia ngẫu nhiên, Tạo nhóm, ghi chú hướng dẫn; bảng sinh viên của U05 có cột Nhóm ("Chưa có nhóm"). Lớp có nhóm: "k nhóm · n sinh viên · n chưa có nhóm", nút Tiến độ bài nhóm, Sửa nhóm; thẻ từng nhóm: tên, số thành viên, trưởng nhóm (biểu tượng vương miện), thành viên kèm email; phân trang nhóm |
| `RandomSplitDialog` | `POST .../groups:random-preview`, `PUT .../groups` | "Chia ngẫu nhiên": lớp · số sinh viên chưa có nhóm; Số thành viên tối đa mỗi nhóm (2–20); dòng tính trước số nhóm và cỡ nhóm; Xem trước; "Trưởng nhóm được chọn ngẫu nhiên; bạn đổi được trước khi lưu."; bảng Nhóm, Thành viên, Trưởng nhóm; Hủy, Lưu k nhóm |
| `GroupEditor` | `PUT .../groups` | Thanh "Đang sửa nhóm. Thay đổi chỉ áp dụng khi bạn bấm Lưu cho cả lớp.", Hủy, Lưu cho cả lớp; mỗi nhóm một cột (tên, số người, thành viên có nút chọn Trưởng nhóm); nhóm mới ghi "(mới)", "Chưa có trưởng nhóm"; cột "Chưa có nhóm (n)"; kéo thả, hoặc chọn nhóm qua nút ⇄; Thêm nhóm; lỗi hiện theo nhóm |
| `LeaderRequestList` | `GET .../leader-requests`, `:approve`, `:reject` | Thẻ "Yêu cầu đổi trưởng nhóm" (số chờ duyệt): nhóm · người gửi, Chờ duyệt, đề xuất trưởng nhóm mới, lý do; ô "Lý do từ chối (tùy chọn, tối đa 1.000 ký tự)"; Từ chối, Duyệt |
| `GroupAssignmentsCard` | `GET /api/v1/classes/{classId}/assignments` (U07, lọc bài nhóm đang mở) | "Bài nhóm đang mở: tên bài" kèm link Xem tiến độ từng nhóm |
| `GroupProgressTable` | `GET .../group-assignments/{assignmentId}/progress` | Quay lại danh sách nhóm; chọn bài nhóm (tên · hạn); "Mục đã Xong / tổng số mục"; cột Nhóm, Trưởng nhóm, Mục xong, Tiến độ, Trạng thái (Chưa bắt đầu, Đang làm, Đã nộp), Mục đang giữ (mục · người · số ngày giữ) kèm nút Nhả mục |
| `ReleaseSectionDialog` | `POST .../sections/{sectionId}:release` | "Nhả mục X?": nhóm · bài; người giữ và ngày bắt đầu giữ; cảnh báo "Bản nháp chưa Xong của người đang giữ sẽ bị bỏ"; sau khi nhả, mục về Xong nếu đã có nội dung đã Xong, không thì về Mở (BR-U12-36); người giữ nhận thông báo; Hủy, Nhả mục |
| `GroupCountCard` | `GET .../groups` | Thẻ số Nhóm và số sinh viên chưa có nhóm trên tab Thông tin lớp |
| `StudentGroupInfo` | `GET .../groups` | Tên nhóm của sinh viên trong popup Student Detail |
| `MyGroupCard` | `GET /api/v1/me/classes/{classId}/group` | "Nhóm của tôi" trên Student Class Detail: tên nhóm, số thành viên, trưởng nhóm, avatar thành viên, link "Mở Nhóm của tôi" |
| `MyGroupPage` | `GET /api/v1/me/classes/{classId}/group` | Nút quay lại; "Nhóm n"; đường dẫn "Lớp của tôi › lớp › Nhóm của tôi"; nút Yêu cầu đổi trưởng nhóm. "Thành viên (n)", "Nhóm cố định của lớp": avatar, tên, nhãn Trưởng nhóm, Bạn. "Yêu cầu đổi trưởng nhóm gần nhất": trạng thái (Chờ duyệt, Đã duyệt, Từ chối kèm lý do, Đã hủy), thời điểm gửi, đề xuất, lý do, nút Hủy yêu cầu khi đang chờ; dòng tóm tắt yêu cầu trước. "Bài nhóm": Bài nhóm (tên, Graded), Hạn nộp, Mục của bạn, Trạng thái (Đang làm · x / y mục xong, Đã nộp), Mở tài liệu hoặc Xem. Chỉ nhóm của mình |
| `LeaderChangeRequestDialog` | `POST /api/v1/groups/{groupId}/leader-requests`, `POST /api/v1/leader-requests/{id}:cancel` | Nhóm · lớp; "Giảng viên X sẽ duyệt hoặc từ chối. Mỗi nhóm chỉ có một yêu cầu chờ duyệt."; Trưởng nhóm hiện tại; Lý do (tùy chọn) (0–1.000 ký tự, đếm ký tự); Đề xuất trưởng nhóm mới (tùy chọn); Hủy, Gửi yêu cầu |
| `GroupAssignmentPanel` | `GET .../groups` | Hộp "Kiểm tra sẵn sàng" trên Assignment Form bài nhóm: lỗi chặn phát hành (nhóm chưa có trưởng nhóm, nhóm không có thành viên), cảnh báo sinh viên chưa có nhóm; nút Mở tab Sinh viên |
| `OpenGroupWorkButton` | `GET /api/v1/assignments/{assignmentId}/my-group-doc` | Trên Assignment Detail bài nhóm: Nhóm (tên · trưởng nhóm), Mục của bạn, Tiến độ (x / y mục Xong), nút Mở tài liệu nhóm |
| `GroupWorkspacePage`, `GroupWorkspaceTopBar`, `SectionList`, `GroupDocumentView` | `GET /api/v1/group-docs/{id}`, `:claim`, `:assign`, `:release` | Thanh trên: Thoát, "tên bài · Nhóm n", "lớp · bạn là thành viên (hoặc trưởng nhóm) · trưởng nhóm X", nhãn Đang làm hoặc Đã nộp, "Còn n ngày · hạn …"; trưởng nhóm có Nộp tài liệu (Nộp lại sau khi đã nộp), thành viên thấy "Trưởng nhóm nộp bài"; hướng dẫn "Bạn chỉ sửa được mục mình đang giữ. Bấm vào mục để mở trình soạn toàn trang; nội dung tự lưu mỗi 10 giây." Cột "Các mục" (x / y Xong): tên mục, trạng thái (Mở, Đang làm, Xong), người giữ ("Bạn", tên, "Chưa ai nhận"); nút Nhận mục ở mục Mở hoặc Xong (BR-U12-33), Nhả mục ở mục mình giữ, Giao mục cho trưởng nhóm; "Trưởng nhóm nộp bài. Mục Xong mới được tính khi nộp." Phần tài liệu: từng mục kèm tác giả và nội dung đã Xong; mục mình giữ có "Bạn đang giữ" và nút Mở trình soạn; mục người khác giữ ghi "Chỉ người giữ mục xem được bản nháp." Cập nhật từ SSE |
| `AssignSectionDialog` | `:assign` | "Giao mục X": chọn một thành viên; mục đang do người khác giữ thì cảnh báo nhả trước; Hủy, Giao mục; "Thành viên cũng có thể tự nhận mục." |
| `SectionEditorDialog` | `PUT .../draft`, `:done` | Trình soạn che kín trang: tên mục, "Bạn đang giữ", "Tự lưu lúc … · mỗi 10 giây", Đóng, Xong; Hướng dẫn của giảng viên; Nội dung (`DocumentEditor`); khối sơ đồ ghi số sơ đồ bắt buộc, nút Mở trình vẽ Draw.io; `409` báo tải lại; Xong thì đóng popup |
| `SubmitGroupDialog` | `POST /api/v1/group-docs/{id}/submit` | Chỉ trưởng nhóm: "Nộp tài liệu nhóm?"; còn mục chưa Xong thì cảnh báo "Chỉ nội dung của mục Xong được đưa vào bản nộp", bảng Mục, Người giữ, Trạng thái; "Bạn có thể nộp lại trước hạn; mỗi lần nộp tạo một biên nhận mới."; Quay lại, Vẫn nộp |
| `GroupSubmissionTab` | `GET .../export.docx` | Thẻ "Bản nộp": Đã nộp, "Trưởng nhóm X nộp lúc …", Mã biên nhận; bảng Mục, Tác giả, Trạng thái lúc nộp; Tải DOCX; "Nộp lại được trước hạn …". Không hiện số lần nộp (chỉ lưu bản nộp cuối; mọi lần nộp nằm trong audit) |
| `AutoSubmitOverlay`, `useGroupDocStream` | `GET .../events` | Khi tự nộp lúc hết hạn: khóa trang "Đang nộp… Đã hết hạn nộp lúc …. Hệ thống đang tự nộp bản mới nhất của các mục; trang bị khóa trong lúc nộp." rồi chuyển sang thẻ Bản nộp |

Route `/group-docs/*` cho Student và giảng viên của lớp; `/classes/*` cho Student (backend kiểm quyền thật).

## Source behavior delta — 2026-10-30

groups và groupdocs thuộc cùng U12. Leader request reason tùy chọn, null thành chuỗi rỗng, tối đa 1.000; reject reason tùy chọn, trống thành null. GroupRenameService cho đổi tên theo scope; editor hiển thị lỗi nhóm đã bị gỡ khỏi draft. Claims/locks/leader submit giữ theo SectionRules và services; không tự thêm peer assessment hay đổi AI Student GROUP.

[Source/API/schema/test inventory](../code/as-built-baseline.md). Các rule và test cũ trái delta này là thiết kế trước baseline; không tiếp tục áp điều kiện cũ vào AS-BUILT.

## Sinh viên tự chia nhóm — 2026-11-11

US-GRP-007…010; quy tắc BR-U12-70…96 trong [business-rules](business-rules.md). Không thêm trang; thêm vào trang Nhóm của tôi và `ClassGroupsPanel` (quyết định Q9). Chữ hiển thị tiếng Việt, theo quy ước UI hiện có (WCAG 2.2 AA, `PRODUCT.md`).

### Cây component

```
(StudentClassSection của U05)  MyGroupCard           chưa có nhóm: "Đợt tự chia nhóm đang mở đến …" hoặc "Chờ giảng viên xếp nhóm";
                                                      số lời mời đang chờ; link "Mở Nhóm của tôi"
app/classes/[classId]/group/   MyGroupPage           có nhóm chính thức: như cũ; chưa có: GroupProposalSection
  GroupProposalSection                               nạp GET /me/classes/{classId}/group-proposal
    FormationStatusBanner                            đợt mở: sĩ số min–max, hạn chót; đóng: lý do
    ProposalRejectedNotice                           đề xuất bị từ chối gần nhất, lý do
    InvitationList                                   lời mời đến tôi: Chấp nhận, Từ chối
    ProposalCard                                     đề xuất của tôi: tên, trạng thái, thành viên, lời mời chờ
      CreateProposalDialog                           tên nhóm
      RenameProposalDialog                           tên nhóm
      InviteMembersDialog                            chọn bạn mời được, tìm theo tên/email
      ConfirmDialog (dùng chung)                     hủy đề xuất, rời đề xuất, gửi duyệt, rút về
(TeacherClassTab "students")   ClassGroupsPanel      thêm nút "Tự chia nhóm" và nhãn trạng thái đợt
    SelfGroupingSettingsDialog                       bật/tắt, sĩ số, hạn chót
    ProposalReviewList                               cạnh LeaderRequestList: đề xuất đang hoạt động, duyệt, từ chối
      RejectProposalDialog                           lý do tùy chọn
    UngroupedStudentsList                            sinh viên chưa có nhóm kèm đề xuất họ đang ở
```

### Component và API

| Component | API | Hành vi |
|---|---|---|
| `MyGroupCard` | `GET /api/v1/me/classes/{classId}/group`, `GET .../group-proposal` | Chưa có nhóm: trạng thái đợt, số lời mời, tên và trạng thái đề xuất của tôi; link tới trang Nhóm của tôi |
| `GroupProposalSection` | `GET /api/v1/me/classes/{classId}/group-proposal` | Hiện các khối theo trạng thái (bảng dưới); tải lại sau mỗi thao tác; `409` báo "Đề xuất vừa thay đổi" và tải lại |
| `FormationStatusBanner` | — | Mở: "Bạn tự lập nhóm từ min đến max người, hạn …"; đóng: "Đợt tự chia nhóm đã đóng. Giảng viên sẽ xếp nhóm cho bạn." |
| `InvitationList` | `POST /api/v1/group-proposals/{id}:accept`, `:decline` | Mỗi lời mời: tên đề xuất, trưởng nhóm, số thành viên/max; Chấp nhận (ẩn khi đợt đóng, khi tôi đang ở đề xuất khác thì ghi "Rời đề xuất hiện tại để chấp nhận"), Từ chối |
| `ProposalCard` | `PUT .../name`, `POST .../invitations`, `.../invitations/{accountId}:revoke`, `:leave`, `:submit`, `:withdraw`, `:cancel` | Tên, nhãn Nháp/Chờ duyệt, thành viên (Trưởng nhóm, Bạn), lời mời chờ (hết hạn khi đợt đóng). Trưởng nhóm, Nháp: Đổi tên, Mời bạn, Thu hồi, Gửi duyệt (tắt kèm lý do khi dưới min), Hủy đề xuất. Trưởng nhóm, Chờ duyệt: Rút về để sửa. Thành viên, Nháp: Rời đề xuất |
| `CreateProposalDialog`, `RenameProposalDialog` | `POST /api/v1/classes/{classId}/group-proposals`, `PUT .../name` | Ô tên bắt buộc, 1–100 ký tự sau khi cắt khoảng trắng, đếm ký tự; lỗi trùng tên hiện dưới ô |
| `InviteMembersDialog` | dữ liệu "bạn mời được" từ `GET .../group-proposal` | Danh sách chọn nhiều có ô tìm; ghi "Đang ở đề xuất khác" cạnh người đó; số chỗ còn lại = max − thành viên − lời mời chờ, chặn chọn quá |
| `SelfGroupingSettingsDialog` | `GET`, `PUT /api/v1/classes/{classId}/group-formation` | Công tắc "Cho sinh viên tự chia nhóm"; Sĩ số tối thiểu, tối đa (2–20, min ≤ max); Hạn chót (tùy chọn, phải ở tương lai, giờ Việt Nam); lỗi dưới đúng ô; ghi chú "Đề xuất chờ duyệt vẫn được duyệt sau khi đóng" |
| `ProposalReviewList` | `GET /api/v1/classes/{classId}/group-proposals`, `POST .../group-proposals:approve`, `POST /api/v1/group-proposals/{id}:reject` | Thẻ "Đề xuất nhóm" (số chờ duyệt): tên, trưởng nhóm, thành viên, trạng thái; cảnh báo sĩ số; ô chọn ở đề xuất Chờ duyệt, "Duyệt (n)", Duyệt, Từ chối từng cái; đề xuất Nháp chỉ xem. Sau khi duyệt: toast số đề xuất được duyệt, lỗi hiện tại từng thẻ; tải lại cả bộ nhóm |
| `RejectProposalDialog` | `:reject` | "Từ chối đề xuất X?"; Lý do (tùy chọn, tối đa 1.000 ký tự, đếm ký tự); Hủy, Từ chối |
| `UngroupedStudentsList` | `GET .../group-proposals` | Sinh viên chưa có nhóm: tên, email, đề xuất đang ở (tên, trạng thái) hoặc "Chưa có đề xuất"; dùng cùng dữ liệu cho nút Chia ngẫu nhiên hiện có |

### Trạng thái màn sinh viên

| Trạng thái | Hiện |
|---|---|
| Có nhóm chính thức | Màn Nhóm của tôi như cũ |
| Chưa có nhóm, đợt tắt hoặc lớp không `OPEN` | `FormationStatusBanner` đóng; đề xuất cũ (nếu có) chỉ xem |
| Chưa có nhóm, đợt mở, không ở đề xuất | Banner mở, `ProposalRejectedNotice` nếu có, `InvitationList`, nút Tạo đề xuất nhóm |
| Ở đề xuất Nháp hoặc Chờ duyệt | Banner, `ProposalCard`, `InvitationList` (lời mời khác) |

### Hợp đồng dữ liệu frontend

- `features/groups/model/types.ts`: thêm `FormationSettings`, `FormationState` (`open`, `closedReason`), `ProposalStatus`, `ProposalMemberStatus`, `ProposalView`, `InvitationView`, `MyProposalView`, `ClassProposalsView`, `ApproveResult`.
- `features/groups/api/index.ts`: một hàm cho mỗi API mới, dùng client và cách báo lỗi hiện có.
- Kiểm hợp lệ form ở frontend chỉ để báo sớm; backend vẫn kiểm (BR-U12-91, 93).


## Thao tác nhóm rõ nghĩa — 2026-11-13

Mỗi nhóm có nút Đổi trưởng nhóm riêng, mở popup chỉ chọn trong thành viên hiện tại và lưu bằng group-set version hiện hữu. Tên/thành viên nhóm không bị sửa qua popup này. Form tạo/sửa nhóm thêm checkbox chọn tất cả kết quả lọc, giữ lựa chọn ngoài bộ lọc và trạng thái chọn một phần; không đưa sinh viên đang ở nhóm khác vào danh sách. Lớp ARCHIVED giữ chế độ chỉ xem. Xác nhận Nhả mục ghi rõ tên mục, người giữ, bản nháp đang làm sẽ bị bỏ và nội dung đã hoàn thành được giữ; quyền nhả không đổi.

## Mã số sinh viên (MSSV) — 2026-11-13

Thay đổi giao diện để hiện và tìm theo MSSV ghi ở mục "Giao diện" của phần MSSV trong [business-rules.md](business-rules.md). Cách hiển thị chung (cột MSSV trước họ tên trong bảng, dòng phụ `SE123456 · email` trong thẻ, ô trống khi chưa có mã) theo [frontend-components U01 mục 6](../../u01-account-and-access/functional-design/frontend-components.md).
