# U09 Assignment Grading — frontend-components

**Naming update 2026-11-05:** internal API actions use explicit names such as `loadEvaluation`, `saveEvaluation`, `finalizeGrades`, `publishGrades`, `requestProposals` and `loadStudentGrades`; scoring/navigation/error helpers use verbs. Hook `save`/`reload`/`navigate` return keys remain stable through aliases, UI action values remain `finalize`/`publish`, and all mocks/imports are updated. [Current evidence](../../build-and-test/2026-11-05-u05-u09-u15-conventions-results.md).

## Typed submitted document — 2026-11-05

`model/submissionDocument.ts` derives `SubmissionDocumentDto` from the shared document block union, adding the submitted IMAGE `fileId`. `toEditorDocument` preserves block IDs/origins and content, derives image `hasFile` without mutating the input and replaces the workspace's unchecked double cast. `SubmissionViewer`, heading labels and image-link loading narrow by block kind. Image retrieval still uses the evaluation-authorized endpoint. This is compile-time typing, not a replacement for backend validation. [Verification](../../build-and-test/2026-11-05-u05-u09-u15-followup-results.md).

## Audit remediation — 2026-11-04

Submission links carry `status`/`late` into the evaluation page, detail request and previous/next/save-next links. Explicit student selection clears the queue context. Direct proposal links remain compatible. Conflict copy distinguishes locked grades, changed submissions, changed rubrics and version conflicts while retaining local edits. Pure rubric calculations live in `model/rubricScores.ts`; components and workspace hooks consume that module. Automated execution evidence is tracked in [cleanup results](../../build-and-test/2026-11-04-u05-u09-u15-cleanup-results.md); browser acceptance is separate.

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## Bỏ số lượt ở danh sách bài nộp — 2026-11-10

`SubmissionDetailPage` bỏ cột "Lượt" (`6a787e3f`); nhãn trạng thái `SUPERSEDED` đổi từ "Lượt cũ" thành "Bài nộp cũ". `GradeDistributionSettings` không còn ghi "ẩn danh" (`4ff7a0da`). Lý do: bài có chấm nộp lại không giới hạn, xem BR-U08-70…74.

## AI review và điều hướng bài nhóm — 2026-11-09

- `ClassGradebookTab`: bài nhóm đang chọn có danh sách "Bản nộp chung theo nhóm" (`useSubmissionDirectory`), mỗi nhóm một link "Xem bản nộp chung" mở Grading Workspace của đánh giá nhóm; có trạng thái đang tải, lỗi kèm Thử lại và "Chưa có bản nộp chung để xem".
- `GradingWorkspacePage`: bài nhóm ghi "Người thực hiện: tên" theo tiêu đề từng phần (`sectionBylines`), người làm của phần không gắn được tiêu đề ghi ở đầu trang; hiện "Nhận xét chung" và "Điểm mạnh và điểm cần cải thiện" ngay trên trang; thanh công cụ có nút "Điểm thành viên" cuộn tới `#group-member-grades` và nút "Xem đề xuất AI". Với đánh giá `MEMBER`, nút AI có nhãn "Mở bài nộp nhóm để chấm bằng AI" và chuyển sang `groupEvaluationId`.
- `RubricChecklistPanel` nhận `feedbackItems` và `acknowledgedReviewItems` để hiện nhận xét AI theo từng mục; `MemberContributionPanel` dùng trường Điểm và Lý do có nhãn cho từng thành viên.
- Bài có phần bỏ trống: đề xuất AI cho 0 điểm ở các phần đó mà không gọi AI (BR-U09-36); giao diện không đổi.

Nguồn: `baf349b0`, `d85b5429`; tests nguồn `gradebook-layout.test.tsx`, `workspace.test.tsx`, không chạy lại trong đợt đối chiếu 2026-11-10.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 32 | Teacher Class Detail: tab Sổ điểm (`t02b`), popup Xuất sổ điểm (`t02j`); tab Thông tin lớp, phần tiến độ và phân bố (`t18`); tab Đánh giá, cột Bài nộp (`t21`) | `ClassGradebookTab`, `GradebookExportDialog`, `ClassProgressSection`, `SubmissionsLink` | 42, 44, 45 | Teacher, Subject Manager được giao dạy |
| 33 | Student Detail, phần bài nộp và điểm (`t19`) | `StudentGradesSection` | 45 | Như trên |
| 34 | Submission Detail (`t16`), popup AI đề xuất hàng loạt (`t16b`), Chốt / Công bố điểm (`t16c`) | `SubmissionDetailPage`, `BatchAiDialog`, `BatchFinalizeDialog`, `BatchPublishDialog` | 42, 43, 44 | Như trên |
| 35 | Grading Workspace: bài cá nhân (`t17`), bài nhóm (`t17b`) | `GradingWorkspacePage` | 42, 43, 44 | Như trên |
| 38, 39, 41 | Điểm trên Student Assignments (`s19`), Assignment Detail đã công bố (`s12c`), Submission History đã chấm (`s13b`) | `StudentGradeCell`, `StudentGradePanel` | 37, 38, 40 | Student |

Điểm luôn hiện "x / tổng điểm rubric", 2 chữ số thập phân, không quy đổi thang 10 (BR-U09-23).

## 2. Cây component

```
(TeacherClassTab "info" của U05)
  ClassProgressSection                       thẻ Bài đang mở, Chờ chấm; Tiến độ nộp bài; Phân bố điểm
(TeacherClassTab "gradebook" của U05)
  ClassGradebookTab                          tab Chấm điểm: danh sách theo bài/sinh viên, nút Bảng điểm
    GradebookMatrix                          gradebookView=matrix: sinh viên × bài, lọc bài/tìm sinh viên/phân trang
      GradebookExportDialog                  CSV hoặc XLSX, toàn bộ hoặc một bài
(dòng bài ở tab Đánh giá, đăng ký qua AssignmentRowAction của U07)
  SubmissionsLink                            "x / N nộp · n chờ chấm", mở Submission Detail
(StudentDetailSection của U05)
  StudentGradesSection                       bài nộp và điểm của một sinh viên trong lớp
app/teaching/classes/[classId]/assignments/[assignmentId]/submissions/   SubmissionDetailPage   #34
  ProgressSummary                            đã nộp, đang làm, chưa nộp, nộp trễ, thời gian còn lại
  SubmissionFilters, SubmissionTable         tab trạng thái, lọc đúng hạn/trễ, chọn nhiều
  BatchAiDialog                              AiSourcePicker (U06), kết quả từng bài
  BatchFinalizeDialog, BatchPublishDialog    xác nhận, kết quả từng bài
app/teaching/evaluations/[evaluationId]/      GradingWorkspacePage   #35
  GradingTopBar, SubmissionNavList, PrevNextNav
  SubmissionViewer                           DocumentViewer (U07); chọn lượt; bài nhóm: bản nộp theo mục kèm tác giả
  RubricChecklistPanel                       tick mục chấm theo phần · tiêu chí, điểm tự tính
  AdjustmentField, FeedbackEditor
  MemberContributionPanel                    bài nhóm: điểm nhóm và điểm từng thành viên
  AiProposalPanel                            AiSourcePicker, AiTaskStatus (U06), các version đề xuất, Dùng đề xuất
  ActivityTimeline (U08)                     khi bài bật theo dõi
  GradeHistoryPanel                          lịch sử sửa điểm
  FinalizeButton, PublishButton
(đăng ký vào slot của U08)
  StudentGradeCell                           StudentAssignmentResult: điểm đã công bố, nút Phân bố
  StudentGradePanel                          SubmissionResultSection: điểm, nhận xét, mục rubric đạt, nhãn "đã cập nhật"
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `SubmissionDetailPage`, `ProgressSummary` | `GET .../assignments/{assignmentId}/submissions` | Nút quay lại; "Bài nộp · tên bài" kèm nhãn trạng thái; đường dẫn "Lớp được phân công › lớp › Đánh giá"; nút Xem sổ điểm. Thẻ: Đã nộp (trên N sinh viên), Đang làm (có lượt làm dở), Chưa nộp (chưa mở bài), Nộp trễ (sau hạn nộp), Còn lại (đếm ngược tới hạn) |
| `SubmissionFilters`, `SubmissionTable` | như trên | Tab Tất cả, Chờ chấm, Đã chấm (nháp), Đã chốt, Đã công bố (kèm số); lọc Nộp (đúng hạn, trễ); ô chọn từng dòng và chọn tất cả, "Đã chọn n bài"; nút AI đề xuất hàng loạt, Chốt điểm, Công bố điểm. Cột Sinh viên (avatar, tên, email), Nộp lúc (kèm "trễ n phút"), Lượt (x / y), Trạng thái, Điểm ("x / tổng"), Chấm hoặc Xem; phân trang; ghi chú "Bài Practice không có ở đây. Công bố điểm thì sinh viên nhận thông báo, không kèm điểm trong thông báo." |
| `BatchAiDialog` | `POST /api/v1/evaluations:ai-proposals` | Tên bài · số bài chờ chấm; chọn nguồn (API của trường: còn x / y token, trường chịu chi phí; API cá nhân theo từng nhà cung cấp: tính phí vào tài khoản của bạn), không chọn sẵn; ước tính token cho cả lô; "Đề xuất chỉ là bản nháp; bạn kiểm tra và lưu từng bài."; bảng kết quả từng bài (Đã có đề xuất, Đang chạy, Lỗi, Hết hạn mức, Ngoài quyền; Điểm đề xuất); Đóng, Chạy cho n bài; không tự đổi nguồn khi hết quota |
| `BatchFinalizeDialog`, `BatchPublishDialog` | `POST /api/v1/evaluations:finalize`, `:publish` | "Công bố điểm cho n bài?": sinh viên thấy điểm và nhận xét sau khi công bố, thông báo không kèm điểm; bảng Sinh viên, Kết quả (Sẽ công bố; Bỏ qua: chưa chốt điểm); "Chỉ bài đã chốt mới công bố được."; Chốt điểm có xác nhận và bảng kết quả tương tự |
| `GradingWorkspacePage`, `GradingTopBar`, `SubmissionNavList` | `GET`, `PUT /api/v1/evaluations/{id}` | Thanh trên: về Bài nộp, tên bài · lớp, sinh viên (hoặc nhóm) · thứ tự i / N · email, Lượt x / y, nộp lúc · đúng hạn hoặc trễ, nút trước / sau, Lưu nháp, Chốt, Công bố. Cột trái: danh sách bài nộp (bài cá nhân) hoặc danh sách nhóm (bài nhóm) kèm trạng thái chấm |
| `SubmissionViewer` | `GET /api/v1/evaluations/{id}` | Bài làm chỉ đọc; chọn lượt (lượt mới nhất được chấm, các lượt trước để xem). Bài nhóm: thời điểm nộp, trưởng nhóm nộp, bảng Mục, Tác giả, Điểm mục; xem nội dung từng mục |
| `RubricChecklistPanel`, `AdjustmentField`, `FeedbackEditor` | `PUT /api/v1/evaluations/{id}` | Chỉ PENDING/DRAFT hiện hành được sửa; FINALIZED/PUBLISHED khóa rubric, điểm, nhận xét, áp dụng AI và điểm thành viên; không có ô lý do sửa điểm đã chốt. FINALIZED hiện hành vẫn có Công bố điểm. Reload khi đã chốt hoặc đổi receipt lấy lại form từ server; receipt mới xóa điểm nháp/AI selection và tải lại trạng thái đề xuất, tránh áp điểm cũ lên bài mới. Cùng receipt chưa chốt giữ nháp. Lưu draft thành công sang bài kế. |
| `AiProposalPanel` | `POST`, `GET .../ai-proposals` | "Đề xuất AI": nguồn đang chọn và Đổi nguồn (`AiSourcePicker`); các version đề xuất (#n · thời điểm · điểm, tóm tắt); đề xuất cũ ghi "Cũ · bài đã sửa" và không dùng được (`STALE`); "Dùng đề xuất" chỉ điền sẵn rubric và nhận xét; Yêu cầu đề xuất mới; đề xuất do agent MCP ghi mang nhãn Agent |
| `MemberContributionPanel` | `PUT .../members/{accountId}` | "Điểm nhóm và thành viên": Điểm nhóm "x / tổng"; điểm từng thành viên mặc định bằng điểm nhóm, giảng viên sửa từng người, lý do tùy chọn (BR-U09-51) |
| `ActivityTimeline` (U08) | `IActivityQuery` của U08 | "Hoạt động khi làm bài": thời điểm rời trang và thời lượng; "Chỉ để tham khảo; nền tảng không kết luận gian lận." |
| `GradeHistoryPanel` | `GET /api/v1/evaluations/{id}` | "Lịch sử sửa điểm": Lúc (thời điểm · người), Trước → sau, Lý do |
| `ClassGradebookTab`, `GradebookMatrix` | `GET .../gradebook` | Tab Chấm điểm giữ danh sách theo bài/sinh viên và thao tác hàng loạt. Nút Bảng điểm mở `gradebookView=matrix` trong trang lớp: một dòng/sinh viên (STT, tên, email), cột là tên bài và điểm tối đa, lọc một bài/toàn bộ, tìm theo tên/email và phân trang20 hàng. Điểm đã lưu có liên kết xem chi tiết, nhãn truy cập/tooltip trạng thái; giữ0, chưa chấm/chưa nộp hiển thị gạch ngang. Bài nhóm lấy điểm riêng thành viên. Cuộn ngang trong bảng, cố định cột định danh và hàng tiêu đề; Quay lại chấm điểm giữ bộ lọc trước. Không gồm Practice/Quiz, không tự tính trọng số/tổng. |
| `GradebookExportDialog` | `GET .../gradebook/export` | Mở từ ma trận, mặc định phạm vi bài đang xem và định dạng Excel; CSV hoặc XLSX, toàn bộ bài tính điểm hoặc một bài. Xuất mọi sinh viên trong phạm vi bài, kể cả ngoài trang/tìm kiếm hiện tại; tải tệp dạng luồng, không lưu tệp trên máy chủ. |
| `ClassProgressSection` | `GET /api/v1/classes/{classId}/submission-progress`, `GET .../grade-distribution` | Trên tab Thông tin lớp: thẻ Bài đang mở (kèm số bài hết hạn hôm nay), Chờ chấm (bài nộp chưa có điểm); "Tiến độ nộp bài": từng bài (tên, hình thức · chế độ, hạn, số đã nộp / N sinh viên hoặc nhóm, phần trăm), link mở tab Đánh giá; "Phân bố điểm" của một bài đã công bố: số bài theo khoảng điểm (theo phần trăm tổng điểm) |
| `SubmissionsLink` | `GET .../assignments/{assignmentId}/submissions` (tóm tắt) | Cột Bài nộp: "x / N nộp · n chờ chấm", "đã công bố", bài nhóm "x / k nhóm nộp"; bấm mở Submission Detail |
| `StudentGradesSection` | `GET /api/v1/classes/{classId}/students/{accountId}/grades` | Trong popup Student Detail: Đã nộp x / y bài Graded, Nộp trễ n; bảng Bài, Hình thức, Nộp (Đúng hạn, Trễ, Chưa nộp), Điểm ("x / tổng" hoặc Chờ chấm) |
| `GradeDistributionSettings` (slot `AssignmentSettingsSection`) | `GET`, `PUT /api/v1/classes/{classId}/assignments/{assignmentId}/grade-distribution-visibility` | Trong popup Cài đặt bài GRADED của lớp: công tắc "Cho sinh viên xem phổ điểm ẩn danh", mặc định tắt. Đọc `{enabled,version,canEdit}`, ghi `{enabled,version}`; quyền dựa `canEdit`, độc lập với quyền sửa nội dung bài. Bài SUBJECT dùng lớp hiện tại; không có lớp thì không hiện công tắc. Quiz/Practice không hiện; `canEdit=false` ẩn control. Đang lưu khóa thao tác, lỗi giữ trạng thái đã xác nhận, `409` yêu cầu tải lại. Bật chưa đủ mẫu không vẽ histogram |
| `StudentGradeCell`, `StudentGradePanel` | `GET /api/v1/me/grades`, `/me/grades/attempts/{attemptId}`, `/me/grade-distributions` | Chỉ điểm công bố của chính mình, x/tổng, ngày công bố, nhận xét và rubric. Phân bố theo BR-U09-62. Lịch sử gồm thay đổi điểm nháp và dữ liệu legacy; không có thao tác sửa điểm official mới. |

Route `/teaching/*` cho Teacher, Subject Manager (dùng `RoleGuard` của U01). Backend vẫn kiểm phân công lớp thật.

## Defect remediation — 2026-11-02 (KAN-153/154/156)

- Opening and saving an evaluation preserves explicit partial rubric item scores, feedback and existing adjustment/reason. Checkbox clicks explicitly award full/zero points; untouched partial values remain visible. The displayed total includes the retained adjustment and is clamped to the assignment maximum. Legacy checklist-only responses remain compatible.
- Applying an AI proposal fills the local draft with its item scores, general/structured feedback, evidence and review flags; it resets previous acknowledgements instead of clearing structured feedback. A legacy proposal without structured feedback preserves existing explanations for re-review. No automatic save/finalize/publish occurs.
- Flagged items and scores changed after explanation review expose acknowledgement controls. Finalize stays disabled while unresolved reviews exist; server-side review/version checks remain authoritative. Removing a previous acknowledgement by editing points requires a fresh review.
- SUBJECT assignment context loads the existing authorized teaching-class endpoint, requires matching class ID and subject ID, and uses that class's code/name rather than the subject label. CLASS context retains its exact class check. Failed/stale metadata never supplies a heading for another route or hides independently loaded submission rows.

## Grading remediation — 2026-11-10

READY proposals requested in the workspace fill the inline draft. They do not open a proposal popup or trigger the 1.5-second autosave until the teacher edits or explicitly saves. Direct historical proposal links still open the existing review panel without applying anything.

Rubric items allow partial points; structured item explanations and review acknowledgements are editable, together with general feedback and score adjustment. Grade history remains visible, and official grades remain read-only. Saving a group reloads all member versions, including overrides; a receipt changed during reload resets the form instead of carrying old marks forward.

ClassGradebookTab assignment mode exposes GradebookBatchControls with root submission selection and the existing BatchActionDialog. Publish retries only items successfully finalized and retains individual review/score errors. A missing publish result remains a failure. Zero-token estimates display normally.

SubmissionHistoryPage renders approved general/structured feedback and per-item evidence/reasons/suggestions. StudentPublishedFeedback provides group results through the U08 slot in the U12 submitted tab. StudentGradeDistribution renders only the server's allowed anonymous buckets, retaining existing sample/privacy thresholds.


### Feedback formatting follow-up — 2026-11-10

GradingHeaderSection offers explicit Vietnamese sentence-to-paragraph formatting for general plain-text feedback and preserves existing multiline text. Editable summary groups use independent bulleted AutoTextarea entries, max5 per group/500 characters each, with add/remove/disabled states. A lone populated group uses full width; mobile stacks groups. Newlines remain during editing; only blank rows are excluded at the save boundary. Existing historical nextSteps and item/review data remain. No rich-text storage contract or automatic save. See [verification](../../build-and-test/2026-11-10-grading-feedback-format.md).


### Repeated grade actions — 2026-11-10

Gradebook and submission-detail batch selection share raw-state action eligibility: finalize DRAFT, publish DRAFT/FINALIZED, AI PENDING/DRAFT. Known completed grades are omitted and actions with no candidates disable. BatchActionDialog reconciles only INVALID_GRADE_STATE failures using a fresh read, skips already-completed grades, preserves original-version checks and never auto-finalizes an already published or unconfirmed grade. Outcomes are Vietnamese; mixed new/previously completed counts stay separate and result dialogs can close. [Verification](../../build-and-test/2026-11-10-grading-repeat-actions.md).
