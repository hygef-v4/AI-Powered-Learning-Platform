# Current code baseline — structure refactor 2026-10-30

Frontend layout follow-up: parent HEAD `5480c267` + working-tree CSS/test relocation. Tests and fixtures: [frontend/tests](../../../frontend/tests); shared/global CSS: [shared/styles](../../../frontend/src/shared/styles); feature CSS: each feature styles directory. [Fresh verification and limits](../../construction/build-and-test/2026-10-30-frontend-styles-tests-results.md).

Parent HEAD `5a76e428` + working-tree refactor on branch `aidlc-docs`; no new commit. 66 UC, 15 logical units and their ownership are retained. Java 17/Spring Boot modular monolith + worker; Next.js/React/TypeScript frontend.

Verification: [refactor results and limits](../../construction/build-and-test/2026-10-30-package-moves-only-results.md). Source inventory does not imply full UC/runtime/provider acceptance.

## U01 — Account & Access

Primary UC 01–12; FR-001, FR-002, FR-015.

| Source | Trách nhiệm |
|---|---|
| [identity/controller](../../../backend/src/main/java/edu/aiplatform/identity/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [identity/service](../../../backend/src/main/java/edu/aiplatform/identity/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [identity/repository](../../../backend/src/main/java/edu/aiplatform/identity/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [identity/model](../../../backend/src/main/java/edu/aiplatform/identity/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [identity/integration](../../../backend/src/main/java/edu/aiplatform/identity/integration) | Adapter hạ tầng/provider, listener và sender. |
| [identity/config](../../../backend/src/main/java/edu/aiplatform/identity/config) | Properties, cấu hình bean và topology. |
| [identity/contract](../../../backend/src/main/java/edu/aiplatform/identity/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

### Controllers

- [AdminAccountController.java](../../../backend/src/main/java/edu/aiplatform/identity/controller/AdminAccountController.java)
- [AuthController.java](../../../backend/src/main/java/edu/aiplatform/identity/controller/AuthController.java)
- [MeController.java](../../../backend/src/main/java/edu/aiplatform/identity/controller/MeController.java)
- [OtpFlowController.java](../../../backend/src/main/java/edu/aiplatform/identity/controller/OtpFlowController.java)

### Tests

- [IdentityWebSecurityTest.java](../../../backend/src/test/java/edu/aiplatform/identity/controller/IdentityWebSecurityTest.java)
- [JwtServiceTest.java](../../../backend/src/test/java/edu/aiplatform/identity/integration/JwtServiceTest.java)
- [OtpStoresTest.java](../../../backend/src/test/java/edu/aiplatform/identity/integration/OtpStoresTest.java)
- [RequestThrottleIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/identity/integration/RequestThrottleIntegrationTest.java)
- [SessionStoreIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/identity/integration/SessionStoreIntegrationTest.java)
- [SessionStoreTest.java](../../../backend/src/test/java/edu/aiplatform/identity/integration/SessionStoreTest.java)
- [AccountTest.java](../../../backend/src/test/java/edu/aiplatform/identity/model/AccountTest.java)
- [EmailPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/model/EmailPolicyTest.java)
- [PasswordPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/model/PasswordPolicyTest.java)
- [ProfilePolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/model/ProfilePolicyTest.java)
- [AccountRepositoryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/identity/repository/AccountRepositoryIntegrationTest.java)
- [AccountRepositoryTest.java](../../../backend/src/test/java/edu/aiplatform/identity/repository/AccountRepositoryTest.java)
- [AccountAdminServiceTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/AccountAdminServiceTest.java)
- [AccountImportServiceTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/AccountImportServiceTest.java)
- [AccountLookupAndPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/AccountLookupAndPolicyTest.java)
- [AdminDashboardPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/AdminDashboardPolicyTest.java)
- [AuthorizationServiceTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/AuthorizationServiceTest.java)
- [AuthServiceTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/AuthServiceTest.java)
- [GradingPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/GradingPolicyTest.java)
- [McpPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/McpPolicyTest.java)
- [McpRolePolicyTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/McpRolePolicyTest.java)
- [NotificationAccountLookupTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/NotificationAccountLookupTest.java)
- [OtpMailHandlerTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/OtpMailHandlerTest.java)
- [OtpServiceTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/OtpServiceTest.java)
- [PasswordCompletionTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/PasswordCompletionTest.java)
- [ProfileAndPasswordChangeTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/ProfileAndPasswordChangeTest.java)
- [SessionAuthorityTest.java](../../../backend/src/test/java/edu/aiplatform/identity/service/SessionAuthorityTest.java)

### Migrations

- [V20260925_0930__create_accounts.sql](../../../backend/src/main/resources/db/migration/identity/V20260925_0930__create_accounts.sql)
- [V20260925_0931__add_audit_actor_fk.sql](../../../backend/src/main/resources/db/migration/identity/V20260925_0931__add_audit_actor_fk.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [identity](../../../frontend/src/features/identity) | [api](../../../frontend/src/features/identity/api) | [model](../../../frontend/src/features/identity/model) | [pages](../../../frontend/src/features/identity/pages) | [components](../../../frontend/src/features/identity/components) | [hooks](../../../frontend/src/features/identity/hooks) | — | [tests](../../../frontend/tests/features/identity) |

## U02 — Audit

Primary UC 13; FR-014.

| Source | Trách nhiệm |
|---|---|
| [audit/controller](../../../backend/src/main/java/edu/aiplatform/audit/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [audit/service](../../../backend/src/main/java/edu/aiplatform/audit/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [audit/repository](../../../backend/src/main/java/edu/aiplatform/audit/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [audit/model](../../../backend/src/main/java/edu/aiplatform/audit/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [audit/contract](../../../backend/src/main/java/edu/aiplatform/audit/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

### Controllers

- [AuditController.java](../../../backend/src/main/java/edu/aiplatform/audit/controller/AuditController.java)

### Tests

- [AuditControllerTest.java](../../../backend/src/test/java/edu/aiplatform/audit/controller/AuditControllerTest.java)
- [AuditLogRepositoryWhereTest.java](../../../backend/src/test/java/edu/aiplatform/audit/repository/AuditLogRepositoryWhereTest.java)
- [AuditQueryServiceTest.java](../../../backend/src/test/java/edu/aiplatform/audit/service/AuditQueryServiceTest.java)
- [AuditStoreTest.java](../../../backend/src/test/java/edu/aiplatform/audit/service/AuditStoreTest.java)

### Migrations

- [V20260925_0900__create_audit_logs.sql](../../../backend/src/main/resources/db/migration/audit/V20260925_0900__create_audit_logs.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [audit](../../../frontend/src/features/audit) | [api](../../../frontend/src/features/audit/api) | [model](../../../frontend/src/features/audit/model) | [pages](../../../frontend/src/features/audit/pages) | [components](../../../frontend/src/features/audit/components) | — | — | [tests](../../../frontend/tests/features/audit) |

## U03 — Job and Settings

Primary UC 14–15; FR-013, FR-033, FR-038.

| Source | Trách nhiệm |
|---|---|
| [files/controller](../../../backend/src/main/java/edu/aiplatform/files/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [files/service](../../../backend/src/main/java/edu/aiplatform/files/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [files/model](../../../backend/src/main/java/edu/aiplatform/files/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [files/integration](../../../backend/src/main/java/edu/aiplatform/files/integration) | Adapter hạ tầng/provider, listener và sender. |
| [files/config](../../../backend/src/main/java/edu/aiplatform/files/config) | Properties, cấu hình bean và topology. |
| [files/contract](../../../backend/src/main/java/edu/aiplatform/files/contract) | Public interface/DTO xuyên unit; giữ nguyên. |
| [jobs/service](../../../backend/src/main/java/edu/aiplatform/jobs/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [jobs/integration](../../../backend/src/main/java/edu/aiplatform/jobs/integration) | Adapter hạ tầng/provider, listener và sender. |
| [jobs/config](../../../backend/src/main/java/edu/aiplatform/jobs/config) | Properties, cấu hình bean và topology. |
| [jobs/contract](../../../backend/src/main/java/edu/aiplatform/jobs/contract) | Public interface/DTO xuyên unit; giữ nguyên. |
| [settings/controller](../../../backend/src/main/java/edu/aiplatform/settings/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [settings/service](../../../backend/src/main/java/edu/aiplatform/settings/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [settings/repository](../../../backend/src/main/java/edu/aiplatform/settings/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [settings/model](../../../backend/src/main/java/edu/aiplatform/settings/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [settings/contract](../../../backend/src/main/java/edu/aiplatform/settings/contract) | Public interface/DTO xuyên unit; giữ nguyên. |
| [backup/service](../../../backend/src/main/java/edu/aiplatform/backup/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [backup/repository](../../../backend/src/main/java/edu/aiplatform/backup/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [backup/model](../../../backend/src/main/java/edu/aiplatform/backup/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [backup/integration](../../../backend/src/main/java/edu/aiplatform/backup/integration) | Adapter hạ tầng/provider, listener và sender. |
| [backup/config](../../../backend/src/main/java/edu/aiplatform/backup/config) | Properties, cấu hình bean và topology. |
| [backup/contract](../../../backend/src/main/java/edu/aiplatform/backup/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

### Controllers

- [DownloadController.java](../../../backend/src/main/java/edu/aiplatform/files/controller/DownloadController.java)
- [FileController.java](../../../backend/src/main/java/edu/aiplatform/files/controller/FileController.java)
- [SettingsController.java](../../../backend/src/main/java/edu/aiplatform/settings/controller/SettingsController.java)

### Tests

- [BackupDashboardUnavailableTest.java](../../../backend/src/test/java/edu/aiplatform/backup/contract/stub/BackupDashboardUnavailableTest.java)
- [BackupConcurrentDeliveryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/backup/integration/BackupConcurrentDeliveryIntegrationTest.java)
- [BackupDashboardStatusIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/backup/integration/BackupDashboardStatusIntegrationTest.java)
- [LocalBackupStoreTest.java](../../../backend/src/test/java/edu/aiplatform/backup/integration/LocalBackupStoreTest.java)
- [BackupDashboardContractTest.java](../../../backend/src/test/java/edu/aiplatform/backup/service/BackupDashboardContractTest.java)
- [BackupEncryptorTest.java](../../../backend/src/test/java/edu/aiplatform/backup/service/BackupEncryptorTest.java)
- [BackupJobHandlerTest.java](../../../backend/src/test/java/edu/aiplatform/backup/service/BackupJobHandlerTest.java)
- [BackupRetentionServiceTest.java](../../../backend/src/test/java/edu/aiplatform/backup/service/BackupRetentionServiceTest.java)
- [BackupScheduleScannerTest.java](../../../backend/src/test/java/edu/aiplatform/backup/service/BackupScheduleScannerTest.java)
- [BackupStatusServiceTest.java](../../../backend/src/test/java/edu/aiplatform/backup/service/BackupStatusServiceTest.java)
- [LocalFolderStorageAdapterTest.java](../../../backend/src/test/java/edu/aiplatform/files/integration/LocalFolderStorageAdapterTest.java)
- [FileNameSanitizerTest.java](../../../backend/src/test/java/edu/aiplatform/files/model/FileNameSanitizerTest.java)
- [PurposePolicyTest.java](../../../backend/src/test/java/edu/aiplatform/files/model/PurposePolicyTest.java)
- [ArtifactServiceTest.java](../../../backend/src/test/java/edu/aiplatform/files/service/ArtifactServiceTest.java)
- [ContentInspectorTest.java](../../../backend/src/test/java/edu/aiplatform/files/service/ContentInspectorTest.java)
- [DownloadServiceTest.java](../../../backend/src/test/java/edu/aiplatform/files/service/DownloadServiceTest.java)
- [DownloadTokenServiceTest.java](../../../backend/src/test/java/edu/aiplatform/files/service/DownloadTokenServiceTest.java)
- [FileRefSignerTest.java](../../../backend/src/test/java/edu/aiplatform/files/service/FileRefSignerTest.java)
- [UploadServiceTest.java](../../../backend/src/test/java/edu/aiplatform/files/service/UploadServiceTest.java)
- [JobListenerTest.java](../../../backend/src/test/java/edu/aiplatform/jobs/integration/JobListenerTest.java)
- [EventPublisherTest.java](../../../backend/src/test/java/edu/aiplatform/jobs/service/EventPublisherTest.java)
- [JobDispatcherTest.java](../../../backend/src/test/java/edu/aiplatform/jobs/service/JobDispatcherTest.java)
- [JobHandlerRegistryTest.java](../../../backend/src/test/java/edu/aiplatform/jobs/service/JobHandlerRegistryTest.java)
- [JobPublisherTest.java](../../../backend/src/test/java/edu/aiplatform/jobs/service/JobPublisherTest.java)
- [RunnersTest.java](../../../backend/src/test/java/edu/aiplatform/jobs/service/RunnersTest.java)
- [SettingValidatorTest.java](../../../backend/src/test/java/edu/aiplatform/settings/model/SettingValidatorTest.java)
- [SettingsSectionRegistryTest.java](../../../backend/src/test/java/edu/aiplatform/settings/service/SettingsSectionRegistryTest.java)
- [SettingsServiceTest.java](../../../backend/src/test/java/edu/aiplatform/settings/service/SettingsServiceTest.java)

### Migrations

- [V20260925_0920__create_backup_tables.sql](../../../backend/src/main/resources/db/migration/backup/V20260925_0920__create_backup_tables.sql)
- [V20260925_0910__create_system_settings.sql](../../../backend/src/main/resources/db/migration/settings/V20260925_0910__create_system_settings.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [files](../../../frontend/src/features/files) | [api](../../../frontend/src/features/files/api) | [model](../../../frontend/src/features/files/model) | — | [components](../../../frontend/src/features/files/components) | [hooks](../../../frontend/src/features/files/hooks) | — | [tests](../../../frontend/tests/features/files) |
| [settings](../../../frontend/src/features/settings) | [api](../../../frontend/src/features/settings/api) | [model](../../../frontend/src/features/settings/model) | [pages](../../../frontend/src/features/settings/pages) | [components](../../../frontend/src/features/settings/components) | — | — | [tests](../../../frontend/tests/features/settings) |

## U04 — Subject Administration

Primary UC 16–19; FR-003.

| Source | Trách nhiệm |
|---|---|
| [subjects/controller](../../../backend/src/main/java/edu/aiplatform/subjects/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [subjects/service](../../../backend/src/main/java/edu/aiplatform/subjects/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [subjects/repository](../../../backend/src/main/java/edu/aiplatform/subjects/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [subjects/model](../../../backend/src/main/java/edu/aiplatform/subjects/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [subjects/dto](../../../backend/src/main/java/edu/aiplatform/subjects/dto) | Kiểu dữ liệu request/response riêng. |
| [subjects/contract](../../../backend/src/main/java/edu/aiplatform/subjects/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

### Controllers

- [SubjectAdminController.java](../../../backend/src/main/java/edu/aiplatform/subjects/controller/SubjectAdminController.java)

### Tests

- [SubjectAdminControllerTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/controller/SubjectAdminControllerTest.java)
- [SubjectCodeTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/model/SubjectCodeTest.java)
- [SubjectTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/model/SubjectTest.java)
- [SubjectRepositoryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/repository/SubjectRepositoryIntegrationTest.java)
- [SubjectAdminCommandTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/service/SubjectAdminCommandTest.java)
- [SubjectAdminReadTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/service/SubjectAdminReadTest.java)
- [SubjectAuditTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/service/SubjectAuditTest.java)
- [SubjectClassesReaderTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/service/SubjectClassesReaderTest.java)
- [SubjectContractsWiringTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/service/SubjectContractsWiringTest.java)
- [SubjectMutationGuardTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/service/SubjectMutationGuardTest.java)
- [SubjectQueryServiceTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/service/SubjectQueryServiceTest.java)
- [SubjectScopeServiceTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/service/SubjectScopeServiceTest.java)
- [SubjectStatusTest.java](../../../backend/src/test/java/edu/aiplatform/subjects/service/SubjectStatusTest.java)

### Migrations

- [V20260925_1000__create_subjects.sql](../../../backend/src/main/resources/db/migration/subjects/V20260925_1000__create_subjects.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [subjects](../../../frontend/src/features/subjects) | [api](../../../frontend/src/features/subjects/api) | [model](../../../frontend/src/features/subjects/model) | [pages](../../../frontend/src/features/subjects/pages) | [components](../../../frontend/src/features/subjects/components) | [hooks](../../../frontend/src/features/subjects/hooks) | — | [tests](../../../frontend/tests/features/subjects) |

## U05 — Class & Enrollment

Primary UC 20–30; FR-003, FR-005, FR-024.

| Source | Trách nhiệm |
|---|---|
| [academics/controller](../../../backend/src/main/java/edu/aiplatform/academics/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [academics/service](../../../backend/src/main/java/edu/aiplatform/academics/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [academics/repository](../../../backend/src/main/java/edu/aiplatform/academics/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [academics/model](../../../backend/src/main/java/edu/aiplatform/academics/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [academics/dto](../../../backend/src/main/java/edu/aiplatform/academics/dto) | Kiểu dữ liệu request/response riêng. |
| [academics/contract](../../../backend/src/main/java/edu/aiplatform/academics/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

### Controllers

- [AcademicsController.java](../../../backend/src/main/java/edu/aiplatform/academics/controller/AcademicsController.java)

### Tests

- [AcademicsApiContractTest.java](../../../backend/src/test/java/edu/aiplatform/academics/controller/AcademicsApiContractTest.java)
- [AcademicsControllerTest.java](../../../backend/src/test/java/edu/aiplatform/academics/controller/AcademicsControllerTest.java)
- [AcademicsPersistenceTest.java](../../../backend/src/test/java/edu/aiplatform/academics/integration/AcademicsPersistenceTest.java)
- [CourseClassTest.java](../../../backend/src/test/java/edu/aiplatform/academics/model/CourseClassTest.java)
- [EnrollmentListParserTest.java](../../../backend/src/test/java/edu/aiplatform/academics/model/EnrollmentListParserTest.java)
- [EnrollmentTest.java](../../../backend/src/test/java/edu/aiplatform/academics/model/EnrollmentTest.java)
- [AcademicsReadRepositoryTest.java](../../../backend/src/test/java/edu/aiplatform/academics/repository/AcademicsReadRepositoryTest.java)
- [AcademicsAccessAndReadTest.java](../../../backend/src/test/java/edu/aiplatform/academics/service/AcademicsAccessAndReadTest.java)
- [AcademicsBatchTransactionTest.java](../../../backend/src/test/java/edu/aiplatform/academics/service/AcademicsBatchTransactionTest.java)
- [AcademicsContractAdapterTest.java](../../../backend/src/test/java/edu/aiplatform/academics/service/AcademicsContractAdapterTest.java)
- [AcademicsMcpToolsTest.java](../../../backend/src/test/java/edu/aiplatform/academics/service/AcademicsMcpToolsTest.java)
- [AcademicsServiceTest.java](../../../backend/src/test/java/edu/aiplatform/academics/service/AcademicsServiceTest.java)

### Migrations

- [V20260925_1100__create_classes_enrollments.sql](../../../backend/src/main/resources/db/migration/classes/V20260925_1100__create_classes_enrollments.sql)
- [V20261027_1100__allow_open_class_without_teacher.sql](../../../backend/src/main/resources/db/migration/classes/V20261027_1100__allow_open_class_without_teacher.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [academics](../../../frontend/src/features/academics) | [api](../../../frontend/src/features/academics/api) | [model](../../../frontend/src/features/academics/model) | [pages](../../../frontend/src/features/academics/pages) | [components](../../../frontend/src/features/academics/components) | [hooks](../../../frontend/src/features/academics/hooks) | [styles](../../../frontend/src/features/academics/styles) | [tests](../../../frontend/tests/features/academics) |

## U06 — AI Connections

Primary UC 31–33; FR-012, FR-021, FR-034, FR-035, FR-036, FR-037.

| Source | Trách nhiệm |
|---|---|
| [ai/controller](../../../backend/src/main/java/edu/aiplatform/ai/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [ai/service](../../../backend/src/main/java/edu/aiplatform/ai/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [ai/repository](../../../backend/src/main/java/edu/aiplatform/ai/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [ai/model](../../../backend/src/main/java/edu/aiplatform/ai/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [ai/integration](../../../backend/src/main/java/edu/aiplatform/ai/integration) | Adapter hạ tầng/provider, listener và sender. |
| [ai/config](../../../backend/src/main/java/edu/aiplatform/ai/config) | Properties, cấu hình bean và topology. |
| [ai/contract](../../../backend/src/main/java/edu/aiplatform/ai/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

### Controllers

- [AiAdminController.java](../../../backend/src/main/java/edu/aiplatform/ai/controller/AiAdminController.java)
- [AiConnectionController.java](../../../backend/src/main/java/edu/aiplatform/ai/controller/AiConnectionController.java)
- [AiUsageController.java](../../../backend/src/main/java/edu/aiplatform/ai/controller/AiUsageController.java)
- [McpController.java](../../../backend/src/main/java/edu/aiplatform/ai/controller/McpController.java)
- [McpCredentialController.java](../../../backend/src/main/java/edu/aiplatform/ai/controller/McpCredentialController.java)

### Tests

- [AiTokenEstimateTest.java](../../../backend/src/test/java/edu/aiplatform/ai/contract/AiTokenEstimateTest.java)
- [McpInputTest.java](../../../backend/src/test/java/edu/aiplatform/ai/contract/McpInputTest.java)
- [AiControllersTest.java](../../../backend/src/test/java/edu/aiplatform/ai/controller/AiControllersTest.java)
- [HttpProviderGatewayTest.java](../../../backend/src/test/java/edu/aiplatform/ai/integration/HttpProviderGatewayTest.java)
- [AiDomainTest.java](../../../backend/src/test/java/edu/aiplatform/ai/model/AiDomainTest.java)
- [AiRepositoriesIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/ai/model/AiRepositoriesIntegrationTest.java)
- [AdminAiMcpToolsTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/AdminAiMcpToolsTest.java)
- [AiConnectionServiceTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/AiConnectionServiceTest.java)
- [AiSettingsSectionsTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/AiSettingsSectionsTest.java)
- [AiSourceServiceTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/AiSourceServiceTest.java)
- [AiTaskFlowTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/AiTaskFlowTest.java)
- [AiTaskResultHandlerRegistryTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/AiTaskResultHandlerRegistryTest.java)
- [AiUsageServiceTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/AiUsageServiceTest.java)
- [KeyCipherAndCatalogTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/KeyCipherAndCatalogTest.java)
- [McpCredentialServiceTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/McpCredentialServiceTest.java)
- [McpServerTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/McpServerTest.java)
- [McpStaffAdmissionTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/McpStaffAdmissionTest.java)
- [McpStaffCatalogTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/McpStaffCatalogTest.java)
- [McpWriteRequestIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/McpWriteRequestIntegrationTest.java)
- [QuotaServicesTest.java](../../../backend/src/test/java/edu/aiplatform/ai/service/QuotaServicesTest.java)

### Migrations

- [V20260925_1200__create_ai_tables.sql](../../../backend/src/main/resources/db/migration/ai/V20260925_1200__create_ai_tables.sql)
- [V20261026_1800__create_mcp_write_requests.sql](../../../backend/src/main/resources/db/migration/ai/V20261026_1800__create_mcp_write_requests.sql)
- [V20261027_0900__allow_many_institutional_keys.sql](../../../backend/src/main/resources/db/migration/ai/V20261027_0900__allow_many_institutional_keys.sql)
- [V20261027_1000__store_mcp_token_ciphertext.sql](../../../backend/src/main/resources/db/migration/ai/V20261027_1000__store_mcp_token_ciphertext.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [ai](../../../frontend/src/features/ai) | [api](../../../frontend/src/features/ai/api) | [model](../../../frontend/src/features/ai/model) | [pages](../../../frontend/src/features/ai/pages) | [components](../../../frontend/src/features/ai/components) | — | — | [tests](../../../frontend/tests/features/ai) |

## U07 — Assignment Authoring

Primary UC 34–36; FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

| Source | Trách nhiệm |
|---|---|
| [assignments/controller](../../../backend/src/main/java/edu/aiplatform/assignments/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [assignments/service](../../../backend/src/main/java/edu/aiplatform/assignments/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [assignments/repository](../../../backend/src/main/java/edu/aiplatform/assignments/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [assignments/model](../../../backend/src/main/java/edu/aiplatform/assignments/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [assignments/dto](../../../backend/src/main/java/edu/aiplatform/assignments/dto) | Kiểu dữ liệu request/response riêng. |
| [assignments/contract](../../../backend/src/main/java/edu/aiplatform/assignments/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

### Controllers

- [AssignmentAiDraftController.java](../../../backend/src/main/java/edu/aiplatform/assignments/controller/AssignmentAiDraftController.java)
- [AssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignments/controller/AssignmentController.java)
- [ClassAssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignments/controller/ClassAssignmentController.java)
- [RubricController.java](../../../backend/src/main/java/edu/aiplatform/assignments/controller/RubricController.java)
- [SubjectAssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignments/controller/SubjectAssignmentController.java)

### Tests

- [PartCalculatorTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/model/PartCalculatorTest.java)
- [AssignmentAiDraftServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/service/AssignmentAiDraftServiceTest.java)
- [AssignmentAuthoringServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/service/AssignmentAuthoringServiceTest.java)
- [AssignmentCopyServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/service/AssignmentCopyServiceTest.java)
- [AssignmentDraftGenerationSchemaTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/service/AssignmentDraftGenerationSchemaTest.java)
- [AssignmentDraftPromptBuilderTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/service/AssignmentDraftPromptBuilderTest.java)
- [AssignmentDraftValidatorTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/service/AssignmentDraftValidatorTest.java)
- [AssignmentMcpAuthoringServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/service/AssignmentMcpAuthoringServiceTest.java)
- [AssignmentMcpToolsTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/service/AssignmentMcpToolsTest.java)
- [AssignmentPublicationServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/service/AssignmentPublicationServiceTest.java)
- [AssignmentQueryServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/service/AssignmentQueryServiceTest.java)
- [DocumentModelServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/service/DocumentModelServiceTest.java)
- [DocxRoundTripTest.java](../../../backend/src/test/java/edu/aiplatform/assignments/service/DocxRoundTripTest.java)

### Migrations

- [V20260925_1300__create_assignment_tables.sql](../../../backend/src/main/resources/db/migration/assignments/V20260925_1300__create_assignment_tables.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [assignments](../../../frontend/src/features/assignments) | [api](../../../frontend/src/features/assignments/api) | [model](../../../frontend/src/features/assignments/model) | [pages](../../../frontend/src/features/assignments/pages) | [components](../../../frontend/src/features/assignments/components) | [hooks](../../../frontend/src/features/assignments/hooks) | — | [tests](../../../frontend/tests/features/assignments) |

## U08 — Assignment Work

Primary UC 37–41; FR-018, FR-030, FR-039.

| Source | Trách nhiệm |
|---|---|
| [assignmentwork/controller](../../../backend/src/main/java/edu/aiplatform/assignmentwork/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [assignmentwork/service](../../../backend/src/main/java/edu/aiplatform/assignmentwork/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [assignmentwork/repository](../../../backend/src/main/java/edu/aiplatform/assignmentwork/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [assignmentwork/model](../../../backend/src/main/java/edu/aiplatform/assignmentwork/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [assignmentwork/dto](../../../backend/src/main/java/edu/aiplatform/assignmentwork/dto) | Kiểu dữ liệu request/response riêng. |
| [assignmentwork/contract](../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

### Controllers

- [AttemptController.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/controller/AttemptController.java)
- [StudentAssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/controller/StudentAssignmentController.java)

### Tests

- [ActivityTest.java](../../../backend/src/test/java/edu/aiplatform/assignmentwork/service/ActivityTest.java)
- [AttemptServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignmentwork/service/AttemptServiceTest.java)
- [PracticeFeedbackConcurrencyIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/assignmentwork/service/PracticeFeedbackConcurrencyIntegrationTest.java)
- [PracticeFeedbackMcpParityTest.java](../../../backend/src/test/java/edu/aiplatform/assignmentwork/service/PracticeFeedbackMcpParityTest.java)
- [PracticeFeedbackServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignmentwork/service/PracticeFeedbackServiceTest.java)
- [StudentAssignmentServiceTest.java](../../../backend/src/test/java/edu/aiplatform/assignmentwork/service/StudentAssignmentServiceTest.java)

### Migrations

- [V20260925_1400__create_assignment_work_tables.sql](../../../backend/src/main/resources/db/migration/assignmentwork/V20260925_1400__create_assignment_work_tables.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [assignmentwork](../../../frontend/src/features/assignmentwork) | [api](../../../frontend/src/features/assignmentwork/api) | [model](../../../frontend/src/features/assignmentwork/model) | [pages](../../../frontend/src/features/assignmentwork/pages) | [components](../../../frontend/src/features/assignmentwork/components) | [hooks](../../../frontend/src/features/assignmentwork/hooks) | — | [tests](../../../frontend/tests/features/assignmentwork) |

## U09 — Assignment Grading

Primary UC 42–45; FR-008, FR-009, FR-020, FR-024.

| Source | Trách nhiệm |
|---|---|
| [grading/controller](../../../backend/src/main/java/edu/aiplatform/grading/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [grading/service](../../../backend/src/main/java/edu/aiplatform/grading/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [grading/repository](../../../backend/src/main/java/edu/aiplatform/grading/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [grading/model](../../../backend/src/main/java/edu/aiplatform/grading/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [grading/dto](../../../backend/src/main/java/edu/aiplatform/grading/dto) | Kiểu dữ liệu request/response riêng. |
| [grading/contract](../../../backend/src/main/java/edu/aiplatform/grading/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

### Controllers

- [EvaluationController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/EvaluationController.java)
- [GradebookController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/GradebookController.java)
- [GradingProposalController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/GradingProposalController.java)
- [MyGradeController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/MyGradeController.java)
- [SubmissionImageController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/SubmissionImageController.java)
- [SubmissionReviewController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/SubmissionReviewController.java)

### Tests

- [GroupSourceUnavailableTest.java](../../../backend/src/test/java/edu/aiplatform/grading/contract/stub/GroupSourceUnavailableTest.java)
- [GradingPersistenceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/integration/GradingPersistenceTest.java)
- [EvaluationTest.java](../../../backend/src/test/java/edu/aiplatform/grading/model/EvaluationTest.java)
- [GradeDistributionCalculatorTest.java](../../../backend/src/test/java/edu/aiplatform/grading/model/GradeDistributionCalculatorTest.java)
- [EvaluationMappingTest.java](../../../backend/src/test/java/edu/aiplatform/grading/repository/EvaluationMappingTest.java)
- [EvaluationVersionIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/grading/repository/EvaluationVersionIntegrationTest.java)
- [EvaluationLifecycleServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/EvaluationLifecycleServiceTest.java)
- [GradebookExporterTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradebookExporterTest.java)
- [GradebookServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradebookServiceTest.java)
- [GradingAccessTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradingAccessTest.java)
- [GradingBatchTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradingBatchTest.java)
- [GradingCommandsTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradingCommandsTest.java)
- [GradingMcpBatchServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradingMcpBatchServiceTest.java)
- [GradingMcpQueryServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradingMcpQueryServiceTest.java)
- [GradingMcpReplayTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradingMcpReplayTest.java)
- [GradingMcpToolsTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradingMcpToolsTest.java)
- [GradingPromptSchemaTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradingPromptSchemaTest.java)
- [GradingProposalResultHandlerTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradingProposalResultHandlerTest.java)
- [GradingProposalServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradingProposalServiceTest.java)
- [GradingProposalValidatorTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradingProposalValidatorTest.java)
- [GradingServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GradingServiceTest.java)
- [GroupEvaluationTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/GroupEvaluationTest.java)
- [SubmissionImageServiceTest.java](../../../backend/src/test/java/edu/aiplatform/grading/service/SubmissionImageServiceTest.java)

### Migrations

- [V20260925_1500__create_grading_tables.sql](../../../backend/src/main/resources/db/migration/grading/V20260925_1500__create_grading_tables.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [grading](../../../frontend/src/features/grading) | [api](../../../frontend/src/features/grading/api) | [model](../../../frontend/src/features/grading/model) | [pages](../../../frontend/src/features/grading/pages) | [components](../../../frontend/src/features/grading/components) | [hooks](../../../frontend/src/features/grading/hooks) | — | [tests](../../../frontend/tests/features/grading) |

## U10 — Quiz Authoring

Primary UC 46–48; FR-006, FR-007, FR-016, FR-017, FR-028.

| Source | Trách nhiệm |
|---|---|
| [quizzes/controller](../../../backend/src/main/java/edu/aiplatform/quizzes/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [quizzes/service](../../../backend/src/main/java/edu/aiplatform/quizzes/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [quizzes/repository](../../../backend/src/main/java/edu/aiplatform/quizzes/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [quizzes/model](../../../backend/src/main/java/edu/aiplatform/quizzes/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [quizzes/contract](../../../backend/src/main/java/edu/aiplatform/quizzes/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

### Controllers

- [ClassQuizController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/controller/ClassQuizController.java)
- [QuestionController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/controller/QuestionController.java)
- [QuizController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/controller/QuizController.java)
- [SubjectQuizController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/controller/SubjectQuizController.java)

### Tests

- [QuestionControllerTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/controller/QuestionControllerTest.java)
- [QuizControllersTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/controller/QuizControllersTest.java)
- [QuizPublicationMigrationIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/integration/QuizPublicationMigrationIntegrationTest.java)
- [QuestionRulesTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/model/QuestionRulesTest.java)
- [QuestionTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/model/QuestionTest.java)
- [QuizTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/model/QuizTest.java)
- [JdbcQuestionRepositoryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/repository/JdbcQuestionRepositoryIntegrationTest.java)
- [JdbcQuizRepositoryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/repository/JdbcQuizRepositoryIntegrationTest.java)
- [QuestionBankMcpToolsTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/service/QuestionBankMcpToolsTest.java)
- [QuestionBankServiceTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/service/QuestionBankServiceTest.java)
- [QuestionImporterTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/service/QuestionImporterTest.java)
- [QuizAiDraftServiceTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/service/QuizAiDraftServiceTest.java)
- [QuizDraftPromptTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/service/QuizDraftPromptTest.java)
- [QuizMcpAuthoringServiceTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/service/QuizMcpAuthoringServiceTest.java)
- [QuizMcpToolsSchemaTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/service/QuizMcpToolsSchemaTest.java)
- [QuizServicesTest.java](../../../backend/src/test/java/edu/aiplatform/quizzes/service/QuizServicesTest.java)

### Migrations

- [V20260925_1600__create_quiz_tables.sql](../../../backend/src/main/resources/db/migration/quizzes/V20260925_1600__create_quiz_tables.sql)
- [V20261030_1810__preserve_unpublished_quiz_retirement.sql](../../../backend/src/main/resources/db/migration/quizzes/V20261030_1810__preserve_unpublished_quiz_retirement.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [quizzes](../../../frontend/src/features/quizzes) | [api](../../../frontend/src/features/quizzes/api) | [model](../../../frontend/src/features/quizzes/model) | [pages](../../../frontend/src/features/quizzes/pages) | [components](../../../frontend/src/features/quizzes/components) | — | — | [tests](../../../frontend/tests/features/quizzes) |

## U11 — Quiz Practice

Primary UC 49–52; FR-017, FR-018, FR-039.

| Source | Trách nhiệm |
|---|---|
| [quizpractice/controller](../../../backend/src/main/java/edu/aiplatform/quizpractice/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [quizpractice/service](../../../backend/src/main/java/edu/aiplatform/quizpractice/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [quizpractice/repository](../../../backend/src/main/java/edu/aiplatform/quizpractice/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [quizpractice/model](../../../backend/src/main/java/edu/aiplatform/quizpractice/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [quizpractice/dto](../../../backend/src/main/java/edu/aiplatform/quizpractice/dto) | Kiểu dữ liệu request/response riêng. |

### Controllers

- [QuizPracticeController.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/controller/QuizPracticeController.java)
- [TeacherQuizAttemptController.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/controller/TeacherQuizAttemptController.java)

### Tests

- [QuizPracticeControllerTest.java](../../../backend/src/test/java/edu/aiplatform/quizpractice/controller/QuizPracticeControllerTest.java)
- [QuizAttemptTest.java](../../../backend/src/test/java/edu/aiplatform/quizpractice/model/QuizAttemptTest.java)
- [JdbcQuizAttemptRepositoryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/quizpractice/repository/JdbcQuizAttemptRepositoryIntegrationTest.java)
- [QuizPracticeServicesTest.java](../../../backend/src/test/java/edu/aiplatform/quizpractice/service/QuizPracticeServicesTest.java)

### Migrations

- [V20260925_1700__create_quiz_attempts.sql](../../../backend/src/main/resources/db/migration/quizpractice/V20260925_1700__create_quiz_attempts.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [quizpractice](../../../frontend/src/features/quizpractice) | [api](../../../frontend/src/features/quizpractice/api) | [model](../../../frontend/src/features/quizpractice/model) | [pages](../../../frontend/src/features/quizpractice/pages) | [components](../../../frontend/src/features/quizpractice/components) | — | — | [tests](../../../frontend/tests/features/quizpractice) |

## U12 — Group Assignment

Primary UC 53–57; FR-025, FR-026, FR-039.

| Source | Trách nhiệm |
|---|---|
| [groups/controller](../../../backend/src/main/java/edu/aiplatform/groups/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [groups/service](../../../backend/src/main/java/edu/aiplatform/groups/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [groups/repository](../../../backend/src/main/java/edu/aiplatform/groups/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [groups/model](../../../backend/src/main/java/edu/aiplatform/groups/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [groupdocs/controller](../../../backend/src/main/java/edu/aiplatform/groupdocs/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [groupdocs/service](../../../backend/src/main/java/edu/aiplatform/groupdocs/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [groupdocs/repository](../../../backend/src/main/java/edu/aiplatform/groupdocs/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [groupdocs/model](../../../backend/src/main/java/edu/aiplatform/groupdocs/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [groupdocs/integration](../../../backend/src/main/java/edu/aiplatform/groupdocs/integration) | Adapter hạ tầng/provider, listener và sender. |
| [groupdocs/contract](../../../backend/src/main/java/edu/aiplatform/groupdocs/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

### Controllers

- [GroupDocController.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/controller/GroupDocController.java)
- [GroupController.java](../../../backend/src/main/java/edu/aiplatform/groups/controller/GroupController.java)

### Tests

- [RabbitSseHubTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/integration/RabbitSseHubTest.java)
- [GroupRepositoriesIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/model/GroupRepositoriesIntegrationTest.java)
- [GroupSectionTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/model/GroupSectionTest.java)
- [GroupAutoSubmitScannerTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/service/GroupAutoSubmitScannerTest.java)
- [GroupDocumentServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/service/GroupDocumentServiceTest.java)
- [GroupSectionServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/service/GroupSectionServiceTest.java)
- [GroupSubmissionServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/service/GroupSubmissionServiceTest.java)
- [GroupWorkQueryServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/service/GroupWorkQueryServiceTest.java)
- [GroupWorkspaceServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groupdocs/service/GroupWorkspaceServiceTest.java)
- [GroupControllersTest.java](../../../backend/src/test/java/edu/aiplatform/groups/controller/GroupControllersTest.java)
- [GroupSetRulesTest.java](../../../backend/src/test/java/edu/aiplatform/groups/model/GroupSetRulesTest.java)
- [ClassGroupServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groups/service/ClassGroupServiceTest.java)
- [GroupRenameServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groups/service/GroupRenameServiceTest.java)
- [LeaderRequestServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groups/service/LeaderRequestServiceTest.java)
- [MyGroupServiceTest.java](../../../backend/src/test/java/edu/aiplatform/groups/service/MyGroupServiceTest.java)
- [RandomGroupSplitterTest.java](../../../backend/src/test/java/edu/aiplatform/groups/service/RandomGroupSplitterTest.java)

### Migrations

- [V20260925_1800__create_group_tables.sql](../../../backend/src/main/resources/db/migration/groups/V20260925_1800__create_group_tables.sql)
- [V20261025_1800__add_group_documents_assignment_fk.sql](../../../backend/src/main/resources/db/migration/groups/V20261025_1800__add_group_documents_assignment_fk.sql)
- [V20261029_1100__make_leader_request_reason_optional.sql](../../../backend/src/main/resources/db/migration/groups/V20261029_1100__make_leader_request_reason_optional.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [groups](../../../frontend/src/features/groups) | [api](../../../frontend/src/features/groups/api) | [model](../../../frontend/src/features/groups/model) | — | [components](../../../frontend/src/features/groups/components) | — | — | [tests](../../../frontend/tests/features/groups) |
| [groupdocs](../../../frontend/src/features/groupdocs) | [api](../../../frontend/src/features/groupdocs/api) | [model](../../../frontend/src/features/groupdocs/model) | [pages](../../../frontend/src/features/groupdocs/pages) | [components](../../../frontend/src/features/groupdocs/components) | [hooks](../../../frontend/src/features/groupdocs/hooks) | — | [tests](../../../frontend/tests/features/groupdocs) |

## U13 — Reference Materials

Primary UC 58–62; FR-004, FR-005.

| Source | Trách nhiệm |
|---|---|
| [materials/controller](../../../backend/src/main/java/edu/aiplatform/materials/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [materials/service](../../../backend/src/main/java/edu/aiplatform/materials/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [materials/repository](../../../backend/src/main/java/edu/aiplatform/materials/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [materials/model](../../../backend/src/main/java/edu/aiplatform/materials/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [materials/integration](../../../backend/src/main/java/edu/aiplatform/materials/integration) | Adapter hạ tầng/provider, listener và sender. |

### Controllers

- [ClassMaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/controller/ClassMaterialController.java)
- [MaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/controller/MaterialController.java)
- [MaterialModuleController.java](../../../backend/src/main/java/edu/aiplatform/materials/controller/MaterialModuleController.java)
- [MyMaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/controller/MyMaterialController.java)
- [SubjectMaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/controller/SubjectMaterialController.java)

### Tests

- [MaterialControllerTest.java](../../../backend/src/test/java/edu/aiplatform/materials/controller/MaterialControllerTest.java)
- [MaterialTextExtractorTest.java](../../../backend/src/test/java/edu/aiplatform/materials/integration/MaterialTextExtractorTest.java)
- [YoutubeVideoTest.java](../../../backend/src/test/java/edu/aiplatform/materials/model/YoutubeVideoTest.java)
- [MaterialAccessPolicyTest.java](../../../backend/src/test/java/edu/aiplatform/materials/service/MaterialAccessPolicyTest.java)
- [MaterialMcpTransactionTest.java](../../../backend/src/test/java/edu/aiplatform/materials/service/MaterialMcpTransactionTest.java)
- [MaterialModuleServiceTest.java](../../../backend/src/test/java/edu/aiplatform/materials/service/MaterialModuleServiceTest.java)
- [MaterialQueryAndReferenceTest.java](../../../backend/src/test/java/edu/aiplatform/materials/service/MaterialQueryAndReferenceTest.java)
- [MaterialServiceTest.java](../../../backend/src/test/java/edu/aiplatform/materials/service/MaterialServiceTest.java)
- [MaterialTextExtractJobHandlerTest.java](../../../backend/src/test/java/edu/aiplatform/materials/service/MaterialTextExtractJobHandlerTest.java)
- [ReferenceMaterialWiringTest.java](../../../backend/src/test/java/edu/aiplatform/materials/service/ReferenceMaterialWiringTest.java)

### Migrations

- [V20260925_1900__create_material_tables.sql](../../../backend/src/main/resources/db/migration/materials/V20260925_1900__create_material_tables.sql)
- [V20261029_1000__create_material_modules.sql](../../../backend/src/main/resources/db/migration/materials/V20261029_1000__create_material_modules.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [materials](../../../frontend/src/features/materials) | [api](../../../frontend/src/features/materials/api) | [model](../../../frontend/src/features/materials/model) | [pages](../../../frontend/src/features/materials/pages) | [components](../../../frontend/src/features/materials/components) | [hooks](../../../frontend/src/features/materials/hooks) | [styles](../../../frontend/src/features/materials/styles) | [tests](../../../frontend/tests/features/materials) |

## U14 — Announcements & Notifications

Primary UC 63–65; FR-011, FR-019, FR-023.

| Source | Trách nhiệm |
|---|---|
| [announcements/controller](../../../backend/src/main/java/edu/aiplatform/announcements/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [announcements/service](../../../backend/src/main/java/edu/aiplatform/announcements/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [announcements/repository](../../../backend/src/main/java/edu/aiplatform/announcements/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [announcements/model](../../../backend/src/main/java/edu/aiplatform/announcements/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [announcements/dto](../../../backend/src/main/java/edu/aiplatform/announcements/dto) | Kiểu dữ liệu request/response riêng. |
| [notifications/controller](../../../backend/src/main/java/edu/aiplatform/notifications/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [notifications/service](../../../backend/src/main/java/edu/aiplatform/notifications/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [notifications/repository](../../../backend/src/main/java/edu/aiplatform/notifications/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [notifications/model](../../../backend/src/main/java/edu/aiplatform/notifications/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [notifications/integration](../../../backend/src/main/java/edu/aiplatform/notifications/integration) | Adapter hạ tầng/provider, listener và sender. |
| [notifications/config](../../../backend/src/main/java/edu/aiplatform/notifications/config) | Properties, cấu hình bean và topology. |

### Controllers

- [AnnouncementController.java](../../../backend/src/main/java/edu/aiplatform/announcements/controller/AnnouncementController.java)
- [MyAnnouncementController.java](../../../backend/src/main/java/edu/aiplatform/announcements/controller/MyAnnouncementController.java)
- [NotificationController.java](../../../backend/src/main/java/edu/aiplatform/notifications/controller/NotificationController.java)
- [NotificationPreferenceController.java](../../../backend/src/main/java/edu/aiplatform/notifications/controller/NotificationPreferenceController.java)

### Tests

- [AnnouncementSanitizationTest.java](../../../backend/src/test/java/edu/aiplatform/announcements/model/AnnouncementSanitizationTest.java)
- [AnnouncementServiceTest.java](../../../backend/src/test/java/edu/aiplatform/announcements/service/AnnouncementServiceTest.java)
- [NotificationControllerTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/controller/NotificationControllerTest.java)
- [NotificationDeliveryIntegrationTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/integration/NotificationDeliveryIntegrationTest.java)
- [NotificationEventListenerTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/integration/NotificationEventListenerTest.java)
- [NotificationTopologyContextTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/integration/NotificationTopologyContextTest.java)
- [AssignmentNotificationRoutingTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/service/AssignmentNotificationRoutingTest.java)
- [DeadlineReminderScannerTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/service/DeadlineReminderScannerTest.java)
- [EmailDeliveryServiceTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/service/EmailDeliveryServiceTest.java)
- [NotificationEventServiceTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/service/NotificationEventServiceTest.java)
- [NotificationServiceTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/service/NotificationServiceTest.java)
- [RecipientResolverTest.java](../../../backend/src/test/java/edu/aiplatform/notifications/service/RecipientResolverTest.java)

### Migrations

- [V20260925_2000__create_notification_tables.sql](../../../backend/src/main/resources/db/migration/notifications/V20260925_2000__create_notification_tables.sql)

### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [announcements](../../../frontend/src/features/announcements) | [api](../../../frontend/src/features/announcements/api) | [model](../../../frontend/src/features/announcements/model) | [pages](../../../frontend/src/features/announcements/pages) | [components](../../../frontend/src/features/announcements/components) | — | [styles](../../../frontend/src/features/announcements/styles) | [tests](../../../frontend/tests/features/announcements) |
| [notifications](../../../frontend/src/features/notifications) | [api](../../../frontend/src/features/notifications/api) | [model](../../../frontend/src/features/notifications/model) | — | [components](../../../frontend/src/features/notifications/components) | — | [styles](../../../frontend/src/features/notifications/styles) | [tests](../../../frontend/tests/features/notifications) |

## U15 — Admin Dashboard

Primary UC 66; FR-024, FR-038.

| Source | Trách nhiệm |
|---|---|
| [admindashboard/controller](../../../backend/src/main/java/edu/aiplatform/admindashboard/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [admindashboard/service](../../../backend/src/main/java/edu/aiplatform/admindashboard/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [admindashboard/dto](../../../backend/src/main/java/edu/aiplatform/admindashboard/dto) | Kiểu dữ liệu request/response riêng. |
| [admindashboard/integration](../../../backend/src/main/java/edu/aiplatform/admindashboard/integration) | Adapter hạ tầng/provider, listener và sender. |

### Controllers

- [AdminDashboardController.java](../../../backend/src/main/java/edu/aiplatform/admindashboard/controller/AdminDashboardController.java)

### Tests

- [AdminDashboardApiContractTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/controller/AdminDashboardApiContractTest.java)
- [AdminDashboardControllerTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/controller/AdminDashboardControllerTest.java)
- [DashboardSourceReaderTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/integration/DashboardSourceReaderTest.java)
- [AdminDashboardAccessTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/service/AdminDashboardAccessTest.java)
- [AdminDashboardActivityServiceTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/service/AdminDashboardActivityServiceTest.java)
- [AdminDashboardBackupTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/service/AdminDashboardBackupTest.java)
- [AdminDashboardServiceTest.java](../../../backend/src/test/java/edu/aiplatform/admindashboard/service/AdminDashboardServiceTest.java)

### Migrations



### Frontend

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [admindashboard](../../../frontend/src/features/admindashboard) | [api](../../../frontend/src/features/admindashboard/api) | [model](../../../frontend/src/features/admindashboard/model) | [pages](../../../frontend/src/features/admindashboard/pages) | [components](../../../frontend/src/features/admindashboard/components) | [hooks](../../../frontend/src/features/admindashboard/hooks) | — | [tests](../../../frontend/tests/features/admindashboard) |

## Shared verification

- [ArchitectureTest.java](../../../backend/src/test/java/edu/aiplatform/ArchitectureTest.java)
