# Kịch bản demo — Chia nhóm (U12)

Thời lượng: khoảng 12–15 phút. Demo trên stack local, dữ liệu từ `node scripts/e2e/seed.mjs`.

## Chuẩn bị

| Vai trò | Tài khoản | Ghi chú |
|---|---|---|
| Giảng viên | `gv.test@…` (Giảng viên Thử) | Giảng viên chính của lớp SE-TEST1 |
| Sinh viên 1 | `sv1.test@…` | Trưởng nhóm "Nhóm Alpha" |
| Sinh viên 2 | `sv2.test@…` | Thành viên Alpha |
| Sinh viên 3 | `sv3.test@…` | Nhận hai lời mời |
| Sinh viên 4 | `sv4.test@…` | Trưởng nhóm "Nhóm Beta" (bị từ chối) |
| Sinh viên 5 | `sv5.test@…` | Không lập nhóm, giảng viên xếp |

- Lớp SE-TEST1 ở trạng thái `OPEN`, 5 sinh viên `ACTIVE`, **chưa có nhóm nào**.
- Mở sẵn một cửa sổ thường (giảng viên) và các cửa sổ ẩn danh/profile khác nhau cho sinh viên, để chuyển nhanh mà không đăng nhập lại.
- Mật khẩu lấy từ `TEST_ACCOUNT_PASSWORD` trong `infra/.env.local`; không chiếu mật khẩu lên màn hình.

Đường đi:
- Giảng viên: **Lớp SE-TEST1 → tab Sinh viên** (khu "Sinh viên tự chia nhóm" và "Danh sách nhóm").
- Sinh viên: **Lớp SE-TEST1 → thẻ "Nhóm của tôi" → Mở Nhóm của tôi**.

---

## Phần 1 — Giảng viên mở đợt tự chia nhóm (SG-01) · ~2 phút

1. Đăng nhập giảng viên, vào **SE-TEST1 → tab Sinh viên**.
   - Lời thoại: "Lớp có 5 sinh viên, chưa có nhóm. Thay vì xếp tay, tôi cho sinh viên tự lập nhóm rồi tôi duyệt."
2. Ở khu **Sinh viên tự chia nhóm**, bấm **Cài đặt**.
3. Bật **Cho sinh viên tự chia nhóm**, nhập:
   - **Sĩ số tối thiểu**: `2`
   - **Sĩ số tối đa**: `3`
   - **Hạn chót**: một thời điểm ngày mai (tùy chọn).
4. (Tùy chọn, chứng minh kiểm tra dữ liệu) nhập tối thiểu `4`, tối đa `3` → hiện "Tối đa phải lớn hơn hoặc bằng tối thiểu." Sửa lại rồi **Lưu**.
5. Kết quả: toast "Đã lưu cài đặt tự chia nhóm", dòng trạng thái "Tự chia nhóm: Đang mở đến …, nhóm từ 2 đến 3 người."

## Phần 2 — Sinh viên lập đề xuất và mời bạn (SG-02) · ~3 phút

6. Chuyển sang **Sinh viên 1** → **Nhóm của tôi**.
   - Thấy khu "Tự chia nhóm", sĩ số và hạn chót, nút **Tạo đề xuất nhóm**.
7. Bấm **Tạo đề xuất nhóm**, tên `Nhóm Alpha` → **Tạo**. Toast "Đã tạo đề xuất nhóm"; SV1 là trưởng nhóm, trạng thái **Nháp**, dòng "cần thêm 1 người để gửi duyệt".
8. Bấm **Mời bạn**: hộp thoại liệt kê **Sinh viên chưa có nhóm**, ghi "Còn 2 chỗ". Chọn SV2 và SV3 → **Mời 2 người**. Toast "Đã gửi lời mời"; hai dòng "Đang chờ trả lời".
9. Chuyển sang **Sinh viên 4** → **Tạo đề xuất nhóm** tên `nhóm alpha`.
   - Bị chặn vì trùng tên (không phân biệt hoa thường). Đổi thành `Nhóm Beta` → **Tạo**.
   - **Mời bạn** → chọn SV3 (SV1, SV2 hiện "Đang ở đề xuất khác" và không chọn được) → **Mời**.
10. Chuyển sang **Sinh viên 3** → **Nhóm của tôi**: khu **Lời mời vào nhóm (2)** hiện lời mời từ Alpha và Beta. Có chuông thông báo lời mời mới (SG-08).
    - Lời thoại: "Một sinh viên nhận được nhiều lời mời, nhưng chỉ vào được một đề xuất."
11. SV3 **Chấp nhận** lời mời của **Nhóm Beta**. Toast "Đã vào nhóm"; lời mời của Alpha tự hủy.
12. Chuyển sang **Sinh viên 2** → **Chấp nhận** lời mời của Alpha.

## Phần 3 — Gửi duyệt (SG-03) · ~1,5 phút

13. Quay lại **Sinh viên 1**: Alpha có SV1 + SV2 (2/3); lời mời SV3 hiện "Lời mời hết hạn"/đã hủy. Bấm **Gửi duyệt**.
    - Toast "Đã gửi đề xuất cho giảng viên"; trạng thái **Chờ duyệt**, nút đổi thành **Rút về để sửa** (đề xuất bị khóa).
14. Chuyển sang **Sinh viên 4**: Beta có SV4 + SV3 → **Gửi duyệt**.

## Phần 4 — Giảng viên duyệt và từ chối (SG-04, SG-05) · ~3 phút

15. Quay lại **giảng viên**, tải lại tab Sinh viên.
    - Khu **Chờ duyệt (2)**: thẻ Alpha và Beta, mỗi thẻ có trưởng nhóm và số thành viên.
    - Khu **Chưa có nhóm (…)**: SV5 ghi "Chưa có đề xuất"; các sinh viên đang ở đề xuất hiện tên đề xuất và trạng thái.
    - Chuông báo "có đề xuất mới chờ duyệt".
16. Thẻ **Nhóm Beta** → **Từ chối**, lý do: `Nhóm nên có ít nhất một bạn đã học môn tiên quyết.` → xác nhận. Toast "Đã từ chối Nhóm Beta".
17. Tick chọn **Nhóm Alpha** → **Duyệt đã chọn (1)** (hoặc **Duyệt** trên thẻ).
    - Toast "Đã duyệt 1 đề xuất thành nhóm"; Alpha xuất hiện trong **Danh sách nhóm** như nhóm chính thức.
    - Lời thoại: "Duyệt nhiều đề xuất một lần được; đề xuất nào lỗi thì báo lý do ở từng thẻ, các đề xuất còn lại vẫn được duyệt."
18. Chuyển nhanh sang **Sinh viên 4**: thấy "Đề xuất "Nhóm Beta" bị từ chối", kèm lý do; có thể lập hoặc vào đề xuất khác.
19. Chuyển sang **Sinh viên 1**: trang Nhóm của tôi giờ hiện nhóm chính thức Alpha (thành viên, trưởng nhóm, Bài nhóm).

## Phần 5 — Giảng viên xếp người còn lại (SG-05, SG-07) · ~2 phút

20. **Giảng viên** → **Cài đặt** → tắt **Cho sinh viên tự chia nhóm** → **Lưu**. Trạng thái "Đang tắt."
    - Lời thoại: "Đóng đợt thì sinh viên không lập hay mời được nữa; đề xuất đang chờ duyệt vẫn duyệt được."
21. Còn SV3, SV4, SV5 chưa có nhóm. Chọn một trong hai cách:
    - **Chia ngẫu nhiên**: số thành viên tối đa `3` → **Xem trước** → (có thể **Chia lại**) → **Lưu 1 nhóm**. Toast "Đã chia nhóm".
    - Hoặc **Tạo nhóm**: tên `Nhóm Gamma`, chọn SV3, SV4, SV5, chọn **Trưởng nhóm** → **Lưu nhóm**.
22. Khu **Chưa có nhóm** báo "Mọi sinh viên đã có nhóm."; **Danh sách nhóm** có 2 nhóm.

## Phần 6 — Sau khi có nhóm (SG-06) · ~1,5 phút, tùy chọn

23. **Sinh viên 1** (trưởng nhóm Alpha) → **Sửa tên nhóm** → `Alpha Team` → **Lưu**. Toast "Đã đổi tên nhóm".
24. **Sinh viên 2** → **Yêu cầu đổi trưởng nhóm**, chọn SV2 làm trưởng nhóm mới, ghi lý do → **Gửi yêu cầu**.
25. **Giảng viên** → khu **Yêu cầu đổi trưởng nhóm** → **Duyệt**. Danh sách nhóm cập nhật trưởng nhóm mới.
26. (Kết nối phần sau) Tạo bài tập loại bài nhóm: hộp **Kiểm tra sẵn sàng** báo "Lớp đã sẵn sàng cho bài nhóm."

---

## Điểm nhấn khi trình bày

- Mọi quy tắc kiểm ở backend: sinh viên chỉ thao tác trong lớp mình, chỉ trưởng nhóm sửa đề xuất, chỉ giảng viên chính duyệt.
- Mỗi sinh viên chỉ ở **một** đề xuất đang hoạt động, bảo đảm ở database.
- Duyệt chạy trong một transaction, khóa `version` của bộ nhóm; nếu hai người sửa cùng lúc thì báo "Nhóm vừa được thay đổi ở nơi khác, hãy tải lại trang", không ghi một nửa.
- Thông báo trong ứng dụng cho lời mời, đề xuất mới chờ duyệt, kết quả duyệt/từ chối; không gửi email.

## Tình huống dự phòng

| Sự cố | Cách xử lý nhanh |
|---|---|
| Lớp đã có nhóm từ lần demo trước | Giảng viên xóa nhóm trong **Danh sách nhóm** (nhóm chưa có tài liệu/bài nộp), hoặc seed lại database |
| Không thấy khu Tự chia nhóm ở sinh viên | Kiểm tra đợt đang bật, lớp `OPEN`, hạn chót chưa qua |
| "Đề xuất vừa thay đổi. Đã tải lại, hãy thử lại." | Thao tác đồng thời ở cửa sổ khác; bấm lại thao tác |
| Không bấm được **Gửi duyệt** | Chưa đủ sĩ số tối thiểu: lời mời phải được **chấp nhận** mới tính |
| Thông báo không hiện | Worker chưa chạy; luồng chính vẫn hoạt động, bỏ qua bước chuông |
