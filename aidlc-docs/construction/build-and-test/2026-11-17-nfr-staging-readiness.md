# Kiểm tra khả năng đưa bản sửa NFR lên staging

Ngày 17/11/2026, giờ Việt Nam. Yêu cầu: “kiể tra xem đưa lên staging đuộc chưa”. Phạm vi là kiểm tra bản sửa F01–F08; chưa có thao tác commit, push, merge hay triển khai trong lượt kiểm này.

## Kết luận

**Có thể chuẩn bị PR vào staging về mặt mã nguồn; chưa sẵn sàng merge/triển khai ngay từ trạng thái workspace hiện tại.** Phần NFR chưa commit, nhánh local chậm staging 4 commit và có hai xung đột tài liệu khi mô phỏng gộp. Không phát hiện xung đột ở mã nguồn, test hoặc cấu hình NFR.

## Bằng chứng

| Hạng mục | Kết quả |
|---|---|
| Remote đã kiểm | `git fetch origin staging`; staging hiện ở `706e46f119a9b3bc243d6ebf31f65e9062e4e459`. |
| Baseline local | `perf/production-fixes@e7110051046c17e6ef49cababd608baeea39274d`; `HEAD...origin/staging` là 0 commit riêng local, 4 commit riêng staging. |
| CI staging hiện tại | [Run 43997391530](https://github.com/hygef-v4/AI-Powered-Learning-Platform/actions/runs/43997391530): cả backend, frontend, deploy-script và images đều success. Đây là CI của baseline staging, chưa bao gồm NFR chưa commit. |
| Khác biệt từ baseline local tới staging | Chỉ 5 tệp: `aidlc-state.md`, `audit.md`, runbook deploy và hai script deploy/test. Backend/frontend/Compose/CI ở baseline không đổi. |
| Mô phỏng gộp từng tệp đã sửa | Các tệp mã nguồn/test/cấu hình áp dụng sạch. Three-way merge có xung đột ở `aidlc-docs/aidlc-state.md` và `aidlc-docs/audit.md`; chỉ thực hiện trên bản sao dưới `.tmp/nfr-staging-merge-review`, không đặt conflict marker vào workspace chính. |
| Backend NFR đã kiểm | Full verify 2.570 test, 0 failure/error, 1 skip AI evaluation; Dockerfile backend build và restore PostgreSQL thật qua. Xem [báo cáo khắc phục](2026-11-17-nfr-remediation-results.md). Không chạy lại toàn bộ vì mã ứng dụng giữa baseline đó và staging không đổi. |
| Guard chạy lại trong lượt này | Image pin guard qua 13 reference và restore; 7 suite PostgreSQL bắt buộc và 58 ca truy vết đạt; `git diff --check` qua. |
| Script deploy mới nhất của staging | Export đúng `origin/staging` sang thư mục tạm, chạy Linux harness: **16/16 qua**, bao gồm restart nginx khi cấu hình đổi. |
| Frontend image với digest mới | Dockerfile đầy đủ build thành công, gồm TypeScript và production build; image `platform-frontend:nfr-staging-review-20261117`, ID `sha256:7e8719bc5b00bbafdebe11c6ba8eafd55c38d426627854ddd81da22e169f11cf`. |
| Smoke frontend runtime | Chạy container tạm với 384 MiB/0,5 CPU như staging: `/api/health` 200, body `status: UP`; `/login` 200; UID 100 (non-root). Đã xóa container sau kiểm. Không coi đây là kiểm đăng nhập bằng tài khoản thật hoặc load test. |

Log local: `.tmp/nfr-staging-frontend-image.log`, `.tmp/nfr-staging-frontend-smoke.log`, `.tmp/nfr-staging-deploy-harness.log`. Kết quả build/test được chốt lúc khoảng 10:06 giờ Việt Nam; các reference remote phản ánh thời điểm fetch trong lượt kiểm này.

## Các bước cần trước khi merge

1. Tạo nhánh NFR từ staging mới nhất, đưa đầy đủ source/test/script và `infra/images.lock.json` cùng guard vào commit. Không gộp báo cáo readiness chưa theo dõi của công việc khác vào commit một cách tự động.
2. Gộp nội dung hai tài liệu xung đột, giữ cả lịch sử NFR và thay đổi deploy/nginx từ staging; không dùng cách chọn toàn bộ ours/theirs làm mất nội dung.
3. Mở PR vào staging và yêu cầu cả backend, frontend, deploy-script của CI trên PR thành công. CI hiện tại của staging không thay kết quả CI của PR mới.
4. Sau khi merge, chờ workflow push tạo đủ hai app image theo SHA. Theo runbook, staging có cơ chế tự deploy khi CI thành công; merge là bước có thể kích hoạt triển khai.
5. Khi staging đã nhận SHA mới, kiểm health backend/worker/frontend, đăng nhập, bắt đầu Quiz đồng thời, một job nền và dấu hiệu timeout/queue tồn đọng. So sánh SHA đang chạy với SHA đã qua CI.

Không có migration mới hoặc yêu cầu đổi secret cho bản sửa NFR. Các giới hạn SQL/pool và worker cần tái tạo backend/worker để có hiệu lực. Chưa truy cập VPS trong lượt này, nên không khẳng định tài nguyên trống, tình trạng container hoặc SHA thực đang chạy trên VPS.

## Giới hạn và tuân thủ

Load test mục tiêu, RPO/RTO dữ liệu production, accessibility toàn hành trình và soak test vẫn chưa nghiệm thu; đây là các kiểm chứng tiếp theo, không phải lý do để giữ bản sửa khỏi môi trường staging kiểm thử. Không xem kết quả này là chấp thuận production.

Giữ phạm vi Security/Resiliency và PBT disabled như [báo cáo khắc phục](2026-11-17-nfr-remediation-results.md). Lượt này chỉ đọc remote/CI, chạy build/harness cô lập và ghi tài liệu; không đổi cơ chế bảo mật, public API, schema hoặc dữ liệu người dùng.
