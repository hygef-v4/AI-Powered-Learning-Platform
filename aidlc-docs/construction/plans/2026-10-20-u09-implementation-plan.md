# U09 Assignment Grading and Gradebook Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Implementation was authorised by the user on 2026-10-20. See the execution status below; integration acceptance remains gated.

**Goal:** Triển khai UC42–45: xem bài nộp, chấm theo rubric bằng tay hoặc đề xuất AI, chốt/công bố điểm và xem/xuất sổ điểm.

**Architecture:** Giữ modular monolith, package `edu.aiplatform.grading` với `contract/application/domain/infrastructure/web`. U09 chỉ truy cập unit khác qua public contract; frontend đăng ký nội dung vào slot của U05/U07/U08. Làm luồng chấm tay đầy đủ trước, sau đó thêm AI như một nguồn đề xuất; dùng fixture trong test khi provider chưa triển khai.

**Tech Stack:** Java 17, Spring Boot 3.5.6, PostgreSQL/Flyway/JPA; Next.js 16.3.8, React 19.3.0, TypeScript 5.9.3; JUnit/Mockito/Testcontainers, Vitest/Testing Library. Commons CSV có sẵn; XLSX dùng writer ZIP/inline-string không thêm dependency; thay đổi so với POI trong thiết kế được ghi trong implementation-summary.

**Spec:**
- `aidlc-docs/inception/requirements/requirements.md`: FR-002/007/008/009/014/019/020/024/026/030/039, NFR/SEC liên quan.
- `aidlc-docs/inception/requirements/use-cases.md`, `docs/usecase_table.md`: UC42–45 và các luồng hỗ trợ UC37/38/40.
- `aidlc-docs/inception/user-stories/stories.md`: US-GRD-002–005, US-RPT-001/003, US-INT-001; giao tiếp US-GRP-006.
- `aidlc-docs/construction/u09-assignment-grading/functional-design/{business-rules,business-logic-model,domain-entities,frontend-components}.md`.
- `aidlc-docs/construction/u09-assignment-grading/nfr-requirements/{nfr-requirements,tech-stack-decisions}.md`.
- `aidlc-docs/construction/u09-assignment-grading/nfr-design/{logical-components,nfr-design-patterns}.md`.
- `aidlc-docs/construction/u09-assignment-grading/infrastructure-design/{infrastructure-design,deployment-architecture}.md`.
- Existing baseline: `aidlc-docs/construction/plans/code-generation-plan/u09-assignment-grading-code-generation-plan.md`.

## Execution status — 2026-10-20

Mã UC42–45 đã được triển khai. Chi tiết quyết định, test và integration gates: [implementation-summary](../u09-assignment-grading/code/implementation-summary.md). Các checkbox cũ bao gồm nhiều điều kiện nghiệm thu/commit trong một dòng; chỉ đánh dấu khi toàn bộ điều kiện của dòng đã có bằng chứng.

| Task | Đã triển khai | Chưa nghiệm thu |
|---|---|---|
| 1 | U01 policy, access guards, DTO strict version, OpenAPI | Owner review/real-provider contract acceptance |
| 2 | Aggregate, JPA, migration, listener transaction, resubmit | PostgreSQL constraints/rollback/locking (Docker), U07/U08 migrations |
| 3 | Submissions/detail/progress/activity, scoped images | UI/real U08 and U12 acceptance |
| 4 | Checklist, adjustment/history, version, batch, event | PostgreSQL concurrency, real event delivery |
| 5 | Manual workspace/routes/registration, conflict/next tests | Visual fidelity/keyboard in browser |
| 6 | Gradebook/student/export/distribution, Blob API helper | Real spreadsheet app and performance benchmarks |
| 7 | Proposal pipeline/schema/correlation/source checks | Real U06 provider/quota acceptance |
| 8 | MCP proposal-only tool and proposal UI | U06 credential/READ_WRITE connector integration |
| 9 | Group/member lifecycle, overrides, metadata/activity/authors | End-to-end U12 and concurrent group submit |
| 10 | Automated checks, independent code review, handoff summary | Full runtime/NFR, requirement and test-owner acceptance |

## Global Constraints

- Chỉ Assignment `GRADED`; không tạo điểm chính thức cho Quiz hoặc Assignment `PRACTICE`.
- Chỉ Teacher/Subject Manager được phân công dạy lớp; quản lý môn đơn thuần và Admin không có quyền chấm/xuất điểm. Ngoài scope trả `404`; lỗi phụ thuộc từ chối an toàn.
- `PENDING → DRAFT → FINALIZED → PUBLISHED`; `SUPERSEDED` không được sửa/chốt/công bố. Student chỉ đọc điểm `PUBLISHED` của mình khi còn quyền học lớp.
- Điểm từ checklist rubric đã khóa, tính ở server bằng `BigDecimal`; không nhận điểm tổng do client/AI tự quyết. Điều chỉnh cần lý do, kẹp điểm cuối trong `[0,maxScore]`; không tự trừ điểm nộp trễ.
- Hiển thị `x / maxScore`, 2 chữ số thập phân, không quy đổi thang 10; feedback tối đa 10.000 ký tự.
- Sửa điểm `FINALIZED/PUBLISHED` cần lý do, giữ trạng thái và lịch sử chỉ thêm. Không đồng nhất lý do sửa điểm với lý do điều chỉnh: có thể cần cả hai.
- Mỗi ghi/chốt/công bố phải kiểm `version`; lệch trả `409`. Batch tối đa 200 mục, transaction riêng từng mục, trả kết quả từng mục.
- AI chỉ tạo proposal; người dùng chủ động chọn nguồn, không tự fallback sang personal key. Không gửi tên/email sinh viên trong prompt; không log bài làm/feedback/proposal.
- Chỉ U06 gọi provider, giữ credentials/quota, vận hành MCP; U09 không tự triển khai lại các phần đó.
- Sổ điểm không có điểm tổng/trọng số, Practice, Quiz hay điểm đề xuất AI. Export chỉ đưa điểm đã chốt/công bố; mục chưa nộp/chưa chốt phải ghi rõ.
- CSV/XLSX tạo theo yêu cầu, không lưu trên hệ thống; chặn công thức bảng tính, kiểm quyền trước khi mở response stream; không dùng dữ liệu cá nhân trong filename.
- Phân bố điểm Student: lớp bật cờ, ít nhất 20 người có điểm đã công bố; ẩn bucket dưới 5 người; không lộ điểm/danh tính cá nhân khác.
- Activity chỉ để tham khảo; phân biệt disabled/no data/available/expired/unavailable, không kết luận gian lận, không tự đổi điểm.
- Runtime targets theo NFR U09, cần đo mới được tuyên bố đạt: submissions p95 ≤500 ms/200 SV; gradebook p95 ≤1 s/200 SV×30 bài; export ≤10 s; finalize/publish p95 ≤2 s/batch 200, trong môi trường đo được ghi lại.
- Không Property-Based Testing; developer viết unit/component test, requirement owner và independent test-case owner là hai người khác developer theo AI-DLC. Hiện hai vai trò này chưa được phân công.

## Review Focus

1. Nộp lại khi proposal hoặc thao tác chốt đang chạy: không áp dụng điểm/proposal cho snapshot cũ — Tasks 2, 3, 7, 9.
2. Giáo viên bị đổi phân công sau khi mở trang hoặc dispatch AI: kiểm quyền lại khi ghi/nhận kết quả — Tasks 1, 4, 7.
3. Batch có trùng ID, version cũ, ID ngoài lớp hoặc lỗi một mục: không chốt nhầm, không phát thông báo trùng — Tasks 4, 7, 9.
4. Nội dung bài/nhận xét chứa HTML, XML không an toàn hoặc ô CSV bắt đầu bằng ký tự công thức: không thực thi nội dung — Tasks 5, 6.
5. Provider/stub không sẵn sàng, trả chậm hoặc thiếu provenance: không giả điểm thành công, không coi unknown token là 0 — Tasks 7, 8, 10.

---

## 1. Baseline đã kiểm tra trên branch

Ngày lập: 2026-10-20. Branch `grading-gradebook`, HEAD `6341aed2`. Owner U09: **Khuất Quang Hưng**, sau swap Iteration 2 với Võ Minh Tài; Jira đã cập nhật trong phiên trước. Header cũ ghi Người 2 là lịch sử, không phải phân công hiện hành. Không đổi owner Iteration 1/3 trong plan này.

| Thành phần | Bằng chứng hiện có | Hệ quả triển khai |
|---|---|---|
| U09 backend | `grading/contract/` có `IGroupEvaluation`, `IGroupSubmissionSource` và 3 record; phần application/domain/web chưa có nghiệp vụ | Cần triển khai mới, không phải sửa module đã hoàn tất |
| U09 frontend | `frontend/src/features/grading/index.ts` trống; chưa được import trong `features/register.ts` | Tạo feature và route, đăng ký slot cả server/client |
| U09 design | README Construction trỏ vào `u09-assignment-grading`; `u09-assignment-grading-gradebook` chỉ có thư mục, không có file | Dùng bộ đầu tiên làm nguồn thiết kế |
| U01/U05 | Auth và lớp/ghi danh đã có code; có `IAuthorization`, `IClassAccess`, slots cho gradebook/info/student detail | Tái sử dụng, không đọc repository của hai unit này |
| U06/U07/U08 | Các public contract đã có nhưng nghiệp vụ chủ yếu là stub | Unit test dùng fake; chưa tuyên bố AI/submit thật hoạt động |
| Database | Chưa có migrations `assignments` 1300, `assignmentwork` 1400 hay `grading` 1500 | Full migration U09 có FK phải chờ schema U07/U08; không bỏ FK hoặc tạo bảng giả trong production để chạy được |
| AI stub | `AiContractStubs` trả task `FAILED` ngay | Phải có trạng thái AI unavailable/failed; vẫn chấm tay được |
| Frontend API | `apiFetch<T>` luôn đọc JSON và đặt Accept JSON | Export cần helper nhận Blob giữ nguyên refresh/session/CSRF |
| Toolchain | `node`, `git` có trên PATH; lần kiểm tra chưa tìm thấy `mvn` | Các lệnh test dưới đây là lệnh dự kiến, chưa được chạy trong tác vụ lập plan |

## 2. Quyết định tích hợp và điểm lệch cần xử lý

| Điểm lệch | Cách triển khai trong plan | Điều kiện nghiệm thu |
|---|---|---|
| Thiết kế nói `inputVersion=submissionHash`, nhưng `AiTaskRequest.inputVersion` là `long` | Giữ nguyên contract U06: `targetRef="evaluation:"+evaluationId`, `inputVersion=proposal.versionNo`; lưu riêng full submission hash và rubric fingerprint trên proposal. Callback tìm đúng evaluation/version, kiểm cả task identity và snapshot trước ghi. Không ép/cắt hash thành số | Contract test với Tài/U06 phải chứng minh correlation và callback lặp/đến sớm được xử lý; nếu đổi contract phải có thay đổi phối hợp riêng |
| Logic dùng proposal `REJECTED`, domain list thiếu trạng thái đó | Bổ sung `REJECTED` vào enum nội bộ U09 cho output sai schema/rubric; `ApplyOutcome.REJECTED` đã có | Domain/schema/API/UI dùng cùng tập trạng thái |
| `SubmissionView.receiptHash` khác tên `submissionHash` trong tài liệu | Mapper U09 coi `receiptHash` là opaque immutable snapshot hash; không tự tính lại từ bản render | Test đối chiếu U08 và callback AI |
| Contract U07 hiện chỉ đọc rubric/content theo assignment, không chỉ rõ snapshot theo attempt | Lưu snapshot rubric và fingerprint khi tạo evaluation; yêu cầu U07/U08 bảo đảm content/rubric đọc được thuộc phiên bản bài đã nộp. Nếu có thể sửa sau publish, cần thêm public snapshot contract trước tích hợp thật | Không cho chấm bằng rubric mới của bài cũ; test version mismatch là release gate, không âm thầm lấy bản latest |
| `IGroupSubmissionSource` stub có thể trả document mẫu rỗng | Không coi tồn tại bean là U12 ready; không tạo bài nhóm giả để demo. Chỉ đọc evaluation đã nhận từ submit thật; fake nhóm chỉ nằm ở test | Spring wiring test và U12 handoff test |
| Chưa có action quyền grading trong U01 | Thêm `GRADING_REVIEW`, `GRADING_WRITE`, `GRADEBOOK_READ`, `GRADEBOOK_EXPORT` (Teacher/Subject Manager, CLASS) và `MY_GRADE_READ` (Student, CLASS) trong `Actions`/`AccessPolicy` theo cơ chế mở rộng hiện có | Kiểm đủ role, assignment, enrollment, thay đổi role và dependency failure; không import U01 application từ U09 |
| `IAccountLookup` chỉ có lookup từng ID | Dùng public lookup với cache trong request sau kiểm quyền; đo NFR. Nếu lookup tạo N+1 đáng kể, đề xuất additive batch contact contract với U01, không đọc bảng accounts trực tiếp | Benchmark gradebook/export; không tuyên bố đạt chỉ dựa vào fake |
| Nộp lại, sửa nhóm và optimistic locking có thể race | Lifecycle/chấm dùng cùng khóa logic cho identity bài nộp trong U09 + `@Version`; callback so full hash dưới lock. Individual published cũ giữ lịch sử, latest mới vào gradebook; nhóm reset có history | Test concurrency bằng PostgreSQL, không chỉ mock |
| Quiz activity trong FR-039 vs review chỉ GRADED của design U09 | Không tạo evaluation/gradebook cho Quiz; review activity Quiz phối hợp read-only với U11 khi unit đó có. Không mở luồng chấm Quiz để giải quyết khác biệt tài liệu | Handoff với U11; supporting review phải giữ assignment/policy scope, đánh dấu riêng khi chưa tích hợp |

Các quyết định trên không đổi public contract của unit khác trong tác vụ lập plan. Task 1 ghi nhận agreement trước khi code phần phụ thuộc; manual core có thể phát triển với test double trong thời gian chờ.

## 3. Cấu trúc file dự kiến

Quy ước dưới đây là prefix chính xác, các Files trong task dùng đường dẫn tương đối dưới prefix này:

- **B** = `backend/src/main/java/edu/aiplatform/grading/`
- **BT** = `backend/src/test/java/edu/aiplatform/grading/`
- **F** = `frontend/src/features/grading/`
- Domain: `Evaluation.java`, `EvaluationStatus.java`, `EvaluationKind.java`, `EvaluationHistory.java`, `GradingProposal.java`, `ProposalStatus.java`, `GradeDistributionCalculator.java`, `IEvaluationRepository.java`, `IGradingProposalRepository.java`.
- Application: mỗi service một file; DTO gom `GradingCommands.java`, `GradingViews.java`; access, mapper và prompt builder tách file.
- Infrastructure: `EvaluationJpaEntity.java`, `GradingProposalJpaEntity.java`, `IEvaluationJpaRepository.java`, `IGradingProposalJpaRepository.java`, `EvaluationRepositoryAdapter.java`, `GradingProposalRepositoryAdapter.java`; DB mapping không lộ qua contract.
- Web: `SubmissionReviewController.java`, `EvaluationController.java`, `GradingProposalController.java`, `GradebookController.java`, `MyGradeController.java`, `GradingExceptionHandler.java`.
- Frontend: `types.ts`, `api.ts`, `register.ts`, `index.ts`, `grading.module.css`; các page/panel/dialog tách theo task, không tạo một file chứa toàn feature.
- API spec mới: `docs/contracts/openapi/grading.yaml` (repo hiện chưa có thư mục OpenAPI; đây là artifact được tạo khi triển khai, không giả định đã có).
- Migration: `backend/src/main/resources/db/migration/grading/V20260925_1500__create_grading_tables.sql`.

## 4. Các mốc và thứ tự

1. Tasks 1–2: contracts, quyền, domain, persistence và lifecycle đã kiểm được.
2. Tasks 3–5: demo chấm tay hoàn chỉnh từ bài nộp đến công bố, dùng test fixture nếu U07/U08 chưa sẵn sàng.
3. Task 6: sổ điểm, Student result, phân bố và export.
4. Tasks 7–8: AI proposal và MCP, không thay đổi cơ chế chốt điểm.
5. Task 9: group grading adapter sẵn sàng cho U12 Iter 3.
6. Task 10: tích hợp thật, kiểm thử độc lập, đo NFR và bàn giao.

### Task 1: Contract baseline và quyền U09

**Files:** Create B`application/GradingAccess.java`, B`application/GradingCommands.java`, B`application/GradingViews.java`; modify `backend/src/main/java/edu/aiplatform/identity/contract/Actions.java`, `backend/src/main/java/edu/aiplatform/identity/application/AccessPolicy.java`; create BT`application/GradingAccessTest.java`; extend `backend/src/test/java/edu/aiplatform/identity/application/AuthorizationServiceTest.java` if existing, otherwise create that test; create API spec tại đường dẫn ở §3.

**Interfaces:** `GradingAccess.requireTeacher(Actor actor, UUID classId, String action): void`; `requireStudent(Actor actor, UUID classId): void`. Consumes `IAuthorization.authorize`, `IClassAccess.getClassRef/isActiveStudent`; current actor từ `@AuthenticationPrincipal`, không lấy từ body.

DTO decisions dùng cho các task sau:
- `GradingCommands.Save(version: long, checkedItemsByRubric: Map<UUID,Set<UUID>>, adjustment: BigDecimal, adjustmentReason: String, feedback: String, changeReason: String, usedProposalId: UUID?)`.
- `ItemVersion(evaluationId: UUID, version: long)`, `Batch(items: List<ItemVersion>)`.
- `AiRequest(source: AiSource, provider: AiProvider?, requestRef: String)`; `MemberSave(version: long, score: BigDecimal, reason: String?)`.
- `GradingViews.EvaluationDetail`: evaluation ID/kind/assignment/class/account, immutable submission, rubric snapshot, checks/score/maxScore/feedback/status/version, proposal summaries, append-only grade history, activity state; teacher response only.
- `ItemResult(evaluationId, outcome, code, newVersion)`, `BatchResult(List<ItemResult>)`; `SubmissionPage(rows,total,page,size,summary)`; `GradebookPage(rows,assignments,total,page,size)`; `StudentGradeView` không chứa proposal/teacher-only history; `DistributionView` chỉ chứa bucket được phép.
- Decimal wire fields dùng chuỗi thập phân; frontend dùng điểm server để hiển thị, không quyết định điểm cuối.

- [ ] Viết test `teacherMustBeAssignedToClass`, `subjectManagerWithoutTeachingAssignmentDenied`, `adminDenied`, `removedStudentDenied`, `roleRevokedAfterPageOpenDenied`, `dependencyFailureDoesNotReturnData`.
- [ ] Chạy `mvn -f backend/pom.xml -Dtest=GradingAccessTest,AuthorizationServiceTest test`; test mới phải fail trước khi thêm policy/service và pass sau đó.
- [ ] Định nghĩa DTO và OpenAPI: status/schema, quyền, errors, version và batch semantics. Chốt mapping hash/numeric version, rubric snapshot và AI provenance với owners U06/U07/U08 bằng contract tests; không đổi interface liên unit đơn phương.
- [x] Thêm policy qua U01; U09 dùng contract, trả `404` khi denied, ghi audit `ACCESS_DENIED`; dependency unavailable trả `503` an toàn, không coi là không có bài.
- [ ] Khi test pass, review scope diff rồi commit riêng `feat(grading): define API and access contracts`.

### Task 2: Evaluation domain, persistence và lifecycle nộp bài

**Files:** Create domain + infrastructure files ở §3; B`application/EvaluationLifecycleService.java`, B`application/SubmissionSnapshotMapper.java`; modify B`contract/stub/GradingContractStubs.java` chỉ khi cần tránh bean thật/giả trùng nhau; create migration §3; tests BT`domain/EvaluationTest.java`, BT`application/EvaluationLifecycleServiceTest.java`, BT`infrastructure/GradingPersistenceTest.java`.

**Interfaces:** `EvaluationLifecycleService implements ISubmissionListener, IGroupEvaluation` giữ đúng chữ ký public hiện có. Repository `findByAttemptId(UUID): Optional<Evaluation>`, `findById(UUID): Optional<Evaluation>`, `save(Evaluation): Evaluation`, `findForAssignmentAndClass(UUID,UUID): List<Evaluation>`, `findForClass(UUID): List<Evaluation>`. Lifecycle nhận snapshot content/hash/rubric đã khóa từ contract U07/U08. Group implementation chi tiết trong Task 9. Consumer lấy group source tùy chọn qua `ObjectProvider<IGroupSubmissionSource>`: chưa có U12 và chưa có group evaluation thì không hiển thị nhóm; có evaluation nhóm mà thiếu source thì báo unavailable, không giả nội dung rỗng.

- [ ] Viết test `practiceSubmissionCreatesNoEvaluation`, `duplicateSubmitIsIdempotent`, `newAttemptSupersedesOnlyUnpublishedEvaluations`, `publishedOldAttemptRemainsReadable`, `submissionRollbackRollsBackEvaluation`, `rubricSnapshotDoesNotFollowLaterAssignmentChanges`.
- [ ] Chạy `mvn -f backend/pom.xml -Dtest=EvaluationTest,EvaluationLifecycleServiceTest test`; xác nhận fail rồi triển khai aggregate/mapper/service, chạy lại đến pass.
- [x] Dùng `@Transactional(propagation=MANDATORY)` cho listener bắt buộc cùng transaction submit; unique attempt/member/group document/requestRef, JSONB checks/history/proposal, NUMERIC(6,2), CHECK trạng thái/kind/score; `@Version`, index theo design. Cho điểm chưa chấm là null, không giả 0.
- [ ] Testcontainers kiểm unique/check/rollback/locking và quyền app không DELETE. Test prerequisite schema chỉ được đặt trong test resources; full migration có FK sang `assignments`, `assignment_attempts`, `course_classes`, `accounts` phải chạy sau migrations U07/U08 thật. Không tạo production placeholder tables.
- [ ] Chạy `mvn -f backend/pom.xml -Dtest=GradingPersistenceTest test`; khi Docker hoặc schema prerequisite thiếu, ghi rõ blocked/skipped; không tính là pass migration. Commit phần đã xác minh, đánh dấu gate còn thiếu.

### Task 3: UC42 — Submission review và activity read-only

**Files:** Create B`application/SubmissionReviewService.java`, B`web/SubmissionReviewController.java`, B`web/EvaluationController.java`, B`web/GradingExceptionHandler.java`; tests BT`application/SubmissionReviewServiceTest.java`, BT`web/GradingReviewControllerTest.java`.

**Interfaces:** `list(Actor, UUID classId, UUID assignmentId, String status, Boolean late, int page, int size): SubmissionPage`; `detail(Actor, UUID evaluationId): EvaluationDetail`. Consumes U07 assignment/content/rubric, U08 submissions/activity, U05 class roster, U01 contacts; group adapter Task 9.

- [ ] Test `subjectAssignmentIsScopedToRequestedClass`, `cannotPairAssignmentWithOtherClass`, `practiceAndQuizExcluded`, `countsIncludeNotStartedInProgressSubmittedLate`, `pastAttemptIsReadOnly`, `activityUnavailableIsNotNoData`, `historyOnlyVisibleToAssignedTeacher`.
- [ ] Chạy `mvn -f backend/pom.xml -Dtest=SubmissionReviewServiceTest,GradingReviewControllerTest test`; xác nhận fail, rồi implement joins qua contract và filter/pagination có giới hạn; chạy lại pass.
- [ ] Implement GET submissions/detail/progress/student-grades routes theo §5; không hydrate nội dung toàn bộ mọi bài trong list. GET list không tạo evaluation như side effect; lifecycle thiếu dữ liệu báo unavailable và điều tra integration.
- [x] Activity lỗi chỉ ảnh hưởng panel activity: trả `UNAVAILABLE`, không giả `NO_DATA`; assignment/submission/access không đọc được thì từ chối toàn bộ tài nguyên. Activity của bài nhóm giữ account từng người, không trừ điểm.
- [ ] Commit `feat(grading): review class submissions and activity` sau test.

### Task 4: UC44 — Chấm tay, chốt, công bố và sửa điểm

**Files:** Create B`application/GradingService.java`, B`application/EvaluationItemTransaction.java`; extend Evaluation/History, controller/commands Task 1; tests BT`application/GradingServiceTest.java`, BT`application/GradingBatchTest.java`, BT`infrastructure/GradingConcurrencyTest.java`.

**Interfaces:** `save(Actor, UUID evaluationId, Save): EvaluationDetail`; `finalizeBatch(Actor, Batch): BatchResult`; `publishBatch(Actor, Batch): BatchResult`. `EvaluationItemTransaction` là bean riêng dùng REQUIRES_NEW cho từng batch item; không self-invoke annotation. Audit `IAudit.record`; event `IEventPublisher.publish(DomainEvent)`/`EventTypes.GRADE_PUBLISHED`.

- [ ] Test với rubric 4+6 điểm: tick mục 4, adjustment -1 có lý do → 3.00/10.00; thêm test không tin client score, sai rubric item, feedback 10.001 ký tự, adjustment thiếu lý do, clamp 0/max, sửa finalized/published thiếu lý do và không tự đổi trạng thái.
- [ ] Test `cannotFinalizePending`, `cannotPublishDraft`, `versionConflictReturns409`, `mixedBatchCommitsOnlyValidRows`, `duplicateIdsProduceOneTransition`, `publishRetryDoesNotEmitDuplicateEvent`, `failureDoesNotRollBackOtherItems`.
- [ ] Chạy `mvn -f backend/pom.xml -Dtest=GradingServiceTest,GradingBatchTest test`; xác nhận fail; triển khai scoring bằng `IRubric.score`, lưu actor/time/history và state machine; chạy lại pass.
- [x] Publish chỉ phát event cho item vừa chuyển trạng thái thành công: schemaVersion=1, sourceUnit=U09, payload assignmentId/classId/studentIds; không chứa raw điểm/bài làm/feedback. U03 hiện after-commit best effort, không hứa exactly-once hoặc durable outbox; U14 dedup theo event khi tích hợp.
- [ ] Test PostgreSQL race submit lại/save/finalize/callback và hai tab cùng version; audit SUCCESS trong transaction ghi điểm, rollback không phát event. Chạy `mvn -f backend/pom.xml -Dtest=GradingConcurrencyTest test` có Docker; commit riêng.

### Task 5: Frontend manual workflow hoàn chỉnh

**Files:** Create F`types.ts`, F`api.ts`, F`register.ts`, F`grading.module.css`, F`SubmissionDetailPage.tsx`, F`SubmissionTable.tsx`, F`SubmissionFilters.tsx`, F`ProgressSummary.tsx`, F`GradingWorkspacePage.tsx`, F`SubmissionViewer.tsx`, F`RubricChecklistPanel.tsx`, F`GradeHistoryPanel.tsx`, F`ActivityPanel.tsx`, F`BatchFinalizeDialog.tsx`, F`BatchPublishDialog.tsx`, F`SubmissionsLink.tsx`; modify F`index.ts`, `frontend/src/features/register.ts`; create `frontend/src/app/teaching/classes/[classId]/assignments/[assignmentId]/submissions/page.tsx`, `frontend/src/app/teaching/evaluations/[evaluationId]/page.tsx`; tests F`manual-workflow.test.tsx`, F`registration.test.tsx`, F`api.test.ts`.

**Interfaces:** `SubmissionDetailPage({classId,assignmentId})`, `GradingWorkspacePage({evaluationId})`; API functions `getSubmissions`, `getEvaluation`, `saveEvaluation`, `finalizeEvaluations`, `publishEvaluations` dùng DTO §1. `register.ts` đăng ký `assignmentRowActions` với props hiện có; route dùng RoleGuard TEACHER/SUBJECT_MANAGER.

- [ ] Test role gate, list filters/pagination, draft không hiện nút publish khả dụng, batch partial result, stale version báo reload nhưng giữ input chưa lưu, save thành công chuyển bài kế tiếp; save lỗi không chuyển bài.
- [ ] Chạy `npm --prefix frontend test -- src/features/grading/manual-workflow.test.tsx src/features/grading/registration.test.tsx src/features/grading/api.test.ts`; xác nhận fail, triển khai feature, chạy lại pass.
- [x] Dùng DocumentViewer public của U07 khi được cung cấp; trong thời gian chờ chỉ tạo safe read-only renderer U09 cho block types đã biết, không sửa framework editor U07. Escape HTML; không dùng raw XML/SVG với `dangerouslySetInnerHTML`, URL/file đi qua backend đã kiểm quyền. Test malicious text/diagram không chạy script.
- [x] Form gửi checklist/version, không gửi score tổng; hiện pending/error/empty, keyboard/focus/dialog theo shared UI; disable double submit; cảnh báo dirty navigation; không reset rubric form do polling background.
- [ ] Import registration qua `features/register.ts` để ClientRegistrations cũng load; không sửa U05 sang import ngược U09. Đối chiếu màn hình t16/t17 theo mô tả frontend-components; fidelity hình ảnh chỉ xác nhận khi có reference/preview đã kiểm. Chạy typecheck/lint, commit.

### Task 6: UC45 — Gradebook, Student results, distribution và export

**Files:** Create B`application/GradebookService.java`, B`application/GradebookExporter.java`, B`application/StudentGradeService.java`, B`domain/GradeDistributionCalculator.java`, B`web/GradebookController.java`, B`web/MyGradeController.java`; F`ClassGradebookTab.tsx`, F`GradebookExportDialog.tsx`, F`ClassProgressSection.tsx`, F`StudentGradesSection.tsx`, F`StudentGradeCell.tsx`, F`StudentGradePanel.tsx`; extend F`register.ts`, F`api.ts`; modify `frontend/src/shared/api/client.ts` and its test; add pinned POI dependency in `backend/pom.xml`; tests BT`application/GradebookServiceTest.java`, BT`application/GradebookExporterTest.java`, BT`domain/GradeDistributionCalculatorTest.java`, BT`application/StudentGradeServiceTest.java`, F`gradebook.test.tsx`.

**Interfaces:** `GradebookService.list(Actor,UUID classId,String query,int page,int size): GradebookPage`; `GradebookExporter.write(Actor,UUID classId,UUID assignmentId?,String format,OutputStream): void`; `StudentGradeService.list(Actor,UUID classId): List<StudentGradeView>` and `forAttempt(Actor,UUID attemptId): StudentGradeView`; calculator `calculate(List<BigDecimal> scores,List<BigDecimal> maxScores): DistributionView`. Shared helper `apiDownload(path:string,init?:RequestInit):Promise<Blob>` reuses session refresh/CSRF/error parsing; call sites set explicit Accept MIME.

- [ ] Test active roster, subject assignment scoping, latest graded attempt chọn đúng khi bản cũ đã publish, group member score, không tổng điểm, không Quiz/Practice/proposal, Student không đọc draft/finalized/người khác.
- [ ] Test threshold 19 vs 20 published learners và bucket 4 vs 5; đề xuất implementation bins theo % `[0,20),[20,40),[40,60),[60,80),[80,100]`. Đây là quyết định hiển thị của plan, không phải ngưỡng mới của requirements; xử lý maxScore=0 bằng không cung cấp distribution, không chia 0; không trả raw scores/IDs và không tiết lộ suppressed counts qua DTO phụ.
- [ ] Test CSV có dấu phẩy/newline/quote và các prefix `= + - @`, kể cả leading whitespace/control characters; XLSX dùng string cell với text nhập bởi người dùng, không tạo formula. `PENDING/DRAFT` → ghi rõ chưa chốt, không xuất điểm/feedback nháp; `FINALIZED/PUBLISHED` → xuất điểm/feedback đã chốt trong phạm vi Teacher được phép. Unauthorized phải fail trước write byte đầu tiên.
- [ ] Chạy `mvn -f backend/pom.xml -Dtest=GradebookServiceTest,GradebookExporterTest,GradeDistributionCalculatorTest,StudentGradeServiceTest test`; xác nhận fail, implement services/exporter/DTO, chạy lại pass. POI pin phiên bản chính xác đã kiểm nguồn chính thức và tương thích Java17 khi triển khai; SXSSFWorkbook close/dispose xóa spool sau cả success/error.
- [ ] Viết test `apiDownload` giữ cookies, refresh retry, Blob và API error; bảo đảm không đổi Accept JSON của call cũ. Chạy `npm --prefix frontend test -- src/shared/api/client.test.ts src/features/grading/gradebook.test.tsx` qua chu kỳ fail/pass.
- [ ] Đăng ký gradebook/info vào `teacherClassTabs`, popup vào `studentDetailSections`, Student result vào `studentAssignmentResults`/`submissionResultSections`; component host chọn theo tab, không render toàn bộ feature mọi tab. Commit.

### Task 7: UC43 — AI proposals backend và callback

**Files:** Create B`application/GradingProposalService.java`, B`application/GradingProposalResultHandler.java`, B`application/GradingPromptBuilder.java`, B`application/GradingProposalValidator.java`, B`web/GradingProposalController.java`; extend proposal domain/repository; schema `backend/src/main/resources/grading/grading-proposal.schema.json`; tests BT`application/GradingProposalServiceTest.java`, BT`application/GradingProposalResultHandlerTest.java`, BT`application/GradingPromptBuilderTest.java`.

**Interfaces:** `request(Actor,UUID evaluationId,AiRequest): GradingProposalView`; `requestBatch(Actor,List<ItemVersion>,AiRequest): BatchResult`; `list(Actor,UUID evaluationId): List<GradingProposalView>`. `GradingProposalView` thuộc GradingViews: id/versionNo/status/submissionHash, checklist + section/global feedback, origin/taskId/requester/source/provider/model/agentLabel/usage-state, created/completed/used timestamps. Handler implements đúng `IAiTaskResultHandler.taskType/onReady/onFailed`; taskType `GRADING_PROPOSAL`.

- [ ] Test prompt chỉ gồm hướng dẫn/khung/rubric IDs và bài làm; không đưa tên/email/member contribution vào structured metadata. Sanitize identity metadata ở source; không hứa tự loại mọi thông tin cá nhân do sinh viên gõ trong nội dung tự do.
- [ ] Test user phải chọn source, shared quota hết không gọi personal, FAILED trả ngay từ stub không treo PENDING, requestRef retry không dispatch lần hai, cùng requestRef khác payload bị conflict, batch 200/201 và quota cạn giữa batch. Batch dùng requestRef gốc + evaluationId để tạo key ổn định riêng mỗi item, lưu payload fingerprint gồm source/provider/evaluation snapshot; retry không được gửi lại các item đã nhận task. Khi hết shared quota, ghi outcome riêng cho item đó và các item chưa dispatch, không đổi nguồn.
- [ ] Test callback JSON sai/ID rubric lạ/duplicate item/điểm đề xuất không khớp checklist → REJECTED; full hash cũ/rubric snapshot sai → STALE; actor hết quyền → REJECTED; callback lặp không overwrite READY/used result; onFailed không đảo READY thành FAILED; điểm Evaluation không đổi khi proposal READY.
- [ ] Chạy `mvn -f backend/pom.xml -Dtest=GradingProposalServiceTest,GradingProposalResultHandlerTest,GradingPromptBuilderTest test`; fail rồi implement, pass. Lưu proposal PENDING có correlation trước dispatch; phối hợp callback visibility với transaction U06: callback đến trước commit không được mất hoặc tạo điểm giả. Test trường hợp đó là gate tích hợp, không dựa vào sleep.
- [ ] Store snapshot hash riêng, `inputVersion=proposal.versionNo` theo §2; kiểm lại current submitted snapshot và quyền khi callback và khi save với `usedProposalId`. Provenance chỉ ghi điều biết từ U06; dữ liệu thiếu ghi unavailable, không dựng model/token từ phỏng đoán.
- [ ] Lưu/áp dụng proposal vẫn đi qua Task 4, preserve original AI output; AI không tự save/finalize/publish. Commit sau unit + correlation contract tests.

### Task 8: MCP proposal tool và AI UI

**Files:** Create B`application/GradingMcpTools.java`, BT`application/GradingMcpToolsTest.java`; F`AiProposalPanel.tsx`, F`BatchAiDialog.tsx`, F`ai-proposal.test.tsx`; extend workspace/submission page/api của Tasks 5/7. UI `AiSourcePicker`/`AiTaskStatus` của U06 hiện chưa có: dùng public exports của U06 khi delivered; U09 fallback unavailable/manual, không tự làm trang quản lý credentials.

**Interfaces:** `GradingMcpTools implements IMcpToolProvider`; tool `record_grading_proposal` WRITE, allowedRoles TEACHER/SUBJECT_MANAGER, args `submissionRef,submissionHash,proposal,requestRef,expectedVersion`; `submissionRef` có kind ATTEMPT/GROUP_DOCUMENT và id. Result safe proposal ID/status, không trả credentials. Tool dùng cùng validator/service của Task 7, không có finalize/publish tool.

- [ ] Test MCP student/admin/out-of-class bị từ chối, revoked credential do U06 chặn trước dispatch, expectedVersion/hash stale không ghi, spoofed actor trong args bị bỏ qua/từ chối, tool retry idempotent và group source chưa ready không trả proposal thành công giả.
- [ ] Chạy `mvn -f backend/pom.xml -Dtest=GradingMcpToolsTest test`; fail/implement/pass. Test READ_WRITE enforcement ở U06 connector integration khi có provider, không tự coi allowedRoles đã đủ xác thực.
- [ ] UI test chưa chọn source không submit; hiển thị cost bearer/quota hoặc unavailable, trạng thái pending/ready/rejected/stale/failed, proposal history/Agent label; dùng proposal chỉ điền form; chưa click Lưu không có request ghi điểm. DRAFT chỉnh khác đề xuất không bắt lý do chỉ vì khác AI.
- [ ] Poll hữu hạn bằng shared status hook, dừng khi unmount/terminal và không ghi đè manual form. Batch trả riêng từng bài, quota lỗi không tự đổi nguồn. Chạy `npm --prefix frontend test -- src/features/grading/ai-proposal.test.tsx`, typecheck/lint, commit.

### Task 9: Group grading contract và U12 handoff

**Files:** Extend B`application/EvaluationLifecycleService.java`, `GradingService.java`, `SubmissionReviewService.java`, `GradebookService.java`; create F`MemberContributionPanel.tsx`; tests BT`application/GroupEvaluationTest.java`, BT`infrastructure/GroupGradingConcurrencyTest.java`, F`group-grading.test.tsx`.

**Interfaces:** Existing `IGroupEvaluation.onGroupSubmitted(UUID,String,UUID,UUID,List<UUID>)`; consumes `IGroupSubmissionSource.getGroupSubmission/listGroupSubmissions`. `GradingService.saveMember(Actor,UUID groupEvaluationId,UUID memberId,MemberSave): EvaluationDetail`; only current submission snapshot members allowed. Group save/finalize/publish updates document and members coherently in one item transaction; group counts as one batch item.

- [ ] Test first submit tạo document + đúng member evaluations, same hash duplicate no-op, resubmit hash mới reset PENDING/history và stale proposals, không mutate membership lịch sử theo danh sách lớp mới.
- [ ] Test điểm member mặc định bằng điểm nhóm, chỉnh riêng trong 0..max; reason optional trước chốt nhưng sửa finalized/published vẫn cần lý do theo rule chung; không công thức contribution tự động. Document/member version conflict làm rollback cả group item, không publish nửa nhóm.
- [ ] Chạy `mvn -f backend/pom.xml -Dtest=GroupEvaluationTest test`; fail/implement/pass; test PostgreSQL race callback nhóm cũ vs nộp lại bằng `GroupGradingConcurrencyTest`.
- [ ] Teacher UI hiển thị section authors, score chung và từng người; Student chỉ thấy member score của mình. `grade.published` tới đúng member snapshot vừa công bố, không lộ raw điểm trong event. Chạy frontend test, commit.
- [ ] Nếu U12 thật chưa delivered: ghi **adapter tests passed; end-to-end U12 blocked**. Không đánh dấu group integration hoàn tất hoặc bắt U09 import `groups/groupdocs` internals.

### Task 10: Integration, NFR và bàn giao

**Files:** Create `aidlc-docs/construction/u09-assignment-grading/code/implementation-summary.md` khi đã có code; independent tester bổ sung `backend/src/test/java/edu/aiplatform/grading/integration/GradingWorkflowIntegrationTest.java`, `GradingApiSecurityIntegrationTest.java`, `GradingPerformanceTest.java` và system test evidence vào `aidlc-docs/construction/build-and-test/`; cập nhật OpenAPI/plan checkboxes theo phần thực sự chạy.

**Interfaces:** Chạy trên U01/U03/U05 thật và U06/U07/U08 public contract implementation; U12/U11/U14 integration ghi trạng thái riêng theo readiness, không lấy mock làm bằng chứng thực tế.

- [ ] Independent tester kiểm submit GRADED → manual/AI proposal → lưu DRAFT → finalize → publish → Student result → export; thêm admin/subject manager không dạy/student khác lớp, thay giáo viên và removed enrollment.
- [ ] Kiểm real migrations mới/cũ, `spring.jpa.hibernate.ddl-auto=validate`, rollback submit/evaluation, app DB privilege, event sau commit và broker failure không rollback điểm. Chạy acceptance profile với `platform.contract-stubs.enabled=false` và real required providers; thiếu provider bắt buộc phải báo unavailable hoặc fail startup rõ ràng, không lấy stub làm bằng chứng nghiệm thu. U14 delivery chỉ nghiệm thu khi có consumer thật.
- [ ] Chạy `mvn -f backend/pom.xml verify` trong môi trường có JDK17/Maven/Docker; xem test report để phát hiện skipped Testcontainers. Chạy frontend: `npm --prefix frontend run lint`, `npm --prefix frontend run typecheck`, `npm --prefix frontend test`, `npm --prefix frontend run build`.
- [ ] Đo p95 và export theo các dataset/giới hạn Global Constraints; ghi warmup/sample size/môi trường/timing. Kiểm row-query/contact lookup để tránh N+1; không tuyên bố đạt NFR trên kết quả unit test.
- [ ] Kiểm UI theo design t02b/t02j/t16/t17/t17b/t18/t19 và Student grade slots khi host U08 có; kiểm keyboard/dialog/409/form recovery, nội dung XML/HTML không an toàn, CSV/XLSX với Unicode và công thức.
- [ ] Ghi summary mapping UC→BR→files→tests và integration gates còn mở; requirement/test/code owners ký review theo phân công thực tế. Không auto commit/push/merge trong tác vụ lập plan này; implementation commits chỉ thực hiện ở giai đoạn code được giao sau.

## 5. API inventory và traceability

| Endpoint, dưới `/api/v1` | Owner task | UC / mục đích |
|---|---|---|
| GET `/classes/{classId}/assignments/{assignmentId}/submissions` | 3 | UC42 list/progress |
| GET `/evaluations/{id}` | 3 | UC42 read-only detail/history/activity |
| PUT `/evaluations/{id}` | 4 | UC44 save/grade changes; UC43 dùng proposal |
| POST `/evaluations:finalize`, `/evaluations:publish` | 4 | UC44/45 state transitions, 1 hoặc nhiều item |
| GET `/classes/{classId}/gradebook`, `/gradebook/export` | 6 | UC45 |
| GET `/classes/{classId}/submission-progress`, `/grade-distribution?assignmentId=` | 3, 6 | Teacher class info |
| GET `/classes/{classId}/students/{accountId}/grades` | 6 | Teacher student detail |
| GET `/me/grades?classId=`, `/me/grades/attempts/{attemptId}`, `/me/grade-distributions?classId=` | 6 | Student supporting UC37/38/40 |
| POST/GET `/evaluations/{id}/ai-proposals` | 7 | UC43 |
| POST `/evaluations:ai-proposals` | 7 | UC43 batch |
| MCP `record_grading_proposal` | 8 | UC43, U06 authenticated dispatch |
| PUT `/evaluations/{id}/members/{accountId}` | 9 | UC44 group member grade |

| Rule coverage | Tasks |
|---|---|
| BR-U09-01–04: access/class/Graded/Student visibility | 1, 2, 3, 6 |
| BR-U09-10–13: lifecycle, review, navigation, activity | 2, 3, 5 |
| BR-U09-20–23: checklist, adjustment, feedback, display | 4, 5 |
| BR-U09-30–35: source, prompt, proposal, review, batch, failure | 7, 8 |
| BR-U09-40–43: finalize/publish/change/history | 4, 5 |
| BR-U09-50–51: group/member grading | 9 |
| BR-U09-60–62: gradebook/export/distribution | 6 |
| BR-U09-70–72: audit/concurrency/exclusions | 1–10 |
| US-RPT-001 reminders | U14 owns reminders; U09 only review/progress, no reminder scheduler added |
| FR-039 Quiz activity | Read-only U11 handoff gate §2/Task10; never Quiz grades |

## 6. Definition of done và tình trạng plan

- [x] Đọc nguồn Inception, U09 functional/NFR/infrastructure, baseline code-generation plan và code liên quan.
- [x] Xác định branch, skeleton, contracts/stubs, frontend slots và missing migration dependencies.
- [x] Lập task có file, interface, test/acceptance, dependency gate và thứ tự triển khai; tự đối chiếu coverage ở §5.
- [ ] Task 1–10 triển khai và kiểm chứng; chưa bước implementation nào được thực hiện trong phiên lập plan.
- [ ] U06/U07/U08 callback/snapshot/schema contracts xác nhận với owners và test qua implementation thật.
- [ ] Group/U11/U14 integration được ghi rõ passed/blocked theo readiness, không bỏ yêu cầu hoặc báo hoàn tất giả.
- [ ] Test độc lập, đo NFR, review và implementation summary đầy đủ trước khi đóng U09.

**Kết quả tác vụ hiện tại:** tài liệu kế hoạch; không thay code, schema, dependency, Jira, trạng thái approval hoặc tiến độ runtime.
