# Project bug review — 2026-11-12

Scope: current checkout `65fcc764c5aa9e28f2ff6201ea0bb88096be89b6`, branch `feat/ai-prompt-guardrail`. User requests inspection and a report, not source fixes. Reuse existing AI-DLC architecture and requirements; design, code generation and operations are not applicable.

- [x] Read project instructions/conventions, identify modules and verification commands.
- [x] Run frontend suite (1,018 tests passed); inspect lint and generated-type failures.
- [x] Run backend Maven verification and required integration-suite guard (2,342 passed, no skips).
- [x] Review backend/frontend module boundaries and high-impact business flows.
- [x] Validate four findings with precise source locations and targeted local reproductions.
- [x] Write Vietnamese report with severity, impact, evidence and verification limits; update audit/state.

Report: [review results](../build-and-test/2026-11-12-project-bug-review.md). Production frontend build, clean-cache typecheck, lint, 14 MCP hook scenarios and deploy ShellCheck pass. Python MCP fixture and live browser/provider/production checks remain outside verified coverage.

Security and Resiliency remain enabled in the existing project scope. Property-based extension remains disabled. Review conclusions are evidence-based, not a certification that every possible defect is absent.
