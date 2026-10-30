import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { ClassAnnouncementsPage } from "./ClassAnnouncementsPage";
import { announcementsApi } from "../api";
import type { Announcement, ClassOption } from "../model/types";
vi.mock("../api", async original => ({ ...await original<typeof import("../api")>(), announcementsApi: { list: vi.fn(), create: vi.fn() } }));
const classes: ClassOption[] = [
  { classId: "c1", code: "SE1801", name: "Thiết kế", canManage: true, studentCount: 30 },
  { classId: "c2", code: "SE1802", name: "Thiết kế", canManage: true, studentCount: 28 },
];
const item: Announcement = { id: "a", classId: "c1", className: "Thiết kế", title: "Đổi lịch bảo vệ đồ án sang thứ Sáu tuần sau", body: "Nội dung đầy đủ của thông báo", important: true, sendEmail: false, createdBy: "t", authorName: "Giảng viên", createdAt: "2026-10-21T08:00:00Z", updatedAt: "2026-10-22T08:00:00Z", version: 1, canManage: true };
beforeEach(() => { vi.clearAllMocks(); vi.mocked(announcementsApi.list).mockResolvedValue({ items: [item], classes, page: 1, size: 20, hasMore: false }); });

it("trong lớp: không có ô chọn lớp, không ghi mã lớp hay đã sửa; tạo thông báo đăng thẳng vào lớp đó", async () => {
  vi.mocked(announcementsApi.create).mockResolvedValue(item);
  render(<ClassAnnouncementsPage classId="c1" />);
  const card = await screen.findByTestId("announcement-card");
  expect(announcementsApi.list).toHaveBeenCalledWith("c1", 1, 20, expect.any(AbortSignal));
  expect(screen.queryByTestId("announcement-class-filter")).not.toBeInTheDocument();
  expect(within(card).queryByText("SE1801")).not.toBeInTheDocument();
  expect(card).not.toHaveTextContent("đã sửa");
  expect(within(card).getByRole("heading", { name: item.title })).toBeInTheDocument();
  expect(card).toHaveTextContent("Nội dung đầy đủ của thông báo");

  fireEvent.click(screen.getByTestId("announcement-create"));
  const dialog = await screen.findByRole("dialog");
  expect(within(dialog).queryByTestId("announcement-classes")).not.toBeInTheDocument();
  expect(dialog).not.toHaveTextContent("Markdown");
  expect(dialog).not.toHaveTextContent("SE1801");
  fireEvent.change(within(dialog).getByTestId("announcement-create-title"), { target: { value: " Mới " } });
  fireEvent.change(within(dialog).getByTestId("announcement-create-body"), { target: { value: "Nội dung" } });
  fireEvent.click(within(dialog).getByTestId("announcement-create-submit"));
  await waitFor(() => expect(announcementsApi.create).toHaveBeenCalledWith("c1", { title: "Mới", body: "Nội dung", important: false, sendEmail: false }));
});

it("menu chính: lọc theo lớp, ghi mã lớp; popup chọn nhiều lớp ngay dưới Tiêu đề và đăng từng lớp", async () => {
  vi.mocked(announcementsApi.create).mockResolvedValue(item);
  render(<ClassAnnouncementsPage />);
  const card = await screen.findByTestId("announcement-card");
  expect(within(card).getByText("SE1801")).toBeInTheDocument();
  // Ô lọc cùng kiểu ô chuyển lớp trên sidebar: mã nhỏ trên tên, chọn một mục trong menu.
  expect(screen.getByTestId("announcement-class-filter")).toHaveTextContent("Tất cả lớp");
  expect(screen.getByTestId("announcement-class-filter")).not.toHaveTextContent("LỚP");
  fireEvent.click(screen.getByRole("menuitem", { name: /SE1802/, hidden: true }));
  await waitFor(() => expect(announcementsApi.list).toHaveBeenLastCalledWith("c2", 1, 20, expect.any(AbortSignal)));

  fireEvent.click(screen.getByTestId("announcement-create"));
  const dialog = await screen.findByRole("dialog");
  const order = [...dialog.querySelectorAll("[data-testid='announcement-create-title'], [data-testid='announcement-classes'], [data-testid='announcement-create-body']")].map((node) => node.getAttribute("data-testid"));
  expect(order).toEqual(["announcement-create-title", "announcement-classes", "announcement-create-body"]);
  fireEvent.change(within(dialog).getByTestId("announcement-create-title"), { target: { value: "Mới" } });
  fireEvent.change(within(dialog).getByTestId("announcement-create-body"), { target: { value: "Nội dung" } });
  expect(within(dialog).getByTestId("announcement-create-submit")).toBeDisabled();
  fireEvent.focus(within(dialog).getByTestId("announcement-classes-input"));
  // Gợi ý nổi ở body, đè lên phần dưới của popup.
  expect(within(dialog).queryByRole("listbox")).not.toBeInTheDocument();
  fireEvent.click(await screen.findByRole("option", { name: /Chọn tất cả/ }));
  fireEvent.click(within(dialog).getByTestId("announcement-create-submit"));
  await waitFor(() => expect(announcementsApi.create).toHaveBeenCalledTimes(2));
  expect(vi.mocked(announcementsApi.create).mock.calls.map((call) => call[0])).toEqual(["c1", "c2"]);
});

it("đăng lỗi ở một lớp thì giữ lại đúng lớp đó và báo tên lớp", async () => {
  vi.mocked(announcementsApi.create).mockImplementation(async (classId) => { if (classId === "c2") throw new Error("Lớp đã lưu trữ"); return item; });
  render(<ClassAnnouncementsPage />);
  fireEvent.click(await screen.findByTestId("announcement-create"));
  const dialog = await screen.findByRole("dialog");
  fireEvent.change(within(dialog).getByTestId("announcement-create-title"), { target: { value: "Mới" } });
  fireEvent.change(within(dialog).getByTestId("announcement-create-body"), { target: { value: "Nội dung" } });
  fireEvent.focus(within(dialog).getByTestId("announcement-classes-input"));
  fireEvent.click(await screen.findByRole("option", { name: /Chọn tất cả/ }));
  fireEvent.click(within(dialog).getByTestId("announcement-create-submit"));
  expect(await within(dialog).findByRole("alert")).toHaveTextContent("Chưa đăng được ở SE1802");
  expect(within(dialog).getByTestId("announcement-classes-input")).toHaveAttribute("title", "SE1802");
});

it("100 lớp: ô Lớp đóng chỉ một dòng tóm tắt; mở ra tìm được và chọn tất cả", async () => {
  const many = Array.from({ length: 100 }, (_, i) => ({ classId: `k${i}`, code: `SE${1800 + i}`, name: "Thiết kế", canManage: true, studentCount: 30 }));
  vi.mocked(announcementsApi.list).mockResolvedValue({ items: [], classes: many, page: 1, size: 20, hasMore: false });
  render(<ClassAnnouncementsPage />);
  expect(await screen.findByTestId("announcement-class-filter")).toBeInTheDocument();
  fireEvent.click(screen.getByTestId("announcement-create"));
  const dialog = await screen.findByRole("dialog");
  const input = within(dialog).getByTestId("announcement-classes-input");
  expect(input).toHaveValue("");
  expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  fireEvent.focus(input);
  fireEvent.change(input, { target: { value: "SE1899" } });
  expect(within(await screen.findByRole("listbox")).getAllByRole("option")).toHaveLength(1);
  fireEvent.change(input, { target: { value: "" } });
  fireEvent.click(await screen.findByRole("option", { name: /Chọn tất cả/ }));
  fireEvent.keyDown(input, { key: "Escape" });
  expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  expect(input).toHaveValue("Tất cả lớp (100)");
  expect(screen.getByRole("dialog")).toBeInTheDocument();
});

it("sinh viên không có nút tạo thông báo", async () => {
  vi.mocked(announcementsApi.list).mockResolvedValue({ items: [{ ...item, canManage: false }], classes: classes.map((c) => ({ ...c, canManage: false })), page: 1, size: 20, hasMore: false });
  render(<ClassAnnouncementsPage classId="c1" />);
  await screen.findByTestId("announcement-card");
  expect(screen.queryByTestId("announcement-create")).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: /Sửa/ })).not.toBeInTheDocument();
});
