# AI grading button and error diagnosis — 2026-11-10

## UI change

The grading toolbar action now displays “Chấm bằng AI” permanently, with 36px height (previously 28px), larger horizontal padding and 14px bold text. Existing icon, accessibility label, request logic, member navigation and disabled guards remain. Below the small-screen breakpoint, the read-only toolbar places student selection and actions on separate rows so the text action fits. The existing measured toolbar offset keeps the score/comments clear of the taller toolbar.

## Reported PROPOSAL_SCHEMA_MISMATCH

Read-only runtime evidence identifies two attempts for evaluation 399d6cea-de68-42db-bfae-27a6ae4a329b:

- Task 156bc215-e59d-436e-8d17-1c739bda110d: provider finished at 12:00:53 +07; READY followed by delivery REJECTED.
- Task f4a853fa-a05e-4c99-a701-5a4aeac82e48: provider finished at 12:01:15 +07; READY followed by delivery REJECTED.
- Provider/model: GEMINI / gemini-3.5-flash-lite. Proposals 12 and 13 are REJECTED with the generic schema diagnostic, without a stored proposal payload.
- The worker checks JSON Schema before marking the task READY; the grading callback then performs normalization and rubric/feedback validation. The observed rejection comes from this grading validation stage. It was not applied as an AI proposal to the draft.
- Both tasks are handlerDone and result_payload is null. AiTask.handled deliberately removes prompt/schema/output after delivery. The generic diagnostic does not preserve which validation branch failed. Consequently the exact offending field cannot be established from these past attempts; no reproduced field-level cause or fix is claimed.

No provider retry or live grade write was performed to investigate this screenshot, and no backend validation was bypassed or modified. A precise diagnosis would require observing a new failed response at the validator boundary with a safe diagnostic/replay signal; guessing from old successful proposals would not reproduce this failure.

## Verification

- Regression first: the new test failed because the AI action had no visible text.
- Affected grading/document tests: 206 passed / 20 files.
- Typecheck and scoped ESLint passed.
- Browser fixture used the actual DocumentEditor and ToolbarButton components with representative student/history/AI actions. At 390px the action measured 140×36px, the toolbar wrapped to 85px tall, and page scrollWidth equaled 390px. At 1440px the action stayed 36px tall; toolbar bottom 177.33 and score top 185 confirmed clearance.
- Desktop/mobile screenshots inspected; fixtures use synthetic data, not the authenticated grading session.
- Production build passed; optimized compilation, TypeScript and all 32 static pages completed. Scoped diff whitespace validation passed. Temporary browser/server closed afterward.

Evidence remains ignored under `.tmp/grading-ai-button-*`. No backend rerun, authenticated end-to-end/provider acceptance, application restart, data migration, commit or push. The reported field-level AI validation failure remains unresolved; this change fixes the action's appearance only.
