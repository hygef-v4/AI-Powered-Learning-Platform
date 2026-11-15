# U13 — Áp dụng quy ước mã nguồn (2026-11-15)

Thay đổi chỉ đổi tên và định dạng lại theo [Shared Code Conventions](../../code-conventions.md). Hành vi, URL, JSON, SQL, text block, MCP tool, job type, mã lỗi và message giữ nguyên. Danh mục đổi tên đầy đủ nằm trong [kế hoạch sinh mã](../../plans/code-generation-plan/2026-11-15-u04-u13-u14-code-conventions-code-generation-plan.md).

## Backend

- Sửa 25 file trong `backend/src/main/java/edu/aiplatform/materials/`.
- Controller: `listClassMaterials`, `createClassMaterial`, `loadMaterial`, `replaceMaterialVersion`, `issueDownloadToken`, `listSubjectModules`, `attachQuiz`…
- Service:
  - `MaterialService`: `createMaterial`, `updateMaterial`, `archiveMaterial`, `reorderMaterials`;
  - `MaterialModuleService`: `listClassModules`, `renameModule`, `deleteModule`;
  - `MaterialQueryService`: `listSubjectMaterials`, `loadAuthorizedMaterial`;
  - `MaterialAccessPolicy`: `requireReadAccess`, `requireWriteAccess`, `requireClassListAccess`, `canManageMaterial`, `denyAccess`.
- Repository: `findMaterial`, `listMaterials`, `insertMaterial`, `updateMaterial`, `findVersion`, `listVersions`, `listStalePendingVersions`; module: `findModule`, `insertModule`, `linkQuiz`, `unlinkModuleQuiz`…
- `MaterialRepositoryAdapter` định dạng lại, mỗi `.param` một dòng; SQL và text block giữ nguyên từng ký tự.
- Domain: `Material.inModule` → `moveToModule`, `withPresentation` → `changePresentation`; constructor phụ xếp mỗi tham số một dòng.
- Trong `MaterialModuleService`, lambda dựng Quiz của module được tách thành `buildModuleQuiz`. Trong `MaterialService.updateMaterial`, biểu thức ternary lồng được tách thành các biến `enabled`, `sameDescription`, `document`; thứ tự và kết quả tính không đổi.
- Giữ nguyên:
  - method hiện thực interface của unit khác (`IArtifactDownloadPolicy`, `IMcpToolProvider`, `IJobHandler`, `IPendingSweeper`, `IQuizLifecycleListener`, `IReferenceMaterial`, `IAssignmentSourceTextExtractor.extract`);
  - `MaterialViews.Version.of`, `YoutubeVideo.parse`;
  - `IMaterialRepository.findByFileId`, `insertVersion`, `completeText`.
- Sửa 10 file test và các lời gọi trong `requirements/RequirementsCoverageIntegrationTest.java`. Dòng code còn dài hơn 100 ký tự chỉ là chuỗi SQL.

## Frontend

- Đổi tên file `components/moduleNav.tsx` thành `components/ModuleNav.tsx` (`git mv`).
- API:
  - đường dẫn: `buildScopePath`, `buildMaterialPath`, `buildModulePath`, `buildOutlinePaths`, `buildContextQuery`;
  - helper ghi: `sendWrite`;
  - `materialApi`: `createMaterial`, `updateMaterial`, `replaceMaterialVersion`, `archiveMaterial`, `reorderMaterials`, `issueDownloadToken`;
  - `moduleApi`: `createModule`, `renameModule`, `deleteModule`, `reorderModules`;
  - `describeMaterialError`, `isValidYoutubeUrl`;
  - `enc` → `encodePathSegment`.
- Helper:
  - `detectDocumentKind`, `readPageNonce`, `applyStyleNonce`, `formatCellText`;
  - `describeQuiz`, `resolveRemovalAction`, `describeRemoval`;
  - `downloadMaterial`, `resolveSource`, `resolveModuleContext`, `fetchAttemptOwner`;
  - `buildStorageKey`, `readCollapsed`, `writeCollapsed`, `subscribeCollapse`;
  - `describePending`, `buildItemKey`, `resolveItemTitle`, `resolveItemIcon`, `buildItemHref`, `buildBoardHref`, `describeFile`, `buildMaterialHref`.
- Callback UI: `handleSave`, `handleRemove`, `handleDownload`, `handleMove`, `handleSaved`, `handleSubmit`, `handleEnabledChange`, `handleLoadError`, `handleToggleFullscreen`, `handleScroll`, `handleZoomStep`; `run` trong `ModuleBoard` → `runAction`.
- Giữ `reload` và `toggle` mà hook trả về (`useMaterialResource`, `useModuleCollapse`) vì đó là hình dạng chung của hook.
- Sửa 14 file trong `frontend/src/features/materials/`, 3 file test và caller `frontend/src/features/quizzes/pages/QuizDetailPage.tsx`.
- Tài liệu hiện hành: `code-summary.md` đổi `noncedStyles()` thành `applyStyleNonce()`.

## Kiểm tra

- Backend `mvn -o -B clean test` (subjects, materials, announcements, notifications): 493 test, 0 lỗi; 40 test bỏ qua vì không có Docker.
- So sánh literal trong diff: không đổi chuỗi nào. Mục `).param(` báo khác chỉ do tách chuỗi `.param` thành nhiều dòng.
- Frontend:
  - `tsc --noEmit` và ESLint sạch;
  - Vitest bốn feature: 23 file, 178 test đạt.
