import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { resolveAssignmentContext } from "@/features/assignments/components/AssignmentNav";
import { outlineOf, publishOutline } from "@/features/assignments/model/outline";
import type { DocumentBlock } from "@/shared/document";

const heading = (id: string, style: "H1" | "H2" | "H3", text: string): DocumentBlock => ({ id, origin: "TEACHER", type: "HEADING", content: { style, text } });
const blocks: DocumentBlock[] = [
  heading("p1", "H1", "Hệ thống thư viện"),
  { id: "t1", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Mô tả" }] } },
  heading("q1", "H2", "Vẽ use case"),
  heading("q2", "H2", ""),
  heading("q2a", "H3", "Ý a"),
  heading("p2", "H1", "Đề phụ"),
];

afterEach(() => { act(() => publishOutline(null)); });

it("dàn ý theo cây câu hỏi, đánh số I., I.1., I.2.a. như trong trang; đoạn văn không ghi", () => {
  expect(outlineOf(blocks)).toEqual([
    { id: "p1", depth: 1, title: "Hệ thống thư viện", number: "I." },
    { id: "q1", depth: 2, title: "Vẽ use case", number: "I.1." },
    { id: "q2", depth: 2, title: "", number: "I.2." },
    { id: "q2a", depth: 3, title: "Ý a", number: "I.2.a." },
    { id: "p2", depth: 1, title: "Đề phụ", number: "II." },
  ]);
});

it("sidebar của bài: lối về Bài tập, mỗi phần một dòng; bấm thì báo trình soạn chuyển tới phần đó", () => {
  const context = resolveAssignmentContext({ pathname: "/assignments/a1", search: new URLSearchParams() })!;
  expect(resolveAssignmentContext({ pathname: "/assignments/a1/history", search: new URLSearchParams() })).toBeNull();
  render(<>{context.header}</>);
  act(() => publishOutline({ assignmentId: "a1", backHref: "/teaching/classes/c1?tab=evals", backLabel: "Bài tập", entries: outlineOf(blocks) }));
  expect(screen.getByRole("link", { name: /Bài tập/ })).toHaveAttribute("href", "/teaching/classes/c1?tab=evals");
  const problem = screen.getByRole("button", { name: /Hệ thống thư viện/ });
  expect(problem).toHaveClass("font-semibold");
  expect(screen.getByRole("button", { name: /^I\.2\.\s*I\.2\.$/ })).not.toHaveClass("font-semibold");
  const shown = vi.fn();
  window.addEventListener("document:show-part", shown);
  fireEvent.click(screen.getByRole("button", { name: /Vẽ use case/ }));
  expect((shown.mock.calls[0]![0] as CustomEvent).detail).toEqual({ headingId: "q1" });
  window.removeEventListener("document:show-part", shown);
});

it("sidebar có header Câu hỏi với nút + thêm câu hỏi ngoài cùng; mỗi hàng có + thêm ý tới tầng 4; chỉ khi sửa được", () => {
  const context = resolveAssignmentContext({ pathname: "/assignments/a1", search: new URLSearchParams() })!;
  render(<>{context.header}</>);
  act(() => publishOutline({ assignmentId: "a1", backHref: "/x", backLabel: "Bài tập", entries: outlineOf(blocks), editable: true }));
  expect(screen.getByText("Câu hỏi")).toBeInTheDocument();
  const added = vi.fn();
  window.addEventListener("document:add-part", added);
  fireEvent.click(screen.getByRole("button", { name: "Thêm câu hỏi" }));
  fireEvent.click(screen.getByRole("button", { name: "Thêm ý vào I." }));
  expect(added.mock.calls.map((call) => (call[0] as CustomEvent).detail)).toEqual([{ parentId: null }, { parentId: "p1" }]);
  // Tầng 3 (I.2.a.) vẫn thêm được ý tầng 4.
  expect(screen.getByRole("button", { name: "Thêm ý vào I.2.a." })).toBeInTheDocument();
  window.removeEventListener("document:add-part", added);
  act(() => publishOutline({ assignmentId: "a1", backHref: "/x", backLabel: "Bài tập", entries: outlineOf(blocks), editable: false }));
  expect(screen.queryByRole("button", { name: "Thêm câu hỏi" })).toBeNull();
});

it("văn bản ngoài mọi mục là một dòng tầng 0 trong Mục lục: mấy chữ đầu, chữ thường, không số, không nút +", () => {
  const loose: DocumentBlock[] = [{ id: "r", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Đọc kỹ đề trước khi làm" }] } }, ...blocks];
  const entries = outlineOf(loose);
  expect(entries[0]).toEqual({ id: "DOCUMENT", depth: 0, title: "Đọc kỹ đề trước khi làm", number: "", loose: true });
  const context = resolveAssignmentContext({ pathname: "/assignments/a1", search: new URLSearchParams() })!;
  render(<>{context.header}</>);
  act(() => publishOutline({ assignmentId: "a1", backHref: "/x", backLabel: "Bài tập", entries, editable: true }));
  const row = screen.getByRole("button", { name: /Đọc kỹ đề/ });
  expect(row).not.toHaveClass("font-semibold");
  expect(row.parentElement!.querySelectorAll("button")).toHaveLength(1);
});

it("Mục lục sáng theo mục trình soạn đang hiện", () => {
  const context = resolveAssignmentContext({ pathname: "/assignments/a1", search: new URLSearchParams() })!;
  render(<>{context.header}</>);
  act(() => publishOutline({ assignmentId: "a1", backHref: "/x", backLabel: "Bài tập", entries: outlineOf(blocks), editable: true }));
  act(() => { window.dispatchEvent(new CustomEvent("document:current-part", { detail: { headingId: "q1" } })); });
  expect(screen.getByRole("button", { name: /Vẽ use case/ })).toHaveAttribute("aria-current", "true");
  expect(screen.getByRole("button", { name: /Hệ thống thư viện/ })).not.toHaveAttribute("aria-current");
});

it("chữ nằm giữa hai câu là một dòng riêng ngay sau câu trước, không gộp với văn bản mở đầu (yêu cầu 2026-11-27)", () => {
  const p = (id: string, text: string, under?: number): DocumentBlock => ({ id, origin: "TEACHER", type: "PARAGRAPH",
    content: { runs: [{ text }], ...(under !== undefined ? { under } : {}) } });
  const h = (id: string, style: "H1" | "H2", text: string): DocumentBlock => ({ id, origin: "TEACHER", type: "HEADING", content: { style, text } });
  const entries = outlineOf([p("i", "Mở đầu"), h("q1", "H1", "Câu 1"), h("a", "H2", "Ý a"), p("m", "Giữa ý", 1), h("b", "H2", "Ý b"),
    p("g", "Giữa câu", 0), h("q2", "H1", "Câu 2")]);
  expect(entries.map((e) => [e.id, e.depth, e.title, e.loose ?? false])).toEqual([
    ["DOCUMENT", 0, "Mở đầu", true],
    ["q1", 1, "Câu 1", false],
    ["a", 2, "Ý a", false],
    ["after-a", 2, "Giữa ý", true],
    ["b", 2, "Ý b", false],
    ["after-q1", 0, "Giữa câu", true],
    ["q2", 1, "Câu 2", false],
  ]);
});
