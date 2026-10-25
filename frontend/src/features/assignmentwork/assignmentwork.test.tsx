import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { formatDuration } from "@/shared/activity/ActivityTimeline";
import { HEARTBEAT_MS, useActivityTracker } from "@/shared/activity/useActivityTracker";
import { computeParts, type DocumentModel } from "@/shared/document";
import { insertIntoPart, precheck, submitCheck } from "./AssignmentWorkspacePage";
import { countdown, remaining } from "./format";
import type { StudentRow } from "./types";
import { upcoming } from "./useStudentAssignments";

describe("precheck", () => {
  const teacher: DocumentModel["blocks"][number] = { id: "t", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Đề" }] } };
  it("báo thiếu sơ đồ bắt buộc và bài chưa có nội dung", () => {
    const issues = precheck({ requiredDiagrams: { CLASS: 1 } }, { blocks: [teacher] });
    expect(issues.map((i) => i.code)).toEqual(["REQUIRED_DIAGRAMS_MISSING", "SUBMISSION_EMPTY"]);
  });
  it("hợp lệ khi có sơ đồ đúng loại", () => {
    const content: DocumentModel = { blocks: [teacher, { id: "d", origin: "STUDENT", type: "DIAGRAM", content: { diagramType: "CLASS", xml: "<mxfile/>" } }] };
    expect(precheck({ requiredDiagrams: { CLASS: 1 } }, content)).toEqual([]);
  });
  it("sơ đồ của giảng viên đã vẽ (có ảnh xem trước) tính là bài làm", () => {
    const slot = (svgPreview?: string): DocumentModel["blocks"][number] => ({ id: "s", origin: "TEACHER", type: "DIAGRAM", content: { diagramType: "CLASS", xml: "<mxfile/>", svgPreview } });
    expect(precheck({ requiredDiagrams: { CLASS: 1 } }, { blocks: [teacher, slot()] }).map((i) => i.code)).toEqual(["REQUIRED_DIAGRAMS_MISSING", "SUBMISSION_EMPTY"]);
    expect(precheck({ requiredDiagrams: { CLASS: 1 } }, { blocks: [teacher, slot("<svg/>")] })).toEqual([]);
  });
  it("loại sơ đồ do server suy từ hình vẽ: máy khách chỉ kiểm tổng số sơ đồ, không chặn sơ đồ chưa có loại mới", () => {
    const drawn = (id: string): DocumentModel["blocks"][number] => ({ id, origin: "STUDENT", type: "DIAGRAM", content: { diagramType: "OTHER", xml: "<mxfile/>" } });
    expect(precheck({ requiredDiagrams: { USE_CASE: 1, CLASS: 1 } }, { blocks: [teacher, drawn("d1"), drawn("d2")] })).toEqual([]);
    const issues = precheck({ requiredDiagrams: { USE_CASE: 1, CLASS: 1 } }, { blocks: [teacher, drawn("d1")] });
    expect(issues.map((i) => i.code)).toEqual(["REQUIRED_DIAGRAMS_MISSING"]);
    expect(issues[0]!.message).toContain("Cần ít nhất 2 sơ đồ");
  });
  it("bảng của giảng viên có chữ tính như đã điền (server kiểm chính xác)", () => {
    const table: DocumentModel["blocks"][number] = { id: "tb", origin: "TEACHER", type: "TABLE", content: { rows: [[{ runs: [{ text: "Đăng nhập" }] }]] } };
    expect(precheck({ requiredDiagrams: {} }, { blocks: [teacher, table] })).toEqual([]);
  });
});

describe("submitCheck", () => {
  const teacher: DocumentModel["blocks"][number] = { id: "t", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Đề" }] } };
  const drawnByTeacher: DocumentModel["blocks"][number] = { id: "s", origin: "TEACHER", type: "DIAGRAM", content: { diagramType: "OTHER", xml: "<mxfile/>", svgPreview: "<svg/>" } };
  const empty = { code: "SUBMISSION_EMPTY", blockId: null, message: "Bài làm chưa có nội dung của bạn" };

  it("có kết quả của server thì dùng, kể cả khi tự kiểm thấy sơ đồ của đề đã có hình", () => {
    const content: DocumentModel = { blocks: [teacher, drawnByTeacher] };
    expect(precheck({ requiredDiagrams: {} }, content)).toEqual([]);
    expect(submitCheck({ requiredDiagrams: {} }, content, [empty])).toEqual([empty]);
    expect(submitCheck({ requiredDiagrams: {} }, content, [])).toEqual([]);
  });
  it("chưa có kết quả của server thì tự kiểm sơ bộ", () => {
    expect(submitCheck({ requiredDiagrams: {} }, { blocks: [teacher] }, null).map((i) => i.code)).toEqual(["SUBMISSION_EMPTY"]);
  });
});

describe("insertIntoPart", () => {
  const heading = (id: string, style: "H1" | "H2", text: string): DocumentModel["blocks"][number] => ({ id, origin: "TEACHER", type: "HEADING", content: { style, text } });
  const para = (id: string): DocumentModel["blocks"][number] => ({ id, origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: id }] } });
  const content: DocumentModel = { blocks: [heading("a", "H1", "A"), para("a-text"), heading("b", "H1", "B"), para("b-text")] };
  const parts = computeParts(content);
  const added = [{ id: "new", origin: "STUDENT" as const, type: "PARAGRAPH" as const, content: { runs: [{ text: "nhập" }] } }];

  it("chèn vào cuối phần đang chọn; không chọn phần thì thêm vào cuối bài", () => {
    expect(insertIntoPart(content, parts, "a", added).blocks.map((b) => b.id)).toEqual(["a", "a-text", "new", "b", "b-text"]);
    expect(insertIntoPart(content, parts, null, added).blocks.map((b) => b.id)).toEqual(["a", "a-text", "b", "b-text", "new"]);
  });
});

describe("danh sách bài", () => {
  const row = (patch: Partial<StudentRow>): StudentRow => ({
    assignmentId: "a", classId: "c", classCode: "C1", className: "Lớp", scope: "CLASS", title: "Bài", submissionMode: "INDIVIDUAL",
    gradingMode: "GRADED", diagramEnabled: false, status: "OPEN", opensAt: null, closesAt: null, lateUntil: null, maxAttempts: 1,
    myStatus: "NOT_STARTED", attemptCount: 0, submittedCount: 0, inProgressAttemptId: null, lastSubmittedAt: null, group: null, ...patch,
  });
  it("sắp đến hạn là bài đang mở chưa nộp", () => {
    expect(upcoming(row({}))).toBe(true);
    expect(upcoming(row({ myStatus: "SUBMITTED" }))).toBe(false);
    expect(upcoming(row({ status: "CLOSED" }))).toBe(false);
  });
  it("đếm ngược tới hạn", () => {
    const now = Date.parse("2026-10-20T00:00:00Z");
    expect(remaining("2026-10-21T02:30:00Z", now)).toBe("1 ngày 2 giờ");
    expect(remaining("2026-10-20T00:10:00Z", now)).toBe("10 phút");
    expect(remaining("2026-10-19T00:00:00Z", now)).toBe("Đã quá hạn");
  });
  it("dòng đếm ngược trên trang làm bài không ghi 'Còn Đã quá hạn'", () => {
    const now = Date.parse("2026-10-20T00:00:00Z");
    expect(countdown("2026-10-20T00:10:00Z", now)).toBe("Còn 10 phút");
    expect(countdown("2026-10-19T00:00:00Z", now)).toBe("Đã quá hạn");
  });
  it("định dạng thời lượng ISO", () => {
    expect(formatDuration("PT1M5S")).toBe("1 phút 5 giây");
    expect(formatDuration("PT0S")).toBe("0 giây");
  });
});

describe("useActivityTracker", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("gửi SESSION_START, HIDDEN khi rời trang, HEARTBEAT mỗi 60 giây; tắt thì không gửi gì", async () => {
    const send = vi.fn().mockResolvedValue(undefined);
    const { unmount } = renderHook(() => useActivityTracker({ enabled: true, send }));
    expect(send.mock.calls[0]![0][0].kind).toBe("SESSION_START");
    await act(async () => { await Promise.resolve(); });
    await act(async () => {
      Object.defineProperty(document, "visibilityState", { value: "hidden", configurable: true });
      document.dispatchEvent(new Event("visibilitychange"));
    });
    expect(send.mock.calls.flatMap((c) => c[0]).map((e: { kind: string }) => e.kind)).toContain("HIDDEN");
    await act(async () => { await vi.advanceTimersByTimeAsync(HEARTBEAT_MS + 10_000); });
    expect(send.mock.calls.flatMap((c) => c[0]).map((e: { kind: string }) => e.kind)).toContain("HEARTBEAT");
    unmount();
    const off = vi.fn();
    renderHook(() => useActivityTracker({ enabled: false, send: off }));
    expect(off).not.toHaveBeenCalled();
    Object.defineProperty(document, "visibilityState", { value: "visible", configurable: true });
  });
});
