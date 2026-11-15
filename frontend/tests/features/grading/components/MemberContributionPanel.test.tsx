import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { MemberContributionPanel } from "@/features/grading/components/MemberContributionPanel";
import type { Evaluation } from "@/features/grading/model/types";

const mocks = vi.hoisted(() => ({ saveMemberGrade: vi.fn() }));
vi.mock("@/features/grading/api", () => ({ gradingApi: mocks }));

const root = { id: "root", score: 8, maxScore: 10, state: "DRAFT" } as Evaluation;
const member = {
  id: "member", accountId: "student", kind: "MEMBER", state: "DRAFT", version: 3,
  score: 8, maxScore: 10, adjustmentReason: "Ghi chú nhóm", memberOverride: false,
} as Evaluation;

beforeEach(() => vi.resetAllMocks());

function renderPanel(overrides: Partial<Evaluation> = {}, editable = true) {
  const onSaved = vi.fn();
  const props = { root, members: [{ ...member, ...overrides }], names: { student: "Sinh viên A" }, onSaved, editable };
  return { ...render(<MemberContributionPanel {...props} />), onSaved, props };
}

it("starts unassessed and saves the selected level without changing the inherited score", async () => {
  mocks.saveMemberGrade.mockResolvedValue({});
  const { onSaved } = renderPanel();
  expect(screen.getByLabelText(/Mức đóng góp/)).toHaveValue("");
  expect(screen.getByLabelText(/Căn cứ đánh giá/)).toHaveValue("");
  fireEvent.change(screen.getByLabelText(/Mức đóng góp/), { target: { value: "PARTIALLY_MEETS" } });
  expect(screen.getByLabelText("Điểm cá nhân")).toHaveValue(8);
  fireEvent.change(screen.getByLabelText(/Căn cứ đánh giá/), { target: { value: "Thiếu luồng lỗi" } });
  fireEvent.click(screen.getByRole("button", { name: "Lưu điểm thành viên" }));
  await waitFor(() => expect(onSaved).toHaveBeenCalledOnce());
  expect(mocks.saveMemberGrade).toHaveBeenCalledWith("root", "student", 3, "8", "Thiếu luồng lỗi", "PARTIALLY_MEETS");
});

it("restores the saved level and personal reason, and allows clearing the level", async () => {
  mocks.saveMemberGrade.mockResolvedValue({});
  renderPanel({ contributionLevel: "MEETS_EXPECTATIONS", memberOverride: true, adjustmentReason: "Hoàn thành API" });
  expect(screen.getByLabelText(/Mức đóng góp/)).toHaveValue("MEETS_EXPECTATIONS");
  expect(screen.getByLabelText(/Căn cứ đánh giá/)).toHaveValue("Hoàn thành API");
  fireEvent.change(screen.getByLabelText(/Mức đóng góp/), { target: { value: "" } });
  fireEvent.click(screen.getByRole("button", { name: "Lưu điểm thành viên" }));
  await waitFor(() => expect(mocks.saveMemberGrade).toHaveBeenCalledWith("root", "student", 3, "8", "Hoàn thành API", null));
});

it("has a collapsed guide with five descriptions and no fixed score conversion", () => {
  renderPanel();
  const guide = screen.getByText("Cách đánh giá").closest("details");
  expect(guide).not.toHaveAttribute("open");
  expect(guide).toHaveTextContent("Mức 1 là cao nhất");
  expect(guide).toHaveTextContent("không tự quy đổi thành điểm");
  expect(within(screen.getByRole("combobox", { name: /Mức đóng góp/ })).getAllByRole("option")).toHaveLength(6);
});

it("disables all edits for a locked class", () => {
  renderPanel({}, false);
  expect(screen.getByLabelText(/Mức đóng góp/)).toBeDisabled();
  expect(screen.getByLabelText("Điểm cá nhân")).toBeDisabled();
  expect(screen.getByRole("button", { name: "Lưu điểm thành viên" })).toBeDisabled();
});

it("keeps the entered assessment when saving fails", async () => {
  mocks.saveMemberGrade.mockRejectedValue(new Error("Không lưu được"));
  renderPanel();
  fireEvent.change(screen.getByLabelText(/Mức đóng góp/), { target: { value: "NEEDS_SUPPORT" } });
  fireEvent.click(screen.getByRole("button", { name: "Lưu điểm thành viên" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Không lưu được");
  expect(screen.getByLabelText(/Mức đóng góp/)).toHaveValue("NEEDS_SUPPORT");
  expect(screen.getByLabelText("Điểm cá nhân")).toHaveValue(8);
});

it("keeps each member's draft while switching between members and refreshing another member", () => {
  const second = { ...member, id: "member-2", accountId: "student-2" };
  const props = { root, members: [member, second], names: { student: "An", "student-2": "Bình" }, onSaved: vi.fn() };
  const view = render(<MemberContributionPanel {...props} />);
  fireEvent.change(screen.getByLabelText(/Căn cứ đánh giá/), { target: { value: "An thiếu luồng lỗi" } });
  fireEvent.change(screen.getByLabelText("Thành viên"), { target: { value: "student-2" } });
  expect(screen.getByLabelText(/Căn cứ đánh giá/)).toHaveValue("");
  fireEvent.change(screen.getByLabelText(/Căn cứ đánh giá/), { target: { value: "Bình hiểu nghiệp vụ" } });
  view.rerender(<MemberContributionPanel {...props} members={[{ ...member, version: 4 }, second]} />);
  fireEvent.click(screen.getByRole("button", { name: "Thành viên trước" }));
  expect(screen.getByLabelText(/Căn cứ đánh giá/)).toHaveValue("An thiếu luồng lỗi");
  expect(screen.getByText("Chưa lưu")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Thành viên sau" }));
  expect(screen.getByLabelText(/Căn cứ đánh giá/)).toHaveValue("Bình hiểu nghiệp vụ");
});

it("saving one member leaves another member's draft intact and rebases an unchanged server grade", async () => {
  const second = { ...member, id: "member-2", accountId: "student-2" };
  const props = { root, members: [member, second], names: { student: "An", "student-2": "Bình" }, onSaved: vi.fn() };
  const view = render(<MemberContributionPanel {...props} />);
  fireEvent.change(screen.getByLabelText(/Căn cứ đánh giá/), { target: { value: "An cần bổ sung" } });
  fireEvent.change(screen.getByLabelText("Thành viên"), { target: { value: "student-2" } });
  fireEvent.change(screen.getByLabelText(/Căn cứ đánh giá/), { target: { value: "Bình hoàn thành" } });
  const saved = { ...second, version: 4, memberOverride: true, adjustmentReason: "Bình hoàn thành" };
  mocks.saveMemberGrade.mockResolvedValue(saved);
  fireEvent.click(screen.getByRole("button", { name: "Lưu điểm thành viên" }));
  await waitFor(() => expect(props.onSaved).toHaveBeenCalledOnce());
  view.rerender(<MemberContributionPanel {...props} members={[{ ...member, version: 4 }, saved]} />);
  fireEvent.click(screen.getByRole("button", { name: "Thành viên trước" }));
  expect(screen.getByLabelText(/Căn cứ đánh giá/)).toHaveValue("An cần bổ sung");
  mocks.saveMemberGrade.mockResolvedValue({ ...member, version: 5, memberOverride: true, adjustmentReason: "An cần bổ sung" });
  fireEvent.click(screen.getByRole("button", { name: "Lưu điểm thành viên" }));
  await waitFor(() => expect(mocks.saveMemberGrade).toHaveBeenLastCalledWith("root", "student", 4, "8", "An cần bổ sung", null));
});

it("retains drafts without silently overwriting a conflicting server grade", async () => {
  const { rerender, props } = renderPanel();
  fireEvent.change(screen.getByLabelText(/Căn cứ đánh giá/), { target: { value: "Đang đối chiếu" } });
  rerender(<MemberContributionPanel {...props} members={[{ ...member, version: 4, score: 9 }]} />);
  mocks.saveMemberGrade.mockRejectedValue(new Error("Điểm đã thay đổi, hãy tải lại phiên bản"));
  fireEvent.click(screen.getByRole("button", { name: "Lưu điểm thành viên" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Điểm đã thay đổi");
  expect(mocks.saveMemberGrade).toHaveBeenCalledWith("root", "student", 3, "8", "Đang đối chiếu", null);
  expect(screen.getByLabelText("Điểm cá nhân")).toHaveValue(8);
  expect(screen.getByLabelText(/Căn cứ đánh giá/)).toHaveValue("Đang đối chiếu");
});
