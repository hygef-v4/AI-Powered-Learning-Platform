# Server-managed MCP workflows

User request (raw): “giúp tôi lập kết hoạch và triển khai”. Approved scope follows the agreed read_agent_workflow proposal: serve the complete authoring and grading workflows through MCP, with no required local skill download. Existing external-agent chat initiation and platform UI review flow remain intact.

## Requirements and stories

- SW01: Teacher/Subject Manager can call read_agent_workflow with workflow AUTHOR_ASSIGNMENT or GRADE_WITH_RUBRIC and receive complete English instructions plus a deterministic content version. The tool is read-only and available with either token access level; it grants no write privileges. Student/Admin cannot discover or call educator workflows.
- SW02: Full instructions have one canonical source in backend classpath resources. Docker builds copy only backend/src today, so runtime must not depend on a sibling repository directory, working directory or network fetch. Existing agent-skills paths become optional thin wrappers that fetch the server workflow; full instructions are not duplicated.
- SW03: Capabilities and relevant tool descriptions direct the agent to fetch the appropriate workflow. ASSIGNMENT requirements refer to AUTHOR_ASSIGNMENT; QUIZ/BANK do not falsely inherit the assignment workflow. Existing gradingInstructions remain the API/MCP grading policy, not replaced by operational workflow prose.
- SW04: Existing backend authorization, read-progress/fingerprint/rubric/idempotency checks remain authoritative. Serving instructions does not enforce model obedience or prove semantic grading quality. No mandatory workflow-session table, new hooks, local installation or provider invocation.
- SW05: Update current user guides and U06/U07/U09 AI-DLC rules, plan, state, audit and verification evidence. Test a read-only MCP fetch without using local skills. Do not alter/publish assignments or grades to test this feature.

An educator starts in an external agent, which discovers the workflow through capabilities, reads it and asks clarifying questions in chat. It then uses current domain schemas/tools and returns exact reviewUrl links for human review on the platform. Optional local wrappers are compatibility/convenience only, not required setup.

No new dependencies, schema migration, private configuration changes, commit or push. Existing security/resiliency scope retained; PBT disabled.
