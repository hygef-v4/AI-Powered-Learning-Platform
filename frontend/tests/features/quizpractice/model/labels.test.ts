import { describe, expect, it } from "vitest";
import { describeSignal } from "@/features/quizpractice/model/labels";

describe("nhãn rời trang", () => {

  it("chỉ gắn cờ khi thiếu tín hiệu", () => {
    expect(describeSignal("OK")).toBeNull();
    expect(describeSignal("DISABLED")).toBeNull();
    expect(describeSignal(null)).toBeNull();
    expect(describeSignal("NO_SIGNAL")).toBe("Không nhận được tín hiệu");
    expect(describeSignal("SIGNAL_LOST")).toBe("Mất tín hiệu một phần");
  });
});
