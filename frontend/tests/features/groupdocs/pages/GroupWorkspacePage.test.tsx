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
    assigneeId: holderId,
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
        assigneeId: LEADER,
        publishedBlocks: [{ id: "b1", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Nội dung đã xong" }] } }],
      }),
      section("s2", "CLAIMED", MEMBER),
      section("s3", "OPEN", null, { assigneeId: viewerId }),
    ],
    role: { member: true, leader: viewerId === LEADER, teacher: false },
    lastSubmission: null,
    activityMonitoring: false,
    diagramEnabled: false,
    version: 5,
    viewerId,
    assignmentLockedBy: LEADER,
    assignmentLockedAt: "2026-10-21T03:00:00Z",
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
      if (url.endsWith(":claim")) return json(section("s3", "CLAIMED", view.viewerId));
      if (url.endsWith("/assignment:lock")) return json({ lockedBy: view.viewerId, lockedAt: "2026-10-22T03:00:00Z" });
      if (url.endsWith("/assignment:unlock")) return Promise.resolve(new Response(null, { status: 204 }));
      if (url.endsWith("/revisions"))
        return json([
          { id: "r2", authorId: MEMBER, kind: "IMPORT", createdAt: "2026-10-22T04:00:00Z", blocks: [{ id: "x2", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Bản nháp cũ" }] } }] },
          { id: "r1", authorId: LEADER, kind: "DONE", createdAt: "2026-10-22T03:00:00Z", blocks: [] },
        ]);
      if (url.endsWith("/peer-reviews") && method === "GET")
        return json(view.role.teacher
          ? { open: true, closedReason: null, closesAt: "2026-11-06T10:00:00Z", mine: [], reviewers: 1, members: 2,
              summary: [{ accountId: LEADER, average: 4.5, outOfTen: 9, count: 2, reviews: [{ reviewerId: MEMBER, score: 5, comment: "Gánh team", updatedAt: "2026-10-22T03:00:00Z" }] },
                        { accountId: MEMBER, average: null, outOfTen: null, count: 0, reviews: [] }] }
          : { open: true, closedReason: null, closesAt: "2026-11-06T10:00:00Z", mine: [], summary: [], reviewers: 0, members: 2 });
      if (url.endsWith("/peer-reviews") && method === "PUT")
        return json({ open: true, closedReason: null, closesAt: null, mine: [{ revieweeId: LEADER, score: 4, comment: "Ổn" }], summary: [], reviewers: 0, members: 2 });
      if (url.endsWith("/import:preview"))
        return json({ sections: [
          { sectionId: "s3", title: "Mục s3", version: 2, blocks: [{ id: "n1", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Từ Word" }] } }], skipReason: null, replaces: false },
          { sectionId: "s2", title: "Mục s2", version: 2, blocks: [], skipReason: "Mục đang có người giữ", replaces: false },
        ], unmatched: ["Tiêu đề “Phụ lục”"], warnings: [] });
      if (url.endsWith("/import:apply")) return json({ applied: 1, skipped: [] });
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

  it("đã chốt phân công: thành viên thấy nội dung đã Xong, soạn mục mình phụ trách, không có nút nộp", async () => {
    const calls = backend(workspace(MEMBER));
    render(<GroupWorkspacePage documentId="doc1" />);

    expect(await screen.findByText("Nội dung đã xong")).toBeInTheDocument();
    expect(screen.getByText("Trưởng nhóm nộp bài")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Nộp tài liệu/ })).not.toBeInTheDocument();
    const item = (title: string) => screen.getAllByRole("listitem").find((li) => within(li).queryByText(title))!;
    const mine = item("Mục s2");
    expect(within(mine).getByText("Đang soạn: Bạn")).toBeInTheDocument();
    expect(within(mine).getByRole("button", { name: "Làm" })).toBeInTheDocument();

    fireEvent.click(within(item("Mục s3")).getByRole("button", { name: "Làm" }));
    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s3:claim"))).toBe(true));
    expect(await screen.findByText("Bạn đang giữ")).toBeInTheDocument();
    // Trình soạn toàn trang gắn thẳng vào body, không bị thanh trên và sidebar của khung trang che.
    expect(screen.getByRole("dialog", { name: /Soạn Mục s3/ }).parentElement).toBe(document.body);
  });

  it("đã chốt phân công: bấm vào mục mình phụ trách thì mở trình soạn", async () => {
    const calls = backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);

    fireEvent.click(await screen.findByRole("button", { name: /Mục s3 · Mở · Bấm để soạn/ }));
    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s3:claim"))).toBe(true));
    expect(await screen.findByText("Bạn đang giữ")).toBeInTheDocument();
  });

  it("mục người khác phụ trách chỉ xem, không có tiếp quản (kể cả trưởng nhóm)", async () => {
    const view = workspace(LEADER, { sections: [section("s2", "CLAIMED", MEMBER)] });
    const calls = backend(view);
    render(<GroupWorkspacePage documentId="doc1" />);

    fireEvent.click(await screen.findByRole("button", { name: /Mục s2 · Đang làm · Xem nội dung/ }));
    expect(screen.queryByRole("button", { name: "Tiếp quản" })).not.toBeInTheDocument();
    expect(screen.queryByRole("dialog", { name: /Tiếp quản/ })).not.toBeInTheDocument();
    expect(calls.some((c) => c.url.endsWith(":take-over"))).toBe(false);
  });

  it("đang phân công: thành viên chỉ nhận mục, chưa mở trình soạn; trưởng nhóm chưa chốt được khi còn mục trống", async () => {
    const planning = { assignmentLockedBy: null, assignmentLockedAt: null };
    const view = workspace(MEMBER, { ...planning, sections: [section("s1", "OPEN", null), section("s2", "OPEN", null, { assigneeId: LEADER })] });
    const calls = backend(view);
    const { unmount } = render(<GroupWorkspacePage documentId="doc1" />);

    expect(await screen.findByText("Đang phân công")).toBeInTheDocument();
    expect(screen.getByText(/0\/2 mục|1\/2 mục đã có người phụ trách/)).toBeInTheDocument();
    const item = (title: string) => screen.getAllByRole("listitem").find((li) => within(li).queryByText(title))!;
    expect(within(item("Mục s2")).getByText("Phụ trách: An")).toBeInTheDocument();
    expect(within(item("Mục s2")).queryByRole("button", { name: "Nhận mục" })).not.toBeInTheDocument();
    fireEvent.click(within(item("Mục s1")).getByRole("button", { name: "Nhận mục" }));
    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s1:claim"))).toBe(true));
    expect(screen.queryByText("Bạn đang giữ")).not.toBeInTheDocument();
    unmount();

    backend(workspace(LEADER, { ...planning, sections: [section("s1", "OPEN", null), section("s2", "OPEN", null, { assigneeId: LEADER })] }));
    render(<GroupWorkspacePage documentId="doc1" />);
    expect(await screen.findByRole("button", { name: "Chốt phân công" })).toBeDisabled();
  });

  it("trưởng nhóm chốt phân công khi mọi mục có người phụ trách, mở chốt phải xác nhận", async () => {
    const planning = { assignmentLockedBy: null, assignmentLockedAt: null };
    const calls = backend(workspace(LEADER, { ...planning, sections: [section("s1", "OPEN", null, { assigneeId: MEMBER })] }));
    const { unmount } = render(<GroupWorkspacePage documentId="doc1" />);
    fireEvent.click(await screen.findByRole("button", { name: "Chốt phân công" }));
    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/assignment:lock"))).toBe(true));
    unmount();

    const after = backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);
    expect(await screen.findByText("Đã chốt phân công")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Giao mục" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Mở chốt" }));
    const dialog = await screen.findByRole("dialog", { name: /Mở chốt phân công/ });
    fireEvent.click(within(dialog).getByRole("button", { name: "Mở chốt" }));
    await waitFor(() => expect(after.some((c) => c.url.endsWith("/assignment:unlock"))).toBe(true));
  });

  it("lịch sử mục hiện từng bản với loại và tác giả", async () => {
    backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = (await screen.findAllByRole("listitem")).find((li) => within(li).queryByText("Mục s2"))!;

    fireEvent.click(within(item).getByRole("button", { name: "Lịch sử" }));

    const dialog = await screen.findByRole("dialog", { name: /Lịch sử · Mục s2/ });
    expect(await within(dialog).findByText(/Nhập từ file · Bình/)).toBeInTheDocument();
    expect(within(dialog).getByText("Bản nháp cũ")).toBeInTheDocument();
    expect(within(dialog).queryByRole("button", { name: /Dùng bản này/ })).not.toBeInTheDocument();
  });

  it("thành viên chấm đánh giá chéo cho thành viên khác", async () => {
    const calls = backend(workspace(MEMBER));
    render(<GroupWorkspacePage documentId="doc1" />);
    fireEvent.click(await screen.findByRole("tab", { name: "Đánh giá chéo" }));

    const save = await screen.findByRole("button", { name: "Lưu đánh giá" });
    expect(save).toBeDisabled();
    fireEvent.change(screen.getByLabelText("Điểm cho An"), { target: { value: "4" } });
    fireEvent.change(screen.getByLabelText("Nhận xét cho An"), { target: { value: "Ổn" } });
    fireEvent.click(save);

    expect(await screen.findByText("Đã lưu đánh giá.")).toBeInTheDocument();
    expect(calls.find((c) => c.method === "PUT" && c.url.endsWith("/peer-reviews"))?.body).toEqual({
      reviews: [{ revieweeId: LEADER, score: 4, comment: "Ổn" }],
    });
  });

  it("giảng viên xem trung bình đánh giá chéo theo thang 10", async () => {
    backend(workspace("teacher", { role: { member: false, leader: false, teacher: true } }));
    render(<GroupWorkspacePage documentId="doc1" />);
    fireEvent.click(await screen.findByRole("tab", { name: "Đánh giá chéo" }));

    expect(await screen.findByText("1/2 thành viên đã đánh giá", { exact: false })).toBeInTheDocument();
    expect(screen.getByText("4.5 (2 người)")).toBeInTheDocument();
    expect(screen.getByText("9.0")).toBeInTheDocument();
    expect(screen.getByText(/Gánh team/)).toBeInTheDocument();
  });

  it("trưởng nhóm nhập file: xem trước theo mục, bỏ qua mục đang giữ, áp dụng mục đã chọn", async () => {
    const calls = backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);
    fireEvent.click(await screen.findByRole("button", { name: "Nhập từ file" }));
    const dialog = await screen.findByRole("dialog", { name: /Nhập tài liệu nhóm từ file/ });

    fireEvent.change(within(dialog).getByLabelText("Chọn tệp DOCX"), {
      target: { files: [new File(["x"], "nhom.docx", { type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document" })] },
    });
    fireEvent.click(within(dialog).getByRole("button", { name: "Xem trước" }));

    expect(await within(dialog).findByText("Mục đang có người giữ")).toBeInTheDocument();
    expect(within(dialog).getByText("Tiêu đề “Phụ lục”")).toBeInTheDocument();
    expect(within(dialog).getByLabelText("Nhập vào Mục s2")).toBeDisabled();
    fireEvent.click(within(dialog).getByRole("button", { name: "Nhập vào 1 mục" }));

    expect(await screen.findByText("Đã nhập vào 1 mục.")).toBeInTheDocument();
    expect(calls.find((c) => c.url.endsWith("/import:apply"))?.body).toEqual({
      sections: [{ sectionId: "s3", version: 2, blocks: [{ id: "n1", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Từ Word" }] } }] }],
    });
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

  it("người giữ khôi phục một bản lịch sử vào bản nháp", async () => {
    const calls: { url: string; method: string; body?: unknown }[] = [];
    vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      calls.push({ url, method: init?.method ?? "GET", body: typeof init?.body === "string" ? JSON.parse(init.body) : undefined });
      if (url.endsWith("/revisions"))
        return json([{ id: "r1", authorId: LEADER, kind: "DONE", createdAt: "2026-10-22T03:00:00Z",
          blocks: [{ id: "old", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Bản cũ của An" }] } }] }]);
      if (url.endsWith(":restore"))
        return json(section("s2", "CLAIMED", MEMBER, { version: 3, draftSavedAt: "2026-10-22T05:00:00Z",
          draftBlocks: [{ id: "old", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Bản cũ của An" }] } }] }));
      return json({});
    }));
    const shared = { blocks: [{ id: "s2", origin: "TEACHER" as const, type: "HEADING" as const, content: { style: "H1" as const, text: "Mục s2" } }] };
    render(<SectionEditorDialog documentId="doc1" section={section("s2", "CLAIMED", MEMBER)} shared={shared} diagramEnabled={false}
      members={[{ accountId: LEADER, name: "An", leader: true }]} viewerId={MEMBER} onClose={() => undefined} onChanged={() => undefined} />);

    fireEvent.click(screen.getByRole("button", { name: "Lịch sử" }));
    const dialog = await screen.findByRole("dialog", { name: /Lịch sử · Mục s2/ });
    fireEvent.click(await within(dialog).findByRole("button", { name: "Dùng bản này làm bản nháp" }));

    await waitFor(() => expect(screen.queryByRole("dialog", { name: /Lịch sử/ })).not.toBeInTheDocument());
    expect(calls.find((c) => c.url.endsWith(":restore"))?.body).toEqual({ revisionId: "r1", version: 2 });
    expect(screen.getByText("Bản cũ của An")).toBeInTheDocument();
  });

  it("partOf lấy heading và hướng dẫn của phần; không có heading thì dựng theo tên mục", () => {
    const shared = { blocks: [{ id: "h", origin: "TEACHER" as const, type: "HEADING" as const, content: { style: "H1" as const, text: "A" } }] };
    expect(partOf(shared, { partId: "h", title: "A" })).toMatchObject({ heading: { id: "h" }, guide: [] });
    expect(partOf(shared, { partId: "x", title: "Toàn bài" }).heading).toMatchObject({ id: "x", content: { text: "Toàn bài" } });
  });
});
