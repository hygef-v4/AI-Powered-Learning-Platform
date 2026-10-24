import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ClassListPage } from "./ClassListPage";
import { ClassDashboardPage } from "./ClassDashboardPage";
import { ClassFormDialog } from "./ClassFormDialog";
import { ClassInfoForm } from "./ClassInfoForm";
import { ClassStateActions, AssignTeacherDialog } from "./ClassDialogs";
import { EnrollmentPanel, EnrollmentResultTable } from "./EnrollmentPanel";
import { ReadOnlyClassPage } from "./ReadOnlyClassPage";
import { ManagedClassPage } from "./ManagedClassPage";
import { parseEnrollmentList } from "./enrollmentList";
import type { CourseClassView, EnrollmentView } from "./types";

const subject = { id: "subject-1", code: "SWP391", name: "Software Development Project", description: "Môn học dự án" };
const teacher = { id: "teacher-1", name: "Nguyễn Minh Kiên", email: "kien@example.test" };
const course: CourseClassView = { id: "class-1", subject, code: "SE1801", name: "Nhóm thứ Hai", term: "2026-1", description: "Lớp sáng thứ Hai",
  status: "OPEN", teacher, activeStudentCount: 2, removedStudentCount: 1, createdAt: "2026-09-01", showGradeDistribution: false, version: 3 };
const student: EnrollmentView = { accountId: "student-1", name: "Nguyễn Văn An", email: "an@example.test", enrolledAt: "2026-09-05", status: "ACTIVE" };

describe("U05 reference UI", () => {
  it("filters manager classes by status and text without losing the source rows", () => {
    const onOpen = vi.fn();
    render(<ClassListPage classes={[course, { ...course, id: "class-2", code: "SE1901", status: "DRAFT", teacher: null }]} subjects={[subject]} onOpen={onOpen} onCreate={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: /Nháp/ }));
    expect(screen.queryByRole("button", { name: "Xem lớp SE1801" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Xem lớp SE1901" }));
    expect(onOpen).toHaveBeenCalledWith("class-2");
    fireEvent.click(screen.getByRole("button", { name: /Tất cả/ }));
    fireEvent.change(screen.getByRole("searchbox"), { target: { value: "không có" } });
    expect(screen.getByText("Không tìm thấy lớp phù hợp")).toBeInTheDocument();
  });

  it("hides draft classes for students and keeps archived cards read-only", () => {
    const onOpen = vi.fn();
    render(<ClassDashboardPage audience="student" classes={[course, { ...course, id: "draft", status: "DRAFT" }, { ...course, id: "archived", code: "SE1701", status: "ARCHIVED" }]} onOpen={onOpen} />);
    expect(screen.getAllByText("SE1801")).toHaveLength(1);
    fireEvent.click(screen.getByRole("button", { name: /Đã kết thúc/ }));
    expect(screen.getByText("SE1701")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Xem lớp/ })).not.toBeInTheDocument();
    expect(onOpen).not.toHaveBeenCalled();
  });

  it("creates a draft with an optional teacher and normalized text", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<ClassFormDialog subject={subject} teachers={[teacher]} onSubmit={onSubmit} onClose={vi.fn()} />);
    fireEvent.change(screen.getByLabelText(/Mã lớp/), { target: { value: " SE1902 " } });
    fireEvent.change(screen.getByLabelText(/Tên lớp/), { target: { value: " Lớp mới " } });
    fireEvent.change(screen.getByLabelText(/Học kỳ/), { target: { value: "2026-2" } });
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Tạo lớp" }));
    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({ subjectId: subject.id, code: "SE1902", name: "Lớp mới", term: "2026-2", description: "", teacherId: null }));
  });

  it("preserves unsaved info and displays version conflict", async () => {
    const onSave = vi.fn().mockRejectedValue(new Error("Dữ liệu đã thay đổi. Vui lòng tải lại."));
    render(<ClassInfoForm value={course} onSave={onSave} />);
    fireEvent.change(screen.getByLabelText(/Tên lớp/), { target: { value: "Lớp mới" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
    await waitFor(() => expect(onSave).toHaveBeenCalledWith({ name: "Lớp mới", description: course.description, term: course.term, version: 3 }));
    expect(await screen.findByRole("alert")).toHaveTextContent("tải lại");
    expect(screen.getByLabelText(/Tên lớp/)).toHaveValue("Lớp mới");
  });

  it("does not offer editing for archived classes", () => {
    render(<ClassInfoForm value={{ ...course, status: "ARCHIVED" }} onSave={vi.fn()} />);
    expect(screen.getByLabelText(/Tên lớp/)).toBeDisabled();
    expect(screen.queryByRole("button", { name: "Lưu thay đổi" })).not.toBeInTheDocument();
  });

  it("refreshes unsaved info when a newer server version arrives", () => {
    const { rerender } = render(<ClassInfoForm value={course} onSave={vi.fn()} />);
    fireEvent.change(screen.getByLabelText(/Tên lớp/), { target: { value: "Chưa lưu" } });
    rerender(<ClassInfoForm value={{ ...course, name: "Mới từ server", version: 4 }} onSave={vi.fn()} />);
    expect(screen.getByLabelText(/Tên lớp/)).toHaveValue("Mới từ server");
  });

  it("shows server enrollment conflicts with their original row number", () => {
    render(<EnrollmentResultTable rows={[{ line: 4, email: "an@example.test", result: "IN_OTHER_CLASS" }]} />);
    const row = screen.getByText("an@example.test").closest("tr")!;
    expect(within(row).getByText("4")).toBeInTheDocument();
    expect(within(row).getByText("Đang ở lớp khác cùng môn")).toBeInTheDocument();
  });

  it("rejects oversized CSV files before reading or submitting them", () => {
    const onBatch = vi.fn();
    render(<EnrollmentPanel students={[]} editable onBatch={onBatch} />);
    const file = new File(["a".repeat(102_401)], "list.csv", { type: "text/csv" });
    const read = vi.fn();
    Object.defineProperty(file, "text", { value: read });
    fireEvent.change(screen.getByLabelText("Chọn tệp CSV một cột"), { target: { files: [file] } });
    expect(screen.getByRole("alert")).toHaveTextContent("100 KB");
    expect(read).not.toHaveBeenCalled();
    expect(onBatch).not.toHaveBeenCalled();
  });

  it("blocks batch submission when every preview email is invalid", () => {
    const onBatch = vi.fn();
    render(<EnrollmentPanel students={[]} editable onBatch={onBatch} />);
    fireEvent.change(screen.getByLabelText(/Dán email/), { target: { value: "invalid" } });
    fireEvent.click(screen.getByRole("button", { name: "Xem trước" }));
    expect(screen.getByRole("button", { name: "Thêm vào lớp" })).toBeDisabled();
    expect(onBatch).not.toHaveBeenCalled();
  });

  it("requires confirmation for archiving and sends the version", async () => {
    const onChange = vi.fn().mockResolvedValue(undefined);
    render(<ClassStateActions value={course} onChange={onChange} />);
    fireEvent.click(screen.getByRole("button", { name: "Lưu trữ lớp" }));
    expect(onChange).not.toHaveBeenCalled();
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Lưu trữ lớp" }));
    await waitFor(() => expect(onChange).toHaveBeenCalledWith("ARCHIVED", 3));
  });

  it("blocks clearing the teacher of an open class", () => {
    render(<AssignTeacherDialog value={course} teachers={[teacher]} onAssign={vi.fn()} onClose={vi.fn()} />);
    expect(screen.getByRole("radio", { name: "Chưa phân công" })).toBeDisabled();
  });

  it("does not allow saving an unchanged teacher assignment absent from active candidates", () => {
    const onAssign = vi.fn();
    render(<AssignTeacherDialog value={course} teachers={[]} onAssign={onAssign} onClose={vi.fn()} />);
    expect(screen.getByRole("radio", { name: teacher.name })).toBeChecked();
    expect(screen.getByRole("button", { name: "Lưu phân công" })).toBeDisabled();
    expect(onAssign).not.toHaveBeenCalled();
  });

  it("requires confirmation before removing an enrollment", async () => {
    const onStatusChange = vi.fn().mockResolvedValue(undefined);
    render(<EnrollmentPanel students={[student]} editable onStatusChange={onStatusChange} />);
    fireEvent.click(screen.getByRole("button", { name: "Gỡ Nguyễn Văn An" }));
    expect(onStatusChange).not.toHaveBeenCalled();
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Gỡ sinh viên" }));
    await waitFor(() => expect(onStatusChange).toHaveBeenCalledWith("student-1", "REMOVED"));
  });

  it("never offers manager controls on a teacher class page", () => {
    render(<ReadOnlyClassPage audience="teacher" value={course} students={[student]} onBack={vi.fn()} />);
    expect(screen.queryByRole("button", { name: /Phân công/ })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("tab", { name: /Sinh viên/ }));
    expect(screen.queryByRole("button", { name: /Gỡ/ })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Xem Nguyễn Văn An" }));
    expect(screen.getByRole("dialog")).toHaveTextContent("an@example.test");
  });

  it("does not show removed enrollments to a teacher", () => {
    render(<ReadOnlyClassPage audience="teacher" value={course} students={[student, { ...student, accountId: "removed", name: "Đã gỡ", status: "REMOVED" }]} onBack={vi.fn()} />);
    fireEvent.click(screen.getByRole("tab", { name: /Sinh viên/ }));
    expect(screen.queryByText("Đã gỡ")).not.toBeInTheDocument();
  });

  it("does not present group membership as empty before group data has loaded", () => {
    render(<ReadOnlyClassPage audience="teacher" value={course} students={[student]} onBack={vi.fn()} />);
    fireEvent.click(screen.getByRole("tab", { name: /Sinh viên/ }));
    expect(screen.queryByRole("columnheader", { name: "Nhóm" })).not.toBeInTheDocument();
  });

  it("keeps a selected subject when returning to its classes", () => {
    const other = { ...subject, id: "subject-2", code: "SWR302" };
    render(<ClassListPage classes={[course]} subjects={[subject, other]} initialSubjectId={other.id} onOpen={vi.fn()} onCreate={vi.fn()} />);
    expect(screen.getByRole("combobox", { name: "Môn học" })).toHaveValue(other.id);
    expect(screen.queryByRole("button", { name: "Xem lớp SE1801" })).not.toBeInTheDocument();
  });

  it("allows managers to read removed enrollments in an archived class without mutation controls", () => {
    render(<EnrollmentPanel students={[{ ...student, status: "REMOVED" }]} includeRemoved />);
    fireEvent.click(screen.getByRole("button", { name: /Đã gỡ/ }));
    expect(screen.getByText(student.name)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Thêm lại/ })).not.toBeInTheDocument();
  });

  it("invalidates CSV preview after editing and waits for confirmation before enrollment", async () => {
    const onBatch = vi.fn().mockResolvedValue([{ line: 1, email: "an@example.test", result: "ENROLLED" }]);
    render(<EnrollmentPanel students={[]} editable onBatch={onBatch} />);
    const input = screen.getByLabelText(/Dán email/);
    fireEvent.change(input, { target: { value: "an@example.test\nbad" } });
    fireEvent.click(screen.getByRole("button", { name: "Xem trước" }));
    expect(screen.getByText("Email không hợp lệ")).toBeInTheDocument();
    expect(onBatch).not.toHaveBeenCalled();
    fireEvent.change(input, { target: { value: "an@example.test" } });
    expect(screen.queryByRole("button", { name: "Thêm vào lớp" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Xem trước" }));
    fireEvent.click(screen.getByRole("button", { name: "Thêm vào lớp" }));
    await waitFor(() => expect(onBatch).toHaveBeenCalledWith("an@example.test"));
    expect(await screen.findByText("Đã thêm")).toBeInTheDocument();
  });

  it("requires confirmation before enabling grade distribution", async () => {
    const onDistribution = vi.fn().mockResolvedValue(undefined);
    const action = vi.fn().mockResolvedValue(undefined);
    render(<ManagedClassPage value={course} students={[]} teachers={[]} candidates={[]} onBack={vi.fn()} actions={{ onSaveInfo: action, onAssign: action, onChangeStatus: action, onDistribution, onEnrollmentStatus: action, onEnroll: action, onBatch: vi.fn().mockResolvedValue([]) }} />);
    fireEvent.click(screen.getByRole("switch"));
    expect(onDistribution).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận bật" }));
    await waitFor(() => expect(onDistribution).toHaveBeenCalledWith(true, 3));
  });
});

describe("enrollment CSV preview", () => {
  it("rejects leading, trailing and repeated dots in the local part", () => {
    const rows = parseEnrollmentList(".a@example.test\na.@example.test\na..b@example.test\na.b@example.test");
    expect(rows.map((row) => row.error)).toEqual(["INVALID_EMAIL", "INVALID_EMAIL", "INVALID_EMAIL", null]);
  });
  it("preserves row numbers and flags malformed or duplicate rows", () => {
    expect(parseEnrollmentList('\uFEFFemail\r\n"A@example.test"\r\n\r\na@example.test\r\nbad')).toEqual([
      { line: 2, email: "a@example.test", error: null }, { line: 4, email: "a@example.test", error: "DUPLICATE_IN_LIST" }, { line: 5, email: "bad", error: "INVALID_EMAIL" },
    ]);
  });
  it("enforces byte and row limits", () => {
    expect(() => parseEnrollmentList("đ".repeat(51_201))).toThrow("100 KB");
    expect(() => parseEnrollmentList(Array.from({ length: 201 }, (_, i) => `s${i}@example.test`).join("\n"))).toThrow("200");
    expect(() => parseEnrollmentList("\n ")).toThrow();
  });
});
