# Quiet30-second autosave and one optional note

User reports reload-like behavior after score edits, asks for diagnosis, then authorizes30-second autosave and/or background saving without reload, plus replacing separate adjustment reason with one optional change note. Preserve all preceding uncommitted work; no live grade edits or subagents.

- [x] Confirmed with isolated diagnostic:1500ms debounce enters global busy, removes score input and rebuilds it without focus; no evaluation reload or navigation. Diagnostic fixture corrected to declare fractional-score support; one diagnostic case passes. Log `.tmp/grading-autosave-diagnosis.log`.
- [x] RED regressions:4 frontend failures/48 cases for30-second quiet save/focus continuity, edit preservation while saving/version reuse and optional unified note; backend1 error/6 cases for optional adjustment notes.
- [x] Separate background-save status from blocking commands, serialize saves and preserve edits made in flight; retain error pause and explicit save/Ctrl+S. Set idle delay30s. Use one optional note, preserving historical adjustment notes; remove dedicated required-adjustment-reason rule as explicitly requested.
- [x] Final176 frontend grading cases and206 backend grading/architecture cases pass; final13 in-flight hook cases, TypeScript/scoped lint and whitespace pass. Additional RED reproduced focus-only renders resetting the debounce; latest-save ref fixes it. Local API/worker refreshed, health200. All mutations use mocks, no live grade edits, commit/push or deployment. Evidence: construction/build-and-test/2026-11-10-grading-quiet-autosave.md.

Keep locked/versioned grading, score bounds, AI evidence/review controls and group-member refresh. Client-only hiding is insufficient for optional notes, so update the existing aggregate rule as well. No API field/schema/migration/dependency addition. Existing Security/Resiliency scope retained; PBT disabled.
