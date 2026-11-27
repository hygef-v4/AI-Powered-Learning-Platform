import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { PracticeOverallCard, practiceFeedbackCards, usePracticeFeedback } from "@/features/assignmentwork/components/PracticeFeedbackPanel";
import type { DocumentPart } from "@/shared/document";
import type { Feedback } from "@/features/assignmentwork/model/types";
const mocks = vi.hoisted(() => ({ feedback: vi.fn(), requestFeedback: vi.fn() }));
vi.mock("@/features/assignmentwork/api", () => ({ workApi: mocks }));
afterEach(() => { vi.useRealTimers(); vi.clearAllMocks(); });

const parts: DocumentPart[] = [
  { partId: "q1", headingBlockId: "q1", ancestorHeadingIds: [], title: "Câu 1" },
  { partId: "q1a", headingBlockId: "q1a", ancestorHeadingIds: ["q1"], title: "Câu 1 › Ý a" },
  { partId: "q2", headingBlockId: "q2", ancestorHeadingIds: [], title: "Câu 2" },
];

function Cards({ blockIds = ["q1", "q2"] }: { blockIds?: string[] }) {
  const state = usePracticeFeedback("a");
  const cards = practiceFeedbackCards(state, parts, new Set(blockIds));
  return <>{cards.map((card) => <div key={card.key} data-testid={card.key} data-anchor={card.anchorBlockId ?? ""}>{card.content}</div>)}</>;
}

const ready: Feedback = {
  feedbackId: "f", attemptId: "a", origin: "AI_TASK", status: "READY", taskId: null, provenance: {}, safeError: null,
  createdAt: "2026-11-27T01:00:00Z", completedAt: "2026-11-27T01:01:00Z",
  feedback: { label: "", summary: "Bài khá", strengths: ["Sơ đồ rõ"], suggestions: ["Thêm ràng buộc"], score: 6.5, maxScore: 10,
    parts: [{ partId: "q1a", comment: "Thiếu quan hệ", score: 2.5, maxScore: 4 }, { partId: "q2", comment: "Tốt", score: 4, maxScore: 6 }] },
};

it("thẻ Tổng quan gọn ở đầu cột bên lề: điểm cùng dòng tên thẻ, danh sách thu gọn tới khi bấm; mỗi câu một thẻ gắn vào tiêu đề câu", async () => {
  mocks.feedback.mockResolvedValue([ready]);
  render(<Cards />);
  const overall = await screen.findByTestId("practice-overall");
  expect(overall).toHaveAttribute("data-anchor", "");
  expect(within(overall).getByRole("heading", { name: "Tổng quan" })).toBeInTheDocument();
  expect(overall).toHaveTextContent("6.5/ 10");
  expect(within(overall).queryByText("Sơ đồ rõ")).toBeNull();
  fireEvent.click(within(overall).getByRole("button", { name: /Điểm mạnh/ }));
  expect(within(overall).getByRole("list", { name: "Điểm mạnh" })).toHaveTextContent("Sơ đồ rõ");
  fireEvent.click(within(overall).getByRole("button", { name: /Cần cải thiện/ }));
  expect(within(overall).getByRole("list", { name: "Cần cải thiện" })).toHaveTextContent("Thêm ràng buộc");
  expect(within(overall).queryByText("Sơ đồ rõ")).toBeNull();
  // Đã có góp ý thì không còn nút xin góp ý lại.
  expect(within(overall).queryByRole("button", { name: /Nhận góp ý/ })).toBeNull();

  const first = screen.getByTestId("practice-part-q1");
  expect(first).toHaveAttribute("data-anchor", "q1");
  expect(first).toHaveTextContent("Câu 1");
  expect(first).toHaveTextContent("2.5 / 4");
  expect(first).toHaveTextContent("Ý a: Thiếu quan hệ");
  expect(screen.getByTestId("practice-part-q2")).toHaveTextContent("4 / 6");
});

it("phần AI bỏ sót (server thêm với điểm 0, nhận xét rỗng) ghi rõ là chưa có nhận xét", async () => {
  mocks.feedback.mockResolvedValue([{ ...ready, feedback: { ...ready.feedback!, parts: [{ partId: "q2", comment: "", score: 0, maxScore: 6 }] } }]);
  render(<Cards />);
  const card = await screen.findByTestId("practice-part-q2");
  expect(card).toHaveTextContent("0 / 6");
  expect(card).toHaveTextContent("AI chưa nhận xét phần này.");
});

it("chưa có góp ý thì có nút Nhận góp ý AI; lỗi thì xin lại được", async () => {
  mocks.feedback.mockResolvedValue([{ ...ready, status: "FAILED", feedback: null, safeError: "Hết hạn mức" }]);
  mocks.requestFeedback.mockResolvedValue({ ...ready, feedbackId: "g", status: "PENDING", feedback: null, createdAt: "2026-11-27T02:00:00Z" });
  render(<Cards />);
  const button = await screen.findByRole("button", { name: "Nhận góp ý AI" });
  expect(screen.getByText("Hết hạn mức")).toBeInTheDocument();
  fireEvent.click(button);
  expect(await screen.findByText("AI đang góp ý…")).toBeInTheDocument();
  expect(mocks.requestFeedback).toHaveBeenCalledExactlyOnceWith("a", expect.any(String));
});

it("bounds automatic polling and permits an explicit recheck", async () => {
  vi.useFakeTimers();
  mocks.feedback.mockResolvedValue([{ feedbackId: "f", status: "PENDING", provenance: {}, createdAt: "2026-11-10T00:00:00Z" }]);
  function Overall() {
    return <PracticeOverallCard state={usePracticeFeedback("a")} />;
  }
  render(<Overall />);
  await act(async () => {});
  for (let i = 0; i < 60; i++) await act(async () => { await vi.advanceTimersByTimeAsync(3000); });
  expect(mocks.feedback).toHaveBeenCalledTimes(61);
  await act(async () => { await vi.advanceTimersByTimeAsync(30000); });
  expect(mocks.feedback).toHaveBeenCalledTimes(61);
  fireEvent.click(screen.getByRole("button", { name: "Kiểm tra lại góp ý" }));
  await act(async () => {});
  expect(mocks.feedback).toHaveBeenCalledTimes(62);
});
