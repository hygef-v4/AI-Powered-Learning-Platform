# Accurate AI grading error messages

User requests readable Vietnamese errors matching actual causes, with no uppercase diagnostic codes in UI. Latest local rejection is TOTAL mismatch, confirmed in proposal storage and validator logs; it is not evidence of token exhaustion.

- [x] Inspect callback/provider/UI paths and verify the reported failure in local logs. Provider gateway currently ignores completion stop reasons; HTTP429 alone does not distinguish quota from rate limiting.
- [x] Reproduce total-mismatch UI text, legacy/dispatch codes and provider token truncation with failing tests:6 failures/16 tests before implementation.
- [x] Translate grading diagnostics centrally for new and old proposal reads; preserve backend codes for logs and existing safe Vietnamese provider errors. Read explicit provider stop reasons before parsing output; retain measured usage for charged failures. Distinguish quota/rate errors only with explicit codes.
- [x] Initial34 tests and final343 AI/grading/diagram/architecture tests pass with0failures/errors/skips. Refreshed local backend; authenticated temporary browser tab displays the accurate Vietnamese total-mismatch message for existing failed proposals, with no PROPOSAL_ text. Original tab restored; saved grade/state/version/submission hash match before/after. No new paid AI call or grade mutation. Prior diagram fix and all existing work retained.

No quota policy, score validation, auto-correction, schema, dependency, commit/push or VPS release change. Existing Security/Resiliency retained; PBT disabled. No subagents.
