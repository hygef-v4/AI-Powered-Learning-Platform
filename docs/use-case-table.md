# 4.2 Use Cases (UC)

> Review draft: names and IDs in this table have not yet been synchronized to other documents.

This table contains 64 active MVP use cases, numbered consecutively from 1 to 64. Related actions are joined with `/` where appropriate. A subject manager can add YouTube sources to subject materials; a teacher can add them to class content. The previous IDs remain in older documents until this draft is reviewed; Simulation Exam is outside the MVP.

| ID | Use Case | Feature | Use Case Description |
|---|---|---|---|
| 1 | Activate Account | Authentication | Lets a user request activation for a school-issued account using a school email, verify an emailed OTP and set a password. The user can request another OTP within the rate limit; responses do not reveal whether an account exists. |
| 2 | Sign In | Authentication | Lets a user sign in with the school email and password, with account status checks, attempt limits and safe error messages. |
| 3 | Sign Out | Authentication | Lets a signed-in user end the current session and revoke the related credentials. |
| 4 | Recover Password | Authentication | Lets a user request a password reset, verify an OTP and set a new password without revealing whether the account exists. |
| 5 | Change Password | Authentication | Lets a signed-in user change the password after verifying the current one and meeting the password policy. |
| 6 | View/Update Profile | Profile Management | Lets a signed-in user view their own profile and update permitted fields, but not their identifying email or role. |
| 7 | View/Create/Update Accounts | Account Management | Lets the administrator browse accounts by role or status; create one school-issued account in PENDING without setting its password or sending email; update account information, highest role and active status with privilege checks, without deleting history. Temporary sign-in restrictions remain with LoginThrottle. |
| 8 | Import Accounts | Account Management | Lets the administrator import school-issued accounts from a file into PENDING, receive per-row success or error results, and send no email during import. |
| 9 | View/Create/Update Subjects | Subject Management | Lets the administrator view subjects with their details, manager and classes; create a subject with a valid code; update its information without losing history; and assign an account with a suitable role as its subject manager. |
| 10 | View Classes | Class Management | Lets an authorized user view the class list and class details within the assigned scope. |
| 11 | Create Class | Class Management | Lets the administrator create a class that belongs to exactly one subject. |
| 12 | Update Class/Assign Teacher | Class Management | Lets the administrator or assigned teacher update class information within their permissions. Only the administrator can assign or change the class's one primary teacher. |
| 13 | Open/Archive Class | Class Lifecycle | Lets the class manager open or archive a class while keeping its content and learning history. |
| 14 | View/Enroll/Remove Students | Enrollment | Lets the class manager view current or past enrollments, enroll a student only once with a notification, and remove access while keeping learning history. |
| 15 | Join Class | Enrollment | Lets a student self-enroll into an open class with a valid invite code, with attempt limits and no class information disclosed for an invalid code. |
| 16 | View/Upload/Update Materials | Subject Content | Lets the subject manager view, upload, update, archive and index learning materials of an assigned subject for RAG. This includes adding a YouTube video or playlist, using existing captions without transcribing audio, and tracking timestamped transcript processing within the subject scope. |
| 17 | Create/Update/Publish Content | Class Content | Lets the teacher view, create, upload, update, reorder, archive and publish valid content to enrolled students in an assigned class. This includes adding a YouTube video or playlist as a lesson RAG source, using existing captions without transcribing audio, and tracking timestamped transcript processing within the class scope. |
| 18 | Access Lesson | Learning Content | Lets an enrolled student open published content and download the permitted files. |
| 19 | Post Class Announcement | Class Communication | Lets the teacher post an announcement to an assigned class. |
| 20 | Discuss in Class Q&A | Class Communication | Lets class members post questions and replies within the scope of their class. |
| 21 | View Group Information | Group Management | Lets an authorized user view groups, leaders, members and contribution status in a class. |
| 22 | Create/Update Groups | Group Management | Lets the teacher create or update groups, add or remove members and appoint exactly one leader. |
| 23 | Request Leader Change | Leader Management | Lets a member submit a reason and a proposed new leader for the teacher to review. |
| 24 | Review Leader Change | Leader Management | Lets the teacher approve or reject a leader-change request and notify the decision. |
| 25 | Define Group Sections | Group Assignment | Lets the teacher prepare a group document with sections for members to claim. The teacher or group leader can release a section claim when needed. |
| 26 | Submit Group Document | Group Submission | Lets group members view the shared document as it updates. The leader submits it; the system submits the current document automatically when the deadline passes. |
| 27 | Grade Group Document | Group Grading | Lets the teacher review the submitted shared document and each member's contribution, assess integration and consistency, and decide each member's final grade without an automatic formula. |
| 28 | View Learning Overview | Learning Overview | Lets the student see enrolled classes, upcoming assignments and notifications. Submission status and published grades appear in the personal results dashboard. |
| 29 | Access Enrolled Class | Learning Dashboard | Lets the student open an enrolled class with its content, assignments and groups within the permitted scope. |
| 30 | View/Create/Update Rubrics | Rubric Bank | Lets an authorized user view, create, update, clone and version rubrics within the permitted scope. |
| 31 | View/Create/Update Questions | Question Bank | Lets an authorized user view, create, update, clone and preview questions within the permitted scope. |
| 32 | Create Assignment with AI | AI Creation | Lets the teacher ask the AI service to create an assignment draft from class content, review the sources, edit, accept or discard the result before publishing. |
| 33 | Create Template with AI | AI Creation | Lets the subject manager ask the AI service to create a subject-level template or question draft grounded in subject RAG, review its sources, edit, accept or discard the result. |
| 34 | View/Configure AI Service | AI Administration | Lets the administrator view AI usage, quota and cost, configure the allowed model and enable or disable the AI service. |
| 35 | View Managed Assignments | Assignment Management | Lets an authorized user view the assignment list and assignment details within the assigned scope. |
| 36 | Create Text Essay | Assignment Creation | Lets an authorized user create a Text Essay in GRADED or PRACTICE mode with instructions, limits and a rubric. |
| 37 | Create Multiple-Choice Quiz | Assignment Creation | Lets an authorized user create a GRADED or PRACTICE multiple-choice quiz with questions, answer keys and scoring rules. Self-study quizzes are PRACTICE; subject-manager templates may use either mode; teacher class quizzes follow the selected mode. |
| 38 | Create Diagram Essay | Assignment Creation | Lets an authorized user create a GRADED or PRACTICE Diagram Essay using the DOCUMENT editor, an outline, optional DOCX import and embedded Draw.io diagrams with XML validation. |
| 39 | Create Code Lab | Assignment Creation | Lets an authorized user create a GRADED or PRACTICE Code Lab, configure the coding problem, language, quota and test cases, and run the sample solution in the sandbox. |
| 40 | Create Group Assignment | Assignment Creation | Lets the teacher create a GRADED group document assignment with a section outline and rubric for collaborative work. |
| 41 | Publish Assignment | Assignment Publication | Lets the teacher configure a valid GRADED or PRACTICE mode, schedule and attempts, then preview, approve and publish an assignment to an assigned class. |
| 42 | View Assigned Work | Assignment Delivery | Lets the student view assigned work with its requirements, rubric, deadline, attempts and status. |
| 43 | Complete and Submit Essay | Assignment Workspace | Lets the student write, autosave and submit an open-ended answer while the assignment is open. |
| 44 | Complete and Submit Quiz | Assignment Workspace | Lets the student answer, autosave and submit a quiz; closed questions are auto-scored against the answer-key version. |
| 45 | Submit Diagram Essay | Diagram Assignment | Lets the student write a document, preview and import DOCX into the active attempt, draw on the embedded Draw.io canvas, save a draft and submit it with the full Draw.io XML. |
| 46 | Submit Code Lab | Code Assignment | Lets the student write code, run it in the sandbox, autosave and submit the source within the assignment limits. |
| 47 | Review Attempts | Submission | Lets the student view their attempts and create a new attempt while time and attempts remain. |
| 48 | Clone/Version/Retire Assignment | Assignment Lifecycle | Lets an authorized user clone, create a new version of or retire an assignment without changing historical data. |
| 49 | Publish and Copy Templates | Assignment Template | Lets the subject manager publish a versioned subject template, including PRACTICE or GRADED quizzes, and a teacher copy it into an independent class draft. |
| 50 | Copy Class Assignment | Assignment Reuse | Lets a teacher copy assignment content and its rubric between classes they teach without copying schedules, attempts, submissions or grades. |
| 51 | Review Submissions | Submission Review | Lets the teacher view GRADED submissions, student work, rubric, attempts and auto-scored results of an assigned class; PRACTICE is outside the teacher grading queue. |
| 52 | Grade Manually | Manual Grading | Lets the teacher enter the score and feedback for an individual submission without calling the AI service. |
| 53 | Grade with AI Assistance | AI-Assisted Grading | Lets the teacher request, review, accept or override an AI grading proposal; the AI never decides the final score. |
| 54 | Publish Grades | Grade Finalization | Lets the teacher confirm the final score and publish the score and feedback to the right student. |
| 55 | Bulk Finalize Grades | Grade Finalization | Lets the teacher check eligibility and finalize many valid scores in a class at once. |
| 56 | View Grades | Gradebook | Lets the student view their own published final score and feedback. |
| 57 | View Gradebook History | Gradebook | Lets an authorized user view the gradebook together with the change history, actor, timestamp and reason. |
| 58 | Monitor Submission Status | Submission Monitoring | Lets the teacher view submission status in an assigned class. The system automatically reminds students who have not submitted 24 hours before the deadline. |
| 59 | View Results Dashboard | Learning Analytics | Lets the student view upcoming assignments, submission status and published grades. An anonymized class distribution is shown only when its privacy conditions are met. |
| 60 | Export Gradebook | Grade Export | Lets an authorized user export a CSV or XLSX gradebook for a class or assignment, containing only data within their permitted scope. |
| 61 | Buy AI Credits | Payment | Lets an ACTIVE Student, Teacher, Subject Manager or Administrator view AI credit packages, pay and track payment status. Verified PayOS payment grants purchased credits exactly once. Student credit use is limited to Practice Text/Diagram Essay grading. |
| 62 | View Notifications | Notification | Lets a user receive and read notifications about class posts and Q&A, assignments, deadlines, groups, grades and payments within their scope. |
| 63 | View Audit Log | Audit | Lets an authorized administrator view audit events by actor, action, object, result and time; audit records cannot be edited or deleted. |
| 64 | Grade Practice with AI | Practice Feedback | Lets a student receive one AI score and feedback for each submitted PRACTICE Text or Diagram Essay attempt when enough credits are available. Without enough credits the attempt is submitted without an AI score; the student must submit a new attempt after buying credits. Practice results stay outside the official gradebook. |

