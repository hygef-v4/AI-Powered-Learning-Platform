# U04/U13/U14 — áp dụng quy ước mã nguồn: kế hoạch thực hiện

Dựa trên [yêu cầu CC-01…06](../requirements/2026-11-15-u04-u13-u14-conventions.md). Không có User Stories.

## Phân tích

- **Loại thay đổi**: refactor đặt tên và trình bày trong ranh giới unit hiện có. Không thêm component, không đổi kiến trúc.
- **Thành phần**:
  - Backend `edu.aiplatform.subjects`, `materials`, `announcements`, `notifications` (main và test).
  - Frontend `features/subjects`, `materials`, `announcements`, `notifications`, cùng các file ngoài feature import chúng (9 file).
  - Tài liệu code summary của U04/U13/U14.
- **Ảnh hưởng**:
  - Người dùng: không.
  - Cấu trúc: không.
  - Dữ liệu: không.
  - API: không (giữ URL, JSON, contract).
  - NFR: không.
- **Rủi ro**: Thấp đến trung bình. Đổi tên nhiều nên dễ sót caller, method reference, mock trong test hoặc import frontend. Compiler, TypeScript và bộ test bắt được phần lớn. Rollback bằng git, không có migration.

## Các stage

| Stage | Quyết định | Lý do |
|---|---|---|
| Workspace Detection | Đã xong | Brownfield |
| Reverse Engineering | Bỏ qua | Dùng tài liệu hiện hành |
| Requirements Analysis | Đã xong (Minimal) | CC-01…06 |
| User Stories | Bỏ qua | Không ảnh hưởng người dùng |
| Application Design | Bỏ qua | Không có component hay method mới về nghiệp vụ |
| Units Generation | Bỏ qua | Dùng 3 unit sẵn có |
| Functional Design | Bỏ qua | Không có logic nghiệp vụ mới |
| NFR Requirements / NFR Design | Bỏ qua | Không đổi NFR |
| Infrastructure Design | Bỏ qua | Không đổi hạ tầng |
| Code Generation | Thực hiện | Một kế hoạch gộp, làm tuần tự U04 → U13 → U14 → frontend, mỗi bước compile hoặc typecheck ngay |
| Build and Test | Thực hiện | `mvn -o -B verify`; frontend tsc, ESLint, Vitest, build |
| Operations | Giữ chỗ | Không có bước |

## Thứ tự Code Generation

1. Lập danh mục đổi tên chi tiết (tên cũ → tên mới) cho từng unit, đánh dấu tên giữ nguyên vì là contract hoặc framework.
2. U04 backend main và test → compile và chạy test U04.
3. U13 backend main và test → compile và chạy test U13.
4. U14 backend main và test → compile và chạy test U14.
5. Frontend 4 feature, test và các file import → tsc, ESLint, Vitest.
6. Cập nhật code summary, grep lại các mẫu vi phạm, rà diff.

Kế hoạch đề xuất gộp Code Generation của 3 unit thành một kế hoạch, với một lần duyệt kế hoạch và một lần duyệt kết quả, thay vì duyệt riêng từng unit. Bạn có thể yêu cầu tách theo từng unit.
