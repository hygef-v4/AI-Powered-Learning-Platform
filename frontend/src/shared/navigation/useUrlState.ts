"use client";

import { useSearchParams } from "next/navigation";
import { useCallback, useState } from "react";

/** Keep page selections in browser history so Back/Forward and reload restore them. */
export function useUrlState<T extends string>(key: string, fallback: T, allowed?: readonly T[], history: "push" | "replace" = "push") {
  const params = useSearchParams();
  const candidate = params?.get(key);
  const urlValue = candidate !== null && candidate !== undefined && (!allowed || allowed.includes(candidate as T))
    ? candidate as T : fallback;
  const [value, setValue] = useState<T>(urlValue);
  const [seenUrlValue, setSeenUrlValue] = useState(urlValue);
  if (seenUrlValue !== urlValue) {
    setSeenUrlValue(urlValue);
    setValue(urlValue);
  }
  const update = useCallback((next: T) => {
    setValue(next);
    const url = new URL(window.location.href);
    url.searchParams.set(key, next);
    if (url.href !== window.location.href) {
      // Next copies its own history state; passing it back makes Next skip URL synchronization.
      if (history === "replace") window.history.replaceState(null, "", url);
      else window.history.pushState(null, "", url);
    }
  }, [history, key]);
  return [value, update] as const;
}
