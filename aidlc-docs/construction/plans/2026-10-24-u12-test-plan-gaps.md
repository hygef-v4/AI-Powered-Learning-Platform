# U12 – sửa 4 chỗ lệch kế hoạch kiểm thử hộp đen (2026-10-24)

Nguồn: kế hoạch kiểm thử hộp đen của nhóm (artifact), ca U12-03, U12-05, U12-08, U12-13.
Nhánh: `fix/u12-test-plan-gaps` từ `origin/staging` (301c573). Người dùng duyệt: "sửa cả 4 chỗ U12 đi".
Chỉ đổi frontend; không đổi API, quy tắc nghiệp vụ hay dữ liệu.

## Các bước

- [x] 1. U12-03 `features/groups/GroupEditor.tsx`: kéo thả thành viên giữa các nhóm và khu Chưa có nhóm (giữ ô ⇄ Chuyển
      cho bàn phím); nhóm mới ghi "(mới)". Test kéo thả và nhãn.
- [x] 2. U12-05 `features/groups/TeacherGroupWidgets.tsx`: nút "Mở tab Sinh viên" trong hộp Kiểm tra sẵn sàng, mở
      `/teaching/classes/{id}?tab=students`; `features/academics/ReadOnlyClassPage.tsx` nhận `?tab=` khi mở trang. Test.
- [x] 3. U12-08 `features/groupdocs/GroupWorkspacePage.tsx`: bấm vào mục Mở/Xong để nhận và mở luôn trình soạn; bấm mục
      mình giữ thì mở trình soạn; bấm mục người khác giữ thì chỉ cuộn tới nội dung (U12-09). Nút Nhận mục cũng mở trình soạn
      sau khi nhận. Test.
- [x] 4. U12-13 cùng file: popup tự nộp ghi "Đã hết hạn nộp lúc …". Test.
- [x] 5. Chạy test frontend liên quan, typecheck, lint, build; cập nhật code-summary U12, aidlc-state, audit.
