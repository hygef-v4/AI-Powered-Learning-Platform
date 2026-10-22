import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { DocumentModel, RubricCriterion } from "@/shared/document";
import { assignmentsApi } from "./api";
import { emptyFilter, toListFilter } from "./AssignmentFilters";
import { mergeSaved, missingText } from "./AssignmentFormPage";
import { CreateAssignmentDialog } from "./CreateAssignmentDialog";
import { fromLocalInput, toLocalInput } from "./format";
import { RubricEditor, cleanCriteria } from "./RubricEditor";
import { ScheduleForm } from "./ScheduleForm";
import { statusLabels, type AssignmentDetail } from "./types";

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

  it("điểm gửi lên: số dương, làm tròn 2 chữ số; 0 và số âm là chưa có điểm", () => {
    const dirty: RubricCriterion[] = [{ ...criteria[0]!, items: [
      { itemId: "a", text: "x", points: 1.256 }, { itemId: "b", text: "y", points: 0 }, { itemId: "c", text: "z", points: -1 },
    ] }];
    expect(cleanCriteria(dirty)[0]!.items.map((item) => item.points)).toEqual([1.26, null, null]);
  });
});

describe("CreateAssignmentDialog", () => {
  it("một ô tệp DOCX: chọn xong chỉ hiện tên tệp; tạo bài rồi nhập luôn tệp vào khung, không có dòng hướng dẫn", async () => {
    const created = { id: "a1", version: 0 } as AssignmentDetail;
    const saved = { id: "a1", version: 1 } as AssignmentDetail;
    const document: DocumentModel = { blocks: [{ id: "h", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Phân tích" } }] };
    const create = vi.spyOn(assignmentsApi, "createForClass").mockResolvedValue(created);
    const importDocx = vi.spyOn(assignmentsApi, "importDocx").mockResolvedValue({ document, warnings: [], headingCount: 1, imageCount: 0, diagramCount: 0 });
    const saveSkeleton = vi.spyOn(assignmentsApi, "saveSkeleton").mockResolvedValue(saved);
    const onCreated = vi.fn();
    render(<CreateAssignmentDialog target={{ kind: "class", classId: "c1", groupSupported: false }} onClose={vi.fn()} onCreated={onCreated} />);
    expect(screen.queryByRole("radiogroup", { name: "Cách tạo" })).toBeNull();
    expect(screen.queryByText(/Sơ đồ Draw.io luôn dùng được/)).toBeNull();
    const file = new File(["docx"], "De-bai.docx");
    fireEvent.change(screen.getByTestId("create-docx"), { target: { files: [file] } });
    expect(screen.getByTestId("docx-file")).toHaveTextContent("De-bai.docx");
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
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Đã tạo bài, chưa nhập tệp");
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    await waitFor(() => expect(importDocx).toHaveBeenCalledTimes(2));
    expect(create).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByRole("button", { name: "Mở bài" }));
    expect(onCreated).toHaveBeenCalledWith(created);
    vi.restoreAllMocks();
  });
});

describe("không còn trạng thái Đã duyệt", () => {
  it("bài đã duyệt từ trước hiện là Nháp; lọc Nháp lấy cả bài đó", () => {
    expect(statusLabels.REVIEWED).toBe("Nháp");
    expect(toListFilter({ ...emptyFilter, status: "DRAFT" }).status).toEqual(["DRAFT", "REVIEWED"]);
    expect(toListFilter({ ...emptyFilter, status: "OPEN" }).status).toEqual(["OPEN"]);
  });
});

describe("missingText", () => {
  it("xếp lịch mà bài còn thiếu: báo chung chung, chỉ ghi kèm lỗi không đánh dấu được trong bài", () => {
    expect(missingText([
      { code: "RUBRIC_INCOMPLETE", blockId: "h1", message: "Mục chưa đủ rubric" },
      { code: "HEADING_EMPTY", blockId: "h2", message: "Tiêu đề trống" },
    ])).toBe("Bài còn chỗ chưa đủ");
    expect(missingText([
      { code: "TITLE_REQUIRED", blockId: null, message: "Bài cần tiêu đề" },
      { code: "ACTIVITY_NOTICE_REQUIRED", blockId: null, message: "Cần lời báo" },
    ])).toBe("Bài còn chỗ chưa đủ; Cần lời báo");
  });
});

describe("ScheduleForm", () => {
  it("một trang: mở ngay mặc định, tích thì mới hiện ngày giờ; chặn nộp muộn quá 30 ngày và số lượt ngoài 1–10", async () => {
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
    fireEvent.click(screen.getByTestId("schedule-late"));
    fireEvent.change(screen.getByLabelText(/Số lượt nộp/), { target: { value: "11" } });
    fireEvent.click(screen.getByTestId("schedule-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Số lượt ngoài 1–10");
    expect(submit).not.toHaveBeenCalled();
  });

  it("đổi lịch bài đang mở không gửi lại ngày mở (ô chỉ tới phút); bài đã lên lịch bỏ hẹn giờ là mở ngay", async () => {
    const submit = vi.fn().mockResolvedValue(undefined);
    const open = { ...reviewed, status: "OPEN", opensAt: "2026-10-20T03:00:12.345Z", closesAt: "2099-01-01T16:59:00.000Z", maxAttempts: 2 } as AssignmentDetail;
    const { unmount } = render(<ScheduleForm assignment={open} mode="change" onSubmit={submit} />);
    expect(screen.queryByTestId("schedule-later")).toBeNull();
    expect(screen.queryByLabelText("Mở lúc (ngày)")).toBeNull();
    fireEvent.click(screen.getByTestId("schedule-submit"));
    await vi.waitFor(() => expect(submit).toHaveBeenCalledTimes(1));
    expect(submit.mock.calls[0]![0]).toMatchObject({ opensAt: null, closesAt: "2099-01-01T16:59:00.000Z", maxAttempts: 2 });
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
    expect(submit.mock.calls[0]![0]).toMatchObject({ openNow: true, opensAt: null, closesAt: "2099-01-01T16:59:00.000Z", lateUntil: null, maxAttempts: 1 });
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
