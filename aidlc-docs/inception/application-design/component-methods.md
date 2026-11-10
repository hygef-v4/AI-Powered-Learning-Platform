# Public contract types — source snapshot 2026-11-07

Source: `f68a6ec5889074de80a1f1f0faf7bcb7e7054ec8`. Canonical signatures are in these top-level contract interfaces/DTOs/enums; fallback implementations under contract/stub are not public contract declarations. HTTP mappings: [api-inventory.md](../reverse-engineering/api-inventory.md). Source inventory is not a claim of complete runtime acceptance.

## U01 Account & Access

- [AccountContact.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/AccountContact.java)
- [AccountStatus.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/AccountStatus.java)
- [Actions.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/Actions.java)
- [Actor.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/Actor.java)
- [AuthorizationDecision.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/AuthorizationDecision.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/IAccountLookup.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/IAccountLookup.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/IAuthorization.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/IAuthorization.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/IClassScope.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/IClassScope.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/IMail.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/IMail.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/ISubjectScope.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/ISubjectScope.java)
- [MailMessage.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/MailMessage.java)
- [ResourceRef.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/ResourceRef.java)
- [Role.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/Role.java)
- [ScopeKind.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/ScopeKind.java)

## U02 Audit

- [AuditEvent.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/AuditEvent.java)
- [AuditFilter.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/AuditFilter.java)
- [AuditLogView.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/AuditLogView.java)
- [AuditResult.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/AuditResult.java)
- [backend/src/main/java/edu/aiplatform/audit/contract/IAudit.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/IAudit.java)
- [backend/src/main/java/edu/aiplatform/audit/contract/IAuditQuery.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/IAuditQuery.java)
- [backend/src/main/java/edu/aiplatform/audit/contract/IAuditStats.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/IAuditStats.java)

## U03 Job and Settings

- [BackupDashboardRunView.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/BackupDashboardRunView.java)
- [BackupDashboardSummary.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/BackupDashboardSummary.java)
- [BackupRunView.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/BackupRunView.java)
- [BackupState.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/BackupState.java)
- [backend/src/main/java/edu/aiplatform/backup/contract/IBackupDashboardStatus.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/IBackupDashboardStatus.java)
- [backend/src/main/java/edu/aiplatform/backup/contract/IBackupStatus.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/IBackupStatus.java)
- [ArtifactPurpose.java](../../../backend/src/main/java/edu/aiplatform/files/contract/ArtifactPurpose.java)
- [DownloadToken.java](../../../backend/src/main/java/edu/aiplatform/files/contract/DownloadToken.java)
- [FileRef.java](../../../backend/src/main/java/edu/aiplatform/files/contract/FileRef.java)
- [FileUpload.java](../../../backend/src/main/java/edu/aiplatform/files/contract/FileUpload.java)
- [backend/src/main/java/edu/aiplatform/files/contract/IArtifact.java](../../../backend/src/main/java/edu/aiplatform/files/contract/IArtifact.java)
- [StoredFileInfo.java](../../../backend/src/main/java/edu/aiplatform/files/contract/StoredFileInfo.java)
- [DomainEvent.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/DomainEvent.java)
- [EventTypes.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/EventTypes.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IEventPublisher.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IEventPublisher.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IJob.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IJob.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IJobHandler.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IJobHandler.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IPendingSweeper.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IPendingSweeper.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IScheduledScanner.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IScheduledScanner.java)
- [JobMessage.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/JobMessage.java)
- [JobQueue.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/JobQueue.java)
- [JobTypes.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/JobTypes.java)
- [PendingJob.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/PendingJob.java)
- [RetryableJobException.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/RetryableJobException.java)
- [SafeJobErrors.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/SafeJobErrors.java)
- [backend/src/main/java/edu/aiplatform/settings/contract/ISettings.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/ISettings.java)
- [backend/src/main/java/edu/aiplatform/settings/contract/ISettingsSectionProvider.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/ISettingsSectionProvider.java)
- [SettingDefinition.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/SettingDefinition.java)
- [SettingGroup.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/SettingGroup.java)
- [SettingKey.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/SettingKey.java)
- [SettingType.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/SettingType.java)

## U04 Subject Administration

- [backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectClasses.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectClasses.java)
- [backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectMutationGuard.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectMutationGuard.java)
- [backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectQuery.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectQuery.java)
- [SubjectClassSummary.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/SubjectClassSummary.java)
- [SubjectRef.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/SubjectRef.java)
- [SubjectStatus.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/SubjectStatus.java)

## U05 Class & Enrollment

- [ClassRef.java](../../../backend/src/main/java/edu/aiplatform/academics/contract/ClassRef.java)
- [ClassStatus.java](../../../backend/src/main/java/edu/aiplatform/academics/contract/ClassStatus.java)
- [backend/src/main/java/edu/aiplatform/academics/contract/IClassAccess.java](../../../backend/src/main/java/edu/aiplatform/academics/contract/IClassAccess.java)

## U06 AI Connections

- [AiGradingLimits.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiGradingLimits.java)
- [AiPrompt.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiPrompt.java)
- [AiProvider.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiProvider.java)
- [AiSource.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiSource.java)
- [AiSourceChoice.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiSourceChoice.java)
- [AiTaskFailure.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiTaskFailure.java)
- [AiTaskRequest.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiTaskRequest.java)
- [AiTaskResult.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiTaskResult.java)
- [AiTaskState.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiTaskState.java)
- [AiTaskView.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiTaskView.java)
- [AiTokenEstimate.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiTokenEstimate.java)
- [AiUsageSummary.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiUsageSummary.java)
- [ApplyOutcome.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/ApplyOutcome.java)
- [AssignmentSourceFile.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AssignmentSourceFile.java)
- [AssignmentSourceText.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AssignmentSourceText.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IAiTask.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IAiTask.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IAiTaskResultHandler.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IAiTaskResultHandler.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IAssignmentSourceTextExtractor.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IAssignmentSourceTextExtractor.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IMcpToolProvider.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IMcpToolProvider.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IMcpWriteRequest.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IMcpWriteRequest.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IReferenceMaterial.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IReferenceMaterial.java)
- [MaterialText.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/MaterialText.java)
- [McpFingerprint.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/McpFingerprint.java)
- [McpInput.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/McpInput.java)
- [McpSchemas.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/McpSchemas.java)
- [McpToolDefinition.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/McpToolDefinition.java)
- [McpToolKind.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/McpToolKind.java)
- [McpToolResult.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/McpToolResult.java)
- [SelectableMaterial.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/SelectableMaterial.java)
- [UsageMeasurement.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/UsageMeasurement.java)

## U07 Assignment Authoring

- [AssignmentContent.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentContent.java)
- [AssignmentDayCount.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentDayCount.java)
- [AssignmentScope.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentScope.java)
- [AssignmentStatus.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentStatus.java)
- [AssignmentView.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentView.java)
- [BlockOrigin.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/BlockOrigin.java)
- [BlockType.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/BlockType.java)
- [CriterionKind.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/CriterionKind.java)
- [DiagramType.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/DiagramType.java)
- [DocumentBlock.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/DocumentBlock.java)
- [DocumentFiles.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/DocumentFiles.java)
- [DocumentModel.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/DocumentModel.java)
- [DocumentPart.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/DocumentPart.java)
- [DocxImportPreview.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/DocxImportPreview.java)
- [GradingMode.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/GradingMode.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IAssignmentLifecycleListener.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IAssignmentLifecycleListener.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IAssignmentQuery.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IAssignmentQuery.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IDiagramLinker.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IDiagramLinker.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IDiagramResolver.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IDiagramResolver.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IDocumentModel.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IDocumentModel.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IDocx.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IDocx.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IGroupAssignmentSupport.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IGroupAssignmentSupport.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IRubric.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IRubric.java)
- [ReadinessResult.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/ReadinessResult.java)
- [RubricCriterion.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/RubricCriterion.java)
- [RubricItem.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/RubricItem.java)
- [RubricScore.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/RubricScore.java)
- [RubricView.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/RubricView.java)
- [SubmissionMode.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/SubmissionMode.java)
- [SubmissionWindow.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/SubmissionWindow.java)
- [ValidationIssue.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/ValidationIssue.java)
- [ValidationResult.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/ValidationResult.java)

## U08 Assignment Work

- [ActivityEventInput.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivityEventInput.java)
- [ActivityInterval.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivityInterval.java)
- [ActivityKind.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivityKind.java)
- [ActivityPolicyState.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivityPolicyState.java)
- [ActivitySummary.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivitySummary.java)
- [AttemptStatus.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/AttemptStatus.java)
- [GroupWorkStatus.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/GroupWorkStatus.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/IActivityQuery.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/IActivityQuery.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/IActivityWrite.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/IActivityWrite.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/IGroupWorkStatus.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/IGroupWorkStatus.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/ISubmissionListener.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ISubmissionListener.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/ISubmissionQuery.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ISubmissionQuery.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/IntervalCertainty.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/IntervalCertainty.java)
- [StudentSubmissionStatus.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/StudentSubmissionStatus.java)
- [SubmissionView.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/SubmissionView.java)
- [SubmitMode.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/SubmitMode.java)
- [WorkKind.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/WorkKind.java)
- [WorkRef.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/WorkRef.java)

## U09 Assignment Grading

- [GroupSectionAuthor.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/GroupSectionAuthor.java)
- [GroupSubmissionStatus.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/GroupSubmissionStatus.java)
- [GroupSubmissionView.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/GroupSubmissionView.java)
- [backend/src/main/java/edu/aiplatform/grading/contract/IGroupEvaluation.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/IGroupEvaluation.java)
- [backend/src/main/java/edu/aiplatform/grading/contract/IGroupSubmissionSource.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/IGroupSubmissionSource.java)

## U10 Quiz Authoring

- [AnswerKey.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/AnswerKey.java)
- [AnswerReview.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/AnswerReview.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizAttemptPresence.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizAttemptPresence.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizLifecycleListener.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizLifecycleListener.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizQuery.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizQuery.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizScoring.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizScoring.java)
- [LeavePenalty.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/LeavePenalty.java)
- [QuestionScore.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuestionScore.java)
- [QuestionType.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuestionType.java)
- [QuizAnswer.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizAnswer.java)
- [QuizOptionView.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizOptionView.java)
- [QuizQuestionView.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizQuestionView.java)
- [QuizScope.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizScope.java)
- [QuizScore.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizScore.java)
- [QuizSettings.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizSettings.java)
- [QuizStatus.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizStatus.java)
- [QuizStudentView.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizStudentView.java)
- [QuizView.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizView.java)

## U11 Quiz Practice

- Không có file thuộc nhóm này trong source snapshot.

## U12 Group Assignment

- [backend/src/main/java/edu/aiplatform/groupdocs/contract/ISseHub.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/contract/ISseHub.java)

## U13 Reference Materials

- Không có file thuộc nhóm này trong source snapshot.

## U14 Announcements & Notifications

- Không có file thuộc nhóm này trong source snapshot.

## U15 Admin Dashboard

- Không có file thuộc nhóm này trong source snapshot.
