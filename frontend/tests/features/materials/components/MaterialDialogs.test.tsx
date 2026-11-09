import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { apiFetch, ApiError } from "@/shared/api/client";
import { ModuleItemDialog } from "@/features/materials/components/MaterialDialogs";
vi.mock("@/shared/api/client", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/shared/api/client")>()),
  apiFetch: vi.fn(),
}));
const { push } = vi.hoisted(() => ({ push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
beforeEach(() => {
  push.mockReset();
  vi.mocked(apiFetch).mockReset();
  vi.mocked(apiFetch).mockImplementation(async (path) =>
    String(path).includes("/files/") ? [] : {},
  );
});
const posts = () => vi.mocked(apiFetch).mock.calls.filter(([, init]) => init?.method === "POST");

const scope = { kind: "subject" as const, subjectId: "s1" };
function youtube(saved = vi.fn()) {
  render(<ModuleItemDialog scope={scope} moduleId="m1" onClose={vi.fn()} onSaved={saved} />);
  fireEvent.click(screen.getByRole("radio", { name: "YouTube" }));
  fireEvent.change(screen.getByTestId("material-source-title"), { target: { value: "Video" } });
  return saved;
}

it("lays out name, content choice and description in that order", () => {
  render(<ModuleItemDialog scope={scope} moduleId="m1" onClose={vi.fn()} onSaved={vi.fn()} />);
  const labels = [...document.querySelectorAll("label, span")].map(
    (node) => node.textContent ?? "",
  );
  expect(labels.findIndex((t) => t.startsWith("Tên học liệu"))).toBeLessThan(
    labels.findIndex((t) => t.startsWith("Nội dung")),
  );
  expect(labels.findIndex((t) => t.startsWith("Nội dung"))).toBeLessThan(
    labels.findIndex((t) => t.startsWith("Mô tả")),
  );
  expect(screen.getByRole("radio", { name: "Tệp" })).toBeChecked();
});

it("rejects playlist before creating anything", async () => {
  youtube();
  fireEvent.change(screen.getByTestId("material-source-url"), {
    target: { value: "https://youtube.com/playlist?list=x" },
  });
  fireEvent.click(screen.getByTestId("material-source-save"));
  expect(await screen.findByRole("alert")).toHaveTextContent("một video YouTube");
  expect(posts()).toHaveLength(0);
});

it("creates a video copied from a playlist inside the module", async () => {
  const saved = youtube();
  fireEvent.change(screen.getByTestId("material-source-url"), {
    target: { value: "https://youtu.be/abcdefghijk?list=x&index=2" },
  });
  fireEvent.click(screen.getByTestId("material-source-save"));
  await waitFor(() => expect(saved).toHaveBeenCalledOnce());
  const [path, init] = posts()[0]!;
  expect(path).toBe("/api/v1/subjects/s1/materials");
  expect(JSON.parse(String(init!.body))).toMatchObject({
    kind: "YOUTUBE_LINK",
    title: "Video",
    moduleId: "m1",
  });
});

it("keeps the dialog open and shows conflict with reload guidance", async () => {
  vi.mocked(apiFetch).mockImplementation(async (path, init) => {
    if (init?.method === "POST") throw new ApiError({ status: 409 });
    return String(path).includes("/files/") ? [] : {};
  });
  const saved = youtube();
  fireEvent.change(screen.getByTestId("material-source-url"), {
    target: { value: "https://youtu.be/abcdefghijk" },
  });
  fireEvent.click(screen.getByTestId("material-source-save"));
  expect(await screen.findByRole("alert")).toHaveTextContent("Tải lại");
  expect(saved).not.toHaveBeenCalled();
});

it("creating a new quiz notifies the parent before navigating to the attached quiz", async () => {
  vi.mocked(apiFetch).mockImplementation(async (_path, init) =>
    !init?.method || init.method === "GET"
      ? []
      : {
          quiz: {
            id: "q9",
            version: 1,
            title: "Quiz mới",
            subjectId: "s1",
            scope: "SUBJECT",
            classId: null,
            status: "DRAFT",
          },
          items: [],
          drawRules: [],
        },
  );
  const saved = vi.fn();
  render(<ModuleItemDialog scope={scope} moduleId="m1" onClose={vi.fn()} onSaved={saved} />);
  fireEvent.click(screen.getByRole("radio", { name: "Quiz" }));
  fireEvent.change(screen.getByTestId("material-source-title"), { target: { value: "Quiz mới" } });
  fireEvent.click(screen.getByTestId("material-source-save"));
  await waitFor(() => expect(saved).toHaveBeenCalledWith("Đã tạo quiz"));
  expect(push).toHaveBeenCalledExactlyOnceWith("/quizzes/q9");
  expect(saved.mock.invocationCallOrder[0]).toBeLessThan(push.mock.invocationCallOrder[0]!);
  expect(posts().map(([path]) => path)).toContain("/api/v1/material-modules/m1/quizzes");
});

it("creates disabled material with structured description and matching legacy plain text", async () => {
  const saved = youtube();
  fireEvent.change(screen.getByTestId("material-source-url"), { target: { value: "https://youtu.be/abcdefghijk" } });
  fireEvent.click(screen.getByRole("switch", { name: "Bật học liệu" }));
  const description = screen.getByRole("textbox", { name: "Mô tả" });
  description.textContent = "Nội dung chính";
  fireEvent.input(description);
  fireEvent.focus(description);
  fireEvent.click(screen.getByRole("button", { name: "Đậm" }));
  fireEvent.click(screen.getByTestId("material-source-save"));
  await waitFor(() => expect(saved).toHaveBeenCalledOnce());
  expect(JSON.parse(String(posts()[0]![1]!.body))).toMatchObject({
    description: "Nội dung chính", enabled: false,
    descriptionDocument: { blocks: [{ runs: [{ text: "Nội dung chính", bold: true }] }] },
  });
});
