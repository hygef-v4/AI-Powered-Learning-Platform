"use client";

import Link from "next/link";
import { useState } from "react";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";
import { Menu } from "@/shared/ui/Menu";
import type { NavItem } from "./navigation";

/** Một môn, một lớp trên sidebar: mã nhỏ trên tên. */
export type NavTreeNode = { id: string; code?: string; name?: string };

type Props = {
  label: string;
  /** Chữ của ô chọn khi chưa mở môn, lớp nào (trang danh sách). */
  placeholder: string;
  nodes: NavTreeNode[];
  /** Môn, lớp của trang đang mở. */
  currentId: string | null;
  /** Các mục của một môn, lớp; chỉ mục của môn, lớp đang mở mới mang `active`. */
  itemsFor: (id: string) => NavItem[];
  loading?: boolean;
};

/**
 * Nhánh cây dưới một mục của menu role (yêu cầu 2026-11-10): đầu nhánh là ô thả xuống chọn môn, lớp như trước. Chọn trong
 * ô chỉ đổi nhánh bên dưới sang các mục của môn, lớp đó, không chuyển trang; bấm một mục của nhánh mới chuyển trang. Mặc định
 * nhánh là môn, lớp của trang đang mở; đang ở một trang trong nhánh thì đổi môn, lớp sang luôn đúng trang đó của môn, lớp mới. Sidebar thu gọn (màn rộng) thì ẩn cả nhánh, mục cha giữ vai trò mục đang mở.
 */
export function NavTree({ label, placeholder, nodes, currentId, itemsFor, loading = false }: Props) {
  // Môn, lớp đang chọn trong ô; trang đổi sang môn, lớp khác thì chọn theo trang mới.
  const [picked, setPicked] = useState<{ id: string | null; page: string | null }>({ id: currentId, page: currentId });
  const pickedId = picked.page === currentId ? picked.id : currentId;
  const current = nodes.find((node) => node.id === pickedId);
  const items = pickedId ? itemsFor(pickedId) : [];
  // Đang ở một trang trong nhánh (ví dụ Bài tập của lớp A): đổi lớp trong ô thì sang đúng trang đó của lớp mới (yêu cầu
  // 2026-11-10). Ở ngoài nhánh (đang ở lớp, mở nhánh môn) thì đổi trong ô chỉ đổi các mục bên dưới, không chuyển trang.
  const openIndex = currentId ? itemsFor(currentId).findIndex((item) => item.active) : -1;
  const hrefFor = (id: string) => (openIndex >= 0 ? itemsFor(id)[openIndex]?.href : undefined);
  return (
    <div role="group" aria-label={label} className="ml-[18px] flex flex-col gap-1 border-l border-line py-1 pl-1.5 md:group-data-[collapsed]/sidebar:hidden">
      <Menu
        ariaLabel={current?.name ? `${placeholder}: ${current.name}` : placeholder}
        variant="tool"
        className="w-full"
        summaryClassName="!h-11 w-full !whitespace-normal rounded-lg border border-line bg-surface !px-2.5 !py-1 text-left hover:!bg-surface hover:border-line-strong [&>svg:last-child]:ml-auto [&>svg:last-child]:shrink-0"
        label={
          pickedId && !current?.name ? (
            <span aria-hidden className="flex flex-1 flex-col gap-1">
              <span className="h-2 w-10 rounded bg-sunken" />
              <span className="h-3 w-28 max-w-full rounded bg-sunken" />
            </span>
          ) : current ? (
            <span className="min-w-0 flex-1 leading-none" title={current.name}>
              <span className="block overflow-hidden font-mono text-xs leading-none tracking-wide whitespace-nowrap text-muted">{current.code}</span>
              <span className="mt-[3px] block truncate text-[13px] leading-[1.15] font-semibold text-ink">{current.name}</span>
            </span>
          ) : (
            <span className="min-w-0 flex-1 truncate text-[13px] font-semibold text-muted">{placeholder}</span>
          )
        }
      >
        <div className="flex max-h-72 w-[208px] flex-col gap-0.5 overflow-y-auto">
          {loading && nodes.length === 0 && <span className="px-2.5 py-2 text-[13px] text-muted">Đang tải…</span>}
          {!loading && nodes.length === 0 && <span className="px-2.5 py-2 text-[13px] text-muted">Chưa có mục nào.</span>}
          {nodes.filter((node) => node.name).map((node) => {
            const selected = node.id === pickedId;
            const href = hrefFor(node.id);
            const className = cx("flex items-center gap-2 rounded-lg px-2.5 py-1.5 text-left text-[13px] text-ink transition-colors hover:bg-sunken", selected && "bg-primary-soft");
            const body = (
              <>
                <span className="min-w-0 flex-1 leading-none" title={node.name}>
                  <span className="block overflow-hidden font-mono text-xs leading-none tracking-wide whitespace-nowrap text-muted">{node.code}</span>
                  <span className="mt-[3px] block truncate text-[13px] leading-[1.15] font-semibold">{node.name}</span>
                </span>
                {selected && <Icon name="check" size={15} className="shrink-0 text-primary-ink" />}
              </>
            );
            return href ? (
              <Link key={node.id} href={href} role="menuitemradio" aria-checked={selected} data-menu-item className={className}>{body}</Link>
            ) : (
              <button key={node.id} type="button" role="menuitemradio" aria-checked={selected} data-menu-item className={className}
                onClick={() => setPicked({ id: node.id, page: currentId })}>{body}</button>
            );
          })}
        </div>
      </Menu>
      {items.length > 0 && (
        <ul className="ml-[15px] flex flex-col gap-0.5 border-l border-line pl-1.5">
          {items.map((item) => (
            <li key={item.href}>
              <Link
                href={item.href}
                aria-current={item.active ? "page" : undefined}
                className={cx(
                  "group flex items-center gap-2 rounded-lg px-2 py-1.5 text-[13px] font-semibold whitespace-nowrap transition-colors duration-200",
                  item.active ? "bg-primary-soft text-ink" : "text-body hover:bg-sidebar-hover hover:text-ink",
                )}
              >
                <Icon
                  name={item.icon}
                  size={15}
                  filled={item.active}
                  className={cx("shrink-0 transition-colors", item.active ? "text-primary-ink" : "text-muted group-hover:text-ink")}
                />
                <span className="truncate">{item.label}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
