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
- `US-IAM-007` permits an admin to issue a temporary password, while the service design says accounts are seeded without passwords and activated by one-time OTP. Resolve this conflict before writing the business rules.
- Session expiration values are not fixed in Functional Design. Keep expiration configurable and settle idle/absolute TTL under U01 NFR Requirements.
- Admin MFA is required by the security baseline. Keep the domain behavior factor-neutral here; select the concrete factor during NFR Requirements.

## 3. Clarification gate

- [ ] Answer the account onboarding question in [`u01-account-and-access-functional-design-questions.md`](u01-account-and-access-functional-design-questions.md).
- [ ] Review the answer for consistency with `US-IAM-001`, `US-IAM-007` and the OTP contract in `component-methods.md`.
- [ ] Resolve any follow-up ambiguity before generating Functional Design artifacts.

## 4. Functional Design artifacts

- [ ] Create `aidlc-docs/construction/u01-account-and-access/functional-design/business-logic-model.md` for activation, authentication, logout, reset, profile, role/scope changes and account lifecycle.
- [ ] Create `aidlc-docs/construction/u01-account-and-access/functional-design/business-rules.md` for identity uniqueness, password/OTP policy, session revocation, role inheritance, scope checks, account status and safe failure.
- [ ] Create `aidlc-docs/construction/u01-account-and-access/functional-design/domain-entities.md` for account, credential, role/scope assignment, activation/reset challenge, session reference, profile and authorization decision.
- [ ] Create `aidlc-docs/construction/u01-account-and-access/functional-design/frontend-components.md` for sign-in, activation/reset, profile and admin account/role workflows.
- [ ] Trace every rule and flow to the seven U01 stories and twelve IAM use cases.
- [ ] Review Security Baseline rules applicable to U01: SECURITY-03, 05, 08, 11, 12 and 15; review enabled Resiliency rules for OTP/email and auth dependency failures.
- [ ] Record extension compliance, unresolved findings and stage audit entry.
- [ ] Present U01 Functional Design for explicit review and approval before NFR Requirements.

## 5. Exclusions

- No implementation code, database migration or final OpenAPI schema in Functional Design.
- No audit query/reporting behavior (`US-AUD-001`), job lease/retry implementation, file artifact handling or lesson-progress state.
- No concrete session TTL, MFA factor, email provider or infrastructure selection; these belong to the corresponding NFR/Infrastructure Design decisions.
