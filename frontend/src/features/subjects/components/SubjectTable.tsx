"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ClickableRow, Icon, Pill, TableFrame, Td, Th } from "@/shared/ui";
import type { SubjectListItem } from "../model/types";

/** Bảng môn học cùng kiểu bảng Tài khoản: tên đậm bấm được, mũi tên mở chi tiết; cột cố định, tên môn rộng nhất. */
export function SubjectTable({ items }: { items: SubjectListItem[]; offset?: number }) {
  const router = useRouter();
  return <TableFrame filledHead className="[&_table]:min-w-[680px] [&_table]:table-fixed"><caption className="sr-only">Danh sách môn học</caption>
    <colgroup><col className="w-[130px]" /><col /><col className="w-[28%]" /><col className="w-[120px]" /><col className="w-[48px]" /></colgroup>
    <thead><tr><Th>Mã môn</Th><Th>Tên môn</Th><Th>Chủ nhiệm môn</Th><Th>Trạng thái</Th><Th><span className="sr-only">Mở</span></Th></tr></thead>
    <tbody>{items.map((subject) => {
      const href = `/admin/subjects/${encodeURIComponent(subject.id)}`;
      return <ClickableRow key={subject.id} data-testid={`subjects-row-${subject.id}`} onClick={(event) => {
        if (!(event.target as HTMLElement).closest("a,button")) router.push(href);
      }}>
        <Td className="truncate font-mono font-semibold" title={subject.code}>{subject.code}</Td>
        <Td><Link href={href} className="block truncate font-semibold text-ink hover:text-primary-ink" title={subject.name}>{subject.name}</Link></Td>
        <Td className="truncate">{subject.managerId === null ? <Pill>Chưa phân công</Pill> : subject.manager?.displayName ?? "Không tra được chủ nhiệm"}</Td>
        <Td><Pill tone={subject.status === "ACTIVE" ? "success" : "danger"}>{subject.status === "ACTIVE" ? "Hoạt động" : "Lưu trữ"}</Pill></Td>
        <Td className="text-faint"><Link href={href} aria-label={`Chi tiết môn học ${subject.code}`} className="inline-flex"><Icon name="chevron_right" size={18} /></Link></Td>
      </ClickableRow>;
    })}{items.length === 0 && <tr><Td colSpan={5} className="py-10 text-center! text-muted">Không có môn học phù hợp với bộ lọc.</Td></tr>}</tbody>
  </TableFrame>;
}
