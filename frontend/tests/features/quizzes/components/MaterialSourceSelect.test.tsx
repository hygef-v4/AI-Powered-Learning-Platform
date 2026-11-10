import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { MaterialSourceSelect } from "@/features/quizzes/components/MaterialSourceSelect";

const materials = [
  { materialId: "one", title: "Chương 1.pdf", hasText: true },
  { materialId: "two", title: "Bài giảng.docx", hasText: true },
  { materialId: "video", title: "Video YouTube", hasText: false },
];
function Picker({ count = 3 }: { count?: number }) {
  const [value, onChange] = useState<string[]>([]);
  return <MaterialSourceSelect materials={count === 3 ? materials : Array.from({ length: count }, (_, i) => ({
    materialId: String(i), title: `Tài liệu ${i}`, hasText: true,
  }))} value={value} onChange={onChange} />;
}

describe("AI source document dropdown", () => {
  it("searches, selects several sources without closing and removes selected chips", () => {
    render(<Picker />);
    fireEvent.click(screen.getByText("Chọn tài liệu nguồn"));
    fireEvent.click(screen.getByRole("checkbox", { name: "Chương 1.pdf" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Bài giảng.docx" }));
    expect(screen.getByText("Đã chọn 2/10 tài liệu")).toBeInTheDocument();
    expect(screen.getByRole("searchbox", { name: "Tìm tài liệu nguồn" })).toBeVisible();
    fireEvent.change(screen.getByRole("searchbox"), { target: { value: "Chương" } });
    expect(screen.getAllByRole("checkbox")).toHaveLength(1);
    fireEvent.click(screen.getByRole("button", { name: "Bỏ Chương 1.pdf" }));
    expect(screen.getByRole("checkbox", { name: "Chương 1.pdf" })).not.toBeChecked();
    expect(screen.getByText("Đã chọn 1/10 tài liệu")).toBeInTheDocument();
  });

  it("shows why a video cannot be selected", () => {
    render(<Picker />);
    fireEvent.click(screen.getByText("Chọn tài liệu nguồn"));
    expect(screen.getByRole("checkbox", { name: /Video YouTube/ })).toBeDisabled();
    expect(screen.getByText(/Chưa có nội dung văn bản/)).toBeInTheDocument();
  });

  it("selects only ten sources and keeps already selected items removable", () => {
    render(<Picker count={11} />);
    fireEvent.click(screen.getByText("Chọn tài liệu nguồn"));
    fireEvent.click(screen.getByRole("button", { name: "Chọn kết quả tìm kiếm" }));
    expect(screen.getByText("Đã chọn 10/10 tài liệu")).toBeInTheDocument();
    expect(screen.getAllByRole("checkbox").filter((box) => (box as HTMLInputElement).checked)).toHaveLength(10);
    expect(screen.getByRole("checkbox", { name: "Tài liệu 10" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Bỏ Tài liệu 0" }));
    expect(screen.getByRole("checkbox", { name: "Tài liệu 10" })).toBeEnabled();
  });
});
