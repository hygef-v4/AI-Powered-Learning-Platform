# Live AI quiz failure diagnosis

Read-only inspection of the user's running backend terminal, local Docker status, RabbitMQ consumers and selected non-secret database metadata established:

- Failed draft `62a43b57-20c8-4ab7-a71a-88a8b5fb6c3a` belongs to the quiz in the screenshot. Task `59215daa-505f-4118-9a6f-e590373cc690` used PERSONAL / GEMINI / `gemini-3.5-flash-lite`.
- Task dispatch at 15:51:59.293 UTC and failure at 15:52:02.004 UTC recorded provider HTTP 503. This is not the prior missing-source HTTP validation failure.
- Only one eligible ACTIVE AI connection exists for this actor: that personal Gemini model. Its connection test passed at 15:52:13.734 UTC; a passed probe does not establish that quiz generation succeeds.
- The current backend terminal confirms startup with local,worker profiles at 22:46:43 +07:00. It contains no new-code AI task completion event for the failed task.
- A Docker worker created on 2026-10-24 was still running. Both processes consumed the same queues (eight jobs.ai consumers before intervention). The 2.71 second failed task duration cannot include the current worker's 2+5 second transient retry waits. These observations strongly indicate the old worker handled this task; no per-task old-worker completion event was retained to prove ownership directly.

Stopped only the stale `platform-worker-1` container. Verified the local backend remains running and jobs.ai has four consumers, zero ready and zero unacknowledged messages. Other infrastructure containers remain running. No container/data deletion, connection/model change, API key access, schema change, source change, live provider request or UI test performed.

New requests now use the current local worker, including existing bounded retry/fallback logic. The previous failed draft is unchanged and requires a new request. Gemini may still return 503; this intervention does not claim to restore upstream availability.
