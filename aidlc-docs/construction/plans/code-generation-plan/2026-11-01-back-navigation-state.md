# Restore the previous page and selected tab — 2026-11-01

User requested that Back return to the actual previous page with its previous tab/selection, including pages with multiple tabs. Current Back links often point to a fixed parent URL; several tabs exist only in local React state, and class tab changes replace rather than append history.

- [x] 1. Trace shared PageHeader, explicit Back links and tab/selection callers. Preserve existing pending work and unsaved-edit guards.
- [x] 2. Add a shared Back link using native browser history for normal clicks with a fallback destination for direct entry. Preserve modified link clicks and caller cancellation.
- [x] 3. Store page tabs and relevant assignment/grading selections in query parameters; use native history entries for tab switches and replacement for text filters. Keep modal-local editing tabs outside navigation history. Validate tab values and preserve unrelated query parameters.
- [x] 4. Add regression tests for Back/fallback, modified and cancelled clicks, tab history/reload/Back/Forward and query preservation. Run related checks and verify actual navigation through Orca. [Results and limits](../../build-and-test/2026-11-01-back-navigation-state-results.md).

No backend, authentication, scoring, feature or dependency changes. No custom history stack or cross-session selection persistence. Current security/resiliency scope retained; PBT disabled. No commit or push requested.
