import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SettingValueForm } from "./SettingValueForm";
import type { SettingItem } from "../model/types";

const item: SettingItem = {
  key: "files.material.maxSizeMb",
  group: "FILES",
  name: "Học liệu tối đa",
  description: "",
  type: "INTEGER",
  value: 50,
  defaultValue: 50,
  constraints: { min: 1, max: 50, unit: "MB" },
  version: 4,
  updatedBy: null,
  updatedAt: null,
};

const input = () => screen.getByLabelText(/Giá trị mới/);
const saveButton = () => screen.getByRole("button", { name: "Lưu" });

describe("SettingValueForm", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("báo lỗi giới hạn ngay khi nhập, khóa nút Lưu và không gọi API", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    render(<SettingValueForm item={item} onSaved={vi.fn()} onReload={vi.fn()} />);

    fireEvent.change(input(), { target: { value: "80" } });

    expect(await screen.findByRole("alert")).toHaveTextContent("Giá trị phải từ 1 đến 50");
    expect(saveButton()).toBeDisabled();
    fireEvent.submit(saveButton().closest("form")!);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("hiện đơn vị cạnh ô nhập và khóa Lưu khi chưa đổi gì", () => {
    render(<SettingValueForm item={item} onSaved={vi.fn()} onReload={vi.fn()} />);

    expect(screen.getByText("MB")).toBeInTheDocument();
    expect(saveButton()).toBeDisabled();
  });

  it("gửi kèm version và báo người khác vừa sửa khi 409, giữ giá trị đang nhập", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ detail: "x" }), { status: 409 }));
    vi.stubGlobal("fetch", fetchMock);
    const onReload = vi.fn();
    render(<SettingValueForm item={item} onSaved={vi.fn()} onReload={onReload} />);

    fireEvent.change(input(), { target: { value: "20" } });
    fireEvent.click(saveButton());

    expect(await screen.findByRole("alert")).toHaveTextContent("đã được người khác sửa");
    expect(JSON.parse(fetchMock.mock.calls[0]?.[1].body)).toEqual({ value: 20, version: 4 });
    expect(input()).toHaveValue(20);
    fireEvent.click(screen.getByRole("button", { name: "Tải lại" }));
    expect(onReload).toHaveBeenCalled();
  });

  it("hiện lý do backend trả khi 400", async () => {
    const problem = { detail: "Chỉ bật được khi đã đặt giờ chạy" };
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify(problem), { status: 400 })));
    const toggle = { ...item, key: "backup.enabled", type: "BOOLEAN" as const, value: false, defaultValue: false, constraints: {} };
    render(<SettingValueForm item={toggle} onSaved={vi.fn()} onReload={vi.fn()} />);

    fireEvent.click(screen.getByRole("switch", { name: "Giá trị mới" }));
    fireEvent.click(saveButton());

    expect(await screen.findByRole("alert")).toHaveTextContent("Chỉ bật được khi đã đặt giờ chạy");
    expect(screen.getByRole("switch")).toHaveAttribute("aria-checked", "true");
  });

  it("Hủy trả về giá trị hiện hành", () => {
    render(<SettingValueForm item={item} onSaved={vi.fn()} onReload={vi.fn()} />);

    fireEvent.change(input(), { target: { value: "80" } });
    fireEvent.click(screen.getByRole("button", { name: "Hủy" }));

    expect(input()).toHaveValue(50);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("chọn nhiều bằng nhãn bật tắt và lưu theo thứ tự lựa chọn", async () => {
    const types = {
      ...item,
      key: "files.material.allowedTypes",
      type: "ENUM_SET" as const,
      value: ["PDF"],
      defaultValue: ["PDF", "DOCX", "PPTX"],
      constraints: { options: ["PDF", "DOCX", "PPTX"], nonEmpty: true },
    };
    const saved = { ...types, value: ["PDF", "PPTX"], version: 5 };
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(saved), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const onSaved = vi.fn();
    render(<SettingValueForm item={types} onSaved={onSaved} onReload={vi.fn()} />);

    fireEvent.click(screen.getByRole("button", { name: "PPTX" }));
    fireEvent.click(saveButton());

    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(saved));
    expect(JSON.parse(fetchMock.mock.calls[0]?.[1].body)).toEqual({ value: ["PDF", "PPTX"], version: 4 });
  });

  it("lưu thành công trả mục mới", async () => {
    const saved = { ...item, value: 20, version: 5 };
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify(saved), { status: 200 })));
    const onSaved = vi.fn();
    render(<SettingValueForm item={item} onSaved={onSaved} onReload={vi.fn()} />);

    fireEvent.change(input(), { target: { value: "20" } });
    fireEvent.click(saveButton());

    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(saved));
  });
});
