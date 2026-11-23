# Kiểm tra khả năng đưa lên staging — 16/11/2026

- [x] Fetch origin và đối chiếu HEAD với staging.
- [x] Kiểm tra file chưa merge, conflict marker và diff whitespace.
- [x] Xác minh CI trên đúng SHA, gồm backend/frontend/deploy scripts/images.
- [x] Đối chiếu kiểm tra local và ghi giới hạn môi trường.

## Kết luận

Code đã ở `origin/staging`: HEAD nhánh local `docs/sync-ops-docs-staging` và staging cùng SHA `d8ac7d212a0f07d27e83aa476291b24a3a45e2ac`; ahead/behind là 0/0 sau fetch. Không có commit code cần merge thêm, không có unmerged entries hoặc conflict markers trong backend/src, frontend/src, infra và .github. `git diff --check` đạt.

Các thay đổi chưa commit quan sát được chỉ là README và tài liệu AI-DLC/runbook đang được sửa ở lượt khác; giữ nguyên. Chúng chưa nằm trong SHA đã kiểm CI.

## Bằng chứng CI

[CI run 43866345656](https://github.com/hygef-v4/AI-Powered-Learning-Platform/actions/runs/43866345656) thuộc đúng SHA trên, trạng thái completed/success:

| Job | Kết quả |
|---|---|
| backend | Maven verify và guard PostgreSQL bắt buộc không skip đều success |
| frontend | npm ci, lint, typecheck, tests và production build đều success |
| deploy-script | ShellCheck, test deploy và restore đều success |
| images | Build và push backend/frontend image theo SHA đều success |

Đủ bằng chứng CI để dùng SHA này trên môi trường staging. Chưa kiểm tra VPS đang chạy SHA nào hoặc smoke test trên staging thật; CI thành công không tự chứng minh deploy đã hoàn tất.

## Kiểm tra local và giới hạn

Local node_modules thiếu `next-intl`, trong khi package.json và package-lock.json đều khai báo dependency. Typecheck báo TS2307 và các suite frontend phụ thuộc i18n không load được. Đây là môi trường local chưa đồng bộ dependency; không tính lượt local là PASS và không thay đổi dependency để làm xanh kiểm tra.

Kết quả local cuối: lint exit 0; typecheck exit 2; tests exit 1, 146 suites lỗi load module/11 suites qua (46 tests qua). Log tại `.tmp/staging-1116-{lint,types,tests,backend}.log`. Không chạy build local khi dependency còn thiếu; production build CI đúng SHA đã thành công.

Docker Desktop Linux engine không chạy, nên lượt backend local chưa khởi động được. Không tự khởi động dịch vụ hoặc dùng DB thật. CI đúng SHA ở trên đã chạy các bước tương ứng thành công trong môi trường sạch.

Không push, merge, deploy hoặc sửa mã ứng dụng. Đây là kiểm tra sẵn sàng tích hợp, không phải chứng nhận không còn mọi bug. Security/Resiliency giữ phạm vi hiện có; review không thay đổi quyền, secret hay dữ liệu. PBT vẫn tắt; kiểm tra vận hành thực tế chưa thực hiện.
