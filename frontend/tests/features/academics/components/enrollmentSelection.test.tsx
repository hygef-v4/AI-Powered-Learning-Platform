import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { EnrollmentPanel } from "@/features/academics/components/EnrollmentPanel";

const people = Array.from({ length: 35 }, (_, index) => ({ id: `s${index}`, name: `Student ${index}`, email: `s${index}@test.edu` }));

describe("enrollment selection", () => {
  it("loads more than 20, excludes enrolled students and submits one batch", async () => {
    const onBatch = vi.fn(async (text: string) => text.split("\n").map((email, index) => ({ line: index + 1, email, result: "ENROLLED" as const })));
    render(<EnrollmentPanel students={[{ accountId: "s0", name: "Student 0", email: people[0]!.email, status: "ACTIVE", enrolledAt: "2026-01-01" }]}
      editable candidates={[]} loadCandidates={async () => people} onEnroll={vi.fn()} onBatch={onBatch} />);
    fireEvent.click(screen.getByRole("button", { name: "Thêm sinh viên" }));
    const dialog = screen.getByRole("dialog", { name: "Thêm sinh viên" });
    const all = await within(dialog).findByRole("checkbox", { name: /Chọn tất cả kết quả lọc \(34\)/ });
    fireEvent.click(all);
    fireEvent.click(within(dialog).getByRole("button", { name: "Thêm 34 sinh viên" }));
    await waitFor(() => expect(onBatch).toHaveBeenCalledOnce());
    expect(onBatch.mock.calls[0]![0].split("\n")).toHaveLength(34);
    expect(onBatch.mock.calls[0]![0].split("\n")).not.toContain(people[0]!.email);
  });

  it("preserves selections across filters and leaves failed rows selected after partial success", async () => {
    const onBatch = vi.fn(async () => [
      { line: 1, email: people[0]!.email, result: "ENROLLED" as const },
      { line: 2, email: people[1]!.email, result: "IN_OTHER_CLASS" as const },
    ]);
    render(<EnrollmentPanel students={[]} editable candidates={people.slice(0, 2)} onEnroll={vi.fn()} onBatch={onBatch} />);
    fireEvent.click(screen.getByRole("button", { name: "Thêm sinh viên" }));
    const dialog = screen.getByRole("dialog", { name: "Thêm sinh viên" });
    fireEvent.click(within(dialog).getByRole("checkbox", { name: /Student 0/ }));
    fireEvent.change(within(dialog).getByRole("searchbox"), { target: { value: "Student 1" } });
    fireEvent.click(within(dialog).getByRole("checkbox", { name: /Chọn tất cả/ }));
    fireEvent.click(within(dialog).getByRole("button", { name: "Thêm 2 sinh viên" }));
    expect(await within(dialog).findByText("Đang ở lớp khác cùng môn")).toBeInTheDocument();
    expect(within(dialog).getByRole("button", { name: "Thêm 1 sinh viên" })).toBeEnabled();
  });
});
