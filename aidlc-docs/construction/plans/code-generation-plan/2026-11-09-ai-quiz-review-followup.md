# AI quiz review follow-up

User requests direct implementation and retains manual UI acceptance. Reuse current inline editor, warning provenance and versioned accept APIs; no dependency or migration.

- [x] Reproduce active Xem, document-only request and bulk acceptance issues with regression tests.
- [x] Show Xem/Thu gọn from the draft actually displayed; preserve unsaved-change confirmation when switching/closing.
- [x] Permit blank/missing instructions only with selected materials at HTTP/service/UI boundaries; normalize server-side to “Tạo quiz theo nội dung tài liệu đã chọn.” Keep prompt-only generation and validation limits.
- [x] Prioritize material contents in the existing generation prompt. Ask the same AI response for a boolean semantic relevance assessment; map false to a fixed yellow sourceWarning persisted with the draft. Preserve legacy outputs and existing truncation warnings. Treat this as model judgment, not proof.
- [x] Add Chấp nhận tất cả using the existing versioned accept API once, excluding explicitly dropped questions. Validate all remaining questions; show invalid cards and save nothing if any fail. Keep individual acceptance.
- [x] Run related frontend/backend tests, lint/typecheck and backend package. Synchronize current U10 requirements/UI/domain and evidence. No live provider submission or UI acceptance claimed.

One model call with structured metadata avoids unreliable keyword matching and a second provider request. The model can misjudge relevance or fail to follow source-grounding instructions; the user still reviews proposals before acceptance. Existing access/version/server checks remain authoritative.
