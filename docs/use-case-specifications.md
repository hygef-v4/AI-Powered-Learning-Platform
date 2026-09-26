# II. Use Case Specifications — 11 Key Use Cases

These eleven use cases were selected from the 77 cases in [the use case table](use-case-table.md) because they represent distinctive learning and assessment workflows with significant business rules or failure paths. The numeric IDs and names below match that table exactly. Simpler CRUD and data-viewing cases remain in the table and should be covered by the report's Functional Requirements section. The system being specified is not listed as a secondary actor. Secondary actors are external services that directly participate in a flow.

## 1. Subject Content

### 1.1 UC 26 — Manage Subject Materials and RAG

| Field | Specification |
|---|---|
| Primary Actors | Subject Manager |
| Secondary Actors | Google Drive; AI Service |
| Description | The subject manager organizes subject learning materials, uploads or updates content, monitors RAG processing, and publishes lesson versions within the assigned subject. |
| Preconditions | The subject manager is signed in and assigned to the subject. An uploaded file, if any, uses a supported format and meets the size limit. |
| Normal Flow | 1. The subject manager selects a subject, chapter, and lesson. |
|  | 2. The system checks scope; the manager creates or edits a DRAFT and adds text or file items. |
|  | 3. For a new file, the system validates its type, stores it through Google Drive, and creates a PENDING source document. |
|  | 4. A background job extracts text, splits it into chunks, requests embeddings from the AI service, and updates processing status. |
|  | 5. The manager reviews the status and publishes a valid draft. The previously published version becomes SUPERSEDED. |
| Alternative Flows | **A1 — Editing a published lesson:** Create a new draft while learners continue to see the current published version. |
|  | **A2 — Identical source already indexed:** Reuse its index without another AI call or credit charge. |
|  | **A3 — Insufficient text, AI failure, or insufficient credits:** Show the appropriate NO_TEXT or FAILED status. The material may still be published, and the manager can retry processing after resolving the issue. |
|  | **A4 — Draft with no items:** Reject publication. |
| Postconditions | The content and version history are saved. Published content is visible within the permitted scope. A RAG failure does not erase the learning material. |

## 2. AI Authoring

### 2.1 UC 45 — Generate and Review Class Assignment Draft with AI

| Field | Specification |
|---|---|
| Primary Actors | Instructor |
| Secondary Actors | AI Service |
| Description | An instructor requests assignment suggestions grounded in published class content, then selects, edits, or discards the proposed questions. |
| Preconditions | The instructor manages the class and has a DRAFT assignment. Source materials are published and indexed within the class scope, and the instructor has sufficient AI credits. |
| Normal Flow | 1. The instructor selects the question type, quantity, difficulty, and optional chapter or lesson scope. |
|  | 2. The system checks permission, sources, AI limits, and reserves estimated credits from the instructor. |
|  | 3. A background job retrieves relevant learning-material passages, calls the AI service, and validates the proposed questions. |
|  | 4. The system displays valid proposals with source citations and settles credits against actual use. |
|  | 5. The instructor chooses and edits questions to keep; the system adds them to the DRAFT assignment. |
| Alternative Flows | **A1 — Source outside scope or not indexed:** Reject the request before calling AI and explain which source is unavailable. |
|  | **A2 — AI disabled, system limit reached, or insufficient credits:** Do not call AI; report the relevant condition without charging for a rejected call. |
|  | **A3 — AI error or invalid output:** Retry according to job policy; if still unsuccessful, mark the proposal FAILED and release unused reserved credits. |
|  | **A4 — Instructor discards the proposal:** Mark it DISCARDED and add nothing to the assignment. |
| Postconditions | Accepted questions remain draft content. AI does not approve or publish the assignment, and invalid proposals do not become official questions. |

## 3. Assignment Workspace

### 3.1 UC 56 — Complete and Submit Essay

| Field | Specification |
|---|---|
| Primary Actors | Learner |
| Secondary Actors | None |
| Description | The learner writes an open-ended answer, saves a draft, and submits it for instructor grading. |
| Preconditions | The learner has an ACTIVE enrollment, the class is OPEN, the ESSAY publication accepts submissions, and an attempt is available. |
| Normal Flow | 1. The learner starts or resumes an attempt; the system records the assignment version and deadline. |
|  | 2. The learner writes the essay response. |
|  | 3. The system autosaves the response with a content version and shows the last saved time. |
|  | 4. The learner submits before the attempt deadline. The system validates the attempt and content, locks the submitted answer, and issues a receipt. |
|  | 5. The submission enters the instructor grading workflow. |
| Alternative Flows | **A1 — No available attempt or publication not accepting submissions:** Refuse to start a new attempt and explain the restriction. |
|  | **A2 — Stale draft version:** Return a conflict instead of overwriting the newer saved answer; the learner reloads before continuing. |
|  | **A3 — Deadline, time limit, or publication retirement occurs before manual submission:** Automatically submit the latest saved answer, including an empty answer if nothing was saved, and record any validation warning for the instructor. |
|  | **A4 — Manual submission fails validation:** Keep the draft available for correction while the attempt remains open. |
| Postconditions | The submitted essay and receipt are immutable. The answer awaits an instructor's grading decision; saving a draft alone does not submit it. |

### 3.2 UC 57 — Complete and Submit Quiz

| Field | Specification |
|---|---|
| Primary Actors | Learner |
| Secondary Actors | None |
| Description | The learner answers and submits a quiz; the system scores closed questions against the answer-key version captured for the attempt. |
| Preconditions | The learner has an ACTIVE enrollment, the class is OPEN, the QUIZ publication accepts submissions, and an attempt is available. |
| Normal Flow | 1. The learner starts or resumes an attempt; the system records the quiz version, question order, answer-key version, and deadline. |
|  | 2. The learner answers questions and reviews the current responses. |
|  | 3. The system autosaves valid responses with a content version and shows the last saved time. |
|  | 4. The learner submits before the attempt deadline. The system locks the answers and issues a receipt. |
|  | 5. A grading job scores closed questions against the captured answer-key version. The system shows the score and correct answers only as permitted by the publication settings. |
| Alternative Flows | **A1 — Invalid question or option reference:** Reject that draft update without replacing the last valid saved answers. |
|  | **A2 — Stale draft version:** Return a conflict and require the learner to reload instead of silently overwriting answers. |
|  | **A3 — Deadline, time limit, or publication retirement occurs before manual submission:** Automatically submit the latest saved answers, even if incomplete or empty. |
|  | **A4 — Grading job is delayed or fails:** Preserve the submission and receipt; keep the score pending until grading succeeds, without inventing a result. |
| Postconditions | The submitted answers and receipt are immutable. Closed-question results are calculated for that attempt's answer-key version, and their visibility follows the publication settings. |

## 4. Diagram Assignment

### 4.1 UC 58 — Complete and Submit DOCUMENT Assignment

| Field | Specification |
|---|---|
| Primary Actors | Learner |
| Secondary Actors | None |
| Description | The learner edits a DOCUMENT assignment, may preview and import a DOCX file, uses the embedded Draw.io canvas, and submits the complete document. |
| Preconditions | The learner has an ACTIVE enrollment. The DOCUMENT publication accepts submissions, and the learner has an available attempt that has not reached its deadline. |
| Normal Flow | 1. The learner starts an attempt; the system snapshots the assignment, document outline, and deadline. |
|  | 2. The learner edits permitted blocks while preserving the instructor's locked blocks. |
|  | 3. The learner creates or edits diagrams in the embedded Draw.io canvas; the system retains the full XML. |
|  | 4. The system autosaves the draft with a content version and shows the last saved time. |
|  | 5. The learner submits. The system validates the document, diagrams, and deadline, makes the submission immutable, and returns a receipt. |
| Alternative Flows | **A1 — DOCX import:** Show a preview and unsupported-content report. Add imported learner blocks only after confirmation; do not replace instructor blocks. |
|  | **A2 — Invalid diagram XML or missing required diagrams:** Reject manual submission and keep the draft available for correction. |
|  | **A3 — Stale content version:** Return conflict 409 instead of silently overwriting a newer draft. |
|  | **A4 — Deadline reached while editing:** Automatically submit the latest saved content, even if it does not meet manual-submission checks, and record a warning for the instructor. |
| Postconditions | The submitted attempt contains the document and full Draw.io XML, an immutable receipt, and a preserved history. An autosaved draft alone is not a submission. |

## 5. Code Assignment

### 5.1 UC 59 — Complete and Submit Code Lab

| Field | Specification |
|---|---|
| Primary Actors | Learner |
| Secondary Actors | Code Sandbox |
| Description | The learner writes code, runs public tests, and submits source code. The submitted version is graded in an isolated sandbox. |
| Preconditions | The learner may access the class. The CODE_LAB publication accepts submissions, and an attempt remains available and within its deadline. |
| Normal Flow | 1. The learner starts an attempt; the system snapshots the task, language, limits, and deadline. |
|  | 2. The learner writes code and the system autosaves the versioned source. |
|  | 3. The learner may run the code against public tests in the Code Sandbox under resource limits. |
|  | 4. The learner submits; the system locks the source, issues a receipt, and queues grading. |
|  | 5. The Code Sandbox runs all tests for the submitted version; the system records the deterministic result for instructor review. |
| Alternative Flows | **A1 — More than five trial runs per minute:** Reject the extra trial without losing the source draft. |
|  | **A2 — Trial tests fail:** Show permitted results; never disclose hidden-test inputs or outputs. |
|  | **A3 — Sandbox failure during grading:** Preserve the submission, show that grading is pending or unavailable, and retry by policy; do not invent a score. |
|  | **A4 — Deadline reached before manual submission:** Automatically submit the latest saved source. |
| Postconditions | Submitted source and its receipt are immutable. A trial run is not a submission; an automatic score exists only after successful grading. |

## 6. Group Submission

### 6.1 UC 38 — Claim and Complete Group Document Section

| Field | Specification |
|---|---|
| Primary Actors | Learner |
| Secondary Actors | None |
| Description | A group member claims a section, works on it in a private workspace, and marks it Done so the completed content appears in the shared document in real time. |
| Preconditions | The learner belongs to the group. Its publication still accepts work, the group document exists, and the selected section is OPEN or IN_REVIEW. |
| Normal Flow | 1. The learner opens the group document and selects an available section. |
|  | 2. The system conditionally claims the section, locks it to that learner, and copies its current published blocks into a private draft. |
|  | 3. The learner edits only that section. The system autosaves the versioned draft, which other members cannot yet see. |
|  | 4. The learner marks the section Done. The system validates the blocks, stores a revision and its author, publishes the completed blocks, and releases the claim. |
|  | 5. The system pushes the section update to members currently viewing the shared document. |
| Alternative Flows | **A1 — Another member claims the section first:** Reject the competing claim and show its current state; do not overwrite the existing claim. |
|  | **A2 — Draft version conflict or invalid blocks:** Reject the save or Done action without replacing the last valid draft or published content. |
|  | **A3 — Learner releases the claim, or the leader or instructor releases it:** Unlock the section; uncompleted private edits do not enter the shared document. |
|  | **A4 — Learner leaves the group:** Release the claim while retaining draft history, without publishing unfinished work. |
|  | **A5 — Connection drops:** Reload the full section and shared-document state after reconnection. |
| Postconditions | The section is IN_REVIEW with a recorded revision and author. Only completed published blocks are visible in the shared document. |

### 6.2 UC 39 — View and Submit Group Document

| Field | Specification |
|---|---|
| Primary Actors | Learner (group leader); Instructor |
| Secondary Actors | None |
| Description | Group members view the shared document as completed sections appear. The leader submits its current version, or the system submits it when the deadline passes. |
| Preconditions | The user is an authorized group member or the class instructor. The group document exists and its publication is accepting submissions. |
| Normal Flow | 1. A group member opens the document; the system checks access and shows shared content, completed sections, section states, and assignees. |
|  | 2. When a member marks a section Done, the system merges its published content and pushes an update to current viewers. |
|  | 3. The group leader reviews the current document and requests submission. |
|  | 4. The system checks the leader role and deadline, warning about unfinished sections while allowing submission. |
|  | 5. The system snapshots the shared document and section authors into an immutable group submission, returns a receipt, and starts the grading workflow. |
| Alternative Flows | **A1 — A member other than the leader attempts submission:** Reject submission while retaining their viewing access. |
|  | **A2 — A section is claimed but not marked Done:** Include only its last published content, never the owner's private draft. |
|  | **A3 — Leader resubmits before the deadline:** Keep both submissions; the latest submission is the one to grade. |
|  | **A4 — Deadline passes or publication is retired:** Automatically submit the current document and record warnings for unfinished sections. |
| Postconditions | The group submission, receipt, and submission history are stored. The document becomes read-only after the final submission deadline. |

## 7. Group Grading

### 7.1 UC 40 — Review and Grade Group Document

| Field | Specification |
|---|---|
| Primary Actors | Instructor |
| Secondary Actors | AI Service (only for an individual member's contribution, when requested) |
| Description | The instructor reviews the group's latest submitted document and each member's recorded contribution, evaluates the shared work, and assigns a final score to each learner. |
| Preconditions | The instructor is assigned to the class. The group has a submitted document, section authorship history, and an applicable rubric. |
| Normal Flow | 1. The instructor opens the latest group submission and the contribution view for every member. |
|  | 2. The system shows the immutable shared document, section revisions and authors, and separate areas for shared-document, contribution, and member-final scores. |
|  | 3. The instructor grades the shared document manually against the rubric, including integration and consistency. |
|  | 4. The instructor reviews each member's authored sections and assesses individual contribution, manually or with an optional AI proposal for that contribution only. |
|  | 5. The instructor enters each member's final score, records required reasons, and saves the grade decisions for finalization and publication. |
| Alternative Flows | **A1 — Request to send the shared document to AI for grading:** Reject that path; the shared document is graded manually. |
|  | **A2 — Member's final score differs from the shared-document score:** Require a reason; do not calculate the final score from a fixed formula. |
|  | **A3 — Additional deduction for an integration problem attributed to one member:** Require a reason and a reference to the relevant section. |
|  | **A4 — AI proposal for a member fails:** Preserve the submission and current grades; the instructor may grade that contribution manually. |
| Postconditions | The shared-document assessment, individual contribution assessments, and per-member final draft scores are stored with the instructor's decisions. They are not visible to learners until finalized and published. |

## 8. AI-Assisted Grading

### 8.1 UC 67 — Grade with AI Assistance

| Field | Specification |
|---|---|
| Primary Actors | Instructor |
| Secondary Actors | AI Service |
| Description | The instructor requests an AI grading proposal for an individual submission or a group member's contribution, then makes the grading decision. |
| Preconditions | The instructor is assigned to the class. A submission and suitable content or rubric are available; AI is enabled and the instructor has sufficient credits. |
| Normal Flow | 1. The instructor opens a submission and selects AI assistance. |
|  | 2. The system checks authorization and AI limits, reserves the instructor's credits, and sends only the relevant submission or contribution and rubric. |
|  | 3. The AI service proposes outcomes for rubric criteria; the system validates the response and calculates the proposed total from the rubric rather than relying on AI arithmetic. |
|  | 4. The instructor reviews evidence and comments, then accepts or overrides the proposal. |
|  | 5. The system records the instructor's draft score and decision. Finalization and publication occur separately. |
| Alternative Flows | **A1 — AI unavailable or invalid response:** Keep the submission unchanged; the instructor may retry or grade manually. |
|  | **A2 — Insufficient credits or a usage limit:** Do not call AI; manual grading remains available. |
|  | **A3 — Instructor disagrees:** Enter a different score or feedback; a reason is required when finalizing a score that differs from the proposal. |
|  | **A4 — Shared group document:** Do not send the shared document for AI grading; only an individual member's contribution may receive a proposal. |
| Postconditions | The instructor controls a draft score. Learners cannot see the AI proposal or an unpublished grade. |

## 9. Payment

### 9.1 UC 75 — Buy AI Credits

| Field | Specification |
|---|---|
| Primary Actors | User |
| Secondary Actors | PayOS Payment Gateway |
| Description | A user selects a credit package, pays through PayOS, and receives AI credits exactly once after payment verification. |
| Preconditions | The user is signed in with an ACTIVE account. The package is active, and the account has fewer than three simultaneous PENDING payments. |
| Normal Flow | 1. The user selects a package and starts checkout. |
|  | 2. The system creates a payment with a snapshot of its price and credits, then asks PayOS for a checkout link. |
|  | 3. The user pays through PayOS. The return page displays verification status rather than granting credits. |
|  | 4. The system receives a valid webhook or obtains the status through automatic reconciliation, checking the order code, amount, and successful payment result. |
|  | 5. In one database transaction, the system marks the payment PAID, writes one PURCHASE ledger entry, and adds the credits. The user can view the updated balance and history. |
| Alternative Flows | **A1 — PayOS cannot create a link:** Mark the payment FAILED, grant no credits, and allow a new checkout. |
|  | **A2 — User returns before verification:** Keep the payment PENDING and grant no credits yet. |
|  | **A3 — Invalid signature, mismatched amount, or duplicate webhook:** Reject or record the duplicate without granting extra credits. |
|  | **A4 — Missing webhook:** Reconcile automatically with PayOS and apply the same exactly-once purchase rule. |
|  | **A5 — Valid payment arrives after link expiration or cancellation:** Mark it PAID and grant the credits because payment was received. |
| Postconditions | A verified payment creates exactly one purchase ledger entry and increases the balance. Unverified, failed, or rejected payments do not increase it. |

## Sources

- [Use case IDs, names, features, and descriptions](use-case-table.md)
- Unit business designs: [U05](../aidlc-docs/construction/u05-content-material-rag/functional-design/business-logic-model.md), [U07](../aidlc-docs/construction/u07-payment-credit/functional-design/business-logic-model.md), [U08](../aidlc-docs/construction/u08-assessment-core-publication/functional-design/business-logic-model.md), [U09](../aidlc-docs/construction/u09-question-type-authoring/functional-design/business-logic-model.md), [U11](../aidlc-docs/construction/u11-attempt-submission/functional-design/business-logic-model.md), [U13](../aidlc-docs/construction/u13-ai-code-execution/functional-design/business-logic-model.md), [U14](../aidlc-docs/construction/u14-group-document-submission/functional-design/business-logic-model.md), [U15](../aidlc-docs/construction/u15-grading/functional-design/business-logic-model.md)
