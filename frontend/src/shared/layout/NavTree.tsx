"use client";

import Link from "next/link";
import { useState } from "react";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";
import type { NavItem } from "./navigation";

/** Một nút của cây trên sidebar (một môn, một lớp): mã nhỏ trên tên; mở ra là các mục của nút đó. */
export type NavTreeNode = { id: string; code?: string; name?: string };

type Props = {
  label: string;
  nodes: NavTreeNode[];
  /** Nút của trang đang mở: mở sẵn và mục đang mở được tô. */
  currentId: string | null;
  /** Các mục của một nút; chỉ mục của nút đang mở mới mang `active`. */
  itemsFor: (id: string) => NavItem[];
  loading?: boolean;
};

/**
 * Nhánh cây dưới một mục của menu role (kiểu cây thư mục): mỗi nút bấm để mở, đóng; nút của trang đang mở mở sẵn. Sidebar
 * thu gọn (màn rộng) thì ẩn cả nhánh, mục cha giữ vai trò mục đang mở.
 */
export function NavTree({ label, nodes, currentId, itemsFor, loading = false }: Props) {
  const [toggled, setToggled] = useState<Record<string, boolean>>({});
  const isOpen = (id: string) => toggled[id] ?? id === currentId;

  function toggleNode(id: string) {
    setToggled((previous) => ({ ...previous, [id]: !isOpen(id) }));
  }

  return (
    <ul aria-label={label} className="ml-[18px] flex flex-col gap-0.5 border-l border-line py-0.5 pl-1.5 md:group-data-[collapsed]/sidebar:hidden">
      {loading && nodes.length === 0 && (
        <li aria-hidden className="flex flex-col gap-1 px-2.5 py-2">
          <span className="h-2 w-10 rounded bg-sunken" />
          <span className="h-3 w-28 max-w-full rounded bg-sunken" />
        </li>
      )}
      {!loading && nodes.length === 0 && <li className="px-2.5 py-1.5 text-[12.5px] text-muted">Chưa có mục nào.</li>}
      {nodes.map((node) => {
        const open = isOpen(node.id);
        const current = node.id === currentId;
        return (
          <li key={node.id}>
            <button
              type="button"
              aria-expanded={open}
              onClick={() => toggleNode(node.id)}
              title={node.name}
              className={cx(
                "flex w-full items-center gap-1.5 rounded-lg px-2 py-1.5 text-left transition-colors hover:bg-surface/60",
                current ? "text-ink" : "text-body hover:text-ink",
              )}
            >
              <Icon name="chevron_right" size={13} className={cx("shrink-0 text-muted transition-transform duration-200", open && "rotate-90")} />
              <span className="min-w-0 flex-1 leading-none">
                {node.code && <span className="block truncate font-mono text-[9.5px] leading-none tracking-wide text-muted">{node.code}</span>}
                {node.name ? (
                  <span className={cx("block truncate text-[13px] leading-[1.15] font-semibold", node.code && "mt-[3px]")}>{node.name}</span>
                ) : (
                  <span aria-hidden className="mt-[3px] block h-3 w-24 rounded bg-sunken" />
                )}
              </span>
            </button>
            {open && (
              <ul className="mt-0.5 mb-1 ml-[15px] flex flex-col gap-0.5 border-l border-line pl-1.5">
                {itemsFor(node.id).map((item) => (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      aria-current={item.active ? "page" : undefined}
                      className={cx(
                        "group flex items-center gap-2 rounded-lg px-2 py-1.5 text-[13px] font-semibold whitespace-nowrap transition-colors duration-200",
                        item.active ? "bg-surface text-ink shadow-[0_1px_2px_rgb(60_38_14/0.08)]" : "text-body hover:bg-surface/60 hover:text-ink",
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
          </li>
        );
      })}
    </ul>
  );
}
