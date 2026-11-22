import json
from collections import Counter
from pathlib import Path
from statistics import mean
from uuid import uuid4

import pandas as pd
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter

from dataset import Row
from runner import overall_status


SCHEMA_VERSION = 2
CASE_COLUMNS = (
    "test_case_id", "assignment_id", "run_index", "scenario", "expected_behavior",
    "gemini_model", "rubric_version", "ground_truth_source", "expected_score", "ai_score",
    "provider_status", "deterministic_status", "replay_status", "evaluation_status",
    "quality_status", "overall_status", "evidence_paths",
)
METRIC_COLUMNS = (
    "test_case_id", "run_index", "layer", "judge_provider", "judge_model", "metric",
    "score", "threshold", "status", "reason",
)
ERROR_COLUMNS = ("test_case_id", "run_index", "layer", "status", "error_code", "provider_http_status",
                 "detail", "evidence_paths")
REGRESSION_COLUMNS = (
    "assignment_id", "test_case_id", "run_index", "findings", "baseline_quality",
    "current_quality", "baseline_scores", "current_scores",
)


def build_report(rows: list[Row], metadata: dict, thresholds: dict, *, layers=None, automated=None, checks=None) -> dict:
    run_id = metadata.get("evaluation_run_id") or str(uuid4())
    layers = layers or {name: {"status": "NOT_EVALUATED"} for name in ("automated", "replay", "live", "semantic")}
    automated = automated or []
    checks = checks or {}
    input_counts = Counter(row.input_status for row in rows)
    completed = [row for row in rows if row.evaluation_status == "EVALUATED"]
    passed = sum(row.quality_status == "QUALITY_PASS" for row in completed)
    serialized = []
    for row in rows:
        result = row.model_dump(mode="json")
        current = row.deterministic_results.actual_score if row.deterministic_results.status == "PASS" else None
        from_archive = not metadata.get("mode") or metadata["mode"] == "imported_legacy"
        score = row.ai_score if from_archive and current is None else current
        result.update(
            evaluation_run_id=run_id, dataset_version=metadata.get("dataset_hash"),
            gemini_model=result.pop("grading_model"), provider_status=result.pop("input_status"),
            judge_provider=metadata.get("judge_provider"), judge_model=metadata.get("judge_model"),
            semantic_metrics=result.pop("metrics"),
            execution_metadata={"mode": metadata.get("mode"), "grading_started_at": metadata.get("grading_started_at")},
            overall_status=overall_status(row, metadata.get("semantic_required", False)),
            score_comparison={
                "expected_score": row.expected_score, "current_score": score, "saved_score": row.ai_score,
                "source": row.ground_truth_source, "basis": "ARCHIVE" if from_archive else "CURRENT_VALIDATION",
                "absolute_error": abs(score - row.expected_score) if score is not None and row.expected_score is not None
                else None,
            },
        )
        serialized.append(result)
    mae = {}
    for source in ("SYNTHETIC_TEAM", "LECTURER_VERIFIED"):
        differences = [
            row["score_comparison"]["absolute_error"] for row in serialized
            if row["ground_truth_source"] == source and row["score_comparison"]["absolute_error"] is not None
        ]
        mae[source] = {"value": mean(differences) if differences else None, "count": len(differences)}
    deterministic = Counter(row.deterministic_results.status for row in rows)
    automatic = Counter(test["status"] for test in automated)
    statuses = Counter(row["overall_status"] for row in serialized)
    failures = {layer["status"] for layer in layers.values()} | statuses.keys()
    overall = next((status for status in (
        "DETERMINISTIC_FAIL", "REPLAY_FAIL", "PROVIDER_ERROR", "INVALID_OUTPUT", "JUDGE_ERROR", "QUALITY_FAIL",
    ) if status in failures), None)
    if overall is None:
        overall = ("NOT_EVALUATED" if statuses["NOT_EVALUATED"] else
                   "PASS" if "PASS" in failures else "SKIPPED" if "SKIPPED" in failures else "NOT_EVALUATED")
    provider_attempts = input_counts["VALID_OUTPUT"] + input_counts["INVALID_OUTPUT"] + input_counts["PROVIDER_ERROR"]
    return {
        "schema_version": SCHEMA_VERSION, "evaluation_run_id": run_id, "overall_status": overall,
        "metadata": metadata, "thresholds": thresholds, "layers": layers,
        "automated_results": automated, "deterministic_metrics": checks,
        "summary": {
            "attempts": len(rows), "cases": len({row.test_case_id for row in rows}),
            "input_status_counts": dict(input_counts), "evaluated": len(completed),
            "quality_passes": passed,
            "judge_errors": sum(row.evaluation_status == "JUDGE_ERROR" for row in rows),
            "coverage": len(completed) / len(rows) if rows else None,
            "valid_output_coverage": len(completed) / input_counts["VALID_OUTPUT"]
            if input_counts["VALID_OUTPUT"] else None,
            "quality_pass_rate": passed / len(completed) if completed else None,
            "provider_success_rate": 1 - input_counts["PROVIDER_ERROR"] / provider_attempts if provider_attempts else None,
            "deterministic_pass_rate": pass_rate(deterministic), "automated_pass_rate": pass_rate(automatic),
            "deterministic_status_counts": dict(deterministic), "automated_status_counts": dict(automatic),
            "overall_status_counts": dict(statuses),
            "defect_detection_rate": None,
            "mae": mae,
        },
        "rows": serialized, "regressions": [],
    }


def pass_rate(counts: Counter) -> float | None:
    total = counts["PASS"] + counts["DETERMINISTIC_FAIL"]
    return counts["PASS"] / total if total else None


def compare_reports(current: dict, baseline: dict, drop_tolerance=0.05) -> list[dict]:
    def index(report):
        rows = {}
        for row in report["rows"]:
            key = (row["assignment_id"], row["test_case_id"], row["run_index"])
            if key in rows:
                raise ValueError(f"Duplicate row in report: {key}")
            rows[key] = row
        return rows

    def scores(row):
        metrics = row.get("semantic_metrics", row.get("metrics", {}))
        return {key: value["score"] for key, value in metrics.items() if value["score"] is not None}

    left, right = index(baseline), index(current)
    old_meta, new_meta = baseline["metadata"], current["metadata"]
    judge_keys = ("judge_provider", "judge_model", "judge_model_versions", "judge_cli_or_model_version",
                  "judge_reasoning_effort")
    same_judge = all(old_meta.get(key) == new_meta.get(key) for key in judge_keys)
    same_contract = current["thresholds"] == baseline["thresholds"] and all(
        old_meta.get(key) == new_meta.get(key) for key in ("metric_version", "dataset_hash")
    )
    changes = []
    for key in sorted(left.keys() | right.keys()):
        old, new = left.get(key, {}), right.get(key, {})
        before, after = scores(old), scores(new)
        findings = []
        if not new:
            findings.append("MISSING_RUN")
        elif not old:
            findings.append("NEW_RUN")
        elif not same_judge:
            findings.append("CROSS_JUDGE_COMPARISON")
            if old.get("output_hash") != new.get("output_hash"):
                findings.append("DIFFERENT_GEMINI_OUTPUT")
        elif not same_contract:
            findings.append("DIFFERENT_EVALUATION_CONFIG")
        else:
            if old.get("replay_results", {}).get("status") == "PASS" and new.get("replay_results", {}).get("status") == "REPLAY_FAIL":
                findings.append("NEW_REPLAY_FAILURE")
            if old.get("quality_status") == "QUALITY_PASS" and new.get("quality_status") == "QUALITY_FAIL":
                findings.append("NEW_QUALITY_FAILURE")
            if before.keys() - after.keys():
                findings.append("MISSING_METRICS")
            if set(new.get("missing_criteria", [])) - set(old.get("missing_criteria", [])):
                findings.append("MISSING_CRITERIA")
            if any(round(before[name] - after[name], 9) > drop_tolerance for name in before.keys() & after.keys()):
                findings.append("METRIC_DROP")
            if old.get("expected_score") == new.get("expected_score") and old.get("expected_score") is not None:
                expected = old["expected_score"]
                previous_score = old.get("score_comparison", {}).get("current_score", old.get("ai_score"))
                current_score = new.get("score_comparison", {}).get("current_score", new.get("ai_score"))
                if previous_score is not None and current_score is not None and (
                    abs(current_score - expected) - abs(previous_score - expected) > 1e-9
                ):
                    findings.append("GRADING_SCORE_ERROR_INCREASE")
        if findings:
            changes.append(dict(zip(REGRESSION_COLUMNS, [
                *key, findings, old.get("quality_status"), new.get("quality_status"), before, after,
            ])))
    return changes


def safe_cell(value, *, excel=False):
    if isinstance(value, (dict, list)):
        value = json.dumps(value, ensure_ascii=False)
    if not isinstance(value, str):
        return value
    value = "".join(char for char in value if ord(char) >= 32 or char in "\n\r\t")
    stripped = value.lstrip()
    if stripped and stripped[0] in "=+-@＝＋－＠":
        value = "'" + value
    return value[:32767] if excel else value


def frame(rows, columns, *, excel=False):
    return pd.DataFrame(
        [{column: safe_cell(row.get(column), excel=excel) for column in columns} for row in rows],
        columns=columns,
    )


def summary_markdown(report: dict, shared=False) -> str:
    summary = report["summary"]

    def percent(value):
        return f"{value:.1%}" if value is not None else "N/A"

    heading = "# Unified AI grading evaluation" if not shared else "# AI grading QA summary for sharing"
    lines = [
        heading, "",
        "GEval measures judge-assessed quality. Score error compares the saved grading score with a labeled reference.",
        "",
        "| Measurement | Value |", "|---|---:|",
        f"| Overall status | {report['overall_status']} |",
        f"| Saved attempts | {summary['attempts']} |",
        f"| Distinct cases | {summary['cases']} |",
        f"| Valid Gemini outputs | {summary['input_status_counts'].get('VALID_OUTPUT', 0)} |",
        f"| Gemini provider errors | {summary['input_status_counts'].get('PROVIDER_ERROR', 0)} |",
        f"| Invalid Gemini outputs | {summary['input_status_counts'].get('INVALID_OUTPUT', 0)} |",
        f"| Skipped input attempts | {summary['input_status_counts'].get('SKIPPED', 0)} |",
        f"| Judge errors | {summary['judge_errors']} |",
        f"| Fully evaluated | {summary['evaluated']} |",
        f"| Coverage across saved attempts | {percent(summary['coverage'])} |",
        f"| Coverage of valid outputs | {percent(summary['valid_output_coverage'])} |",
        f"| Quality pass rate among completed attempts | {percent(summary['quality_pass_rate'])} |",
        f"| Provider success rate | {percent(summary['provider_success_rate'])} |",
        f"| Current deterministic pass rate | {percent(summary['deterministic_pass_rate'])} |",
        f"| Automated assertion pass rate | {percent(summary['automated_pass_rate'])} |",
        "| Defect detection rate | N/A (no verified defect labels) |",
    ]
    for source, label in (("SYNTHETIC_TEAM", "Synthetic team reference"),
                          ("LECTURER_VERIFIED", "Lecturer-verified reference")):
        error = summary["mae"][source]
        value = f"{error['value']:.3f} points" if error["value"] is not None else "N/A"
        lines.append(f"| MAE · {label} | {value} (n={error['count']}) |")
    if not shared:
        lines += ["", "## Layers", "", "| Layer | Status |", "|---|---|"]
        lines += [f"| {layer} | {details['status']} |" for layer, details in report["layers"].items()]
        lines += ["", "Use the workbook to review per-metric reasons, failed cases, source errors and evidence."]
        if report["regressions"]:
            lines += ["", f"Baseline comparison records: {len(report['regressions'])}."]
    return "\n".join(lines) + "\n"


def write_reports(report: dict, output: Path, *, share=False, prepared=False) -> None:
    if not prepared:
        output.mkdir(parents=True, exist_ok=False)
    if share:
        (output / "ai-grading-evaluation.md").write_text(summary_markdown(report, shared=True), encoding="utf-8")
        return
    (output / "ai-grading-evaluation.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2, allow_nan=False) + "\n", encoding="utf-8"
    )
    rows = [
        {**row, "deterministic_status": row["deterministic_results"]["status"],
         "replay_status": row["replay_results"]["status"]}
        for row in report["rows"]
    ]
    metric_rows = [
        {**row, "layer": "semantic", "metric": key, **value}
        for row in rows for key, value in row["semantic_metrics"].items()
    ]
    comparisons = [
        {"test_case_id": row["test_case_id"], "run_index": row["run_index"], "scope": "TOTAL",
         **row["score_comparison"]}
        for row in rows if row["expected_score"] is not None
    ]
    comparisons.extend(
        {"test_case_id": row["test_case_id"], "run_index": row["run_index"], "scope": "ITEM",
         "item_key": item["itemKey"], "source": row["ground_truth_source"], "basis": "CURRENT_VALIDATION",
         "expected_score": item["expected"], "current_score": item["awarded"],
         "absolute_error": abs(item["expected"] - item["awarded"])}
        for row in rows if row["expected_score"] is not None for item in row["deterministic_results"]["items"]
    )
    errors = [
        {**row, "layer": "gemini", "status": row["provider_status"], "detail": row["error_detail"]}
        for row in rows if row["provider_status"] in {"PROVIDER_ERROR", "INVALID_OUTPUT"}
    ] + [
        {**row, "layer": "semantic", "status": row["evaluation_status"],
         "detail": [metric["reason"] for metric in row["semantic_metrics"].values() if metric["status"] == "JUDGE_ERROR"]
         or report["layers"]["semantic"].get("detail")}
        for row in rows if row["evaluation_status"] == "JUDGE_ERROR"
    ]
    failures = [
        {**row, "layer": failure_layer(row["overall_status"]),
         "detail": row["replay_results"]["detail"] if row["overall_status"] == "REPLAY_FAIL" else
         row["error_detail"] or row["error_code"] or "See tests and semantic metric explanations"}
        for row in rows if row["overall_status"] not in {"PASS", "SKIPPED", "NOT_EVALUATED"}
    ] + [
        {**row, "layer": "automated", "overall_status": row["status"], "evidence_paths": row["evidence"]}
        for row in report["automated_results"] if row["status"] == "DETERMINISTIC_FAIL"
    ]
    summary = [
        {"measurement": key, "value": value}
        for key, value in {"overall_status": report["overall_status"], **report["summary"],
                           **{f"{key}_layer": value["status"] for key, value in report["layers"].items()}}.items()
    ]
    sheets = (
        ("Executive Summary", summary, ("measurement", "value")),
        ("Test Cases", rows, CASE_COLUMNS),
        ("Automated Test Results", report["automated_results"], (
            "component", "suite", "name", "test_case_id", "status", "duration_seconds", "detail", "evidence")),
        ("Replay Results", [
            {**row, **row["replay_results"], "current_validation": row["deterministic_results"]["status"],
             "layer": "replay"} for row in rows
        ], ("test_case_id", "run_index", "layer", "status", "current_validation", "engine_status", "actual_score",
            "error_code", "detail", "changes", "evidence_paths")),
        ("Gemini Evaluation", rows, (
            "test_case_id", "run_index", "gemini_model", "provider_status", "provider_http_status",
            "ai_score", "expected_score", "missing_criteria", "actual_gemini_output", "evidence_paths")),
        ("DeepEval Semantic Metrics", metric_rows, METRIC_COLUMNS),
        ("Score Comparison", comparisons, (
            "test_case_id", "run_index", "scope", "item_key", "source", "basis", "expected_score", "current_score", "saved_score",
            "absolute_error")),
        ("Failed Cases & Root Causes", failures, (
            "test_case_id", "run_index", "layer", "overall_status", "name", "detail", "evidence_paths")),
        ("Provider & Judge Errors", errors, ERROR_COLUMNS),
        ("Regression Comparison", report["regressions"], REGRESSION_COLUMNS),
        ("Execution Metadata",
         [{"field": key, "value": value} for key, value in {
             "evaluation_run_id": report["evaluation_run_id"], **report["metadata"], "layers": report["layers"],
             "thresholds": report["thresholds"], "deterministic_metrics": report["deterministic_metrics"]}.items()],
         ("field", "value")),
    )
    with pd.ExcelWriter(output / "ai-grading-evaluation.xlsx", engine="openpyxl") as writer:
        for name, records, columns in sheets:
            frame(records, columns, excel=True).to_excel(writer, sheet_name=name, index=False)
            sheet = writer.sheets[name]
            sheet.freeze_panes = "C2"
            sheet.auto_filter.ref = sheet.dimensions
            for cell in sheet[1]:
                cell.fill = PatternFill("solid", fgColor="17364D")
                cell.font = Font(bold=True, color="FFFFFF")
            for i, column in enumerate(columns, start=1):
                sheet.column_dimensions[get_column_letter(i)].width = (
                    46 if column in {"actual_gemini_output", "reason", "scenario", "value", "error_detail"}
                    else max(16, min(30, len(column) + 3))
                )
            for line in sheet.iter_rows(min_row=2):
                for cell in line:
                    cell.alignment = Alignment(vertical="top", wrap_text=True)
    (output / "ai-grading-evaluation.md").write_text(summary_markdown(report), encoding="utf-8")


def failure_layer(status: str) -> str:
    return {"REPLAY_FAIL": "replay", "DETERMINISTIC_FAIL": "deterministic", "PROVIDER_ERROR": "gemini",
            "INVALID_OUTPUT": "gemini", "JUDGE_ERROR": "semantic", "QUALITY_FAIL": "semantic"}[status]
