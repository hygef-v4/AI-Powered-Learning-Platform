# U09 feedback recommendations from the 7A1 report

Status: user authorized implementation on 2026-10-26. Execute the scoped plan in `2026-10-26-u09-structured-feedback.md` after the Gemini delivery bugfix checkpoint. Class analytics/export remain deferred.

## Reference

Read all 8 pages of the user-provided local PDF `C:\Users\Quang Hung\Downloads\Bao_cao_7A1.pdf`; visually inspected rendered page 1. Do not copy student identities into fixtures or send this PDF to a provider.

Page 1 combines class metrics, distribution and individual feedback split into strengths, corrections with question/point attribution, and a next-step hint. Pages 2 and 3 include teacher-review cases for an alternative solution and unclear input. Later pages repeat the structure. Some full-mark submissions have presentation suggestions: distinguish unscored advice from scored deductions.

Current U09 supports partial item scores, overall feedback, AI sections by part, editable drafts, finalization and publication. Extend this existing model rather than introducing a separate score calculation.

## Recommended feedback structure

1. Total earned/max and question earned/max, calculated by the existing grading engine.
2. Expandable rubric rows: criterion, earned/max, observed evidence, explanation, and one concrete improvement.
3. Overall summary: strengths, highest-priority improvements, and next practice steps. Avoid repeating all item comments.
4. Explicit review flags for unreadable content, insufficient evidence, alternative designs and rubric ambiguity.
5. Teacher editing and approval; students see approved feedback only with published grades.

Each partial-score explanation answers: Which criterion? What is visible in the submission? How does it compare with the rubric? What should the student change?

Illustrative example, not an assessment of the live submission:

| Criterion | Earned/max | Explanation | Next step |
| --- | --- | --- | --- |
| Model multiplicity | 0.50 / 1.00 | Association is present, but multiplicities are absent. The example rubric gives 0.50 for association and 0.50 for complete multiplicities. | Add multiplicities matching the stated business rules and explain them. |

The explanation must follow the actual rubric; AI cannot invent a half-point subcriterion to justify its score. Flag unclear partial-credit rules for teacher review.

## Teacher screen

- Keep submitted work on the left and grading on the right. Put editable feedback and collapsible evidence under each item score.
- Distinguish AI-suggested points from teacher draft points. Preserve explicit Apply, Save, Finalize and Publish actions.
- Put review flags above ordinary feedback. Use a specific reason rather than invented confidence percentages.
- Distinguish uncertain/not visible from missing/incorrect. An unreadable diagram must not silently receive zero.
- Evidence links can target a question, document block, diagram/page or verified Draw.io cell ID. Only provide resolvable anchors; do not invent image coordinates.
- Suggested SWD392 issue labels: requirement interpretation, UML notation, relationships/multiplicity, interaction flow, concurrency/transactions, architecture/responsibilities, pattern justification, presentation and missing evidence. Show full labels and allow other subjects to configure them.

## Grading and data rules

- Associate comments with stable rubric/item IDs and submission/rubric versions. Invalidate stale feedback.
- Item scores remain the source of truth. Backend derives totals and points not awarded; AI prose must reconcile with those values.
- Do not subtract a second penalty for the same issue unless an independent rubric criterion permits it. Teacher adjustments remain separate with a reason.
- Prefer additive optional structured fields for strengths, issues, evidence, improvement and review reasons. Preserve historical free-text feedback and old grading documents. Exact DTO/schema design belongs to a later implementation plan.
- When teachers change scores, prompt review of affected explanations so feedback matches the final score.
- Validate AI item IDs, bounds, evidence references and output structure before Apply. Manual grading remains available when output is invalid.

## Later class reports

- Compute distribution from published approved grades; label the denominator and distinguish not submitted, awaiting grading and awaiting publication.
- Summarize recurring rubric issues and revision topics from supported structured evidence, not repeated boilerplate comments.
- PDF/CSV export is a later step. Do not hardcode the reference's maths labels or score bands into SWD392.

## Recommended sequence

1. Complete and validate the active result-delivery bugfix.
2. Add item evidence/explanation/improvement and overall strengths/improvements/next steps, including teacher editing and publication behavior.
3. Add explicit review flags and navigable evidence anchors.
4. Add class analytics and export after feedback data has been validated.

## Future acceptance cases

- A 0.50/1.00 item shows criterion, evidence, reason and concrete revision; totals reconcile.
- Full marks can include optional advice without implying a deduction.
- A valid alternative design is reviewed rather than rejected merely for differing from a sample.
- Unreadable diagrams yield a review flag, not unsupported content claims.
- One underlying issue is explained consistently without unauthorized double deductions.
- Teacher changes survive save/reload; students see approved content only after publication.
- Historical records without new fields still render; feedback is not applied to a newer submission.
