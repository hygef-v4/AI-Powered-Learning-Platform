# Student assignment class and subject columns

User requests visible class/subject columns in the student assignment list. Existing StudentRow includes classCode and subjectCode; no backend or API change needed. Keep all3 tabs, class/kind/search filters, pagination and navigation. Use existing TableFrame horizontal overflow on narrow screens; retain titles for truncated codes and neutral fallback for missing context.

- [x] Inspect page, hook, DTO, callers and existing tests; retain previous grading changes. Scope: StudentAssignmentsPage.tsx and its existing test.
- [x] Add Lớp and Môn học columns, matching colgroup widths and loading/empty spans; update existing list/filter/submitted assertions.
- [x] Assignment-work23 tests/5files pass; final4 list-page cases pass after correcting a test query typing option. TypeScript and scoped lint pass. Shared TableFrame supplies horizontal overflow; no live account switching or data writes. Evidence: construction/build-and-test/2026-11-10-student-assignment-context.md.

Existing authorized scope and Product.md/Impeccable refinement guidance apply. Security/Resiliency retained: reuse authorized list response, no extra fetch or data writes. PBT disabled. No subagents, new dependency, migration, commit/push or deployment. No broader requirements/design needed for this isolated display change.
