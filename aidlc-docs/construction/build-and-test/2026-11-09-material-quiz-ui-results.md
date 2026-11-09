# Học liệu và soạn quiz tại chỗ — kết quả 2026-11-09

Nguồn: [đề xuất đã duyệt](../../inception/requirements/2026-11-09-material-quiz-ui-proposal.md), [kế hoạch thực hiện](../plans/code-generation-plan/2026-11-09-material-quiz-ui.md). Giữ nguyên thay đổi nhập Excel từ yêu cầu trước; không commit/push/deploy.

## Hành vi đã sửa

- Học liệu có `enabled` độc lập ACTIVE/ARCHIVED, mặc định true cho dữ liệu/yêu cầu tạo cũ. Disabled ẩn với sinh viên; kiểm quyền tại danh sách, xem trực tiếp, cấp token và đổi cả token cũ lấy nội dung. Staff được quản lý/bật lại theo quyền. Tài liệu bị tắt không được dùng làm nguồn AI mới; giữ version/lịch sử.
- Mô tả học liệu và quiz có editor nhỏ: đậm/nghiêng/gạch chân, căn lề, danh sách, indent và undo/redo. Lưu `descriptionDocument`/`instructionsDocument` có giới hạn, đồng thời giữ plain text cũ. Render React an toàn, không chèn HTML chưa lọc. Format được giữ qua đổi cấu hình, clone/reopen, chuyển tới sinh viên và cập nhật plain text tương đương từ client cũ.
- Soạn tay, chọn ngân hàng và AI nằm trong khung Câu hỏi; sửa câu ở ngay thẻ câu đó. Đang lưu thì khóa đổi chế độ. Lỗi giữ form, retry AI giữ yêu cầu/số câu/tài liệu; cảnh báo trước khi bỏ bản soạn khi chuyển mode/liên kết/đóng trang/Back–Forward.
- Dropdown nguồn AI có tìm kiếm, chọn nhiều, chip bỏ chọn, bộ đếm 10 và lý do nguồn không dùng được. Giữ việc hệ thống tự chọn nguồn API. Draft lưu `sourceWarnings` khi tài liệu bị cắt hoặc hết ngân sách; toàn bộ nguồn hết nội dung thì FAILED/MATERIAL_TEXT_UNAVAILABLE trước khi gửi provider. Không coi URL YouTube là transcript.
- Theo ảnh bổ sung của người dùng, sửa dấu bullet/số tràn viền mô tả: inline margin thụt lề không còn ghi đè marker gutter; editor chừa 2rem cho marker, viewer dùng padding tương ứng. Không cắt dấu bằng overflow để che lỗi. Yêu cầu bổ sung tiếp theo bỏ nhãn tiếng Anh: form tạo/sửa, trang chi tiết và nhãn danh sách chỉ hiện “Bật/Tắt”; giữ nguyên tên trường API.

## Kiểm chứng

| Phạm vi | Kết quả |
|---|---|
| Backend clean kết hợp material/quiz/files và integration được yêu cầu | 533 test / 48 suite, 0 failures/errors/skips |
| Sau sửa giữ format quiz | 104 test qua, 0 skip |
| Sau sửa giữ format material | 119 test qua, 0 skip |
| Quiz JSONB insert/update/clone thật | 10 test qua, 0 skip |
| Required PostgreSQL guard | 5 suite bắt buộc và 57 traced cases qua |
| Backend package `-DskipTests` | JAR executable tạo thành công |
| Toàn bộ frontend trước sửa gutter cuối | 836 test / 124 file qua |
| Sau sửa gutter | 82 test / 10 file liên quan qua; lint hai file, typecheck độc lập và production build qua |
| Sau đổi nhãn Bật/Tắt cuối cùng | 55 test học liệu / 8 file qua; thay đổi chỉ copy, không chạy lại full build/suite |
| Full frontend lint | Qua; thay đổi copy sau đó được scoped tests kiểm tra |
| Source/test/OpenAPI diff check, parse OpenAPI JSON | Qua |

Các scope backend chồng nhau, không cộng thành tổng mới. Sau test red tái hiện, đã sửa bốn finding Important từ reviewer: mất format khi lưu cấu hình, mất bản soạn khi đổi mode giữa lúc lưu, retry AI xóa input, và Back/Forward không hỏi trước khi rời. Reviewer đọc lại các bản sửa không tìm thấy Important còn lại trong các đường đó.

Log: `.tmp/material-quiz-combined-clean.log`, `material-quiz-focused-final.log`, `material-presentation-final.log`, `quiz-rich-jdbc-final.log`, `material-quiz-required-guard.log`, `material-quiz-backend-package.log`, `material-quiz-frontend-final-tests.log`, `material-quiz-frontend-final-lint.log`, `material-quiz-border-regression.log`, `material-vietnamese-labels.log`. Bản build sau gutter dùng `material-quiz-frontend-build-final.log`; không lấy kết quả của lần nhập Excel làm bằng chứng cho tính năng này.

## Áp dụng và giới hạn

Có hai migration mới:

- `materials/V20261109_2200__material_visibility_and_formatted_description.sql`: enabled mặc định true, JSONB mô tả nullable, index tra file cho kiểm quyền tải.
- `quizzes/V20261109_2201__quiz_formatted_instructions.sql`: JSONB mô tả quiz nullable.

Chỉ đã thử migrations trên database tạm. Chưa chạy migration hoặc khởi động lại backend đang hoạt động; khi áp dụng bản backend mới cần cho Flyway chạy hai migration trước khi tự thử UI. Artifact backend: `backend/target/platform-backend-0.1.0-SNAPSHOT.jar`. Artifact frontend: `frontend/.next/`.

Người dùng yêu cầu tự test UI, nên dừng kiểm tra trình duyệt; không có nghiệm thu hình ảnh/mobile/E2E. Chưa gọi provider AI thật hoặc tạo dữ liệu production. Chưa chạy toàn bộ backend ngoài các phạm vi đã nêu. Không thêm dependency, không đổi cách phát hành quiz hay xuất CSV.

Security: validation structured text, phân quyền, version CAS/transaction, direct URL và token redemption được kiểm trong test liên quan; không chứng nhận bảo mật toàn hệ thống. Resiliency: giữ input khi lỗi, khóa chuyển mode khi request đang chạy, báo thiếu/cắt nguồn rõ. PBT tắt theo cấu hình. Fallback browser cũ khi hủy Back/Forward có thể bỏ forward path bằng pushState; bản soạn hiện tại được giữ.
