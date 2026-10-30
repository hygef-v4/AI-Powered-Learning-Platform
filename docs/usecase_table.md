# 4.2 Use Cases (UC)

**Current revision: 2026-10-14 — 66 active use cases, renumbered by unit.** UC 79 (ID cũ) was removed, UC 26 (ID cũ) was consolidated into Assignment completion, and individual/group Assignment authoring was separated by user decision. UC 53 and UC 15 are reinstated for their original group-authoring and settings-editing responsibilities. Institutional AI API configuration is included in UC 14 — View Settings and UC 15 — Edit Setting; UC 76/82 (ID cũ) are consolidated and retired. This table replaces the former 73-use-case table. The [Inception catalog](../aidlc-docs/inception/requirements/use-cases.md) and current Construction designs/plans are synchronized to this table. Jira, Google Reports, diagram files and actual code/contracts/migrations remain pending; documentation synchronization is not runtime evidence.

The platform supports two assessment types: **ASSIGNMENT** and **QUIZ**. Assignment supports individual/group submission and optional Draw.io diagrams. Learning materials include private reference files and links to educational YouTube videos that learners can open or watch. AI uses institutional educator quotas, personal APIs or external agents through MCP; AI credits are not sold. Backup, work-activity signals and important-event email are included in the proposed scope. These are requirements, not implementation evidence.

Active IDs were renumbered 01–66 in unit order (U01 → U15) on 2026-10-14; see the old → new mapping at the end. Retired IDs keep their old numbers and are marked "(ID cũ)" where referenced. Decided on 2026-10-15: Quizzes are Practice only and Group Assignments are Graded only.

| ID | Use Case | Feature | Use Case Description |
| --- | --- | --- | --- |
| 01 | Activate Account | Authentication | Allows a user to activate a school-issued account by verifying an email OTP and setting an initial password. |
| 02 | Login | Authentication | Allows a user with an active account to sign in using their email and password. |
| 03 | Logout | Authentication | Allows a signed-in user to end their session and return to the login screen. |
| 04 | Reset Password | Authentication | Allows a user to reset their password after verifying an OTP sent to their email. |
| 05 | Change Password | Authentication | Allows a signed-in user to change their password after verifying the current password. |
| 06 | View Profile Information | Profile Management | Allows a user to view their own profile information, including account details and role. |
| 07 | Update Profile Information | Profile Management | Allows a user to update permitted profile fields, such as display name and phone number. |
| 08 | View Account List | Account Management | Allows an administrator to view platform accounts and select an account to inspect or manage. |
| 09 | Add Account | Account Management | Allows an administrator to create a school-issued account and assign its initial information and role. |
| 10 | View Account Detail | Account Management | Allows an administrator to view an account's profile information, role and current status. |
| 11 | Update Account Information | Account Management | Allows an administrator to update permitted account information and role settings. |
| 12 | Change Account Status | Account Management | Allows an administrator to change an account's status and control its access to the platform. |
| 13 | View Audit Log | Audit Management | Allows an administrator to search and view read-only audit logs by user, action, affected object, result and time. |
| 14 | View Settings | System Settings | Allows an administrator to view operational settings, backup and activity-retention policies, educator AI quotas and institutional API configuration with masked keys. |
| 15 | Edit Setting | System Settings | Allows an administrator to update operational settings and educator AI quotas, and configure, test, replace or revoke institutional AI API keys and models. |
| 16 | View Subject List | Administrative Subject Management | Allows an administrator to view subjects and select a subject to inspect or manage. |
| 17 | Add Subject | Administrative Subject Management | Allows an administrator to create a subject and assign its subject manager. |
| 18 | View Subject Detail | Administrative Subject Management | Allows an administrator to view a subject's information, assigned subject manager and related classes. |
| 19 | Update Subject Information | Administrative Subject Management | Allows an administrator to update subject information or its subject-manager assignment, and archive or reopen the subject. |
| 20 | View Enrolled Classes | Student Class Access | Allows a student to view their enrolled classes and select a class to access. |
| 21 | View Enrolled Class Detail | Student Class Access | Allows a student to view an enrolled class's information and access its materials, assignments, announcements and group information. |
| 22 | View Assigned Class List | Teacher Class Access | Allows a teacher to view their assigned classes and select a class to manage. |
| 23 | View Assigned Class Detail | Teacher Class Access | Allows a teacher to view an assigned class's information and access its teaching functions. |
| 24 | View Managed Subject Classes | Subject Class Management | Allows a subject manager to view classes belonging to their assigned subjects. |
| 25 | View Managed Class Detail | Subject Class Management | Allows a subject manager to view a managed class's information, assigned teacher and enrolled students. |
| 26 | Create Class | Subject Class Management | Allows a subject manager to create a class within a subject they manage. |
| 27 | Assign Teacher To Class | Subject Class Management | Allows a subject manager to assign or change the teacher responsible for a managed class. |
| 28 | Add/Remove Class Students | Class Enrollment Management | Allows a subject manager to add students to or remove students from a managed class while preserving their learning history. |
| 29 | Edit Class Information | Subject Class Management | Allows a subject manager to update class information, open or archive a class, and control anonymous grade-distribution visibility. |
| 30 | View Managed Subject | Subject Management | Allows a subject manager to view their assigned subjects and access each subject's information and management functions. |
| 31 | Manage Personal AI API | Personal AI API Configuration | Allows a student, teacher or subject manager to configure, test, replace or revoke their own Gemini, OpenAI or Claude API key. |
| 32 | Connect/Revoke MCP Agent | External Agent Connection | Allows an authorized user to connect or revoke their own MCP agent and access platform tools within their permissions. |
| 33 | View AI Usage History | AI Usage Transparency | Allows a user to view their own AI usage, source, provider/model and available token counts. Administrators can view permitted institutional usage summaries. |
| 34 | View Teacher Assignment List | Assignment Lifecycle | Allows an assigned teacher to view and manage a class's individual and group Assignments, and a subject manager to view and manage subject-level individual Assignments, including publication, version and copy status. |
| 35 | Create/Update/Delete Individual Assignment | Individual Assignment Authoring | Allows an assigned teacher or authorized subject manager to create, update or delete individual Assignments with document sections, rubrics and optional Draw.io diagrams. |
| 36 | Add/Update/Delete Rubric | Assignment Rubrics | Allows an authorized educator to add, update or delete document-section rubrics while an Assignment is in draft. Rubrics are locked after publication. |
| 37 | View Assignment List | Student Assignment Access | Allows a student to view available individual and group Assignments in their enrolled classes. |
| 38 | View Assignment Detail | Student Assignment Access | Allows a student to view an Assignment's instructions, rubric, submission mode, optional diagram requirements, deadline and applicable policies. |
| 39 | Complete Individual Assignment | Assignment Workspace | Allows a student to edit and save an individual Assignment, optionally use Draw.io when enabled, and submit their work. |
| 40 | View Submission History | Student Submission Review | Allows a student to view previous Assignment submissions, submitted content, submission times and permitted scores or feedback. |
| 41 | Request Practice Feedback | Private Practice Feedback | Allows a student to request private AI feedback on their own submitted Practice Assignment through a personal API or MCP agent, without creating official grades. |
| 42 | View Student Submissions | Teacher Work Review | Allows an assigned teacher to view student submissions, grading status and available work-activity records when monitoring is enabled. |
| 43 | Grade Submission With AI | AI-Assisted Grading | Allows an assigned teacher to use an institutional API, personal API or MCP agent to generate rubric-based scores and feedback, then review and confirm the result. |
| 44 | Grade Submissions Manually | Teacher Grading | Allows an assigned teacher to manually grade submissions and record scores and feedback. |
| 45 | View/Export GradeBook | Gradebook | Allows an assigned teacher to view a class gradebook and export Assignment scores and submission or grading status. |
| 46 | View Subject Question Bank | Subject Quiz Bank | Allows a subject manager to browse multiple-choice Quiz questions and answer keys in an assigned subject's question bank. |
| 47 | Create/Update/Delete Subject Question | Subject Quiz Bank | Allows a subject manager to create, update or delete multiple-choice Quiz questions in an assigned subject's question bank. |
| 48 | Create/Update/Delete Quiz | Quiz Authoring | Allows an assigned teacher (class Quizzes) or a subject manager (subject-level Quizzes) to create, update or delete standalone multiple-choice Quizzes and configure answers, scoring, attempt limits and activity monitoring. |
| 49 | View Quiz Practice History | Student Quiz Access | Allows a student to view their quiz practice history and select a quiz to inspect. |
| 50 | View Quiz Practice Detail | Student Quiz Access | Allows a student to view a quiz's information and available practice attempts. |
| 51 | Take Quiz | Student Quiz Completion | Allows a student to answer quiz questions and submit an attempt for automatic scoring. |
| 52 | View Quiz Result | Student Quiz Review | Allows a student to view the score and permitted feedback for a completed quiz attempt. |
| 53 | Create/Update/Delete Group Assignment | Group Assignment Authoring | Allows an assigned teacher to create, update or delete class group Assignments with shared document sections, rubrics and optional Draw.io diagrams. |
| 54 | View My Group | Student Group | Allows a student to view their group members, leader and group-assignment documents in a class. |
| 55 | Request Leader Change | Student Group | Allows a group member to request a leader change, provide a reason and optionally propose a replacement. |
| 56 | Complete Group Assignment | Group Assignment Workspace | Allows group members to work on authorized document sections and optionally use Draw.io when enabled. The group leader submits the completed Assignment. |
| 57 | Create/Update/Delete Class Group | Class Group Management | Allows an assigned teacher to create class groups manually or randomly, add, move or remove members, appoint group leaders, and approve or reject leader-change requests. |
| 58 | View Subject Materials | Subject Reference Materials | Allows a subject manager to view learning files and educational YouTube video links in an assigned subject. |
| 59 | Add/Update/Delete Subject Material | Subject Reference Materials | Allows a subject manager to add, update or delete subject learning files and educational YouTube video links shared with authorized classes. |
| 60 | View Uploaded Learning Materials | Class Reference Materials | Allows an assigned teacher to view a class's learning files and educational YouTube video links. |
| 61 | Add/Update/Delete Learning Material | Class Reference Materials | Allows an assigned teacher to add, update or delete class learning files and educational YouTube video links. |
| 62 | View Learning Material | Reference Materials | Allows an authorized user to preview or download learning files from Google Drive and open educational YouTube video links. |
| 63 | View Class Announcements | Class Communication | Allows authorized students and teachers to view announcements from their classes. |
| 64 | Create/Update/Delete Announcement | Class Communication | Allows an assigned teacher to create, update or delete class announcements and select email delivery for important new announcements. |
| 65 | View Notifications | Notification | Allows a user to view their own notifications, open related information and mark notifications as read. |
| 66 | View Admin Dashboard | System Overview | Allows an administrator to view platform statistics and backup status or history, including the latest successful backup and reported errors. |

## Removed and consolidated IDs

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

Scheduled backup, activity capture and event email are supporting processes; they do not create extra user-facing UCs beyond the current catalog. Backup/activity/email requirements and acceptance criteria are in the [council feature specification](../aidlc-docs/inception/requirements/council-features-2026-10-13.md).

The original 73-use-case table is retained as a [historical baseline](history/use-cases-73.md). Historical Construction baselines and diagram files retain older scope. Current Construction designs/plans are synchronized; this table does not confirm implementation.

Consolidation changes the user-facing catalog only: U07 still owns quota, U13 still owns institutional AI connection data and protected credentials, U03 still owns backup, and U11/U14 still provide activity data to U15. Functional requirements and acceptance criteria are retained.

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
| 38 | 43 |
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

IDs 01–66 remain unchanged. UC14/15 do not expose editable activity.retentionDays: source retains activity for one day. UC26 creates DRAFT; UC29 allows OPEN without teacher. UC32 is for Student/Teacher/Subject Manager, not Admin; owner can reveal an ACTIVE credential when ciphertext exists. UC31 personal API remains available to Student. UC48 supports edit OPEN and retire/reopen under version/container guards; review has not been removed. UC55 request/rejection reasons are optional, max 1,000 characters. UC58–62 include U13 material modules and scoped Quiz attachment; UC66 includes /activity. Admin institutional key reveal is a documented AS-BUILT exception to prior no-readback design. See [decision-register.md](../aidlc-docs/inception/reverse-engineering/decision-register.md).
