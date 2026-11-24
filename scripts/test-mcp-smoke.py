"""Offline checks for mcp-smoke.py: endpoint guard, no redirect, token never printed, proposal shape."""
import importlib.util
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import threading
import unittest

spec = importlib.util.spec_from_file_location("mcp_smoke", Path(__file__).with_name("mcp-smoke.py"))
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)


class McpSmokeTest(unittest.TestCase):
    def test_rejects_remote_http_and_credentials_in_url(self):
        for url in ["http://example.com/mcp", "https://user:pw@example.com/mcp", "http://localhost/mcp?token=x"]:
            with self.subTest(url=url), self.assertRaises(SystemExit):
                smoke.Mcp(url, "fixture-token")

    def test_redirect_is_not_followed_and_token_not_reported(self):
        class Redirect(BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass

            def do_POST(self):
                self.send_response(307)
                self.send_header("Location", "http://127.0.0.1:1/steal")
                self.end_headers()

        server = ThreadingHTTPServer(("127.0.0.1", 0), Redirect)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        try:
            with self.assertRaises(SystemExit) as failure:
                smoke.Mcp(f"http://127.0.0.1:{server.server_port}/mcp", "fixture-token").rpc("ping", {})
        finally:
            server.shutdown()
            server.server_close()
        self.assertIn("HTTP 307", str(failure.exception))
        self.assertNotIn("fixture-token", str(failure.exception))

    def test_proposal_covers_every_rubric_item(self):
        context = {"documentBlocks": [{"id": "b1"}], "rubricSnapshot": [
            {"rubricId": "r1", "partId": "p1", "maxPoints": 3.0, "criteria": [
                {"items": [{"itemId": "i1", "points": 1.0}, {"itemId": "i2", "points": 2.0}]}]}]}
        proposal = smoke.build_proposal(context)
        self.assertEqual(proposal["score"], "3")
        self.assertEqual(proposal["checks"], {"r1": ["i1", "i2"]})
        self.assertEqual([i["itemId"] for i in proposal["structuredFeedback"]["items"]], ["i1", "i2"])


if __name__ == "__main__":
    unittest.main()
