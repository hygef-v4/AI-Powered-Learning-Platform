import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SelfGroupingSettingsDialog } from "@/features/groups/components/SelfGroupingSettingsDialog";
import type { FormationView } from "@/features/groups/model/types";

const VIEW: FormationView = { classId: "c1", enabled: false, minSize: 2, maxSize: 5, deadline: null, version: 0, open: false, closedReason: "DISABLED" };

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }));
}

function stub(put: () => Promise<Response>) {
  const calls: { method: string; body?: unknown }[] = [];
  vi.stubGlobal("fetch", vi.fn((_input: RequestInfo | URL, init?: RequestInit) => {
    const method = init?.method ?? "GET";
    calls.push({ method, body: init?.body ? JSON.parse(String(init.body)) : undefined });
    return method === "PUT" ? put() : json(VIEW);
  }));
  return calls;
}

describe("SelfGroupingSettingsDialog", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("bật đợt gửi sĩ số, hạn chót theo giờ Việt Nam và version", async () => {
    const saved = { ...VIEW, enabled: true, version: 1, open: true, closedReason: null };
    const calls = stub(() => json(saved));
    const onSaved = vi.fn();
    render(<SelfGroupingSettingsDialog classId="c1" onClose={vi.fn()} onSaved={onSaved} />);

    fireEvent.click(await screen.findByRole("switch"));
    fireEvent.change(screen.getByTestId("formation-max"), { target: { value: "4" } });
    fireEvent.change(screen.getByTestId("formation-deadline"), { target: { value: "2099-11-20T10:00" } });
    fireEvent.click(screen.getByTestId("formation-save"));

    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(saved));
    expect(calls.at(-1)).toMatchObject({
      method: "PUT", body: { enabled: true, minSize: 2, maxSize: 4, deadline: "2099-11-20T03:00:00.000Z", version: 0 },
    });
  });

  it("sĩ số sai thì báo dưới ô và không gửi", async () => {
    const calls = stub(() => json(VIEW));
    render(<SelfGroupingSettingsDialog classId="c1" onClose={vi.fn()} onSaved={vi.fn()} />);

    fireEvent.change(await screen.findByTestId("formation-min"), { target: { value: "6" } });
    fireEvent.click(screen.getByTestId("formation-save"));

    expect(await screen.findByText("Tối đa phải lớn hơn hoặc bằng tối thiểu.")).toBeInTheDocument();
    expect(calls.some((c) => c.method === "PUT")).toBe(false);
  });

  it("server từ chối thì hiện thông điệp của server", async () => {
    stub(() => json({ code: "FORMATION_VERSION_CONFLICT", detail: "Cài đặt vừa được thay đổi, hãy tải lại" }, 409));
    render(<SelfGroupingSettingsDialog classId="c1" onClose={vi.fn()} onSaved={vi.fn()} />);

    fireEvent.click(await screen.findByTestId("formation-save"));

    expect(await screen.findByRole("alert")).toHaveTextContent("vừa thay đổi");
  });
});
