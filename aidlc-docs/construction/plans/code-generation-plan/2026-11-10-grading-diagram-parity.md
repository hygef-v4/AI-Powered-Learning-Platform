# Diagram-only grading consistency

User requests investigation of a2/10 AI proposal on the visible local teacher browser and consistency with assignment/rubric authoring. Existing draft grade remains untouched.

- [x] Capture the actual proposal and three submitted compressed Draw.io blocks. Proposal deterministically zeros the two diagram-only sections; rubric maxima remain2+4+4. Reuse anonymized diagram content as regression fixtures.
- [x] Reproduce empty-part detection against the actual saved diagrams using the real document model and grading prompt builder. Both connected regression tests fail on compressed Draw.io content before the fix.
- [x] Use the shared bounded Draw.io parser for grading content detection, preserving blank/unchanged teacher slots and strict score/evidence validation. Review authoring rubric mapping and snapshot identity: live snapshot canonically matches all3 rubrics and their heading IDs;2+4+4 totals10. TEXT criterion labels do not control whether diagram evidence can be graded.
- [x] Connected46 tests pass; final full Maven verify2116 tests/253suites with0failures/errors/skips and executable JAR, required-suite/57-case guard pass. Refreshed local API/worker and made one successful Gemini grading request. New READY proposal8.50 reads all3 diagrams; persisted grade remains DRAFT0.00/version1 and original submission hash unchanged. No save/finalize/publish or assignment/submission edit.

Existing Security/Resiliency retained; PBT disabled. No new parser, dependency or schema migration. Sequential work without subagents. The same initial diagram classification must apply to token estimation, provider prompts and MCP grading.
