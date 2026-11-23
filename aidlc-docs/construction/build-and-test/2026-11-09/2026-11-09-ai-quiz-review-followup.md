# AI quiz review follow-up — results

Implemented user-requested improvements on the existing working tree:

- Quiz AI proposal list reflects the actually displayed draft with Xem/Thu gọn and aria-expanded. Opening an automatically selected proposal also updates the list state. Closing/switching preserves unsaved-change confirmation.
- HTTP/shared request normalization permits absent/null/blank instructions only when materialIds is nonempty; default text is “Tạo quiz theo nội dung tài liệu đã chọn.” Frontend accepts document-only requests, shows the default as placeholder/help text and still requires instructions without documents. Existing size/count/source authorization checks remain.
- The existing generation call requests boolean requestMatchesSources, with system instructions to prioritize source documents over unrelated user instructions. False with selected sources becomes a fixed yellow sourceWarning above the proposal list. The warning is persisted alongside existing truncation warnings and appears on reopen. No additional provider call or keyword heuristic. Legacy AI outputs without this field remain readable; malformed types are rejected.
- Chấp nhận tất cả calls the existing accept API once with all nondropped questions using current edited content and quiz version. Invalid questions are opened for correction; no acceptance request is sent until all selected questions are valid. Individual accept/save remains. Proposal controls lock during saving; server validation/version checks remain authoritative.
- Existing automatic-source label fix remains; no null personal label is reintroduced.

## Verification

- Frontend red: five new behavior tests failed before implementation (`.tmp/ai-quiz-review-ui-red.log`). Backend red: missing-instructions/default/source-assessment cases failed before implementation (`.tmp/ai-quiz-review-backend-red.log`).
- Final frontend: 66 tests / five files passed (`.tmp/ai-quiz-review-frontend-tests.log`), scoped ESLint and standalone typecheck passed. Includes dropped/edited questions, invalid bulk blocking, pending-save lock, quiz version, document-only request and canceled collapse/switch preserving edits.
- Final backend: 98 tests / five suites passed, zero failures/errors/skips; executable JAR packaged (`.tmp/ai-quiz-review-backend-final.log`). Suites: QuestionControllerTest, QuizControllersTest, QuizAiDraftServiceTest, QuizDraftPromptTest and QuizServicesTest. Includes optional HTTP fields, default prompts for bank/quiz, persistent combined warnings, no-source guard, legacy output and invalid assessment type.
- Scoped application diff whitespace passed. Markdown links checked separately. No full-suite rerun or UI/browser test in this follow-up.

## Applying and limits

Restart the current local backend/worker with the new code; keep the old Docker worker stopped. Frontend dev reload uses current source. No additional migration, dependency or API endpoint; existing drafts stay unchanged. Previously generated drafts do not gain a retrospective relevance assessment.

The semantic warning is AI judgment and may be inaccurate. Source grounding is a prompt instruction, not proof that every generated question is supported; the author still reviews before accepting. No real provider request, app restart, commit, push or deployment performed here; provider availability and manual UI acceptance remain unverified at the user's preference. Security/Resiliency scope retained; PBT disabled.
