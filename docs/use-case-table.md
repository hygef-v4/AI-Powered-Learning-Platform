# 4.2 Use Cases (UC)

This table contains 40 active MVP use cases, numbered consecutively from 1 to 40. Same-actor view, create and update actions are merged into a single "Manage" use case, and related use cases are merged where listed in [Merged IDs](#merged-ids). A subject manager can add YouTube sources to subject materials; a teacher can add them to class content. Simulation Exam is outside the MVP. Quiz questions are managed, searched, imported and reused inside UC 25 Manage Quiz rather than in a separate question-bank use case.

| ID | Use Case | Actor | Feature | Use Case Description |
|---|---|---|---|---|
| 1 | Activate Account | All roles | Authentication | Lets a user request activation for a school-issued account using a school email, verify an emailed OTP and set a password. The user can request another OTP within the rate limit; responses do not reveal whether an account exists. |
| 2 | Sign In | All roles | Authentication | Lets a user sign in with the school email and password, with account status checks, attempt limits and safe error messages. |
| 3 | Sign Out | All roles | Authentication | Lets a signed-in user end the current session and revoke the related credentials. |
| 4 | Recover Password | All roles | Authentication | Lets a user request a password reset, verify an OTP and set a new password without revealing whether the account exists. |
| 5 | Change Password | All roles | Authentication | Lets a signed-in user change the password after verifying the current one and meeting the password policy. |
| 6 | Manage Profile | All roles | Profile Management | Lets a signed-in user view their own profile and update permitted fields, but not their identifying email or role. |
| 7 | Manage Accounts | Administrator | Account Management | Lets the administrator browse accounts by role or status; create one school-issued account in PENDING without setting its password or sending email; import school-issued accounts from a file into PENDING with per-row success or error results and no email sent; and update account information, highest role and active status with privilege checks, without deleting history. |
| 8 | Manage Subjects | Administrator | Subject Management | Lets the administrator view subjects with their details, manager and classes; create a subject with a valid code; update its information without losing history; and assign an account with a suitable role as its subject manager. |
| 9 | Manage Classes | Administrator, Teacher, Subject Manager | Class Management | Lets an authorized user view the class list and class details within the assigned scope. The administrator creates a class that belongs to exactly one subject and is the only one who assigns or changes its one primary teacher. The class manager updates class information, opens or archives the class while keeping its content and learning history, views current or past enrollments, enrolls a student only once with a notification, removes access while keeping learning history, and enables, disables or regenerates the class invite code. In the class student list, the teacher views groups, leaders, members and contribution status; creates or updates groups manually or randomly splits ungrouped students into balanced groups, adds or removes members and appoints exactly one leader; and approves or rejects a leader-change request with a notification of the decision. Class groups are used by every group assignment of the class. |
| 10 | Join Class | Student | Enrollment | Lets a student self-enroll into an open class with a valid invite code, with attempt limits and no class information disclosed for an invalid code. |
| 11 | Manage Content | Subject Manager, Teacher | Content Management | Lets the subject manager view, upload, update, archive and index learning materials of an assigned subject for RAG, and lets the teacher view, create, upload, update, reorder, archive and publish valid content to enrolled students in an assigned class. Both can add a YouTube video or playlist as a RAG source, using existing captions without transcribing audio, and track timestamped transcript processing within their subject or class scope. |
| 12 | Access Lesson | Student | Learning Content | Lets an enrolled student open published content and download the permitted files. |
| 13 | Post Class Announcement | Teacher | Class Communication | Lets the teacher post an announcement to an assigned class. |
| 14 | Discuss in Class Q&A | Student, Teacher | Class Communication | Lets class members post questions and replies within the scope of their class. |
| 15 | Request Leader Change | Student | Leader Management | Lets a member submit a reason and a proposed new leader for the teacher to review. |
| 16 | Submit Group Document | Student | Group Submission | Lets group members work on the shared document of a group assignment and view it as it updates. The leader can add, edit, reorder or remove detailed sub-sections under the teacher's main sections and assign any section to a member; a member can also claim an open section. The member works on the section in a private draft and marks it Done so the completed content appears in the shared document in real time. Only one member can hold a section at a time. When every section is Done, the document enters REVIEW so all members can read it and comment; reopening a section returns the document to work. The leader submits only from REVIEW; the system submits the current document automatically when the deadline passes. |
| 17 | Grade Group Document | Teacher | Group Grading | Lets the teacher review the submitted shared document and each member's contribution, assess integration and consistency, and decide each member's final grade without an automatic formula. |
| 18 | View Learning Overview | Student | Learning Overview | Lets the student see enrolled classes, upcoming assignments, notifications, submission status and published grades. An anonymized class distribution is shown only when its privacy conditions are met. |
| 19 | Access Enrolled Class | Student | Learning Dashboard | Lets the student open an enrolled class with its content, assignments and groups within the permitted scope. |
| 20 | Manage Rubrics | Teacher, Subject Manager | Rubric Bank | Lets an authorized user view, create, update, clone and version rubrics within the permitted scope. |
| 21 | Create Assignment with AI | Teacher | AI Creation | Lets the teacher ask the AI service to create an assignment draft from class content, review the sources, edit, accept or discard the result before publishing. |
| 22 | Create Template with AI | Subject Manager | AI Creation | Lets the subject manager ask the AI service to create a subject-level template or question draft grounded in subject RAG, review its sources, edit, accept or discard the result. |
| 23 | Manage AI Service | Administrator | AI Administration | Lets the administrator view AI usage, quota and cost, configure the allowed model and enable or disable the AI service, and manage AI credit packages and the monthly free-credit grant for Teacher, Subject Manager and Administrator accounts. |
| 24 | Manage Text Essay | Teacher, Subject Manager | Assignment Creation | Lets an authorized user view, create, update, clone and preview a Text Essay in GRADED or PRACTICE mode with instructions, limits and a rubric. |
| 25 | Manage Quiz | Teacher, Subject Manager | Assignment Creation | Lets an authorized user view, create, update, clone and preview a GRADED or PRACTICE multiple-choice quiz with its single-answer and multiple-answer questions, answer keys and scoring rules. Questions are versioned and can be searched, imported in bulk from a file and reused across quizzes within the permitted scope; editing a question used by a published quiz creates a new version. The subject manager prepares subject-level quizzes in either mode; the teacher publishes quizzes to an assigned class. Students only take assigned PRACTICE quizzes and never create quizzes. |
| 26 | Manage Diagram Essay | Teacher, Subject Manager | Assignment Creation | Lets an authorized user view, create, update, clone and preview a GRADED or PRACTICE Diagram Essay using the DOCUMENT editor, an outline, optional DOCX import and embedded Draw.io diagrams with XML validation. |
| 27 | Manage Code Lab | Teacher, Subject Manager | Assignment Creation | Lets an authorized user view, create, update, clone and preview a GRADED or PRACTICE Code Lab, configure the coding problem, language, quota and test cases, and run the sample solution in the sandbox. |
| 28 | Manage Assignments | Teacher, Subject Manager | Assignment Management | Lets an authorized user view the assignment list and assignment details within the assigned scope. The teacher creates and updates a GRADED group document assignment with a rubric and prepares its main sections; the group leader can then add detailed sub-sections and assign sections to members (UC 16), and the teacher or group leader can release a section claim when needed. A group assignment is published only when the class groups are valid. The teacher configures a valid GRADED or PRACTICE mode, schedule and attempts, then previews, approves and publishes an assignment to an assigned class. An authorized user can clone, create a new version of or retire an assignment without changing historical data. The subject manager publishes a versioned subject template, including PRACTICE or GRADED quizzes, and a teacher copies it into an independent class draft. A teacher can copy assignment content and its rubric between classes they teach without copying schedules, attempts, submissions or grades. |
| 29 | View Assigned Work | Student | Assignment Delivery | Lets the student view assigned work with its requirements, rubric, deadline, attempts and status. |
| 30 | Submit Assignment | Student | Assignment Submission | Lets the student complete and submit an individual assignment while it is open, with autosave and a submission receipt: write an open-ended Text Essay; answer a quiz whose closed questions are auto-scored against the answer-key version; write a Diagram Essay document, preview and import DOCX into the active attempt and draw on the embedded Draw.io canvas, submitting the full Draw.io XML; or write code, run it in the sandbox and submit the source within the assignment limits. |
| 31 | Review Attempts | Student | Submission | Lets the student view their attempts and create a new attempt while time and attempts remain. |
| 32 | Review Submissions | Teacher | Submission Review | Lets the teacher view GRADED submissions, student work, rubric, attempts and auto-scored results of an assigned class; PRACTICE is outside the teacher grading queue. |
| 33 | Grade Submissions | Teacher | Grading | Lets the teacher enter the score and feedback for an individual submission manually, or request, review, accept or override an AI grading proposal; the AI never decides the final score. |
| 34 | Finalize Grades | Teacher | Grade Finalization | Lets the teacher confirm the final score and publish the score and feedback to the right student, or check eligibility and finalize many valid scores in a class at once. |
| 35 | View Grades | Student | Gradebook | Lets the student view their own published final score and feedback. |
| 36 | Monitor Submissions | Teacher, Subject Manager, Administrator | Submission Monitoring | Lets the teacher view submission status in an assigned class; the system automatically reminds students who have not submitted 24 hours before the deadline. The teacher can enable an anonymized class grade distribution that students see in their learning overview when its privacy conditions are met. An authorized user can view the gradebook together with the change history, actor, timestamp and reason, and export a CSV or XLSX gradebook for a class or assignment containing only data within their permitted scope. |
| 37 | Buy AI Credits | Student, Teacher, Subject Manager, Administrator | Payment | Lets an ACTIVE Student, Teacher, Subject Manager or Administrator view AI credit packages, pay and track payment status. Verified PayOS payment grants purchased credits exactly once. Student credit use is limited to Practice Text/Diagram Essay grading. |
| 38 | View Notifications | All roles | Notification | Lets a user receive and read notifications about class posts and Q&A, assignments, deadlines, groups, grades and payments within their scope. |
| 39 | View Audit Log | Administrator | Audit | Lets an authorized administrator view audit events by actor, action, object, result and time; audit records cannot be edited or deleted. |
| 40 | Grade Practice with AI | Student | Practice Feedback | Lets a student receive one AI score and feedback for each submitted PRACTICE Text or Diagram Essay attempt when enough credits are available. Without enough credits the attempt is submitted without an AI score; the student must submit a new attempt after buying credits. Practice results stay outside the official gradebook. |

## Merged IDs

Previous IDs refer to the 64-case draft that this table replaces.

| New ID | Use Case | Previous IDs |
|---|---|---|
| 6 | Manage Profile | 6 |
| 7 | Manage Accounts | 7, 8 |
| 8 | Manage Subjects | 9 |
| 9 | Manage Classes | 10, 11, 12, 13, 14, 21, 22, 24 |
| 11 | Manage Content | 16, 17 |
| 18 | View Learning Overview | 28, 59 |
| 20 | Manage Rubrics | 30 |
| 22 | Create Template with AI | 33 |
| 23 | Manage AI Service | 34 |
| 24 | Manage Text Essay | 36 |
| 25 | Manage Quiz | 31, 37 |
| 26 | Manage Diagram Essay | 38 |
| 27 | Manage Code Lab | 39 |
| 28 | Manage Assignments | 25, 35, 40, 41, 48, 49, 50 |
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
| UC-AIG-01, 02, 03 | 21, 22, 23 |
| UC-ASM-02 | 24 |
| UC-QBK-02, UC-ASM-03 | 25 |
| UC-ASM-04 | 26 |
| UC-ASM-05 | 27 |
| UC-ASM-01, 06, 07, 15, 16, 17; UC-GRP-05 | 28 |
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
