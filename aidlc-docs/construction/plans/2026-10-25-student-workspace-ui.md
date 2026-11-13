# Student workspace UI remediation

User authorized planning and implementation after the UI review.

## Design and scope
Keep the existing document model, Draw.io integration, autosave and submit contracts. Enhance the individual attempt workspace with an opt-in editor presentation so authoring, group work and read-only viewers retain their behavior.

- Label answer areas, keep teacher prompts locked, show writing guidance and focus borders.
- Add diagrams directly to the chosen leaf section, without relying on previous focus.
- Keep save status, manual save and submit in a sticky bottom action bar; retain the confirmation dialog.
- Group DOCX import/export in a native disclosure menu. Add a reversible focus toggle that hides only the sidebar.
- Label remaining time as time to the submission deadline. Do not derive an exam countdown from instructions.

An editor rewrite or API/schema change is unnecessary for these UI issues. Reuse current insertion and persistence methods.

## Implementation checklist
- [x] Regression tests for section placement, write affordance and read-only/author isolation.
- [x] Implement opt-in answer layout and section actions.
- [x] Implement workspace action bar, document menu and focus mode.
- [x] Run frontend tests, typecheck, lint and production build.
- [x] Record results and runtime verification limits.

Do not edit or submit the real student draft during visual verification. Existing unrelated changes in audit/state and the route repair script remain untouched.

## Verification results
- Full frontend suite: 85 files, 506 tests passed.
- Final affected editor/workspace tests: 35 tests passed after the last guidance adjustments.
- Typecheck, full lint, final affected-file lint and production build passed.
- Manual UI verification remains pending: localhost:8090 was unreachable from both the browser and shell. The temporary dev server was stopped.
- Updating the running Docker frontend was attempted through scripts/repair-student-routes.ps1 but failed at docker ps because access to the Docker Desktop named pipe was denied. No containers changed. The existing 8088 image has not received this update.
- No real student draft was edited or submitted. No commit or push performed.
