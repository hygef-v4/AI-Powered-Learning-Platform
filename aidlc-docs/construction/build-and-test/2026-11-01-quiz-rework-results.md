# Kết quả làm lại phần Quiz — 2026-11-01

Nhánh `feat/quiz-rework-2026-11-01`, tách từ `origin/staging`. Đã commit trên nhánh này, chưa push, chưa triển khai. [Roadmap](../plans/code-generation-plan/2026-11-01-quiz-rework.md), [đánh giá](../../inception/requirements/2026-11-01-quiz-feedback-assessment.md), [câu hỏi](../../inception/requirements/2026-11-01-quiz-rework-questions.md).

## Đã làm

| # | Góp ý | Kết quả |
|---|---|---|
| 1 | Tài liệu tham khảo thừa | Bỏ gắn tài liệu vào quiz và mục "Quiz gắn với học liệu"; popup AI đổi nhãn thành "Tài liệu nguồn cho AI". Cột DB giữ lại, không còn dùng |
| 2 | Sinh đề theo tag | Chế độ "Sinh đề mới cho mỗi lượt làm": quy tắc thẻ, độ khó, số câu, điểm; gợi ý thẻ kèm số câu trong kho; xem thử đề mẫu; thiếu câu thì chặn phát hành |
| 3 | Out tab | Thêm `blur`/`focus` cho Quiz; cờ "Không nhận được tín hiệu", "Mất tín hiệu một phần"; chụp số liệu vào lượt khi nộp; báo đúng khi dữ liệu hết hạn |
| 4 | Mỗi câu một trang | Xem kết quả và xem bài của giảng viên hiện từng câu; bảng số có màu; giữ câu đang xem ở `?q=` |
| 5 | Chống trùng và duyệt | Giống hệt thì chặn, gần giống thì cảnh báo; giảng viên đề xuất, chủ nhiệm môn duyệt hoặc từ chối, từng câu và hàng loạt; thông báo hai chiều |
| 6 | Trừ điểm | Cấu hình mức gợi ý theo quiz; hệ thống không tự trừ; giảng viên trừ (được chọn số điểm) hoặc không trừ từng lượt, kèm lý do, đổi được; sinh viên thấy quy tắc, quyết định và lý do |
| 7 | Bỏ duyệt quiz | Bỏ `REVIEWED`; phát hành thẳng từ nháp, có hộp xác nhận |

Phát sinh khi kiểm tra trên trình duyệt:

- **Slot `QuizDetailSection` đăng ký nhưng chưa từng render**, nên trước đây giảng viên không có giao diện xem lượt làm. Đã render trên Quiz Detail; nếu không, #3 và #6 không dùng được. Báo cáo đánh giá đã ghi sai điểm này và đã được đính chính.
- **Phần Quiz của lớp đã gỡ khỏi trang lớp từ 29/10**, nên mục đề xuất câu được đặt ở tab Học liệu của trang giảng dạy, thay vì tab Bài tập như dự định. Link thông báo sửa theo.

Sửa thêm: chặn hai phiên bản của cùng một câu trong một quiz; kích hoạt câu hàng loạt; bỏ giới hạn MCP lệch 20 và 10 tài liệu (MCP không còn nhận tài liệu cho Quiz).

## Quyết định của người dùng

Ban đầu [file câu hỏi](../../inception/requirements/2026-11-01-quiz-rework-questions.md) chưa được điền nên các phương án đề xuất được áp dụng mặc định. Ngày 2026-11-02 người dùng chốt: **hệ thống chỉ gợi ý mức trừ, giảng viên quyết định từng lượt**, thay cho tự trừ; **Quiz vẫn không vào sổ điểm**; các câu còn lại giữ nguyên.

## Kiểm chứng

| Kiểm tra | Kết quả |
|---|---|
| Backend `mvn -B clean verify` trên code cuối | 1.712 test đạt, 0 lỗi, 0 bỏ qua; gồm integration test PostgreSQL thật |
| `scripts/assert-required-backend-tests.ps1` | Đạt |
| Frontend vitest trên code cuối | 677 test đạt, 101 file |
| Frontend typecheck, lint, build | Typecheck đạt; lint 0 lỗi, 1 cảnh báo; build đạt |
| API đầu-cuối trên stack Docker riêng, migrate từ đầu | 56 kiểm tra đạt trên code cuối, gồm đủ 7 mục: nộp không tự trừ, sinh viên không thấy mức gợi ý, thiếu lý do, trừ quá điểm gốc, lượt không có gợi ý, sinh viên tự quyết định bị chặn, trừ số điểm tự chọn, đổi quyết định |
| Trình duyệt trên stack đó | Sinh viên: làm bài, chuyển câu, `?q=`, sự kiện `blur`/`focus`, quy tắc trừ điểm, kết quả từng câu. Giảng viên: quy tắc sinh đề, đề mẫu, cột rời trang, cờ tín hiệu, bài từng câu, đề xuất. Chủ nhiệm môn: lọc từ URL, duyệt hàng loạt, câu trùng. Không tràn ngang ở 1024 px. Sau khi đổi quyết định, chỉ kiểm lại luồng trừ điểm trên code cuối: giảng viên thấy nhãn chờ, mức gợi ý, trừ 1 điểm kèm lý do; sinh viên thấy 5 / 6, lý do và quy tắc mới |
| Log và dữ liệu | Không có lỗi ở backend; không còn câu thiếu hash; không có migration lỗi |

Các phần khác trong dòng trình duyệt được kiểm ở đợt trước và không đổi code. Stack thử dùng project riêng `quiz-rework-e2e`, tài khoản thử riêng, không đụng database local của người dùng, và đã được xóa sau khi kiểm tra. Việc chuyển `REVIEWED` về `DRAFT` được kiểm bằng `QuizPublicationMigrationIntegrationTest`.

## Giới hạn

- **Tín hiệu rời trang vẫn có thể bị né.** Chặn request, giả sự kiện hoặc dùng thiết bị thứ hai đều không bắt được chắc chắn; chỉ có cờ khi thiếu tín hiệu. Người cố tình gửi sự kiện giả sẽ không có mức gợi ý. Biện pháp chính là đề khác nhau và giới hạn thời gian.
- **Có thể báo nhầm.** `blur` tính cả lúc bấm sang thanh địa chỉ hoặc một cửa sổ thông báo; thời gian rời tối thiểu chỉ giảm bớt, nên mức trừ chỉ là gợi ý, giảng viên quyết định. Chưa thử trên điện thoại thật hoặc hai màn hình thật.
- **Trừ điểm chỉ đổi điểm của Quiz luyện tập,** vì Quiz chưa vào sổ điểm.
- **Sinh viên không nhận thông báo khi giảng viên quyết định;** chỉ thấy khi mở lại kết quả. Lượt tắt hiện điểm thì sinh viên không thấy dòng này.
- **Không có nơi gom lượt chờ quyết định;** giảng viên xem theo từng quiz, qua nhãn "Chờ quyết định" trên danh sách lượt làm.
- **Không có thoát toàn màn hình,** vì ứng dụng không có chế độ làm bài toàn màn hình. Khác với đề xuất trong báo cáo đánh giá.
- **Gần giống so bằng trigram trong Java,** không dùng `pg_trgm`. Không bắt được câu viết lại khác hẳn từ ngữ. Chưa đo tốc độ với ngân hàng rất lớn, cũng chưa đo tải khi nhiều sinh viên bắt đầu sinh đề cùng lúc.
- **Trình duyệt vẫn nhận cả đề ngay từ đầu;** mỗi câu một trang chỉ là hiển thị.
- **Assignment không đổi:** vẫn có `REVIEWED`, không gợi ý trừ điểm, tracker không bật `blur`.
- **Sơ đồ:** hai file draw.io chỉ sửa ghi chú trạng thái; ảnh `exports/BF-06.png` và các bước vẽ của BF-06 chưa vẽ lại.

## Cần làm khi triển khai

1. Migration chuyển các quiz đang `REVIEWED` về `DRAFT`; người soạn cần bấm Phát hành lại.
2. Backfill hash chạy một lần khi khởi động; sau đó xem báo cáo "Câu trùng" trên Question List.
3. Agent MCP đang giữ schema cũ cần làm mới; `create_quiz_draft` không còn nhận `referenceMaterialIds`.
4. Trước khi xóa cột `reference_material_ids`, kiểm: `select count(*) from quizzes where jsonb_array_length(reference_material_ids) > 0`.
