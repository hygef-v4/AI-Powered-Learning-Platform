# U10 Quiz Authoring — code summary (giai đoạn 1: ngân hàng câu hỏi; giai đoạn 2: Quiz)

Code viết ngày 2026-10-20 trên nhánh `quiz-authoring` (tách từ `staging` `6341aed`) theo [plan giai đoạn 1](../../plans/code-generation-plan/u10-quiz-authoring-code-generation-plan.md). Phạm vi: UC 46 View Subject Question Bank (KAN-143), UC 47 Create/Update/Delete Subject Question (KAN-78); luồng F1, F2, F3. Chưa gồm F4 (AI soạn câu, giai đoạn 4) và phần Quiz (UC 48, giai đoạn 2). Chưa commit.

## Business logic

| Luồng | Class chính |
|---|---|
| F1 Xem ngân hàng | `QuestionBankService.list`, `.detail` (bản đã kích hoạt mới nhất mỗi lineage + cờ có bản nháp, lịch sử phiên bản, số Quiz đang ghim) |
| F2 Tạo, sửa, kích hoạt, ngưng, xóa | `QuestionBankService.create`, `.update` (chỉ bản nháp, kèm `version`), `.openDraft` (sửa câu đang dùng → bản nháp mới cùng lineage, đã có thì trả bản đó), `.activate`, `.retire`, `.delete` (chỉ nháp chưa từng kích hoạt) |
| F3 Nhập tệp | `QuestionImporter` (`.xlsx` qua Apache POI, `.csv` UTF-8 qua Commons CSV; nhận loại theo nội dung; ≤ 5 MB, ≤ 500 dòng; ô công thức giữ là văn bản; `dryRun` chỉ kiểm) |
| Quyền, audit | `QuizAccess` (R2 qua `IAuthorization`, ngoài phạm vi `404` + `ACCESS_DENIED`, U01 lỗi → `503`), `ActiveSubjects` (môn lưu trữ chỉ xem, U04 lỗi → `503`), `QuizAudit` (chỉ id, số đếm; không nội dung câu) |

Domain: `Question` (phiên bản, chuyển trạng thái), `QuestionContent` (chuẩn hóa), `QuestionRules` (BR-U10-02, 03), `QuestionOption`, `Violation`, enum `QuestionStatus`, `QuestionScope`, `Difficulty`; `IQuestionRepository`. Hạ tầng: `JdbcQuestionRepository` (`JdbcClient`, `jsonb` cho lựa chọn, đáp án, thẻ).

## API

| Endpoint | UC | Ghi chú |
|---|---|---|
| `GET /api/v1/subjects/{subjectId}/questions` | 46 | Lọc `type`, `difficulty`, `status`, `q` (nội dung hoặc thẻ), `page`, `size` ≤ 100; tham số lạ → `400` |
| `POST /api/v1/subjects/{subjectId}/questions` | 47 | `201` + `Location`; nội dung sai → `400 QUESTION_INVALID` kèm `violations` |
| `POST /api/v1/subjects/{subjectId}/questions:import` | 47 | Multipart `file`, `dryRun=true` chỉ kiểm |
| `GET`, `PATCH`, `DELETE /api/v1/questions/{id}` | 46, 47 | `PATCH` kèm `version`; `DELETE ?version=`; lệch → `409 QUESTION_VERSION_CONFLICT` |
| `POST /api/v1/questions/{id}/versions` | 47 | Mở bản nháp mới (BR-U10-04) |
| `POST /api/v1/questions/{id}/activate`, `/retire` | 47 | Body `{version}` |

Mã lỗi khác: `QUESTION_NOT_FOUND`, `SUBJECT_NOT_FOUND`, `SUBJECT_NOT_ACTIVE`, `QUESTION_NOT_EDITABLE`, `QUESTION_NOT_DRAFT`, `QUESTION_NOT_ACTIVE`, `QUESTION_DELETE_BLOCKED`, `IMPORT_*`, `QUIZ_AUTHORIZATION_UNAVAILABLE`, `SUBJECT_UNAVAILABLE`, `SYSTEM_BUSY`.

## Migration

`db/migration/quizzes/V20260925_1600__create_quiz_tables.sql` tạo cả 4 bảng của U10 (D7): `quizzes`, `questions`, `quiz_items`, `quiz_ai_drafts` với check, unique `(lineage_id, version_no)`, unique riêng phần "một nháp mỗi lineage ngân hàng" và "một version mới đang soạn mỗi Quiz nguồn", index theo infrastructure-design, FK sang `subjects`, `course_classes`, `accounts`. User `app` có `SELECT, INSERT, UPDATE, DELETE` (điều kiện xóa kiểm trong code).

## Frontend

- Route: `app/manager/questions` (Question List), `app/manager/questions/[id]` (Question Detail; `new?subjectId=` để thêm câu). Menu "Ngân hàng câu hỏi" đã đăng ký sẵn trong `features/quizzes/register.ts`.
- `features/quizzes`: `api.ts`, `types.ts`, `labels.ts`, `questionForm.ts` (trạng thái form và kiểm theo BR-U10-02, 03), `McqEditor`, `QuestionSidePanels` (`QuestionMetaPanel`, `QuestionVersionPanel`), `QuestionListPage`, `QuestionImportDialog`, `QuestionDetailPage`.
- Tệp mẫu: `public/templates/question-import.csv` (UTF-8 có BOM).
- Chọn môn qua `GET /api/v1/manager/subjects` của U05. Nội dung hiển thị dạng văn bản thuần (D5).

## Unit test

- Backend: `QuestionRulesTest` 7, `QuestionTest` 3, `QuestionBankServiceTest` 13, `QuestionImporterTest` 7, `QuestionControllerTest` 7, `JdbcQuestionRepositoryIntegrationTest` 6 (PostgreSQL 16 qua Testcontainers, chạy migration thật), thêm 1 test bảng quyền trong `AccountLookupAndPolicyTest`.
- Frontend: `questionForm.test.ts`, `McqEditor.test.tsx`, `QuestionScreens.test.tsx` (20 test).
- Chạy đủ bước CI ngày 2026-10-20: backend `mvn -B verify` trên `maven:3.9-eclipse-temurin-17` 720 test, 0 lỗi, 31 bỏ qua (`SubjectRepositoryIntegrationTest` của U04 cần `U04_TEST_JDBC_URL`); frontend `npm ci`, lint, typecheck, 234 test, build đều đạt.

## Thay đổi file dùng chung

- `backend/pom.xml`: thêm `poi-ooxml` 5.4.1 (D2).
- `identity/contract/Actions.java`, `identity/application/AccessPolicy.java`: `QUESTION_BANK_VIEW`, `QUESTION_BANK_MANAGE` (Subject Manager, phạm vi môn) (D4).

## Khác với thiết kế

- Lưu trữ bằng `JdbcClient` thay vì JPA (D1).
- Nhập tệp dùng một endpoint kèm `dryRun` cho bước xem trước (D3).
- Không tạo `contracts/openapi/quizzes.yaml`; API ghi ở đây (D6).
- Hiển thị văn bản thuần, chưa render markdown (D5).
- Kích hoạt bản nháp mới không đổi trạng thái bản `ACTIVE` cũ (Quiz đang ghim vẫn dùng); danh sách và ô chọn câu lấy bản đã kích hoạt mới nhất mỗi lineage.
- Mọi thao tác ghi bị chặn khi môn đã lưu trữ (`409 SUBJECT_NOT_ACTIVE`), kể cả ngưng và xóa.
- Nhập tệp: thẻ cách nhau bằng `;`; ô điểm trống lấy 1, ô độ khó trống lấy `MEDIUM`; nhận cả tên tiếng Việt ("Một đáp án", "Nhiều đáp án", "Dễ", "Trung bình", "Khó").

## Đối chiếu UC 46, 47 (bước 1.12)

| Rule | Code | Test |
|---|---|---|
| BR-U10-01 chỉ Chủ nhiệm môn của môn; Teacher, Student, Admin không vào | `AccessPolicy` (role `SUBJECT_MANAGER`, phạm vi môn), `QuizAccess` | `AccountLookupAndPolicyTest`, `QuestionBankServiceTest.outOfScopeIsNotFoundAndAuditedAsDenied` |
| BR-U10-02 `MCQ_SINGLE`/`MCQ_MULTI`, 2–6 lựa chọn, đáp án theo loại, không trùng | `QuestionRules`, check trong migration | `QuestionRulesTest`, `databaseRejectsInvalidQuestionsEvenIfCodeIsBypassed` |
| BR-U10-03 ≤ 20.000 ký tự, điểm > 0 tối đa 2 chữ số thập phân, độ khó, ≤ 10 thẻ | `QuestionRules`, `QuestionContent.of` | `QuestionRulesTest`, `questionForm.test.ts` |
| BR-U10-04 bản đã kích hoạt bất biến, sửa mở nháp mới, một nháp mỗi lineage | `Question.nextDraft`, `QuestionBankService.openDraft`, unique riêng phần | `QuestionTest`, `QuestionBankServiceTest`, `lineageKeepsAtMostOneDraftAndVersionsInOrder` |
| BR-U10-05 kích hoạt khi hợp lệ, ngưng, xóa chỉ nháp | `activate`, `retire`, `delete` | `QuestionBankServiceTest` |
| BR-U10-06 mỗi lineage một dòng + cờ nháp, lọc, lịch sử | `JdbcQuestionRepository.searchBank`, `QuestionVersionPanel` | `bankListShowsOneRowPerLineageWithDraftFlagAndFilters`, `QuestionScreens.test.tsx` |
| BR-U10-07 nhập `.xlsx`/`.csv`, ≤ 500 dòng, ≤ 5 MB, kết quả từng dòng | `QuestionImporter`, `QuestionImportDialog` | `QuestionImporterTest`, `QuestionScreens.test.tsx` |
| BR-U10-50 ngoài phạm vi `404` + audit, `version` lệch `409` | `QuizAccess`, `QuestionBankService` | `QuestionBankServiceTest`, `QuestionControllerTest` |
| BR-U10-51 audit `QUESTION_*`, `QUESTIONS_IMPORTED` | `QuizAudit` | `QuestionBankServiceTest`, `QuestionImporterTest` |
| BR-U10-52 U01/U04 lỗi thì từ chối | `QuizAccess`, `ActiveSubjects` | `authorizationOutageFailsClosedWithoutDeniedAudit`, `archivedSubjectIsReadOnlyAndSubjectOutageIsBusy` |
| NFR-U10-30 tệp nhập: loại theo nội dung, công thức là văn bản | `QuestionImporter` | `formulaLikeCellsStayPlainText`, `readsFirstSheetOfXlsxWithoutEvaluatingFormulas` |
| NFR-U10-32 không log nội dung câu | `QuizAudit` chỉ id/số đếm | `createStoresDraftAndAuditsWithoutContent` |

# Giai đoạn 2 — Quiz (UC 48)

Code viết ngày 2026-10-20 trên `quiz-authoring` sau commit giai đoạn 1 `5d70173`, theo [plan giai đoạn 2](../../plans/code-generation-plan/u10-quiz-authoring-code-generation-plan.md) (E1–E8). Phạm vi: UC 48 (KAN-44); F5, F6, F7, F9, F10, F11, F12, F14. Chưa gồm F8 (AI soạn câu cho Quiz, giai đoạn 4). Chưa commit.

## Business logic (giai đoạn 2)

| Luồng | Class chính |
|---|---|
| F5 Danh sách | `QuizAuthoringService.listSubject` (Quiz List của môn), `.listClass` (Quiz của lớp + Quiz của môn đã phát hành, chỉ xem) |
| F6 Tạo | `createForSubject`, `createForClass` (cài đặt mặc định, lớp/môn chưa lưu trữ, audit `QUIZ_CREATED`) |
| F7 Soạn | `updateDetails` (BR-U10-15; bật ghi nhận hoạt động bị chặn khi chưa có thời hạn lưu, E3), `saveItems` (câu ngân hàng `ACTIVE` cùng môn, câu đã ghim được giữ, câu riêng bị bỏ thì xóa), `randomPreview`, `bankQuestions` (E2), `addInline`, `updateInline`, `deleteInline`; mọi thay đổi kèm `version`, Quiz đã duyệt về nháp |
| F9 Duyệt, phát hành | `QuizPublicationService.review` (lỗi theo câu `items[n]`), `.publish` (lớp `OPEN` hoặc môn `ACTIVE`, kiểm lại câu, version cũ đang mở → `RETIRED` + `IQuizLifecycleListener` trong cùng transaction, phát `quiz.published` sau commit) |
| F10 Ngưng | `QuizPublicationService.retire` (lý do bắt buộc, gọi listener) |
| F11 Nhân bản, version, copy | `QuizCopyService.cloneQuiz`, `.newVersion` (chỉ từ Quiz đã phát hành; một version đang soạn mỗi chuỗi), `.copySources`, `.copyToClass` (dạy cả hai lớp, cùng môn); câu ngân hàng giữ phiên bản ghim, câu riêng sao mới |
| F12 Xóa | `QuizAuthoringService.delete` (chưa từng phát hành; xóa kèm câu, câu riêng, đề xuất AI) |
| F14 Interface | `QuizQueryService` (cài `IQuizQuery`, đề cho người học không có đáp án), `QuizScorer` (cài `IQuizScoring`, đúng đủ tập đáp án mới có điểm) |
| Dùng chung | `QuizSupport` (tải, quyền sửa/xem, lớp/môn còn hoạt động, dựng Quiz Detail, lưu có version), `QuizAccess.requireClass`, `.allows`, `RuleViolationException` |

Domain: `Quiz`, `QuizItem`, `QuizOrigin`, `QuizRules`, `IQuizRepository`; `Question.newInline`, `.copyInline`; `IQuestionRepository` thêm câu riêng và chọn/bốc câu `ACTIVE`. Hạ tầng: `JdbcQuizRepository` (cài đặt, học liệu dạng `jsonb`; chuỗi version bằng truy vấn đệ quy).

## API (giai đoạn 2)

| Endpoint | Ghi chú |
|---|---|
| `GET`, `POST /api/v1/subjects/{subjectId}/quizzes` | Quiz của môn; lọc `status`, `q`, `page`, `size` |
| `GET`, `POST /api/v1/classes/{classId}/quizzes`; `GET .../quiz-copy-sources`; `POST .../quizzes:copy` | Quiz của lớp, copy từ lớp khác |
| `GET /api/v1/quizzes/{id}?classId=` | `canManage` sai khi giảng viên xem Quiz của môn từ lớp |
| `PATCH`, `DELETE /api/v1/quizzes/{id}` | Thông tin, cài đặt kèm `version`; xóa `?version=` |
| `PUT .../items`, `POST .../items:random-preview`, `GET .../bank-questions` | Danh sách câu, bốc thử, chọn tay (E2) |
| `POST .../inline-questions`, `PATCH`, `DELETE .../inline-questions/{questionId}` | Câu riêng |
| `POST .../review`, `/publish`, `/retire`, `/clone`, `/versions` | Vòng đời, nhân bản, version mới |

Mã lỗi mới: `QUIZ_NOT_FOUND`, `QUIZ_INVALID`, `QUIZ_REVIEW_FAILED` (kèm `violations`), `QUIZ_VERSION_CONFLICT`, `QUIZ_NOT_EDITABLE`, `QUIZ_NOT_DRAFT`, `QUIZ_NOT_REVIEWED`, `QUIZ_NOT_OPEN`, `QUIZ_DELETE_BLOCKED`, `QUIZ_NOT_PUBLISHED`, `QUIZ_VERSION_EXISTS`, `QUIZ_FULL`, `QUIZ_RANDOM_COUNT_INVALID`, `QUIZ_COPY_SUBJECT_MISMATCH`, `CLASS_NOT_OPEN`, `CLASS_ARCHIVED`, `ACTIVITY_RETENTION_NOT_SET`, `QUIZ_REQUEST_INVALID`.

## Frontend (giai đoạn 2)

- Route: `app/manager/quizzes` (Quiz List), `app/quizzes/[id]` + `app/quizzes/layout.tsx` (Teacher, Subject Manager).
- `features/quizzes`: `SubjectQuizListPage`, `CreateQuizDialog`, `QuizDetailPage`, `QuizDetailParts` (`QuizItemList`, `InlineQuestionDialog`, `QuizSettingsPanel`, `QuizPreviewDialog`), `BankQuestionPicker`, `ClassQuizzesSection` (đăng ký `teacherClassTabs` tab `evals`), `QuizBankSummary` (đăng ký `subjectSummarySections`), slot mới `quizRowActions` cho U11.
- Ghi nhận hoạt động hiện công tắc tắt kèm ghi chú (E3); không có `MaterialRefPicker` (E4).

## Unit test (giai đoạn 2)

- Backend: `QuizTest` 3, `QuizServicesTest` 17 (repository giả trong bộ nhớ), `QuizControllersTest` 5, `JdbcQuizRepositoryIntegrationTest` 5 (PostgreSQL 16 qua Testcontainers), thêm 1 test bảng quyền.
- Frontend: `QuizScreens.test.tsx` 9.
- Chạy đủ bước CI ngày 2026-10-20: backend `mvn -B verify` trên JDK 17 751 test, 0 lỗi, 31 bỏ qua (U04); frontend `npm ci`, lint, typecheck, 243 test, build đều đạt.

## Thay đổi file dùng chung (giai đoạn 2)

- `identity/contract/Actions.java`, `identity/application/AccessPolicy.java`: `QUIZ_SUBJECT_MANAGE` (Subject Manager, phạm vi môn), `QUIZ_CLASS_MANAGE` (Teacher, Subject Manager, phạm vi lớp) (E1).

## Khác với thiết kế (giai đoạn 2)

- Chọn câu ngân hàng cho giảng viên qua `GET /api/v1/quizzes/{id}/bank-questions` thay vì API ngân hàng của Chủ nhiệm môn (E2).
- Tab Đánh giá hiện Quiz của môn ở trạng thái `OPEN` và `RETIRED` (chỉ xem); bản nháp của môn không hiện ở lớp.
- `QuizItemView` có cờ `inline` để frontend phân biệt câu riêng và câu ngân hàng.
- `QuizBankSummary` đếm bằng tổng của các API danh sách (5 lời gọi), không có API đếm riêng.
- Version mới lấy `version_no` lớn nhất của cả chuỗi + 1; phát hành ngưng version khác đang mở trong chuỗi.

## Đối chiếu UC 48 (bước 2.13)

| Rule | Code | Test |
|---|---|---|
| BR-U10-10 Quiz của lớp do giảng viên lớp, của môn do Chủ nhiệm môn; giảng viên chỉ xem Quiz của môn | `QuizSupport.requireManage`, `.requireView`, `AccessPolicy` | `teacherViewsPublishedSubjectQuizFromTheirClassButCannotEditIt`, `teacherOfAnotherClassGetsNotFoundAndIsAudited` |
| BR-U10-12 chỉ Practice | Không có lịch, sổ điểm; nhãn "Quiz · Practice" | — |
| BR-U10-13 1–200 câu, câu ngân hàng ghim phiên bản hoặc câu riêng, điểm > 0 | `saveItems`, `addInline`, `QuizRules` | `savingItemsPinsActiveBankVersions…`, `itemsRejectDuplicatesAndBadPoints`, `inlineQuestionsAre…` |
| BR-U10-14 chọn tay, bốc ngẫu nhiên không trùng, không vượt chỗ trống | `randomPreview`, `bankQuestions`, `searchActive`, `randomActive` | `randomPreviewSkips…`, `activePicksSkipExistingLineages…` |
| BR-U10-15 cài đặt; theo dõi hoạt động cần thời hạn lưu | `QuizRules.validateSettings`, `updateDetails` | `settingsAreValidatedAndActivityMonitoring…`, `settingsInput` |
| BR-U10-16 chấm theo đáp án ghim, `MCQ_MULTI` đúng đủ | `QuizScorer` | `scoringNeedsExactAnswerSetAndStudentViewHidesAnswers` |
| BR-U10-20 vòng đời, sửa Quiz đã duyệt về nháp | `Quiz` | `QuizTest`, `savingItems…ReturnsReviewedQuizToDraft` |
| BR-U10-21 điều kiện duyệt theo câu | `QuizRules.validateForReview` | `reviewListsProblemsPerQuestion…` |
| BR-U10-22 phát hành, version cũ ngưng, `quiz.published` | `QuizPublicationService.publish` | `publishNeedsOpenClassThenOpensAndAnnounces`, `publishingNewVersionRetires…` |
| BR-U10-23 ngưng có lý do, listener trong transaction | `retire` | `retireNeedsReasonAndNotifiesListeners` |
| BR-U10-24 xóa Quiz chưa phát hành kèm câu riêng | `delete`, `JdbcQuizRepository.delete` | `deletingDraftRemovesItsInlineQuestions`, `deleteRemovesUnpublishedQuiz…` |
| BR-U10-25 người học thấy Quiz `OPEN` của lớp và của môn | `QuizQueryService.listForClass` | `studentsSeeOpenClassAndSubjectQuizzesOnly` |
| BR-U10-30, 31 nhân bản, version mới, copy | `QuizCopyService` | `cloneKeepsPinnedBankVersions…`, `copyBetweenClasses…`, `versionChainFindsOpenSibling…` |
| BR-U10-50 ngoài phạm vi `404` + audit, `409` khi lệch version | `QuizAccess`, `QuizSupport` | `QuizServicesTest`, `QuizControllersTest` |
| NFR-U10-31 đề cho người học không có đáp án | `QuizQueryService.getStudentView` | `scoringNeeds…StudentViewHidesAnswers` |

## Còn lại

- Chưa chạy end-to-end trên trình duyệt với backend thật; integration test phân quyền 4 role trên endpoint do tester viết.
- Giai đoạn 3 (U11) cài `IQuizLifecycleListener`, slot `quizDetailSections`, `quizRowActions`; giai đoạn 4 thêm AI soạn câu (F4, F8) và tool MCP.
