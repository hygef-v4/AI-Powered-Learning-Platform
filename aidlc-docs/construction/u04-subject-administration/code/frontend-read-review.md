# U04 — review màn hình đọc và kiểm quyền consumer

> Ghi chú xuất bản18/10/2026: tài liệu này ghi bằng chứng của một checkpoint. Trạng thái và gate mới nhất ở [frontend-handoff](frontend-handoff.md); quyết định cần giữ ở [implementation-decisions](implementation-decisions.md). File TEMP đã được loại khỏi thay đổi xuất bản theo yêu cầu commit/push sau đó.

Lát cắt ngày 18/10/2026 theo yêu cầu “tiếp tục đi”. Có route danh sách/chi tiết, tìm kiếm/phân trang và tab lớp chỉ đọc. F03/F07 hoàn thành ở cấp code và developer checks; F02 còn RoleGuard, F05/F06 mới có phần hiển thị. Chưa hoàn thành toàn U04.

## Thứ tự đọc code

Các đường dẫn dưới đây tính từ repository root.

| Thứ tự | File | Điều cần tự giải thích |
|---|---|---|
| 1 | `frontend/src/features/subjects/useSubjectRead.ts` | Key gồm ID/query và lần retry. Đổi key che dữ liệu cũ ngay; cleanup bỏ response đến muộn. Không hủy network request, không cache quyền giữa các request. |
| 2 | `frontend/src/features/subjects/SubjectReadError.tsx` | Thông báo an toàn cho 401/403/404 và lỗi tạm thời; không đưa raw provider detail ra UI. |
| 3 | `frontend/src/features/subjects/SubjectListPage.tsx`, `SubjectFilters.tsx`, `SubjectTable.tsx` | UC16, BR-U04-50: mặc định ACTIVE, debounce300ms, đổi tìm kiếm/trạng thái/size reset trang; size20/50/100. Counts hai trạng thái lấy từ server, không đếm trang hiện tại. Hàng và tên mở chi tiết, tránh điều hướng hai lần khi bấm link. |
| 4 | `frontend/src/features/subjects/SubjectDetailPage.tsx`, `ManagerCard.tsx`, `SubjectMetaCard.tsx` | UC18: mã/tên/mô tả, chủ nhiệm và ngày tạo timezone Asia/Ho_Chi_Minh. Hai tab dùng bàn phím mũi tên được. Chỉ đọc metadata, chưa có edit form. |
| 5 | `frontend/src/features/subjects/SubjectClassesTab.tsx`, `SubjectClassesTable.tsx` | UC18, BR-U04-05/Q07: chỉ năm cột mã/tên/học kỳ/giảng viên/trạng thái; không student count, link vào lớp hoặc nút sửa lớp. Filter client theo contract danh sách hiện tại, tổng môn không đổi theo filter. |
| 6 | `frontend/src/app/admin/subjects/page.tsx`, `frontend/src/app/admin/subjects/[id]/page.tsx` | Route mỏng dựng feature; dynamic params là Promise theo Next16. Giữ đăng ký menu hiện có, không thêm nav trùng. |
| 7 | Ba file test `SubjectListPage.test.tsx`, `SubjectDetailPage.test.tsx`, `SubjectClassesTable.test.tsx` trong feature | Đọc tình huống trước rồi lần theo luồng state/request/render tương ứng. |

`managerId=null` hiển thị “Chưa phân công”; có managerId nhưng không lấy được contact hiển thị “Không tra được chủ nhiệm”. `classCount=null` là “Không khả dụng”, không ép thành0. Classes UNAVAILABLE có retry, khác AVAILABLE với items rỗng. Tab lớp chỉ fetch khi mở và đã đọc được môn. Nếu classes trả401/403, che cả phần chi tiết và retry phải đọc lại môn, tránh dùng dữ liệu cũ sau khi quyền bị từ chối.

## Tự kiểm và bằng chứng

Tại thư mục `frontend`, chạy `npm.cmd run test`, `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build`. Kết quả lượt này: **70 test/17 file PASS**, typecheck/lint/build PASS; thêm21 test màn hình. Build nhận hai route subjects. Lỗi đầu tiên do dùng tên icon search chưa có trong thư viện chung đã được sửa tại feature; không sửa thư viện icon, tắt lint hoặc nới test.

1. Đọc test list: typing nhanh và response đảo thứ tự; đổi filter/size reset page; chuyển trang; unassigned/unknown/zero; empty/error/retry và401/403. Khi debounce chưa hết, không trình bày hàng cũ như kết quả query mới.
2. Đọc test detail: lazy classes, timezone, contact fallback, unavailable khác0,401/403/404, class403 che chi tiết rồi retry xác thực lại, đổi subjectId khi request cũ chưa xong, điều hướng tab bằng bàn phím.
3. Đọc test classes: đủ ba trạng thái/nhiều kỳ/thiếu giảng viên, lọc không đổi tổng,0 lớp; kiểm không có điều hướng hoặc mutation lớp.
4. Đã mở trình duyệt desktop để kiểm bố cục list/detail/classes và bấm filter OPEN (1/3 lớp, tổng tab vẫn3). API fixture chỉ chạy loopback trong `.tmp` cho QA, không import vào source hay làm fallback production. Đã đóng tab và dừng hai server QA. Đây không phải chứng cứ tích hợp provider/auth thật hoặc kiểm toàn bộ mobile/cross-browser.

Backend: mở `backend/src/test/java/edu/aiplatform/subjects/web/SubjectAdminControllerTest.java`, tìm matrix cho cả bảy endpoint. Thêm7 lượt kiểm: ba role khác Admin, anonymous, U01 deny, unavailable và null decision. Các nhánh đều chặn truy cập repository/account/classes/gate và ghi DENIED. Chạy `mvn.cmd -f backend/pom.xml "-Dtest=SubjectAdminControllerTest,ArchitectureTest" test`: **42 PASS =39 HTTP consumer +3 architecture**,0 lỗi/skip. Principal do fixture phía server đặt; standalone MockMvc không chạy filter chain/JWT/CSRF. Không có thay đổi backend production ở lát cắt này. Full Maven271/31 PostgreSQL là bằng chứng lượt B11 trước, không chạy lại toàn bộ DB ở lượt này.

## Các phần còn lại và điểm bàn giao

- F02: hai route đã có; RoleGuard chưa có. `useCurrentUser.ts` hiện không cung cấp role/loading/auth state và admin layout hard-code ADMIN. Không dùng sidebar/layout để khẳng định quyền; backend vẫn quyết định. Không thêm token localStorage, login/refresh hoặc U01 guard giả.
- B12 vẫn pending JWT/session/current account/CSRF/filter-chain thật; U01/U02 cần thống nhất audit chống trùng giữa filter và service. OTP/email thuộc U01; không tự thêm OTP hay email vào CRUD môn.
- F04 tiếp theo: dialog thêm môn và ManagerPicker. Account search endpoint hiện là đề xuất cần owner xác nhận; không đưa fixture account vào production. Có thể tiếp tục component/consumer test độc lập trước khi nhận provider.
- F05/F06/F08 còn edit metadata, đổi/bỏ chủ nhiệm, archive/reopen, conflict/reject dialogs và tests tương ứng. Chưa có nút giả không hoạt động để tạo cảm giác những phần đó đã xong.
- B03 schema U01, B08 account guard/Q14 disable auto-unassign, B09 checker/writers U05, B10 durable/dedup audit, NFR p95 và tester độc lập giữ pending. Không lấy70/42 test thay bằng chứng tích hợp thật.

Sau checkpoint này, người dùng yêu cầu tạo commit/push. Tài liệu TEMP đã được dọn khỏi tập thay đổi xuất bản; quyết định hiện hành ở [implementation-decisions](implementation-decisions.md). Lịch sử kiểm thử bên trên giữ nguyên phạm vi; không có cập nhật Jira.

## Phạm vi kiểm soát

SECURITY-05: field/query bound và thông báo lỗi an toàn; SECURITY-08/15: consumer authorization-before-data/fail-closed được kiểm thêm, UI không coi counts unknown là0. SECURITY-03/09: không thêm secret, auth fallback hoặc provider production giả. SECURITY-12 và bảo mật HTTP/filter chain SECURITY-04 chưa được chứng nhận bằng fixture. RESILIENCY-10: bỏ response cũ, retry và unavailable đã kiểm ở consumer; thời gian provider/p95 vẫn pending. RESILIENCY-04/06 không đổi deploy/infra/health. PBT disabled; các control ngoài subset cấu hình không áp dụng cho lát cắt này.
