import json
from pathlib import Path

import pytest

from test_evaluation import saved_run


def test_replay_is_joined_by_identity_and_preserves_provider_error(saved_run, tmp_path):
    from dataset import BUNDLED_DATASET, load_artifacts, load_dataset
    from runner import merge_replay, overall_status

    _, rows = load_artifacts(load_dataset(BUNDLED_DATASET), saved_run)
    replay_file = tmp_path / "replayed.json"
    replay_file.write_text(json.dumps({
        "assignment": "badminton-booking",
        "run": {"mode": "replay", "provider": "GEMINI", "model": "flash"},
        "runs": [
            {"caseKey": "08-wrong-actors", "runIndex": 2, "status": "PROVIDER_ERROR",
             "errorCode": "REJECTED_503"},
            {"caseKey": "08-wrong-actors", "runIndex": 1, "status": "VALID", "aiTotal": 2.45},
            {"caseKey": "08-wrong-actors", "runIndex": 3, "status": "INVALID_OUTPUT",
             "errorCode": "SCHEMA_MISMATCH"},
        ],
    }), encoding="utf-8")
    merge_replay(rows, saved_run, replay_file)
    assert rows[0].deterministic_results.status == "PASS"
    assert rows[0].replay_results.status == "REPLAY_FAIL"
    assert rows[0].deterministic_results.actual_score == 2.45
    assert overall_status(rows[1]) == "PROVIDER_ERROR"
    assert overall_status(rows[2]) == "INVALID_OUTPUT"
    assert overall_status(rows[3]) == "SKIPPED"


@pytest.mark.parametrize("judge_fails", [False, True])
def test_offline_routes_to_replay_and_preserves_results(monkeypatch, saved_run, tmp_path, judge_fails):
    from evaluate import main

    def run_replay(_args, output, _source):
        replay_folder = output / "raw" / "replay" / "emulated"
        replay_folder.mkdir(parents=True)
        artifact = json.loads(saved_run.read_text(encoding="utf-8"))
        artifact["run"]["mode"] = "replay"
        (replay_folder / "results.json").write_text(json.dumps(artifact), encoding="utf-8")
        return replay_folder / "results.json", {"status": "PASS"}, []

    def unexpected(*_):
        pytest.fail("Offline must not call Gemini live")

    monkeypatch.setattr("evaluate.run_live", unexpected)
    monkeypatch.setattr("evaluate.run_replay", run_replay)
    def semantic(*_):
        if judge_fails:
            raise ValueError("Hosted judge was not allowed for this dataset")
        return {"status": "SKIPPED"}

    monkeypatch.setattr("evaluate.add_semantic_results", semantic)
    out = tmp_path / "unified"
    assert main(["--results", str(saved_run), "--output", str(out)]) == (2 if judge_fails else 0)
    doc = json.loads((out / "ai-grading-evaluation.json").read_text(encoding="utf-8"))
    assert doc["layers"]["automated"]["status"] == "NOT_EVALUATED"
    assert doc["layers"]["replay"]["status"] == "PASS"
    assert doc["layers"]["live"]["status"] == "NOT_EVALUATED"
    assert doc["rows"][0]["evaluation_run_id"] == doc["evaluation_run_id"]
    assert doc["rows"][0]["deterministic_results"]["status"] == "PASS"
    assert doc["rows"][1]["overall_status"] == "PROVIDER_ERROR"
    if judge_fails:
        assert doc["rows"][0]["overall_status"] == "JUDGE_ERROR"
    assert {file.suffix for file in out.glob("ai-grading-evaluation.*")} == {".xlsx", ".json", ".md"}


def test_automated_collector_never_assigns_a_scenario_id(tmp_path):
    from runner import collect_junit

    file = tmp_path / "TEST-Grading.xml"
    file.write_text(
        '<testsuite name="Grading"><testcase classname="Grading" name="valid"/>'
        '<testcase classname="Grading" name="invalid"><failure message="rubric mismatch"/></testcase>'
        '<testcase classname="Grading" name="live"><skipped/></testcase></testsuite>', encoding="utf-8",
    )
    rows = collect_junit([file], "backend")
    assert [row["status"] for row in rows] == ["PASS", "DETERMINISTIC_FAIL", "SKIPPED"]
    assert all(row["test_case_id"] is None for row in rows)


def test_live_requires_opt_in_before_creating_run(tmp_path, monkeypatch):
    from evaluate import main

    def unexpected(*_):
        pytest.fail("Live gateway invocation must be gated")

    monkeypatch.setattr("evaluate.run_live", unexpected)
    out = tmp_path / "unintended"
    assert main(["--mode", "live", "--output", str(out)]) == 1
    assert not out.exists()


def test_report_mode_rebuilds_without_starting_a_test_or_a_judge(monkeypatch, tmp_path):
    from evaluate import main
    from reports import build_report

    def unexpected(*_):
        pytest.fail("Report mode must not execute an evaluation")

    monkeypatch.setattr("evaluate.run_evaluation", unexpected)
    source = tmp_path / "existing.json"
    report = build_report([], {"mode": "automated"}, {})
    source.write_text(json.dumps(report), encoding="utf-8")
    output = tmp_path / "rebuild"
    assert main(["--mode", "report", "--input", str(source), "--baseline", str(source), "--output", str(output)]) == 0
    rebuilt = json.loads((output / "ai-grading-evaluation.json").read_text(encoding="utf-8"))
    assert rebuilt["evaluation_run_id"] == report["evaluation_run_id"]
    assert rebuilt["regressions"] == []
