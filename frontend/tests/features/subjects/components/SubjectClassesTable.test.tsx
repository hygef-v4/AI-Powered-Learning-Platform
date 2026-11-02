import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SubjectClassesTab } from "@/features/subjects/components/SubjectClassesTab";
import type { SubjectClassSummary } from "@/features/subjects/model/types";

vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams(window.location.search) }));
afterEach(() => window.history.replaceState(null, "", "/"));

const items: SubjectClassSummary[] = [
  { classId: "a", code: "C-A", name: "Lớp nháp", term: "2026-1", status: "DRAFT", teacherName: null, activeStudentCount: 100 },
  { classId: "b", code: "C-B", name: "Lớp mở", term: "2026-1", status: "OPEN", teacherName: "Giảng viên B", activeStudentCount: 200 },
  { classId: "c", code: "C-C", name: "Lớp lưu trữ", term: "2026-2", status: "ARCHIVED", teacherName: "Giảng viên C", activeStudentCount: 300 },
];

describe("SubjectClassesTable/Tab", () => {
  afterEach(cleanup);
  it("bảng chỉ đọc có ba cột (mã lớp, giảng viên, trạng thái), không link/class mutation hoặc sĩ số", () => {
    render(<SubjectClassesTab items={items} />);
    const table = screen.getByRole("table");
    expect(within(table).getAllByRole("columnheader")).toHaveLength(3);
    expect(within(table).queryByRole("link")).not.toBeInTheDocument();
    expect(within(table).queryByRole("button")).not.toBeInTheDocument();
    expect(within(table).getByText("Chưa phân công")).toBeInTheDocument();
    expect(within(table).queryByText("200")).not.toBeInTheDocument();
  });

  it("lọc theo trạng thái và ô tìm mã lớp", () => {
    render(<SubjectClassesTab items={items} />);
    fireEvent.click(screen.getByTestId("subjects-classes-filter-OPEN"));
    expect(screen.getByText("C-B")).toBeInTheDocument();
    expect(screen.queryByText("C-A")).not.toBeInTheDocument();
    expect(screen.getByTestId("subjects-classes-filter-OPEN")).toHaveAttribute("aria-selected", "true");
    expect(screen.queryByLabelText("Lọc lớp theo học kỳ")).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Lọc theo mã lớp"), { target: { value: "C-C" } });
    expect(screen.getByText("Không có lớp phù hợp.")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("subjects-classes-filter-ARCHIVED"));
    expect(screen.getByText("C-C")).toBeInTheDocument();
    expect(screen.queryByText(/\/ 3 lớp/)).not.toBeInTheDocument();
  });

  it("verified0 hiển thị empty thật và bộ lọc vẫn dùng được", () => {
    render(<SubjectClassesTab items={[]} />);
    expect(screen.getByText("Không có lớp phù hợp.")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("subjects-classes-filter-DRAFT"));
    expect(screen.getByText("Không có lớp phù hợp.")).toBeInTheDocument();
  });
});

it("restores the subject class status and search independently of the subject tab", () => {
  window.history.replaceState(null, "", "/subjects/s?tab=classes&subjectClassStatus=ARCHIVED&subjectClassQuery=C-C");
  const first = render(<SubjectClassesTab items={items} />);
  expect(screen.getByRole("tab", { name: "Lưu trữ" })).toHaveAttribute("aria-selected", "true");
  expect(screen.getByRole("searchbox")).toHaveValue("C-C");
  expect(screen.getByText("C-C")).toBeInTheDocument();
  expect(screen.queryByText("C-B")).not.toBeInTheDocument();
  first.unmount();
  render(<SubjectClassesTab items={items} />);
  expect(screen.getByText("C-C")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("tab", { name: "Đang mở" }));
  expect(new URLSearchParams(window.location.search).get("tab")).toBe("classes");
});
