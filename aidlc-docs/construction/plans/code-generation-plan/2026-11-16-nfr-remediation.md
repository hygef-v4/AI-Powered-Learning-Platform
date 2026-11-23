# Khắc phục NFR ngày 16/11/2026

Người dùng yêu cầu “sửa toàn bộ cho tôi” sau khi nhận [báo cáo và thứ tự xử lý](../../build-and-test/2026-11-16/2026-11-16-non-functional-review.md). Đây là chấp thuận thực hiện F01–F08 và các kiểm chứng liên quan trong phạm vi local; không phải chấp thuận deploy, commit/push, thay dữ liệu production hoặc mở rộng scope MFA/PBT/HA. Giữ thay đổi performance đang có của lượt khác.

## Quy trình và thiết kế

Brownfield, dùng lại requirements/reverse engineering. Requirements và hướng xử lý trong báo cáo đã được người dùng yêu cầu thực hiện; không mở lại câu hỏi nghiệp vụ. Bỏ user stories/new functional design vì sửa NFR hiện hữu, không thêm use case. Thực hiện Code Generation và Build and Test; các chi tiết NFR được ghi ngay dưới đây.

1. [x] F01: health Drive trả cache ngay, refresh nền một luồng, không xếp nhiều probe khi mạng chậm; báo DEGRADED khi quá ngân sách và shutdown executor. Test probe chậm/lỗi/cache/concurrent.
2. [x] F02: giới hạn lấy connection, connect/socket, statement và lock trên pool ứng dụng; Flyway/pg_dump không thừa hưởng deadline SQL của request. Map lỗi timeout/lock thành SYSTEM_BUSY an toàn; kiểm bằng PostgreSQL thật khi môi trường sẵn sàng.
3. [x] F03: dùng row lock FOR SHARE khi bắt đầu Quiz, giữ advisory lock theo người học; test nhiều sinh viên cùng Quiz, start cùng người và edit/retire vẫn chờ đúng.
4. [x] F04: executor hữu hạn riêng từng queue, backpressure giữ message để broker requeue khi hết chỗ, không coi saturation là lỗi nghiệp vụ; không xóa khóa running cho handler vẫn sống. Test handler bỏ qua interrupt và queue khác vẫn hoạt động.
5. [x] F05: deadline và kiểm container khi chờ restore, xử lý lỗi/dọn tệp; test decrypt fail, startup exit, timeout và success bằng harness.
6. [x] F06: tra registry và khóa digest cho base/service images đang dùng; giữ phiên bản dự án, thêm kiểm tra tránh vô tình bỏ pin và hướng dẫn cập nhật.
7. [x] F07/F08: đồng bộ checklist API key write-only và công thức RPO/RTO, cập nhật runbook theo cấu hình mới.
8. [x] Chạy unit/integration/shell/config checks; bổ sung artifact kiểm tải/restore nếu có môi trường cô lập khả dụng; không biến trường hợp chưa đo thành PASS.
9. [x] Tự review diff, kiểm tra tương tác với performance WIP, ghi kết quả, giới hạn và trạng thái đóng từng finding.

## Ràng buộc

- Không đổi public API/DTO/schema hoặc thuật toán chấm; không bỏ kiểm quyền/transaction/CAS.
- Timeout cấu hình áp vào SQL ứng dụng, không đóng transaction backup đang chờ upload chỉ vì thời gian thao tác ngoài DB.
- Bounded pool theo queue giữ cách ly AI/backup/email; message chưa được nhận xử lý phải có thể requeue.
- Digest phải lấy từ registry thật, không tự đặt chuỗi checksum.
- Security 03/04/05/08/09/12/15 và Resiliency 04/06/10 giữ phạm vi đã duyệt; PBT tắt.
- Benchmark/restore toàn hệ thống và a11y chỉ đóng khi thực sự chạy đủ; nếu môi trường thiếu sẽ ghi chính xác thay vì khẳng định đã nghiệm thu.

## Kết quả

Hoàn tất 17/11/2026. Xem [báo cáo khắc phục và kiểm chứng](../../build-and-test/2026-11-17/2026-11-17-nfr-remediation-results.md). Đóng F01–F08; tải mục tiêu, RPO/RTO production và accessibility toàn hành trình vẫn chưa nghiệm thu. Không deploy/commit/push trong lượt này.

## Đưa bản sửa lên PR staging — được yêu cầu ngày 17/11/2026

- [x] Đồng bộ staging mới nhất trên nhánh NFR riêng; giữ nguyên nội dung ngoài phạm vi.
- [x] Gộp đủ lịch sử của state/audit và kiểm lại diff, image pins, guard test.
- [ ] Commit phần NFR, push và mở PR vào staging; chưa merge/deploy.
- [ ] Theo dõi CI đúng commit của PR, sửa nếu có lỗi và chỉ báo hoàn tất khi các job bắt buộc đạt.
