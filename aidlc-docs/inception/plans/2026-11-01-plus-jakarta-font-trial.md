# Plus Jakarta Sans font trial

Goal: replace the current self-hosted UI font with Plus Jakarta Sans at the user's request while preserving Vietnamese coverage and the existing visual system.

- [x] Download the official variable WOFF2 subsets and OFL license into the frontend.
- [x] Switch font faces, theme tokens and preload paths; mark the product font choice as a trial.
- [x] Remove unused Be Vietnam Pro assets after confirming no references remain.
- [x] Verify the rendered font and responsive layout in Orca; run frontend checks and production build.

This remains a font trial. The user subsequently requested a commit; no push was requested.

Verification: Orca confirmed normal, bold and italic Vietnamese font faces load, and the assignment page has no document-level overflow at 390 px or 1280 px in light and dark themes. Production build passed. Lint passed with two pre-existing unused-import warnings. Automated tests were not rerun because this change affects only static font assets and CSS.
