"""Smoke-test the real platform MCP flows: authoring and grading, including idempotent replays.

Read-only by default. --write also creates one DRAFT assignment and records one grading proposal; both stay
proposals for human review and never change official grades.

    set PLATFORM_MCP_TOKEN=...   (never printed)
    python scripts/mcp-smoke.py --class-id <uuid> --evaluation-id <uuid> [--write]
"""
import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args):
        return None  # never forward the bearer token to another endpoint


class Mcp:
    def __init__(self, endpoint, token):
        url = urllib.parse.urlsplit(endpoint)
        loopback = url.hostname in ("localhost", "127.0.0.1", "::1")
        if url.scheme not in ("http", "https") or (url.scheme == "http" and not loopback) or url.username \
                or url.query or url.fragment:
            raise SystemExit("Use HTTPS, or HTTP on localhost; do not put credentials in the URL.")
        self.endpoint, self.token, self.next_id = endpoint, token, 0
        self.opener = urllib.request.build_opener(NoRedirect)

    def rpc(self, method, params):
        self.next_id += 1
        body = json.dumps({"jsonrpc": "2.0", "id": self.next_id, "method": method, "params": params}).encode()
        request = urllib.request.Request(self.endpoint, body, {
            "Content-Type": "application/json", "Accept": "application/json, text/event-stream",
            "Authorization": "Bearer " + self.token, "MCP-Protocol-Version": "2025-06-18"})
        try:
            with self.opener.open(request, timeout=60) as response:
                reply = json.loads(response.read())
        except urllib.error.HTTPError as e:
            raise SystemExit(f"MCP {method} failed (HTTP {e.code}). Check endpoint, token expiry and server health.")
        if reply.get("id") != self.next_id or "result" not in reply:
            raise SystemExit(f"MCP {method} returned JSON-RPC error {reply.get('error', {}).get('code')}.")
        return reply["result"]

    def call(self, name, arguments):
        """Returns (isError, parsed content)."""
        result = self.rpc("tools/call", {"name": name, "arguments": arguments})
        text = result["content"][0]["text"]
        return result["isError"], json.loads(text) if text.startswith("{") else text


def check(ok, label):
    print(("PASS " if ok else "FAIL ") + label)
    if not ok:
        check.failed = True
check.failed = False


def smoke_discovery(mcp):
    init = mcp.rpc("initialize", {"protocolVersion": "2025-06-18", "capabilities": {},
                                  "clientInfo": {"name": "platform-mcp-smoke", "version": "1.0.0"}})
    check(init["serverInfo"]["name"] == "ai-learning-platform", "initialize")
    tools = {t["name"] for t in mcp.rpc("tools/list", {})["tools"]}
    check({"create_assignment_draft", "read_grading_context", "record_grading_proposal"} <= tools,
          f"tools/list has authoring and grading tools ({len(tools)} tools)")
    error, caps = mcp.call("get_my_capabilities", {})
    check(not error and caps["role"] in ("TEACHER", "SUBJECT_MANAGER"),
          f"capabilities role={caps.get('role')} access={caps.get('access')}")


def smoke_authoring(mcp, class_id, run):
    args = {"classId": class_id, "title": f"[MCP smoke] {run}", "submissionMode": "GROUP", "gradingMode": "GRADED",
            "requestRef": f"mcp-smoke-assignment-{run}", "agentLabel": "MCP smoke test",
            "draft": {"instructions": "Smoke test draft; safe to delete.", "sections": [
                {"level": 1, "title": "Phần 1", "guidance": "Mô tả ngắn.",
                 "rubric": [{"title": "Tiêu chí", "items": [{"text": "Có nội dung", "points": 10}]}]}]}}
    error, first = mcp.call("create_assignment_draft", args)
    check(not error and first.get("status") == "DRAFT" and first.get("reviewUrl"), "create_assignment_draft")
    error, replay = mcp.call("create_assignment_draft", args)
    check(not error and replay.get("assignmentId") == first.get("assignmentId"), "replay returns the same draft")
    error, conflict = mcp.call("create_assignment_draft", dict(args, title=args["title"] + " changed"))
    check(error and conflict.get("code") == "MCP_REQUEST_CONFLICT", "changed payload with same requestRef conflicts")


def read_all_pages(mcp, evaluation_id):
    args, pages = {"evaluationId": evaluation_id}, []
    while True:
        error, page = mcp.call("read_grading_context", args)
        if error:
            return pages, page
        pages.append(page)
        if page["complete"]:
            return pages, None
        args = page["continuation"]


def build_proposal(context):
    """A full-credit proposal shaped by the context's outputSchema: every rubric item checked and explained."""
    rubrics, block = context["rubricSnapshot"], context["documentBlocks"][0]["id"] if context["documentBlocks"] else None
    items = [(r, i) for r in rubrics for c in r["criteria"] for i in c["items"]]
    return {
        "checks": {r["rubricId"]: [i["itemId"] for c in r["criteria"] for i in c["items"]] for r in rubrics},
        "itemScoresByRubric": {r["rubricId"]: {i["itemId"]: i["points"] for c in r["criteria"] for i in c["items"]}
                               for r in rubrics},
        "score": f"{sum(r['maxPoints'] for r in rubrics):g}",
        "feedback": "MCP smoke test proposal; review or discard.",
        "sections": [{"partId": r["partId"], "feedback": "Smoke test."} for r in rubrics],
        "structuredFeedback": {"schemaVersion": 1, "summary": {"strengths": [], "improvements": [], "nextSteps": []},
                               "items": [{"rubricId": r["rubricId"], "itemId": i["itemId"], "evidence": None,
                                          "reason": "Smoke test.", "suggestion": None, "evidenceBlockId": block,
                                          "requiresReview": True, "reviewReason": "Smoke test proposal"}
                                         for r, i in items]}}


def smoke_grading(mcp, evaluation_id, write, run):
    pages, error = read_all_pages(mcp, evaluation_id)
    check(error is None and pages and len({p["snapshotFingerprint"] for p in pages}) == 1,
          f"read_grading_context reads {len(pages)} page(s) with one fingerprint" + (f" ({error})" if error else ""))
    if error:
        return
    error, batch = mcp.call("read_grading_batch", {"items": [{"evaluationId": evaluation_id}]})
    check(not error and batch["items"][0]["outcome"] in ("OK", "ERROR") and
          batch["items"][0].get("code") in (None, "MCP_BATCH_RESULT_LIMIT"), "read_grading_batch")
    context = pages[0]
    args = {"submissionRef": context["submissionRef"], "expectedVersion": context["expectedVersion"],
            "submissionHash": context["submissionHash"], "snapshotFingerprint": context["snapshotFingerprint"],
            "requestRef": f"mcp-smoke-grade-{run}", "agentLabel": "MCP smoke test", "proposal": build_proposal(context)}
    error, stale = mcp.call("record_grading_proposal", dict(args, snapshotFingerprint="0" * 64))
    check(error and stale.get("code") == "SUBMISSION_CHANGED", "wrong fingerprint is refused before any write")
    if not write:
        return
    error, first = mcp.call("record_grading_proposal", args)
    check(not error and first.get("reviewUrl"), f"record_grading_proposal {first.get('status') if not error else first}")
    error, replay = mcp.call("record_grading_proposal", args)
    check(not error and replay.get("proposalId") == first.get("proposalId"), "replay returns the same proposal")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--endpoint", default="http://localhost:3000/mcp")
    parser.add_argument("--class-id", help="class for the authoring check (needs --write)")
    parser.add_argument("--evaluation-id", help="evaluation (attempt or group document root) for grading checks")
    parser.add_argument("--write", action="store_true", help="also create a draft and record a proposal")
    options = parser.parse_args()
    token = os.environ.get("PLATFORM_MCP_TOKEN", "")
    if not token:
        raise SystemExit("Set PLATFORM_MCP_TOKEN to an MCP agent token (READ_WRITE for --write).")
    sys.stdout.reconfigure(encoding="utf-8")
    mcp, run = Mcp(options.endpoint, token), time.strftime("%Y%m%d-%H%M%S")
    smoke_discovery(mcp)
    if options.class_id and options.write:
        smoke_authoring(mcp, options.class_id, run)
    if options.evaluation_id:
        smoke_grading(mcp, options.evaluation_id, options.write, run)
    sys.exit(1 if check.failed else 0)


if __name__ == "__main__":
    main()
