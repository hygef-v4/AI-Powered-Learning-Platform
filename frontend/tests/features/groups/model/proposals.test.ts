import { describe, expect, it } from "vitest";
import {
  canSubmit,
  describeFormation,
  fromLocalInput,
  toLocalInput,
  validateFormation,
  validateProposalName,
} from "@/features/groups/model/proposals";
import type { FormationSummary, ProposalView } from "@/features/groups/model/types";

const OPEN: FormationSummary = { enabled: true, minSize: 2, maxSize: 4, deadline: null, open: true, closedReason: null };

const proposal = (members: number, status: ProposalView["status"] = "DRAFT"): ProposalView => ({
  id: "p1",
  name: "Nhóm Mây",
  status,
  leaderId: "a",
  leader: true,
  version: 1,
  members: Array.from({ length: members }, (_, i) => ({ accountId: `m${i}`, name: `SV ${i}`, email: null, leader: i === 0, expired: false })),
  invitations: [],
  freeSeats: 4 - members,
  submittedAt: null,
});

describe("đề xuất nhóm", () => {
  it("tóm tắt đợt theo trạng thái mở, hạn chót và lý do đóng", () => {
    expect(describeFormation(OPEN)).toBe("Đang mở, nhóm từ 2 đến 4 người.");
    expect(describeFormation({ ...OPEN, deadline: "2026-11-20T03:00:00Z" })).toContain("Đang mở đến 10:00 20/11/2026");
    expect(describeFormation({ ...OPEN, open: false, closedReason: "DISABLED" })).toBe("Đang tắt.");
    expect(describeFormation({ ...OPEN, open: false, closedReason: "CLASS_NOT_OPEN" })).toContain("Lớp chưa mở");
    expect(describeFormation({ ...OPEN, open: false, closedReason: "DEADLINE_PASSED", deadline: "2026-11-20T03:00:00Z" }))
      .toContain("Đã hết hạn");
  });

  it("chỉ gửi duyệt được khi đợt mở, đề xuất nháp và đủ tối thiểu", () => {
    expect(canSubmit(proposal(2), OPEN)).toBe(true);
    expect(canSubmit(proposal(1), OPEN)).toBe(false);
    expect(canSubmit(proposal(2, "SUBMITTED"), OPEN)).toBe(false);
    expect(canSubmit(proposal(2), { ...OPEN, open: false, closedReason: "DEADLINE_PASSED" })).toBe(false);
  });

  it("tên nhóm cắt khoảng trắng và cần 1–100 ký tự", () => {
    expect(validateProposalName("  Nhóm Mây ")).toEqual({ name: "Nhóm Mây", error: null });
    expect(validateProposalName("   ").error).not.toBeNull();
    expect(validateProposalName("a".repeat(101)).error).not.toBeNull();
  });

  it("form cài đặt báo lỗi đúng ô", () => {
    const now = new Date("2026-11-11T08:00:00Z");
    expect(validateFormation({ enabled: true, minSize: "2", maxSize: "4", deadline: "" }, now)).toEqual({});
    expect(validateFormation({ enabled: true, minSize: "1", maxSize: "4", deadline: "" }, now)).toHaveProperty("minSize");
    expect(validateFormation({ enabled: true, minSize: "5", maxSize: "4", deadline: "" }, now)).toHaveProperty("maxSize");
    expect(validateFormation({ enabled: true, minSize: "2", maxSize: "2.5", deadline: "" }, now)).toHaveProperty("maxSize");
    expect(validateFormation({ enabled: true, minSize: "2", maxSize: "4", deadline: "2026-11-11T14:00" }, now))
      .toHaveProperty("deadline");
    expect(validateFormation({ enabled: false, minSize: "2", maxSize: "4", deadline: "2026-11-11T14:00" }, now)).toEqual({});
  });

  it("đổi hạn chót qua lại giữa ISO và giờ Việt Nam", () => {
    expect(toLocalInput("2026-11-20T03:00:00Z")).toBe("2026-11-20T10:00");
    expect(fromLocalInput("2026-11-20T10:00")).toBe("2026-11-20T03:00:00.000Z");
    expect(fromLocalInput("")).toBeNull();
  });
});
