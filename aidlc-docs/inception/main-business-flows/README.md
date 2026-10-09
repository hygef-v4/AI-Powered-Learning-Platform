# Main Business Flows

The eight main business flows follow the current MVP: 16 units, the 73 use cases of the SRS (version 6) and 49 stories, five assignment types with `GRADED`/`PRACTICE` modes, roles `STUDENT`, `TEACHER`, `SUBJECT_MANAGER` and `ADMIN`. They are derived from the approved requirements (`../requirements/requirements.md`, decisions up to 2026-10-04) and the `business-logic-model.md` / `business-rules.md` of each unit under `aidlc-docs/construction/`. Learning materials have no publication step, groups belong to the class, a group document has no review stage, there is no Simulation Exam, no lesson progress tracking and no refund. Payment only buys AI credits.

They cover the core journeys of the requirements (USCN-001 to USCN-006): class setup and enrollment, learning materials for AI authoring, assignment publication, individual and group submission, teacher grading, AI practice grading and AI credit purchase. Supporting flows (account activation and sign-in, material viewing, class groups and leader change, AI credit usage, notifications, reports and statistics) are not drawn as BF; their rules are in the unit designs (U01, U04, U05, U07, U12, U13, U16).

This directory contains one eight-page draw.io file, eight individual draw.io files, and eight PNG images for the SRS. The PNG files in `exports/` are rendered at 2x from the individual draw.io files with the diagrams.net viewer, so they match what draw.io displays. The diagrams are 1340 to 1930 px wide (seven to ten columns), so they stay readable at 100 % zoom when inserted at full width on a landscape page.

| ID | Business flow | Swimlanes | End outcomes | Use cases | Units |
|---|---|---|---|---|---|
| BF-01 | Subject and Class Setup, Staff Assignment and Enrollment | Administrator, Subject Manager, Student, System | Refused with reasons; Enrolled | UC 49, 50, 51, 65 | U04 |
| BF-02 | Learning Material Upload and Scanning | Subject Manager or Teacher, System | Upload rejected; NO_TEXT or NO_CAPTION; BUSY or NO_CREDIT; FAILED; Indexed | UC 34, 55 | U03, U05, U07, U13 |
| BF-03 | Assignment Authoring, Review and Publication | Teacher, System, Student | Publication refused; Assignment visible to students | UC 35, 42–46 | U06, U08, U09, U10, U12, U13 |
| BF-04 | Individual Attempt and Submission | Student, System | Attempt refused; Submission recorded | UC 20, 24–26 | U08, U09, U11, U13 |
| BF-05 | Group Document Collaboration and Submission | Group Member, Group Leader, System | Group submission recorded | UC 16, 27 | U12, U14 |
| BF-06 | Grading and Grade Publication (GRADED) | Teacher, System, Student | Student sees grade and feedback | UC 21, 28, 37–39 | U13, U15 |
| BF-07 | Practice Result and Grade with AI | Student, System | Practice result shown; Saved, not AI-graded; Refused or not enough credit; Error with credit released; AI practice feedback shown | UC 20, 24–26, 29 | U11, U13, U07 |
| BF-08 | AI Credit Purchase | User, System, PayOS | Payment FAILED; Rejected; Expired, no credit; Credits added | UC 9–11 | U07 |

## Flow details

### BF-01 Subject and Class Setup, Staff Assignment and Enrollment

**Trigger**: An administrator creates a subject and assigns its Subject Manager.

**End condition**: The student is enrolled in the open class, or an ineligible student is refused with a reason.

**Text alternative**: Only the administrator creates a subject and assigns one Subject Manager, who must hold the `SUBJECT_MANAGER` role and not be `DISABLED`. The subject's Subject Manager (or the administrator) creates a `DRAFT` class in an `ACTIVE` subject and assigns exactly one primary teacher with the `TEACHER` or `SUBJECT_MANAGER` role (UC 49, 50). Invalid input loops back for correction, and each save is audited. A class can open only when its subject is `ACTIVE` and a primary teacher is assigned. The Subject Manager (or the administrator) selects students and adds them to the class (UC 51); a teacher cannot add students, and there is no invitation code or self-enrollment. The backend checks each selected student separately: the account must be an `ACTIVE` student who has no other `ACTIVE` enrollment in a non-archived class of the same subject, and the class must have room. Ineligible students are refused with a reason; eligible students are enrolled once as `ACTIVE`, the enrollment is audited, and when the class is `OPEN` the student receives an in-app notification and an email.

### BF-02 Learning Material Upload and Scanning

**Trigger**: A Subject Manager or teacher uploads a file or attaches a YouTube video to a module.

**End condition**: The lesson is visible and indexed for AI, stays unindexed, or awaits a rescan; an invalid upload is rejected.

**Text alternative**: Only the Subject Manager creates, orders and archives subject modules; every class of the subject uses them. In a module the Subject Manager uploads a subject material (visible to all classes) and a class teacher uploads a class material (visible to that class only). Each lesson is one PDF, DOCX or PPTX file, or one YouTube video; playlists are not accepted. The backend checks the scope, the file type by magic bytes, the 50 MB size limit and the single-video URL, and rejects invalid input with a reason. Files are stored in the Shared Drive. The lesson is saved as `ACTIVE` and is visible to students at once; there is no draft, version or publication step. Its scan status is `PENDING` and a scan job is queued. The worker extracts text without OCR or fetches existing captions without transcription. Without text the status is `NO_TEXT` or `NO_CAPTION` and the lesson stays visible. The worker then checks the AI switch and the daily cost cap and reserves the uploader's credit; a refused call ends as `BUSY` or `NO_CREDIT` with no charge. Gemini creates the lesson embedding. Transient errors are retried; when retries run out, or the error is permanent, the status is `FAILED`. On success the text, embedding and `INDEXED` status are saved in one transaction and the credit is settled. The uploader and the scope manager can select Rescan for `BUSY`, `NO_CREDIT` and `FAILED`.

### BF-03 Assignment Authoring, Review and Publication

**Trigger**: A teacher creates a draft assignment for a class they teach.

**End condition**: The assignment opens and students are notified, or publication is refused with the reasons.

**Text alternative**: The teacher creates a `DRAFT` assignment, chooses one of the five types and the `GRADED` or `PRACTICE` mode (a group assignment is always `GRADED`), and starts blank, from a copied subject template or from a copy of another class's assignment. For an AI draft the backend checks the AI switch, the daily cap and the rate limit and reserves the teacher's credit. If AI is busy or credit is short, the teacher writes items or picks them from the question bank. Otherwise the worker retrieves the subject and class materials through RAG, drafts items, or a document skeleton with rubric suggestions for Diagram Essay and group work, and settles the credit. The teacher keeps and edits the selected items. The teacher arranges the items and sets points and rubrics: one rubric per Text Essay question and one per document part; Quiz and Code Lab have none. After a preview the backend validates the items, the type configuration and the rubrics, and checks that a Code Lab sample solution passes every test; failures loop back. A valid assignment becomes `REVIEWED`. The teacher sets the opening and closing times, the late policy, 1 to 10 attempts and the instant-results option, then publishes. The backend checks that the version is `REVIEWED`, the class is `OPEN` and the opening time is before the closing time. Group work also needs ready class groups: at least one group and exactly one leader in each; ungrouped students only produce a warning. A refused publication lists the reasons. Otherwise the assignment becomes `SCHEDULED`, its content is locked and the publication is audited. At the opening time it becomes `OPEN`, `assignment.opened` is emitted, group documents are created, and students see the assignment and receive an in-app notice and an email.

### BF-04 Individual Attempt and Submission

**Trigger**: A student starts an attempt on an open individual assignment.

**End condition**: The submission is recorded with a receipt, or the attempt is refused.

**Text alternative**: The backend checks the `ACTIVE` enrollment, the `OPEN` class, the submission window and the remaining attempts for both `GRADED` and `PRACTICE`. A student has at most one attempt in progress; starting again reopens it. A new attempt stores a snapshot of the version and policy, and its deadline is the earlier of the time limit and the final deadline. The student answers with autosave every 10 seconds; Code Lab can run the public tests in Judge0 up to 5 times a minute without submitting. A manual submission is validated (a document needs the full Draw.io XML) and invalid content loops back. At the time limit, the deadline (plus a 30-second network grace) or on retirement, the worker submits the last saved draft. The submission is frozen with the server time, a late flag, a receipt hash and an audit entry. Quiz is scored by its answer key and Code Lab by all tests; essays wait for BF-06 (`GRADED`) or BF-07 (`PRACTICE`).

### BF-05 Group Document Collaboration and Submission

**Trigger**: A group assignment opens for the class groups.

**End condition**: The group document is submitted by the leader, or automatically at the deadline.

**Text alternative**: When the assignment opens, the worker creates one group document for each class group, including groups added later, and turns each leaf part of the teacher's skeleton into a fixed part with its own rubric. Nobody, including the leader, adds, deletes, renames or moves parts. Members open the document with live updates over SSE. The leader may assign parts to members, and members can also take an assigned part or claim one that is `OPEN` or `DONE`. Each part has one holder at a time; a taken part loops back. The holder edits a private draft with autosave in a full-screen popup that shows only the headings of that part's branch, then selects Done. The backend validates the blocks, publishes them into the shared document, pushes the update and records a revision with its author; the part becomes `DONE` and the lock is released. The leader or the teacher can release a held part after a warning that the unfinished draft will be discarded. There is no review stage. The leader can submit or resubmit at any time before the deadline, with a warning if parts are unfinished; drafts that are not Done are excluded. While the leader does not submit, members keep editing their parts. Reaching the deadline is a separate system start: thirty seconds after the final deadline, or when the assignment is retired, the worker submits the current document including held drafts and closes it. The frozen snapshot keeps the part authors, a resubmission overwrites the previous snapshot, and grading is queued.

### BF-06 Grading and Grade Publication (GRADED)

**Trigger**: A student or group submits GRADED work.

**End condition**: The grade is published and the student sees the score with feedback.

**Text alternative**: A `PENDING` evaluation is created for the last submitted attempt, or for the group document plus one per member. Quiz is scored by its answer key and Code Lab by all Judge0 tests; a sandbox error keeps the grade `PENDING`. The auto score is saved as `DRAFT`, or is `PUBLISHED` at once when instant results are on. For other work the teacher opens the Grading Queue and Workspace. The teacher can ask for an AI proposal for one submission or a selected batch: the backend checks the AI switch and caps and reserves the teacher's credit, and Gemini proposes a rubric checklist, comments and evidence. When the switch, caps or credit refuse the request, the AI returns an error or an invalid result, or no result arrives within 5 minutes, the reserved credit is released (nothing is charged, late output is ignored) and the flow returns to the teacher's choice: request the proposal again or grade manually. The teacher ticks the rubric checklist per question or part, prefilled when a proposal is ready; a score that differs from the proposal needs no reason. For a group document the score is the sum of the parts, and each member's score starts at the document score and can be changed manually with an optional reason. Overriding an auto score, or changing a finalized or published grade, requires a reason. The teacher saves a `DRAFT`, then finalizes one or many grades; history and audit are kept. Publishing, one by one or for the selected finalized grades, sets `PUBLISHED` and emits `grade.published`. The student sees "x / total" and the feedback. A group resubmission returns the evaluations to `PENDING` and keeps earlier scores in the history.

### BF-07 Practice Result and Grade with AI

**Trigger**: A student submits a PRACTICE attempt.

**End condition**: The student sees a private result or AI feedback, the essay stays saved without AI grading, or the AI request ends with no credit charged.

**Text alternative**: `PRACTICE` work never goes to the teacher queue, the gradebook or exports. Quiz and Code Lab results come from the answer key or the tests and use no credit. Submitting an essay does not call AI. If the student never requests AI grading, the essay stays saved without a score (MSG18 in the SRS) and the flow ends there. On a submitted Text Essay or Diagram Essay attempt the student selects Grade with AI. The backend checks that the account is `ACTIVE`, the student owns the attempt and is enrolled, the attempt is a submitted `PRACTICE` Text or Diagram Essay without a valid AI result, the AI switch and quota allow the call, and enough credit can be reserved. A refusal or a lack of credit costs nothing; the student can buy credits and select Grade with AI again. Gemini grades against the rubric using a compact diagram XML derived from the full submission. An error, an invalid output or no result within 5 minutes releases the credit and allows a retry. Otherwise the one practice evaluation for the attempt is saved and the credit is settled by tokens.

### BF-08 AI Credit Purchase

**Trigger**: A user selects an AI credit package to buy.

**End condition**: Credits are added once after a verified payment, or the payment fails, is rejected or expires with no credit added.

**Text alternative**: The user chooses one of the fixed packages configured at deployment. The backend creates a `PENDING` payment with an order code, a price snapshot and an `Idempotency-Key`; a user can have at most three pending payments. PayOS creates a payment link valid for 15 minutes; if that fails the payment is `FAILED` and the user starts a new one. The user pays by QR, and the return page only shows the status. PayOS sends a signed webhook. The backend verifies the HMAC signature, the order code, the amount and the result code; an invalid webhook is rejected and audited as a security event with no credit. A valid one sets `PAID` and adds purchased credits once in a single transaction, and duplicates are ignored. A valid payment that arrives after the link has `EXPIRED` is still credited. Every 10 minutes the worker queries PayOS for payments that have been `PENDING` for more than 5 minutes or `EXPIRED` in the last 24 hours, applies the same step when PayOS reports them paid, and otherwise expires links older than 15 minutes: the payment becomes `EXPIRED` and no credit is added. There are no refunds and purchased credits do not expire.

## Diagram conventions

- Each page shows one pool named after the business flow, with horizontal lanes and a left-to-right flow. Lanes show the main actors only, one lane per actor: the people involved and one **System** lane. When several roles may perform a step, the lane shows the usual actor and the text alternative names the others. The System lane covers everything inside the platform and the services it calls on its own (backend, worker, PostgreSQL, Redis, RabbitMQ, Google Drive, Gemini, YouTube, Judge0, SMTP). PayOS keeps its own lane in BF-08 because the user pays on the PayOS checkout.
- Lane colors: green for people, yellow for the System, purple for PayOS.
- Start and End are BPMN-style circles: Start is a green circle and End is a red circle, with the trigger or business outcome written next to the circle. Every End states its business outcome, and every decision branch is labelled. A lane can hold a second Start when a scheduled job or another actor starts part of the same flow; a Start in the System lane marks a system trigger.
- Connectors have fixed exit and entry points and never pass through a shape, overlap or cross another connector. Loop-backs and bypass routes run along the top or bottom edge of a lane.
- To keep each flow compact, a check and its decision are drawn as one diamond (for example "File valid?"); the checked conditions are listed in the text alternative.
- Terminology follows the current catalog: Student, Teacher, Subject Manager, Administrator.
- Technical placement stays in the text alternatives and unit designs: PostgreSQL holds business state, RabbitMQ carries jobs, Redis holds OTP, sessions and download tokens with a TTL, and Google Drive holds file bytes.
