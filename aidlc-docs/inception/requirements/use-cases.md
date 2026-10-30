# Use case catalog — synchronized to the current UC table

**66 active UC; 50 MVP stories + 1 extension; 15 logical units.**

Source: [English UC table](../../../docs/usecase_table.md). User-approved catalog changes are documented in sync plan. Documentation-only; code-derived documentation synchronized 2026-10-30; Jira/Google Reports are outside this local sync; source-observed is not runtime acceptance.

| ID | Use case | Primary unit | Scope/behavior |
|---|---|---|---|
| 01 | Activate Account | U01 | Allows a user to activate a school-issued account by verifying an OTP sent through Brevo and setting an initial password. |
| 02 | Login | U01 | Allows a user with an active account to sign in using their email and password and access the functions available to their role. |
| 03 | Logout | U01 | Allows a signed-in user to end the current session and return to the login screen. |
| 04 | Reset Password | U01 | Allows a user to verify an OTP sent through Brevo and set a new password to recover account access. |
| 05 | Change Password | U01 | Allows a signed-in user to change their password after verifying the current password. |
| 06 | View Profile Information | U01 | Allows a signed-in user to view their own profile information, including account details and role. |
| 07 | Update Profile Information | U01 | Allows a signed-in user to update permitted profile fields, such as display name and phone number. |
| 08 | View Account List | U01 | Allows an administrator to view platform accounts and select an account to inspect or manage. |
| 09 | Add Account | U01 | Allows an administrator to create a school-issued account and assign its initial information and role. The account holder completes account activation separately. |
| 10 | View Account Detail | U01 | Allows an administrator to view an account's profile information, role and current status. |
| 11 | Update Account Information | U01 | Allows an administrator to update permitted information and role settings of a selected account. |
| 12 | Change Account Status | U01 | Allows an administrator to change an account's status and control its eligibility to access the platform. |
| 13 | View Audit Log | U02 | Allows an administrator to view and search audit logs by actor, action, affected object, result and time. Audit records are read-only and cannot be edited or deleted. |
| 14 | View Settings | U03 | Allows an administrator to view permitted operational settings, file constraints, backup schedule, fixed one-day activity retention information and institutional AI token quota policies for eligible teachers and subject managers. The settings also show institutional Gemini, OpenAI or Claude API configuration, selected providers/models and connection status. API keys remain masked; viewing settings does not change configuration or grant academic content access. |
| 15 | Edit Setting | U03 | Allows an administrator to update permitted operational settings, file constraints, backup schedule, fixed one-day activity retention information and institutional AI token quota policies for eligible teachers and subject managers. Within the same settings, the administrator can configure, replace or revoke institutional Gemini, OpenAI or Claude API keys, select supported models and test connections with synthetic data. Credentials are protected, and changes are validated, version-checked and audited. Shared API access is educator-only within quota; quota is not sold or allocated to students, and exhausted shared quota never automatically uses a personal API. Configuration does not grant academic content access; personal APIs and MCP agents remain separate through UC 31/32. |
| 16 | View Subject List | U04 | Allows an administrator to view the platform's subjects and select a subject to inspect or manage. |
| 17 | Add Subject | U04 | Allows an administrator to create a subject with the required identifying information and subject-manager assignment. |
| 18 | View Subject Detail | U04 | Allows an administrator to view a subject's information, assigned subject manager and related class information. |
| 19 | Update Subject Information | U04 | Allows an administrator to update permitted subject information and its subject-manager assignment, and to archive or reopen the subject. |
| 20 | View Enrolled Classes | U05 | Allows a student to view the classes in which they are enrolled and select a class to access. |
| 21 | View Enrolled Class Detail | U05 | Allows a student to view an enrolled class's information and access its learning materials, assignments, announcements and group information. |
| 22 | View Assigned Class List | U05 | Allows a teacher to view the classes assigned to them and select a class to manage. |
| 23 | View Assigned Class Detail | U05 | Allows a teacher to view an assigned class's information and access its teaching and management functions. |
| 24 | View Managed Subject Classes | U05 | Allows a subject manager to view the classes belonging to the subjects they manage. |
| 25 | View Managed Class Detail | U05 | Allows a subject manager to view a managed class's information, assigned teacher and enrolled students. |
| 26 | Create Class | U05 | Allows a subject manager to create a class within a subject they manage and enter the required class information. |
| 27 | Assign Teacher To Class | U05 | Allows a subject manager to assign or change the teacher responsible for a managed class. |
| 28 | Add/Remove Class Students | U05 | Allows a subject manager to add selected students to a managed class or remove students from it. Removed students keep their history. |
| 29 | Edit Class Information | U05 | Allows a subject manager to update permitted information and settings of a managed class, including opening, archiving or reopening the class and showing the anonymous grade distribution to students. |
| 30 | View Managed Subject | U05 | Allows a subject manager to view the subjects assigned to them and access a subject's information and management functions. |
| 31 | Manage Personal AI API | U06 | Allows a student, teacher or subject manager to add their own Gemini, OpenAI or Claude API key, test the connection, replace the key or revoke it. API credentials are protected and masked; personal API access does not grant additional academic permissions or institutional quota. External MCP agents are managed separately through UC 32. |
| 32 | Connect/Revoke MCP Agent | U06 | Allows an authorized user to connect or revoke their own agent through ALAP's authenticated MCP server and follow client-specific setup instructions. Each tool call checks current permissions, scope and state; official submission and grade publication are not initial MCP tools. |
| 33 | View AI Usage History | U06 | Allows a user to view their own AI usage, connection source, provider/model when known and token counts with measured, estimated, externally reported or unavailable provenance. Administrators view authorized shared-usage aggregates. Unknown MCP model usage is not displayed as zero. |
| 34 | View Teacher Assignment List | U07 | Allows an assigned teacher to view and manage a class's individual or group Assignments, and a subject manager to view and manage subject-level individual Assignments, including publication/version/copy status and submission settings. Draw.io is optional within Assignment; Code Lab is outside scope. |
| 35 | Create/Update/Delete Individual Assignment | U07 | Allows an assigned teacher, or a subject manager within their authorized subject scope, to create, update or delete individual Assignments using document sections and rubrics. Each student completes and submits their own work. Draw.io diagrams are optional and enabled by the educator; optional activity monitoring is configured before publication. Student feedback is available only for submitted Practice Assignments, not editable drafts or Graded work. |
| 36 | Add/Update/Delete Rubric | U07 | Allows an authorized educator to add, update or delete rubrics for document sections in individual or group Assignments, including diagram criteria when diagrams are enabled. Rubrics are validated, locked at publication and snapshotted for attempts and submissions. |
| 37 | View Assignment List | U08 | Allows a student to view available individual or group Assignments in their enrolled classes. An Assignment may contain document content only or optional Draw.io diagrams; Quiz is a separate assessment type. |
| 38 | View Assignment Detail | U08 | Student views permitted pre-start metadata, schedule and available actions; full instructions/document content become available through an authorized attempt, rubric stays hidden. |
| 39 | Complete Individual Assignment | U08 | Allows a student to start or resume an individual Assignment, edit permitted document sections, autosave drafts and submit the completed work. When the teacher enables diagrams, the student can optionally create or edit Draw.io diagram blocks within the workspace; full Draw.io XML and previews are preserved with the document. Diagram editing is part of Assignment completion, not a separate use case or assessment type. |
| 40 | View Submission History | U08 | Allows a student to view their previous submissions for an assignment, including submitted content, submission time and permitted scores or feedback. |
| 41 | Request Practice Feedback | U08 | Allows a student to request private feedback on their own submitted Practice Assignment through a personal AI API connection or external MCP agent. Feedback is versioned and does not create official Gradebook grades; no shared AI token grant is provided to students. |
| 42 | View Student Submissions | U09 | Allows a teacher assigned to the class to inspect individual/group submissions and grading status, and review available activity signals within permitted work or Quiz-attempt details when monitoring is enabled. Signals show page-leaving counts, duration, timelines and missing data per student; they do not automatically establish cheating or change grades. Quizzes are Practice only. |
| 43 | Grade Submission With AI | U09 | Allows an assigned teacher to use an institutional AI API within quota, a personal AI API or an external MCP agent to assess a submitted Assignment against its rubric and receive versioned scores and feedback. The teacher reviews and confirms the result; AI cannot finalize or publish official grades. |
| 44 | Grade Submissions Manually | U09 | Allows a teacher to evaluate submitted work using the configured scoring criteria and record scores and feedback manually. |
| 45 | View/Export GradeBook | U09 | Allows a teacher to view the class gradebook and export assignment scores and submission or grading status to a supported file format. |
| 46 | View Subject Question Bank | U10 | Allows a subject manager to browse versioned multiple-choice questions and answer keys in an assigned subject's Quiz bank. Code questions and execution tests are outside scope. |
| 47 | Create/Update/Delete Subject Question | U10 | Allows a subject manager to create, update or delete multiple-choice Quiz questions in an assigned subject's question bank. Question versions are preserved for published Quiz snapshots; no Code Lab questions are supported. |
| 48 | Create/Update/Delete Quiz | U10 | Allows an assigned teacher (class Quizzes) or a subject manager (subject-level Quizzes) to create, update or delete standalone multiple-choice Quizzes with answer keys, scoring, attempt/time limits and an optional activity-monitoring policy. A Lesson is not required. Quizzes are Practice only. |
| 49 | View Quiz Practice History | U11 | Allows a student to view their quiz practice history and select a quiz to inspect. |
| 50 | View Quiz Practice Detail | U11 | Allows a student to view a quiz's information and available practice attempts. |
| 51 | Take Quiz | U11 | Allows a student to answer quiz questions and submit an attempt for automatic scoring. |
| 52 | View Quiz Result | U11 | Allows a student to view the score and permitted feedback for a completed quiz attempt. |
| 53 | Create/Update/Delete Group Assignment | U12 | Allows a teacher assigned to a class to create, update or delete group Assignments using a shared document structure and section rubrics. Group members work on authorized sections and the group leader submits the document. Draw.io diagrams are optional and enabled by the teacher; optional activity monitoring is configured before publication. Group Assignments are Graded only; subject-wide group authoring is not supported. |
| 54 | View My Group | U12 | Allows a student to view their group in a class, including its members, leader and group-assignment documents. |
| 55 | Request Leader Change | U12 | Allows a group member to submit a request to change the group leader, provide a reason and optionally propose a replacement. |
| 56 | Complete Group Assignment | U12 | Allows group members to claim and edit authorized sections of a shared Assignment document. Members can optionally create or edit available Draw.io diagram blocks in sections they are permitted to edit; full Draw.io XML and previews are preserved with the document. The group leader submits the group document; version history and individual activity signals are retained when monitoring is enabled. |
| 57 | Create/Update/Delete Class Group | U12 | Allows an assigned teacher to create class groups manually or randomly, add, move or remove members, appoint group leaders, and approve or reject leader-change requests. |
| 58 | View Subject Materials | U13 | Allows a subject manager to view permitted reference files and educational YouTube video links in an assigned subject and inspect their versions or details. |
| 59 | Add/Update/Delete Subject Material | U13 | Allows a subject manager to add, update or remove subject reference files stored privately through Google Drive and educational YouTube video links shared with authorized classes. YouTube links are part of the core scope; full Lesson processing, automatic transcription, AI summaries and advanced RAG remain extensions. |
| 60 | View Uploaded Learning Materials | U13 | Allows a teacher assigned to a class to view and manage its permitted reference files and educational YouTube video links. Full Lesson functionality remains an extension. |
| 61 | Add/Update/Delete Learning Material | U13 | Allows an assigned teacher to add, update or remove class reference files stored privately through Google Drive and links to educational YouTube videos. File and link validation and historical references are preserved. YouTube links are part of the core scope; full Lesson processing, automatic transcription, AI summaries and advanced RAG remain extensions. |
| 62 | View Learning Material | U13 | Allows an authorized user to preview or download permitted reference files stored privately through Google Drive, or open and watch educational videos through YouTube links provided as learning materials. YouTube support means viewing linked videos; automatic transcription, AI summaries and advanced RAG are not required. |
| 63 | View Class Announcements | U14 | Allows authorized students and teachers to view announcements published in a class and read their contents. |
| 64 | Create/Update/Delete Announcement | U14 | Allows an assigned teacher to create, update or delete class announcements. New announcements generate in-app notifications; the teacher may mark a new announcement important and select email delivery. Editing does not automatically resend the creation email. |
| 65 | View Notifications | U14 | Allows a user to view their own notifications, open authorized related information and mark notifications as read. Important learning events may also generate email according to recipient preferences. |
| 66 | View Admin Dashboard | U15 | Allows an administrator to view authorized platform summaries and backup status/history, including the latest successful backup, timestamps, size and sanitized errors. U03 supplies backup information; this view does not grant access to academic content or a production restore action. |

## Retired and consolidated IDs



| Former ID (old numbering) | Change |
| --- | --- |
| 08, 09, 10, 11, 68, 69, 72 | Removed AI-credit packages, purchases, payment results and payment history. Institutional quota is not a purchase or wallet. |
| 25, 43 | Removed Code Lab completion/authoring and Judge0 execution. |
| 26 | Consolidated optional Draw.io diagram editing into UC 39 and UC 56. Diagram functionality is retained within Assignment workspaces; the separate UC ID is retired and not reused. |
| 79 | Removed draft feedback by user decision; Student feedback is available only after submitting a Practice Assignment (UC 41). ID retained as retired. |
| 44 | Consolidated separate Diagram Assignment authoring into UC 35 and UC 53 as optional Draw.io support. UC 53 is reinstated for its original Group Assignment authoring responsibility; UC 35 now covers Individual Assignment authoring. |
| 78 | Educator quota configuration is included in UC 14 — View Settings and UC 15 — Edit Setting. UC 15 is reinstated for its original settings-editing responsibility; UC 78 (ID cũ) remains retired. |
| 80 | Consolidated backup status/history into UC 66 — View Admin Dashboard. |
| 81 | Consolidated work-activity review into UC 42 — View Student Submissions. |
| 76, 82 | Consolidated institutional AI API viewing, editing and connection testing into UC 14 — View Settings and UC 15 — Edit Setting. IDs retained as retired and not reused. |

Optional Draw.io diagram editing is described within UC 39 and UC 56; UC 26 (ID cũ) is no longer a separate active use case. UC 56 remains the group-work/submission workflow. UC 41 concerns only submitted Practice Assignments. Requests on editable drafts or Graded Assignments are rejected. UC 79 (ID cũ) is retired and its ID is not reused.


## Ownership boundary

UC14/15 primary U03 orchestrate settings UI; U06 owns quota, protected institutional API records and tests. UC35 primary U07 authors Individual work, UC53 primary U12 authors Group work; UC39/56 keep optional diagrams without UC26 (ID cũ). UC41 primary U08 accepts only submitted Practice work; UC79 (ID cũ) retired. Units follow the 2026-10-14 three-iteration split in [unit-of-work.md](../application-design/unit-of-work.md); no microservice or approval is introduced.

## Old → new ID mapping (2026-10-14)

Active use cases were renumbered 01–66 in unit order (U01 → U15) on 2026-10-14. References written as "UC NN (ID cũ)" and the Former ID column use the old numbering.

| Old ID | New ID |
|---|---|
| 01 | 01 |
| 02 | 02 |
| 03 | 03 |
| 04 | 04 |
| 05 | 05 |
| 06 | 06 |
| 07 | 07 |
| 59 | 08 |
| 60 | 09 |
| 61 | 10 |
| 62 | 11 |
| 63 | 12 |
| 73 | 13 |
| 70 | 14 |
| 71 | 15 |
| 64 | 16 |
| 65 | 17 |
| 66 | 18 |
| 67 | 19 |
| 13 | 20 |
| 14 | 21 |
| 31 | 22 |
| 32 | 23 |
| 47 | 24 |
| 48 | 25 |
| 49 | 26 |
| 50 | 27 |
| 51 | 28 |
| 52 | 29 |
| 53 | 30 |
| 74 | 31 |
| 75 | 32 |
| 77 | 33 |
| 41 | 34 |
| 42 | 35 |
| 46 | 36 |
| 22 | 37 |
| 23 | 38 |
| 24 | 39 |
| 28 | 40 |
| 29 | 41 |
| 37 | 42 |
| 38 | View Assignment Detail | U08 | Student views permitted pre-start metadata, schedule and available actions; full instructions/document content become available through an authorized attempt, rubric stays hidden. |
| 39 | 44 |
| 40 | 45 |
| 56 | 46 |
| 57 | 47 |
| 35 | 48 |
| 18 | 49 |
| 19 | 50 |
| 20 | 51 |
| 21 | 52 |
| 45 | 53 |
| 16 | 54 |
| 17 | 55 |
| 27 | 56 |
| — (new) | 57 |
| 54 | 58 |
| 55 | 59 |
| 33 | 60 |
| 34 | 61 |
| 15 | 62 |
| 30 | 63 |
| 36 | 64 |
| 12 | 65 |
| 58 | 66 |

## Code-derived clarifications — 2026-10-30

IDs 01–66 remain unchanged. UC14/15 do not expose editable activity.retentionDays: source retains activity for one day. UC26 creates DRAFT; UC29 allows OPEN without teacher. UC32 is for Student/Teacher/Subject Manager, not Admin; owner can reveal an ACTIVE credential when ciphertext exists. UC31 personal API remains available to Student. UC48 supports edit OPEN and retire/reopen under version/container guards; review has not been removed. UC55 request/rejection reasons are optional, max 1,000 characters. UC58–62 include U13 material modules and scoped Quiz attachment; UC66 includes /activity. Admin institutional key reveal is a documented AS-BUILT exception to prior no-readback design. See [decision-register.md](../reverse-engineering/decision-register.md).

## MCP correction and audit remediation — 2026-10-30

Student/Teacher/Subject Manager ACTIVE manage own MCP credentials; Admin is denied, including pre-existing tokens. UC41 personal API and MCP share own/SUBMITTED/PRACTICE/current-enrollment/hash/schema/pending/daily-budget restrictions. MCP READ_ONLY cannot record feedback; READ_WRITE can. API and MCP quota checks are serialized per account inside a transaction. UC52 remains deterministic permitted Quiz result/explanation, not an AI generation service. Current code is a remediation working tree on794a4053; dated initial audit and staff-expansion docs are historical where superseded.
