import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SettingValueForm } from "./SettingValueForm";
import type { SettingItem } from "./types";

const item: SettingItem = {
  key: "files.material.maxSizeMb",
  group: "FILES",
  name: "Dung lượng tối đa học liệu (MB)",
  description: "",
  type: "INTEGER",
  value: 50,
  defaultValue: 50,
  constraints: { min: 1, max: 50 },
  version: 4,
  updatedBy: null,
  updatedAt: null,
};

describe("SettingValueForm", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("báo lỗi giới hạn mà không gọi API", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    render(<SettingValueForm item={item} onSaved={vi.fn()} onReload={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/Giá trị của/), { target: { value: "80" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Giá trị phải từ 1 đến 50");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("gửi kèm version và báo xung đột khi 409, giữ giá trị đang nhập", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ detail: "x" }), { status: 409 }));
    vi.stubGlobal("fetch", fetchMock);
    render(<SettingValueForm item={item} onSaved={vi.fn()} onReload={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/Giá trị của/), { target: { value: "20" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("đã được người khác sửa");
    expect(JSON.parse(fetchMock.mock.calls[0]?.[1].body)).toEqual({ value: 20, version: 4 });
    expect(screen.getByLabelText(/Giá trị của/)).toHaveValue(20);
  });

  it("lưu thành công trả mục mới", async () => {
    const saved = { ...item, value: 20, version: 5 };
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify(saved), { status: 200 })));
    const onSaved = vi.fn();
    render(<SettingValueForm item={item} onSaved={onSaved} onReload={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/Giá trị của/), { target: { value: "20" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(saved));
  });
});
