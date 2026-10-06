# Main Business Flows

The eight main business flows follow the current MVP: 16 units, 40 use cases and 49 stories, five assignment types with `GRADED`/`PRACTICE` modes, roles `STUDENT`, `TEACHER`, `SUBJECT_MANAGER` and `ADMIN`. They are derived from the approved requirements (`../requirements/requirements.md`, decisions up to 2026-10-04) and the `business-logic-model.md` / `business-rules.md` of each unit under `aidlc-docs/construction/`. Learning materials have no publication step, groups belong to the class, a group document has no review stage, there is no Simulation Exam, no lesson progress tracking and no refund. Payment only buys AI credits.

They cover the core journeys of the requirements (USCN-001 to USCN-006): class setup and enrollment, learning materials for AI authoring, assignment publication, individual and group submission, teacher grading, AI practice grading and AI credit purchase. Supporting flows (account activation and sign-in, material viewing, class groups and leader change, AI credit usage, notifications, reports and statistics) are not drawn as BF; their rules are in the unit designs (U01, U04, U05, U07, U12, U13, U16).

This directory contains one eight-page draw.io file, eight individual draw.io files, and eight PNG images for the SRS. The PNG files in `exports/` are rendered from the individual draw.io files with the diagrams.net viewer, so they match what draw.io displays.

| ID | Business flow | Swimlanes | End outcomes | Use cases | Units |
|---|---|---|---|---|---|
| BF-01 | Subject and Class Setup, Staff Assignment and Enrollment | Administrator, Administrator or Subject Manager, Class Manager, Student, System | Generic invalid-code message; Enrolled | UC 8, 9, 10 | U04 |
| BF-02 | Learning Material Upload and Scanning | Subject Manager or Teacher, System | Upload rejected; NO_TEXT or NO_CAPTION; BUSY or NO_CREDIT; FAILED; Indexed | UC 11 | U03, U05, U07, U13 |
| BF-03 | Assignment Authoring, Review and Publication | Teacher, System, Student | Publication refused; Assignment visible to students | UC 20, 21, 23–28 | U06, U08, U09, U10, U12, U13 |
| BF-04 | Individual Attempt and Submission | Student, System | Attempt refused; Submission recorded | UC 29, 30, 31 | U08, U09, U11, U13 |
| BF-05 | Group Document Collaboration and Submission | Group Member, Group Leader, System | Group submission recorded | UC 16, 27 | U12, U14 |
| BF-06 | Grading and Grade Publication (GRADED) | Teacher, System, Student | Student sees grade and feedback | UC 17, 32–35, 40 | U13, U15 |
| BF-07 | Practice Result and Grade with AI | Student, System | Practice result shown; Refused or not enough credit; Error with credit released; AI practice feedback shown | UC 30, 31, 40 | U11, U13, U07 |
| BF-08 | AI Credit Purchase | User, System, PayOS | Payment FAILED; Rejected; Credits added | UC 37 | U07 |

## Flow details

### BF-01 Subject and Class Setup, Staff Assignment and Enrollment

**Trigger**: An administrator creates a subject and assigns its Subject Manager.

**End condition**: The class is set up with its teacher and students are enrolled, or the invite code is rejected.

**Text alternative**: Only the administrator creates a subject and assigns one Subject Manager, who must hold the `SUBJECT_MANAGER` role and not be `DISABLED`. The administrator or the subject's Subject Manager creates a `DRAFT` class in an `ACTIVE` subject and assigns exactly one primary teacher with the `TEACHER` or `SUBJECT_MANAGER` role. Invalid input loops back for correction, and each save is audited. A class manager (the administrator, the class teacher or the Subject Manager) opens the class, which needs a teacher and an `ACTIVE` subject, and manages the invite code. Students are added by search, pasted emails or a one-column CSV of up to 200 rows; each row is checked separately and valid rows are enrolled. A student can also self-enroll with an invite code; the backend checks the attempt limit, that the code is enabled and unexpired, that the class is `OPEN` and that the caller is a student. An invalid code gets a generic message that reveals nothing about the class. Each student is enrolled once as `ACTIVE`, the enrollment is audited, and when the class is `OPEN` the `enrollment.activated` event leads to an in-app notification and an email.

### BF-02 Learning Material Upload and Scanning

**Trigger**: A Subject Manager or teacher uploads a file or attaches a YouTube video to a module.

**End condition**: The material is visible to students and indexed for AI, or it is rejected or awaits a rescan.

**Text alternative**: Only the Subject Manager creates, orders and archives subject modules; every class of the subject uses them. In a module the Subject Manager uploads a subject material (visible to all classes) and a class teacher uploads a class material (visible to that class only). Each lesson is one PDF, DOCX or PPTX file, or one YouTube video; playlists are not accepted. The backend checks the scope, the file type by magic bytes, the 50 MB size limit and the single-video URL, and rejects invalid input with a reason. Files are stored in the Shared Drive. The lesson is saved as `ACTIVE` and is visible to students at once; there is no draft, version or publication step. Its scan status is `PENDING` and a scan job is queued. The worker extracts text without OCR or fetches existing captions without transcription. Without text the status is `NO_TEXT` or `NO_CAPTION` and the lesson stays visible. The worker then checks the AI switch and the daily cost cap and reserves the uploader's credit; a refused call ends as `BUSY` or `NO_CREDIT` with no charge. Gemini creates the lesson embedding. Transient errors are retried; when retries run out, or the error is permanent, the status is `FAILED`. On success the text, embedding and `INDEXED` status are saved in one transaction and the credit is settled. The uploader and the scope manager can select Rescan for `BUSY`, `NO_CREDIT` and `FAILED`.

### BF-03 Assignment Authoring, Review and Publication

**Trigger**: A teacher creates an assignment for a class they teach.

**End condition**: The assignment opens for students, or its publication is refused.

**Text alternative**: The teacher creates a `DRAFT` assignment, chooses one of the five types and the `GRADED` or `PRACTICE` mode (a group assignment is always `GRADED`), and starts blank, from a copied subject template or from a copy of another class's assignment. For an AI draft the backend checks the AI switch, the daily cap and the rate limit and reserves the teacher's credit. If AI is busy or credit is short, the teacher writes items or picks them from the question bank. Otherwise the worker retrieves the subject and class materials through RAG, drafts items, or a document skeleton with rubric suggestions for Diagram Essay and group work, and settles the credit. The teacher keeps and edits the selected items. The teacher arranges the items and sets points and rubrics: one rubric per Text Essay question and one per document part; Quiz and Code Lab have none. After a preview the backend validates the items, the type configuration and the rubrics, and checks that a Code Lab sample solution passes every test; failures loop back. A valid assignment becomes `REVIEWED`. The teacher sets the opening and closing times, the late policy, 1 to 10 attempts and the instant-results option, then publishes. The backend checks that the version is `REVIEWED`, the class is `OPEN` and the opening time is before the closing time. Group work also needs ready class groups: at least one group and exactly one leader in each; ungrouped students only produce a warning. A refused publication lists the reasons. Otherwise the assignment becomes `SCHEDULED`, its content is locked and the publication is audited. At the opening time it becomes `OPEN`, `assignment.opened` is emitted, group documents are created, and students see the assignment and receive an in-app notice and an email.

### BF-04 Individual Attempt and Submission

**Trigger**: A student starts an attempt on an open individual assignment.

**End condition**: The submission is recorded with a receipt, or the attempt is refused.

**Text alternative**: The backend checks the `ACTIVE` enrollment, the `OPEN` class, the submission window and the remaining attempts for both `GRADED` and `PRACTICE`. A student has at most one attempt in progress; starting again reopens it. A new attempt stores a snapshot of the version and policy, and its deadline is the earlier of the time limit and the final deadline. The student answers with autosave every 10 seconds; Code Lab can run the public tests in Judge0 up to 5 times a minute without submitting. A manual submission is validated (a document needs the full Draw.io XML) and invalid content loops back. At the time limit, the deadline (plus a 30-second network grace) or on retirement, the worker submits the last saved draft. The submission is frozen with the server time, a late flag, a receipt hash and an audit entry. Quiz is scored by its answer key and Code Lab by all tests; essays wait for BF-06 (`GRADED`) or BF-07 (`PRACTICE`).

### BF-05 Group Document Collaboration and Submission

**Trigger**: A group assignment opens for the class groups.

**End condition**: The group document is submitted by the leader, or automatically at the deadline.

**Text alternative**: When the assignment opens, the worker creates one group document for each class group, including groups added later, and turns each leaf part of the teacher's skeleton into a fixed part with its own rubric. Nobody, including the leader, adds, deletes, renames or moves parts. Members open the document with live updates over SSE. The leader may assign parts to members, and members can also take an assigned part or claim one that is `OPEN` or `DONE`. Each part has one holder at a time; a taken part loops back. The holder edits a private draft with autosave in a full-screen popup that shows only the headings of that part's branch, then selects Done. The backend validates the blocks, publishes them into the shared document, pushes the update and records a revision with its author; the part becomes `DONE` and the lock is released. The leader or the teacher can release a held part after a warning that the unfinished draft will be discarded. There is no review stage. The leader can submit or resubmit at any time before the deadline, with a warning if parts are unfinished; drafts that are not Done are excluded. Thirty seconds after the final deadline, or when the assignment is retired, the worker submits the current document including held drafts and closes it. The frozen snapshot keeps the part authors, a resubmission overwrites the previous snapshot, and grading is queued.

### BF-06 Grading and Grade Publication (GRADED)

**Trigger**: A student or group submits GRADED work.

**End condition**: The teacher publishes the final grade, and the student sees it with feedback.

**Text alternative**: A `PENDING` evaluation is created for the last submitted attempt, or for the group document plus one per member. Quiz is scored by its answer key and Code Lab by all Judge0 tests; a sandbox error keeps the grade `PENDING`. The auto score is saved as `DRAFT`, or is `PUBLISHED` at once when instant results are on. For other work the teacher opens the Grading Queue and Workspace. The teacher can ask for an AI proposal for one submission or a selected batch: the backend checks the AI switch and caps and reserves the teacher's credit, and Gemini proposes a rubric checklist, comments and evidence. An error, or no result within 5 minutes, releases the credit and the teacher grades manually. The teacher ticks the rubric checklist per question or part, prefilled when a proposal is ready; a score that differs from the proposal needs no reason. For a group document the score is the sum of the parts, and each member's score starts at the document score and can be changed manually with an optional reason. Overriding an auto score, or changing a finalized or published grade, requires a reason. The teacher saves a `DRAFT`, then finalizes one or many grades; history and audit are kept. Publishing, one by one or for the selected finalized grades, sets `PUBLISHED` and emits `grade.published`. The student sees "x / total" and the feedback. A group resubmission returns the evaluations to `PENDING` and keeps earlier scores in the history.

### BF-07 Practice Result and Grade with AI

**Trigger**: A student submits a PRACTICE attempt.

**End condition**: The student sees a private practice result, or the AI request ends with no credit charged.

**Text alternative**: `PRACTICE` work never goes to the teacher queue, the gradebook or exports. Quiz and Code Lab results come from the answer key or the tests and use no credit. Submitting an essay does not call AI. On a submitted Text Essay or Diagram Essay attempt the student selects Grade with AI. The backend checks that the account is `ACTIVE`, the student owns the attempt and is enrolled, the attempt is a submitted `PRACTICE` Text or Diagram Essay without a valid AI result, the AI switch and quota allow the call, and enough credit can be reserved. A refusal or a lack of credit costs nothing; the student can buy credits and select Grade with AI again. Gemini grades against the rubric using a compact diagram XML derived from the full submission. An error, an invalid output or no result within 5 minutes releases the credit and allows a retry. Otherwise the one practice evaluation for the attempt is saved and the credit is settled by tokens.

### BF-08 AI Credit Purchase

**Trigger**: A user buys an AI credit package.

**End condition**: Credits are added once after a verified payment, or no credit is added.

**Text alternative**: The user chooses one of the fixed packages configured at deployment. The backend creates a `PENDING` payment with an order code, a price snapshot and an `Idempotency-Key`; a user can have at most three pending payments. PayOS creates a payment link valid for 15 minutes; if that fails the payment is `FAILED` and the user starts a new one. The user pays by QR, and the return page only shows the status. PayOS sends a signed webhook. The backend verifies the HMAC signature, the order code, the amount and the result code; an invalid webhook is rejected and audited as a security event with no credit. A valid one sets `PAID` and adds purchased credits once in a single transaction, and duplicates are ignored. A valid payment that arrives after the link has `EXPIRED` is still credited. Every 10 minutes the worker queries PayOS for payments that have been `PENDING` for more than 5 minutes or `EXPIRED` in the last 24 hours, applies the same step when PayOS reports them paid, and expires stale links. There are no refunds and purchased credits do not expire.

## Diagram conventions

- Each page shows one pool named after the business flow, with horizontal lanes for the main actors only: the people involved and one **System** lane. The System lane covers everything inside the platform and the services it calls on its own (backend, worker, PostgreSQL, Redis, RabbitMQ, Google Drive, Gemini, YouTube, Judge0, SMTP). PayOS keeps its own lane in BF-08 because the user pays on the PayOS checkout.
- Lane colors: green for people, yellow for the System, purple for PayOS.
- Every End shape states its business outcome, and every decision branch is labelled. A lane can hold a second Start when a scheduled job or another actor starts part of the same flow; a Start in the System lane marks a system trigger.
- Connectors have fixed exit and entry points and never pass through a shape, overlap or cross another connector. Loop-backs and bypass routes run along the top or bottom edge of a lane.
- Terminology follows the current catalog: Student, Teacher, Subject Manager, Administrator.
- Technical placement stays in the text alternatives and unit designs: PostgreSQL holds business state, RabbitMQ carries jobs, Redis holds OTP, sessions and download tokens with a TTL, and Google Drive holds file bytes.
