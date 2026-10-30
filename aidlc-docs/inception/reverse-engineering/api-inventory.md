# API inventory — source annotations

Baseline `794a405304bf02dd2593a9fa2c88662d7cd52845`, source-observed. Controller-level RequestMapping plus method-level mappings below are exact source annotations; resolve their composition before client use. Non-controller MCP tool schemas are registered by IMcpToolProvider. Four existing OpenAPI specs are partial coverage, not a platform-wide contract.

## [academics\web\AcademicsController.java](../../../backend/src/main/java/edu/aiplatform/academics/web/AcademicsController.java)

```java
@RequestMapping("/api/v1")
@GetMapping("/me/classes")
@GetMapping("/teaching/classes")
@GetMapping("/manager/classes")
@GetMapping("/me/classes/{id}")
@GetMapping("/teaching/classes/{id}")
@GetMapping("/manager/classes/{id}")
@GetMapping("/teaching/classes/{id}/students")
@GetMapping("/manager/classes/{id}/students")
@GetMapping("/teaching/classes/{id}/students/{accountId}")
@GetMapping("/manager/subjects")
@GetMapping("/manager/subjects/{id}")
@GetMapping("/manager/teacher-candidates")
@GetMapping("/manager/classes/{id}/student-candidates")
@PostMapping("/manager/classes")
@PatchMapping("/manager/classes/{id}")
@PutMapping("/manager/classes/{id}/teacher")
@PutMapping("/manager/classes/{id}/grade-distribution")
@PutMapping("/manager/classes/{id}/status")
@PostMapping("/manager/classes/{id}/enrollments")
@PostMapping("/manager/classes/{id}/enrollments:batch")
@PutMapping("/manager/classes/{id}/enrollments/{accountId}/status")
```

## [admindashboard\web\AdminDashboardController.java](../../../backend/src/main/java/edu/aiplatform/admindashboard/web/AdminDashboardController.java)

```java
@RequestMapping("/api/v1/admin/dashboard")
@GetMapping
@GetMapping("/activity")
@GetMapping("/backups")
```

## [ai\web\AiAdminController.java](../../../backend/src/main/java/edu/aiplatform/ai/web/AiAdminController.java)

```java
@RequestMapping("/api/v1/admin/ai")
@GetMapping("/connections")
@PostMapping("/connections")
@PostMapping("/connections:test")
@PutMapping("/connections/{id}")
@GetMapping("/connections/{id}/key")
@PostMapping("/connections/{id}:test")
@PutMapping("/connections/{id}/status")
@DeleteMapping("/connections/{id}")
@GetMapping("/quota-policy")
@PutMapping("/quota-policy")
@GetMapping("/teacher-quotas")
@PutMapping("/teacher-quotas/{accountId}")
@DeleteMapping("/teacher-quotas/{accountId}")
@GetMapping("/teacher-candidates")
@GetMapping("/usage-summary")
```

## [ai\web\AiConnectionController.java](../../../backend/src/main/java/edu/aiplatform/ai/web/AiConnectionController.java)

```java
@GetMapping("/api/v1/ai/providers")
@GetMapping("/api/v1/me/ai-connections")
@PutMapping("/api/v1/me/ai-connections/{provider}")
@PostMapping("/api/v1/me/ai-connections/{provider}:test")
@DeleteMapping("/api/v1/me/ai-connections/{provider}")
```

## [ai\web\AiUsageController.java](../../../backend/src/main/java/edu/aiplatform/ai/web/AiUsageController.java)

```java
@GetMapping("/api/v1/me/ai-usage")
@GetMapping("/api/v1/ai/sources")
@GetMapping("/api/v1/ai/tasks/{id}")
@PostMapping("/api/v1/ai/tasks/{id}:cancel")
```

## [ai\web\McpController.java](../../../backend/src/main/java/edu/aiplatform/ai/web/McpController.java)

```java
@RequestMapping("/mcp")
@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@RequestMapping(method = {RequestMethod.GET, RequestMethod.DELETE})
```

## [ai\web\McpCredentialController.java](../../../backend/src/main/java/edu/aiplatform/ai/web/McpCredentialController.java)

```java
@RequestMapping("/api/v1/me/mcp-credentials")
@GetMapping
@PostMapping
@PatchMapping("/{id}")
@GetMapping("/{id}/token")
@DeleteMapping("/{id}")
```

## [announcements\web\AnnouncementController.java](../../../backend/src/main/java/edu/aiplatform/announcements/web/AnnouncementController.java)

```java
@GetMapping("/classes/{classId}/announcements")
@PostMapping("/classes/{classId}/announcements") @ResponseStatus(HttpStatus.CREATED)
@PatchMapping("/announcements/{id}")
@DeleteMapping("/announcements/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
```

## [announcements\web\MyAnnouncementController.java](../../../backend/src/main/java/edu/aiplatform/announcements/web/MyAnnouncementController.java)

```java
@GetMapping
```

## [assignments\web\AssignmentAiDraftController.java](../../../backend/src/main/java/edu/aiplatform/assignments/web/AssignmentAiDraftController.java)

```java
@RequestMapping("/api/v1/assignments/{id}/ai-drafts")
@PostMapping(consumes = "application/json")
@GetMapping("/{draftId}")
@PostMapping(value = "/sources:preview", consumes = "application/json")
@PostMapping(value = "/{draftId}:apply", consumes = "application/json")
@PostMapping("/{draftId}:discard")
```

## [assignments\web\AssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignments/web/AssignmentController.java)

```java
@RequestMapping("/api/v1/assignments/{id}")
@GetMapping
@PatchMapping(consumes = "application/json")
@DeleteMapping
@GetMapping("/selectable-materials")
@PutMapping(value = "/skeleton", consumes = "application/json")
@PostMapping(value = "/skeleton:import-docx", consumes = "multipart/form-data")
@GetMapping("/docx")
@GetMapping("/rubric/docx")
@GetMapping("/diagrams/{blockId}")
@GetMapping("/blocks/{blockId}/image")
@PostMapping(value = "/review", consumes = "application/json")
@PostMapping(value = "/publish", consumes = "application/json")
@PatchMapping(value = "/schedule", consumes = "application/json")
@PostMapping(value = "/retire", consumes = "application/json")
@PostMapping("/clone")
@PostMapping("/versions")
```

## [assignments\web\ClassAssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignments/web/ClassAssignmentController.java)

```java
@RequestMapping("/api/v1/classes/{classId}")
@GetMapping("/assignments")
@PostMapping(value = "/assignments", consumes = "application/json")
@GetMapping("/assignment-copy-sources")
@PostMapping(value = "/assignments:copy", consumes = "application/json")
```

## [assignments\web\RubricController.java](../../../backend/src/main/java/edu/aiplatform/assignments/web/RubricController.java)

```java
@RequestMapping("/api/v1/assignments/{id}/parts/{partId}/rubric")
@GetMapping
@PutMapping(consumes = "application/json")
```

## [assignments\web\SubjectAssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignments/web/SubjectAssignmentController.java)

```java
@RequestMapping("/api/v1/subjects/{subjectId}/assignments")
@GetMapping("/copy-sources")
@PostMapping(value = "/copies", consumes = "application/json")
@GetMapping
@PostMapping(consumes = "application/json")
```

## [assignmentwork\web\AttemptController.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/web/AttemptController.java)

```java
@PostMapping(value = "/api/v1/assignments/{assignmentId}/attempts", consumes = "application/json")
@GetMapping("/api/v1/assignments/{assignmentId}/attempts/mine")
@GetMapping("/api/v1/attempts/{attemptId}")
@PutMapping(value = "/api/v1/attempts/{attemptId}/content", consumes = "application/json")
@PostMapping(value = "/api/v1/attempts/{attemptId}/docx:preview", consumes = "multipart/form-data")
@PostMapping(value = "/api/v1/attempts/{attemptId}/submit", consumes = "application/json")
@GetMapping("/api/v1/attempts/{attemptId}/receipt")
@GetMapping("/api/v1/attempts/{attemptId}/export.docx")
@GetMapping("/api/v1/attempts/{attemptId}/blocks/{blockId}/image")
@GetMapping("/api/v1/attempts/{attemptId}/diagrams/{blockId}")
@PostMapping(value = "/api/v1/attempts/{attemptId}/feedback", consumes = "application/json")
@GetMapping("/api/v1/attempts/{attemptId}/feedback")
@PostMapping(value = "/api/v1/attempts/{attemptId}/activity", consumes = "application/json")
```

## [assignmentwork\web\StudentAssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/web/StudentAssignmentController.java)

```java
@RequestMapping("/api/v1/me/assignments")
@GetMapping
@GetMapping("/{assignmentId}")
```

## [audit\web\AuditController.java](../../../backend/src/main/java/edu/aiplatform/audit/web/AuditController.java)

```java
@RequestMapping("/api/v1/admin/audit-logs")
@GetMapping
```

## [files\web\DownloadController.java](../../../backend/src/main/java/edu/aiplatform/files/web/DownloadController.java)

```java
@GetMapping("/api/v1/files/download/{token}")
```

## [files\web\FileController.java](../../../backend/src/main/java/edu/aiplatform/files/web/FileController.java)

```java
@RequestMapping("/api/v1/files")
@PostMapping(consumes = "multipart/form-data")
@GetMapping("/policies")
```

## [grading\web\EvaluationController.java](../../../backend/src/main/java/edu/aiplatform/grading/web/EvaluationController.java)

```java
@RequestMapping("/api/v1")
@GetMapping("/evaluations/{id}")
@PutMapping("/evaluations/{id}")
@PutMapping("/evaluations/{id}/members/{accountId}")
@PostMapping("/evaluations:finalize")
@PostMapping("/evaluations:publish")
```

## [grading\web\GradebookController.java](../../../backend/src/main/java/edu/aiplatform/grading/web/GradebookController.java)

```java
@RequestMapping("/api/v1/classes/{classId}")
@GetMapping("/gradebook")
@GetMapping("/gradebook/export")
@GetMapping("/grade-distribution")
@GetMapping("/students/{accountId}/grades")
```

## [grading\web\GradingProposalController.java](../../../backend/src/main/java/edu/aiplatform/grading/web/GradingProposalController.java)

```java
@RequestMapping("/api/v1")
@PostMapping("/evaluations/{id}/ai-proposals")
@GetMapping("/evaluations/{id}/ai-proposals")
@PostMapping("/evaluations:ai-proposals")
@PostMapping("/evaluations:ai-proposal-estimate")
```

## [grading\web\MyGradeController.java](../../../backend/src/main/java/edu/aiplatform/grading/web/MyGradeController.java)

```java
@RequestMapping("/api/v1/me")
@GetMapping("/grades")
@GetMapping("/grades/attempts/{attemptId}")
@GetMapping("/grade-distributions")
```

## [grading\web\SubmissionImageController.java](../../../backend/src/main/java/edu/aiplatform/grading/web/SubmissionImageController.java)

```java
@RequestMapping("/api/v1/evaluations/{evaluationId}/images")
@GetMapping("/{fileId}")
```

## [grading\web\SubmissionReviewController.java](../../../backend/src/main/java/edu/aiplatform/grading/web/SubmissionReviewController.java)

```java
@RequestMapping("/api/v1/classes/{classId}")
@GetMapping("/assignments/{assignmentId}/submissions")
@GetMapping("/submission-progress")
```

## [groupdocs\web\GroupDocController.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/web/GroupDocController.java)

```java
@GetMapping("/api/v1/assignments/{assignmentId}/my-group-doc")
@GetMapping("/api/v1/group-docs/{id}")
@GetMapping(value = "/api/v1/group-docs/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping("/api/v1/group-docs/{id}/activity")
@PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:claim")
@PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:assign")
@PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:release")
@PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:done")
@PutMapping("/api/v1/group-docs/{id}/sections/{sectionId}/draft")
@PostMapping("/api/v1/group-docs/{id}/submit")
@GetMapping("/api/v1/group-docs/{id}/export.docx")
```

## [groups\web\GroupController.java](../../../backend/src/main/java/edu/aiplatform/groups/web/GroupController.java)

```java
@GetMapping("/api/v1/classes/{classId}/groups")
@PutMapping("/api/v1/classes/{classId}/groups")
@PostMapping("/api/v1/classes/{classId}/groups:random-preview")
@GetMapping("/api/v1/classes/{classId}/leader-requests")
@PostMapping("/api/v1/leader-requests/{id}:approve")
@PostMapping("/api/v1/leader-requests/{id}:reject")
@GetMapping("/api/v1/me/classes/{classId}/group")
@PutMapping("/api/v1/groups/{groupId}/name")
@PostMapping("/api/v1/groups/{groupId}/leader-requests")
@PostMapping("/api/v1/leader-requests/{id}:cancel")
@GetMapping("/api/v1/classes/{classId}/students/{studentId}/group")
@GetMapping("/api/v1/classes/{classId}/group-assignments")
@GetMapping("/api/v1/classes/{classId}/group-assignments/{assignmentId}/progress")
```

## [identity\web\AdminAccountController.java](../../../backend/src/main/java/edu/aiplatform/identity/web/AdminAccountController.java)

```java
@RequestMapping("/api/v1/admin/accounts")
@GetMapping
@PostMapping
@GetMapping("/{id}")
@PatchMapping("/{id}")
@PutMapping("/{id}/role")
@PutMapping("/{id}/status")
@PostMapping("/imports:validate")
@PostMapping("/imports:commit")
```

## [identity\web\AuthController.java](../../../backend/src/main/java/edu/aiplatform/identity/web/AuthController.java)

```java
@RequestMapping("/api/v1/auth")
@PostMapping("/login")
@PostMapping("/refresh")
@PostMapping("/logout")
```

## [identity\web\MeController.java](../../../backend/src/main/java/edu/aiplatform/identity/web/MeController.java)

```java
@RequestMapping("/api/v1/me")
@GetMapping("/profile")
@PatchMapping("/profile")
@PostMapping("/password")
```

## [identity\web\OtpFlowController.java](../../../backend/src/main/java/edu/aiplatform/identity/web/OtpFlowController.java)

```java
@RequestMapping("/api/v1/auth")
@PostMapping("/activation/request")
@PostMapping("/activation/verify")
@PostMapping("/activation/complete")
@PostMapping("/password-reset/request")
@PostMapping("/password-reset/verify")
@PostMapping("/password-reset/complete")
```

## [materials\web\ClassMaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/web/ClassMaterialController.java)

```java
@RequestMapping("/api/v1/classes/{classId}")
@GetMapping("/materials") public List<MaterialViews.Detail> list(@AuthenticationPrincipal Actor actor,@PathVariable UUID classId) {
@PostMapping("/materials") @ResponseStatus(HttpStatus.CREATED)
@PutMapping("/materials:reorder") @ResponseStatus(HttpStatus.NO_CONTENT)
```

## [materials\web\MaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/web/MaterialController.java)

```java
@RequestMapping("/api/v1/materials/{id}")
@GetMapping({"","/view"}) public MaterialViews.Detail detail(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
@PatchMapping public MaterialViews.Detail update(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
@PostMapping("/versions") public MaterialViews.Detail replace(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
@PostMapping("/archive") public MaterialViews.Detail archive(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
@PostMapping("/download-token") public DownloadToken download(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
```

## [materials\web\MaterialModuleController.java](../../../backend/src/main/java/edu/aiplatform/materials/web/MaterialModuleController.java)

```java
@RequestMapping("/api/v1")
@GetMapping("/subjects/{subjectId}/modules")
@PostMapping("/subjects/{subjectId}/modules") @ResponseStatus(HttpStatus.CREATED)
@PutMapping("/subjects/{subjectId}/modules:reorder") @ResponseStatus(HttpStatus.NO_CONTENT)
@GetMapping("/classes/{classId}/modules")
@GetMapping("/me/classes/{classId}/modules")
@PatchMapping("/material-modules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
@DeleteMapping("/material-modules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
@PostMapping("/material-modules/{id}/quizzes") @ResponseStatus(HttpStatus.NO_CONTENT)
@DeleteMapping("/material-modules/{id}/quizzes/{quizId}") @ResponseStatus(HttpStatus.NO_CONTENT)
```

## [materials\web\MyMaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/web/MyMaterialController.java)

```java
@RequestMapping("/api/v1/me/classes/{classId}/materials")
@GetMapping public List<MaterialViews.Detail> list(@AuthenticationPrincipal Actor actor,@PathVariable UUID classId,
```

## [materials\web\SubjectMaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/web/SubjectMaterialController.java)

```java
@RequestMapping("/api/v1/subjects/{subjectId}")
@GetMapping("/materials") public List<MaterialViews.Detail> list(@AuthenticationPrincipal Actor actor,@PathVariable UUID subjectId) {
@PostMapping("/materials") @ResponseStatus(HttpStatus.CREATED)
@PutMapping("/materials:reorder") @ResponseStatus(HttpStatus.NO_CONTENT)
```

## [notifications\web\NotificationController.java](../../../backend/src/main/java/edu/aiplatform/notifications/web/NotificationController.java)

```java
@GetMapping("/notifications")
@GetMapping("/notifications/unread-count")
@PostMapping("/notifications/{id}:read") @ResponseStatus(HttpStatus.NO_CONTENT)
@PostMapping("/notifications:read-all") @ResponseStatus(HttpStatus.NO_CONTENT)
@GetMapping(value="/notifications/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
```

## [notifications\web\NotificationPreferenceController.java](../../../backend/src/main/java/edu/aiplatform/notifications/web/NotificationPreferenceController.java)

```java
@GetMapping public Map<String,Boolean> get(@AuthenticationPrincipal Actor actor) {return service.get(actor);}
@PutMapping public Map<String,Boolean> put(@AuthenticationPrincipal Actor actor,@RequestBody Map<String,Boolean> input) {return service.put(actor,input);}
```

## [quizpractice\web\QuizPracticeController.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/web/QuizPracticeController.java)

```java
@RequestMapping("/api/v1")
@GetMapping("/me/quizzes")
@GetMapping("/me/quizzes/{quizId}")
@PostMapping(value = "/quizzes/{quizId}/attempts", consumes = "application/json")
@GetMapping("/quiz-attempts/{attemptId}")
@PutMapping(value = "/quiz-attempts/{attemptId}/answers", consumes = "application/json")
@PostMapping("/quiz-attempts/{attemptId}/submit")
@GetMapping("/quiz-attempts/{attemptId}/result")
@PostMapping(value = "/quiz-attempts/{attemptId}/activity", consumes = "application/json")
```

## [quizpractice\web\TeacherQuizAttemptController.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/web/TeacherQuizAttemptController.java)

```java
@RequestMapping("/api/v1/classes/{classId}")
@GetMapping("/quizzes/{quizId}/attempts")
@GetMapping("/quiz-attempts/{attemptId}")
```

## [quizzes\web\ClassQuizController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/web/ClassQuizController.java)

```java
@RequestMapping("/api/v1/classes/{classId}")
@GetMapping("/quizzes")
@PostMapping(value = "/quizzes", consumes = "application/json")
@GetMapping("/quiz-copy-sources")
@PostMapping(value = "/quizzes:copy", consumes = "application/json")
```

## [quizzes\web\QuestionController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/web/QuestionController.java)

```java
@RequestMapping("/api/v1")
@GetMapping("/subjects/{subjectId}/reference-materials")
@GetMapping("/subjects/{subjectId}/question-ai-drafts")
@PostMapping(value = "/subjects/{subjectId}/question-ai-drafts", consumes = "application/json")
@GetMapping("/subjects/{subjectId}/question-ai-drafts/{draftId}")
@PostMapping(value = "/subjects/{subjectId}/question-ai-drafts/{draftId}:accept", consumes = "application/json")
@PostMapping("/subjects/{subjectId}/question-ai-drafts/{draftId}:discard")
@GetMapping("/subjects/{subjectId}/questions")
@PostMapping(value = "/subjects/{subjectId}/questions", consumes = "application/json")
@PostMapping(value = "/subjects/{subjectId}/questions:import", consumes = "multipart/form-data")
@GetMapping("/questions/{id}")
@PatchMapping(value = "/questions/{id}", consumes = "application/json")
@DeleteMapping("/questions/{id}")
@PostMapping("/questions/{id}/versions")
@PostMapping(value = "/questions/{id}/activate", consumes = "application/json")
@PostMapping(value = "/questions/{id}/retire", consumes = "application/json")
```

## [quizzes\web\QuizController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/web/QuizController.java)

```java
@RequestMapping("/api/v1/quizzes/{id}")
@GetMapping("/reference-materials")
@GetMapping("/ai-drafts")
@PostMapping(value = "/ai-drafts", consumes = "application/json")
@GetMapping("/ai-drafts/{draftId}")
@PostMapping(value = "/ai-drafts/{draftId}:accept", consumes = "application/json")
@PostMapping("/ai-drafts/{draftId}:discard")
@GetMapping
@PatchMapping(consumes = "application/json")
@DeleteMapping
@PutMapping(value = "/items", consumes = "application/json")
@PostMapping(value = "/items:random-preview", consumes = "application/json")
@GetMapping("/bank-questions")
@PostMapping(value = "/inline-questions", consumes = "application/json")
@PatchMapping(value = "/inline-questions/{questionId}", consumes = "application/json")
@DeleteMapping("/inline-questions/{questionId}")
@PostMapping(value = "/review", consumes = "application/json")
@PostMapping(value = "/publish", consumes = "application/json")
@PostMapping(value = "/retire", consumes = "application/json")
@PostMapping(value = "/reopen", consumes = "application/json")
@PostMapping("/clone")
@PostMapping("/versions")
```

## [quizzes\web\SubjectQuizController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/web/SubjectQuizController.java)

```java
@RequestMapping("/api/v1/subjects/{subjectId}/quizzes")
@GetMapping
@PostMapping(consumes = "application/json")
```

## [settings\web\SettingsController.java](../../../backend/src/main/java/edu/aiplatform/settings/web/SettingsController.java)

```java
@RequestMapping("/api/v1/admin/settings")
@GetMapping
@GetMapping("/{key}")
@PatchMapping("/{key}")
```

## [subjects\web\SubjectAdminController.java](../../../backend/src/main/java/edu/aiplatform/subjects/web/SubjectAdminController.java)

```java
@RequestMapping("/api/v1/admin/subjects")
@GetMapping
@PostMapping(consumes = "application/json")
@GetMapping("/{id}")
@GetMapping("/{id}/classes")
@PatchMapping(value = "/{id}", consumes = "application/json")
@PutMapping(value = "/{id}/manager", consumes = "application/json")
@PutMapping(value = "/{id}/status", consumes = "application/json")
```
