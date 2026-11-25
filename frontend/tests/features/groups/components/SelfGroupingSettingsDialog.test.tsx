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

  it("does not save an invalid optional deadline as null; clearing it deliberately is allowed", async () => {
    const calls = stub(() => json(VIEW));
    render(<SelfGroupingSettingsDialog classId="c1" onClose={vi.fn()} onSaved={vi.fn()} />);
    const input = await screen.findByTestId("formation-deadline");
    fireEvent.change(input, { target: { value: "31/04/2099" } });
    expect(screen.getByTestId("formation-save")).toBeDisabled();
    fireEvent.submit(input.closest("form")!);
    expect(calls.some((call) => call.method === "PUT")).toBe(false);
    fireEvent.change(input, { target: { value: "" } });
    fireEvent.click(screen.getByTestId("formation-save"));
    await waitFor(() => expect(calls.at(-1)).toMatchObject({ method: "PUT", body: { deadline: null } }));
  });

  it("server từ chối thì hiện thông điệp của server", async () => {
    stub(() => json({ code: "FORMATION_VERSION_CONFLICT", detail: "Cài đặt vừa được thay đổi, hãy tải lại" }, 409));
    render(<SelfGroupingSettingsDialog classId="c1" onClose={vi.fn()} onSaved={vi.fn()} />);

    fireEvent.click(await screen.findByTestId("formation-save"));

    expect(await screen.findByRole("alert")).toHaveTextContent("vừa thay đổi");
  });
});
