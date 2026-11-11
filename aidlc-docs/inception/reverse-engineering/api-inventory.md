# API inventory — source annotations, 2026-11-07

Source snapshot: `f68a6ec5889074de80a1f1f0faf7bcb7e7054ec8`; đối chiếu lại 2026-11-10 với `staging@1e77eb79`: 48 controller files, 241 mapping annotation khác nhau khớp hoàn toàn với source (không thừa, không thiếu) sau khi thêm ba endpoint đọc của PR #60 (`/ai-drafts/templates`, `/assignment-materials`, `/subjects/{subjectId}/assignments/materials`). `POST /assignments/{id}/ai-drafts` nhận thêm `mode = REVISE` trong body mà không đổi annotation; `GET /api/v1/attempts/{attemptId}/receipt` còn ở backend nhưng frontend không gọi nữa. Entries reproduce class/method mapping annotations from source; compose class-level RequestMapping with each method mapping and retain HTTP method, path arrays, consumes/produces and parameters. They are an annotation inventory, not complete request/response schemas or fresh endpoint acceptance. Non-controller MCP schemas remain owned by IMcpToolProvider. Existing OpenAPI specs are partial coverage.

## [academics/controller/AcademicsController.java](../../../backend/src/main/java/edu/aiplatform/academics/controller/AcademicsController.java)

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
@PutMapping("/manager/classes/{id}/status")
@PostMapping("/manager/classes/{id}/enrollments")
@PostMapping("/manager/classes/{id}/enrollments:batch")
@PutMapping("/manager/classes/{id}/enrollments/{accountId}/status")
```

## [admindashboard/controller/AdminDashboardController.java](../../../backend/src/main/java/edu/aiplatform/admindashboard/controller/AdminDashboardController.java)

```java
@RequestMapping("/api/v1/admin/dashboard")
@GetMapping
@GetMapping("/activity")
@GetMapping("/backups")
```

## [ai/controller/AiAdminController.java](../../../backend/src/main/java/edu/aiplatform/ai/controller/AiAdminController.java)

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

## [ai/controller/AiConnectionController.java](../../../backend/src/main/java/edu/aiplatform/ai/controller/AiConnectionController.java)

```java
@GetMapping("/api/v1/me/ai-source-preference")
@PutMapping("/api/v1/me/ai-source-preference")
@GetMapping("/api/v1/ai/providers")
@GetMapping("/api/v1/me/ai-connections")
@PostMapping("/api/v1/me/ai-connections")
@PostMapping("/api/v1/me/ai-connections:test")
@PutMapping("/api/v1/me/ai-connections/{id}")
@GetMapping("/api/v1/me/ai-connections/{id}/key")
@PostMapping("/api/v1/me/ai-connections/{id}:test")
@PutMapping("/api/v1/me/ai-connections/{id}/status")
@DeleteMapping("/api/v1/me/ai-connections/{id}")
```

## [ai/controller/AiUsageController.java](../../../backend/src/main/java/edu/aiplatform/ai/controller/AiUsageController.java)

```java
@GetMapping("/api/v1/me/ai-usage")
@GetMapping("/api/v1/ai/sources")
@GetMapping("/api/v1/ai/tasks/{id}")
@PostMapping("/api/v1/ai/tasks/{id}:cancel")
```

## [ai/controller/McpController.java](../../../backend/src/main/java/edu/aiplatform/ai/controller/McpController.java)

```java
@RequestMapping("/mcp")
@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@RequestMapping(method = {RequestMethod.GET, RequestMethod.DELETE})
```

## [ai/controller/McpCredentialController.java](../../../backend/src/main/java/edu/aiplatform/ai/controller/McpCredentialController.java)

```java
@RequestMapping("/api/v1/me/mcp-credentials")
@GetMapping
@PostMapping
@PatchMapping("/{id}")
@GetMapping("/{id}/token")
@DeleteMapping("/{id}")
```

## [announcements/controller/AnnouncementController.java](../../../backend/src/main/java/edu/aiplatform/announcements/controller/AnnouncementController.java)

```java
@RequestMapping("/api/v1")
@GetMapping("/classes/{classId}/announcements")
@PostMapping("/classes/{classId}/announcements")
@PatchMapping("/announcements/{id}")
@DeleteMapping("/announcements/{id}")
```

## [announcements/controller/MyAnnouncementController.java](../../../backend/src/main/java/edu/aiplatform/announcements/controller/MyAnnouncementController.java)

```java
@RequestMapping("/api/v1/me/announcements")
@GetMapping
```

## [assignments/controller/AssignmentAiDraftController.java](../../../backend/src/main/java/edu/aiplatform/assignments/controller/AssignmentAiDraftController.java)

```java
@RequestMapping("/api/v1/assignments/{id}/ai-drafts")
@PostMapping(consumes = "application/json")
@GetMapping("/{draftId}")
@PostMapping(value = "/sources:preview", consumes = "application/json")
@GetMapping("/templates")
@PostMapping(value = "/{draftId}:apply", consumes = "application/json")
@PostMapping("/{draftId}:discard")
```

## [assignments/controller/AssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignments/controller/AssignmentController.java)

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

## [assignments/controller/ClassAssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignments/controller/ClassAssignmentController.java)

```java
@RequestMapping("/api/v1/classes/{classId}")
@GetMapping("/assignments")
@PostMapping(value = "/assignments", consumes = "application/json")
@GetMapping("/assignment-copy-sources")
@GetMapping("/assignment-materials")
@PostMapping(value = "/assignments:copy", consumes = "application/json")
```

## [assignments/controller/RubricController.java](../../../backend/src/main/java/edu/aiplatform/assignments/controller/RubricController.java)

```java
@RequestMapping("/api/v1/assignments/{id}/parts/{partId}/rubric")
@GetMapping
@PutMapping(consumes = "application/json")
```

## [assignments/controller/SubjectAssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignments/controller/SubjectAssignmentController.java)

```java
@RequestMapping("/api/v1/subjects/{subjectId}/assignments")
@GetMapping("/copy-sources")
@GetMapping("/materials")
@PostMapping(value = "/copies", consumes = "application/json")
@GetMapping
@PostMapping(consumes = "application/json")
```

## [assignmentwork/controller/AttemptController.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/controller/AttemptController.java)

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

## [assignmentwork/controller/StudentAssignmentController.java](../../../backend/src/main/java/edu/aiplatform/assignmentwork/controller/StudentAssignmentController.java)

```java
@RequestMapping("/api/v1/me/assignments")
@GetMapping
@GetMapping("/{assignmentId}")
```

## [audit/controller/AuditController.java](../../../backend/src/main/java/edu/aiplatform/audit/controller/AuditController.java)

```java
@RequestMapping("/api/v1/admin/audit-logs")
@GetMapping
```

## [files/controller/DownloadController.java](../../../backend/src/main/java/edu/aiplatform/files/controller/DownloadController.java)

```java
@GetMapping("/api/v1/files/download/{token}")
```

## [files/controller/FileController.java](../../../backend/src/main/java/edu/aiplatform/files/controller/FileController.java)

```java
@RequestMapping("/api/v1/files")
@PostMapping(consumes = "multipart/form-data")
@GetMapping("/policies")
```

## [grading/controller/EvaluationController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/EvaluationController.java)

```java
@RequestMapping("/api/v1")
@GetMapping("/evaluations/{id}")
@PutMapping("/evaluations/{id}")
@PutMapping("/evaluations/{id}/members/{accountId}")
@PostMapping("/evaluations:finalize")
@PostMapping("/evaluations:publish")
```

## [grading/controller/GradeDistributionVisibilityController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/GradeDistributionVisibilityController.java)

```java
@RequestMapping("/api/v1/classes/{classId}/assignments/{assignmentId}/grade-distribution-visibility")
@GetMapping
@PutMapping
```

## [grading/controller/GradebookController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/GradebookController.java)

```java
@RequestMapping("/api/v1/classes/{classId}")
@GetMapping("/gradebook")
@GetMapping("/gradebook/export")
@GetMapping("/grade-distribution")
@GetMapping("/students/{accountId}/grades")
```

## [grading/controller/GradingProposalController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/GradingProposalController.java)

```java
@RequestMapping("/api/v1")
@PostMapping("/evaluations/{id}/ai-proposals")
@GetMapping("/evaluations/{id}/ai-proposals")
@PostMapping("/evaluations:ai-proposals")
@PostMapping("/evaluations:ai-proposal-estimate")
```

## [grading/controller/MyGradeController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/MyGradeController.java)

```java
@RequestMapping("/api/v1/me")
@GetMapping("/grades")
@GetMapping("/grades/attempts/{attemptId}")
@GetMapping("/grade-distributions")
```

## [grading/controller/SubmissionImageController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/SubmissionImageController.java)

```java
@RequestMapping("/api/v1/evaluations/{evaluationId}/images")
@GetMapping("/{fileId}")
```

## [grading/controller/SubmissionReviewController.java](../../../backend/src/main/java/edu/aiplatform/grading/controller/SubmissionReviewController.java)

```java
@RequestMapping("/api/v1/classes/{classId}")
@GetMapping("/assignments/{assignmentId}/submissions")
@GetMapping("/submission-progress")
```

## [groupdocs/controller/GroupDocController.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/controller/GroupDocController.java)

```java
@GetMapping("/api/v1/assignments/{assignmentId}/my-group-doc")
@GetMapping("/api/v1/group-docs/{id}")
@GetMapping(value = "/api/v1/group-docs/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PostMapping("/api/v1/group-docs/{id}/activity")
@PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:claim")
@PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:assign")
@PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:release")
@PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:done")
@GetMapping("/api/v1/group-docs/{id}/sections/{sectionId}/revisions")
@PostMapping(value = "/api/v1/group-docs/{id}/sections/{sectionId}/docx:preview", consumes = "multipart/form-data")
@PutMapping("/api/v1/group-docs/{id}/sections/{sectionId}/draft")
@PostMapping("/api/v1/group-docs/{id}/submit")
@GetMapping("/api/v1/group-docs/{id}/export.docx")
```

## [groupdocs/controller/GroupDocImageController.java](../../../backend/src/main/java/edu/aiplatform/groupdocs/controller/GroupDocImageController.java)

```java
@GetMapping("/api/v1/group-docs/{id}/blocks/{blockId}/image")
```

## [groups/controller/GroupController.java](../../../backend/src/main/java/edu/aiplatform/groups/controller/GroupController.java)

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

## [groups/controller/GroupProposalController.java](../../../backend/src/main/java/edu/aiplatform/groups/controller/GroupProposalController.java)

Thêm 2026-11-11 trên nhánh `feat/student-self-grouping` (sinh viên tự chia nhóm, US-GRP-007…010): 15 mapping.

```java
@GetMapping("/api/v1/classes/{classId}/group-formation")
@PutMapping("/api/v1/classes/{classId}/group-formation")
@GetMapping("/api/v1/classes/{classId}/group-proposals")
@PostMapping("/api/v1/classes/{classId}/group-proposals:approve")
@PostMapping("/api/v1/group-proposals/{proposalId}:reject")
@GetMapping("/api/v1/me/classes/{classId}/group-proposal")
@PostMapping("/api/v1/classes/{classId}/group-proposals")
@PutMapping("/api/v1/group-proposals/{proposalId}/name")
@PostMapping("/api/v1/group-proposals/{proposalId}/invitations")
@PostMapping("/api/v1/group-proposals/{proposalId}/invitations/{accountId}:revoke")
@PostMapping("/api/v1/group-proposals/{proposalId}:accept")
@PostMapping("/api/v1/group-proposals/{proposalId}:decline")
@PostMapping("/api/v1/group-proposals/{proposalId}:leave")
@PostMapping("/api/v1/group-proposals/{proposalId}:submit")
@PostMapping("/api/v1/group-proposals/{proposalId}:withdraw")
@PostMapping("/api/v1/group-proposals/{proposalId}:cancel")
```

## [identity/controller/AdminAccountController.java](../../../backend/src/main/java/edu/aiplatform/identity/controller/AdminAccountController.java)

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

## [identity/controller/AuthController.java](../../../backend/src/main/java/edu/aiplatform/identity/controller/AuthController.java)

```java
@RequestMapping("/api/v1/auth")
@PostMapping("/login")
@PostMapping("/refresh")
@PostMapping("/logout")
```

## [identity/controller/MeController.java](../../../backend/src/main/java/edu/aiplatform/identity/controller/MeController.java)

```java
@RequestMapping("/api/v1/me")
@GetMapping("/profile")
@PatchMapping("/profile")
@PostMapping("/password")
```

## [identity/controller/OtpFlowController.java](../../../backend/src/main/java/edu/aiplatform/identity/controller/OtpFlowController.java)

```java
@RequestMapping("/api/v1/auth")
@PostMapping("/activation/request")
@PostMapping("/activation/verify")
@PostMapping("/activation/complete")
@PostMapping("/password-reset/request")
@PostMapping("/password-reset/verify")
@PostMapping("/password-reset/complete")
```

## [materials/controller/ClassMaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/controller/ClassMaterialController.java)

```java
@RequestMapping("/api/v1/classes/{classId}")
@GetMapping("/materials")
@PostMapping("/materials")
@PutMapping("/materials:reorder")
```

## [materials/controller/MaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/controller/MaterialController.java)

```java
@RequestMapping("/api/v1/materials/{id}")
@GetMapping({"", "/view"})
@PatchMapping
@PostMapping("/versions")
@PostMapping("/archive")
@PostMapping("/download-token")
```

## [materials/controller/MaterialModuleController.java](../../../backend/src/main/java/edu/aiplatform/materials/controller/MaterialModuleController.java)

```java
@RequestMapping("/api/v1")
@GetMapping("/subjects/{subjectId}/modules")
@PostMapping("/subjects/{subjectId}/modules")
@PutMapping("/subjects/{subjectId}/modules:reorder")
@GetMapping("/classes/{classId}/modules")
@GetMapping("/me/classes/{classId}/modules")
@PatchMapping("/material-modules/{id}")
@DeleteMapping("/material-modules/{id}")
@PostMapping("/material-modules/{id}/quizzes")
@DeleteMapping("/material-modules/{id}/quizzes/{quizId}")
```

## [materials/controller/MyMaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/controller/MyMaterialController.java)

```java
@RequestMapping("/api/v1/me/classes/{classId}/materials")
@GetMapping
```

## [materials/controller/SubjectMaterialController.java](../../../backend/src/main/java/edu/aiplatform/materials/controller/SubjectMaterialController.java)

```java
@RequestMapping("/api/v1/subjects/{subjectId}")
@GetMapping("/materials")
@PostMapping("/materials")
@PutMapping("/materials:reorder")
```

## [notifications/controller/NotificationController.java](../../../backend/src/main/java/edu/aiplatform/notifications/controller/NotificationController.java)

```java
@RequestMapping("/api/v1/me")
@GetMapping("/notifications")
@GetMapping("/notifications/unread-count")
@PostMapping("/notifications/{id}:read")
@PostMapping("/notifications:read-all")
@GetMapping(value = "/notifications/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
```

## [notifications/controller/NotificationPreferenceController.java](../../../backend/src/main/java/edu/aiplatform/notifications/controller/NotificationPreferenceController.java)

```java
@RequestMapping("/api/v1/me/notification-preferences")
@GetMapping
@PutMapping
```

## [quizpractice/controller/QuizPracticeController.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/controller/QuizPracticeController.java)

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

## [quizpractice/controller/TeacherQuizAttemptController.java](../../../backend/src/main/java/edu/aiplatform/quizpractice/controller/TeacherQuizAttemptController.java)

```java
@RequestMapping("/api/v1/classes/{classId}")
@GetMapping("/quizzes/{quizId}/attempts")
@PostMapping(value = "/quiz-attempts/{attemptId}/penalty:decide", consumes = "application/json")
@GetMapping("/quiz-attempts/{attemptId}")
```

## [quizzes/controller/ClassQuizController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/controller/ClassQuizController.java)

```java
@RequestMapping("/api/v1/classes/{classId}")
@GetMapping("/question-proposals")
@PostMapping(value = "/question-proposals", consumes = "application/json")
@PatchMapping(value = "/question-proposals/{id}", consumes = "application/json")
@DeleteMapping("/question-proposals/{id}")
@GetMapping("/quizzes")
@PostMapping(value = "/quizzes", consumes = "application/json")
@GetMapping("/quiz-copy-sources")
@PostMapping(value = "/quizzes:copy", consumes = "application/json")
```

## [quizzes/controller/QuestionController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/controller/QuestionController.java)

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
@PostMapping(value = "/questions/{id}/approve", consumes = "application/json")
@PostMapping(value = "/questions/{id}/reject", consumes = "application/json")
@PostMapping(value = "/subjects/{subjectId}/questions:activate", consumes = "application/json")
@PostMapping(value = "/subjects/{subjectId}/questions:approve", consumes = "application/json")
@PostMapping(value = "/subjects/{subjectId}/questions:reject", consumes = "application/json")
@GetMapping("/subjects/{subjectId}/questions:duplicates")
@PostMapping(value = "/questions/{id}/retire", consumes = "application/json")
```

## [quizzes/controller/QuizController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/controller/QuizController.java)

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
@PutMapping(value = "/draw-rules", consumes = "application/json")
@GetMapping("/bank-tags")
@PostMapping("/paper-preview")
@PostMapping(value = "/publish", consumes = "application/json")
@PostMapping(value = "/retire", consumes = "application/json")
@PostMapping(value = "/reopen", consumes = "application/json")
@PostMapping("/clone")
@PostMapping("/versions")
```

## [quizzes/controller/SubjectQuizController.java](../../../backend/src/main/java/edu/aiplatform/quizzes/controller/SubjectQuizController.java)

```java
@RequestMapping("/api/v1/subjects/{subjectId}/quizzes")
@GetMapping
@PostMapping(consumes = "application/json")
```

## [settings/controller/SettingsController.java](../../../backend/src/main/java/edu/aiplatform/settings/controller/SettingsController.java)

```java
@RequestMapping("/api/v1/admin/settings")
@GetMapping
@GetMapping("/{key}")
@PatchMapping("/{key}")
```

## [subjects/controller/SubjectAdminController.java](../../../backend/src/main/java/edu/aiplatform/subjects/controller/SubjectAdminController.java)

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
