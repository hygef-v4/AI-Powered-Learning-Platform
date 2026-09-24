# Superseded: U01 Platform Foundation and Identity - Functional Design Recovery Plan

> This recovery plan targets the old eight-unit boundary and incorrectly includes `US-AUD-001` under U01. Do not answer its questions or generate its artifacts. Current U01 is Account & Access; use `u01-account-and-access-functional-design-plan.md` and its linked clarification file.

## 1. Mục tiêu

Tái tạo Functional Design cho U01 từ bộ Inception đã duyệt và các quyết định còn truy vết được trong audit. U01 bao phủ tám stories: `US-IAM-001` đến `US-IAM-007` và `US-AUD-001`.

## 2. Artefact cần tái tạo

- [ ] `business-logic-model.md`
- [ ] `business-rules.md`
- [ ] `domain-entities.md`
- [ ] `database-design.md`
- [ ] `api-contract.md`
- [ ] `frontend-components.md`
- [ ] `foundation-contracts.md`
- [ ] Kiểm tra đủ 8/8 story, Security/Resiliency compliance, Markdown và traceability
- [ ] Trình checkpoint Functional Design U01

## 3. Quyết định đã khôi phục, không hỏi lại

- Backend Spring Boot modular monolith; frontend Next.js; REST `/api/v1` và OpenAPI.
- Mỗi account có một highest role; `SUBJECT_MANAGER` kế thừa capability giảng viên nhưng quyền dữ liệu phụ thuộc subject/class assignment.
- Admin bootstrap chỉ seed identity/role/status; mật khẩu được đặt qua activation link, không hardcode credential.
- File upload bị quarantine cho đến khi real malware scanner trả trạng thái sạch; scanner là thành phần MVP bắt buộc.
- Worker là project/process/container riêng; frontend không gọi worker trực tiếp.
- Audit append-only, không có update/delete API; production retention tối thiểu 90 ngày.
- Password dùng adaptive hash, kiểm tra breached-password list; admin hỗ trợ MFA; session server-side có thể revoke.
- File, job, audit, idempotency và authorization là foundation contracts dùng chung cho các unit khác.

## 4. Câu hỏi recovery cần làm rõ

### Question 1
Khi quản trị viên tạo/import tài khoản người học hoặc giảng viên, cơ chế bắt đầu sử dụng tài khoản nên thống nhất thế nào?

A) Luôn gửi activation link để người dùng tự đặt mật khẩu; không cấp mật khẩu tạm thời. (Đề xuất, phù hợp admin bootstrap đã duyệt)

B) Tạo đơn lẻ dùng activation link; import hàng loạt có thể cấp mật khẩu tạm thời bắt buộc đổi lần đầu.

C) Quản trị viên chọn activation link hoặc mật khẩu tạm thời cho từng lần tạo/import.

D) Other (please describe after [Answer]: tag below)

[Answer]: 

### Question 2
Chính sách thời hạn phiên đăng nhập cho MVP nên là gì?

A) Access/session không hoạt động hết hạn sau 30 phút; refresh/session tối đa 7 ngày; đổi mật khẩu, khóa tài khoản hoặc phát hiện reuse sẽ revoke toàn bộ phiên liên quan. (Đề xuất)

B) Phiên không hoạt động 60 phút; tối đa 30 ngày; vẫn revoke khi có sự kiện bảo mật.

C) Chỉ dùng phiên 8 giờ, không có refresh session dài hạn.

D) Other (please describe after [Answer]: tag below)

[Answer]: 

### Question 3
Allowlist email trường nên được cấu hình ở phạm vi nào?

A) Một danh sách domain/subdomain cấp nền tảng do quản trị viên quản lý; áp dụng cho mọi tài khoản người học/giảng viên. (Đề xuất)

B) Mỗi môn hoặc lớp có allowlist riêng.

C) Một domain cố định trong cấu hình triển khai, không có màn hình quản trị.

D) Other (please describe after [Answer]: tag below)

[Answer]: 

### Question 4
OpenAPI contract cho frontend và internal worker nên được quản lý thế nào?

A) Một contract nguồn được version hóa nhưng build hai surface: public/frontend API và internal worker API; production không công khai Swagger UI. (Đề xuất)

B) Một OpenAPI contract duy nhất chứa cả public và internal endpoints; authorization phân biệt quyền truy cập.

C) Chỉ public API dùng OpenAPI; internal worker contract dùng message schema riêng và không có internal HTTP API.

D) Other (please describe after [Answer]: tag below)

[Answer]: 

### Question 5
Worker lấy input và trả kết quả job bằng cơ chế nào?

A) Backend phát versioned message qua queue; worker dùng scoped internal API/artifact access để lấy input và phát result event qua queue. Backend là owner duy nhất của business state. (Đề xuất)

B) Backend phát job qua queue; worker đọc/ghi trực tiếp database nghiệp vụ dùng chung.

C) Backend gọi HTTP worker và worker callback HTTP về backend.

D) Other (please describe after [Answer]: tag below)

[Answer]: 

## 5. Gate

Không sinh lại artefact U01 cho đến khi năm câu hỏi trên được trả lời và kiểm tra không còn mâu thuẫn.

