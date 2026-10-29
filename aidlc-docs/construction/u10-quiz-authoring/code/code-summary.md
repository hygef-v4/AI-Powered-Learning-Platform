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

## Chạy thử end-to-end (2026-10-20, stack Docker local)

Chạy trên code `staging` `c7a4791` (cây code giống `quiz-practice` `cfc4cbf`), stack `docker-compose.yml` + `docker-compose.local.yml` qua Nginx `:8088`, Mailpit thật, thêm file override tạm (PostgreSQL ra cổng 15432 vì máy đã có PostgreSQL ở 5432; healthcheck RabbitMQ timeout 30 giây). Dữ liệu nền tạo bằng API: Admin kích hoạt qua OTP, Chủ nhiệm môn, giảng viên, 3 sinh viên, môn `E2E101`, lớp `OPEN` có 3 sinh viên. Thao tác U10 làm trên trình duyệt (khung trình duyệt của app), kiểm thêm bằng API và PostgreSQL.

| Bước | Kết quả |
|---|---|
| Chủ nhiệm môn vào `/manager/questions`, thêm câu một đáp án, lưu nháp, kích hoạt | Đạt |
| Nhập `question-import.csv` qua hộp thoại: xem trước 2/2 hợp lệ, nhập 2 câu ở trạng thái Nháp, kích hoạt từng câu | Đạt sau khi sửa lỗi 1; API `questions:import` với `dryRun=true` và thật cũng đạt |
| Giảng viên vào tab Đánh giá của lớp, tạo Quiz, chọn tay 3 câu, bốc ngẫu nhiên 3 câu ("Có 8 câu phù hợp", không trùng câu đã chọn) | Đạt; tổng điểm 7 |
| Cài đặt 10 phút, 3 lượt, hiện điểm, đáp án sau khi nộp, đảo câu và lựa chọn; Lưu, Duyệt, Phát hành | Đạt; settings lưu đúng |
| Phần Lượt làm trên Quiz Detail (mở từ lớp) | Đạt; hiện 3 sinh viên, lượt, điểm cao nhất |
| Tạo version mới từ Quiz đang mở, đổi cài đặt (1 phút, ẩn điểm, không hiện đáp án), Duyệt, Phát hành | Đạt; v1 `RETIRED`, v2 `OPEN`, lượt đang làm của v1 được U11 tự nộp `AUTO_RETIRED` (xem code-summary U11) |

Lỗi tìm thấy:

1. **Đã sửa (chưa commit)** — tệp mẫu không có trong image frontend. `frontend/Dockerfile` (Next standalone) chỉ copy `.next/standalone` và `.next/static`, không copy `public/`; U10 là unit đầu tiên có tệp trong `public/`. Tái hiện: build image frontend, `curl -i http://localhost:8088/templates/question-import.csv` → `404` trang HTML 8.713 byte; "Tải tệp mẫu" tải về HTML; nhập tệp mẫu qua UI báo "Không đọc được tệp CSV". Sửa: thêm `COPY --from=build --chown=app:app /app/public ./public`; build lại thì `200 text/csv`, nhập qua UI đạt. Đây là file dùng chung, cần báo người giữ frontend/infra.
2. Nhỏ, chưa sửa: các công tắc ở `QuizSettingsPanel` (Hiện điểm, Đảo thứ tự câu, Đảo thứ tự lựa chọn, Ghi nhận hoạt động) không có tên truy cập vì `Switch` đặt nhãn ngoài nút và không truyền `ariaLabel`.

## Giai đoạn 4 — AI soạn câu, tool MCP, tài liệu tham khảo (2026-10-22)

Nhánh `quiz-ai-mcp` tách từ `staging` `08a6134`, theo plan giai đoạn 4 (H1–H10). Chạy trên contract và stub U06 của `staging`; U13 dùng code thật (`ReferenceMaterialService`).

### Business logic

| Luồng | Class chính |
|---|---|
| F4, F8 Gửi yêu cầu | `QuizAiDraftService.requestForBank`, `.requestForQuiz`. Hai bước (H2): ghi đề xuất `PENDING` trong transaction, gọi `IAiTask.submit` ngoài transaction với `targetRef = quiz-ai-draft:{draftId}`, rồi ghi `taskId`. U06 từ chối hoặc lỗi thì đề xuất `FAILED` kèm mã. Trùng `requestRef` thì trả đề xuất cũ; dùng cho đích khác thì `409 AI_REQUEST_REF_REUSED` |
| Prompt | `QuizDraftPrompt`: schema JSON 2020-12 (`maxItems` = số câu), `maxOutputTokens = min(16000, 600 × số câu)`, chữ tài liệu ≤ 60.000 ký tự trong `<material>` ghi rõ là dữ liệu không tin cậy; đọc kết quả, đáp án là số thứ tự từ 1 đổi thành mã lựa chọn |
| Nhận kết quả (H6) | `QuizDraftResultHandler` (cài `IAiTaskResultHandler`, `QUIZ_QUESTION_DRAFT`): kiểm lại quyền bằng `QuizSupport.canManage` / `QuizAccess.allows` (không ghi `ACCESS_DENIED`), Quiz còn sửa được, môn còn `ACTIVE`; câu sai quy tắc vẫn lưu kèm `violations` |
| Xem | `listForQuiz`, `listForBank`, `getForQuiz`, `getForBank`; đề xuất `PENDING` có `taskId` thì hỏi lại `IAiTask.get` (lỗi về trước khi ghi `taskId`) |
| Chấp nhận (H4) | `acceptForQuiz`: câu chọn thành câu riêng ở cuối Quiz, ≤ 200 câu, `version` +1 một lần; `acceptForBank`: câu `DRAFT` của ngân hàng. Lỗi theo câu `422 AI_DRAFT_INVALID` (`questions[i].field`); audit `QUIZ_AI_DRAFT_ACCEPTED` (số câu, không có nội dung) |
| Bỏ | `discardForQuiz`, `discardForBank`; đề xuất đang chờ thì thử `IAiTask.cancel` |
| F13 MCP (H7) | `QuizMcpTools` (cài `IMcpToolProvider`): `read_quiz_context` (`READ`) trả Quiz Detail kèm đáp án; `save_quiz_draft` (`WRITE`) cần Quiz `DRAFT` và `expectedVersion` khớp, lưu đề xuất `READY` nguồn `MCP`. Role `TEACHER`, `SUBJECT_MANAGER`; người học `403 MCP_TOOL_FORBIDDEN`; ngoài phạm vi `404` |
| Tài liệu (H8) | `QuizReferenceMaterials`: danh sách chọn được theo lớp hoặc môn của Quiz; `updateDetails` chỉ kiểm id mới gắn (U13 lỗi mà không gắn thêm thì vẫn lưu được) |

Domain: `QuizAiDraft`, `DraftQuestion`, `DraftTarget`, `DraftOrigin`, `DraftStatus`, `IQuizAiDraftRepository`. Hạ tầng: `JdbcQuizAiDraftRepository` (đổi trạng thái có điều kiện; tìm `requestRef` trong `provenance`). Không thêm migration: bảng `quiz_ai_drafts` đã có từ giai đoạn 1; mã lỗi của đề xuất lưu trong `provenance` (`failureCode`, `failureMessage`) vì bảng không có cột riêng.

### API

| Endpoint | Ghi chú |
|---|---|
| `GET /api/v1/quizzes/{id}/reference-materials`, `GET /api/v1/subjects/{subjectId}/reference-materials` | Tài liệu chọn được (`materialId`, `title`, `hasText`) |
| `GET`, `POST /api/v1/quizzes/{id}/ai-drafts` | Danh sách đề xuất chưa chốt; gửi yêu cầu (`instructions`, `count`, `type`, `difficulty`, `materialIds`, `source`, `provider`, `requestRef`, `version`) → `201` |
| `GET .../ai-drafts/{draftId}`, `POST .../{draftId}:accept` (`questions`, `version`), `POST .../{draftId}:discard` | Chấp nhận trả Quiz Detail |
| `GET`, `POST /api/v1/subjects/{subjectId}/question-ai-drafts`, `GET .../{draftId}`, `POST .../{draftId}:accept` (`questions`), `:discard` | H3; chấp nhận trả đề xuất và id các câu nháp mới |

Mã lỗi mới: `AI_DRAFT_NOT_FOUND`, `AI_DRAFT_CLOSED`, `AI_DRAFT_NOT_READY`, `AI_DRAFT_SELECTION_INVALID`, `AI_DRAFT_INVALID` (422), `AI_DRAFT_REQUEST_INVALID`, `AI_REQUEST_REF_REUSED`, `MATERIAL_NOT_SELECTABLE`, `MATERIAL_SERVICE_UNAVAILABLE`, `INVALID_MCP_ARGUMENTS`, `MCP_TOOL_FORBIDDEN`.

### Frontend

- `AiDrafts.tsx`:
  - `AiSourceSelect`: nguồn AI theo H1, cùng dạng `SourceChoice` của U06.
  - `AiDraftDialog`: form yêu cầu; chờ AI, hỏi lại mỗi 3 giây; danh sách câu sửa / bỏ / chấp nhận từng câu; "Lưu n câu đã chấp nhận".
- `AiDraftTargets.tsx`:
  - `QuestionAiDraftDialog`: nút "Tạo nháp bằng AI" trên Question List.
  - `QuizAiDraftPanel`: phần "Đề xuất AI" trên Quiz Detail, gồm cả đề xuất từ agent MCP.
  - `MaterialRefPicker`: trong `QuizSettingsPanel`, tối đa 10, giữ cả tài liệu không còn truy cập được.
- `SettingsForm` thêm `materialIds`; `settingsInput(form)` bỏ tham số thứ hai.

### Unit test

- Backend:
  - `QuizAiDraftServiceTest`: 17 test cho H2–H8 và MCP.
  - `QuizDraftPromptTest`: 3 test.
  - `JdbcQuizRepositoryIntegrationTest`: thêm 2 test repository đề xuất.
  - MockMvc: định tuyến `:accept`, `:discard`, đọc yêu cầu AI, tài liệu.
- Frontend:
  - `AiDrafts.test.tsx`: 5 test (gửi yêu cầu, chấp nhận từng câu đã sửa, lỗi, `409` từ Quiz Detail, giới hạn tài liệu).

### Kiểm (2026-10-22)

- Backend `mvn -B verify` (JDK 17, Testcontainers):
  - 930 test, 31 bỏ qua (`SubjectRepositoryIntegrationTest`).
  - 4 lỗi, đều ở bước Flyway: `AccountRepositoryIntegrationTest`, `JdbcQuizAttemptRepositoryIntegrationTest`, `JdbcQuestionRepositoryIntegrationTest`, `JdbcQuizRepositoryIntegrationTest`.
- Nguyên nhân 4 lỗi: `grading/V20260925_1500__create_grading_tables.sql` (U09, đã có trên `staging`) tham chiếu bảng `assignments` của U07. U07 vẫn ở nhánh `assignment`, chưa merge, nên `staging` không dựng được DB mới. Lỗi có sẵn, không do nhánh này.
- Bỏ riêng migration `grading` trong `target/` thì 17 test tích hợp quiz (gồm 2 test mới) đều qua.
- Frontend: lint, typecheck, 331 test, `next build` đều đạt.

### Khác với thiết kế

- `AiSourcePicker` của U06 chưa có trên `staging`, nên dùng `AiSourceSelect` riêng (H1). Khi U06 merge thì thay trong `AiDrafts.tsx`.
- Chưa chạy với AI thật: stub U06 trả `FAILED` ngay, nên trên `staging` mọi yêu cầu AI kết thúc ở "AI chưa sẵn sàng". Luồng chấp nhận chạy thật được qua đề xuất từ MCP khi U06 có MCP server.
- Nút "Tạo nháp bằng AI" của Quiz nằm ở phần "Đề xuất AI" thay vì cạnh "Thêm câu", để danh sách đề xuất và popup ở cùng một chỗ.

## Còn lại

- Integration test phân quyền 4 role trên endpoint do tester viết, gồm hai trường hợp AI/MCP mới trong plan.
- Chạy AI thật khi U06 (`ai-connections`) merge; thay `AiSourceSelect` bằng `AiSourcePicker`.

## Sửa sau chạy thử (2026-10-22)

- **L2 (J2):** bốn công tắc của `QuizSettingsPanel` (Hiện điểm sau khi nộp, Đảo thứ tự câu, Đảo thứ tự lựa chọn, Ghi nhận hoạt động) truyền `ariaLabel`. Không sửa `Switch` dùng chung.
- **Ghi chú cho người giữ UI kit:** `Switch` không có tên truy cập khi chỗ gọi không truyền `ariaLabel`, vì nhãn nằm ngoài nút. Còn 2 chỗ dùng ở unit khác.
- **Test:** `QuizScreens.test.tsx` thêm kiểm tên bốn công tắc.

## E3 — Bật ghi nhận hoạt động (2026-10-22)

Làm cùng G3 của U11 (plan U11 giai đoạn 6, K1–K3).

- **Kiểm thời hạn lưu (K1):** `QuizActivityPolicy` đọc `ISettings` khóa `activity.retentionDays` (U08 khai báo). U03 lỗi thì coi như chưa đặt.
- **Bật và tắt (K1, K2):**
  - `updateDetails`: bật mới khi chưa đặt thời hạn thì `409 ACTIVITY_RETENTION_NOT_SET`; bật mà thiếu lời báo thì `ACTIVITY_NOTICE_REQUIRED` theo trường. Tắt luôn được.
  - Duyệt (`QuizRules.validateActivity` trong `QuizPublicationService`) kiểm lại lời báo và thời hạn.
- **Cờ cho frontend (K3):** `QuizDetail.activityRetentionConfigured`, chỉ `true` với người có quyền sửa. `QuizSettingsPanel` mở công tắc khi đã đặt thời hạn (hoặc đang bật) và hiện ô "Lời báo cho sinh viên" bắt buộc; chưa đặt thì giữ ghi chú.
- **Test:**
  - `QuizServicesTest`: chưa đặt thời hạn; thiếu lời báo; bật được; duyệt khi thời hạn bị xóa; tắt khi U03 lỗi; bật khi U03 lỗi.
  - `QuizScreens.test.tsx`: công tắc khóa hoặc mở theo cờ; thiếu lời báo thì không gửi.
- **Kiểm:** backend `mvn -B verify` (JDK 17) 1112 test, 0 lỗi, 31 bỏ qua (`SubjectRepositoryIntegrationTest`); frontend lint, typecheck, 411 test, build đều đạt. Chưa chạy thử Docker vì U14 chưa sửa bean `notificationTopology`.

## Chạy thử end-to-end giai đoạn 4 và E3 (2026-10-22, stack Docker local)

Chạy trên `staging` `20c1458` (sau PR #8). Stack `docker-compose.yml` + `docker-compose.local.yml` dựng từ mã nguồn trên volume mới (project `quiz-e2e`, override tạm đẩy PostgreSQL sang cổng 15432), Nginx `:8088`, Mailpit thật.

Dữ liệu nền tạo bằng API:
- Admin kích hoạt bằng OTP qua Mailpit; Chủ nhiệm môn, giảng viên, 2 sinh viên.
- Môn `QZE2E`, lớp `QZE2E-01` ở trạng thái `OPEN`, có 2 sinh viên.
- 2 học liệu link YouTube: một của môn, một của lớp.
- 3 câu ngân hàng đã kích hoạt; Quiz của lớp có 3 câu đó.

Các thao tác kiểm thì làm trên trình duyệt (khung trình duyệt của app), kiểm thêm bằng API và PostgreSQL. Stack đã gỡ cùng volume sau khi chạy.

| Bước | Kết quả |
|---|---|
| Chủ nhiệm môn mở "Tạo nháp bằng AI" trên Question List | Đạt. Học liệu môn hiện nhưng bị khóa ("chưa trích được chữ"), vì U13 trả `hasText: false` cho link YouTube |
| Gửi yêu cầu AI, nguồn "API của trường" | Đạt theo thiết kế: U06 trên `staging` là stub nên đề xuất `FAILED` "AI chưa sẵn sàng", có nút "Yêu cầu mới" |
| Giảng viên mở Quiz Detail: "Gắn với học liệu" liệt kê học liệu môn và lớp; chọn học liệu lớp, Lưu | Đạt; `referenceMaterialIds` có học liệu lớp, version 1 → 2 |
| Gắn id học liệu không thuộc lớp qua API | Đạt; `400 MATERIAL_NOT_SELECTABLE` |
| Đề xuất nguồn MCP (`READY`, agent "Claude Desktop", 2 câu) ghi thẳng vào `quiz_ai_drafts`, vì MCP server của U06 chưa có trên `staging` | Phần "Đề xuất AI" hiện "Chờ chấp nhận · Agent: Claude Desktop · 2 câu" |
| Mở đề xuất, chấp nhận câu 1, bỏ câu 2, "Lưu 1 câu đã chấp nhận" | Đạt; câu thành câu riêng ở vị trí 4, version 2 → 3, đề xuất `ACCEPTED`, audit `QUIZ_AI_DRAFT_ACCEPTED` (`count` 1, `origin` MCP, không có nội dung câu) |
| Chưa đặt `activity.retentionDays`: công tắc "Ghi nhận hoạt động" khóa, có ghi chú | Đạt |
| Admin đặt `activity.retentionDays` = 30 (API cài đặt); tải lại: công tắc mở được | Đạt |
| Bật mà không có lời báo, bấm Lưu | Lỗi theo trường "Cần lời báo cho sinh viên…", không gửi request. Banner phía trên lại báo "Hệ thống tạm bận" (lỗi 1 bên dưới) |
| Nhập lời báo, Lưu; Duyệt; Phát hành | Đạt; Quiz `OPEN`, bật ghi nhận hoạt động, 4 câu, 1 học liệu |

Lỗi phát hiện:

1. **Đã sửa (2026-10-22, nhánh `fix/quiz-detail-client-validation-banner`)** — `QuizDetailPage` báo "Hệ thống tạm bận, vui lòng thử lại sau." khi form không hợp lệ phía trình duyệt.
   - Nguyên nhân: `save` ném `Error` thường ("Cấu hình chưa hợp lệ.", "Điểm mỗi câu phải lớn hơn 0…"), còn `errorMessage` chỉ đọc `ApiError` nên biến mọi lỗi khác thành câu "hệ thống tạm bận". Có từ giai đoạn 2; lỗi theo trường vẫn hiện đúng, chỉ banner sai.
   - Sửa: `save` kiểm cấu hình và điểm trước khi gọi API, sai thì đặt thẳng banner với đúng thông điệp. Trước đây cấu hình hợp lệ mà điểm sai thì cấu hình đã lưu nhưng màn hình giữ `version` cũ, lần lưu sau dính `409`; nay không gọi API nào. `run`, `errorMessage` và cách xử lý `ApiError` (`409 QUIZ_VERSION_CONFLICT` tải lại, `violations`) giữ nguyên.
   - Test: `QuizScreens.test.tsx` thêm "điểm sai thì báo đúng lỗi…"; test E3 kiểm thêm banner "Cấu hình chưa hợp lệ." và không có "Hệ thống tạm bận". Hai test thất bại trên code cũ. Frontend lint, typecheck, 438 test, build đạt.
2. Quan sát: U13 trả `hasText: false` cho link YouTube dù `extractText` có trả tiêu đề, mô tả, URL. Vì vậy popup AI chỉ chọn được học liệu dạng tệp đã trích chữ. Chưa thử với tệp tải lên.

Chưa kiểm (lúc đó):
- Tool MCP qua MCP server thật: đã chạy ngày 2026-10-23, xem mục bên dưới.
- AI soạn câu thật: U06 vẫn là stub.

## Chạy thử MCP thật và AiSourcePicker (giai đoạn 7, bước 7.4, 2026-10-23)

Chạy trên `staging` `eb0cf1a` (sau PR #14). Stack `docker-compose.yml` + `docker-compose.local.yml` dựng từ mã nguồn trên volume mới (project `mcp-e2e`, override tạm đổi cổng: Nginx `:18088`, Mailpit `:18025`, vì stack `platform` khác đang giữ `:8088`). Env tạm có secret ngẫu nhiên; stack, volume và file tạm đã gỡ sau khi chạy.

Dữ liệu tạo bằng API: Admin kích hoạt bằng OTP qua Mailpit; Chủ nhiệm môn, 2 giảng viên, 1 sinh viên; môn `MCPE2E`; 2 lớp `OPEN` (mỗi lớp một giảng viên, cùng sinh viên); 1 Quiz nháp của lớp 1.

**M5 — tool MCP qua `POST /mcp` (JSON-RPC, script, 16/16 đạt):**

| Kiểm | Kết quả |
|---|---|
| Giảng viên tạo credential `READ_WRITE` và `READ_ONLY` (`POST /api/v1/me/mcp-credentials`) | `201`, có `token`, `endpoint` `/mcp` |
| `initialize` | `protocolVersion` `2025-06-18` |
| `tools/list` với `READ_WRITE` | Có `read_quiz_context`, `save_quiz_draft` |
| `tools/list` với `READ_ONLY` | Có `read_quiz_context`, không có `save_quiz_draft` |
| `read_quiz_context` | Trả Quiz và `version` |
| `save_quiz_draft` (2 câu: một đáp án, nhiều đáp án; `agentLabel` "Claude Desktop") | Đề xuất `READY`, 2 câu, 0 câu lỗi |
| `save_quiz_draft` với `expectedVersion` sai | `isError`, audit `FAILURE` `QUIZ_VERSION_CONFLICT` |
| Credential `READ_ONLY` gọi `save_quiz_draft` | Bị từ chối, audit `ACCESS_DENIED` |
| Tham số thừa (`extra`) | U06 trả `INVALID_PARAMS` theo schema |
| Giảng viên lớp khác gọi `read_quiz_context` | "Không tìm thấy hoặc không có quyền", audit `ACCESS_DENIED` |
| Sinh viên: `tools/list`; gọi `read_quiz_context` | Không thấy tool U10; gọi bị từ chối, audit `ACCESS_DENIED` |
| Request có header `Origin` | `403` |
| Token sai | `401` |
| `GET /api/v1/quizzes/{id}/ai-drafts` | Có đề xuất `origin` MCP, agent "Claude Desktop", `taskId` trống |

**Trên trình duyệt (giảng viên):**

- Quiz Detail hiện "Chờ chấp nhận · Agent: Claude Desktop · 2 câu". Mở, chấp nhận cả 2 câu, "Lưu 2 câu đã chấp nhận": 2 câu thành câu riêng ở cuối Quiz, tổng điểm 2; audit `QUIZ_AI_DRAFT_ACCEPTED` (`count` 2, `origin` MCP, không có nội dung câu).
- **M6:** "Tạo nháp bằng AI" → điền yêu cầu, 3 câu → "Gửi yêu cầu": popup U10 thay bằng popup "Chọn nguồn AI" của U06, dòng tác vụ "Sinh câu hỏi · Quiz chạy thử MCP". Cả 4 nguồn hiện mờ kèm lý do ("API của trường chưa được bật"; "Chưa lưu khóa API cho nhà cung cấp này"), nút "Gửi yêu cầu" khóa. Bấm "Hủy" thì quay lại form, giữ yêu cầu và số câu đã nhập.
- Log backend, worker không có `ERROR` hay exception.

Quan sát (U06, không phải lỗi U10): lời gọi tool sai schema bị trả `INVALID_PARAMS` mà không ghi audit; lời gọi bị từ chối quyền ghi `ACCESS_DENIED` chứ không phải `MCP_TOOL_CALLED`.

Chưa kiểm: AI soạn câu bằng khóa thật (cần người dùng nhập khóa ở trang Kết nối AI hoặc Admin bật API của trường); ước tính token trên popup (chỉ hiện khi có nguồn dùng được); `AiTaskStatus` với task thật.

## Thay đổi 2026-10-24

- Thời hạn giữ dữ liệu hoạt động cố định 1 ngày (U08), không còn cài đặt `activity.retentionDays`: bỏ `QuizActivityPolicy` và trường `activityRetentionConfigured`, nên lỗi `ACTIVITY_RETENTION_NOT_SET` không còn xảy ra; bỏ `FakeSettings` trong test.
