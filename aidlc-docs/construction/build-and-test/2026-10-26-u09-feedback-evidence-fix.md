# U09 missing-evidence proposal rejection follow-up

User request: diagnose and fix the rejected proposal without recreating the assignment or submission.

## Evidence and cause

- Evaluation: `8b88dd01-6d2f-48df-9bd0-6525e6627800`.
- Existing proposal version 2 was REJECTED after a successful READY provider response. Result payload had already been removed by normal task delivery; its exact validation failure was not retained.
- Added an allowlisted diagnostic classifier that exposes only fixed codes, never provider text, field values, prompts or output payloads.
- A UI retry using the existing teacher personal Gemini connection reproduced rejection on version 3, proposal `119fdb11-c717-46f9-8232-a9975009e3f5`, task `01999fca-a4b0-4b8f-8f13-d10192a3edc4`.
- The callback logged and UI displayed `FEEDBACK_MISSING_EVIDENCE`: an item lacked nonblank evidence without an explicit teacher-review reason.
- Generated schema had permitted this combination: evidence and reviewReason were independently nullable while requiresReview could be false. Callback validation required evidence OR explicit review-with-reason. This contract mismatch is reproduced by a failing schema regression test.

## Changes

- GradingPromptBuilder adds two schema branches: observed nonempty evidence with requiresReview=false; or requiresReview=true with a nonempty reviewReason. Backend semantic validation remains strict.
- Prompt explains that zero scores need an observation of missing/incorrect work, and uncertain/unreadable evidence needs human review rather than invented evidence or blank fields.
- GradingProposalValidator and callback expose fixed rejection codes, including missing evidence, unknown anchors/items, coverage, duplicates and total mismatch. Invalid proposal data is not silently accepted and no AI output is logged.
- Existing Gemini work and uncommitted changes preserved. No assignment/rubric recreation, grade save/apply/finalize/publish, model/key changes or commits.

## Verification

- [x] Diagnostic test initially failed compilation before helper implementation; related tests then passed.
- [x] Schema regression failed on the original schema accepting null evidence/requiresReview=false.
- [x] Final focused suite: GradingPromptSchemaTest, GradingProposalValidatorTest, GradingProposalResultHandlerTest and GradingServiceTest: 31 tests, 0 failures/errors/skips.
- [x] git diff --check passed.
- [x] Live UI proposal version 4 (`c75503c1-8704-4cb7-a36e-c6da481e3867`) reached READY with 9 structured item feedback entries. Task `355cd231-2420-4ac0-ae30-d0b46fe9ff1c` reached READY/APPLIED in 7.636 seconds using the existing Gemini connection/model.
- [x] Browser displayed Sẵn sàng để xem, the structured summary and item explanations. Expanded the first item and verified nonblank evidence, reason and a real submitted-block link. No Apply/Save/Finalize/Publish action performed.
- [x] Backend /health returned UP after final restart with local,worker; Docker worker remains stopped. Frontend remains running on port 3000.

Reference checked for schema compatibility: https://ai.google.dev/gemini-api/docs/structured-output.md?example=recipe (conditional anyOf examples). Provider schema enforcement is not a substitute for application validation.
