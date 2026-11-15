# Báo cáo rà soát bug toàn dự án — 12/11/2026

## Kết quả

Phát hiện **4 lỗi có bằng chứng tái hiện cục bộ**: 1 lỗi ưu tiên cao (P1), 3 lỗi ưu tiên trung bình (P2). Các bộ kiểm thử hiện tại đều qua nhưng chưa bao phủ các tình huống này. Báo cáo không thay đổi mã ứng dụng.

Baseline: nhánh `feat/ai-prompt-guardrail`, commit `65fcc764c5aa9e28f2ff6201ea0bb88096be89b6`. Kết luận áp dụng cho checkout này, không tự suy rộng sang bản đang chạy trên VPS hoặc HEAD mới của staging.

| Mã | Ưu tiên | Lỗi | Ảnh hưởng |
|---|---|---|---|
| BUG-01 | P1 | Thoát quiz dù lưu đáp án thất bại | Có thể mất các đáp án chưa lưu |
| BUG-02 | P2 | Lỗi quota làm rollback cả nhánh chuyển nguồn AI | Có khóa cá nhân dùng được nhưng tác vụ không chạy tiếp |
| BUG-03 | P2 | Quiz ở lớp mới tiếp tục nhầm lượt của lớp cũ | Sinh viên bị kẹt, không mở/lưu/nộp được lượt đó |
| BUG-04 | P2 | Timeout toàn job chỉ đủ cho một lần gọi AI | Cắt ngang chuỗi retry/fallback hợp lệ |

## BUG-01 — Thoát quiz làm mất cơ hội lưu lại đáp án

Vị trí: `frontend/src/features/quizpractice/pages/QuizTakingPage.tsx:228`; liên quan nhánh xử lý lỗi lưu ở dòng 110–123 và `flush` ở dòng 132.

`flush()` trả về kết quả `failed` khi API lưu đáp án lỗi. Hàm `exit` không kiểm tra kết quả này mà luôn gọi `router.push('/quiz-practice')` trong `.then(...)`. Vì `flush` resolve thành công với một giá trị lỗi nghiệp vụ, `.then` vẫn chạy.

Tái hiện:

1. Mở quiz không giới hạn thời gian, chọn một đáp án mới.
2. Cho API lưu đáp án lỗi mạng/503.
3. Bấm **Thoát** trước khi đáp án được lưu.
4. Giao diện chuyển trang dù trạng thái vẫn là **Chưa lưu được**. Nếu mạng vẫn lỗi, lần flush khi unmount cũng không cứu được dữ liệu; không có bản nháp bền vững để khôi phục.

Kiểm thử dùng component thật, mock API từ chối lưu và router: xác nhận đồng thời `saveAnswers` đã nhận đáp án mới, `push('/quiz-practice')` đã chạy, trạng thái lưu còn lỗi. **1/1 ca tái hiện qua**.

Hướng sửa: chỉ điều hướng khi lưu thành công; khi lỗi giữ màn hình và đáp án, cho thử lại. Nếu cho phép bỏ dữ liệu, cần lựa chọn bỏ thay đổi rõ ràng. Áp dụng cả nhánh thoát quiz có giới hạn thời gian vì dùng chung hàm `exit`.

Bằng chứng: `.tmp/review-quiz-exit.test.tsx`, `.tmp/review-20261112-quiz-repro.log`. Probe được chạy tạm dưới `frontend/tests` để phân giải dependency rồi gỡ; mã tái hiện giữ trong `.tmp`.

## BUG-02 — Không chuyển được sang khóa cá nhân khi dự phòng gặp lỗi quota

Vị trí: `backend/src/main/java/edu/aiplatform/ai/service/AiTaskJobHandler.java:306` (gọi `quota.reserve`, bắt `RuntimeException` rồi tiếp tục); `backend/src/main/java/edu/aiplatform/ai/service/QuotaReservationService.java:70` (`Propagation.MANDATORY`).

`reserve()` tham gia transaction của worker thông qua Spring proxy. Khi quota hết/bị chặn, phương thức ném `ApiException`; interceptor đánh dấu transaction chung là rollback-only. Bắt exception trong `next()` không xóa cờ này. Worker có thể chọn được khóa cá nhân tiếp theo nhưng commit của `transactions.execute(...finish...)` ném `UnexpectedRollbackException` trước khi gateway cá nhân được gọi.

Tình huống: tác vụ đang dùng khóa chung bị provider từ chối; còn một khóa chung dự phòng và một khóa cá nhân hợp lệ; hạn mức bị chặn/hết sau lúc enqueue. Khóa chung dự phòng không giữ được quota, khiến việc chuyển sang khóa cá nhân thất bại. Các cập nhật quyết toán/trạng thái trong transaction lỗi cũng bị rollback trên DB thật.

Đã tái hiện bằng worker thật, `QuotaReservationService` thật được bọc Spring `TransactionInterceptor`, `DataSourceTransactionManager`; repository/JDBC/provider dùng mock. Kết quả: **`UnexpectedRollbackException`; gateway cá nhân không được gọi; JDBC rollback được gọi**. Đây là kiểm tra cơ chế transaction, không phải thử trên tài khoản hay DB nghiệp vụ thật.

Hướng sửa: tách lỗi nghiệp vụ “không đủ quota” khỏi lỗi DB và xử lý transaction tương ứng; có thể trả kết quả từ chối thay vì ném exception làm rollback transaction chung. Không bắt mọi lỗi DB rồi tiếp tục trên transaction đã lỗi. Bổ sung test sử dụng Spring proxy và transaction thật, thay vì chỉ mock service quota.

Bằng chứng: `.tmp/ReviewAiHarness.java` (chế độ mặc định), `.tmp/review-20261112-ai-repro.log`.

## BUG-03 — Lượt quiz ở lớp cũ chặn làm quiz ở lớp mới

Vị trí: `backend/src/main/java/edu/aiplatform/quizpractice/service/QuizAttemptService.java:63`; truy vấn ở `quizpractice/repository/JdbcQuizAttemptRepository.java:61`; kiểm quyền mở lượt ở `quizpractice/service/PracticeSupport.java:122`.

`start()` tìm lượt `IN_PROGRESS` chỉ theo `(quizId, accountId)`, rồi trả lại ngay mà không đối chiếu lớp của lượt với lớp đang yêu cầu. Ngược lại, mở/lưu/nộp lượt đều kiểm quyền và trạng thái **lớp gốc**.

Tái hiện bằng service thật và fixture repository trong bộ nhớ:

1. Sinh viên bắt đầu quiz phạm vi môn học tại lớp A, không giới hạn thời gian, chưa nộp.
2. Lớp A chuyển sang `ARCHIVED`; sinh viên học lớp B đang `OPEN` của cùng môn.
3. Bắt đầu cùng quiz tại B: nhận lại ID lượt của A và `classId=A`.
4. Mở lượt vừa nhận bị từ chối `CLASS_NOT_OPEN`.

Vì lượt không có `deadlineAt`, scanner tự nộp theo hạn không giải phóng trường hợp này. Màn lịch sử/chi tiết cũng chọn lượt đang làm theo quiz, nên có thể tiếp tục dẫn sinh viên tới lượt không còn dùng được. Việc hết quyền ở lớp cũ cũng tạo cùng mâu thuẫn.

Hướng sửa: xác định rõ cách xử lý lượt dở dang khi đổi lớp hoặc lớp đóng; không tiếp tục lượt mà người học không còn được thao tác. Cần thống nhất việc tìm lượt, ràng buộc duy nhất, số lượt được phép và lịch sử, tránh chỉ thêm điều kiện classId ở một truy vấn.

Bằng chứng: `.tmp/ReviewQuizTransferHarness.java`, `.tmp/review-20261112-transfer-repro.log` — **đã xác nhận chuỗi start B → lượt A → CLASS_NOT_OPEN**.

## BUG-04 — Timeout tổng ngắt chuỗi retry/chuyển khóa AI

Vị trí: `backend/src/main/java/edu/aiplatform/ai/service/AiTaskJobHandler.java:124`; vòng gọi/chờ/thử lại ở dòng 151–157; `backend/src/main/java/edu/aiplatform/jobs/service/JobDispatcher.java:100`.

`timeout()` trả `requestTimeout + 30 giây`, nhưng một lần `handle()` có thể gọi lại cùng provider hai lần, đợi 2 + 5 giây, rồi tiếp tục các khóa/provider khác. Dispatcher áp timeout này cho **toàn bộ** `handle()` và interrupt thread khi hết giờ; mỗi lần gọi riêng vẫn nhận đầy đủ `requestTimeout`.

Ví dụ với cấu hình mặc định: job bị ngắt sau 150 giây. Lần đầu trả lỗi có thể retry sau 90 giây, rồi lần sau cần thêm 90 giây; cả hai đều dưới giới hạn 120 giây/lần nhưng tổng vượt 150 giây. Chuỗi bị ngắt trước khi provider thứ hai hoàn tất.

Tái hiện cục bộ với worker và dispatcher thật, gateway giả: `requestTimeout=1 giây`, 40 khóa, mỗi lần trả sau 900ms. Job bị ngắt sau **31.016ms**, trả `RETRIED` trước khi đến kết quả thành công cuối chuỗi. Log còn ghi việc xử lý các khóa tiếp theo với lỗi `UNKNOWN` do thread đã bị interrupt. Không gọi provider thật.

Hướng sửa: quản lý riêng ngân sách tổng và timeout từng request, hoặc tách các lượt fallback thành job nối tiếp có trạng thái bền vững. Đồng bộ mốc recovery của tác vụ với chính sách mới; dừng vòng fallback khi bị interrupt thay vì tiếp tục thử trên thread đã bị hủy.

Bằng chứng: `.tmp/ReviewAiHarness.java` với tham số `timeout`, `.tmp/review-20261112-timeout-repro.log`.

## Kiểm chứng đã thực hiện

| Kiểm tra | Kết quả |
|---|---|
| Backend `mvn -B verify` trong Maven/JDK 17 container | **BUILD SUCCESS; 2.342 test; 0 failure/error/skip** |
| Guard integration bắt buộc | Qua: 31 subject + 7 notification + 11 evaluation + 1 visibility + 26 workflow; 57 ca có trace không thiếu/bỏ qua (các số này nằm trong tổng suite, không cộng thêm) |
| Frontend `npm test` | **1.018 test / 146 file qua** |
| Frontend ESLint | Qua |
| Frontend production build | Qua |
| TypeScript sau làm mới cache | Qua |
| MCP hook fixture | **14 tình huống qua** |
| ShellCheck hai script deploy | Qua |
| Bốn bản tái hiện nêu trên | Xác nhận hành vi lỗi; chưa sửa code |

TypeScript ban đầu lỗi do `.next/types` và `.next/dev/types` chứa route đã xóa. `next typegen` đơn lẻ không dọn hết các tệp cũ. Đã chuyển hai thư mục type cache vào `.tmp/review-cache-*`, build lại và chạy typecheck thành công. Không tính cache local này là bug ứng dụng thứ năm.

Log kiểm tra chính nằm ở `.tmp/review-20261112-{backend,frontend,lint,build,typecheck-clean,shellcheck}.log`. Một lượt chạy cấu hình probe Vitest ban đầu vô tình chạy lại suite và lỗi phân giải dependency của probe ở ngoài frontend; đã khắc phục cách chạy, probe cuối qua. Không tính lỗi harness đó là lỗi dự án.

## Phạm vi và giới hạn

- Đã lập inventory 767 tệp Java production, 479 tệp frontend; rà soát cấu hình và các luồng chính qua identity, audit/settings, file/jobs/backup, subject/class/enrollment, AI, assignment/work/grading, quiz/practice, group/groupdocs, material/announcement/notification và dashboard. Đây là rà soát theo luồng/rủi ro, không khẳng định đọc từng dòng của toàn bộ các tệp.
- Bộ test toàn dự án đã chạy trên checkout hiện tại; các test tích hợp dùng tài nguyên Testcontainers riêng. Không sửa điểm/bài làm/tài khoản thật, không gọi AI trả phí, không restart ứng dụng, commit, push hay deploy.
- Chưa kiểm tra UI bằng trình duyệt thật, tải đồng thời/độ bền dài hạn, provider AI thật, SMTP/Drive thật hoặc cấu hình production đang chạy. Không thực hiện kiểm toán CVE/dependency trực tuyến.
- Script fixture Python `scripts/test-check-mcp.py` chưa chạy được vì môi trường không có Python trên PATH. ShellCheck đã qua nhưng chưa chạy lại toàn bộ harness deploy shell.
- Security/Resiliency giữ cấu hình dự án; BUG-02 và BUG-04 là khoảng hở phục hồi lỗi/transaction liên quan RESILIENCY-10, cần xử lý khi sửa. Báo cáo này không chứng nhận toàn bộ các quy tắc bảo mật/hạ tầng đạt chuẩn. Các stage thiết kế/triển khai không áp dụng; PBT vẫn tắt.
- Một chat khác bổ sung mục audit merge PR77 trong lúc rà soát; đã giữ nguyên mục đó. HEAD và mã ứng dụng không đổi trong đợt kiểm tra này.

Ưu tiên sửa: BUG-01 trước để bảo vệ đáp án; sau đó BUG-02, BUG-03, BUG-04 và bổ sung regression test tương ứng.
