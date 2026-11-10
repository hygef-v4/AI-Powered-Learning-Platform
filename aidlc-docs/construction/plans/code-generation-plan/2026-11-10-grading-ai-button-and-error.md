# U09 AI grading action and reported error — 2026-11-10

Continue the existing authorized grading follow-up without subagents.

1. [x] Reproduce the icon-only action with a failing workspace test requiring visible “Chấm bằng AI” text and a larger control.
2. [x] Increase the existing action from 28px to 36px, with larger padding/type and a permanent label. Stack the read-only toolbar at narrow widths so the longer action fits; preserve request, member navigation and disabled guards.
3. [x] Inspect the reported AI task using read-only Orca terminal/database evidence. Explain the confirmed rejection stage and explicitly distinguish it from an identified field-level cause. Do not fabricate a payload or relax validation.
4. [x] Verify affected frontend tests, lint/typecheck/build and desktop/mobile component fixtures; record results and remaining diagnostic limits.

Scope: GradingWorkspacePage, DocumentEditor's narrow read-only toolbar layout, workspace regression test, AI-DLC records. No backend/data/provider call, grade write, runtime restart, commit or push. Security/Resiliency retained; PBT disabled.

[Results and diagnostic limits](../../build-and-test/2026-11-10-grading-ai-button-and-error.md).
