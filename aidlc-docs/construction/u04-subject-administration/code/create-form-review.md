# U04 — review form thêm môn và ManagerPicker

> Ghi chú xuất bản18/10/2026: tài liệu này ghi bằng chứng của một checkpoint. Trạng thái và gate mới nhất ở [frontend-handoff](frontend-handoff.md); quyết định cần giữ ở [implementation-decisions](implementation-decisions.md). File TEMP đã được loại khỏi thay đổi xuất bản theo yêu cầu commit/push sau đó.

Lát cắt F04 ngày18/10/2026, UC17/BR-U04-02/03/06/53. Đã có code form/picker và consumer tests; **F04 tổng còn chờ adapter tìm chủ nhiệm U01 và tích hợp thật**, không đánh dấu cả unit hoàn thành.

## Thứ tự đọc để nắm code

| Bước | File trong repository root | Điều cần kiểm |
|---|---|---|
| 1 | `frontend/src/features/subjects/managerSearch.ts` | Boundary nhận q/limit20, trả candidate accountId/displayName/email/role/status. Đây là kiểu nội bộ đề xuất, không phải schema HTTP U01 đã thống nhất. Mặc định ném503 ngay, không gọi endpoint đoán hoặc trả fixture. |
| 2 | `frontend/src/features/subjects/ManagerPicker.tsx` | Debounce300ms/q max200; bỏ response cũ qua useSubjectRead; pending/empty/error/retry khác nhau. Chọn bằng select bàn phím chuẩn, hiển thị lựa chọn riêng, bỏ lựa chọn trả null. Phòng adapter trả role/status sai và giới hạn20 option; không tải mọi account rồi lọc thay server. |
| 3 | `frontend/src/features/subjects/SubjectFormDialog.tsx` | Draft code/name/description/manager; validate trước POST; normalize code/name, giữ nội dung description, gửi managerId hoặc null. Component mount khi mở/unmount khi đóng để lần sau có draft mới. |
| 4 | `frontend/src/features/subjects/SubjectListPage.tsx` | Nút thêm bị vô hiệu khi list đang tải/lỗi. Chỉ sau create thành công mới đóng/refetch list/counts và hiện thông báo. Giữ filter hiện tại: môn mới có thể chưa khớp filter hoặc ở trang khác. Không tự ghép fake row/count vào list. |
| 5 | `frontend/src/shared/ui/Dialog.tsx` | Thêm size lg540px, mặc định md420px; phần nội dung scroll trong viewport, footer vẫn nhìn được. Focus nằm trong dialog, trả về nút mở khi đóng; đổi callback không kéo focus khỏi ô đang nhập. dismissible=false khóa close/Escape/backdrop trong POST pending. Các consumer cũ giữ mặc định đóng được. |
| 6 | `frontend/src/features/subjects/SubjectFormDialog.test.tsx`, `ManagerPicker.test.tsx`, `SubjectListPage.test.tsx`; `frontend/src/shared/ui/Dialog.test.tsx` | Lần theo mỗi lỗi/luồng submit, phân biệt mock consumer với provider thật. |

`pending` ref được đặt trước await để cả hai sự kiện submit liên tiếp chỉ có một POST. Busy khóa input/picker/submit/cancel/close đến khi biết kết quả. Lỗi trùng mã hiện dưới code; chủ nhiệm mất hiệu lực hiện lỗi để chọn lại hoặc bỏ;401/403 có thông báo riêng. Lỗi khác giữ draft và báo chưa xác nhận thành công: không tự retry một mutation vì mất response không có nghĩa server chưa commit. Không đưa raw SQL/provider detail ra UI. Callback sau thành công tách khỏi catch POST, nên lỗi refetch không thành lỗi tạo và không kích hoạt POST lại.

## Tự kiểm

1. Chạy tại `frontend`: `npm.cmd run test`, `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build`.
2. Tạo tên trắng/mã sai phải bị chặn trước API; code tự uppercase, max20, tên max200, mô tả max2000. Quy tắc server vẫn quyết định cuối cùng.
3. Mặc định chưa nối U01: thấy thông báo không tải danh sách chủ nhiệm, vẫn nhập và tạo môn chưa phân công. Không được diễn giải thông báo này là “không có Chủ nhiệm môn trong hệ thống”.
4. Test dùng hàm search được inject để chọn/bỏ manager; chọn từ kết quả rồi mô phỏng backend trả SUBJECT_MANAGER_INVALID, giữ input và cho bỏ lựa chọn. Nhãn ACTIVE trong UI không thay khóa/kiểm account của backend.
5. Giữ POST chưa resolve rồi submit hai lần, Escape/backdrop/close/cancel: chỉ một POST, dialog không đóng. Khi POST thất bại, input được mở lại và giữ draft. Cancel trước submit không gọi POST.
6. Test create success rồi refetch list thất bại: vẫn báo môn đã tạo, retry chỉ GET; mở lại form rỗng. Tạo thành công không phụ thuộc lookup manager khi managerId=null.

Kết quả cuối: **92 test/20 file PASS**, thêm22 lượt so với baseline70. Gồm7 picker,10 form,3 shared Dialog và2 list integration case mới. Typecheck/lint/production build PASS; build kiểm TypeScript của source cuối sau sửa focus. Lần đầu Vitest lỗi ENOENT cache transform và SWC lỗi Access denied trước khi kiểm source; chạy lại với quyền đọc đường dẫn hợp lệ đã pass, không sửa dependency/config hoặc nới assertion.

Browser desktop QA dùng API fixture loopback riêng: mở form, nhập chữ thường được uppercase, tạo chưa phân công, thấy success/count/row từ refetch. Phát hiện nút mở disabled lúc refetch làm mất focus sau close; đã sửa list chờ GET xong trả focus về nút, hoặc focus thông báo success nếu GET lỗi, thêm assertions và chạy lại92 test. Đã xác nhận focus trên trình duyệt sau sửa. Fixture ban đầu có ID trùng khi tạo hai môn gây warning React; sửa UUID trong fixture, chạy lại với dữ liệu sạch, không sửa component để che warning. Đây là QA UI, không phải backend/provider/auth integration. Ảnh bằng chứng `u04-create-qa.png` được gửi riêng, không đưa vào Git. Đã đóng tab, xác minh PID/command line rồi dừng server riêng, dọn fixture/script/metadata và file hướng dẫn Next tự sinh. Không chứng nhận mobile/cross-browser.

Backend production không sửa trong lát cắt này; không chạy lại Maven/DB vì không có thay đổi backend. Chưa có bằng chứng JWT/session/CSRF hay U01 account search thật.

## Bàn giao cần chốt

Thiết kế có dự kiến `GET /api/v1/admin/accounts?role=SUBJECT_MANAGER&status=ACTIVE&q=...`, nhưng checkout chưa có implementation/schema response hoặc xác nhận owner. Không tự quyết định items/content, page/size, limit hoặc tên field contact của U01. Khi bàn giao: cài adapter trong managerSearch.ts dùng apiFetch/shared auth, chuyển schema sang candidate; lọc role/status ở server và phân trang tối đa20. Sau đó kiểm API thật với Admin/non-Admin/session hết hạn, người bị disable khi picker đang mở, và manager gate khi POST. Props searchManagers phục vụ consumer test hoặc kết nối client-to-client; không truyền function qua Server Component route.

Không bổ sung OTP/email: UC17 không yêu cầu; không sửa auth/shared API/CSRF hoặc triển khai U01. B03 schema, B08 account guard/Q14, B09 U05 checker/writers, B10 durable/dedup audit, B12 RoleGuard/JWT/CSRF và tester/NFR vẫn pending. Bước tiếp theo F05 metadata edit/version conflict. Tài liệu TEMP phải dọn trước PR main; không commit/push/Jira hoặc sửa index.

SECURITY-05/15: validation và lỗi an toàn; SECURITY-08: không coi picker/role UI là authorization, server vẫn chặn. SECURITY-03/09: không log PII/secret hoặc thêm provider giả cho production; SECURITY-12/04 auth/header integration pending, không thay đổi. RESILIENCY-10: stale-response/retry và khóa gửi trùng được kiểm tại consumer, deadline provider thật chưa được chứng nhận. RESILIENCY-04/06 không đổi infra/deploy/health; PBT disabled, ngoài subset cấu hình N/A.
