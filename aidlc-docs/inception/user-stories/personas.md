# Personas — 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

| Persona | Nhu cầu và trách nhiệm | AI access |
|---|---|---|
| P-STUDENT | Assignment tài liệu SWR/SWD cá nhân/nhóm, diagram tùy chọn, Quiz; autosave/nộp, sửa theo phản hồi | API/agent riêng, không quota chung; policy từng bài và ownership |
| P-TEACHER | Soạn/giao/nhận/chấm bài lớp, rubric, phản hồi sớm và công bố điểm | API chung có quota hoặc API/MCP cá nhân; không auto-fallback |
| P-SUBJECT-MANAGER | Tài nguyên/ngân hàng môn, triển khai lớp, phân công/ghi danh; bài cá nhân cấp môn | R2 cho môn; R4 khi dạy/chấm lớp; quota educator trong quyền |
| P-ADMIN | Accounts/subjects/Settings, institutional connection/quota/audit, thống kê | Test synthetic connection, không đọc bài hay key cá nhân |

## Bối cảnh và pain points cần khảo sát

Giảng viên/sinh viên SWR/SWD và môn tương tự: cách giao/làm/nộp/chấm tài liệu/sơ đồ, version, phối hợp nhóm, feedback sớm, công cụ agent đang có. Đây là giả thuyết khảo sát, không dữ liệu đã xác minh. Questionnaire/interview phải ghi nguồn và xác nhận AS-IS trước Report 1.

## Quyền và giả định

- R1 dữ liệu cá nhân/kết nối/feedback; R2 môn; R3/R4 lớp được giao; R5 enrollment. MCP không mở rộng quyền.
- Student không được gán shared tokens; mọi feedback theo teacher policy, không điểm official.
- Admin không dạy/chấm; Subject Manager chỉ chấm lớp mình được giao. Không role mới.
- Tổ chức dự kiến có provider API để Admin cấu hình; đây là giả định vận hành, không khẳng định trường đã cung cấp key.
- Không mọi giảng viên đã có agent/key; manual workflow vẫn hoàn chỉnh khi chưa kết nối.

Stories và mapping: [stories.md](stories.md), [unit map](../application-design/unit-of-work-story-map.md).
## Bổ sung trách nhiệm theo hội đồng — 2026-10-13

Admin theo dõi trạng thái backup và cấu hình lịch/retention; quyền này không cấp quyền tải dữ liệu học vụ. Người vận hành giữ secrets khôi phục riêng và thực hiện restore theo runbook. Giảng viên được giao dạy chọn policy ghi nhận rời trang và xem timeline đúng lớp; sinh viên được thông báo policy và chỉ xem dữ liệu của mình. Không tự kết luận gian lận. Email quan trọng và minh bạch nguồn/token tuân [đặc tả hội đồng](../requirements/council-features-2026-10-13.md).


Admin sử dụng UC 70 cho Settings/quota, UC 76 cho kết nối API chung, UC 58 cho trạng thái backup. Teacher/Subject Manager được giao dạy dùng UC 37 cho activity trong chi tiết bài làm/lượt được phép. Các ID 71/78/80/81 đã gộp, không chức năng độc lập.
