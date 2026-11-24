import hashlib
import json
import re
from dataclasses import dataclass
from decimal import Decimal
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, Field


ROOT = Path(__file__).resolve().parents[2]
BUNDLED_DATASET = ROOT / "backend/src/test/resources/ai-eval/grading/badminton-booking"


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def fingerprint(value) -> str:
    encoded = json.dumps(value, sort_keys=True, ensure_ascii=False).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def stable_id(assignment: str, key: str) -> str:
    # Matches UUID.nameUUIDFromBytes in GradingEvalDataset.
    digest = hashlib.md5(f"ai-eval:{assignment}:{key}".encode(), usedforsecurity=False).digest()
    return str(UUID(bytes=digest, version=3))


def read_resource(root: Path, relative: str) -> Path:
    path = (root / relative).resolve()
    if not path.is_relative_to(root):
        raise ValueError("Dataset resource is outside the dataset directory")
    return path


class JavaRun(BaseModel):
    caseKey: str
    runIndex: int = Field(ge=1)
    status: Literal["VALID", "PROVIDER_ERROR", "INVALID_OUTPUT"]
    errorCode: str | None = None
    aiTotal: Decimal | None = None
    expectedTotal: Decimal | None = None


class Recording(BaseModel):
    caseKey: str
    runIndex: int
    provider: str
    model: str
    output: str | None = None
    errorKind: str | None = None
    httpStatus: int = 0
    errorMessage: str | None = None
    # Normalised by the backend gateway; outputTokens already includes reasoning tokens.
    usage: dict | None = None


class MetricResult(BaseModel):
    score: float | None = Field(default=None, ge=0, le=1)
    threshold: float = Field(ge=0, le=1)
    reason: str | None = None
    status: Literal["QUALITY_PASS", "QUALITY_FAIL", "JUDGE_ERROR"]


class CheckResult(BaseModel):
    status: Literal["PASS", "DETERMINISTIC_FAIL", "REPLAY_FAIL", "NOT_EVALUATED", "SKIPPED"] = "NOT_EVALUATED"
    engine_status: str | None = None
    actual_score: float | None = None
    error_code: str | None = None
    detail: str | None = None
    items: list[dict] = Field(default_factory=list)
    changes: list[dict] = Field(default_factory=list)


class Row(BaseModel):
    test_case_id: str
    assignment_id: str
    run_index: int
    scenario: str
    assignment: str
    rubric: list[dict]
    applicable_item_ids: list[str]
    submission: str
    expected_behavior: str
    expected_score: float | None
    ground_truth_source: str
    rubric_version: str
    case_version: str
    grading_model: str
    actual_gemini_output: dict | None = None
    output_hash: str | None = None
    ai_score: float | None = None
    input_status: Literal["VALID_OUTPUT", "PROVIDER_ERROR", "INVALID_OUTPUT", "SKIPPED"]
    evaluation_status: Literal["EVALUATED", "SKIPPED", "JUDGE_ERROR"] = "SKIPPED"
    quality_status: Literal["QUALITY_PASS", "QUALITY_FAIL"] | None = None
    error_code: str | None = None
    error_detail: str | None = None
    provider_http_status: int | None = None
    input_tokens: int | None = None
    output_tokens: int | None = None
    missing_criteria: list[str] = Field(default_factory=list)
    metrics: dict[str, MetricResult] = Field(default_factory=dict)
    deterministic_results: CheckResult = Field(default_factory=CheckResult)
    replay_results: CheckResult = Field(default_factory=CheckResult)
    evidence_paths: dict[str, str] = Field(default_factory=dict)


@dataclass(frozen=True)
class Case:
    case_id: str
    scenario: str
    rubric: list[dict]
    submission: str
    expected_behavior: str
    expected_score: Decimal | None
    source: str
    active_items: dict


@dataclass(frozen=True)
class Dataset:
    assignment_id: str
    instructions: str
    rubric_version: str
    dataset_hash: str
    cases: dict[str, Case]


def load_dataset(root: Path) -> Dataset:
    root = root.resolve()
    definition = read_json(root / "assignment.json")
    assignment = definition["assignmentKey"]
    rubric = definition["rubrics"]
    headings = {heading["key"]: heading["text"] for heading in definition["skeleton"]}
    points = {}
    for part in rubric:
        part["id"] = stable_id(assignment, part["key"])
        for criterion in part["criteria"]:
            for item in criterion["items"]:
                item["id"] = stable_id(assignment, item["key"])
                points[item["key"]] = Decimal(str(item["points"]))

    cases = {}
    fixture_digests = {}
    for relative in definition["cases"]:
        fixture = read_json(read_resource(root, relative))
        key = fixture["caseKey"]
        if not re.fullmatch(r"[\w-]+", key) or key in cases:
            raise ValueError(f"Invalid or duplicate case key: {key}")
        submission = []
        active_parts = set()
        part = None
        index = 0
        for block in fixture["blocks"]:
            if "heading" in block:
                part = block["heading"]
                submission.append({"heading": headings[part]})
                continue
            body = dict(block, id=stable_id(assignment, f"{key}#{index}"))
            index += 1
            if block["type"] == "DIAGRAM":
                body["diagram_xml"] = read_resource(root, body.pop("file")).read_text(encoding="utf-8-sig")
            submission.append(body)
            active_parts.add(part)
        active_items = {
            (p["id"], item["id"]): item["key"]
            for p in rubric if p["partHeading"] in active_parts
            for criterion in p["criteria"] for item in criterion["items"]
        }
        source = fixture.get("labelSource", definition.get("labelSource", "UNKNOWN"))
        expected = reference_total(fixture.get("expected"), points, source)
        behavior = fixture.get("expectedBehavior") or "Apply the supplied rubric to the submission evidence."
        if fixture.get("absentItems"):
            behavior += " Reference marks these items absent: " + ", ".join(fixture["absentItems"]) + "."
        cases[key] = Case(
            key, fixture["description"], rubric, json.dumps(submission, ensure_ascii=False),
            behavior, expected, source, active_items,
        )
        fixture_digests[key] = fingerprint([fixture, submission])
    return Dataset(
        assignment, definition["instructions"], fingerprint(rubric),
        fingerprint([definition, fixture_digests]), cases,
    )


def reference_total(expected: dict | None, points: dict, source: str) -> Decimal | None:
    if not expected or source not in {"SYNTHETIC_TEAM", "LECTURER_VERIFIED"}:
        return None
    scores = {key: Decimal(str(value)) for key, value in expected.items()}
    if not scores.keys() <= points.keys() or any(
        not value.is_finite() or not 0 <= value <= points[key] for key, value in scores.items()
    ):
        raise ValueError("Reference score outside its rubric")
    return sum(scores.values(), Decimal(0)) if scores.keys() == points.keys() else None


def load_artifacts(dataset: Dataset, results: Path, case_filter=None, run_filter=None):
    artifact = read_json(results)
    metadata = artifact["run"]
    if artifact["assignment"] != dataset.assignment_id or metadata["provider"] != "GEMINI":
        raise ValueError("Expected Gemini results for the selected assignment")
    if metadata["mode"] != "live":
        raise ValueError("Select the original live results with their sibling recordings directory")
    rows = []
    seen = set()
    for raw in artifact["runs"]:
        run = JavaRun.model_validate(raw)
        sample = dataset.cases.get(run.caseKey)
        if sample is None:
            raise ValueError(f"Result refers to an unknown case: {run.caseKey}")
        key = (run.caseKey, run.runIndex)
        if key in seen:
            raise ValueError(f"Duplicate case and run: {key}")
        seen.add(key)
        if (case_filter and run.caseKey != case_filter) or (run_filter and run.runIndex != run_filter):
            continue
        if sample.expected_score is not None and (
            (run.expectedTotal is not None or run.status == "VALID") and run.expectedTotal != sample.expected_score
        ):
            raise ValueError(f"Saved reference total differs for {run.caseKey}. Check dataset and rubric")
        path = results.parent / "recordings" / run.caseKey / f"run-{run.runIndex}.json"
        record = Recording.model_validate(read_json(path)) if path.is_file() else None
        if record and (record.caseKey, record.runIndex, record.provider, record.model) != (
            run.caseKey, run.runIndex, "GEMINI", metadata["model"],
        ):
            raise ValueError(f"Recording identity mismatch for {run.caseKey} run {run.runIndex}")
        row = adapt_run(dataset, sample, run, record, metadata["model"])
        row.evidence_paths["grading_results"] = str(results.resolve())
        if record:
            row.evidence_paths["gemini_recording"] = str(path.resolve())
        rows.append(row)
    if not rows:
        raise ValueError("No saved attempts match the requested selection")
    return metadata, rows


def adapt_run(dataset: Dataset, sample: Case, run: JavaRun, record: Recording | None, model: str) -> Row:
    status = run.status
    error = run.errorCode
    detail = record.errorMessage if record else None
    payload = None
    missing = []
    http = record.httpStatus if record else 0
    usage = (record.usage if record else None) or {}
    if status == "PROVIDER_ERROR":
        code = re.search(r"_(\d{3})$", error or "")
        http = http or (int(code[1]) if code else 0)
    if status == "VALID":
        if record and record.errorKind:
            raise ValueError(f"Successful result has a failed recording for {sample.case_id}")
        if not record or not record.output:
            status, error = "SKIPPED", "NO_GEMINI_RECORDING"
        else:
            try:
                payload = json.loads(record.output)
                missing = read_missing_items(payload, sample.active_items)
                status = "VALID_OUTPUT"
            except (ValueError, TypeError, KeyError, AttributeError):
                payload = None
                status, error = "INVALID_OUTPUT", "UNREADABLE_GEMINI_OUTPUT"
    return Row(
        test_case_id=sample.case_id, assignment_id=dataset.assignment_id, run_index=run.runIndex,
        scenario=sample.scenario, assignment=dataset.instructions, rubric=sample.rubric,
        applicable_item_ids=[item_id for _, item_id in sample.active_items],
        submission=sample.submission, expected_behavior=sample.expected_behavior,
        expected_score=sample.expected_score, ground_truth_source=sample.source,
        rubric_version=dataset.rubric_version, case_version=fingerprint(sample.submission),
        grading_model=model, actual_gemini_output=payload,
        output_hash=fingerprint(payload) if payload is not None else None,
        ai_score=run.aiTotal if status == "VALID_OUTPUT" else None,
        input_status=status, error_code=error, error_detail=detail, provider_http_status=http or None,
        input_tokens=usage.get("inputTokens"), output_tokens=usage.get("outputTokens"),
        missing_criteria=missing,
    )


def read_missing_items(payload: dict, items: dict) -> list[str]:
    scores = {(rubric_id, item_id) for rubric_id, awarded in payload["itemScoresByRubric"].items()
              for item_id in awarded.keys()}
    feedback = {(item["rubricId"], item["itemId"]) for item in payload["structuredFeedback"]["items"]}
    if not (scores | feedback) <= items.keys():
        raise ValueError("Response contains item IDs outside the active rubric")
    return sorted(items[identity] for identity in items.keys() - (scores & feedback))
