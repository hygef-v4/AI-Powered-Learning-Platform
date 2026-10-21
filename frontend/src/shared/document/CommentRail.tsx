"use client";

import { useEffect, useLayoutEffect, useRef, useSyncExternalStore, type ReactNode, type RefObject } from "react";

/** Ghi chú bên lề (kiểu comment Google Docs) gắn với một block; {@code anchorBlockId} null là đầu tài liệu. */
export type DocumentComment = { key: string; anchorBlockId: string | null; content: ReactNode };

const WIDE = "(min-width: 1280px)";
const GAP = 8;

function subscribe(onChange: () => void) {
  const query = typeof window !== "undefined" && window.matchMedia ? window.matchMedia(WIDE) : null;
  query?.addEventListener("change", onChange);
  return () => query?.removeEventListener("change", onChange);
}

/** Màn đủ rộng để đặt ghi chú bên cạnh trang; hẹp hơn thì ghi chú nằm ngay dưới block. */
export function useWideLayout(): boolean {
  return useSyncExternalStore(subscribe, () => (window.matchMedia ? window.matchMedia(WIDE).matches : false), () => false);
}

function anchorTop(paper: HTMLElement, blockId: string | null, base: number): number | null {
  if (!blockId) return 0;
  const anchor = paper.querySelector(`[data-block-id="${blockId.replace(/["\\]/g, "\\$&")}"]`);
  return anchor ? anchor.getBoundingClientRect().top - base : null;
}

/**
 * Cột ghi chú bên phải trang (yêu cầu 2026-10-21): mỗi thẻ bắt đầu ngang block của nó (tiêu đề của phần) và nằm trong
 * một dải kéo tới thẻ kế tiếp. Thẻ dính ở mép trên màn hình khi người dùng cuộn qua đầu phần, đứng yên trong suốt phần
 * đó, và chỉ bị đẩy đi khi đầu phần kế tiếp cuộn tới. Thẻ chồng nhau thì thẻ sau dời xuống. Vị trí đặt trực tiếp lên
 * DOM (không qua state) nên gõ chữ làm block cao lên thì dải đi theo mà không render lại.
 */
export function CommentRail({ paper, comments, activeKey }: {
  paper: RefObject<HTMLElement | null>;
  comments: DocumentComment[];
  activeKey?: string | null;
}) {
  const rail = useRef<HTMLDivElement>(null);
  const bands = useRef(new Map<string, HTMLDivElement>());
  const cards = useRef(new Map<string, HTMLDivElement>());
  const state = useRef({ comments, activeKey });
  const observer = useRef<ResizeObserver | null>(null);

  const layout = useRef(() => {
    const railEl = rail.current;
    const paperEl = paper.current;
    if (!railEl || !paperEl) return;
    const base = railEl.getBoundingClientRect().top;
    const end = paperEl.getBoundingClientRect().bottom - base;
    const entries = state.current.comments.flatMap((comment) => {
      const band = bands.current.get(comment.key);
      const card = cards.current.get(comment.key);
      if (!band || !card) return [];
      const top = anchorTop(paperEl, comment.anchorBlockId, base);
      band.style.visibility = top === null ? "hidden" : "";
      return top === null ? [] : [{ band, anchor: top, height: card.offsetHeight }];
    }).sort((a, b) => a.anchor - b.anchor);
    if (entries.length === 0) {
      railEl.style.minHeight = "";
      return;
    }
    const tops: number[] = [];
    entries.forEach((entry, i) => {
      tops.push(i === 0 ? Math.max(0, entry.anchor) : Math.max(entry.anchor, tops[i - 1]! + entries[i - 1]!.height + GAP));
    });
    let bottom = 0;
    entries.forEach((entry, i) => {
      const next = i + 1 < entries.length ? tops[i + 1]! - GAP : Math.max(end, tops[i]! + entry.height);
      const height = Math.max(entry.height, next - tops[i]!);
      entry.band.style.top = `${tops[i]}px`;
      entry.band.style.height = `${height}px`;
      bottom = Math.max(bottom, tops[i]! + height);
    });
    railEl.style.minHeight = `${bottom}px`;
  });

  useLayoutEffect(() => {
    state.current = { comments, activeKey };
    cards.current.forEach((card) => observer.current?.observe(card));
    layout.current();
  });

  useEffect(() => {
    const run = () => layout.current();
    if (typeof ResizeObserver !== "undefined") {
      observer.current = new ResizeObserver(run);
      if (paper.current) observer.current.observe(paper.current);
      cards.current.forEach((card) => observer.current?.observe(card));
    }
    window.addEventListener("resize", run);
    void document.fonts?.ready.then(run);
    return () => {
      observer.current?.disconnect();
      observer.current = null;
      window.removeEventListener("resize", run);
    };
  }, [paper]);

  return (
    <div ref={rail} className="relative" data-testid="comment-rail">
      {comments.map((comment) => (
        <div key={comment.key} data-comment-band={comment.key} className="absolute inset-x-0 top-0"
          ref={(node) => { if (node) bands.current.set(comment.key, node); else bands.current.delete(comment.key); }}>
          {/* Dính dưới thanh trên cùng (64px) và thanh công cụ của trang soạn. */}
          <div data-comment={comment.key} className="sticky top-[7.25rem]"
            ref={(node) => { if (node) cards.current.set(comment.key, node); else cards.current.delete(comment.key); }}>
            {comment.content}
          </div>
        </div>
      ))}
    </div>
  );
}
