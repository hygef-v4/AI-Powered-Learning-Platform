"use client";

import Link from "next/link";
import type { ComponentProps } from "react";

/** The href is a direct-entry fallback; ordinary Back clicks use browser history. */
export function BackLink({ onClick, ...props }: ComponentProps<typeof Link>) {
  return <Link {...props} onClick={(event) => {
    onClick?.(event);
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey
      || (props.target && props.target !== "_self") || props.download) return;
    if (window.history.length > 1) {
      event.preventDefault();
      window.history.back();
    }
  }} />;
}
