# Kết quả sửa 4 lỗi — 13/11/2026

Đã sửa BUG-01 đến BUG-04 của báo cáo ngày 12/11 trong workspace hiện tại (HEAD khi tiếp tục: `af245ac3`). Đây là kết quả kiểm thử cục bộ, chưa triển khai.

| Lỗi | Hành vi sau sửa |
|---|---|
| BUG-01 | Chỉ thoát quiz khi lưu thành công. Nếu lưu lỗi, giữ màn hình, đáp án và cho thử lại. |
| BUG-02 | Từ chối quota dùng exception riêng không làm transaction rollback-only, cho phép chuyển sang khóa cá nhân. Lỗi DB không bị nuốt như lỗi quota. |
| BUG-03 | Lượt đang làm, số lượt và giới hạn được tách theo lớp. Giữ bài/đáp án/lớp sở hữu cũ; lớp học lại tạo lượt mới. Số lượt cũ không bị đánh lại. |
| BUG-04 | Timeout job tính cả retry, backoff và các kết nối có thể thử. Scanner dùng cửa sổ retry tương ứng; interrupt dừng thử tiếp. Lỗi DB khi đọc ngân sách thời gian được phép retry. |

## Quy tắc người dùng đã chốt

Không đổi giữa các lớp đang học cùng môn; cho học lại khi lớp cũ đã lưu trữ. Giữ bài cũ tại lớp cũ, cho làm lượt mới tại lớp mới. Điều kiện ghi danh hiện có (`ACTIVE`, lớp khác cùng môn chưa `ARCHIVED`) được giữ nguyên.

Migration `V20261113_1000__scope_quiz_attempts_to_class.sql` thêm `class_id` vào unique constraint số lượt và partial unique index lượt đang làm. Không UPDATE/DELETE dữ liệu cũ. Mỗi lớp vẫn chỉ có một lượt đang làm cho cùng sinh viên/quiz. Cần áp dụng migration cùng phiên bản backend khi triển khai; chưa chạy trên DB thật.

## Kiểm thử

- Backend cuối: **88 tests, 0 failures, 0 errors, 0 skipped**, `BUILD SUCCESS`. Chạy trên bản sao riêng trong container Maven/Java 17; PostgreSQL 16 Testcontainers. Bao gồm `AiTaskFlowTest`, `JobDispatcherTest`, `QuizPracticeServicesTest`, `JdbcQuizAttemptRepositoryIntegrationTest`.
- Flyway áp dụng đủ 42 migrations, bao gồm migration mới. Kiểm thử xác nhận hai lớp có lượt số 1 độc lập, bài cũ giữ nguyên và unique index chặn lượt đang làm trùng trong cùng lớp.
- Test quota chạy qua Spring transaction interceptor thật, kiểm tra fallback cá nhân thành công và không rollback JDBC.
- Frontend quiz: **35 tests/3 files** qua; TypeScript và ESLint cho hai file chỉnh sửa qua.
- `git diff --check` cho mã sửa qua.
- Log: `.tmp/fix-review-isolated-backend.log`, `.tmp/fix-review-final-frontend.log`, `.tmp/fix-review-final-types.log`, `.tmp/fix-review-final-lint.log`. Lượt backend trước bản bổ sung xử lý lỗi DB có 87 tests qua.

Lệnh kiểm thử backend trong container: `mvn -o -B -Dtest=AiTaskFlowTest,JobDispatcherTest,QuizPracticeServicesTest,JdbcQuizAttemptRepositoryIntegrationTest test`. Frontend: `npx vitest run tests/features/quizpractice`, `npm run typecheck`, scoped ESLint.

Không chạy lại toàn bộ suite/build production sau sửa; kết quả full-suite trong báo cáo 12/11 thuộc baseline cũ. Không gọi provider trả phí hoặc thử trên dữ liệu thật. Giữ nguyên thay đổi đồng thời ngoài phạm vi. Không commit/push/deploy.

## Tuân thủ trong phạm vi sửa

- Security: giữ kiểm tra quyền/lớp, không thêm endpoint hoặc đưa secret vào log; đáp án cũ không chuyển sở hữu.
- Resiliency: regression test cho lưu lỗi, transaction fallback, timeout và lỗi DB; migration kiểm thử trên PostgreSQL riêng.
- PBT: tắt theo cấu hình hiện có. Performance/load và live-provider checks: N/A trong lượt sửa lỗi cục bộ này, không tuyên bố đã kiểm chứng.
