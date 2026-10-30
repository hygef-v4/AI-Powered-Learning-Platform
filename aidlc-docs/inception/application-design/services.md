# Services — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed, không phải runtime acceptance. 66 active UC, 50 MVP stories + 1 extension, 15 logical units.

| Unit | Trách nhiệm hiện hành | UC | Packages |
|---|---|---|---|
| U01 | Account & Access | 01–12 | [identity](../../../backend/src/main/java/edu/aiplatform/identity) |
| U02 | Audit | 13 | [audit](../../../backend/src/main/java/edu/aiplatform/audit) |
| U03 | Job and Settings | 14–15 | [files](../../../backend/src/main/java/edu/aiplatform/files), [jobs](../../../backend/src/main/java/edu/aiplatform/jobs), [settings](../../../backend/src/main/java/edu/aiplatform/settings), [backup](../../../backend/src/main/java/edu/aiplatform/backup) |
| U04 | Subject Administration | 16–19 | [subjects](../../../backend/src/main/java/edu/aiplatform/subjects) |
| U05 | Class & Enrollment | 20–30 | [academics](../../../backend/src/main/java/edu/aiplatform/academics) |
| U06 | AI Connections | 31–33 | [ai](../../../backend/src/main/java/edu/aiplatform/ai) |
| U07 | Assignment Authoring | 34–36 | [assignments](../../../backend/src/main/java/edu/aiplatform/assignments) |
| U08 | Assignment Work | 37–41 | [assignmentwork](../../../backend/src/main/java/edu/aiplatform/assignmentwork) |
| U09 | Assignment Grading | 42–45 | [grading](../../../backend/src/main/java/edu/aiplatform/grading) |
| U10 | Quiz Authoring | 46–48 | [quizzes](../../../backend/src/main/java/edu/aiplatform/quizzes) |
| U11 | Quiz Practice | 49–52 | [quizpractice](../../../backend/src/main/java/edu/aiplatform/quizpractice) |
| U12 | Group Assignment | 53–57 | [groups](../../../backend/src/main/java/edu/aiplatform/groups), [groupdocs](../../../backend/src/main/java/edu/aiplatform/groupdocs) |
| U13 | Reference Materials | 58–62 | [materials](../../../backend/src/main/java/edu/aiplatform/materials) |
| U14 | Announcements & Notifications | 63–65 | [announcements](../../../backend/src/main/java/edu/aiplatform/announcements), [notifications](../../../backend/src/main/java/edu/aiplatform/notifications) |
| U15 | Admin Dashboard | 66 | [admindashboard](../../../backend/src/main/java/edu/aiplatform/admindashboard) |

## Application service inventory

### U01

- [backend/src/main/java/edu/aiplatform/identity/application/AccountAdminService.java](../../../backend/src/main/java/edu/aiplatform/identity/application/AccountAdminService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/AccountImportService.java](../../../backend/src/main/java/edu/aiplatform/identity/application/AccountImportService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/AccountLookupService.java](../../../backend/src/main/java/edu/aiplatform/identity/application/AccountLookupService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/ActivationService.java](../../../backend/src/main/java/edu/aiplatform/identity/application/ActivationService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/AuthorizationService.java](../../../backend/src/main/java/edu/aiplatform/identity/application/AuthorizationService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/AuthService.java](../../../backend/src/main/java/edu/aiplatform/identity/application/AuthService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/OtpMailHandler.java](../../../backend/src/main/java/edu/aiplatform/identity/application/OtpMailHandler.java)
- [backend/src/main/java/edu/aiplatform/identity/application/OtpService.java](../../../backend/src/main/java/edu/aiplatform/identity/application/OtpService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/PasswordChangeService.java](../../../backend/src/main/java/edu/aiplatform/identity/application/PasswordChangeService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/PasswordResetService.java](../../../backend/src/main/java/edu/aiplatform/identity/application/PasswordResetService.java)
- [backend/src/main/java/edu/aiplatform/identity/application/ProfileService.java](../../../backend/src/main/java/edu/aiplatform/identity/application/ProfileService.java)

### U02

- [backend/src/main/java/edu/aiplatform/audit/application/AuditQueryService.java](../../../backend/src/main/java/edu/aiplatform/audit/application/AuditQueryService.java)

### U03

- [backend/src/main/java/edu/aiplatform/backup/application/BackupJobHandler.java](../../../backend/src/main/java/edu/aiplatform/backup/application/BackupJobHandler.java)
- [backend/src/main/java/edu/aiplatform/backup/application/BackupRetentionService.java](../../../backend/src/main/java/edu/aiplatform/backup/application/BackupRetentionService.java)
- [backend/src/main/java/edu/aiplatform/backup/application/BackupScheduleScanner.java](../../../backend/src/main/java/edu/aiplatform/backup/application/BackupScheduleScanner.java)
- [backend/src/main/java/edu/aiplatform/backup/application/BackupStatusService.java](../../../backend/src/main/java/edu/aiplatform/backup/application/BackupStatusService.java)
- [backend/src/main/java/edu/aiplatform/files/application/ArtifactService.java](../../../backend/src/main/java/edu/aiplatform/files/application/ArtifactService.java)
- [backend/src/main/java/edu/aiplatform/files/application/DownloadService.java](../../../backend/src/main/java/edu/aiplatform/files/application/DownloadService.java)
- [backend/src/main/java/edu/aiplatform/files/application/DownloadTokenService.java](../../../backend/src/main/java/edu/aiplatform/files/application/DownloadTokenService.java)
- [backend/src/main/java/edu/aiplatform/files/application/DriveCleanupJobHandler.java](../../../backend/src/main/java/edu/aiplatform/files/application/DriveCleanupJobHandler.java)
- [backend/src/main/java/edu/aiplatform/files/application/FilePolicyService.java](../../../backend/src/main/java/edu/aiplatform/files/application/FilePolicyService.java)
- [backend/src/main/java/edu/aiplatform/files/application/UploadService.java](../../../backend/src/main/java/edu/aiplatform/files/application/UploadService.java)
- [backend/src/main/java/edu/aiplatform/jobs/application/JobHandlerRegistry.java](../../../backend/src/main/java/edu/aiplatform/jobs/application/JobHandlerRegistry.java)
- [backend/src/main/java/edu/aiplatform/settings/application/SettingsService.java](../../../backend/src/main/java/edu/aiplatform/settings/application/SettingsService.java)

### U04

- [backend/src/main/java/edu/aiplatform/subjects/application/SubjectAdminService.java](../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectAdminService.java)
- [backend/src/main/java/edu/aiplatform/subjects/application/SubjectCommandService.java](../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectCommandService.java)
- [backend/src/main/java/edu/aiplatform/subjects/application/SubjectQueryService.java](../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectQueryService.java)
- [backend/src/main/java/edu/aiplatform/subjects/application/SubjectScopeService.java](../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectScopeService.java)
- [backend/src/main/java/edu/aiplatform/subjects/application/SubjectStatusService.java](../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectStatusService.java)

### U05

- [backend/src/main/java/edu/aiplatform/academics/application/AcademicsMcpTools.java](../../../backend/src/main/java/edu/aiplatform/academics/application/AcademicsMcpTools.java)
- [backend/src/main/java/edu/aiplatform/academics/application/AcademicsService.java](../../../backend/src/main/java/edu/aiplatform/academics/application/AcademicsService.java)

### U06

- [backend/src/main/java/edu/aiplatform/ai/application/AdminAiMcpTools.java](../../../backend/src/main/java/edu/aiplatform/ai/application/AdminAiMcpTools.java)
- [backend/src/main/java/edu/aiplatform/ai/application/AiConnectionService.java](../../../backend/src/main/java/edu/aiplatform/ai/application/AiConnectionService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/AiSourceService.java](../../../backend/src/main/java/edu/aiplatform/ai/application/AiSourceService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/AiTaskJobHandler.java](../../../backend/src/main/java/edu/aiplatform/ai/application/AiTaskJobHandler.java)
- [backend/src/main/java/edu/aiplatform/ai/application/AiTaskResultHandlerRegistry.java](../../../backend/src/main/java/edu/aiplatform/ai/application/AiTaskResultHandlerRegistry.java)
- [backend/src/main/java/edu/aiplatform/ai/application/AiTaskService.java](../../../backend/src/main/java/edu/aiplatform/ai/application/AiTaskService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/AiUsageService.java](../../../backend/src/main/java/edu/aiplatform/ai/application/AiUsageService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/ConnectionTestService.java](../../../backend/src/main/java/edu/aiplatform/ai/application/ConnectionTestService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/McpAuthoringRequirementsTools.java](../../../backend/src/main/java/edu/aiplatform/ai/application/McpAuthoringRequirementsTools.java)
- [backend/src/main/java/edu/aiplatform/ai/application/McpCapabilitiesTools.java](../../../backend/src/main/java/edu/aiplatform/ai/application/McpCapabilitiesTools.java)
- [backend/src/main/java/edu/aiplatform/ai/application/McpCredentialService.java](../../../backend/src/main/java/edu/aiplatform/ai/application/McpCredentialService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/McpWriteRequestService.java](../../../backend/src/main/java/edu/aiplatform/ai/application/McpWriteRequestService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/QuotaPolicyService.java](../../../backend/src/main/java/edu/aiplatform/ai/application/QuotaPolicyService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/QuotaReservationService.java](../../../backend/src/main/java/edu/aiplatform/ai/application/QuotaReservationService.java)

### U07

- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentAiDraftService.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentAiDraftService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentAuthoringService.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentAuthoringService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentCopyService.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentCopyService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentDraftResultHandler.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentDraftResultHandler.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentMcpAuthoringService.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentMcpAuthoringService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentMcpQueryTools.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentMcpQueryTools.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentMcpTools.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentMcpTools.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentPublicationService.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentPublicationService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentQueryService.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentQueryService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentScheduleScanner.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentScheduleScanner.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/DocumentModelService.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/DocumentModelService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/DocxService.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/DocxService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/RubricService.java](../../../backend/src/main/java/edu/aiplatform/assignments/application/RubricService.java)

### U08

- [backend/src/main/java/edu/aiplatform/assignmentwork/application/ActivityRetentionScanner.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/ActivityRetentionScanner.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/application/ActivityService.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/ActivityService.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/application/AssignmentWorkMcpTools.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/AssignmentWorkMcpTools.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/application/AttemptService.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/AttemptService.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/application/AutoSubmitScanner.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/AutoSubmitScanner.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/application/PracticeFeedbackResultHandler.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/PracticeFeedbackResultHandler.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/application/PracticeFeedbackService.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/PracticeFeedbackService.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/application/StudentAssignmentService.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/StudentAssignmentService.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/application/SubmissionQueryService.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/SubmissionQueryService.java)

### U09

- [backend/src/main/java/edu/aiplatform/grading/application/EvaluationLifecycleService.java](../../../backend/src/main/java/edu/aiplatform/grading/application/EvaluationLifecycleService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradebookService.java](../../../backend/src/main/java/edu/aiplatform/grading/application/GradebookService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradingBatchService.java](../../../backend/src/main/java/edu/aiplatform/grading/application/GradingBatchService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradingMcpBatchService.java](../../../backend/src/main/java/edu/aiplatform/grading/application/GradingMcpBatchService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradingMcpQueryService.java](../../../backend/src/main/java/edu/aiplatform/grading/application/GradingMcpQueryService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradingMcpTools.java](../../../backend/src/main/java/edu/aiplatform/grading/application/GradingMcpTools.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradingProposalResultHandler.java](../../../backend/src/main/java/edu/aiplatform/grading/application/GradingProposalResultHandler.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradingProposalService.java](../../../backend/src/main/java/edu/aiplatform/grading/application/GradingProposalService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradingService.java](../../../backend/src/main/java/edu/aiplatform/grading/application/GradingService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/SubmissionImageService.java](../../../backend/src/main/java/edu/aiplatform/grading/application/SubmissionImageService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/SubmissionReviewService.java](../../../backend/src/main/java/edu/aiplatform/grading/application/SubmissionReviewService.java)

### U10

- [backend/src/main/java/edu/aiplatform/quizzes/application/QuestionBankMcpTools.java](../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuestionBankMcpTools.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuestionBankService.java](../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuestionBankService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizAiDraftService.java](../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizAiDraftService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizAuthoringService.java](../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizAuthoringService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizCopyService.java](../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizCopyService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizDraftResultHandler.java](../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizDraftResultHandler.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizMcpAuthoringService.java](../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizMcpAuthoringService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizMcpQueryTools.java](../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizMcpQueryTools.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizMcpTools.java](../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizMcpTools.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizPublicationService.java](../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizPublicationService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizQueryService.java](../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizQueryService.java)

### U11

- [backend/src/main/java/edu/aiplatform/quizpractice/application/QuizActivityService.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/application/QuizActivityService.java)
- [backend/src/main/java/edu/aiplatform/quizpractice/application/QuizAttemptService.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/application/QuizAttemptService.java)
- [backend/src/main/java/edu/aiplatform/quizpractice/application/QuizAutoSubmitScanner.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/application/QuizAutoSubmitScanner.java)
- [backend/src/main/java/edu/aiplatform/quizpractice/application/QuizPracticeQueryService.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/application/QuizPracticeQueryService.java)
- [backend/src/main/java/edu/aiplatform/quizpractice/application/QuizRetiredHandler.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/application/QuizRetiredHandler.java)
- [backend/src/main/java/edu/aiplatform/quizpractice/application/TeacherQuizAttemptService.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/application/TeacherQuizAttemptService.java)

### U12

- [backend/src/main/java/edu/aiplatform/groupdocs/application/GroupAutoSubmitScanner.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/application/GroupAutoSubmitScanner.java)
- [backend/src/main/java/edu/aiplatform/groupdocs/application/GroupDocCreateJobHandler.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/application/GroupDocCreateJobHandler.java)
- [backend/src/main/java/edu/aiplatform/groupdocs/application/GroupDocumentService.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/application/GroupDocumentService.java)
- [backend/src/main/java/edu/aiplatform/groupdocs/application/GroupSectionService.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/application/GroupSectionService.java)
- [backend/src/main/java/edu/aiplatform/groupdocs/application/GroupSubmissionService.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/application/GroupSubmissionService.java)
- [backend/src/main/java/edu/aiplatform/groupdocs/application/GroupWorkQueryService.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/application/GroupWorkQueryService.java)
- [backend/src/main/java/edu/aiplatform/groupdocs/application/GroupWorkspaceService.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/application/GroupWorkspaceService.java)
- [backend/src/main/java/edu/aiplatform/groups/application/ClassGroupService.java](../../../backend/src/main/java/edu/aiplatform/groups/application/ClassGroupService.java)
- [backend/src/main/java/edu/aiplatform/groups/application/GroupReadinessService.java](../../../backend/src/main/java/edu/aiplatform/groups/application/GroupReadinessService.java)
- [backend/src/main/java/edu/aiplatform/groups/application/GroupRenameService.java](../../../backend/src/main/java/edu/aiplatform/groups/application/GroupRenameService.java)
- [backend/src/main/java/edu/aiplatform/groups/application/LeaderRequestService.java](../../../backend/src/main/java/edu/aiplatform/groups/application/LeaderRequestService.java)
- [backend/src/main/java/edu/aiplatform/groups/application/MyGroupService.java](../../../backend/src/main/java/edu/aiplatform/groups/application/MyGroupService.java)

### U13

- [backend/src/main/java/edu/aiplatform/materials/application/AssignmentSourceTextService.java](../../../backend/src/main/java/edu/aiplatform/materials/application/AssignmentSourceTextService.java)
- [backend/src/main/java/edu/aiplatform/materials/application/MaterialMcpQueryTools.java](../../../backend/src/main/java/edu/aiplatform/materials/application/MaterialMcpQueryTools.java)
- [backend/src/main/java/edu/aiplatform/materials/application/MaterialMcpTools.java](../../../backend/src/main/java/edu/aiplatform/materials/application/MaterialMcpTools.java)
- [backend/src/main/java/edu/aiplatform/materials/application/MaterialModuleService.java](../../../backend/src/main/java/edu/aiplatform/materials/application/MaterialModuleService.java)
- [backend/src/main/java/edu/aiplatform/materials/application/MaterialQueryService.java](../../../backend/src/main/java/edu/aiplatform/materials/application/MaterialQueryService.java)
- [backend/src/main/java/edu/aiplatform/materials/application/MaterialService.java](../../../backend/src/main/java/edu/aiplatform/materials/application/MaterialService.java)
- [backend/src/main/java/edu/aiplatform/materials/application/MaterialTextExtractJobHandler.java](../../../backend/src/main/java/edu/aiplatform/materials/application/MaterialTextExtractJobHandler.java)
- [backend/src/main/java/edu/aiplatform/materials/application/ReferenceMaterialService.java](../../../backend/src/main/java/edu/aiplatform/materials/application/ReferenceMaterialService.java)

### U14

- [backend/src/main/java/edu/aiplatform/announcements/application/AnnouncementService.java](../../../backend/src/main/java/edu/aiplatform/announcements/application/AnnouncementService.java)
- [backend/src/main/java/edu/aiplatform/notifications/application/DeadlineReminderScanner.java](../../../backend/src/main/java/edu/aiplatform/notifications/application/DeadlineReminderScanner.java)
- [backend/src/main/java/edu/aiplatform/notifications/application/EmailDeliveryService.java](../../../backend/src/main/java/edu/aiplatform/notifications/application/EmailDeliveryService.java)
- [backend/src/main/java/edu/aiplatform/notifications/application/EmailSendJobHandler.java](../../../backend/src/main/java/edu/aiplatform/notifications/application/EmailSendJobHandler.java)
- [backend/src/main/java/edu/aiplatform/notifications/application/NotificationEventService.java](../../../backend/src/main/java/edu/aiplatform/notifications/application/NotificationEventService.java)
- [backend/src/main/java/edu/aiplatform/notifications/application/NotificationMaintenanceScanner.java](../../../backend/src/main/java/edu/aiplatform/notifications/application/NotificationMaintenanceScanner.java)
- [backend/src/main/java/edu/aiplatform/notifications/application/NotificationPreferenceService.java](../../../backend/src/main/java/edu/aiplatform/notifications/application/NotificationPreferenceService.java)
- [backend/src/main/java/edu/aiplatform/notifications/application/NotificationService.java](../../../backend/src/main/java/edu/aiplatform/notifications/application/NotificationService.java)

### U15

- [backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardActivityService.java](../../../backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardActivityService.java)
- [backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardMcpTools.java](../../../backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardMcpTools.java)
- [backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardService.java](../../../backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardService.java)

## Nguồn chi tiết

[Manifest code/schema/UI/tests](../reverse-engineering/current-code-baseline.md); [Public contract signatures](component-methods.md); [HTTP API inventory](../reverse-engineering/api-inventory.md); [Decision/delta register](../reverse-engineering/decision-register.md).
