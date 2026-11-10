# Integration Test Scope

## Confirmed request

- Work on `integration-test`, created from the latest fetched `origin/staging` (`321f88929360a4bf3c733a26a4a0f06a15ecb323`). Branch creation is complete.
- Design automated integration tests using requirements in `aidlc-docs` and knowledge of the implementation to identify integration boundaries and failure paths.
- Reuse suitable existing tests; add missing scenarios rather than duplicate coverage.
- Use isolated test infrastructure and record the actual components under test and any mocked external boundaries.
- Prepare an English report from `Report-5.2_Integration Test_Template.xlsx`, preserving its layout and styling. Keep the source template unchanged.
- Trace report cases to requirement IDs and automated test methods. Record actual execution evidence against a source revision; do not mark unexecuted or skipped cases as passed.
- The template contains sample data and broken sample references. Replace relevant sample content and repair affected references in the output report without redesigning the template.
- Retain the project's existing security/resiliency scope and disabled property-based testing decision.

## Question 1: Phạm vi integration test và báo cáo

Lần này bạn muốn kiểm thử và lập báo cáo cho phạm vi nào?

A) U04, U13, U14 và các điểm tích hợp của ba unit này với các unit/dịch vụ liên quan. Đây là phạm vi tiếp nối báo cáo unit test vừa hoàn thành; không bao gồm kiểm thử độc lập toàn bộ chức năng của các unit khác.

B) Toàn bộ 15 unit của dự án, ưu tiên các điểm tích hợp quan trọng và bổ sung theo từng nhóm. Báo cáo tổng hợp các nhóm đã triển khai và thực thi, đồng thời ghi rõ các phần còn thiếu nếu có.

C) Phạm vi khác (ghi rõ sau thẻ [Answer] bên dưới).

[Answer]: b

## Next steps after scope confirmation

- [x] Verify a clean workspace and fetch the current staging revision.
- [x] Create and switch to the requested `integration-test` branch.
- [x] Inspect existing test infrastructure and the report template.
- [x] Confirm the scope above: user selected B (all 15 units).
- [x] Map requirements and existing integration tests; identify missing integration scenarios.
- [x] Prepare a concrete design and implementation plan under `aidlc-docs` using the existing project workflow.
- [x] Add and execute approved integration tests with isolated dependencies:16 new methods, full backend2,020 passed, no failures/errors/skips.
- [x] Populate and visually verify the English report with real execution evidence:49 representative cases across15 units; original template unchanged.

Question2=A approved implementation. A composed integration suite and traceability-aware required-test guard have been added; fresh final verification and report completion are tracked in the implementation plan. Production source remains unchanged.

## Question 2: Review the concrete design

Thiết kế đã được viết tại [2026-11-08-integration-test-design.md](../application-design/2026-11-08-integration-test-design.md), gồm phạm vi từng unit, thành phần chạy thật/giả lập, thứ tự triển khai, tiêu chí bằng chứng và cách giữ styling của template Excel. Bạn muốn tiếp tục theo thiết kế này như thế nào?

A) Duyệt thiết kế; tiếp tục lập kế hoạch chi tiết và triển khai lần lượt U01–U05, U06–U09, U10–U12, U13–U15, sau đó xác minh và hoàn thiện báo cáo.

B) Yêu cầu sửa thiết kế trước khi triển khai (ghi rõ phần cần sửa sau [Answer]).

C) Khác (ghi rõ sau [Answer]).

[Answer]: a
