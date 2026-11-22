import os
import re
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

from dataset import ROOT, CheckResult, Row, read_json


BACKEND = ROOT / "backend"
FRONTEND = ROOT / "frontend"
JAVA_TESTS = BACKEND / "src/test/java/edu/aiplatform/grading"
JAVA_DATASETS = BACKEND / "src/test/resources/ai-eval/grading"
MODE_LAYERS = {
    "automated": ("automated",), "replay": ("replay",),
    "offline": ("replay", "semantic"), "live": ("live", "semantic"),
    "full": ("automated", "replay", "semantic"),
}


def child_environment() -> dict[str, str]:
    removed = {
        "GEMINI_API_KEY", "GOOGLE_API_KEY", "OPENAI_API_KEY", "ANTHROPIC_API_KEY",
        "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "MAVEN_ARGS", "MAVEN_OPTS",
    }
    return {key: value for key, value in os.environ.items()
            if key.upper() not in removed and not key.upper().startswith("AI_EVAL_")}


def maven_path(preferred=None) -> str:
    if preferred:
        path = shutil.which(preferred) or (preferred if Path(preferred).is_file() else None)
    else:
        path = shutil.which("mvn.cmd") or shutil.which("mvn")
    if not path and not preferred:
        wrappers = (Path.home() / ".m2/wrapper/dists").glob("apache-maven-*/*/bin/mvn.cmd")
        path = max(wrappers, key=lambda p: tuple(map(int, re.findall(r"\d+", p.parents[2].name))), default=None)
    if not path:
        raise OSError("Maven is unavailable. Set --maven or QA_MAVEN to the program path.")
    return str(Path(path).resolve())


def execute(args: list[str], cwd: Path, log: Path, env: dict[str, str]) -> int:
    log.parent.mkdir(parents=True, exist_ok=True)
    print(f"Running {log.stem} | {log}", flush=True)
    if os.name == "nt" and Path(args[0]).suffix.lower() in {".cmd", ".bat"} and any(
        re.search(r'[%!&|<>^"\r\n]', value) for value in args
    ):
        raise ValueError("A Windows batch argument contains a shell metacharacter")
    with log.open("wb") as stream:
        return subprocess.run(args, cwd=cwd, env=env, stdout=stream, stderr=subprocess.STDOUT,
                              stdin=subprocess.DEVNULL, shell=False).returncode


def run_maven(args, raw: Path, cases: list[str], mode: str, extra_env=None) -> int:
    env = child_environment()
    env["JAVA_TOOL_OPTIONS"] = "-Dfile.encoding=UTF-8"
    if args.java_home:
        env["JAVA_HOME"] = str(Path(args.java_home).resolve())
    env.update(extra_env or {})
    command = [maven_path(args.maven), "-B", "-ntp", "-Dstyle.color=never",
               f"-Dtest={','.join(cases)}", f"-Dai.eval.mode={mode}"]
    if not args.maven_online:
        command.insert(1, "-o")
    return execute([*command, "test"], BACKEND, raw / "maven.log", env)


def collect_junit(files: list[Path], component: str) -> list[dict]:
    rows = []
    for file in files:
        xml = file.read_bytes()
        if b"<!DOCTYPE" in xml or b"<!ENTITY" in xml:
            raise ValueError("Unsupported JUnit XML declaration")
        for test in ET.fromstring(xml).iter("testcase"):
            failure = test.find("failure")
            if failure is None:
                failure = test.find("error")
            status = ("DETERMINISTIC_FAIL" if failure is not None else
                      "SKIPPED" if test.find("skipped") is not None else "PASS")
            rows.append({
                "component": component, "suite": test.get("classname"), "name": test.get("name"),
                "test_case_id": None, "status": status, "duration_seconds": test.get("time"),
                "detail": failure.get("message", "") if failure is not None else None,
                "evidence": str(file.resolve()),
            })
    return rows


def collect_vitest(file: Path) -> list[dict]:
    status_map = {"passed": "PASS", "failed": "DETERMINISTIC_FAIL", "pending": "SKIPPED",
                  "skipped": "SKIPPED", "todo": "SKIPPED"}
    return [
        {
            "component": "frontend", "suite": suite["name"], "name": test["fullName"],
            "test_case_id": None, "status": status_map.get(test["status"], "NOT_EVALUATED"),
            "duration_seconds": test["duration"] / 1000 if test.get("duration") is not None else None,
            "detail": "\n".join(test.get("failureMessages", [])) or None, "evidence": str(file.resolve()),
        }
        for suite in read_json(file)["testResults"] for test in suite["assertionResults"]
    ]


def run_automated(args, output: Path) -> tuple[dict, list[dict]]:
    raw = output / "raw" / "automated"
    raw.mkdir(parents=True)
    components, rows = {}, []
    for component, input_path in (("backend", args.backend_xml), ("frontend", args.frontend_json)):
        try:
            if args.collect_only and not input_path:
                components[component] = {"status": "NOT_EVALUATED"}
                continue
            if component == "backend":
                found, code = backend_tests(args, raw, input_path)
            else:
                found, code = frontend_tests(raw, input_path)
            rows.extend(found)
            outcomes = {row["status"] for row in found}
            failed = code != 0 or not found or "DETERMINISTIC_FAIL" in outcomes
            status = ("DETERMINISTIC_FAIL" if failed else "PASS" if "PASS" in outcomes else "SKIPPED")
            components[component] = {"status": status,
                                     "observed_tests": len(found), "exit_code": code,
                                     "source": "imported" if input_path else "executed"}
        except (OSError, ValueError, KeyError, ET.ParseError) as error:
            components[component] = {"status": "DETERMINISTIC_FAIL", "detail": str(error)}
    statuses = {value["status"] for value in components.values()}
    status = ("DETERMINISTIC_FAIL" if "DETERMINISTIC_FAIL" in statuses else
              "PASS" if "PASS" in statuses else "SKIPPED" if "SKIPPED" in statuses else "NOT_EVALUATED")
    return {"status": status, "components": components, "evidence": str(raw)}, rows


def backend_tests(args, raw: Path, source: Path | None) -> tuple[list[dict], int]:
    if source:
        files = sorted(source.glob("TEST-*.xml")) if source.is_dir() else [source]
        return collect_junit(files, "backend"), 0
    names = sorted({file.stem for file in JAVA_TESTS.rglob("*Test.java")})
    target = BACKEND / "target/surefire-reports"
    started = time.time_ns()
    code = run_maven(args, raw / "backend", names, "disabled")
    evidence = raw / "backend" / "junit"
    evidence.mkdir(parents=True)
    files = [
        Path(shutil.copy2(file, evidence / file.name))
        for file in sorted(target.glob("TEST-edu.aiplatform.grading.*.xml"))
        if file.stat().st_mtime_ns >= started and file.stem.rsplit(".", 1)[-1] in names
    ]
    return collect_junit(files, "backend"), code


def frontend_tests(raw: Path, source: Path | None) -> tuple[list[dict], int]:
    if source:
        return collect_vitest(source), 0
    node = shutil.which("node.exe") or shutil.which("node")
    vitest = FRONTEND / "node_modules/vitest/vitest.mjs"
    if not node or not vitest.is_file():
        raise OSError("Node and the existing frontend dependencies are required for grading tests")
    output = raw / "frontend" / "vitest.json"
    code = execute([node, str(vitest), "run", "tests/features/grading", "--maxWorkers=4",
                    "--reporter=json", "--outputFile", str(output)],
                   FRONTEND, raw / "frontend" / "vitest.log", child_environment())
    return collect_vitest(output) if output.is_file() else [], code


def run_java_evaluation(args, output: Path, mode: str, extra_env: dict):
    dataset = args.dataset.resolve()
    if dataset.parent != JAVA_DATASETS.resolve():
        raise ValueError("Java replay/live needs a dataset under backend/src/test/resources/ai-eval/grading")
    raw = output / "raw" / mode
    env = {"AI_EVAL_DATASET": dataset.name, "AI_EVAL_OUTPUT_DIR": str(raw), "AI_EVAL_ENFORCE": "false",
           **extra_env}
    if args.case:
        env["AI_EVAL_CASES"] = args.case
    status = "REPLAY_FAIL" if mode == "replay" else "PROVIDER_ERROR"
    try:
        code = run_maven(args, raw, ["GradingAiEvaluationTest"], mode, env)
        files = sorted(raw.glob("*/results.json"))
        if len(files) != 1:
            return None, {"status": status, "exit_code": code, "evidence": str(raw),
                          "detail": "Java runner did not produce a single report"}, []
        artifact = read_json(files[0])
        return files[0], {"status": "PASS" if code == 0 else status, "exit_code": code,
                          "evidence": str(files[0])}, artifact["metrics"]
    except (OSError, ValueError, KeyError) as error:
        return None, {"status": status, "evidence": str(raw), "detail": str(error)}, []


def run_replay(args, output: Path, source: Path):
    return run_java_evaluation(args, output, "replay", {
        "AI_EVAL_REPLAY_DIR": str(source.resolve().parent / "recordings"),
    })


def run_live(args, output: Path):
    if not args.allow_live or not args.gemini_model or not os.environ.get("AI_EVAL_API_KEY"):
        raise ValueError("Live requires --allow-live, --gemini-model and AI_EVAL_API_KEY")
    print(f"Gemini Live may use billed quota. Requested {args.repeats} attempt(s) per selected case.", flush=True)
    return run_java_evaluation(args, output, "live", {
        "AI_EVAL_PROVIDER": "GEMINI", "AI_EVAL_MODEL": args.gemini_model,
        "AI_EVAL_RUNS": str(args.repeats), "AI_EVAL_API_KEY": os.environ["AI_EVAL_API_KEY"],
        "AI_EVAL_REASONING_RESERVE": str(args.reasoning_reserve),
    })


def index_runs(source: Path) -> dict:
    artifact = read_json(source)
    index = {}
    for run in artifact["runs"]:
        key = (run["caseKey"], run["runIndex"])
        if key in index:
            raise ValueError(f"Duplicate Java attempt: {key}")
        index[key] = run
    return index


def deterministic_check(run: dict) -> CheckResult:
    status = {"VALID": "PASS", "INVALID_OUTPUT": "DETERMINISTIC_FAIL",
              "PROVIDER_ERROR": "SKIPPED"}[run["status"]]
    return CheckResult(status=status, engine_status=run["status"], actual_score=run.get("aiTotal"),
                       error_code=run.get("errorCode"), items=run.get("items", []))


def merge_replay(rows: list[Row], source: Path, replay: Path | None) -> None:
    saved = index_runs(source)
    replayed = index_runs(replay) if replay else {}
    if replay:
        left, right = read_json(source), read_json(replay)
        if right["run"]["mode"] != "replay" or any(
            left["run"][key] != right["run"][key] for key in ("provider", "model")
        ) or left["assignment"] != right["assignment"]:
            raise ValueError("Replay results do not match the source assignment and provider")
    for row in rows:
        key = (row.test_case_id, row.run_index)
        if replay:
            row.evidence_paths["replay_results"] = str(replay)
        if row.input_status == "SKIPPED":
            row.replay_results = CheckResult(status="SKIPPED", detail="No Gemini recording")
            continue
        if key not in replayed:
            row.replay_results = CheckResult(status="REPLAY_FAIL", detail="No result for the saved attempt")
            continue
        previous, current = saved[key], replayed[key]
        row.deterministic_results = deterministic_check(current)
        differences = [
            {"field": field, "saved": previous.get(field), "current": current.get(field)}
            for field in ("status", "errorCode", "aiTotal") if previous.get(field) != current.get(field)
        ]
        saved_items = item_verdicts(previous.get("items", []))
        current_items = item_verdicts(current.get("items", []))
        for item_id in sorted(saved_items.keys() | current_items.keys()):
            old, new = saved_items.get(item_id), current_items.get(item_id)
            if old != new:
                differences.append({"field": item_id, "saved": old, "current": new})
        row.replay_results = CheckResult(
            status="REPLAY_FAIL" if differences else "PASS", engine_status=current["status"],
            actual_score=current.get("aiTotal"), error_code=current.get("errorCode"),
            detail="Changed " + ", ".join(item["field"] for item in differences)
            if differences else "Saved behavior reproduced",
            changes=differences,
        )


def item_verdicts(items: list[dict]) -> dict:
    return {item["itemKey"]: {"awarded": item["awarded"], "requires_review": item.get("requiresReview")}
            for item in items}


def overall_status(row: Row, semantic_required=False) -> str:
    if row.input_status in {"PROVIDER_ERROR", "INVALID_OUTPUT", "SKIPPED"}:
        return row.input_status
    if row.deterministic_results.status == "DETERMINISTIC_FAIL":
        return "DETERMINISTIC_FAIL"
    if row.replay_results.status == "REPLAY_FAIL":
        return "REPLAY_FAIL"
    if row.evaluation_status == "JUDGE_ERROR":
        return "JUDGE_ERROR"
    if row.quality_status == "QUALITY_FAIL":
        return "QUALITY_FAIL"
    if semantic_required and row.evaluation_status != "EVALUATED":
        return "NOT_EVALUATED"
    return "PASS" if row.deterministic_results.status == "PASS" else "NOT_EVALUATED"
