---
name: author-assignment
description: Use when a teacher or subject manager asks to create or revise an assignment through the AI learning platform MCP.
---

# Use the server-managed workflow

This local skill is optional. The complete workflow is supplied by the connected platform, not copied into this package.

1. Call `get_my_capabilities` and verify that `read_agent_workflow` is available for the current teaching role.
2. Call `read_agent_workflow` with `{"workflow":"AUTHOR_ASSIGNMENT"}`.
3. Follow the returned `instructions` for this task, using current domain tool schemas and the returned workflow `version`. Return the exact reviewUrl for human review; never publish official grades.

If the workflow tool is missing or fails, report the problem and ask to refresh the MCP tool catalog or update the server. Do not fall back to a stale local copy, claim successful work or install anything automatically.
