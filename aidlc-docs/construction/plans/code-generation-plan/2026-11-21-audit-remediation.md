# Sửa tuần tự các phát hiện audit mã nguồn

Phạm vi được duyệt qua yêu cầu sửa và test các lỗi trong báo cáo tại 91d593be. Mỗi mục có ca tái hiện hoặc kiểm tra hành vi trước khi sửa, sau đó chạy lại ca đó và bộ liên quan. Giữ quyền, biên nhận và hợp đồng đã công bố. Dùng dữ liệu tổng hợp. Các quyết định về chuyển nháp, thứ tự khóa và lưu trạng thái bền vững có rà soát kiến trúc.

- [x] 1. U09. Rebase form chấm theo version trong useGradingWorkspace. Kiểm nhận xét riêng, xung đột và điểm rubric. Ba ca hồi quy thất bại trước sửa. Sau sửa 226 test grading đạt, TypeScript và ESLint đạt.
- [x] 2. U12. Ghi đúng tác giả khi tự chốt phần nhóm trong GroupSubmissionService. Kiểm nháp đã lưu và phần chỉ mới được nhận. Ca nhầm người nhận thành tác giả thất bại trước sửa. Sau sửa 14 test GroupSubmissionServiceTest đạt.
- [x] 3. U08. Đặt lại lịch tự lưu AssignmentWorkspacePage khi còn thay đổi. Kiểm lưu đang gửi và khôi phục sau lỗi dữ liệu. Ba ca tự lưu không được đặt lịch thất bại trước sửa. Sau sửa 53 test assignmentwork đạt.
- [x] 4. U11. Giữ câu trả lời cục bộ khi QuizTakingPage xử lý xung đột version. Kiểm mất phản hồi, chọn nháp mới và lưu thành công. Ba ca mất phản hồi và chồng lấp lựa chọn thất bại trước sửa. Sau sửa 39 test quizpractice đạt.
- [x] 5. U12. Kiểm lại hạn dưới khóa trước GroupSubmissionService tự nộp. Kiểm gia hạn, quá hạn và đóng chủ động. Hai ca hạn mới và ranh giới ân hạn thất bại trước sửa. Sau sửa 24 test nhóm đạt.
- [x] 6. U12. Thống nhất thứ tự khóa lưu và chốt bài nhóm. Kiểm lưu song song với đóng và từ chối lưu sau khi đóng. Ba ca khóa và ghi sau khi đóng thất bại trước sửa. Sau sửa 52 test đạt, có ca đồng thời PostgreSQL.
- [x] 7. U10. Khóa chuỗi phiên bản giữa publish và reopen trong QuizPublicationService. Kiểm hai transaction cùng mở. Ca hai lượt mở quiz đồng thời thất bại trước sửa. Sau sửa 62 test đạt.
- [x] 8. U03. Chỉ giữ thời gian chờ sau resend thành công trong PendingSweepRunner và JobPublisher. Kiểm lỗi gửi và phục hồi. Ca lỗi gửi lại bị bỏ qua lần thử thất bại trước sửa. Sau sửa 8 test runner đạt.
- [x] 9. Infra. Fail deploy readiness khi lệnh inspect lỗi. Bổ sung ca trong bộ test deploy. Ca inspect exit 42 thất bại trước sửa. Sau sửa cả 17 ca deploy giả lập đạt.
- [x] 10. U12. Đồng bộ version khi nhả và nhận lại phần nhóm trong GroupSectionEditorPage. Kiểm lưu sau khi nhận lại qua SSE. Ba ca mất và nhận lại mục nhóm thất bại trước sửa. Sau sửa 38 test groupdocs đạt.
- [x] 11. U08. Khôi phục nháp toàn môn khi chuyển lớp. Giữ biên nhận đã nộp và kiểm quyền của lớp mới. Hai ca PostgreSQL và một ca màn hình thất bại trước sửa. Sau sửa 27 test Java và 51 test frontend đạt.
- [x] 12. U06. Áp giới hạn đồng thời lên provider thực tế trong AiTaskJobHandler. Kiểm fallback chờ khi provider đích bận. Ca fallback vào provider đã đủ suất thất bại trước sửa. Sau sửa 54 test AI đạt.
- [x] 13. U06. Dùng cùng trần token cho chọn nguồn và giữ quota trong AiTaskService. Kiểm fallback sang khóa cá nhân. Ca chọn nhầm nguồn khi thiếu quota suy luận thất bại trước sửa. Sau sửa 49 test AiTaskFlow đạt.
- [x] 14. U09. Kiểm version batch trong bước prepareProposal dưới khóa. Kiểm yêu cầu cũ không tạo tác vụ. Ca version đổi trong lúc đợi khóa thất bại trước sửa. Sau sửa 15 test grading và PostgreSQL đạt.
- [x] 15. U03. Lưu lịch dọn file chưa gắn qua vòng đời Redis. Kiểm redis restart và file đã gắn. Ca mất lịch khi Redis mất trạng thái thất bại trước sửa. Sau sửa 13 test đạt, có PostgreSQL và Drive giả lập.
- [x] 16. Infra. Giới hạn thời gian đợi readiness và dọn container restore lỗi. Kiểm timeout và restore được giữ lại sau thành công. Ca PostgreSQL không sẵn sàng thất bại trước sửa. Sau sửa 3 ca restore giả lập đạt.
- [x] 17. U06. Tuần tự hóa giới hạn credential MCP theo tài khoản. Kiểm hai lần tạo đồng thời. Ca hai credential cùng được tạo thất bại trước sửa. Bộ 98 test AI, jobs và PostgreSQL đạt.
- [x] 18. U13. Tuần tự hóa tạo module đầu tiên ở MaterialModuleService. Kiểm hai lần mở môn mới. Ca tạo hai Module 1 thất bại trước sửa. Sau sửa 27 test đạt, có PostgreSQL đồng thời.
- [x] 19. U08. Dùng client tải DOCX có gia hạn phiên. Kiểm 401, refresh, replay và tên file. Hai ca tải DOCX hết phiên thất bại trước sửa. Sau sửa 158 test liên quan đạt.
- [x] Kiểm bản ghép. Đạt 249 test backend được chọn, 1.186 test frontend, TypeScript, ESLint, 17 ca deploy và 3 ca restore giả lập.
