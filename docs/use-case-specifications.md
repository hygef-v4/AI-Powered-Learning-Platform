# II. Use Case Specifications — 8 Key Use Cases

These eight use cases were selected from the 40 active cases in [the use case table](use-case-table.md) because they represent distinctive learning and assessment workflows with significant business rules or failure paths. The numeric IDs and names below match that table. UC 30 Submit Assignment is specified once per assignment type because each type has its own editing and scoring rules. Same-actor management actions are grouped in the table; their detailed validation remains in the Functional Requirements and unit designs. The system being specified is not listed as a secondary actor. Secondary actors are external services that directly participate in a flow.

## 1. Content Management

### 1.1 UC 11 — Manage Content

| Field | Specification |
|---|---|
| Primary Actors | Subject Manager; Teacher |
| Secondary Actors | Google Drive; YouTube; AI Service |
| Description | The subject manager manages subject learning materials and the teacher manages class content. Each organizes lessons, uploads or updates content, monitors RAG processing, and publishes lesson versions within the assigned subject or class. |
| Preconditions | The user is signed in and is the subject manager of the subject or the assigned teacher of the class. An uploaded file, if any, uses a supported format and meets the size limit. |
| Normal Flow | 1. The user selects a subject or class, then a chapter and lesson. |
|  | 2. The system checks scope; the user creates or edits a DRAFT and adds text, file or YouTube items. |
|  | 3. For a new file, the system validates its type, stores it through Google Drive, and creates a PENDING source document. For a YouTube video or playlist, the system fetches existing captions without transcribing audio and keeps the transcript with its language and timestamps. |
|  | 4. A background job extracts text, splits it into chunks, requests embeddings from the AI service, and updates processing status. |
|  | 5. The user reviews the status and publishes a valid draft. The previously published version becomes SUPERSEDED. Published class content becomes visible to enrolled students. |
| Alternative Flows | **A1 — Editing a published lesson:** Create a new draft while students continue to see the current published version. |
|  | **A2 — Identical source already indexed:** Reuse its index without another AI call or credit charge. |
|  | **A3 — Insufficient text (including a YouTube video without captions), AI failure, or insufficient credits:** Show the appropriate NO_TEXT or FAILED status and do not index the source. The material may still be published, and the user can retry processing after resolving the issue. |
|  | **A4 — Draft with no items:** Reject publication. |
|  | **A5 — Teacher attempts to change subject materials:** Reject the change; a teacher manages only the content of an assigned class. |
| Postconditions | The content and version history are saved. Published content is visible within the permitted scope. A RAG failure does not erase the learning material. |

## 2. Group Assignment

### 2.1 UC 16 — Submit Group Document

| Field | Specification |
|---|---|
| Primary Actors | Student (group member; group leader) |
| Secondary Actors | None |
| Description | The group leader breaks the teacher's main sections into detailed sub-sections and assigns them to members. Members work on their sections in a private workspace and mark them Done so the completed content appears in the shared document in real time. When every section is Done, the document enters REVIEW for the whole group to read and comment on, and the leader then submits it; the system submits the current version when the deadline passes. |
| Preconditions | The student belongs to a group of the class (UC 9). The group assignment and its main sections were prepared under UC 28, and its publication still accepts work. |
| Normal Flow | 1. A group member opens the document; the system checks access and shows shared content, main sections and sub-sections, section states, assignees and the document state (IN_PROGRESS or REVIEW). |
|  | 2. The leader may add, rename, reorder or remove detailed sub-sections under a main section and assign a section to a member. The system locks an assigned section to that member. |
|  | 3. A member opens an assigned section or claims an OPEN or DONE section. The system conditionally claims it, locks it to that student, and copies its current published blocks into a private draft. |
|  | 4. The member edits only that section. The system autosaves the versioned draft, which other members cannot yet see. |
|  | 5. The member marks the section Done. The system validates the blocks, stores a revision and its author, publishes the completed blocks, releases the claim, and pushes the update to members currently viewing the document. |
|  | 6. When every section is Done, the system moves the document to REVIEW and notifies the members. Members read the whole document and comment on sections. |
|  | 7. The group leader requests submission. The system checks the leader role, the deadline and the REVIEW state, and warns about unresolved comments while allowing submission. |
|  | 8. The system snapshots the shared document and section authors into an immutable group submission, returns a receipt, and starts the grading workflow. |
| Alternative Flows | **A1 — Another member claims the section first:** Reject the competing claim and show its current state; do not overwrite the existing claim. |
|  | **A2 — Draft version conflict or invalid blocks:** Reject the save or Done action without replacing the last valid draft or published content. |
|  | **A3 — The member releases the claim, the leader reassigns the section, or the leader or teacher releases it:** Unlock the section (and lock it to the new assignee when reassigned); uncompleted private edits do not enter the shared document. |
|  | **A4 — A member other than the leader tries to add, edit, remove or assign sections:** Reject the change. The teacher's main sections cannot be removed or renamed by the group. |
|  | **A5 — A section must change during REVIEW:** A member reclaims the section or the leader reassigns it; the document returns to IN_PROGRESS and re-enters REVIEW only when every section is Done again. |
|  | **A6 — The member leaves the group:** Release the claim while retaining draft history, without publishing unfinished work. |
|  | **A7 — Connection drops:** Reload the full section and shared-document state after reconnection. |
|  | **A8 — Submission attempted by a non-leader, or before the document reaches REVIEW:** Reject submission and explain the reason; viewing access remains. |
|  | **A9 — Leader resubmits before the deadline:** The document must be back in REVIEW; keep both submissions, and the latest submission is the one to grade. |
|  | **A10 — Deadline passes or publication is retired:** Automatically submit the current document in any state, take only published content of claimed sections, and record warnings for unfinished sections or a document that was not reviewed. |
| Postconditions | Each completed section has a recorded revision and author. The group submission, receipt, and submission history are stored. The document becomes read-only after the final submission deadline. |

### 2.2 UC 17 — Grade Group Document

| Field | Specification |
|---|---|
| Primary Actors | Teacher |
| Secondary Actors | AI Service (only for an individual member's contribution, when requested) |
| Description | The teacher reviews the group's latest submitted document and each member's recorded contribution, evaluates the shared work, and assigns a final score to each student. |
| Preconditions | The teacher is assigned to the class. The group has a submitted document, section authorship history, and an applicable rubric. |
| Normal Flow | 1. The teacher opens the latest group submission and the contribution view for every member. |
|  | 2. The system shows the immutable shared document, section revisions and authors, and separate areas for shared-document, contribution, and member-final scores. |
|  | 3. The teacher grades the shared document manually against the rubric, including integration and consistency. |
|  | 4. The teacher reviews each member's authored sections and assesses individual contribution, manually or with an optional AI proposal for that contribution only (UC 33). |
|  | 5. The teacher enters each member's final score, records required reasons, and saves the grade decisions for finalization and publication (UC 34). |
| Alternative Flows | **A1 — Request to send the shared document to AI for grading:** Reject that path; the shared document is graded manually. |
|  | **A2 — Member's final score differs from the shared-document score:** Require a reason; do not calculate the final score from a fixed formula. |
|  | **A3 — Additional deduction for an integration problem attributed to one member:** Require a reason and a reference to the relevant section. |
|  | **A4 — AI proposal for a member fails:** Preserve the submission and current grades; the teacher may grade that contribution manually. |
| Postconditions | The shared-document assessment, individual contribution assessments, and per-member final draft scores are stored with the teacher's decisions. They are not visible to students until finalized and published. |

## 3. AI Creation

### 3.1 UC 21 — Create Assignment with AI

| Field | Specification |
|---|---|
| Primary Actors | Teacher |
| Secondary Actors | AI Service |
| Description | A teacher requests assignment suggestions grounded in published class content, then selects, edits, or discards the proposed questions. |
| Preconditions | The teacher manages the class and has a DRAFT assignment. Source materials are published and indexed within the class scope, and the teacher has sufficient AI credits. |
| Normal Flow | 1. The teacher selects the question type, quantity, difficulty, and optional chapter or lesson scope. |
|  | 2. The system checks permission, sources, AI limits, and reserves estimated credits from the teacher. |
|  | 3. A background job retrieves relevant learning-material passages, calls the AI service, and validates the proposed questions. |
|  | 4. The system displays valid proposals with source citations and settles credits against actual use. |
|  | 5. The teacher chooses and edits questions to keep; the system adds them to the DRAFT assignment. |
| Alternative Flows | **A1 — Source outside scope or not indexed:** Reject the request before calling AI and explain which source is unavailable. |
|  | **A2 — AI disabled, system limit reached, or insufficient credits:** Do not call AI; report the relevant condition without charging for a rejected call. |
|  | **A3 — AI error or invalid output:** Retry according to job policy; if still unsuccessful, mark the proposal FAILED and release unused reserved credits. |
|  | **A4 — Teacher discards the proposal:** Mark it DISCARDED and add nothing to the assignment. |
| Postconditions | Accepted questions remain draft content. AI does not approve or publish the assignment, and invalid proposals do not become official questions. |

## 4. Assignment Submission

### 4.1 UC 30 — Submit Assignment

The four specifications below share the primary actor and attempt rules of UC 30. A GRADED submission enters the teacher's grading queue (UC 32). A PRACTICE submission never creates a teacher grade record; its result is shown only to the student and stays outside the official gradebook.

#### 4.1.1 Text Essay

| Field | Specification |
|---|---|
| Primary Actors | Student |
| Secondary Actors | None |
| Description | The student writes an open-ended answer, the system autosaves the draft, and the student submits it. |
| Preconditions | The student has an ACTIVE enrollment in an OPEN class. The Text Essay publication accepts submissions (on time or within the late-submission period). The attempt limit has not been reached; starting an attempt consumes one attempt. |
| Normal Flow | 1. The student starts a new attempt or resumes the single IN_PROGRESS attempt. The system snapshots the assignment version and rubric and sets the attempt deadline to the closing time, or to the late-submission limit when late work is allowed. |
|  | 2. The student writes the response using basic formatting only: paragraphs, headings, lists, bold and italic. |
|  | 3. The system autosaves 10 seconds after the last edit and when the student leaves the page. Each save carries a content version, and the screen shows the last saved time. |
|  | 4. The student submits before the deadline. The system validates the attempt and content, locks the submitted answer, and issues a receipt showing the attempt ID, attempt number, server submission time, late flag and hash. Work submitted after the closing time but within the late-submission limit is marked LATE. |
|  | 5. For a GRADED assignment, in the same transaction the system creates a PENDING grade record so that the submission enters the teacher grading queue. For a PRACTICE assignment, no grade record is created and AI practice grading follows UC 40. |
| Alternative Flows | **A1 — No attempt remains or the publication is not accepting submissions:** Refuse to start a new attempt and explain the restriction (MSG07). |
|  | **A2 — Stale draft version (the attempt is open elsewhere):** Return a conflict instead of overwriting the newer saved answer; the student reloads before continuing (MSG05). |
|  | **A3 — Deadline passes or the publication is retired before manual submission:** Automatically submit the latest saved answer, including an empty answer if nothing was saved, and record any validation warning for the teacher. |
|  | **A4 — Content exceeds the size limit or uses unsupported formatting:** Reject the submission and keep the draft available for correction while the attempt remains open. |
|  | **A5 — Submission after the final deadline (the closing time when late work is not allowed, otherwise the late-submission limit):** Reject the submission (MSG06). A 30-second grace period absorbs network delay. |
| Postconditions | The submitted essay and receipt are immutable. A GRADED essay awaits the teacher's grading decision; a PRACTICE essay receives only practice feedback. Saving a draft alone does not submit it. For a GRADED assignment, the last submitted attempt is the one graded. |

#### 4.1.2 Multiple-Choice Quiz

| Field | Specification |
|---|---|
| Primary Actors | Student |
| Secondary Actors | None |
| Description | The student answers and submits a quiz of single-answer and multiple-answer questions; the system scores it against the answer key captured for the attempt. |
| Preconditions | The student has an ACTIVE enrollment in an OPEN class. The quiz publication accepts submissions. The attempt limit has not been reached; starting an attempt consumes one attempt. |
| Normal Flow | 1. The student starts a new attempt or resumes the IN_PROGRESS attempt. The system snapshots the quiz version and answer key and stores a per-attempt seed for question and option order. The attempt deadline is the earlier of the start time plus the time limit (if any) and the submission deadline. |
|  | 2. The student answers questions and reviews the current responses. A countdown is shown when the quiz has a time limit. |
|  | 3. The system autosaves valid responses with a content version and shows the last saved time. |
|  | 4. The student submits before the deadline. The system locks the answers and issues a receipt showing the attempt ID, attempt number, submission time, late flag and hash. |
|  | 5. A grading job created in the submission transaction scores each question. A single-answer question earns full points when correct; a multiple-answer question earns points only when all correct options and no wrong options are selected. |
|  | 6. For a GRADED quiz, the score is published immediately if "show score after submit" is enabled; otherwise it awaits teacher finalization and publication. For a PRACTICE quiz, the score is shown only to the student as practice feedback. Correct answers are shown according to the NEVER, AFTER_SUBMIT or AFTER_CLOSE setting. |
| Alternative Flows | **A1 — Invalid question or option reference:** Reject that draft update without replacing the last valid saved answers. |
|  | **A2 — Stale draft version:** Return a conflict and require the student to reload instead of silently overwriting answers (MSG05). |
|  | **A3 — Time limit, deadline, or publication retirement occurs before manual submission:** Automatically submit the latest saved answers, even if incomplete or empty. |
|  | **A4 — Grading job is delayed or fails:** Preserve the submission and receipt; keep the score pending until grading succeeds, without inventing a result. |
|  | **A5 — No attempt remains, or submission after the final deadline (including the 30-second grace period):** Reject the request (MSG07 / MSG06). |
| Postconditions | The submitted answers and receipt are immutable. The score is calculated against that attempt's answer-key version, and its visibility follows the quiz settings. For a GRADED quiz, the teacher may change an automatic score with a recorded reason. |

#### 4.1.3 Diagram Essay

| Field | Specification |
|---|---|
| Primary Actors | Student |
| Secondary Actors | None |
| Description | The student edits a Diagram Essay document, may preview and import a DOCX file, uses the embedded Draw.io canvas, and submits the complete document. |
| Preconditions | The student has an ACTIVE enrollment. The Diagram Essay publication accepts submissions, and the student has an available attempt that has not reached its deadline. |
| Normal Flow | 1. The student starts an attempt; the system snapshots the assignment, document outline, and deadline. |
|  | 2. The student edits permitted blocks while preserving the teacher's locked blocks. |
|  | 3. The student creates or edits diagrams in the embedded Draw.io canvas; the system retains the full XML. |
|  | 4. The system autosaves the draft with a content version and shows the last saved time. |
|  | 5. The student submits. The system validates the document, diagrams, and deadline, makes the submission immutable, and returns a receipt. A GRADED submission enters the teacher grading queue; a PRACTICE submission follows UC 40. |
| Alternative Flows | **A1 — DOCX import:** Show a preview and unsupported-content report. Add imported student blocks only after confirmation; do not replace teacher blocks. |
|  | **A2 — Invalid diagram XML or missing required diagrams:** Reject manual submission and keep the draft available for correction. |
|  | **A3 — Stale content version:** Return conflict 409 instead of silently overwriting a newer draft. |
|  | **A4 — Deadline reached while editing:** Automatically submit the latest saved content, even if it does not meet manual-submission checks, and record a warning for the teacher. |
| Postconditions | The submitted attempt contains the document and full Draw.io XML, an immutable receipt, and a preserved history. An autosaved draft alone is not a submission. |

#### 4.1.4 Code Lab

| Field | Specification |
|---|---|
| Primary Actors | Student |
| Secondary Actors | Code Sandbox |
| Description | The student writes code, runs public tests, and submits source code. The submitted version is graded in an isolated sandbox. |
| Preconditions | The student may access the class. The Code Lab publication accepts submissions, and an attempt remains available and within its deadline. |
| Normal Flow | 1. The student starts an attempt; the system snapshots the task, language, limits, and deadline. |
|  | 2. The student writes code and the system autosaves the versioned source. |
|  | 3. The student may run the code against public tests in the Code Sandbox under resource limits. |
|  | 4. The student submits; the system locks the source, issues a receipt, and queues grading. |
|  | 5. The Code Sandbox runs all tests for the submitted version. The system records the deterministic result for teacher review (GRADED) or shows it to the student as practice feedback (PRACTICE). |
| Alternative Flows | **A1 — More than five trial runs per minute:** Reject the extra trial without losing the source draft. |
|  | **A2 — Trial tests fail:** Show permitted results; never disclose hidden-test inputs or outputs. |
|  | **A3 — Sandbox failure during grading:** Preserve the submission, show that grading is pending or unavailable, and retry by policy; do not invent a score. |
|  | **A4 — Deadline reached before manual submission:** Automatically submit the latest saved source. |
| Postconditions | Submitted source and its receipt are immutable. A trial run is not a submission; an automatic score exists only after successful grading. |

## 5. Grading

### 5.1 UC 33 — Grade Submissions

| Field | Specification |
|---|---|
| Primary Actors | Teacher |
| Secondary Actors | AI Service (only when AI assistance is requested) |
| Description | The teacher grades an individual GRADED submission, or a group member's contribution, manually or with an AI grading proposal, and records the draft score and feedback. |
| Preconditions | The teacher is assigned to the class. A GRADED submission and suitable content or rubric are available. For AI assistance, AI is enabled and the teacher has sufficient credits. |
| Normal Flow | 1. The teacher opens a submission from the grading queue (UC 32) and reviews the work, rubric, attempts and any auto-scored result. |
|  | 2. The teacher chooses manual grading or AI assistance. For manual grading, the teacher enters criterion scores and feedback against the rubric without calling AI and continues at step 6. |
|  | 3. For AI assistance, the system checks authorization and AI limits, reserves the teacher's credits, and sends only the relevant submission or contribution and rubric. |
|  | 4. The AI service proposes outcomes for rubric criteria; the system validates the response and calculates the proposed total from the rubric rather than relying on AI arithmetic. |
|  | 5. The teacher reviews evidence and comments, then accepts or overrides the proposal. |
|  | 6. The system records the teacher's draft score, feedback, grading method, actor and time. Finalization and publication occur separately (UC 34). |
| Alternative Flows | **A1 — AI unavailable or invalid response:** Keep the submission unchanged; the teacher may retry or grade manually. |
|  | **A2 — Insufficient credits or a usage limit:** Do not call AI; manual grading remains available. |
|  | **A3 — Teacher disagrees with the AI proposal:** Enter a different score or feedback; a reason is required when finalizing a score that differs from the proposal. |
|  | **A4 — Shared group document:** Do not send the shared document for AI grading; only an individual member's contribution may receive a proposal (UC 17). |
|  | **A5 — Teacher changes an automatic quiz or Code Lab score:** Require a reason and record the actor and time. |
| Postconditions | The teacher controls a draft score. Students cannot see an AI proposal or an unpublished grade. |

## 6. Payment

### 6.1 UC 37 — Buy AI Credits

| Field | Specification |
|---|---|
| Primary Actors | Student, Teacher, Subject Manager, Administrator |
| Secondary Actors | PayOS Payment Gateway |
| Description | An authorized Student, Teacher, Subject Manager or Administrator selects a credit package, pays through PayOS, and receives purchased AI credits exactly once after verification. Student may spend credits only on Practice Text/Diagram Essay grading. |
| Preconditions | The buyer is signed in with an ACTIVE account and one of the four current roles. The package is active, and the account has fewer than three PENDING payments. |
| Normal Flow | 1. The user selects a package and starts checkout; the request carries an Idempotency-Key. |
|  | 2. The system creates a CREATED payment with a snapshot of the package price and credits, and requests a PayOS checkout link valid for 15 minutes. When the link is created, the payment becomes PENDING and the user is redirected to PayOS. |
|  | 3. The user pays on PayOS. On return, the result page polls the payment status every 3 seconds for up to 2 minutes; the return page never grants credits. |
|  | 4. The system receives the PayOS webhook and verifies the HMAC-SHA256 signature, order code, amount and successful result. |
|  | 5. In one database transaction, the system marks the payment PAID, writes one PURCHASE ledger entry, adds the credits to the purchased balance and records an audit event. After commit, an in-app notification is sent. The user can view the updated balance and history. |
| Alternative Flows | **A0 — Student attempts an unauthorized AI task:** Allow wallet and checkout access, but reject AI authoring, learning-material processing and GRADED submission grading before reserving credits or calling AI. Student credits are used only for Practice Text/Diagram Essay grading of their own attempt (UC 40). |
|  | **A1 — PayOS cannot create a link:** Mark the payment FAILED and grant no credits; the user may start a new checkout. |
|  | **A2 — User returns before verification:** Keep the payment PENDING and grant no credits yet (MSG11). |
|  | **A3 — Invalid signature, or mismatched order code or amount:** Record the event as REJECTED, write a security audit event, and grant no credits (MSG12). |
|  | **A4 — Duplicate webhook:** Record it as DUPLICATE and return success to PayOS without granting extra credits. |
|  | **A5 — Missing webhook:** A reconciliation job runs every 10 minutes, checks PENDING payments older than 5 minutes and payments EXPIRED within the last 24 hours with PayOS, and applies the same exactly-once purchase rule. If PayOS does not respond, the status is kept and checked again on the next run. |
|  | **A6 — User cancels on PayOS:** The system confirms with PayOS and marks the payment CANCELLED if it has not been paid. |
|  | **A7 — Link not paid within 15 minutes:** Mark the payment EXPIRED. |
|  | **A8 — Valid payment arrives after expiration or cancellation:** Mark it PAID and grant the credits because the money was received. |
|  | **A9 — Same Idempotency-Key resubmitted, or three payments already PENDING:** Return the existing PENDING payment for the same key; refuse to create a fourth concurrent PENDING payment. |
| Postconditions | A verified payment creates exactly one purchase ledger entry and increases the balance. Purchased credits do not expire. Unverified, failed or rejected payments do not increase the balance. |

## 7. Practice Feedback

### 7.1 UC 40 — Grade Practice with AI

| Field | Specification |
|---|---|
| Primary Actors | Student |
| Secondary Actors | AI Service |
| Description | A student submits a PRACTICE Text Essay or Diagram Essay (UC 30). If enough credits are available, the system grades that attempt once with AI and shows private practice feedback. The teacher does not grade or publish an official score for PRACTICE. |
| Preconditions | The student has an ACTIVE account and enrollment, owns the attempt, the assignment is PRACTICE and of a supported type, and the publication accepts submission. |
| Normal Flow | 1. The student submits the attempt; the system locks its content and issues a receipt. |
|  | 2. The system checks the student's credit balance, AI limits, supported type and attempt ownership. If eligible, it reserves estimated credits with an idempotent attempt reference and queues one AI grading job. |
|  | 3. AI evaluates the submitted snapshot against the rubric; the system validates the response, calculates the score and settles actual credit usage. |
|  | 4. Only the student sees the practice score and feedback in that attempt's history. The result is excluded from the official gradebook, dashboard grade distribution and exports. |
| Alternative Flows | **A1 — Insufficient credits:** Keep the submitted attempt without an AI score or feedback and charge nothing. Buying credits later does not grade that attempt; a new submission is needed. |
|  | **A2 — AI unavailable or invalid output:** Keep the submission, release unused reserved credits and show that no valid AI score is available. Technical retries use the same request reference and cannot duplicate a score or debit. |
|  | **A3 — Unsupported type or GRADED mode:** Do not queue AI grading and charge nothing. Practice Quiz and Code Lab use answer keys or tests instead; Group Assignment has no PRACTICE mode. |
| Postconditions | At most one valid AI result and one settled debit exist per submitted attempt. A submission without enough credits remains ungraded by AI, and no PRACTICE result becomes an official grade. |

## Sources

- [Use case IDs, names, actors, features, and descriptions](use-case-table.md)
- Unit business designs: [U05](../aidlc-docs/construction/u05-content-material-rag/functional-design/business-logic-model.md), [U07](../aidlc-docs/construction/u07-payment-credit/functional-design/business-logic-model.md), [U08](../aidlc-docs/construction/u08-assessment-core-publication/functional-design/business-logic-model.md), [U09](../aidlc-docs/construction/u09-question-type-authoring/functional-design/business-logic-model.md), [U11](../aidlc-docs/construction/u11-attempt-submission/functional-design/business-logic-model.md), [U13](../aidlc-docs/construction/u13-ai-code-execution/functional-design/business-logic-model.md), [U14](../aidlc-docs/construction/u14-group-document-submission/functional-design/business-logic-model.md), [U15](../aidlc-docs/construction/u15-grading/functional-design/business-logic-model.md)
