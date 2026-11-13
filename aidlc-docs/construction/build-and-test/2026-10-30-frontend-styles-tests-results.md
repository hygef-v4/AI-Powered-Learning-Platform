# Frontend styles and test separation — 2026-10-30

Scope follows the user's request to separate CSS and frontend tests from ordinary TS/TSX and update AI-DLC. Parent HEAD `5480c267`, working tree only; no commit/push requested.

## Layout

- All 94 test files move from `src/` into `frontend/tests/`, mirroring the paths below `src/`. The test-only Admin Dashboard fixture moves into the same test tree; setup moves from `vitest.setup.ts` to `tests/setup.ts`.
- Four feature stylesheets move into `src/features/{academics,announcements,materials,notifications}/styles/`.
- Global CSS, fonts and ErrorPage CSS move into `src/shared/styles/`; layout and components retain their existing style imports at updated paths.
- Vitest include becomes `tests/**/*.test.{ts,tsx}` and setup becomes `./tests/setup.ts`. The `@/` alias continues to resolve production source in `src/`.
- No CSS, application logic, test assertions, dependency/lockfile or backend changes. All 103 moves use `git mv`; import/export/mock/import-type references follow the new locations.

## Verification

Pre-move snapshots and AST comparison cover 509 TS/TSX/MTS files. Only resolved module paths change, plus the two specified Vitest discovery/setup strings. All seven CSS files retain their original contents. Source snapshots are saved under ignored `.tmp/` for final comparison.

- TypeScript: passed.
- ESLint: exit 0; two existing unused-import warnings (`navEntries`, `Icon`).
- Full Vitest: **599 passed across 94 files**, exit 0, `--maxWorkers=1`, 116.39 seconds. First two-worker run terminated in the native Windows runtime with worker teardown errors, exit -1073740791; no complete pass count claimed for that run. The retry changed worker count only; assertions and dependencies stayed unchanged. Existing React act/navigation warnings remain in test output.
- Production build and live HTTP verification: pending.

README, current source/Construction inventories and test instructions follow the new layout. Dated reports and prior audit entries remain historical evidence. Backend test/build evidence is from the prior unchanged-source run; backend tests are not rerun for this frontend-only relocation. Full browser/provider/load/restore acceptance remains outside scope.
