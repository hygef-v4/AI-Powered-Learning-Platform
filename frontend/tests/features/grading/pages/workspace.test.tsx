import { render, screen, fireEvent, waitFor, cleanup, act, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, it, expect, vi } from "vitest";
import { GradingWorkspacePage } from "@/features/grading/pages/GradingWorkspacePage";
import { ApiError } from "@/shared/api/client";
import { Toaster } from "@/shared/ui";
import type { Detail } from "@/features/grading/model/types";
const mocks = vi.hoisted(() => ({
  loadEvaluation: vi.fn(),
  saveEvaluation: vi.fn(),
  listProposals: vi.fn(),
  requestProposal: vi.fn(),
  push: vi.fn(),
  loadTask: vi.fn(),
  saveMemberGrade: vi.fn(),
  listSubmissions: vi.fn(),
}));
const originalScrollIntoView = Object.getOwnPropertyDescriptor(HTMLElement.prototype, "scrollIntoView");
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: mocks.push }) }));
vi.mock("@/features/grading/api", () => ({ gradingApi: mocks }));
const detail: Detail = {
  evaluation: {
    id: "e",
    classId: "c",
    assignmentId: "a",
    attemptId: "t",
    groupDocumentId: null,
    accountId: "s",
    kind: "ATTEMPT",
    submissionHash: "hash",
    state: "DRAFT",
    score: 0,
    maxScore: 10,
    version: 2,
    rubricChecks: "{}",
    adjustment: 0,
    adjustmentReason: null,
    feedback: "original",
    publishedAt: null,
    changes: [],
  },
  document: { blocks: [] },
  rubrics: [],
  navigationIds: ["e", "next"],
  activity: null,
  activityState: "DISABLED",
  members: [],
  memberActivities: [],
  sectionAuthors: [],
  current: true,
};
beforeEach(() => {
  vi.useRealTimers();
  vi.resetAllMocks();
  mocks.loadEvaluation.mockResolvedValue(detail);
  mocks.listProposals.mockResolvedValue([]);
  mocks.listSubmissions.mockResolvedValue({ rows: [
    { student: { accountId: "s", name: "Nguyễn An", email: "an@school.test" }, evaluationId: "e" },
  ], total: 1, page: 1, size: 20 });
});
afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  if (originalScrollIntoView) Object.defineProperty(HTMLElement.prototype, "scrollIntoView", originalScrollIntoView);
  else Reflect.deleteProperty(HTMLElement.prototype, "scrollIntoView");
  vi.useRealTimers();
});

function mockGroupWorkspace(wide = true) {
  vi.stubGlobal("matchMedia", () => ({ matches: wide, addEventListener: vi.fn(), removeEventListener: vi.fn() }));
  const group: Detail = {
    ...detail,
    evaluation: { ...detail.evaluation, kind: "GROUP_DOCUMENT", groupDocumentId: "doc", accountId: null, score: 8 },
    document: { blocks: [
      { id: "p", type: "HEADING", origin: "TEACHER", content: { style: "H1", text: "Class Diagram" } },
      { id: "answer", type: "PARAGRAPH", origin: "STUDENT", content: { runs: [{ text: "Bài làm sơ đồ lớp" }] } },
      { id: "q", type: "HEADING", origin: "TEACHER", content: { style: "H1", text: "Sequence Diagram" } },
    ] },
    members: ["s1", "s2"].map((accountId) => ({ ...detail.evaluation, id: `member-${accountId}`, kind: "MEMBER", accountId, score: 8 })),
    memberActivities: ["s1", "s2"].map((accountId, index) => ({ student: { accountId, name: index ? "Bình" : "An", email: `${accountId}@school.test` }, activity: null, state: "NO_DATA" })),
    sectionAuthors: [{ partId: "p", accountId: "s1" }, { partId: "q", accountId: "s2" }],
  };
  mocks.loadEvaluation.mockResolvedValue(group);
  return group;
}

it("grades a member beside the paper and opens the right person from a section", async () => {
  mockGroupWorkspace();
  const scrollIntoView = vi.fn();
  Object.defineProperty(HTMLElement.prototype, "scrollIntoView", { value: scrollIntoView, configurable: true });
  const showPart = vi.fn();
  window.addEventListener("document:show-part", showPart);
  render(<GradingWorkspacePage evaluationId="e" />);
  expect(await screen.findByRole("tab", { name: "Rubric nhóm" })).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Đánh giá Bình" }));
  expect(screen.getByRole("combobox", { name: "Thành viên" })).toHaveValue("s2");
  expect(screen.getByRole("tab", { name: "Thành viên" })).toHaveAttribute("aria-selected", "true");
  expect(screen.getByTestId("document-side-panel")).toContainElement(screen.getByLabelText("Điểm cá nhân"));
  fireEvent.change(screen.getByLabelText(/Căn cứ đánh giá/), { target: { value: "Đã vấn đáp Bình" } });
  fireEvent.click(screen.getByRole("button", { name: "Xem phần Sequence Diagram" }));
  expect(showPart).toHaveBeenCalled();
  await waitFor(() => expect(scrollIntoView).toHaveBeenCalled());
  fireEvent.click(screen.getByRole("tab", { name: "Rubric nhóm" }));
  fireEvent.click(screen.getByRole("tab", { name: "Thành viên" }));
  expect(screen.getByLabelText(/Căn cứ đánh giá/)).toHaveValue("Đã vấn đáp Bình");
  const leaving = new Event("beforeunload", { cancelable: true });
  window.dispatchEvent(leaving);
  expect(leaving.defaultPrevented).toBe(true);
  fireEvent.click(screen.getByRole("button", { name: "Công bố điểm" }));
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Bài sau" }));
  expect(await screen.findByRole("dialog")).toHaveTextContent("Bạn có thay đổi chưa lưu");
  expect(mocks.push).not.toHaveBeenCalled();
  window.removeEventListener("document:show-part", showPart);
});

it("keeps a mobile member draft after closing and reopening the modeless drawer", async () => {
  mockGroupWorkspace(false);
  render(<GradingWorkspacePage evaluationId="e" />);
  fireEvent.click(await screen.findByRole("button", { name: "Điểm thành viên" }));
  fireEvent.change(screen.getByLabelText(/Căn cứ đánh giá/), { target: { value: "Ghi chú trên mobile" } });
  fireEvent.keyDown(screen.getByRole("region", { name: "Chấm bài nhóm" }), { key: "Escape" });
  expect(screen.queryByRole("region", { name: "Chấm bài nhóm" })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Điểm thành viên" }));
  expect(screen.getByLabelText(/Căn cứ đánh giá/)).toHaveValue("Ghi chú trên mobile");
});

it("keeps every recorded author on a shared section and only lists their known parts", async () => {
  const group = mockGroupWorkspace();
  group.sectionAuthors.push({ partId: "p", accountId: "s2" });
  render(<GradingWorkspacePage evaluationId="e" />);
  expect(await screen.findByText("Người thực hiện: An, Bình")).toBeInTheDocument();
  fireEvent.click(within(screen.getByTestId("block-p").closest("section")!)
    .getByRole("button", { name: "Đánh giá Bình" }));
  expect(screen.getByRole("button", { name: "Xem phần Class Diagram" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Xem phần Sequence Diagram" })).toBeInTheDocument();
});
/** Đề xuất AI mở bằng nút trên thanh công cụ của tài liệu. */
function openAi() {
  const btn = screen.queryByRole("button", { name: "Xem đề xuất AI" }) || screen.getByRole("button", { name: "Chấm bằng AI" });
  fireEvent.click(btn);
}
describe("workspace save", () => {
  it("keeps published grades editable, removes finalize and shows the student-visible save notice", async () => {
    mocks.loadEvaluation.mockResolvedValue({ ...detail, editable: true,
      evaluation: { ...detail.evaluation, state: "PUBLISHED" } });
    render(<GradingWorkspacePage evaluationId="e" />);
    expect(await screen.findByRole("textbox", { name: "Nhận xét chung" })).toBeEnabled();
    expect(screen.queryByRole("button", { name: "Chốt điểm" })).not.toBeInTheDocument();
    expect(screen.getByText("Thay đổi sẽ hiển thị cho sinh viên sau khi lưu.")).toBeInTheDocument();
  });

  it("keeps manual editing and navigation available while an AI submission is being dispatched", async () => {
    let finish!: (proposal: unknown) => void;
    mocks.requestProposal.mockReturnValue(new Promise((resolve) => { finish = resolve; }));
    render(<GradingWorkspacePage evaluationId="e" />);
    const input = await screen.findByRole("textbox", { name: "Nhận xét chung" });
    fireEvent.click(screen.getByRole("button", { name: "Chấm bằng AI" }));
    expect(input).toBeEnabled();
    expect(screen.getByRole("button", { name: "Bài sau" })).toBeEnabled();
    expect(screen.queryByTestId("ai-progress")).not.toBeInTheDocument();
    fireEvent.change(input, { target: { value: "Teacher keeps working" } });
    await act(async () => {
      finish({ id: "ready", status: "READY", versionNo: 1,
        proposal: { checks: {}, score: 0, feedback: "Late AI result" } });
    });
    expect(input).toHaveValue("Teacher keeps working");
    expect(mocks.saveEvaluation).not.toHaveBeenCalled();
  });
  it("waits for 30 idle seconds after the latest edit while Ctrl+S saves immediately", async () => {
    mocks.loadEvaluation.mockResolvedValue({ ...detail, evaluation: { ...detail.evaluation, rubricItemScores: {} },
      rubrics: [{ rubricId: "r", partId: "p", maxPoints: 1,
        criteria: [{ criterionId: "c", title: "Design", items: [{ itemId: "i", text: "Test criterion", points: 1 }] }] }] });
    mocks.saveEvaluation.mockResolvedValue({ ...detail.evaluation, version: 3 });
    render(<GradingWorkspacePage evaluationId="e" />);
    const feedback = await screen.findByRole("textbox", { name: "Nhận xét chung" });
    await act(async () => { await Promise.resolve(); });
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    fireEvent.change(feedback, { target: { value: "First edit" } });
    await act(async () => { await vi.advanceTimersByTimeAsync(29000); });
    fireEvent.change(feedback, { target: { value: "Latest edit" } });
    await act(async () => { await vi.advanceTimersByTimeAsync(29000); });
    expect(mocks.saveEvaluation).not.toHaveBeenCalled();
    act(() => screen.getByRole("spinbutton", { name: "Điểm mục Test criterion" }).focus());
    await act(async () => { await vi.advanceTimersByTimeAsync(1000); });
    expect(mocks.saveEvaluation).toHaveBeenCalledTimes(1);
    expect(mocks.saveEvaluation).toHaveBeenLastCalledWith("e", expect.objectContaining({ feedback: "Latest edit" }));
    fireEvent.change(feedback, { target: { value: "Save now" } });
    await act(async () => { fireEvent.keyDown(window, { key: "s", ctrlKey: true }); });
    expect(mocks.saveEvaluation).toHaveBeenCalledTimes(2);
    expect(mocks.saveEvaluation).toHaveBeenLastCalledWith("e", expect.objectContaining({ feedback: "Save now" }));
  });

  it("autosaves after 30 seconds without removing score editors or losing focus", async () => {
    let finishSave!: (evaluation: Detail["evaluation"]) => void;
    mocks.saveEvaluation.mockReturnValue(new Promise<Detail["evaluation"]>((resolve) => { finishSave = resolve; }));
    mocks.loadEvaluation.mockResolvedValue({ ...detail, evaluation: { ...detail.evaluation, rubricItemScores: {} },
      rubrics: [{ rubricId: "r", partId: "p", maxPoints: 1,
      criteria: [{ criterionId: "c", title: "Design", items: [{ itemId: "i", text: "Test criterion", points: 1 }] }] }] });
    render(<GradingWorkspacePage evaluationId="e" />);
    const score = await screen.findByRole("spinbutton", { name: "Điểm mục Test criterion" });
    await act(async () => { await Promise.resolve(); });
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    act(() => score.focus());
    fireEvent.change(score, { target: { value: "0.5" } });
    await act(async () => { await vi.advanceTimersByTimeAsync(1499); });
    expect(mocks.saveEvaluation).not.toHaveBeenCalled();
    expect(score).toHaveFocus();
    await act(async () => { await vi.advanceTimersByTimeAsync(1); });
    expect(mocks.saveEvaluation).not.toHaveBeenCalled();
    await act(async () => { await vi.advanceTimersByTimeAsync(28500); });
    expect(mocks.saveEvaluation).toHaveBeenCalledTimes(1);
    expect(score.isConnected).toBe(true);
    expect(score).toBeEnabled();
    expect(score).toHaveFocus();
    await act(async () => { finishSave({ ...detail.evaluation, version: 3, score: 0.5,
      rubricItemScores: { r: { i: 0.5 } } }); });
    const replacement = screen.getByRole("spinbutton", { name: "Điểm mục Test criterion" });
    expect(replacement).toBe(score);
    expect(replacement).toHaveFocus();
    expect(mocks.loadEvaluation).toHaveBeenCalledTimes(1);
    expect(mocks.push).not.toHaveBeenCalled();
  });

  it.each(["0.5", "-0.5"])("saves an adjustment with only one optional note (%s)", async (adjustment) => {
    mocks.saveEvaluation.mockResolvedValue({ ...detail.evaluation, version: 3,
      adjustment: Number(adjustment), adjustmentReason: "Điều chỉnh theo quy định" });
    render(<GradingWorkspacePage evaluationId="e" />);
    const points = await screen.findByRole("spinbutton", { name: "Điểm cộng/trừ" });
    fireEvent.change(points, { target: { value: adjustment } });
    expect(screen.queryByRole("textbox", { name: "Lý do cộng/trừ điểm" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
    await waitFor(() => expect(mocks.saveEvaluation).toHaveBeenCalledTimes(1));
    expect(mocks.saveEvaluation).toHaveBeenLastCalledWith("e", expect.objectContaining({
      adjustment, adjustmentReason: null, changeReason: null,
    }));
    await waitFor(() => expect(points).toBeEnabled());
    fireEvent.click(screen.getByText("Ghi chú thay đổi (tùy chọn)"));
    fireEvent.change(screen.getByRole("textbox", { name: "Ghi chú thay đổi" }), { target: { value: "Điều chỉnh theo quy định" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
    await waitFor(() => expect(mocks.saveEvaluation).toHaveBeenCalledTimes(2));
    expect(mocks.loadEvaluation).toHaveBeenCalledTimes(1);
    expect(points).toHaveValue(Number(adjustment));
    expect(screen.queryByRole("button", { name: "Tải lại phiên bản" })).not.toBeInTheDocument();
    expect(mocks.saveEvaluation).toHaveBeenLastCalledWith("e", expect.objectContaining({
      adjustment, adjustmentReason: "Điều chỉnh theo quy định", changeReason: "Điều chỉnh theo quy định",
    }));
  });

  it("pauses autosave after a failed save and resumes after an edit without reloading the form", async () => {
    mocks.saveEvaluation.mockRejectedValueOnce(new Error("Offline"))
      .mockResolvedValue({ ...detail.evaluation, feedback: "Đã bổ sung", version: 3 });
    render(<GradingWorkspacePage evaluationId="e" />);
    const feedback = await screen.findByRole("textbox", { name: "Nhận xét chung" });
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    fireEvent.change(feedback, { target: { value: "Đang chấm" } });
    await act(async () => { await vi.advanceTimersByTimeAsync(30000); });
    expect(screen.getByRole("alert")).toHaveTextContent("Offline");
    await act(async () => { await vi.advanceTimersByTimeAsync(60000); });
    expect(mocks.saveEvaluation).toHaveBeenCalledTimes(1);
    expect(mocks.loadEvaluation).toHaveBeenCalledTimes(1);
    expect(feedback).toHaveValue("Đang chấm");

    fireEvent.change(feedback, { target: { value: "Đã bổ sung" } });
    await act(async () => { await vi.advanceTimersByTimeAsync(30000); });
    expect(mocks.saveEvaluation).toHaveBeenCalledTimes(2);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(feedback).toHaveValue("Đã bổ sung");
  });

  it("keeps manually entered paragraphs without an extra paragraph-format button", async () => {
    mocks.loadEvaluation.mockResolvedValue({ ...detail, evaluation: { ...detail.evaluation, feedback: "Đạt 1.25/2.00 điểm. Cần bổ sung sơ đồ. Hoàn thiện phần còn thiếu." } });
    render(<GradingWorkspacePage evaluationId="e" />);
    const feedback = await screen.findByRole("textbox", { name: "Nhận xét chung" });
    fireEvent.change(feedback, { target: { value: "Đạt 1.25/2.00 điểm.\n\nCần bổ sung sơ đồ." } });
    expect(feedback).toHaveValue("Đạt 1.25/2.00 điểm.\n\nCần bổ sung sơ đồ.");
    expect(screen.queryByRole("button", { name: "Tách đoạn" })).not.toBeInTheDocument();
    expect(mocks.saveEvaluation).not.toHaveBeenCalled();
  });
  it("shows a larger AI grading button with a visible text label", async () => {
    render(<GradingWorkspacePage evaluationId="e" />);
    const button = await screen.findByRole("button", { name: "Chấm bằng AI" });
    expect(button).toHaveTextContent("Chấm bằng AI");
    expect(button).toHaveClass("h-9!", "px-3!", "text-sm!");
  });
  it("shows the section author beside the heading and focuses the member grading drawer", async () => {
    mocks.loadEvaluation.mockResolvedValue({
      ...detail,
      evaluation: { ...detail.evaluation, kind: "GROUP_DOCUMENT", accountId: null },
      document: { blocks: [{ id: "part", type: "HEADING", content: { text: "Thiết kế" } }] },
      sectionAuthors: [{ partId: "part", accountId: "s" }],
      memberActivities: [{
        student: { accountId: "s", name: "Nguyễn An", email: "an@school.test" },
        state: "DISABLED",
        activity: null,
      }],
      members: [{ ...detail.evaluation, id: "member", kind: "MEMBER" }],
    });
    render(<GradingWorkspacePage evaluationId="e" />);

    const section = await screen.findByTestId("block-part");
    expect(within(section).getByText("Người thực hiện: Nguyễn An")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Điểm thành viên" }));
    expect(screen.getByRole("region", { name: "Chấm bài nhóm" })).toHaveFocus();
    expect(screen.getByRole("region", { name: "Chấm bài nhóm" }))
      .toContainElement(screen.getByRole("region", { name: "Điểm thành viên" }));
  });

  it("opens the matching group submission when AI grading is clicked from a member grade", async () => {
    mocks.loadEvaluation.mockResolvedValue({
      ...detail,
      evaluation: { ...detail.evaluation, kind: "MEMBER", groupDocumentId: "document" },
      navigationIds: ["another-group", "own-group"],
      groupEvaluationId: "own-group",
    });
    render(<GradingWorkspacePage evaluationId="e" status="PENDING" />);
    fireEvent.click(await screen.findByRole("button", { name: "Chấm bằng AI" }));
    expect(mocks.push).toHaveBeenCalledWith("/teaching/evaluations/own-group?status=PENDING");
    expect(mocks.requestProposal).not.toHaveBeenCalled();
  });

  it("explains a concurrent grade lock without discarding unsaved checklist changes or retrying", async () => {
    mocks.loadEvaluation.mockResolvedValue({ ...detail, rubrics: [
      { rubricId: "r", partId: "p", maxPoints: 1, criteria: [{ criterionId: "c", title: "Đúng", items: [{ itemId: "i", text: "Đúng nội dung", points: 1 }] }] },
    ] });
    mocks.saveEvaluation.mockRejectedValue(new ApiError({ status: 409, title: "Conflict", code: "GRADE_LOCKED" }));
    render(<GradingWorkspacePage evaluationId="e" />);
    fireEvent.click(await screen.findByRole("checkbox", { name: "Đúng nội dung" }));
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Điểm đã chốt");
    expect(screen.getByRole("checkbox", { name: "Đúng nội dung" })).toBeChecked();
    expect(mocks.saveEvaluation).toHaveBeenCalledTimes(1);
    expect(mocks.push).not.toHaveBeenCalled();
  });
  it("preserves the originating queue for save-next and clears it for an explicit student switch", async () => {
    mocks.saveEvaluation.mockResolvedValue({ ...detail.evaluation, version: 3 });
    mocks.listSubmissions.mockResolvedValue({ rows: [
      { student: { accountId: "s2", name: "Trần Bình", email: "binh@school.test" }, evaluationId: "next" },
    ], total: 1 });
    render(<GradingWorkspacePage evaluationId="e" status="PENDING" late="true" />);
    fireEvent.click(await screen.findByRole("button", { name: "Bài sau" }));
    expect(mocks.push).toHaveBeenLastCalledWith("/teaching/evaluations/next?status=PENDING&late=true");
    expect(mocks.loadEvaluation).toHaveBeenCalledWith("e", { status: "PENDING", late: "true" });
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
    await waitFor(() => expect(mocks.saveEvaluation).toHaveBeenCalled());
    expect(mocks.push).toHaveBeenCalledTimes(1);
    expect(mocks.push).toHaveBeenLastCalledWith("/teaching/evaluations/next?status=PENDING&late=true");
    fireEvent.click(screen.getByLabelText("Chọn sinh viên"));
    fireEvent.click(await screen.findByRole("button", { name: /Trần Bình/ }));
    expect(mocks.push).toHaveBeenLastCalledWith("/teaching/evaluations/next");
  });

  it("stays on the last filtered submission after saving", async () => {
    mocks.loadEvaluation.mockResolvedValue({ ...detail, navigationIds: ["e"] });
    mocks.saveEvaluation.mockResolvedValue({ ...detail.evaluation, version: 3 });
    render(<GradingWorkspacePage evaluationId="e" status="PENDING" />);
    fireEvent.click(await screen.findByRole("button", { name: "Lưu thay đổi" }));
    await waitFor(() => expect(mocks.saveEvaluation).toHaveBeenCalled());
    await waitFor(() => expect(screen.getByRole("button", { name: "Lưu thay đổi" })).toBeEnabled());
    expect(mocks.push).not.toHaveBeenCalled();
  });
  it("does not jump backwards when the current grade has left the filtered queue", async () => {
    mocks.loadEvaluation.mockResolvedValue({ ...detail, navigationIds: ["earlier-pending"] });
    mocks.saveEvaluation.mockResolvedValue({ ...detail.evaluation, version: 3 });
    render(<GradingWorkspacePage evaluationId="e" status="PENDING" />);
    fireEvent.click(await screen.findByRole("button", { name: "Lưu thay đổi" }));
    await waitFor(() => expect(mocks.saveEvaluation).toHaveBeenCalled());
    await waitFor(() => expect(screen.getByRole("button", { name: "Lưu thay đổi" })).toBeEnabled());
    expect(mocks.push).not.toHaveBeenCalled();
  });
  it.each(["FINALIZED", "PUBLISHED"] as const)("keeps %s grades editable while publication remains explicit", async (state) => {
    mocks.loadEvaluation.mockResolvedValue({ ...detail, evaluation: { ...detail.evaluation, state }, rubrics: [
      { rubricId: "r", partId: "p", maxPoints: 1, criteria: [{ criterionId: "c", title: "Đúng", items: [{ itemId: "i", text: "Đúng nội dung", points: 1 }] }] },
    ] });
    render(<GradingWorkspacePage evaluationId="e" />);
    expect(await screen.findByRole("checkbox", { name: "Đúng nội dung" })).toBeEnabled();
    expect(screen.queryByText(/Điểm đã chốt. Bản chấm chỉ có thể xem/)).not.toBeInTheDocument();
    const save = screen.getByRole("button", { name: /Lưu thay đổi/ });
    expect(save).toBeEnabled();
    expect(mocks.saveEvaluation).not.toHaveBeenCalled();
    expect(screen.queryByLabelText(/Lý do sửa điểm/)).not.toBeInTheDocument();
    const publish = screen.getByRole("button", { name: "Công bố điểm" });
    if (state === "FINALIZED") expect(publish).toBeEnabled();
    else expect(publish).toBeDisabled();
  });
  it("reports an unavailable proposal link without creating or applying a replacement", async () => {
    render(<GradingWorkspacePage evaluationId="e" proposalId="missing" />);
    expect(await screen.findByText(/Không tìm thấy đề xuất từ liên kết/)).toBeInTheDocument();
    expect(mocks.saveEvaluation).not.toHaveBeenCalled();
    expect(mocks.requestProposal).not.toHaveBeenCalled();
  });
  it("opens the exact older proposal from an agent link without applying or saving it", async () => {
    mocks.listProposals.mockResolvedValue([
      { id: "new", versionNo: 2, status: "READY", origin: "MCP", proposal: { checks: {}, score: 2, feedback: "Latest agent feedback" } },
      { id: "old", versionNo: 1, status: "READY", origin: "MCP", proposal: { checks: {}, score: 1, feedback: "Requested older feedback" } },
    ]);
    render(<GradingWorkspacePage evaluationId="e" proposalId="old" />);
    expect(await screen.findByText("Requested older feedback")).toBeVisible();
    expect(screen.getByRole("region", { name: "Chấm bằng AI" })).toBeInTheDocument();
    expect(mocks.saveEvaluation).not.toHaveBeenCalled();
    expect(mocks.requestProposal).not.toHaveBeenCalled();
  });
  it("names the current student and searches all pages before switching submissions", async () => {
    mocks.listSubmissions.mockResolvedValueOnce({ rows: [
      { student: { accountId: "s", name: "Nguyễn An", email: "an@school.test" }, evaluationId: "e" },
    ], total: 2 }).mockResolvedValueOnce({ rows: [
      { student: { accountId: "s2", name: "Trần Bình", email: "binh@school.test" }, evaluationId: "next" },
    ], total: 2 });
    render(<GradingWorkspacePage evaluationId="e" />);
    expect(await screen.findByRole("heading", { level: 1, name: "Nguyễn An" })).toBeInTheDocument();
    expect(screen.getByLabelText("Chọn sinh viên")).toHaveTextContent("Nguyễn An");
    fireEvent.click(screen.getByLabelText("Chọn sinh viên"));
    fireEvent.change(screen.getByRole("searchbox", { name: "Tìm sinh viên" }), { target: { value: "binh@school.test" } });
    fireEvent.click(await screen.findByRole("button", { name: /Trần Bình/ }));
    expect(mocks.listSubmissions).toHaveBeenCalledWith("c", "a", "ALL", 2);
    expect(mocks.push).toHaveBeenCalledWith("/teaching/evaluations/next");
    expect(mocks.saveEvaluation).not.toHaveBeenCalled();
  });

  it("keeps unsaved work when the teacher cancels switching students", async () => {
    mocks.listSubmissions.mockResolvedValue({ rows: [
      { student: { accountId: "s2", name: "Trần Bình", email: "binh@school.test" }, evaluationId: "next" },
    ], total: 1 });
    mocks.loadEvaluation.mockResolvedValue({ ...detail, rubrics: [
      { rubricId: "r", partId: "p", maxPoints: 1, criteria: [{ criterionId: "c", title: "Đúng", items: [{ itemId: "i", text: "Đúng nội dung", points: 1 }] }] },
    ] });
    render(<GradingWorkspacePage evaluationId="e" />);
    fireEvent.click(await screen.findByRole("checkbox", { name: "Đúng nội dung" }));
    fireEvent.click(screen.getByLabelText("Chọn sinh viên"));
    fireEvent.click(await screen.findByRole("button", { name: /Trần Bình/ }));
    expect(await screen.findByRole("dialog", { name: "Bạn có thay đổi chưa lưu. Rời bài này?" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));
    expect(mocks.push).not.toHaveBeenCalled();
    expect(screen.getByRole("checkbox", { name: "Đúng nội dung" })).toBeChecked();
  });
  it("leaves the unsaved submission when the teacher confirms in the dialog", async () => {
    mocks.listSubmissions.mockResolvedValue({ rows: [
      { student: { accountId: "s2", name: "Trần Bình", email: "binh@school.test" }, evaluationId: "next" },
    ], total: 1 });
    mocks.loadEvaluation.mockResolvedValue({ ...detail, rubrics: [
      { rubricId: "r", partId: "p", maxPoints: 1, criteria: [{ criterionId: "c", title: "Đúng", items: [{ itemId: "i", text: "Đúng nội dung", points: 1 }] }] },
    ] });
    render(<GradingWorkspacePage evaluationId="e" />);
    fireEvent.click(await screen.findByRole("checkbox", { name: "Đúng nội dung" }));
    fireEvent.click(screen.getByLabelText("Chọn sinh viên"));
    fireEvent.click(await screen.findByRole("button", { name: /Trần Bình/ }));
    fireEvent.click(await screen.findByRole("button", { name: "Bỏ thay đổi" }));
    expect(mocks.push).toHaveBeenCalledWith("/teaching/evaluations/next");
  });
  it("sends version and checklist, and stays on the submission after saving", async () => {
    mocks.saveEvaluation.mockResolvedValue({ ...detail.evaluation, version: 3 });
    render(<GradingWorkspacePage evaluationId="e" />);
    // Tạm thời không có ô nhận xét: nhận xét đã lưu được giữ nguyên khi lưu điểm.
    expect(screen.queryByRole("textbox", { name: /Nhận xét/ })).not.toBeInTheDocument();
    fireEvent.click(await screen.findByRole("button", { name: "Lưu thay đổi" }));
    await waitFor(() => expect(mocks.saveEvaluation).toHaveBeenCalled());
    expect(mocks.push).not.toHaveBeenCalled();
    expect(mocks.saveEvaluation.mock.calls[0]![1]).toMatchObject({
      version: 2,
      checkedItemsByRubric: {},
      adjustment: "0",
      adjustmentReason: null,
      feedback: "original",
    });
    expect(mocks.saveEvaluation.mock.calls[0]![1]).not.toHaveProperty("score");
  });
  it("stays on the submission after a version conflict", async () => {
    mocks.saveEvaluation.mockRejectedValue(
      new ApiError({ status: 409, title: "Conflict", code: "GRADE_VERSION_CONFLICT" }),
    );
    render(<GradingWorkspacePage evaluationId="e" />);
    fireEvent.click(await screen.findByRole("button", { name: "Lưu thay đổi" }));
    await screen.findByRole("alert");
    expect(mocks.push).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Công bố điểm" })).toBeDisabled();
  });
});


it("shows a live score and named sections while AI is unavailable, without saving any grade", async () => {
  mocks.loadEvaluation.mockResolvedValue({ ...detail, document: { blocks: [{ id: "part", type: "HEADING", content: { text: "Câu 1: Class Diagram" } }] },
    rubrics: [{ rubricId: "r", partId: "part", maxPoints: 10, criteria: [{ criterionId: "c", title: "Kiến trúc", items: [{ itemId: "i", text: "Đúng quan hệ", points: 2.5 }] }] }] });
  mocks.listProposals.mockResolvedValue([{ id: "p", versionNo: 1, origin: "AI_TASK", status: "FAILED", safeError: "AI task failed" }]);
  render(<GradingWorkspacePage evaluationId="e" />);
  fireEvent.click(await screen.findByRole("checkbox", { name: /Đúng quan hệ/ }));
  expect(screen.getByLabelText("Điểm tạm tính")).toHaveTextContent("2.50 / 10.00");
  expect(screen.getByRole("region", { name: "Rubric: Câu 1: Class Diagram" })).toHaveTextContent("Đạt 2.5");
  expect(screen.getAllByRole("spinbutton").length).toBeGreaterThan(0);
  expect(screen.queryByText(/Điều chỉnh/)).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Lưu thay đổi" })).toBeEnabled();
  expect(screen.queryByRole("button", { name: "Chốt điểm" })).not.toBeInTheDocument();
  expect(mocks.saveEvaluation).not.toHaveBeenCalled();
});


it("treats a saved fractional point as not achieved (checklist only) and ticking the item saves full points", async () => {
  const partialDetail = { ...detail, evaluation: { ...detail.evaluation, rubricItemScores: { r: { i: 0.5 } } },
    rubrics: [{ rubricId: "r", partId: "q", maxPoints: 1, criteria: [{ criterionId: "c", title: "Thiết kế", items: [{ itemId: "i", text: "Đạt một phần", points: 1 }] }] }] };
  mocks.loadEvaluation.mockResolvedValue(partialDetail);
  mocks.saveEvaluation.mockResolvedValue({ ...partialDetail.evaluation, version: 3, score: 1 });
  render(<GradingWorkspacePage evaluationId="e" />);
  const item = await screen.findByRole("checkbox", { name: /Đạt một phần/ });
  expect(item).not.toBeChecked();
  expect(screen.queryByText(/Điểm lẻ/)).not.toBeInTheDocument();
  fireEvent.click(item);
  expect(item).toBeChecked();
  fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
  await waitFor(() => expect(mocks.saveEvaluation).toHaveBeenCalled());
  expect(mocks.saveEvaluation.mock.calls[0]![1]).toMatchObject({ version: 2, checkedItemsByRubric: { r: ["i"] }, itemScoresByRubric: { r: { i: 1 } } });
  expect(mocks.saveEvaluation.mock.calls[0]![1]).not.toHaveProperty("score");
});

it("unticking an item gives it zero and a backend without item scores only gets the checklist", async () => {
  mocks.loadEvaluation.mockResolvedValue({ ...detail, evaluation: { ...detail.evaluation, rubricChecks: JSON.stringify({ r: ["i"] }) },
    rubrics: [{ rubricId: "r", partId: "q", maxPoints: 1, criteria: [{ criterionId: "c", title: "Thiết kế", items: [{ itemId: "i", text: "Đạt một phần", points: 1 }] }] }] });
  mocks.saveEvaluation.mockResolvedValue({ ...detail.evaluation, version: 3 });
  render(<GradingWorkspacePage evaluationId="e" />);
  const item = await screen.findByRole("checkbox", { name: /Đạt một phần/ });
  expect(item).toBeChecked();
  fireEvent.click(item);
  expect(screen.getByLabelText("Điểm tạm tính")).toHaveTextContent("0.00 / 10.00");
  fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
  await waitFor(() => expect(mocks.saveEvaluation).toHaveBeenCalled());
  expect(mocks.saveEvaluation.mock.calls[0]![1]).toMatchObject({ checkedItemsByRubric: { r: [] } });
  expect(mocks.saveEvaluation.mock.calls[0]![1]).not.toHaveProperty("itemScoresByRubric");
});

it("recovers the actual task error when an older backend only returns AI task failed", async () => {
  mocks.listProposals.mockResolvedValue([{ id: "p", taskId: "task", versionNo: 2, status: "FAILED", origin: "AI_TASK", safeError: "AI task failed" }]);
  mocks.loadTask.mockResolvedValue({ state: "FAILED", safeError: "Yêu cầu chấm không hợp lệ (HTTP 400)" });
  render(<GradingWorkspacePage evaluationId="e" />);
  await screen.findByRole("button", { name: "Chấm bằng AI" });
  openAi();
  expect(await screen.findByText("Yêu cầu chấm không hợp lệ (HTTP 400)")).toBeInTheDocument();
  expect(mocks.loadTask).toHaveBeenCalledWith("task");
  expect(screen.getByRole("button", { name: "Lưu thay đổi" })).toBeEnabled();
});

describe("AI proposal comparison and draft application", () => {
  const sampleRubrics = [
    {
      rubricId: "r1",
      partId: "part-1",
      maxPoints: 3,
      criteria: [
        {
          criterionId: "c1",
          title: "Kiến trúc",
          items: [
            { itemId: "i1", text: "Thực thể chính", points: 1 },
            { itemId: "i2", text: "Quan hệ lớp", points: 2 },
          ],
        },
      ],
    },
  ];

  it("compares the current checklist with the AI proposal and applies only to the local draft", async () => {
    mocks.loadEvaluation.mockResolvedValue({
      ...detail,
      evaluation: {
        ...detail.evaluation,
        maxScore: 3,
        rubricChecks: JSON.stringify({ r1: ["i2"] }),
        rubricItemScores: { r1: { i1: 0.5, i2: 2 } },
        adjustment: -0.5,
        adjustmentReason: "Nộp muộn",
      },
      document: { blocks: [{ id: "part-1", type: "HEADING", content: { text: "Câu 1: Class Diagram" } }] },
      rubrics: sampleRubrics,
    });
    mocks.listProposals.mockResolvedValue([
      {
        id: "prop-latest",
        versionNo: 2,
        status: "READY",
        origin: "AI_TASK",
        source: "PERSONAL",
        provider: "GEMINI",
        model: "gemini-3.5-flash-lite",
        proposal: {
          checks: { r1: ["i1", "i2"] },
          itemScoresByRubric: { r1: { i1: 1, i2: 2 } },
          score: 3,
          feedback: "Nhận xét chung tiếng Việt",
          sections: [{ partId: "part-1", feedback: "Tốt phần 1" }],
          structuredFeedback: {
            schemaVersion: 1,
            summary: { strengths: ["Mô hình rõ ràng"], improvements: [], nextSteps: [] },
            items: [{
              rubricId: "r1",
              itemId: "i1",
              evidence: "Có lớp người dùng",
              reason: "Xác định đúng thực thể chính",
              suggestion: null,
              evidenceBlockId: null,
              requiresReview: false,
              reviewReason: null,
            }],
          },
        },
      },
      {
        id: "prop-old-ready",
        versionNo: 1,
        status: "READY",
        origin: "AI_TASK",
        source: "PERSONAL",
        proposal: {
          checks: { r1: ["i2"] },
          score: 2,
          feedback: "Bản cũ",
        },
      },
    ]);

    render(<GradingWorkspacePage evaluationId="e" />);
    await screen.findByRole("button", { name: "Xem đề xuất AI" });
    expect(screen.queryByText("Có đề xuất AI")).not.toBeInTheDocument();
    openAi();
    const review = screen.getByRole("region", { name: "Chấm bằng AI" });
    expect(screen.queryByRole("dialog", { name: "Chấm bằng AI" })).not.toBeInTheDocument();
    expect(review).toHaveTextContent(/Điểm đang chấm: 2\.50/);
    expect(review).toHaveTextContent(/AI đề xuất: 3\.00/);
    expect(review).not.toHaveTextContent(/Điều chỉnh/);
    expect(review).toHaveTextContent(/Lệch \+0\.50/);

    fireEvent.click(screen.getByRole("button", { name: "Áp dụng vào bản nháp" }));
    expect(mocks.saveEvaluation).not.toHaveBeenCalled();
    expect(screen.queryByRole("region", { name: "Chấm bằng AI" })).not.toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: /Thực thể chính/ })).toBeChecked();
    expect(screen.getByRole("region", { name: "Nhận xét chung" }))
      .toHaveTextContent("Nhận xét chung tiếng Việt");
    const item = within(screen.getByTestId("rubric-item-i1"));
    expect(item.getByText("Nhận xét & minh chứng")).toBeVisible();
    expect(item.getByText("Xác định đúng thực thể chính")).toBeInTheDocument();
  });

  it("shows the AI comparison without NaN when a fractional point was saved before", async () => {
    mocks.loadEvaluation.mockResolvedValue({
      ...detail,
      evaluation: { ...detail.evaluation, maxScore: 3, rubricItemScores: { r1: { i1: 0.5, i2: 2 } } },
      rubrics: sampleRubrics,
    });
    mocks.listProposals.mockResolvedValue([
      {
        id: "prop-latest",
        versionNo: 1,
        status: "READY",
        origin: "AI_TASK",
        proposal: {
          checks: { r1: ["i1", "i2"] },
          score: 3,
          feedback: "OK",
        },
      },
    ]);

    render(<GradingWorkspacePage evaluationId="e" />);
    await screen.findByRole("checkbox", { name: /Thực thể chính/ });
    // Đề xuất AI tải bằng yêu cầu riêng, có thể về sau phần chấm điểm: chờ có đề xuất rồi mới mở popup.
    await screen.findByRole("button", { name: "Xem đề xuất AI" });
    expect(screen.queryByText("Có đề xuất AI")).not.toBeInTheDocument();
    openAi();
    expect(screen.getByRole("region", { name: "Chấm bằng AI" })).toHaveTextContent(/Điểm đang chấm: 2\.50/);
    expect(document.body.textContent).not.toContain("NaN");
  });

  it("keeps activity review without displaying empty or populated grade history", async () => {
    const { unmount } = render(<GradingWorkspacePage evaluationId="e" />);
    const activityHeading = await screen.findByRole("heading", { name: "Hoạt động khi làm bài" });
    expect(activityHeading.closest("details")).not.toHaveAttribute("open");
    expect(screen.queryByRole("heading", { name: "Lịch sử sửa điểm" })).not.toBeInTheDocument();
    expect(screen.getByText(/nền tảng không kết luận gian lận/)).toBeInTheDocument();
    unmount();

    mocks.loadEvaluation.mockResolvedValue({
      ...detail,
      evaluation: {
        ...detail.evaluation,
        changes: [{ actorId: "t", at: "2026-10-25T10:00:00Z", before: 8, after: 9, reason: "Cộng điểm sơ đồ" }],
      },
    });
    render(<GradingWorkspacePage evaluationId="e" />);
    await screen.findByRole("heading", { name: "Hoạt động khi làm bài" });
    expect(screen.queryByRole("heading", { name: "Lịch sử sửa điểm" })).not.toBeInTheDocument();
    expect(screen.queryByText(/Cộng điểm sơ đồ/)).not.toBeInTheDocument();
  });
});

describe("AI proposal polling resilience and progress states", () => {
  const rubricFixture = [
    {
      rubricId: "r1",
      partId: "part-1",
      maxPoints: 1,
      criteria: [
        {
          criterionId: "c1",
          title: "Kiến trúc",
          items: [{ itemId: "i1", text: "Thực thể chính", points: 1 }],
        },
      ],
    },
  ];

  it("transitions QUEUED -> RUNNING -> READY task with PENDING proposal -> READY proposal while preserving a manual tick", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    mocks.loadEvaluation.mockResolvedValue({
      ...detail,
      evaluation: { ...detail.evaluation, maxScore: 1, rubricItemScores: {} },
      rubrics: rubricFixture,
    });
    mocks.listProposals
      .mockResolvedValueOnce([
        { id: "p1", versionNo: 1, status: "PENDING", taskState: "QUEUED", origin: "AI_TASK", source: "PERSONAL" },
      ])
      .mockResolvedValueOnce([
        { id: "p1", versionNo: 1, status: "PENDING", taskState: "RUNNING", origin: "AI_TASK", source: "PERSONAL" },
      ])
      .mockResolvedValueOnce([
        { id: "p1", versionNo: 1, status: "PENDING", taskState: "READY", origin: "AI_TASK", source: "PERSONAL" },
      ])
      .mockResolvedValue([
        {
          id: "p1",
          versionNo: 1,
          status: "READY",
          origin: "AI_TASK",
          source: "PERSONAL",
          proposal: {
            checks: { r1: ["i1"] },
            itemScoresByRubric: { r1: { i1: 1 } },
            score: 1,
            feedback: "Đề xuất hoàn tất",
          },
        },
      ]);

    render(<GradingWorkspacePage evaluationId="e" />);
    await act(async () => {});
    const scoreInput = screen.getByRole("checkbox", { name: /Thực thể chính/ });
    fireEvent.click(scoreInput);
    expect(scoreInput).toBeChecked();
    openAi();

    expect(screen.getByText("Đang chờ xử lý")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Áp dụng vào bản nháp" })).toBeDisabled();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.getByText("AI đang chấm bài")).toBeInTheDocument();
    expect(scoreInput).toBeChecked();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.getByText("Đã nhận kết quả AI, đang xử lý phản hồi")).toBeInTheDocument();
    expect(scoreInput).toBeChecked();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.getByText("Sẵn sàng để xem")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Áp dụng vào bản nháp" })).toBeEnabled();
    expect(scoreInput).toBeChecked();
    expect(mocks.saveEvaluation).not.toHaveBeenCalled();
  });

  it("recovers after a temporary network error without spamming duplicate error alerts", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    mocks.listProposals
      .mockResolvedValueOnce([
        { id: "p1", versionNo: 1, status: "PENDING", taskState: "RUNNING", origin: "AI_TASK" },
      ])
      .mockRejectedValueOnce(new Error("Temporary network error"))
      .mockResolvedValue([
        {
          id: "p1",
          versionNo: 1,
          status: "READY",
          origin: "AI_TASK",
          proposal: { checks: {}, score: 8, feedback: "Đã hồi phục" },
        },
      ]);

    render(<GradingWorkspacePage evaluationId="e" />);
    await act(async () => {});
    openAi();
    expect(screen.getByText("AI đang chấm bài")).toBeInTheDocument();

    // First poll fails with transient error -> no error alert spam, schedules retry.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();

    // Backoff poll succeeds -> proposal becomes READY.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.getByText("Sẵn sàng để xem")).toBeInTheDocument();
    expect(screen.getByText("Đã hồi phục")).toBeInTheDocument();
  });

  it("stops polling on 403 error and surfaces the error once", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    mocks.listProposals
      .mockResolvedValueOnce([
        { id: "p1", versionNo: 1, status: "PENDING", taskState: "RUNNING", origin: "AI_TASK" },
      ])
      .mockRejectedValueOnce(new ApiError({ status: 403, title: "Forbidden", code: "FORBIDDEN" }));

    render(<GradingWorkspacePage evaluationId="e" />);
    await act(async () => {});
    openAi();
    expect(screen.getByText("AI đang chấm bài")).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.getByRole("alert")).toBeInTheDocument();
    const callsAfter403 = mocks.listProposals.mock.calls.length;

    await act(async () => {
      await vi.advanceTimersByTimeAsync(15000);
    });
    expect(mocks.listProposals).toHaveBeenCalledTimes(callsAfter403);
  });

  it("surfaces delayed status and Kiểm tra lại button after repeated poll failures, resuming existing request without a new AI job", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    mocks.listProposals
      .mockResolvedValueOnce([
        { id: "p1", versionNo: 1, status: "PENDING", taskState: "RUNNING", origin: "AI_TASK" },
      ])
      .mockRejectedValueOnce(new Error("Poll timeout 1"))
      .mockRejectedValueOnce(new Error("Poll timeout 2"))
      .mockRejectedValueOnce(new Error("Poll timeout 3"))
      .mockRejectedValueOnce(new Error("Poll timeout 4"))
      .mockResolvedValue([
        {
          id: "p1",
          versionNo: 1,
          status: "READY",
          origin: "AI_TASK",
          proposal: { checks: {}, score: 9, feedback: "Hoàn tất sau khi kiểm tra lại" },
        },
      ]);

    render(<GradingWorkspacePage evaluationId="e" />);
    await act(async () => {});
    openAi();
    expect(screen.getByText("AI đang chấm bài")).toBeInTheDocument();

    for (const waitMs of [3000, 3000, 6000, 12000]) {
      await act(async () => {
        await vi.advanceTimersByTimeAsync(waitMs);
      });
    }

    expect(screen.getByText(/Phản hồi AI đang mất nhiều thời gian hơn dự kiến/)).toBeInTheDocument();
    const recheckBtn = screen.getByRole("button", { name: "Kiểm tra lại" });

    await act(async () => {
      fireEvent.click(recheckBtn);
    });
    expect(screen.getByText("Sẵn sàng để xem")).toBeInTheDocument();
    expect(screen.getByText("Hoàn tất sau khi kiểm tra lại")).toBeInTheDocument();
    expect(mocks.requestProposal).not.toHaveBeenCalled();
  });

  it("cleans up polling timers on unmount", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    mocks.listProposals.mockResolvedValue([
      { id: "p1", versionNo: 1, status: "PENDING", taskState: "QUEUED", origin: "AI_TASK" },
    ]);

    const { unmount } = render(<GradingWorkspacePage evaluationId="e" />);
    await act(async () => {});
    openAi();
    expect(screen.getByText("Đang chờ xử lý")).toBeInTheDocument();
    expect(mocks.listProposals).toHaveBeenCalledTimes(1);

    unmount();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(15000);
    });
    expect(mocks.listProposals).toHaveBeenCalledTimes(1);
  });
});

describe("AI grading request lifecycle", () => {
  const rubrics = [
    {
      rubricId: "r1",
      partId: "part-1",
      maxPoints: 1,
      criteria: [{ criterionId: "c1", title: "Kiến trúc", items: [{ itemId: "i1", text: "Thực thể chính", points: 1 }] }],
    },
  ];
  const pending = { id: "p9", versionNo: 1, status: "PENDING", taskState: "QUEUED", origin: "AI_TASK", source: "PERSONAL" };

  async function requestAiGrade() {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    mocks.loadEvaluation.mockResolvedValue({ ...detail, evaluation: { ...detail.evaluation, maxScore: 1, rubricItemScores: {} }, rubrics });
    mocks.requestProposal.mockResolvedValue(pending);
    render(<><GradingWorkspacePage evaluationId="e" /><Toaster /></>);
    await act(async () => {});
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Chấm bằng AI" }));
    });
    expect(screen.queryByRole("alertdialog", { name: "Đang chấm bằng AI" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Chấm bằng AI" })).toHaveAttribute("aria-busy", "true");
  }

  it("retains manual work when AI becomes READY and only applies on request", async () => {
    mocks.listProposals.mockResolvedValueOnce([]).mockResolvedValueOnce([pending]).mockResolvedValue([
      { ...pending, status: "READY", taskState: undefined, proposal: { checks: { r1: ["i1"] }, itemScoresByRubric: { r1: { i1: 1 } }, score: 1, feedback: "AI xong" } },
    ]);
    await requestAiGrade();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.queryByRole("alertdialog", { name: "Đang chấm bằng AI" })).not.toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: /Thực thể chính/ })).not.toBeChecked();
    fireEvent.click(screen.getByRole("button", { name: "Xem đề xuất AI" }));
    fireEvent.click(screen.getByRole("button", { name: "Áp dụng vào bản nháp" }));
    expect(screen.getByRole("checkbox", { name: /Thực thể chính/ })).toBeChecked();
    expect(mocks.saveEvaluation).not.toHaveBeenCalled();
  });

  it("reports a proposal that FAILED while polling and lets the teacher request again", async () => {
    mocks.listProposals.mockResolvedValueOnce([]).mockResolvedValueOnce([pending]).mockResolvedValue([
      { ...pending, status: "FAILED", taskState: undefined, safeError: "Nhà cung cấp AI từ chối yêu cầu" },
    ]);
    await requestAiGrade();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.queryByRole("alertdialog", { name: "Đang chấm bằng AI" })).not.toBeInTheDocument();
    expect(screen.getAllByText("Nhà cung cấp AI từ chối yêu cầu").length).toBeGreaterThan(0);
    expect(screen.getByRole("button", { name: "Chấm bằng AI" })).toBeEnabled();
    expect(screen.getByRole("checkbox", { name: /Thực thể chính/ })).not.toBeChecked();
  });

  it("stops waiting and reports the error when the request itself fails", async () => {
    vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
    mocks.loadEvaluation.mockResolvedValue({ ...detail, evaluation: { ...detail.evaluation, maxScore: 1, rubricItemScores: {} }, rubrics });
    mocks.requestProposal.mockRejectedValue(new ApiError({ status: 503, title: "Unavailable", detail: "Dịch vụ AI tạm thời không khả dụng" }));
    render(<><GradingWorkspacePage evaluationId="e" /><Toaster /></>);
    await act(async () => {});
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Chấm bằng AI" }));
    });
    expect(screen.queryByRole("alertdialog", { name: "Đang chấm bằng AI" })).not.toBeInTheDocument();
    expect(screen.getAllByText(/Dịch vụ AI tạm thời không khả dụng/).length).toBeGreaterThan(0);
  });
});
