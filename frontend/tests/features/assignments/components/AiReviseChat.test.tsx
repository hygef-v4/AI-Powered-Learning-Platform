import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { assignmentsApi } from "@/features/assignments/api";
import { AiReviseChat } from "@/features/assignments/components/AiReviseChat";
import type { AiDraft } from "@/features/assignments/model/types";

function draft(status: AiDraft["status"], extra: Partial<AiDraft> = {}): AiDraft {
  return {
    draftId: "d1", assignmentId: "a1", origin: "AI_TASK", status, taskId: "t1",
    proposal: status === "READY" ? {
      title: "English", businessContext: "", instructions: "", sections: [],
      validation: { status: "NEEDS_REVIEW", questionCount: 1, rubricLeafCount: 1, totalPoints: 0, issues: [
        { severity: "WARNING", code: "RUBRIC_RECOMMENDED", path: "blocks.rubric", message: "Mục \"Q1\" chưa có tiêu chí chấm." },
      ] },
    } : null,
    provenance: {}, safeError: null, baseVersion: 3, createdAt: "now", decidedAt: null, ...extra,
  };
}

function send(text: string) {
  fireEvent.change(screen.getByTestId("ai-revise-input"), { target: { value: text } });
  fireEvent.keyDown(screen.getByTestId("ai-revise-input"), { key: "Enter" });
}

describe("AiReviseChat", () => {
  afterEach(() => vi.restoreAllMocks());

  it("saves first, sends the instruction as a revision and applies the result to the page", async () => {
    const request = vi.spyOn(assignmentsApi, "requestAiRevision").mockResolvedValue(draft("READY"));
    const apply = vi.spyOn(assignmentsApi, "applyAiDraft").mockResolvedValue({} as never);
    const prepare = vi.fn().mockResolvedValueOnce(3).mockResolvedValueOnce(4);
    const onApplied = vi.fn().mockResolvedValue(undefined);
    render(<AiReviseChat assignmentId="a1" prepare={prepare} onApplied={onApplied} />);

    send("Dịch đề sang tiếng Anh");

    expect(await screen.findByText(/Đã sửa đề theo yêu cầu/)).toBeInTheDocument();
    expect(screen.getByText("Dịch đề sang tiếng Anh")).toBeInTheDocument();
    expect(screen.getByText(/chưa có tiêu chí chấm/)).toBeInTheDocument();
    expect(request).toHaveBeenCalledWith("a1", "Dịch đề sang tiếng Anh", expect.any(String));
    expect(prepare.mock.invocationCallOrder[0]).toBeLessThan(request.mock.invocationCallOrder[0]!);
    expect(apply).toHaveBeenCalledWith("a1", "d1", 4, true, true);
    expect(onApplied).toHaveBeenCalledOnce();
  });

  it("reports an AI failure in the chat and leaves the assignment unchanged", async () => {
    vi.spyOn(assignmentsApi, "requestAiRevision").mockResolvedValue(draft("FAILED", { safeError: "Hết hạn mức." }));
    const apply = vi.spyOn(assignmentsApi, "applyAiDraft");
    render(<AiReviseChat assignmentId="a1" prepare={vi.fn().mockResolvedValue(3)} onApplied={vi.fn()} />);

    send("Thêm một câu hỏi");

    expect(await screen.findByText("Hết hạn mức. Đề chưa thay đổi.")).toBeInTheDocument();
    expect(apply).not.toHaveBeenCalled();
    await waitFor(() => expect(screen.getByTestId("ai-revise-input")).not.toBeDisabled());
  });

  it("says when the AI changed nothing instead of applying", async () => {
    const ready = draft("READY");
    vi.spyOn(assignmentsApi, "requestAiRevision").mockResolvedValue({ ...ready, proposal: { ...ready.proposal!, unchanged: true } });
    const apply = vi.spyOn(assignmentsApi, "applyAiDraft");
    const discard = vi.spyOn(assignmentsApi, "discardAiDraft").mockResolvedValue(ready);
    render(<AiReviseChat assignmentId="a1" prepare={vi.fn().mockResolvedValue(3)} onApplied={vi.fn()} />);

    send("tạo cho tôi 10 yêu cầu");

    expect(await screen.findByText(/AI chưa thay đổi gì/)).toBeInTheDocument();
    expect(apply).not.toHaveBeenCalled();
    expect(discard).toHaveBeenCalledWith("a1", "d1");
  });

  it("does not send an empty instruction", () => {
    const request = vi.spyOn(assignmentsApi, "requestAiRevision");
    render(<AiReviseChat assignmentId="a1" prepare={vi.fn()} onApplied={vi.fn()} />);

    send("   ");

    expect(request).not.toHaveBeenCalled();
    expect(screen.getByTestId("ai-revise-send")).toBeDisabled();
  });

  it("is a popup that stays closed until its icon button is pressed, and closes again", () => {
    vi.spyOn(assignmentsApi, "listAiTemplates").mockResolvedValue([]);
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue([]);
    render(<AiReviseChat assignmentId="a1" prepare={vi.fn()} onApplied={vi.fn()} />);
    const toggle = screen.getByTestId("ai-revise-toggle");

    // Nút chỉ có biểu tượng, tên đọc được qua aria-label.
    expect(toggle).toHaveTextContent("");
    expect(toggle).toHaveAccessibleName("Sửa đề bằng AI");
    expect(toggle).toHaveAttribute("aria-expanded", "false");
    expect(screen.getByTestId("ai-revise-chat")).not.toBeVisible();
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByTestId("ai-revise-chat")).toBeVisible();
    // Chưa có tệp nào: không có hàng tệp gửi kèm.
    expect(screen.queryByTestId("ai-revise-attachments")).toBeNull();
    fireEvent.click(toggle);
    expect(screen.getByTestId("ai-revise-chat")).not.toBeVisible();
  });

  it("adds a template and a reference from the three-row + menu, shows them in one row template first, sends and clears them", async () => {
    vi.spyOn(assignmentsApi, "listAiTemplates").mockResolvedValue([
      { assignmentId: "t1", title: "Đề mẫu của môn", subjectTemplate: true },
      { assignmentId: "o1", title: "Đề giữa kỳ năm trước", subjectTemplate: false },
    ]);
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue([{ materialId: "m1", title: "Chương 3 UML", hasText: true } as never]);
    const request = vi.spyOn(assignmentsApi, "requestAiRevision").mockResolvedValue(draft("READY"));
    vi.spyOn(assignmentsApi, "applyAiDraft").mockResolvedValue({} as never);
    render(<AiReviseChat assignmentId="a1" prepare={vi.fn().mockResolvedValue(3)} onApplied={vi.fn().mockResolvedValue(undefined)} />);
    fireEvent.click(screen.getByTestId("ai-revise-toggle"));

    // Chọn tài liệu tham khảo trước: mẫu đề thêm sau vẫn đứng đầu hàng.
    fireEvent.click(screen.getByTestId("ai-revise-attach"));
    expect(screen.getAllByRole("menuitem").map((item) => item.textContent)).toEqual(["Chọn đề mẫu có sẵn", "Chọn học liệu của môn", "Tải từ máy"]);
    fireEvent.click(screen.getByRole("menuitem", { name: "Chọn học liệu của môn" }));
    expect(screen.queryByRole("menuitem", { name: "Tải tệp lên" })).not.toBeInTheDocument();
    fireEvent.click(await screen.findByRole("menuitem", { name: "Chương 3 UML" }));
    expect(screen.queryByTestId("ai-revise-attach-menu")).toBeNull();
    fireEvent.click(screen.getByTestId("ai-revise-attach"));
    fireEvent.click(screen.getByRole("menuitem", { name: "Chọn đề mẫu có sẵn" }));
    expect(screen.getAllByRole("menuitem").map((item) => item.textContent))
      .toEqual(["Đề mẫu của môn", "Đề giữa kỳ năm trước"]);
    fireEvent.click(screen.getByRole("menuitem", { name: "Đề giữa kỳ năm trước" }));

    const chips = within(screen.getByTestId("ai-revise-attachments")).getAllByRole("listitem");
    expect(chips[0]).toHaveTextContent("Mẫu đề");
    expect(chips[0]).toHaveTextContent("Đề giữa kỳ năm trước");
    expect(chips[1]).toHaveTextContent("Tham khảo · học liệu");
    // Đã có mẫu đề: dòng Mẫu đề của menu khóa và đổi chữ.
    fireEvent.click(screen.getByTestId("ai-revise-attach"));
    expect(screen.getByRole("menuitem", { name: "Chọn đề mẫu có sẵn" })).toBeEnabled();
    fireEvent.click(screen.getByTestId("ai-revise-attach"));
    send("Làm lại đề theo mẫu này");

    expect(await screen.findByText(/Đã sửa đề theo yêu cầu/)).toBeInTheDocument();
    expect(request).toHaveBeenCalledWith("a1", "Làm lại đề theo mẫu này", expect.any(String),
      { formatText: undefined, templateAssignmentId: "o1", materialIds: ["m1"], sourceFileRefs: [] });
    const sent = screen.getByRole("list", { name: "Tệp gửi kèm" });
    expect(sent).toHaveTextContent("Mẫu đề: Đề giữa kỳ năm trước");
    expect(sent).toHaveTextContent("Tài liệu tham khảo: Chương 3 UML");
    expect(screen.queryByTestId("ai-revise-attachments")).toBeNull();
  });
});
