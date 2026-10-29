import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Dialog } from "./Dialog";

afterEach(cleanup);
describe("Dialog focus và pending", () => {
  it("giữ Tab/Shift+Tab trong dialog và trả focus về nút mở khi đóng", () => {
    const trigger = document.createElement("button");
    document.body.appendChild(trigger); trigger.focus();
    const view = render(<Dialog open title="Form" onClose={vi.fn()} footer={<button>Cuối</button>}><input aria-label="Tên" /></Dialog>);
    expect(screen.getByRole("dialog")).toHaveFocus();
    fireEvent.keyDown(document, { key: "Tab" });
    expect(screen.getByRole("button", { name: "Đóng" })).toHaveFocus();
    fireEvent.keyDown(document, { key: "Tab", shiftKey: true });
    expect(screen.getByRole("button", { name: "Cuối" })).toHaveFocus();
    fireEvent.keyDown(document, { key: "Tab" });
    expect(screen.getByRole("button", { name: "Đóng" })).toHaveFocus();
    trigger.focus();
    expect(screen.getByRole("dialog")).toHaveFocus();
    view.unmount();
    expect(trigger).toHaveFocus(); trigger.remove();
  });

  it("thay callback khi rerender không cướp focus đang nhập", () => {
    const old = vi.fn(), latest = vi.fn();
    const view = render(<Dialog open title="Form" onClose={old}><input aria-label="Tên" /></Dialog>);
    screen.getByLabelText("Tên").focus();
    view.rerender(<Dialog open title="Form" onClose={latest}><input aria-label="Tên" /></Dialog>);
    expect(screen.getByLabelText("Tên")).toHaveFocus();
    fireEvent.keyDown(document, { key: "Escape" });
    expect(old).not.toHaveBeenCalled(); expect(latest).toHaveBeenCalledTimes(1);
  });

  it("dismissible=false chặn close/Escape/backdrop; mặc định backdrop vẫn đóng", () => {
    const onClose = vi.fn();
    const view = render(<Dialog open dismissible={false} title="Form" onClose={onClose}>Đang lưu</Dialog>);
    fireEvent.keyDown(document, { key: "Escape" });
    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));
    fireEvent.mouseDown(screen.getByRole("dialog").parentElement!);
    expect(onClose).not.toHaveBeenCalled();
    view.rerender(<Dialog open title="Form" onClose={onClose}>Xong</Dialog>);
    fireEvent.mouseDown(screen.getByRole("dialog").parentElement!);
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
