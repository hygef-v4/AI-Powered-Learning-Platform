# 4.2 Use Cases (UC)

This table contains 40 active MVP use cases, numbered consecutively from 1 to 40, synchronized with the supplied UC wave map and current AI-DLC designs on 2026-10-04. Related actions are merged into Manage use cases. UC 23-27 extend the assignment lifecycle in UC 28; UC 16 inherits common submission behavior from UC 30. Teachers author class assignments; subject managers author subject templates and act as teachers for classes they teach. A versioned question bank supports all five types: Text Essay, Quiz and Code Lab use question rows; Diagram Essay and group assignments copy DOCUMENT skeletons into assignment configuration. Simulation Exam is outside MVP.

| ID | Use Case | Actor | Feature | Use Case Description |
|---|---|---|---|---|
| 1 | Activate Account | All roles | Authentication | Lets a user activate a school-issued account by requesting an emailed OTP, verifying it, then setting a password. Successful activation automatically signs the user in and opens the menu for their role. OTP requests are rate-limited and responses do not reveal whether an account exists. |
| 2 | Sign In | All roles | Authentication | Lets an ACTIVE user sign in with school email and password, with rate limits, temporary locking after repeated failures and safe error messages. Opens Student Menu, Teacher Menu or Admin Menu according to role. |
| 3 | Sign Out | All roles | Authentication | Lets a signed-in user confirm sign-out, end the current session and return to Login. Sessions on other devices remain active. |
| 4 | Recover Password | All roles | Authentication | Lets a user request password recovery, verify an emailed OTP, then set a new password without revealing whether the account exists. Recovery revokes all sessions and clears temporary login locks; it does not automatically sign the user in. |
| 5 | Change Password | All roles | Authentication | Lets a signed-in user change their password after verifying the current password and meeting the policy. Keeps the current session and revokes other sessions. |
| 6 | Manage Profile | All roles | Profile Management | Lets a signed-in user view their own profile and update permitted fields, but not their identifying email or role. |
| 7 | Manage Accounts | Administrator | Account Management | Lets the administrator browse accounts by role or status; create one school-issued account in PENDING without setting its password or sending email; import school-issued accounts from a file into PENDING with per-row success or error results and no email sent; and update account information, highest role and active status with privilege checks, without deleting history. |
| 8 | Manage Subjects | Administrator | Subject Management | Lets the administrator view subjects, managers and classes; create a subject with a unique immutable code; update its information; assign a suitable subject manager; and archive or reopen the subject. Archiving requires all classes to be archived; history is retained. |
| 9 | Manage Classes | Administrator, Teacher, Subject Manager | Class Management | Lets the administrator or assigned subject manager create a class in an ACTIVE subject and assign or change its primary teacher. Authorized managers view/update class details, open/archive/reopen classes, manage enrollments and expiring invite codes, and enable anonymized grade distribution. Teachers manage class groups manually or with a previewed balanced random split, appoint exactly one leader per group and review leader-change requests. A student has at most one unarchived class per subject and one group per class. Class groups serve every group assignment; removing enrollment revokes access while retaining history. |
| 10 | Join Class | Student | Enrollment | Lets a student self-enroll into an open class with a valid invite code, with attempt limits and no class information disclosed for an invalid code. |
| 11 | Manage Content | Subject Manager, Teacher | Content Management | Lets the subject manager create, rename, reorder and archive subject modules shared by all current and future classes. Subject materials are uploaded from Subject Detail; teachers upload class-only materials from Class Detail. Each PDF/DOCX/PPTX file or single YouTube video becomes a material visible immediately within its scope, followed by background text/caption extraction and embedding using the uploader's AI credits. Users track scan status, retry processing and rename, reorder or archive materials within their permissions. There is no direct content authoring, versioning or publication step; playlists, OCR and audio transcription are unsupported. |
| 12 | View Learning Material | Student | Learning Content | Lets an ACTIVE enrolled student view subject and class materials in the modules of an OPEN class, open the View Learning Material popup and download permitted files through a short-lived authorized link. Materials remain viewable before scanning finishes or when text/captions are unavailable. Archived or out-of-scope materials are inaccessible. |
| 13 | Post Class Announcement | Teacher | Class Communication | Lets the teacher post an announcement to an assigned OPEN class; enrolled students receive an in-app notification without email. Posted announcements cannot be edited or hard-deleted; the teacher may hide inappropriate announcements with a reason. A subject manager posts only when also teaching the class. |
| 14 | Comment on Announcement | Student, Teacher | Class Communication | Lets ACTIVE enrolled students and the class teacher comment below an announcement. Shows the two latest comments and a popup with all comments in chronological order. Comments cannot be edited or hard-deleted; the teacher can hide inappropriate comments with a reason. Comments create no notifications; students cannot create separate discussion topics or announcements. |
| 15 | Request Leader Change | Student | Leader Management | Lets a group member submit a reason and optionally propose a replacement leader, or cancel their pending request. The teacher approves by selecting a new leader or rejects with a reason; relevant members receive the decision. Only one request per group may be pending. Changing the leader does not change section claims. |
| 16 | Submit Group Document | Student | Group Submission | Lets group members view a shared document and live updates. Sections come from the teacher's skeleton; students, including the leader, cannot add, delete, rename or move them or edit teacher blocks. The leader assigns sections and members claim available sections. One member holds each section and edits a private autosaved draft in a popup showing only its heading branch. Done merges content into the shared document and releases the claim. The leader may submit or resubmit before the deadline, with a warning for unfinished sections; unfinished drafts are excluded from manual submission. There is no REVIEW stage or section commenting. At the final deadline or retirement, the system submits the latest saved content including claimed drafts, closes the document and redirects all viewers to Submitted Assignment. Inherits common submission behavior from UC 30. |
| 17 | Grade Group Document | Teacher | Group Grading | Lets the teacher grade the submitted shared document as a DOCUMENT assignment using each part's rubric, manually or with an AI proposal. The document score is the sum of part scores; each member's contribution score initially equals it and may be adjusted manually with an optional reason before finalization. There is no separate integration score, automatic combining formula or AI grading of individual contribution. Resubmission resets evaluations to pending and preserves previous scores in history. Finalization/publication follow UC 34; changing finalized or published scores requires a reason. |
| 18 | View Statistics | Administrator | System Statistics | Shows statistics directly on Admin Menu when the administrator opens it: account counts by role/status, subject and class counts by status, and ACTIVE enrollment counts, with the counting time. Results are calculated on demand, contain no personal data and are inaccessible to other roles. There is no student learning-overview dashboard or separate statistics popup. |
| 19 | Access Enrolled Class | Student | Learning Access | Lets a student view enrolled classes on Student Menu, open an authorized Class Detail with teacher information, modules, materials and assigned work, and open My Group to see members, the leader and group documents. Access requires ACTIVE enrollment; out-of-scope data is inaccessible. |
| 20 | Manage Rubrics | Teacher, Subject Manager | Assignment Rubrics | Lets an authorized author create checklist rubrics inside Assignment Editor or Template Editor: one per Text Essay question and one per skeleton part for Diagram Essay or group assignments. Scores are calculated from checklist items; Quiz and Code Lab have no rubrics. Question Bank permits viewing/editing existing rubrics without separate creation or cloning. Editing creates a new version: only a DRAFT owner is repointed, while reviewed/published assignments keep pinned versions. Cloning, versioning or copying an assignment clones its rubrics; two assignments do not share a rubric. |
| 21 | Manage Templates | Subject Manager | Template Management | Lets the current subject manager view, create, edit, approve, publish and delete templates of the assigned subject, including those created by a previous manager. After selecting type and mode, the author works manually or previews AI-proposed questions or a skeleton with guidance and per-part rubric suggestions grounded in subject materials. Published versions are read-only; editing creates a new version. Teachers copy released templates into independent class drafts. Templates have no schedule or attached groups; deleting a released template prevents further copying without changing existing copies. |
| 22 | Manage AI Service | Administrator | AI Administration | Lets the administrator view AI requests, latency, failures, tokens and estimated cost by day, task and model; select allowed models per task; set daily cost/request limits; and enable or disable tasks or the whole service. Reports do not expose prompt content. Credit packages and monthly free-credit grants are fixed deployment configuration and cannot be edited here. |
| 23 | Manage Text Essay | Teacher, Subject Manager | Assignment Creation | Extends UC 28 for GRADED or PRACTICE Text Essay. Lets the author write instructions and create, search, bulk-import or reuse versioned essay questions, with one rubric per question. Each question's maximum score equals its rubric total; the assignment score is the sum. Student responses support basic text formatting without tables, images or diagrams and have no word/line limit apart from technical safety limits. Reviewing requires a rubric for every question; published assignments keep pinned question versions. |
| 24 | Manage Quiz | Teacher, Subject Manager | Assignment Creation | Extends UC 28 for GRADED or PRACTICE multiple-choice quizzes. Lets the author create, search, bulk-import or reuse versioned single-answer/multiple-answer questions and configure answer keys, points, question/answer shuffling, time limits and score/answer visibility. Scoring is automatic without a rubric; multiple-answer questions score only when the answer set is exactly correct. Subject managers prepare subject templates; teachers publish class assignments. |
| 25 | Manage Diagram Essay | Teacher, Subject Manager | Assignment Creation | Extends UC 28 for GRADED or PRACTICE Diagram Essay using the DOCUMENT editor. Requires a teacher skeleton authored manually, copied from a bank skeleton, imported from DOCX with preview, or proposed by AI as headings, guidance and per-part rubric suggestions without generated diagrams. Replacing the skeleton with AI output requires preview and confirmation. Leaf headings define parts, each with one rubric; a skeleton without headings is one part. The copied skeleton is stored in assignment configuration without assignment-question rows. Supports embedded Draw.io, XML validation and required diagram types/counts; diagrams appear as previews rather than raw XML. |
| 26 | Manage Code Lab | Teacher, Subject Manager | Assignment Creation | Extends UC 28 for GRADED or PRACTICE Code Lab. Lets the author create, search, import or reuse versioned coding questions and configure language, starter files, sample solutions, public/hidden scored tests, time and memory limits. The sample solution must pass every test for the current content before review; changes require re-verification. Code runs in an isolated sandbox and is scored automatically from tests without a rubric. |
| 27 | Manage Group Assignment | Teacher, Subject Manager | Assignment Creation | Extends UC 28 for GRADED group assignments. The teacher authors a mandatory skeleton manually, from the question bank or from a previewed AI skeleton with per-part rubric suggestions. Leaf headings define fixed work sections, each with one rubric. Subject managers author group templates through UC 21. Class publication requires valid class groups under UC 9. When the assignment opens, each group receives a shared document, including groups created later. The leader assigns work under UC 16; the teacher monitors groups and releases claims but does not assign sections or permit students to change the skeleton structure. |
| 28 | Manage Assignments | Teacher, Subject Manager | Assignment Management | Lets the teacher view class assignments and create a draft after selecting type and valid GRADED/PRACTICE mode, starting empty or copying a matching released subject template or assignment from another class they teach. Questions are selected manually or randomly from the bank with preview, authored privately or drafted by AI; Diagram Essay/group assignments use skeletons instead of assignment-question rows. The teacher previews, reviews and publishes with opening/closing times, late-submission policy and attempt limits. Supports schedule changes, retirement, cloning, new versions and version comparisons without changing historical data. Copies include independent rubrics and omit schedules, attempts, submissions and grades. Subject managers/administrators only view class assignments unless also teaching that class. UC 23-27 extend this lifecycle; subject template publication follows UC 21. |
| 29 | View Assigned Work | Student | Assignment Delivery | Lets a student view assignments on Assignment List and open Assignment Overview with instructions, mode, rubric, deadline, remaining attempts and previous work. Work not yet open is hidden. Retired work remains visible only when the student already has a published score or Practice result, for viewing without further work or submission. |
| 30 | Submit Assignment | Student | Assignment Submission | Lets a student start or resume one active attempt with autosave: write a Text Essay, answer a quiz, edit a Diagram Essay with Draw.io and previewed DOCX import, or write code and run public tests. Submission validates and freezes work, marks lateness and returns a receipt; time expiry, the final deadline or retirement submits the latest saved draft automatically. Teacher blocks remain locked and submitted attempts cannot be edited. GRADED work enters evaluation with automatic Quiz/Code Lab scoring; PRACTICE Quiz/Code Lab returns Practice results. Submitting a Practice Text/Diagram Essay does not automatically request AI grading. Common submission behavior is inherited by UC 16 for groups. |
| 31 | Review Attempts | Student | Submission | Lets a student review their own immutable attempts, open Submitted Assignment at the latest attempt and navigate previous/next attempts, view the receipt and permitted scores/answers, and download document work as DOCX. The last submission is the official GRADED attempt. A new attempt can start while time and attempts remain without overwriting previous attempts. |
| 32 | Review Submissions | Teacher | Submission Review | Lets the teacher view GRADED submissions in Grading Queue, filter by grading status or lateness and open Grading Workspace to inspect work, rubrics, attempts and automatic Quiz/Code Lab results. The final submitted attempt is official. PRACTICE stays outside this queue; sandbox errors are shown as not yet graded. |
| 33 | Grade Submissions | Teacher | Grading | Lets the teacher grade by checking rubric items, with calculated question/part scores and optional feedback, or request and use an AI proposal through UC 40. The teacher can edit the checklist and explanation; differing from the proposal does not require a reason. Supports batch AI proposals after checking credits for the entire batch, followed by individual confirmation. Grading Workspace navigates submissions and advances after saving. Saves a draft with grader/time without publishing; students cannot see drafts or AI proposals. Changing an automatic score requires a reason. |
| 34 | Finalize Grades | Teacher | Grade Finalization | Lets the teacher finalize eligible draft grades individually or in a batch with per-item results, then publish a submission or selected finalized submissions in a batch. Students receive notifications without scores in notification/email content. Changing finalized or published grades requires a reason and retains history; differing from an AI proposal before finalization does not. |
| 35 | View Grades | Student, Teacher, Subject Manager | Gradebook | Lets students view their own published GRADED scores and feedback beside assignments on Assignment List. The class teacher or assigned subject manager opens Gradebook grouped by student, expanding each student to see last-submission assignment scores and submission/grading status. Gradebook has no overall score or PRACTICE results; administrators cannot view it. Students cannot see drafts, AI proposals or other students' scores. There is no separate My Grades page. |
| 36 | Monitor Submissions | Teacher, Subject Manager | Submission Monitoring | Lets the teacher inspect submission progress in Check Progress, with group work counted by group. Automatically reminds unsubmitted students once, 24 hours before the deadline. Lets the class teacher or assigned subject manager view grade-change history and export scoped CSV/XLSX gradebooks containing GRADED work without overall scores, Practice results or AI proposals. The teacher enables anonymized grade distribution on Class Detail; students see it beside assignments on Assignment List only with at least 20 published scores, hiding intervals with fewer than five students. |
| 37 | Buy AI Credits | Student, Teacher, Subject Manager, Administrator | Payment | Lets ACTIVE users view their own balance, fixed packages, purchase history and credit usage, then pay via PayOS and track the result. Verified payment or automatic reconciliation grants purchased credits exactly once; redirects cannot grant credits. All four roles receive the same fixed monthly grant, spent before purchased credits. Students use credits only for their own Practice Text/Diagram Essay grading through UC 40. Purchased credits do not expire; refunds are outside MVP. |
| 38 | View Notifications | All roles | Notification | Lets users receive scoped in-app notifications for enrollment, assignments, deadlines, groups/leaders, group submissions, grades, payments and class announcements; view them and mark one or all as read. Enrollment, assignment opening, grades and deadline reminders may also send email according to preferences and the daily limit. Announcements are in-app only; comments create no notifications. Notifications contain no scores, are deduplicated and retained for 180 days; delivery failure does not undo the original action. |
| 39 | View Audit Log | Administrator | Audit | Lets the administrator filter audit events by actor, action, object, result and time and open read-only details. The query itself is audited with its filters. Records cannot be edited/deleted; other roles cannot access them. |
| 40 | Grade with AI | Student, Teacher | AI Grading | Lets a student explicitly press Grade with AI on a submitted PRACTICE Text/Diagram Essay attempt. Checks credits when pressed and returns at most one valid score/feedback per attempt. Insufficient credits leave the same attempt available after purchase. Lets the teacher request rubric-based AI proposals for GRADED individual/shared group documents and accept or edit them under UC 33/17; AI never finalizes or publishes grades. Requests time out after five minutes, release reserved credits and permit retry; late results are discarded. Practice stays outside official gradebooks, distributions and exports; students cannot see teacher AI proposals. |

## Merged IDs

Previous IDs refer to the 64-case draft that this table replaces. These mappings preserve historical traceability, not unchanged behavior or actors: UC 18 now represents administrator statistics; student class, assignment and grade access is covered by UC 19, UC 29 and UC 35.

| New ID | Use Case | Previous IDs |
|---|---|---|
| 6 | Manage Profile | 6 |
| 7 | Manage Accounts | 7, 8 |
| 8 | Manage Subjects | 9 |
| 9 | Manage Classes | 10, 11, 12, 13, 14, 21, 22, 24 |
| 11 | Manage Content | 16, 17 |
| 18 | View Statistics | 28, 59 |
| 20 | Manage Rubrics | 30 |
| 21 | Manage Templates | 33 |
| 22 | Manage AI Service | 34 |
| 23 | Manage Text Essay | 36 |
| 24 | Manage Quiz | 31, 37 |
| 25 | Manage Diagram Essay | 38 |
| 26 | Manage Code Lab | 39 |
| 27 | Manage Group Assignment | 25, 40 |
| 28 | Manage Assignments | 32, 35, 41, 48, 49, 50 |
| 30 | Submit Assignment | 43, 44, 45, 46 |
| 33 | Grade Submissions | 52, 53 |
| 34 | Finalize Grades | 54, 55 |
| 36 | Monitor Submissions | 57, 58, 60 |

## Legacy UC codes

Design files written before 2026-09-29 used domain codes such as `UC-IAM-01`. Live Inception and Construction documents now use the IDs above; this table keeps older plans, question files and `audit.md` traceable.

| Legacy codes | New ID |
|---|---|
| UC-IAM-01 … UC-IAM-05 | 1 … 5 |
| UC-IAM-06, 07 | 6 |
| UC-IAM-08 … UC-IAM-12 | 7 |
| UC-CAT-01 … UC-CAT-04 | 8 |
| UC-CAT-05 … UC-CAT-12; UC-GRP-01, 02, 04 | 9 |
| UC-CAT-13 | 10 |
| UC-CNT-01, 02, 03, 08 | 11 |
| UC-CNT-04 | 12 |
| UC-CNT-06 | 13 |
| UC-CNT-07 | 14 |
| UC-GRP-03 | 15 |
| UC-GRP-06, 07 | 16 |
| UC-GRP-08 | 17 |
| UC-LRN-01, UC-RPT-02 | 18 |
| UC-LRN-02 | 19 |
| UC-QBK-01 | 20 |
| UC-AIG-01 | 28 |
| UC-AIG-02, 03 | 21, 22 |
| UC-ASM-02 | 23 |
| UC-QBK-02, UC-ASM-03 | 24 |
| UC-ASM-04 | 25 |
| UC-ASM-05 | 26 |
| UC-ASM-06; UC-GRP-05 | 27 |
| UC-ASM-01, 07, 15, 16, 17 | 28 |
| UC-ASM-09 | 29 |
| UC-ASM-10 … UC-ASM-13 | 30 |
| UC-ASM-14 | 31 |
| UC-GRD-01 | 32 |
| UC-GRD-02, 03 | 33 |
| UC-GRD-04, 05 | 34 |
| UC-GRD-06 | 35 |
| UC-GRD-07, UC-RPT-01, UC-RPT-03 | 36 |
| UC-PAY-01 | 37 |
| UC-OPS-01 | 38 |
| UC-OPS-02 | 39 |
| UC-ASM-19 | 40 |
| UC-ASM-18 (Simulation Exam), UC-PAY-02 (manual reconciliation) | Retired, not reused |
