import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { GroupWorkspacePage } from "@/features/groupdocs/pages/GroupWorkspacePage";
import { SectionEditorDialog, partOf } from "@/features/groupdocs/components/SectionDialogs";
import type { SectionView, WorkspaceView } from "@/features/groupdocs/model/types";

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
      calls.push({ url, method, body: typeof init?.body === "string" ? JSON.parse(init.body) : init?.body });
      if (url === "/api/v1/group-docs/doc1" && method === "GET") return json(view);
      if (url.endsWith(":claim")) {
        const id = url.match(/sections\/([^/:]+):claim/)![1]!;
        // Như server: nhận mục tăng version.
        const version = (view.sections.find((s) => s.id === id)?.version ?? 1) + 1;
        return json(section(id, "CLAIMED", view.viewerId, { version }));
      }
      if (url.endsWith(":release")) return json(section("s2", "OPEN", null));
      if (url.endsWith(":assign")) return json(section("s3", "CLAIMED", MEMBER));
      if (url.endsWith("/revisions"))
        return json([
          { id: "r2", authorId: MEMBER, kind: "DONE", createdAt: "2026-10-22T04:00:00Z", blocks: [{ id: "x2", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Bản nháp cũ" }] } }] },
          { id: "r1", authorId: LEADER, kind: "DONE", createdAt: "2026-10-22T03:00:00Z", blocks: [] },
        ]);
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

  it("thành viên thấy nội dung đã Xong, nhận mục Mở là soạn ngay, không có nút nộp", async () => {
    const calls = backend(workspace(MEMBER));
    render(<GroupWorkspacePage documentId="doc1" />);

    expect(await screen.findByText("Nội dung đã xong")).toBeInTheDocument();
    expect(screen.getByText("Trưởng nhóm nộp bài")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Nộp tài liệu/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Chốt phân công|Nhập từ file/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("tab", { name: "Đánh giá chéo" })).not.toBeInTheDocument();
    const item = (title: string) => screen.getAllByRole("listitem").find((li) => within(li).queryByText(title))!;
    expect(within(item("Mục s1")).getByText("Viết bởi: An")).toBeInTheDocument();
    expect(within(item("Mục s2")).getByText("Đang soạn: Bạn")).toBeInTheDocument();
    expect(within(item("Mục s2")).getByRole("button", { name: "Soạn tiếp" })).toBeInTheDocument();

    fireEvent.click(within(item("Mục s3")).getByRole("button", { name: "Nhận mục" }));
    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s3:claim"))).toBe(true));
    expect(await screen.findByText("Bạn đang giữ")).toBeInTheDocument();
    // Trình soạn toàn trang gắn thẳng vào body, không bị thanh trên và sidebar của khung trang che.
    expect(screen.getByRole("dialog", { name: /Soạn Mục s3/ }).parentElement).toBe(document.body);
  });

  it("mục đã Xong vẫn nhận lại để sửa (BR-U12-33); mục người khác viết thì hỏi lại trước", async () => {
    const calls = backend(workspace(MEMBER));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = (await screen.findAllByRole("listitem")).find((li) => within(li).queryByText("Mục s1"))!;

    fireEvent.click(within(item).getByRole("button", { name: "Sửa mục" }));
    const confirm = await screen.findByRole("dialog", { name: /Sửa mục Mục s1/ });
    expect(confirm).toHaveTextContent("Mục này do An viết");
    expect(calls.some((c) => c.url.endsWith(":claim"))).toBe(false);

    // Quay lại thì không nhận mục.
    fireEvent.click(within(confirm).getByRole("button", { name: "Quay lại" }));
    await waitFor(() => expect(screen.queryByRole("dialog", { name: /Sửa mục Mục s1/ })).not.toBeInTheDocument());
    expect(calls.some((c) => c.url.endsWith(":claim"))).toBe(false);

    // Bấm vào tên mục cũng hỏi lại; xác nhận thì nhận và mở trình soạn.
    fireEvent.click(within(item).getByRole("button", { name: /Mục s1 · Xong/ }));
    fireEvent.click(within(await screen.findByRole("dialog", { name: /Sửa mục Mục s1/ })).getByRole("button", { name: "Sửa mục" }));

    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s1:claim"))).toBe(true));
    expect(await screen.findByRole("dialog", { name: /Soạn Mục s1/ })).toBeInTheDocument();
  });

  it("mục người khác đang soạn: thành viên chỉ xem; trưởng nhóm nhả mục sau khi xác nhận", async () => {
    const view = workspace(LEADER, { sections: [section("s2", "CLAIMED", MEMBER)] });
    const calls = backend(view);
    render(<GroupWorkspacePage documentId="doc1" />);

    fireEvent.click(await screen.findByRole("button", { name: /Mục s2 · Đang làm · Xem nội dung/ }));
    expect(calls.some((c) => c.url.endsWith(":claim"))).toBe(false);
    fireEvent.click(screen.getByRole("button", { name: "Nhả mục" }));
    const dialog = await screen.findByRole("dialog", { name: /Nhả mục Mục s2/ });
    expect(within(dialog).getByText(/Bản nháp chưa Xong sẽ bị bỏ/)).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole("button", { name: "Nhả mục" }));
    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s2:release"))).toBe(true));
  });

  it("trưởng nhóm giao mục: người được giao soạn ngay; mục đang có người giữ thì cảnh báo nhả trước", async () => {
    const calls = backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = (title: string) => screen.getAllByRole("listitem").find((li) => within(li).queryByText(title))!;
    await screen.findByText("Nội dung đã xong");

    fireEvent.click(within(item("Mục s2")).getByRole("button", { name: "Giao mục" }));
    const held = await screen.findByRole("dialog", { name: /Giao mục Mục s2/ });
    expect(within(held).getByText(/Bình đang giữ mục này/)).toBeInTheDocument();
    fireEvent.click(within(held).getByRole("button", { name: "Đóng" }));

    fireEvent.click(within(item("Mục s3")).getByRole("button", { name: "Giao mục" }));
    const dialog = await screen.findByRole("dialog", { name: /Giao mục Mục s3/ });
    fireEvent.change(within(dialog).getByLabelText("Thành viên nhận mục"), { target: { value: MEMBER } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Giao mục" }));
    await waitFor(() => expect(calls.find((c) => c.url.endsWith("/sections/s3:assign"))?.body).toEqual({ assigneeId: MEMBER, replaceHolder: false }));
  });

  it("lịch sử mục chỉ xem: hiện từng bản với tác giả, không có khôi phục", async () => {
    backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = (await screen.findAllByRole("listitem")).find((li) => within(li).queryByText("Mục s2"))!;

    fireEvent.click(within(item).getByRole("button", { name: "Lịch sử" }));

    const dialog = await screen.findByRole("dialog", { name: /Lịch sử · Mục s2/ });
    expect(await within(dialog).findByText(/Xong · Bình/)).toBeInTheDocument();
    expect(within(dialog).getByText("Bản nháp cũ")).toBeInTheDocument();
    expect(within(dialog).queryByRole("button", { name: /Dùng bản này/ })).not.toBeInTheDocument();
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
    // Kênh sự kiện mở trong effect riêng, có thể sau khi nội dung đã hiện: chờ kênh sẵn sàng rồi mới bắn sự kiện.
    await waitFor(() => expect(source?.onmessage).toBeTypeOf("function"));

    act(() => source!.onmessage!({ data: JSON.stringify({ type: "document.auto-submitted" }) }));
    const lock = await screen.findByRole("alertdialog", { name: "Đang nộp" });
    expect(lock).toHaveTextContent(/Đã hết hạn nộp lúc 30\/10\/2026 17:00/);
    expect(lock.parentElement).toBe(document.body);
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

  it("thẻ Bản nộp hiện tác giả lúc nộp, không đổi khi mục được sửa sau đó (BR-U12-44)", async () => {
    backend(workspace(MEMBER, {
      lastSubmission: {
        submittedBy: LEADER,
        mode: "MANUAL",
        submittedAt: "2026-10-22T03:00:00Z",
        late: false,
        receiptHash: "sha256:abc",
        authors: [
          { partId: "s1", title: "Mục s1", accountId: MEMBER, done: true },
          { partId: "s3", title: "Mục s3", accountId: null, done: false },
        ],
      },
    }));
    render(<GroupWorkspacePage documentId="doc1" />);
    fireEvent.click(await screen.findByRole("tab", { name: "Bản nộp" }));

    // Mục s1 hiện do An viết lại sau khi nộp, nhưng bản nộp vẫn của Bình.
    const row = (title: string) => screen.getAllByRole("row").find((r) => within(r).queryByText(title))!;
    expect(within(row("Mục s1")).getByText("Bình")).toBeInTheDocument();
    expect(within(row("Mục s1")).getByText("Xong")).toBeInTheDocument();
    expect(within(row("Mục s3")).getByText("Chưa Xong, không vào bản nộp")).toBeInTheDocument();
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

    // Bấm lần nữa thì thoát, không bị kẹt trong trình soạn toàn trang.
    fireEvent.click(screen.getByRole("button", { name: "Đóng, bỏ phần chưa lưu" }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("đang soạn mà mục bị giao cho người khác hoặc tài liệu bị khóa thì đóng trình soạn và báo", async () => {
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
    let view = workspace(MEMBER);
    vi.stubGlobal("fetch", vi.fn(() => json(view)));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = (await screen.findAllByRole("listitem")).find((li) => within(li).queryByText("Mục s2"))!;
    await waitFor(() => expect(source?.onmessage).toBeTypeOf("function"));

    fireEvent.click(within(item).getByRole("button", { name: "Soạn tiếp" }));
    expect(screen.getByRole("dialog", { name: /Soạn Mục s2/ })).toBeInTheDocument();

    // Trưởng nhóm giao lại mục s2 cho người khác: version tăng, người giữ đổi.
    view = workspace(MEMBER, { sections: [section("s2", "CLAIMED", LEADER, { version: 4 })] });
    act(() => source!.onmessage!({ data: JSON.stringify({ type: "section.assigned", sectionId: "s2" }) }));
    await waitFor(() => expect(screen.queryByRole("dialog", { name: /Soạn Mục s2/ })).not.toBeInTheDocument());
    expect(screen.getByRole("alert")).toHaveTextContent("Bạn không còn giữ mục “Mục s2”");

    // Hết hạn, hệ thống tự nộp: trình soạn của mục đang giữ cũng đóng.
    view = workspace(MEMBER, { sections: [section("s3", "CLAIMED", MEMBER, { version: 3 })] });
    act(() => source!.onmessage!({ data: JSON.stringify({ type: "section.claimed", sectionId: "s3" }) }));
    fireEvent.click(await screen.findByRole("button", { name: "Soạn tiếp" }));
    expect(screen.getByRole("dialog", { name: /Soạn Mục s3/ })).toBeInTheDocument();
    view = workspace(MEMBER, { status: "CLOSED", window: "CLOSED", sections: [section("s3", "DONE", null, { version: 4 })] });
    act(() => source!.onmessage!({ data: JSON.stringify({ type: "document.auto-submitted" }) }));
    await waitFor(() => expect(screen.queryByRole("dialog", { name: /Soạn Mục s3/ })).not.toBeInTheDocument());
    expect(screen.getByRole("alert")).toHaveTextContent("Đã hết hạn nộp nên trình soạn mục “Mục s3” đã đóng.");
  });

  it("vừa nhận mục mà bản xem chưa tải lại thì trình soạn vẫn mở", async () => {
    // Nhận mục tăng version; GET vẫn trả bản xem cũ (mục Mở) như lần tải lại chưa kịp về.
    const calls = backend(workspace(MEMBER));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = (await screen.findAllByRole("listitem")).find((li) => within(li).queryByText("Mục s3"))!;

    fireEvent.click(within(item).getByRole("button", { name: "Nhận mục" }));

    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s3:claim"))).toBe(true));
    expect(await screen.findByRole("dialog", { name: /Soạn Mục s3/ })).toBeInTheDocument();
    expect(screen.queryByText(/Bạn không còn giữ mục/)).not.toBeInTheDocument();
  });

  it("mục Xong do chính mình viết thì nhận lại ngay, không hỏi", async () => {
    const calls = backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = (await screen.findAllByRole("listitem")).find((li) => within(li).queryByText("Mục s1"))!;

    fireEvent.click(within(item).getByRole("button", { name: "Sửa mục" }));

    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s1:claim"))).toBe(true));
    expect(screen.queryByRole("dialog", { name: /Sửa mục Mục s1/ })).not.toBeInTheDocument();
  });

  it("mục người khác đang giữ hiện lúc lưu nháp cuối hoặc lúc nhận, không lộ bản nháp", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true, now: new Date("2026-10-30T08:00:00Z") });
    backend(
      workspace(LEADER, {
        sections: [
          section("s2", "CLAIMED", MEMBER, { claimedAt: "2026-10-30T07:30:00Z", holderSavedAt: "2026-10-30T07:57:00Z" }),
          section("s3", "CLAIMED", MEMBER, { claimedAt: "2026-10-30T06:00:00Z", holderSavedAt: null }),
        ],
      }),
    );
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = (title: string) => screen.getAllByRole("listitem").find((li) => within(li).queryByText(title))!;

    expect(await screen.findByText("Đang soạn: Bình · lưu lần cuối 3 phút trước")).toBeInTheDocument();
    expect(within(item("Mục s3")).getByText("Đang soạn: Bình · nhận 2 giờ trước, chưa lưu")).toBeInTheDocument();
  });

  it("có mục người khác đang giữ thì tải lại mỗi phút để cập nhật lần lưu cuối", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true, now: new Date("2026-10-30T08:00:00Z") });
    const calls = backend(workspace(MEMBER, { sections: [section("s2", "CLAIMED", LEADER)] }));
    render(<GroupWorkspacePage documentId="doc1" />);
    await screen.findByText(/Đang soạn: An/);
    const loads = () => calls.filter((c) => c.url === "/api/v1/group-docs/doc1" && c.method === "GET").length;
    const before = loads();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(60_000);
    });

    expect(loads()).toBe(before + 1);
  });

  it("còn ≤ 30 phút thì nhắc hạn kèm mục chưa Xong; xa hạn thì không nhắc", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true, now: new Date("2026-10-30T09:42:00Z") });
    backend(workspace(MEMBER, { closesAt: "2026-10-30T10:00:00Z" }));
    const { unmount } = render(<GroupWorkspacePage documentId="doc1" />);

    expect(await screen.findByText(/Còn 18 phút tới hạn nộp/)).toBeInTheDocument();
    expect(screen.getByText("1 mục chưa ai làm, sẽ không vào bản nộp.")).toBeInTheDocument();
    expect(screen.getByText(/1 mục đang soạn: hãy bấm Xong/)).toBeInTheDocument();
    expect(screen.getByText(/Nhóm chưa nộp bản nào/)).toBeInTheDocument();
    unmount();

    backend(workspace(MEMBER, { closesAt: "2026-10-30T12:00:00Z" }));
    render(<GroupWorkspacePage documentId="doc1" />);
    await screen.findByText("Nội dung đã xong");
    expect(screen.queryByText(/tới hạn nộp/)).not.toBeInTheDocument();
  });

  it("trong khoảng nộp trễ thì báo bị tính trễ và còn bao lâu tới khi đóng nhận bài", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true, now: new Date("2026-10-30T10:20:00Z") });
    backend(
      workspace(MEMBER, {
        window: "LATE",
        lateUntil: "2026-10-30T11:00:00Z",
        sections: [section("s1", "DONE", null, { lastAuthorId: LEADER })],
      }),
    );
    render(<GroupWorkspacePage documentId="doc1" />);

    expect(await screen.findByText(/nộp bây giờ bị tính trễ\. Còn 40 phút tới khi đóng nhận bài/)).toBeInTheDocument();
    expect(screen.getByText("Mọi mục đã Xong.")).toBeInTheDocument();
  });

  it("trình soạn hỏi lại khi rời trang lúc còn phần chưa lưu và hiện nhắc hạn", () => {
    vi.stubGlobal("fetch", vi.fn(() => json({})));
    const shared = { blocks: [{ id: "s2", origin: "TEACHER" as const, type: "HEADING" as const, content: { style: "H1" as const, text: "Mục s2" } }] };
    render(
      <SectionEditorDialog
        documentId="doc1"
        section={section("s2", "CLAIMED", MEMBER)}
        shared={shared}
        diagramEnabled={false}
        deadlineNotice="Còn 5 phút tới hạn nộp (10:00)."
        onClose={() => undefined}
        onChanged={() => undefined}
      />,
    );
    expect(screen.getByText(/Còn 5 phút tới hạn nộp \(10:00\)\. Nên bấm Xong trước hạn\./)).toBeInTheDocument();

    const leave = () => {
      const event = new Event("beforeunload", { cancelable: true });
      window.dispatchEvent(event);
      return event.defaultPrevented;
    };
    expect(leave()).toBe(false);
    fireEvent.click(screen.getByTestId("write-part-s2"));
    const field = screen.getByLabelText("Đoạn văn");
    field.textContent = "Đoạn chưa lưu";
    fireEvent.input(field);
    expect(leave()).toBe(true);
  });

  it("partOf lấy heading và hướng dẫn của phần; không có heading thì dựng theo tên mục", () => {
    const shared = { blocks: [{ id: "h", origin: "TEACHER" as const, type: "HEADING" as const, content: { style: "H1" as const, text: "A" } }] };
    expect(partOf(shared, { partId: "h", title: "A" })).toMatchObject({ heading: { id: "h" }, guide: [] });
    expect(partOf(shared, { partId: "x", title: "Toàn bài" }).heading).toMatchObject({ id: "x", content: { text: "Toàn bài" } });
  });
});
