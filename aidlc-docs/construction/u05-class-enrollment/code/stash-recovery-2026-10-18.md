# U05 — kết quả ghép stash và UI mới

> Latest implementation: [U05 code completion and integration handoff](completion-2026-10-18.md). The earlier dependency-free-only scope and test counts below are historical; they do not describe the current completed application/API/frontend code.


Nguồn: stash `86fc91e092915f1b292019f782b99bbcfa55ee63` của nhánh Class&Enrollment. Người dùng cho phép xử lý sau khi xem nội dung stash; giữ phạm vi code độc lập khi U01/U04 chưa xong.

## Đã ghép

- Khôi phục 20 file backend: domain lớp/ghi danh, parser/policy, repository interface và JDBC adapter opt-in, khóa transaction theo môn, SQL pending và bốn file unit test.
- Giữ nguyên toàn bộ component, types, parser, CSS và preview UI mới. Không đưa lại component UI cũ trùng chức năng; chuyển bốn tình huống test còn thiếu sang `academics.test.tsx`: cập nhật version, kết quả lỗi theo dòng, file CSV quá lớn và danh sách toàn email sai.
- Đồng bộ regex backend với UI: chặn dấu chấm đầu/cuối hoặc liên tiếp trong phần trước @. Test backend mới đã thất bại với code cũ, rồi qua sau sửa; frontend có test cùng tình huống.
- Khôi phục kế hoạch/code summary/approval questions lịch sử; append các bản ghi audit còn thiếu thay vì ghi đè audit hiện tại. Summary cũ chỉ là bằng chứng lịch sử, tài liệu này mô tả kết quả mới nhất.

## Kiểm chứng mới

- Backend: chạy toàn bộ `mvn -o -q test`, exit 0; 123 test/29 suite, không failure/error/skipped; U05 29 test và architecture 3 test.
- Frontend: Node 24.19.0 chạy toàn bộ Vitest, 37 test/8 file qua, gồm 21 test U05. Typecheck và lint exit 0.
- Hash các file frontend trước/sau ghép giống nhau, ngoại trừ file test được bổ sung. Không chạy lại production build/browser vì không đổi code frontend thực thi; bằng chứng UI/build trước đó nằm trong `ui-summary-2026-10-18.md`.
- Không có conflict marker trong source U05, `git diff --check` qua. Git index không thay đổi, không commit/push; cả ba stash giữ nguyên hash.
- Bản sao file hiện tại, toàn bộ 33 blob trong stash và manifest SHA-256 ở `.tmp/u05-stash-recovery-2026-10-18/`. Đây là bản sao cục bộ được Git ignore; stash gốc vẫn được giữ.

## Phần chờ tích hợp

Chưa có API/service U05 thật, session/authorization từ U01, lookup môn từ U04 hoặc audit thật U02. SQL ở `db/pending/academics/` chưa được Flyway chạy; repository mặc định tắt. Test repository dùng mock, chưa xác minh PostgreSQL, concurrency hoặc p95. Preview chỉ lưu thay đổi trong bộ nhớ tab, tải lại sẽ reset.

Khi nối API, map DTO về `types.ts`; parser Java dùng `lineNumber`, UI dùng `line`. Hai phía là kiểu nội bộ, chưa có API contract chung. Không đổi UI về kiểu cũ chỉ để khớp stash.

Security/Resiliency subset giữ nguyên: validation/query tham số hóa và giới hạn CSV có test; auth/audit/integration vẫn chờ, không khẳng định runtime compliance. Không thêm external call/deployment; các rule ngoài subset N/A, PBT disabled.
