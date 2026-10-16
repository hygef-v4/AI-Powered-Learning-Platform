# Main Business Flows (TO-BE)

**Revision 2026-10-15** — redrawn after the council review. The ten swimlane diagrams follow the current scope: SWR/SWD focus, two assessment types (ASSIGNMENT with INDIVIDUAL/GROUP mode and optional Draw.io; QUIZ for practice), 66 active use cases ([docs/usecase_table.md](../../../docs/usecase_table.md)), 15 units, roles `STUDENT`, `TEACHER`, `SUBJECT_MANAGER`, `ADMIN`. AI uses an institution-sponsored educator quota, personal API keys or the user's own MCP agent; AI credits are **not** sold. There is no Code Lab, Judge0, payment or PayOS. Sources: [requirements.md](../requirements/requirements.md) and [council-features-2026-10-13.md](../requirements/council-features-2026-10-13.md).

These are TO-BE flows of the proposed system. The AS-IS process (how SWR/SWD teachers and students work today) is a separate deliverable that must come from a real survey; nothing here claims survey evidence or implemented runtime behaviour.

Files: one draw.io file per flow, a combined ten-page file `AI-Powered-Learning-Platform-Main-Business-Flows.drawio`, and PNG exports at 2x in `exports/` for the report.

## Modeling convention

The same note is printed under every diagram.

- **Lanes are responsibilities.** Each lane is one role. A lane holds only the work that role is responsible for.
- **System lane = automated processing only.** It is included to make ALAP's automatic work visible: checking conditions, validating data, evaluating eligibility or validity, saving information, updating status, and sending notifications/emails. The System never makes a human judgement (for example it never concludes that a student cheated).
- **Actor lanes = human work only.** Entering data, selecting information, viewing results and making decisions that need human judgement (for example "Ready to submit?", "Use an AI grading proposal?", "Activity signals need follow-up?").
- **External lanes** (purple) are services outside ALAP that take part in the flow: AI provider, Google Drive backup folder, email provider.
- **Decisions** (diamonds) sit in the lane of whoever decides: a System diamond is an automatic check, an actor diamond is a human decision. Every outgoing branch is labelled.
- **Numbering.** Every activity and decision has a number, in left-to-right order, that matches the activity table of the flow. Start (green) and End (red) events are not numbered; every End states its business outcome. A second Start in the System lane marks a time trigger (deadline, schedule).
- Connectors never pass through a shape or cross another connector; loop-backs run along the top or bottom edge of a lane.

## Flow overview

| ID | Business flow | Lanes | Use cases | Units |
|---|---|---|---|---|
| [BF-01](#bf-01) | Subject and Class Setup, Staff Assignment and Enrollment | Administrator, Subject Manager, System, Student | 17, 19, 26, 27, 28, 29, 20, 21, 65 | U04, U05, U14, U02 |
| [BF-02](#bf-02) | Reference Material Management and Access | Subject Manager / Teacher, System, Student | 58, 59, 60, 61, 62 | U13, U03, U02 |
| [BF-03](#bf-03) | Assignment Authoring, Review and Publication | Teacher, System, Student | 34, 35, 36, 53, 37, 38, 65 | U07, U06, U12, U14, U02 |
| [BF-04](#bf-04) | Individual Assignment Work, Activity Recording and Submission | Student, System | 37, 38, 39, 40 | U08, U02 |
| [BF-05](#bf-05) | Group Assignment Collaboration, Activity Recording and Submission | Group Leader, Group Member, System | 54, 56 | U12, U08, U02 |
| [BF-06](#bf-06) | Quiz Creation and Practice | Teacher, System, Student | 46, 47, 48, 49, 50, 51, 52 | U10, U11, U02 |
| [BF-07](#bf-07) | Grading, Activity Review and Grade Publication | Teacher, System, Student | 42, 43, 44, 45, 40, 65 | U09, U08, U06, U14, U02 |
| [BF-08](#bf-08) | AI Request with Source Selection and Usage Transparency | Administrator, Teacher / Student, System, AI Provider | 14, 15, 31, 32, 33, 35, 41, 43, 48, 53 | U06, U03, U02 |
| [BF-09](#bf-09) | Scheduled Backup to Google Drive and Restore Verification | Admin / Operator, System, Google Drive | 14, 15, 66, 65 | U03, U15, U14, U02 |
| [BF-10](#bf-10) | Important-Event Notification and Email | Teacher, Recipient, System, Email Provider | 63, 64, 65 | U14, U01 (OTP), U02 |

## Council feedback coverage

| Council comment | Where it is addressed |
|---|---|
| Separate System and actor responsibilities; System only for automated processing | Every flow; see the convention above and the "Lane" column of each table |
| Add a note or modeling convention when System is included | Convention note printed under each diagram and repeated above |
| Number activities and describe them | Numbers in every diagram; activity tables below |
| Back up data to Google Drive | BF-09 (backup, retention, failure alert, restore verification) |
| Detect switching screens during work (cheating) | BF-03 step 5 (policy), BF-04 step 5, BF-05 step 7, BF-06 step 6 (recording), BF-07 steps 3–5 (human review) |
| Send email for important activities | BF-10 (dispatch, deduplication, retry); email is triggered in BF-01 step 12, BF-03 step 8, BF-07 step 10, BF-09 step 7 |
| Transparent AI credit so it cannot become a disguised credit business | BF-08 (no purchase; source, payer, quota and token provenance shown; Admin-set quotas with audit) |

## BF-01

**Subject and Class Setup, Staff Assignment and Enrollment**

![BF-01 Subject and Class Setup, Staff Assignment and Enrollment](exports/BF-01.png)

- **Diagram**: [BF-01-Subject-Class-Setup-and-Enrollment.drawio](BF-01-Subject-Class-Setup-and-Enrollment.drawio)
- **Trigger**: An administrator creates a subject and assigns its Subject Manager.
- **End condition**: Eligible students are enrolled in the open class and notified; ineligible students are refused with a reason.
- **Use cases**: UC 17, 19, 26, 27, 28, 29, 20, 21, 65 — **Units**: U04, U05, U14, U02

| No. | Lane | Type | Activity | Description | UC |
|---|---|---|---|---|---|
| 1 | Administrator | Human | Add subject and manager | The Administrator enters the subject code, name and description and selects one Subject Manager for the subject. | 17 |
| 2 | System | Automatic check | Subject valid? | The System checks required fields, a unique subject code and that the selected manager is an ACTIVE account with the SUBJECT_MANAGER role. If not, it shows the reasons and the Administrator corrects the input (back to 1). | 17 |
| 3 | System | Automated | Save subject | The System saves the subject as ACTIVE, stores the manager assignment and writes an audit entry. | 17, 19 |
| 4 | Subject Manager | Human | Create class, add teacher | The Subject Manager enters the class information and selects the primary teacher (a Teacher or Subject Manager). | 26, 27 |
| 5 | System | Automatic check | Class valid? | The System checks the class data, that the subject is ACTIVE and managed by this user, and that the teacher is ACTIVE with an allowed role. Invalid input returns to 4 with the reasons. | 26, 27 |
| 6 | System | Automated | Save class as DRAFT | The System saves the class in DRAFT status with its teacher assignment and writes an audit entry. | 26, 27 |
| 7 | Subject Manager | Human | Select students | The Subject Manager searches student accounts and selects the students to enrol. There is no invitation code or self-enrollment. | 28 |
| 8 | System | Automatic check | Students eligible? | For every selected student the System checks an ACTIVE STUDENT account, no other ACTIVE enrollment in a class of the same subject, and the class capacity. | 28 |
| 9 | Subject Manager | Human | Review refused students | For ineligible students the System returns a reason per student; the Subject Manager reviews them and may select other students (back to 7). Eligible students are not blocked by refused ones. | 28 |
| 10 | System | Automated | Enroll students | The System creates one ACTIVE enrollment per eligible student and writes an audit entry. | 28 |
| 11 | Subject Manager | Human | Open class | The Subject Manager changes the class status to OPEN when it is ready to run. | 29 |
| 12 | System | Automated | Set OPEN, notify students | The System checks that the subject is ACTIVE and a primary teacher is assigned, sets the class to OPEN, records audit and queues an in-app notification and an enrollment email for each enrolled student (BF-10). | 29, 65 |
| 13 | Student | Human | View class | The student opens the notification or Class Dashboard and views the class detail, materials and assignments. | 20, 21, 65 |

## BF-02

**Reference Material Management and Access**

![BF-02 Reference Material Management and Access](exports/BF-02.png)

- **Diagram**: [BF-02-Reference-Material-Management-and-Access.drawio](BF-02-Reference-Material-Management-and-Access.drawio)
- **Trigger**: A Subject Manager or teacher adds a learning file or a YouTube video link.
- **End condition**: The material is stored privately and enrolled students can view it; invalid input or unauthorized access is refused.
- **Use cases**: UC 58, 59, 60, 61, 62 — **Units**: U13, U03, U02

| No. | Lane | Type | Activity | Description | UC |
|---|---|---|---|---|---|
| 1 | Subject Manager / Teacher | Human | Add file or YouTube link | A Subject Manager adds a subject material shared with all classes of the subject; a Teacher adds a class material for an assigned class. The material is one file or one YouTube video link with a title. | 59, 61 |
| 2 | System | Automatic check | Input valid? | The System checks the user's scope (R2 subject manager or R4 class teacher), the file type by content, the configured size limit, or that the link is a single YouTube video. Invalid input is refused with a reason and the user corrects it (back to 1). | 59, 61 |
| 3 | System | Automated | Store and save material | The System uploads file bytes to the private Google Drive storage (no public link) or stores the YouTube link, saves the material as ACTIVE in its subject or class scope and records audit. | 59, 61 |
| 4 | Student | Human | Select material | A student opens the class detail and selects a material of the subject or the class. | 21, 62 |
| 5 | System | Automatic check | Access allowed? | The System checks an ACTIVE enrollment in a class of the scope and that the material is not archived or deleted. | 62 |
| 6 | System | Automated | Issue access link | The System creates a short-lived, user-bound access link to the file, or returns the YouTube link. | 62 |
| 7 | Student | Human | View material | The student previews or downloads the file, or watches the video on YouTube. | 62 |

## BF-03

**Assignment Authoring, Review and Publication**

![BF-03 Assignment Authoring, Review and Publication](exports/BF-03.png)

- **Diagram**: [BF-03-Assignment-Authoring-Review-and-Publication.drawio](BF-03-Assignment-Authoring-Review-and-Publication.drawio)
- **Trigger**: A teacher creates an individual or group assignment for an assigned class.
- **End condition**: The assignment opens and students are notified, or publication is refused with the reasons.
- **Use cases**: UC 34, 35, 36, 53, 37, 38, 65 — **Units**: U07, U06, U12, U14, U02

| No. | Lane | Type | Activity | Description | UC |
|---|---|---|---|---|---|
| 1 | Teacher | Human | Set mode and instructions | The Teacher creates a DRAFT Assignment, chooses submission mode INDIVIDUAL or GROUP, turns the Draw.io diagram option on or off and enters the instructions. | 35, 53 |
| 2 | Teacher | Human decision | Use AI draft? | Human decision: the Teacher decides whether to ask AI for a draft. Writing everything manually is always possible. | 35, 53 |
| 3 | System | Automated | Generate AI draft | The System runs the AI request through the source the Teacher selects (shared quota, personal API or MCP agent) as described in BF-08. The result is only a draft with provenance. | 35, 53 |
| 4 | Teacher | Human | Edit sections and rubrics | The Teacher writes or edits the document skeleton and one rubric per section, including diagram requirements when Draw.io is enabled. | 36 |
| 5 | Teacher | Human | Set schedule and monitoring | The Teacher sets GRADED or PRACTICE (group work is GRADED), opening and closing times, late policy and attempts, and turns activity monitoring on or off (off by default). | 35, 53 |
| 6 | Teacher | Human | Request publication | The Teacher previews the Assignment as a student would see it and requests publication. | 34 |
| 7 | System | Automatic check | Content valid? | The System validates sections, rubrics and Draw.io settings, that the class is OPEN and the opening time is before the closing time; for group mode every class group must have exactly one leader. Failures are listed and the Teacher corrects them (back to 6). | 34 |
| 8 | System | Automated | Publish, notify students | The System locks the version, stores a snapshot of content, rubric and monitoring policy, sets SCHEDULED and records audit. At the opening time it sets the Assignment OPEN, creates one group document per group in group mode, and queues an in-app notification and an email for enrolled students (BF-10). | 34, 65 |
| 9 | Student | Human | View Assignment | Students see the instructions, rubric, deadline, submission mode and, when monitoring is on, which activity data is recorded and why. | 37, 38 |

## BF-04

**Individual Assignment Work, Activity Recording and Submission**

![BF-04 Individual Assignment Work, Activity Recording and Submission](exports/BF-04.png)

- **Diagram**: [BF-04-Individual-Assignment-Work-and-Submission.drawio](BF-04-Individual-Assignment-Work-and-Submission.drawio)
- **Trigger**: A student starts work on an open individual assignment.
- **End condition**: The submission is frozen with a receipt, either manually or automatically at the deadline, or the work is refused.
- **Use cases**: UC 37, 38, 39, 40 — **Units**: U08, U02

| No. | Lane | Type | Activity | Description | UC |
|---|---|---|---|---|---|
| 1 | Student | Human | Start work | The student opens the Assignment from the Assignment list and selects Start or Continue. | 37, 38, 39 |
| 2 | System | Automatic check | Work allowed? | The System checks an ACTIVE enrollment, an OPEN class, the submission window and the remaining attempts. | 39 |
| 3 | System | Automated | Create attempt | The System reopens the attempt in progress or creates a new one with a snapshot of the Assignment version and its monitoring policy. The policy cannot change during the attempt. | 39 |
| 4 | Student | Human | Edit document and diagrams | The student writes the sections and, when enabled, draws diagrams in the embedded Draw.io editor. A monitoring notice is shown when monitoring is on. | 39 |
| 5 | System | Automated | Autosave, log page leaves | The System autosaves the draft with a version check. When monitoring is on it records page hidden/visible events with server time, without double counting and without treating work inside Draw.io as leaving the page. | 39 |
| 6 | Student | Human decision | Ready to submit? | Human decision: the student continues editing (back to 4) or submits. | 39 |
| 7 | System | Automated | Auto-submit draft | If the student has not submitted when the time limit or deadline (plus network grace) is reached, the System submits the last saved draft. Leaving the page never triggers auto-submission or penalties. | 39 |
| 8 | System | Automatic check | Content valid? | The System validates the document blocks and the complete Draw.io XML. Invalid content is reported and the student corrects it (back to 4). | 39 |
| 9 | System | Automated | Freeze submission | The System freezes the content snapshot, stores server time, late flag and receipt hash, records audit and routes GRADED work to grading (BF-07). | 39, 40 |
| 10 | Student | Human | View receipt | The student sees the receipt and the submission in Submission History. | 40 |

## BF-05

**Group Assignment Collaboration, Activity Recording and Submission**

![BF-05 Group Assignment Collaboration, Activity Recording and Submission](exports/BF-05.png)

- **Diagram**: [BF-05-Group-Assignment-Collaboration-and-Submission.drawio](BF-05-Group-Assignment-Collaboration-and-Submission.drawio)
- **Trigger**: A group assignment opens for the class groups.
- **End condition**: The leader submits the group document, or the system submits it at the deadline.
- **Use cases**: UC 54, 56 — **Units**: U12, U08, U02

| No. | Lane | Type | Activity | Description | UC |
|---|---|---|---|---|---|
| 1 | System | Automated | Create group document | The System creates one shared document per class group from the Teacher's skeleton; sections and their rubrics are fixed. | 56 |
| 2 | Group Leader | Human | Assign sections | The leader may assign sections to members; members may also claim open sections themselves. | 54, 56 |
| 3 | Group Member | Human | Claim a section | A member opens the group document from View My Group and claims a section to work on. | 54, 56 |
| 4 | System | Automatic check | Section available? | The System checks group membership, the Assignment state and that no other member holds the section. A held section is refused (back to 3). | 56 |
| 5 | System | Automated | Lock section | The System gives the member an exclusive edit lock on the section. | 56 |
| 6 | Group Member | Human | Edit section | The member writes the section and, when enabled, draws diagrams. A monitoring notice is shown when monitoring is on. | 56 |
| 7 | System | Automated | Autosave, log page leaves | The System autosaves the draft and, when monitoring is on, records activity for that member only, so signals are never attributed to the whole group. | 56 |
| 8 | Group Member | Human | Mark section Done | The member marks the section as Done when it is finished. | 56 |
| 9 | System | Automatic check | Section valid? | The System validates the blocks and Draw.io XML. Invalid content is reported (back to 8). | 56 |
| 10 | System | Automated | Publish section | The System publishes the section into the shared document, records a revision with its author and releases the lock. | 56 |
| 11 | Group Leader | Human decision | Ready to submit? | Human decision: the leader reviews the shared document and either submits it (a warning is shown when sections are unfinished; resubmission is allowed before the deadline) or lets members continue (back to 2). | 56 |
| 12 | System | Automated | Auto-submit document | If the leader has not submitted by the deadline, the System submits the current document. Activity signals never trigger submission or penalties. | 56 |
| 13 | System | Automated | Freeze, queue grading | The System checks the document version, freezes the snapshot with section authors, queues grading (BF-07) and records audit. | 56 |

## BF-06

**Quiz Creation and Practice**

![BF-06 Quiz Creation and Practice](exports/BF-06.png)

- **Diagram**: [BF-06-Quiz-Creation-and-Practice.drawio](BF-06-Quiz-Creation-and-Practice.drawio)
- **Trigger**: A teacher creates a practice quiz for an assigned class.
- **End condition**: The student sees the automatic score, or the attempt is refused.
- **Use cases**: UC 46, 47, 48, 49, 50, 51, 52 — **Units**: U10, U11, U02

| No. | Lane | Type | Activity | Description | UC |
|---|---|---|---|---|---|
| 1 | Teacher | Human | Build Quiz | The Teacher builds a Quiz from the subject question bank or new multiple-choice questions and sets scoring, shuffling, attempt limits, answer review and activity monitoring. | 46, 48 |
| 2 | System | Automatic check | Quiz valid? | The System checks every question has a valid answer key, scores and limits are valid and the class is assigned to the Teacher. Errors return to 1. | 48 |
| 3 | System | Automated | Publish Quiz | The System stores a snapshot of questions, keys and policy and makes the Quiz available as PRACTICE (no Gradebook entry). | 48 |
| 4 | Student | Human | Start Quiz | The student opens the Quiz detail and starts a practice attempt. | 49, 50, 51 |
| 5 | System | Automatic check | Attempt allowed? | The System checks an ACTIVE enrollment and the remaining attempts. | 51 |
| 6 | System | Automated | Create attempt, log page leaves | The System creates the attempt with the policy snapshot and, when monitoring is on, records page hidden/visible events. | 51 |
| 7 | Student | Human | Answer and submit | The student answers and submits; at the time limit the System submits the saved answers. | 51 |
| 8 | System | Automated | Auto-score attempt | The System scores the attempt automatically and saves the result. Activity signals never change the score. | 51 |
| 9 | Student | Human | View result | The student views the score and, if the policy allows, correct answers. | 52 |

## BF-07

**Grading, Activity Review and Grade Publication**

![BF-07 Grading, Activity Review and Grade Publication](exports/BF-07.png)

- **Diagram**: [BF-07-Grading-Activity-Review-and-Grade-Publication.drawio](BF-07-Grading-Activity-Review-and-Grade-Publication.drawio)
- **Trigger**: A student or group submits graded work.
- **End condition**: The teacher publishes the grade, and the student sees the score and feedback.
- **Use cases**: UC 42, 43, 44, 45, 40, 65 — **Units**: U09, U08, U06, U14, U02

| No. | Lane | Type | Activity | Description | UC |
|---|---|---|---|---|---|
| 1 | System | Automated | Queue for grading | The System creates a PENDING evaluation for the submission (one per member for group work). | 42 |
| 2 | Teacher | Human | Open submission | The assigned Teacher opens the class submissions list and selects a submission. | 42 |
| 3 | System | Automated | Show work and activity log | The System checks the Teacher's class assignment and shows the frozen content, the rubric and, if monitoring was on, the count and duration of leave-page events and offline gaps per student. | 42 |
| 4 | Teacher | Human decision | Needs follow-up? | Human decision: signals are evidence to compare, not proof of cheating. The Teacher decides whether follow-up is needed. | 42 |
| 5 | Teacher | Human | Verify with student | The Teacher clarifies the situation with the student outside automatic scoring; ALAP never deducts points or concludes cheating by itself. | 42 |
| 6 | Teacher | Human decision | Use AI proposal? | Human decision: grade manually or ask for an AI proposal. | 43, 44 |
| 7 | System | Automated | Generate AI proposal | The System runs the request through the source the Teacher selects as in BF-08 and validates the output against the rubric and submission version. | 43 |
| 8 | Teacher | Human | Grade and publish | The Teacher accepts or changes the proposal or grades manually, enters feedback, finalizes and selects Publish for one or many grades. | 43, 44, 45 |
| 9 | System | Automatic check | Grades valid? | The System checks scores against rubric limits, required reasons for overrides and that grades are finalized. Problems are returned (back to 8). | 44, 45 |
| 10 | System | Automated | Publish, notify students | The System publishes the grades, keeps grade history, records audit and queues in-app notifications and emails (BF-10). | 45, 65 |
| 11 | Student | Human | View grade | The student views the published score and feedback in Submission History. | 40 |

## BF-08

**AI Request with Source Selection and Usage Transparency**

![BF-08 AI Request with Source Selection and Usage Transparency](exports/BF-08.png)

- **Diagram**: [BF-08-AI-Request-Source-Selection-and-Usage-Transparency.drawio](BF-08-AI-Request-Source-Selection-and-Usage-Transparency.drawio)
- **Trigger**: A teacher or student requests AI help.
- **End condition**: The user receives a draft result with its source and token usage recorded; no credit is charged.
- **Use cases**: UC 14, 15, 31, 32, 33, 35, 41, 43, 48, 53 — **Units**: U06, U03, U02

| No. | Lane | Type | Activity | Description | UC |
|---|---|---|---|---|---|
| 1 | Administrator | Human | Configure API and quotas | The Administrator configures the institutional API key and model, the kill-switch and hourly/daily token quotas for educators. Quotas are allowances, not purchases; Students receive none. | 14, 15 |
| 2 | System | Automated | Save config, audit | The System stores the key protected and masked, saves the quotas and records audit without secrets. | 15 |
| 3 | Teacher / Student | Human | Select AI action | The user selects an AI action: draft an Assignment or Quiz, propose a grade, or request feedback on a submitted Practice Assignment. | 35, 41, 43, 48, 53 |
| 4 | System | Automated | Show sources and quota | The System lists only the sources the user may use: shared quota (educators only), personal API key, or own MCP agent; it shows who bears the cost, remaining quota and a labeled estimate. | 31, 32, 33 |
| 5 | Teacher / Student | Human decision | Which source? | Human decision: the user chooses the source. Nothing is switched automatically. | 31, 32 |
| 6 | System | Automatic check | Source allowed? | The System checks the role, scope and connection state; for shared quota it checks the kill-switch and remaining hourly/daily quota. Students are always refused shared quota. | 15, 31 |
| 7 | Teacher / Student | Human | Choose another option | The user sees why the request was refused. When quota is exhausted the user must actively choose another source or work manually; ALAP never falls back to a personal key by itself. | 33 |
| 8 | System | Automated | Reserve quota, send request | The System atomically reserves the estimated tokens (shared quota only) and sends the request with only the data the user is allowed to share. | 43 |
| 9 | AI Provider | External | Process request | The provider processes the request and returns output and usage data. | — |
| 10 | System | Automated | Validate output | The System validates the response; errors, timeouts or stale versions are not applied and do not overwrite newer work. | 41, 43 |
| 11 | Teacher / Student | Human | Use own MCP agent | The user connects an own MCP agent; each tool call re-checks permissions and writes only drafts. ALAP sees tool calls, not the agent's model tokens. | 32 |
| 12 | System | Automated | Record usage | The System records source, provider/model, time, status and tokens with provenance. Unknown usage is recorded as unavailable, never as 0. Unused reservation is released; failures charge nothing. | 33 |
| 13 | Teacher / Student | Human | Review draft and usage | The user accepts, edits or discards the draft and can open the AI usage history at any time. Results never publish or finalize anything by themselves. | 33 |

## BF-09

**Scheduled Backup to Google Drive and Restore Verification**

![BF-09 Scheduled Backup to Google Drive and Restore Verification](exports/BF-09.png)

- **Diagram**: [BF-09-Scheduled-Backup-and-Restore-Verification.drawio](BF-09-Scheduled-Backup-and-Restore-Verification.drawio)
- **Trigger**: The administrator sets a backup schedule; each scheduled time starts a backup.
- **End condition**: A verified backup is stored on Google Drive and its restore is tested; a failed backup alerts the administrator.
- **Use cases**: UC 14, 15, 66, 65 — **Units**: U03, U15, U14, U02

| No. | Lane | Type | Activity | Description | UC |
|---|---|---|---|---|---|
| 1 | Admin / Operator | Human | Set schedule and retention | The Administrator sets the backup schedule and the number of backups to keep in Settings. | 14, 15 |
| 2 | System | Automated | Save policy | The System validates the values, saves the policy with a version check, records audit and then waits for each scheduled run. | 15 |
| 3 | System | Automated | Create encrypted backup | The System makes a consistent PostgreSQL dump, independent copies of referenced files, a manifest with schema/app version and checksums, and encrypts the archive. Keys stay outside the archive and the VPS. | — |
| 4 | Google Drive | External | Store backup | Google Drive stores the archive in a private folder separate from active learning files. | — |
| 5 | System | Automatic check | Backup verified? | The System verifies the upload and every checksum in the manifest. A failed check is retried a limited number of times (back to 3). | — |
| 6 | System | Automated | Mark SUCCESS | The System marks the run SUCCESS with time and size, and removes backups beyond retention without ever deleting the latest good one. | 66 |
| 7 | System | Automated | Mark FAILED, alert Admin | After the retry limit the System marks the run FAILED with a sanitized error, keeps the last good backup and sends an alert to Administrators (BF-10). Email failure does not change the result. | 65, 66 |
| 8 | Admin / Operator | Human | View backup status | The Administrator views the last successful backup, status history, size, time and errors. | 66 |
| 9 | Admin / Operator | Human decision | Test restore? | Human decision: the operator runs a restore test on a planned cycle or when data is lost. | — |
| 10 | Admin / Operator | Human | Restore and verify in test env | Following the runbook, the operator decrypts the archive and restores the database and files into an isolated environment, remapping file links, then checks record counts, permissions, documents and Draw.io XML, submissions, grades and AI usage, and records evidence with the measured RPO/RTO. ALAP has no in-app production restore button. | — |

## BF-10

**Important-Event Notification and Email**

![BF-10 Important-Event Notification and Email](exports/BF-10.png)

- **Diagram**: [BF-10-Important-Event-Notification-and-Email.drawio](BF-10-Important-Event-Notification-and-Email.drawio)
- **Trigger**: A teacher posts an important announcement, or another important event occurs.
- **End condition**: Recipients receive an in-app notification and, when required, an email; an email failure does not undo the action.
- **Use cases**: UC 63, 64, 65 — **Units**: U14, U01 (OTP), U02

| No. | Lane | Type | Activity | Description | UC |
|---|---|---|---|---|---|
| 1 | Teacher | Human | Post important announcement | The Teacher creates a class announcement, marks it important and selects email delivery. Editing it later does not resend the creation email. | 64 |
| 2 | System | Automated | Save announcement | The System validates and saves the announcement and records audit. | 64 |
| 3 | System | Automated | Create in-app notifications | After the business transaction commits, the System resolves current recipients (enrolled students, or Administrators for backup alerts) and creates one in-app notification each. | 65 |
| 4 | System | Automatic check | Email needed? | The System checks whether the event type sends email and the recipient's email preference (OTP and operational alerts follow their own policy). | — |
| 5 | System | Automated | Queue email | The System queues one email per event, recipient and channel (duplicates are ignored) with minimal content and a sign-in link; no raw grades, submissions, secrets or attachments. | — |
| 6 | Email Provider | External | Accept email | The email provider accepts or rejects the message within its free-tier limits. | — |
| 7 | System | Automatic check | Email accepted? | The System reads the provider response. A rejection is retried with backoff up to a limit (back to 5); after that the email is FAILED. The business action is never rolled back. | — |
| 8 | System | Automated | Mark SENT | The System marks the email SENT, meaning accepted by the provider, not guaranteed in the inbox. | — |
| 9 | Recipient | Human | Read notification | The recipient reads the in-app notification or the email and signs in to see the details. | 63, 65 |

## Rules shared by all flows

- Every System check is performed on the server; hiding a button in the UI is not a control.
- Business data is saved before notifications or emails are queued; an email failure never rolls back the business action (BF-10).
- Activity monitoring is off by default, announced to the student before work, recorded per student, and only a signal for a human to review. ALAP never deducts points, auto-submits or concludes cheating because a page was hidden; it does not use webcams, screenshots or OS blocking.
- AI output is always a draft with provenance. Only the Teacher confirms and publishes official grades; only the author publishes an Assignment or Quiz.
- Technical placement (PostgreSQL state, RabbitMQ jobs, Redis TTL data, Google Drive file bytes) stays in the unit designs, not in the business flows.
