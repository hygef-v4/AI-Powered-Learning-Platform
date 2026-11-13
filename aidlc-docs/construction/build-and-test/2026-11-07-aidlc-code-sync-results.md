# Đồng bộ tài liệu theo source — 2026-11-07

**Source snapshot:** `f68a6ec5889074de80a1f1f0faf7bcb7e7054ec8`, nhánh `fix/u05-u09-u15`. Đã xử lý sáu nhóm lệch được xác nhận trong [review](../../inception/reverse-engineering/2026-11-07-aidlc-consistency-review.md), theo [kế hoạch được yêu cầu](../../inception/plans/2026-11-07-aidlc-code-sync.md). Source review không phải runtime acceptance.

## Nội dung đã cập nhật

- [API inventory](../../inception/reverse-engineering/api-inventory.md) lấy lại mapping hiện hành của toàn bộ 49 controller: 334 annotation, gồm AI connection theo ID, group import/peer review, lock/revisions và các API Quiz mới. Đây là annotation inventory, không thay thế OpenAPI hoặc DTO schema đầy đủ.
- [Source baseline](../../inception/reverse-engineering/current-code-baseline.md) bổ sung đủ 32 migration, controller và test theo owner, kể cả ba shared tests trước đây chưa liệt kê. [Services](../../inception/application-design/services.md) và [public contract types](../../inception/application-design/component-methods.md) lấy từ source; giữ nhãn link cũ khi target không đổi.
- 15 unit as-built summaries có snapshot hiện hành và trỏ central inventory; không gọi working tree 30/10 là code hiện tại. Các delta có ngày giữ phạm vi gốc. Wording JDBC thay mô tả SQL/JPA mơ hồ ở owner đã dùng JDBC; U04/U13/U14 vẫn được ghi là đang JPA.
- Application design, U01 requirements/NFR/domain/infrastructure và U14 mail boundary mô tả SMTP mặc định/Mailpit local hoặc Brevo HTTPS API với MAIL_TRANSPORT/BREVO_API_KEY. SMTP key và API key được phân biệt. Sơ đồ giữ cấu trúc cũ, chỉ đổi nhãn transport; không tạo topology mới. Sửa câu JwtAuthFilter chỉ kiểm chữ ký thành kiểm phiên/account qua SessionAuthority theo code hiện hữu.
- Build/run guidance phản ánh CI kiểm deploy-script và VPS pull workflow thành công trên main; không diễn đạt thành deployment bị tắt. Không xác nhận trạng thái VPS đang chạy từ source.
- Build instructions nêu đủ bốn required PostgreSQL suites và ngưỡng của script guard. [Evidence matrix](build-and-test-summary.md) giữ số test theo ngày/commit, không gắn kết quả trước merge thành fresh HEAD acceptance. Frontend test guidance giữ npm test, thêm lựa chọn hai worker trên máy hạn chế mà không tăng timeout.
- Khi kiểm nội dung U06, đã sửa thêm các mô tả cũ mâu thuẫn trực tiếp với source: nhiều key PERSONAL/provider, DISABLED cho personal key, owner reveal, CRUD/test/status theo ID, automatic source khi bỏ source, thứ tự ConnectionOrder và worker retry/fallback. Thay đổi chỉ mô tả hành vi đã có; không bổ sung retry hoặc đổi lựa chọn chi phí trong code. UNKNOWN usage vẫn giữ giới hạn accounting đã ghi.

## Kiểm chứng

| Kiểm tra | Kết quả |
|---|---|
| Bộ đọc annotation độc lập so với inventory sinh ra | 49 controller / 334 mapping annotation khớp tên, tham số và thứ tự; giữ class/method composition |
| Inventory controller/migration/test | 320 source entries được đối chiếu bằng target link; không thiếu controller hoặc migration |
| Service/public contract inventory | Các type trong source owner được liệt kê; contract/stub không giả làm public declaration |
| Required PostgreSQL guidance | Tên/ngưỡng cả bốn suite khớp script guard |
| Relative links | Không có target thiếu trong 220 file ứng viên hiện hành; các file mới và link nội bộ thêm được kiểm riêng |
| Markdown | Fence cân bằng, whitespace diff đạt; cấu trúc cạnh sơ đồ có sẵn không đổi |
| Phạm vi | Không đổi backend/frontend/infra/scripts/CI; không sửa history hoặc báo cáo kiểm thử có ngày trước đây |

Kiểm tra dùng `.tmp/check-aidlc-sync-20261107.cjs` và `.tmp/verify-aidlc-sync-20261107.cjs`; các parser có assert mẫu bao gồm annotation nhiều dòng/chuỗi chứa ngoặc và đường dẫn link thông thường. Không thêm script/dependency vào application. Bộ lọc tài liệu hiện hành loại lịch sử, audit, kế hoạch, remediation và tên file có ngày; link của snapshot cũ không tự coi là lỗi hiện hành.

## Giới hạn

Không chạy lại application test/build, không gọi provider/VPS hoặc triển khai. Kết quả 1.830 backend/799 frontend ngày 06/11 là bằng chứng trước merge, không phải run mới trên snapshot này. Không kiểm toàn bộ từng BR/UC bằng runtime, external URL, Markdown anchor hoặc render lại các sơ đồ cũ. Không chứng nhận provider delivery, performance, restore, CI/VPS health hay operational acceptance.

Security/resiliency scope giữ nguyên, PBT disabled. Báo cáo review và audit trước đó được giữ như lịch sử; chỉ append audit cho yêu cầu này. Không commit/push trong lần đồng bộ.
