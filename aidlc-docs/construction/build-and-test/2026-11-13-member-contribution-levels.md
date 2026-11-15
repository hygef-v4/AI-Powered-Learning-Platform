# Kiểm chứng 5 mức đóng góp thành viên — 2026-11-13

## Phạm vi đã triển khai

- Trường contributionLevel nullable trong Evaluation JSONB hiện có; không migration.
- Năm enum có tên rõ; JSON số/thứ tự và giá trị lạ bị từ chối. MemberSave constructor
  cũ vẫn hoạt động, dữ liệu không có trường mặc định chưa đánh giá. Bỏ trường hoặc
  null khi lưu nghĩa là chưa đánh giá; caller mới gửi null để xóa mức.
- Endpoint lưu điểm thành viên giữ kiểm quyền, ARCHIVED, submission và version;
  lưu mức/điểm/lý do trong một transaction, audit mức trước/sau kể cả điểm không đổi.
- Mức không tính điểm. Điểm/căn cứ/mức cá nhân giữ khi chấm lại nhóm; nộp lại xóa.
- Giao diện giữ shared Select/Field/Button, hướng dẫn thu gọn, không tự chọn mức.
  Căn cứ lấy từ đánh giá cá nhân đã lưu, không lấy nhầm ghi chú điểm nhóm.
- DTO sinh viên chỉ trả mức/căn cứ MEMBER của chính mình sau công bố; sửa bản
  công bố hiển thị bản mới khi tải lại. Không đổi MCP, Excel hoặc gọi AI.

## Kết quả thực chạy

| Kiểm tra | Kết quả | Log |
|---|---|---|
| Regression UI trước sửa | 6 fail đúng chức năng thiếu, 1 control pass | .tmp/member-levels-red.log |
| UI mới sau sửa | 7/7, hai file | .tmp/member-levels-ui.log |
| Toàn feature grading frontend | 199/199, 22 file | .tmp/member-levels-frontend.log |
| API payload sau bổ sung case | 4/4 (ba case đã nằm trong 199 ở trên) | .tmp/member-levels-api-test.log |
| Backend model/JSON/repository mapping/service/group/privacy | 55/55, không skip | .tmp/member-levels-backend.log |
| ESLint feature grading và test | Exit 0 | .tmp/member-levels-lint.log |
| TypeScript toàn source/test, loại cache Next cũ | Exit 0 | .tmp/member-levels-types-isolated.log |

Backend: EvaluationTest, EvaluationJsonContractTest, EvaluationMappingTest,
GradingServiceTest, GradebookServiceTest, GroupEvaluationTest. Kiểm chứng gồm
5 mức/legacy round-trip, invalid enum, audit chỉ đổi mức, không đổi điểm, xóa mức,
stale version, ARCHIVED/quyền từ chối, giữ override và reset, nháp/công bố/cập nhật
công bố và không lộ thành viên khác. Tests dùng model/Jackson/JDBC document mapper
thật, service dependency mock; không coi đây là kiểm thử DB transaction live.

Typecheck thông thường vẫn fail ở .next/types/validator.ts do route đã xóa
(admin/mcp, ai/usage, nested submissions). Dùng config tạm đặt trong frontend,
include toàn src/tests và .next-ai-chat-files/types hợp lệ, incremental=false;
config đã xóa sau khi kiểm. Không xóa cache hoặc đổi tsconfig/next config sản phẩm.

## Rà soát và giới hạn

Đối chiếu caller saveMemberGrade, hai nơi tạo StudentGradeView, JSONB mapper,
inherit/reset và UI hiển thị. Security: validation enum/cỡ chuỗi, quyền và bản nháp
được giữ và kiểm bằng test. Resiliency: không thêm phụ thuộc ngoài, khóa/version và
transaction hiện hữu giữ nguyên. PBT disabled theo cấu hình. Không phát hiện vấn đề
còn lại trong phạm vi diff; không phải audit toàn hệ thống.

Người dùng tự test UI theo yêu cầu. Chưa chạy browser/production build/full backend
hoặc full frontend toàn dự án trong lượt này; không gọi AI, ghi điểm thật, restart,
commit, push hay deploy. Cần backend nạp mã mới và frontend mới để test trường mới;
backend cũ có strict input sẽ từ chối contributionLevel.

Luồng tự thử: Teacher → Chấm điểm → bài nhóm đã lưu điểm → Điểm thành viên →
chọn mức, sửa điểm/căn cứ tùy ý → Lưu điểm thành viên → tải lại. Mở Cách đánh giá
để đọc hướng dẫn; sau công bố, đăng nhập đúng sinh viên để xem mức/căn cứ riêng.
