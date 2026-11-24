# MCP capability review across all roles

Date: 2026-10-26. Scope: source review and recommendations requested by the user; no implementation or authenticated testing of additional roles.

## Review checklist

- [x] Identify every account role and existing MCP admission rules.
- [x] Inventory registered MCP tools and role visibility.
- [x] Trace existing provider-backed AI workflows and their domain permissions.
- [x] Identify missing discovery, authoring, grading and student workflows.
- [x] Separate extensions of existing AI features from new AI use cases.
- [x] Recommend a delivery order and domain constraints.

Existing working-tree changes were preserved. The earlier Teacher connection check demonstrated seven tools at runtime. Student and Subject Manager tool counts below are derived from source definitions and registry filtering, not new runtime sessions.

## Current capability and permission matrix

The system has STUDENT, TEACHER, SUBJECT_MANAGER and ADMIN. Across the five domain MCP providers there are nine distinct tools. Visibility depends on role and token READ_ONLY/READ_WRITE; each invocation also needs resource authorization.

| Role | READ_WRITE token maximum | Current domain capabilities |
|---|---:|---|
| Student | 4 tools; 3 with READ_ONLY | Assignment metadata; authorized learning material; read own submitted Practice work; record private Practice feedback. |
| Teacher | 7 tools; 4 with READ_ONLY | Assignment context/proposals, quiz context/question proposals, authorized material, individual submission read, grading proposal write. |
| Subject Manager | 7 tools; 4 with READ_ONLY | Same tool names as Teacher, with subject authoring rights for managed subjects and class actions only where assigned to teach. Managing a subject does not grant grading rights for every class in it. |
| Admin | No MCP admission | Institutional AI provider configuration, quotas, usage summaries and dashboard exist in web services, but ADMIN is excluded from MCP credential authentication. |

Current tools:

| Tool | Student | Teacher | Subject Manager |
|---|---|---|---|
| read_assignment_context | Metadata and schedule only | Authorized authoring context | Authorized authoring context |
| save_assignment_draft | No | Proposal for existing assignment | Proposal for existing assignment |
| read_authorized_material | Authorized material | Authorized material | Authorized material |
| read_own_work | Own submitted Practice attempt | No | No |
| record_private_feedback | Own submitted Practice attempt | No | No |
| read_authorized_submission | No | Authorized individual submitted attempt | Authorized individual submitted attempt |
| record_grading_proposal | No | Authorized evaluation | Authorized evaluation |
| read_quiz_context | No | Editable quiz | Editable quiz |
| save_quiz_draft | No | Question proposal for existing draft quiz | Question proposal for existing draft quiz |

Student Practice feedback is limited to submitted Practice attempts of the requesting student while eligible/enrolled. It is not an existing tool for reading an active graded exam. Current student assignment context intentionally excludes assignment body and rubric. Admin is not an academic superuser under current authorization.

## Existing AI features and MCP gaps

There are four implemented provider-backed task families: ASSIGNMENT_DRAFT, QUIZ_QUESTION_DRAFT, GRADING_PROPOSAL and PRACTICE_FEEDBACK. Quiz question generation supports both quiz and subject question-bank targets, so it covers two user workflows.

| Existing AI workflow | Roles and scope | MCP coverage | Missing capability |
|---|---|---|---|
| Generate assignment instructions, sections and rubric | Teacher for assigned class; Subject Manager for managed subject or assigned class | Save proposal into an existing assignment | Create the assignment itself; discover scopes/materials; inspect generation constraints. |
| Generate quiz questions and explanations | Teacher for assigned class; Subject Manager for managed subject or assigned class | Save question proposal into an existing draft quiz | Create the quiz itself; discover quizzes/materials; prepare settings and question requirements. |
| Generate question-bank questions | Subject Manager for managed subject | No bank-target MCP tool | Read bank context, search bank items, save a bank question proposal. Teacher bank access must follow existing quiz-authoring permissions, not a new unrestricted bank-management tool. |
| Generate grading proposals, including group document root evaluations | Teacher/Subject Manager assigned to the class | One proposal write; individual submission text read | Evaluation/submission discovery; complete grading context; group document reads; batch context/proposal writes. Provider-backed batch proposal requests already exist in GradingProposalService, but are not MCP batch tools. |
| Private feedback for submitted Practice work | Student, own eligible attempt | Existing read/write pair | Discover own attempts, inspect feedback history and remaining eligibility/quota. |

### Important grading contract gap

AssignmentWorkMcpTools.submission returns attemptId, assignmentId, title, attemptNo, submittedAt, late and submission text. It does not return evaluation version, submissionHash, classId, rubric snapshot, structured blocks or proposal output schema. GradingMcpTools requires submissionRef, submissionHash, expectedVersion, requestRef and a validated proposal. Reading an assignment separately does not provide evaluation version or the locked grading snapshot.

This is a source-level gap in completing grading entirely through current MCP tools; no failed runtime grading attempt was performed in this review. The write tool recognizes ATTEMPT and GROUP_DOCUMENT, while the submission read tool accepts only attemptId. Add a complete grading-context read contract before treating bulk grading as ready.

## Proposed additions

All names below are proposals, not currently registered tools. Prefer extending existing application services and authorization checks rather than making a separate MCP business implementation.

### Shared discovery and constraints

- get_my_capabilities: current role, applicable permissions, token access and resource scope summaries. Resource access must still be checked at call time.
- list_my_classes, list_my_subjects: authorized scopes with IDs and labels. Academic tools are for Student/Teacher/Subject Manager according to current business rules.
- list_assignments, list_quizzes, list_materials: authorized, paginated search by scope, status and text. Assignment filters should distinguish published/assigned status, deadline, submission state and grading state rather than one ambiguous current flag.
- read_authoring_requirements: server defaults, supported modes, assignment document/proposal schemas, rubric constraints, quiz question schemas and material eligibility.
- Extend material reads with version-aware paging/chunks. Existing material text is limited to 32,000 characters and YouTube content currently returns title/description/URL rather than a transcript.

### Create assignment and quiz

- create_assignment_draft: explicit CLASS/SUBJECT target, title, configuration, reference material IDs and generated content proposal. Return assignmentId, proposalId and version so the author can review/apply the proposal on the app. Use the existing authoring and proposal validators and keep the new assignment in DRAFT.
- create_quiz_draft: explicit CLASS/SUBJECT target, title/settings, references and question proposal. Return quizId, proposalId and version; keep the quiz unpublished and use the existing review flow.
- Retain save_assignment_draft and save_quiz_draft for proposing revisions to existing objects.
- Use requestRef/idempotency for create calls and an atomic creation/proposal boundary to avoid duplicates or abandoned empty drafts after retries.
- Teacher targets assigned classes. Subject Manager targets managed subjects or assigned classes. Preserve the current subject-assignment restriction to individual submission; group class assignments still need group support.

### Question bank

- read_question_bank_context, list_bank_questions and save_question_bank_draft for the Subject Manager's managed subject.
- Support the same question types, difficulty, explanations and validation as existing QuizAiDraftService bank proposals. Accepted bank questions remain drafts under the existing review flow.
- Expose selectable bank questions to quiz authors through quiz-scoped permissions; do not grant Teacher the Subject Manager's bank-management rights.

### Complete individual and group grading

- list_submissions: filter by assignment/class, individual/group, submitted/late, evaluation state and proposal availability; return evaluation IDs and versions.
- read_grading_context: frozen rubric, submission reference/hash, evaluation version/state, content blocks/IDs and proposal output schema, for both individual attempts and group document roots. Include authorized image/diagram references where needed rather than relying only on flattened text.
- read_grading_batch: bounded selection of the same context, returning per-item errors and continuations when content exceeds the response budget.
- record_grading_proposals_batch: use existing proposal validation, snapshots and request idempotency per item; keep partial successes and retry failed items only.
- get_grading_proposals/list_grading_proposals: inspect stored proposal state so the agent can resume without regrading completed work.
- A job/status tool is useful only if the server actually introduces asynchronous batch work. For an external agent doing inference, bounded reads and proposal writes do not by themselves require a server grading job.

### Student learning support

- list_my_attempts and list_private_feedback complete the existing submitted-Practice feedback flow.
- read_my_quiz_result lets the agent explain mistakes after submission using only fields the application's show-score/show-correct-answer policy already exposes. This is a new MCP learning workflow, not a current AI feature.
- read_my_progress/get_my_deadlines enables personal study planning from permitted attempts, released grades and deadlines. The agent can generate the plan without a new model-calling backend service.
- Existing authorized material reads already allow agent-side summaries, explanations, flashcards and practice questions in chat. Discovery and chunked reading are the missing parts; saving notes, decks or generated personal quizzes would need new product storage/services.
- Do not expose authoring quiz context to Student. Broader tutoring on an active graded assignment would require an explicit product decision because current student MCP intentionally restricts that context.

### Teacher and Subject Manager reporting/content assistance

- read_class_learning_summary and read_grade_distribution can support feedback summaries, common-error analysis and remediation suggestions for classes the caller teaches. These are proposed AI analyses over existing data, not existing provider-backed AI task types.
- Subject-level reports need an explicitly authorized aggregate contract. Current Subject Manager status alone does not allow arbitrary per-student class-grade reads.
- Material summaries, rubric improvement and question quality checks can be generated by the external agent using authorized context. Reuse proposal writes to persist proposed assignment/quiz changes.
- Announcement drafting is another candidate. Current AnnouncementService.create posts immediately and may emit email/notification events; a genuine draft/preview path is needed before exposing a tool named save_announcement_draft. Posting is a separate action.
- Diagram review can be added later using authorized structured diagram/image context. Current text-only MCP reads do not establish complete multimodal grading support.

### Admin

- Existing services provide institutional connection status, quota policy, per-teacher quota, usage summary and operational dashboard. These are candidates for read_ai_usage_summary, read_ai_quota_policy and read_platform_health_summary so an external agent can analyze trends and prepare reports.
- These tools require a deliberate new Admin MCP admission and scope policy; simply adding ADMIN to existing academic tools would violate current separation of responsibilities.
- Start an optional Admin surface with aggregate read-only reporting and masked provider status. Provider secrets, configuration changes, account changes and backups are outside the proposed first academic MCP scope.

## Delivery recommendation

1. Foundation plus authoring: shared discovery, authoring constraints, create_assignment_draft and create_quiz_draft. This makes the user's natural-language creation request executable without manually copying IDs.
2. Grading completion: individual/group grading context, submission filters and bounded batch proposal writes. Reuse current review/approval services.
3. Subject Manager bank proposals and Student Practice discovery/history. These close gaps in already implemented AI workflows.
4. New learning/reporting use cases: quiz-result explanation, personal progress, teaching summaries and optional separate Admin reporting.

Keep external-agent inference as the default MCP path: the agent generates content with its own model/account and the platform supplies authorized data and stores validated proposals. Calling the platform's configured AI providers through MCP is a distinct optional path requiring explicit source/provider, quota/usage accounting and task tracking; it is not necessary merely to support AI authoring through MCP.

All list/batch tools need bounded pages, resource checks, version/hash consistency, stable item identifiers and explicit partial-failure results. Current MCP argument/result limits are 64 KB/256 KB; large grading/material content needs chunking rather than a silently incomplete grading context. Human publication and grading approval follow the existing product workflow unless a separately approved change alters it.

## Source evidence

- backend/src/main/java/edu/aiplatform/identity/contract/Role.java
- backend/src/main/java/edu/aiplatform/identity/application/AccessPolicy.java
- backend/src/main/java/edu/aiplatform/identity/application/AuthorizationService.java
- backend/src/main/java/edu/aiplatform/ai/application/McpCredentialService.java
- backend/src/main/java/edu/aiplatform/ai/application/McpToolRegistry.java
- backend/src/main/java/edu/aiplatform/ai/application/McpServer.java
- backend/src/main/java/edu/aiplatform/assignments/application/AssignmentMcpTools.java
- backend/src/main/java/edu/aiplatform/assignments/application/AssignmentAuthoringService.java
- backend/src/main/java/edu/aiplatform/assignments/application/AssignmentAiDraftService.java
- backend/src/main/java/edu/aiplatform/quizzes/application/QuizMcpTools.java
- backend/src/main/java/edu/aiplatform/quizzes/application/QuizAuthoringService.java
- backend/src/main/java/edu/aiplatform/quizzes/application/QuizAiDraftService.java
- backend/src/main/java/edu/aiplatform/assignmentwork/application/AssignmentWorkMcpTools.java
- backend/src/main/java/edu/aiplatform/assignmentwork/application/PracticeFeedbackService.java
- backend/src/main/java/edu/aiplatform/grading/application/GradingMcpTools.java
- backend/src/main/java/edu/aiplatform/grading/application/GradingProposalTransactions.java
- backend/src/main/java/edu/aiplatform/grading/application/GradingProposalService.java
- backend/src/main/java/edu/aiplatform/grading/application/GradebookService.java
- backend/src/main/java/edu/aiplatform/materials/application/MaterialMcpTools.java
- backend/src/main/java/edu/aiplatform/quizpractice/application/QuizPracticeQueryService.java
- backend/src/main/java/edu/aiplatform/announcements/application/AnnouncementService.java
- backend/src/main/java/edu/aiplatform/ai/web/AiAdminController.java
- backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardService.java

Validation: inspected role policy, all registered domain MCP providers, AI task submission/handlers and related application/UI flows. Source review only; no test suites, provider requests, tool writes or user data mutations were performed. Proposed schemas and tool names still require implementation design and acceptance checks.
