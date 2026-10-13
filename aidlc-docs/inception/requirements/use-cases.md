# Use case catalog — synchronized to the current UC table

**65 active UC; 50 MVP stories + 1 extension; 15 logical units.**

Source: [English UC table](../../../docs/usecase_table.md). User-approved catalog changes are documented in [sync plan](../plans/uc-table-65-sync-2026-10-13.md). Documentation-only; code, Jira, Google Reports and diagram files are not synchronized by this request.

| ID | Use case | Primary unit | Scope/behavior |
|---|---|---|---|
| 01 | Activate Account | U01 | Allows a user to activate a school-issued account by verifying an OTP sent through Brevo and setting an initial password. |
| 02 | Login | U01 | Allows a user with an active account to sign in using their email and password and access the functions available to their role. |
| 03 | Logout | U01 | Allows a signed-in user to end the current session and return to the login screen. |
| 04 | Reset Password | U01 | Allows a user to verify an OTP sent through Brevo and set a new password to recover account access. |
| 05 | Change Password | U01 | Allows a signed-in user to change their password after verifying the current password. |
| 06 | View Profile Information | U01 | Allows a signed-in user to view their own profile information, including account details and role. |
| 07 | Update Profile Information | U01 | Allows a signed-in user to update permitted profile fields, such as display name and phone number. |
| 12 | View Notifications | U16 | Allows a user to view their own notifications, open authorized related information and mark notifications as read. Important learning events may also generate email according to recipient preferences. |
| 13 | View Enrolled Classes | U04 | Allows a student to view the classes in which they are enrolled and select a class to access. |
| 14 | View Enrolled Class Detail | U04 | Allows a student to view an enrolled class's information and access its learning materials, assignments, announcements and group information. |
| 15 | View Learning Material | U05 | Allows an authorized user to preview or download permitted reference files stored privately through Google Drive, or open and watch educational videos through YouTube links provided as learning materials. YouTube support means viewing linked videos; automatic transcription, AI summaries and advanced RAG are not required. |
| 16 | View My Group | U12 | Allows a student to view their group in a class, including its members, leader and group-assignment documents. |
| 17 | Request Leader Change | U12 | Allows a group member to submit a request to change the group leader, provide a reason and optionally propose a replacement. |
| 18 | View Quiz Practice History | U11 | Allows a student to view their quiz practice history and select a quiz to inspect. |
| 19 | View Quiz Practice Detail | U11 | Allows a student to view a quiz's information and available practice attempts. |
| 20 | Take Quiz | U11 | Allows a student to answer quiz questions and submit an attempt for automatic scoring. |
| 21 | View Quiz Result | U11 | Allows a student to view the score and permitted feedback for a completed quiz attempt. |
| 22 | View Assignment List | U11 | Allows a student to view available individual or group Assignments in their enrolled classes. An Assignment may contain document content only or optional Draw.io diagrams; Quiz is a separate assessment type. |
| 23 | View Assignment Detail | U11 | Allows a student to view an Assignment's instructions, document structure, rubric, submission mode, diagram requirements, deadline, attempt limits and applicable AI/activity policies. |
| 24 | Complete Individual Assignment | U11 | Allows a student to start or resume an individual Assignment, edit permitted document sections, autosave drafts and submit the completed work. When the teacher enables diagrams, the student can optionally create or edit Draw.io diagram blocks within the workspace; full Draw.io XML and previews are preserved with the document. Diagram editing is part of Assignment completion, not a separate use case or assessment type. |
| 27 | Complete Group Assignment | U14 | Allows group members to claim and edit authorized sections of a shared Assignment document. When the teacher enables diagrams, members can optionally create or edit Draw.io diagram blocks in sections they are permitted to edit; full Draw.io XML and previews are preserved with the document. The group leader submits the group document; version history and individual activity signals are retained when monitoring is enabled. |
| 28 | View Submission History | U11 | Allows a student to view their previous submissions for an assignment, including submitted content, submission time and permitted scores or feedback. |
| 29 | Request Practice Feedback | U11 | Allows a student to request private feedback on their own submitted Practice Assignment through a personal AI API connection or external MCP agent. Feedback is versioned and does not create official Gradebook grades; no shared AI token grant is provided to students. |
| 30 | View Class Announcements | U05 | Allows authorized students and teachers to view announcements published in a class and read their contents. |
| 31 | View Assigned Class List | U04 | Allows a teacher to view the classes assigned to them and select a class to manage. |
| 32 | View Assigned Class Detail | U04 | Allows a teacher to view an assigned class's information and access its teaching and management functions. |
| 33 | View Uploaded Learning Materials | U05 | Allows a teacher assigned to a class to view and manage its permitted reference files and educational YouTube video links. Full Lesson functionality remains an extension. |
| 34 | Add/Update/Delete Learning Material | U05 | Allows an assigned teacher to add, update or remove class reference files stored privately through Google Drive and links to educational YouTube videos. File and link validation and historical references are preserved. YouTube links are part of the core scope; full Lesson processing, automatic transcription, AI summaries and advanced RAG remain extensions. |
| 35 | Create/Update/Delete Quiz | U09 | Allows an assigned teacher to create, update or delete standalone multiple-choice Quizzes with answer keys, scoring, attempt/time limits and an optional activity-monitoring policy. A Lesson is not required. Practice is the current provisional mode; Graded Quiz remains an open decision. |
| 36 | Create/Update/Delete Announcement | U05 | Allows an assigned teacher to create, update or delete class announcements. New announcements generate in-app notifications; the teacher may mark a new announcement important and select email delivery. Editing does not automatically resend the creation email. |
| 37 | View Student Submissions | U15 | Allows a teacher assigned to the class to inspect individual/group submissions and grading status, and review available activity signals within permitted work or Quiz-attempt details when monitoring is enabled. Signals show page-leaving counts, duration, timelines and missing data per student; they do not automatically establish cheating or change grades. Quiz remains Practice under the current provisional policy. |
| 38 | Grade Submission With AI | U15 | Allows an assigned teacher to use an institutional AI API within quota, a personal AI API or an external MCP agent to assess a submitted Assignment against its rubric and receive versioned scores and feedback. The teacher reviews and confirms the result; AI cannot finalize or publish official grades. |
| 39 | Grade Submissions Manually | U15 | Allows a teacher to evaluate submitted work using the configured scoring criteria and record scores and feedback manually. |
| 40 | View/Export GradeBook | U15 | Allows a teacher to view the class gradebook and export assignment scores and submission or grading status to a supported file format. |
| 41 | View Teacher Assignment List | U08 | Allows an assigned teacher to view and manage individual or group Assignments, publication/version/copy status and submission settings. Draw.io is optional within Assignment; Code Lab is outside scope. |
| 42 | Create/Update/Delete Individual Assignment | U09 | Allows an assigned teacher, or a subject manager within their authorized subject scope, to create, update or delete individual Assignments using document sections and rubrics. Each student completes and submits their own work. Draw.io diagrams are optional and enabled by the educator; optional activity monitoring is configured before publication. Student feedback is available only for submitted Practice Assignments, not editable drafts or Graded work. |
| 45 | Create/Update/Delete Group Assignment | U09 | Allows a teacher assigned to a class to create, update or delete group Assignments using a shared document structure and section rubrics. Group members work on authorized sections and the group leader submits the document. Draw.io diagrams are optional and enabled by the teacher; optional activity monitoring is configured before publication. Group Assignments remain Graded under the provisional policy; subject-wide group authoring is not supported. |
| 46 | Add/Update/Delete Rubric | U06 | Allows an authorized educator to add, update or delete rubrics for document sections in individual or group Assignments, including diagram criteria when diagrams are enabled. Rubrics are validated, locked at publication and snapshotted for attempts and submissions. |
| 47 | View Managed Subject Classes | U04 | Allows a subject manager to view the classes belonging to the subjects they manage. |
| 48 | View Managed Class Detail | U04 | Allows a subject manager to view a managed class's information, assigned teacher and enrolled students. |
| 49 | Create Class | U04 | Allows a subject manager to create a class within a subject they manage and enter the required class information. |
| 50 | Assign Teacher To Class | U04 | Allows a subject manager to assign or change the teacher responsible for a managed class. |
| 51 | Add/Remove Class Students | U04 | Allows a subject manager to add selected students to a managed class or remove students from it. Removed students keep their history. |
| 52 | Edit Class Information | U04 | Allows a subject manager to update permitted information and settings of a managed class, including opening, archiving or reopening the class and showing the anonymous grade distribution to students. |
| 53 | View Managed Subject | U04 | Allows a subject manager to view the subjects assigned to them and access a subject's information and management functions. |
| 54 | View Subject Materials | U05 | Allows a subject manager to view permitted reference files and educational YouTube video links in an assigned subject and inspect their versions or details. |
| 55 | Add/Update/Delete Subject Material | U05 | Allows a subject manager to add, update or remove subject reference files stored privately through Google Drive and educational YouTube video links shared with authorized classes. YouTube links are part of the core scope; full Lesson processing, automatic transcription, AI summaries and advanced RAG remain extensions. |
| 56 | View Subject Question Bank | U06 | Allows a subject manager to browse versioned multiple-choice questions and answer keys in an assigned subject's Quiz bank. Code questions and execution tests are outside scope. |
| 57 | Create/Update/Delete Subject Question | U06 | Allows a subject manager to create, update or delete multiple-choice Quiz questions in an assigned subject's question bank. Question versions are preserved for published Quiz snapshots; no Code Lab questions are supported. |
| 58 | View Admin Dashboard | U16 | Allows an administrator to view authorized platform summaries and backup status/history, including the latest successful backup, timestamps, size and sanitized errors. U03 supplies backup information; this view does not grant access to academic content or a production restore action. |
| 59 | View Account List | U01 | Allows an administrator to view platform accounts and select an account to inspect or manage. |
| 60 | Add Account | U01 | Allows an administrator to create a school-issued account and assign its initial information and role. The account holder completes account activation separately. |
| 61 | View Account Detail | U01 | Allows an administrator to view an account's profile information, role and current status. |
| 62 | Update Account Information | U01 | Allows an administrator to update permitted information and role settings of a selected account. |
| 63 | Change Account Status | U01 | Allows an administrator to change an account's status and control its eligibility to access the platform. |
| 64 | View Subject List | U04 | Allows an administrator to view the platform's subjects and select a subject to inspect or manage. |
| 65 | Add Subject | U04 | Allows an administrator to create a subject with the required identifying information and subject-manager assignment. |
| 66 | View Subject Detail | U04 | Allows an administrator to view a subject's information, assigned subject manager and related class information. |
| 67 | Update Subject Information | U04 | Allows an administrator to update permitted subject information and its subject-manager assignment, and to archive or reopen the subject. |
| 70 | View Settings | U03 | Allows an administrator to view permitted operational settings, file constraints, backup schedule/retention, activity retention and institutional AI token quota policies for eligible teachers and subject managers. The settings also show institutional Gemini, OpenAI or Claude API configuration, selected providers/models and connection status. API keys remain masked; viewing settings does not change configuration or grant academic content access. |
| 71 | Edit Setting | U03 | Allows an administrator to update permitted operational settings, file constraints, backup schedule/retention, activity retention and institutional AI token quota policies for eligible teachers and subject managers. Within the same settings, the administrator can configure, replace or revoke institutional Gemini, OpenAI or Claude API keys, select supported models and test connections with synthetic data. Credentials are protected, and changes are validated, version-checked and audited. Shared API access is educator-only within quota; quota is not sold or allocated to students, and exhausted shared quota never automatically uses a personal API. Configuration does not grant academic content access; personal APIs and MCP agents remain separate through UC 74/75. |
| 73 | View Audit Log | U02 | Allows an administrator to view and search audit logs by actor, action, affected object, result and time. Audit records are read-only and cannot be edited or deleted. |
| 74 | Manage Personal AI API | U13 | Allows a student, teacher or subject manager to add their own Gemini, OpenAI or Claude API key, test the connection, replace the key or revoke it. API credentials are protected and masked; personal API access does not grant additional academic permissions or institutional quota. External MCP agents are managed separately through UC 75. |
| 75 | Connect/Revoke MCP Agent | U13 | Allows an authorized user to connect or revoke their own agent through ALAP's authenticated MCP server and follow client-specific setup instructions. Each tool call checks current permissions, scope and state; official submission and grade publication are not initial MCP tools. |
| 77 | View AI Usage History | U07 | Allows a user to view their own AI usage, connection source, provider/model when known and token counts with measured, estimated, externally reported or unavailable provenance. Administrators view authorized shared-usage aggregates. Unknown MCP model usage is not displayed as zero. |

## Retired and consolidated IDs



| Former ID | Change |
| --- | --- |
| 08, 09, 10, 11, 68, 69, 72 | Removed AI-credit packages, purchases, payment results and payment history. Institutional quota is not a purchase or wallet. |
| 25, 43 | Removed Code Lab completion/authoring and Judge0 execution. |
| 26 | Consolidated optional Draw.io diagram editing into UC 24 and UC 27. Diagram functionality is retained within Assignment workspaces; the separate UC ID is retired and not reused. |
| 79 | Removed draft feedback by user decision; Student feedback is available only after submitting a Practice Assignment (UC 29). ID retained as retired. |
| 44 | Consolidated separate Diagram Assignment authoring into UC 42 and UC 45 as optional Draw.io support. UC 45 is reinstated for its original Group Assignment authoring responsibility; UC 42 now covers Individual Assignment authoring. |
| 78 | Educator quota configuration is included in UC 70 — View Settings and UC 71 — Edit Setting. UC 71 is reinstated for its original settings-editing responsibility; UC 78 remains retired. |
| 80 | Consolidated backup status/history into UC 58 — View Admin Dashboard. |
| 81 | Consolidated work-activity review into UC 37 — View Student Submissions. |
| 76, 82 | Consolidated institutional AI API viewing, editing and connection testing into UC 70 — View Settings and UC 71 — Edit Setting. IDs retained as retired and not reused. |

Optional Draw.io diagram editing is described within UC 24 and UC 27; UC 26 is no longer a separate active use case. UC 27 remains the group-work/submission workflow. UC 29 concerns only submitted Practice Assignments. Requests on editable drafts or Graded Assignments are rejected. UC 79 is retired and its ID is not reused.


## Ownership boundary

UC70/71 primary U03 orchestrate settings UI; U07 owns quota, U13 owns protected institutional API records and tests. UC42/45 primary U09 author Individual/Group work; UC24/27 keep optional diagrams without UC26. UC29 primary U11 accepts only submitted Practice work; UC79 retired. No new unit, microservice or approval is introduced.
