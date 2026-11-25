import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ErrorPage } from "@/shared/ui/ErrorPage";

describe("ErrorPage", () => {

  it("có nút đổi giao diện sáng/tối", () => {
    document.documentElement.dataset.theme = "light";
    render(<ErrorPage kind="not-found" />);

    fireEvent.click(screen.getByRole("button", { name: "Chuyển giao diện tối" }));
    expect(document.documentElement.dataset.theme).toBe("dark");
    fireEvent.click(screen.getByRole("button", { name: "Chuyển giao diện sáng" }));
    expect(document.documentElement.dataset.theme).toBe("light");
  });
});
