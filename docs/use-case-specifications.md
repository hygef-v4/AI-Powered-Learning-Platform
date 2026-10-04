# II. Use Case Specifications — 15 Key Use Cases

These fifteen use cases are selected from the [40 active MVP use cases](use-case-table.md) to cover the main journey from learning materials and subject templates through all five assignment types, submission, grading/publication and AI credit purchasing. IDs, names and actors follow the current catalog. Business behavior is specified here; implementation details remain in the AI-DLC unit designs.

| ID | Use Case | Reason for Selection |
|---|---|---|
| 11 | Manage Content | Shared modules, scoped learning materials and RAG |
| 21 | Manage Templates | Versioned subject templates and independent class copies |
| 28 | Manage Assignments | Shared lifecycle, sources, schedules, copies and AI drafts |
| 23 | Manage Text Essay | Versioned essay questions and per-question rubrics |
| 24 | Manage Quiz | Answer keys, exact-match scoring, shuffling and visibility |
| 25 | Manage Diagram Essay | Mandatory skeleton, leaf parts, Draw.io/DOCX and AI drafts |
| 26 | Manage Code Lab | Scored tests, isolated execution and sample verification |
| 27 | Manage Group Assignment | Fixed skeleton parts, rubrics and class-group readiness |
| 30 | Submit Assignment | Four individual types and immutable attempts |
| 16 | Submit Group Document | Section claims, live collaboration and automatic submission |
| 33 | Grade Submissions | Checklist grading, AI batches and teacher decisions |
| 17 | Grade Group Document | Shared-document rubrics and member contribution scores |
| 34 | Finalize Grades | Finalization, publication and score corrections |
| 37 | Buy AI Credits | Verified payments and exactly-once credit grants |
| 40 | Grade with AI | Explicit requests, credits, timeouts and retries |

Subject Managers perform Teacher actions only when assigned as the class teacher; otherwise they author subject templates under UC 21. The platform itself is not a secondary actor. UC 23–27 inherit the common lifecycle in UC 28, and detail only type-specific authoring and validation. UC 30 covers student editing/submission; UC 16 inherits its common submission rules for groups.

## 1. UC 11 — Manage Content

| Field | Specification |
|---|---|
| Primary Actors | Subject Manager; Teacher |
| Secondary Actors | Google Drive; YouTube; AI Service |
| Description | Manage subject modules and scoped materials, then process uploaded text/captions for RAG. Materials are visible immediately without publication or versioning. |
| Preconditions | The user is signed in and manages the subject or teaches the class within the authorized scope. |
| Normal Flow | 1. The Subject Manager opens Subject Detail and creates, renames, reorders or archives modules. All current and future classes share this module structure. |
| | 2. The user opens Upload Learning Materials from a module and uploads PDF/DOCX/PPTX files or a single YouTube video link. Each source becomes one material. |
| | 3. Uploads from Subject Detail are subject materials; Teacher uploads from Class Detail are class-only materials. The system validates sources, stores files through Google Drive and saves their scope. |
| | 4. Materials become viewable immediately. Background processing extracts text or existing captions, checks AI eligibility and uploader credits, creates an embedding and records processing status. |
| | 5. Authorized users inspect scan status, retry processing and rename, reorder or archive materials. |
| Alternative Flows | **A1 — Invalid source:** Reject unsupported files, files over 50 MB, invalid links and playlists. OCR and audio transcription are unsupported. |
| | **A2 — No text/captions:** Record NO_TEXT/NO_CAPTION and skip indexing; the material remains viewable. |
| | **A3 — Credit or system limit:** Record NO_CREDIT/BUSY without charging for an unmade AI call; allow later retry. |
| | **A4 — Processing failure:** Apply bounded retries, then record FAILED and release reserved credits as applicable; retain the material. |
| | **A5 — Unauthorized change:** Teachers cannot edit subject modules/materials. The current Subject Manager may manage materials throughout the assigned subject. |
| | **A6 — Archive:** Hide the material without hard deletion; archiving a module hides its contents. |
| Postconditions | Materials and processing status are retained within scope. AI failure does not remove viewing access. Only INDEXED materials are eligible for RAG. |
| Traceability | U05 F1–F4; U03 file handling; U13 AI usage; UC 12 student access. |

## 2. UC 21 — Manage Templates

| Field | Specification |
|---|---|
| Primary Actors | Subject Manager |
| Secondary Actors | AI Service (when drafting); Google Drive (document assets) |
| Description | Manage versioned subject templates for independent class copies, including templates inherited from previous managers. |
| Preconditions | The user is the current manager of the subject. AI drafting requires eligible INDEXED subject sources, enabled AI and sufficient manager credits. |
| Normal Flow | 1. Open Template List from Subject Detail and select assignment type/mode. Group templates are GRADED only. |
| | 2. Author in Template Editor using UC 23–27 editors. Text Essay has one rubric per question; Diagram Essay/group templates have one per skeleton part. |
| | 3. Optionally request AI questions for Quiz/Text Essay/Code Lab or a skeleton with guidance and per-part rubric suggestions for Diagram Essay/group work, using subject materials and manager credits. |
| | 4. Preview citations/content, edit and accept or discard. Skeleton replacement requires confirmation and a warning when existing content will be replaced. |
| | 5. Preview, review and release the valid template. Released versions are read-only and available to teachers of the subject. |
| | 6. A Teacher copies a matching released template under UC 28 into an independent class draft with cloned rubrics. |
| Alternative Flows | **A1 — Edit released template:** Create a new version; existing class copies remain unchanged. |
| | **A2 — Delete:** Delete an unreleased draft; withdraw released versions, prevent further copying and preserve existing copies/history. |
| | **A3 — AI/source/credit failure:** Reject invalid requests, release unused reservations on failure and permit manual authoring. |
| | **A4 — Invalid rubric/configuration/sample solution:** Refuse review/release and identify the failed checks. |
| | **A5 — Unauthorized manager:** Reject changes even if the user originally created the template. |
| Postconditions | Released templates can be copied without schedules or group attachments. Templates are never delivered directly to students or synchronized into existing copies. |
| Traceability | U10 F1–F2; U09 authoring; U13 F1; UC 20 and 28. |

## 3. UC 28 — Manage Assignments

| Field | Specification |
|---|---|
| Primary Actors | Teacher; Subject Manager |
| Secondary Actors | AI Service (when drafting) |
| Description | Manage assignment drafts, sources, review, publication, schedules, copies and retirement. Subject Managers manage subject templates under UC 21; they edit class assignments only when teaching that class. |
| Preconditions | The user has the appropriate scope. Content changes require DRAFT state. AI requests require permitted INDEXED sources, enabled AI and sufficient requester credits. |
| Normal Flow | 1. The Teacher selects type and valid GRADED/PRACTICE mode; group work is GRADED only. |
| | 2. Start empty, copy a matching released subject template or copy from another class they teach. Copies clone rubrics and omit schedules, attempts, submissions and grades. |
| | 3. Author through UC 23–27. Quiz/Text Essay/Code Lab use bank/private questions; Diagram Essay/group work use mandatory skeletons without assignment-question rows. |
| | 4. For AI drafts, choose source scope and options. Check permissions, indexing, limits and credits, reserve estimated credits and process in the background. |
| | 5. Show validated questions or a heading skeleton with guidance, rubric suggestions and citations. Edit/select questions to retain, or confirm skeleton replacement after preview. Discarding leaves the draft unchanged; settle actual usage. |
| | 6. Preview and review. Validate questions, configurations and rubrics, verified Code Lab sample solutions and group readiness. |
| | 7. Publish with opening/closing times, late-submission policy and attempt limits. Lock content; open/close on schedule and notify students when work opens. |
| Alternative Flows | **A1 — Invalid RAG source:** Reject outside-scope/unindexed sources before calling AI. |
| | **A2 — AI/credit limit:** Reject without charging for an unmade call; manual authoring remains available. |
| | **A3 — AI failure/invalid output:** Apply bounded retries, release unused reservations after final failure and preserve the draft. |
| | **A4 — Invalid content/rubric/sample solution:** Refuse review/publication and show failed checks. |
| | **A5 — Group readiness:** Require at least one valid group and one leader per group; warn about ungrouped students. |
| | **A6 — Schedule change:** Validate times; an OPEN assignment may only have its deadline extended. |
| | **A7 — Retirement:** Require a reason, stop further work and automatically submit current attempts/group documents. Retired work stays visible to students with a published score or Practice result. |
| | **A8 — Clone/new version:** Create an independent draft with cloned rubrics and keep historical work/grades. Published content cannot be edited in place. |
| Postconditions | Accepted AI content remains draft until human review/publication. The assignment has a valid lifecycle and schedule; AI never approves or publishes it. |
| Traceability | U08 F1–F8; U09 authoring; U10 copies; U12 readiness; U13 F1; UC 21 and 23–27. |

## 4. UC 23 — Manage Text Essay

| Field | Specification |
|---|---|
| Primary Actors | Teacher; Subject Manager |
| Secondary Actors | AI Service (when drafting questions) |
| Description | Author a GRADED or PRACTICE Text Essay with versioned questions and one checklist rubric per question. Extends the common lifecycle in UC 28; subject templates follow UC 21. |
| Preconditions | The Teacher teaches the class or the Subject Manager manages the template's subject. The target is DRAFT with TEXT_ESSAY type and a valid mode. |
| Normal Flow | 1. Open Assignment Editor or Template Editor through UC 28/21 and enter the title and instructions. |
| | 2. Write private essay questions or search and select versioned bank questions by type, difficulty, tags and module. Optionally bulk-import essay questions or request AI questions through UC 28/21. |
| | 3. Create one checklist rubric per question in the editor under UC 20. |
| | 4. Calculate each question's maximum points from its rubric; calculate assignment points as the sum of question totals. Points are not entered independently of the rubric. |
| | 5. Preview the student view and request review. Validate the question count and a valid ACTIVE rubric for every question; publication, copying and versioning follow UC 28. |
| Alternative Flows | **A1 — Missing rubric or invalid question:** Refuse review and identify the question requiring correction. Text Essay supports 1–20 questions. |
| | **A2 — Edit a question used by published work:** Create a new question version; existing assignments/attempts keep their pinned version. |
| | **A3 — Edit a rubric:** Create a new version and repoint only a DRAFT owner. Reviewed/published work keeps its pinned rubric; copies clone rubrics. |
| | **A4 — Bulk-import errors:** Validate each row, create valid question drafts and report invalid rows. Importing questions does not create rubrics; the author must add them in the editor. |
| | **A5 — AI or credit failure:** Follow UC 28/21 alternatives; retain the draft and allow manual authoring. |
| Postconditions | The draft contains essay questions and independent per-question rubrics with calculated points. Student answers use basic text formatting without tables, images or diagrams and no word/line limit beyond the technical cap. GRADED work is teacher-graded; Practice AI is requested explicitly under UC 40. |
| Traceability | U09 BR-U09-20–23; U06 question/rubric versions and imports; U08 review; UC 20, 21 and 28. |

## 5. UC 24 — Manage Quiz

| Field | Specification |
|---|---|
| Primary Actors | Teacher; Subject Manager |
| Secondary Actors | AI Service (when drafting questions) |
| Description | Author a GRADED or PRACTICE multiple-choice quiz with answer keys, scoring and display policies. Extends UC 28; subject templates follow UC 21. |
| Preconditions | The author has class/template scope and a DRAFT MULTIPLE_CHOICE_QUIZ with a valid mode. |
| Normal Flow | 1. Open the appropriate editor and enter instructions. |
| | 2. Create single-answer/multiple-answer questions or search, select, bulk-import or reuse bank versions. Bank selection may be manual or random with preview under UC 28. |
| | 3. Specify answer choices, correct answers and positive question points. Single-answer questions have 2–6 choices with exactly one correct answer. |
| | 4. Configure question/answer shuffling, optional time limit, immediate score visibility and correct-answer visibility (NEVER, AFTER_SUBMIT or AFTER_CLOSE). |
| | 5. Preview and review. Validate 1–200 questions with valid answer keys and positive points; publish/schedule/copy through UC 28 or release the template through UC 21. |
| Alternative Flows | **A1 — Invalid answer key or points:** Refuse review and report the exact question. |
| | **A2 — Invalid import row:** Report per-row errors; valid rows become bank drafts and no rubric is created. |
| | **A3 — Edit a bank question used by published work:** Create a new version; keep published versions and attempt answer keys unchanged. |
| | **A4 — Random selection unavailable:** Report insufficient matching questions without duplicating already selected questions; permit filter changes or manual selection. |
| | **A5 — AI failure/insufficient credits:** Preserve draft questions and permit manual authoring. |
| Postconditions | The draft has versioned questions, scoring and visibility configuration without rubrics. Multiple-answer questions earn points only for the exact correct answer set. Each student attempt retains its shuffle seed/answer-key snapshot; students do not author quizzes. |
| Traceability | U09 BR-U09-10–14; U06 question validation; U08 question selection; UC 21, 28 and 30. |

## 6. UC 25 — Manage Diagram Essay

| Field | Specification |
|---|---|
| Primary Actors | Teacher; Subject Manager |
| Secondary Actors | AI Service (when drafting a skeleton); Google Drive (document assets) |
| Description | Author a GRADED or PRACTICE Diagram Essay with a mandatory document skeleton, leaf-heading parts and one rubric per part. Extends UC 28 and reuses UC 21 for subject templates. |
| Preconditions | The author has class/template scope and a DRAFT DIAGRAM_ESSAY with a valid mode. |
| Normal Flow | 1. Open the DOCUMENT skeleton editor and write the teacher blocks, copy a DOCUMENT skeleton from the bank, or import DOCX with a preview and unsupported-content report. |
| | 2. Optionally request SKELETON_DRAFT through UC 28/21: preview a heading tree, guidance, per-part rubric suggestions and citations. Confirm replacement of the existing skeleton, with a warning if content already exists. |
| | 3. Use Heading 1–6 to build nested structure. Automatically identify leaf headings as parts; title/subtitle and content before the first heading are shared content. A skeleton without headings is one part. |
| | 4. Create one checklist rubric per part; calculate part points from rubric items and the assignment total from part totals. AI suggestions must be reviewed and saved as rubrics by the author. |
| | 5. Add embedded Draw.io diagrams and optionally specify required diagram types/counts. Display diagram previews rather than raw XML. |
| | 6. Preview and request review. Validate the skeleton, diagram XML and an ACTIVE rubric for every part; follow UC 28 for publication or UC 21 for template release. |
| Alternative Flows | **A1 — Missing skeleton/rubric:** Refuse review. The skeleton is mandatory; no blank-page assignment is allowed. |
| | **A2 — Change heading structure:** Recalculate leaf parts. New parts need rubrics; headings that cease to be leaves lose their rubric association. Preserve unchanged part identity. |
| | **A3 — Invalid or oversized DOCX:** Reject files over 20 MB or unsafe imports. Preview accepted/omitted content before replacing the skeleton. |
| | **A4 — Invalid diagram XML:** Reject unsafe/invalid XML and preserve valid draft content. If required diagrams are configured, each type has a minimum of 1–20. |
| | **A5 — AI discarded/fails:** Keep the existing skeleton; apply UC 28/21 credit/error rules. AI proposes no diagrams and replacement is never automatic. |
| | **A6 — Copy or new version:** Clone part rubrics; copying a bank skeleton does not make edits update the bank source. |
| Postconditions | The skeleton, parts and rubric references are stored in assignment configuration without assignment-question rows. Every teacher block is locked for students; they add/edit only their own blocks under UC 30. |
| Traceability | U09 F2/F2a/F2c/F3 and BR-U09-24–48; U06 rubrics; U13 SKELETON_DRAFT; UC 20, 21 and 28. |

## 7. UC 26 — Manage Code Lab

| Field | Specification |
|---|---|
| Primary Actors | Teacher; Subject Manager |
| Secondary Actors | Code Sandbox; AI Service (when drafting questions) |
| Description | Author a GRADED or PRACTICE Code Lab with versioned problems, scored tests, resource limits and verified sample solutions. Extends UC 28; subject templates follow UC 21. |
| Preconditions | The author has class/template scope and a DRAFT CODE_LAB with a valid mode. |
| Normal Flow | 1. Open the code configuration editor; author questions or create/search/import/reuse bank versions. Code Lab supports 1–20 questions. |
| | 2. Configure language, starter files, sample solution, public/hidden tests and test points. Supported languages are Java, Python, C, C++, JavaScript, Dart and C#. |
| | 3. Set per-test time and memory limits: 100–10,000 ms and 64–1024 MB. |
| | 4. Press Verify Sample Solution. Run all tests in an isolated sandbox and retain verification tied to the current question content. |
| | 5. Preview and request review. Require every sample solution to pass all tests for the current content; publication/versioning follow UC 28 and template release follows UC 21. |
| Alternative Flows | **A1 — Sample solution fails:** Show verification results, refuse review and allow corrections/reverification. |
| | **A2 — Change problem, tests or sample solution:** Invalidate the prior verification and require a new run. |
| | **A3 — Sandbox unavailable:** Report verification failure/unavailability; never mark an unverified solution as passing. Code does not run on the application server. |
| | **A4 — Invalid language, resource limits, tests or import:** Reject invalid configuration/rows and preserve valid drafts. |
| | **A5 — Edit a used bank question:** Create a new version, preserving published assignment/attempt snapshots. AI question generation follows UC 28/21 and does not bypass sample verification. |
| Postconditions | The draft contains coding questions and current successful verification before review. Scoring is deterministic from passed tests without a rubric or AI scoring. Students receive only permitted public-test details and pass/fail for hidden tests. |
| Traceability | U09 type configuration; U06 CODE definitions; U13 BR-U13-30–37; UC 21, 28 and 30. |

## 8. UC 27 — Manage Group Assignment

| Field | Specification |
|---|---|
| Primary Actors | Teacher; Subject Manager |
| Secondary Actors | AI Service (when drafting a skeleton); Google Drive (document assets) |
| Description | Author a GRADED group-document assignment with fixed skeleton parts, independent per-part rubrics and valid class groups. Extends UC 28; Subject Managers author group templates under UC 21. |
| Preconditions | The Teacher teaches the class or the Subject Manager manages the template subject. The target is DRAFT GROUP_ASSIGNMENT and its mode is GRADED. |
| Normal Flow | 1. Open the appropriate editor and prepare the mandatory teacher skeleton manually, from the DOCUMENT bank or by supported DOCX import. |
| | 2. Optionally preview AI skeleton headings, guidance and rubric suggestions. Confirm replacing the current skeleton with a warning for existing content, as in UC 25. |
| | 3. Derive fixed work parts from leaf headings and create one rubric per part under UC 20. Calculate the document maximum from part totals. |
| | 4. Preview/review the assignment. Before class publication under UC 28, check the groups managed in UC 9: at least one group, valid membership and exactly one leader per group. |
| | 5. Publish with the class schedule. When work opens, initialize one shared document per group from the fixed skeleton; groups created later also receive documents for open group assignments. |
| | 6. The Teacher monitors group submissions and may release section claims. Leaders assign parts and students collaborate/submit under UC 16. |
| Alternative Flows | **A1 — PRACTICE mode:** Reject; group work is GRADED only. |
| | **A2 — Missing parts/rubrics:** Refuse review. Recalculate parts when headings change and require a rubric for every new leaf. |
| | **A3 — Invalid/no groups:** Refuse class publication. Warn about ungrouped students rather than treating the warning alone as a publication failure. |
| | **A4 — Subject template:** Do not attach groups or a schedule; check class groups only when its class copy is published. |
| | **A5 — Change/retire published work:** Follow UC 28; retirement triggers automatic group submission under UC 16. |
| | **A6 — Invalid source/import or AI failure:** Preserve the draft and follow UC 25/28/21 alternatives. |
| Postconditions | The class draft has fixed skeleton parts and per-part rubrics without assignment-question rows. The Teacher does not assign parts to students; the leader does. No student may add/delete/rename/move skeleton sections. Published work uses class groups and is assessed under UC 17. |
| Traceability | U09 skeleton authoring; U12 group readiness; U14 F1/F7; U13 AI drafts; UC 9, 16, 17, 20, 21 and 28. |

## 9. UC 30 — Submit Assignment

| Field | Specification |
|---|---|
| Primary Actors | Student |
| Secondary Actors | Code Sandbox (Code Lab); Google Drive (document assets) |
| Description | Complete individual work with autosave, immutable submission snapshots, receipts and type-appropriate scoring. UC 16 handles group work using common submission rules. |
| Preconditions | ACTIVE enrollment in an OPEN class, work still accepted and an attempt available. Starting a new attempt consumes one attempt; only one IN_PROGRESS attempt per student/assignment exists. |
| Normal Flow | 1. Start or resume an attempt. Snapshot the assignment, questions/rubrics and policies, including quiz shuffle seed. The attempt deadline is the earlier of the time limit and final submission deadline. |
| | 2. Complete the type-specific work below. Autosave 10 seconds after the last edit and when leaving, with a content version and last-saved indicator. |
| | 3. Submit: validate ownership, deadline and content, freeze the snapshot, record server time/lateness and return attempt ID/number, timestamp, late flag and receipt hash. |
| | 4. GRADED work creates an evaluation through U15. Score Quiz directly from the snapshot answer key; run all Code Lab tests asynchronously. PRACTICE Quiz/Code Lab returns private results; Practice Text/Diagram Essay stays ungraded until explicit UC 40. |
| Alternative Flows | **A1 — Attempts exhausted or assignment unavailable:** Reject a new attempt. Resuming the existing attempt does not consume another. |
| | **A2 — Version conflict:** Require reload without overwriting the latest valid draft. |
| | **A3 — Invalid manual submission:** Preserve the draft for correction while work is accepted. |
| | **A4 — Time limit/deadline/retirement:** Submit the latest saved content automatically, including final saves within the 30-second network grace period. Empty/incomplete work is still submitted, with validation warnings as applicable. |
| | **A5 — After deadline plus grace:** Reject. An autosave or code trial alone is not submission. |
| Postconditions | Submitted attempts/receipts are immutable. New attempts do not overwrite previous ones; the last GRADED submission is official. Practice results stay outside teacher grading, official gradebooks and exports. Automatic GRADED score visibility follows publication policy. |
| Traceability | U11 F2–F5; U09 validation; U13 code execution; U15 F1/F8; UC 31 and 40. |

### Type-specific flows

| Type | Editing and Validation | Scoring and Alternatives |
|---|---|---|
| Text Essay | Basic paragraphs, headings, lists, bold/italic; no tables, images or diagrams. No word/line limit beyond technical character limits. Snapshot each question and rubric. | GRADED answers await teacher rubric grading. Practice is submitted without automatic AI grading; UC 40 is available afterwards. |
| Multiple-Choice Quiz | Answer single/multiple-answer questions in the attempt's fixed shuffled order, with countdown if configured. Invalid references cannot replace saved valid answers. | Score from captured answer keys without a grading job. Multiple-answer questions require the exact correct answer set. GRADED scores publish immediately only if configured, otherwise await finalization/publication. Practice results are private; answer visibility follows NEVER, AFTER_SUBMIT or AFTER_CLOSE. |
| Diagram Essay | Edit student blocks while retaining every locked teacher block. Store validated full Draw.io XML and display previews. DOCX import shows a preview/report and adds STUDENT blocks only after confirmation, without replacing teacher content. | Manual submission validates structure, required/nonempty diagrams and student content; failures require correction. GRADED work awaits per-part rubric grading; Practice is eligible for explicit UC 40. |
| Code Lab | Autosave source in the configured language; run public tests in an isolated sandbox at most five times per minute. Hidden-test inputs/outputs stay private. | Run all tests on submitted source, with resource limits and no rubric. Sandbox failure preserves the submission, applies retry policy and reports pending/unavailable scoring without inventing a result. |

## 10. UC 16 — Submit Group Document

| Field | Specification |
|---|---|
| Primary Actors | Student (group member; group leader) |
| Secondary Actors | None |
| Description | Collaborate on fixed skeleton sections, merge completed private drafts into a shared document and submit manually or automatically. There is no REVIEW stage or section commenting. |
| Preconditions | The student is a current group member. A GRADED group assignment accepts work and has a shared document initialized from its teacher skeleton. |
| Normal Flow | 1. A member opens Assignment Workspace or the document from My Group. The system verifies access and shows shared content, section states, assignees and holders. |
| | 2. The leader assigns a section, or a member claims an OPEN/DONE section. One member holds each section; its completed content becomes their private draft. |
| | 3. The holder edits in a full-page popup showing the section and its ancestor heading branch, excluding sibling branches. The system autosaves a versioned private draft. |
| | 4. Done validates content, records revision/authorship, merges completed blocks and releases the claim. The popup closes and all viewers receive the update. |
| | 5. The leader requests submission before the deadline. Unfinished sections trigger a warning but do not prevent submission; unfinished private drafts are excluded from manual submission. |
| | 6. The system snapshots shared content/authors, returns a receipt, creates or resets pending group/member evaluations and notifies the group. |
| Alternative Flows | **A1 — Competing claim or version conflict:** Reject without replacing the current claim or last valid saved content. |
| | **A2 — Invalid content or changed teacher blocks:** Reject. No student, including the leader, may add, delete, rename or move skeleton sections. |
| | **A3 — Release/reassign:** The holder, leader or teacher may release a claim. Warn before manual release that unfinished edits will be discarded. Return to DONE if completed content exists, otherwise OPEN. Leaving the group also releases claims. |
| | **A4 — Non-leader submission:** Reject. Completing all sections does not change document state or send a review notification. |
| | **A5 — Resubmit before the deadline:** Replace the latest submitted snapshot; retain submission metadata in audit and previous scores in evaluation history. Grade only the latest submission. |
| | **A6 — Deadline or retirement:** At the final deadline plus 30-second final-save grace, or on retirement, submit the latest saved document including claimed private drafts, record revisions and close the document. All viewers see a submitting indicator then Submitted Assignment. |
| | **A7 — Connection loss:** Reload document/claim state on reconnection and recheck access and deadline. |
| Postconditions | Completed content has recorded authorship. The latest submission and receipt are preserved. Manual submission permits editing/resubmission before the deadline; an automatically closed document is read-only. |
| Traceability | U14 F1–F9; U12 membership; U15 evaluations; UC 30 common submission rules. |

## 11. UC 33 — Grade Submissions

| Field | Specification |
|---|---|
| Primary Actors | Teacher |
| Secondary Actors | AI Service (when assistance is requested) |
| Description | Grade individual/shared group GRADED documents using checklist rubrics or reviewed AI proposals and save teacher-controlled drafts. Individual member contribution grading remains manual under UC 17. |
| Preconditions | The Teacher teaches the class and has eligible submitted GRADED work with applicable rubrics. AI additionally requires enabled service and sufficient teacher credits. Practice is excluded. |
| Normal Flow | 1. Open work from Grading Queue and inspect pinned rubrics, attempts and automatic scores. |
| | 2. For manual rubric grading, check items per Text Essay question or Diagram Essay/group part. Compute rubric totals; feedback is optional. |
| | 3. Optionally request UC 40, review checklist/evidence/comments and press Use Proposal to fill the checklist/explanation; the Teacher may edit both. |
| | 4. For batch AI assistance, select submissions. Check each scope and sufficient credits for the whole batch before creating one request per submission. |
| | 5. Confirm each batch result in Grading Workspace. Previous/next controls navigate the filtered queue/batch; saving advances to the next submission. |
| | 6. Save draft score, feedback, grader and time through the same path for manual/AI-assisted grading. Finalization/publication are separate under UC 34. |
| Alternative Flows | **A1 — AI failure/timeout:** Preserve work/drafts; permit manual grading or retry. |
| | **A2 — Insufficient batch credits:** Reject the entire batch before AI runs. |
| | **A3 — Disagree with AI:** Edit checklist/explanation; differing from AI alone does not require a reason. |
| | **A4 — Change automatic Quiz/Code Lab score:** Require a reason and retain history. |
| | **A5 — Concurrent edit or unauthorized work:** Reject without overwriting newer data or exposing another class's work. |
| Postconditions | Draft evaluations are saved without publication. Students cannot see drafts or AI proposals; the Teacher controls the score. |
| Traceability | U15 F2, BR-U15-20–25; U13 F2; UC 17, 32, 34 and 40. |

## 12. UC 17 — Grade Group Document

| Field | Specification |
|---|---|
| Primary Actors | Teacher |
| Secondary Actors | AI Service (when assistance is requested) |
| Description | Grade the shared group document by part rubrics and assign member contribution scores without an automatic combining formula. |
| Preconditions | The Teacher teaches the class. The group has a submitted snapshot with a rubric for each skeleton part. |
| Normal Flow | 1. Open the latest submission in Grading Workspace as an ordinary DOCUMENT without author coloring. |
| | 2. Check part rubrics manually or request an AI proposal for the shared document through UC 40, then review/edit the checklist. |
| | 3. Sum part scores for the document score. Integration inconsistencies affect relevant part rubrics; there is no separate integration score. |
| | 4. Set each member's contribution score initially equal to the document score. The Teacher may adjust individual scores manually with an optional reason before finalization. |
| | 5. Save drafts and finalize/publish through UC 34. |
| Alternative Flows | **A1 — AI for individual contribution:** Reject; AI assists only with the shared document. |
| | **A2 — AI failure/insufficient credits:** Preserve work and drafts; allow manual grading or retry. |
| | **A3 — Group resubmits:** Reset group/member evaluations to PENDING for the latest snapshot and keep previous scores in history. |
| | **A4 — Change finalized/published score:** Require a reason and retain before/after values, actor and time; the optional reason for initial contribution grading does not override this rule. |
| Postconditions | Shared-document/member draft evaluations are saved. Students see their own final results only after publication; AI never finalizes grades. |
| Traceability | U15 F6, BR-U15-40–44; U14 F9; UC 33, 34 and 40. |

## 13. UC 34 — Finalize Grades

| Field | Specification |
|---|---|
| Primary Actors | Teacher |
| Secondary Actors | None |
| Description | Finalize valid draft evaluations and publish scores/feedback individually or in selected batches. |
| Preconditions | The Teacher teaches the class. Finalization requires GRADED DRAFT evaluations with valid scores and matching versions; publication requires FINALIZED evaluations. |
| Normal Flow | 1. Select a draft or batch in Grading Queue and request finalization. |
| | 2. Check scope, state, score and version per evaluation. Mark valid items FINALIZED and record audit/history. |
| | 3. Publish one finalized submission from Grading Workspace or selected finalized submissions in a batch from Grading Queue. |
| | 4. Mark eligible items PUBLISHED and record the assignment's first grade-release timestamp. |
| | 5. Students see their own final score/feedback on Assignment List. Send notifications after commit without scores in notification/email content. |
| Alternative Flows | **A1 — Invalid/concurrently changed item:** Reject that item and return its error; other valid batch items may succeed independently. |
| | **A2 — Difference from AI:** Do not require a reason solely for differing from the proposal. |
| | **A3 — Edit finalized/published score:** Require a reason and retain before/after values, actor/time. Published corrections display an updated indicator. |
| | **A4 — Practice or unauthorized target:** Reject; Practice has a separate private result flow. |
| | **A5 — Notification failure:** Keep the committed grade and retry notification delivery by policy. |
| Postconditions | Valid final grades are available only to authorized students. Failed batch items stay unchanged; history is retained and AI proposals remain private. |
| Traceability | U15 F3–F5, BR-U15-31–35; U16 notifications; UC 35. |

## 14. UC 37 — Buy AI Credits

| Field | Specification |
|---|---|
| Primary Actors | Student; Teacher; Subject Manager; Administrator |
| Secondary Actors | PayOS Payment Gateway |
| Description | Purchase fixed packages and receive credits exactly once after payment verification. All four roles receive the same fixed monthly free-credit grant. |
| Preconditions | ACTIVE signed-in buyer accessing their own wallet; active package and fewer than three PENDING payments for a new checkout. |
| Normal Flow | 1. Open AI Credits to view balance, packages, purchases and usage. First balance access in a new month resets free credits to the configured grant without accumulating prior grants. |
| | 2. Select a package and send an idempotent checkout request. Snapshot price/credits, create a CREATED payment and request a PayOS link valid for 15 minutes. |
| | 3. Mark PENDING after link creation and redirect. Payment Result checks status on return but never grants credits. |
| | 4. Receive the webhook; verify signature, order code, amount and successful status. |
| | 5. In one transaction change an unpaid payment to PAID, add purchased credits once and record audit. Notify the buyer after commit. |
| Alternative Flows | **A1 — Link creation fails:** Mark FAILED, grant nothing and permit a new checkout. |
| | **A2 — Invalid signature/mismatch:** Reject, write security audit and grant nothing. |
| | **A3 — Duplicate webhook/checkout:** Do not duplicate payment or credit. Return the existing checkout/status as applicable; reject a fourth PENDING payment. |
| | **A4 — Missing webhook:** Every ten minutes reconcile PENDING payments older than five minutes and EXPIRED payments within 24 hours, using the same exactly-once rule. Provider errors keep status for later retry. |
| | **A5 — Cancel/expiry:** Verify cancellation or expire unpaid links without granting credit. A later verified payment still grants credits because money was received. |
| | **A6 — Unauthorized Student AI request:** Retain wallet/checkout access but reject AI authoring, materials processing and GRADED grading. Students spend credit only on their own Practice Text/Diagram Essay under UC 40. |
| Postconditions | Verified payment increases purchased balance once. Free credits are spent first; purchased credits do not expire. Packages/monthly grants are fixed deployment configuration. Purchases are retained in payments and AI credit usage in ai_suggestions; there is no separate purchase ledger or webhook-event table. Refunds are outside MVP. |
| Traceability | U07 F1–F7; U13 usage; UC 22 and 40. |

## 15. UC 40 — Grade with AI

| Field | Specification |
|---|---|
| Primary Actors | Student; Teacher |
| Secondary Actors | AI Service |
| Description | Explicitly request private Practice grading for the student's submitted Text/Diagram Essay, or a Teacher proposal for a GRADED individual/shared group document. Practice submission does not invoke AI automatically. |
| Preconditions | ACTIVE requester with target access. Student targets are their own submitted PRACTICE Text/Diagram Essay with no valid AI result; Teacher targets are GRADED work in a class they teach. Check AI eligibility/credits at request time. |
| Normal Flow | 1. Student opens the popup from Submitted Assignment and presses Grade with AI; Teacher opens it from Grading Workspace and requests a proposal. |
| | 2. Check scope/type, limits and credits, reserve requester credits and create an AI request. Technical retries use the same reference. |
| | 3. Process the immutable submitted snapshot with pinned rubrics and compacted Draw.io content if needed; validate AI checklist outcomes, comments and evidence. |
| | 4. Compute the rubric score, settle actual credit usage and mark the result ready. |
| | 5. Student mode saves one private PRACTICE evaluation and displays score/feedback. Teacher mode shows a proposal; Use Proposal fills the checklist/explanation for confirmation under UC 33/17. |
| Alternative Flows | **A1 — Insufficient credits:** Do not call AI or charge. Buying credits permits another request on the same submitted attempt without a new submission. |
| | **A2 — AI disabled/quota reached:** Reject without charging for an unmade call; Teacher manual grading remains available. |
| | **A3 — Failure/invalid output:** Preserve submitted work, release reserved credits after final failure and permit a new request. Technical retries cannot duplicate a valid result/debit. |
| | **A4 — Timeout:** After five minutes from the request, mark failed, release credits and allow retry; discard late results. Batch items have five minutes from the start of their processing. |
| | **A5 — Existing result/concurrent request:** Return the existing result/state instead of duplicating successful Practice grading. |
| | **A6 — Unsupported Student target:** Reject other users' attempts, GRADED work and authoring requests. Practice Quiz/Code Lab use answer keys/tests; group work has no Practice mode. |
| | **A7 — Member contribution score:** Reject AI for individual contributions; Teacher assistance applies to the shared group document only. |
| Postconditions | At most one valid Practice AI result exists per attempt, without duplicate settlement. Practice stays outside official gradebooks, distributions and exports. Teacher proposals remain private and never finalize/publish grades. The assignment need not still accept submissions for Student grading, but access to the submitted attempt must remain authorized. |
| Traceability | U11 BR-U11-35; U13 F2/F3a, BR-U13-24; U07 F6; U15 F2/F8. |

## Sources

- [Current use-case catalog](use-case-table.md)
- [U05 content business rules](../aidlc-docs/construction/u05-content-material-rag/functional-design/business-rules.md)
- [U07 payment and credit flows](../aidlc-docs/construction/u07-payment-credit/functional-design/business-logic-model.md)
- [U08 assignment lifecycle](../aidlc-docs/construction/u08-assessment-core-publication/functional-design/business-logic-model.md)
- [U09 authoring and document rules](../aidlc-docs/construction/u09-question-type-authoring/functional-design/business-rules.md)
- [U10 template rules](../aidlc-docs/construction/u10-template-copy-simulation/functional-design/business-rules.md)
- [U11 attempts and submissions](../aidlc-docs/construction/u11-attempt-submission/functional-design/business-rules.md)
- [U13 AI and code execution](../aidlc-docs/construction/u13-ai-code-execution/functional-design/business-rules.md)
- [U14 group document flows](../aidlc-docs/construction/u14-group-document-submission/functional-design/business-logic-model.md)
- [U15 grading rules](../aidlc-docs/construction/u15-grading/functional-design/business-rules.md)
