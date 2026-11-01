# Gradebook read compatibility and editor interactions — 2026-11-01

User requested fixing HTTP 500 in class submission progress/gradebook, toolbar icon hover affordances and a header dropdown obscured by the editor toolbar. Preserve all other pending changes.

- [x] 1. Reproduce the class gradebook request and trace the backend exception. Six local rubric rows contain a historical object envelope with a `criteria` array; the current repository accepts only a top-level array. Header and toolbar share z-20, allowing the later toolbar to cover the header dropdown.
- [x] 2. Read both the current array and the recognized object envelope in RubricRepository without rewriting stored data. Reject malformed shapes; retain all criteria, IDs, item points and existing scoring rules. Add a PostgreSQL repository regression for both shapes and invalid input.
- [x] 3. Add visible hover/pointer/focus affordances to enabled editor tools, preserve disabled states, and place the header dropdown above the toolbar using the existing stacking hierarchy.
- [x] 4. Run relevant backend/frontend checks, production build and live Orca verification of gradebook/progress plus toolbar/menu layering. Record outcomes and limits in [verification results](../../build-and-test/2026-11-01-gradebook-toolbar-fixes-results.md).

No new features, dependencies, schema changes, data conversion, commit or push. Existing AI-DLC security/resiliency scope retained; PBT remains disabled. U07 owns rubric reading and editor UI, U09 consumes rubric totals for gradebook/progress.
