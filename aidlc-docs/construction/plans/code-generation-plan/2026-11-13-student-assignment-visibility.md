# Keep closed unsubmitted assignments visible

Approved scope: user requested fixing the diagnosed omission with Ponytail. The previous diagnosis/proposal covers a discoverable closed/unsubmitted category and matching list/detail labels. Work in the current checkout, preserving diagnosis documents already modified; no subagents.

- [x] Trace callers: StudentAssignmentsPage, shared rowStatus used by StudentAssignmentDetailPage, ClassTodoCard's actionable upcoming/doing predicates, existing URL state and tests.
- [x] Add failing regression tests through the real list page for CLOSED group/individual unsubmitted rows, closed submitted rows, tab counts, filtering and return-focus navigation; test the shared closed-group label. RED: nine failures and seven passing controls in two files.
- [x] Add a bounded closed/unsubmitted predicate in the existing hook module and use it for the new Đã đóng tab plus shared row label. Preserve active-work rules and submitted histories; no backend, API, schedule or permission changes.
- [x] Verify affected tests, full frontend tests, ESLint and TypeScript with the existing generated-type environment limitation recorded if needed. GREEN: 40 affected tests; full frontend 1047 tests/150 files; full lint and isolated TypeScript pass. Standard typecheck still hits stale generated route references; no cache deletion or product config change. Temporary verification config removed.
- [x] Update current U08 frontend documentation, diagnosis evidence, state and audit with the actual outcome and unverified deployed-record limitation.

User-facing behavior: closed unsubmitted assessments appear in Đã đóng with the existing Hết hạn label, including links back from notification detail via focus. Closed submitted assessments remain under Đã nộp. Chưa làm/Đang làm and the class todo summary retain their actionable meaning. Existing tab URL values remain compatible; new value is closed. No commit/push/deploy authorized in this turn.

Security/resiliency unchanged: only classification of already authorized API rows changes. Existing query failure/cancellation, class scope and group submission guards remain. PBT disabled; no infrastructure work needed.
