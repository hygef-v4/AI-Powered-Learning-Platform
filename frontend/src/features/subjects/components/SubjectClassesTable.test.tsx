import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import { SubjectClassesTab } from "./SubjectClassesTab";
import type { SubjectClassSummary } from "../model/types";

const items: SubjectClassSummary[] = [
  { classId: "a", code: "C-A", name: "Lớp nháp", term: "2026-1", status: "DRAFT", teacherName: null, activeStudentCount: 100 },
  { classId: "b", code: "C-B", name: "Lớp mở", term: "2026-1", status: "OPEN", teacherName: "Giảng viên B", activeStudentCount: 200 },
  { classId: "c", code: "C-C", name: "Lớp lưu trữ", term: "2026-2", status: "ARCHIVED", teacherName: "Giảng viên C", activeStudentCount: 300 },
];

describe("SubjectClassesTable/Tab", () => {
  afterEach(cleanup);
  it("bảng chỉ đọc có năm cột, không link/class mutation hoặc sĩ số", () => {
    render(<SubjectClassesTab items={items} />);
    const table = screen.getByRole("table");
    expect(within(table).getAllByRole("columnheader")).toHaveLength(5);
    expect(within(table).queryByRole("link")).not.toBeInTheDocument();
    expect(within(table).queryByRole("button")).not.toBeInTheDocument();
    expect(within(table).getByText("Chưa phân công")).toBeInTheDocument();
    expect(within(table).queryByText("200")).not.toBeInTheDocument();
  });

  it("lọc theo trạng thái và ô tìm mã hoặc tên lớp", () => {
    render(<SubjectClassesTab items={items} />);
    fireEvent.click(screen.getByTestId("subjects-classes-filter-OPEN"));
    expect(screen.getByText("Lớp mở")).toBeInTheDocument();
    expect(screen.queryByText("Lớp nháp")).not.toBeInTheDocument();
    expect(screen.getByTestId("subjects-classes-filter-OPEN")).toHaveAttribute("aria-selected", "true");
    expect(screen.queryByLabelText("Lọc lớp theo học kỳ")).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Lọc theo mã hoặc tên lớp"), { target: { value: "lưu trữ" } });
    expect(screen.getByText("Không có lớp phù hợp.")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("subjects-classes-filter-ARCHIVED"));
    expect(screen.getByText("Lớp lưu trữ")).toBeInTheDocument();
    expect(screen.queryByText(/\/ 3 lớp/)).not.toBeInTheDocument();
  });

  it("verified0 hiển thị empty thật và bộ lọc vẫn dùng được", () => {
    render(<SubjectClassesTab items={[]} />);
    expect(screen.getByText("Không có lớp phù hợp.")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("subjects-classes-filter-DRAFT"));
    expect(screen.getByText("Không có lớp phù hợp.")).toBeInTheDocument();
  });
});
