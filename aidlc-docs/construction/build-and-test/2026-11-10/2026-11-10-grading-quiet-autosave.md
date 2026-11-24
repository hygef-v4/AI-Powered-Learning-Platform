# Quiet grading autosave and one optional note

## Cause and resulting behavior

The previous1500ms autosave called the same blocking save path as manual save. `busy` made the whole editor disabled; the rubric removed number inputs and feedback editors switched to read-only content. After the response, new inputs appeared without the previous focus. An isolated mock diagnostic reproduced the removal/recreation with one initial evaluation load and zero navigations; a full page reload was not needed. Historical diagnosis log: `.tmp/grading-autosave-diagnosis.log`.

User then explicitly requested30-second saving and/or saving without reload, and replacing the separate mandatory adjustment reason with one optional note. This supersedes the earlier required-reason UI fix; historical reports remain unchanged.

- Autosave waits30 seconds after the latest form edit. A latest-save ref keeps focus/view-only renders from resetting the timer. Another isolated RED test reproduced that reset before the ref fix.
- Background saves use separate saving status; inputs and expanded editors stay mounted and editable. Conflicting save/navigation/AI/publish commands are disabled during the request. Explicit Save/Ctrl+S still execute immediately.
- A synchronous in-flight guard serializes saves. An edit revision prevents an old response from overwriting newer rubric points, feedback, adjustment or note, or marking newer edits saved. Successful responses update the evaluation version for the next request. Pending newer AI application state is retained. Existing group-member refresh and stale/official receipt handling remain.
- Errors still pause autosave instead of retrying continuously; user editing or explicit retry allows recovery. Version/lock/score/AI review protections remain.
- UI now contains only `Ghi chú thay đổi (tùy chọn)`. Historical adjustment notes initialize it. Supplied notes map to the existing changeReason and, for a nonzero adjustment, adjustmentReason API fields; blanks map to null. Removed the aggregate's dedicated mandatory-adjustment-reason check as explicitly requested. Existing score clamping/precision/range and member-contribution rules remain.

## Verification

- Initial RED:4 frontend failures/48 cases and1 backend error/6 cases. Follow-up debounce RED: focus-only change prevented the expected save. Synthetic data only.
- Final `npm test -- tests/features/grading --maxWorkers=1`:176 tests/18files,0failures, completed2026-11-10T18:52 local. Final expanded in-flight test checks rubric point0.75 as well as feedback/adjustment/note and returned-version reuse;13 hook cases pass afterward. Node heap512MiB.
- Final TypeScript and scoped ESLint pass. CRLF-aware `git diff --check` passes. Logs `.tmp/quiet-autosave-{frontend-final,inflight-final,typecheck-final,lint-final,debounce-red,red,green}.log`.
- Backend grading plus architecture:206 tests,0failures/errors/skips, completed2026-11-10T18:51:31+07:00. Maven/test heaps256MiB and SerialGC. Log `.tmp/quiet-autosave-backend-final.log`.
- Cases cover no save at1.5seconds,30 idle seconds from the latest edit, focus continuity during a pending response, no evaluation refetch/navigation for an individual save, immediate Ctrl+S, no duplicate in-flight save, preservation of newer points/text/notes and server version reuse, error pause/resume, optional positive/negative adjustment notes and preservation of legacy adjustment notes.
- Local API/worker refreshed to current compiled classes; health200. Runtime `.tmp/quiet-autosave-runtime.log`. No application migration added.

## Scope and limits

No browser interaction or real grade mutation in this turn; all save/finalize scenarios use mocks. No AI provider call, schema/dependency addition, commit/push or VPS deployment. Other pending manual-feedback/history/student-list changes retained. Full suites/JAR/frontend production build and real multi-session/mobile visual acceptance not repeated. Pending edits still rely on successful saving; the existing unsaved-change warning remains until saved.

Security/Resiliency: existing authorization, stale/version checks, locked grades, score bounds and AI evidence/review validation retained. Optional adjustment notes are the user's explicit policy choice; no invented placeholder reasons. Background save serialization/revision handling and error pause protect pending edits. Infrastructure rules N/A; PBT disabled. No subagents.
