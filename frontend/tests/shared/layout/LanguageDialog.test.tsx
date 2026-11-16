import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

const chooseLocale = vi.hoisted(() => vi.fn());
vi.mock("@/i18n/locales", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/i18n/locales")>(), chooseLocale,
}));

import { LanguageDialog } from "@/shared/layout/LanguageDialog";

describe("LanguageDialog", () => {
  afterEach(() => { vi.useRealTimers(); chooseLocale.mockReset(); });

  it("hiện ba ngôn ngữ dạng radio, ngôn ngữ đang dùng được chọn sẵn", () => {
    render(<LanguageDialog open onClose={vi.fn()} />);
    const radios = screen.getAllByRole("radio");
    expect(radios.map((radio) => radio.closest("label")?.textContent)).toEqual(["Tiếng Việt", "English", "日本語"]);
    expect(screen.getByRole("radio", { name: "Tiếng Việt" })).toBeChecked();
  });

  it("bấm một ngôn ngữ là chọn luôn: đóng popup rồi đổi ngôn ngữ", () => {
    vi.useFakeTimers();
    const onClose = vi.fn();
    render(<LanguageDialog open onClose={onClose} />);
    fireEvent.click(screen.getByRole("radio", { name: "English" }));
    expect(screen.getByRole("radio", { name: "English" })).toBeChecked();
    act(() => { vi.runAllTimers(); });
    expect(onClose).toHaveBeenCalledOnce();
    expect(chooseLocale).toHaveBeenCalledExactlyOnceWith("en");
  });

  it("bấm lại ngôn ngữ đang dùng chỉ đóng popup", () => {
    const onClose = vi.fn();
    render(<LanguageDialog open onClose={onClose} />);
    fireEvent.click(screen.getByText("Tiếng Việt"));
    expect(onClose).toHaveBeenCalledOnce();
    expect(chooseLocale).not.toHaveBeenCalled();
  });
});
