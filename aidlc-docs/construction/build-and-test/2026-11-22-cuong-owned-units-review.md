# Rà soát các phần Đào Chí Cường phụ trách — 2026-11-22

**Phạm vi.** Rà soát các unit có code owner là Đào Chí Cường trên Jira: U01 Account & Access (KAN-18), U10 Quiz Authoring & Bank (KAN-23), U11 Quiz Practice (KAN-25). Gồm backend `identity`, `quizzes`, `quizpractice` và frontend `features/identity`, `features/quizzes`, `features/quizpractice`.

**Baseline:** `origin/staging` @ `210b0550`, kiểm tra trên một worktree tách riêng. Chỉ đọc và rà soát; chưa sửa code, commit hay đổi Jira.

## Kết luận

Không có lỗi chặn release. Đã tạo bug Jira: KAN-211 (mục 3.1), KAN-212 (3.2 và 3.5), KAN-213 (3.3), KAN-214 (3.4). CI trên staging đạt toàn bộ, Jira không còn Bug nào mở. Rà soát code tìm thêm **1 lỗi người dùng gặp được**, **2 lỗi nhỏ có sẵn từ trước** và **2 điểm vặt**, đều chưa được ghi trên Jira.

## 1. Kết quả kiểm thử (CI staging, run 44998732763, commit 210b0550)

| Hạng mục | Kết quả |
|---|---|
| Backend `mvn -B verify` | 2.625 ca, 0 failure/error, 1 skipped (`GradingAiEvaluationTest` của U09, ngoài phạm vi) |
| Test của U01 (`identity`) | 263 ca đạt |
| Test của U10 (`quizzes`) | 188 ca đạt |
| Test của U11 (`quizpractice`) | 57 ca đạt |
| Frontend `npm test` | 157/157 file test đạt |
| Frontend lint | 0 lỗi, 1 cảnh báo ở `src/app/opengraph-image.tsx` (ngoài phạm vi) |

Chưa chạy test trên máy local vì máy không có Maven và Docker đang tắt. Kết quả trên lấy từ CI của chính commit được rà soát.

## 2. Jira

- Không còn Bug nào mở trong project KAN.
- Mọi story của U10 (KAN-143, 78, 44) và U11 (KAN-128, 129, 130, 57) đã Done. Bug KAN-182 đã Done.
- Epic KAN-23 và KAN-25 vẫn **To Do**, còn KAN-18 đang **In Progress**. Checklist của ba Epic này còn thiếu người viết test độc lập, kết quả kiểm thử tích hợp độc lập và xác nhận của reviewer.

## 3. Lỗi tìm thấy

| # | Mức | Unit | Vị trí | Vấn đề |
|---|---|---|---|---|
| 1 | Nên sửa | U11 | `QuizTakingPage.tsx:217`, `QuizAttemptService.java:139` | Lượt quá hạn bị chuyển qua lại giữa trang làm bài và trang kết quả |
| 2 | Nhỏ | U11 | `QuizAttemptService.java:63` | "Tiếp tục" mở lại lượt đang làm mà không kiểm lớp/Quiz còn mở và còn hạn |
| 3 | Nhỏ | U01 | `OtpChallengeStore.java:72` | Ghi OTP vào Redis không nguyên tử, có thể mất thời hạn 10 phút |
| 4 | Vặt | U10 | `QuizPublicationService.java:101` | Thông báo lỗi lý do ngưng Quiz sai nghĩa |
| 5 | Vặt | U11 | `QuizAttemptService.java:67` | `start()` tải Quiz hai lần |

### 3.1 Lượt quá hạn bị chuyển qua lại giữa hai trang (U11)

**Cách tái hiện:**
1. Quiz giới hạn 10 phút. Sinh viên đóng tab ở phút thứ 5.
2. Sinh viên mở lại lượt (hoặc bấm "Tiếp tục") ở phút 10:45, trước khi job tự nộp chạy (job chạy mỗi phút).
3. Trang làm bài hiện đồng hồ 00:00 và tự nộp. Máy chủ trả `409 ATTEMPT_TIME_UP`, vì đã quá hạn cộng 30 giây ân hạn.
4. Trang chuyển sang kết quả. Trang kết quả nhận `409 ATTEMPT_IN_PROGRESS` và chuyển ngược về trang làm bài.
5. Vòng này lặp lại mỗi 1–3 giây, hộp thoại "Hết giờ" nhấp nháy, cho tới khi job tự nộp xử lý lượt (tối đa khoảng 1 phút).

**Đề xuất:** khi lượt đã quá hạn, `QuizAttemptService.submit` (và nên cả `loadAttempt`, `loadResult`) tự nộp ngay bằng `SubmitMode.AUTO_TIME_LIMIT` thay vì trả 409. Kết quả có ngay và vòng lặp biến mất. Nên thêm test cho trường hợp mở lại lượt sau hạn.

### 3.2 Mở lại lượt đang làm không kiểm trạng thái (U11)

`start()` trả lượt đang làm (dòng 63–66) trước khi kiểm lớp và Quiz còn `OPEN` (dòng 68), và không kiểm lượt còn hạn.

**Hệ quả:** nếu lớp không còn mở, sinh viên vào trang làm bài nhưng mọi lần tự lưu đều báo "Không lưu được" (`403 CLASS_NOT_OPEN`). Nếu lượt đã quá hạn, sinh viên rơi vào lỗi 3.1.

**Đề xuất:** với lượt đang làm, kiểm trạng thái trước khi trả về. Lượt quá hạn thì tự nộp rồi chuyển sang kết quả.

### 3.3 OTP có thể mất thời hạn (U01)

`writeChallenge` gọi ba lệnh Redis riêng: `DEL`, `HSET`, `EXPIRE`. Nếu lệnh `EXPIRE` lỗi (Redis timeout, tiến trình dừng giữa chừng), mã OTP kích hoạt hoặc đặt lại mật khẩu không bao giờ hết hạn. Mã đó vẫn dùng được cho tới khi được dùng hoặc sai đủ 5 lần, trái với BR-U01-20 (10 phút).

**Đề xuất:** gộp ba lệnh vào một Lua script hoặc `MULTI/EXEC`.

### 3.4 Thông báo lỗi khi ngưng Quiz (U10)

Lý do ngưng dài hơn 1.000 ký tự nhận thông báo "Lý do ngưng bắt buộc". Nhưng theo BR-U10-23, lý do trống vẫn được chấp nhận và mặc định là "Lưu trữ", nên thông báo này sai nghĩa. Javadoc ở dòng 91 cũng đã cũ. **Đề xuất:** đổi thành "Lý do tối đa 1.000 ký tự".

### 3.5 Tải Quiz hai lần (U11)

`start()` gọi `loadQuizInClass` ở dòng 62 và lặp lại ở dòng 67, trong khi đang giữ khóa dòng Quiz. **Đề xuất:** bỏ lần gọi thứ hai.

## 4. Những phần đã rà và không thấy lỗi

**U01:**
- Đăng nhập trả cùng một lỗi cho mọi trường hợp thất bại, có hash giả để thời gian phản hồi đồng đều.
- Khóa tạm sau nhiều lần sai, giới hạn theo IP. Nginx ghi đè `X-Forwarded-For` nên client không giả được IP.
- OTP: trừ lượt rồi mới so mã trong một lệnh Redis, dùng một lần.
- Kích hoạt và đặt lại mật khẩu: xóa khóa tạm và hủy mọi phiên trước và sau commit.
- Đổi quyền hoặc trạng thái tài khoản thì hủy phiên. Access token được kiểm với phiên còn sống ở mỗi request, nên token cũ hết tác dụng ngay.

**U10:**
- Sửa câu của Quiz đang mở bị chặn khi đã có lượt làm.
- Câu ngân hàng đang dùng không sửa hay xóa trực tiếp được (sửa thì mở bản nháp mới).
- Phát hành thì ngưng version cũ trong cùng transaction. Mở lại không cho hai version cùng mở.

**U11:**
- Khóa dòng Quiz cộng khóa theo người học khi bắt đầu lượt, nên không vượt số lượt và không đua với việc giáo viên sửa Quiz.
- Lưu câu trả lời và nộp bài có kiểm `answerVersion`. Nộp lặp không chấm hai lần.
- Ẩn đúng điểm và đáp án theo cài đặt đã chụp lúc bắt đầu lượt.
- Trừ điểm rời trang chỉ là gợi ý, giáo viên quyết định và phải ghi lý do.
- Kiểm tra JSON đầu vào chặt chẽ.

**Giới hạn của lần rà soát:** đã đọc kỹ các service, model và luồng chính. Các trang quản trị tài khoản, nhập tài khoản và giao diện soạn Quiz bằng AI ở frontend chỉ được kiểm qua test CI, chưa đọc từng dòng. Chưa kiểm thử thủ công trên staging.
