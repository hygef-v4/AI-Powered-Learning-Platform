# Grading UI screenshot follow-up — 2026-11-10

The U09 follow-up fixes the score/comments sticking beneath the grading toolbar, cramped feedback inputs, technical identifiers in evidence links and undated AI proposal history. Existing draft/save/review limits and task reconciliation remain intact.

## Changes

- DocumentEditor measures either toolbar with ResizeObserver. CommentRail uses its actual height plus the inherited workspace header offset and an 8px gap, instead of a fixed 116px top.
- Structured summary, per-item feedback and general feedback reuse AutoTextarea, with larger text, padding and a 96px minimum. Fields grow and shrink with their content and width. The shared sizing calculation includes borders.
- Evidence links display “Đi tới đoạn bài làm”; navigation retains the internal block ID. Missing anchors use a readable message without an ID.
- Proposal cards retain successful results and the current pending request, excluding failed/rejected attempts. The latest failure is an actionable notice outside the history cards, preserving error recovery and manual grading. Internal proposal/task records are retained for reconciliation and diagnostics; no data is deleted.
- Successful cards show the backend's createdAt using the existing Vietnam date/time formatter, including older collapsed cards. Missing/invalid legacy timestamps are omitted rather than invented.

## Verification

- Regression first: six expected failures across the three affected test files (55 existing tests passed), covering offset, growth, labels, failure cards and dates.
- Affected grading/document tests: **205 passed / 20 files**.
- Full frontend: **873 passed / 127 files**, `npm test -- --maxWorkers=2`.
- Full lint, typecheck and production build passed. The initial typecheck caught missing required props in the new test harness; corrected before the passing check/build.
- Scoped `git diff --check` passed.
- Orca browser fixture rendered the real shared editor and grading feedback/history components with synthetic data. At 1440×900, score top188 was below toolbar bottom179.73 while scrolling. Increasing the header to120px and toolbar to96px gave toolbar bottom272 and score top280; ResizeObserver updated the measured variable to96px.
- At a390px mobile viewport, the three expanded fields had clientHeight/scrollHeight95/95,312/312 and289/289, without horizontal page overflow. Two successful history cards showed11:21 10/11/2026 and08:05 09/11/2026; the failed attempt had no card and the evidence link had no UUID.
- Inspected desktop and mobile screenshots. Temporary fixture tab/server closed afterward. Fixture source and evidence remain ignored under `.tmp`.

Logs: `.tmp/grading-ui-red.log`, `.tmp/grading-ui-tests.log`, `.tmp/grading-ui-full-tests.log`, `.tmp/grading-ui-lint.log`, `.tmp/grading-ui-types.log`, `.tmp/grading-ui-build.log`. Browser metrics: `.tmp/grading-ui-browser-sticky.json`, `.tmp/grading-ui-browser-mobile.json`; images: `.tmp/grading-ui-feedback-desktop.png`, `.tmp/grading-ui-feedback-mobile.png`.

## Acceptance on the running application

1. Open a teacher grading workspace and scroll the submission; the score and comment cards should stay below both toolbars. Resize the window and repeat.
2. Open “Chi tiết nhận xét & minh chứng”, paste a long comment, then shorten it. The field should expand/shrink without an inner scrollbar. “Đi tới đoạn bài làm” should navigate without displaying a technical ID.
3. Open AI proposals: successful current and older cards show creation date/time. A failed attempt shows a current error notice with retry/manual grading available, but no failed history card. Loading/reopening history must not change grades.

This follow-up changes frontend only. No backend rerun, authenticated end-to-end/provider request, live grade write, runtime restart, migration, commit or push. Browser evidence is a component fixture, not acceptance on the user's live grading session. Security/Resiliency retained; PBT disabled.
