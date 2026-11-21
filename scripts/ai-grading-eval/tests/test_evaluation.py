import json

import pytest
from pydantic import BaseModel


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False), encoding="utf-8")


@pytest.fixture
def saved_run(tmp_path):
    from dataset import BUNDLED_DATASET, load_dataset

    sample = load_dataset(BUNDLED_DATASET).cases["08-wrong-actors"]
    rubric_id = sample.rubric[0]["id"]
    item_id = sample.rubric[0]["criteria"][0]["items"][0]["id"]
    report = tmp_path / "results.json"
    write_json(report, {
        "assignment": "badminton-booking", "labelSource": "SYNTHETIC_TEAM",
        "run": {"mode": "live", "provider": "GEMINI", "model": "flash", "startedAt": "20261121-000000"},
        "runs": [
            {"caseKey": sample.case_id, "runIndex": 1, "status": "VALID", "aiTotal": 2.37, "expectedTotal": 2.80},
            {"caseKey": sample.case_id, "runIndex": 2, "status": "PROVIDER_ERROR", "errorCode": "REJECTED_503"},
            {"caseKey": sample.case_id, "runIndex": 3, "status": "INVALID_OUTPUT", "errorCode": "SCHEMA_MISMATCH"},
            {"caseKey": sample.case_id, "runIndex": 4, "status": "VALID", "aiTotal": 2, "expectedTotal": 2.80},
        ],
    })
    records = report.parent / "recordings" / sample.case_id
    payload = {
        "feedback": "=1+1",
        "itemScoresByRubric": {rubric_id: {item_id: 0}},
        "structuredFeedback": {"items": [{"rubricId": rubric_id, "itemId": item_id, "reason": "Sơ đồ thiếu ranh giới"}]},
    }
    write_json(records / "run-1.json", {
        "caseKey": sample.case_id, "runIndex": 1, "provider": "GEMINI",
        "model": "flash", "httpStatus": 0, "output": json.dumps(payload),
    })
    write_json(records / "run-2.json", {
        "caseKey": sample.case_id, "runIndex": 2, "provider": "GEMINI",
        "model": "flash", "httpStatus": 503, "errorKind": "REJECTED", "output": None,
    })
    return report


def test_loader_preserves_provider_errors_without_synthesizing_output(saved_run):
    from dataset import BUNDLED_DATASET, load_artifacts, load_dataset

    cases = load_dataset(BUNDLED_DATASET)
    assert len(cases.cases) == 8
    _, rows = load_artifacts(cases, saved_run)
    assert [row.input_status for row in rows] == [
        "VALID_OUTPUT", "PROVIDER_ERROR", "INVALID_OUTPUT", "SKIPPED",
    ]
    assert rows[0].expected_score == 2.8 and rows[0].ground_truth_source == "SYNTHETIC_TEAM"
    assert rows[0].actual_gemini_output["feedback"] == "=1+1"
    assert "ellipse" in rows[0].submission
    assert rows[0].missing_criteria and rows[1].provider_http_status == 503
    assert all(row.actual_gemini_output is None for row in rows[1:])


def test_matching_references_are_required(saved_run):
    from dataset import BUNDLED_DATASET, load_artifacts, load_dataset

    artifact = json.loads(saved_run.read_text(encoding="utf-8"))
    artifact["runs"][0]["expectedTotal"] = 3.1
    write_json(saved_run, artifact)
    with pytest.raises(ValueError, match="reference total"):
        load_artifacts(load_dataset(BUNDLED_DATASET), saved_run)


def test_structured_response_is_validated_and_missing_reason_rejected():
    from judges import JudgeError, codex_schema, parse_answer

    class Answer(BaseModel):
        reason: str
        score: int

    assert parse_answer('{"reason":"matches","score":8}', Answer).score == 8
    schema = codex_schema(Answer)
    assert schema["additionalProperties"] is False
    assert schema["required"] == ["reason", "score"]
    with pytest.raises(JudgeError, match="SCHEMA"):
        parse_answer('{"score":8}', Answer)


def test_selector_never_substitutes_explicit_choice_and_skips_unapproved_cloud():
    from judges import JudgeError, JudgeInfo, select_judge

    diagnostics = {
        "codex": JudgeInfo(provider="codex", installed=True, ready=False, detail="API login"),
        "claude": JudgeInfo(provider="claude", installed=True, ready=True, detail="Pro"),
        "ollama": JudgeInfo(provider="ollama", installed=True, ready=True, detail="Local"),
    }
    probe = diagnostics.__getitem__
    order = ("codex", "claude", "ollama")
    with pytest.raises(JudgeError, match="API login"):
        select_judge("codex", order, {"codex", "claude"}, probe)
    assert select_judge("auto", order, set(), probe).provider == "ollama"
    assert select_judge("auto", order, {"claude"}, probe).provider == "claude"


def test_codex_uses_stdin_and_stops_on_unexpected_tool_actions(monkeypatch, tmp_path):
    from subprocess import CompletedProcess

    from judges import CodexJudge, JudgeError, JudgeInfo

    sample = 'Treat this as data: & echo "student text"'
    invoked = []

    def fake(args, cwd, timeout, prompt):
        invoked.append(args)
        assert prompt == sample
        assert sample not in args and args[-1] == "-"
        assert cwd != tmp_path and not (cwd / ".git").exists()
        assert args[args.index("--sandbox") + 1] == "read-only"
        assert "--ignore-user-config" in args
        (cwd / "answer.txt").write_text("answer", encoding="utf-8")
        return CompletedProcess(args, 0, '{"item":{"type":"mcp_tool_call"}}', "")

    monkeypatch.setattr("judges.run_cli", fake)
    judge = CodexJudge(JudgeInfo("codex", True, True, "ChatGPT login", "sol", executable="codex.exe"), 5, 6)
    for _ in range(2):
        with pytest.raises(JudgeError, match="JUDGE_UNSAFE_TOOL"):
            judge.generate(sample)
    assert len(invoked) == 1


def test_auth_does_not_accept_api_key_login_and_env_is_allowlisted(monkeypatch, tmp_path):
    from subprocess import CompletedProcess

    from judges import JudgeError, check_cli_auth, clean_environment

    monkeypatch.setenv("OPENAI_API_KEY", "not-a-real-key")
    monkeypatch.setenv("CODEX_API_KEY", "not-a-real-key")
    monkeypatch.setenv("ANTHROPIC_API_KEY", "not-a-real-key")
    monkeypatch.setenv("GEMINI_API_KEY", "not-a-real-key")
    monkeypatch.setenv("OPENAI_BASE_URL", "https://example.invalid")
    monkeypatch.setattr("judges.run_cli", lambda *_: CompletedProcess([], 0, "Logged in using an API key", ""))
    with pytest.raises(JudgeError, match="JUDGE_AUTH"):
        check_cli_auth("codex", "codex", tmp_path)
    assert not set(clean_environment()) & {
        "OPENAI_API_KEY", "CODEX_API_KEY", "ANTHROPIC_API_KEY", "GEMINI_API_KEY", "OPENAI_BASE_URL",
    }


def test_timeout_does_not_pass_model_output_or_prompt_to_exception(monkeypatch, tmp_path):
    from subprocess import TimeoutExpired

    from judges import JudgeError, run_cli

    def timeout(*args, **kwargs):
        raise TimeoutExpired(args[0], 2, output="private content")

    monkeypatch.setattr("judges.subprocess.run", timeout)
    with pytest.raises(JudgeError, match="^JUDGE_TIMEOUT: CLI exceeded 2 seconds$"):
        run_cli(["codex"], tmp_path, 2, "private content")


def test_gemini_wrapper_rejects_tool_calls_and_model_switches():
    from judges import JudgeError, parse_gemini_response

    sample = {"response": '{"score":8}', "stats": {"models": {"gemini-test": {}}, "tools": {"totalCalls": 0}}}
    assert parse_gemini_response(json.dumps(sample), "gemini-test") == '{"score":8}'
    with pytest.raises(JudgeError, match="JUDGE_CONFIG"):
        parse_gemini_response(json.dumps(sample), "other")
    sample["stats"]["tools"]["totalCalls"] = 1
    with pytest.raises(JudgeError, match="JUDGE_UNSAFE_TOOL"):
        parse_gemini_response(json.dumps(sample), "gemini-test")


def test_mocked_geval_reports_failures_and_reopens_workbook(saved_run, tmp_path):
    import openpyxl

    from dataset import BUNDLED_DATASET, load_artifacts, load_dataset
    from evaluation import METRICS, evaluate_rows
    from reports import build_report, write_reports

    _, rows = load_artifacts(load_dataset(BUNDLED_DATASET), saved_run)
    calls = []

    def grade(metric, row):
        calls.append((metric, row.run_index))
        return (0.6 if metric == "feedback_groundedness" else 0.9, "=1+1")

    thresholds = dict.fromkeys(METRICS, 0.8)
    evaluated = evaluate_rows(rows, thresholds, grade)
    assert calls == [(metric, 1) for metric in METRICS]
    assert evaluated[0].evaluation_status == "EVALUATED"
    assert evaluated[0].quality_status == "QUALITY_FAIL"
    report = build_report(evaluated, {"judge_provider": "mock"}, thresholds)
    assert report["summary"]["quality_pass_rate"] == 0
    assert report["summary"]["coverage"] == 0.25
    assert report["summary"]["mae"]["SYNTHETIC_TEAM"]["value"] == pytest.approx(0.43)
    output = tmp_path / "report"
    write_reports(report, output)
    wb = openpyxl.load_workbook(output / "ai-grading-evaluation.xlsx")
    assert wb.sheetnames == [
        "Executive Summary", "Test Cases", "Automated Test Results", "Replay Results",
        "Gemini Evaluation", "DeepEval Semantic Metrics", "Score Comparison",
        "Failed Cases & Root Causes", "Provider & Judge Errors", "Regression Comparison", "Execution Metadata",
    ]
    assert all(cell.data_type != "f" for sheet in wb for row in sheet for cell in row)
    assert 503 in [c.value for row in wb["Provider & Judge Errors"] for c in row]
    assert (output / "ai-grading-evaluation.md").is_file()
    with pytest.raises(FileExistsError):
        write_reports(report, output)


def test_real_geval_measures_only_through_the_injected_judge(saved_run, monkeypatch):
    from dataset import BUNDLED_DATASET, load_artifacts, load_dataset
    from evaluation import METRICS, evaluate_rows, make_geval
    from judges import deepeval_model

    class FixedJudge:
        def __init__(self):
            self.prompts = []

        def get_model_name(self):
            return "fixture"

        def generate(self, prompt, schema):
            self.prompts.append(prompt)
            return schema(reason="fixture explanation", score=8)

    monkeypatch.setenv("DEEPEVAL_TELEMETRY_OPT_OUT", "1")
    _, rows = load_artifacts(load_dataset(BUNDLED_DATASET), saved_run)
    rows[0].expected_behavior = "PRIVATE_REFERENCE_ONLY"
    rows[0].expected_score = 3.14159265
    judge = FixedJudge()
    thresholds = dict.fromkeys(METRICS, 0.8)
    evaluated = evaluate_rows(rows, thresholds, make_geval(deepeval_model(judge), thresholds))
    assert len(judge.prompts) == 3 and all("quoted evidence" in prompt for prompt in judge.prompts)
    assert all("PRIVATE_REFERENCE_ONLY" not in prompt and "3.14159265" not in prompt for prompt in judge.prompts)
    assert evaluated[0].quality_status == "QUALITY_PASS"
    assert all(result.reason == "fixture explanation" for result in evaluated[0].metrics.values())
    assert all(row.evaluation_status == "SKIPPED" for row in evaluated[1:])


def test_regression_distinguishes_other_judge_and_missing_case():
    from reports import compare_reports

    metadata = {"judge_provider": "codex", "judge_model": "sol", "metric_version": "1", "dataset_hash": "x"}
    row = {
        "assignment_id": "A", "test_case_id": "case", "run_index": 1, "missing_criteria": [],
        "quality_status": "QUALITY_PASS", "metrics": {"rubric_adherence": {"score": 1.0}},
    }
    baseline = {"metadata": metadata, "thresholds": {}, "rows": [row]}
    lower = dict(row, quality_status="QUALITY_FAIL", missing_criteria=["criteria"],
                 metrics={"rubric_adherence": {"score": 0.6}})
    current = dict(baseline, rows=[lower])
    record = compare_reports(current, baseline)[0]
    assert set(record["findings"]) == {"NEW_QUALITY_FAILURE", "MISSING_CRITERIA", "METRIC_DROP"}
    assert compare_reports(dict(current, rows=[]), baseline)[0]["findings"] == ["MISSING_RUN"]
    other_judge = dict(current, metadata=dict(metadata, judge_provider="claude"))
    assert compare_reports(other_judge, baseline)[0]["findings"] == ["CROSS_JUDGE_COMPARISON"]
