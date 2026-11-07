# Đồng bộ AI-DLC theo code — 2026-11-07

Yêu cầu: “giúp tôi đồng bộ aidlc khớp với code hiên jtaij”. Đây là chấp thuận sửa tài liệu theo sáu nhóm trong [review](../reverse-engineering/2026-11-07-aidlc-consistency-review.md). Source baseline: `f68a6ec5`. Giữ nguyên application/test/config, public contracts, migration và các báo cáo lịch sử; không triển khai hoặc commit/push.

- [x] Đọc review và đối chiếu source API, inventory, CI/deploy, mail transport, guard và bằng chứng kiểm thử.
- [x] Làm mới API annotation inventory, source/service/public-contract inventory và baseline hiện hành theo source: 49 controller; source sections, service/contract types và 15 unit summaries đã cập nhật.
- [x] Đồng bộ mail/deployment, required-suite guidance, các mục evidence/current gates và wording persistence liên quan. Bổ sung U06 nhiều key/provider, công tắc/reveal/CRUD theo ID và automatic source/retry/fallback; sửa mô tả JwtAuthFilter theo SessionAuthority đã có trong code.
- [x] Kiểm tính đầy đủ của inventory, link tài liệu, các claim cũ còn sót và diff; ghi kết quả vào state/audit và [báo cáo](../../construction/build-and-test/2026-11-07-aidlc-code-sync-results.md). Bộ kiểm độc lập xác nhận 49 controller/334 annotation, 320 controller/migration/test entries và bốn required suites; không đổi application/config/history.

Chỉ kiểm tài liệu và source tĩnh. Không chạy lại application test/build; không coi kết quả trước merge là fresh HEAD acceptance. Security/resiliency scope giữ nguyên, PBT disabled. Kiểm tra Markdown/fence/link trước khi hoàn tất; giữ sơ đồ có sẵn nếu không cần đổi nội dung.
