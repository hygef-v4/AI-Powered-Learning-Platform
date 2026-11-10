# Accurate Vietnamese AI grading failures

The screenshot showed an uppercase total-mismatch diagnostic in a grading toast. Stored rejected proposals and validator logs confirm TOTAL validation failures. A validation error does not establish quota exhaustion or truncated output; old provider completion metadata was not retained, so token exhaustion must not be inferred from this screenshot.

## Changes

- GradingProposalCallbackSupport translates known total/checklist/rubric/section/evidence diagnostics into specific Vietnamese messages. Total mismatch says the AI total differs from the sum of rubric item scores. Dispatch quota/service errors and legacy English placeholders are also translated; precise safe Vietnamese provider errors remain unchanged. Unrecognized internal codes use a readable fallback instead of leaking uppercase identifiers.
- GradingProposalService normalizes old stored safeError messages when reading or reusing a proposal, without rewriting history or adding a migration. Validator diagnostic codes remain in server logs. Toast and history consume the same server messages.
- HttpProviderGateway checks explicit completion reasons before accepting output. Gemini MAX_TOKENS, OpenAI length and Claude max_tokens produce an explicit incomplete-response/token-limit failure, even if the partial JSON parses. Measured token usage and CHARGED accounting are retained; no quota or fallback policy was changed. A normal completion at the numerical token cap is still accepted.
- HTTP429 uses structured provider error codes to distinguish insufficient quota/balance from rate limiting. Ambiguous429 responses retain the existing honest combined message. Provider body text is never echoed; API keys/raw provider details remain hidden.

Completion fields follow the official [Gemini API](https://ai.google.dev/api/generate-content?hl=en), [OpenAI Chat Completions reference](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create) and [Claude stop-reason guidance](https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons). Quota classification follows the [OpenAI error guide](https://developers.openai.com/api/docs/guides/error-codes).

## Verification

- RED:6 failures/16 cases reproduce user-visible code leakage, quota ambiguity and ignored truncation flags. Initial GREEN34 cases passed.
- Final connected Maven run:343 tests,0failures/errors/skips, including all AI/grading packages, compressed-diagram regression and architecture checks. No production source edits afterward. Logs: ignored .tmp/grading-errors-red.log, grading-errors-green.log and grading-errors-final.log.
- Final cases include real HTTP fixtures for each provider's truncation flag, charged usage, normal output at token cap, safe quota/rate classification, legacy message translation and read-only historical proposal normalization.
- Restarted only the identified existing local API/worker launcher; health200 and application startup confirmed. No new migration.
- Opened a separate authenticated Chrome tab to the reported evaluation and inspected Xem đề xuất AI. Existing rejection now displays: Tổng điểm AI trả về không khớp với tổng điểm từng tiêu chí rubric. Hãy thử chấm lại bằng AI hoặc chấm thủ công. No PROPOSAL_ text found. Closed only the temporary tab and restored the user's original tab, preserving its form.
- Read-only before/after database checks prove state, saved score, version and submission hash unchanged. No new provider request was made for this task; no grade save/finalize/publish.

## Limits

Provider exhaustion cases were tested using local HTTP fixtures, not by consuming real quotas. No frontend source change, whole-backend verification/JAR or frontend build repeated for this message fix; earlier diagram work is preserved. No dependency, commit/push, VPS update or subagents. Existing Security/Resiliency retained; PBT disabled.
