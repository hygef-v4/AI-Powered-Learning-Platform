import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BackLink } from "@/shared/navigation/BackLink";

vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams(window.location.search) }));

afterEach(() => { cleanup(); vi.restoreAllMocks(); window.history.replaceState(null, "", "/"); });

describe("BackLink", () => {
  it("opens the fixed parent page instead of going back through browser history", () => {
    vi.spyOn(window.history, "length", "get").mockReturnValue(3);
    const back = vi.spyOn(window.history, "back").mockImplementation(() => {});
    render(<BackLink href="#parent">Quay lại</BackLink>);
    expect(screen.getByRole("link")).toHaveAttribute("href", "#parent");
    expect(fireEvent.click(screen.getByRole("link"))).toBe(true);
    expect(back).not.toHaveBeenCalled();
  });

  it("honors an unsaved-work guard cancelling navigation", () => {
    const back = vi.spyOn(window.history, "back").mockImplementation(() => {});
    render(<BackLink href="#parent" onClick={(event) => event.preventDefault()}>Quay lại</BackLink>);
    expect(fireEvent.click(screen.getByRole("link"))).toBe(false);
    expect(back).not.toHaveBeenCalled();
  });
});


