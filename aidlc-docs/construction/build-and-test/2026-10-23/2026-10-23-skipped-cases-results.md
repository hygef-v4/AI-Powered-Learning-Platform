# Kết quả xử lý các ca bỏ qua — 2026-10-23

Artifact: [Kiểm thử hệ thống U01–U15](https://claude.ai/artifact/Wbj4CKoyBcDE6dPyCHbour)

## Tóm tắt

- Baseline khi lập kế hoạch: 354 ca, 62 Đạt, 3 Lỗi, 49 Bỏ qua, 240 Chưa test.
- Snapshot artifact đọc trực tiếp ngày 2026-10-23: 354 ca, 65 Đạt, 2 Lỗi, 46 Bỏ qua, 241 Chưa test; riêng U13 có 11 Đạt/11 Bỏ qua.
- U13-02, U13-04 và U13-06 hiện mang verdict Đạt; U05-06 cũng Đạt. Hai lỗi còn lại không thuộc đợt 49 ca bỏ qua.
- Chưa hoàn tất 46 ca Bỏ qua còn lại; không coi việc tạo fixture là bằng chứng pass.
- Dữ liệu QA được tạo qua UI, không INSERT trực tiếp DB. Không có migration, commit, push hay deploy.

## Ca đã chạy

| Ca | Kết quả hiện tại | Dữ liệu và bằng chứng |
|---|---|---|
| U05-06 | Đạt | Tạo lớp `QA-SKIP-20261023A` ở Nháp khi chưa có Teacher; UI chặn mở lớp và nêu rõ phải phân công Teacher. Gán U05 Mock Teacher, mở lớp thành công; đăng nhập Teacher xác nhận lớp hiện trong “Lớp được phân công”. Artifact đã ghi note và Đạt. |
| U13-02 | Đạt | PDF hợp lệ được upload ở Manager > Học liệu; thấy progress và nút Hủy khi đang tải, trạng thái hoàn tất/dung lượng sau tải; lưu học liệu phiên bản 1. Lỗi `CSRF_INVALID`/“Phiên làm việc không hợp lệ” đã không còn tái hiện sau fix XHR CSRF. Nhánh ZIP bị chặn đã được kiểm trong lượt trước. Artifact cập nhật Đạt. |
| U13-04 | Đạt sau sửa | PDF text hai trang có 544 ký tự trích xuất. Lần đầu worker cập nhật DB `EXTRACTED`, nhưng danh sách vẫn ghi “Đang xử lý” tới khi reload — ghi Lỗi vào artifact. Sau thêm polling danh sách khi còn PENDING, upload fixture retest chuyển “Đang xử lý” → “Đã trích” ngay trên tab in-app, không reload. Artifact hiện Đạt và giữ lại ghi chú lần lỗi đầu. |
| U13-06 | Đạt | Thay PDF trên học liệu qua UI tạo phiên bản 2 “Hiện hành”; phiên bản 1 còn trong lịch sử và có `EXTRACTED`/544 ký tự. PDF mới không có chữ và worker đưa về `NO_TEXT`. |
| U13-09 | Bỏ qua | Đăng nhập Student, mở lớp U05-A: thấy 4 mục theo thứ tự, loại PDF và dung lượng 1 KB. Đọc DB chỉ xác nhận cả 4 học liệu là scope `SUBJECT`; chưa có fixture `CLASS` để kiểm nhãn “của lớp”. Ghi chú U13-09 trong artifact được thay bằng kết quả này. |
| U13-10 | Bỏ qua | Student mở chi tiết PDF có chữ và thấy nút tải; thao tác tải điều hướng tới endpoint tệp cục bộ nhưng chưa đối chiếu bytes. Iframe xem trước trắng, không xác nhận được hiển thị/chuyển trang; bằng chứng video/embed/highlight trước đó vẫn còn. |

## Nguyên nhân và thay đổi code

1. Upload tệp dùng XHR ngoài shared fetch client nên bỏ header CSRF. Đưa thao tác đọc cookie và tên header vào shared client; XHR gửi cookie `XSRF-TOKEN` đã decode qua `X-XSRF-TOKEN`, giữ `withCredentials` và không tự đặt `Content-Type` multipart. Thêm `frontend/src/features/files/api.test.ts`.
2. Danh sách học liệu chỉ lấy trạng thái trích chữ một lần. `useMaterialResource` giờ hỗ trợ poll có điều kiện, giữ dữ liệu cũ trong lúc làm mới và dừng khi không còn tệp PENDING; `MaterialList` poll mỗi 3 giây khi còn tệp như vậy. Thêm regression trong `frontend/src/features/materials/MaterialList.test.tsx`.

## Fixture manifest

Chi tiết ID lớp/học liệu, `fileId`, SHA-256, kích thước và giới hạn của từng PDF nằm trong `.tmp/skipped-cases/manifest.json` (thư mục `.tmp/` đã được ignore). `qa-text-pages.pdf` được xác minh là hai trang và mỗi trang có 257 ký tự theo PyPDF; worker trích 544 ký tự. Hai PDF fixture đầu ghi `NO_TEXT`: tệp đầu không có lớp chữ; tệp thứ hai ít hơn ngưỡng 50 ký tự/trang nên không dùng để đánh giá trích chữ.

## Kiểm chứng kỹ thuật

- Regression upload: lần đầu đỏ đúng tại thiếu `X-XSRF-TOKEN`; sau sửa, upload helper test đạt.
- U13-04: regression list refresh đỏ trước sửa; sau sửa, test đạt.
- Test liên quan cuối: 6 file / 37 test đạt (MaterialList, MaterialDialogs, materials API, shared status, file upload API, shared API client).
- TypeScript check và ESLint các file đổi đã qua sau sửa.
- Full frontend suite: 79 file, 450 test đạt. `npm run typecheck`, ESLint các file đổi và `npm run build` đều hoàn tất thành công.

## Môi trường và phần còn lại

- Backend, worker, PostgreSQL local đều báo healthy; UI in-app tại `http://127.0.0.1:3000` dùng được. Nginx container restart do không phân giải upstream `frontend`; test UI trực tiếp không đi qua upstream đó.
- Còn 46 ca Bỏ qua theo snapshot artifact: U05-07/09/12/21/22/23, 24 ca U09, 11 ca U13, 4 ca U14 và U15-05. Cần tiếp tục tạo fixture tài khoản, nhóm, bài nộp/điểm, backup và kiểm gate provider/notification/time-window theo plan.
- Khi tiếp tục, đăng nhập Teacher và Subject Manager đều bị từ chối; truy vấn chỉ đọc cho thấy ba account U05 đang `ACTIVE`, chưa bị khóa tạm. Sau khi refresh login và thử Student đúng một lần nữa, tài khoản vẫn bị từ chối; bộ đếm Student là 2, Teacher/Manager là 1. Không đổi thông tin đăng nhập. Student đã mở lớp/học liệu thành công trước khi đăng xuất.
- Artifact hiện cho kết quả/vấn đề đúng ở U13 nhưng một số ghi chú U13-02/04/06 cũ không nhận thay đổi văn bản qua editor (note chỉ lưu ở field tạm sau khi đổi unit). Không tuyên bố các note đó đã cập nhật; U13-09 đã lưu được note mới. Cần xử lý đồng bộ ghi chú artifact trước khi đóng P6.
- Chưa dùng provider AI thật, chưa thử hiệu ứng tính phí, không gọi email thật và không ghi nhận notification/reminder đã seed như sự kiện đã gửi.

## U09 continuation — 2026-10-23T15:54:12Z

- Đọc lại artifact `Kiểm thử hệ thống U01–U15`: U09 có 26 ca, 2 Đạt, 0 Lỗi, 24 Bỏ qua; verdict không đổi vì chưa có bằng chứng UI mới.
- DB PostgreSQL local khỏe. Truy vấn chỉ đọc xác nhận bài Graded `Kiểm thử U09 - bài nộp mẫu` đã tồn tại ở U05-A (ID `6f2b4350-c727-4864-a39c-09da8496bf13`, OPEN, tối đa 2 lượt), nhưng có 0 submission và 0 evaluation. Không tạo bài trùng và không ghi dữ liệu DB trong lượt này.
- In-app UI đang ở `/login` với thông báo xác thực chung. Audit DB ghi `BAD_PASSWORD` cho lần đăng nhập Teacher, Subject Manager và Student; tài khoản ACTIVE, chưa bị khóa. Dừng đăng nhập để không tăng bộ đếm sai.
- Test frontend liên quan: 4 file, 26/26 đạt. Full frontend suite: 79 file, 450/450 đạt. Lần chạy đầu bị ENOENT ở temp sandbox; chạy lại với thư mục temp trong workspace và nhận kết quả trên. Không có thay đổi source trong lượt này.
- Claude artifact: ghi chú U09-02 đã lưu và xác nhận lại sau khi đổi unit; ghi chú U09-03 vẫn trở về nội dung cũ. Artifact giữ 24 ca Bỏ qua; không tuyên bố live U09 đã test xong.
- Backend test không chạy được vì máy không có lệnh Maven; Docker/PostgreSQL được kiểm tra chỉ đọc. Submission/evaluation fixture còn thiếu, cần phiên đăng nhập Teacher/Student hợp lệ để tiếp tục tạo qua UI và xác minh.

## U09 live-test continuation — 2026-10-23T16:52:53Z

- Người dùng cung cấp ảnh cho thấy tài khoản Teacher đã đăng nhập tại `/classes`; nhận định BAD_PASSWORD ở lượt trước đã lỗi thời. Không nhập lại mật khẩu. Fixture U09 được tạo qua UI trong lượt test trước: assignment `Kiểm thử U09 - bài nộp mẫu` ở U05-A (`6f2b4350-c727-4864-a39c-09da8496bf13`), submission `c60d54a1-5c0c-4178-a3ee-187c66295776`, evaluation `b3e8472d-079f-4e65-86cf-e239e0f20664`. Lượt hiện tại không ghi DB.
- Đã lưu ghi chú theo bằng chứng cho U09-01/02/03/05/08/13/15/16/18/19/20. Artifact đang hiển thị U09: 26 ca, 4 Đạt, 2 Lỗi, 20 Bỏ qua. U09-03, U09-13, U09-15, U09-16 và U09-18 vẫn Bỏ qua vì chỉ xác minh được một phần hoặc thiếu fixture; các ca chưa chạy khác vẫn Bỏ qua. Đã re-read U09-03 sau khi đổi unit; các ghi chú còn lại được nhập, blur và chờ autosave. Lệnh đổi unit kế tiếp bị timeout nên chưa kiểm chứng lại toàn bộ ghi chú qua unmount.
- U09-03: Teacher lưu rubric 10/10; điều chỉnh +1 thiếu lý do bị chặn, có lý do lưu bản nháp; chỉ một evaluation nên chưa kiểm chuyển bài kế — giữ Bỏ qua.
- Rà soát verdict cuối trong artifact: đặt U09-03 về Bỏ qua do thiếu bước chuyển sang bài kế; đặt U09-20 Đạt do Student đã xác nhận không thấy điểm/nhận xét khi evaluation còn DRAFT. Đọc lại trạng thái hai thẻ: U09-03 Bỏ qua, U09-20 Đạt; tổng U09 vẫn 4 Đạt/2 Lỗi/20 Bỏ qua.
- U09-05: “Chốt điểm” cho bản nháp 10/10 vẫn trả `FAILED · GRADE_VERSION_CONFLICT` sau tải lại. Không công bố điểm. Lỗi còn mở.
- U09-08: Hai tab mở cùng bài; tab mới lưu 10/10, tab cũ báo dữ liệu đã đổi/tải lại và không ghi đè server — Đạt.
- U09-13/U09-15: Sổ điểm có bốn cột Graded, không có Practice/Quiz hoặc tổng trọng số; U09 là 10/10 Đã chấm (nháp), ba bài khác Chưa nộp. Tiến độ lớp U09 là 1/1 (100%), Chờ chấm 0. Chưa có điểm công bố để kiểm phân bố; chỉ có một sinh viên — giữ Bỏ qua.
- U09-18: Đã kiểm biến thể cận 10+1→10 và 0−3→0; chưa kiểm chính xác 9+3 và 1−3 vì fixture rubric là một checkbox 10 điểm — giữ Bỏ qua.
- U09-19: UI trước sửa lưu được 10.000 ký tự; chặn 10.001 bằng thông báo chung `Invalid request content` và gợi ý tải lại phiên bản. Test-first `GlobalExceptionHandlerTest` đỏ vì response không có `code`. Đã thêm xử lý `MethodArgumentNotValidException`: giới hạn nhận xét trả `FEEDBACK_TOO_LONG` / “Nhận xét tối đa 10.000 ký tự.”, lỗi validation khác trả `INVALID_REQUEST`. Test đỏ trước sửa, xanh 1/1 sau sửa và re-run lúc `2026-10-23T17:01:44Z`; BUILD SUCCESS. Backend local đã rebuild và healthy; chưa chạy lại thao tác UI nên verdict vẫn Lỗi.
- U09-20: Student mở assignment trong khi evaluation còn DRAFT 10/10; danh sách và chi tiết đều báo chưa có điểm công bố, không lộ điểm/feedback — Đạt.
- Frontend dev server được khởi động lại ở `127.0.0.1:3000`; backend local được build lại từ source, Flyway báo schema up to date và container healthy. Công cụ in-app từ chối quan sát tab app theo chính sách URL; không dùng cách thay thế để đọc UI, vì vậy chưa xác nhận thay đổi hiển thị trên app. Không gửi email và không publish điểm.
- Cài đặt thông báo email `Công bố điểm` của Student đã được tắt ở lượt test trước để tránh email thật; lượt này chưa bật lại được do không thể thao tác tab app. Không có điểm được publish và không có email gửi đi. Cần bật lại tùy chọn này khi mở được UI.
- Cập nhật verdict U09 từ snapshot trước (2 Đạt/0 Lỗi/24 Bỏ qua) lên 4 Đạt/2 Lỗi/20 Bỏ qua. Toàn artifact suy ra 67 Đạt/4 Lỗi/42 Bỏ qua/241 Chưa test từ snapshot toàn cục gần nhất 65/2/46/241; bộ lọc toàn cục chưa được đọc lại. Lỗi GRADE_VERSION_CONFLICT vẫn chặn U09-05; U09-19 chờ retest trên runtime đã build mới.

## U09 tiếp tục: kiểm provider, sửa AI batch estimate và đồng bộ artifact — 2026-10-23T18:23:22Z

- Artifact Claude `Kiểm thử hệ thống U01–U15` vẫn có 26 ca U09: 4 Đạt, 2 Lỗi, 20 Bỏ qua. Không đổi trạng thái khi chưa có bằng chứng hộp đen mới. Đã cập nhật và kiểm tra note U09-02/03/05/08/10/11/13/18/19/20/22/23; note U09-11 vẫn giữ sau khi chuyển khỏi U09 rồi quay lại.
- Kiểm tra kết nối AI ở chế độ chỉ đọc: có 2 kết nối Gemini cá nhân ACTIVE, lần test lưu gần nhất SUCCESS; không đọc hoặc ghi khóa. Người dùng cho phép gọi Gemini để chấm, nhưng chưa gửi request mới vì công cụ in-app từ chối truy cập URL local theo chính sách. Không thay bằng HTTP/API/CDP hoặc browser khác.
- Đã sửa lỗi stale selection ở U09 AI picker: popup chỉ mount lúc mở nên nguồn AI cũ không còn chọn sẵn sau khi đóng/mở; regression test ban đầu đỏ đúng hành vi cũ.
- Đã bổ sung API estimate read-only cho U09 AI batch. Nó dựng prompt cho từng evaluation hiện hành, tổng hợp evaluation ID duy nhất và dùng chung công thức ước tính quota; không gọi provider và không giữ chỗ quota. UI truyền ước tính vào bộ chọn nguồn. OpenAPI đã đồng bộ.
- Xác minh: frontend 79 file / 452 test đạt và `npm run typecheck` đạt; backend 21 test U09/validation đạt, gồm batch estimate và công thức token. U09-05 vẫn Lỗi `GRADE_VERSION_CONFLICT` vì test không tái hiện được version xung đột; U09-19 vẫn Lỗi cho tới khi retest 10.000/10.001 ký tự trên app sau rebuild.
- Các ca hộp đen U09-10/11 chưa gọi Gemini; chưa kiểm bảng kết quả từng item hoặc quota SHARED hết hạn mức. Không seed DB trong lượt này, không publish điểm/gửi email, không commit/push/deploy. Tổng artifact giữ 4 Đạt / 2 Lỗi / 20 Bỏ qua.

- Bổ sung kiểm tra cuối lúc 2026-10-23T18:25:51Z: `npm run build` thành công (Next production build). ESLint mục tiêu không hoàn tất trong thời gian môi trường trả kết quả; không tính là pass.
- Đồng bộ artifact lại lúc 2026-10-23T18:28:56Z: U09-05 ghi rõ lỗi `GRADE_VERSION_CONFLICT` vẫn chưa tái hiện được bằng regression backend; U09-13 mô tả đúng một bài 10/10 nháp và ba bài chưa nộp. Đã đọc lại trực tiếp ghi chú sau autosave. Verdict vẫn 4 Đạt / 2 Lỗi / 20 Bỏ qua; chưa có lần gọi Gemini mới hoặc retest UI.
- Kiểm tra runtime lúc 2026-10-23T18:29:48Z: không có listener local trên cổng 3000/8080/5432; UI in-app hiện không vào được app để làm lượt retest/Gemini cuối. Không gọi endpoint thay thế.

## U05/U09/U15 tiếp tục — 2026-10-24

- U05-05: thêm regression đỏ trước sửa; nút lưu phân công hiện bị khóa khi giảng viên hiện tại không còn trong danh sách ACTIVE và người dùng chưa chọn giảng viên khác. U05-12: trong lúc dữ liệu nhóm chưa tải xong, không hiển thị dấu “—” như thể sinh viên không thuộc nhóm. Focused `academics.test.tsx`: 23/23 đạt sau sửa.
- U09: `BatchActionDialog` dùng key của lựa chọn hiện tại để loại estimate cũ và biểu diễn trạng thái đang tải; không còn gọi setState đồng bộ từ effect. `ai-source-integration.test.tsx`: 9/9 đạt.
- Frontend: toàn bộ Vitest 79 file / 454 test đạt; `npm run typecheck`, `npm run lint`, và production `npm run build` đều thành công. Sau build đã loại các đường dẫn `.next-verify-u05-u09-u15` do Next tự thêm vào `tsconfig.json`.
- Backend Maven suite mục tiêu: 233 test, 0 lỗi, 0 failure, 9 skip do Docker/Testcontainers. `GradingPersistenceTest` (U09 chốt qua persistence DB) và `BackupDashboardStatusIntegrationTest` (U15 lịch sử backup với DB thật) vẫn chưa chạy vì Docker engine access denied.
- Đã cập nhật và đọc lại ghi chú artifact: U05-05, U05-12, U09-05, U09-19, U15-05. Snapshot không đổi: U05 17/0/6; U09 4/2/20; U15 7/0/1 (Đạt/Lỗi/Bỏ qua). U15-05 ghi rõ test mock/backend đạt nhưng DB thật và mixed-state >20 lượt chưa xác minh. Không chuyển verdict chỉ từ test đơn vị.
- Chính sách trình duyệt tiếp tục chặn URL localhost và cấm lách bằng browser khác, HTTP/API, CDP hoặc thực thi gián tiếp. Vì vậy chưa retest app, chưa gọi Gemini. U09-05 (GRADE_VERSION_CONFLICT) và U09-19 (10.000/10.001 ký tự trên app đã build) vẫn Lỗi chờ retest; U15-05 vẫn Bỏ qua. Không ghi DB, publish điểm, gửi email, commit, push hoặc deploy.
