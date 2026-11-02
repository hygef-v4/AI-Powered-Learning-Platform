import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BackLink } from "@/shared/navigation/BackLink";
import { useUrlState } from "@/shared/navigation/useUrlState";

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

function Tabs() {
  const [tab, setTab] = useUrlState("tab", "info", ["info", "students"]);
  return <><output>{tab}</output><button onClick={() => setTab("students")}>Students</button></>;
}

describe("URL page state", () => {
  it("pushes tab changes through Next's native-history bridge, preserves query/hash, and restores Back/Forward", async () => {
    window.history.replaceState({ __NA: true, marker: "keep" }, "", "/classes/c?classId=c&tab=info#section");
    const push = vi.spyOn(window.history, "pushState");
    const view = render(<Tabs />);
    fireEvent.click(screen.getByRole("button"));
    expect(screen.getByRole("status")).toHaveTextContent("students");
    expect(window.location.search).toBe("?classId=c&tab=students");
    expect(window.location.hash).toBe("#section");
    expect(push).toHaveBeenCalledWith(null, "", expect.any(URL));
    act(() => window.history.back());
    await waitFor(() => expect(window.location.search).toContain("tab=info"));
    view.rerender(<Tabs />);
    expect(screen.getByRole("status")).toHaveTextContent("info");
    act(() => window.history.forward());
    await waitFor(() => expect(window.location.search).toContain("tab=students"));
    view.rerender(<Tabs />);
    expect(screen.getByRole("status")).toHaveTextContent("students");
  });

  it("restores a valid selected tab on remount and rejects invalid query values", () => {
    window.history.replaceState(null, "", "/classes/c?tab=students");
    const view = render(<Tabs />);
    expect(screen.getByRole("status")).toHaveTextContent("students");
    view.unmount();
    window.history.replaceState(null, "", "/classes/c?tab=unknown");
    render(<Tabs />);
    expect(screen.getByRole("status")).toHaveTextContent("info");
  });
});
