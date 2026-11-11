# Chọn bài hàng loạt ngay trong bảng chấm điểm

User approved proposal with “ok giúp tôi sửa”. Inline, no subagents/commit/push/deploy.

- [x] Replace duplicate checkbox list/toggle with row and indeterminate header checkboxes,20 rows/page, page selection and explicit select-all-filtered up to100 submissions. Unsubmitted rows disabled.
- [x] Keep selections across pages, reset on assignment/search/status/mode changes; show action-specific counts and preserve batch confirmation/error/version behavior. Use current submission-directory paging, no backend changes.
- [x] Group actions target group-root evaluations once; retain access to member grades separately. No batch member grading.
- [x] Tests for30+ rows, page/all/filter/reset, maximum100, eligibility, loading/error and correct batch ID/version. Verify related tests/typecheck/lint and real local browser layout without sending grade/AI writes.

Reuse UI/table/pagination and BatchActionDialog. Retain existing security/resiliency and server authorization/version checks; PBT disabled. No new dependency/API or live class mutations.

## Evidence and limits

- RED4 new tests failed before implementation. GREEN189 grading tests/20 files; after final directory-reuse and100-limit additions,20 targeted tests/2 files pass. TypeScript/scoped ESLint pass. Logs: .tmp/bulk-red.log, bulk-tests.log, bulk-final.log. Counts overlap.
- Reused useSubmissionDirectory for complete paginated source, with client paging/filtering; search/status changes reset selection without re-fetching the directory. Action dialog captures ID/version snapshot, so clearing selection after completion does not alter an in-flight batch's items. BatchActionDialog and backend unchanged.
- Direct local browser: existing5-student class, header selects only2 submitted, unpublished-action count0 disabled, unsubmitted3 cannot be selected. Changing status clears selection. Group assignment renders1 group-root row, member grades remain under an expandable section. No AI/publish requests or database writes sent.
- Viewed desktop1440px and mobile390px/light-dark; document has no horizontal overflow, wide table scrolls inside its container.30/101-submission scenarios use test fixtures rather than adding fake students to live data. Restored light/desktop and cleared selections.
- No production build/backend suite repeated for this frontend-only change. No commit/push/deploy. Existing directory loads all pages; no new server-wide selection API added. Security/Resiliency retained; PBT off.
- Follow-up: user requested pointer cursor on checkbox hover. Added cursor-pointer with disabled:cursor-not-allowed to both header and row checkboxes. Browser computed styles confirm pointer for enabled inputs and not-allowed for disabled; scoped ESLint passes. Announcement menu investigation: global /announcements spans all permitted classes, nested class announcements is fixed to one class. Recommended distinct global label; no navigation change made in this follow-up.
- User then approved the naming proposal: global announcement menu for Teacher/Student/Subject Manager and /announcements metadata/title now use “Tất cả thông báo”. Class-specific menu/page keeps “Thông báo lớp”; routes, filters and permissions unchanged.11 announcement tests/2 files and scoped lint pass; direct browser confirms both different labels/links and both page headings. No commit/push.
- Cursor follow-up: native group-selection buttons lacked explicit cursor. Added shared base CSS for native buttons/links/summary/select/checkbox/radio/file controls and interactive ARIA roles; disabled controls use not-allowed. Keep utilities able to override for text editing, dragging and pending operations. Removed duplicate per-checkbox cursor classes. Reviewed native onClick controls and non-native handlers in source; custom clickable surfaces already supply cursor or contain controls. No blanket pointer on decorative hover/text fields.
- Browser verifies pointer on all3 actual group choices.14 computed-style probes for native/custom enabled/disabled controls pass, text inputs are not pointer and cursor-grab override remains.324 related tests/44 files, scoped lint and production Next.js build/typecheck pass (.tmp/cursor-tests.log, cursor-build.log). No backend/data changes or commit/push; exhaustive real interaction of every screen/state was not performed.
- Subsequent user request authorizes local commits. Separate bulk selection, announcement copy, shared cursor styling and docs; fresh15 precommit tests pass. No push/merge/deploy.
