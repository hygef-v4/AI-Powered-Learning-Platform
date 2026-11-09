"use client";

import { useCallback, useEffect } from "react";

/** Giữ bản soạn tại chỗ khi đóng trang hoặc chuyển liên kết trước khi lưu. */
export function useAuthoringGuard(dirty: boolean) {
  const confirmDiscard = useCallback(() => !dirty || window.confirm("Bạn có nội dung chưa lưu. Bỏ các thay đổi này?"), [dirty]);
  useEffect(() => {
    if (!dirty) return;
    const currentUrl = window.location.href;
    const currentState: unknown = window.history.state;
    const samePage = (href: string) => {
      const next = new URL(href, currentUrl);
      const current = new URL(currentUrl);
      return next.pathname === current.pathname && next.search === current.search;
    };
    let approvedTraversal = false;
    const navigation = "navigation" in window && window.navigation instanceof EventTarget ? window.navigation : null;
    const traverse = (event: Event) => {
      const next = event as Event & { navigationType?: string; destination?: { url: string } };
      if (next.navigationType !== "traverse" || !event.cancelable || !next.destination || samePage(next.destination.url)) return;
      approvedTraversal = confirmDiscard();
      if (!approvedTraversal) event.preventDefault();
    };
    const popState = (event: PopStateEvent) => {
      if (approvedTraversal || samePage(window.location.href)) { approvedTraversal = false; return; }
      if (!confirmDiscard()) {
        event.stopImmediatePropagation();
        // shortcut: older browsers restore a canceled traversal with pushState, remove when Navigation API is universal.
        window.history.pushState(currentState, "", currentUrl);
      }
    };
    const beforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };
    const followLink = (event: MouseEvent) => {
      if (event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
      const link = event.target instanceof Element ? event.target.closest("a[href]") : null;
      if (!(link instanceof HTMLAnchorElement) || link.target === "_blank" || link.hasAttribute("download")) return;
      const destination = new URL(link.href, window.location.href);
      if (destination.pathname === window.location.pathname && destination.search === window.location.search) return;
      if (!confirmDiscard()) {
        event.preventDefault();
        event.stopPropagation();
      }
    };
    window.addEventListener("beforeunload", beforeUnload);
    window.addEventListener("popstate", popState, true);
    navigation?.addEventListener("navigate", traverse);
    document.addEventListener("click", followLink, true);
    return () => {
      window.removeEventListener("beforeunload", beforeUnload);
      window.removeEventListener("popstate", popState, true);
      navigation?.removeEventListener("navigate", traverse);
      document.removeEventListener("click", followLink, true);
    };
  }, [dirty, confirmDiscard]);
  return confirmDiscard;
}
