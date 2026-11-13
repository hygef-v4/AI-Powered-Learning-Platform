# U11 Quiz Practice — domain-entities

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Thiết kế độc lập công nghệ.

## 1. Tổng quan

| Entity | Loại | Lưu ở |
|---|---|---|
| `QuizAttempt` | Aggregate root, một dòng là một lượt làm Quiz | `quiz_attempts` |

U11 không sở hữu: Quiz, câu, đáp án (U10), dữ liệu hoạt động (U08), tài liệu (U13).

## 2. `QuizAttempt`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | Khóa |
| `quizId`, `classId`, `accountId` | UUID | Quiz (một version), lớp người học làm, người học |
| `attemptNo` | số | Duy nhất theo (`quizId`, `accountId`) |
| `snapshot` | JSON | `versionNo`, cài đặt, chính sách theo dõi, seed, thứ tự câu và đáp án |
| `answers` | JSON | Câu → danh sách lựa chọn |
| `answerVersion` | số | Tăng mỗi lần lưu |
| `status` | enum | `IN_PROGRESS`, `SUBMITTED` |
| `submitMode` | enum | `MANUAL`, `AUTO_TIME_LIMIT`, `AUTO_RETIRED` |
| `startedAt`, `deadlineAt`, `lastSavedAt`, `submittedAt` | thời gian | `deadlineAt` trống khi Quiz không giới hạn giờ |
| `score`, `maxScore`, `correctCount` | số | Có sau khi nộp |
| `result` | JSON | Đúng sai, điểm từng câu |

```mermaid
stateDiagram-v2
    [*] --> IN_PROGRESS: Bấm Làm Quiz
    IN_PROGRESS --> SUBMITTED: Người học nộp
    IN_PROGRESS --> SUBMITTED: Tự nộp khi hết giờ hoặc Quiz bị ngưng
    SUBMITTED --> [*]
```

**Text alternative**: Bấm Làm Quiz tạo lượt `IN_PROGRESS`. Người học nộp, hoặc hệ thống tự nộp khi hết giờ hay khi Quiz bị ngưng hoặc có version mới, thì lượt sang `SUBMITTED`, được chấm ngay và không đổi nữa; làm lại là tạo lượt mới nếu còn lượt.

## 3. Contract

### Interface U11 cung cấp

| Interface | Phương thức | Dùng bởi | Dùng để |
|---|---|---|---|
| `IQuizLifecycleListener` (contract của U10, U11 cài) | `onRetired(quizId)` | U10 | Tự nộp và chấm lượt đang làm khi Quiz bị ngưng hoặc có version mới |
| Slot `QuizDetailSection` (của U10, U11 cài) | - | U10 | Phần Lượt làm và hoạt động cho giảng viên trên Quiz Detail |
| `quizDetailSections`, `quizRowActions` (contract của U10, U11 cài) | - | U10 | Tab Lượt làm của sinh viên trên Quiz Detail; số sinh viên đã làm trên dòng Quiz. Không còn cài `NavEntry` Luyện tập Quiz (yêu cầu 29/10) |
| Slot `QuizRowAction` (của U10, U11 cài) | - | U10 | Số sinh viên đã làm trên dòng Quiz ở tab Đánh giá |
| `IScheduledScanner` (contract của U03, U11 cài) | - | U03 | Tự nộp khi hết giờ |

### Contract U11 khai báo (unit khác cài)

| Contract | Unit cài | Dùng để |
|---|---|---|
| (không còn) Slot frontend `QuizMaterialSection` | U13 | Đã bỏ: Quiz không gắn tài liệu (BR-U10-11). Quiz Practice Detail chỉ hiện bài trước, bài sau trong module qua `ModuleNeighbours` của U13 |

### Interface U11 dùng

| Interface | Unit | Dùng để |
|---|---|---|
| `IAuthorization` | U01 | Kiểm R5, R3/R4 |
| `IAudit` | U02 | Audit |
| `IClassAccess` | U05 | Ghi danh, lớp đang mở của người học |
| `IActivityWrite`, `IActivityQuery`; `useActivityTracker`, `ActivityTimeline` | U08 | Ghi và xem dữ liệu rời trang của lượt Quiz |
| `IQuizQuery`, `IQuizScoring` | U10 | Quiz, đề không có đáp án, chấm, đáp án sau khi nộp |

## 4. API

| API | UC |
|---|---|
| `GET /api/v1/me/quizzes`, `GET /api/v1/me/quizzes/{quizId}` | 49, 50 |
| `POST /api/v1/quizzes/{quizId}/attempts`, `GET /api/v1/quiz-attempts/{attemptId}`, `PUT .../answers`, `POST .../submit`, `POST .../activity` | 51 |
| `GET /api/v1/quiz-attempts/{attemptId}/result` | 52 |
| `GET /api/v1/classes/{classId}/quizzes/{quizId}/attempts`, `GET /api/v1/classes/{classId}/quiz-attempts/{attemptId}` | 42 |

## Delta 2026-11-01

`AttemptSnapshot` thêm `questionPoints` (Quiz sinh đề; lượt cũ trống). `QuizAttempt` thêm `ActivityOutcome`: `rawScore`, `suggestedPenalty`, `penaltyPoints` (đã trừ), `leaveCount`, `signal`, `decision` (`APPLIED`, `DISMISSED`), người quyết định, lúc quyết định, lý do. Migration `quizpractice/V20261101_1210__quiz_attempt_activity_penalty.sql`. Lượt cũ để trống, không có gợi ý.
