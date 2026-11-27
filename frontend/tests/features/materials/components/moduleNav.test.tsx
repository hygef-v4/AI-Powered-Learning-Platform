import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { fetchApi } from "@/shared/api/client";
import { resolveModuleContext } from "@/features/materials/components/ModuleNav";

vi.mock("@/shared/api/client", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/shared/api/client")>()),
  fetchApi: vi.fn(),
}));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => "/quizzes/q1",
}));
vi.mock("@/features/identity/hooks/SessionProvider", () => ({
  useSession: () => ({ status: "authenticated", profile: { role: "TEACHER" } }),
}));

const paths = () => vi.mocked(fetchApi).mock.calls.map(([path]) => String(path));

function backend(quiz: { subjectId: string; scope: string; classId: string | null }) {
  vi.mocked(fetchApi).mockImplementation(async (path) => {
    if (String(path).startsWith("/api/v1/quizzes/")) return { quiz };
    return String(path).endsWith("/modules")
      ? [{ id: "m1", title: "Module 1", orderNo: 1, quizzes: [], canManage: true, version: 0 }]
      : [];
  });
}

function nav(search = "") {
  const context = resolveModuleContext({ pathname: "/quizzes/q1", search: new URLSearchParams(search) });
  render(<>{context?.header}</>);
}

beforeEach(() => {
  vi.mocked(fetchApi).mockReset();
});

it("Quiz của lớp mở không kèm classId vẫn dùng module của lớp", async () => {
  backend({ subjectId: "s1", scope: "CLASS", classId: "c1" });
  nav();

  expect(await screen.findByText("Module 1")).toBeInTheDocument();
  expect(paths()).toContain("/api/v1/classes/c1/modules");
  expect(paths().some((path) => path.includes("/subjects/"))).toBe(false);
  expect(screen.getByRole("link", { name: /Học liệu/ })).toHaveAttribute(
    "href",
    "/teaching/classes/c1?tab=materials",
  );
});

it("Quiz của môn vẫn dùng module của môn", async () => {
  backend({ subjectId: "s1", scope: "SUBJECT", classId: null });
  nav();

  await waitFor(() => expect(paths()).toContain("/api/v1/subjects/s1/modules"));
  expect(await screen.findByRole("link", { name: /Học liệu/ })).toHaveAttribute(
    "href",
    "/manager/materials?subjectId=s1",
  );
});

it("có classId trên URL thì không cần đọc Quiz", async () => {
  backend({ subjectId: "s1", scope: "SUBJECT", classId: null });
  nav("classId=c2");

  await waitFor(() => expect(paths()).toContain("/api/v1/classes/c2/modules"));
  expect(paths().some((path) => path.startsWith("/api/v1/quizzes/"))).toBe(false);
});

it("học liệu đã lưu trữ có nút khôi phục trên thanh bên", async () => {
  const archived = {
    id: "mat1", moduleId: "m1", title: "Slide cũ", status: "ARCHIVED", kind: "FILE", orderNo: 0, version: 3,
    canManage: true, enabled: true, current: { textStatus: "READY" },
  };
  vi.mocked(fetchApi).mockImplementation(async (path) => {
    const value = String(path);
    if (value.startsWith("/api/v1/quizzes/")) return { quiz: { subjectId: "s1", scope: "SUBJECT", classId: null } };
    if (value.endsWith("/restore")) return { ...archived, status: "ACTIVE", version: 4 };
    if (value.endsWith("/modules")) return [{ id: "m1", title: "Module 1", orderNo: 1, quizzes: [], canManage: true, version: 0 }];
    return value.includes("/materials") ? [archived] : [];
  });
  nav();

  const restore = await screen.findByRole("button", { name: "Khôi phục Slide cũ" });
  restore.click();
  await waitFor(() => expect(paths()).toContain("/api/v1/materials/mat1/restore"));
});
