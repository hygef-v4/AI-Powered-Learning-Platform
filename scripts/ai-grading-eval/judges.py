import asyncio
import json
import os
import re
import shutil
import subprocess
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Protocol

from pydantic import BaseModel, ValidationError


PROVIDERS = ("codex", "claude", "gemini", "opencode", "ollama")
HOSTED = frozenset(PROVIDERS) - {"ollama"}
DEFAULT_MODELS = {
    "codex": "gpt-6.2-sol",
    "claude": "sonnet",
    "ollama": "qwen3:4b-instruct-2507-q4_K_M",
}
SYSTEM_ENV = {
    "SYSTEMROOT", "WINDIR", "SYSTEMDRIVE", "COMSPEC", "PATHEXT", "PATH", "PROGRAMDATA",
    "PROGRAMFILES", "PROGRAMFILES(X86)", "USERPROFILE", "HOMEDRIVE", "HOMEPATH", "HOME",
    "APPDATA", "LOCALAPPDATA", "TEMP", "TMP", "USERNAME", "OS", "CODEX_HOME", "CLAUDE_CONFIG_DIR",
}


class JudgeError(RuntimeError):
    def __init__(self, code: str, message: str):
        self.code = code
        super().__init__(f"{code}: {message}")


@dataclass(frozen=True)
class JudgeInfo:
    provider: str
    installed: bool
    ready: bool
    detail: str
    model: str | None = None
    version: str | None = None
    executable: str | None = None


class Judge(Protocol):
    def generate(self, prompt: str, schema: type[BaseModel] | None = None): ...
    async def async_generate(self, prompt: str, schema: type[BaseModel] | None = None): ...
    def get_model_name(self) -> str: ...
    def check_authentication(self) -> str: ...
    def health_check(self) -> JudgeInfo: ...
    def supports_structured_output(self) -> bool: ...


def clean_environment() -> dict[str, str]:
    # Keep the OS and CLI login locations. API keys, upstream overrides, and agent hooks are excluded.
    return {key: value for key, value in os.environ.items() if key.upper() in SYSTEM_ENV}


def classify_error(text: str) -> JudgeError:
    text = text.lower()
    if "invalid_json_schema" in text:
        return JudgeError("JUDGE_CONFIG", "Provider rejected the output schema")
    if "error_max_turns" in text or "reached maximum number of turns" in text:
        return JudgeError("JUDGE_TURN_LIMIT", "CLI reached its per-invocation turn limit")
    if re.search(r"insufficient_quota|rate.limit|usage.limit|out of credits|\b429\b|\b402\b|billing", text):
        return JudgeError("JUDGE_QUOTA_OR_BILLING", "Judge quota or billing gate reached")
    if re.search(r"not logged in|unauthori[sz]ed|invalid api key|authentication|\b401\b|\b403\b", text):
        return JudgeError("JUDGE_AUTH", "CLI authentication or provider access failed")
    if re.search(r"\b503\b|service unavailable|overloaded", text):
        return JudgeError("JUDGE_UNAVAILABLE", "Judge service unavailable")
    return JudgeError("JUDGE_PROCESS", "CLI exited unsuccessfully. Check authentication and model availability.")


def run_cli(args: list[str], cwd: Path, timeout: float, prompt: str = "", *,
            extra_env: dict[str, str] | None = None) -> subprocess.CompletedProcess:
    try:
        return subprocess.run(
            args, input=prompt, capture_output=True, encoding="utf-8", errors="replace",
            shell=False, timeout=timeout, cwd=cwd, env={**clean_environment(), **(extra_env or {})},
        )
    except subprocess.TimeoutExpired as error:
        raise JudgeError("JUDGE_TIMEOUT", f"CLI exceeded {timeout:g} seconds") from error
    except OSError as error:
        raise JudgeError("JUDGE_PROCESS", "Unable to start CLI executable") from error


def parse_answer(answer: str, schema: type[BaseModel] | None):
    if not isinstance(answer, str) or not answer.strip():
        raise JudgeError("JUDGE_SCHEMA", "Judge returned an empty response")
    if schema is None:
        return answer
    try:
        return schema.model_validate_json(answer, strict=True)
    except ValidationError as error:
        raise JudgeError("JUDGE_SCHEMA", "Judge response does not match the requested JSON schema") from error


def codex_schema(schema: type[BaseModel]) -> dict:
    def close_properties(node):
        if isinstance(node, list):
            for child in node:
                close_properties(child)
        elif isinstance(node, dict):
            if node.get("type") == "object":
                if node.get("additionalProperties") not in (None, False):
                    raise JudgeError("JUDGE_CONFIG", "Codex strict output does not support open objects")
                node["additionalProperties"] = False
                node["required"] = list(node.get("properties", {}))
            node.pop("default", None)
            for key in ("$defs", "properties"):
                for child in node.get(key, {}).values():
                    close_properties(child)
            for key in ("items", "anyOf", "oneOf", "allOf", "prefixItems"):
                close_properties(node.get(key))

    result = schema.model_json_schema()
    close_properties(result)
    return result


def select_judge(requested: str, order: tuple[str, ...], allowed_hosted: set[str], probe) -> JudgeInfo:
    candidates = order if requested == "auto" else (requested,)
    reasons = []
    for provider in candidates:
        if provider in HOSTED and provider not in allowed_hosted:
            reasons.append(f"{provider}: hosted provider has no explicit opt-in")
            continue
        info = probe(provider)
        if info.ready:
            return info
        reasons.append(f"{provider}: {info.detail}")
    raise JudgeError("JUDGE_UNAVAILABLE", "; ".join(reasons) or "No usable judge found")


def check_cli_auth(provider: str, executable: str, cwd: Path) -> str:
    if provider == "codex":
        check = run_cli([executable, "login", "status"], cwd, 10)
        if check.returncode == 0 and "logged in using chatgpt" in (check.stdout + check.stderr).lower():
            return "ChatGPT login"
    if provider == "claude":
        check = run_cli([executable, "auth", "status", "--json"], cwd, 10)
        try:
            auth = json.loads(check.stdout)
        except ValueError:
            auth = {}
        if check.returncode == 0 and auth.get("loggedIn") and (
            auth.get("authMethod") == "claude.ai" and auth.get("apiProvider") == "firstParty"
            and auth.get("subscriptionType") in {"pro", "max", "team", "enterprise", "ultra"}
        ):
            return "Claude subscription login"
    raise JudgeError("JUDGE_AUTH", "A supported subscription login could not be verified")


def local_model(model: str) -> dict:
    if re.search(r"(?:^|[:-])cloud(?:$|[:-])", model, re.IGNORECASE):
        raise JudgeError("JUDGE_CONFIG", "Ollama cloud model tags are disabled")
    from ollama import Client

    try:
        response = Client(host="http://127.0.0.1:11434", timeout=5, trust_env=False).list()
    except Exception as error:
        raise JudgeError("JUDGE_UNAVAILABLE", "Ollama server is unreachable on localhost") from error
    for entry in response.model_dump().get("models", []):
        if entry.get("model") == model and (entry.get("size") or 0) > 0 and not (
            entry.get("remote_host") or entry.get("remote_model")
        ):
            return entry
    raise JudgeError("JUDGE_UNAVAILABLE", "The requested Ollama model is not installed locally")


def probe_judge(provider: str, model: str | None = None) -> JudgeInfo:
    model = model or DEFAULT_MODELS.get(provider)
    executable = shutil.which(provider)
    if provider == "ollama":
        try:
            entry = local_model(model)
            return JudgeInfo(provider, True, True, "Local model installed", model, entry.get("digest"), executable)
        except JudgeError as error:
            return JudgeInfo(provider, bool(executable), False, str(error), model)
    if not executable:
        return JudgeInfo(provider, False, False, "CLI not installed", model)
    if provider == "gemini":
        try:
            launcher = gemini_launcher(executable)
            with tempfile.TemporaryDirectory(prefix="u09-judge-") as folder:
                version_check = run_cli([*launcher, "--version"], Path(folder), 8)
                help_check = run_cli([*launcher, "--help"], Path(folder), 8)
            flags = ("--admin-policy", "--allowed-mcp-server-names", "--extensions", "--output-format")
            if version_check.returncode or help_check.returncode or not all(flag in help_check.stdout for flag in flags):
                raise JudgeError("JUDGE_CONFIG", "CLI lacks the required isolation options")
            if not model:
                raise JudgeError("JUDGE_CONFIG", "Choose an explicit Gemini model")
            return JudgeInfo(provider, True, True, "Cached personal Google OAuth configured", model,
                             version_check.stdout.strip(), executable)
        except JudgeError as error:
            return JudgeInfo(provider, True, False, str(error), model)
    if provider == "opencode":
        # shortcut: enable once isolated config and effective non-key upstream are both verifiable.
        return JudgeInfo(provider, True, False, "Effective auth and tool isolation are unverified", model)
    if Path(executable).suffix.lower() not in {"", ".exe"}:
        return JudgeInfo(provider, True, False, "A native executable is required", model)
    try:
        with tempfile.TemporaryDirectory(prefix="u09-judge-") as folder:
            cwd = Path(folder)
            version_check = run_cli([executable, "--version"], cwd, 8)
            help_args = [executable, "exec", "--help"] if provider == "codex" else [executable, "--help"]
            help_check = run_cli(help_args, cwd, 8)
            flags = (
                ("--ignore-user-config", "--sandbox", "--ephemeral", "--output-schema")
                if provider == "codex" else ("--tools", "--json-schema", "--strict-mcp-config", "--no-session-persistence")
            )
            if version_check.returncode or help_check.returncode or not all(flag in help_check.stdout for flag in flags):
                raise JudgeError("JUDGE_CONFIG", "CLI lacks the required headless safety or schema flags")
            details = check_cli_auth(provider, executable, cwd)
        return JudgeInfo(provider, True, True, details, model, version_check.stdout.strip(), executable)
    except JudgeError as error:
        return JudgeInfo(provider, True, False, str(error), model, executable=executable)


class CliJudge:
    def __init__(self, info: JudgeInfo, timeout: float, max_calls: int):
        self.info, self.timeout, self.max_calls = info, timeout, max_calls
        self.call_count = 0
        self.reported_models = set()
        self.blocked: JudgeError | None = None

    def get_model_name(self) -> str:
        return self.info.model

    def health_check(self) -> JudgeInfo:
        return self.info

    def check_authentication(self) -> str:
        return self.info.detail

    def supports_structured_output(self) -> bool:
        return True

    def generate(self, prompt: str, schema: type[BaseModel] | None = None):
        if self.blocked:
            raise self.blocked
        for attempt in range(2):
            if self.call_count >= self.max_calls:
                raise JudgeError("JUDGE_CALL_BUDGET", "Configured judge invocation limit reached")
            self.call_count += 1
            try:
                with tempfile.TemporaryDirectory(prefix="u09-judge-") as folder:
                    answer = self.invoke(prompt, schema, Path(folder))
                return parse_answer(answer, schema)
            except JudgeError as error:
                if error.code in {"JUDGE_AUTH", "JUDGE_CONFIG", "JUDGE_QUOTA_OR_BILLING", "JUDGE_UNSAFE_TOOL"}:
                    self.blocked = error
                if error.code != "JUDGE_SCHEMA" or attempt:
                    raise

    async def async_generate(self, prompt: str, schema: type[BaseModel] | None = None):
        return await asyncio.to_thread(self.generate, prompt, schema)

    def invoke(self, prompt: str, schema: type[BaseModel] | None, cwd: Path) -> str:
        raise NotImplementedError


class CodexJudge(CliJudge):
    def invoke(self, prompt, schema, cwd):
        args = [
            self.info.executable, "-a", "never", "exec", "--ignore-user-config",
            "--sandbox", "read-only", "--ephemeral", "--skip-git-repo-check",
            "--disable", "shell_tool", "--disable", "multi_agent", "--disable", "multi_agent_v2",
            "--disable", "plugins", "-c", 'web_search="disabled"', "-c", "mcp_servers={}",
            "--model", self.info.model, "--json", "--output-last-message", "answer.txt",
        ]
        if schema is not None:
            (cwd / "schema.json").write_text(json.dumps(codex_schema(schema)), encoding="utf-8")
            args += ["--output-schema", "schema.json"]
        proc = run_cli([*args, "-"], cwd, self.timeout, prompt)
        if proc.returncode:
            raise classify_error(proc.stderr + proc.stdout)
        for line in proc.stdout.splitlines():
            try:
                event = json.loads(line)
            except ValueError:
                continue
            item = event.get("item", {})
            if item.get("type") in {"command_execution", "mcp_tool_call", "web_search", "file_change", "collab_tool_call"}:
                raise JudgeError("JUDGE_UNSAFE_TOOL", "Codex attempted a forbidden tool action")
        try:
            return (cwd / "answer.txt").read_text(encoding="utf-8")
        except OSError as error:
            raise JudgeError("JUDGE_SCHEMA", "Codex did not write a final response") from error


class ClaudeJudge(CliJudge):
    def invoke(self, prompt, schema, cwd):
        (cwd / "mcp.json").write_text('{"mcpServers":{}}', encoding="utf-8")
        (cwd / "settings.json").write_text(
            '{"disableAllHooks":true,"disableClaudeAiConnectors":true}', encoding="utf-8"
        )
        args = [
            self.info.executable, "-p", "--output-format", "json", "--model", self.info.model,
            "--tools", "", "--disallowedTools", "mcp__*", "--disable-slash-commands",
            "--strict-mcp-config", "--mcp-config", "mcp.json",
            "--setting-sources", "", "--settings", "settings.json", "--no-session-persistence", "--max-turns", "3",
        ]
        if schema is not None:
            args += ["--json-schema", json.dumps(schema.model_json_schema())]
        proc = run_cli(args, cwd, self.timeout, prompt)
        if proc.returncode:
            raise classify_error(proc.stderr + proc.stdout)
        try:
            payload = json.loads(proc.stdout)
            if payload.get("is_error"):
                raise classify_error(proc.stdout)
            self.reported_models.update(payload.get("modelUsage", {}).keys())
            if schema is not None:
                return json.dumps(payload["structured_output"], ensure_ascii=False)
            return payload["result"]
        except (ValueError, KeyError, TypeError) as error:
            raise JudgeError("JUDGE_SCHEMA", "Claude did not return the expected JSON envelope") from error


def gemini_launcher(executable: str) -> list[str]:
    if os.name != "nt":
        raise JudgeError("JUDGE_CONFIG", "Gemini judge configuration is verified for Windows only")
    system = Path(os.environ.get("PROGRAMDATA", "C:/ProgramData")) / "gemini-cli"
    if os.getenv("GEMINI_CLI_SYSTEM_SETTINGS_PATH") or any(
        (system / filename).exists() for filename in ("settings.json", "system-defaults.json")
    ) or any((system / "policies").glob("*.toml")):
        raise JudgeError("JUDGE_CONFIG", "Managed Gemini configuration prevents judge isolation")
    config = Path.home() / ".gemini" / "settings.json"
    try:
        selected_auth = json.loads(config.read_text(encoding="utf-8-sig"))["security"]["auth"]["selectedType"]
    except (OSError, ValueError, KeyError, TypeError):
        selected_auth = None
    if selected_auth != "oauth-personal":
        raise JudgeError("JUDGE_AUTH", "Gemini personal Google OAuth is not configured")
    # Run the official npm entrypoint directly. Student text is never passed through a batch shim.
    entry = Path(executable).parent / "node_modules" / "@google" / "gemini-cli" / "bundle" / "gemini.js"
    node = shutil.which("node.exe")
    if Path(executable).suffix.lower() != ".cmd" or not node or not entry.is_file():
        raise JudgeError("JUDGE_CONFIG", "Expected the official Gemini npm CLI and a native Node.js executable")
    return [node, str(entry)]


def parse_gemini_response(stdout: str, model: str) -> str:
    try:
        payload = json.loads(stdout)
        if payload.get("error"):
            raise classify_error(json.dumps(payload["error"]))
        stats = payload["stats"]
        if stats["tools"]["totalCalls"]:
            raise JudgeError("JUDGE_UNSAFE_TOOL", "Gemini attempted a forbidden tool action")
        if set(stats["models"]) != {model}:
            raise JudgeError("JUDGE_CONFIG", "Reported Gemini model differs from the requested model")
        return payload["response"]
    except (ValueError, KeyError, TypeError, AttributeError) as error:
        raise JudgeError("JUDGE_SCHEMA", "Gemini did not return a verifiable JSON envelope") from error


class GeminiJudge(CliJudge):
    def supports_structured_output(self) -> bool:
        return False

    def invoke(self, prompt, schema, cwd):
        launcher = gemini_launcher(self.info.executable)
        settings = cwd / "settings.json"
        policy = cwd / "deny-tools.toml"
        no_server = cwd.name
        settings.write_text(json.dumps({
            "hooksConfig": {"enabled": False},
            "security": {"disableYoloMode": True, "auth": {"enforcedType": "oauth-personal"}},
            "advanced": {"ignoreLocalEnv": True},
            "context": {"fileName": f"{cwd.name}.md"},
            "mcp": {"allowed": [no_server]},
            "telemetry": {"enabled": False},
            "general": {"enableAutoUpdate": False},
        }), encoding="utf-8")
        policy.write_text('[[rule]]\ntoolName = "*"\ndecision = "deny"\npriority = 999\n', encoding="utf-8")
        args = [*launcher, "--model", self.info.model, "--output-format", "json",
                "--approval-mode", "plan", "--admin-policy", str(policy),
                "--extensions", "none", "--allowed-mcp-server-names", no_server]
        if schema is not None:
            contract = json.dumps(schema.model_json_schema(), ensure_ascii=False)
            prompt = f"Return only a JSON document matching this schema:\n{contract}\n\n{prompt}"
        proc = run_cli(args, cwd, self.timeout, prompt,
                       extra_env={"GEMINI_CLI_SYSTEM_SETTINGS_PATH": str(settings)})
        if proc.returncode:
            raise classify_error(proc.stderr + proc.stdout)
        answer = parse_gemini_response(proc.stdout, self.info.model)
        self.reported_models.add(self.info.model)
        return answer


class OllamaJudge(CliJudge):
    def invoke(self, prompt, schema, cwd):
        from ollama import Client

        try:
            response = Client(host="http://127.0.0.1:11434", timeout=self.timeout, trust_env=False).chat(
                model=self.info.model, messages=[{"role": "user", "content": prompt}],
                format=schema.model_json_schema() if schema else None,
                options={"temperature": 0, "num_ctx": 16384},
            )
        except Exception as error:
            raise JudgeError("JUDGE_OLLAMA", f"Local inference failed ({type(error).__name__})") from error
        return response.message.content


def create_judge(info: JudgeInfo, timeout: float, max_calls: int) -> Judge:
    adapter = {"codex": CodexJudge, "claude": ClaudeJudge, "gemini": GeminiJudge, "ollama": OllamaJudge}[info.provider]
    return adapter(info, timeout, max_calls)


def deepeval_model(judge: Judge):
    os.environ["DEEPEVAL_TELEMETRY_OPT_OUT"] = "1"
    os.environ["DEEPEVAL_UPDATE_WARNING_OPT_OUT"] = "1"
    os.environ["OTEL_SDK_DISABLED"] = "true"
    from deepeval.models import DeepEvalBaseLLM

    class JudgeModel(DeepEvalBaseLLM):
        def load_model(self):
            return judge

        def generate(self, prompt: str, schema: type[BaseModel] | None = None):
            return judge.generate(prompt, schema)

        async def a_generate(self, prompt: str, schema: type[BaseModel] | None = None):
            return await judge.async_generate(prompt, schema)

        def get_model_name(self):
            return judge.get_model_name()

    return JudgeModel()
