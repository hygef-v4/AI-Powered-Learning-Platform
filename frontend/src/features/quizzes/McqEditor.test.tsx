import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { McqEditor } from "./McqEditor";
import { emptyForm, type FormErrors, type QuestionForm } from "./questionForm";

function Harness({ initial = emptyForm(), errors = {} }: { initial?: QuestionForm; errors?: FormErrors }) {
  const [value, setValue] = useState(initial);
  return <>
    <McqEditor value={value} onChange={setValue} errors={errors} />
    <output data-testid="state">{JSON.stringify(value)}</output>
  </>;
}

const state = () => JSON.parse(screen.getByTestId("state").textContent ?? "{}") as QuestionForm;

describe("McqEditor", () => {
  it("thêm lựa chọn tới 6 thì khóa nút, ít nhất 2 thì không xóa được", () => {
    render(<Harness />);
    expect(screen.getByTestId("mcq-editor-remove-0")).toBeDisabled();
    for (let i = 0; i < 4; i++) fireEvent.click(screen.getByTestId("mcq-editor-add-option"));
    expect(state().options).toHaveLength(6);
    expect(screen.getByTestId("mcq-editor-add-option")).toBeDisabled();
  });

  it("xóa lựa chọn đang là đáp án đúng thì bỏ khỏi đáp án", () => {
    render(<Harness />);
    fireEvent.click(screen.getByTestId("mcq-editor-add-option"));
    fireEvent.click(screen.getByTestId("mcq-editor-correct-2"));
    expect(state().correctOptionIds).toEqual(["3"]);
    fireEvent.click(screen.getByTestId("mcq-editor-remove-2"));
    expect(state().correctOptionIds).toEqual([]);
  });

  it("câu nhiều đáp án cho chọn nhiều ô", () => {
    render(<Harness />);
    fireEvent.click(screen.getByTestId("mcq-editor-type-MCQ_MULTI"));
    fireEvent.click(screen.getByTestId("mcq-editor-correct-0"));
    fireEvent.click(screen.getByTestId("mcq-editor-correct-1"));
    expect(state().correctOptionIds).toEqual(["1", "2"]);
    fireEvent.change(screen.getByTestId("mcq-editor-option-0"), { target: { value: "Hà Nội" } });
    expect(state().options[0]?.text).toBe("Hà Nội");
  });

  it("hiện lỗi theo trường", () => {
    render(<Harness errors={{ stem: "Nội dung câu hỏi không được để trống", correctOptionIds: "Chọn đúng 1 đáp án đúng" }} />);
    expect(screen.getByText("Nội dung câu hỏi không được để trống")).toBeInTheDocument();
    expect(screen.getByText("Chọn đúng 1 đáp án đúng")).toBeInTheDocument();
  });
});
