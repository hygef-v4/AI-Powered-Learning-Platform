# Kế hoạch đồng bộ tài liệu AI-DLC theo code hiện tại

**Goal:** Tạo baseline tài liệu AS-BUILT nhất quán với `staging@794a4053`, có truy vết code và phân biệt bằng chứng kiểm thử với yêu cầu/đề xuất.

**Architecture:** Đồng bộ từ ownership và public contracts sang nghiệp vụ, UI, hạ tầng và evidence. Giữ ID FR/UC/story, ghi nhận delta trái yêu cầu; chỉ chỉnh tài liệu trong đợt thực hiện này.

**Tech Stack:** Java 17, Spring Boot 3.5.6, Maven; Next.js 16.3.8, React 19.3.0, TypeScript, npm; PostgreSQL/Flyway, Redis, RabbitMQ, Google Drive, SMTP, Docker Compose, GitHub Actions.

**Spec:** [Báo cáo đối chiếu và bằng chứng D01–D14](2026-10-30-doc-code-drift-audit.md). Kế hoạch đã được người dùng duyệt và thực hiện; checkbox ghi tiến độ thực tế. Đồng bộ source-observed, không phải runtime acceptance.

## Ràng buộc và phương pháp

- Code mô tả hành vi đã xây; không tự phê duyệt nghiệp vụ mới chỉ vì code có hành vi đó. Mỗi delta ghi old requirement, current code, evidence, impact và disposition.
- Không sửa application code, DB, secret/config thực, không chạy migration live; không commit/push/deploy trong scope đồng bộ doc.
- Không xóa hoặc viết lại audit/approvals lịch sử. Giữ tên folder unit hiện có, dùng bảng mapping để tránh rename hàng loạt.
- Mỗi claim có trạng thái: `source-observed`, `historical-test-evidence`, `verified-at-baseline`, `pending` hoặc `proposed`; có ngày/commit và giới hạn khi cần.
- Chỉ nâng evidence thành `verified-at-baseline` sau khi chạy và kiểm kết quả trên đúng source. Số test lịch sử không cộng dồn thành tổng mới.
- Giữ cấu hình extension hiện tại: Security 03/04/05/08/09/12/15; Resiliency 04/06/10; PBT disabled. Lượt plan không thay thiết kế security/infra nên kiểm hiệu lực runtime là N/A; không tuyên bố compliant toàn hệ thống.

## Tiến độ chuẩn bị

- [x] Đọc CLAUDE và rule workflow/continuity/content validation; giữ quyết định extension cũ.
- [x] Chốt baseline Git, inventory tài liệu/code, kiểm tra các nguồn và delta trọng điểm.
- [x] Lập báo cáo D01–D14, mapping 15 unit và kế hoạch theo thứ tự phụ thuộc.
- [x] Người dùng duyệt: “Giúp tôi triển khai theo plan” ngày 2026-10-30.

## Task 1 — Chốt snapshot và truy vết (P0)

**Create:** `aidlc-docs/inception/reverse-engineering/current-code-baseline.md`, `aidlc-docs/inception/reverse-engineering/doc-code-traceability.md`.

**Inputs:** `backend/src/main/java/edu/aiplatform/`, `backend/src/main/resources/db/migration/`, `frontend/src/app/`, `frontend/src/features/`, Git HEAD và working tree.

- [x] Nếu HEAD đã đổi, ghi delta từ `794a4053` và recheck D01–D14 trước khi sửa doc.
- [x] Lập manifest 15 unit: package, contract/interface, controller/route, migrations, UI routes và test paths; phân biệt stub/conditional adapter với provider thật.
- [x] Tạo mapping FR → UC → story → unit → rule → code/API/schema/UI → test/evidence. Unknown để pending, không đoán hoàn thành.
- [x] Phân loại từng item góp ý 29/10; ghi phần đã có code và phần mới đề xuất. Tách module cơ bản khỏi Lesson/RAG; ghi đúng U13 ownership.

**Acceptance:** Mọi D-ID có owner và source path có thật; không thiếu unit hoặc nhầm U16 thành unit hiện hành. Manifest không chứa secret hay kết luận runtime chưa đo.

## Task 2 — Sửa nguồn tổng quan và ownership (P0)

**Modify:** `aidlc-docs/inception/README.md`; `aidlc-docs/inception/application-design/{application-design,components,component-methods,services,component-dependency,unit-of-work,unit-of-work-story-map}.md`; `aidlc-docs/construction/{README,unit-summary}.md`; `aidlc-docs/aidlc-state.md`.

- [x] Hợp nhất U01–U15 theo unit-of-work và Task 1, sửa toàn bộ port provider/consumer và UC ownership tương ứng (D01).
- [x] Giữ ngày khởi tạo Greenfield như lịch sử; thêm current review là brownfield, source có nghiệp vụ, không còn mô tả “chỉ scaffold”.
- [x] Thay current status và Unit Progress bằng snapshot có source/evidence/gate; không dùng tên Payment/Code Lab hoặc U16 trong bảng hiện hành (D02).
- [x] Đối chiếu active UC ở `requirements/use-cases.md` và `docs/usecase_table.md`, chốt 65/66 bằng catalog thay vì replace số hàng loạt; hợp nhất decision register D01–D05 (D13).
- [x] Giữ audit và approvals trước đây; chuyển mô tả cũ sang mục lịch sử rõ baseline.

**Acceptance:** Đọc từ README tới unit-summary/state không gặp ownership hay current-stage mâu thuẫn; các số lượng có nguồn đối chiếu. Global acceptance vẫn pending nếu chưa có evidence.

## Task 3 — Đồng bộ quyền AI/MCP và hợp đồng bảo mật (P0)

**Modify:** `aidlc-docs/inception/requirements/requirements.md`, `user-stories/{stories,personas}.md`; U06 `functional-design/{business-rules,domain-entities,business-logic-model,frontend-components}.md`, NFR/infrastructure/code summary và code plan liên quan.

**Inputs:** `ai/application/{McpCredentialService,AiConnectionService,QuotaPolicyService,AiTaskService}.java`, MCP registry/auth và contracts; staff expansion requirement/results 26/10; AI migrations 27/10.

- [x] Hợp nhất role/tool/action matrix Teacher/Manager/Admin, Student API cá nhân và MCP bị từ chối; bổ sung per-call current role/scope, write proposal/retry/confirmation theo implementation.
- [x] Cập nhật cardinality key chung, lựa chọn key và quota/task/source behavior (D04).
- [x] Cập nhật reveal token: owner/active checks, encryption/hash, audit, token cũ không reveal được, lỗi thiếu key; kiểm UI không đưa secret vào report/log (D05).
- [x] Truy vết từng quyền và lỗi tới test hiện có; không ghi test pass nếu chưa chạy ở baseline.

**Acceptance:** BR-U06-30/31, FR-035, personas/stories và actual credential APIs không mâu thuẫn; có negative cases Student, owner khác, expired/revoked và role đổi.

## Task 4 — Đồng bộ nghiệp vụ và schema của 15 unit (P1)

**Modify:** Mỗi folder unit trong bảng báo cáo: `functional-design/*.md`, `nfr-requirements/*.md`, `nfr-design/*.md`, `infrastructure-design/*.md`, `code/*summary.md`; `inception/requirements/{requirements,use-cases,council-features-2026-10-13,2026-10-29-quiz-assignment-feedback}.md` và stories bị tác động. Chỉ sửa file có delta được xác minh.

- [x] U01–U04: kiểm auth/audit query/stats/Settings/file/backup/subject against service, SettingDefinition và tests; khôi phục handoff U04 từ Git hoặc viết summary có nguồn thay vì giữ raw path mất (D14).
- [x] U05: lớp OPEN thiếu teacher được phép, create vẫn DRAFT; quyền và enrollment/lifecycle/notification không suy diễn từ comment migration (D06).
- [x] U07–U09: đối chiếu editor/copy/sample/rubric, nộp bài/feedback, activity retention 1 ngày, structured AI feedback/review/finalize/publish và gradebook; hợp nhất reports 24–26/10 (D07).
- [x] U10–U11: ghi state machine edit/retire/reopen thực tế, điều kiện publication/snapshot/attempt và trang result; tách góp ý sinh đề theo tag/trừ điểm khỏi hành vi chưa xác minh (D10).
- [x] U12: lý do leader request/reject tùy chọn; rename group, removed-group errors, claims/locks và submission theo actual services; không tự thêm peer grading (D08).
- [x] U13: module entity/repository/cardinality/version/reorder/delete, backfill và quiz attachment; UI permissions U13/U05/U10 (D09).
- [x] U14–U15: đối chiếu recipients/email/SSE/reminders, dashboard counts/activity/backup/partial failures và consumer contracts (D11).
- [x] Với thay đổi trái yêu cầu đã duyệt, thêm decision-needed entry; tài liệu AS-BUILT ghi đúng code, yêu cầu TO-BE giữ dấu vết cho review.

**Acceptance:** Từng delta D06–D11 có rule, schema, flow, UI và negative test tương ứng; không có blanket claim “15 unit hoàn thành”.

## Task 5 — Đồng bộ API, kiến trúc và sơ đồ (P1)

**Modify:** `docs/contracts/openapi/{materials,admin-dashboard,grading,notifications}.yaml` nếu có delta; `aidlc-docs/inception/main-business-flows/README.md` và các BF bị tác động; `application-design/unit-of-work-dependency.drawio`. Đánh giá riêng các diagram ở `docs/`/`uml/` trước khi chọn file hiện hành.

- [x] So inventory controller/DTO với OpenAPI hiện có; thêm module APIs và dashboard `/activity`; kiểm scope/query/version/error/response fields thay vì chỉ thêm URL (D09/D11).
- [x] Liệt kê API unit chưa có OpenAPI và chọn ghi API inventory hoặc tạo spec theo nhu cầu; không mặc định bốn file hiện có bao phủ toàn hệ thống.
- [x] Cập nhật BF-01 (class), BF-02/06 (module/quiz), BF-04/07 (activity/grading), BF-08 (AI/MCP), BF-12 (leader); rà soát BF-03/05/09/10/11 theo Task 4.
- [x] Khi sửa draw.io, sửa cả trang lẻ, file tổng hợp và PNG export; gắn baseline/AS-BUILT hoặc ghi TO-BE delta rõ ràng. Kiểm hình render, không sửa XML rồi bỏ export cũ.
- [x] Kiểm YAML/reference/schema và sự tồn tại của mọi source path; đánh dấu diagram lịch sử chỉ tham khảo.

**Acceptance:** API inventory không bỏ module/activity; diagram đơn, tổng hợp, ảnh xuất và flow text cùng revision, không nhầm AS-BUILT với khảo sát AS-IS.

## Task 6 — Hợp nhất bằng chứng build/test và đóng vòng kiểm tra (P1/P2)

**Modify:** `aidlc-docs/construction/build-and-test/{build-instructions,unit-test-instructions,integration-test-instructions,e2e-test-instructions,security-test-instructions,performance-test-instructions,build-and-test-summary}.md`; state/audit, các index/code summaries liên quan.

- [x] Hợp nhất evidence matrix theo unit/check/commit/date/command/result/skips/log/provider status; chuyển U14 checkout 21/10 và Docker/schema blockers cũ thành lịch sử (D12).
- [x] So hướng dẫn với `.github/workflows/ci.yml`, `scripts/assert-required-backend-tests.ps1`, package scripts, Maven và Compose; ghi rõ deploy job đang disabled.
- [x] Không chạy lại application suites chỉ để thay câu chữ. Nếu cần chứng nhận test tại baseline, chạy trong môi trường phù hợp: backend `mvn -B verify`, DB guard sau verify; frontend lint/typecheck/test/build theo CI, tuần tự khi máy hạn chế. Ghi failures/skips đúng thực tế.
- [x] Kiểm relative links và cả raw path trong Markdown, duplicate ownership, số UC/story/unit, code fences/YAML, broken references và stale status. Không xóa audit/history để hết kết quả tìm kiếm.
- [x] Kiểm diff bảo đảm chỉ tài liệu; cập nhật checkbox ngay khi xong từng bước, state giữ pending gates có bằng chứng thiếu.
- [x] Viết summary đợt sync: file thay đổi, D-ID đã đóng, quyết định cần duyệt, evidence còn pending và baseline cuối.

**Acceptance:** Không còn current snapshot tự mâu thuẫn D01–D14; mọi claim hoàn thành có evidence phù hợp. Link thiếu được sửa hoặc ghi lịch sử xác minh được; application source/DB không thay đổi.

## Thứ tự thực hiện và bàn giao

Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6. Làm trực tiếp từng nhóm, review diff sau mỗi nhóm. Task 2/3 ưu tiên vì ownership và quyền sai sẽ làm lan sai sang các doc còn lại.

Đợt lập kế hoạch ban đầu chỉ thực hiện workspace detection, đối chiếu brownfield và lập kế hoạch; người dùng đã duyệt thực thi và các bước sync hoàn tất ngày 30/10. Không mở một chu kỳ thiết kế/tạo code mới, không reset lifecycle/approval lịch sử. Việc viết lại các doc hiện hành bắt đầu sau khi kế hoạch được review; có thể điều chỉnh hoặc thêm phạm vi khi duyệt theo `CLAUDE.md`, mục Workflow Planning.
