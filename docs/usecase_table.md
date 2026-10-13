# 4.2 Use Cases (UC)

**Current revision: 2026-10-13 — 66 active use cases.** This table replaces the former 73-use-case table and follows the [current Inception catalog](../aidlc-docs/inception/requirements/use-cases.md).

The platform supports two assessment types: **ASSIGNMENT** and **QUIZ**. Assignment supports individual/group submission and optional Draw.io diagrams. AI uses institutional educator quotas, personal APIs or external agents through MCP; AI credits are not sold. Backup, work-activity signals and important-event email are included in the proposed scope. These are requirements, not implementation evidence.

IDs remain stable for traceability; removed IDs are not reused or renumbered. Quiz Practice and Group Graded are provisional policies; remaining decisions are recorded in the [decision log](../aidlc-docs/inception/requirements/revision-decisions-2026-10-13.md).

| ID | Use Case | Feature | Use Case Description |
| --- | --- | --- | --- |
| 01 | Activate Account | Authentication | Allows a user to activate a school-issued account by verifying an OTP sent through Brevo and setting an initial password. |
| 02 | Login | Authentication | Allows a user with an active account to sign in using their email and password and access the functions available to their role. |
| 03 | Logout | Authentication | Allows a signed-in user to end the current session and return to the login screen. |
| 04 | Reset Password | Authentication | Allows a user to verify an OTP sent through Brevo and set a new password to recover account access. |
| 05 | Change Password | Authentication | Allows a signed-in user to change their password after verifying the current password. |
| 06 | View Profile Information | Profile Management | Allows a signed-in user to view their own profile information, including account details and role. |
| 07 | Update Profile Information | Profile Management | Allows a signed-in user to update permitted profile fields, such as display name and phone number. |
| 12 | View Notifications | Notification | Allows a user to view their own notifications, open authorized related information and mark notifications as read. Important learning events may also generate email according to recipient preferences. |
| 13 | View Enrolled Classes | Student Class Access | Allows a student to view the classes in which they are enrolled and select a class to access. |
| 14 | View Enrolled Class Detail | Student Class Access | Allows a student to view an enrolled class's information and access its learning materials, assignments, announcements and group information. |
| 15 | View Learning Material | Reference Materials | Allows an authorized user to preview or download permitted reference files stored privately through Google Drive. Full Lesson, YouTube, summaries and advanced RAG remain extensions. |
| 16 | View My Group | Student Group | Allows a student to view their group in a class, including its members, leader and group-assignment documents. |
| 17 | Request Leader Change | Student Group | Allows a group member to submit a request to change the group leader, provide a reason and optionally propose a replacement. |
| 18 | View Quiz Practice History | Student Quiz Access | Allows a student to view their quiz practice history and select a quiz to inspect. |
| 19 | View Quiz Practice Detail | Student Quiz Access | Allows a student to view a quiz's information and available practice attempts. |
| 20 | Take Quiz | Student Quiz Completion | Allows a student to answer quiz questions and submit an attempt for automatic scoring. |
| 21 | View Quiz Result | Student Quiz Review | Allows a student to view the score and permitted feedback for a completed quiz attempt. |
| 22 | View Assignment List | Student Assignment Access | Allows a student to view available individual or group Assignments in their enrolled classes. An Assignment may contain document content only or optional Draw.io diagrams; Quiz is a separate assessment type. |
| 23 | View Assignment Detail | Student Assignment Access | Allows a student to view an Assignment's instructions, document structure, rubric, submission mode, diagram requirements, deadline, attempt limits and applicable AI/activity policies. |
| 24 | Complete Individual Assignment | Assignment Workspace | Allows a student to start or resume an individual Assignment, edit permitted document sections, autosave drafts and submit the completed work. Diagram editing is available only when diagramEnabled is true. |
| 26 | Edit Assignment Diagram | Assignment Diagram Editing | Allows a student to create or edit Draw.io diagram blocks within an authorized Assignment workspace when diagrams are enabled. Full Draw.io XML is preserved; this is an Assignment capability, not a separate assessment type. |
| 27 | Complete Group Assignment | Group Assignment Workspace | Allows group members to claim and edit authorized sections of a shared Assignment document, with optional Draw.io diagrams. The group leader submits the group document; version history and individual activity signals are retained when monitoring is enabled. |
| 28 | View Submission History | Student Submission Review | Allows a student to view their previous submissions for an assignment, including submitted content, submission time and permitted scores or feedback. |
| 29 | Request Practice Feedback | Private Practice Feedback | Allows a student to request private feedback on their own submitted Practice Assignment through a personal AI API connection or external MCP agent. Feedback is versioned and does not create official Gradebook grades; no shared AI token grant is provided to students. |
| 30 | View Class Announcements | Class Communication | Allows authorized students and teachers to view announcements published in a class and read their contents. |
| 31 | View Assigned Class List | Teacher Class Access | Allows a teacher to view the classes assigned to them and select a class to manage. |
| 32 | View Assigned Class Detail | Teacher Class Access | Allows a teacher to view an assigned class's information and access its teaching and management functions. |
| 33 | View Uploaded Learning Materials | Class Reference Materials | Allows a teacher assigned to a class to view and manage its permitted reference materials. Full Lesson functionality remains an extension. |
| 34 | Add/Update/Delete Learning Material | Class Reference Materials | Allows an assigned teacher to add, update or remove class reference files stored privately through Google Drive. File validation and historical references are preserved; YouTube and full Lesson features remain extensions. |
| 35 | Create/Update/Delete Quiz | Quiz Authoring | Allows an assigned teacher to create, update or delete standalone multiple-choice Quizzes with answer keys, scoring, attempt/time limits and an optional activity-monitoring policy. A Lesson is not required. Practice is the current provisional mode; Graded Quiz remains an open decision. |
| 36 | Create/Update/Delete Announcement | Class Communication | Allows an assigned teacher to create, update or delete class announcements. New announcements generate in-app notifications; the teacher may mark a new announcement important and select email delivery. Editing does not automatically resend the creation email. |
| 37 | View Student Submissions | Teacher Work Review | Allows a teacher assigned to the class to inspect individual/group submissions and grading status, and review available activity signals within permitted work or Quiz-attempt details when monitoring is enabled. Signals show page-leaving counts, duration, timelines and missing data per student; they do not automatically establish cheating or change grades. Quiz remains Practice under the current provisional policy. |
| 38 | Request AI Grading Proposal | AI-Assisted Grading | Allows an assigned teacher to request a versioned rubric-based grading proposal through an institutional API within quota, a personal API, or an external MCP agent. The teacher reviews and confirms grades; AI cannot finalize or publish official grades. |
| 39 | Grade Submissions Manually | Teacher Grading | Allows a teacher to evaluate submitted work using the configured scoring criteria and record scores and feedback manually. |
| 40 | View/Export GradeBook | Gradebook | Allows a teacher to view the class gradebook and export assignment scores and submission or grading status to a supported file format. |
| 41 | View Teacher Assignment List | Assignment Lifecycle | Allows an assigned teacher to view and manage individual or group Assignments, publication/version/copy status and submission settings. Draw.io is optional within Assignment; Code Lab is outside scope. |
| 42 | Create/Update/Delete Assignment | Assignment Authoring | Allows an assigned teacher to create, update or delete Assignments using a document structure and rubrics. submissionMode (INDIVIDUAL/GROUP) and diagramEnabled (true/false) are independent settings. Optional activity monitoring and permitted student AI feedback policies are configured before publication. |
| 46 | Manage Assignment Rubrics | Assignment Rubrics | Allows an authorized educator to manage document-section rubrics and diagram criteria when diagrams are enabled. Rubrics are validated, locked at publication and snapshotted for attempts and submissions. |
| 47 | View Managed Subject Classes | Subject Class Management | Allows a subject manager to view the classes belonging to the subjects they manage. |
| 48 | View Managed Class Detail | Subject Class Management | Allows a subject manager to view a managed class's information, assigned teacher and enrolled students. |
| 49 | Create Class | Subject Class Management | Allows a subject manager to create a class within a subject they manage and enter the required class information. |
| 50 | Assign Teacher To Class | Subject Class Management | Allows a subject manager to assign or change the teacher responsible for a managed class. |
| 51 | Add/Remove Class Students | Class Enrollment Management | Allows a subject manager to add selected students to a managed class or remove students from it. Removed students keep their history. |
| 52 | Edit Class Information | Subject Class Management | Allows a subject manager to update permitted information and settings of a managed class, including opening, archiving or reopening the class and showing the anonymous grade distribution to students. |
| 53 | View Managed Subject | Subject Management | Allows a subject manager to view the subjects assigned to them and access a subject's information and management functions. |
| 54 | View Subject Materials | Subject Reference Materials | Allows a subject manager to view permitted reference files in an assigned subject and inspect their versions or details. |
| 55 | Add/Update/Delete Subject Material | Subject Reference Materials | Allows a subject manager to add, update or remove subject reference files shared with authorized classes through private Google Drive storage. Full Lesson, YouTube and advanced RAG remain extensions. |
| 56 | View Subject Question Bank | Subject Quiz Bank | Allows a subject manager to browse versioned multiple-choice questions and answer keys in an assigned subject's Quiz bank. Code questions and execution tests are outside scope. |
| 57 | Manage Subject Quiz Questions | Subject Quiz Bank | Allows a subject manager to create, update or delete multiple-choice Quiz questions in an assigned subject's question bank. Question versions are preserved for published Quiz snapshots; no Code Lab questions are supported. |
| 58 | View Admin Dashboard | System Overview | Allows an administrator to view authorized platform summaries and backup status/history, including the latest successful backup, timestamps, size and sanitized errors. U03 supplies backup information; this view does not grant access to academic content or a production restore action. |
| 59 | View Account List | Account Management | Allows an administrator to view platform accounts and select an account to inspect or manage. |
| 60 | Add Account | Account Management | Allows an administrator to create a school-issued account and assign its initial information and role. The account holder completes account activation separately. |
| 61 | View Account Detail | Account Management | Allows an administrator to view an account's profile information, role and current status. |
| 62 | Update Account Information | Account Management | Allows an administrator to update permitted information and role settings of a selected account. |
| 63 | Change Account Status | Account Management | Allows an administrator to change an account's status and control its eligibility to access the platform. |
| 64 | View Subject List | Administrative Subject Management | Allows an administrator to view the platform's subjects and select a subject to inspect or manage. |
| 65 | Add Subject | Administrative Subject Management | Allows an administrator to create a subject with the required identifying information and subject-manager assignment. |
| 66 | View Subject Detail | Administrative Subject Management | Allows an administrator to view a subject's information, assigned subject manager and related class information. |
| 67 | Update Subject Information | Administrative Subject Management | Allows an administrator to update permitted subject information and its subject-manager assignment, and to archive or reopen the subject. |
| 70 | Manage System Settings | System Settings | Allows an administrator to view and update permitted operational settings, file constraints, backup schedule/retention, activity retention and institutional AI token quotas for eligible teachers and subject managers. Quota is not sold or allocated to students; exhausted shared quota never automatically uses a personal API. Changes are validated, version-checked and audited. AI credentials are managed separately through UC 76. |
| 73 | View Audit Log | Audit Management | Allows an administrator to view and search audit logs by actor, action, affected object, result and time. Audit records are read-only and cannot be edited or deleted. |
| 74 | Manage Personal AI Connection | Personal AI Connection | Allows a student, teacher or subject manager to configure, test, replace or revoke their own Gemini, OpenAI or Claude API connection. Credentials are protected and masked; the connection does not grant additional academic access or shared quota. |
| 75 | Connect/Revoke MCP Agent | External Agent Connection | Allows an authorized user to connect or revoke their own agent through ALAP's authenticated MCP server and follow client-specific setup instructions. Each tool call checks current permissions, scope and state; official submission and grade publication are not initial MCP tools. |
| 76 | Manage Institutional AI Connection | Institutional AI Connection | Allows an administrator to configure, test with synthetic data, update or revoke an institutional Gemini, OpenAI or Claude API connection and select supported models. Shared AI is educator-only; configuration does not grant academic content access. |
| 77 | View AI Usage History | AI Usage Transparency | Allows a user to view their own AI usage, connection source, provider/model when known and token counts with measured, estimated, externally reported or unavailable provenance. Administrators view authorized shared-usage aggregates. Unknown MCP model usage is not displayed as zero. |
| 79 | Request Draft Feedback | Early Draft Feedback | Allows a student to request repeatable, versioned private feedback on their own editable Assignment draft using a personal API or MCP agent. Graded drafts require the teacher's explicit AI-feedback policy; group context is limited to authorized content. Feedback does not enter the official Gradebook. |

## Removed and consolidated IDs

| Former ID | Change |
| --- | --- |
| 08, 09, 10, 11, 68, 69, 72 | Removed AI-credit packages, purchases, payment results and payment history. Institutional quota is not a purchase or wallet. |
| 25, 43 | Removed Code Lab completion/authoring and Judge0 execution. |
| 44, 45 | Consolidated separate Diagram/Group Assignment authoring into UC 42, configured through submissionMode and diagramEnabled. |
| 71, 78 | Consolidated settings editing and educator quota configuration into UC 70 — Manage System Settings. |
| 80 | Consolidated backup status/history into UC 58 — View Admin Dashboard. |
| 81 | Consolidated work-activity review into UC 37 — View Student Submissions. |

UC 26 remains diagram editing within an Assignment. UC 27 remains the group-work/submission workflow. UC 29 concerns submitted Practice work; UC 79 concerns editable drafts, including teacher-approved Graded drafts. Neither is a new assessment type.

Scheduled backup, activity capture and event email are supporting processes; they do not create extra user-facing UCs beyond the current catalog. Backup/activity/email requirements and acceptance criteria are in the [council feature specification](../aidlc-docs/inception/requirements/council-features-2026-10-13.md).

The original 73-use-case table is retained as a [historical baseline](history/use-cases-73.md). Older Construction designs and diagrams still require a separate scope revision; this table does not confirm their implementation.

Consolidation changes the user-facing catalog only: U07 still owns quota, U03 still owns backup, and U11/U14 still provide activity data to U15. Functional requirements and acceptance criteria are retained.
