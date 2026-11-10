# U09 AI rejection diagnosis and fix — 2026-11-10

User explicitly requests checking logs and fixing the AI error. Continue in place without subagents; existing grading changes remain intact.

1. [x] Read runtime evidence and replay the last successful proposal against the current rubric/validator. Old successful payload is accepted; past rejected outputs were cleared after delivery.
2. [x] Add safe validation-stage diagnostics and refresh the local API/worker after checking no AI tasks are in flight. Reproduce through the authenticated teacher API without applying a proposal or saving/finalizing grades.
3. [x] Capture the failing validation condition, construct a deterministic regression, fix the cause with existing contracts and security checks preserved, then verify the regression and connected suites.
4. [x] Refresh the local runtime with the tested fix and verify a new proposal can become READY. Record evidence and limits, including any provider calls. No official grade writes, commit or push.

Scope: grading proposal parsing/normalization/validation and prompt/schema contracts as evidence requires. Do not persist raw failed output or log provider values. Keep safe operational diagnostics and the established output-clearing policy. Security/Resiliency retained; PBT disabled.

[Results and evidence](../../build-and-test/2026-11-10-grading-ai-rejection-fix.md). Root cause: redundant provider-generated checks contradicted zero item scores. Provider requests now omit checks; the existing normalizer derives them. MCP's full proposal schema/validation remains intact. Two provider calls: one reproduction and one successful verification.
