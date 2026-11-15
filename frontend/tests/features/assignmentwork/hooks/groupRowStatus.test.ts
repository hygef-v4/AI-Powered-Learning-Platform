import { expect, it } from "vitest";
import { doing, rowStatus, upcoming } from "@/features/assignmentwork/hooks/useStudentAssignments";
import type { StudentRow } from "@/features/assignmentwork/model/types";

const groupRow = (group: StudentRow["group"]) => ({ status: "OPEN", myStatus: "GROUP", group }) as StudentRow;

it("bài nhóm: nộp xong là Đã nộp, sửa sau khi nộp thì lại Đang làm", () => {
  expect(rowStatus(groupRow({ groupDocumentId: "d", submitted: false, submittedAt: null })).label).toBe("Đang làm");
  expect(rowStatus(groupRow({ groupDocumentId: "d", submitted: true, submittedAt: "2026-11-12T01:00:00Z" })).label).toBe("Đã nộp");
  expect(rowStatus(groupRow({ groupDocumentId: "d", submitted: true, submittedAt: "2026-11-12T01:00:00Z", changedSinceSubmit: true })).label)
    .toBe("Đang làm");
});

it("bài nhóm đã nộp không nằm ở Chưa làm; sửa sau khi nộp thì lại là bài cần làm", () => {
  expect(upcoming(groupRow({ groupDocumentId: null, submitted: false, submittedAt: null }))).toBe(true);
  expect(upcoming(groupRow({ groupDocumentId: "d", submitted: true, submittedAt: "2026-11-12T01:00:00Z" }))).toBe(false);
  expect(upcoming(groupRow({ groupDocumentId: "d", submitted: true, submittedAt: "2026-11-12T01:00:00Z", changedSinceSubmit: true }))).toBe(true);
});

it.each([null, "d"])("bài nhóm đã đóng chưa nộp hiện Hết hạn thay cho Chưa làm, document=%s", groupDocumentId => {
  const closed = { ...groupRow({ groupDocumentId, submitted: false, submittedAt: null }), status: "CLOSED", submittedCount: 0 } as StudentRow;
  expect(rowStatus(closed)).toEqual({ label: "Hết hạn", tone: "danger" });
  expect(upcoming(closed)).toBe(false);
  expect(doing(closed)).toBe(false);
});
