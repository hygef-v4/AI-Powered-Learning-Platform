import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { gradingApi } from "@/features/grading/api";
import { assignmentsApi } from "@/features/assignments/api";
import { GradeDistributionSettings } from "@/features/grading/components/GradeDistributionSettings";
import { AssignmentSettingsPanel, settingsOf } from "@/features/assignments/components/AssignmentSettingsPanel";
import { AssignmentFormPage } from "@/features/assignments/pages/AssignmentFormPage";
import type { AssignmentDetail } from "@/features/assignments/model/types";
import "@/features/grading/register";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useSearchParams: () => new URLSearchParams("classId=class-context"),
}));

const label = "Cho sinh viên xem phổ điểm ẩn danh";
const initial = { enabled: false, version: 0, canEdit: true };
const subjectAssignment = {
  id: "a", classId: null, subjectId: "s", scope: "SUBJECT", title: "Bài của môn", instructions: "",
  gradingMode: "GRADED", submissionMode: "INDIVIDUAL", status: "OPEN", skeleton: { blocks: [] }, parts: [],
  requiredDiagrams: {}, referenceMaterialIds: [], diagramEnabled: true, activityMonitoringEnabled: false,
  activityNotice: null, permissions: { canEditContent: false, canManage: false },
} as unknown as AssignmentDetail;

afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });

describe("assignment/class grade distribution visibility", () => {
  it.each([30, 31])("uses the server AI limit for the authoring page with %s rubric items", async (count) => {
    const detail = { ...subjectAssignment, maxAiGradingRubricItems: 30,
      skeleton: { blocks: [{ id: "p", type: "HEADING", origin: "TEACHER", content: { style: "H1", text: "Question" } }] },
      parts: [{ partId: "p", headingBlockId: "p", ancestorHeadingIds: [], title: "Question", rubricId: "r",
        criteriaCount: 2, totalPoints: 12, rubricComplete: true, locked: false }] } as AssignmentDetail;
    vi.spyOn(assignmentsApi, "detail").mockResolvedValue(detail);
    vi.spyOn(assignmentsApi, "rubric").mockResolvedValue({ rubricId: "r", assignmentId: "a", partId: "p",
      partTitle: "Question", criteria: [{ criterionId: "c", title: "Criterion", description: "", kind: "TEXT",
        items: Array.from({ length: count }, (_, index) => ({ itemId: `i${index}`, text: `Requirement ${index}`, points: 1 })) }],
      totalPoints: 12, locked: false, complete: true, diagramEnabled: false, editable: false, assignmentVersion: 1 });
    render(<AssignmentFormPage id="a" />);
    await screen.findByText("Question");
    await waitFor(() => expect(assignmentsApi.rubric).toHaveBeenCalled());
    if (count > 30) expect(await screen.findByText(/Rubric có 31 mục, vượt giới hạn 30/)).toBeVisible();
    else expect(screen.queryByText(/vượt giới hạn/)).not.toBeInTheDocument();
  });
  it("uses the versioned GET/PUT contract and encodes both path identifiers", async () => {
    const fetch = vi.fn().mockImplementation(() => Promise.resolve(new Response(JSON.stringify(initial))));
    vi.stubGlobal("fetch", fetch);
    expect(await gradingApi.loadDistributionVisibility("c/1", "a/1")).toEqual(initial);
    expect(fetch.mock.calls[0]![0]).toBe("/api/v1/classes/c%2F1/assignments/a%2F1/grade-distribution-visibility");
    expect(await gradingApi.saveDistributionVisibility("c/1", "a/1", true, 7)).toEqual(initial);
    expect(fetch.mock.calls[1]![1]).toMatchObject({ method: "PUT", body: JSON.stringify({ enabled: true, version: 7 }) });
  });

  it("starts off, saves immediately, waits for confirmation, and uses the returned version for the next save", async () => {
    vi.spyOn(gradingApi, "loadDistributionVisibility").mockResolvedValue(initial);
    let confirm!: (value: typeof initial) => void;
    const save = vi.spyOn(gradingApi, "saveDistributionVisibility").mockImplementationOnce(() => new Promise((resolve) => { confirm = resolve; }))
      .mockResolvedValue({ ...initial, version: 2 });
    render(<GradeDistributionSettings classId="c" assignmentId="a" gradingMode="GRADED" />);
    const toggle = await screen.findByRole("switch", { name: label });
    expect(toggle).toHaveAttribute("aria-checked", "false");
    expect(screen.getByText(/ít nhất 20 kết quả/)).toHaveTextContent("điểm đã công bố");
    fireEvent.click(toggle);
    expect(save).toHaveBeenCalledWith("c", "a", true, 0);
    // Gạt ngay sang bật và khóa lại trong lúc chờ server.
    expect(toggle).toBeDisabled();
    expect(toggle).toHaveAttribute("aria-checked", "true");
    await act(async () => confirm({ enabled: true, version: 1, canEdit: true }));
    expect(toggle).toHaveAttribute("aria-checked", "true");
    fireEvent.click(toggle);
    await waitFor(() => expect(save).toHaveBeenLastCalledWith("c", "a", false, 1));
  });

  it("does not load PRACTICE settings", () => {
    const load = vi.spyOn(gradingApi, "loadDistributionVisibility");
    render(<GradeDistributionSettings classId="c" assignmentId="a" gradingMode="PRACTICE" />);
    expect(load).not.toHaveBeenCalled();
    expect(screen.queryByRole("switch")).not.toBeInTheDocument();
  });

  it.each([403, 404])("hides an unauthorized GET (%s) without an editable default", async (status) => {
    vi.spyOn(gradingApi, "loadDistributionVisibility").mockRejectedValue(new ApiError({ status }));
    await act(async () => { render(<GradeDistributionSettings classId="c" assignmentId="a" gradingMode="GRADED" />); });
    expect(screen.queryByRole("switch")).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("hides the control when the server denies editing", async () => {
    vi.spyOn(gradingApi, "loadDistributionVisibility").mockResolvedValue({ ...initial, canEdit: false });
    await act(async () => { render(<GradeDistributionSettings classId="c" assignmentId="a" gradingMode="GRADED" />); });
    expect(screen.queryByRole("switch")).not.toBeInTheDocument();
  });

  it("offers GET retry on server/network failure without exposing an unverified switch", async () => {
    const load = vi.spyOn(gradingApi, "loadDistributionVisibility").mockRejectedValueOnce(new Error("offline")).mockResolvedValue(initial);
    render(<GradeDistributionSettings classId="c" assignmentId="a" gradingMode="GRADED" />);
    expect(await screen.findByRole("alert")).toHaveTextContent("Không tải được");
    expect(screen.queryByRole("switch")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Tải lại cài đặt phổ điểm" }));
    expect(await screen.findByRole("switch", { name: label })).toBeEnabled();
    expect(load).toHaveBeenCalledTimes(2);
  });

  it.each([409, 500, 0])("reports failed PUT (%s; 0 is offline), retains confirmed state and reloads before retrying", async (status) => {
    const load = vi.spyOn(gradingApi, "loadDistributionVisibility").mockResolvedValueOnce(initial)
      .mockResolvedValue({ ...initial, enabled: true, version: 5 });
    const save = vi.spyOn(gradingApi, "saveDistributionVisibility").mockRejectedValueOnce(status ? new ApiError({ status }) : new Error("offline"))
      .mockResolvedValue({ ...initial, version: 6 });
    render(<GradeDistributionSettings classId="c" assignmentId="a" gradingMode="GRADED" />);
    fireEvent.click(await screen.findByRole("switch", { name: label }));
    expect(await screen.findByRole("alert")).toHaveTextContent(status === 409 ? "đã thay đổi" : "Chưa lưu được");
    const toggle = screen.getByRole("switch", { name: label });
    expect(toggle).toHaveAttribute("aria-checked", "false");
    expect(toggle).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Tải lại cài đặt phổ điểm" }));
    const reloaded = await screen.findByRole("switch", { name: label });
    expect(reloaded).toHaveAttribute("aria-checked", "true");
    fireEvent.click(reloaded);
    await waitFor(() => expect(save).toHaveBeenLastCalledWith("c", "a", false, 5));
    expect(load).toHaveBeenCalledTimes(2);
  });

  it("isolates shared SUBJECT settings by class and ignores an old class response", async () => {
    let oldResponse!: (value: typeof initial) => void;
    const load = vi.spyOn(gradingApi, "loadDistributionVisibility").mockImplementationOnce(() => new Promise((resolve) => { oldResponse = resolve; }))
      .mockResolvedValue(initial);
    const save = vi.spyOn(gradingApi, "saveDistributionVisibility").mockResolvedValue({ ...initial, enabled: true, version: 1 });
    const props = { detail: subjectAssignment, value: settingsOf(subjectAssignment), onChange: vi.fn(), readOnly: true };
    const { rerender } = render(<AssignmentSettingsPanel {...props} classId="c1" />);
    rerender(<AssignmentSettingsPanel {...props} classId="c2" />);
    const toggle = await screen.findByRole("switch", { name: label });
    await act(async () => oldResponse({ ...initial, enabled: true, version: 99 }));
    expect(toggle).toHaveAttribute("aria-checked", "false");
    expect(load.mock.calls.map((call) => call.slice(0, 2))).toEqual([["c1", "a"], ["c2", "a"]]);
    fireEvent.click(toggle);
    await waitFor(() => expect(save).toHaveBeenCalledWith("c2", "a", true, 0));
    expect(props.onChange).not.toHaveBeenCalled();
    expect(screen.getByRole("radio", { name: "Graded" })).toBeDisabled();
  });

  it("loads no settings for a SUBJECT assignment without a class context", () => {
    const load = vi.spyOn(gradingApi, "loadDistributionVisibility");
    render(<AssignmentSettingsPanel detail={subjectAssignment} value={settingsOf(subjectAssignment)} onChange={vi.fn()} readOnly />);
    expect(load).not.toHaveBeenCalled();
  });

  it("passes the page's query class context into settings even when SUBJECT content is read-only", async () => {
    vi.spyOn(assignmentsApi, "detail").mockResolvedValue(subjectAssignment);
    const update = vi.spyOn(assignmentsApi, "updateDetails");
    const load = vi.spyOn(gradingApi, "loadDistributionVisibility").mockResolvedValue(initial);
    const save = vi.spyOn(gradingApi, "saveDistributionVisibility").mockResolvedValue({ ...initial, enabled: true, version: 1 });
    render(<AssignmentFormPage id="a" />);
    fireEvent.click(await screen.findByRole("button", { name: "Cài đặt bài" }));
    fireEvent.click(await screen.findByRole("switch", { name: label }));
    await waitFor(() => expect(save).toHaveBeenCalledWith("class-context", "a", true, 0));
    expect(load).toHaveBeenCalledWith("class-context", "a", expect.any(AbortSignal));
    expect(update).not.toHaveBeenCalled();
  });
});
