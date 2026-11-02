# Previous-page and tab restoration — 2026-11-01

## Result

Shared PageHeader Back links and the explicit assignment/grading/material/sidebar return links now use browser history for normal clicks, rather than discarding context by always navigating to a fixed parent URL. A supplied href remains the fallback when the browser has no previous entry. Modified/new-tab clicks retain link behavior; caller cancellation remains authoritative, including the grading unsaved-work confirmation. Group workspace exit uses the same opt-in Back behavior.

Page tabs and relevant selections are query-backed through a small shared hook. Coverage includes class dashboards/detail/list tabs; subject/detail/class tabs; assignment state/kind/search/page; student assignment tabs and filters; class progress and todo tabs; grading mode, selected assignment/student, search/status/page; submission list filters/page; quiz status/question type; personal/admin AI tabs and AI usage source; group document/submission tabs. Values are validated where a fixed list exists. Existing class/subject identifiers, deep links and unrelated query parameters remain intact. Text searches and pagination replace the current entry; tab and item choices append history, so Back/Forward can restore the previous selection. Modal editor tabs and explicit submission/authentication steps retain their own workflow behavior.

The hook uses Next.js's native-history bridge with null user state. Passing the current Next internal state causes the installed router to skip URL synchronization; browser verification caught this before completion. Next itself copies its internal routing metadata. No custom history stack or cross-session storage was introduced.

## Fresh verification

- Full frontend suite: 641 passed across 96 files. Tests cover history Back/Forward, fallback entry, modified/cancelled clicks, invalid URL values, remount restoration, unrelated query/hash preservation, permissions for MCP tabs, filters and pagination.
- Production build/typecheck passed. Full ESLint: zero errors, two existing unused-import warnings in AI registration and account details. Diff whitespace validation passed.
- Orca verified the live flow: class gradebook opened in student mode with a selected student and NOT_SUBMITTED filter; opening submissions and clicking its Back link restored the exact prior query URL, Chấm điểm tab, Theo sinh viên mode, selected student and filter.
- Orca also changed Theo sinh viên to Theo bài tập on the same page: browser Back restored both the URL and visible student tab, and Forward restored assignment mode. No server data was changed by these checks.
- Logs: `.tmp/2026-11-01-back-navigation-tests.log`, `.tmp/2026-11-01-back-navigation-build.log`, `.tmp/2026-11-01-back-navigation-lint.log`.

## Limits

No backend, schema, permissions, scoring or dependency changes. Backend tests were not rerun for this frontend-only change. Browser verification focused on the reported navigation pattern rather than every role and page combination. No commit/push or deployment. Native browser history may include a page outside the app; Back follows that actual history. Security/resiliency scope retained; PBT disabled.
