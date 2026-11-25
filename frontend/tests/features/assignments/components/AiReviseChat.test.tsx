import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { assignmentsApi } from "@/features/assignments/api";
import { AiReviseChat } from "@/features/assignments/components/AiReviseChat";
import type { AiDraft } from "@/features/assignments/model/types";
import { confirmAiSource, mockAiSources } from "./aiSourceFixture";

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
  beforeEach(() => mockAiSources());
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("reports an AI failure in the chat and leaves the assignment unchanged", async () => {
    vi.spyOn(assignmentsApi, "requestAiRevision").mockResolvedValue(draft("FAILED", { safeError: "Hết hạn mức." }));
    const apply = vi.spyOn(assignmentsApi, "applyAiDraft");
    render(<AiReviseChat assignmentId="a1" prepare={vi.fn().mockResolvedValue(3)} onApplied={vi.fn()} />);

    send("Thêm một câu hỏi");
    await confirmAiSource();

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
    await confirmAiSource();

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
});
