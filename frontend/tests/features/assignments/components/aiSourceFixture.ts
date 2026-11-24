import { fireEvent, screen, within } from "@testing-library/react";
import { vi } from "vitest";
import type { AiTaskView, SourceOption } from "@/shared/ai";

const SOURCES: SourceOption[] = [
  {
    source: "SHARED", provider: "OPENAI", model: "school-model", keyHint: null, available: true,
    reason: null, quota: { used: 20, limit: 100, left: 80, resetAt: "now" },
    costBearer: "SCHOOL", preferred: true,
  },
  {
    source: "PERSONAL", provider: "OPENAI", model: "personal-model", keyHint: null,
    available: true, reason: null, quota: null, costBearer: "SELF",
  },
];

export function mockAiSources(readTask?: () => AiTaskView) {
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url.endsWith("/api/v1/ai/sources")) return Response.json(SOURCES);
    if (readTask && url.endsWith("/api/v1/ai/tasks/t1")) return Response.json(readTask());
    return Response.json({ status: 404 }, { status: 404 });
  }));
}

export async function confirmAiSource(label?: RegExp) {
  const picker = within(await screen.findByRole("dialog", { name: "Chọn nguồn AI" }));
  const button = picker.getByRole("button", { name: "Gửi yêu cầu" });
  const row = await picker.findByRole("radio", { name: label ?? /API của trường/ });
  if (label) fireEvent.click(row);
  fireEvent.click(button);
}
