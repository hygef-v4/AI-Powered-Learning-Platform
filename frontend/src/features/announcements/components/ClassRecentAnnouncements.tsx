"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Pill, Section } from "@/shared/ui";
import { announcementsApi } from "../api";
import type { Announcement } from "../model/types";

const RECENT = 5;

/**
 * "Thông báo gần đây" trên trang lớp của sinh viên: 5 thông báo mới nhất của lớp, vùng danh sách luôn đủ chỗ cho 5 dòng (cùng
 * chiều cao với Bài cần làm bên cạnh); gắn qua StudentClassBoard của U05.
 */
export function ClassRecentAnnouncements({ classId, teaching = false }: { classId: string; teaching?: boolean }) {
  const [result, setResult] = useState<{ classId: string; items?: Announcement[]; failed?: boolean }>({ classId: "" });
  useEffect(() => {
    const controller = new AbortController();
    announcementsApi.list(classId, 1, RECENT, controller.signal).then(
      (data) => { if (!controller.signal.aborted) setResult({ classId, items: data.items }); },
      () => { if (!controller.signal.aborted) setResult({ classId, failed: true }); },
    );
    return () => controller.abort();
  }, [classId]);
  const current = result.classId === classId ? result : undefined;
  // Trang Thông báo lớp nằm dưới trang lớp (giảng viên hay sinh viên) để chuyển sang không dựng lại khung trang.
  const all = `${teaching ? "/teaching/classes" : "/classes"}/${encodeURIComponent(classId)}/announcements`;
  return (
    <Section title="Thông báo gần đây" actions={<Link href={all} className="text-[13px] font-semibold text-primary-ink hover:underline">Tất cả thông báo</Link>}>
      {/* Vùng nội dung luôn đủ chỗ 5 dòng, kể cả khi đang tải, lỗi hay chưa có thông báo. */}
      <div className="h-[296px]">
      {current?.failed ? <p role="alert" className="flex h-full items-center justify-center text-center text-[13px] text-danger">Không tải được thông báo.</p>
        : !current?.items ? <p role="status" className="flex h-full items-center justify-center text-center text-[13px] text-muted">Đang tải thông báo…</p>
        : current.items.length === 0 ? <p className="flex h-full items-center justify-center text-center text-[13px] text-muted">Chưa có thông báo.</p>
        : (
          <ul className="-my-1 flex flex-col">
            {current.items.map((item) => (
              <li key={item.id} className="border-b border-line last:border-b-0">
                <Link href={all} className="-mx-2 flex flex-col gap-0.5 rounded-lg px-2 py-2.5 transition-colors hover:bg-shell">
                  <span className="flex min-w-0 items-center gap-2">
                    <span className="truncate text-[13.5px] font-semibold text-ink">{item.title}</span>
                    {item.important && <Pill tone="danger">Quan trọng</Pill>}
                  </span>
                  <span className="text-xs text-muted">{item.authorName} · {new Date(item.createdAt).toLocaleString("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric" })}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Section>
  );
}
