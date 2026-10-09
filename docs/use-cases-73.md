# 4.2 Use Cases (UC)

| ID | Use Case | Feature | Use Case Description |
| --- | --- | --- | --- |
| 01 | Activate Account | Authentication | Allows a user to activate a school-issued account by verifying an OTP sent through Brevo and setting an initial password. |
| 02 | Login | Authentication | Allows a user with an active account to sign in using their email and password and access the functions available to their role. |
| 03 | Logout | Authentication | Allows a signed-in user to end the current session and return to the login screen. |
| 04 | Reset Password | Authentication | Allows a user to verify an OTP sent through Brevo and set a new password to recover account access. |
| 05 | Change Password | Authentication | Allows a signed-in user to change their password after verifying the current password. |
| 06 | View Profile Information | Profile Management | Allows a signed-in user to view their own profile information, including account details and role. |
| 07 | Update Profile Information | Profile Management | Allows a signed-in user to update permitted profile fields, such as display name and phone number. |
| 08 | View My Credit Package | Credit Management | Allows a user to view their own credit-package information and available AI credits. |
| 09 | View Public Credit Package | Credit Management | Allows a user to view available AI credit packages and their prices and credit amounts. |
| 10 | Purchase Credit Package | Payment | Allows a user to select an AI credit package and complete payment through PayOS. Credits are added after payment is verified. |
| 11 | View Payment Result | Payment | Allows a user to view the result of a credit-package payment, including whether the payment is pending, successful, cancelled or failed. |
| 12 | View Notifications | Notification | Allows a user to view their own notifications, open related information and mark notifications as read. |
| 13 | View Enrolled Classes | Student Class Access | Allows a student to view the classes in which they are enrolled and select a class to access. |
| 14 | View Enrolled Class Detail | Student Class Access | Allows a student to view an enrolled class's information and access its learning materials, assignments, announcements and group information. |
| 15 | View Learning Material | Learning Content | Allows an authorized user to open a learning material, preview its content and download permitted files. Google Drive provides stored material files. |
| 16 | View My Group | Student Group | Allows a student to view their group in a class, including its members, leader and group-assignment documents. |
| 17 | Request Leader Change | Student Group | Allows a group member to submit a request to change the group leader, provide a reason and optionally propose a replacement. |
| 18 | View Quiz Practice History | Student Quiz Access | Allows a student to view their quiz practice history and select a quiz to inspect. |
| 19 | View Quiz Practice Detail | Student Quiz Access | Allows a student to view a quiz's information and available practice attempts. |
| 20 | Take Quiz | Student Quiz Completion | Allows a student to answer quiz questions and submit an attempt for automatic scoring. |
| 21 | View Quiz Result | Student Quiz Review | Allows a student to view the score and permitted feedback for a completed quiz attempt. |
| 22 | View Student Assignment List | Student Assignment Access | Allows a student to view Essay, Code Lab, Diagram and Group Assignments available in an enrolled class. |
| 23 | View Assignment Detail | Student Assignment Access | Allows a student to view an assignment's instructions, questions or document structure, scoring criteria, deadline and remaining attempts. |
| 24 | Complete Essay Assignment | Student Assignment Completion | Allows a student to start or resume an essay attempt, write and save responses, and submit the completed work. |
| 25 | Complete Code Lab | Student Assignment Completion | Allows a student to write code, run public test cases through Judge0 and submit the code for evaluation against the configured tests. |
| 26 | Complete Diagram Assignment | Student Assignment Completion | Allows a student to complete the required document sections, create or edit diagrams using Drawio, and submit the work. |
| 27 | Complete Group Assignment | Student Assignment Completion | Allows group members to complete sections of a shared assignment document. Members claim and edit sections, while the group leader coordinates work and submits the group document. |
| 28 | View Submission History | Student Submission Review | Allows a student to view their previous submissions for an assignment, including submitted content, submission time and permitted scores or feedback. |
| 29 | Grade Practice Assignment | Practice Grading | Allows a student to request AI grading for their own submitted Practice Essay or Diagram Assignment. The system checks available credits and returns a private practice score and feedback. |
| 30 | View Class Announcements | Class Communication | Allows authorized students and teachers to view announcements published in a class and read their contents. |
| 31 | View Assigned Class List | Teacher Class Access | Allows a teacher to view the classes assigned to them and select a class to manage. |
| 32 | View Assigned Class Detail | Teacher Class Access | Allows a teacher to view an assigned class's information and access its teaching and management functions. |
| 33 | View Uploaded Learning Materials | Class Material Management | Allows a teacher to view the learning materials uploaded for an assigned class and select a material to view or manage. |
| 34 | Add/Update/Delete Learning Material | Class Material Management | Allows a teacher to add, update or remove class learning materials. Files are stored through Google Drive, and video materials may reference YouTube. |
| 35 | Create/Update/Delete Quiz | Quiz Management | Allows a teacher to create, update or delete quizzes associated with class learning materials, including questions, answer keys, points and quiz settings. |
| 36 | Create/Update/Delete Announcement | Class Communication | Allows a teacher to create, update or delete announcements for an assigned class. |
| 37 | View Student Submissions | Teacher Submission Review | Allows a teacher to view student and group submissions for class assignments, inspect submitted work and check grading status. |
| 38 | Grade Submission With AI | Teacher Grading | Allows a teacher to request AI Grading Proposals for eligible submissions using the assignment's rubrics. The teacher reviews the proposed scores and feedback before recording grades. |
| 39 | Grade Submissions Manually | Teacher Grading | Allows a teacher to evaluate submitted work using the configured scoring criteria and record scores and feedback manually. |
| 40 | View/Export GradeBook | Gradebook | Allows a teacher to view the class gradebook and export assignment scores and submission or grading status to a supported file format. |
| 41 | View Teacher Assignment List | Teacher Assignment Management | Allows a teacher to view and manage Essay, Code Lab, Diagram and Group Assignments in an assigned class. |
| 42 | Create/Update/Delete Essay | Teacher Assignment Authoring | Allows a teacher to create, update or delete essay assignments, including instructions, questions, rubrics and assignment settings. |
| 43 | Create/Update/Delete Code Lab | Teacher Assignment Authoring | Allows a teacher to create, update or delete Code Lab assignments, including coding questions, starter code, sample solutions, tests and execution limits. Judge0 supports code verification and execution. |
| 44 | Create/Update/Delete Diagram Assignment | Teacher Assignment Authoring | Allows a teacher to create, update or delete Diagram Assignments, including the document structure, diagram requirements, rubrics and assignment settings. |
| 45 | Create/Update/Delete Group Assignment | Teacher Assignment Authoring | Allows a teacher to create, update or delete Group Assignments, including the shared-document structure, section instructions, rubrics and assignment settings. |
| 46 | Add/Update/Delete Rubric | Assignment Rubrics | Allows a teacher to add, update or delete rubrics for Essay, Diagram and Group Assignments where permitted. |
| 47 | View Managed Subject Classes | Subject Class Management | Allows a subject manager to view the classes belonging to the subjects they manage. |
| 48 | View Managed Class Detail | Subject Class Management | Allows a subject manager to view a managed class's information, assigned teacher and enrolled students. |
| 49 | Create Class | Subject Class Management | Allows a subject manager to create a class within a subject they manage and enter the required class information. |
| 50 | Assign Teacher To Class | Subject Class Management | Allows a subject manager to assign or change the teacher responsible for a managed class. |
| 51 | Add/Remove Class Students | Class Enrollment Management | Allows a subject manager to add selected students to a managed class or remove students from it. Removed students keep their history. |
| 52 | Edit Class Information | Subject Class Management | Allows a subject manager to update permitted information and settings of a managed class, including opening, archiving or reopening the class and showing the anonymous grade distribution to students. |
| 53 | View Managed Subject | Subject Management | Allows a subject manager to view the subjects assigned to them and access a subject's information and management functions. |
| 54 | View Subject Materials | Subject Material Management | Allows a subject manager to view the learning materials maintained for a managed subject and select a material to view or manage. |
| 55 | Add/Update/Delete Subject Material | Subject Material Management | Allows a subject manager to add, update or remove subject learning materials shared with the subject's classes. Files are stored through Google Drive, and video materials may reference YouTube. |
| 56 | View Subject Question Bank | Subject Question Bank | Allows a subject manager to browse questions maintained for a managed subject and view question details. |
| 57 | Create/Update/Delete Subject Question | Subject Question Bank | Allows a subject manager to create, update or delete questions in a managed subject's question bank. |
| 58 | View Admin Dashboard | System Overview | Allows an administrator to view a summary of platform activity and system statistics. |
| 59 | View Account List | Account Management | Allows an administrator to view platform accounts and select an account to inspect or manage. |
| 60 | Add Account | Account Management | Allows an administrator to create a school-issued account and assign its initial information and role. The account holder completes account activation separately. |
| 61 | View Account Detail | Account Management | Allows an administrator to view an account's profile information, role and current status. |
| 62 | Update Account Information | Account Management | Allows an administrator to update permitted information and role settings of a selected account. |
| 63 | Change Account Status | Account Management | Allows an administrator to change an account's status and control its eligibility to access the platform. |
| 64 | View Subject List | Administrative Subject Management | Allows an administrator to view the platform's subjects and select a subject to inspect or manage. |
| 65 | Add Subject | Administrative Subject Management | Allows an administrator to create a subject with the required identifying information and subject-manager assignment. |
| 66 | View Subject Detail | Administrative Subject Management | Allows an administrator to view a subject's information, assigned subject manager and related class information. |
| 67 | Update Subject Information | Administrative Subject Management | Allows an administrator to update permitted subject information and its subject-manager assignment, and to archive or reopen the subject. |
| 68 | View Credit Package Setting | Credit Package Administration | Allows an administrator to view the configured AI credit packages and their prices and credit amounts. |
| 69 | Add/Edit Credit Package | Credit Package Administration | Allows an administrator to add an AI credit package or edit an existing package's information, price and credit amount. |
| 70 | View Settings | System Settings | Allows an administrator to view system settings for AI models and usage limits, periodic credit allocation, and file-upload size and allowed formats. |
| 71 | Edit Setting | System Settings | Allows an administrator to update AI models and usage limits, periodic credit allocation, and file-upload size and allowed formats within permitted limits. |
| 72 | View Payment History | Payment Administration | Allows an administrator to view credit-purchase transactions, including the purchasing account, selected package, amount, payment time and payment status. |
| 73 | View Audit Log | Audit Management | Allows an administrator to view and search audit logs by actor, action, affected object, result and time. Audit records are read-only and cannot be edited or deleted. |
