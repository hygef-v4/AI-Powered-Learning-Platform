import argparse
import os
import re
import sys
from datetime import datetime, timezone
from importlib.metadata import version
from pathlib import Path
from uuid import uuid4

from dotenv import dotenv_values

from dataset import BUNDLED_DATASET, ROOT, Row, fingerprint, load_artifacts, load_dataset, read_json
from evaluation import METRICS, METRIC_VERSION, evaluate_rows, make_geval
from judges import HOSTED, PROVIDERS, JudgeError, create_judge, deepeval_model, probe_judge, select_judge
from reports import build_report, compare_reports, write_reports
from runner import MODE_LAYERS, deterministic_check, index_runs, merge_replay, run_automated, run_live, run_replay


HERE = Path(__file__).resolve().parent
DEFAULT_PRIORITY = "codex,claude,gemini,opencode,ollama"


def default_output() -> Path:
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S-%fZ")
    return HERE / "reports" / stamp


def probability(text: str) -> float:
    value = float(text)
    if not 0 <= value <= 1:
        raise argparse.ArgumentTypeError("Threshold must be between 0 and 1")
    return value


def non_negative(text: str) -> float:
    value = float(text)
    if not value >= 0:
        raise argparse.ArgumentTypeError("Value must be zero or greater")
    return value


def positive_int(text: str) -> int:
    value = int(text)
    if value < 1:
        raise argparse.ArgumentTypeError("Value must be positive")
    return value


def parser(settings: dict) -> argparse.ArgumentParser:
    cli = argparse.ArgumentParser(description="U09 unified AI grading evaluation")
    cli.add_argument("--mode", choices=(*MODE_LAYERS, "report", "doctor"), default="offline")
    cli.add_argument("--results", type=Path, default=settings.get("QA_RESULTS") or None,
                     help="Original Java live results.json with sibling recordings")
    cli.add_argument("--dataset", type=Path, default=BUNDLED_DATASET)
    cases = cli.add_mutually_exclusive_group()
    cases.add_argument("--case")
    cases.add_argument("--all", action="store_true")
    cli.add_argument("--run", type=positive_int, help="Select exactly one recorded attempt for --case")
    cli.add_argument("--judge", choices=("auto", *PROVIDERS), default="auto")
    cli.add_argument("--model", help="Explicit model for a named judge")
    cli.add_argument("--allow-hosted", choices=sorted(HOSTED), action="append", default=[],
                     help="Allow this judge to receive bundled synthetic data and use account quota")
    cli.add_argument("--priority", default=settings.get("QA_JUDGE_PRIORITY") or DEFAULT_PRIORITY)
    cli.add_argument("--score-tolerance", type=non_negative, default=0.0,
                     help="Largest total-score error that still counts as a completed task")
    cli.add_argument("--input-price", type=non_negative, help="USD per million input tokens of the graded model")
    cli.add_argument("--output-price", type=non_negative, help="USD per million output tokens of the graded model")
    cli.add_argument("--timeout", type=positive_int, default=180, help="Seconds per judge invocation")
    cli.add_argument("--max-calls", type=positive_int, default=6)
    cli.add_argument("--no-semantic", "--prepare-only", dest="prepare_only", action="store_true",
                     help="Run the deterministic layers without a judge")
    cli.add_argument("--allow-live", action="store_true", help="Permit billable Gemini grading in live/full")
    cli.add_argument("--gemini-model")
    cli.add_argument("--repeats", type=positive_int, default=1)
    cli.add_argument("--reasoning-reserve", type=int, default=4096)
    cli.add_argument("--backend-xml", type=Path, help="Collect existing JUnit XML file or folder")
    cli.add_argument("--frontend-json", type=Path, help="Collect an existing Vitest JSON report")
    cli.add_argument("--collect-only", action="store_true", help="Skip automated suites without an input report")
    cli.add_argument("--maven", default=settings.get("QA_MAVEN") or None)
    cli.add_argument("--java-home", type=Path, default=settings.get("QA_JAVA_HOME") or None)
    cli.add_argument("--maven-online", action="store_true", help="Permit Maven to resolve dependencies")
    cli.add_argument("--input", type=Path, help="Saved unified JSON for report mode")
    cli.add_argument("--share", action="store_true", help="In report mode, export only aggregate Markdown")
    cli.add_argument("--output", type=Path, default=default_output())
    cli.add_argument("--baseline", type=Path)
    cli.add_argument("--drop-tolerance", type=probability, default=0.05)
    for metric in METRICS:
        cli.add_argument(
            f"--{metric.replace('_', '-')}-threshold", type=probability,
            default=settings.get(f"QA_{metric.upper()}_THRESHOLD") or "0.8",
        )
    return cli


def add_semantic_results(args, rows, thresholds, metadata):
    priority = tuple(name.strip() for name in args.priority.split(",") if name.strip())
    if not priority or len(set(priority)) != len(priority) or set(priority) - set(PROVIDERS):
        raise ValueError("Judge priority must contain distinct, supported provider names")
    candidates = [row for row in rows if row.input_status == "VALID_OUTPUT"
                  and row.deterministic_results.status == "PASS"]
    if args.prepare_only or not candidates:
        return {"status": "SKIPPED", "detail": "Judge disabled or no valid response passed current validation"}
    allowed = set(args.allow_hosted)
    if allowed and (
        args.dataset.resolve() != BUNDLED_DATASET.resolve()
        or any(row.ground_truth_source != "SYNTHETIC_TEAM" for row in candidates)
    ):
        raise ValueError("Hosted judges accept only the built-in synthetic dataset")
    if args.judge in HOSTED and args.judge not in allowed:
        return {"status": "SKIPPED", "detail": f"Use --allow-hosted {args.judge} to permit that judge"}
    try:
        info = select_judge(args.judge, priority, allowed, lambda provider: probe_judge(provider, args.model))
    except JudgeError as error:
        if args.judge == "auto" and not allowed:
            return {"status": "SKIPPED", "detail": str(error)}
        for row in candidates:
            row.evaluation_status = "JUDGE_ERROR"
        return {"status": "JUDGE_ERROR", "detail": str(error)}
    effort = f" / {info.reasoning_effort}" if info.reasoning_effort else ""
    print(f"Selected judge: {info.provider} / {info.model}{effort} ({info.detail})", flush=True)
    judge = create_judge(info, timeout=args.timeout, max_calls=args.max_calls)
    try:
        evaluate_rows(candidates, thresholds, make_geval(deepeval_model(judge), thresholds))
    except (ImportError, JudgeError) as error:
        for row in candidates:
            row.evaluation_status = "JUDGE_ERROR"
        return {"status": "JUDGE_ERROR", "detail": str(error)}
    metadata.update(
        judge_provider=info.provider, judge_model=info.model, judge_cli_or_model_version=info.version,
        judge_reasoning_effort=info.reasoning_effort,
        judge_model_versions=sorted(judge.reported_models), judge_authentication=info.detail,
        judge_calls=judge.call_count, deepeval_version=version("deepeval"),
    )
    status = ("JUDGE_ERROR" if any(row.evaluation_status == "JUDGE_ERROR" for row in candidates) else
              "QUALITY_FAIL" if any(row.quality_status == "QUALITY_FAIL" for row in candidates) else "PASS")
    return {"status": status, "eligible": len(candidates), "judge_calls": judge.call_count}


def saved_source(args) -> Path:
    if args.results:
        return args.results.resolve(strict=True)
    directory = ROOT / "backend/target/ai-eval"
    found = max(directory.glob(f"{args.dataset.name}-live-*/results.json"), default=None)
    if found is None:
        raise ValueError("Set --results or QA_RESULTS to the original Gemini results.json")
    return found.resolve()


def run_evaluation(args) -> dict:
    scheduled = MODE_LAYERS[args.mode]
    with_live = args.mode == "live" or (args.mode == "full" and args.allow_live)
    if args.allow_live and args.mode not in {"live", "full"}:
        raise ValueError("--allow-live is only valid with live or full")
    if with_live and (not args.allow_live or not args.gemini_model or not os.environ.get("AI_EVAL_API_KEY")):
        raise ValueError("Live requires --allow-live, --gemini-model and AI_EVAL_API_KEY")
    if args.run is not None and (not args.case or with_live):
        raise ValueError("--run requires --case on a saved source")
    if args.model and (args.judge == "auto" or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.:/#-]*", args.model)):
        raise ValueError("--model requires a named judge and a valid model identifier")
    if args.gemini_model and not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.:/-]*", args.gemini_model):
        raise ValueError("Invalid Gemini grading model")
    if args.mode == "automated" and args.case:
        raise ValueError("Automated mode runs suites. Use full for scenario results.")
    dataset = None if args.mode == "automated" else load_dataset(args.dataset)
    if args.case and args.case not in dataset.cases:
        raise ValueError(f"Unknown case: {args.case}")
    rows, automated, checks = [], [], {}
    source = None
    if dataset and not with_live:
        source = saved_source(args)
        print(f"Using saved grading results: {source}", flush=True)
        original, rows = load_artifacts(dataset, source, args.case, args.run)
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    layers = {name: {"status": "NOT_EVALUATED"} for name in ("automated", "replay", "live", "semantic")}
    metadata = {
        "evaluation_run_id": str(uuid4()), "mode": args.mode,
        "evaluation_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "metric_version": METRIC_VERSION,
        "score_tolerance": args.score_tolerance, "input_price": args.input_price, "output_price": args.output_price,
        "judge_calls": 0, "judge_call_limit": args.max_calls,
        "semantic_required": "semantic" in scheduled and not args.prepare_only,
    }
    thresholds = {metric: getattr(args, f"{metric}_threshold") for metric in METRICS}
    if "automated" in scheduled:
        layers["automated"], automated = run_automated(args, output)
    if with_live:
        source, layers["live"], checks["live"] = run_live(args, output)
        if source:
            original, rows = load_artifacts(dataset, source, args.case)
            saved = index_runs(source)
            for row in rows:
                row.deterministic_results = deterministic_check(saved[(row.test_case_id, row.run_index)])
    if dataset:
        metadata.update(assignment_id=dataset.assignment_id, rubric_version=dataset.rubric_version,
                        dataset_hash=dataset.dataset_hash)
    if source:
        metadata.update(grading_provider=original["provider"], grading_model=original["model"],
                        grading_started_at=original.get("startedAt"), grading_results=str(source),
                        source_hash=fingerprint(read_json(source)))
        if not with_live:
            layers["live"].update(source="IMPORTED", evidence=str(source))
        if "replay" in scheduled:
            replay, layers["replay"], checks["replay"] = run_replay(args, output, source)
            try:
                merge_replay(rows, source, replay)
                if any(row.replay_results.status == "REPLAY_FAIL" for row in rows):
                    layers["replay"]["status"] = "REPLAY_FAIL"
            except (ValueError, KeyError) as error:
                layers["replay"].update(status="REPLAY_FAIL", detail=str(error))
    if "semantic" in scheduled:
        try:
            layers["semantic"] = add_semantic_results(args, rows, thresholds, metadata)
        except (ValueError, OSError, JudgeError) as error:
            layers["semantic"] = {"status": "JUDGE_ERROR", "detail": str(error)}
            for row in rows:
                if (row.input_status == "VALID_OUTPUT" and row.deterministic_results.status == "PASS"
                        and row.evaluation_status != "EVALUATED"):
                    row.evaluation_status = "JUDGE_ERROR"
    metadata["finished_utc"] = datetime.now(timezone.utc).isoformat(timespec="seconds")
    return build_report(rows, metadata, thresholds, layers=layers, automated=automated, checks=checks)


def load_report(path: Path) -> dict:
    report = read_json(path)
    if report.get("schema_version") == 1:
        metadata = {**report["metadata"], "mode": "imported_legacy", "legacy_evidence": str(path.resolve())}
        rows = [Row.model_validate({"applicable_item_ids": [], **row}) for row in report["rows"]]
        return build_report(rows, metadata, report["thresholds"])
    if report.get("schema_version") != 2:
        raise ValueError("Expected a unified evaluation JSON (schema v2) or a legacy judge JSON (v1)")
    return report


def main(argv=None) -> int:
    settings = dotenv_values(HERE / ".env", interpolate=False)
    cli = parser(settings)
    argv = list(sys.argv[1:] if argv is None else argv)
    if argv and argv[0] in {"evaluate", "report", "doctor"}:
        mode = "offline" if argv[0] == "evaluate" else argv[0]
        argv = ["--mode", mode, *argv[1:]]
    args = cli.parse_args(argv)
    try:
        if args.mode == "doctor":
            if args.model and args.judge == "auto":
                raise ValueError("--model requires a named judge")
            for provider in (args.judge,) if args.judge != "auto" else PROVIDERS:
                info = probe_judge(provider, args.model)
                effort = f" ({info.reasoning_effort})" if info.reasoning_effort else ""
                print(f"{provider}: {'ready' if info.ready else 'unavailable'} | {info.model or '-'}{effort} | {info.detail}")
            return 0
        if args.output.exists():
            raise ValueError("Output directory already exists. Choose a new directory to keep previous reports.")
        if args.share and args.mode != "report":
            raise ValueError("--share is only valid in report mode")
        baseline = load_report(args.baseline) if args.baseline else None
        if args.mode == "report":
            source = args.input or max((HERE / "reports").glob("*/ai-grading-evaluation.json"),
                                       key=lambda path: path.stat().st_mtime_ns, default=None)
            if not source:
                raise ValueError("Set --input to a saved evaluation JSON")
            print(f"Using saved evaluation: {source.resolve()}", flush=True)
            report = load_report(source)
        else:
            report = run_evaluation(args)
        if baseline is not None:
            report["regressions"] = compare_reports(report, baseline, args.drop_tolerance)
        write_reports(report, args.output, share=args.share, prepared=args.mode != "report")
        print(f"Reports: {args.output.resolve()}")
        if args.mode != "report" and any(layer["status"] in {
            "DETERMINISTIC_FAIL", "REPLAY_FAIL", "JUDGE_ERROR", "PROVIDER_ERROR",
        } for layer in report["layers"].values()):
            return 2
        return 0
    except (OSError, ValueError, KeyError, JudgeError) as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
