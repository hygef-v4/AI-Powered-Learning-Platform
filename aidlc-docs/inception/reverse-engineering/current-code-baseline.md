# Current code baseline — 2026-10-30

**Source:** `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`. Brownfield documentation synchronization, source-observed. 66 UC (catalog and English table each contain IDs 01–66); 15 logical units, 50 MVP stories + 1 extension preserved. No runtime or fresh application-test claim.

Java 17 / Spring Boot 3.5.6 / Maven; Next.js 16.3.8 / React 19.3.0 / TypeScript / npm. One modular Spring Boot application and worker; PostgreSQL/Flyway, Redis, RabbitMQ, private Drive storage and SMTP. CI Java 17 / Node 22; deploy job disabled.

## Ownership manifest

| Unit | Responsibility | UC | Backend packages | Design folder |
|---|---|---|---|---|
| U01 | Account & Access | 01–12 | [identity](../../../backend/src/main/java/edu/aiplatform/identity) | [u01-account-and-access](../../construction/u01-account-and-access) |
| U02 | Audit | 13 | [audit](../../../backend/src/main/java/edu/aiplatform/audit) | [u02-audit-job-and-outbox](../../construction/u02-audit-job-and-outbox) |
| U03 | Job and Settings | 14–15 | [files](../../../backend/src/main/java/edu/aiplatform/files), [jobs](../../../backend/src/main/java/edu/aiplatform/jobs), [settings](../../../backend/src/main/java/edu/aiplatform/settings), [backup](../../../backend/src/main/java/edu/aiplatform/backup) | [u03-job-and-settings](../../construction/u03-job-and-settings) |
| U04 | Subject Administration | 16–19 | [subjects](../../../backend/src/main/java/edu/aiplatform/subjects) | [u04-subject-administration](../../construction/u04-subject-administration) |
| U05 | Class & Enrollment | 20–30 | [academics](../../../backend/src/main/java/edu/aiplatform/academics) | [u05-class-enrollment](../../construction/u05-class-enrollment) |
| U06 | AI Connections | 31–33 | [ai](../../../backend/src/main/java/edu/aiplatform/ai) | [u06-ai-connections](../../construction/u06-ai-connections) |
| U07 | Assignment Authoring | 34–36 | [assignments](../../../backend/src/main/java/edu/aiplatform/assignments) | [u07-assignment-authoring](../../construction/u07-assignment-authoring) |
| U08 | Assignment Work | 37–41 | [assignmentwork](../../../backend/src/main/java/edu/aiplatform/assignmentwork) | [u08-assignment-work](../../construction/u08-assignment-work) |
| U09 | Assignment Grading | 42–45 | [grading](../../../backend/src/main/java/edu/aiplatform/grading) | [u09-assignment-grading](../../construction/u09-assignment-grading) |
| U10 | Quiz Authoring | 46–48 | [quizzes](../../../backend/src/main/java/edu/aiplatform/quizzes) | [u10-quiz-authoring](../../construction/u10-quiz-authoring) |
| U11 | Quiz Practice | 49–52 | [quizpractice](../../../backend/src/main/java/edu/aiplatform/quizpractice) | [u11-quiz-practice](../../construction/u11-quiz-practice) |
| U12 | Group Assignment | 53–57 | [groups](../../../backend/src/main/java/edu/aiplatform/groups), [groupdocs](../../../backend/src/main/java/edu/aiplatform/groupdocs) | [u12-group-assignment](../../construction/u12-group-assignment) |
| U13 | Reference Materials | 58–62 | [materials](../../../backend/src/main/java/edu/aiplatform/materials) | [u13-reference-materials](../../construction/u13-reference-materials) |
| U14 | Announcements & Notifications | 63–65 | [announcements](../../../backend/src/main/java/edu/aiplatform/announcements), [notifications](../../../backend/src/main/java/edu/aiplatform/notifications) | [u14-announcements-and-notifications](../../construction/u14-announcements-and-notifications) |
| U15 | Admin Dashboard | 66 | [admindashboard](../../../backend/src/main/java/edu/aiplatform/admindashboard) | [u15-admin-dashboard](../../construction/u15-admin-dashboard) |

## U01 — Account & Access

FR: FR-001, FR-002, FR-015; primary UC 01–12. Source present; runtime/acceptance pending.

### Public contracts

- [backend/src/main/java/edu/aiplatform/identity/contract/AccountContact.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/AccountContact.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/AccountStatus.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/AccountStatus.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/Actions.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/Actions.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/Actor.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/Actor.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/AuthorizationDecision.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/AuthorizationDecision.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/IAccountLookup.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/IAccountLookup.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/IAuthorization.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/IAuthorization.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/IClassScope.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/IClassScope.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/IMail.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/IMail.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/ISubjectScope.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/ISubjectScope.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/MailMessage.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/MailMessage.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/ResourceRef.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/ResourceRef.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/Role.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/Role.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/ScopeKind.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/ScopeKind.java)

### Controllers

- [backend/src/main/java/edu/aiplatform/identity/web/AdminAccountController.java](../../../backend/src/main/java/edu/aiplatform/identity/web/AdminAccountController.java)
- [backend/src/main/java/edu/aiplatform/identity/web/AuthController.java](../../../backend/src/main/java/edu/aiplatform/identity/web/AuthController.java)
- [backend/src/main/java/edu/aiplatform/identity/web/MeController.java](../../../backend/src/main/java/edu/aiplatform/identity/web/MeController.java)
- [backend/src/main/java/edu/aiplatform/identity/web/OtpFlowController.java](../../../backend/src/main/java/edu/aiplatform/identity/web/OtpFlowController.java)

### Migrations

- [backend/src/main/resources/db/migration/identity/V20260925_0930__create_accounts.sql](../../../backend/src/main/resources/db/migration/identity/V20260925_0930__create_accounts.sql)
- [backend/src/main/resources/db/migration/identity/V20260925_0931__add_audit_actor_fk.sql](../../../backend/src/main/resources/db/migration/identity/V20260925_0931__add_audit_actor_fk.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/identity/application/AccountAdminServiceTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/AccountAdminServiceTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/AccountImportServiceTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/AccountImportServiceTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/AccountLookupAndPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/AccountLookupAndPolicyTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/AdminDashboardPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/AdminDashboardPolicyTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/AuthorizationServiceTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/AuthorizationServiceTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/AuthServiceTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/AuthServiceTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/GradingPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/GradingPolicyTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/McpPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/McpPolicyTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/NotificationAccountLookupTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/NotificationAccountLookupTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/OtpMailHandlerTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/OtpMailHandlerTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/OtpServiceTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/OtpServiceTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/PasswordCompletionTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/PasswordCompletionTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/ProfileAndPasswordChangeTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/ProfileAndPasswordChangeTest.java)
- [backend/src/test/java/edu/aiplatform/identity/application/SessionAuthorityTest.java](../../../backend/src/test/java/edu/aiplatform/identity/application/SessionAuthorityTest.java)
- [backend/src/test/java/edu/aiplatform/identity/domain/AccountRepositoryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/identity/domain/AccountRepositoryIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/identity/domain/AccountRepositoryTest.java](../../../backend/src/test/java/edu/aiplatform/identity/domain/AccountRepositoryTest.java)
- [backend/src/test/java/edu/aiplatform/identity/domain/AccountTest.java](../../../backend/src/test/java/edu/aiplatform/identity/domain/AccountTest.java)
- [backend/src/test/java/edu/aiplatform/identity/domain/EmailPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/domain/EmailPolicyTest.java)
- [backend/src/test/java/edu/aiplatform/identity/domain/PasswordPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/domain/PasswordPolicyTest.java)
- [backend/src/test/java/edu/aiplatform/identity/domain/ProfilePolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/domain/ProfilePolicyTest.java)
- [backend/src/test/java/edu/aiplatform/identity/infrastructure/JwtServiceTest.java](../../../backend/src/test/java/edu/aiplatform/identity/infrastructure/JwtServiceTest.java)
- [backend/src/test/java/edu/aiplatform/identity/infrastructure/OtpStoresTest.java](../../../backend/src/test/java/edu/aiplatform/identity/infrastructure/OtpStoresTest.java)
- [backend/src/test/java/edu/aiplatform/identity/infrastructure/RequestThrottleIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/identity/infrastructure/RequestThrottleIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/identity/infrastructure/SessionStoreIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/identity/infrastructure/SessionStoreIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/identity/infrastructure/SessionStoreTest.java](../../../backend/src/test/java/edu/aiplatform/identity/infrastructure/SessionStoreTest.java)
- [backend/src/test/java/edu/aiplatform/identity/web/IdentityWebSecurityTest.java](../../../backend/src/test/java/edu/aiplatform/identity/web/IdentityWebSecurityTest.java)


## U02 — Audit

FR: FR-014; primary UC 13. Source present; runtime/acceptance pending.

### Public contracts

- [backend/src/main/java/edu/aiplatform/audit/contract/AuditEvent.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/AuditEvent.java)
- [backend/src/main/java/edu/aiplatform/audit/contract/AuditFilter.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/AuditFilter.java)
- [backend/src/main/java/edu/aiplatform/audit/contract/AuditLogView.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/AuditLogView.java)
- [backend/src/main/java/edu/aiplatform/audit/contract/AuditResult.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/AuditResult.java)
- [backend/src/main/java/edu/aiplatform/audit/contract/IAudit.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/IAudit.java)
- [backend/src/main/java/edu/aiplatform/audit/contract/IAuditQuery.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/IAuditQuery.java)
- [backend/src/main/java/edu/aiplatform/audit/contract/IAuditStats.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/IAuditStats.java)

### Controllers

- [backend/src/main/java/edu/aiplatform/audit/web/AuditController.java](../../../backend/src/main/java/edu/aiplatform/audit/web/AuditController.java)

### Migrations

- [backend/src/main/resources/db/migration/audit/V20260925_0900__create_audit_logs.sql](../../../backend/src/main/resources/db/migration/audit/V20260925_0900__create_audit_logs.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/audit/application/AuditQueryServiceTest.java](../../../backend/src/test/java/edu/aiplatform/audit/application/AuditQueryServiceTest.java)
- [backend/src/test/java/edu/aiplatform/audit/application/AuditStoreTest.java](../../../backend/src/test/java/edu/aiplatform/audit/application/AuditStoreTest.java)
- [backend/src/test/java/edu/aiplatform/audit/domain/AuditLogRepositoryWhereTest.java](../../../backend/src/test/java/edu/aiplatform/audit/domain/AuditLogRepositoryWhereTest.java)
- [backend/src/test/java/edu/aiplatform/audit/web/AuditControllerTest.java](../../../backend/src/test/java/edu/aiplatform/audit/web/AuditControllerTest.java)


## U03 — Job and Settings

FR: FR-013, FR-033, FR-038; primary UC 14–15. Source present; runtime/acceptance pending.

### Public contracts

- [backend/src/main/java/edu/aiplatform/backup/contract/BackupDashboardRunView.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/BackupDashboardRunView.java)
- [backend/src/main/java/edu/aiplatform/backup/contract/BackupDashboardSummary.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/BackupDashboardSummary.java)
- [backend/src/main/java/edu/aiplatform/backup/contract/BackupRunView.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/BackupRunView.java)
- [backend/src/main/java/edu/aiplatform/backup/contract/BackupState.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/BackupState.java)
- [backend/src/main/java/edu/aiplatform/backup/contract/IBackupDashboardStatus.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/IBackupDashboardStatus.java)
- [backend/src/main/java/edu/aiplatform/backup/contract/IBackupStatus.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/IBackupStatus.java)
- [backend/src/main/java/edu/aiplatform/files/contract/ArtifactPurpose.java](../../../backend/src/main/java/edu/aiplatform/files/contract/ArtifactPurpose.java)
- [backend/src/main/java/edu/aiplatform/files/contract/DownloadToken.java](../../../backend/src/main/java/edu/aiplatform/files/contract/DownloadToken.java)
- [backend/src/main/java/edu/aiplatform/files/contract/FileRef.java](../../../backend/src/main/java/edu/aiplatform/files/contract/FileRef.java)
- [backend/src/main/java/edu/aiplatform/files/contract/FileUpload.java](../../../backend/src/main/java/edu/aiplatform/files/contract/FileUpload.java)
- [backend/src/main/java/edu/aiplatform/files/contract/IArtifact.java](../../../backend/src/main/java/edu/aiplatform/files/contract/IArtifact.java)
- [backend/src/main/java/edu/aiplatform/files/contract/StoredFileInfo.java](../../../backend/src/main/java/edu/aiplatform/files/contract/StoredFileInfo.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/DomainEvent.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/DomainEvent.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/EventTypes.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/EventTypes.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IEventPublisher.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IEventPublisher.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IJob.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IJob.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IJobHandler.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IJobHandler.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IPendingSweeper.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IPendingSweeper.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/IScheduledScanner.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/IScheduledScanner.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/JobMessage.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/JobMessage.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/JobQueue.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/JobQueue.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/JobTypes.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/JobTypes.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/PendingJob.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/PendingJob.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/RetryableJobException.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/RetryableJobException.java)
- [backend/src/main/java/edu/aiplatform/settings/contract/ISettings.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/ISettings.java)
- [backend/src/main/java/edu/aiplatform/settings/contract/ISettingsSectionProvider.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/ISettingsSectionProvider.java)
- [backend/src/main/java/edu/aiplatform/settings/contract/SettingDefinition.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/SettingDefinition.java)
- [backend/src/main/java/edu/aiplatform/settings/contract/SettingGroup.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/SettingGroup.java)
- [backend/src/main/java/edu/aiplatform/settings/contract/SettingKey.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/SettingKey.java)
- [backend/src/main/java/edu/aiplatform/settings/contract/SettingType.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/SettingType.java)

### Controllers

- [backend/src/main/java/edu/aiplatform/files/web/DownloadController.java](../../../backend/src/main/java/edu/aiplatform/files/web/DownloadController.java)
- [backend/src/main/java/edu/aiplatform/files/web/FileController.java](../../../backend/src/main/java/edu/aiplatform/files/web/FileController.java)
- [backend/src/main/java/edu/aiplatform/settings/web/SettingsController.java](../../../backend/src/main/java/edu/aiplatform/settings/web/SettingsController.java)

### Migrations

- [backend/src/main/resources/db/migration/backup/V20260925_0920__create_backup_tables.sql](../../../backend/src/main/resources/db/migration/backup/V20260925_0920__create_backup_tables.sql)
- [backend/src/main/resources/db/migration/settings/V20260925_0910__create_system_settings.sql](../../../backend/src/main/resources/db/migration/settings/V20260925_0910__create_system_settings.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/backup/application/BackupDashboardContractTest.java](../../../backend/src/test/java/edu/aiplatform/backup/application/BackupDashboardContractTest.java)
- [backend/src/test/java/edu/aiplatform/backup/application/BackupEncryptorTest.java](../../../backend/src/test/java/edu/aiplatform/backup/application/BackupEncryptorTest.java)
- [backend/src/test/java/edu/aiplatform/backup/application/BackupJobHandlerTest.java](../../../backend/src/test/java/edu/aiplatform/backup/application/BackupJobHandlerTest.java)
- [backend/src/test/java/edu/aiplatform/backup/application/BackupRetentionServiceTest.java](../../../backend/src/test/java/edu/aiplatform/backup/application/BackupRetentionServiceTest.java)
- [backend/src/test/java/edu/aiplatform/backup/application/BackupScheduleScannerTest.java](../../../backend/src/test/java/edu/aiplatform/backup/application/BackupScheduleScannerTest.java)
- [backend/src/test/java/edu/aiplatform/backup/application/BackupStatusServiceTest.java](../../../backend/src/test/java/edu/aiplatform/backup/application/BackupStatusServiceTest.java)
- [backend/src/test/java/edu/aiplatform/backup/contract/stub/BackupDashboardUnavailableTest.java](../../../backend/src/test/java/edu/aiplatform/backup/contract/stub/BackupDashboardUnavailableTest.java)
- [backend/src/test/java/edu/aiplatform/backup/infrastructure/BackupDashboardStatusIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/backup/infrastructure/BackupDashboardStatusIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/backup/infrastructure/LocalBackupStoreTest.java](../../../backend/src/test/java/edu/aiplatform/backup/infrastructure/LocalBackupStoreTest.java)
- [backend/src/test/java/edu/aiplatform/files/application/ArtifactServiceTest.java](../../../backend/src/test/java/edu/aiplatform/files/application/ArtifactServiceTest.java)
- [backend/src/test/java/edu/aiplatform/files/application/ContentInspectorTest.java](../../../backend/src/test/java/edu/aiplatform/files/application/ContentInspectorTest.java)
- [backend/src/test/java/edu/aiplatform/files/application/DownloadServiceTest.java](../../../backend/src/test/java/edu/aiplatform/files/application/DownloadServiceTest.java)
- [backend/src/test/java/edu/aiplatform/files/application/DownloadTokenServiceTest.java](../../../backend/src/test/java/edu/aiplatform/files/application/DownloadTokenServiceTest.java)
- [backend/src/test/java/edu/aiplatform/files/application/FileRefSignerTest.java](../../../backend/src/test/java/edu/aiplatform/files/application/FileRefSignerTest.java)
- [backend/src/test/java/edu/aiplatform/files/application/UploadServiceTest.java](../../../backend/src/test/java/edu/aiplatform/files/application/UploadServiceTest.java)
- [backend/src/test/java/edu/aiplatform/files/domain/FileNameSanitizerTest.java](../../../backend/src/test/java/edu/aiplatform/files/domain/FileNameSanitizerTest.java)
- [backend/src/test/java/edu/aiplatform/files/domain/PurposePolicyTest.java](../../../backend/src/test/java/edu/aiplatform/files/domain/PurposePolicyTest.java)
- [backend/src/test/java/edu/aiplatform/files/infrastructure/LocalFolderStorageAdapterTest.java](../../../backend/src/test/java/edu/aiplatform/files/infrastructure/LocalFolderStorageAdapterTest.java)
- [backend/src/test/java/edu/aiplatform/jobs/application/EventPublisherTest.java](../../../backend/src/test/java/edu/aiplatform/jobs/application/EventPublisherTest.java)
- [backend/src/test/java/edu/aiplatform/jobs/application/JobDispatcherTest.java](../../../backend/src/test/java/edu/aiplatform/jobs/application/JobDispatcherTest.java)
- [backend/src/test/java/edu/aiplatform/jobs/application/JobHandlerRegistryTest.java](../../../backend/src/test/java/edu/aiplatform/jobs/application/JobHandlerRegistryTest.java)
- [backend/src/test/java/edu/aiplatform/jobs/application/JobPublisherTest.java](../../../backend/src/test/java/edu/aiplatform/jobs/application/JobPublisherTest.java)
- [backend/src/test/java/edu/aiplatform/jobs/application/RunnersTest.java](../../../backend/src/test/java/edu/aiplatform/jobs/application/RunnersTest.java)
- [backend/src/test/java/edu/aiplatform/settings/application/SettingsSectionRegistryTest.java](../../../backend/src/test/java/edu/aiplatform/settings/application/SettingsSectionRegistryTest.java)
- [backend/src/test/java/edu/aiplatform/settings/application/SettingsServiceTest.java](../../../backend/src/test/java/edu/aiplatform/settings/application/SettingsServiceTest.java)
- [backend/src/test/java/edu/aiplatform/settings/domain/SettingValidatorTest.java](../../../backend/src/test/java/edu/aiplatform/settings/domain/SettingValidatorTest.java)


## U04 — Subject Administration

FR: FR-003; primary UC 16–19. Source present; runtime/acceptance pending.

### Public contracts

- [backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectClasses.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectClasses.java)
- [backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectMutationGuard.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectMutationGuard.java)
- [backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectQuery.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/ISubjectQuery.java)
- [backend/src/main/java/edu/aiplatform/subjects/contract/SubjectClassSummary.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/SubjectClassSummary.java)
- [backend/src/main/java/edu/aiplatform/subjects/contract/SubjectRef.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/SubjectRef.java)
- [backend/src/main/java/edu/aiplatform/subjects/contract/SubjectStatus.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/SubjectStatus.java)

### Controllers

- [backend/src/main/java/edu/aiplatform/subjects/web/SubjectAdminController.java](../../../backend/src/main/java/edu/aiplatform/subjects/web/SubjectAdminController.java)

### Migrations

- [backend/src/main/resources/db/migration/subjects/V20260925_1000__create_subjects.sql](../../../backend/src/main/resources/db/migration/subjects/V20260925_1000__create_subjects.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/subjects/application/SubjectAdminCommandTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectAdminCommandTest.java)
- [backend/src/test/java/edu/aiplatform/subjects/application/SubjectAdminReadTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectAdminReadTest.java)
- [backend/src/test/java/edu/aiplatform/subjects/application/SubjectAuditTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectAuditTest.java)
- [backend/src/test/java/edu/aiplatform/subjects/application/SubjectClassesReaderTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectClassesReaderTest.java)
- [backend/src/test/java/edu/aiplatform/subjects/application/SubjectContractsWiringTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectContractsWiringTest.java)
- [backend/src/test/java/edu/aiplatform/subjects/application/SubjectMutationGuardTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectMutationGuardTest.java)
- [backend/src/test/java/edu/aiplatform/subjects/application/SubjectQueryServiceTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectQueryServiceTest.java)
- [backend/src/test/java/edu/aiplatform/subjects/application/SubjectScopeServiceTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectScopeServiceTest.java)
- [backend/src/test/java/edu/aiplatform/subjects/application/SubjectStatusTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/application/SubjectStatusTest.java)
- [backend/src/test/java/edu/aiplatform/subjects/domain/SubjectCodeTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/domain/SubjectCodeTest.java)
- [backend/src/test/java/edu/aiplatform/subjects/domain/SubjectTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/domain/SubjectTest.java)
- [backend/src/test/java/edu/aiplatform/subjects/infrastructure/SubjectRepositoryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/infrastructure/SubjectRepositoryIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/subjects/web/SubjectAdminControllerTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/web/SubjectAdminControllerTest.java)


## U05 — Class & Enrollment

FR: FR-003, FR-005, FR-024; primary UC 20–30. Source present; runtime/acceptance pending.

### Public contracts

- [backend/src/main/java/edu/aiplatform/academics/contract/ClassRef.java](../../../backend/src/main/java/edu/aiplatform/academics/contract/ClassRef.java)
- [backend/src/main/java/edu/aiplatform/academics/contract/ClassStatus.java](../../../backend/src/main/java/edu/aiplatform/academics/contract/ClassStatus.java)
- [backend/src/main/java/edu/aiplatform/academics/contract/IClassAccess.java](../../../backend/src/main/java/edu/aiplatform/academics/contract/IClassAccess.java)

### Controllers

- [backend/src/main/java/edu/aiplatform/academics/web/AcademicsController.java](../../../backend/src/main/java/edu/aiplatform/academics/web/AcademicsController.java)

### Migrations

- [backend/src/main/resources/db/migration/classes/V20260925_1100__create_classes_enrollments.sql](../../../backend/src/main/resources/db/migration/classes/V20260925_1100__create_classes_enrollments.sql)
- [backend/src/main/resources/db/migration/classes/V20261027_1100__allow_open_class_without_teacher.sql](../../../backend/src/main/resources/db/migration/classes/V20261027_1100__allow_open_class_without_teacher.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/academics/application/AcademicsAccessAndReadTest.java](../../../backend/src/test/java/edu/aiplatform/academics/application/AcademicsAccessAndReadTest.java)
- [backend/src/test/java/edu/aiplatform/academics/application/AcademicsBatchTransactionTest.java](../../../backend/src/test/java/edu/aiplatform/academics/application/AcademicsBatchTransactionTest.java)
- [backend/src/test/java/edu/aiplatform/academics/application/AcademicsMcpToolsTest.java](../../../backend/src/test/java/edu/aiplatform/academics/application/AcademicsMcpToolsTest.java)
- [backend/src/test/java/edu/aiplatform/academics/application/AcademicsServiceTest.java](../../../backend/src/test/java/edu/aiplatform/academics/application/AcademicsServiceTest.java)
- [backend/src/test/java/edu/aiplatform/academics/domain/CourseClassTest.java](../../../backend/src/test/java/edu/aiplatform/academics/domain/CourseClassTest.java)
- [backend/src/test/java/edu/aiplatform/academics/domain/EnrollmentListParserTest.java](../../../backend/src/test/java/edu/aiplatform/academics/domain/EnrollmentListParserTest.java)
- [backend/src/test/java/edu/aiplatform/academics/domain/EnrollmentTest.java](../../../backend/src/test/java/edu/aiplatform/academics/domain/EnrollmentTest.java)
- [backend/src/test/java/edu/aiplatform/academics/infrastructure/AcademicsContractAdapterTest.java](../../../backend/src/test/java/edu/aiplatform/academics/infrastructure/AcademicsContractAdapterTest.java)
- [backend/src/test/java/edu/aiplatform/academics/infrastructure/AcademicsPersistenceTest.java](../../../backend/src/test/java/edu/aiplatform/academics/infrastructure/AcademicsPersistenceTest.java)
- [backend/src/test/java/edu/aiplatform/academics/infrastructure/AcademicsReadRepositoryTest.java](../../../backend/src/test/java/edu/aiplatform/academics/infrastructure/AcademicsReadRepositoryTest.java)
- [backend/src/test/java/edu/aiplatform/academics/web/AcademicsApiContractTest.java](../../../backend/src/test/java/edu/aiplatform/academics/web/AcademicsApiContractTest.java)
- [backend/src/test/java/edu/aiplatform/academics/web/AcademicsControllerTest.java](../../../backend/src/test/java/edu/aiplatform/academics/web/AcademicsControllerTest.java)


## U06 — AI Connections

FR: FR-012, FR-021, FR-034, FR-035, FR-036, FR-037; primary UC 31–33. Source present; runtime/acceptance pending.

### Public contracts

- [backend/src/main/java/edu/aiplatform/ai/contract/AiPrompt.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiPrompt.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/AiProvider.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiProvider.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/AiSource.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiSource.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/AiTaskFailure.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiTaskFailure.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/AiTaskRequest.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiTaskRequest.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/AiTaskResult.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiTaskResult.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/AiTaskState.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiTaskState.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/AiTaskView.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiTaskView.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/AiTokenEstimate.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiTokenEstimate.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/AiUsageSummary.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AiUsageSummary.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/ApplyOutcome.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/ApplyOutcome.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/AssignmentSourceFile.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AssignmentSourceFile.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/AssignmentSourceText.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/AssignmentSourceText.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IAiTask.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IAiTask.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IAiTaskResultHandler.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IAiTaskResultHandler.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IAssignmentSourceTextExtractor.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IAssignmentSourceTextExtractor.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IMcpToolProvider.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IMcpToolProvider.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IMcpWriteRequest.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IMcpWriteRequest.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/IReferenceMaterial.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/IReferenceMaterial.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/MaterialText.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/MaterialText.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/McpFingerprint.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/McpFingerprint.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/McpInput.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/McpInput.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/McpSchemas.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/McpSchemas.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/McpToolDefinition.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/McpToolDefinition.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/McpToolKind.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/McpToolKind.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/McpToolResult.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/McpToolResult.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/SelectableMaterial.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/SelectableMaterial.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/UsageMeasurement.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/UsageMeasurement.java)

### Controllers

- [backend/src/main/java/edu/aiplatform/ai/web/AiAdminController.java](../../../backend/src/main/java/edu/aiplatform/ai/web/AiAdminController.java)
- [backend/src/main/java/edu/aiplatform/ai/web/AiConnectionController.java](../../../backend/src/main/java/edu/aiplatform/ai/web/AiConnectionController.java)
- [backend/src/main/java/edu/aiplatform/ai/web/AiUsageController.java](../../../backend/src/main/java/edu/aiplatform/ai/web/AiUsageController.java)
- [backend/src/main/java/edu/aiplatform/ai/web/McpController.java](../../../backend/src/main/java/edu/aiplatform/ai/web/McpController.java)
- [backend/src/main/java/edu/aiplatform/ai/web/McpCredentialController.java](../../../backend/src/main/java/edu/aiplatform/ai/web/McpCredentialController.java)

### Migrations

- [backend/src/main/resources/db/migration/ai/V20260925_1200__create_ai_tables.sql](../../../backend/src/main/resources/db/migration/ai/V20260925_1200__create_ai_tables.sql)
- [backend/src/main/resources/db/migration/ai/V20261026_1800__create_mcp_write_requests.sql](../../../backend/src/main/resources/db/migration/ai/V20261026_1800__create_mcp_write_requests.sql)
- [backend/src/main/resources/db/migration/ai/V20261027_0900__allow_many_institutional_keys.sql](../../../backend/src/main/resources/db/migration/ai/V20261027_0900__allow_many_institutional_keys.sql)
- [backend/src/main/resources/db/migration/ai/V20261027_1000__store_mcp_token_ciphertext.sql](../../../backend/src/main/resources/db/migration/ai/V20261027_1000__store_mcp_token_ciphertext.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/ai/application/AdminAiMcpToolsTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/AdminAiMcpToolsTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/AiConnectionServiceTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/AiConnectionServiceTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/AiSettingsSectionsTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/AiSettingsSectionsTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/AiSourceServiceTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/AiSourceServiceTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/AiTaskFlowTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/AiTaskFlowTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/AiTaskResultHandlerRegistryTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/AiTaskResultHandlerRegistryTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/AiUsageServiceTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/AiUsageServiceTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/KeyCipherAndCatalogTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/KeyCipherAndCatalogTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/McpCredentialServiceTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/McpCredentialServiceTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/McpServerTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/McpServerTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/McpStaffAdmissionTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/McpStaffAdmissionTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/McpStaffCatalogTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/McpStaffCatalogTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/McpWriteRequestIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/McpWriteRequestIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/ai/application/QuotaServicesTest.java](../../../backend/src/test/java/edu/aiplatform/ai/application/QuotaServicesTest.java)
- [backend/src/test/java/edu/aiplatform/ai/contract/AiTokenEstimateTest.java](../../../backend/src/test/java/edu/aiplatform/ai/contract/AiTokenEstimateTest.java)
- [backend/src/test/java/edu/aiplatform/ai/contract/McpInputTest.java](../../../backend/src/test/java/edu/aiplatform/ai/contract/McpInputTest.java)
- [backend/src/test/java/edu/aiplatform/ai/domain/AiDomainTest.java](../../../backend/src/test/java/edu/aiplatform/ai/domain/AiDomainTest.java)
- [backend/src/test/java/edu/aiplatform/ai/domain/AiRepositoriesIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/ai/domain/AiRepositoriesIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/ai/infrastructure/HttpProviderGatewayTest.java](../../../backend/src/test/java/edu/aiplatform/ai/infrastructure/HttpProviderGatewayTest.java)
- [backend/src/test/java/edu/aiplatform/ai/web/AiControllersTest.java](../../../backend/src/test/java/edu/aiplatform/ai/web/AiControllersTest.java)


## U07 — Assignment Authoring

FR: FR-006, FR-007, FR-016, FR-017, FR-027, FR-028; primary UC 34–36. Source present; runtime/acceptance pending.

### Public contracts

- [backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentContent.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentContent.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentDayCount.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentDayCount.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentScope.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentScope.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentStatus.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentStatus.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentView.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/AssignmentView.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/BlockOrigin.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/BlockOrigin.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/BlockType.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/BlockType.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/CriterionKind.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/CriterionKind.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/DiagramType.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/DiagramType.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/DocumentBlock.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/DocumentBlock.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/DocumentFiles.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/DocumentFiles.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/DocumentModel.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/DocumentModel.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/DocumentPart.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/DocumentPart.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/DocxImportPreview.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/DocxImportPreview.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/GradingMode.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/GradingMode.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IAssignmentLifecycleListener.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IAssignmentLifecycleListener.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IAssignmentQuery.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IAssignmentQuery.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IDiagramLinker.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IDiagramLinker.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IDiagramResolver.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IDiagramResolver.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IDocumentModel.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IDocumentModel.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IDocx.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IDocx.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IGroupAssignmentSupport.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IGroupAssignmentSupport.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/IRubric.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/IRubric.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/ReadinessResult.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/ReadinessResult.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/RubricCriterion.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/RubricCriterion.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/RubricItem.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/RubricItem.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/RubricScore.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/RubricScore.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/RubricView.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/RubricView.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/SubmissionMode.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/SubmissionMode.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/SubmissionWindow.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/SubmissionWindow.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/ValidationIssue.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/ValidationIssue.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/ValidationResult.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/ValidationResult.java)

### Controllers

- [backend/src/main/java/edu/aiplatform/assignments/web/AssignmentAiDraftController.java](../../../backend/src/main/java/edu/aiplatform/assignments/web/AssignmentAiDraftController.java)
- [backend/src/main/java/edu/aiplatform/assignments/web/AssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignments/web/AssignmentController.java)
- [backend/src/main/java/edu/aiplatform/assignments/web/ClassAssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignments/web/ClassAssignmentController.java)
- [backend/src/main/java/edu/aiplatform/assignments/web/RubricController.java](../../../backend/src/main/java/edu/aiplatform/assignments/web/RubricController.java)
- [backend/src/main/java/edu/aiplatform/assignments/web/SubjectAssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignments/web/SubjectAssignmentController.java)

### Migrations

- [backend/src/main/resources/db/migration/assignments/V20260925_1300__create_assignment_tables.sql](../../../backend/src/main/resources/db/migration/assignments/V20260925_1300__create_assignment_tables.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/assignments/application/AssignmentAiDraftServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/application/AssignmentAiDraftServiceTest.java)
- [backend/src/test/java/edu/aiplatform/assignments/application/AssignmentAuthoringServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/application/AssignmentAuthoringServiceTest.java)
- [backend/src/test/java/edu/aiplatform/assignments/application/AssignmentCopyServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/application/AssignmentCopyServiceTest.java)
- [backend/src/test/java/edu/aiplatform/assignments/application/AssignmentDraftGenerationSchemaTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/application/AssignmentDraftGenerationSchemaTest.java)
- [backend/src/test/java/edu/aiplatform/assignments/application/AssignmentDraftPromptBuilderTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/application/AssignmentDraftPromptBuilderTest.java)
- [backend/src/test/java/edu/aiplatform/assignments/application/AssignmentDraftValidatorTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/application/AssignmentDraftValidatorTest.java)
- [backend/src/test/java/edu/aiplatform/assignments/application/AssignmentMcpAuthoringServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/application/AssignmentMcpAuthoringServiceTest.java)
- [backend/src/test/java/edu/aiplatform/assignments/application/AssignmentMcpToolsTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/application/AssignmentMcpToolsTest.java)
- [backend/src/test/java/edu/aiplatform/assignments/application/AssignmentPublicationServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/application/AssignmentPublicationServiceTest.java)
- [backend/src/test/java/edu/aiplatform/assignments/application/AssignmentQueryServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/application/AssignmentQueryServiceTest.java)
- [backend/src/test/java/edu/aiplatform/assignments/application/DocumentModelServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/application/DocumentModelServiceTest.java)
- [backend/src/test/java/edu/aiplatform/assignments/application/DocxRoundTripTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/application/DocxRoundTripTest.java)
- [backend/src/test/java/edu/aiplatform/assignments/domain/PartCalculatorTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/domain/PartCalculatorTest.java)


## U08 — Assignment Work

FR: FR-018, FR-030, FR-039; primary UC 37–41. Source present; runtime/acceptance pending.

### Public contracts

- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivityEventInput.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivityEventInput.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivityInterval.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivityInterval.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivityKind.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivityKind.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivityPolicyState.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivityPolicyState.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivitySummary.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ActivitySummary.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/AttemptStatus.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/AttemptStatus.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/GroupWorkStatus.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/GroupWorkStatus.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/IActivityQuery.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/IActivityQuery.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/IActivityWrite.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/IActivityWrite.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/IGroupWorkStatus.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/IGroupWorkStatus.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/IntervalCertainty.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/IntervalCertainty.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/ISubmissionListener.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ISubmissionListener.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/ISubmissionQuery.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/ISubmissionQuery.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/StudentSubmissionStatus.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/StudentSubmissionStatus.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/SubmissionView.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/SubmissionView.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/SubmitMode.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/SubmitMode.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/WorkKind.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/WorkKind.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/WorkRef.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/WorkRef.java)

### Controllers

- [backend/src/main/java/edu/aiplatform/assignmentwork/web/AttemptController.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/web/AttemptController.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/web/StudentAssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/web/StudentAssignmentController.java)

### Migrations

- [backend/src/main/resources/db/migration/assignmentwork/V20260925_1400__create_assignment_work_tables.sql](../../../backend/src/main/resources/db/migration/assignmentwork/V20260925_1400__create_assignment_work_tables.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/assignmentwork/application/ActivityTest.java](../../../backend/src/test/java/edu/aiplatform/assignmentwork/application/ActivityTest.java)
- [backend/src/test/java/edu/aiplatform/assignmentwork/application/AttemptServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignmentwork/application/AttemptServiceTest.java)
- [backend/src/test/java/edu/aiplatform/assignmentwork/application/PracticeFeedbackServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignmentwork/application/PracticeFeedbackServiceTest.java)
- [backend/src/test/java/edu/aiplatform/assignmentwork/application/StudentAssignmentServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignmentwork/application/StudentAssignmentServiceTest.java)


## U09 — Assignment Grading

FR: FR-008, FR-009, FR-020, FR-024; primary UC 42–45. Source present; runtime/acceptance pending.

### Public contracts

- [backend/src/main/java/edu/aiplatform/grading/contract/GroupSectionAuthor.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/GroupSectionAuthor.java)
- [backend/src/main/java/edu/aiplatform/grading/contract/GroupSubmissionStatus.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/GroupSubmissionStatus.java)
- [backend/src/main/java/edu/aiplatform/grading/contract/GroupSubmissionView.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/GroupSubmissionView.java)
- [backend/src/main/java/edu/aiplatform/grading/contract/IGroupEvaluation.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/IGroupEvaluation.java)
- [backend/src/main/java/edu/aiplatform/grading/contract/IGroupSubmissionSource.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/IGroupSubmissionSource.java)

### Controllers

- [backend/src/main/java/edu/aiplatform/grading/web/EvaluationController.java](../../../backend/src/main/java/edu/aiplatform/grading/web/EvaluationController.java)
- [backend/src/main/java/edu/aiplatform/grading/web/GradebookController.java](../../../backend/src/main/java/edu/aiplatform/grading/web/GradebookController.java)
- [backend/src/main/java/edu/aiplatform/grading/web/GradingProposalController.java](../../../backend/src/main/java/edu/aiplatform/grading/web/GradingProposalController.java)
- [backend/src/main/java/edu/aiplatform/grading/web/MyGradeController.java](../../../backend/src/main/java/edu/aiplatform/grading/web/MyGradeController.java)
- [backend/src/main/java/edu/aiplatform/grading/web/SubmissionImageController.java](../../../backend/src/main/java/edu/aiplatform/grading/web/SubmissionImageController.java)
- [backend/src/main/java/edu/aiplatform/grading/web/SubmissionReviewController.java](../../../backend/src/main/java/edu/aiplatform/grading/web/SubmissionReviewController.java)

### Migrations

- [backend/src/main/resources/db/migration/grading/V20260925_1500__create_grading_tables.sql](../../../backend/src/main/resources/db/migration/grading/V20260925_1500__create_grading_tables.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/grading/application/EvaluationLifecycleServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/EvaluationLifecycleServiceTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradebookExporterTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradebookExporterTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradebookServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradebookServiceTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradingAccessTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingAccessTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradingBatchTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingBatchTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradingCommandsTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingCommandsTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradingMcpBatchServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingMcpBatchServiceTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradingMcpQueryServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingMcpQueryServiceTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradingMcpReplayTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingMcpReplayTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradingMcpToolsTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingMcpToolsTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradingPromptSchemaTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingPromptSchemaTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradingProposalResultHandlerTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingProposalResultHandlerTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradingProposalServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingProposalServiceTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradingProposalValidatorTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingProposalValidatorTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GradingServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GradingServiceTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/GroupEvaluationTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/GroupEvaluationTest.java)
- [backend/src/test/java/edu/aiplatform/grading/application/SubmissionImageServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/application/SubmissionImageServiceTest.java)
- [backend/src/test/java/edu/aiplatform/grading/contract/stub/GroupSourceUnavailableTest.java](../../../backend/src/test/java/edu/aiplatform/grading/contract/stub/GroupSourceUnavailableTest.java)
- [backend/src/test/java/edu/aiplatform/grading/domain/EvaluationTest.java](../../../backend/src/test/java/edu/aiplatform/grading/domain/EvaluationTest.java)
- [backend/src/test/java/edu/aiplatform/grading/domain/GradeDistributionCalculatorTest.java](../../../backend/src/test/java/edu/aiplatform/grading/domain/GradeDistributionCalculatorTest.java)
- [backend/src/test/java/edu/aiplatform/grading/infrastructure/EvaluationMappingTest.java](../../../backend/src/test/java/edu/aiplatform/grading/infrastructure/EvaluationMappingTest.java)
- [backend/src/test/java/edu/aiplatform/grading/infrastructure/EvaluationVersionIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/grading/infrastructure/EvaluationVersionIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/grading/infrastructure/GradingPersistenceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/infrastructure/GradingPersistenceTest.java)


## U10 — Quiz Authoring

FR: FR-006, FR-007, FR-016, FR-017, FR-028; primary UC 46–48. Source present; runtime/acceptance pending.

### Public contracts

- [backend/src/main/java/edu/aiplatform/quizzes/contract/AnswerKey.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/AnswerKey.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/AnswerReview.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/AnswerReview.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizLifecycleListener.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizLifecycleListener.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizQuery.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizQuery.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizScoring.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/IQuizScoring.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/QuestionScore.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuestionScore.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/QuestionType.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuestionType.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/QuizAnswer.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizAnswer.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/QuizOptionView.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizOptionView.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/QuizQuestionView.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizQuestionView.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/QuizScope.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizScope.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/QuizScore.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizScore.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/QuizSettings.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizSettings.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/QuizStatus.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizStatus.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/QuizStudentView.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizStudentView.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/QuizView.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/QuizView.java)

### Controllers

- [backend/src/main/java/edu/aiplatform/quizzes/web/ClassQuizController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/web/ClassQuizController.java)
- [backend/src/main/java/edu/aiplatform/quizzes/web/QuestionController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/web/QuestionController.java)
- [backend/src/main/java/edu/aiplatform/quizzes/web/QuizController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/web/QuizController.java)
- [backend/src/main/java/edu/aiplatform/quizzes/web/SubjectQuizController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/web/SubjectQuizController.java)

### Migrations

- [backend/src/main/resources/db/migration/quizzes/V20260925_1600__create_quiz_tables.sql](../../../backend/src/main/resources/db/migration/quizzes/V20260925_1600__create_quiz_tables.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/quizzes/application/QuestionBankMcpToolsTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/application/QuestionBankMcpToolsTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/application/QuestionBankServiceTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/application/QuestionBankServiceTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/application/QuestionImporterTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/application/QuestionImporterTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/application/QuizAiDraftServiceTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/application/QuizAiDraftServiceTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/application/QuizDraftPromptTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/application/QuizDraftPromptTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/application/QuizMcpAuthoringServiceTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/application/QuizMcpAuthoringServiceTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/application/QuizMcpToolsSchemaTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/application/QuizMcpToolsSchemaTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/application/QuizServicesTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/application/QuizServicesTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/domain/QuestionRulesTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/domain/QuestionRulesTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/domain/QuestionTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/domain/QuestionTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/domain/QuizTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/domain/QuizTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/infrastructure/JdbcQuestionRepositoryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/infrastructure/JdbcQuestionRepositoryIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/infrastructure/JdbcQuizRepositoryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/infrastructure/JdbcQuizRepositoryIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/web/QuestionControllerTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/web/QuestionControllerTest.java)
- [backend/src/test/java/edu/aiplatform/quizzes/web/QuizControllersTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/web/QuizControllersTest.java)


## U11 — Quiz Practice

FR: FR-017, FR-018, FR-039; primary UC 49–52. Source present; runtime/acceptance pending.

### Public contracts

- Không có file tương ứng trong checkout; pending.

### Controllers

- [backend/src/main/java/edu/aiplatform/quizpractice/web/QuizPracticeController.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/web/QuizPracticeController.java)
- [backend/src/main/java/edu/aiplatform/quizpractice/web/TeacherQuizAttemptController.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/web/TeacherQuizAttemptController.java)

### Migrations

- [backend/src/main/resources/db/migration/quizpractice/V20260925_1700__create_quiz_attempts.sql](../../../backend/src/main/resources/db/migration/quizpractice/V20260925_1700__create_quiz_attempts.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/quizpractice/application/QuizPracticeServicesTest.java](../../../backend/src/test/java/edu/aiplatform/quizpractice/application/QuizPracticeServicesTest.java)
- [backend/src/test/java/edu/aiplatform/quizpractice/domain/QuizAttemptTest.java](../../../backend/src/test/java/edu/aiplatform/quizpractice/domain/QuizAttemptTest.java)
- [backend/src/test/java/edu/aiplatform/quizpractice/infrastructure/JdbcQuizAttemptRepositoryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/quizpractice/infrastructure/JdbcQuizAttemptRepositoryIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/quizpractice/web/QuizPracticeControllerTest.java](../../../backend/src/test/java/edu/aiplatform/quizpractice/web/QuizPracticeControllerTest.java)


## U12 — Group Assignment

FR: FR-025, FR-026, FR-039; primary UC 53–57. Source present; runtime/acceptance pending.

### Public contracts

- [backend/src/main/java/edu/aiplatform/groupdocs/contract/ISseHub.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/contract/ISseHub.java)

### Controllers

- [backend/src/main/java/edu/aiplatform/groupdocs/web/GroupDocController.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/web/GroupDocController.java)
- [backend/src/main/java/edu/aiplatform/groups/web/GroupController.java](../../../backend/src/main/java/edu/aiplatform/groups/web/GroupController.java)

### Migrations

- [backend/src/main/resources/db/migration/groups/V20260925_1800__create_group_tables.sql](../../../backend/src/main/resources/db/migration/groups/V20260925_1800__create_group_tables.sql)
- [backend/src/main/resources/db/migration/groups/V20261025_1800__add_group_documents_assignment_fk.sql](../../../backend/src/main/resources/db/migration/groups/V20261025_1800__add_group_documents_assignment_fk.sql)
- [backend/src/main/resources/db/migration/groups/V20261029_1100__make_leader_request_reason_optional.sql](../../../backend/src/main/resources/db/migration/groups/V20261029_1100__make_leader_request_reason_optional.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/groupdocs/application/GroupAutoSubmitScannerTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/application/GroupAutoSubmitScannerTest.java)
- [backend/src/test/java/edu/aiplatform/groupdocs/application/GroupDocumentServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/application/GroupDocumentServiceTest.java)
- [backend/src/test/java/edu/aiplatform/groupdocs/application/GroupSectionServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/application/GroupSectionServiceTest.java)
- [backend/src/test/java/edu/aiplatform/groupdocs/application/GroupSubmissionServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/application/GroupSubmissionServiceTest.java)
- [backend/src/test/java/edu/aiplatform/groupdocs/application/GroupWorkQueryServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/application/GroupWorkQueryServiceTest.java)
- [backend/src/test/java/edu/aiplatform/groupdocs/application/GroupWorkspaceServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/application/GroupWorkspaceServiceTest.java)
- [backend/src/test/java/edu/aiplatform/groupdocs/domain/GroupRepositoriesIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/domain/GroupRepositoriesIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/groupdocs/domain/GroupSectionTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/domain/GroupSectionTest.java)
- [backend/src/test/java/edu/aiplatform/groupdocs/infrastructure/RabbitSseHubTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/infrastructure/RabbitSseHubTest.java)
- [backend/src/test/java/edu/aiplatform/groups/application/ClassGroupServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groups/application/ClassGroupServiceTest.java)
- [backend/src/test/java/edu/aiplatform/groups/application/GroupRenameServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groups/application/GroupRenameServiceTest.java)
- [backend/src/test/java/edu/aiplatform/groups/application/LeaderRequestServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groups/application/LeaderRequestServiceTest.java)
- [backend/src/test/java/edu/aiplatform/groups/application/MyGroupServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groups/application/MyGroupServiceTest.java)
- [backend/src/test/java/edu/aiplatform/groups/application/RandomGroupSplitterTest.java](../../../backend/src/test/java/edu/aiplatform/groups/application/RandomGroupSplitterTest.java)
- [backend/src/test/java/edu/aiplatform/groups/domain/GroupSetRulesTest.java](../../../backend/src/test/java/edu/aiplatform/groups/domain/GroupSetRulesTest.java)
- [backend/src/test/java/edu/aiplatform/groups/web/GroupControllersTest.java](../../../backend/src/test/java/edu/aiplatform/groups/web/GroupControllersTest.java)


## U13 — Reference Materials

FR: FR-004, FR-005; primary UC 58–62. Source present; runtime/acceptance pending.

### Public contracts

- Không có file tương ứng trong checkout; pending.

### Controllers

- [backend/src/main/java/edu/aiplatform/materials/web/ClassMaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/web/ClassMaterialController.java)
- [backend/src/main/java/edu/aiplatform/materials/web/MaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/web/MaterialController.java)
- [backend/src/main/java/edu/aiplatform/materials/web/MaterialModuleController.java](../../../backend/src/main/java/edu/aiplatform/materials/web/MaterialModuleController.java)
- [backend/src/main/java/edu/aiplatform/materials/web/MyMaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/web/MyMaterialController.java)
- [backend/src/main/java/edu/aiplatform/materials/web/SubjectMaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/web/SubjectMaterialController.java)

### Migrations

- [backend/src/main/resources/db/migration/materials/V20260925_1900__create_material_tables.sql](../../../backend/src/main/resources/db/migration/materials/V20260925_1900__create_material_tables.sql)
- [backend/src/main/resources/db/migration/materials/V20261029_1000__create_material_modules.sql](../../../backend/src/main/resources/db/migration/materials/V20261029_1000__create_material_modules.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/materials/application/MaterialAccessPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/materials/application/MaterialAccessPolicyTest.java)
- [backend/src/test/java/edu/aiplatform/materials/application/MaterialMcpTransactionTest.java](../../../backend/src/test/java/edu/aiplatform/materials/application/MaterialMcpTransactionTest.java)
- [backend/src/test/java/edu/aiplatform/materials/application/MaterialModuleServiceTest.java](../../../backend/src/test/java/edu/aiplatform/materials/application/MaterialModuleServiceTest.java)
- [backend/src/test/java/edu/aiplatform/materials/application/MaterialQueryAndReferenceTest.java](../../../backend/src/test/java/edu/aiplatform/materials/application/MaterialQueryAndReferenceTest.java)
- [backend/src/test/java/edu/aiplatform/materials/application/MaterialServiceTest.java](../../../backend/src/test/java/edu/aiplatform/materials/application/MaterialServiceTest.java)
- [backend/src/test/java/edu/aiplatform/materials/application/MaterialTextExtractJobHandlerTest.java](../../../backend/src/test/java/edu/aiplatform/materials/application/MaterialTextExtractJobHandlerTest.java)
- [backend/src/test/java/edu/aiplatform/materials/application/ReferenceMaterialWiringTest.java](../../../backend/src/test/java/edu/aiplatform/materials/application/ReferenceMaterialWiringTest.java)
- [backend/src/test/java/edu/aiplatform/materials/domain/YoutubeVideoTest.java](../../../backend/src/test/java/edu/aiplatform/materials/domain/YoutubeVideoTest.java)
- [backend/src/test/java/edu/aiplatform/materials/infrastructure/MaterialTextExtractorTest.java](../../../backend/src/test/java/edu/aiplatform/materials/infrastructure/MaterialTextExtractorTest.java)
- [backend/src/test/java/edu/aiplatform/materials/web/MaterialControllerTest.java](../../../backend/src/test/java/edu/aiplatform/materials/web/MaterialControllerTest.java)


## U14 — Announcements & Notifications

FR: FR-011, FR-019, FR-023; primary UC 63–65. Source present; runtime/acceptance pending.

### Public contracts

- Không có file tương ứng trong checkout; pending.

### Controllers

- [backend/src/main/java/edu/aiplatform/announcements/web/AnnouncementController.java](../../../backend/src/main/java/edu/aiplatform/announcements/web/AnnouncementController.java)
- [backend/src/main/java/edu/aiplatform/announcements/web/MyAnnouncementController.java](../../../backend/src/main/java/edu/aiplatform/announcements/web/MyAnnouncementController.java)
- [backend/src/main/java/edu/aiplatform/notifications/web/NotificationController.java](../../../backend/src/main/java/edu/aiplatform/notifications/web/NotificationController.java)
- [backend/src/main/java/edu/aiplatform/notifications/web/NotificationPreferenceController.java](../../../backend/src/main/java/edu/aiplatform/notifications/web/NotificationPreferenceController.java)

### Migrations

- [backend/src/main/resources/db/migration/notifications/V20260925_2000__create_notification_tables.sql](../../../backend/src/main/resources/db/migration/notifications/V20260925_2000__create_notification_tables.sql)

### Tests

- [backend/src/test/java/edu/aiplatform/announcements/application/AnnouncementServiceTest.java](../../../backend/src/test/java/edu/aiplatform/announcements/application/AnnouncementServiceTest.java)
- [backend/src/test/java/edu/aiplatform/announcements/domain/AnnouncementSanitizationTest.java](../../../backend/src/test/java/edu/aiplatform/announcements/domain/AnnouncementSanitizationTest.java)
- [backend/src/test/java/edu/aiplatform/notifications/application/AssignmentNotificationRoutingTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/application/AssignmentNotificationRoutingTest.java)
- [backend/src/test/java/edu/aiplatform/notifications/application/DeadlineReminderScannerTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/application/DeadlineReminderScannerTest.java)
- [backend/src/test/java/edu/aiplatform/notifications/application/EmailDeliveryServiceTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/application/EmailDeliveryServiceTest.java)
- [backend/src/test/java/edu/aiplatform/notifications/application/NotificationEventServiceTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/application/NotificationEventServiceTest.java)
- [backend/src/test/java/edu/aiplatform/notifications/application/NotificationServiceTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/application/NotificationServiceTest.java)
- [backend/src/test/java/edu/aiplatform/notifications/application/RecipientResolverTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/application/RecipientResolverTest.java)
- [backend/src/test/java/edu/aiplatform/notifications/infrastructure/NotificationDeliveryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/infrastructure/NotificationDeliveryIntegrationTest.java)
- [backend/src/test/java/edu/aiplatform/notifications/infrastructure/NotificationEventListenerTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/infrastructure/NotificationEventListenerTest.java)
- [backend/src/test/java/edu/aiplatform/notifications/infrastructure/NotificationTopologyContextTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/infrastructure/NotificationTopologyContextTest.java)
- [backend/src/test/java/edu/aiplatform/notifications/web/NotificationControllerTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/web/NotificationControllerTest.java)


## U15 — Admin Dashboard

FR: FR-024, FR-038; primary UC 66. Source present; runtime/acceptance pending.

### Public contracts

- Không có file tương ứng trong checkout; pending.

### Controllers

- [backend/src/main/java/edu/aiplatform/admindashboard/web/AdminDashboardController.java](../../../backend/src/main/java/edu/aiplatform/admindashboard/web/AdminDashboardController.java)

### Migrations

- Không có file tương ứng trong checkout; pending.

### Tests

- [backend/src/test/java/edu/aiplatform/admindashboard/application/AdminDashboardAccessTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/application/AdminDashboardAccessTest.java)
- [backend/src/test/java/edu/aiplatform/admindashboard/application/AdminDashboardActivityServiceTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/application/AdminDashboardActivityServiceTest.java)
- [backend/src/test/java/edu/aiplatform/admindashboard/application/AdminDashboardBackupTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/application/AdminDashboardBackupTest.java)
- [backend/src/test/java/edu/aiplatform/admindashboard/application/AdminDashboardServiceTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/application/AdminDashboardServiceTest.java)
- [backend/src/test/java/edu/aiplatform/admindashboard/infrastructure/DashboardSourceReaderTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/infrastructure/DashboardSourceReaderTest.java)
- [backend/src/test/java/edu/aiplatform/admindashboard/web/AdminDashboardApiContractTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/web/AdminDashboardApiContractTest.java)
- [backend/src/test/java/edu/aiplatform/admindashboard/web/AdminDashboardControllerTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/web/AdminDashboardControllerTest.java)

## Frontend routes

- [frontend/src/app/(auth)/activate/page.tsx](../../../frontend/src/app/%28auth%29/activate/page.tsx)
- [frontend/src/app/(auth)/login/page.tsx](../../../frontend/src/app/%28auth%29/login/page.tsx)
- [frontend/src/app/(auth)/reset-password/page.tsx](../../../frontend/src/app/%28auth%29/reset-password/page.tsx)
- [frontend/src/app/admin/accounts/[id]/page.tsx](../../../frontend/src/app/admin/accounts/[id]/page.tsx)
- [frontend/src/app/admin/accounts/page.tsx](../../../frontend/src/app/admin/accounts/page.tsx)
- [frontend/src/app/admin/audit/page.tsx](../../../frontend/src/app/admin/audit/page.tsx)
- [frontend/src/app/admin/mcp/page.tsx](../../../frontend/src/app/admin/mcp/page.tsx)
- [frontend/src/app/admin/page.tsx](../../../frontend/src/app/admin/page.tsx)
- [frontend/src/app/admin/settings/[key]/page.tsx](../../../frontend/src/app/admin/settings/[key]/page.tsx)
- [frontend/src/app/admin/settings/page.tsx](../../../frontend/src/app/admin/settings/page.tsx)
- [frontend/src/app/admin/settings/sections/[sectionKey]/page.tsx](../../../frontend/src/app/admin/settings/sections/[sectionKey]/page.tsx)
- [frontend/src/app/admin/subjects/[id]/page.tsx](../../../frontend/src/app/admin/subjects/[id]/page.tsx)
- [frontend/src/app/admin/subjects/page.tsx](../../../frontend/src/app/admin/subjects/page.tsx)
- [frontend/src/app/ai/connection/page.tsx](../../../frontend/src/app/ai/connection/page.tsx)
- [frontend/src/app/ai/usage/page.tsx](../../../frontend/src/app/ai/usage/page.tsx)
- [frontend/src/app/announcements/page.tsx](../../../frontend/src/app/announcements/page.tsx)
- [frontend/src/app/assignments/[id]/diagrams/[blockId]/page.tsx](../../../frontend/src/app/assignments/[id]/diagrams/[blockId]/page.tsx)
- [frontend/src/app/assignments/[id]/page.tsx](../../../frontend/src/app/assignments/[id]/page.tsx)
- [frontend/src/app/attempts/[attemptId]/diagrams/[blockId]/page.tsx](../../../frontend/src/app/attempts/[attemptId]/diagrams/[blockId]/page.tsx)
- [frontend/src/app/attempts/[attemptId]/page.tsx](../../../frontend/src/app/attempts/[attemptId]/page.tsx)
- [frontend/src/app/attempts/[attemptId]/receipt/page.tsx](../../../frontend/src/app/attempts/[attemptId]/receipt/page.tsx)
- [frontend/src/app/classes/[id]/announcements/page.tsx](../../../frontend/src/app/classes/[id]/announcements/page.tsx)
- [frontend/src/app/classes/[id]/group/page.tsx](../../../frontend/src/app/classes/[id]/group/page.tsx)
- [frontend/src/app/classes/[id]/page.tsx](../../../frontend/src/app/classes/[id]/page.tsx)
- [frontend/src/app/classes/page.tsx](../../../frontend/src/app/classes/page.tsx)
- [frontend/src/app/group-docs/[id]/page.tsx](../../../frontend/src/app/group-docs/[id]/page.tsx)
- [frontend/src/app/manager/assignments/page.tsx](../../../frontend/src/app/manager/assignments/page.tsx)
- [frontend/src/app/manager/classes/[id]/page.tsx](../../../frontend/src/app/manager/classes/[id]/page.tsx)
- [frontend/src/app/manager/classes/page.tsx](../../../frontend/src/app/manager/classes/page.tsx)
- [frontend/src/app/manager/materials/page.tsx](../../../frontend/src/app/manager/materials/page.tsx)
- [frontend/src/app/manager/page.tsx](../../../frontend/src/app/manager/page.tsx)
- [frontend/src/app/manager/questions/[id]/page.tsx](../../../frontend/src/app/manager/questions/[id]/page.tsx)
- [frontend/src/app/manager/questions/page.tsx](../../../frontend/src/app/manager/questions/page.tsx)
- [frontend/src/app/manager/quizzes/page.tsx](../../../frontend/src/app/manager/quizzes/page.tsx)
- [frontend/src/app/manager/subjects/[id]/page.tsx](../../../frontend/src/app/manager/subjects/[id]/page.tsx)
- [frontend/src/app/manager/subjects/page.tsx](../../../frontend/src/app/manager/subjects/page.tsx)
- [frontend/src/app/materials/[id]/page.tsx](../../../frontend/src/app/materials/[id]/page.tsx)
- [frontend/src/app/materials/[id]/view/page.tsx](../../../frontend/src/app/materials/[id]/view/page.tsx)
- [frontend/src/app/my-assignments/[assignmentId]/history/page.tsx](../../../frontend/src/app/my-assignments/[assignmentId]/history/page.tsx)
- [frontend/src/app/my-assignments/[assignmentId]/page.tsx](../../../frontend/src/app/my-assignments/[assignmentId]/page.tsx)
- [frontend/src/app/my-assignments/page.tsx](../../../frontend/src/app/my-assignments/page.tsx)
- [frontend/src/app/page.tsx](../../../frontend/src/app/page.tsx)
- [frontend/src/app/post-login/page.tsx](../../../frontend/src/app/post-login/page.tsx)
- [frontend/src/app/profile/page.tsx](../../../frontend/src/app/profile/page.tsx)
- [frontend/src/app/quiz-attempts/[attemptId]/page.tsx](../../../frontend/src/app/quiz-attempts/[attemptId]/page.tsx)
- [frontend/src/app/quiz-attempts/[attemptId]/result/page.tsx](../../../frontend/src/app/quiz-attempts/[attemptId]/result/page.tsx)
- [frontend/src/app/quiz-practice/[quizId]/page.tsx](../../../frontend/src/app/quiz-practice/[quizId]/page.tsx)
- [frontend/src/app/quiz-practice/page.tsx](../../../frontend/src/app/quiz-practice/page.tsx)
- [frontend/src/app/quizzes/[id]/page.tsx](../../../frontend/src/app/quizzes/[id]/page.tsx)
- [frontend/src/app/teaching/classes/[id]/announcements/page.tsx](../../../frontend/src/app/teaching/classes/[id]/announcements/page.tsx)
- [frontend/src/app/teaching/classes/[id]/assignments/[assignmentId]/submissions/page.tsx](../../../frontend/src/app/teaching/classes/[id]/assignments/[assignmentId]/submissions/page.tsx)
- [frontend/src/app/teaching/classes/[id]/page.tsx](../../../frontend/src/app/teaching/classes/[id]/page.tsx)
- [frontend/src/app/teaching/classes/page.tsx](../../../frontend/src/app/teaching/classes/page.tsx)
- [frontend/src/app/teaching/evaluations/[evaluationId]/page.tsx](../../../frontend/src/app/teaching/evaluations/[evaluationId]/page.tsx)

## Frontend components and tests

- [frontend/src/features/academics/academics.test.tsx](../../../frontend/src/features/academics/academics.test.tsx)
- [frontend/src/features/academics/api.test.ts](../../../frontend/src/features/academics/api.test.ts)
- [frontend/src/features/academics/integration.test.tsx](../../../frontend/src/features/academics/integration.test.tsx)
- [frontend/src/features/academics/navContexts.test.tsx](../../../frontend/src/features/academics/navContexts.test.tsx)
- [frontend/src/features/academics/pagination.characterization.test.tsx](../../../frontend/src/features/academics/pagination.characterization.test.tsx)
- [frontend/src/features/academics/register.ts](../../../frontend/src/features/academics/register.ts)
- [frontend/src/features/academics/routes.characterization.test.tsx](../../../frontend/src/features/academics/routes.characterization.test.tsx)
- [frontend/src/features/academics/routes.test.tsx](../../../frontend/src/features/academics/routes.test.tsx)
- [frontend/src/features/academics/useCandidates.test.tsx](../../../frontend/src/features/academics/useCandidates.test.tsx)
- [frontend/src/features/admindashboard/AdminDashboardPage.test.tsx](../../../frontend/src/features/admindashboard/AdminDashboardPage.test.tsx)
- [frontend/src/features/admindashboard/api.test.ts](../../../frontend/src/features/admindashboard/api.test.ts)
- [frontend/src/features/admindashboard/navigation.test.tsx](../../../frontend/src/features/admindashboard/navigation.test.tsx)
- [frontend/src/features/admindashboard/register.ts](../../../frontend/src/features/admindashboard/register.ts)
- [frontend/src/features/ai/admin/AdminMcpPage.test.tsx](../../../frontend/src/features/ai/admin/AdminMcpPage.test.tsx)
- [frontend/src/features/ai/admin/AiSettingsPanels.test.tsx](../../../frontend/src/features/ai/admin/AiSettingsPanels.test.tsx)
- [frontend/src/features/ai/AiConnectionPage.test.tsx](../../../frontend/src/features/ai/AiConnectionPage.test.tsx)
- [frontend/src/features/ai/AiUsageHistoryPage.test.tsx](../../../frontend/src/features/ai/AiUsageHistoryPage.test.tsx)
- [frontend/src/features/ai/register.ts](../../../frontend/src/features/ai/register.ts)
- [frontend/src/features/announcements/Announcements.test.tsx](../../../frontend/src/features/announcements/Announcements.test.tsx)
- [frontend/src/features/announcements/ClassAnnouncementsPage.test.tsx](../../../frontend/src/features/announcements/ClassAnnouncementsPage.test.tsx)
- [frontend/src/features/announcements/register.ts](../../../frontend/src/features/announcements/register.ts)
- [frontend/src/features/assignments/AiDraftDialog.test.tsx](../../../frontend/src/features/assignments/AiDraftDialog.test.tsx)
- [frontend/src/features/assignments/AiDraftPreview.test.tsx](../../../frontend/src/features/assignments/AiDraftPreview.test.tsx)
- [frontend/src/features/assignments/assignments.test.tsx](../../../frontend/src/features/assignments/assignments.test.tsx)
- [frontend/src/features/assignments/register.ts](../../../frontend/src/features/assignments/register.ts)
- [frontend/src/features/assignmentwork/assignmentwork.test.tsx](../../../frontend/src/features/assignmentwork/assignmentwork.test.tsx)
- [frontend/src/features/assignmentwork/register.ts](../../../frontend/src/features/assignmentwork/register.ts)
- [frontend/src/features/assignmentwork/StudentAssignmentsPage.test.tsx](../../../frontend/src/features/assignmentwork/StudentAssignmentsPage.test.tsx)
- [frontend/src/features/assignmentwork/SubmissionHistoryPage.test.tsx](../../../frontend/src/features/assignmentwork/SubmissionHistoryPage.test.tsx)
- [frontend/src/features/assignmentwork/workspace-ui.test.tsx](../../../frontend/src/features/assignmentwork/workspace-ui.test.tsx)
- [frontend/src/features/audit/AuditLogPage.test.tsx](../../../frontend/src/features/audit/AuditLogPage.test.tsx)
- [frontend/src/features/audit/register.ts](../../../frontend/src/features/audit/register.ts)
- [frontend/src/features/files/api.test.ts](../../../frontend/src/features/files/api.test.ts)
- [frontend/src/features/files/FileUploader.test.tsx](../../../frontend/src/features/files/FileUploader.test.tsx)
- [frontend/src/features/files/purposes.test.ts](../../../frontend/src/features/files/purposes.test.ts)
- [frontend/src/features/grading/ai-source-integration.test.tsx](../../../frontend/src/features/grading/ai-source-integration.test.tsx)
- [frontend/src/features/grading/batch-action-feedback.test.tsx](../../../frontend/src/features/grading/batch-action-feedback.test.tsx)
- [frontend/src/features/grading/class-progress-layout.test.tsx](../../../frontend/src/features/grading/class-progress-layout.test.tsx)
- [frontend/src/features/grading/gradebook-layout.test.tsx](../../../frontend/src/features/grading/gradebook-layout.test.tsx)
- [frontend/src/features/grading/manual-workflow.test.tsx](../../../frontend/src/features/grading/manual-workflow.test.tsx)
- [frontend/src/features/grading/partial-scores.test.tsx](../../../frontend/src/features/grading/partial-scores.test.tsx)
- [frontend/src/features/grading/register.ts](../../../frontend/src/features/grading/register.ts)
- [frontend/src/features/grading/route-shell.test.tsx](../../../frontend/src/features/grading/route-shell.test.tsx)
- [frontend/src/features/grading/structured-feedback.test.tsx](../../../frontend/src/features/grading/structured-feedback.test.tsx)
- [frontend/src/features/grading/submissions.test.tsx](../../../frontend/src/features/grading/submissions.test.tsx)
- [frontend/src/features/grading/workspace.test.tsx](../../../frontend/src/features/grading/workspace.test.tsx)
- [frontend/src/features/groupdocs/GroupWorkspacePage.test.tsx](../../../frontend/src/features/groupdocs/GroupWorkspacePage.test.tsx)
- [frontend/src/features/groupdocs/register.ts](../../../frontend/src/features/groupdocs/register.ts)
- [frontend/src/features/groups/ClassGroupsPanel.test.tsx](../../../frontend/src/features/groups/ClassGroupsPanel.test.tsx)
- [frontend/src/features/groups/MyGroup.test.tsx](../../../frontend/src/features/groups/MyGroup.test.tsx)
- [frontend/src/features/groups/register.ts](../../../frontend/src/features/groups/register.ts)
- [frontend/src/features/groups/splitUngrouped.test.ts](../../../frontend/src/features/groups/splitUngrouped.test.ts)
- [frontend/src/features/groups/TeacherGroupWidgets.test.tsx](../../../frontend/src/features/groups/TeacherGroupWidgets.test.tsx)
- [frontend/src/features/identity/admin/AdminScreens.test.tsx](../../../frontend/src/features/identity/admin/AdminScreens.test.tsx)
- [frontend/src/features/identity/admin/Pagination.test.ts](../../../frontend/src/features/identity/admin/Pagination.test.ts)
- [frontend/src/features/identity/components/OtpInput.test.tsx](../../../frontend/src/features/identity/components/OtpInput.test.tsx)
- [frontend/src/features/identity/LoginForm.test.tsx](../../../frontend/src/features/identity/LoginForm.test.tsx)
- [frontend/src/features/identity/otp/OtpFlow.test.tsx](../../../frontend/src/features/identity/otp/OtpFlow.test.tsx)
- [frontend/src/features/identity/passwordRules.test.ts](../../../frontend/src/features/identity/passwordRules.test.ts)
- [frontend/src/features/identity/profile/PasswordChangeDialog.test.tsx](../../../frontend/src/features/identity/profile/PasswordChangeDialog.test.tsx)
- [frontend/src/features/identity/profile/ProfileForm.test.tsx](../../../frontend/src/features/identity/profile/ProfileForm.test.tsx)
- [frontend/src/features/identity/register.ts](../../../frontend/src/features/identity/register.ts)
- [frontend/src/features/identity/session.test.ts](../../../frontend/src/features/identity/session.test.ts)
- [frontend/src/features/identity/shell/RoleGuard.test.tsx](../../../frontend/src/features/identity/shell/RoleGuard.test.tsx)
- [frontend/src/features/materials/api.test.ts](../../../frontend/src/features/materials/api.test.ts)
- [frontend/src/features/materials/MaterialDialogs.test.tsx](../../../frontend/src/features/materials/MaterialDialogs.test.tsx)
- [frontend/src/features/materials/MaterialPreview.test.tsx](../../../frontend/src/features/materials/MaterialPreview.test.tsx)
- [frontend/src/features/materials/ModuleBoard.test.tsx](../../../frontend/src/features/materials/ModuleBoard.test.tsx)
- [frontend/src/features/materials/register.ts](../../../frontend/src/features/materials/register.ts)
- [frontend/src/features/materials/xlsx.test.ts](../../../frontend/src/features/materials/xlsx.test.ts)
- [frontend/src/features/notifications/Notifications.test.tsx](../../../frontend/src/features/notifications/Notifications.test.tsx)
- [frontend/src/features/notifications/register.ts](../../../frontend/src/features/notifications/register.ts)
- [frontend/src/features/quizpractice/MaterialQuizList.test.tsx](../../../frontend/src/features/quizpractice/MaterialQuizList.test.tsx)
- [frontend/src/features/quizpractice/QuizPracticeScreens.test.tsx](../../../frontend/src/features/quizpractice/QuizPracticeScreens.test.tsx)
- [frontend/src/features/quizpractice/register.ts](../../../frontend/src/features/quizpractice/register.ts)
- [frontend/src/features/quizzes/AiDrafts.test.tsx](../../../frontend/src/features/quizzes/AiDrafts.test.tsx)
- [frontend/src/features/quizzes/McqEditor.test.tsx](../../../frontend/src/features/quizzes/McqEditor.test.tsx)
- [frontend/src/features/quizzes/questionForm.test.ts](../../../frontend/src/features/quizzes/questionForm.test.ts)
- [frontend/src/features/quizzes/QuestionScreens.test.tsx](../../../frontend/src/features/quizzes/QuestionScreens.test.tsx)
- [frontend/src/features/quizzes/QuizScreens.test.tsx](../../../frontend/src/features/quizzes/QuizScreens.test.tsx)
- [frontend/src/features/quizzes/register.ts](../../../frontend/src/features/quizzes/register.ts)
- [frontend/src/features/register.test.ts](../../../frontend/src/features/register.test.ts)
- [frontend/src/features/register.ts](../../../frontend/src/features/register.ts)
- [frontend/src/features/settings/register.ts](../../../frontend/src/features/settings/register.ts)
- [frontend/src/features/settings/SettingListPage.test.tsx](../../../frontend/src/features/settings/SettingListPage.test.tsx)
- [frontend/src/features/settings/SettingValueForm.test.tsx](../../../frontend/src/features/settings/SettingValueForm.test.tsx)
- [frontend/src/features/settings/validation.test.ts](../../../frontend/src/features/settings/validation.test.ts)
- [frontend/src/features/subjects/api.test.ts](../../../frontend/src/features/subjects/api.test.ts)
- [frontend/src/features/subjects/ManagerChangeDialog.test.tsx](../../../frontend/src/features/subjects/ManagerChangeDialog.test.tsx)
- [frontend/src/features/subjects/ManagerPicker.test.tsx](../../../frontend/src/features/subjects/ManagerPicker.test.tsx)
- [frontend/src/features/subjects/register.ts](../../../frontend/src/features/subjects/register.ts)
- [frontend/src/features/subjects/SubjectClassesTable.test.tsx](../../../frontend/src/features/subjects/SubjectClassesTable.test.tsx)
- [frontend/src/features/subjects/SubjectDetailMutations.test.tsx](../../../frontend/src/features/subjects/SubjectDetailMutations.test.tsx)
- [frontend/src/features/subjects/SubjectDetailPage.test.tsx](../../../frontend/src/features/subjects/SubjectDetailPage.test.tsx)
- [frontend/src/features/subjects/SubjectEditForm.test.tsx](../../../frontend/src/features/subjects/SubjectEditForm.test.tsx)
- [frontend/src/features/subjects/SubjectFormDialog.test.tsx](../../../frontend/src/features/subjects/SubjectFormDialog.test.tsx)
- [frontend/src/features/subjects/SubjectListPage.test.tsx](../../../frontend/src/features/subjects/SubjectListPage.test.tsx)
- [frontend/src/features/subjects/SubjectStatusActions.test.tsx](../../../frontend/src/features/subjects/SubjectStatusActions.test.tsx)
- [frontend/src/features/subjects/validation.test.ts](../../../frontend/src/features/subjects/validation.test.ts)

## Adapter and evidence limits

Contract stub classes remain conditional fallbacks, not proof that every running provider is stubbed. Runtime bean selection depends on profile/configuration. Drive/SMTP/AI adapters need live credentials; docs do not certify their availability.

- [backend/src/main/java/edu/aiplatform/academics/contract/stub/AcademicsContractStubs.java](../../../backend/src/main/java/edu/aiplatform/academics/contract/stub/AcademicsContractStubs.java)
- [backend/src/main/java/edu/aiplatform/ai/contract/stub/AiContractStubs.java](../../../backend/src/main/java/edu/aiplatform/ai/contract/stub/AiContractStubs.java)
- [backend/src/main/java/edu/aiplatform/assignments/contract/stub/AssignmentsContractStubs.java](../../../backend/src/main/java/edu/aiplatform/assignments/contract/stub/AssignmentsContractStubs.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/contract/stub/AssignmentWorkContractStubs.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract/stub/AssignmentWorkContractStubs.java)
- [backend/src/main/java/edu/aiplatform/audit/contract/stub/AuditContractStubs.java](../../../backend/src/main/java/edu/aiplatform/audit/contract/stub/AuditContractStubs.java)
- [backend/src/main/java/edu/aiplatform/backup/contract/stub/BackupContractStubs.java](../../../backend/src/main/java/edu/aiplatform/backup/contract/stub/BackupContractStubs.java)
- [backend/src/main/java/edu/aiplatform/files/contract/stub/FilesContractStubs.java](../../../backend/src/main/java/edu/aiplatform/files/contract/stub/FilesContractStubs.java)
- [backend/src/main/java/edu/aiplatform/grading/contract/stub/GradingContractStubs.java](../../../backend/src/main/java/edu/aiplatform/grading/contract/stub/GradingContractStubs.java)
- [backend/src/main/java/edu/aiplatform/groupdocs/contract/stub/GroupDocsContractStubs.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/contract/stub/GroupDocsContractStubs.java)
- [backend/src/main/java/edu/aiplatform/identity/contract/stub/IdentityContractStubs.java](../../../backend/src/main/java/edu/aiplatform/identity/contract/stub/IdentityContractStubs.java)
- [backend/src/main/java/edu/aiplatform/jobs/contract/stub/JobsContractStubs.java](../../../backend/src/main/java/edu/aiplatform/jobs/contract/stub/JobsContractStubs.java)
- [backend/src/main/java/edu/aiplatform/quizzes/contract/stub/QuizzesContractStubs.java](../../../backend/src/main/java/edu/aiplatform/quizzes/contract/stub/QuizzesContractStubs.java)
- [backend/src/main/java/edu/aiplatform/settings/contract/stub/SettingsContractStubs.java](../../../backend/src/main/java/edu/aiplatform/settings/contract/stub/SettingsContractStubs.java)
- [backend/src/main/java/edu/aiplatform/subjects/contract/stub/SubjectsContractStubs.java](../../../backend/src/main/java/edu/aiplatform/subjects/contract/stub/SubjectsContractStubs.java)

## Subsequent approved remediation working tree

Source changes after this initial HEAD inventory restore Student/remove Admin MCP, make worker/backup terminal handling durable, recheck enrollment targets, serialize API/MCP feedback budgets and correct Quiz publication/retry guards. HEAD remains unchanged. [Fresh verification and release notes](../../construction/build-and-test/2026-10-30-remediation-student-mcp-results.md).
