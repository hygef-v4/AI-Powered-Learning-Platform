import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { GroupWorkspacePage } from "./GroupWorkspacePage";
import { SectionEditorDialog, partOf } from "./SectionDialogs";
import type { SectionView, WorkspaceView } from "./types";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

const LEADER = "leader";
const MEMBER = "member";

function section(id: string, status: SectionView["status"], holderId: string | null, extra: Partial<SectionView> = {}): SectionView {
  return {
    id,
    partId: id,
    title: `Mục ${id}`,
    orderNo: 1,
    status,
    holderId,
    assignedBy: null,
    publishedBlocks: null,
    draftBlocks: null,
    draftSavedAt: null,
    lastAuthorId: null,
    version: 2,
    ...extra,
  };
}

function workspace(viewerId: string, overrides: Partial<WorkspaceView> = {}): WorkspaceView {
  return {
    documentId: "doc1",
    assignmentId: "as1",
    assignmentTitle: "Sơ đồ lớp",
    closesAt: "2026-10-30T10:00:00Z",
    lateUntil: null,
    classId: "c1",
    groupId: "g1",
    groupName: "Nhóm 1",
    leaderId: LEADER,
    members: [
      { accountId: LEADER, name: "An", leader: true },
      { accountId: MEMBER, name: "Bình", leader: false },
    ],
    status: "IN_PROGRESS",
    window: "ON_TIME",
    sharedBlocks: { blocks: [{ id: "h1", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "1. Sơ đồ" } }] },
    sections: [
      section("s1", "DONE", null, {
        lastAuthorId: LEADER,
        publishedBlocks: [{ id: "b1", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Nội dung đã xong" }] } }],
      }),
      section("s2", "CLAIMED", MEMBER),
      section("s3", "OPEN", null),
    ],
    role: { member: true, leader: viewerId === LEADER, teacher: false },
    lastSubmission: null,
    activityMonitoring: false,
    diagramEnabled: false,
    version: 5,
    viewerId,
    ...overrides,
  };
}

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }));
}

function backend(view: WorkspaceView) {
  const calls: { url: string; method: string; body?: unknown }[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = init?.method ?? "GET";
      calls.push({ url, method, body: init?.body ? JSON.parse(String(init.body)) : undefined });
      if (url === "/api/v1/group-docs/doc1" && method === "GET") return json(view);
      if (url.endsWith(":claim")) return json(section("s3", "CLAIMED", view.viewerId));
      if (url.endsWith("/submit"))
        return json({ documentId: "doc1", mode: "MANUAL", submittedAt: "2026-10-22T03:00:00Z", late: false, receiptHash: "sha256:abc", submittedBy: LEADER, warnings: [] });
      return json({ status: 404 }, 404);
    }),
  );
  return calls;
}

describe("GroupWorkspacePage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it("thành viên thấy nội dung đã Xong, nhận mục mở, không có nút nộp", async () => {
    const calls = backend(workspace(MEMBER));
    render(<GroupWorkspacePage documentId="doc1" />);

    expect(await screen.findByText("Nội dung đã xong")).toBeInTheDocument();
    expect(screen.getByText("Trưởng nhóm nộp bài")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Nộp tài liệu/ })).not.toBeInTheDocument();
    const item = (title: string) => screen.getAllByRole("listitem").find((li) => within(li).queryByText(title))!;
    const mine = item("Mục s2");
    expect(within(mine).getByText("Đang giữ: Bạn")).toBeInTheDocument();
    expect(within(mine).getByRole("button", { name: "Làm" })).toBeInTheDocument();

    fireEvent.click(within(item("Mục s3")).getByRole("button", { name: "Nhận mục" }));
    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s3:claim"))).toBe(true));
    expect(await screen.findByText("Bạn đang giữ")).toBeInTheDocument();
  });

  it("bấm vào mục Mở thì nhận và mở trình soạn; mục người khác giữ chỉ xem", async () => {
    const calls = backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);

    fireEvent.click(await screen.findByRole("button", { name: /Mục s2 · Đang làm · Chỉ xem/ }));
    expect(screen.getByText("Đang giữ: Bình")).toBeInTheDocument();
    expect(calls.some((c) => c.url.endsWith(":claim"))).toBe(false);
    expect(screen.queryByText("Bạn đang giữ")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /Mục s3 · Mở · Bấm để nhận và soạn/ }));
    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s3:claim"))).toBe(true));
    expect(await screen.findByText("Bạn đang giữ")).toBeInTheDocument();
  });

  it("giảng viên bấm mục không nhận được", async () => {
    const calls = backend(workspace("teacher", { role: { member: false, leader: false, teacher: true } }));
    render(<GroupWorkspacePage documentId="doc1" />);

    fireEvent.click(await screen.findByRole("button", { name: /Mục s3 · Mở · Xem nội dung/ }));
    expect(calls.some((c) => c.url.endsWith(":claim"))).toBe(false);
    expect(screen.queryByRole("button", { name: "Nhận mục" })).not.toBeInTheDocument();
  });

  it("tự nộp khi hết hạn khóa trang và ghi giờ hết hạn", async () => {
    let source: { onmessage: ((m: { data: string }) => void) | null } | null = null;
    vi.stubGlobal(
      "EventSource",
      class {
        onmessage: ((m: { data: string }) => void) | null = null;
        onerror = null;
        onopen = null;
        constructor() {
          // eslint-disable-next-line @typescript-eslint/no-this-alias
          source = this;
        }
        close() {}
      },
    );
    let resolveReload: (() => void) | null = null;
    let first = true;
    const view = workspace(MEMBER);
    vi.stubGlobal(
      "fetch",
      vi.fn(() => {
        if (first) {
          first = false;
          return json(view);
        }
        return new Promise<Response>((resolve) => {
          resolveReload = () => resolve(new Response(JSON.stringify(view), { status: 200, headers: { "Content-Type": "application/json" } }));
        });
      }),
    );
    render(<GroupWorkspacePage documentId="doc1" />);
    await screen.findByText("Nội dung đã xong");

    act(() => source!.onmessage!({ data: JSON.stringify({ type: "document.auto-submitted" }) }));
    const lock = await screen.findByRole("alertdialog", { name: "Đang nộp" });
    expect(lock).toHaveTextContent(/Đã hết hạn nộp lúc 30\/10\/2026 17:00/);
    await act(async () => resolveReload!());
    await waitFor(() => expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument());
  });

  it("trưởng nhóm nộp được, popup cảnh báo mục chưa Xong và gửi version", async () => {
    const calls = backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);
    fireEvent.click(await screen.findByRole("button", { name: /Nộp tài liệu/ }));

    const dialog = await screen.findByRole("dialog");
    expect(within(dialog).getByText("Chỉ nội dung của mục Xong được đưa vào bản nộp.")).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole("button", { name: "Vẫn nộp" }));

    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/submit"))).toBe(true));
    expect(calls.find((c) => c.url.endsWith("/submit"))!.body).toEqual({ version: 5 });
  });

  it("thẻ Bản nộp hiện biên nhận và cách nộp", async () => {
    backend(workspace(MEMBER, {
      lastSubmission: { submittedBy: null, mode: "AUTO_DEADLINE", submittedAt: "2026-10-22T03:00:00Z", late: true, receiptHash: "sha256:xyz" },
      status: "CLOSED",
      window: "CLOSED",
    }));
    render(<GroupWorkspacePage documentId="doc1" />);
    fireEvent.click(await screen.findByRole("tab", { name: "Bản nộp" }));

    expect(screen.getByText(/Tự nộp khi hết hạn/)).toBeInTheDocument();
    expect(screen.getByText("Mã biên nhận: sha256:xyz")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Nhận mục" })).not.toBeInTheDocument();
  });

  it("trình soạn dùng DocumentEditor: chỉ lưu block của người học, tự lưu sau 10 giây kèm version", async () => {
    vi.useFakeTimers();
    const calls: { url: string; body: unknown }[] = [];
    vi.stubGlobal(
      "fetch",
      vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
        calls.push({ url: String(input), body: init?.body ? JSON.parse(String(init.body)) : undefined });
        return json(section("s2", "CLAIMED", MEMBER, { version: 3, draftBlocks: [], draftSavedAt: "2026-10-22T03:00:10Z" }));
      }),
    );
    const shared = {
      blocks: [
        { id: "s1", origin: "TEACHER" as const, type: "HEADING" as const, content: { style: "H1" as const, text: "Mục s1" } },
        { id: "s2", origin: "TEACHER" as const, type: "HEADING" as const, content: { style: "H1" as const, text: "Mục s2" } },
        { id: "g1", origin: "TEACHER" as const, type: "PARAGRAPH" as const, content: { runs: [{ text: "Vẽ sơ đồ lớp" }] } },
        { id: "s3", origin: "TEACHER" as const, type: "HEADING" as const, content: { style: "H1" as const, text: "Mục s3" } },
      ],
    };
    render(
      <SectionEditorDialog
        documentId="doc1"
        section={section("s2", "CLAIMED", MEMBER)}
        shared={shared}
        diagramEnabled
        onClose={() => undefined}
        onChanged={() => undefined}
      />,
    );

    expect(screen.getByText("Hướng dẫn của giảng viên cho mục này")).toBeInTheDocument();
    expect(screen.getByText("Vẽ sơ đồ lớp")).toBeInTheDocument();
    expect(screen.queryByText("Mục s3")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("write-part-s2"));
    const field = screen.getByLabelText("Đoạn văn");
    field.textContent = "Đoạn một";
    fireEvent.input(field);
    expect(calls).toHaveLength(0);
    await act(async () => {
      await vi.advanceTimersByTimeAsync(10_000);
    });

    expect(calls).toHaveLength(1);
    expect(calls[0]!.url).toBe("/api/v1/group-docs/doc1/sections/s2/draft");
    const body = calls[0]!.body as { version: number; blocks: { origin: string; type: string; content: unknown }[] };
    expect(body.version).toBe(2);
    expect(body.blocks).toHaveLength(1);
    expect(body.blocks[0]).toMatchObject({ origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Đoạn một" }] } });
  });

  it("bấm Đóng khi lưu nháp lỗi thì không đóng trình soạn, giữ nội dung và báo lỗi", async () => {
    vi.stubGlobal("fetch", vi.fn(() => json({ status: 409, detail: "Mục vừa được thay đổi ở nơi khác, hãy tải lại" }, 409)));
    const onClose = vi.fn();
    const shared = { blocks: [{ id: "s2", origin: "TEACHER" as const, type: "HEADING" as const, content: { style: "H1" as const, text: "Mục s2" } }] };
    render(<SectionEditorDialog documentId="doc1" section={section("s2", "CLAIMED", MEMBER)} shared={shared} diagramEnabled={false} onClose={onClose} onChanged={() => undefined} />);

    fireEvent.click(screen.getByTestId("write-part-s2"));
    const field = screen.getByLabelText("Đoạn văn");
    field.textContent = "Đoạn chưa lưu";
    fireEvent.input(field);
    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Mục vừa được thay đổi ở nơi khác");
    expect(onClose).not.toHaveBeenCalled();
    expect(screen.getByText("Đoạn chưa lưu")).toBeInTheDocument();
  });

  it("partOf lấy heading và hướng dẫn của phần; không có heading thì dựng theo tên mục", () => {
    const shared = { blocks: [{ id: "h", origin: "TEACHER" as const, type: "HEADING" as const, content: { style: "H1" as const, text: "A" } }] };
    expect(partOf(shared, { partId: "h", title: "A" })).toMatchObject({ heading: { id: "h" }, guide: [] });
    expect(partOf(shared, { partId: "x", title: "Toàn bài" }).heading).toMatchObject({ id: "x", content: { text: "Toàn bài" } });
  });
});
