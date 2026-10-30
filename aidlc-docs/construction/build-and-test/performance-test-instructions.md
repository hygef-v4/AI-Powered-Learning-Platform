# Performance and resilience verification

No performance/capacity/latency run was performed in the 2026-10-30 documentation synchronization. Existing NFR targets remain targets. Timeouts, isolated unit tests and source presence are not measured compliance. Applicable resiliency scope remains RESILIENCY-04/06/10.

Use isolated representative data; record commit/config/topology, concurrency/cardinality, warmup/duration/sample count, p50/p95/p99, errors, DB/worker pool/queue metrics and provider conditions. Evaluate source-scoped targets in each unit NFR document before accepting numbers.

Priority scenarios: module list/Quiz visibility and reorder CAS; dashboard activity independent timeout/UNAVAILABLE; AI quota/idempotency/batch partial outcomes and continuation; notification fanout/SSE/email retry; submission/deadline scanners and activity one-day retention; DB backup/isolated restore. Keep unknown/unavailable data visible; do not replace it with zero.

The older U14-specific procedure and targets are preserved in [historical instructions](../../history/build-and-test-before-2026-10-30/performance-test-instructions.md); validate target applicability before reusing them across all units. CI deployment is disabled; this document does not enable it.
