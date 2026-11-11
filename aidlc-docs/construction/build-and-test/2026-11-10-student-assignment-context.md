# Student assignment class/subject columns

Added Lớp and Môn học to all3 tabs of the student assignment table using existing classCode and subjectCode values. Colgroup widths match new cells; loading/empty spans use7 columns or8 when submitted scores are shown. Existing TableFrame scrolls horizontally below the1000px table minimum. Missing values use an em dash; truncated codes retain a title.

Verification:23 assignment-work tests/5files pass, then final4 student-list cases pass. `npm run typecheck`, scoped ESLint and CRLF-aware `git diff --check` pass. Tests cover metadata for two classes/subjects, class filtering and submitted list/pagination restoration. Initial typecheck caught an unsupported Testing Library `exact` query option; removed it and reran checks.

Commands: `npm test -- tests/features/assignmentwork --maxWorkers=1`, final list-page test, `npm run typecheck`, ESLint on the page/test. Node heap512MiB. Logs `.tmp/student-assignment-context-{tests,final,typecheck,lint}.log`.

No backend/API/data change, dependency, account switching, live grade write, commit/push, deployment or subagents. Prior grading changes retained. Full frontend suite/build and live student/mobile visual acceptance not repeated. Existing authorization/privacy scope retained; no additional request. Infrastructure resiliency N/A and PBT disabled.
