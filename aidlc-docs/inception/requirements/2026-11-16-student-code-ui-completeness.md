# Rà soát MSSV tại các danh sách và bộ chọn sinh viên

## Yêu cầu

Người dùng: “giúp tôi check lại giao diện chỗ nào có danh sách sinh viên thì nênđi kèm với mssv”.

Đối chiếu yêu cầu hiện hữu [SC-10, SC-11 và NFR-SC-01…07](2026-11-13-student-code.md).
Kết quả dưới đây được xác minh từ component và DTO hiện tại; chưa nghiệm thu qua trình duyệt có đăng nhập.

## Đã có MSSV

- Danh sách tài khoản Admin; danh sách ghi danh và chọn sinh viên để ghi danh.
- Bảng điểm ma trận, danh sách bài nộp để chấm và tiêu đề bài nộp cá nhân.
- Danh sách sinh viên có nhóm/chưa có nhóm; danh sách chọn thành viên khi sửa nhóm.
- Thẻ thành viên “Nhóm của tôi”, thành viên đề xuất và hộp mời bạn cùng lớp.
- Bảng lượt làm quiz của giảng viên.

Các nơi này đang dùng cột MSSV hoặc component có sẵn `PersonSubline`.

## Các chỗ còn thiếu đã xác minh

| Khu vực | Vị trí trong mã nguồn | Hiện tại | Bổ sung tối thiểu đề xuất |
|---|---|---|---|
| Chấm điểm, chế độ Theo sinh viên | `frontend/src/features/grading/components/ClassGradebookTab.tsx` | Danh sách bên trái và tiêu đề sinh viên chỉ có tên/email, dù API có `studentCode` | Hiện MSSV trong dòng phụ cùng email |
| Chấm thành viên bài nhóm | `frontend/src/features/grading/pages/GradingWorkspacePage.tsx`, `components/MemberContributionPanel.tsx` | `memberNames` chỉ giữ tên; bộ chọn, nhãn người thực hiện và nút đánh giá không phân biệt người trùng tên | Lấy mã từ `memberActivities[].student`, hiện tên kèm MSSV tại các nhãn chọn thành viên |
| Theo dõi hoạt động thành viên khi chấm | `frontend/src/features/grading/components/GradingReviewPanels.tsx` | Mỗi dòng chỉ có tên sinh viên và trạng thái hoạt động | Hiện MSSV của cùng sinh viên, giữ nguyên dữ liệu và ý nghĩa hoạt động |
| Duyệt đề xuất nhóm | `frontend/src/features/groups/components/ProposalReviewList.tsx` | Trưởng nhóm và chuỗi danh sách thành viên chỉ có tên/email; `Person` đã có mã | Hiện tên kèm MSSV ở từng người |
| Đổi/đề xuất trưởng nhóm | `frontend/src/features/groups/components/GroupFormDialog.tsx`, `LeaderRequestList.tsx`, `MyGroup.tsx` | Các lựa chọn trưởng nhóm và ô trưởng nhóm hiện tại chỉ có tên/email | Hiện dạng `Tên · MSSV` trong select/ô một dòng, không đổi `accountId` của lựa chọn |
| Lời mời vào đề xuất nhóm | `frontend/src/features/groups/components/GroupProposalSection.tsx`, `backend/src/main/java/edu/aiplatform/groups/service/GroupProposalService.java` | Lời mời chỉ trả và hiện `leaderName` | Thêm `leaderStudentCode` có thể rỗng từ người đã được phép xem; hiện cạnh trưởng nhóm |
| Tài liệu nhóm | `frontend/src/features/groupdocs/model/types.ts`, `model/status.ts`, `components/SectionDialogs.tsx`, `SectionHistoryDialog.tsx`, `GroupProgressCard.tsx`, `pages/GroupWorkspacePage.tsx`; `backend/src/main/java/edu/aiplatform/groupdocs/service/GroupWorkspaceService.java` | `WorkspaceView.members`/`Member` chỉ có id, tên và cờ trưởng nhóm; bộ chọn giao mục, người giữ/người viết, danh sách tiến độ và tác giả lịch sử không có mã | Thêm `studentCode` có thể rỗng vào member DTO và dùng ở các nơi vốn đã hiện thành viên |
| Hộp chi tiết lượt làm quiz | `frontend/src/features/quizpractice/components/TeacherQuizSections.tsx` | Bảng ngoài có cột MSSV; hộp danh sách lượt và chi tiết chỉ nhắc tên | Giữ MSSV trong ngữ cảnh sinh viên đã chọn; nếu cần thêm vào detail DTO thì chỉ thêm trường có thể rỗng |

Các cột thống kê giảng viên ở trang quản trị AI không phải danh sách sinh viên, nên không áp dụng.

## Quy tắc hiển thị đề xuất

1. Giữ cách trình bày hiện có: cột MSSV riêng trong bảng đã có cột; dòng `MSSV · email` bằng `PersonSubline` ở danh sách nhiều dòng; `Tên · MSSV` ở select/nhãn một dòng.
2. Sinh viên chưa có mã: giữ tên/email hiện tại, không tạo mã giả hoặc thay đổi dữ liệu tài khoản.
3. Không đổi thứ tự danh sách, khóa lựa chọn, phân trang, logic nhóm, điểm hoặc quyền xem.
4. Chỉ trả mã của những người API/màn hình vốn đã được phép hiển thị; không tạo API tra cứu người ngoài lớp/nhóm.
5. Không đưa MSSV vào prompt AI, thông báo, email, log hoặc nội dung audit. Không thêm dependency hay migration.
6. Tái sử dụng `PersonSubline` và `matchesPersonQuery`. Chỗ có ô tìm thành viên phải tìm được theo mã khi DTO đã cung cấp mã.

## Phạm vi kiểm chứng khi được duyệt sửa

- Test frontend ở các danh sách/bộ chọn bị thay đổi: người trùng tên nhưng khác MSSV, sinh viên không có mã, người đã chọn vẫn mang mã, và chọn đúng `accountId`.
- Test DTO/backend cho member tài liệu nhóm và trưởng nhóm trên lời mời; không mở rộng quyền đọc.
- Chạy các test liên quan, ESLint các file đổi và TypeScript. Không đổi cấu hình chỉ để tránh cache Next cũ; dùng kiểm tra riêng nếu cần.
- Kiểm tra trực quan màn rộng/hẹp trên các trang có tài khoản phù hợp. Hiện chưa có phiên đăng nhập trong browser Orca; không tạo hoặc sửa dữ liệu học tập chỉ để chụp giao diện.

## Trạng thái AI-DLC

- [x] Nhận diện workspace brownfield, giữ nhánh `audit-staging` hiện tại và ba báo cáo audit chưa theo dõi.
- [x] Đối chiếu yêu cầu MSSV đã có và truy vết component/DTO.
- [x] Ghi nhận các vị trí đã đủ, còn thiếu và giới hạn kiểm tra.
- [x] Người dùng duyệt yêu cầu/phạm vi sửa tại [câu hỏi duyệt](2026-11-16-student-code-ui-completeness-questions.md).
- [x] Sau khi được duyệt: hoàn thiện kế hoạch Code Generation, sửa và kiểm thử theo AI-DLC.

Người dùng đã duyệt rõ phạm vi và yêu cầu tiếp tục sửa/kiểm thử. Đã hoàn tất tám nhóm vị trí; xem [kế hoạch và kết quả](../../construction/plans/code-generation-plan/2026-11-16-student-code-ui-completeness.md): 338 test frontend và 141 test backend đạt; ESLint/TypeScript đạt, review độc lập không có finding. Đã kiểm tra browser đăng nhập trên màn rộng/hẹp trong các ngữ cảnh có dữ liệu, xem [báo cáo UI](../../construction/build-and-test/2026-11-16-student-code-ui-browser-results.md) để biết các ca còn thiếu dữ liệu/trạng thái phù hợp. Theo yêu cầu tiếp theo đã backfill HE190001–HE190006 trong DB seed local và tách cột MSSV/Email ở bảng Sinh viên. Người dùng đã yêu cầu commit local, không push; thao tác DB và ảnh/script tạm không thuộc nội dung Git. Security/Resiliency compliant trong phạm vi; PBT disabled.
