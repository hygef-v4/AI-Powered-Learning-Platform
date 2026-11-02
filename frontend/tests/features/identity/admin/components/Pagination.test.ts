import { describe, expect, it } from "vitest";
import { pageWindow } from "@/features/identity/admin/components/Pagination";

describe("pageWindow", () => {
  it("giữ trang đầu, cuối, hai bên trang đang mở; khoảng trống thành gap", () => {
    expect(pageWindow(0, 1)).toEqual([0]);
    expect(pageWindow(0, 179)).toEqual([0, 1, "gap", 178]);
    expect(pageWindow(50, 179)).toEqual([0, "gap", 49, 50, 51, "gap", 178]);
    expect(pageWindow(2, 5)).toEqual([0, 1, 2, 3, 4]);
  });
});
