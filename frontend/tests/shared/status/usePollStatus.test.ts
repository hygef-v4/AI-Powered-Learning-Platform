import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { usePollStatus } from "@/shared/status/usePollStatus";

describe("usePollStatus", () => {
  it("dừng poll khi tới trạng thái cuối", async () => {
    const fetcher = vi.fn().mockResolvedValueOnce("QUEUED").mockResolvedValueOnce("READY").mockResolvedValue("X");
    const { result } = renderHook(() => usePollStatus(fetcher, (s) => s === "READY", 10));

    await waitFor(() => expect(result.current.data).toBe("READY"));
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(fetcher).toHaveBeenCalledTimes(2);
  });
});
