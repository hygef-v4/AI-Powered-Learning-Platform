# Services — AS-BUILT 2026-10-30

Current source: parent HEAD `5a76e428` + working-tree package/file moves only; source-observed, không phải runtime acceptance. 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Baseline `794a4053` là snapshot tài liệu trước đó.

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

Current working-tree structure at parent HEAD `5a76e428`; use-case ownership is unchanged.

### U01 — Account & Access

- [AccessPolicy](../../../backend/src/main/java/edu/aiplatform/identity/service/AccessPolicy.java)
- [AccountAdminService](../../../backend/src/main/java/edu/aiplatform/identity/service/AccountAdminService.java)
- [AccountImportService](../../../backend/src/main/java/edu/aiplatform/identity/service/AccountImportService.java)
- [AccountLookupService](../../../backend/src/main/java/edu/aiplatform/identity/service/AccountLookupService.java)
- [AccountViews](../../../backend/src/main/java/edu/aiplatform/identity/service/AccountViews.java)
- [ActivationService](../../../backend/src/main/java/edu/aiplatform/identity/service/ActivationService.java)
- [AuthorizationService](../../../backend/src/main/java/edu/aiplatform/identity/service/AuthorizationService.java)
- [AuthService](../../../backend/src/main/java/edu/aiplatform/identity/service/AuthService.java)
- [BootstrapAdminInitializer](../../../backend/src/main/java/edu/aiplatform/identity/service/BootstrapAdminInitializer.java)
- [HomePaths](../../../backend/src/main/java/edu/aiplatform/identity/service/HomePaths.java)
- [OtpMailHandler](../../../backend/src/main/java/edu/aiplatform/identity/service/OtpMailHandler.java)
- [OtpMailTemplate](../../../backend/src/main/java/edu/aiplatform/identity/service/OtpMailTemplate.java)
- [OtpService](../../../backend/src/main/java/edu/aiplatform/identity/service/OtpService.java)
- [PasswordChangeService](../../../backend/src/main/java/edu/aiplatform/identity/service/PasswordChangeService.java)
- [PasswordPolicyException](../../../backend/src/main/java/edu/aiplatform/identity/service/PasswordPolicyException.java)
- [PasswordResetService](../../../backend/src/main/java/edu/aiplatform/identity/service/PasswordResetService.java)
- [ProfileService](../../../backend/src/main/java/edu/aiplatform/identity/service/ProfileService.java)
- [RoleChangeBlockedException](../../../backend/src/main/java/edu/aiplatform/identity/service/RoleChangeBlockedException.java)
- [SessionAuthority](../../../backend/src/main/java/edu/aiplatform/identity/service/SessionAuthority.java)
- [SessionInfo](../../../backend/src/main/java/edu/aiplatform/identity/service/SessionInfo.java)

### U02 — Audit

- [AuditQueryService](../../../backend/src/main/java/edu/aiplatform/audit/service/AuditQueryService.java)
- [AuditStore](../../../backend/src/main/java/edu/aiplatform/audit/service/AuditStore.java)

### U03 — Job and Settings

- [ArtifactService](../../../backend/src/main/java/edu/aiplatform/files/service/ArtifactService.java)
- [ContentInspector](../../../backend/src/main/java/edu/aiplatform/files/service/ContentInspector.java)
- [DownloadService](../../../backend/src/main/java/edu/aiplatform/files/service/DownloadService.java)
- [DownloadTokenService](../../../backend/src/main/java/edu/aiplatform/files/service/DownloadTokenService.java)
- [DriveCleanupJobHandler](../../../backend/src/main/java/edu/aiplatform/files/service/DriveCleanupJobHandler.java)
- [FileAbusiveException](../../../backend/src/main/java/edu/aiplatform/files/service/FileAbusiveException.java)
- [FileAudit](../../../backend/src/main/java/edu/aiplatform/files/service/FileAudit.java)
- [FilePolicyService](../../../backend/src/main/java/edu/aiplatform/files/service/FilePolicyService.java)
- [FileRefSigner](../../../backend/src/main/java/edu/aiplatform/files/service/FileRefSigner.java)
- [FileSettingDefinitions](../../../backend/src/main/java/edu/aiplatform/files/service/FileSettingDefinitions.java)
- [IDriveCleanupTarget](../../../backend/src/main/java/edu/aiplatform/files/service/IDriveCleanupTarget.java)
- [IStorage](../../../backend/src/main/java/edu/aiplatform/files/service/IStorage.java)
- [StorageUnavailableException](../../../backend/src/main/java/edu/aiplatform/files/service/StorageUnavailableException.java)
- [StoredMetadata](../../../backend/src/main/java/edu/aiplatform/files/service/StoredMetadata.java)
- [UploadResult](../../../backend/src/main/java/edu/aiplatform/files/service/UploadResult.java)
- [UploadService](../../../backend/src/main/java/edu/aiplatform/files/service/UploadService.java)
- [AfterCommitExecutor](../../../backend/src/main/java/edu/aiplatform/jobs/service/AfterCommitExecutor.java)
- [EventPublisher](../../../backend/src/main/java/edu/aiplatform/jobs/service/EventPublisher.java)
- [IMessageSender](../../../backend/src/main/java/edu/aiplatform/jobs/service/IMessageSender.java)
- [JobDispatcher](../../../backend/src/main/java/edu/aiplatform/jobs/service/JobDispatcher.java)
- [JobHandlerRegistry](../../../backend/src/main/java/edu/aiplatform/jobs/service/JobHandlerRegistry.java)
- [JobPublisher](../../../backend/src/main/java/edu/aiplatform/jobs/service/JobPublisher.java)
- [JobRouting](../../../backend/src/main/java/edu/aiplatform/jobs/service/JobRouting.java)
- [PendingSweepRunner](../../../backend/src/main/java/edu/aiplatform/jobs/service/PendingSweepRunner.java)
- [ScheduledScanRunner](../../../backend/src/main/java/edu/aiplatform/jobs/service/ScheduledScanRunner.java)
- [SettingDefinitionRegistry](../../../backend/src/main/java/edu/aiplatform/settings/service/SettingDefinitionRegistry.java)
- [SettingsSectionRegistry](../../../backend/src/main/java/edu/aiplatform/settings/service/SettingsSectionRegistry.java)
- [SettingsService](../../../backend/src/main/java/edu/aiplatform/settings/service/SettingsService.java)
- [BackupCleanupTarget](../../../backend/src/main/java/edu/aiplatform/backup/service/BackupCleanupTarget.java)
- [BackupEncryptor](../../../backend/src/main/java/edu/aiplatform/backup/service/BackupEncryptor.java)
- [BackupFailure](../../../backend/src/main/java/edu/aiplatform/backup/service/BackupFailure.java)
- [BackupJobHandler](../../../backend/src/main/java/edu/aiplatform/backup/service/BackupJobHandler.java)
- [BackupPendingSweeper](../../../backend/src/main/java/edu/aiplatform/backup/service/BackupPendingSweeper.java)
- [BackupRetentionService](../../../backend/src/main/java/edu/aiplatform/backup/service/BackupRetentionService.java)
- [BackupScheduleScanner](../../../backend/src/main/java/edu/aiplatform/backup/service/BackupScheduleScanner.java)
- [BackupSettingDefinitions](../../../backend/src/main/java/edu/aiplatform/backup/service/BackupSettingDefinitions.java)
- [BackupStatusService](../../../backend/src/main/java/edu/aiplatform/backup/service/BackupStatusService.java)
- [IBackupStore](../../../backend/src/main/java/edu/aiplatform/backup/service/IBackupStore.java)
- [IDumpRunner](../../../backend/src/main/java/edu/aiplatform/backup/service/IDumpRunner.java)

### U04 — Subject Administration

- [SubjectAdminAccess](../../../backend/src/main/java/edu/aiplatform/subjects/service/SubjectAdminAccess.java)
- [SubjectAdminService](../../../backend/src/main/java/edu/aiplatform/subjects/service/SubjectAdminService.java)
- [SubjectAuditRecorder](../../../backend/src/main/java/edu/aiplatform/subjects/service/SubjectAuditRecorder.java)
- [SubjectClassesReader](../../../backend/src/main/java/edu/aiplatform/subjects/service/SubjectClassesReader.java)
- [SubjectCommandService](../../../backend/src/main/java/edu/aiplatform/subjects/service/SubjectCommandService.java)
- [SubjectLifecycleBudget](../../../backend/src/main/java/edu/aiplatform/subjects/service/SubjectLifecycleBudget.java)
- [SubjectMutationGuard](../../../backend/src/main/java/edu/aiplatform/subjects/service/SubjectMutationGuard.java)
- [SubjectQueryService](../../../backend/src/main/java/edu/aiplatform/subjects/service/SubjectQueryService.java)
- [SubjectScopeService](../../../backend/src/main/java/edu/aiplatform/subjects/service/SubjectScopeService.java)
- [SubjectStatusService](../../../backend/src/main/java/edu/aiplatform/subjects/service/SubjectStatusService.java)

### U05 — Class & Enrollment

- [AcademicsContractAdapter](../../../backend/src/main/java/edu/aiplatform/academics/service/AcademicsContractAdapter.java)
- [AcademicsMcpTools](../../../backend/src/main/java/edu/aiplatform/academics/service/AcademicsMcpTools.java)
- [AcademicsProblem](../../../backend/src/main/java/edu/aiplatform/academics/service/AcademicsProblem.java)
- [AcademicsService](../../../backend/src/main/java/edu/aiplatform/academics/service/AcademicsService.java)

### U06 — AI Connections

- [AdminAiMcpTools](../../../backend/src/main/java/edu/aiplatform/ai/service/AdminAiMcpTools.java)
- [AiAccess](../../../backend/src/main/java/edu/aiplatform/ai/service/AiAccess.java)
- [AiConnectionService](../../../backend/src/main/java/edu/aiplatform/ai/service/AiConnectionService.java)
- [AiConnectionsSection](../../../backend/src/main/java/edu/aiplatform/ai/service/AiConnectionsSection.java)
- [AiSourceService](../../../backend/src/main/java/edu/aiplatform/ai/service/AiSourceService.java)
- [AiTaskDelivery](../../../backend/src/main/java/edu/aiplatform/ai/service/AiTaskDelivery.java)
- [AiTaskJobHandler](../../../backend/src/main/java/edu/aiplatform/ai/service/AiTaskJobHandler.java)
- [AiTaskResultHandlerRegistry](../../../backend/src/main/java/edu/aiplatform/ai/service/AiTaskResultHandlerRegistry.java)
- [AiTaskService](../../../backend/src/main/java/edu/aiplatform/ai/service/AiTaskService.java)
- [AiUsageService](../../../backend/src/main/java/edu/aiplatform/ai/service/AiUsageService.java)
- [ConnectionTestService](../../../backend/src/main/java/edu/aiplatform/ai/service/ConnectionTestService.java)
- [IMcpRateLimiter](../../../backend/src/main/java/edu/aiplatform/ai/service/IMcpRateLimiter.java)
- [IProviderGateway](../../../backend/src/main/java/edu/aiplatform/ai/service/IProviderGateway.java)
- [JsonSchemaChecker](../../../backend/src/main/java/edu/aiplatform/ai/service/JsonSchemaChecker.java)
- [KeyCipher](../../../backend/src/main/java/edu/aiplatform/ai/service/KeyCipher.java)
- [McpAuthoringRequirementsTools](../../../backend/src/main/java/edu/aiplatform/ai/service/McpAuthoringRequirementsTools.java)
- [McpCapabilitiesTools](../../../backend/src/main/java/edu/aiplatform/ai/service/McpCapabilitiesTools.java)
- [McpCredentialService](../../../backend/src/main/java/edu/aiplatform/ai/service/McpCredentialService.java)
- [McpServer](../../../backend/src/main/java/edu/aiplatform/ai/service/McpServer.java)
- [McpToolRegistry](../../../backend/src/main/java/edu/aiplatform/ai/service/McpToolRegistry.java)
- [McpWriteRequestService](../../../backend/src/main/java/edu/aiplatform/ai/service/McpWriteRequestService.java)
- [ProviderCatalog](../../../backend/src/main/java/edu/aiplatform/ai/service/ProviderCatalog.java)
- [QuotaPolicyService](../../../backend/src/main/java/edu/aiplatform/ai/service/QuotaPolicyService.java)
- [QuotaReservationService](../../../backend/src/main/java/edu/aiplatform/ai/service/QuotaReservationService.java)
- [TeacherQuotaSection](../../../backend/src/main/java/edu/aiplatform/ai/service/TeacherQuotaSection.java)

### U07 — Assignment Authoring

- [AiDraftProposal](../../../backend/src/main/java/edu/aiplatform/assignments/service/AiDraftProposal.java)
- [AssignmentAccess](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentAccess.java)
- [AssignmentAiDraftService](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentAiDraftService.java)
- [AssignmentAudit](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentAudit.java)
- [AssignmentAuthoringService](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentAuthoringService.java)
- [AssignmentCommands](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentCommands.java)
- [AssignmentCopyService](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentCopyService.java)
- [AssignmentDraftGenerationSchema](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentDraftGenerationSchema.java)
- [AssignmentDraftPromptBuilder](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentDraftPromptBuilder.java)
- [AssignmentDraftResultHandler](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentDraftResultHandler.java)
- [AssignmentDraftValidator](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentDraftValidator.java)
- [AssignmentLifecycle](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentLifecycle.java)
- [AssignmentMcpAuthoringService](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentMcpAuthoringService.java)
- [AssignmentMcpQueryTools](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentMcpQueryTools.java)
- [AssignmentMcpTools](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentMcpTools.java)
- [AssignmentProblem](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentProblem.java)
- [AssignmentPublicationService](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentPublicationService.java)
- [AssignmentQueryService](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentQueryService.java)
- [AssignmentScheduleScanner](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentScheduleScanner.java)
- [AssignmentSupport](../../../backend/src/main/java/edu/aiplatform/assignments/service/AssignmentSupport.java)
- [DiagramXml](../../../backend/src/main/java/edu/aiplatform/assignments/service/DiagramXml.java)
- [DocumentModelService](../../../backend/src/main/java/edu/aiplatform/assignments/service/DocumentModelService.java)
- [DocxExporter](../../../backend/src/main/java/edu/aiplatform/assignments/service/DocxExporter.java)
- [DocxImporter](../../../backend/src/main/java/edu/aiplatform/assignments/service/DocxImporter.java)
- [DocxService](../../../backend/src/main/java/edu/aiplatform/assignments/service/DocxService.java)
- [PngChunks](../../../backend/src/main/java/edu/aiplatform/assignments/service/PngChunks.java)
- [RubricService](../../../backend/src/main/java/edu/aiplatform/assignments/service/RubricService.java)
- [SecureXml](../../../backend/src/main/java/edu/aiplatform/assignments/service/SecureXml.java)
- [SvgSanitizer](../../../backend/src/main/java/edu/aiplatform/assignments/service/SvgSanitizer.java)

### U08 — Assignment Work

- [ActivityRetentionScanner](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/ActivityRetentionScanner.java)
- [ActivityService](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/ActivityService.java)
- [ActivitySummaryCalculator](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/ActivitySummaryCalculator.java)
- [AssignmentRetiredListener](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/AssignmentRetiredListener.java)
- [AssignmentWorkMcpTools](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/AssignmentWorkMcpTools.java)
- [AttemptService](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/AttemptService.java)
- [AttemptSubmitter](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/AttemptSubmitter.java)
- [AutoSubmitScanner](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/AutoSubmitScanner.java)
- [FeedbackFormat](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/FeedbackFormat.java)
- [PracticeFeedbackResultHandler](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/PracticeFeedbackResultHandler.java)
- [PracticeFeedbackService](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/PracticeFeedbackService.java)
- [StudentAssignmentService](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/StudentAssignmentService.java)
- [SubmissionQueryService](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/SubmissionQueryService.java)
- [WorkAccess](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/WorkAccess.java)
- [WorkAudit](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/WorkAudit.java)
- [WorkProblem](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service/WorkProblem.java)

### U09 — Assignment Grading

- [EvaluationLifecycleService](../../../backend/src/main/java/edu/aiplatform/grading/service/EvaluationLifecycleService.java)
- [GradebookExporter](../../../backend/src/main/java/edu/aiplatform/grading/service/GradebookExporter.java)
- [GradebookService](../../../backend/src/main/java/edu/aiplatform/grading/service/GradebookService.java)
- [GradingAccess](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingAccess.java)
- [GradingBatchService](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingBatchService.java)
- [GradingCommands](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingCommands.java)
- [GradingFeedbackValidator](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingFeedbackValidator.java)
- [GradingJson](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingJson.java)
- [GradingMcpBatchService](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingMcpBatchService.java)
- [GradingMcpQueryService](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingMcpQueryService.java)
- [GradingMcpTools](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingMcpTools.java)
- [GradingPromptBuilder](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingPromptBuilder.java)
- [GradingProposalResultHandler](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingProposalResultHandler.java)
- [GradingProposalService](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingProposalService.java)
- [GradingProposalTransactions](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingProposalTransactions.java)
- [GradingProposalValidator](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingProposalValidator.java)
- [GradingService](../../../backend/src/main/java/edu/aiplatform/grading/service/GradingService.java)
- [SubmissionImageService](../../../backend/src/main/java/edu/aiplatform/grading/service/SubmissionImageService.java)
- [SubmissionReviewService](../../../backend/src/main/java/edu/aiplatform/grading/service/SubmissionReviewService.java)

### U10 — Quiz Authoring

- [ActiveSubjects](../../../backend/src/main/java/edu/aiplatform/quizzes/service/ActiveSubjects.java)
- [AiDraftViews](../../../backend/src/main/java/edu/aiplatform/quizzes/service/AiDraftViews.java)
- [QuestionBankMcpTools](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuestionBankMcpTools.java)
- [QuestionBankService](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuestionBankService.java)
- [QuestionImporter](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuestionImporter.java)
- [QuestionInvalidException](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuestionInvalidException.java)
- [QuestionViews](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuestionViews.java)
- [QuizAccess](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizAccess.java)
- [QuizAiDraftService](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizAiDraftService.java)
- [QuizAudit](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizAudit.java)
- [QuizAuthoringService](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizAuthoringService.java)
- [QuizCopyService](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizCopyService.java)
- [QuizDraftPrompt](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizDraftPrompt.java)
- [QuizDraftResultHandler](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizDraftResultHandler.java)
- [QuizMcpAuthoringService](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizMcpAuthoringService.java)
- [QuizMcpQueryTools](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizMcpQueryTools.java)
- [QuizMcpTools](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizMcpTools.java)
- [QuizPublicationService](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizPublicationService.java)
- [QuizQueryService](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizQueryService.java)
- [QuizReferenceMaterials](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizReferenceMaterials.java)
- [QuizScorer](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizScorer.java)
- [QuizSupport](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizSupport.java)
- [QuizViews](../../../backend/src/main/java/edu/aiplatform/quizzes/service/QuizViews.java)
- [RuleViolationException](../../../backend/src/main/java/edu/aiplatform/quizzes/service/RuleViolationException.java)

### U11 — Quiz Practice

- [PracticeAccess](../../../backend/src/main/java/edu/aiplatform/quizpractice/service/PracticeAccess.java)
- [PracticeSupport](../../../backend/src/main/java/edu/aiplatform/quizpractice/service/PracticeSupport.java)
- [QuizActivityService](../../../backend/src/main/java/edu/aiplatform/quizpractice/service/QuizActivityService.java)
- [QuizAttemptService](../../../backend/src/main/java/edu/aiplatform/quizpractice/service/QuizAttemptService.java)
- [QuizAutoSubmitScanner](../../../backend/src/main/java/edu/aiplatform/quizpractice/service/QuizAutoSubmitScanner.java)
- [QuizPracticeQueryService](../../../backend/src/main/java/edu/aiplatform/quizpractice/service/QuizPracticeQueryService.java)
- [QuizRetiredHandler](../../../backend/src/main/java/edu/aiplatform/quizpractice/service/QuizRetiredHandler.java)
- [TeacherQuizAttemptService](../../../backend/src/main/java/edu/aiplatform/quizpractice/service/TeacherQuizAttemptService.java)

### U12 — Group Assignment

- [ClassGroupService](../../../backend/src/main/java/edu/aiplatform/groups/service/ClassGroupService.java)
- [GroupAccess](../../../backend/src/main/java/edu/aiplatform/groups/service/GroupAccess.java)
- [GroupReadinessService](../../../backend/src/main/java/edu/aiplatform/groups/service/GroupReadinessService.java)
- [GroupRenameService](../../../backend/src/main/java/edu/aiplatform/groups/service/GroupRenameService.java)
- [GroupSetInvalidException](../../../backend/src/main/java/edu/aiplatform/groups/service/GroupSetInvalidException.java)
- [LeaderRequestService](../../../backend/src/main/java/edu/aiplatform/groups/service/LeaderRequestService.java)
- [MyGroupService](../../../backend/src/main/java/edu/aiplatform/groups/service/MyGroupService.java)
- [RandomGroupSplitter](../../../backend/src/main/java/edu/aiplatform/groups/service/RandomGroupSplitter.java)
- [GroupAssignmentLifecycle](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupAssignmentLifecycle.java)
- [GroupAutoSubmitScanner](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupAutoSubmitScanner.java)
- [GroupDocAccess](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupDocAccess.java)
- [GroupDocContext](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupDocContext.java)
- [GroupDocCreateJobHandler](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupDocCreateJobHandler.java)
- [GroupDocumentService](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupDocumentService.java)
- [GroupSectionService](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupSectionService.java)
- [GroupSnapshot](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupSnapshot.java)
- [GroupSubmissionService](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupSubmissionService.java)
- [GroupWorkQueryService](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupWorkQueryService.java)
- [GroupWorkspaceService](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/GroupWorkspaceService.java)
- [IGroupDocStreams](../../../backend/src/main/java/edu/aiplatform/groupdocs/service/IGroupDocStreams.java)

### U13 — Reference Materials

- [AssignmentSourceTextService](../../../backend/src/main/java/edu/aiplatform/materials/service/AssignmentSourceTextService.java)
- [MaterialAccessPolicy](../../../backend/src/main/java/edu/aiplatform/materials/service/MaterialAccessPolicy.java)
- [MaterialCommands](../../../backend/src/main/java/edu/aiplatform/materials/service/MaterialCommands.java)
- [MaterialMcpQueryTools](../../../backend/src/main/java/edu/aiplatform/materials/service/MaterialMcpQueryTools.java)
- [MaterialMcpTools](../../../backend/src/main/java/edu/aiplatform/materials/service/MaterialMcpTools.java)
- [MaterialModuleService](../../../backend/src/main/java/edu/aiplatform/materials/service/MaterialModuleService.java)
- [MaterialQueryService](../../../backend/src/main/java/edu/aiplatform/materials/service/MaterialQueryService.java)
- [MaterialService](../../../backend/src/main/java/edu/aiplatform/materials/service/MaterialService.java)
- [MaterialTextExtractJobHandler](../../../backend/src/main/java/edu/aiplatform/materials/service/MaterialTextExtractJobHandler.java)
- [MaterialViews](../../../backend/src/main/java/edu/aiplatform/materials/service/MaterialViews.java)
- [ReferenceMaterialService](../../../backend/src/main/java/edu/aiplatform/materials/service/ReferenceMaterialService.java)

### U14 — Announcements & Notifications

- [AnnouncementAccessPolicy](../../../backend/src/main/java/edu/aiplatform/announcements/service/AnnouncementAccessPolicy.java)
- [AnnouncementCommands](../../../backend/src/main/java/edu/aiplatform/announcements/service/AnnouncementCommands.java)
- [AnnouncementService](../../../backend/src/main/java/edu/aiplatform/announcements/service/AnnouncementService.java)
- [AssignmentNotificationLink](../../../backend/src/main/java/edu/aiplatform/notifications/service/AssignmentNotificationLink.java)
- [DeadlineReminderScanner](../../../backend/src/main/java/edu/aiplatform/notifications/service/DeadlineReminderScanner.java)
- [EmailCapPolicy](../../../backend/src/main/java/edu/aiplatform/notifications/service/EmailCapPolicy.java)
- [EmailDeliveryService](../../../backend/src/main/java/edu/aiplatform/notifications/service/EmailDeliveryService.java)
- [EmailSendJobHandler](../../../backend/src/main/java/edu/aiplatform/notifications/service/EmailSendJobHandler.java)
- [NotificationAccess](../../../backend/src/main/java/edu/aiplatform/notifications/service/NotificationAccess.java)
- [NotificationEventService](../../../backend/src/main/java/edu/aiplatform/notifications/service/NotificationEventService.java)
- [NotificationMaintenanceScanner](../../../backend/src/main/java/edu/aiplatform/notifications/service/NotificationMaintenanceScanner.java)
- [NotificationPreferenceService](../../../backend/src/main/java/edu/aiplatform/notifications/service/NotificationPreferenceService.java)
- [NotificationService](../../../backend/src/main/java/edu/aiplatform/notifications/service/NotificationService.java)
- [NotificationSummaries](../../../backend/src/main/java/edu/aiplatform/notifications/service/NotificationSummaries.java)
- [RecipientResolver](../../../backend/src/main/java/edu/aiplatform/notifications/service/RecipientResolver.java)

### U15 — Admin Dashboard

- [AdminDashboardActivityService](../../../backend/src/main/java/edu/aiplatform/admindashboard/service/AdminDashboardActivityService.java)
- [AdminDashboardMcpTools](../../../backend/src/main/java/edu/aiplatform/admindashboard/service/AdminDashboardMcpTools.java)
- [AdminDashboardService](../../../backend/src/main/java/edu/aiplatform/admindashboard/service/AdminDashboardService.java)

## Nguồn chi tiết

[Current source/test inventory](../reverse-engineering/current-code-baseline.md); [verification and limits](../../construction/build-and-test/2026-10-30-package-moves-only-results.md).
