import { describe, expect, it } from "vitest";
import { emptyForm, nextOptionId, parseTags, toContent, toggleCorrect, validateForm, withType, type QuestionForm } from "./questionForm";

function filled(overrides: Partial<QuestionForm> = {}): QuestionForm {
  return {
    ...emptyForm(),
    stem: "Thủ đô của Việt Nam?",
    options: [{ id: "1", text: "Hà Nội" }, { id: "2", text: "Huế" }],
    correctOptionIds: ["1"],
    ...overrides,
  };
}

describe("questionForm (BR-U10-02, 03)", () => {
  it("chấp nhận câu một đáp án hợp lệ", () => {
    expect(validateForm(filled())).toEqual({});
  });

  it("báo thiếu nội dung, lựa chọn trống hoặc trùng", () => {
    expect(validateForm(filled({ stem: "  " })).stem).toBeTruthy();
    expect(validateForm(filled({ options: [{ id: "1", text: "A" }, { id: "2", text: " " }] })).options).toMatch(/trống/);
    expect(validateForm(filled({ options: [{ id: "1", text: "Hà Nội" }, { id: "2", text: "hà nội " }] })).options).toMatch(/trùng/);
    expect(validateForm(filled({ options: [{ id: "1", text: "A" }] })).options).toMatch(/2–6/);
  });

  it("đáp án đúng theo loại câu", () => {
    expect(validateForm(filled({ correctOptionIds: [] })).correctOptionIds).toMatch(/đúng 1/);
    expect(validateForm(filled({ type: "MCQ_MULTI", correctOptionIds: [] })).correctOptionIds).toMatch(/ít nhất 1/);
    expect(validateForm(filled({ type: "MCQ_MULTI", correctOptionIds: ["1", "2"] }))).toEqual({});
  });

  it("điểm > 0, tối đa 2 chữ số thập phân; tối đa 10 thẻ", () => {
    for (const bad of ["0", "-1", "1.234", "abc", ""]) expect(validateForm(filled({ defaultPoints: bad })).defaultPoints).toBeTruthy();
    expect(validateForm(filled({ defaultPoints: "1,5" })).defaultPoints).toBeUndefined();
    expect(validateForm(filled({ tagsText: Array.from({ length: 11 }, (_, i) => `t${i}`).join(",") })).tags).toBeTruthy();
  });

  it("đổi sang một đáp án chỉ giữ đáp án đầu; chọn đáp án theo loại", () => {
    const multi = filled({ type: "MCQ_MULTI", correctOptionIds: ["1", "2"] });
    expect(withType(multi, "MCQ_SINGLE").correctOptionIds).toEqual(["1"]);
    expect(toggleCorrect(filled(), "2").correctOptionIds).toEqual(["2"]);
    expect(toggleCorrect(multi, "2").correctOptionIds).toEqual(["1"]);
    expect(toggleCorrect(filled({ type: "MCQ_MULTI" }), "2").correctOptionIds).toEqual(["1", "2"]);
  });

  it("mã lựa chọn mới không trùng sau khi xóa; thẻ bỏ trùng không phân biệt hoa thường", () => {
    expect(nextOptionId([{ id: "1", text: "" }, { id: "4", text: "" }])).toBe("5");
    expect(parseTags("Đại số, đại số; Hình ,  ")).toEqual(["Đại số", "Hình"]);
  });

  it("chuyển sang nội dung gửi API", () => {
    const content = toContent(filled({ defaultPoints: "2,5", explanation: "  ", tagsText: "a, b", correctOptionIds: ["1", "9"] }));
    expect(content).toMatchObject({ defaultPoints: 2.5, explanation: null, tags: ["a", "b"], correctOptionIds: ["1"] });
  });
});
