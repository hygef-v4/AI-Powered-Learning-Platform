import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { DocumentModel, RubricCriterion } from "@/shared/document";
import { assignmentsApi } from "./api";
import { mergeSaved } from "./AssignmentFormPage";
import { CreateAssignmentDialog } from "./CreateAssignmentDialog";
import { fromLocalInput, toLocalInput } from "./format";
import { RubricEditor, cleanCriteria } from "./RubricEditor";
import { ScheduleForm } from "./ScheduleForm";
import type { AssignmentDetail } from "./types";

const reviewed = {
  id: "a1", status: "REVIEWED", opensAt: null, closesAt: null, lateUntil: null, maxAttempts: null,
} as unknown as AssignmentDetail;

describe("RubricEditor", () => {
  const criteria: RubricCriterion[] = [{ criterionId: "c", title: "Nội dung", description: "", kind: "TEXT", items: [{ itemId: "i", text: "Đủ ý", points: 2 }] }];

  it("mục chấm atomic sửa thẳng: sửa điểm; gõ vào dòng trống cuối là thêm mục, không cần nút", () => {
    const onChange = vi.fn();
    render(<RubricEditor criteria={criteria} onChange={onChange} />);
    expect(screen.getByLabelText("Mục chấm 1")).toHaveValue("Đủ ý");
    expect(screen.queryByText("+ Mục chấm")).toBeNull();
    fireEvent.change(screen.getByLabelText("Điểm mục 1"), { target: { value: "1.5" } });
    expect((onChange.mock.calls[0]![0] as RubricCriterion[])[0]!).toMatchObject({
      criterionId: "c", title: "Đủ ý", kind: "TEXT", items: [{ itemId: "i", text: "Đủ ý", points: 1.5 }],
    });
    fireEvent.change(screen.getByLabelText("Mục chấm mới"), { target: { value: "Có ví dụ" } });
    const added = onChange.mock.calls[1]![0] as RubricCriterion[];
    expect(added).toHaveLength(2);
    expect(added[1]!).toMatchObject({ title: "Có ví dụ", kind: "TEXT", items: [{ text: "Có ví dụ", points: null }] });
  });

  it("điểm gửi lên: số dương, làm tròn 2 chữ số; 0 và số âm là chưa có điểm", () => {
    const dirty: RubricCriterion[] = [{ ...criteria[0]!, items: [
      { itemId: "a", text: "x", points: 1.256 }, { itemId: "b", text: "y", points: 0 }, { itemId: "c", text: "z", points: -1 },
    ] }];
    expect(cleanCriteria(dirty)[0]!.items.map((item) => item.points)).toEqual([1.26, null, null]);
  });
});

describe("CreateAssignmentDialog", () => {
  it("không còn chọn có sơ đồ hay không; tạo từ DOCX thì trang soạn mở hộp nhập", async () => {
    const created = { id: "a1" } as AssignmentDetail;
    const create = vi.spyOn(assignmentsApi, "createForClass").mockResolvedValue(created);
    const onCreated = vi.fn();
    render(<CreateAssignmentDialog target={{ kind: "class", classId: "c1", groupSupported: false }} onClose={vi.fn()} onCreated={onCreated} />);
    expect(screen.queryByRole("radiogroup", { name: "Sơ đồ Draw.io" })).toBeNull();
    fireEvent.click(screen.getByRole("radio", { name: /Từ tệp DOCX/ }));
    fireEvent.click(screen.getByTestId("create-assignment-submit"));
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(created, { importDocx: true }));
    expect(create.mock.calls[0]![1]).toMatchObject({ submissionMode: "INDIVIDUAL", diagramEnabled: true });
    create.mockRestore();
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
    expect(await screen.findByRole("alert")).toHaveTextContent("không quá 30 ngày");
    fireEvent.click(screen.getByTestId("schedule-late"));
    fireEvent.change(screen.getByLabelText(/Số lượt nộp/), { target: { value: "11" } });
    fireEvent.click(screen.getByTestId("schedule-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Số lượt từ 1 đến 10");
    expect(submit).not.toHaveBeenCalled();
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
