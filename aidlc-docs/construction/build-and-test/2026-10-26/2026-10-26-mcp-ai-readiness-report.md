# MCP and AI readiness — 2026-10-26

## Verdict

The platform MCP HTTP endpoint is reachable through the frontend proxy and directly through the API. At initial audit both teacher and student had zero MCP credentials. After the user approved a student-scoped test token, the Codex CLI successfully discovered the saved platform MCP configuration and completed a read-only tool call. The teacher still has no MCP credential.

The current Chrome session is **U05 Mock Student**, verified on `/ai/connection`. The page loads successfully and shows zero personal API keys and zero connected agents, matching database metadata. The teacher connection is separate; do not infer its availability from this student page or replace the user's current login.

## Configuration evidence

- Global Gemini/Antigravity and Codex configurations contain `unityMCP` pointing to `http://127.0.0.1:8080/mcp` without a bearer header. Neither contains a dedicated platform MCP entry. The endpoint address overlaps with this project's API but is not evidence of an authorized platform connection. Existing Unity entries were preserved.
- Teacher `u05.teacher@school.test`: one ACTIVE personal GEMINI connection, model `gemini-3.5-flash-lite`, last saved connection test result `Đạt`. No key values were read into output. No teacher OpenAI/Claude connection; no shared provider connection.
- Student `u05.student@school.test`: no personal provider connection. Practice feedback uses the requesting student's personal API connection; the teacher connection does not cover it.
- Encryption environment variable is present with the expected decoded key length. This format check alone does not prove decryption/provider access; existing successful tasks provide historical operational evidence.
- Historical database task counts at audit time: ASSIGNMENT_DRAFT READY 7 / FAILED 1 / UNKNOWN 3; GRADING_PROPOSAL READY 10 / FAILED 2. No QUIZ_QUESTION_DRAFT or PRACTICE_FEEDBACK task history. Counts are historical, not new provider calls in this audit.

## Feature readiness

| Feature | Configuration / evidence | Still to accept |
|---|---|---|
| Assignment AI draft | Teacher personal Gemini available; historical successful tasks; service/tool regression passes | Fresh disposable draft through UI |
| Grading AI proposal | Same teacher connection; historical successful tasks and prior U09 UI verification; workflow/tool regression passes | Fresh UI request on a test submission if desired |
| Quiz AI draft | Service/tool implementation and regression passes | Fresh UI/provider execution; no historical task evidence |
| Practice AI feedback | Service regression passes; student's own key required | Student currently lacks a key; configure only if this flow is wanted, then UI test |
| School API source | No shared connection configured | Admin configuration required only if school source is wanted |
| External agent MCP | Codex CLI global registration, initialize/list, and read-only tool call passed using a student-scoped token | Teacher-scoped connection is not configured; test student token expires after 7 days |

OpenAI/Claude and school API absence is configuration availability, not a reproduced application defect. Existing provider key/model were left intact. A successful small connection test does not prove all generation schemas or inputs work.

## Changes

- Added `scripts/check-mcp.ps1`: hidden SecureString token prompt; verifies initialize, ping, tools/list and platform identity; never invokes tools. Disables redirects, restricts plaintext HTTP to loopback, excludes remote error text and token from diagnostic output.
- Added `scripts/test-check-mcp.py`: local HTTP fixture for success, HTTP 401, JSON-RPC error, wrong response ID and wrong server identity. Uses only a dummy token and asserts it is absent from captured output.
- Updated MCP guide with actual tool names, separate client setup, secure Codex environment handling, diagnostics, and acceptance limitations. Corrected stale hardcoded model guidance and clarified personal/shared/Practice ownership.
- No application source, provider key/model, client credential, grade, or unrelated Unity registration changed. Preserved `tmp-unit-report-reference/`. No commit/push.

## Fresh verification

- Backend focused suite: **11 classes, 81 tests; 0 failures/errors/skips**, Maven exit 0. Classes: McpServerTest, McpCredentialServiceTest, AiConnectionServiceTest, AiTaskFlowTest, AssignmentAiDraftServiceTest, AssignmentMcpToolsTest, PracticeFeedbackServiceTest, GradingMcpToolsTest, MaterialMcpTransactionTest, QuizAiDraftServiceTest, QuizMcpToolsSchemaTest.
- Frontend AI connection/history suite: **2 files, 9 tests passed**, exit 0. No frontend application changes, so no new production build required for this patch.
- Diagnostic fixture: **2 test methods covering 5 protocol scenarios**, passed with PowerShell 7 and Windows PowerShell 5.1. An initial fixture failure with Windows PowerShell came from inheriting PowerShell 7 module paths via Python; fixture now clears that inherited path when launching 5.1. No application configuration change was needed.
- Both `:3000/mcp` and `:8080/mcp`: missing/invalid token **401**, Origin header **403**, GET **405**. These are negative security checks, not successful authentication.
- API/worker stopped before Maven checks and restarted via `scripts/run-backend.bat`; API PID 46996 listens on 8080, frontend PID 47312 on 3000; `/health` returns UP. Launcher selects `local,worker`. Old Docker worker remains stopped; PostgreSQL/Redis/RabbitMQ healthy.
- Existing Docker nginx container is restart-looping. This audit used the Next proxy on port 3000 and API on 8080 successfully; Docker nginx ingress is not accepted by these checks. Existing backend SSE timeout/closed-stream warnings were observed before restart and not diagnosed as MCP/provider failures.

See [manual guide](2026-10-26-mcp-ai-manual-test-guide.md). No new Gemini/OpenAI provider generation was performed in this audit. Extension scope: the user approved one 7-day, student-scoped, read-only MCP credential; no write access or provider/remote deployment changes were granted.

## Follow-up: user selected Codex CLI

Registered global Codex MCP server `nen-tang-hoc-tap` with URL `http://localhost:3000/mcp` and `bearer_token_env_var = LEARNING_PLATFORM_MCP_TOKEN` using the installed CLI. Re-read TOML to verify both fields and preservation of `unityMCP`. No bearer token was saved. The current process has no token environment value, so authenticated handshake/tool discovery remains pending. Create a platform read-only token and launch Codex from the terminal with that token environment as described in the guide; restarting without the environment value is insufficient.

The user approved a 7-day `READ_ONLY` credential for the currently signed-in student. With its token read from the browser clipboard into process memory (never printed or passed as a command argument), the installed Codex CLI loaded its global `nen-tang-hoc-tap` entry, discovered three student-visible read-only tools, and successfully called `read_authorized_material` for an authorized active class material. CLI execution was ephemeral/read-only, and unrelated configured MCP servers were disabled only for that invocation. The clipboard was cleared afterward. The database shows the credential active and used; it expires 2026-11-02. The browser has a second safe status tab showing it masked and last used, and the one-time token dialog remains open in the first tab for the user to copy if needed. The token value is not stored by Codex; each Codex process that should connect must receive `LEARNING_PLATFORM_MCP_TOKEN` in its environment.

## Follow-up: teacher UI and Gemini assignment draft — 2026-10-26

- Signed into the teacher account in a separate in-app browser tab; kept the existing Chrome student session and its MCP-token handoff tabs intact. The teacher UI showed `u05.teacher@school.test`.
- Re-ran **Kết nối AI → Kiểm tra** for the existing personal Gemini connection. The UI returned **Đạt** at 15:31 local time. No key or model was changed or displayed.
- Created a separate, unpublished QA assignment draft in class U05-A: `QA Gemini draft smoke test 2026-10-26`, assignment ID `6fde44e6-05f1-4321-8d21-659ce2879c31`. The existing published evaluation and its student grade were not opened for mutation.
- First Gemini proposal completed but its rubric totaled 11.33/10 (3.33 + 4 + 4). The UI correctly marked the proposal invalid and disabled **Thêm vào bài tập**. Retried with an explicit 3 + 4 + 3 allocation.
- Second proposal passed the UI validator: **3 câu**, **3/3 phần có rubric**, **9 tiêu chí**, total **10.00/10**. Applied it to the QA draft. Because **Thay tiêu đề bài bằng tiêu đề đề xuất** was selected, applying first changed the draft title; restored the QA label and waited for the editor's **Đã lưu lúc 15:39** status.
- Reloaded the editor and confirmed the QA label, all three questions, nine criteria, fractional rubric maxima of 1.5 + 1.5 in question 2, and the 10-point total persisted. The draft remains unpublished and unscheduled.
- Teacher MCP: the user's teacher token (label teacher-mcp, **Đọc và ghi**, expires 25/11/2026) was used only for read-only validation. Codex global config resolves nen-tang-hoc-tap to http://localhost:3000/mcp and LEARNING_PLATFORM_MCP_TOKEN. The diagnostic confirmed server ai-learning-platform, protocol 2025-06-18, and 7 tools. A temporary Codex CLI run successfully called read_assignment_context for the QA assignment and returned its title. After the user requested persistent setup, LEARNING_PLATFORM_MCP_TOKEN was stored in the Windows user environment; the token is not in config.toml or the repository. A fresh process read the value from the user environment and repeated the read-only MCP call successfully. No write tool was called; clipboard was cleared.
- The Codex CLI run also logged transport errors for two other configured local MCP endpoints (127.0.0.1:8722 and localhost:8085). These did not prevent the platform MCP read call; those other registrations were not changed.
- Still untested: AI quiz generation, a fresh grading proposal, student Practice feedback, fractional manual grading entry/over-limit rejection, and task recovery across reload. The only available reference grade was already published, so it was left untouched; the student has no personal AI provider connection.
