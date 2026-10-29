"""Local protocol fixture; no real credentials, provider calls, or application writes."""
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import shutil
import subprocess
import threading
import unittest


class McpDiagnosticTest(unittest.TestCase):
    def run_fixture(self, mode):
        calls = []

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass

            def do_POST(self):
                request = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
                calls.append(request['method'])
                if self.headers.get('Authorization') != 'Bearer fixture-token':
                    self.send_error(401)
                    return
                if mode == 'unauthorized':
                    self.send_error(401, 'fixture-token')
                    return
                result = {'initialize': {'protocolVersion': '2025-06-18',
                                         'serverInfo': {'name': 'ai-learning-platform'}},
                          'ping': {}, 'tools/list': {'tools': [
                              {'name': 'list_classes', 'annotations': {'readOnlyHint': True}}]}}
                response = {'jsonrpc': '2.0', 'id': request['id'],
                            'result': result[request['method']]}
                if mode == 'rpc-error':
                    response = {'jsonrpc': '2.0', 'id': request['id'],
                                'error': {'code': -32601, 'message': 'fixture-token'}}
                if mode == 'wrong-id':
                    response['id'] = 999
                if mode == 'wrong-server':
                    response['result'] = {'protocolVersion': '2025-06-18',
                                          'serverInfo': {'name': 'unityMCP'}}
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps(response).encode())

        server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        script = Path(__file__).with_name('check-mcp.ps1').resolve()
        command = ("$ErrorActionPreference='Stop'; try { & '" + str(script).replace("'", "''")
                   + "' -Endpoint 'http://127.0.0.1:" + str(server.server_port)
                   + "/mcp' -Token (ConvertTo-SecureString 'fixture-token' -AsPlainText -Force)"
                   + " | ConvertTo-Json -Depth 8 } catch { Write-Output $_.Exception.Message; exit 1 }")
        try:
            shell = os.environ.get('MCP_TEST_POWERSHELL') or shutil.which('pwsh') or 'powershell.exe'
            environment = os.environ.copy()
            if Path(shell).name.lower() == 'powershell.exe':
                # Do not inherit PowerShell 7 module paths into Windows PowerShell 5.1.
                environment = {k: v for k, v in environment.items() if k.lower() != 'psmodulepath'}
            process = subprocess.run([shell,
                                      '-NoProfile', '-Command', command],
                                     capture_output=True, text=True, timeout=30, env=environment)
        finally:
            server.shutdown()
            server.server_close()
        self.assertNotIn('fixture-token', process.stdout + process.stderr)
        return process, calls

    def test_success_only_inspects_protocol_and_tools(self):
        process, calls = self.run_fixture('success')
        self.assertEqual(process.returncode, 0, process.stdout + process.stderr)
        self.assertEqual(calls, ['initialize', 'ping', 'tools/list'])
        self.assertEqual(json.loads(process.stdout)['ToolCount'], 1)

    def test_failures_do_not_leak_token_or_claim_success(self):
        for mode in ['unauthorized', 'rpc-error', 'wrong-id', 'wrong-server']:
            with self.subTest(mode=mode):
                process, calls = self.run_fixture(mode)
                self.assertNotEqual(process.returncode, 0)
                self.assertEqual(calls, ['initialize'])


if __name__ == '__main__':
    unittest.main()
