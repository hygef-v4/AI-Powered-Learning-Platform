# Remove grade history from grading workspace

User requests removal of the confusing grade-change panel. It currently shows every saved total change after the first save, including drafts, and renders empty reason separators. Scope is removal from the teacher workspace, preserving backend history and AI proposal history. No data deletion or API/scoring change.

- [x] Inspect history renderer, callers, tests and backend save behavior. User request authorizes this small UI removal; retain existing Security/Resiliency scope and disabled PBT.
- [x] Delete unused GradeHistory component/import/render; remove empty layout wrappers. Adapt existing workspace test for absent panel with empty and populated histories while retaining activity disclosure coverage.
- [x] Initial168 grading tests pass, TypeScript and scoped lint pass. Local authenticated workspace has no grade-history text and retains the AI history button. No grade writes.

## Follow-up: adjustment validation and autosave

User reports missing adjustment reason causes repeated loading and an English error; asks how the two reason fields differ and what paragraph splitting does. Explained both existing semantics. Root cause: autosave effect retries every1.5s after failure because dirty stays true and busy toggles; client has no adjustment-reason validation.

- [x] Four RED failures across42 cases reproduce missing inline validation for positive/negative adjustments, repeated autosave after failure and English server fallback. Initial fake-timer fixture timeouts corrected before recording RED.
- [x] Validate inline before API calls; pause autosave after errors and resume on user edits. Translate server validation fallback. Keep separate adjustment/change reason contracts and backend enforcement.
- [x] Final173 connected frontend tests plus45 final-polish tests pass; TypeScript/scoped lint pass. Local DOM confirms inline Vietnamese adjustment validation, no repeated busy/reload state, no obsolete controls. A native browser click accidentally changed one local draft criterion; restored via normal versioned save, preserving audit entries. Score/rubric checks/feedback/metadata now semantically match the before-test baseline. See build-and-test/2026-11-10-manual-feedback-parity.md for incident and screenshot limits.

No new behavior, dependency, backend changes, migration, commit/push, deployment, grade write or subagents. Additional requirements/design stages add no value to this isolated removal.
