# Diagram-only grading: diagnosis, consistency review and verification

The user reported a2/10 AI proposal for the visible local sports-court assignment, JS SE Student2, evaluation dbbf3c65-673f-49e9-8dbb-9fff6dcd1bbd. Its architecture section had text plus a diagram; Class/Sequence sections contained only diagrams.

## Root cause and fix

Draw.io saves compressed mxfile pages. GradingPromptParts inspected the raw XML for literal mxCell/UserObject tags, so compressed student diagrams and modified teacher diagram slots appeared empty. It removed the affected rubrics before calling the provider and then merged deterministic zeroes with misleading empty-work comments. The model received only the architecture rubric worth2 points.

IDocumentModel now exposes hasDiagramContent backed by the existing DiagramXml.cellCount parser. Blank detection, active/empty rubric selection and the explicit-parts builder use that same bounded decoder used during submission validation and prompt extraction. No new decompressor or regex fallback was added. Unchanged teacher slots, blank compressed pages, invalid XML and genuinely empty work remain empty. Three diagram-only answers retain all3 rubrics and their extracted labels/messages in a provider prompt.

## Authoring/rubric consistency review

- Live assignment diagramEnabled=true; locked rubric snapshot canonically equals the served rubrics. Their partIds match the actual3 heading IDs, with maxima2+4+4=10.
- CriterionKind.TEXT is authoring metadata; it does not prohibit a student's diagram evidence or justify skipping a diagram-only answer. Regression fixtures intentionally use TEXT criteria.
- Provider and MCP keep their existing output contracts. MCP serves the full raw document and shared grading policy; provider requests use extracted diagram content. Token estimation and provider preparation now use the same corrected active-rubric classification.
- Complete item IDs, score bounds, two-decimal precision, checklist consistency and exact score totals remain enforced by the existing validator. No rubric, score ceiling, student answer or prompt scoring policy was changed to target a desired grade.
- Code validation cannot certify a model's qualitative partial-credit judgment. The new response gives partial credit for some incomplete workflow criteria; a teacher should review this rather than assuming the originally intended6/10 is guaranteed.

## Evidence

- Real compressed XML fixtures reproduce the bug in2 cross-module tests before the fix. Afterward the actual document model and prompt builder retain every rubric and diagram.
- Connected authoring/document/grading regression46 tests pass. Full final Maven verify:2116 tests/253 suites,0failures/errors/skips, executable JAR; completed17:03:02+07. Required5-suite/57-case guard passes. Logs remain ignored at .tmp/grading-diagram-*.log.
- Local API/worker refreshed through its existing launcher; health UP/200. Initial UI request during restart received500 and created no proposal; retry after startup made one successful Gemini provider call, task b563cc4f-d821-455a-91c4-4221a6275547, READY.
- New proposal379a92c1-3a1f-48bf-92fc-3c175afec603 scores8.50: architecture2.00/2, Class3.50/4, Sequence3.00/4. It discusses actual classes, attributes and messages rather than declaring partsII/III blank. Old2.00 proposal remains in history.
- Read-only database verification after the model run: saved evaluation DRAFT, score0.00, version1, submissionHash f803c88366a9c6cdedf7b62de853d033e62e782c9127e7011e405e43771785bb. These match the captured pre-fix values. Browser shows the new proposal in the unsaved grading form; no grade save/finalize/publish occurred.

## Limits

Only the current configured Gemini provider was checked live. No frontend source change, frontend-suite/build rerun, new migration, deployment/VPS update, commit/push or subagents. Security and Resiliency scope retained through the shared safe parser and existing authorization/snapshot/evidence controls; PBT disabled. This is a targeted consistency review, not a guarantee that every model grades identically.
