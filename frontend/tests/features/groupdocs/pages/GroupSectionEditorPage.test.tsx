import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { groupDocContext } from "@/features/groupdocs/components/groupDocNav";
import { GroupSectionEditorPage } from "@/features/groupdocs/pages/GroupSectionEditorPage";
import type { DocumentBlock } from "@/shared/document";
import { publishOutline } from "@/shared/document/outline";
import type { SectionView, WorkspaceView } from "@/features/groupdocs/model/types";

const router = vi.hoisted(() => ({ push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router }));
vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => <a href={href} {...rest}>{children}</a>,
}));

const ME = "member";
const OTHER = "leader";
const heading = (id: string, text: string): DocumentBlock => ({ id, origin: "TEACHER", type: "HEADING", content: { style: "H1", text } });
const paragraph = (id: string, text: string, origin: "TEACHER" | "STUDENT" = "TEACHER"): DocumentBlock =>
  ({ id, origin, type: "PARAGRAPH", content: { runs: [{ text }] } });
const section = (id: string, holderId: string | null, extra: Partial<SectionView> = {}): SectionView => ({
  id, partId: id, title: `Mục ${id}`, orderNo: 1, status: holderId ? "CLAIMED" : "OPEN", holderId, assignedBy: null,
  publishedBlocks: null, draftBlocks: null, draftSavedAt: null, lastAuthorId: null, version: 2, ...extra,
});

function workspace(overrides: Partial<WorkspaceView> = {}): WorkspaceView {
  return {
    documentId: "doc1", assignmentId: "as1", assignmentTitle: "Sơ đồ lớp", closesAt: "2026-12-30T10:00:00Z", lateUntil: null,
    classId: "c1", groupId: "g1", groupName: "Nhóm 1", leaderId: OTHER,
    members: [{ accountId: OTHER, name: "An", leader: true }, { accountId: ME, name: "Bình", leader: false }],
    status: "IN_PROGRESS", window: "ON_TIME",
    sharedBlocks: { blocks: [
      heading("c1", "Giới thiệu"), paragraph("g0", "Đọc kỹ đề"),
      heading("s2", "Mục s2"), paragraph("g2", "Vẽ sơ đồ lớp"),
      heading("s3", "Mục s3"), paragraph("g3", "Hướng dẫn mục người khác"),
    ] },
    sections: [section("s2", ME, { draftBlocks: [paragraph("d1", "Bản nháp của tôi", "STUDENT")] }), section("s3", OTHER)],
    role: { member: true, leader: false, teacher: false }, lastSubmission: null, activityMonitoring: false, diagramEnabled: false,
    version: 5, viewerId: ME, ...overrides,
  };
}

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }));
}

function backend(views: { current: WorkspaceView }) {
  const calls: { url: string; method: string; body?: unknown }[] = [];
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";
    calls.push({ url, method, body: typeof init?.body === "string" ? JSON.parse(init.body) : undefined });
    if (url.endsWith("/draft")) return json(section("s2", ME, { version: 3, draftSavedAt: "2026-10-22T03:00:10Z" }));
    if (url.endsWith(":done")) {
      views.current = workspace({ sections: [section("s2", null, { status: "DONE", lastAuthorId: ME }), section("s3", OTHER)] });
      return json(section("s2", null, { status: "DONE" }));
    }
    return json(views.current);
  }));
  return calls;
}

afterEach(() => {
  act(() => publishOutline(null));
  vi.unstubAllGlobals();
  vi.useRealTimers();
  router.push.mockReset();
});

it("sidebar chỉ có phần chung và mục mình giữ; mục có ô Bài làm dưới hướng dẫn, phần chung không viết được", async () => {
  backend({ current: workspace() });
  const nav = groupDocContext({ pathname: "/group-docs/doc1/edit", search: new URLSearchParams() })!;
  render(<>{nav.header}<GroupSectionEditorPage documentId="doc1" /></>);

  const outline = await screen.findByTestId("assignment-outline");
  expect(within(outline).getByRole("button", { name: /Giới thiệu/ })).toBeInTheDocument();
  expect(within(outline).getByRole("button", { name: /Mục s2/ })).toBeInTheDocument();
  expect(within(outline).queryByRole("button", { name: /Mục s3/ })).not.toBeInTheDocument();
  expect(screen.getByRole("link", { name: /Tài liệu nhóm/ })).toHaveAttribute("href", "/group-docs/doc1");
  expect(screen.queryByText("Hướng dẫn mục người khác")).not.toBeInTheDocument();
  expect(screen.queryByText(/Hướng dẫn của giảng viên/)).not.toBeInTheDocument();
  // Mở ở chế độ Từng phần, trang đầu là phần chung: chỉ xem.
  expect(screen.getByText("Đọc kỹ đề")).toBeInTheDocument();
  expect(screen.queryByTestId("write-part-c1")).not.toBeInTheDocument();
  expect(screen.queryByTestId("answer-c1")).not.toBeInTheDocument();

  fireEvent.click(within(outline).getByRole("button", { name: /Mục s2/ }));
  expect(await screen.findByText("Vẽ sơ đồ lớp")).toBeInTheDocument();
  // Bài viết của mình nằm trong ô Bài làm như trang làm bài cá nhân, dưới hướng dẫn của giảng viên.
  const answer = screen.getByTestId("answer-s2");
  expect(answer).toHaveTextContent("Bài làm của bạn");
  expect(within(answer).getByText("Bản nháp của tôi")).toBeInTheDocument();
  expect(within(answer).queryByText("Vẽ sơ đồ lớp")).not.toBeInTheDocument();
  expect(within(screen.getByRole("group", { name: "Mục s2" })).getByRole("button", { name: "Xong" })).toBeInTheDocument();
});

it("tự lưu sau 10 giây chỉ block của người học, kèm version của mục", async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  const calls = backend({ current: workspace({ sections: [section("s2", ME), section("s3", OTHER)] }) });
  render(<GroupSectionEditorPage documentId="doc1" focusPart="s2" />);

  fireEvent.click(await screen.findByTestId("write-part-s2"));
  const field = screen.getByLabelText("Đoạn văn");
  field.textContent = "Đoạn một";
  fireEvent.input(field);
  expect(screen.queryByText(/Chưa lưu|Đã lưu/)).not.toBeInTheDocument();
  expect(calls.some((c) => c.url.endsWith("/draft"))).toBe(false);
  await act(async () => {
    await vi.advanceTimersByTimeAsync(10_000);
  });

  const saved = calls.find((c) => c.url.endsWith("/draft"))!;
  expect(saved.url).toBe("/api/v1/group-docs/doc1/sections/s2/draft");
  const body = saved.body as { version: number; blocks: { origin: string; content: unknown }[] };
  expect(body.version).toBe(2);
  expect(body.blocks).toHaveLength(1);
  expect(body.blocks[0]).toMatchObject({ origin: "STUDENT", content: { runs: [{ text: "Đoạn một" }] } });
});

it("còn phần chưa lưu thì trình duyệt hỏi lại khi rời trang", async () => {
  backend({ current: workspace({ sections: [section("s2", ME), section("s3", OTHER)] }) });
  render(<GroupSectionEditorPage documentId="doc1" focusPart="s2" />);
  const leave = () => {
    const event = new Event("beforeunload", { cancelable: true });
    window.dispatchEvent(event);
    return event.defaultPrevented;
  };

  const write = await screen.findByTestId("write-part-s2");
  expect(leave()).toBe(false);
  fireEvent.click(write);
  const field = screen.getByLabelText("Đoạn văn");
  field.textContent = "Đoạn chưa lưu";
  fireEvent.input(field);
  expect(leave()).toBe(true);
});

it("bấm Xong thì đăng mục; không còn mục nào để soạn thì về tài liệu nhóm", async () => {
  const calls = backend({ current: workspace() });
  render(<GroupSectionEditorPage documentId="doc1" focusPart="s2" />);

  fireEvent.click(within(await screen.findByRole("group", { name: "Mục s2" })).getByRole("button", { name: "Xong" }));

  await waitFor(() => expect(calls.some((c) => c.url === "/api/v1/group-docs/doc1/sections/s2:done")).toBe(true));
  await waitFor(() => expect(router.push).toHaveBeenCalledWith("/group-docs/doc1"));
});

it("Hoàn thành tất cả: đăng mọi mục đang giữ rồi về tài liệu nhóm", async () => {
  const calls = backend({ current: workspace({ sections: [section("s2", ME), section("s3", ME)] }) });
  render(<GroupSectionEditorPage documentId="doc1" />);

  fireEvent.click(await screen.findByRole("button", { name: "Hoàn thành tất cả" }));

  await waitFor(() => expect(router.push).toHaveBeenCalledWith("/group-docs/doc1"));
  expect(calls.filter((c) => c.url.endsWith(":done")).map((c) => c.url)).toEqual([
    "/api/v1/group-docs/doc1/sections/s2:done", "/api/v1/group-docs/doc1/sections/s3:done",
  ]);
});

it("số thứ tự của mục giữ như trang tài liệu nhóm, dù mục của người khác không hiện", async () => {
  backend({ current: workspace({ sections: [section("s2", OTHER), section("s3", ME)] }) });
  const nav = groupDocContext({ pathname: "/group-docs/doc1/edit", search: new URLSearchParams() })!;
  render(<>{nav.header}<GroupSectionEditorPage documentId="doc1" focusPart="s3" /></>);

  const outline = await screen.findByTestId("assignment-outline");
  expect(within(outline).getByRole("button", { name: /^III\.\s*III\. Mục s3$/ })).toBeInTheDocument();
  expect(await screen.findByTestId("number-s3")).toHaveTextContent("III.");
});

it("mục bị giao cho người khác thì rời trang soạn và báo", async () => {
  let source: { onmessage: ((m: { data: string }) => void) | null } | null = null;
  vi.stubGlobal("EventSource", class {
    onmessage: ((m: { data: string }) => void) | null = null;
    onerror = null;
    onopen = null;
    constructor() {
      // eslint-disable-next-line @typescript-eslint/no-this-alias
      source = this;
    }
    close() {}
  });
  const views = { current: workspace() };
  backend(views);
  render(<GroupSectionEditorPage documentId="doc1" focusPart="s2" />);
  await screen.findByRole("group", { name: "Mục s2" });
  await waitFor(() => expect(source?.onmessage).toBeTypeOf("function"));

  views.current = workspace({ sections: [section("s2", OTHER, { version: 4 }), section("s3", OTHER)] });
  act(() => source!.onmessage!({ data: JSON.stringify({ type: "section.assigned", sectionId: "s2" }) }));

  expect(await screen.findByText(/Bạn không còn giữ mục “Mục s2”/)).toBeInTheDocument();
  expect(screen.getByText("Bạn chưa giữ mục nào để soạn.")).toBeInTheDocument();
});
