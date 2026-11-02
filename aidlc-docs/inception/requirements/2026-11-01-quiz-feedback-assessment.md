# Đánh giá góp ý phần Quiz — 2026-11-01

Đối chiếu 7 góp ý với code trên nhánh `fix/review-bugs-2026-11-01` (`2f1ccdf4`). Tiếp nối [góp ý 29/10](2026-10-29-quiz-assignment-feedback.md). Các điểm cần chốt nằm ở [file câu hỏi](2026-11-01-quiz-rework-questions.md).

**Cập nhật 02/11:** người dùng chốt hệ thống chỉ gợi ý mức trừ, giảng viên quyết định từng lượt, thay đề xuất tự trừ ở mục 6. **Cập nhật:** cả 7 mục đã được làm trên nhánh `feat/quiz-rework-2026-11-01`, xem [kết quả](../../construction/build-and-test/2026-11-01-quiz-rework-results.md). Báo cáo này giữ nguyên hiện trạng lúc đánh giá, trừ hai đính chính: khu vực lượt làm của giảng viên thực ra chưa từng hiện trên giao diện (mục 3); khi triển khai không dùng `pg_trgm` và không làm phần thoát toàn màn hình (xem [roadmap](../../construction/plans/code-generation-plan/2026-11-01-quiz-rework.md)).

**Phạm vi kiểm tra:** chỉ đọc code, migration và tài liệu. Chưa chạy thử trên giao diện, chưa đọc dữ liệu thật, chưa sửa code. Công sức là ước lượng thô cho một người, chưa gồm kiểm thử trên staging.

## Tóm tắt

| # | Góp ý | Kết luận | Hướng xử lý | Công sức |
|---|---|---|---|---|
| 1 | Tài liệu tham khảo thừa | Đúng với phần gắn vào quiz; phần trong popup AI vẫn có ích | Bỏ phần gắn vào quiz, giữ cho AI và đổi tên | 0,5–1 ngày |
| 2 | Sinh đề mới, cấu hình theo tag | Đúng, chưa có | Thêm chế độ sinh đề theo quy tắc, chụp đề vào lượt làm | 4–6 ngày |
| 3 | Ghi out tab, sinh viên có thể tắt | Đúng, đã có ghi nhận nhưng không chống được người cố tình | Coi là tín hiệu, thêm phát hiện mất tín hiệu, dựa chính vào đề ngẫu nhiên | 1–2 ngày |
| 4 | Mỗi câu một trang | Đúng một phần: làm bài đã có, xem kết quả chưa | Thêm bộ chuyển câu dùng chung | 0,5–1 ngày |
| 5 | Chống trùng, chủ nhiệm môn duyệt | Đúng, chưa có cả hai | Tự phát hiện trùng cho mọi nguồn; duyệt riêng câu của giảng viên | 3–5 ngày |
| 6 | Cấu hình trừ điểm theo out tab | Đúng, chưa có và đang trái quyết định FR-039 | Cần chốt trước; đề xuất tính điểm trừ, giảng viên được bỏ trừ | 2–3 ngày |
| 7 | Bước duyệt quiz thừa | Đúng, người soạn tự duyệt chính mình | Bỏ `REVIEWED`, phát hành thẳng từ nháp | 1 ngày |

Thứ tự đề xuất: **Đợt 1** gồm #7, #4, #1 (ít rủi ro). **Đợt 2** là #5. **Đợt 3** là #2. **Đợt 4** là #3 và #6, sau khi chốt FR-039.

## 1. Tài liệu tham khảo

Cùng một tên đang chỉ ba thứ khác nhau:

| Phần | Tình trạng | Bằng chứng |
|---|---|---|
| Gắn tài liệu vào quiz (`reference_material_ids`) | Không dùng được trên web: form giữ giá trị nhưng không có ô chọn. Chỉ tạo được qua MCP | `QuizDetailParts.tsx:124,140,171`; panel cấu hình `:177-212` không có ô này |
| Hiện tài liệu cho sinh viên trên trang quiz | Không hoạt động: slot đã đăng ký nhưng không nơi nào hiển thị; API vẫn trả `referenceMaterialIds` | `quizpractice/slots.ts:5`; `materials/register.ts:19`; `QuizPracticeQueryService.java:131` |
| "Quiz gắn với học liệu" trên trang tài liệu | Chỉ hiện khi đã có liên kết ở dòng đầu, nên gần như không xuất hiện | `MaterialQuizList.tsx`; `JdbcQuizRepository.java:201-208` |
| "Tài liệu tham khảo" trong popup AI | **Có dùng:** là nguồn chữ để AI sinh câu hỏi | `AiDrafts.tsx:264`; `QuizAiDraftService.java:159` |

Lỗi phụ: MCP cho gửi tối đa 20 tài liệu (`QuizMcpAuthoringService.java:33,53`), còn quy tắc chỉ cho 10 (`QuizRules.java:19,41`). Gửi 11–20 tài liệu sẽ qua schema rồi bị từ chối.

**Hướng xử lý**

1. Bỏ `referenceMaterialIds` khỏi form, API quiz, API sinh viên, tool MCP và quy tắc; bỏ slot `quizMaterialSections`.
2. Bỏ `MaterialQuizList` và `listByMaterial`, vì quiz đã nằm trong module học liệu.
3. Giữ phần chọn tài liệu cho AI, đổi nhãn thành "Tài liệu nguồn cho AI". Giữ `QuizReferenceMaterials` vì AI đang dùng.
4. Trước khi xóa cột, chạy `select count(*) from quizzes where jsonb_array_length(reference_material_ids) > 0` trên staging. Xóa cột bằng migration riêng ở đợt sau để dễ quay lại.

Assignment có trường cùng tên và không thuộc phạm vi này.

## 2. Sinh đề mới và cấu hình theo tag

**Hiện trạng:** mọi sinh viên nhận cùng một bộ câu. "Đảo câu" chỉ đổi thứ tự.

- Lượt làm lấy toàn bộ `quiz_items` rồi trộn: `AttemptSnapshot.java:29-47`, `QuizAttemptService.java:77-79`.
- "Bốc ngẫu nhiên" chỉ chạy lúc soạn, mỗi lần một tag; câu bốc được ghim cố định vào quiz: `BankQuestionPicker.tsx:47-63`, `QuizAuthoringService.java:197-208`.
- Tag là chữ tự do, không có danh mục, nên "Chương 1" và "chuong1" thành hai tag: `V20260925_1600__create_quiz_tables.sql:67`, `JdbcQuestionRepository.java:254-257`.

**Chỗ phải sửa nếu không muốn chấm sai**

- `QuizScorer.score` chấm trên toàn bộ `quiz_items` (`:43-63`). Giữ nguyên thì câu sinh viên không được giao sẽ tính sai, điểm tối đa cũng bị thổi lên.
- `getStudentView`, `answerReview`, số câu và điểm tối đa đều đọc từ `quiz_items` (`QuizQueryService.java:61-77`, `QuizScorer.java:67-77`, `QuizPracticeQueryService.java:128`).
- Snapshot chưa lưu điểm từng câu (`AttemptSnapshot.java:19-21`).

**Hướng xử lý**

1. Thêm chế độ đề: `FIXED` (như hiện tại, quiz cũ giữ nguyên) và `GENERATED`.
2. Bảng mới `quiz_draw_rules(quiz_id, position, tag, difficulty, question_count, points_each)`. Ví dụ: tag 1 lấy 4 câu, tag 2 lấy 5 câu, tag 3 lấy 7 câu.
3. Khi bắt đầu lượt: lấy câu `ACTIVE` mới nhất từ ngân hàng của môn, không lặp lineage, rồi lưu `questionId` và điểm vào snapshot. Câu `ACTIVE` không sửa được (BR-U10-04) nên snapshot chỉ cần lưu id.
4. Chấm điểm và dựng kết quả theo snapshot. Snapshot cũ không có điểm thì đọc `quiz_items` như hiện tại.
5. Một câu có thể mang nhiều tag, nên phải kiểm phân bổ để không dùng một câu cho hai quy tắc và để không báo thiếu sai. Có thể dùng ghép cặp: số câu nhỏ, chạy trong Java là đủ.
6. Khi phát hành, kiểm đủ câu cho mọi quy tắc. Khi bắt đầu lượt mà kho đã thiếu (do câu bị ngưng), từ chối với thông báo rõ. Khi ngưng câu làm kho thiếu, cảnh báo chủ nhiệm môn.
7. Giao diện: ô chọn tag có gợi ý và số câu hiện có, tổng câu và tổng điểm, nút "Xem thử đề mẫu".
8. Làm lại nhiều lần: ưu tiên câu sinh viên chưa gặp, hết câu mới thì dùng lại.

## 3. Chống gian lận bằng out tab

**Hiện trạng:** đã có ghi nhận, mặc định tắt, bật theo từng quiz và bắt buộc có lời báo.

- `useActivityTracker.ts:50-53` chỉ nghe `visibilitychange`. Chuyển tab hoặc thu nhỏ cửa sổ thì ghi nhận. Mở cửa sổ khác cạnh bên hoặc dùng màn hình thứ hai thì thường không ghi nhận, vì không nghe `blur` (`:16-18`).
- Lỗi gửi được bỏ qua (`:44-46`). Không có cơ chế buộc phải có tín hiệu thì mới làm bài.
- Đính chính sau khi chạy thử: `TeacherQuizSections.tsx` có phần số liệu, nhưng slot `QuizDetailSection` chưa từng được render, nên giảng viên không có giao diện xem lượt làm.

**Góp ý "sinh viên có thể disable" là đúng.** Tắt hẳn JS thì trang không chạy, nhưng vẫn có thể chặn request `/activity`, ghi đè `visibilityState` bằng extension, tự gửi sự kiện giả, hoặc đơn giản là dùng điện thoại. Không có cách nào dùng JS mà chống được việc này hoàn toàn.

**Lỗi đang có**

- Sự kiện chỉ giữ 1 ngày (`ActivityService.java:37`). Với quiz, sau khi bị xóa giảng viên thấy "Chưa có dữ liệu" thay vì "Đã hết hạn", vì nhánh `EXPIRED` chỉ chạy cho Assignment (`:81-91`).
- Tải lại trang có thể tính thành một lần rời: `HIDDEN` rồi `SESSION_END` vẫn được đếm (`ActivitySummaryCalculator.java:72-83`).
- Sự kiện cuối có thể mất khi nộp bài: tracker gửi song song với lệnh nộp, mà lượt đã nộp thì bị từ chối (`QuizTakingPage.tsx:136-145`, `QuizActivityService.java:39`).

**Hướng xử lý**

1. Thêm `blur` và thoát toàn màn hình như tín hiệu phụ, chỉ cho Quiz. Assignment không dùng được `blur` vì iframe Draw.io sẽ gây báo nhầm.
2. Phát hiện mất tín hiệu ở server: bật theo dõi mà không có `SESSION_START`, hoặc câu trả lời vẫn được lưu (`last_saved_at`) trong khi heartbeat đã ngừng, thì gắn cờ "Mất tín hiệu". Chỉ là cờ để giảng viên xem, không phải bằng chứng.
3. Hiện số lần rời và cờ này ngay trên danh sách lượt làm.
4. Chụp số liệu vào lượt làm khi nộp để không mất sau 1 ngày, và sửa thông báo hết hạn.
5. Biện pháp chính không phụ thuộc JS: đề khác nhau (#2), giới hạn thời gian và số lượt, chỉ hiện đáp án sau khi quiz đóng.
6. Nếu quiz có giá trị điểm cao: thi tại phòng có giám sát hoặc dùng Safe Exam Browser. Không nên chỉ dựa vào out tab.

## 4. Mỗi câu một trang

| Màn hình | Hiện trạng | Bằng chứng |
|---|---|---|
| Làm bài | Đã mỗi lần một câu, có Câu trước/Câu tiếp và bảng số câu. Tải lại trang thì quay về câu 1 | `QuizTakingPage.tsx:36,206,255-273` |
| Sinh viên xem kết quả | Chưa: danh sách dài | `ResultQuestions.tsx:10-38` |
| Giảng viên xem bài | Chưa: dùng chung component trên, trong popup | `TeacherQuizSections.tsx:87` |

**Hướng xử lý** (chỉ sửa frontend)

1. Tách phần chuyển câu thành component `QuestionPager` dùng chung.
2. Đổi `ResultQuestions` thành hiện từng câu, bảng số câu có màu đúng, sai, bỏ trống. Sửa một chỗ là áp dụng cho cả sinh viên và giảng viên.
3. Lưu câu đang xem vào URL (`?q=3`) bằng `useUrlState` có sẵn, dùng kiểu thay thế lịch sử để nút Back không đi qua từng câu.

Lưu ý: đây chỉ là thay đổi hiển thị. Trình duyệt vẫn nhận cả đề ngay từ đầu (`QuizAttemptService.java:175-181`). Tải từng câu từ server là việc lớn hơn và không cần trong đợt này.

## 5. Chống trùng và duyệt câu vào ngân hàng

**Hiện trạng**

- Không kiểm trùng giữa các câu. Chỉ kiểm trùng lựa chọn trong một câu (`QuestionRules.java:55-68`) và trùng id trong một quiz (`QuizRules.java:86-90`). Nhập tệp, AI và MCP đều có thể tạo câu trùng.
- Giảng viên không đưa được câu vào ngân hàng: quyền chỉ cấp cho `SUBJECT_MANAGER` (`AccessPolicy.java:63-64`). Câu riêng của quiz không có đường chuyển lên (`V20260925_1600__create_quiz_tables.sql:77`).
- Chủ nhiệm môn tự kích hoạt từng câu và không có thao tác hàng loạt (`QuestionController.java:155`). Nhập 500 câu nghĩa là bấm 500 lần.
- Lỗi phụ: kích hoạt bản mới không ngưng bản cũ (`QuestionBankService.java:144`), và `saveItems` không kiểm lineage (`QuizAuthoringService.java:169-185`). Gọi API trực tiếp có thể đưa hai phiên bản của một câu vào cùng quiz; giao diện đã ẩn trường hợp này.

**Hướng xử lý:** dùng cả hai, vì chúng giải quyết hai việc khác nhau.

*Chống trùng (mọi nguồn: soạn tay, nhập tệp, AI, MCP, đề xuất của giảng viên)*

1. Chuẩn hóa nội dung (NFC, chữ thường, gộp khoảng trắng), sắp xếp lựa chọn rồi băm thành cột `content_hash`. Giống hệt thì chặn và chỉ ra câu đã có.
2. Gần giống thì cảnh báo, không chặn: dùng `pg_trgm` với `similarity` trên nội dung đã chuẩn hóa, ngưỡng khởi điểm 0,7 rồi chỉnh theo dữ liệu thật. Cần kiểm role chạy Flyway có quyền `create extension` không.
3. Không so với các phiên bản cùng lineage. Dùng kiểm tra trong service kèm khóa theo môn, không dùng unique index, vì bản mới của cùng câu được phép giống bản cũ.
4. Nhập tệp báo lỗi theo dòng. Dữ liệu cũ: tính hash rồi tạo báo cáo cho chủ nhiệm môn, không tự xóa.
5. Giới hạn: cách này không bắt được câu viết lại khác hẳn về từ ngữ. Phần đó do người duyệt phát hiện. Chưa cần dùng embedding ở đợt này.

*Duyệt câu của giảng viên*

1. Thêm trạng thái: `DRAFT → PENDING → ACTIVE`, hoặc `PENDING → REJECTED` kèm lý do; giảng viên sửa rồi gửi lại.
2. Thêm quyền đề xuất cho giảng viên của lớp thuộc môn, và nút "Đề xuất vào ngân hàng" từ câu riêng của quiz. Hệ thống tạo bản sao trong ngân hàng, câu trong quiz giữ nguyên.
3. Thêm tab "Chờ duyệt" cho chủ nhiệm môn: xem câu tương tự cạnh nhau, duyệt hoặc từ chối hàng loạt. Thông báo hai chiều qua U14, ghi audit.
4. Phải sửa truy vấn: `searchBank` và `activeSql` đang xếp hạng theo `status <> 'DRAFT'` (`JdbcQuestionRepository.java:146-147,274-275`). Nếu không sửa, bản `PENDING` mới sẽ đứng đầu và câu `ACTIVE` đang dùng biến mất khỏi kho chọn. Index một-nháp-mỗi-lineage cũng phải tính thêm `PENDING`.
5. Thêm "Kích hoạt hàng loạt" cho chủ nhiệm môn.

## 6. Cấu hình trừ điểm theo out tab

**Hiện trạng:** chưa có. `QuizSettings` không có trường nào cho việc này (`QuizSettings.java:4`), và điểm chỉ lấy từ chấm câu (`PracticeSupport.java:128-135`).

**Phải chốt trước khi làm, vì có 3 chỗ xung đột:**

- Yêu cầu hiện tại ghi rõ không tự trừ điểm: `requirements.md:343`, `BR-U08-53`, `stories.md:1379`, và dòng chữ trên giao diện `TeacherQuizSections.tsx:85`.
- Quiz chỉ là luyện tập và không vào sổ điểm (D01, BR-U10-12). Trừ điểm hiện chỉ làm đổi con số hiển thị, trừ khi Quiz được đưa vào sổ điểm, và đó là thay đổi lớn hơn.
- **Rủi ro lớn nhất:** tín hiệu do trình duyệt tự báo. Người chặn được script có 0 lần rời và không bị trừ, trong khi người trung thực bị thông báo điện thoại hoặc khóa màn hình lại bị trừ. Vì vậy không nên trừ tự động mà không có quyền xem xét của giảng viên.

**Hướng xử lý nếu đồng ý làm**

1. Cấu hình theo quiz, lưu trong `settings` (đang là jsonb nên không cần đổi cấu trúc cột): số lần được miễn, điểm trừ mỗi lần, mức trừ tối đa, thời gian rời tối thiểu để tính.
2. Công thức: `điểm trừ = min(mức tối đa, max(0, số lần − số lần miễn) × điểm mỗi lần)`; `điểm cuối = max(0, điểm gốc − điểm trừ)`. Ví dụ: miễn 1 lần, 0,5 điểm mỗi lần, điểm gốc 8/10, rời 4 lần thì trừ 1,5, còn 6,5.
3. Chỉ đếm khoảng rời chắc chắn (`IntervalCertainty.KNOWN`) và đủ thời gian tối thiểu, ví dụ 5 giây, để loại trường hợp tải lại trang.
4. Tính tại `PracticeSupport.submit` để áp dụng cho cả ba cách nộp. Lưu thêm `raw_score`, `penalty_points`, `leave_count`, `activity_state` vào `quiz_attempts`, vì sự kiện gốc bị xóa sau 1 ngày. `score` là điểm cuối nên điểm cao nhất tự đúng.
5. Sửa lỗi mất sự kiện cuối (#3) trước: gửi hết sự kiện rồi mới nộp.
6. Hiện quy tắc cho sinh viên trước khi làm; kết quả ghi rõ điểm gốc, số lần và điểm trừ. Tuân theo cờ ẩn điểm hiện có.
7. Giảng viên được bỏ trừ điểm kèm lý do, có audit. "Mất tín hiệu" không tự trừ, chỉ gắn cờ để giảng viên xem.
8. Cập nhật FR-039, BR-U08-53, stories, dòng chữ trên giao diện và schema MCP. Snapshot cũ không có cấu hình thì không trừ.

## 7. Bỏ bước duyệt quiz

**Hiện trạng:** `DRAFT → REVIEWED → OPEN`, người bấm Duyệt chính là người soạn (BR-U10-21: "tự duyệt"). Bước này không thêm kiểm soát nào: Duyệt và Phát hành chạy cùng một hàm kiểm tra (`QuizPublicationService.java:64,83`), và sửa bất cứ gì lại quay về nháp.

**Hướng xử lý**

1. `publish` nhận trực tiếp từ `DRAFT`, vẫn chạy `requireReviewable`. Bỏ endpoint `/review`, `Quiz.reviewed()` và `QuizStatus.REVIEWED`.
2. Giao diện: bỏ nút Duyệt (`QuizDetailPage.tsx:170-173`), nút Phát hành hiện từ nháp, kèm xác nhận tóm tắt số câu và tổng điểm. Bỏ tab "Đã duyệt" (`SubjectQuizListPage.tsx:17,28`), nhãn (`labels.ts:48-49`) và 3 khai báo kiểu (`quizzes`, `quizpractice`, `materials`).
3. Migration: chuyển `REVIEWED` về `DRAFT`, sửa `ck_quizzes_status` và `ux_quizzes_open_new_version`. Sau đó index vẫn đúng vì mỗi nguồn vốn chỉ có tối đa một bản đang soạn.
4. Cập nhật enum MCP (`QuizMcpQueryTools.java:14`), OpenAPI (`materials.yaml:2536`), `JdbcQuizRepository.java:114`, BR-U10-20/21/22/24 và các test (`QuizScreens.test.tsx`, `QuizTest`, `QuizServicesTest`, `QuizControllersTest`, `JdbcQuizRepositoryIntegrationTest`, `InMemoryQuizStore`). Giữ nguyên bản ghi audit `QUIZ_REVIEWED` cũ.

Assignment cũng có `REVIEWED` riêng và không nằm trong góp ý lần này.
