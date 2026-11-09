import { render } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { useAuthoringGuard } from "@/features/quizzes/hooks/useAuthoringGuard";

function Guard() { useAuthoringGuard(true); return null; }
const originalNavigation = Object.getOwnPropertyDescriptor(window, "navigation");
afterEach(() => {
  vi.restoreAllMocks(); window.history.replaceState(null, "", "/");
  if (originalNavigation) Object.defineProperty(window, "navigation", originalNavigation);
  else Reflect.deleteProperty(window, "navigation");
});

it("cancels native Navigation API traversal before the page changes", () => {
  const navigation = new EventTarget();
  Object.defineProperty(window, "navigation", { configurable: true, value: navigation });
  window.history.replaceState(null, "", "/quizzes/current");
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  render(<Guard />);
  const event = new Event("navigate", { cancelable: true });
  Object.assign(event, { navigationType: "traverse", destination: { url: "http://localhost/quizzes/previous" } });
  expect(navigation.dispatchEvent(event)).toBe(false);
  expect(confirm).toHaveBeenCalledOnce();
  expect(window.location.pathname).toBe("/quizzes/current");
});

it("keeps the current unsaved page when browser traversal is canceled", () => {
  window.history.replaceState({ quiz: "current" }, "", "/quizzes/current");
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  render(<Guard />);
  window.history.replaceState({ quiz: "previous" }, "", "/quizzes/previous");
  const navigation = vi.fn();
  window.addEventListener("popstate", navigation);
  window.dispatchEvent(new PopStateEvent("popstate", { state: { quiz: "previous" } }));
  expect(confirm).toHaveBeenCalledOnce();
  expect(window.location.pathname).toBe("/quizzes/current");
  expect(navigation).not.toHaveBeenCalled();
  window.removeEventListener("popstate", navigation);
});

it("lets an approved browser traversal proceed", () => {
  window.history.replaceState(null, "", "/quizzes/current");
  vi.spyOn(window, "confirm").mockReturnValue(true);
  render(<Guard />);
  window.history.replaceState(null, "", "/quizzes/previous");
  const navigation = vi.fn();
  window.addEventListener("popstate", navigation);
  window.dispatchEvent(new PopStateEvent("popstate"));
  expect(window.location.pathname).toBe("/quizzes/previous");
  expect(navigation).toHaveBeenCalledOnce();
  window.removeEventListener("popstate", navigation);
});
