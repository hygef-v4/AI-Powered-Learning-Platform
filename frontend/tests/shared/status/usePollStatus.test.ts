import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { usePollStatus } from "@/shared/status/usePollStatus";

describe("usePollStatus", () => {
  it("dừng poll khi tới trạng thái cuối", async () => {
    const fetcher = vi.fn().mockResolvedValueOnce("QUEUED").mockResolvedValueOnce("READY").mockResolvedValue("X");
    const { result } = renderHook(() => usePollStatus(fetcher, (s) => s === "READY", 10));

    await waitFor(() => expect(result.current.data).toBe("READY"));
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(fetcher).toHaveBeenCalledTimes(2);
  });

  it("thử lại sau lỗi mạng tạm thời rồi xóa lỗi khi có kết quả", async () => {
    const fetcher = vi.fn().mockRejectedValueOnce(new TypeError("Failed to fetch")).mockResolvedValue("READY");
    const { result } = renderHook(() => usePollStatus(fetcher, (s) => s === "READY", 10));

    await waitFor(() => expect(result.current.data).toBe("READY"));
    expect(result.current.error).toBeNull();
    expect(fetcher).toHaveBeenCalledTimes(2);
  });

  it("dừng ở lỗi 4xx như không tìm thấy", async () => {
    const fetcher = vi.fn().mockRejectedValue(new ApiError({ status: 404 }));
    const { result } = renderHook(() => usePollStatus(fetcher, () => false, 10));

    await waitFor(() => expect(result.current.error).toBeInstanceOf(ApiError));
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(fetcher).toHaveBeenCalledTimes(1);
  });
});
