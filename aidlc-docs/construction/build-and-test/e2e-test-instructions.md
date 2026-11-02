# End-to-end test instructions — UC63–65

Status: independent E2E pending. The earlier desktop production fixture verifies frontend behavior only. Use the real stack and test-only accounts after [integration setup](integration-test-instructions.md).

1. Assigned Teacher creates important/email and ordinary announcements in an OPEN class; verify Student feed/filter/U05 class card, sanitized Markdown and newest-first pagination. Edit with version, preserve author/flags, race another edit, then confirm delete. Verify read-only Student and unauthorized staff/Admin behavior through UI and direct API.
2. Cause each supported owner event; verify bell/popup for all four roles, all/unread/load more, open target, read/read-all and second-tab refresh. Change account/class/query filter and ensure stale requests cannot show prior rows. Verify lost connection recovery and logout cleanup using real SSE.
3. Student changes all five email preferences; verify only future eligible email behavior changes and in-app notices remain. Staff have no learning-email switches. Confirm emails in Mailpit and role-specific destinations using actual recipients/permissions.
4. Exercise server409/503, empty state and failed submission/retry, keyboard focus/Escape, desktop and mobile viewport. Compare local reference screens s06/t08/t08b/t08c/c14 under `E:/FPTUni/sem9_fa26/ui-reference` while preserving current business rules.

Record browser/version, viewport, roles, source revision, expected/actual results, sanitized screenshots/console/network evidence and tester identity. No automated browser test script is claimed to exist. Do not label fixture screenshots or responsive CSS as full mobile/provider acceptance.
