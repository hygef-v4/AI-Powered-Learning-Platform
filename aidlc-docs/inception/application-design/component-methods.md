# Public contracts — AS-BUILT 2026-10-30

Source-observed at `794a405304bf02dd2593a9fa2c88662d7cd52845`. Canonical signatures are linked below; consumers must use these Java interfaces rather than old logical pseudocode. HTTP annotations: [api-inventory.md](../reverse-engineering/api-inventory.md).

## U01 Account & Access

- [backend/src/main/java/edu/aiplatform/identity/contract/IAccountLookup.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/IAccountLookup.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/IAuthorization.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/IAuthorization.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/IClassScope.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/IClassScope.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/IMail.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/IMail.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/ISubjectScope.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/ISubjectScope.java)

## U02 Audit

- [backend/src/main/java/edu/aiplatform/audit/contract/IAudit.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/IAudit.java)
- [backend/src/main/java/edu/aiplatform/audit/contract/IAuditQuery.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/IAuditQuery.java)
- [backend/src/main/java/edu/aiplatform/audit/contract/IAuditStats.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/IAuditStats.java)

## U03 Job and Settings

- [backend/src/main/java/edu/aiplatform/backup/contract/IBackupDashboardStatus.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/IBackupDashboardStatus.java)
- [backend/src/main/java/edu/aiplatform/backup/contract/IBackupStatus.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/IBackupStatus.java)
- [backend/src/main/java/edu/aiplatform/files/contract/IArtifact.java](../../../backend/src/main/java/edu/aiplatform/files/contract/IArtifact.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IEventPublisher.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IEventPublisher.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IJob.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IJob.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IJobHandler.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IJobHandler.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IPendingSweeper.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IPendingSweeper.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IScheduledScanner.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IScheduledScanner.java)
- [backend/src/main/java/edu/aiplatform/settings/contract/ISettings.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/ISettings.java)
- [backend/src/main/java/edu/aiplatform/settings/contract/ISettingsSectionProvider.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/ISettingsSectionProvider.java)

## U04 Subject Administration

- [backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectClasses.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectClasses.java)
- [backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectMutationGuard.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectMutationGuard.java)
- [backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectQuery.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectQuery.java)

## U05 Class & Enrollment

- [backend/src/main/java/edu/aiplatform/academics/contract/IClassAccess.java](../../../backend/src/main/java/edu/aiplatform/academics/contract/IClassAccess.java)

## U06 AI Connections

- [backend/src/main/java/edu/aiplatform/ai/contract/IAiTask.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IAiTask.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IAiTaskResultHandler.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IAiTaskResultHandler.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IAssignmentSourceTextExtractor.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IAssignmentSourceTextExtractor.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IMcpToolProvider.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IMcpToolProvider.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IMcpWriteRequest.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IMcpWriteRequest.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IReferenceMaterial.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IReferenceMaterial.java)

## U07 Assignment Authoring

- [backend/src/main/java/edu/aiplatform/assignments/contract/IAssignmentLifecycleListener.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IAssignmentLifecycleListener.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IAssignmentQuery.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IAssignmentQuery.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IDiagramLinker.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IDiagramLinker.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IDiagramResolver.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IDiagramResolver.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IDocumentModel.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IDocumentModel.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IDocx.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IDocx.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IGroupAssignmentSupport.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IGroupAssignmentSupport.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IRubric.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IRubric.java)

## U08 Assignment Work

- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/IActivityQuery.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/IActivityQuery.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/IActivityWrite.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/IActivityWrite.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/IGroupWorkStatus.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/IGroupWorkStatus.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/IntervalCertainty.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/IntervalCertainty.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/ISubmissionListener.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ISubmissionListener.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/ISubmissionQuery.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ISubmissionQuery.java)

## U09 Assignment Grading

- [backend/src/main/java/edu/aiplatform/grading/contract/IGroupEvaluation.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/IGroupEvaluation.java)
- [backend/src/main/java/edu/aiplatform/grading/contract/IGroupSubmissionSource.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/IGroupSubmissionSource.java)

## U10 Quiz Authoring

- [backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizLifecycleListener.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizLifecycleListener.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizQuery.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizQuery.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizScoring.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizScoring.java)

## U11 Quiz Practice

- Không có file tương ứng trong checkout; pending.

## U12 Group Assignment

- [backend/src/main/java/edu/aiplatform/groupdocs/contract/ISseHub.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/contract/ISseHub.java)

## U13 Reference Materials

- Không có file tương ứng trong checkout; pending.

## U14 Announcements & Notifications

- Không có file tương ứng trong checkout; pending.

## U15 Admin Dashboard

- Không có file tương ứng trong checkout; pending.
