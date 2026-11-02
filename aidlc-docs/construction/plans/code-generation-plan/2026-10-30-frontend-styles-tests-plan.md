# Frontend: separate styles and tests — 2026-10-30

User requested CSS and frontend tests separate from ordinary TypeScript/TSX, with AI-DLC updated. Scope is file relocation and required path/configuration/documentation updates; preserve all production logic, CSS contents and test assertions. No commit or push requested.

- [ ] Move feature CSS into each feature's `styles/`; move global/shared CSS into `src/shared/styles/`.
- [ ] Move frontend test files and test-only fixtures into `frontend/tests/`, mirroring the corresponding paths below `src/`. Move Vitest setup to `tests/setup.ts`.
- [ ] Update import/export/mock/import-type paths and Vitest discovery/setup configuration. Keep the existing `@/` source alias.
- [ ] Update README and current AI-DLC inventories/test instructions; preserve dated audit/report evidence.
- [ ] Verify unchanged CSS and production/test ASTs apart from module paths; run all frontend tests, lint, typecheck and production build.
- [ ] Restart the frontend in its existing Orca terminal and verify HTTP responses. Leave changes available for review without committing.

Backend source/configuration and running backend/dependency services remain outside this change. Existing full runtime/provider acceptance gaps remain unchanged.
