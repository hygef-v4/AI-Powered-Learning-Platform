import { expect, it } from "vitest";
import { ApiError } from "@/shared/api/client";
import { formatDate, formatOperationError } from "@/features/academics/model/format";

it("uses the Vietnam calendar date and keeps the invalid-date fallback", () => {
  expect(formatDate("2026-11-04T18:00:00Z")).toBe("5/11/2026");
  expect(formatDate("bad-date")).toBe("—");
});

it("keeps conflict copy, hides API internals and preserves local errors", () => {
  expect(formatOperationError(new ApiError({status: 409, detail: "secret SQL"}))).toBe("Dữ liệu đã thay đổi. Vui lòng tải lại trước khi tiếp tục.");
  expect(formatOperationError(new ApiError({status: 500, detail: "secret SQL"}))).toBe("Không thể hoàn tất thao tác. Vui lòng thử lại.");
  expect(formatOperationError(new Error("Local validation"))).toBe("Local validation");
  expect(formatOperationError(null)).toBe("Không thể hoàn tất thao tác. Vui lòng thử lại.");
});
