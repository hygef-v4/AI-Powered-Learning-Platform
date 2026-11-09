import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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

  it("does not send an empty instruction", () => {
    const request = vi.spyOn(assignmentsApi, "requestAiRevision");
    render(<AiReviseChat assignmentId="a1" prepare={vi.fn()} onApplied={vi.fn()} />);

    send("   ");

    expect(request).not.toHaveBeenCalled();
    expect(screen.getByTestId("ai-revise-send")).toBeDisabled();
  });
});
