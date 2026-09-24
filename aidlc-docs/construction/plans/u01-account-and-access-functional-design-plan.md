# U01 Account & Access - Functional Design Plan

## 1. Scope and sources

- Unit: U01 Account & Access, current 16-unit plan.
- Stories: `US-IAM-001` through `US-IAM-007` only. `US-AUD-001` belongs to U02.
- Use cases: `UC-IAM-01` through `UC-IAM-12`.
- Review `unit-of-work.md`, `unit-of-work-story-map.md`, current Requirements/User Stories/Use Cases, `components.md`, `component-methods.md`, `services.md`, and the account/role sections of `global-database-erd.md`.
- Preserve current decisions: no public registration; one highest role per account; `SUBJECT_MANAGER` inherits instructor capabilities but remains subject-scoped; authorization is server-side; password reset and activation tokens are one-time OTPs sent by email and stored as hashes with TTL in Redis.
- U01 owns accounts, credentials, sessions, roles, scopes and authorization decisions. U02 owns append-only audit and job/outbox state. U01 emits audit events through U02's contract and does not query or mutate U02 storage.

## 2. Review findings carried into this design

- The old recovery plan incorrectly included `US-AUD-001` in U01; it is superseded by this plan.
- Resolved: accounts are created pending activation and activated by a one-time email OTP; administrators never set, issue or view passwords. Creation and import send no email; the system sends the activation OTP only when the user requests activation at first sign-in, with a neutral response and rate limiting. Administrators have no resend action; active users use self-service password reset. `FR-015`, `US-IAM-001`, `US-IAM-007`, `UC-IAM-01`, `UC-IAM-09`, `UC-IAM-10` and `component-methods.md` (`requestActivation`) were synchronized to this decision.
- Session expiration values are not fixed in Functional Design. Keep expiration configurable and settle idle/absolute TTL under U01 NFR Requirements.
- Admin MFA is required by the security baseline. Keep the domain behavior factor-neutral here; select the concrete factor during NFR Requirements.

## 2a. Quyết định Functional Design vòng 2

- Màn đăng nhập có liên kết "Kích hoạt tài khoản lần đầu" riêng; tài khoản chưa kích hoạt đăng nhập chỉ nhận lỗi trung tính.
- Mật khẩu tối thiểu 8 ký tự, có cả chữ và số.
- OTP 6 chữ số, hạn 10 phút, sai 5 lần thì hủy; mã mới làm mã cũ hết hiệu lực.
- Sai mật khẩu 5 lần thì khóa tạm 15 phút qua `locked_until`, tự mở, kèm giới hạn theo IP.
- Đổi role hoặc phạm vi thì tăng `credential_version` để thu hồi mọi phiên.
- U01 chỉ đặt `accounts.role`; U04 gán môn/lớp cụ thể. Hạ role bị chặn khi người đó còn phụ trách môn/lớp.
- Admin không tự hạ role hay tự khóa mình; luôn còn ít nhất một ADMIN hoạt động.
- Trạng thái tài khoản chỉ còn PENDING_ACTIVATION, ACTIVE, DISABLED; khóa/mở khóa là ACTIVE ⇄ DISABLED.
- Hồ sơ tự sửa: tên hiển thị, số điện thoại, ảnh đại diện. Ảnh đại diện đi qua `AvatarPort` do U03 cung cấp (cạnh `C` U01 → U03).
- Nhập hàng loạt chỉ CSV, tối đa 1000 dòng; email trùng báo lỗi dòng, không ghi đè.
- Đăng xuất chỉ thu hồi phiên hiện tại; đổi mật khẩu thu hồi các phiên khác.
- Cần đồng bộ ERD sau: bỏ `LOCKED` khỏi enum `accounts.status`, thêm số điện thoại và tham chiếu ảnh đại diện vào `accounts`.

## 3. Clarification gate

- [x] Answer the account onboarding question in [`u01-account-and-access-functional-design-questions.md`](u01-account-and-access-functional-design-questions.md).
- [x] Review the answer for consistency with `US-IAM-001`, `US-IAM-007` and the OTP contract in `component-methods.md`.
- [x] Resolve any follow-up ambiguity before generating Functional Design artifacts.

## 4. Functional Design artifacts

- [x] Create `aidlc-docs/construction/u01-account-and-access/functional-design/business-logic-model.md` for activation, authentication, logout, reset, profile, role/scope changes and account lifecycle.
- [x] Create `aidlc-docs/construction/u01-account-and-access/functional-design/business-rules.md` for identity uniqueness, password/OTP policy, session revocation, role inheritance, scope checks, account status and safe failure.
- [x] Create `aidlc-docs/construction/u01-account-and-access/functional-design/domain-entities.md` for account, credential, role/scope assignment, activation/reset challenge, session reference, profile and authorization decision.
- [x] Create `aidlc-docs/construction/u01-account-and-access/functional-design/frontend-components.md` for sign-in, activation/reset, profile and admin account/role workflows.
- [x] Trace every rule and flow to the seven U01 stories and twelve IAM use cases.
- [x] Review Security Baseline rules applicable to U01: SECURITY-03, 05, 08, 11, 12 and 15; review enabled Resiliency rules for OTP/email and auth dependency failures.
- [x] Record extension compliance, unresolved findings and stage audit entry.
- [ ] Present U01 Functional Design for explicit review and approval before NFR Requirements.

## 5. Exclusions

- No implementation code, database migration or final OpenAPI schema in Functional Design.
- No audit query/reporting behavior (`US-AUD-001`), job lease/retry implementation, file artifact handling or lesson-progress state.
- No concrete session TTL, MFA factor, email provider or infrastructure selection; these belong to the corresponding NFR/Infrastructure Design decisions.

## 6. Extension compliance

### Security Baseline

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | BR-U01-90, 91: audit mọi sự kiện bảo mật, không ghi mật khẩu, OTP, token, số điện thoại |
| SECURITY-05 | Compliant | Chuẩn hóa và kiểm email, domain, hồ sơ, CSV từng dòng |
| SECURITY-08 | Compliant | F13 mặc định từ chối, kiểm role + phạm vi phía server, chặn truy cập theo ID của người khác |
| SECURITY-11 | Compliant | Phản hồi trung tính chống dò tài khoản; bảo vệ admin cuối cùng; nhập hàng loạt không tạo ADMIN |
| SECURITY-12 | Ngoại lệ được chấp nhận | Đạt: ≥ 8 ký tự, bcrypt, khóa tạm sau 5 lần sai, phiên hết hạn và thu hồi khi đăng xuất. Người dùng chủ động bỏ kiểm danh sách mật khẩu bị lộ và MFA admin tại U01 NFR Requirements; `SEC-002` đã sửa theo |
| SECURITY-15 | Compliant | BR-U01-93, 94: không kiểm được quyền thì từ chối, lỗi an toàn |
| Các rule còn lại | N/A | Hạ tầng, header, dependency, alerting thuộc NFR/Infrastructure/Build |

### Resiliency Baseline

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| RESILIENCY-10 | Compliant | Gửi OTP qua outbox U02 với retry hữu hạn; lỗi email không làm hỏng yêu cầu (BR-U01-92) |
| Các rule còn lại | N/A | Mức Functional Design không quyết định topology, backup, alarm |

Không có blocking finding.

## 7. Giả định do AI thêm, cần bạn duyệt

- BR-U01-13: kích hoạt xong không tự đăng nhập, người dùng đăng nhập lại.
- BR-U01-34: đặt lại mật khẩu qua OTP thu hồi mọi phiên; đổi mật khẩu giữ phiên hiện tại.
- BR-U01-84: nhập hàng loạt không được tạo tài khoản ADMIN.
- BR-U01-72: mở lại tài khoản chưa từng kích hoạt thì về PENDING_ACTIVATION.
- Gửi OTP đi qua outbox U02 và handler mail của U01, không chờ U16 ở wave 4.

