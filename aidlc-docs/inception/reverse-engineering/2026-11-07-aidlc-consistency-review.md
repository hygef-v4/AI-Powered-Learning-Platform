# Kiểm tra đồng bộ AI-DLC — 2026-11-07

**Snapshot:** HEAD `f68a6ec5`; working tree sạch trước khi kiểm tra. Kết luận: chưa đồng bộ hoàn toàn. Các cập nhật riêng lẻ đã có, nhưng tài liệu tổng hợp và một số thiết kế hiện hành còn lệch code. Đây là báo cáo kiểm tra; chưa sửa các tài liệu được nêu hoặc mã ứng dụng.

## Phạm vi và kết quả kiểm tra tự động

- Quét 396 file Markdown trong `aidlc-docs`, kiểm 2.890 đích liên kết tương đối ngoài code block/inline code. Nhóm 220 file ứng viên hiện hành, sau khi loại audit/history/plans/remediation và tên file có ngày, không có đích liên kết thiếu. Không kiểm anchor, URL bên ngoài, liên kết sinh động hoặc chứng nhận mọi Markdown construct.
- Những đích không tồn tại ở nhóm tài liệu có ngày/lịch sử/kế hoạch không tự động coi là lỗi hiện hành: nhiều đường dẫn là snapshot trước đổi cấu trúc hoặc ví dụ. Không sửa lịch sử để khớp source mới.
- Catalog UC đang hoạt động có đủ 66 ID duy nhất 01–66; đủ 15 thư mục unit và 15 as-built summaries.
- Đối chiếu inventory với 49 controller và 32 migration SQL hiện có; kiểm các API annotation ứng viên khác biệt bằng nguồn cụ thể trước khi kết luận.
- Đọc đối chiếu tài liệu tổng hợp, CI/guard, mail configuration/adapter, các delta U05/U09/U15 và runbook deployment. Không kiểm chứng toàn bộ từng business rule/UC bằng runtime.

Script kiểm tra và dữ liệu thô nằm trong `.tmp/check-aidlc-sync-20261107.cjs`, `.tmp/aidlc-sync-check-20261107.json`, `.tmp/aidlc-api-gaps-20261107.json`. Parser liên kết có assert kiểm tra trường hợp thường và đường dẫn được bọc trong dấu góc.

## Các điểm cần đồng bộ

### 1. API inventory vẫn công bố contract AI connection cũ và thiếu API nhóm

[API inventory](api-inventory.md), mục `AiConnectionController`, dòng 60–67 còn `PUT /api/v1/me/ai-connections/{provider}` và các thao tác theo provider. [Controller hiện tại](../../../backend/src/main/java/edu/aiplatform/ai/controller/AiConnectionController.java) có tạo kết nối bằng POST, cập nhật/đọc key/test/status/xóa theo UUID `{id}`, cùng GET/PUT `ai-source-preference`.

Ví dụ client dùng `OPENAI` theo placeholder provider cũ sẽ không đáp ứng tham số UUID của endpoint cập nhật hiện tại. Cần lấy lại annotation hiện hành, giữ đúng HTTP method, path và ý nghĩa ID.

Inventory cũng chưa có [GroupDocCollaborationController](../../../backend/src/main/java/edu/aiplatform/groupdocs/controller/GroupDocCollaborationController.java): preview/import DOCX, apply import và GET/PUT peer reviews. Mục GroupDocController còn thiếu lock/unlock assignment và revision/restore. Chỉ đổi tiêu đề baseline sẽ không sửa được các thiếu sót này. Các khác biệt API quiz còn lại là ứng viên cần đối chiếu khi đồng bộ đầy đủ, không dùng số lượng regex làm số endpoint sai đã xác nhận.

### 2. Mô tả deployment tổng hợp không phản ánh cơ chế VPS pull

[Build instructions](../../construction/build-and-test/build-instructions.md), dòng 23 và [build summary](../../construction/build-and-test/build-and-test-summary.md), dòng 27 vẫn ghi deploy job bị disabled. [Workflow CI](../../../.github/workflows/ci.yml) hiện có job `deploy-script` để shellcheck/test script; không có job SSH deploy. Sau push `main` thành công, cơ chế được triển khai là VPS tự poll workflow và chạy [platform-deploy.sh](../../../infra/deploy/platform-deploy.sh), như [runbook mục 10](../../../docs/runbooks/deploy-vps.md) và state hiện hành mô tả.

Cần phân biệt CI không trực tiếp deploy với cơ chế tự deploy trên VPS. Dùng mô tả hiện tại có thể khiến người đọc hiểu sai việc push main có dẫn tới triển khai hay không. Kiểm tra source/runbook này không xác nhận trạng thái VPS đang chạy hôm nay.

### 3. Thiết kế mail vẫn chỉ mô tả Brevo SMTP

[Application design](../application-design/application-design.md), dòng 25; [components](../application-design/components.md), dòng 25; [U01 tech stack](../../construction/u01-account-and-access/nfr-requirements/tech-stack-decisions.md), dòng 20; [U01 infrastructure](../../construction/u01-account-and-access/infrastructure-design/infrastructure-design.md), dòng 20; cùng domain/deployment architecture còn mô tả đường gửi production qua SMTP và `SMTP_*`.

[application.yml](../../../backend/src/main/resources/application.yml), dòng 93–95; [BrevoApiMailAdapter](../../../backend/src/main/java/edu/aiplatform/identity/integration/BrevoApiMailAdapter.java); [infra env example](../../../infra/.env.example) và runbook đã hỗ trợ `MAIL_TRANSPORT=smtp|brevo-api`, `BREVO_API_KEY` và HTTPS tới Brevo. SMTP vẫn là default; cần bổ sung lựa chọn API, không thay mọi mô tả SMTP thành API. NFR vận hành trong requirements cũng còn Brevo SMTP ở demo/production. Thiết kế cũ thiếu lựa chọn cần dùng khi mạng chặn SMTP.

### 4. Current code inventory thiếu 2 controller và 8 migration

[Current code baseline](current-code-baseline.md) chưa liệt kê `GradeDistributionVisibilityController` và `GroupDocCollaborationController`, dù controller thứ nhất đã xuất hiện ở API inventory và U09 delta.

Các migration chưa được liệt kê:

- AI: `V20261102_0900`, `V20261105_1200`, `V20261105_1300`.
- Grading: `V20261101_0900`.
- Groups: `V20261102_1000`, `V20261102_1200`.
- Quiz practice: `V20261101_1210`.
- Quizzes: `V20261101_1200`.

Cần cập nhật inventory từ source hiện hành, không tạo/đổi tên migration. Thiếu ở bảng tổng hợp không đồng nghĩa chưa có tài liệu chi tiết cho tính năng tương ứng.

### 5. Current baseline/evidence tổng hợp vẫn trỏ đợt 30/10

[Services](../application-design/services.md), dòng 3 và [current code baseline](current-code-baseline.md), dòng 7 còn gọi `5a76e428 + working tree` là source hiện tại. [Build summary / Current gates](../../construction/build-and-test/build-and-test-summary.md), dòng 19–23 vẫn đặt 1.611 backend, 599 frontend/94 files và hai lint warning dưới nhãn current/fresh.

Trong khi đó [báo cáo U09 ngày 06/11](../../construction/build-and-test/2026-11-06-u09-structure-full-verification.md) đã ghi 1.830 backend, 799 frontend/116 files và lint không cảnh báo; commit U09 là `1cddff35`. HEAD kiểm tra này còn có merge `f68a6ec5`, nên cũng không được gắn tự động kết quả trước merge thành acceptance mới cho HEAD.

Cần một mục source baseline rõ SHA/ngày và bảng evidence theo scope/commit; giữ nguyên con số trong báo cáo lịch sử, thêm liên kết tới bằng chứng mới. Các câu “chưa commit/deploy” trong snapshot có ngày được giữ theo thời điểm của chúng, không coi riêng là lỗi.

### 6. Hướng dẫn required PostgreSQL suites thiếu hai gate U09

[Build instructions](../../construction/build-and-test/build-instructions.md), dòng 21 chỉ nêu SubjectRepositoryIntegrationTest ≥31 và NotificationDeliveryIntegrationTest ≥2. [Script guard](../../../scripts/assert-required-backend-tests.ps1), dòng 6–10 còn yêu cầu EvaluationVersionIntegrationTest ≥10 và GradeDistributionVisibilityRepositoryTest ≥1, tất cả không lỗi/skip.

Cần ghi đủ bốn suite hoặc dẫn người đọc trực tiếp tới guard làm nguồn chuẩn. Lệnh gọi guard trong hướng dẫn đã đúng; đây là thiếu mô tả, chưa phải guard bị thiếu trong CI.

## Các phần đã đồng bộ và điểm nhỏ

- U09 as-built delta và [JDBC persistence](../../construction/u09-assignment-grading/code/jdbc-persistence.md) đã dùng `EvaluationRepository`, `GradingProposalRepository` và `model/format.ts` đúng với code sau commit `1cddff35`.
- Quy định JDBC toàn dự án phân biệt quyết định thiết kế với các owner còn JPA, không tuyên bố toàn bộ migration đã xong.
- U05/U09 as-built bảng repository vẫn ghi chung “SQL/JPA” dù hai unit đã JDBC. Đây là wording nhỏ cần sửa cùng đợt đồng bộ, không phải phát hiện code dùng sai persistence.
- Không tìm thấy đích link thiếu trong nhóm tài liệu hiện hành đã lọc; link hợp lệ không bảo đảm nội dung đồng bộ.

## Thứ tự sửa đề xuất

Sửa contract API và mô tả deploy/mail trước; bổ sung inventory controller/migration; cập nhật cổng tổng hợp baseline/evidence và required-suite guidance; cuối cùng dọn wording SQL/JPA. Phạm vi chỉ tài liệu, giữ nguyên lịch sử/audit và public contracts của code.

Không chạy lại test/build ứng dụng hoặc gọi dịch vụ/VPS trong lần kiểm tra này. Không sửa code/config, commit/push, migration hoặc triển khai. Các số test trong báo cáo này là evidence có ngày được đối chiếu, không phải run mới tại HEAD kiểm tra.
