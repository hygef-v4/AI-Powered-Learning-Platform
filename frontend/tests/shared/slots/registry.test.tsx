import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { createRegistry, createSlot } from "@/shared/slots/registry";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";

describe("registry", () => {
  it("sắp theo order rồi theo key, đăng ký lại cùng key thì thay", () => {
    const registry = createRegistry<{ key: string; order?: number; title: string }>("test");
    registry.register({ key: "b", order: 2, title: "B" });
    registry.register({ key: "a", order: 2, title: "A" });
    registry.register({ key: "c", order: 1, title: "C" });
    registry.register({ key: "a", order: 2, title: "A2" });

    expect(registry.entries().map((e) => e.title)).toEqual(["C", "A2", "B"]);
  });

  it("SlotOutlet vẽ component đã đăng ký với props của slot", () => {
    const slot = createSlot<{ classId: string }>("StudentClassSection");
    slot.register({ key: "x", component: ({ classId }) => <p>Lớp {classId}</p> });

    render(<SlotOutlet slot={slot} props={{ classId: "c1" }} />);
    expect(screen.getByText("Lớp c1")).toBeInTheDocument();
  });
});
