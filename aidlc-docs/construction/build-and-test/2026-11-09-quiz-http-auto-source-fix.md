# Quiz AI HTTP source validation follow-up

The selected reference documents were valid request inputs. The shared HTTP parser still required `source`, although the frontend sends null and the service supports automatic AI API source selection. This produced `Thiếu source` before document processing or AI dispatch.

- [x] Trace both quiz and question-bank callers of `AiDraftRequests`.
- [x] Reproduce missing/null source failures with selected material IDs through HTTP controller tests.
- [x] Make source optional in the shared parser; preserve enum/type validation for explicit values, material IDs, authorization and service selection rules.
- [x] Verify and package the updated backend.

Red: three automatic-source controller cases returned 400 instead of 201 before the fix (`.tmp/quiz-http-auto-source-red.log`). Green: 50 tests in QuizControllersTest, QuestionControllerTest and QuizAiDraftServiceTest passed with zero failures/errors/skips; executable JAR packaging passed (`.tmp/quiz-http-auto-source-green.log`). Frontend payload tests: two passed, including null API source/provider with selected document IDs. The requested Maven selector AiTaskServiceTest matched no class and contributes no claimed coverage.

The fix changes one parser argument and adds regression coverage. No additional migration, dependency or frontend production change. Restart the application using the newly built backend artifact. No running backend restart, live AI provider request or manual UI acceptance performed; the user requested to test UI themselves. Existing Security/Resiliency scope is retained; PBT remains disabled.
