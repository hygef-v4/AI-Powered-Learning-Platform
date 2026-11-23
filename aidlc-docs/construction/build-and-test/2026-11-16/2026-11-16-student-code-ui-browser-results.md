# Kiểm thử UI MSSV bằng browser Orca — 2026-11-16

## Môi trường và phương pháp

Người dùng yêu cầu mở browser và chạy test UI, sau đó cung cấp ảnh tài khoản local để đăng nhập. Đã mở browser bên phải trong worktree audit-staging, dùng localhost:3000 và backend localhost:8080; kiểm tra vai trò giảng viên và sinh viên trên lớp SE1933. Không chép mật khẩu vào tài liệu.

Hai viewport: desktop 1440×900 và mobile 390×844. Thực hiện thao tác trên browser, đọc cây accessibility/DOM và xem ảnh chụp. Các kết quả bên dưới tách dữ liệu thực và fixture; đây là lượt kiểm tra có điều khiển qua CLI, không thêm dependency hay bộ E2E vào ứng dụng.

**Dữ liệu thực:** API `/api/v1/classes/83ef8404-7394-4ed1-b3ba-bda00a97797d/groups` trả MSSV null cho cả 5 thành viên. Vì vậy không thể xác nhận luồng MSSV khác rỗng từ database bằng các tài khoản này.

**Fixture chỉ trong browser:** tạm bọc fetch để thêm mã SE99xxxx vào trường studentCode rỗng của JSON GET thành công ở các endpoint đọc liên quan. Giữ nguyên ID, tên, email, role, trạng thái và quyền; không sửa response lỗi hay request ghi. Mapping ổn định theo accountId trong mỗi lượt fixture; mã giữa lượt giảng viên/sinh viên không phải dữ liệu thật. Fixture có self-check và nằm tại `.tmp/student-code-ui-overlay.js`, không thuộc mã ứng dụng. Đây chỉ xác minh phần trình bày/client; không phải bằng chứng end-to-end lưu/tìm mã ở backend.

## Kết quả đã quan sát

| Ca | Dữ liệu | Kết quả |
|---|---|---|
| Trang đăng nhập rộng/hẹp | Thật | Hiển thị đủ trường và nút; không tràn ngang |
| Thiếu MSSV ở danh sách/tiêu đề chấm điểm, bộ chọn trưởng nhóm và bảng quiz | Thật | Giữ tên/email, không hiện null/undefined hoặc dấu phân cách thừa |
| Chấm điểm → Theo sinh viên, rộng/hẹp | Fixture | Mã hiện ở danh sách và tiêu đề; chọn Student 2 giữ student=13b56efa-914e-44e7-bb54-4d0d3ff9871b trên URL, nhãn khớp |
| Tìm sinh viên trong danh sách lớp | Fixture | Nhập SE990002 trả đúng một dòng Student 2; đây là tìm phía client |
| Tiến độ bài nhóm | Fixture | Trưởng nhóm hiện tên và mã trên trang Sinh viên |
| Giảng viên mở Đổi trưởng nhóm, rộng/hẹp | Fixture | Tất cả option có mã; chọn Student 2 giữ đúng UUID trong select; đóng form, không lưu |
| Chấm thành viên bài nhóm, rộng/hẹp | Fixture | Nút Đánh giá, người thực hiện, bộ chọn và legend có mã; chọn từ phần bài mở đúng Student 2/UUID; các dòng hoạt động có đúng mã. Không chỉnh/lưu điểm |
| Sinh viên yêu cầu đổi trưởng nhóm | Fixture | Ô trưởng nhóm hiện tại và các option có mã; chọn đúng UUID, mobile hiển thị đủ nhãn; đóng form, không gửi |
| Tác giả lịch sử tài liệu nhóm, rộng/hẹp | Fixture | Tên người khác có mã; lịch sử bản của chính mình hiện Bạn · MSSV; xem được nội dung bản |
| Quiz của lớp | Thật | Tải bảng 5 sinh viên thành công; mỗi người 0 lượt, không mở chi tiết lượt |

Đã đo scrollWidth <= innerWidth ở các màn/overlay được kiểm tra (đăng nhập, gradebook, đổi trưởng nhóm, chấm thành viên và lịch sử). Bảng rộng có vùng cuộn riêng; không đồng nghĩa mọi phần của toàn ứng dụng đã được kiểm tra responsive. Không có regression MSSV được xác nhận trong các ca đã chạy.

## Ca chưa thể nghiệm thu trên browser

- Giao mục/tìm MSSV trong picker giao mục và hộp nộp nhóm: tài liệu thử có status=CLOSED, window=CLOSED, dù tài khoản sinh viên là leader. UI không mở chức năng giao/nộp trong trạng thái này. Không đổi hạn hoặc trạng thái bài để tạo ca thử.
- Duyệt đề xuất nhóm, lời mời vào đề xuất, duyệt yêu cầu đổi trưởng nhóm: lớp đang tắt tự chia nhóm và không xuất hiện danh sách cần duyệt/lời mời tương ứng trong giao diện. Không tạo đề xuất/yêu cầu mới.
- Hộp lịch sử lượt quiz và chi tiết: quiz 1 có 0 lượt của cả 5 sinh viên. Không bắt đầu/nộp quiz để tạo dữ liệu.
- Người trùng tên và tìm MSSV phía server: chưa có dữ liệu thật phù hợp trong browser; test tự động của lần sửa trước vẫn là bằng chứng cho các ca đã phủ ở tầng component/helper.

Các ca này không được tính là browser PASS; các suite component/backend đã chạy ở lượt triển khai được ghi trong kế hoạch riêng.

## Console và dữ liệu

Khi điều hướng đầy đủ tới trang quiz, công cụ ghi nhận 5 thông báo CSP chặn inline style. Chưa xác định nguồn là mã trang hay thao tác automation, chưa kết luận đây là regression của MSSV; cần tái hiện riêng trước khi đề xuất sửa CSP. Không nới CSP để bỏ cảnh báo. Console sau lần reload cuối của tài liệu nhóm không có error/warning trong 50 mục đọc.

Hai lượt fixture giảng viên/sinh viên ghi nhận 0 request ghi qua wrapper. Không bấm Lưu nhóm, Gửi yêu cầu, Lưu điểm, Giao mục hay Nộp bài. Thao tác đăng nhập/đăng xuất chỉ thay phiên local. Đã reload để gỡ fixture và đo lại: fixtureActive=false, syntheticCodesVisible=false. Browser giữ mở ở tài liệu nhóm, tài khoản sinh viên, viewport 1440×900.

## Ảnh kiểm thử local

Ảnh dưới `.tmp` được Git ignore và chỉ phục vụ review local:

- [Gradebook desktop](../../../../.tmp/student-code-ui/gradebook-wide.png), [mobile](../../../../.tmp/student-code-ui/gradebook-mobile.png).
- [Đổi trưởng nhóm desktop](../../../../.tmp/student-code-ui/teacher-leader-wide.png), [mobile](../../../../.tmp/student-code-ui/teacher-leader-mobile.png).
- [Chấm thành viên desktop](../../../../.tmp/student-code-ui/grading-member-wide.png), [mobile](../../../../.tmp/student-code-ui/grading-member-mobile.png).
- [Sinh viên đề xuất trưởng nhóm](../../../../.tmp/student-code-ui/student-leader-mobile.png).
- [Lịch sử desktop](../../../../.tmp/student-code-ui/history-wide.png), [mobile](../../../../.tmp/student-code-ui/history-mobile.png).
- [Quiz chưa có lượt làm](../../../../.tmp/student-code-ui/quiz-empty-attempts-wide.png).

Không sửa mã ứng dụng, dependency, MSSV hoặc dữ liệu học tập; không commit/push trong lượt kiểm tra UI này.


## Sau yêu cầu bổ sung MSSV thật vào seed DB

Người dùng tiếp tục yêu cầu “giúp tôi insert mssv vào seed db có định dạng HE19xxxx”, làm rõ “x là số”. Đã backfill database local cho sáu tài khoản STUDENT chưa có mã:

| Tài khoản local | MSSV |
|---|---|
| u05.student@school.test | HE190001 |
| u05.student2@school.test | HE190002 |
| u05.student3@school.test | HE190003 |
| u05.student4@school.test | HE190004 |
| u05.student5@school.test | HE190005 |
| qa.u15.20261023@school.test | HE190006 |

Transaction kiểm thử đã rollback trước khi ghi thật; sau COMMIT, kiểm tra regex/unique/role đạt, chạy lại UPDATE 0. Chỉ thay mã đang null cùng metadata version/updated_at; không sửa mật khẩu/trạng thái/role. Script thử SQL giữ local ở `.tmp/seed-student-codes.sql`, không thay migration hoặc script e2e seed của bộ tài khoản khác.

Sau reload, API group-docs trả 5 mã HE190001–HE190005 và browser Nhóm của tôi hiển thị đủ năm mã thật; fixtureActive=false, không tràn ngang ở 1440px. [Ảnh mã thật](../../../../.tmp/student-code-ui/real-he19-student-group.png). Đây là xác nhận thêm với dữ liệu database thật; các giới hạn về bài CLOSED, proposal và quiz 0 lượt ở trên vẫn còn. Chưa chạy lại toàn bộ ma trận UI rộng/hẹp sau khi backfill.


## Tách cột MSSV và Email theo ảnh phản hồi

Đã sửa riêng bảng Sinh viên trong ClassGroupsPanel, dùng TableFrame/Th/Td hiện có. Sáu cột lần lượt là MSSV, Họ tên, Email, Nhóm, Ngày vào lớp, Bảng điểm. Mã có độ rộng cố định và chữ số thẳng hàng; tên đậm, email cùng cỡ chữ của dòng và màu phụ, ngày không xuống dòng. Tooltip giữ tên/email/nhóm dài, nhãn Trưởng nhóm có thể xuống dòng để tránh chen tên. Thiếu mã/email hiện dấu gạch ngang.

Kiểm chứng:

- `npx vitest run tests/features/groups --reporter=dot`: 9 file, 50/50 test đạt. Không thêm test chỉ để khóa CSS/bố cục.
- `npx eslint src/features/groups/components/ClassGroupsPanel.tsx`: đạt.
- `npx tsc --noEmit --incremental false`: đạt, không đổi cấu hình.
- Browser với HE19 thật: mỗi dòng 6 ô, ô MSSV chỉ có mã, ô Email chỉ có email; header khớp thứ tự. Tìm HE190002 trả đúng 1 sinh viên, tìm không có kết quả cho colspan=6, xóa tìm kiếm trả lại 5 dòng. Link Bảng điểm vẫn trỏ đúng accountId.
- Desktop 1920×1080: hiện đủ bảng. 1440×900 và 390×844: bảng giữ độ rộng đọc được và cuộn ngang trong khung; trang không bị tràn ngang. Mobile đã cuộn tới cột Ngày vào lớp/Bảng điểm và kiểm tra nhãn/link; không bấm hành động ghi.

[Ảnh desktop](../../../../.tmp/student-code-ui/student-columns-wide.png), [mobile bên trái](../../../../.tmp/student-code-ui/student-columns-mobile-left.png), [mobile cuộn bên phải](../../../../.tmp/student-code-ui/student-columns-mobile-right.png).

Chưa chạy production build hoặc kiểm tra các browser engine khác ngoài browser Orca. Không đổi backend/database hoặc commit/push. Cảnh báo React act/jsdom từ suite hiện hữu vẫn có, không có test thất bại.
