# Member contribution levels implementation plan

Goal: lưu 5 mức đóng góp tùy chọn cùng điểm cá nhân, kèm hướng dẫn chấm gọn.
Spec: ../../../inception/requirements/2026-11-13-member-contribution-levels.md.
Execution: trực tiếp trong workspace hiện tại, không subagent. Người dùng đã duyệt
phương án bằng “giúp tôi thử sửa như vậy xem, sau tôi tự test”. Không commit/push.

Architecture: dùng Evaluation JSONB và endpoint lưu thành viên hiện có; mở rộng
DTO đã lọc quyền sinh viên sau công bố. Java/Spring JDBC, React/TypeScript; không
thêm dependency, migration hoặc công thức điểm.

## Constraints and review focus

- Trường contributionLevel nullable; dữ liệu cũ không có trường vẫn đọc được.
- Chỉ có EXCEEDS_EXPECTATIONS, MEETS_EXPECTATIONS, PARTIALLY_MEETS, NEEDS_SUPPORT,
  NOT_COMPLETED; giá trị lạ/kể cả số thứ tự phải bị từ chối.
- Điểm luôn do giảng viên nhập; thay mức không tính lại điểm.
- Giữ RBAC, version, ARCHIVED và bài nộp hiện hành; audit cả thay mức không đổi điểm.
- Giữ mức khi sửa điểm nhóm; nộp lại xóa mức như các đánh giá cá nhân cũ.
- Nhận xét thành viên khôi phục từ adjustmentReason khi có memberOverride;
  không lấy nhầm ghi chú cộng điểm nhóm làm căn cứ cá nhân.
- Sinh viên chỉ đọc mức/căn cứ cá nhân khi PUBLISHED. Không sửa MCP hoặc Excel.
- Security/Resiliency hiện hữu giữ nguyên, PBT disabled. Không gọi AI hay đổi dữ liệu thật.

## Step 1: regression tests

- [x] Frontend: tạo components/MemberContributionPanel.test.tsx: mức mặc định,
  chọn không đổi điểm, payload cùng version/score/reason, dữ liệu tải lại,
  hướng dẫn thu gọn, khóa/lỗi giữ nội dung; bổ sung StudentPublishedFeedback.test.tsx.
- [x] Backend: EvaluationTest giữ/reset mức và điểm; EvaluationMappingTest JSONB
  5 mức/legacy; EvaluationJsonContractTest MemberSave legacy và giá trị sai;
  GradingServiceTest lưu/audit/chống version; GradebookServiceTest ẩn/công bố/sửa.
- [x] Chạy test frontend RED trước mã sản phẩm, ghi kết quả vào .tmp.

## Step 2: backend contract and persistence

- [x] Thêm model/ContributionLevel.java enum dùng tên ở trên; Jackson creator
  nhận chuỗi tên, từ chối số/giá trị khác. Evaluation thêm field nullable.
- [x] GradingCommands.MemberSave thêm contributionLevel và constructor 3 tham số
  giữ caller cũ. saveMemberGrade lưu trường trong transaction/version hiện hữu,
  ghi previousContributionLevel và contributionLevel vào AuditEvent GRADE_CHANGED.
- [x] resetForGroupSubmission xóa mức; inheritGroupGrade không ghi đè mức riêng.
- [x] GradingViews.StudentGradeView thêm contributionLevel, contributionReason;
  mapStudentView chỉ map từ MEMBER, lấy reason từ override; bản nháp vẫn null.

## Step 3: frontend and guide

- [x] model/contributionLevels.ts là nguồn nhãn/mô tả dùng chung teacher/student.
- [x] types.ts mở rộng Evaluation/StudentGrade; api.saveMemberGrade thêm tham số
  cuối optional contributionLevel, gửi cùng lần lưu điểm.
- [x] MemberContributionPanel: Select của shared UI, mặc định Chưa đánh giá,
  state khởi tạo mức/căn cứ đã lưu, hiển thị điểm nhóm, guide details chứa 5 mức,
  các bước chấm và nhắc không tự quy đổi. Không tự tính hệ số.
- [x] StudentPublishedFeedback hiện mức/căn cứ riêng nếu được trả về; không có
  mức cũ vẫn hoạt động như hiện tại.

## Step 4: verify and document

- [x] Chạy test grading backend/frontend liên quan; ESLint và TypeScript.
- [x] Review diff/callers/persistence/security và ghi kết quả thực chạy vào
  construction/build-and-test/2026-11-13-member-contribution-levels.md.
- [x] Cập nhật hướng dẫn U09 và state/audit. Người dùng tự test UI; không restart,
  thao tác điểm thật, commit, push hoặc deploy trong yêu cầu này.
