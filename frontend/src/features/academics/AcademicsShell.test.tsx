import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import "@/features/register";
import { AcademicsShell } from "./AcademicsShell";

const cases = [
  {
    audience: "manager",
    label: "Chủ nhiệm môn",
    buttons: ["Tổng quan", "Môn học của tôi", "Lớp học", "Lớp được phân công"],
    links: [
      ["Ngân hàng câu hỏi", "/manager/questions"],
      ["Quiz", "/manager/quizzes"],
      ["Học liệu", "/manager/materials"],
      ["Bài tập", "/manager/assignments"],
    ],
  },
  {
    audience: "teacher",
    label: "Giảng viên",
    buttons: ["Lớp được phân công"],
    links: [["Thông báo lớp", "/announcements"]],
  },
  {
    audience: "student",
    label: "Sinh viên",
    buttons: ["Lớp của tôi"],
    links: [["Bài tập", "/my-assignments"], ["Luyện tập Quiz", "/quiz-practice"], ["Thông báo lớp", "/announcements"]],
  },
] as const;

describe("AcademicsShell navigation after merging main", () => {
  it.each(cases)("renders $audience links in registry order without duplicating local navigation", ({ audience, label, buttons, links }) => {
    render(<AcademicsShell audience={audience} userName="Nguyễn An" active="classes" onNavigate={vi.fn()}><p>Nội dung lớp</p></AcademicsShell>);
    const menu = within(screen.getByRole("navigation", { name: `Menu ${label}` }));
    expect(menu.getAllByRole("button").map((button) => button.textContent)).toEqual(buttons);
    expect(menu.getAllByRole("link").map((link) => [link.textContent, link.getAttribute("href")])).toEqual(links);
    for (const title of buttons) expect(menu.getAllByText(title)).toHaveLength(1);
    expect(screen.getByText("Nội dung lớp")).toBeInTheDocument();
  });

  it("keeps navigation callbacks and active state", () => {
    const onNavigate = vi.fn();
    render(<AcademicsShell audience="manager" userName="Nguyễn An" active="overview" onNavigate={onNavigate}><p>Nội dung lớp</p></AcademicsShell>);
    const menu = within(screen.getByRole("navigation", { name: "Menu Chủ nhiệm môn" }));
    expect(menu.getByRole("button", { name: "Tổng quan" })).toHaveAttribute("aria-current", "page");
    fireEvent.click(menu.getByRole("button", { name: "Lớp học" }));
    expect(onNavigate).toHaveBeenCalledExactlyOnceWith("classes");
  });
});
