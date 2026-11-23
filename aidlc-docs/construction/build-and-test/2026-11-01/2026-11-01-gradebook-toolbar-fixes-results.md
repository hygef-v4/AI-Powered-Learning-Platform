# Gradebook and toolbar fixes — 2026-11-01

## Root causes and changes

The pictured class gradebook returned HTTP 500 when calculating rubric totals. RubricRepository expected a top-level JSON array, but six local rows retained a historical object envelope with a `criteria` array and authoring metadata. The reader now recognizes that envelope and reads its criteria directly with Jackson's pointer reader, preserving IDs and decimal precision. Current arrays continue to work. Invalid criteria still fail instead of being silently replaced with an empty rubric. No migration, rubric rewrite, metadata deletion, score change or restoration of withdrawn authoring features was performed.

Editor ToolbarButton, paragraph-style select and image insertion controls now expose distinct enabled hover/focus backgrounds and pointer cursors. Disabled controls preserve their disabled appearance and default cursor. Active state, tool titles and existing document editing behavior remain.

Assignment header and the later sticky toolbar previously both had z-index 20. The later sibling painted above the header dropdown despite its internal z-index 40. Raising the assignment header to 30 keeps every dropdown item above the toolbar while retaining the surrounding layering hierarchy.

## Verification

- PostgreSQL regression first reproduced the envelope read failure. Final tests cover current-array roundtrip, envelope read via list/find with exact criteria/points/lock/version preservation, unchanged stored metadata, and malformed-envelope rejection.
- Assignment and grading backend suites: 175 passed, zero failures/errors/skips, including real PostgreSQL tests. Log: `.tmp/2026-11-01-rubric-green.log`. Test sources compile for Java 17.
- Four related frontend suites: 77 passed. Typecheck and targeted ESLint passed. Production build passed; log `.tmp/2026-11-01-toolbar-build.log`.
- Restarted the existing local backend terminal with the repository script; health returned UP. The same class gradebook request changed from HTTP 500 to HTTP 200 and returned expected assignment maxima and student rows. Orca opened the gradebook and class information pages: grading data rendered, submission progress showed the assignment and 0/1 count, and no HTTP 500 remained.
- Orca menu hit tests found all four menu items in front of the toolbar after loading the new page. Screenshot inspection confirmed the visible menu. Enabled table insertion hover used pointer and rgb(230,210,188), distinct from toolbar rgb(252,241,228); disabled undo retained its default cursor. Shared enabled styles also cover keyboard focus.
- `git diff --check` passed. Existing pending changes were preserved.

## Limits

No full project backend/frontend suite rerun, broad role/viewport/theme audit, provider or load test, production deployment, commit or push. CSS-only changes were verified in the browser rather than new tests mirroring class strings. No local persisted data was changed by these fixes or their runtime verification. Existing security/resiliency scope retained; PBT disabled.
