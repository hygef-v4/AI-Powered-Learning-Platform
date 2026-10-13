# Use case catalog — Inception revision 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

**66 active UC**; giữ ID cũ, thêm các ID 74–79 còn hiệu lực; 71/78/80/81 đã gộp, xem migration, không tái sử dụng retired ID. Đây là catalog Inception mới, đã đồng bộ bảng tiếng Anh [docs/usecase_table.md](../../../docs/usecase_table.md); chưa đồng bộ Google SRS hoặc diagrams cũ.

| ID | Use case | Primary unit | Scope/behavior |
|---|---|---|---|
| 01 | Activate Account | U01 | Theo FR liên quan; backend kiểm scope và state. |
| 02 | Login | U01 | Theo FR liên quan; backend kiểm scope và state. |
| 03 | Logout | U01 | Theo FR liên quan; backend kiểm scope và state. |
| 04 | Reset Password | U01 | Theo FR liên quan; backend kiểm scope và state. |
| 05 | Change Password | U01 | Theo FR liên quan; backend kiểm scope và state. |
| 06 | View Profile Information | U01 | Theo FR liên quan; backend kiểm scope và state. |
| 07 | Update Profile Information | U01 | Theo FR liên quan; backend kiểm scope và state. |
| 12 | View Notifications | U16 | Theo FR liên quan; backend kiểm scope và state. |
| 13 | View Enrolled Classes | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 14 | View Enrolled Class Detail | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 15 | View Learning Material | U05 | Tài liệu tham khảo MVP; Lesson/YouTube/summary/RAG chỉ mở rộng. |
| 16 | View My Group | U12 | Theo FR liên quan; backend kiểm scope và state. |
| 17 | Request Leader Change | U12 | Theo FR liên quan; backend kiểm scope và state. |
| 18 | View Quiz Practice History | U11 | Quiz độc lập Lesson, tạm Practice theo D01. |
| 19 | View Quiz Practice Detail | U11 | Quiz độc lập Lesson, tạm Practice theo D01. |
| 20 | Take Quiz | U11 | Quiz độc lập Lesson, tạm Practice theo D01. |
| 21 | View Quiz Result | U11 | Quiz độc lập Lesson, tạm Practice theo D01. |
| 22 | View Assignment List | U11 | Theo FR liên quan; backend kiểm scope và state. |
| 23 | View Assignment Detail | U11 | Theo FR liên quan; backend kiểm scope và state. |
| 24 | Complete Individual Assignment | U11 | Theo FR liên quan; backend kiểm scope và state. |
| 26 | Edit Assignment Diagram | U11 | Supporting workspace capability khi diagramEnabled, không loại Diagram Essay riêng. |
| 27 | Complete Group Assignment | U14 | Assignment GROUP; leader nộp, diagram tùy chọn. |
| 28 | View Submission History | U11 | Theo FR liên quan; backend kiểm scope và state. |
| 29 | Request Practice Feedback | U11 | Private feedback Practice qua personal API/MCP, nhiều lượt theo version/rate limit. |
| 30 | View Class Announcements | U05 | Theo FR liên quan; backend kiểm scope và state. |
| 31 | View Assigned Class List | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 32 | View Assigned Class Detail | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 33 | View Uploaded Learning Materials | U05 | Tài liệu tham khảo MVP; Lesson/YouTube/summary/RAG chỉ mở rộng. |
| 34 | Add/Update/Delete Learning Material | U05 | Tài liệu tham khảo MVP; Lesson/YouTube/summary/RAG chỉ mở rộng. |
| 35 | Create/Update/Delete Quiz | U09 | Quiz độc lập Lesson, tạm Practice theo D01. |
| 36 | Create/Update/Delete Announcement | U05 | Theo FR liên quan; backend kiểm scope và state. |
| 37 | View Student Submissions | U15 | Giảng viên xem bài nộp cá nhân/nhóm và activity trong chi tiết bài làm/lượt Quiz được phép; policy bật mới có dữ liệu. Không tự kết luận cheating hoặc sửa điểm. |
| 38 | Request AI Grading Proposal | U15 | Theo FR liên quan; backend kiểm scope và state. |
| 39 | Grade Submissions Manually | U15 | Theo FR liên quan; backend kiểm scope và state. |
| 40 | View/Export GradeBook | U15 | Theo FR liên quan; backend kiểm scope và state. |
| 41 | View Teacher Assignment List | U08 | Theo FR liên quan; backend kiểm scope và state. |
| 42 | Create/Update/Delete Assignment | U09 | Theo FR liên quan; backend kiểm scope và state. |
| 46 | Manage Assignment Rubrics | U06 | Theo FR liên quan; backend kiểm scope và state. |
| 47 | View Managed Subject Classes | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 48 | View Managed Class Detail | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 49 | Create Class | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 50 | Assign Teacher To Class | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 51 | Add/Remove Class Students | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 52 | Edit Class Information | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 53 | View Managed Subject | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 54 | View Subject Materials | U05 | Tài liệu tham khảo MVP; Lesson/YouTube/summary/RAG chỉ mở rộng. |
| 55 | Add/Update/Delete Subject Material | U05 | Tài liệu tham khảo MVP; Lesson/YouTube/summary/RAG chỉ mở rộng. |
| 56 | View Subject Question Bank | U06 | Theo FR liên quan; backend kiểm scope và state. |
| 57 | Manage Subject Quiz Questions | U06 | Theo FR liên quan; backend kiểm scope và state. |
| 58 | View Admin Dashboard | U16 | Admin xem thống kê vận hành và trạng thái/lịch sử backup từ U03: thời gian, dung lượng, lần thành công gần nhất, lỗi đã sanitize; không truy cập dữ liệu học vụ. |
| 59 | View Account List | U01 | Theo FR liên quan; backend kiểm scope và state. |
| 60 | Add Account | U01 | Theo FR liên quan; backend kiểm scope và state. |
| 61 | View Account Detail | U01 | Theo FR liên quan; backend kiểm scope và state. |
| 62 | Update Account Information | U01 | Theo FR liên quan; backend kiểm scope và state. |
| 63 | Change Account Status | U01 | Theo FR liên quan; backend kiểm scope và state. |
| 64 | View Subject List | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 65 | Add Subject | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 66 | View Subject Detail | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 67 | Update Subject Information | U04 | Theo FR liên quan; backend kiểm scope và state. |
| 70 | Manage System Settings | U03 | Admin xem/sửa cấu hình vận hành, lịch/retention backup/activity và quota giảng viên; U07 hỗ trợ quota qua port. Credentials AI riêng qua UC 76. |
| 73 | View Audit Log | U02 | Theo FR liên quan; backend kiểm scope và state. |
| 74 | Manage Personal AI Connection | U13 | Chủ tài khoản thêm/test/thay/thu hồi personal API; Gemini/OpenAI/Claude. |
| 75 | Connect/Revoke MCP Agent | U13 | Kết nối/revoke agent MCP và xem hướng dẫn client đã kiểm chứng. |
| 76 | Manage Institutional AI Connection | U13 | Admin quản trị API chung, test synthetic payload, không học vụ. |
| 77 | View AI Usage History | U07 | Usage/provenance của chính chủ; Admin chỉ thống kê chung theo quyền. |
| 79 | Request Draft Feedback | U11 | Private feedback bản nháp; Graded cần teacher allowStudentAiFeedback. |

## Retired/migration

| ID | Quyết định |
|---|---|
| 08 | Retired 2026-10-13; payment/Code hoặc authoring riêng gộp Assignment. Giữ truy vết lịch sử, không reuse. |
| 09 | Retired 2026-10-13; payment/Code hoặc authoring riêng gộp Assignment. Giữ truy vết lịch sử, không reuse. |
| 10 | Retired 2026-10-13; payment/Code hoặc authoring riêng gộp Assignment. Giữ truy vết lịch sử, không reuse. |
| 11 | Retired 2026-10-13; payment/Code hoặc authoring riêng gộp Assignment. Giữ truy vết lịch sử, không reuse. |
| 25 | Retired 2026-10-13; payment/Code hoặc authoring riêng gộp Assignment. Giữ truy vết lịch sử, không reuse. |
| 43 | Retired 2026-10-13; payment/Code hoặc authoring riêng gộp Assignment. Giữ truy vết lịch sử, không reuse. |
| 44 | Retired 2026-10-13; payment/Code hoặc authoring riêng gộp Assignment. Giữ truy vết lịch sử, không reuse. |
| 45 | Retired 2026-10-13; payment/Code hoặc authoring riêng gộp Assignment. Giữ truy vết lịch sử, không reuse. |
| 68 | Retired 2026-10-13; payment/Code hoặc authoring riêng gộp Assignment. Giữ truy vết lịch sử, không reuse. |
| 69 | Retired 2026-10-13; payment/Code hoặc authoring riêng gộp Assignment. Giữ truy vết lịch sử, không reuse. |
| 72 | Retired 2026-10-13; payment/Code hoặc authoring riêng gộp Assignment. Giữ truy vết lịch sử, không reuse. |

UC 44/45 authoring gộp UC 42; UC 26/27 giữ như capability/submission cá nhân có diagram/nhóm. Chỉ hai product types. Không suy ra số màn/API bằng số UC.

## Consolidation 2026-10-13 — yêu cầu người dùng

| Former ID | Current ID | Quyết định |
|---|---|---|
| 71 | 70 | Edit Settings gộp Manage System Settings; giữ xem/sửa/version/audit. |
| 78 | 70 | Quota giảng viên là cấu hình con; U03 primary UC, U07 vẫn sở hữu xử lý quota qua public port. |
| 80 | 58 | Trạng thái/lịch sử backup là mục Admin Dashboard; U16 primary UC, U03 vẫn sở hữu backup/job/manifest. |
| 81 | 37 | Activity là luồng xem chi tiết bài làm/lượt được phép; U15 primary UC, U11 thu thập/U14 tích hợp. |

Không đánh số lại hoặc tái sử dụng IDs đã gộp. Không bỏ FR-038/039 hoặc story backup/activity/quota. Quiz Practice không trở thành Graded vì giảng viên xem activity: chỉ đọc lượt trong lớp được phân công theo policy, không thêm điểm Quiz vào Gradebook.
