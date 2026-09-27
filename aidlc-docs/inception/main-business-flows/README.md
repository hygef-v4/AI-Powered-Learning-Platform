# Main Business Flows

The eleven swimlane flows follow the current MVP design: 16 units, 77 use cases and 49 stories. They are derived from the approved requirements, the use case catalog (`docs/use-case-table.md`) and the `business-logic-model.md` / `business-rules.md` of each unit under `aidlc-docs/construction/`. There is no lesson progress tracking, no AI summarization or lesson splitting, and no payment-based class access. Payment only buys AI credits.

This directory contains one eleven-page draw.io file, eleven individual draw.io files, and eleven PNG images ready for insertion into the SRS. The PNG files in `exports/` are rendered from the individual draw.io files with the diagrams.net viewer, so they match what draw.io displays.

| ID | Business flow | Swimlanes | End outcomes | Use cases | Units |
|---|---|---|---|---|---|
| BF-01 | Account Activation and Sign-In | User, Backend, Redis, Worker and SMTP, PostgreSQL | Neutral response, no email; Neutral sign-in error; Signed in | UC01, UC02 | U01, U02 |
| BF-02 | Subject and Class Setup, Staff Assignment and Enrollment | Administrator, Instructor, Learner, Backend, PostgreSQL | Generic invalid-code message; Learner enrolled | UC14, UC16, UC18, UC20, UC21, UC23, UC25 | U04 |
| BF-03 | Learning Material Upload and RAG Indexing | Instructor or Subject Manager, Backend, Google Drive, Worker, Gemini and YouTube, PostgreSQL | Upload rejected; NO_TEXT or NO_CAPTION; FAILED (busy or no credit); Source indexed | UC26, UC27, UC32 | U03, U05, U07 |
| BF-04 | Assignment Authoring, Review and Publication | Instructor, Backend, Worker and Gemini, PostgreSQL, Learner | Assignment available to learners | UC45, UC49–UC54 | U06, U08, U09, U13 |
| BF-05 | Lesson Publication and Class Content Access | Instructor, Learner, Backend, Google Drive, PostgreSQL | Publication refused; Access denied; Lesson viewed; File downloaded | UC28, UC29, UC42 | U03, U04, U05 |
| BF-06 | Group Set Setup and Leader Change | Instructor, Learner, Backend, PostgreSQL | Leader unchanged; Leader changed | UC34, UC35, UC36 | U12 |
| BF-07 | Individual Attempt and Submission | Learner, Backend, Worker, PostgreSQL | Attempt refused; Submission recorded | UC55–UC60, UC64 | U08, U09, U11, U13 |
| BF-08 | Group Document Collaboration and Submission | Group Member, Group Leader, Backend, Worker, PostgreSQL | Group submission recorded | UC37, UC38, UC39 | U12, U14 |
| BF-09 | Grading and Grade Publication | Instructor, Backend, Worker, Gemini and Judge0, PostgreSQL, Learner | Learner sees final grade and feedback | UC40, UC65–UC70 | U13, U15 |
| BF-10 | AI Credit Purchase | User, Backend, PayOS, Worker, PostgreSQL | Payment FAILED; Event stored as REJECTED; Credits granted | UC75 | U07 |
| BF-11 | Notification and Deadline Reminder | Backend, Worker, PostgreSQL, SMTP, User | In-app notification received; Notification and email received | UC72, UC76 | U16 |

## Flow details

### BF-01 Account Activation and Sign-In

**Trigger**: A user with a school-issued account requests activation or signs in with their school email.

**End condition**: The user is signed in with the correct role, or receives a neutral error.

**Text alternative**: The user asks to activate a school account. If the request limit is exceeded or the account is not a PENDING school account, the backend returns the same neutral response and sends no email. Otherwise a job is saved, the worker generates an OTP, stores its hash in Redis with an expiry and emails the code. The user enters the OTP and a new password. A wrong code or weak password loops back for another try. A valid entry saves the password hash, sets the account ACTIVE and deletes the OTP. On sign-in the backend checks the rate limit, lock, status and password, and locks the account for 15 minutes after five failures. Every failure gets one neutral error. Success creates a refresh session in Redis and a 15-minute access token.

### BF-02 Subject and Class Setup, Staff Assignment and Enrollment

**Trigger**: An administrator creates a subject or class and assigns its staff.

**End condition**: The subject and class are saved with their staff, and learners are enrolled.

**Text alternative**: The administrator creates a subject and assigns its Subject Manager, then creates a DRAFT class with exactly one primary instructor. Invalid codes or roles loop back for correction. The instructor opens the class and enrolls learners from an email list or CSV (up to 200 emails, with per-row results), or learners self-enroll with an invite code. An invalid code gets a generic message that reveals no class information. Each learner is enrolled once as ACTIVE, the enrollment is audited, and a notification event is emitted.

### BF-03 Learning Material Upload and RAG Indexing

**Trigger**: An instructor or Subject Manager adds a file, text or YouTube section to a lesson.

**End condition**: The source is indexed for AI authoring, or it is rejected or marked failed with a reason.

**Text alternative**: An authorized user adds a section to a lesson draft. Uploaded files are checked for permission, size and magic bytes, then stored in Google Shared Drive. Text sections are saved directly, and YouTube URLs are validated (a playlist resolves to up to 50 videos). Each new source is saved as PENDING and gets an ingest job. A source that is already indexed is reused without new cost. The worker extracts text or fetches existing captions. Audio is never transcribed. Sources without text are marked NO_TEXT or NO_CAPTION. The worker then checks the system AI quota and reserves the uploader's credit. If the quota is exhausted the flow reports "system busy". Otherwise Gemini creates embeddings, and the chunks are saved as INDEXED while the credit is settled. A lesson can be published before indexing finishes.

### BF-04 Assignment Authoring, Review and Publication

**Trigger**: An instructor creates an assignment, manually or with an AI draft.

**End condition**: The reviewed version is locked and published, and learners can see it when it opens.

**Text alternative**: The instructor creates a DRAFT assignment (Quiz, Essay, DOCUMENT or Code Lab). An optional AI draft passes the AI switch, daily cap, rate limit and credit reservation first. The worker then retrieves class RAG sources, generates items with Gemini and validates them. The instructor keeps the selected items. When AI is busy or credit is insufficient, the instructor authors items manually or from the bank. After preview, the backend validates items, type configuration and Code Lab sample solutions, and failures loop back. A REVIEWED version is published to an OPEN class with a schedule, attempt limit and late policy. Group work also needs a ready group set. Publishing locks the version and schedules open/close jobs. At the open time the assignment becomes OPEN for learners.

### BF-05 Lesson Publication and Class Content Access

**Trigger**: An instructor publishes a lesson and an enrolled learner opens the class.

**End condition**: The learner views the lesson or downloads its file, or access is denied.

**Text alternative**: The instructor publishes a lesson draft that has at least one section, and the previous version becomes SUPERSEDED. A learner opens an enrolled class. Access requires an ACTIVE enrollment and an OPEN class. The learner reads the published lessons. A file download uses a 5-minute token bound to the account and is streamed from Shared Drive. No completion or progress is recorded.

### BF-06 Group Set Setup and Leader Change

**Trigger**: An instructor sets up groups for a group assignment, or a member requests a leader change.

**End condition**: Every group has exactly one leader, and the leader-change request is approved or rejected.

**Text alternative**: For a group assignment the instructor builds groups manually, reuses another assignment's groups, or splits learners randomly and then adjusts the result. The backend requires enrolled members, no duplicates and exactly one leader per group. A member can request a leader change with a reason, and each group can have only one PENDING request. The instructor rejects the request with a note, or approves it and confirms the new leader, which emits a leader-changed event.

### BF-07 Individual Attempt and Submission

**Trigger**: A learner starts an attempt on an open individual assignment.

**End condition**: The submission is recorded with a receipt, or the attempt is refused.

**Text alternative**: The learner starts an attempt. The backend checks enrollment, the submission window, the remaining attempts and that only one attempt is in progress. The attempt stores a snapshot, a deadline and an auto-submit job. The learner answers with autosave, and Code Lab runs public tests in Judge0. A manual submission is validated (DOCUMENT requires the full Draw.io XML) and invalid content loops back. When the time limit or deadline passes, or the assignment is retired, the worker auto-submits the latest saved content. The frozen submission gets a late flag, a receipt hash and a grading job. Simulation exams use the same flow within their attempt policy.

### BF-08 Group Document Collaboration and Submission

**Trigger**: A group assignment opens.

**End condition**: The shared document is submitted by the leader or automatically at the deadline.

**Text alternative**: When a group assignment opens, the worker creates a group document with sections from the outline and schedules auto-submission. Members claim an open section; a section already taken loops back. Each member edits the claimed section in a personal workspace and marks it Done. The backend validates the section, merges it into the shared document and pushes the update over SSE. A revision is recorded with its author. The leader submits the shared document before the deadline, or the worker auto-submits the current document when the deadline passes. The frozen submission keeps the section authors and creates a grading job.

### BF-09 Grading and Grade Publication

**Trigger**: A submission is recorded.

**End condition**: The instructor finalizes and publishes the grade, and the learner sees it.

**Text alternative**: Each submission gets a PENDING grade. Quiz and Code Lab are auto-scored by answer key or Judge0 tests and saved as DRAFT. The score is published immediately only if instant results are enabled. For other work the instructor grades manually with the rubric checklist, or requests an AI proposal that reserves the instructor's credit. A proposal is reference only; overriding it requires a reason, and an AI failure falls back to manual grading. A group document is always graded manually, and each member's final grade is entered without a formula. Finalized grades keep their history, and publishing makes grades and feedback visible to learners.

### BF-10 AI Credit Purchase

**Trigger**: A user buys an AI credit package.

**End condition**: Credits are added once after verified payment, or the payment fails with no credit.

**Text alternative**: The user picks an active credit package. The backend creates a payment with an order code, a price snapshot and an idempotency key. PayOS creates a 15-minute payment link, and if the link fails the payment is FAILED. The user pays by QR. PayOS sends a signed webhook. The backend verifies the signature, amount and result code, and stores invalid events as REJECTED without credit. A valid event is recorded once, sets the payment PAID and adds PURCHASE credits; duplicates are ignored. Every 10 minutes the worker checks PENDING payments that have no webhook and applies the same step when PayOS reports PAID. The return page never grants credit.

### BF-11 Notification and Deadline Reminder

**Trigger**: A business change is saved, or 24 hours remain before a deadline.

**End condition**: Recipients receive an in-app notification, and an email when enabled.

**Text alternative**: After a business change commits, the owning unit emits a notification event. Separately, a deadline reminder runs 24 hours before the deadline for learners who have not submitted, provided the assignment is still OPEN and the deadline has not changed. The worker resolves recipients within the class or group scope and inserts notifications, skipping duplicates. Every recipient gets the in-app notification over SSE. Email is queued only for email-enabled types the user has not opted out of. It is dispatched within the daily cap, deferred to the next day when the cap is reached, and sent by SMTP.

## Diagram conventions

- Each page shows one pool named after the business flow, with horizontal lanes for actors, the backend, the worker, external services and data stores.
- Lane colors: green for people, yellow for the backend, blue for the platform's own storage and worker (PostgreSQL, Redis, Google Drive, Worker), and purple for external services (Gemini, YouTube, Judge0, PayOS, SMTP).
- Every End shape states its business outcome, and every decision branch is labelled. A lane can hold a second Start when a scheduled job starts the same flow.
- Connectors have fixed exit and entry points and never pass through a shape or overlap another connector. Loop-backs run along the top or bottom edge of a lane.
- Terminology follows the current catalog: Learner, Instructor, Subject Manager, Administrator.
- PostgreSQL holds business state and job records, Redis holds OTP, sessions and download tokens with a TTL, and Google Drive holds file bytes.
