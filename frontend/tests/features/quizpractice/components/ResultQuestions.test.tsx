import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { questionIndex } from "@/features/quizpractice/components/QuestionPager";
import { ResultQuestions } from "@/features/quizpractice/components/ResultQuestions";
import type { ResultQuestion } from "@/features/quizpractice/model/types";

const question = (id: string, stem: string, overrides: Partial<ResultQuestion> = {}): ResultQuestion => ({
  questionId: id, type: "MCQ_SINGLE", stem, options: [{ optionId: "1", text: "Đáp án A" }, { optionId: "2", text: "Đáp án B" }],
  selectedOptionIds: ["1"], correct: true, points: 1, maxPoints: 1, correctOptionIds: ["1"], explanation: `Giải thích ${stem}`, ...overrides,
});
const QUESTIONS = [
  question("q1", "Câu một"),
  question("q2", "Câu hai", { selectedOptionIds: ["2"], correct: false, points: 0 }),
  question("q3", "Câu ba", { selectedOptionIds: [], correct: false, points: 0 }),
];

describe("ResultQuestions: mỗi câu một trang", () => {
  it("chỉ hiện một câu, chuyển bằng nút và bảng số; bảng số có màu đúng, sai, bỏ trống", () => {
    render(<ResultQuestions questions={QUESTIONS} showScore />);
    expect(screen.getByText("Câu một")).toBeInTheDocument();
    expect(screen.queryByText("Câu hai")).not.toBeInTheDocument();
    expect(screen.getByTestId("result-question-prev")).toBeDisabled();

    fireEvent.click(screen.getByTestId("result-question-next"));
    expect(screen.getByText("Câu hai")).toBeInTheDocument();
    expect(screen.getByText("Câu 2 / 3")).toBeInTheDocument();
    expect(screen.queryByText("Câu một")).not.toBeInTheDocument();

    const nav = within(screen.getByTestId("result-question-nav"));
    expect(nav.getByRole("button", { name: "Câu 1" }).className).toContain("success");
    expect(nav.getByRole("button", { name: "Câu 2" }).className).toContain("danger");
    expect(nav.getByRole("button", { name: "Câu 3" }).className).toContain("border-line");
    fireEvent.click(nav.getByRole("button", { name: "Câu 3" }));
    expect(screen.getByText("Bỏ trống")).toBeInTheDocument();
    expect(screen.getByTestId("result-question-next")).toBeDisabled();
  });

  it("trang kết quả điều khiển câu đang xem qua URL", () => {
    const onIndex = vi.fn();
    render(<ResultQuestions questions={QUESTIONS} showScore index={1} onIndex={onIndex} />);
    expect(screen.getByText("Câu hai")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("result-question-next"));
    expect(onIndex).toHaveBeenCalledWith(2);
  });

  it("số câu trên URL sai thì về câu hợp lệ", () => {
    expect(questionIndex("2", 3)).toBe(1);
    expect(questionIndex("99", 3)).toBe(2);
    expect(questionIndex("abc", 3)).toBe(0);
    expect(questionIndex("0", 3)).toBe(0);
    expect(questionIndex("1", 0)).toBe(0);
  });
});
