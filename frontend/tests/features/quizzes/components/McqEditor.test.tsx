import { render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { McqEditor } from "@/features/quizzes/components/McqEditor";
import { createEmptyForm, type FormErrors, type QuestionForm } from "@/features/quizzes/model/questionForm";

function Harness({ initial = createEmptyForm(), errors = {} }: { initial?: QuestionForm; errors?: FormErrors }) {
  const [value, setValue] = useState(initial);
  return <>
    <McqEditor value={value} onChange={setValue} errors={errors} />
    <output data-testid="state">{JSON.stringify(value)}</output>
  </>;
}


describe("McqEditor", () => {

  it("hiện lỗi theo trường", () => {
    render(<Harness errors={{ stem: "Nội dung câu hỏi không được để trống", correctOptionIds: "Chọn đúng 1 đáp án đúng" }} />);
    expect(screen.getByText("Nội dung câu hỏi không được để trống")).toBeInTheDocument();
    expect(screen.getByText("Chọn đúng 1 đáp án đúng")).toBeInTheDocument();
  });
});
