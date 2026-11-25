import { describe, expect, it } from "vitest";
import {
  canSubmit,
  convertFromLocalInput,
  convertToLocalInput,
  validateFormation,
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

  it("chỉ gửi duyệt được khi đợt mở, đề xuất nháp và đủ tối thiểu", () => {
    expect(canSubmit(proposal(2), OPEN)).toBe(true);
    expect(canSubmit(proposal(1), OPEN)).toBe(false);
    expect(canSubmit(proposal(2, "SUBMITTED"), OPEN)).toBe(false);
    expect(canSubmit(proposal(2), { ...OPEN, open: false, closedReason: "DEADLINE_PASSED" })).toBe(false);
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
    expect(convertToLocalInput("2026-11-20T03:00:00Z")).toBe("2026-11-20T10:00");
    expect(convertFromLocalInput("2026-11-20T10:00")).toBe("2026-11-20T03:00:00.000Z");
    expect(convertFromLocalInput("")).toBeNull();
  });
});
