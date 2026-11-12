import { render, screen, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { studentAssignmentFeedback } from "@/features/assignmentwork/slots";
import { GroupProgressCard } from "@/features/groupdocs/components/GroupProgressCard";
import { OpenGroupWorkButton } from "@/features/groupdocs/components/OpenGroupWorkButton";
import type { SectionView, WorkspaceView } from "@/features/groupdocs/model/types";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => <a href={href} {...rest}>{children}</a>,
}));

const section = (id: string, status: SectionView["status"], holderId: string | null, lastAuthorId: string | null = null): SectionView => ({
  id, partId: `p-${id}`, title: `Mục ${id}`, orderNo: 1, status, holderId, assignedBy: null, publishedBlocks: null, draftBlocks: null,
  draftSavedAt: null, lastAuthorId, version: 1,
});

function view(overrides: Partial<WorkspaceView> = {}): WorkspaceView {
  return {
    documentId: "doc1", assignmentId: "as1", assignmentTitle: "Lab", closesAt: null, lateUntil: null, classId: "c1", groupId: "g1",
    groupName: "Nhóm 1", leaderId: "an", members: [{ accountId: "an", name: "An", leader: true }, { accountId: "binh", name: "Bình", leader: false }],
    status: "IN_PROGRESS", window: "ON_TIME", sharedBlocks: { blocks: [] },
    sections: [section("s1", "DONE", null, "an"), section("s2", "CLAIMED", "binh"), section("s3", "OPEN", null)],
    role: { member: true, leader: false, teacher: false }, lastSubmission: null, activityMonitoring: false, diagramEnabled: false,
    version: 1, viewerId: "binh", ...overrides,
  };
}

function serve(body: WorkspaceView) {
  vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { "Content-Type": "application/json" } }))));
}

afterEach(() => vi.unstubAllGlobals());

it("thẻ Tiến độ: số mục Xong / tổng, người nhận từng mục, mục của mình tô nền", async () => {
  serve(view());
  render(<GroupProgressCard assignmentId="as1" classId="c1" />);

  const list = await screen.findByRole("list", { name: "Các mục" });
  expect(screen.getByText("Mục xong")).toBeInTheDocument();
  expect(screen.getByText("/ 3")).toBeInTheDocument();
  const row = (title: string) => within(list).getByText(title).closest("li")!;
  expect(row("Mục s1")).toHaveTextContent("AnXong");
  expect(row("Mục s2")).toHaveTextContent("BạnĐang làm");
  expect(row("Mục s2")).toHaveAttribute("data-own", "true");
  expect(row("Mục s1")).not.toHaveAttribute("data-own");
  expect(row("Mục s3")).toHaveTextContent("Chưa ai nhận");
});

it("nhóm đã nộp: thẻ Tiến độ nhường chỗ cho thẻ Kết quả, kèm tên mục theo phần", async () => {
  const Result = vi.fn(({ partTitles }: { partTitles?: Readonly<Record<string, string>> }) => <p>Kết quả {Object.values(partTitles ?? {}).join(", ")}</p>);
  studentAssignmentFeedback.register({ key: "test-result", order: 1, component: Result });
  serve(view({ lastSubmission: { submittedBy: "an", mode: "MANUAL", submittedAt: "2026-10-22T03:00:00Z", late: false, receiptHash: "h" } }));
  render(<GroupProgressCard assignmentId="as1" classId="c1" />);

  expect(await screen.findByText("Kết quả Mục s1, Mục s2, Mục s3")).toBeInTheDocument();
  expect(screen.queryByText("Mục xong")).not.toBeInTheDocument();
});

it("thẻ Làm bài: dòng bản nộp của nhóm và nút mở tài liệu nhóm", async () => {
  serve(view({ lastSubmission: { submittedBy: "an", mode: "MANUAL", submittedAt: "2026-10-22T03:00:00Z", late: true, receiptHash: "h" } }));
  render(<OpenGroupWorkButton assignmentId="as1" classId="c1" />);

  expect(await screen.findByRole("link", { name: "Mở tài liệu nhóm" })).toHaveAttribute("href", "/group-docs/doc1");
  expect(within(screen.getByRole("list", { name: "Bài nộp" })).getByText("Nộp trễ")).toBeInTheDocument();
});
