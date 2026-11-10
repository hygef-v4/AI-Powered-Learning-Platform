# Student published result entry

User reports the student cannot open the teacher's newly graded submission. Preserve pending grading changes and existing full-history deadline behavior; no grade edits, account switches, subagents or deployment.

- [x] Read-only local evaluation inspection confirms a published8/10 result on the latest submitted attempt while detail displays a newer in-progress attempt. Frontend chooses only latestAttempt; history page hides all graded submissions before deadline. Backend already enforces ownership/enrollment and suppresses unpublished score/feedback.
- [x] RED:3 failures/13 cases reproduce hidden result and blocked pre-deadline details.
- [x] Choose the existing submitted grading candidate independently of the current draft. Show a detail link only when the existing result API returns a published score; reuse the existing read-only document and feedback view.
- [x] Before deadline allow the current submitted grading candidate in the detail viewer, retaining restrictions on older full history and excluding drafts. Keep backend publication/ownership controls unchanged.
- [x] Connected assignment-work26 tests, GradebookServiceTest9 cases, final13 affected frontend cases, TypeScript/scoped lint and whitespace pass. Evidence: construction/build-and-test/2026-11-10-student-published-result-entry.md. No live student-account verification or VPS deployment.

Security: no new API or private grading access. Resiliency: existing load errors and fallback latest-attempt behavior retained. PBT disabled under existing configuration. Scope: detail page, result card, submission viewer and their regression tests; no schema/config/dependency changes.
