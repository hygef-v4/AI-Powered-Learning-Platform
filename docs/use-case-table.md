# 4.2 Use Cases (UC)

This table follows the current 77-use-case MVP catalog. IDs are renumbered sequentially after removing obsolete entries and adding current use cases.

| ID | Use Case | Feature | Use Case Description |
|---|---|---|---|
| 01 | Activate Account | Authentication | Lets a user request activation for a school-issued account using a school email, verify an emailed OTP and set a password. The user can request another OTP within the rate limit; responses do not reveal whether an account exists. |
| 02 | Sign In | Authentication | Lets a user sign in with the school email and password, with account status checks, attempt limits and safe error messages. |
| 03 | Sign Out | Authentication | Lets a signed-in user end the current session and revoke the related credentials. |
| 04 | Recover Password | Authentication | Lets a user request a password reset, verify an OTP and set a new password without revealing whether the account exists. |
| 05 | Change Password | Authentication | Lets a signed-in user change the password after verifying the current one and meeting the password policy. |
| 06 | View Profile | Profile Management | Lets a user view their own profile and account information. |
| 07 | Update Profile | Profile Management | Lets a user edit the permitted profile fields but not their identifying email or role. |
| 08 | View User Accounts | Account Management | Lets the administrator browse the account list and account details by role or status. |
| 09 | Create Account Manually | Account Management | Lets the administrator create one school-issued account in PENDING. No email is sent at creation, and the administrator does not set the password. |
| 10 | Import Accounts in Bulk | Account Management | Lets the administrator import school-issued accounts from a file into PENDING, receive per-row success or error results, and send no email during import. |
| 11 | Update Account and Role | Account Management | Lets the administrator update account information and the account's highest role with privilege-escalation checks. |
| 12 | Manage Account Status | Account Management | Lets the administrator disable or re-enable an account without deleting business history. Temporary sign-in restrictions after failed attempts are handled by LoginThrottle. |
| 13 | View Subjects | Subject Management | Lets the administrator view subjects with their details, subject manager and classes. |
| 14 | Create Subject | Subject Management | Lets the administrator create a subject with a valid code and information. |
| 15 | Update Subject | Subject Management | Lets the administrator update subject information without losing history. |
| 16 | Assign Subject Manager | Subject Management | Lets the administrator assign an account with a suitable role to manage a subject. |
| 17 | View Classes | Class Management | Lets an authorized user view the class list and class details within the assigned scope. |
| 18 | Create Class | Class Management | Lets the administrator create a class that belongs to exactly one subject. |
| 19 | Update Class | Class Management | Lets the administrator or the assigned instructor update class information. |
| 20 | Assign Primary Instructor | Class Management | Lets the administrator assign exactly one primary instructor to a class. |
| 21 | Manage Class Lifecycle | Class Lifecycle | Lets the class manager open or archive a class while keeping its content and learning history. |
| 22 | View Class Roster | Enrollment | Lets the class manager view learners currently or previously enrolled in the class. |
| 23 | Enroll Learner | Enrollment | Lets the class manager enroll a learner into the class once and send the related notification. |
| 24 | Remove Learner from Class | Enrollment | Lets the class manager withdraw further access while keeping historical learning data. |
| 25 | Self-Enroll with Invite Code | Enrollment | Lets a learner self-enroll into an open class with a valid invite code, with attempt limits and no class information disclosed for an invalid code. |
| 26 | Manage Subject Materials and RAG | Subject Content | Lets the subject manager view, upload, update, archive and index learning materials of an assigned subject, including the processing status. |
| 27 | Manage Class Content | Class Content | Lets the instructor view, create, upload, update, reorder and archive content that belongs to an assigned class. |
| 28 | Publish Class Content | Class Content | Lets the instructor publish valid content to enrolled learners. |
| 29 | Access Lesson | Learning Content | Lets an enrolled learner open published content and download the permitted files. |
| 30 | Post Class Announcement | Class Communication | Lets the instructor post an announcement to an assigned class. |
| 31 | Discuss in Class Q&A | Class Communication | Lets class members post questions and replies within the scope of their class. |
| 32 | Use YouTube as a Lesson RAG Source | Lesson RAG | Lets an authorized instructor or subject manager attach a YouTube video or playlist, use existing captions without transcribing audio, track processing and index timestamped transcripts within the permitted scope. |
| 33 | View Group Information | Group Management | Lets an authorized user view groups, leaders, members and contribution status in a class. |
| 34 | Manage Groups and Leaders | Group Management | Lets the instructor create or update groups, add or remove members and appoint exactly one leader. |
| 35 | Request Leader Change | Leader Management | Lets a member submit a reason and a proposed new leader for the instructor to review. |
| 36 | Review Leader Change Request | Leader Management | Lets the instructor approve or reject a leader-change request and notify the decision. |
| 37 | Define Group Document Sections | Group Assignment | Lets the instructor prepare a group document with sections for members to claim. The instructor or group leader can release a section claim when needed. |
| 38 | Claim and Complete Group Document Section | Group Submission | Lets a member claim a section, complete it in a personal workspace and mark it done so the content is merged into the shared document in real time. |
| 39 | View and Submit Group Document | Group Submission | Lets group members view the shared document as it updates. The leader submits it; the system submits the current document automatically when the deadline passes. |
| 40 | Review and Grade Group Document | Group Grading | Lets the instructor review the submitted shared document and each member's contribution, assess integration and consistency, and decide each member's final grade without an automatic formula. |
| 41 | View Learning Overview | Learning Overview | Lets the learner see enrolled classes, upcoming assignments and notifications. Submission status and published grades appear in the personal results dashboard. |
| 42 | Access Enrolled Class | Learning Dashboard | Lets the learner open an enrolled class with its content, assignments and groups within the permitted scope. |
| 43 | Manage Rubric Bank | Rubric Bank | Lets an authorized user view, create, update, clone and version rubrics within the permitted scope. |
| 44 | Manage Question Bank | Question Bank | Lets an authorized user view, create, update, clone and preview questions within the permitted scope. |
| 45 | Generate and Review Class Assignment Draft with AI | AI Authoring | Lets the instructor ask the AI service for an assignment draft from class content, review the sources, edit, accept or discard the result before publishing. |
| 46 | Generate and Review Subject Template or Question Draft with AI | AI Authoring | Lets the subject manager ask the AI service for a subject-level template or question draft grounded in subject RAG, review its sources, edit, accept or discard the result. |
| 47 | Manage and Monitor AI Service | AI Administration | Lets the administrator view AI usage, quota and cost, configure the allowed model and enable or disable the AI service. |
| 48 | View Managed Assignments | Assignment Management | Lets an authorized user view the assignment list and assignment details within the assigned scope. |
| 49 | Author Essay Assignment | Assignment Authoring | Lets an authorized user create an essay assignment with instructions, limits and a rubric. |
| 50 | Author Multiple-Choice Quiz | Assignment Authoring | Lets an authorized user create a multiple-choice quiz with questions, answer keys and scoring rules. |
| 51 | Author DOCUMENT Assignment with Draw.io | Assignment Authoring | Lets an authorized user create a DOCUMENT assignment with a document outline, optional DOCX import, a required embedded Draw.io diagram and XML validation rules. |
| 52 | Author and Test Code Lab | Assignment Authoring | Lets an authorized user configure the coding problem, language, quota and test cases and run the sample solution in the sandbox. |
| 53 | Author Group Assignment | Assignment Authoring | Lets the instructor create a group DOCUMENT assignment with a section outline and rubric for collaborative work. |
| 54 | Approve and Publish Class Assignment | Assignment Publication | Lets the instructor configure schedule and attempts, preview, approve and publish an assignment to an assigned class. |
| 55 | View Assigned Work | Assignment Delivery | Lets the learner view assigned work with its requirements, rubric, deadline, attempts and status. |
| 56 | Complete and Submit Essay | Assignment Workspace | Lets the learner write, autosave and submit an open-ended answer while the assignment is open. |
| 57 | Complete and Submit Quiz | Assignment Workspace | Lets the learner answer, autosave and submit a quiz; closed questions are auto-scored against the answer-key version. |
| 58 | Complete and Submit DOCUMENT Assignment | Diagram Assignment | Lets the learner write a document, preview and import DOCX into the active attempt, draw on the embedded Draw.io canvas, save a draft and submit it with the full Draw.io XML. |
| 59 | Complete and Submit Code Lab | Code Assignment | Lets the learner write code, run it in the sandbox, autosave and submit the source within the assignment limits. |
| 60 | View History and Resubmit | Submission | Lets the learner view their attempts and create a new attempt while time and attempts remain. |
| 61 | Manage Assignment Lifecycle | Assignment Lifecycle | Lets an authorized user clone, create a new version of or retire an assignment without changing historical data. |
| 62 | Publish and Copy Subject Assignment Template | Assignment Template | Lets the subject manager publish a versioned subject template and an instructor copy it into an independent draft for an assigned class. |
| 63 | Copy Assignment and Rubric Across Classes | Assignment Reuse | Lets an instructor copy assignment content and its rubric between classes they teach without copying schedules, attempts, submissions or grades. |
| 64 | Configure and Take Simulation Exam | Simulation Exam | Lets an instructor configure attempt limits, availability, result selection, answer visibility and grading status. A learner takes the exam within those limits, with a snapshot for each attempt. |
| 65 | Review Submissions | Submission Review | Lets the instructor view the submission list, learner work, rubric, attempts and auto-scored results of an assigned class. |
| 66 | Grade Manually | Manual Grading | Lets the instructor enter the score and feedback for an individual submission without calling the AI service. |
| 67 | Grade with AI Assistance | AI-Assisted Grading | Lets the instructor request, review, accept or override an AI grading proposal; the AI never decides the final score. |
| 68 | Finalize and Publish Grades | Grade Finalization | Lets the instructor confirm the final score and publish the score and feedback to the right learner. |
| 69 | Bulk Finalize Grades | Grade Finalization | Lets the instructor check eligibility and finalize many valid scores in a class at once. |
| 70 | View Grades and Feedback | Gradebook | Lets the learner view their own published final score and feedback. |
| 71 | View Gradebook and Grade History | Gradebook | Lets an authorized user view the gradebook together with the change history, actor, timestamp and reason. |
| 72 | Monitor Submission Status | Submission Monitoring | Lets the instructor view submission status in an assigned class. The system automatically reminds learners who have not submitted 24 hours before the deadline. |
| 73 | View Personal Result Dashboard | Learning Analytics | Lets the learner view upcoming assignments, submission status and published grades. An anonymized class distribution is shown only when its privacy conditions are met. |
| 74 | Export Gradebook | Grade Export | Lets an authorized user export a CSV or XLSX gradebook for a class or assignment, containing only data within their permitted scope. |
| 75 | Buy AI Credits | Payment | Lets any user view AI credit packages, start a payment and track its status. The system verifies a webhook or automatically checks PayOS when a webhook is missing, then grants AI credits exactly once after verification. |
| 76 | Receive and View Notifications | Notification | Lets a user receive and read notifications about class posts and Q&A, assignments, deadlines, groups, grades and payments within their scope. |
| 77 | View Audit Log | Audit | Lets an authorized administrator view audit events by actor, action, object, result and time; audit records cannot be edited or deleted. |

