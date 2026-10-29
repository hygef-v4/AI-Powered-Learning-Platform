# U05 navigation merge repair

## Scope
Repair merge 3edc6f1: main removed dashboardNavEntries while AcademicsShell still consumes it. User request: "giúp tôi sửa". This authorizes the isolated repair after the reviewed finding. Resume existing AI-DLC construction; retain current security/resiliency subset and disabled PBT. No new API, authentication, migration or full U05 completion.

## Plan
- [x] Reproduce TypeScript failure and trace the removed registry to the main branch contract.
- [x] Add a regression test using real registrations for all three U05 audiences; confirm failure before repair.
- [x] Read external menu items from navEntries, retaining local preview navigation and avoiding duplicate U05 menu entries.
- [x] Run focused and full frontend checks; record evidence and remaining limitations.

## Results
- Before repair, all four new AcademicsShell tests fail with the real undefined dashboardNavEntries runtime error; existing typecheck also fails with TS2305 and two downstream TS7006 errors.
- After repair, four focused tests pass. Tests cover real registrations for manager/teacher/student, sorted role-specific links, no duplicate local menu entries, local navigation callbacks, active state, preview controls and child content.
- Full frontend regression: 59 tests pass across 14 files. ESLint and TypeScript exit 0. Next.js production build exits 0, compiling and generating all pages including the dynamic /preview/u05 route.
- Source scope: AcademicsShell.tsx and AcademicsShell.test.tsx only. Backend, APIs, authentication and database unchanged. Backend's 123-test pass was verified during the preceding merge review; not rerun for this frontend-only repair.
- Existing extension subset retained: role filtering preserved, no external credentials/requests or infrastructure changes. Authentication/authorization remain existing integration work; PBT stays disabled. This repair does not complete U05 or advance unrelated stages. No commit/push performed.
