# U04 — Implementation decisions

Các quyết định người dùng xác nhận ngày18/10/2026 khi rà KAN-24, SRS local, thiết kế hiện hành và UI. Tài liệu này thay phần quyết định cần giữ của các file TEMP; kế hoạch thực hiện hiện hành ở [code-generation-plan](../../plans/code-generation-plan/u04-subject-administration-code-generation-plan.md), trạng thái frontend và điểm nối liên unit ở [frontend-handoff](frontend-handoff.md).

## Quyết định nghiệp vụ và kỹ thuật

| Mã | Quyết định | Phạm vi/giới hạn |
|---|---|---|
| Q01 | Dùng UC16–19 và thiết kế U04 Subject Administration hiện hành. | SRS local dùng số UC cũ; không triển khai lại unit cũ chứa lớp/ghi danh. U01/U05 có trách nhiệm riêng. |
| Q02 | Admin chỉ quản trị môn và xem lớp chỉ đọc. | Không gán Admin làm chủ nhiệm/giảng viên hoặc cấp quyền học vụ chỉ vì là Admin. |
| Q03 | Chủ nhiệm optional khi tạo và sửa; mỗi môn tối đa một chủ nhiệm, một người có thể quản lý nhiều môn. | Tài khoản được gán phải SUBJECT_MANAGER/ACTIVE; managerId:null biểu thị bỏ phân công. |
| Q04 | Phải xác minh phiên/quyền hiện hành, không grace period để giữ quyền cũ. | Invalid session và dependency unavailable cần phân biệt401/503 theo bàn giao U01; không mở fallback allow khi Redis/U01 lỗi. |
| Q05 | Chưa có U05 hoặc không kiểm được lớp là unavailable, không phải0/rỗng. | Chặn archive nếu không xác minh; dữ liệu fixture chỉ dùng test/QA riêng, không fallback production. |
| Q06 | Archive và các writer tạo/open/reopen lớp phải phối hợp khóa môn trong transaction. | U04 cung cấp mutation guard; U05 phải dùng cùng khóa và kiểm ACTIVE. Contract là yêu cầu bàn giao, không chứng minh U05 đã triển khai. |
| Q07 | Bảng lớp Admin bỏ cột sĩ số theo UI a07b, giữ field contract hiện có. | Không link/mutation lớp; archive rejection trả tổng và blocker snapshot nhất quán, gồm OPEN+DRAFT. Đây là lựa chọn giải quyết khác biệt với BR-U04-05 cũ còn ghi số sinh viên. |
| Q08 | DTO REST riêng: totalElements theo q+status, statusCounts theo q trước status. | Detail có createdAt/version; unavailable khác0/rỗng; stats lớp là tổng môn, không đổi theo filter UI. |
| Q09 | Server trim/uppercase code rồi kiểm `[A-Z0-9-]{1,20}`; name trim và không blank. | PATCH chỉ metadata, reject code/field ngoài whitelist; description optional/max2000, name max200. |
| Q10 | Admin được sửa metadata/chủ nhiệm của môn archived. | Downstream vẫn chặn sửa tài nguyên/tạo lớp theo trạng thái; reopen là thao tác riêng, không đổi lớp. |
| Q11 | Public version bắt đầu1, storage version bắt đầu0. | Stale version luôn409 kể cả PUT no-op. Cùng giá trị và đúng version là no-op, không tăng version/ghi success-change audit. Archive/reopen thay trạng thái thật phải tăng version. |
| Q12 | Người dùng trả lời “Tôi không biết”, không phê duyệt phương án A/B. | Phương án kỹ thuật hiện tại: deadline1s cho lớp tại adapter, synchronous transaction/SQL bounds; dedup contact trong request theo contract lookup hiện có. Chỉ đề xuất batch U01 sau khi đo NFR, chưa coi p95/batch provider hoàn thành. |
| Q13 | ManagerCard dùng initials từ displayName. | Không thêm avatar field hoặc chức năng upload ảnh. |
| Q14 | Người dùng chọn riêng: “Khi disable subject manager thì tự gỡ phân công luôn”. | Yêu cầu phối hợp U01/U04: gỡ tất cả môn ACTIVE/ARCHIVED, tăng version/audit cùng transaction disable, không tự gán lại khi enable. Không suy rộng thành đổi giảng viên lớp hoặc tự quyết định quy tắc đổi role. Code Q14/provider thật còn pending. |

Q01–Q11 và Q13 là các lựa chọn A đã xác nhận. Q12 giữ rõ là quyết định kỹ thuật tạm có giới hạn. Q14 thay đề xuất giữ managerId cũ; lịch sử người cũ lưu qua audit, không giữ bằng phân công hiện thời sau disable.

## Các gate chưa được xác nhận hoàn tất

- U01: schema accounts thật, account guard/disable transaction Q14, current user/RoleGuard, JWT/session/CSRF và account search endpoint/schema.
- U02: durable success audit trong transaction, denied transaction riêng và chống trùng giữa filter/service.
- U05: provider lớp/check archive, deadline ở query/lock và tất cả writer tạo/open/reopen dùng guard tương thích.
- Tester: HTTP/auth/provider thật, concurrency và rollback liên unit, NFR p95 và review độc lập.

Các test PostgreSQL fixture, standalone HTTP principal và browser QA chứng minh consumer theo phạm vi của chúng; không thay các gate trên. Lịch sử câu trả lời và từng checkpoint được ghi trong audit; không cập nhật Jira khi dọn tài liệu/commit.
