# Performance test instructions — U14

Status: not executed; no load harness or latency evidence is claimed. Use real providers and isolated infrastructure after [integration prerequisites](integration-test-instructions.md) pass. Timeouts and unit tests do not prove NFR compliance.

| Measurement | Acceptance target |
|---|---|
| Notification delivery and UI/SSE update | Within5s of committed source event |
| Announcement fanout |200students within5s |
| Feed query |p95≤300ms, pages≤50 |
| Deadline scanner |Reminder within5min of the24h-before-close point |
| Retention |Only older-than180day notifications;≤10000 per batch |

An independent tester should prepare distinct student sessions, realistic announcement/notification histories, multiple OPEN classes and500+assignment/class cardinalities. Record dataset sizes, indexes, instance/worker counts, CPU/RAM, database/broker versions and network topology. Use fixed fixtures and a stable workload for comparable reruns.

Suggested measurement protocol (test design, not a new requirement): warm up2minutes; measure10minutes at the agreed concurrent workload; repeat with200recipient fanout and two workers; then increase load in controlled steps until queue lag/errors appear. Record actual concurrency, request rate, sample count, p50/p95/p99, failures, event-to-SSE delay, queue depth, pool use and lock wait. Schedule reminder cases around the real deadline window; accelerated-clock results alone cannot certify wall-clock delivery.

Exercise DB lock contention, slow provider and SMTP timeout under load. Verify bounded cancellation/resource release, no cap overshoot and no loss after recovery. Scanner batching must eventually cover all eligible rows;500limits alone are insufficient evidence. Retain sanitized raw measurements and compare each target separately. No throughput/error-rate target is invented here; agree unspecified capacity criteria before claiming acceptance.

No k6/JMeter dependency or nonexistent script is introduced. Tester-owned harness and reproducible command/version must accompany the actual report.

## MCP staff expansion — 2026-10-26

See [MCP staff expansion results](2026-10-26-mcp-staff-expansion-results.md) for exact commands, test counts, the isolated PostgreSQL migration/retry checks, limits and [manual acceptance](2026-10-26-mcp-staff-expansion-manual-test-guide.md). Bounded protocol/batch checks are verified; load/performance certification and new live-account acceptance are pending. No live stack migration is part of automated verification.
