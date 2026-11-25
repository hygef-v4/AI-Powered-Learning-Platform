# Câu hỏi thống nhất BR sau system test WF05 (2026-11-25)

Trả lời bằng cách ghi chữ cái (hoặc mô tả) sau thẻ `[Answer]:` của từng câu. Mỗi câu nêu hiện trạng đã kiểm trên systest
và trong mã để chọn dễ hơn. Sau khi có câu trả lời, BR (U02, U03, U13, U14), test case WF05 và code sẽ được sửa theo đó.

## Câu 1: Audit cho học liệu và thông báo

Hiện trạng:
- BR-U02-10 (yêu cầu 2026-11-25) không ghi audit cho học liệu (U13) và thông báo, kể cả khi bị từ chối. Code đã làm đúng như vậy.
- BR-U13-40 và BR-U14-40 vẫn ghi "ngoài phạm vi trả 404 và ghi audit `ACCESS_DENIED`". BR-U14-04/05 vẫn ghi audit khi sửa, xóa thông báo.
- Test case ST-WF05-008 vẫn kỳ vọng "the audit keeps the record".

A) Giữ BR-U02-10: bỏ phần audit trong BR-U13-40, BR-U14-04, BR-U14-05, BR-U14-40; test case chỉ kiểm xóa mềm (`deleted_at`, `deleted_by`)

B) Ghi lại audit khi bị từ chối (`ACCESS_DENIED`) cho U13, U14; thao tác thành công vẫn không ghi

C) Ghi lại audit đầy đủ cho U13, U14 như trước (tạo, sửa, xóa, từ chối); sửa BR-U02-10

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 2: Mã HTTP khi người dùng thấy lớp nhưng không có quyền thao tác

Hiện trạng:
- Sinh viên sv1 (đang học SE-ST1) gọi API tạo học liệu hoặc tạo thông báo cho SE-ST1 thì nhận `404`. Giảng viên t2 (không dạy lớp) cũng nhận `404`.
- BR-U13-40 và BR-U14-40 chỉ nói "ngoài phạm vi trả 404". Test case ST-WF05-005 kỳ vọng `403`.

A) Luôn `404` cho mọi trường hợp không có quyền (không tiết lộ lớp, tài liệu có tồn tại hay không); sửa test case 005, 006, 009 thành 404

B) `404` khi ngoài phạm vi (không học, không dạy lớp); `403` khi trong phạm vi nhưng sai vai trò (sinh viên tạo, sửa, xóa)

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 3: Lịch sử phiên bản học liệu (BR-U13-12)

Hiện trạng:
- BR-U13-12 ghi "Material Detail hiện lịch sử phiên bản". API vẫn trả `versions` (số phiên bản, tên tệp, dung lượng, người tạo, thời điểm, trạng thái trích chữ).
- Bảng "Lịch sử phiên bản" từng có trên trang học liệu, kèm nút tải từng phiên bản. Bảng này bị bỏ ở commit `22a5a400` (feat(ui): ver 2.12, 2026-10-29).
- Hiện trang không hiện số phiên bản. Đây là lý do ST-WF05-001 fail.

A) Khôi phục bảng lịch sử cho staff (Teacher, Subject Manager có quyền) gồm số phiên bản, thời điểm, người tạo, trạng thái trích chữ, và nút tải hoặc xem từng phiên bản; sinh viên chỉ thấy bản mới nhất, không thấy bảng

B) Như A nhưng chỉ hiện danh sách, không cho tải phiên bản cũ

C) Chỉ hiện nhãn "Phiên bản N" của bản hiện hành cho mọi người; bỏ yêu cầu hiện lịch sử khỏi BR-U13-12 (lịch sử chỉ giữ trong dữ liệu)

D) Bỏ hẳn yêu cầu hiện phiên bản trên giao diện; sửa BR-U13-12 và test case 001, 002

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: C

## Câu 4: Ngôn ngữ của thông báo lỗi từ server

Hiện trạng:
- Server trả `detail` tiếng Việt kèm `code`, ví dụ "Loại tệp không được phép" (`FILE_TYPE_NOT_ALLOWED`), "Không tìm thấy" (`ANNOUNCEMENT_NOT_FOUND`), "Chỉ nhận link của một video YouTube" (`MATERIAL_YOUTUBE_INVALID`).
- Giao diện tiếng Anh hiện nguyên văn câu tiếng Việt, ví dụ ở hộp thoại tải tệp hoặc trang thông báo của sv6. Đây là lý do ST-WF05-003 fail.
- Backend có khoảng 377 mã lỗi. Một số màn đã tự dịch theo mã hoặc theo HTTP status (ví dụ 404 của học liệu, lỗi phát hành quiz), số còn lại thì không.

A) Frontend dịch theo `code`: thêm bảng mã → câu (vi, en) dùng chung. Mã chưa có trong bảng thì hiện câu chung theo status (400, 403, 404, 409, 413, 415…) bằng ngôn ngữ giao diện, không hiện `detail` tiếng Việt khi giao diện là tiếng Anh. Làm trước cho mã của các luồng đã test (tệp, học liệu, thông báo, quiz, bài tập)

B) Backend trả `detail` theo ngôn ngữ người dùng (`Accept-Language` hoặc cookie `locale`) bằng bảng thông điệp vi/en cho toàn bộ mã lỗi

C) Chấp nhận `detail` tiếng Việt (người dùng chính là sinh viên Việt Nam); ghi rõ trong BR, test case không tính là lỗi

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 5: Ngôn ngữ email thông báo

Hiện trạng:
- Email gửi tiêu đề "Có thông báo mới của lớp" và nội dung tối thiểu kèm link (BR-U14-23), chỉ bằng tiếng Việt.
- Thông báo trên chuông đã được giao diện dịch theo loại. Hệ thống chưa lưu ngôn ngữ ưa thích của tài khoản (ngôn ngữ chỉ nằm ở cookie trình duyệt).

A) Giữ email tiếng Việt; ghi rõ vào BR-U14-23

B) Email song ngữ: một email có phần tiếng Việt rồi phần tiếng Anh

C) Lưu ngôn ngữ ưa thích vào tài khoản (cập nhật khi người dùng đổi ngôn ngữ) và gửi email theo ngôn ngữ đó

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu 6: Định dạng số và ngày giờ theo ngôn ngữ giao diện

Hiện trạng:
- Đợt sửa trước đã cho điểm quiz, token, dung lượng tệp, biểu đồ hiện số theo ngôn ngữ giao diện.
- Vẫn còn khoảng 13 chỗ cố định `vi-VN`: bộ đếm mô tả "0 / 2.000", điểm rubric, phân trang, dung lượng tệp học liệu, và toàn bộ ngày giờ ở `shared/dateTime.ts`, bảng tin thông báo, Log, dashboard.

A) Số theo ngôn ngữ giao diện ở mọi nơi (tiếng Anh "2,000", "3.5"); ngày giờ vẫn dạng Việt Nam dd/mm/yyyy, 24 giờ, múi giờ Asia/Ho_Chi_Minh cho cả hai ngôn ngữ

B) Cả số và ngày giờ theo ngôn ngữ giao diện (tiếng Anh dùng thứ tự và tên tháng tiếng Anh), múi giờ vẫn Asia/Ho_Chi_Minh

C) Giữ như hiện tại; chỉ sửa bộ đếm mô tả

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 7: Tệp XLSX do công cụ khác tạo

Hiện trạng:
- BR-U13-10 kiểm loại tệp theo nội dung.
- Một XLSX hợp lệ do thư viện kiểu SheetJS tạo (mở được bằng Excel) bị bộ nhận dạng xem là XLSB (macro-enabled binary) và bị từ chối `415`.
- XLSX do Excel tạo thì được nhận.

A) Nhận mọi tệp OOXML có `xl/workbook.xml` hợp lệ là XLSX (sửa bộ kiểm nội dung); vẫn từ chối XLSB, XLSM thật

B) Giữ như hiện tại; ghi vào BR là chỉ nhận XLSX được nhận dạng đúng, người dùng lưu lại bằng Excel nếu bị từ chối

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: B

## Câu 8: Tệp vượt giới hạn khi bỏ qua kiểm tra ở giao diện

Hiện trạng:
- Admin chỉnh giới hạn học liệu trong khoảng 1–50 MB (mặc định 50), và nginx chặn `/api/v1/files` ở 50 MB.
- Tệp 51 MB bị giao diện chặn trước ("The file exceeds 50 MB").
- Nếu gọi API trực tiếp, nginx trả trang HTML `413`, không phải JSON lỗi có `code` như các lỗi khác. Tệp dưới 50 MB nhưng vượt giới hạn Admin thì backend trả lỗi JSON.

A) Chấp nhận: giao diện đã chặn, `413` HTML của nginx chỉ gặp khi gọi API trực tiếp; ghi rõ trong BR-U03

B) Cho nginx trả JSON lỗi (`code` = `FILE_TOO_LARGE`) cho `413` để mọi client nhận cùng định dạng

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A

## Câu 9: Sửa câu chữ test case WF05

Đề xuất sửa (chưa làm), áp dụng sau khi chốt các câu trên:
- 001: tiền điều kiện thành "sv1 is enrolled in SE-ST1 and other classes"; kỳ vọng phiên bản theo Câu 3.
- 003: nêu giới hạn "above the size limit (Admin setting, default 50 MB), e.g. a 51 MB PDF".
- 005: kỳ vọng mã theo Câu 2.
- 006, 009: "Access is denied (HTTP 404, \"not found or no access\")".
- 007: "open the copied link within 5 minutes (link lifetime)".
- 008:
  - "every enrolled student except the poster gets one in-app notification; students with the announcement e-mail preference on get one e-mail";
  - bước kiểm audit theo Câu 1.

A) Đồng ý sửa toàn bộ như trên trong sheet Report-5.3 (giữ kết quả Round 1, ghi chú lý do sửa)

B) Chỉ sửa các case có kỳ vọng sai (005, 008); các case khác giữ nguyên

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A
