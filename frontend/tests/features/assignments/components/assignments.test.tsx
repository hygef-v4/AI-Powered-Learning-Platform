import { studentAssignmentContext } from "@/features/assignments/components/assignmentNav";
import * as filesApi from "@/features/files/api";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RubricNote, rubricComplete, rubricTotal, type DocumentModel, type RubricCriterion } from "@/shared/document";
import { assignmentsApi } from "@/features/assignments/api";
import { AssignmentListCard } from "@/features/assignments/components/AssignmentListCard";
import { WHOLE_PART, cardIssues, hasWholePart, listRestoredCriteria, mergeSaved, missingText, reappearedParts, rubricIssues } from "@/features/assignments/pages/AssignmentFormPage";
import { CreateAssignmentDialog } from "@/features/assignments/components/CreateAssignmentDialog";
import { fromLocalInput, toLocalInput } from "@/features/assignments/model/format";
import { RubricEditor, cleanCriteria, rubricPointsMalformed } from "@/features/assignments/components/RubricEditor";
import { ScheduleForm } from "@/features/assignments/components/ScheduleForm";
import { statusLabels, type AssignmentDetail } from "@/features/assignments/model/types";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }), useSearchParams: () => new URLSearchParams(window.location.search) }));

afterEach(() => window.history.replaceState(null, "", "/"));

const reviewed = {
  id: "a1", status: "REVIEWED", opensAt: null, closesAt: null, lateUntil: null, maxAttempts: null,
} as unknown as AssignmentDetail;

describe("RubricEditor", () => {
  const criteria: RubricCriterion[] = [{ criterionId: "c", title: "Nội dung", description: "", kind: "TEXT", items: [{ itemId: "i", text: "Đủ ý", points: 2 }] }];

  it("mục chấm atomic sửa thẳng; dòng cuối là dấu + để thêm mục, Enter ở một mục thêm mục ngay dưới", () => {
    const onChange = vi.fn();
    render(<RubricEditor criteria={criteria} onChange={onChange} />);
    expect(screen.getByLabelText("Mục chấm 1")).toHaveValue("Đủ ý");
    expect(screen.queryByLabelText("Mục chấm mới")).toBeNull();
    expect(screen.getByRole("button", { name: "Thêm mục chấm" })).toHaveTextContent("+");
    fireEvent.change(screen.getByLabelText("Điểm mục 1"), { target: { value: "1.5" } });
    expect((onChange.mock.calls[0]![0] as RubricCriterion[])[0]!).toMatchObject({
      criterionId: "c", title: "Đủ ý", kind: "TEXT", items: [{ itemId: "i", text: "Đủ ý", points: 1.5 }],
    });
    fireEvent.click(screen.getByRole("button", { name: "Thêm mục chấm" }));
    const added = onChange.mock.calls[1]![0] as RubricCriterion[];
    expect(added).toHaveLength(2);
    expect(added[1]!).toMatchObject({ kind: "TEXT", items: [{ text: "", points: null }] });
    fireEvent.keyDown(screen.getByLabelText("Mục chấm 1"), { key: "Enter" });
    expect(onChange.mock.calls[2]![0] as RubricCriterion[]).toHaveLength(2);
  });

  it("dòng vừa thêm mà chưa gõ gì không gửi lên server", () => {
    const blank: RubricCriterion[] = [...criteria, { criterionId: "n", title: "", description: "", kind: "TEXT", items: [{ itemId: "m", text: " ", points: null }] }];
    expect(cleanCriteria(blank).map((criterion) => criterion.criterionId)).toEqual(["c"]);
  });

  it("điểm gửi lên: số dương và 0 giữ nguyên, không làm tròn; số âm là chưa có điểm", () => {
    const dirty: RubricCriterion[] = [{ ...criteria[0]!, items: [
      { itemId: "a", text: "x", points: 1.25 }, { itemId: "b", text: "y", points: 0 }, { itemId: "c", text: "z", points: -1 },
    ] }];
    expect(cleanCriteria(dirty)[0]!.items.map((item) => item.points)).toEqual([1.25, 0, null]);
  });

  it("điểm quá 2 chữ số thập phân là sai: ô điểm báo lỗi, rubric không lưu, không tính vào tổng", () => {
    const wrong: RubricCriterion[] = [{ ...criteria[0]!, items: [{ itemId: "i", text: "Đủ ý", points: 1.255 }] }];
    render(<RubricEditor criteria={wrong} onChange={vi.fn()} />);
    expect(screen.getByLabelText("Điểm mục 1")).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByLabelText("Điểm mục 1")).toHaveClass("border-danger", "bg-danger-soft", "text-danger");
    expect(screen.getByLabelText("Điểm mục 1")).not.toHaveClass("border-line", "bg-transparent", "text-ink");
    expect(rubricPointsMalformed(wrong)).toBe(true);
    expect(rubricPointsMalformed(criteria)).toBe(false);
    expect(rubricPointsMalformed([{ ...criteria[0]!, items: [{ itemId: "i", text: "Đủ ý", points: 0 }] }])).toBe(false);
    expect(rubricTotal(wrong)).toBe(0);
    expect(rubricComplete(wrong)).toBe(false);
    expect(rubricComplete([{ ...criteria[0]!, items: [{ itemId: "i", text: "Đủ ý", points: 1.25 }] }])).toBe(true);
  });

  it("Enter ở ô điểm cũng thêm mục ngay dưới", () => {
    const onChange = vi.fn();
    render(<RubricEditor criteria={criteria} onChange={onChange} />);
    fireEvent.keyDown(screen.getByLabelText("Điểm mục 1"), { key: "Enter" });
    const added = onChange.mock.calls[0]![0] as RubricCriterion[];
    expect(added).toHaveLength(2);
    expect(added[1]!).toMatchObject({ items: [{ text: "", points: null }] });
  });
});

describe("CreateAssignmentDialog", () => {
  it("một form duy nhất: Cách tạo có 3 lựa chọn, Loại gộp hình thức và chế độ, nút chỉ ghi Tạo", async () => {
    const created = { id: "a1", version: 0 } as AssignmentDetail;
    const create = vi.spyOn(assignmentsApi, "createForClass").mockResolvedValue(created);
    const onCreated = vi.fn();
    render(<CreateAssignmentDialog target={{ kind: "class", classId: "c1", groupSupported: false }} onClose={vi.fn()} onCreated={onCreated} />);
    expect(screen.queryByRole("tab")).toBeNull();
    expect(screen.getAllByRole("radio").slice(0, 3).map((radio) => radio.closest("label")?.textContent))
      .toEqual(["Đề mới", "Đề có sẵn", "Tạo từ AI"]);
    expect(screen.getByRole("radio", { name: "Đề mới" })).toBeChecked();
    // Tiêu đề không có chữ gợi ý; Đề mới chỉ có Mẫu đề (tệp DOCX), không có copy.
    expect(screen.getByTestId("create-assignment-title")).not.toHaveAttribute("placeholder");
    expect(screen.queryByRole("combobox")).toBeNull();
    expect(screen.getByTestId("create-assignment-submit")).toHaveTextContent(/^Tạo$/);
    expect(screen.getByRole("radio", { name: "Nhóm" })).toBeDisabled();
    fireEvent.click(screen.getByRole("radio", { name: "Luyện tập" }));
    fireEvent.change(screen.getByTestId("create-assignment-title"), { target: { value: "Lab 1" } });
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(created));
    expect(create.mock.calls[0]![1]).toMatchObject({ title: "Lab 1", submissionMode: "INDIVIDUAL", gradingMode: "PRACTICE" });
    vi.restoreAllMocks();
  });

  it("một ô tệp DOCX: chọn xong chỉ hiện tên tệp; tạo bài rồi nhập luôn tệp vào khung, không có dòng hướng dẫn", async () => {
    const created = { id: "a1", version: 0 } as AssignmentDetail;
    const saved = { id: "a1", version: 1 } as AssignmentDetail;
    const document: DocumentModel = { blocks: [{ id: "h", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Phân tích" } }] };
    const create = vi.spyOn(assignmentsApi, "createForClass").mockResolvedValue(created);
    const importDocx = vi.spyOn(assignmentsApi, "importDocx").mockResolvedValue({ document, warnings: [], headingCount: 1, imageCount: 0, diagramCount: 0 });
    const saveSkeleton = vi.spyOn(assignmentsApi, "saveSkeleton").mockResolvedValue(saved);
    const onCreated = vi.fn();
    render(<CreateAssignmentDialog target={{ kind: "class", classId: "c1", groupSupported: false }} onClose={vi.fn()} onCreated={onCreated} />);
    expect(screen.queryByText(/Sơ đồ Draw.io luôn dùng được/)).toBeNull();
    const file = new File(["docx"], "De-bai.docx");
    fireEvent.change(screen.getByTestId("create-docx"), { target: { files: [file] } });
    expect(screen.getByTestId("docx-file")).toHaveTextContent("De-bai.docx");
    fireEvent.change(screen.getByTestId("create-assignment-title"), { target: { value: "Lab 1" } });
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(saved));
    expect(create.mock.calls[0]![1]).toMatchObject({ submissionMode: "INDIVIDUAL", diagramEnabled: true });
    expect(importDocx).toHaveBeenCalledWith("a1", file);
    expect(saveSkeleton).toHaveBeenCalledWith("a1", 0, document, { fromDocx: true });
    vi.restoreAllMocks();
  });

  it("nhập tệp lỗi thì không tạo thêm bài: thử lại chỉ nhập lại tệp, hoặc mở bài đã tạo", async () => {
    const created = { id: "a1", version: 0 } as AssignmentDetail;
    const create = vi.spyOn(assignmentsApi, "createForClass").mockResolvedValue(created);
    const importDocx = vi.spyOn(assignmentsApi, "importDocx").mockRejectedValue(new Error("Tệp hỏng"));
    const onCreated = vi.fn();
    render(<CreateAssignmentDialog target={{ kind: "class", classId: "c1", groupSupported: false }} onClose={vi.fn()} onCreated={onCreated} />);
    fireEvent.change(screen.getByTestId("create-docx"), { target: { files: [new File(["x"], "a.docx")] } });
    fireEvent.change(screen.getByTestId("create-assignment-title"), { target: { value: "Lab 1" } });
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Đã tạo bài, chưa nhập tệp");
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    await waitFor(() => expect(importDocx).toHaveBeenCalledTimes(2));
    expect(create).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByRole("button", { name: "Mở bài" }));
    expect(onCreated).toHaveBeenCalledWith(created);
    vi.restoreAllMocks();
  });

  it("đề mẫu của môn cũng copy được từ Đề có sẵn: bài cùng môn đứng đầu, rồi các lớp mình dạy", async () => {
    const created = { id: "a9", version: 0 } as AssignmentDetail;
    const row = (id: string) => ({ id, title: `Bài ${id}` }) as never;
    vi.spyOn(assignmentsApi, "subjectCopySources").mockResolvedValue([
      { classId: "s1", code: "SWD392", name: "Thiết kế", status: null, kind: "SUBJECT", assignments: [row("m1")] },
      { classId: "c2", code: "SE01", name: "Lớp 1", status: "OPEN", kind: "CLASS", assignments: [row("k1")] },
    ]);
    const copy = vi.spyOn(assignmentsApi, "copyToSubject").mockResolvedValue(created);
    const onCreated = vi.fn();
    render(<CreateAssignmentDialog target={{ kind: "subject", subjectId: "s1" }} onClose={vi.fn()} onCreated={onCreated} />);
    fireEvent.click(screen.getByRole("radio", { name: "Đề có sẵn" }));
    expect(screen.queryByRole("radiogroup", { name: "Loại" })).toBeNull();
    const search = screen.getByRole("combobox", { name: "Đề có sẵn" });
    fireEvent.focus(search);
    await screen.findByRole("option", { name: "Bài m1" });
    // Chỉ liệt kê bài, không chia nhóm cùng môn / lớp; gõ để tìm.
    expect(screen.queryAllByRole("group")).toHaveLength(0);
    expect(screen.getAllByRole("option").map((option) => option.textContent)).toEqual(["Bài m1", "Bài k1"]);
    fireEvent.change(search, { target: { value: "k1" } });
    expect(screen.getAllByRole("option").map((option) => option.textContent)).toEqual(["Bài k1"]);
    fireEvent.change(search, { target: { value: "" } });
    fireEvent.mouseDown(screen.getByRole("option", { name: "Bài m1" }));
    // Chỉ chọn một bài: bài đã chọn hiện dưới ô tìm, chọn bài khác thì thay.
    expect(screen.getByTestId("copy-source")).toHaveTextContent("Bài m1");
    fireEvent.focus(search);
    fireEvent.mouseDown(screen.getByRole("option", { name: "Bài k1" }));
    expect(screen.getByTestId("copy-source")).toHaveTextContent("Bài k1");
    fireEvent.click(screen.getByRole("button", { name: "Bỏ bài đã chọn" }));
    fireEvent.focus(search);
    fireEvent.mouseDown(screen.getByRole("option", { name: "Bài m1" }));
    // Tiêu đề không lấy theo bài copy: để trống cho người dùng tự nhập.
    expect(screen.getByTestId("create-assignment-title")).toHaveValue("");
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    expect(await screen.findByText("Nhập tiêu đề bài tập.")).toBeInTheDocument();
    expect(copy).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("create-assignment-title"), { target: { value: "Đề mẫu 1" } });
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(created));
    expect(copy).toHaveBeenCalledWith("s1", "m1", "Đề mẫu 1");
    vi.restoreAllMocks();
  });

  it("tiêu đề bắt buộc ở trên cùng: bỏ trống thì báo lỗi, không gọi API", async () => {
    const create = vi.spyOn(assignmentsApi, "createForSubject");
    render(<CreateAssignmentDialog target={{ kind: "subject", subjectId: "s1" }} onClose={vi.fn()} onCreated={vi.fn()} />);
    expect(screen.getByRole("dialog")).toHaveTextContent("Đề mẫu mới");
    expect(screen.getByLabelText(/Tiêu đề/)).toBe(screen.getByTestId("create-assignment-title"));
    // Chủ nhiệm môn tạo đề mẫu: cũng có Cách tạo; có cả loại Nhóm.
    expect(screen.getByRole("radiogroup", { name: "Cách tạo" })).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: "Nhóm" })).toBeEnabled();
    fireEvent.change(screen.getByTestId("create-assignment-title"), { target: { value: "   " } });
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    expect(await screen.findByText("Nhập tiêu đề bài tập.")).toBeInTheDocument();
    expect(screen.getByTestId("create-assignment-title")).toHaveAttribute("aria-invalid", "true");
    expect(create).not.toHaveBeenCalled();
    vi.restoreAllMocks();
  });

  it("giảng viên tạo bài ở lớp từ Đề có sẵn: đề mẫu của môn đứng trước bài các lớp mình dạy", async () => {
    const created = { id: "a7", version: 0 } as AssignmentDetail;
    const row = (id: string) => ({ id, title: `Bài ${id}` }) as never;
    vi.spyOn(assignmentsApi, "copySources").mockResolvedValue([
      { classId: "s1", code: "SWD392", name: "Thiết kế", status: null, kind: "SUBJECT", assignments: [row("m1"), row("m2")] },
      { classId: "c2", code: "SE01", name: "Lớp 1", status: "OPEN", kind: "CLASS", assignments: [row("k1")] },
    ]);
    const copy = vi.spyOn(assignmentsApi, "copy").mockResolvedValue(created);
    const onCreated = vi.fn();
    render(<CreateAssignmentDialog target={{ kind: "class", classId: "c1", groupSupported: true }} onClose={vi.fn()} onCreated={onCreated} />);
    fireEvent.click(screen.getByRole("radio", { name: "Đề có sẵn" }));
    expect(screen.queryByRole("radiogroup", { name: "Loại" })).toBeNull();
    expect(screen.queryByTestId("create-docx")).toBeNull();
    fireEvent.change(screen.getByTestId("create-assignment-title"), { target: { value: "Lab 1" } });
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Chọn một đề có sẵn để copy.");
    const search = screen.getByRole("combobox", { name: "Đề có sẵn" });
    fireEvent.focus(search);
    await screen.findByRole("option", { name: "Bài m2" });
    expect(screen.getAllByRole("option").map((option) => option.textContent)).toEqual(["Bài m1", "Bài m2", "Bài k1"]);
    fireEvent.mouseDown(screen.getByRole("option", { name: "Bài m2" }));
    expect(screen.getByTestId("copy-source")).toHaveTextContent("Bài m2");
    expect(screen.getByTestId("create-assignment-title")).toHaveValue("Lab 1");
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(created));
    expect(copy).toHaveBeenCalledWith("c1", "m2", "Lab 1");
    vi.restoreAllMocks();
  });

  it("Tạo từ AI ngay trong popup tạo: Ngôn ngữ, Mẫu đề, Tài liệu tham khảo, Yêu cầu; tổng 10 điểm", async () => {
    const created = { id: "a8", version: 0, title: "Lab AI", gradingMode: "GRADED" } as AssignmentDetail;
    const applied = { id: "a8", version: 1 } as AssignmentDetail;
    const create = vi.spyOn(assignmentsApi, "createForClass").mockResolvedValue(created);
    vi.spyOn(assignmentsApi, "copySources").mockResolvedValue([
      { classId: "s1", code: "SWD392", name: "Thiết kế", status: null, kind: "SUBJECT", assignments: [{ id: "t1", title: "Đề mẫu cuối kỳ" }] as never },
      { classId: "c2", code: "SE01", name: "Lớp 1", status: "OPEN", kind: "CLASS", assignments: [{ id: "k1", title: "Bài k1" }] as never },
    ]);
    const classMaterials = vi.spyOn(assignmentsApi, "classMaterials").mockResolvedValue([{ materialId: "m1", title: "UML basics", hasText: true }]);
    vi.spyOn(filesApi, "getPolicies").mockResolvedValue([]);
    const requestAiDraft = vi.spyOn(assignmentsApi, "requestAiDraft").mockResolvedValue({
      draftId: "d1", assignmentId: "a8", origin: "AI_TASK", status: "READY", taskId: null, provenance: {}, safeError: null,
      baseVersion: 0, createdAt: "now", decidedAt: null, proposal: { title: "T", businessContext: "", instructions: "", sections: [
        { level: 1, title: "Câu 1", guidance: "", rubric: [] }] as never,
      validation: { status: "VALID", questionCount: 1, rubricLeafCount: 1, totalPoints: 10, issues: [] } },
    });
    const apply = vi.spyOn(assignmentsApi, "applyAiDraft").mockResolvedValue(applied);
    const onCreated = vi.fn();
    render(<CreateAssignmentDialog target={{ kind: "class", classId: "c1", groupSupported: true }} onClose={vi.fn()} onCreated={onCreated} />);
    fireEvent.click(screen.getByRole("radio", { name: "Tạo từ AI" }));
    expect(screen.getByText("AI soạn đề kèm rubric, tổng 10 điểm.")).toBeInTheDocument();
    expect(screen.queryByTestId("create-docx")).toBeNull();
    // Ngôn ngữ cạnh Mẫu đề, mặc định English; không có Số câu hỏi, Tổng điểm: popup gọn.
    expect(screen.getByLabelText("Ngôn ngữ")).toHaveValue("EN");
    fireEvent.change(screen.getByLabelText("Ngôn ngữ"), { target: { value: "VI" } });
    expect(screen.queryByLabelText("Số câu hỏi")).toBeNull();
    expect(classMaterials).toHaveBeenCalledWith("c1");
    // Mẫu đề như ô copy cũ: tìm đề mẫu của môn (không có bài của lớp khác), dòng cuối là tải DOCX lên.
    const template = screen.getByRole("combobox", { name: "Mẫu đề" });
    fireEvent.focus(template);
    await screen.findByRole("option", { name: "Đề mẫu cuối kỳ" });
    expect(within(screen.getByRole("listbox")).getAllByRole("option").map((option) => option.textContent)).toEqual(["Đề mẫu cuối kỳ", "Tải đề mẫu lên"]);
    fireEvent.mouseDown(screen.getByRole("option", { name: "Đề mẫu cuối kỳ" }));
    // Đã chọn thì tên hiện ngay trong ô như dropdown, không ghi xuống dưới.
    expect(template).toHaveAttribute("placeholder", "Đề mẫu cuối kỳ");
    expect(screen.getByRole("button", { name: "Bỏ mẫu đề" })).toBeInTheDocument();
    fireEvent.blur(template);
    const reference = screen.getByRole("combobox", { name: "Tài liệu tham khảo" });
    fireEvent.focus(reference);
    fireEvent.mouseDown(await screen.findByRole("option", { name: /UML basics/ }));
    fireEvent.change(screen.getByLabelText("Yêu cầu"), { target: { value: "Thiết kế CSDL thư viện" } });
    fireEvent.click(screen.getByRole("radio", { name: "Nhóm" }));
    fireEvent.change(screen.getByTestId("create-assignment-title"), { target: { value: "Lab AI" } });
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(applied));
    expect(create.mock.calls[0]![1]).toMatchObject({ title: "Lab AI", submissionMode: "GROUP", gradingMode: "GRADED" });
    expect(requestAiDraft).toHaveBeenCalledWith("a8", expect.objectContaining({
      request: "Thiết kế CSDL thư viện", materialIds: ["m1"], templateAssignmentId: "t1",
      options: { profile: "AUTO", language: "VI", targetPoints: 10 },
    }));
    expect(apply).toHaveBeenCalledWith("a8", "d1", 0, true, false);
    vi.restoreAllMocks();
  });

  it("Tạo từ AI lỗi: giữ bài đã tạo để thử lại, đóng popup thì bỏ bài vừa tạo", async () => {
    const created = { id: "a9", version: 0, title: "Lab AI", gradingMode: "GRADED" } as AssignmentDetail;
    const create = vi.spyOn(assignmentsApi, "createForClass").mockResolvedValue(created);
    vi.spyOn(assignmentsApi, "copySources").mockResolvedValue([]);
    vi.spyOn(assignmentsApi, "classMaterials").mockResolvedValue([]);
    vi.spyOn(filesApi, "getPolicies").mockResolvedValue([]);
    const requestAiDraft = vi.spyOn(assignmentsApi, "requestAiDraft").mockResolvedValue({
      draftId: "d1", assignmentId: "a9", origin: "AI_TASK", status: "FAILED", taskId: null, proposal: null, provenance: {},
      safeError: "Hết hạn mức AI.", baseVersion: 0, createdAt: "now", decidedAt: null,
    });
    const remove = vi.spyOn(assignmentsApi, "remove").mockResolvedValue(undefined);
    const onClose = vi.fn();
    render(<CreateAssignmentDialog target={{ kind: "class", classId: "c1", groupSupported: true }} onClose={onClose} onCreated={vi.fn()} />);
    fireEvent.click(screen.getByRole("radio", { name: "Tạo từ AI" }));
    fireEvent.change(screen.getByTestId("create-assignment-title"), { target: { value: "Lab AI" } });
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Hết hạn mức AI.");
    expect(screen.getByTestId("create-assignment-title")).toBeDisabled();
    expect(screen.getByTestId("create-assignment-submit")).toHaveTextContent("Thử lại");
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    await waitFor(() => expect(requestAiDraft).toHaveBeenCalledTimes(2));
    expect(create).toHaveBeenCalledTimes(1);
    fireEvent.click(await screen.findByRole("button", { name: "Đóng" }));
    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(remove).toHaveBeenCalledWith("a9", 0);
    vi.restoreAllMocks();
  });
});

describe("không còn trạng thái Đã duyệt", () => {
  it("bài đã duyệt từ trước hiện là Nháp và nằm ở tab Nháp; loại gộp hình thức và chế độ", () => {
    expect(statusLabels.REVIEWED).toBe("Nháp");
    const row = (id: string, status: string, submissionMode = "INDIVIDUAL", gradingMode = "GRADED") =>
      ({ id, title: `Bài ${id}`, status, scope: "SUBJECT", submissionMode, gradingMode, closesAt: null }) as never;
    const page = { data: { items: [row("a", "OPEN"), row("b", "REVIEWED", "INDIVIDUAL", "PRACTICE"), row("c", "DRAFT", "GROUP")], hiddenCount: 0, counts: {}, groupSupported: true, ownerArchived: false },
      error: null, loading: false, reload: () => {} } as never;
    render(<AssignmentListCard page={page} />);
    expect(screen.getByText("Bài a")).toBeInTheDocument();
    expect(screen.queryByText("Bài b")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("tab", { name: /Nháp/ }));
    expect(screen.getByTestId("assignment-row-b")).toHaveTextContent("Luyện tập");
    expect(screen.getByTestId("assignment-row-c")).toHaveTextContent("Nhóm");
  });
});

describe("missingText", () => {
  it("xếp lịch mà bài còn thiếu: nói rõ thiếu gì, không ghi chung chung", () => {
    expect(missingText([
      { code: "RUBRIC_INCOMPLETE", blockId: "h1", message: "Mục chưa đủ rubric" },
      { code: "HEADING_EMPTY", blockId: "h2", message: "Tiêu đề trống" },
    ])).toBe("1 rubric bị lỗi điểm; Đề còn chỗ lỗi");
    expect(missingText([
      { code: "TITLE_REQUIRED", blockId: null, message: "Bài cần tiêu đề" },
      { code: "ACTIVITY_NOTICE_REQUIRED", blockId: null, message: "Cần lời báo" },
    ])).toBe("Bài cần tiêu đề; Cần lời báo");
  });
});

describe("rubricIssues", () => {
  const part = (id: string) => ({ partId: id, headingBlockId: `h-${id}`, ancestorHeadingIds: [], title: id });
  const cards = [WHOLE_PART, part("p1"), part("p2")];
  const rubric = (...items: { text: string; points: number | null }[]) => [{ criterionId: "c", title: "", description: "",
    kind: "TEXT" as const, items: items.map((item, i) => ({ itemId: `i${i}`, ...item })) }];

  it("cả bài tổng 0 điểm: chỉ báo chung, không tô đỏ thẻ nào, kể cả Toàn bài", () => {
    const issues = rubricIssues(cards, { ASSIGNMENT: rubric({ text: "Trình bày", points: 0 }), p1: rubric({ text: "Có", points: null }) });
    expect(issues).toEqual([{ code: "RUBRIC_MISSING", blockId: null, message: "Bài làm chưa có rubric chấm điểm" }]);
    expect(cards.flatMap((card) => cardIssues(issues, card))).toEqual([]);
    expect(missingText(issues)).toBe("Bài làm chưa có rubric chấm điểm");
  });

  it("thẻ tổng không quá 0 hoặc mục ghi dở thì thiếu điểm; dòng trống hẳn bỏ qua", () => {
    const issues = rubricIssues(cards, {
      ASSIGNMENT: rubric({ text: "Trình bày", points: null }),
      p1: rubric({ text: "Đủ actor", points: 2 }, { text: "", points: null }),
      p2: rubric({ text: "", points: 1 }),
    });
    expect(cardIssues(issues, WHOLE_PART).map((issue) => issue.message))
      .toEqual(["Có mục chấm thiếu nội dung hoặc thiếu điểm", "Tổng điểm của rubric phải lớn hơn 0"]);
    expect(cardIssues(issues, part("p1"))).toEqual([]);
    expect(cardIssues(issues, part("p2")).map((issue) => issue.message)).toEqual(["Có mục chấm thiếu nội dung hoặc thiếu điểm"]);
    expect(missingText(issues)).toBe("3 rubric bị lỗi điểm");
  });
});

describe("ScheduleForm", () => {
  it("một trang: mở ngay mặc định, tích thì mới hiện ngày giờ; chặn nộp muộn quá 30 ngày", async () => {
    const submit = vi.fn();
    render(<ScheduleForm assignment={reviewed} mode="publish" onSubmit={submit} />);
    expect(screen.queryByLabelText("Mở lúc (ngày)")).toBeNull();
    fireEvent.click(screen.getByTestId("schedule-later"));
    expect(screen.getByLabelText("Mở lúc (ngày)")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("schedule-later"));
    fireEvent.change(screen.getByLabelText("Hạn nộp (ngày)"), { target: { value: "2099-11-01" } });
    fireEvent.click(screen.getByTestId("schedule-late"));
    fireEvent.change(screen.getByLabelText("Nộp muộn đến (ngày)"), { target: { value: "2099-12-15" } });
    fireEvent.click(screen.getByTestId("schedule-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("quá 30 ngày");
    expect(submit).not.toHaveBeenCalled();
  });

  it("chỉ bài Luyện tập có Số lượt làm (1–10); bài có chấm nộp không giới hạn nên không có ô và gửi null", async () => {
    const submit = vi.fn().mockResolvedValue(undefined);
    const graded = { ...reviewed, gradingMode: "GRADED" } as AssignmentDetail;
    const { unmount } = render(<ScheduleForm assignment={graded} mode="publish" onSubmit={submit} />);
    expect(screen.queryByLabelText(/Số lượt/)).toBeNull();
    fireEvent.change(screen.getByLabelText("Hạn nộp (ngày)"), { target: { value: "2099-01-01" } });
    fireEvent.click(screen.getByTestId("schedule-submit"));
    await vi.waitFor(() => expect(submit).toHaveBeenCalledTimes(1));
    expect(submit.mock.calls[0]![0]).toMatchObject({ maxAttempts: null });
    unmount();
    const practice = { ...reviewed, gradingMode: "PRACTICE" } as AssignmentDetail;
    render(<ScheduleForm assignment={practice} mode="publish" onSubmit={submit} />);
    fireEvent.change(screen.getByLabelText("Hạn nộp (ngày)"), { target: { value: "2099-01-01" } });
    fireEvent.change(screen.getByLabelText(/Số lượt làm/), { target: { value: "11" } });
    fireEvent.click(screen.getByTestId("schedule-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Số lượt ngoài 1–10");
    fireEvent.change(screen.getByLabelText(/Số lượt làm/), { target: { value: "3" } });
    fireEvent.click(screen.getByTestId("schedule-submit"));
    await vi.waitFor(() => expect(submit).toHaveBeenCalledTimes(2));
    expect(submit.mock.calls[1]![0]).toMatchObject({ maxAttempts: 3 });
  });

  it("đổi lịch bài đang mở không gửi lại ngày mở (ô chỉ tới phút); bài đã lên lịch bỏ hẹn giờ là mở ngay", async () => {
    const submit = vi.fn().mockResolvedValue(undefined);
    const open = { ...reviewed, status: "OPEN", opensAt: "2026-10-20T03:00:12.345Z", closesAt: "2099-01-01T16:59:00.000Z", maxAttempts: 2 } as AssignmentDetail;
    const { unmount } = render(<ScheduleForm assignment={open} mode="change" onSubmit={submit} />);
    expect(screen.queryByTestId("schedule-later")).toBeNull();
    expect(screen.queryByLabelText("Mở lúc (ngày)")).toBeNull();
    fireEvent.click(screen.getByTestId("schedule-submit"));
    await vi.waitFor(() => expect(submit).toHaveBeenCalledTimes(1));
    expect(submit.mock.calls[0]![0]).toMatchObject({ opensAt: null, closesAt: "2099-01-01T16:59:00.000Z", maxAttempts: null });
    unmount();
    const scheduled = { ...open, status: "SCHEDULED", opensAt: "2098-12-01T00:00:00.000Z" } as AssignmentDetail;
    render(<ScheduleForm assignment={scheduled} mode="change" onSubmit={submit} />);
    fireEvent.click(screen.getByTestId("schedule-later"));
    const before = Date.now();
    fireEvent.click(screen.getByTestId("schedule-submit"));
    await vi.waitFor(() => expect(submit).toHaveBeenCalledTimes(2));
    expect(new Date(submit.mock.calls[1]![0].opensAt).getTime()).toBeGreaterThanOrEqual(before - 1000);
  });

  it("chỉ chọn ngày là đủ: giờ có sẵn mặc định, gửi theo giờ Việt Nam", async () => {
    const submit = vi.fn().mockResolvedValue(undefined);
    render(<ScheduleForm assignment={reviewed} mode="publish" onSubmit={submit} />);
    fireEvent.change(screen.getByLabelText("Hạn nộp (ngày)"), { target: { value: "2099-01-01" } });
    fireEvent.click(screen.getByTestId("schedule-submit"));
    await vi.waitFor(() => expect(submit).toHaveBeenCalled());
    expect(submit.mock.calls[0]![0]).toMatchObject({ openNow: true, opensAt: null, closesAt: "2099-01-01T16:59:00.000Z", lateUntil: null, maxAttempts: null });
  });
});

describe("format", () => {
  it("đổi qua lại ô datetime-local giờ Việt Nam", () => {
    expect(fromLocalInput("2026-10-20T10:00")).toBe("2026-10-20T03:00:00.000Z");
    expect(toLocalInput("2026-10-20T03:00:00.000Z")).toBe("2026-10-20T10:00");
    expect(fromLocalInput("")).toBeNull();
  });
});

describe("mergeSaved", () => {
  it("bỏ fileRef của ảnh đã được gắn khi lưu", () => {
    const local: DocumentModel = { blocks: [{ id: "i1", origin: "TEACHER", type: "IMAGE", content: { fileRef: "ref", alt: "" } }] };
    const saved: DocumentModel = { blocks: [{ id: "i1", origin: "TEACHER", type: "IMAGE", content: { hasFile: true, alt: "" } }] };
    expect(mergeSaved(local, saved).blocks[0]!.content).toEqual({ fileRef: undefined, hasFile: true, alt: "" });
  });
});

it("restores assignment list filters and page from the URL after returning", () => {
  window.history.replaceState(null, "", "/teaching/classes/c?tab=evals&assignmentState=draft&assignmentKind=GROUP&assignmentQuery=Draft&assignmentPage=1");
  const items = Array.from({ length: 12 }, (_, i) => ({ id: String(i), title: `Draft ${i}`, status: "DRAFT", scope: "CLASS", submissionMode: "GROUP", gradingMode: "GRADED", closesAt: null }));
  const page = { data: { items }, error: null, loading: false, reload: vi.fn() } as never;
  const first = render(<AssignmentListCard page={page} />);
  expect(screen.getByRole("tab", { name: /Nháp/ })).toHaveAttribute("aria-selected", "true");
  expect(screen.getByRole("searchbox")).toHaveValue("Draft");
  expect(screen.getByTestId("assignment-row-10")).toBeInTheDocument();
  expect(screen.queryByTestId("assignment-row-0")).not.toBeInTheDocument();
  first.unmount();
  render(<AssignmentListCard page={page} />);
  expect(screen.getByTestId("assignment-row-10")).toBeInTheDocument();
  fireEvent.change(screen.getByRole("searchbox"), { target: { value: "Draft 0" } });
  expect(new URLSearchParams(window.location.search).get("tab")).toBe("evals");
  expect(new URLSearchParams(window.location.search).get("assignmentPage")).toBe("0");
});

it("uses the first assignment page for invalid URL page numbers", () => {
  window.history.replaceState(null, "", "/?assignmentPage=-1");
  const page = { data: { items: [{ id: "a", title: "Open", status: "OPEN", scope: "CLASS", submissionMode: "GROUP", gradingMode: "GRADED", closesAt: null }] }, error: null, loading: false, reload: vi.fn() } as never;
  render(<AssignmentListCard page={page} />);
  expect(screen.getByTestId("assignment-row-a")).toBeInTheDocument();
});

it("mục thành phần chấm lại mà trang còn giữ tiêu chí cũ thì lưu lại rubric đó; mục mới chưa có tiêu chí thì không", () => {
  const part = (partId: string) => ({ partId, headingBlockId: partId, ancestorHeadingIds: [], title: partId });
  const criterion = { criterionId: "c", title: "Đúng", order: 0, atomic: true, points: 2, items: [] } as never;
  expect(reappearedParts(new Set(["b"]), [part("a"), part("b"), part("n")], { a: [criterion], b: [criterion] })).toEqual(["a"]);
});

it("Ctrl+Z sau khi AI sửa đề: ghi lại tiêu chí của mọi phần đã tải trong bản trước, kể cả phần trống và Toàn bài", () => {
  const part = (id: string | null) => ({ partId: id ?? "DOCUMENT", headingBlockId: id, ancestorHeadingIds: [], title: id ?? "Toàn bài" });
  const criterion = { criterionId: "c", title: "Đúng", description: "", kind: "TEXT", items: [] } as unknown as RubricCriterion;
  // "b" trống trước AI (AI có thể vừa điền) vẫn phải ghi lại; "n" chưa tải rubric thì giữ nguyên; "x" không còn trong đề.
  expect(listRestoredCriteria([part("a"), part("b"), part("n")], { ASSIGNMENT: [criterion], a: [criterion], b: [], x: [criterion] }))
    .toEqual([["ASSIGNMENT", [criterion]], ["a", [criterion]], ["b", []]]);
  // Bài không có mục: phần duy nhất đã là cả bài, không có thẻ Toàn bài riêng.
  expect(listRestoredCriteria([part(null)], { ASSIGNMENT: [criterion], DOCUMENT: [criterion] })).toEqual([["DOCUMENT", [criterion]]]);
});

it("thẻ Toàn bài chỉ có ở bài có mục; thẻ soạn chỉ mở trình sửa khi bấm vào, thẻ Toàn bài ghi tổng điểm", () => {
  const part = (id: string | null) => ({ partId: id ?? "DOCUMENT", headingBlockId: id, ancestorHeadingIds: [], title: id ?? "Toàn bài" });
  expect(hasWholePart([part("h1")])).toBe(true);
  expect(hasWholePart([part(null)])).toBe(false);
  expect(WHOLE_PART.partId).toBe("ASSIGNMENT");
  const list = [{ criterionId: "c", title: "Trình bày", description: "", kind: "TEXT", items: [{ itemId: "a", text: "Trình bày đúng mẫu báo cáo có đủ mục lục và tài liệu tham khảo", points: 0 }] }] as unknown as RubricCriterion[];
  const { rerender } = render(<RubricNote title="Toàn bài" criteria={list} total={7.5} editOnActive editor={<p>Trình sửa</p>} />);
  expect(screen.getByText("Tổng 7,5 điểm")).toBeInTheDocument();
  expect(screen.queryByText("Trình sửa")).toBeNull();
  rerender(<RubricNote title="Toàn bài" criteria={list} total={7.5} editOnActive active editor={<p>Trình sửa</p>} />);
  expect(screen.getByText("Trình sửa")).toBeInTheDocument();
});

describe("studentAssignmentContext", () => {
  it("trang làm bài và lịch sử nộp bài của sinh viên có sidebar mục lục của bài", () => {
    const at = (pathname: string) => studentAssignmentContext({ pathname, search: new URLSearchParams() });
    expect(at("/attempts/a1")?.label).toBe("Các phần của bài");
    expect(at("/my-assignments/x1/history")?.label).toBe("Các phần của bài");
    expect(at("/my-assignments/x1")).toBeNull();
    expect(at("/attempts/a1/receipt")).toBeNull();
  });
});
