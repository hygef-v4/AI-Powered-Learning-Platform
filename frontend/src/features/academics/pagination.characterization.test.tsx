import {fireEvent, render, screen, within} from "@testing-library/react";
import {describe, expect, it} from "vitest";
import {ClassListPage} from "./ClassListPage";
import {EnrollmentPanel} from "./EnrollmentPanel";
import type {CourseClassView, EnrollmentView} from "./types";

const subject = {id: "s", code: "S", name: "Subject"};
const classes: CourseClassView[] = Array.from({length: 65}, (_, index) => ({
  id: `c${index}`, subject, code: `C${index}`, name: `Class ${index}`, term: "1",
  description: "", status: index < 40 ? "OPEN" : "DRAFT", teacher: null,
  activeStudentCount: 0, removedStudentCount: 0, createdAt: "2026-01-01",
  showGradeDistribution: false, version: 1,
}));
const students: EnrollmentView[] = Array.from({length: 90}, (_, index) => ({
  accountId: `s${index}`, name: `Student ${index}`, email: `s${index}@example.test`,
  enrolledAt: "2026-01-01", status: index < 65 ? "ACTIVE" : "REMOVED",
}));

describe.each(["classes", "students"] as const)("%s pagination characterization", kind => {
  function renderList() {
    return render(kind === "classes"
      ? <ClassListPage classes={classes} subjects={[subject]} onOpen={() => {}} onCreate={() => {}}/>
      : <EnrollmentPanel students={students} includeRemoved/>);
  }

  it("shows five rows on the last page and returns to the first page on search", () => {
    renderList();
    const next = screen.getByRole("button", {name: "Trang sau"});
    fireEvent.click(next);
    fireEvent.click(next);
    fireEvent.click(next);
    expect(screen.getByText("Trang 4 / 4")).toBeInTheDocument();
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(6);
    expect(next).toBeDisabled();
    fireEvent.change(screen.getByRole("searchbox"), {target: {value: "no-match"}});
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(1);
    expect(screen.queryByRole("navigation", {name: "Phân trang"})).not.toBeInTheDocument();
    fireEvent.change(screen.getByRole("searchbox"), {target: {value: ""}});
    expect(screen.getByText("Trang 1 / 4")).toBeInTheDocument();
    expect(screen.getByRole("button", {name: "Trang trước"})).toBeDisabled();
  });

  it("preserves the stored page when status filtering clamps the displayed page", () => {
    renderList();
    fireEvent.click(screen.getByRole("button", {name: "Trang sau"}));
    fireEvent.click(screen.getByRole("button", {name: "Trang sau"}));
    fireEvent.click(screen.getByRole("button", {name: kind === "classes" ? /Nháp/ : /Đã gỡ/}));
    expect(screen.getByText("Trang 2 / 2")).toBeInTheDocument();
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(6);
    expect(screen.getByRole("button", {name: "Trang sau"})).toBeDisabled();
    // Legacy: displayed page is clamped, but the first previous click changes stored 3 to 2.
    fireEvent.click(screen.getByRole("button", {name: "Trang trước"}));
    expect(screen.getByText("Trang 2 / 2")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", {name: "Trang trước"}));
    expect(screen.getByText("Trang 1 / 2")).toBeInTheDocument();
    expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(21);
  });
});
