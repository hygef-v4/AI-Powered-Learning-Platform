import {fireEvent, render, screen, within} from "@testing-library/react";
import {describe, expect, it} from "vitest";
import {ClassListPage} from "./ClassListPage";
import {EnrollmentPanel} from "./EnrollmentPanel";
import type {CourseClassView, EnrollmentView} from "./types";

const subject = {id: "s", code: "S", name: "Subject"};
const classes: CourseClassView[] = Array.from({length: 65}, (_, index) => ({
  id: `c${index}`, subject, code: `C${index}`, name: `Class ${index}`, term: "1",
  description: "", status: index < 40 ? "OPEN" : "ARCHIVED", teacher: null,
  activeStudentCount: 0, removedStudentCount: 0, createdAt: "2026-01-01",
  showGradeDistribution: false, version: 1,
}));
const students: EnrollmentView[] = Array.from({length: 90}, (_, index) => ({
  accountId: `s${index}`, name: `Student ${index}`, email: `s${index}@example.test`,
  enrolledAt: "2026-01-01", status: index < 65 ? "ACTIVE" : "REMOVED",
}));

describe("classes pagination", () => {
  function renderList() {
    return render(<ClassListPage classes={classes} subjects={[subject]} onOpen={() => {}} onCreate={() => {}}/>);
  }

  it("shows five rows on the last page and returns to the first page on search", () => {
    renderList();
    fireEvent.click(screen.getByRole("button", {name: "Trang 4"}));
    expect(screen.getByRole("button", {name: "Trang 4"})).toHaveAttribute("aria-current", "page");
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(6);
    expect(screen.getByRole("button", {name: "Trang sau"})).toBeDisabled();
    fireEvent.change(screen.getByRole("searchbox"), {target: {value: "no-match"}});
    expect(screen.getByText("Không có lớp phù hợp.")).toBeInTheDocument();
    expect(screen.queryByRole("navigation", {name: "Phân trang"})).not.toBeInTheDocument();
    fireEvent.change(screen.getByRole("searchbox"), {target: {value: ""}});
    expect(screen.getByRole("button", {name: "Trang 1"})).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("button", {name: "Trang trước"})).toBeDisabled();
  });

  it("changing the status tab goes back to the first page", () => {
    renderList();
    fireEvent.click(screen.getByRole("button", {name: "Trang 2"}));
    fireEvent.click(screen.getByRole("tab", {name: "Lưu trữ"}));
    expect(screen.getByRole("button", {name: "Trang 1"})).toHaveAttribute("aria-current", "page");
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(21);
    fireEvent.click(screen.getByRole("button", {name: "Trang sau"}));
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(6);
  });
});

describe.each(["students"] as const)("%s pagination characterization", kind => {
  function renderList() {
    return render(<EnrollmentPanel students={students} includeRemoved/>);
  }

  it("shows five rows on the last page and returns to the first page on search", () => {
    renderList();
    fireEvent.click(screen.getByRole("button", {name: "Trang 4"}));
    expect(screen.getByRole("button", {name: "Trang 4"})).toHaveAttribute("aria-current", "page");
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(6);
    expect(screen.getByRole("button", {name: "Trang sau"})).toBeDisabled();
    fireEvent.change(screen.getByRole("searchbox"), {target: {value: "no-match"}});
    expect(screen.getByText("Không có sinh viên phù hợp.")).toBeInTheDocument();
    expect(screen.queryByRole("navigation", {name: "Phân trang"})).not.toBeInTheDocument();
    fireEvent.change(screen.getByRole("searchbox"), {target: {value: ""}});
    expect(screen.getByRole("button", {name: "Trang 1"})).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("button", {name: "Trang trước"})).toBeDisabled();
  });

  it("changing the status tab goes back to the first page", () => {
    renderList();
    fireEvent.click(screen.getByRole("button", {name: "Trang 2"}));
    fireEvent.click(screen.getByRole("tab", {name: kind === "students" ? "Đã gỡ" : "Nháp"}));
    expect(screen.getByRole("button", {name: "Trang 1"})).toHaveAttribute("aria-current", "page");
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(21);
    fireEvent.click(screen.getByRole("button", {name: "Trang sau"}));
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(6);
  });
});
