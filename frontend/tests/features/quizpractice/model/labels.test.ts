import { describe, expect, it } from "vitest";
import { describePenaltyResult, describePenaltyRule, describeSignal } from "@/features/quizpractice/model/labels";

describe("nhãn rời trang", () => {
  it("báo quy tắc cho sinh viên, nói rõ giảng viên quyết định", () => {
    expect(describePenaltyRule({ freeLeaves: 1, pointsPerLeave: 0.5, maxPenalty: 3, minAwaySeconds: 5 }))
      .toBe("Được miễn 1 lần đầu rời trang; sau đó mỗi lần có thể bị trừ 0,5 điểm, tối đa 3 điểm, do giảng viên quyết định khi xem lượt làm. Chỉ tính khi rời từ 5 giây trở lên.");
    expect(describePenaltyRule({ freeLeaves: 0, pointsPerLeave: 1, maxPenalty: null, minAwaySeconds: 0 }))
      .toBe("Mỗi lần rời trang có thể bị trừ 1 điểm, do giảng viên quyết định khi xem lượt làm.");
  });

  it("kết quả của sinh viên theo quyết định của giảng viên", () => {
    const base = { rawScore: 8, suggestedPenalty: null, penaltyPoints: 0, leaveCount: 4, decisionPending: false, decision: null, decisionReason: null };
    expect(describePenaltyResult({ ...base, leaveCount: 0 })).toBeNull();
    expect(describePenaltyResult(base)).toBe("Rời trang 4 lần.");
    expect(describePenaltyResult({ ...base, decisionPending: true })).toBe("Rời trang 4 lần · chưa bị trừ điểm, giảng viên sẽ xem xét.");
    expect(describePenaltyResult({ ...base, decision: "APPLIED", penaltyPoints: 1.5, decisionReason: "Rời nhiều" }))
      .toBe("Điểm gốc 8 · rời trang 4 lần · giảng viên trừ 1,5 điểm: Rời nhiều");
    expect(describePenaltyResult({ ...base, decision: "DISMISSED", decisionReason: "Lỗi máy" }))
      .toBe("Rời trang 4 lần · giảng viên quyết định không trừ điểm: Lỗi máy");
  });

  it("chỉ gắn cờ khi thiếu tín hiệu", () => {
    expect(describeSignal("OK")).toBeNull();
    expect(describeSignal("DISABLED")).toBeNull();
    expect(describeSignal(null)).toBeNull();
    expect(describeSignal("NO_SIGNAL")).toBe("Không nhận được tín hiệu");
    expect(describeSignal("SIGNAL_LOST")).toBe("Mất tín hiệu một phần");
  });
});
