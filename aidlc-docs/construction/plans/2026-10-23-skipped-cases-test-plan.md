# Kế hoạch hoàn tất các ca kiểm thử đang Bỏ qua

**Mục tiêu:** bổ sung điều kiện thử và thực thi đủ 49 ca đang Bỏ qua trong artifact, ghi bằng chứng theo từng tiêu chí; lỗi được phát hiện phải chuyển Lỗi thay vì tiếp tục Bỏ qua.

**Nguồn:** https://claude.ai/artifact/Wbj4CKoyBcDE6dPyCHbour — baseline đọc trực tiếp ngày 2026-10-23: 354 ca, 62 Đạt, 3 Lỗi, 49 Bỏ qua, 240 Chưa test. Đây là baseline mới hơn các ghi chú 57 ca/26 bỏ qua trong plan sửa lỗi trước. Không đưa 240 ca Chưa test vào phạm vi này.

**Phương pháp:** một agent thực hiện trực tiếp, UI trong Codex in-app browser. Ưu tiên tạo dữ liệu qua UI để đi qua validation, version, audit và sự kiện thật; SQL chỉ tạo tiền điều kiện khó chuẩn bị qua UI. Seed thành công không phải bằng chứng hành vi ứng dụng đã đạt.

**Công nghệ:** Next.js/React, Spring Boot, PostgreSQL, worker/job/broker và dịch vụ lưu tệp/AI hiện có. Tiếp nối AI-DLC Build & Test, không thiết kế lại hệ thống. Chưa thực hiện seed, sửa code hay chạy 49 ca trong lượt lập plan này.

## Tiến độ và đầu ra

- [x] Đọc CLAUDE.md, trạng thái AI-DLC, plan sửa lỗi cũ và danh sách kết quả trực tiếp trong artifact.
- [x] Kiểm kê 49 ca, 5 unit và các điểm chặn; đối chiếu migrations cùng scanner nhắc hạn hiện tại.
- [x] Lập mapping đủ 49 ca sang dữ liệu, cách chạy và kết quả cần quan sát; tự rà soát số lượng.
- [x] P0: kiểm tra môi trường, chụp baseline, chuẩn bị manifest dữ liệu. Backend, worker và PostgreSQL local khỏe; Nginx đang restart vì không phân giải upstream `frontend`, nhưng UI test trực tiếp tại `127.0.0.1:3000` hoạt động. Đã đọc migration U13, đối chiếu DB và tạo manifest fixture.
- [ ] P1: tạo dữ liệu tài khoản/lớp/nhóm/bài nộp; mở được workspace chấm thật. U09 đã có submission/evaluation QA qua UI và người dùng xác nhận Teacher đang đăng nhập; còn thiếu nhóm, lượt nộp trễ/đa lượt và nhiều sinh viên.
- [ ] P2: U09 đã kiểm một phần: U09-01/08/20/24 Đạt; U09-05 và U09-19 Lỗi; 20 ca còn Bỏ qua. Chưa chốt/công bố điểm do `GRADE_VERSION_CONFLICT`; xuất sổ điểm đã thành công trước đó nhưng nội dung/audit chưa xác minh đủ.
- [ ] P3: phân bố điểm và nhánh AI U09 chưa hoàn tất; thiếu sinh viên/điểm công bố/provider thật.
- [ ] P4: tiếp tục 11 ca U13 còn Bỏ qua sau khi gỡ điểm chặn upload; chưa có fixture CLASS để xác minh nhãn “của lớp”. Đăng nhập Teacher từng thất bại ở một lượt trước; ảnh người dùng sau đó xác nhận phiên Teacher đã hoạt động.
- [ ] P5: thực hiện 4 ca U14 và 1 ca U15.
- [ ] P6: kiểm tra hồi quy liên quan, cập nhật artifact, bàn giao kết quả và dữ liệu thử.

Các file khi thực thi:

- Plan này: checklist tổng và checklist ca.
- `aidlc-docs/construction/build-and-test/2026-10-23-skipped-cases-results.md`: kết quả từng tiêu chí, thời gian, vai trò, fixture, đường dẫn bằng chứng, lỗi và lần chạy lại.
- `.tmp/skipped-cases/`: fixture manifest, script seed SQL nếu cần, log đã lọc, ảnh và file tải về; kiểm tra ignore trước khi tạo. Không đưa khóa/password vào tài liệu hay commit.
- `aidlc-docs/aidlc-state.md`, `aidlc-docs/audit.md`: cập nhật tiến độ cùng lượt thực hiện, audit append-only.
- Source chỉ sửa khi tái hiện được lỗi và đã xác định nguyên nhân; đọc hướng dẫn tại thư mục tương ứng trước khi sửa. Không reset các thay đổi đang có trong working tree.

## Trạng thái thực thi — 2026-10-23

- Baseline khi lập plan: 354 ca, 62 Đạt, 3 Lỗi, 49 Bỏ qua, 240 Chưa test. Snapshot artifact đọc trực tiếp ngày 2026-10-23: 354 ca, 65 Đạt, 2 Lỗi, 46 Bỏ qua, 241 Chưa test; riêng U13 có 11 Đạt/11 Bỏ qua. Không suy ra chênh lệch từng trạng thái từ ghi chú cũ vì một số ghi chú trong artifact chưa đồng bộ với verdict hiện tại. Hai lỗi còn lại không thuộc danh sách 49 ca bỏ qua.
- [x] Tạo fixture lớp `QA-SKIP-20261023A` và bốn học liệu PDF qua UI; ghi ID, hash, trạng thái trích chữ và giới hạn của fixture trong `.tmp/skipped-cases/manifest.json`. Không INSERT trực tiếp vào DB.
- [x] U13-02: upload PDF hợp lệ, thấy tiến trình/nút Hủy/trạng thái hoàn tất và lưu phiên bản 1; lỗi `CSRF_INVALID` không tái hiện sau khi gửi CSRF header.
- [x] U13-04: worker đã trích 544 ký tự nhưng lần đầu danh sách UI không đổi tới khi tải lại. Đã sửa và kiểm thử lại trên UI: đổi “Đang xử lý” thành “Đã trích” khi vẫn ở trang.
- [ ] Snapshot cũ của đợt thực thi ghi 46 ca Bỏ qua. Sau U09 continuation (2026-10-23T16:52:53Z), U09 còn 20 Bỏ qua và có 2 Lỗi; toàn artifact suy ra 42 Bỏ qua, 4 Lỗi, 67 Đạt, 241 Chưa test từ lần đọc toàn cục gần nhất. Còn U05-07/09/12/21/22/23, 20 ca U09, 11 ca U13, 4 ca U14 và U15-05. Teacher đã xác nhận đang đăng nhập; các lần BAD_PASSWORD cũ không còn là blocker hiện tại.
- Student U05-A đã thấy bốn học liệu theo thứ tự, có loại PDF và dung lượng 1 KB; truy vấn chỉ đọc xác nhận cả bốn thuộc scope SUBJECT, nên chưa kiểm được nhãn học liệu CLASS. Mở chi tiết PDF có chữ được, nhưng iframe xem trước trắng trong in-app browser và nội dung download chưa đối chiếu bytes.
- Kết quả và nguyên nhân chi tiết: `aidlc-docs/construction/build-and-test/2026-10-23-skipped-cases-results.md`.

### Cập nhật U09 — 2026-10-23T16:52:53Z

- Artifact đã nhận ghi chú U09 về submission/evaluation, kiểm tra hai tab, rubric, gradebook, tiến độ lớp, trạng thái bản nháp phía Student, giới hạn nhận xét và các fixture còn thiếu. Verdict hiển thị: 4 Đạt, 2 Lỗi, 20 Bỏ qua.
- Đã sửa phản hồi validation cho nhận xét >10.000 ký tự và regression test đạt 1/1. Backend local đã rebuild và healthy; chưa xác minh lại trên UI nên U09-19 vẫn Lỗi đến khi hoàn tất retest app.
- U09-05 `GRADE_VERSION_CONFLICT` còn mở nên không công bố điểm. P1/P2/P3/P6 tiếp tục; không coi toàn bộ U09 hay kế hoạch 49 ca đã hoàn tất.

## P0 — baseline và nguyên tắc tạo dữ liệu

1. Đọc lại artifact khi bắt đầu để nhận các kết quả người khác vừa cập nhật; snapshot danh sách ID và lý do Bỏ qua. Đếm lại trước mỗi đợt, tránh ghi đè kết quả kiểm thử mới.
2. Kiểm tra image backend/frontend/worker có đúng source hiện tại, health, migration và provider thật đang hoạt động. Dùng `infra/docker-compose.yml`, `infra/docker-compose.local.yml` làm điểm tra cứu; không in biến môi trường chứa secret. Chỉ rebuild dịch vụ có source sửa và xác nhận đang chạy image cũ.
3. Xác minh đích database thuộc stack local đang kiểm thử; đọc schema đang chạy và migrations trước mọi INSERT. Lưu backup/snapshot cục bộ trước seed; dùng transaction, UUID riêng và tiền tố `QA-SKIP-<run>`; lưu ID cùng giá trị trước/sau trong manifest. Seed có thể chạy lại mà không nhân bản dữ liệu.
4. UI là lựa chọn mặc định. SQL dành cho bulk 20+ sinh viên, trạng thái tiền điều kiện, thời điểm nộp quá khứ, lịch sử backup. Phải giữ FK, unique, status/version và liên kết attempt/evaluation đúng schema. Không xóa constraint hay tắt authorization/CSRF để chạy được test.
5. Không INSERT sẵn notification, audit xuất, proposal AI thành công hay bản ghi reminder để dùng làm bằng chứng hệ thống đã sinh chúng. Không sửa version để che lỗi concurrency. SQL mô phỏng lịch sử chỉ chứng minh màn đọc; phải ghi rõ giới hạn.
6. Khi thao tác phá trạng thái (gỡ sinh viên, lưu trữ, đổi deadline, nộp lượt 2), dùng bản fixture riêng. Sau seed đối chiếu SELECT số lượng và UI đọc lại, sau thao tác đối chiếu cả UI lẫn persistence khi cần.
7. Tài khoản ở nhiều tab cùng origin có thể dùng chung cookie. Dùng session/profile tách biệt nếu công cụ hỗ trợ; nếu không, chạy tuần tự, kiểm tra vai trò/tên sau mỗi đăng nhập. Test stale version dùng hai tab cùng phiên vẫn hợp lệ.

## P1 — bộ fixture dùng chung

| Bộ | Nội dung phải có | Cách tạo / kiểm tra |
|---|---|---|
| F1 tài khoản | Admin hiện có; 2 Manager quản lý 2 môn khác nhau; 2 Teacher; ít nhất 20 Student có điểm công bố; Student ACTIVE chưa ghi danh, DISABLED, PENDING riêng; Student tên `=1+1` | Tái sử dụng tài khoản QA phù hợp trước; UI tạo bổ sung, bulk SQL chỉ sau kiểm schema và cơ chế đăng nhập hiện có. Không thay credential của tài khoản người dùng. |
| F2 lớp | Cùng môn: A/B với Teacher khác nhau; DRAFT chưa có Teacher; OPEN; ARCHIVED; môn thứ hai do Manager B quản lý; lớp bật và tắt phân bố | UI tạo/gán/mở; manifest ghi student thuộc lớp nào để không vi phạm một lớp đang học cùng môn. Ca mở lại: ghi danh S vào A trước, lưu trữ A, ghi danh S vào B OPEN rồi mở lại A. |
| F3 bài và nhóm | Graded cá nhân tối đa >=2 lượt, thang 10 có rubric; bài nhóm với >=2 nhóm và nhiều thành viên; Practice và Quiz làm đối chứng; bài có/tắt hoạt động | Tạo/phát hành qua UI, giữ snapshot rubric/nội dung đúng. Hai lớp cùng môn dùng bài môn để kiểm cô lập. |
| F4 bài nộp/điểm | Chưa nộp, đang làm, nộp đúng hạn, trễ xác định phút; pending, nháp, chốt, công bố; A/B có lượt 1 và 2 | Ít nhất một attempt thật qua UI; bulk các tiền điều kiện còn lại qua SQL có quan hệ hợp lệ nếu UI quá tốn. Chốt/công bố cần chạy thật để kiểm audit/event. |
| F5 phân bố | 19 rồi 20 sinh viên có điểm công bố, không chỉ ghi danh; một khoảng điểm <5 người, một khoảng >=5 | Kiểm ngưỡng 19 trước, sau đó công bố người thứ 20 qua UI; phân bố không lộ ID/điểm cá nhân. Giữ lớp tắt cờ làm đối chứng. |
| F6 tệp | PDF 2 trang có chữ và bản thay thế, PDF ảnh không có text layer, DOCX, PPTX, PNG đổi đuôi .pdf | Tạo dữ liệu giả, ghi hash/kích thước/trang/text mong đợi; PDF phải upload qua UI thật. Không seed metadata để bỏ qua upload. |
| F7 AI | Submission thật, nguồn AI được cấu hình hợp lệ, nguồn lỗi và hạn mức thử có kiểm soát | Kiểm trang `/ai/connection`, quyền dùng nguồn và quota trước; không đọc/in khóa. Nguồn giả chỉ dùng test tích hợp, không tính pass ca nhà cung cấp thật. |
| F8 backup | Đủ >1 trang lịch sử; SUCCESS, FAILED có lỗi an toàn, bản SUCCESS đã purged >7 ngày, start/end/size | Dùng job local thật nếu sẵn; SQL seed vào `backup_runs` chỉ để kiểm bảng/phân trang/trạng thái. Không gọi đây là kiểm chứng khả năng backup/restore. |

## Các điểm chặn cần xử lý trước ca phụ thuộc

- **U13-02 (đang Lỗi):** tái hiện upload PDF hợp lệ trên session mới; kiểm CSRF/cookie, request upload/finish, API route/proxy, phản hồi và log server để phân biệt lỗi phiên với storage/provider. Sửa nguyên nhân, thêm regression phù hợp rồi upload/xem/tải thật. Không bỏ CSRF hoặc seed blob để đánh dấu các ca upload đạt.
- **Chốt điểm `GRADE_VERSION_CONFLICT`:** ghi version GET, version request và response cho một attempt thật; kiểm snapshot rubric/attempt/evaluation và thao tác hai tab. Tái hiện bằng UI trước, sửa nếu là lỗi ứng dụng; không ghi DB sang FINALIZED/PUBLISHED để né lỗi. Đây là gate cho U09-05/06/12/17/21/22 và U14-12/13.
- **U04-09 và U14-02 (hai Lỗi còn lại trong artifact):** ghi riêng, không tự chuyển Đạt. U14-02 cần đối chiếu yêu cầu sanitize-vs-reject HTML với tài liệu/contract trước khi thay đổi hành vi chỉ để khớp checklist. Không chặn những ca Bỏ qua không phụ thuộc chúng.

## Ma trận chạy 49 ca

Checkbox chỉ đánh dấu khi đã có verdict và bằng chứng, không phải khi chỉ tạo xong fixture. Thứ tự trong nhóm có thể đổi theo trạng thái fixture.

### U05 — 7 ca

| Xong | Ca | Chuẩn bị và thao tác quyết định | Bằng chứng cần có |
|---|---|---|---|
| [x] | U05-06 | F2: mở DRAFT chưa Teacher rồi gán Teacher, mở lớp; đăng nhập Teacher | Đạt: chặn có lý do trước gán; sau mở lớp xuất hiện ở Teacher “Lớp được phân công”. |
| [ ] | U05-07 | F1/F2: active mới, disabled, pending, sai vai trò, đang lớp khác, email sai/trùng; danh sách 200/201 | Mọi nhánh kết quả dòng đúng; pending được thêm; biên 200/201 đúng, không chỉ nhìn thông báo tổng. |
| [ ] | U05-09 | F2: S ở A, archive A, thêm S vào B OPEN, mở lại A | Xác nhận lưu trữ; mở lại chặn và nêu tên S thay UUID. |
| [ ] | U05-12 | F3–F5: lớp có bài mở, pending thật và điểm công bố | Đủ tab/phần/biểu đồ; đếm đúng và không có quyền quản lý roster của Manager. |
| [ ] | U05-21 | F1 active chưa học môn: thêm vào OPEN và DRAFT riêng, rồi mở DRAFT | Chuông/inbox trước/sau theo event ID/thời gian; không gửi lúc DRAFT, mở lớp gửi đủ người đang ghi danh, không trùng. |
| [ ] | U05-22 | F4 đã có bài nộp và điểm; gỡ khi Student đang xem, thêm lại | Direct link bị từ chối; lớp biến mất; thêm lại vẫn thấy cùng submission/điểm. |
| [ ] | U05-23 | F1/F2 Manager B ở môn khác | Danh sách chỉ đúng môn B; direct link lớp môn A không tìm thấy và không lộ dữ liệu. |

### U09 — 24 ca

| Xong | Ca | Chuẩn bị và thao tác quyết định | Bằng chứng cần có |
|---|---|---|---|
| [ ] | U09-02 | F4 nộp trễ và đúng hạn, chọn nhiều | Lọc chỉ trễ; số phút đúng theo timestamp; số bài chọn và action hàng loạt. |
| [ ] | U09-03 | F3/F4: tick rubric, điều chỉnh +1 thiếu/có lý do, lưu nháp | Tổng tự tính, validate lý do, 2 chữ số thập phân; trạng thái và chuyển bài kế. |
| [ ] | U09-04 | F4 hai lượt cùng student/bài | Mới nhất chấm được; lượt cũ/bài làm chỉ đọc. |
| [ ] | U09-05 | Chốt/công bố thật sau khi gate version qua | Student thấy rubric/điểm/nhận xét; thông báo có điểm mới nhưng không chứa số điểm. |
| [ ] | U09-06 | Sửa điểm đã công bố thiếu/có lý do | Chặn thiếu lý do; history trước/sau; nhãn và thời gian cập nhật phía Student. |
| [ ] | U09-07 | F3 hoạt động cá nhân/nhóm; tạo rời/quay tab thật | Timeline từng thành viên, thời lượng đối chiếu; disclaimer không kết luận gian lận; đối chứng bài tắt ghi nhận. |
| [ ] | U09-08 | Hai tab cùng evaluation, lưu tab1 rồi tab2 | Tab2 conflict/refetch, không ghi đè điểm tab1. |
| [ ] | U09-09 | F3 bài nhóm có tác giả mục | Danh sách nhóm, mục/tác giả/điểm; mặc định điểm nhóm; chỉnh riêng thành viên đúng. |
| [ ] | U09-10 | F7 proposal thật, sau đó Student nộp lượt mới | Version/điểm/tóm tắt; Dùng chỉ điền không lưu; proposal cũ bị vô hiệu. |
| [ ] | U09-11 | F7 nhiều pending và nguồn có quota thử | Không chọn sẵn nguồn, token estimate/kết quả từng bài; hết quota không tự đổi nguồn. |
| [ ] | U09-12 | Trộn finalized với chưa finalized | Chỉ công bố bài đã chốt; kết quả từng bài ghi lý do bỏ qua. |
| [ ] | U09-13 | F4 đủ 6 trạng thái, F3 có Practice/Quiz | Từng ô đúng trạng thái; chỉ cột Graded, không tổng có trọng số. |
| [ ] | U09-14 | Export XLSX toàn bộ và CSV một bài sau có điểm | Mở file kiểm header/row/phạm vi/định dạng, audit event xuất thật; đóng dialog chưa đủ. |
| [ ] | U09-15 | F4 pending thật, F5 công bố | Tính tay số mở/chờ chấm, tử/mẫu và phần trăm từng bài; biểu đồ đối chiếu điểm fixture. |
| [ ] | U09-16 | F4 có đúng hạn/trễ và điểm công bố | Tổng x/y Graded, nộp trễ n; đủ Bài/Hình thức/Nộp/Điểm, giá trị đúng. |
| [ ] | U09-17 | F5: 19 rồi 20 điểm công bố, lớp bật/tắt, điểm chưa công bố | Nút/biểu đồ đúng ngưỡng; ẩn khoảng <5; không rò điểm người khác/điểm chưa công bố. |
| [ ] | U09-18 | Thang10: rubric9/+3, rubric1/-3 có lý do, một bài trễ | Clamp10/0, trễ không tự trừ. |
| [ ] | U09-19 | Nhận xét 10.000 rồi 10.001 ký tự | Lưu giới hạn hợp lệ, chặn vượt giới hạn có báo. |
| [ ] | U09-20 | Một bài nháp, một bài chốt chưa công bố | Hai Student không thấy điểm/nhận xét qua UI, kiểm response không lộ khi cần. |
| [ ] | U09-21 | A nháp lượt1; B công bố lượt1; cả hai nộp lượt2 | A pending lượt2; B giữ điểm công bố lượt1 cho tới công bố lượt2; sổ điểm theo lượt mới. |
| [ ] | U09-22 | Trộn nháp đã chấm với pending thật | Chốt riêng bài hợp lệ, báo lỗi dòng pending, không rollback cả nhóm. |
| [ ] | U09-23 | F7 nguồn thử lỗi, giữ nguyên submission | AI báo lỗi, không điểm giả; sau đó chấm tay lưu được. Không thu hồi khóa người dùng đang dùng. |
| [ ] | U09-25 | Hai lớp cùng môn, Teacher A/B, cùng bài môn | A không thấy bài nộp B, công bố A không công bố điểm B. |
| [ ] | U09-26 | F1 tên literal `=1+1`, export CSV/XLSX | Tên hiển thị literal, không thực thi công thức; tên file không chứa tên Student. Dùng Excel thật nếu có; inspect cell/formula hỗ trợ, không thay bằng kết luận UI chưa quan sát. |

### Tiếp tục U09 — 2026-10-23

- [x] Đọc lại artifact và xác nhận U09 có 26 ca: 2 Đạt, 24 Bỏ qua, 0 Lỗi; không đổi verdict vì chưa có bằng chứng UI mới.
- [x] Kiểm tra DB local ở chế độ chỉ đọc: bài `Kiểm thử U09 - bài nộp mẫu` tồn tại ở U05-A, trạng thái OPEN, tối đa 2 lượt; có 0 submission và 0 evaluation. Không tạo bài trùng.
- [x] Chạy 4 file test frontend cho assignments/grading: 26/26 đạt; chạy full frontend suite: 79 file, 450/450 đạt.
- [ ] U09 live UI còn chờ phiên Teacher/Student hợp lệ. Audit đăng nhập ghi `BAD_PASSWORD`; tài khoản vẫn ACTIVE và chưa khóa. Chưa tạo submission/evaluation vì không thể xác minh luồng UI sau seed khi chưa có phiên vai trò.
- Artifact: ghi chú U09-02 đã lưu được; ghi chú U09-03 chưa đồng bộ sau khi đổi unit. Không tuyên bố đã cập nhật note U09-03.

### U13 — 13 ca, chạy sau upload gate

| Xong | Ca | Chuẩn bị và thao tác quyết định | Bằng chứng cần có |
|---|---|---|---|
| [ ] | U13-01 | F6 tệp/video, reorder rồi reload; trích chữ nhiều trạng thái | Thứ tự persist, loại/dung lượng, trạng thái Đang xử lý/Đã trích/Không có chữ/Lỗi; video YouTube/—. |
| [x] | U13-04 | Upload PDF có chữ, giữ trang mở | Đạt sau sửa: tự chuyển Đang xử lý sang Đã trích, không reload. Lần đầu retest đã ghi nhận Lỗi; xem result ledger. |
| [ ] | U13-05 | PDF2trang, sửa metadata hai tab | Xem/chuyển trang, conflict tab sau; video đối chứng. |
| [x] | U13-06 | Thay PDF thật bằng bản khác | Đạt: phiên bản 2 nhãn Hiện hành; phiên bản 1 giữ trong lịch sử với EXTRACTED/544 ký tự. |
| [ ] | U13-09 | Student xem danh sách tệp/video của lớp | Đã thấy thứ tự, loại PDF và dung lượng; bốn fixture hiện tại đều scope SUBJECT nên còn thiếu dữ liệu CLASS để kiểm nhãn “của lớp”. |
| [ ] | U13-10 | Student mở PDF/video, tải PDF | Chi tiết PDF hiển thị; khung iframe trắng, chưa xác minh xem/chuyển trang hay bytes tải xuống. Video/embed/highlight đã có bằng chứng trước đó. |
| [ ] | U13-12 | F6 PNG đổi đuôi .pdf | Server từ chối do nội dung, không chỉ lỗi session/đuôi. |
| [ ] | U13-14 | Đổi metadata/thứ tự rồi thay tệp; Student mở lại | Metadata không tạo content-version; thay tệp có; Student nhận bản mới. |
| [ ] | U13-15 | PDF ảnh không text-layer | Không có chữ, vẫn xem và tải được. |
| [ ] | U13-16 | Teacher quản lý file lớp và thử file môn | Đổi tên/thay tệp/archive lớp được; file môn chỉ xem. |
| [ ] | U13-18 | DOCX/PPTX hợp lệ | Không preview; tải đúng bytes/định dạng qua Student. |
| [ ] | U13-20 | File đã trích gắn Quiz/tham khảo AI, sau đó archive | Không chọn được tại cả picker Quiz lẫn AI; Quiz cũ không hiện học liệu. |
| [ ] | U13-22 | Lấy URL tải thật, tài khoản khác và cùng Student sau6phút | URL không phải Drive; khác tài khoản và hết hạn đều bị từ chối; không ghi signed URL nguyên vẹn vào báo cáo chia sẻ. |

### U14 — 4 ca

| Xong | Ca | Chuẩn bị và thao tác quyết định | Bằng chứng cần có |
|---|---|---|---|
| [ ] | U14-08 | Cắt kết nối chỉ session kiểm thử 30giây; Teacher đăng thông báo trong khoảng đó | Sau nối lại số chưa đọc tự đồng bộ đúng. Ưu tiên khả năng offline theo session của browser; nếu thiếu dùng proxy local dành riêng session để cắt traffic, không tắt mạng toàn máy. Nếu không thực hiện được chỉ ghi integration fallback, chưa đánh pass E2E. |
| [ ] | U14-12 | F3/F4 có điểm, nhóm và đề nghị đổi trưởng; chạy từng event | Mở bài tới mọi student; công bố tới student có điểm; đăng lớp không gửi Teacher đăng; đổi trưởng tới Teacher; từ chối tới người gửi; đúng một thông báo/người/event. |
| [ ] | U14-13 | Công bố điểm thật rồi gỡ recipient | Chuông không chứa điểm; direct link sau gỡ không tìm thấy. |
| [ ] | U14-15 | A ngưng trước mốc; B đã nhắc, dời deadline +2ngày | A không nhắc; B thêm đúng một reminder theo deadline mới, không trùng khi scanner chạy lại. |

U14-15 có giới hạn thời gian thật: scanner hiện nhận `scan(Instant now)` và cửa sổ nhắc từ deadline-24h tới +5phút; loại bài mở sau mốc nhắc. Dùng test tích hợp với Clock/Instant và DB/broker riêng để chạy đầy đủ hai mốc nhanh; **không đổi đồng hồ hệ điều hành** và không coi bản ghi seed là nhắc đã gửi. E2E của đúng tình huống kéo hạn +2 ngày cần giữ fixture và quan sát tới mốc thật; ghi trạng thái chờ và thời điểm cụ thể nếu chưa tới. Không tự tạo automation khi người dùng chưa yêu cầu theo dõi sau phiên.

### U15 — 1 ca

| Xong | Ca | Chuẩn bị và thao tác quyết định | Bằng chứng cần có |
|---|---|---|---|
| [ ] | U15-05 | F8 có >1 trang, success/failed/purged | Cột ngày/bắt đầu/kết thúc/size/kết quả/lỗi an toàn, trạng thái Đã xóa quá7ngày; next/prev và đóng dialog. Nếu dùng SQL lịch sử: ghi rõ chỉ kiểm reader/UI, không khẳng định backup thực thi thành công. |

## Kết luận mỗi ca và quy tắc đóng đợt

1. Chạy đầy đủ các bước và phần Mong đợi trong artifact gốc, bảng trên là phần dữ liệu và trọng tâm bổ sung, không thay thế tiêu chí gốc.
2. Ghi ID, vai trò, run/fixture ID, thời gian, bước, thực tế/kỳ vọng, ảnh UI và log/file/audit liên quan đã lọc. Kiểm UI tải lại, không chỉ toast hoặc dialog đóng.
3. Đủ mọi tiêu chí: Đạt. Hành vi sai: Lỗi kèm bước tái hiện và nguyên nhân nếu biết. Chưa có provider thật/công cụ/time window: giữ Bỏ qua với blocker cụ thể và điều kiện gỡ; không dùng Bỏ qua để xóa dấu lỗi đã biết mà chưa retest được.
4. Với lỗi cần sửa: tái hiện nhỏ nhất, test regression phù hợp, sửa tối thiểu, chạy lại ca lỗi và các ca phụ thuộc. Chỉ broaden suite khi source thay đổi/ảnh hưởng cần thiết; dữ liệu seed không yêu cầu chạy lại mọi unit test.
5. Ghi kết quả ngay sau từng ca vào artifact và local result ledger; kiểm lại status/note đã lưu. Nếu thao tác note trên artifact, dùng nhập bàn phím và blur rồi đọc lại sau đổi unit; `fill` đơn thuần từng không kích hoạt handler change.
6. Báo tổng trước/sau, ID đạt/lỗi/bỏ qua còn lại, phân biệt dữ liệu giả và flow thật. 240 ca Chưa test cùng các unit ngoài phạm vi không tự động được chấp nhận.
7. Giữ fixture có manifest để người dùng xem lại; chỉ cleanup dữ liệu đúng run khi được yêu cầu, không bulk-delete dữ liệu QA cũ hoặc reset DB. Không commit/push/deploy trong kế hoạch kiểm thử này.

## Rà soát rủi ro và tuân thủ

- Cô lập tenant/lớp, truy cập sau gỡ và signed download thuộc phần security cần kiểm thật; không làm yếu chính sách để unblock test.
- Concurrency dùng version lấy mới từ server; stale version là nhánh test, không phải lý do sửa DB ép thành công.
- Snapshot/restore và transaction seed bảo vệ dữ liệu local; provider/worker health kiểm trước để tách lỗi môi trường khỏi lỗi ứng dụng.
- Security Baseline giữ phạm vi đã duyệt trong aidlc-state; kiểm auth/input/audit/secret ở các ca liên quan. Resiliency áp dụng health/dependency isolation và cách thay đổi local có thể khôi phục; các rule ngoài phạm vi đồ án N/A. PBT disabled, không thêm yêu cầu opt-in hay mở rộng triển khai production.
- Provider AI/storage, Excel và thời gian reminder là điều kiện cần xác minh lúc thực thi, không khẳng định hiện đã sẵn sàng. Dữ liệu giả giải quyết thiếu fixture, không thay thế được provider thật hoặc quan sát thời gian thật.

**Tự kiểm tra:** 7 + 24 + 13 + 4 + 1 = 49 ID riêng biệt. Kế hoạch đã sẵn sàng; toàn bộ checkbox thực thi còn mở.

### U09 tiếp tục — sửa AI picker và hoàn thiện estimate U09-11 — 2026-10-23T18:23:22Z

- [x] Đọc lại Claude artifact, cập nhật bằng chứng các note U09-02/03/05/08/10/11/13/18/19/20/22/23 và xác minh U09-11 vẫn còn sau khi đổi unit; giữ verdict 4 Đạt, 2 Lỗi, 20 Bỏ qua.
- [x] Sửa stale source selection trong popup U09 AI bằng mount có điều kiện; thêm test đỏ trước/sáng sau.
- [x] Bổ sung endpoint ước tính token read-only cho AI batch, dùng đúng prompt của từng bài và cùng công thức quota; tổng hợp ID evaluation duy nhất, không gọi Gemini hay giữ quota. Cập nhật frontend và OpenAPI.
- [x] Chạy backend U09/validation regression: 21/21; frontend full suite: 79 file, 452/452; typecheck đạt.
- [ ] Chưa gọi Gemini thật vì tool in-app chặn URL local theo chính sách. Không dùng API/HTTP/CDP hay browser khác để vượt chặn; U09-10/11 vẫn cần quan sát trực tiếp trên app.
- [ ] Retest U09-05 chốt/công bố sau khi ghi lại version GET/PUT thực tế; chưa có reproduction trong 21 test backend.
- [ ] Retest U09-19 trên runtime đã build mới; UI trước đây hiển thị lỗi validation chung dù backend regression nay trả `FEEDBACK_TOO_LONG`.
- [ ] 20 ca chưa được đánh Đạt vẫn cần tiếp tục với fixtures/trạng thái phù hợp; không biến kết quả unit/integration thành pass hộp đen.
