import { act, cleanup, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useTheme } from "@/shared/theme";
import { THEME_BOOT_SCRIPT, THEME_KEY } from "@/shared/themeBoot";

type SchemeListener = (event: { matches: boolean }) => void;

function stubSystemScheme(dark: boolean) {
  const schemeListeners = new Set<SchemeListener>();
  vi.stubGlobal("matchMedia", vi.fn().mockReturnValue({
    matches: dark,
    addEventListener: (_: string, listener: SchemeListener) => schemeListeners.add(listener),
    removeEventListener: (_: string, listener: SchemeListener) => schemeListeners.delete(listener),
  }));
  return (matches: boolean) => schemeListeners.forEach((listener) => listener({ matches }));
}

function runBootScript() {
  new Function(THEME_BOOT_SCRIPT)();
}

beforeEach(() => {
  localStorage.removeItem(THEME_KEY);
  delete document.documentElement.dataset.theme;
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("theme", () => {
  it("chưa chọn theme thì script khởi động theo hệ điều hành", () => {
    stubSystemScheme(true);
    runBootScript();
    expect(document.documentElement.dataset.theme).toBe("dark");

    stubSystemScheme(false);
    runBootScript();
    expect(document.documentElement.dataset.theme).toBe("light");
  });

  it("đã chọn theme thì giữ lựa chọn dù hệ điều hành khác", () => {
    stubSystemScheme(true);
    localStorage.setItem(THEME_KEY, "light");
    runBootScript();
    expect(document.documentElement.dataset.theme).toBe("light");
  });

  it("đi theo hệ điều hành khi nó đổi, tới khi người dùng tự bấm đổi theme", () => {
    const changeSystemScheme = stubSystemScheme(false);
    runBootScript();
    const { result } = renderHook(() => useTheme());
    expect(result.current[0]).toBe("light");

    act(() => changeSystemScheme(true));
    expect(result.current[0]).toBe("dark");

    act(() => result.current[1]());
    expect(result.current[0]).toBe("light");
    expect(localStorage.getItem(THEME_KEY)).toBe("light");

    act(() => changeSystemScheme(true));
    expect(result.current[0]).toBe("light");
  });
  it("gỡ listener hệ điều hành trên đúng MediaQueryList đã gắn, kể cả khi các hook huỷ không theo thứ tự", () => {
    const attached: Set<unknown>[] = [];
    vi.stubGlobal("matchMedia", vi.fn(() => {
      // Mỗi lần gọi trả một đối tượng mới, như trình duyệt thật.
      const listeners = new Set<unknown>();
      attached.push(listeners);
      return {
        matches: false,
        addEventListener: (_: string, listener: unknown) => listeners.add(listener),
        removeEventListener: (_: string, listener: unknown) => listeners.delete(listener),
      };
    }));
    const first = renderHook(() => useTheme());
    const second = renderHook(() => useTheme());
    first.unmount();
    second.unmount();
    expect(attached.reduce((total, listeners) => total + listeners.size, 0)).toBe(0);
  });
});
