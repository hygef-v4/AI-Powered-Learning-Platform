# Roadmap làm lại phần Quiz — 2026-11-01

Hoàn thiện 7 mục trong [báo cáo đánh giá](../../../inception/requirements/2026-11-01-quiz-feedback-assessment.md). Nhánh `feat/quiz-rework-2026-11-01`, tách từ `origin/staging`. Người dùng yêu cầu làm cả 7 mục mà chưa điền [file câu hỏi](../../../inception/requirements/2026-11-01-quiz-rework-questions.md), nên áp dụng các phương án đề xuất (ghi trong file đó).

## Quyết định áp dụng

| Câu | Quyết định |
|---|---|
| Q1 | Bỏ gắn tài liệu vào quiz và mục "Quiz gắn với học liệu"; giữ tài liệu nguồn cho AI |
| Q2 | Người dùng chốt 02/11, thay mặc định tự trừ: tính mức trừ gợi ý theo cấu hình của từng quiz, giảng viên quyết định từng lượt kèm lý do |
| Q3 | Quiz vẫn chỉ luyện tập, không vào sổ điểm |
| Q4 | Sinh đề từ ngân hàng của môn, chỉ câu đang dùng |
| Q5 | Thiếu câu: chặn phát hành; kho thiếu sau đó thì từ chối bắt đầu lượt và cảnh báo khi ngưng câu |
| Q6 | Giống hệt thì chặn, gần giống thì cảnh báo; chủ nhiệm môn duyệt câu giảng viên đề xuất |
| Q7 | Câu riêng của quiz không cần duyệt |

Điều chỉnh so với báo cáo sau khi đọc thêm code:

- **Không dùng `pg_trgm`.** Role `migrator` chỉ có quyền trên schema, không có `CREATE` trên database (`infra/postgres/init/01-roles.sh`), nên `create extension` có thể làm hỏng migration ở môi trường đã có. So gần giống bằng trigram trong Java trên nội dung đã chuẩn hóa của từng môn.
- **Không làm "thoát toàn màn hình".** Ứng dụng không có chế độ làm bài toàn màn hình, nên chưa có gì để phát hiện. Tín hiệu phụ chỉ gồm `blur`/`focus`.
- **Chưa xóa cột `reference_material_ids`.** Giữ cột để dễ quay lại; xóa bằng migration riêng sau khi kiểm dữ liệu staging.

## Thứ tự và phụ thuộc

Thứ tự: #7 → #1 → #4 → #5 → #2 → #3 + #6 → tài liệu → kiểm thử toàn bộ. #2 dùng hash của #5 để không bốc hai câu giống nhau. #6 cần phần chụp số liệu của #3.

## Đợt 1 — Dọn luồng cũ

### #7 Bỏ bước duyệt quiz

- [x] Migration: `REVIEWED` về `DRAFT`, sửa `ck_quizzes_status`, `ux_quizzes_open_new_version`
- [x] Backend: bỏ `QuizStatus.REVIEWED`, `Quiz.reviewed`, `QuizPublicationService.review`, endpoint `/review`; `publish` nhận từ `DRAFT` và vẫn kiểm đủ điều kiện
- [x] MCP và OpenAPI: bỏ `REVIEWED` khỏi enum của Quiz
- [x] Frontend: bỏ nút Duyệt, tab "Đã duyệt", nhãn và khai báo kiểu; thêm xác nhận trước khi phát hành
- [x] Cập nhật test

### #1 Bỏ tài liệu gắn vào quiz

- [x] Backend: bỏ `referenceMaterialIds` khỏi `Quiz`, API, `QuizView`, API sinh viên, MCP, `QuizRules`; bỏ `listByMaterial`, `byMaterial` và endpoint liên quan
- [x] Frontend: bỏ `MaterialQuizList`, slot `quizMaterialSections`, trường trong form và kiểu; đổi nhãn popup AI thành "Tài liệu nguồn cho AI"
- [x] Giữ `QuizReferenceMaterials` cho AI; cập nhật test

### #4 Mỗi câu một trang

- [x] Tạo `QuestionPager` dùng chung
- [x] `ResultQuestions` hiện từng câu, bảng số câu có màu đúng/sai/bỏ trống; áp dụng cho sinh viên và giảng viên
- [x] Lưu câu đang xem vào URL `?q=` ở màn làm bài và màn kết quả (thay thế lịch sử)
- [x] Cập nhật test

## Đợt 2 — Ngân hàng câu hỏi

### #5 Chống trùng và duyệt câu của giảng viên

- [x] Migration: cột `content_hash`, `normalized_stem`; trạng thái `PENDING`, `REJECTED`; bảng `question_reviews`; sửa check và index một-bản-đang-mở
- [x] `QuestionFingerprint`: chuẩn hóa, hash, so trigram; repository tự ghi khi lưu nội dung; backfill dữ liệu cũ khi khởi động
- [x] Chặn trùng khi tạo, sửa, kích hoạt, nhập tệp, nhận đề xuất AI; cảnh báo câu gần giống khi tạo và sửa
- [x] Đề xuất: giảng viên gửi câu riêng của quiz vào ngân hàng, xem danh sách, sửa rồi gửi lại, rút đề xuất
- [x] Duyệt: chủ nhiệm môn duyệt hoặc từ chối từng câu và hàng loạt; kích hoạt hàng loạt
- [x] Sửa xếp hạng phiên bản trong `searchBank`, `activeSql`; chặn hai phiên bản cùng lineage trong một quiz
- [x] Thông báo hai chiều qua U14, audit
- [x] Frontend: lọc "Chờ duyệt", cảnh báo gần giống, báo cáo câu trùng, nút đề xuất, danh sách đề xuất của giảng viên ở tab Học liệu
- [x] Test

## Đợt 3 — Sinh đề

### #2 Sinh đề theo tag

- [x] Migration: `quizzes.paper_mode`, bảng `quiz_draw_rules`
- [x] `QuizPaperPlanner`: phân bổ theo ghép cặp, ưu tiên câu chưa gặp, không lặp lineage hoặc nội dung
- [x] U10: lưu quy tắc, danh sách tag kèm số câu, xem thử đề mẫu, kiểm đủ câu khi phát hành, sao chép quy tắc khi copy, cảnh báo khi ngưng câu
- [x] Contract U10/U11: `drawPaper`, đề và chấm điểm theo danh sách câu của lượt
- [x] U11: lưu câu và điểm vào snapshot; lượt cũ vẫn đọc theo `quiz_items`
- [x] Số câu và điểm tối đa của quiz sinh đề tính theo quy tắc
- [x] Frontend: chọn chế độ đề, bảng quy tắc, tổng câu và điểm, xem thử đề mẫu
- [x] Test

## Đợt 4 — Tín hiệu rời trang và gợi ý trừ điểm

### #3 Tín hiệu rời trang

- [x] Tracker: tùy chọn `blur`/`focus` chỉ cho Quiz; gửi hết sự kiện trước khi nộp
- [x] Gắn cờ "Mất tín hiệu" ở server
- [x] Chụp số liệu vào lượt làm khi nộp; sửa thông báo hết hạn dữ liệu của Quiz
- [x] Hiện số lần rời và cờ trên danh sách lượt làm của giảng viên; render slot `QuizDetailSection` vốn chưa từng hiện

### #6 Cấu hình gợi ý trừ điểm

- [x] `LeavePenalty` trong `QuizSettings`: số lần miễn, điểm mỗi lần, mức tối đa, thời gian rời tối thiểu
- [x] Migration `quiz_attempts`: điểm gốc, mức gợi ý, điểm đã trừ, số lần rời, trạng thái tín hiệu, quyết định của giảng viên
- [x] Tính mức gợi ý khi nộp cho cả ba cách nộp, điểm giữ nguyên; chỉ đếm khoảng rời chắc chắn và đủ thời gian
- [x] API giảng viên quyết định trừ hoặc không trừ, có audit
- [x] Frontend: ô cấu hình, báo quy tắc trước khi làm, hiện quyết định và lý do, khu vực quyết định của giảng viên
- [x] Test

## Đợt 5 — Tài liệu và kiểm thử

- [x] Requirements: FR-039 trong `requirements.md`, `stories.md`, `personas.md`, `decision-register.md`, góp ý 29/10
- [x] Business rules và thiết kế U08, U10, U11; as-built; `docs/guides/mcp-clients.md` nếu liên quan
- [x] Kết quả và giới hạn: `construction/build-and-test/2026-11-01-quiz-rework-results.md`
- [x] Backend: `mvn -B clean verify`
- [x] Frontend: lint, typecheck, vitest, build
- [x] Chạy thử API đầu-cuối và trình duyệt trên stack Docker riêng `quiz-rework-e2e`, đã xóa sau khi kiểm tra

Kết quả và giới hạn: [results](../../build-and-test/2026-11-01-quiz-rework-results.md). Không commit, push hay triển khai nếu người dùng chưa yêu cầu.
