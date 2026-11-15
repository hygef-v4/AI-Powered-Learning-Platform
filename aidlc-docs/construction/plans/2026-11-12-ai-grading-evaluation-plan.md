# Kế hoạch: bộ đánh giá chất lượng AI chấm bài

**Ngày:** 2026-11-12
**Nguồn:** mục J4/P1-3 trong [báo cáo đối chiếu phỏng vấn giảng viên](../../inception/requirements/2026-11-12-lecturer-interview-gap-analysis.md). Người dùng chọn "Làm bộ đánh giá AI".
**Phạm vi:** chỉ thêm công cụ kiểm thử trong `backend/src/test`. Không đổi chức năng, API, schema hay prompt của app.

## Mục tiêu

Đo chất lượng AI chấm theo các tiêu chí giảng viên nêu (độ chính xác, bịa, lạc đề, trích dẫn nguồn…) bằng **đúng pipeline của app**: `GradingPromptBuilder`, `JsonSchemaChecker`, `GradingProposalValidator`, `HttpProviderGateway`.

## Các bước

- [x] Đọc pipeline chấm AI: prompt, schema provider, kiểm schema, gộp phần trống, chuẩn hóa, validate.
- [x] Thiết kế chỉ số, ngưỡng đạt và định dạng bộ dữ liệu (JSON, khóa ngắn đổi thành UUID cố định).
- [x] Viết harness `edu.aiplatform.grading.eval`: nạp dữ liệu, chạy pipeline, tính chỉ số, xuất báo cáo Markdown/JSON.
- [x] Chế độ chạy: `live` (gọi provider thật, lưu bản ghi), `replay` (chấm lại từ bản ghi, không tốn token). Mặc định bỏ qua trong `mvn test`.
- [x] Bộ dữ liệu mẫu tổng hợp: 1 đề (đặt sân cầu lông), 8 bài với điểm kỳ vọng từng mục.
- [x] Test luôn chạy: kiểm dữ liệu, provider "đáp án" phải đạt tuyệt đối, provider "bịa"/sai định dạng phải bị bắt, công thức chỉ số.
- [x] Chạy test: Docker Desktop không khởi động được engine nên chạy bằng `javac` Java 21 (`--release 17`) và JUnit Platform Launcher, dùng thư viện của boot jar và `~/.m2`. `GradingEvalHarnessTest` 8/8 đạt, `GradingAiEvaluationTest` bỏ qua khi không bật. Replay từ bản ghi giả lập sinh `report.md` đúng định dạng. **Chưa chạy qua Maven/surefire.**
- [x] Lần live đầu với Claude (`claude-sonnet-5-5`, 2026-11-12): 24/24 lời gọi bị HTTP 401 (key không hợp lệ), không đo được chỉ số. Đã sửa: báo cáo ghi "không đo được" thay vì "mọi chỉ số đạt", dừng ngay sau lần bị từ chối đầu tiên, test live fail khi provider từ chối.
- [x] Chạy live với Gemini 2.5 Flash (2026-11-13): 9/24 lần hợp lệ, 9,35/10. Phát hiện 2 lỗi app (cắt token khi Gemini suy nghĩ; nhãn `<<include>>` bị xóa). Harness sửa phân loại lỗi 503 và replay; 9/9 test đạt. [Kết quả](../build-and-test/2026-11-13-ai-grading-eval-gemini-results.md).
- [x] Sửa F1/F2 trong app (người dùng duyệt 2026-11-13): giới hạn `thinkingBudget` của Gemini ở 1/4 trần token; giữ nhãn `<<include>>`/`<<extend>>` và đánh dấu cạnh nét đứt trong tóm tắt sơ đồ. 47/47 test liên quan đạt (Java 21, chưa qua Maven).
- [x] Chạy lại với Gemini (2026-11-15): 0 lần hết token, `d-dashed` chấm đúng, kappa 0,85. Bài chèn lệnh đo được 1 lần, đạt. Phát hiện F6 (output vượt schema 3/14).
- [x] Chạy lại riêng bài 04, 05, 06, 08 (2026-11-15): bài lạc đề đo được, chỉ 1/3 lần nhận ra (F7). Bài 04, 08 vẫn lỗi mạng/503.
- [x] Sửa F6 (output vượt `maxItems`, bằng chứng rỗng) và F7 (không kiểm bài đúng đề) trong app (người dùng duyệt 2026-11-15). 76/76 test liên quan đạt.
- [x] Chạy lại cả bộ với Gemini (2026-11-15): F6 đã hết (11/11 đúng định dạng), kappa 0,89. 13 lần bị 429 vì hết hạn mức trong ngày.
- [ ] Khi hạn mức được đặt lại, chạy lại bài 05, 07, 08 để kiểm F7.
- [x] Viết hướng dẫn chạy và cập nhật audit.

## Giới hạn

- Dữ liệu mẫu do nhóm tự soạn (`labelSource: SYNTHETIC_TEAM`), **chưa được giảng viên xác nhận**. Kết quả chỉ dùng để thử pipeline; cần thay bằng 20–30 bài thật đã ẩn danh và được giảng viên chấm.
- Chạy `live` tốn token của key người chạy; harness không dùng quota/kết nối trong app.
