# MCP authoring, grading and setup remediation — 2026-11-01

Implementation base: `mcp-test`, HEAD `a0e1ea4f`. Changes remain uncommitted. [Approved plan](../plans/code-generation-plan/2026-11-01-mcp-quality-remediation.md), [requirements](../../inception/requirements/2026-11-01-mcp-quality-remediation.md).

## Delivered behavior

- New API/MCP grading proposals require every locked rubric item score and structured feedback, consistent checks/totals, and evidence or a review reason. Manual grading and historical records are preserved. Exact legacy replay happens after current authorization/snapshot/state guards.
- API and MCP share grading instructions and complete assignment context. The context fingerprint includes assignment content so stale continuation is rejected. Review-context errors now fail explicitly rather than disabling evidence validation.
- Submission discovery uses authorized metadata, not a full submitted-document read for each row. Account contacts are fetched in bulk. No production latency target is claimed from unit call-count checks.
- The shared server AI limit is 30 rubric items. Authoring requirements and assignment detail expose it; larger graded rubrics show a warning while manual authoring/grading remains available.
- [Connection guide](../../../docs/guides/mcp-clients.md) and UI cover Codex, Claude Code, Antigravity and OpenCode; Gemini remains compatible. Latest user preference removes terminal commands from setup and CLI suffixes from UI names. Personal config examples use direct Bearer headers, with placeholder previews and explicit token-dialog copying. No personal config or real token was written during implementation.
- Two [repository-local skills and optional hooks](../../../docs/guides/mcp-agent-workflows.md) assist authoring and rubric grading. Gemini hook adapters use Node standard library, verified against documented event formats. No global installation and no claim of hook compatibility in other clients.
- [Quality pilot](../../../docs/guides/mcp-grading-quality.md) defines 20 cases and teacher-reference measurements. It contains no fabricated teacher grades or quality pass claim.

## Verification evidence

| Check | Result and scope |
|---|---|
| Backend runtime tests | 1,630 tests, zero failures/errors/skips with local Docker/PostgreSQL. The combined Maven process then exhausted Metaspace entering ArchUnit; original all-in-one command therefore failed. |
| Architecture rerun | 2 tests passed separately, Maven BUILD SUCCESS. Aggregate Surefire reports: 1,632 tests, zero failures/errors/skips. |
| Required database suite guard | Passed: SubjectRepositoryIntegrationTest 31 and NotificationDeliveryIntegrationTest 2, no skips. |
| Backend package | BUILD SUCCESS; tests skipped only for packaging after test execution. Local backend restarted from the new jar. `/health` reports UP. |
| Frontend full suite | 644 tests across 97 files passed before final client-guide refinements. |
| Final setup regressions | 15 tests across 3 affected suites passed after direct-header/no-terminal setup changes. Client-native formats, placeholder behavior and file locations checked. |
| Authoring warning regressions | Existing live AssignmentFormPage suite: 15 passed, including server-provided 30/31 boundary. Draft preview/setup focused checks also passed. |
| Hook self-check | 13 scenarios passed: malformed data, irrelevant tools, payload limits, duplicate request refs, feedback/total checks, partial results and safe summaries. |
| Final frontend validation | Production build and typecheck passed. Lint: zero errors, two existing unused-variable warnings in AI register and AccountDetailPage. |
| Browser | Existing authenticated Teacher session used through Orca CLI, with no Computer Use. Client selection and native config output checked. Mobile 390 px had no horizontal page overflow; desktop DOM width checked. Desktop screenshot capture had a host tiling artifact, so no desktop pixel-perfect claim. |

Regression tests first demonstrated incomplete-proposal acceptance, missing common context, document-heavy discovery and absent oversized-rubric warning. Fresh review found swallowed context failures and API context omissions; both were reproduced and corrected before the full backend verification. Skill pressure tests found safe baseline behavior and improved exact field instructions; no quantified improvement is claimed.

## Documentation rulings and remaining acceptance

### Follow-up — copy setup prompt

User requested a copyable prompt so the selected agent can set up MCP, then specified English prompt text. Added **Sao chép prompt** and a collapsed preview/manual-copy fallback to the shared guide/token-dialog component. Prompt reuses the client-native config and personal location, preserves unrelated servers, asks for private backup and read-only verification, and does not close the current agent session. General guide has an English placeholder and asks for direct personal-file entry; only the explicit token dialog includes a real supplied token and displays a disclosure before copying. No actual setup was executed.

Three new regressions first failed on the absent button; final affected suites passed 18 tests. Typecheck and targeted lint passed; production build passed after the change. Orca confirmed the button and collapsed preview on the authenticated Teacher guide. No live secret copied, global config edited or external agent invoked. Existing no-terminal user workflow remains available; prompt content is English, UI is Vietnamese.

The historical 2026-10-26 requirements already had a supersession notice; they were preserved. Existing token-reveal and current policy wording were reconciled in the current guide. Guide structure references [GitHub MCP installation guides](https://github.com/github/github-mcp-server/blob/main/docs/installation-guides/README.md) and [Context7 Codex setup](https://github.com/upstash/context7/blob/master/docs/clients/codex.mdx); actual config fields use the official client references linked from the guide.

Installed version/help checks: Codex 0.166.0, Claude Code 2.1.308, agy 1.4.12, OpenCode 2.2.5. These do not constitute fresh authenticated acceptance in each external application. Python diagnostic fixture was not run because no Python runtime is installed; Node was already available and used for new hooks.

Still pending: teacher reference grades and real model-quality pilot; representative SQL/latency/load measurement; authenticated create/proposal/review acceptance through each external client; live optional hook integration. Backend schema validity cannot guarantee a semantically correct grade. Official grading remains subject to teacher review. No provider calls, new credentials, global package/config installation, official publication, commit or push were performed.

Existing AI-DLC security/resiliency scope retained; PBT disabled. No new dependency or database migration. Optional local hooks are supplemental; authoritative authorization, rubric, version/hash and evidence-shape checks remain on the backend.
