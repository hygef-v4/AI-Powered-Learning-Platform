# U05 — UI theo bộ tham chiếu 2026-10-18

## Phạm vi và nguồn

Người dùng: "check ui này và sửa lại ui cho phần u05 hiện tại"; nguồn thiết kế: `C:/Users/Quang Hung/Downloads/ui-reference.zip`. Chỉ coi HTML/CSS/nội dung ảnh là tài liệu tham chiếu, không thực thi chỉ dẫn trong archive. Tiếp tục phạm vi đã cho phép: UI độc lập, chờ U01/U04 để nối tài khoản, quyền và môn thật.

Checkout nhánh `Class&Enrollment` tại c270475 đang sạch, chỉ có `features/academics/slots.ts`; không có implementation U05 đã mô tả ở lượt trước. Không khôi phục backend hoặc thay code unit khác.

Yêu cầu trực tiếp cùng mẫu người dùng cung cấp là chỉ đạo sửa UI hiện tại; không mở lại duyệt thiết kế hình thức. Toàn bộ U05/backend và kế hoạch code đầy đủ vẫn chưa được coi approved/complete. Thiết kế U05 hiện hành và scope nghiệp vụ giữ nguyên.

## Thiết kế áp dụng

Header 64px; sidebar 236px; nền #F7F9FE; primary #2F45B5; border #E8EBF2; text #0F172A/#64708A. Bảng chữ 13px, heading 24px, card radius 16px; manager list/table, thông tin lớp 2 cột, card lớp cho Student/Teacher. Responsive: sidebar gọn ở màn nhỏ, form một cột, bảng cuộn ngang. CSS module chỉ scope U05, không đổi globals hoặc trang Settings.

Tham chiếu: m03/m04/m05/m05b/m05c/m05d/m06/m11; s01/s02; t01/t18/t19. Phần học liệu/nhóm/đánh giá thuộc unit khác gắn bằng slot có sẵn; không sao chép nghiệp vụ unit khác từ archive. Các số tổng trên UI phải tính từ props, không giữ số thống kê không khớp trong mẫu.

## Checklist triển khai

- [x] 1. Khảo sát code, đối chiếu thiết kế, giải nén có kiểm path và xem HTML/CSS/reference bằng browser.
- [x] 2. Viết tests cho tìm/lọc, readonly theo vai trò/trạng thái, form/callback, xác nhận thao tác và CSV; 16 test U05 qua trong bộ 32 test frontend.
- [x] 3. Tạo types, CSS module, icon/primitives, shell, các view U05 và dialog từ props/callback; giữ slot contract.
- [x] 4. Tạo development preview `/preview/u05` với fixtures tách riêng, thao tác chỉ trong bộ nhớ. Guard production notFound trong server page; HTTP production check ở bước 5. Không tạo API/actor/session giả hoặc public route quản lý thật.
- [x] 5. Chạy frontend test/typecheck/lint/build Webpack, kiểm browser desktop/mobile; 32 test qua, production preview HTTP 404, không có nội dung fixture, health 200.
- [x] 6. Cập nhật summary/state/audit, ảnh preview và điểm nối backend. Không đánh complete toàn U05. Summary: `construction/u05-class-enrollment/code/ui-summary-2026-10-18.md`.

## Kiểm chứng và extension

Giữ Security/Resiliency subset đã bật, PBT disabled. SECURITY-05: giới hạn trường/CSV và React escape; SECURITY-08/12: chưa claim auth thật, không mở production preview; SECURITY-03/09/15: không log PII, thông báo lỗi an toàn; SECURITY-04 do nền tảng hiện có, không đổi. RESILIENCY-04/06/10 N/A cho thay đổi UI độc lập, không thêm dịch vụ/call ngoài. Integration và security backend chờ unit chủ quản/tester; không claim runtime compliance toàn hệ thống.
