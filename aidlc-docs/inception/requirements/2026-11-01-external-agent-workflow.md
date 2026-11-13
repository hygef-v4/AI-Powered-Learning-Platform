# External-agent authoring and grading

User approved planning and implementation: “ok giúp tôi lập plan và triển khai”; follow-up: “nhớ sửa cả doc aidlc”. This continues the previously approved MCP work in the existing checkout. No commit, push, credential installation or model invocation is authorized by this change.

The user starts and clarifies work in Codex, Claude Code, OpenCode or Antigravity. The platform UI is the destination for viewing, editing and approving an assignment draft or grading proposal. No prerequisite platform form, new workflow session table or UI chat is introduced.

## Requirements and stories

- EA01: As an educator, I can describe my intent in agent chat. The agent resolves authorized existing context and asks one grouped clarification for choices that change the result: target/scope, topic/materials, difficulty, scale and individual/group mode for authoring; assignment/submission selection and overwrite scope for grading. It does not invent IDs or silently fill missing pedagogical choices. Explicit existing assignment settings need not be asked again.
- EA02: As an educator, I receive a reviewUrl for newly created or revised assignment proposals and single/batch/listed grading proposals. The grading URL opens the matching proposal in the existing flat review area, including an older proposal. URL selection never applies a proposal or saves a grade.
- EA03: Backend accepts new MCP grading writes only after that authenticated actor has read a contiguous full current grading snapshot. Track the highest served block offset by actor, evaluation and fingerprint in existing Redis with a 30-minute inactivity TTL. Reject skipped pages, missing/expired progress, changed policy/content/version and Redis failure. A result dropped by the batch byte budget must not advance progress. Repeated reads and concurrent overlapping pages are safe. Empty documents require their initial context read. API callbacks and historical/manual grading remain unchanged.
- EA04: Tool responses describe the external-agent workflow so skill installation is optional. Skills add conversational clarification and exact review handoff instructions; they never publish official grades. Missing intent is handled in agent chat, not by a mandatory platform form.
- EA05: Update current U06/U07/U09 AI-DLC business rules, plan, state, audit and build/test evidence. Do not rewrite dated evidence. State that read-progress checks prove served context, not attention or grading correctness.

## Design and constraints

Reuse existing MCP tools, native proposal records, assignment review query and flat grading workspace. Add no model provider call, database migration or dependencies. Redis already gates authenticated MCP requests; losing progress requires rereading, with no persisted work lost. Progress is account-scoped across that account's credentials and never stores student content or secrets. Current role, scope, submission hash/version and rubric guards remain authoritative on writes. Model-quality pilot still requires real teacher reference grades.
