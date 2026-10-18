# U04 code đã triển khai và hướng dẫn review

Ngày18/10/2026 đã lần lượt triển khai domain/persistence/query/scope/services/audit/REST U04 và các màn/luồng frontend UC16–19 theo yêu cầu tiếp tục, kết thúc phần frontend consumer để bàn giao review/tích hợp. Chưa có real JWT/provider end-to-end. Không thay authentication/OTP của U01 hoặc lớp của U05.

**Mới nhất:** frontend124 test/24 file PASS, typecheck/lint/build PASS; đầy đủ list/create/detail/edit/manager/status/rejection/classes readonly ở cấp U04 consumer. [Tài liệu bàn giao](frontend-handoff.md) có files, rules, API và checklist nhận code. Thêm32 test F05/F06/F08; browser fixture QA hai phiên conflict/edit/unassign/archive/reopen/blocker. F02 RoleGuard, F04 account-search adapter/schema, shared session/CSRF U01 vẫn pending, default search unavailable không giả accounts. Trước commit, Maven verify mới nhất278 test PASS,31 PostgreSQL fixture cases,0 lỗi/skip và jar build PASS. Target42/full271 là bằng chứng checkpoint lịch sử. B10 durable/dedup, B03 schema, B08 account guard/Q14, B09 checker/writers U05 và NFR/tester giữ pending. Assignment changes/archive fail closed503 khi thiếu provider; reopen không cần classes provider. Không coi fixture/mock là whole-unit end-to-end.

## Backend đã kiểm tra

- `backend/src/main/java/edu/aiplatform/subjects/domain/SubjectCode.java`: value object chuẩn hóa trim/uppercase với Locale.ROOT, kiểm định dạng và tối đa20 ký tự (BR-U04-02/Q09).
- `backend/src/main/java/edu/aiplatform/subjects/domain/Subject.java`: snapshot bất biến, tạo ACTIVE/version1, sửa metadata, gán/bỏ manager, archive/reopen. Thay đổi thật tạo snapshot version+1; PUT no-op giữ snapshot. Service sau này phải requireVersion trước khi no-op; repository update phải kiểm version atomic. Cho sửa metadata/manager khi archived theo Q10.
- `backend/src/main/java/edu/aiplatform/subjects/domain/ISubjectRepository.java`: interface chỉ dùng nội bộ U04, đã có adapter JPA ở lượt thứ hai. update nhận expectedVersion để adapter B05 không ghi đè dữ liệu mới và không tăng version hai lần.

20 lượt test domain (10 SubjectCode, 10 Subject) và ba rule ArchUnit đã pass qua Maven. Kiểm mã sai/boundary, null/blank name, độ dài, chuẩn hóa không lệ thuộc locale server, immutable snapshot, version, no-op, đổi/bỏ manager và archive/reopen. Không gọi PostgreSQL hay provider thật; test trạng thái domain không chứng minh rule “mọi lớp đã archived”.

## Frontend đã kiểm tra

`frontend/src/features/subjects/types.ts`, `frontend/src/features/subjects/api.ts`, `frontend/src/features/subjects/validation.ts` cùng hai file test là contract web dự kiến cho B11. Client dùng apiFetch chung, không tự thêm authentication/refresh/CSRF hoặc gọi API U01 chưa bàn giao. Không sửa shared client: nó đã giữ extension JSON của Problem Detail ở runtime; kiểu lỗi U04 nằm trong feature.

Server U04 đã có ở B11, nhưng các API client/component test vẫn mock boundary, chưa phải bằng chứng liên thông provider thật. AVAILABLE với list rỗng và UNAVAILABLE với items=null được biểu diễn riêng. Controller đã validate phía server; whitelist ở client chỉ giúp tránh gửi nhầm field.

Lượt foundation pass49 test/14 file, gồm15 test U04 (10 validation,5 API). Lượt màn hình đọc mới nhất pass70/17 file, thêm21 component test; typecheck/lint/build pass. Các review giữ lỗi đã gặp và cách sửa; không đổi config Vitest hoặc tắt test để làm xanh kết quả. Browser desktop QA qua fixture riêng đã dừng sau kiểm; không tích hợp fixture vào production.

## Bằng chứng và cách chạy lại

- Backend lượt đầu: `mvn.cmd -f backend/pom.xml test` — pass114 test,0 failure/error/skipped, bao gồm20 test domain U04 và3 rule ArchUnit. Lượt thứ hai có thêm15 test PostgreSQL dùng env riêng, tổng129 pass/0 skip; cách chạy tại persistence-review.md. Nếu không đặt env PostgreSQL,15 test persistence bị skip có chủ đích, không được báo là đã kiểm DB. Báo cáo máy tại `backend/target/surefire-reports/` (ignored).
- Frontend: tại `frontend`, chạy `npm.cmd run test`, `npm.cmd run typecheck`, `npm.cmd run lint`. Test API dùng mock fetch; chưa có backend HTTP/U01/U05 thật.
- Đã cài dependencies theo package-lock hiện có, không thay package.json/lockfile/pom và không thêm thư viện. Build production đã pass ở lượt màn hình đọc; session/audit/concurrency liên unit với provider thật vẫn pending.

## Thứ tự đọc để tự nắm code

1. Đọc SubjectCodeTest rồi SubjectCode để thấy một rule trở thành validation thế nào.
2. Đọc SubjectTest rồi Subject; so snapshot trước/sau, version và no-op. Lưu ý archive ở đây chỉ đổi state sau khi service đã kiểm lớp, không tự kiểm U05.
3. Đọc ISubjectRepository rồi persistence-review.md; adapter đã triển khai transaction/optimistic locking, không để unit khác dùng repository này.
4. Đọc types.ts → validation.test.ts → validation.ts → api.test.ts → api.ts; đối chiếu bảy endpoint của kế hoạch.

## Ranh giới cần giữ cho bước tiếp theo

SubjectQueryService và SubjectScopeService là port nội bộ đọc thông tin/phân công, không tự xác thực account. Các service đọc/command/status đã có; B10 thêm ACCESS_DENIED bên cạnh năm success hooks; B11 có bảy HTTP endpoints. ISubjectClasses placeholder fail closed theo Q05; archive dùng checker cùng transaction, không lấy count async. Q14/account guard thật, U05 writers/checker, durable/dedup audit và real JWT/filter chain chưa tích hợp; không coi developer checks là U04 hoàn thành.

U01 kiểm Actor/current session, quyền Admin và manager ACTIVE/role. U05 cung cấp lớp và dùng guard lifecycle chung. Service U04 phải phối hợp các điều kiện này, ghi audit cùng transaction qua U02 và gọi repository có điều kiện. Domain đang viết không cấp quyền hoặc bỏ qua provider failure. Rule Q14 disable tự gỡ môn chưa được cài trong lượt này.
