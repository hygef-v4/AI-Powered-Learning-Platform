"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ClickableRow, Icon, Pill, TableFrame, Td, Th } from "@/shared/ui";
import type { SubjectListItem } from "./types";

/** Bảng môn học cùng kiểu bảng Tài khoản: STT, tên đậm bấm được, mũi tên mở chi tiết. */
export function SubjectTable({ items, offset = 0 }: { items: SubjectListItem[]; offset?: number }) {
  const router = useRouter();
  return <TableFrame filledHead><caption className="sr-only">Danh sách môn học</caption>
    <thead><tr><Th className="w-12">STT</Th><Th>Mã môn</Th><Th>Tên môn</Th><Th>Chủ nhiệm môn</Th><Th className="text-center">Số lớp</Th><Th>Trạng thái</Th><Th className="w-10"><span className="sr-only">Mở</span></Th></tr></thead>
    <tbody>{items.map((subject, index) => {
      const href = `/admin/subjects/${encodeURIComponent(subject.id)}`;
      return <ClickableRow key={subject.id} data-testid={`subjects-row-${subject.id}`} onClick={(event) => {
        if (!(event.target as HTMLElement).closest("a,button")) router.push(href);
      }}>
        <Td className="text-muted tabular-nums">{offset + index + 1}</Td>
        <Td className="font-mono font-semibold">{subject.code}</Td>
        <Td><Link href={href} className="font-semibold text-ink hover:text-primary-ink">{subject.name}</Link></Td>
        <Td>{subject.managerId === null ? <Pill>Chưa phân công</Pill> : subject.manager?.displayName ?? "Không tra được chủ nhiệm"}</Td>
        <Td className="text-center">{subject.classCount === null ? <span className="text-xs text-muted">Không khả dụng</span> : subject.classCount}</Td>
        <Td><Pill tone={subject.status === "ACTIVE" ? "success" : "danger"}>{subject.status === "ACTIVE" ? "Hoạt động" : "Lưu trữ"}</Pill></Td>
        <Td className="text-faint"><Link href={href} aria-label={`Chi tiết môn học ${subject.code}`} className="inline-flex"><Icon name="chevron_right" size={18} /></Link></Td>
      </ClickableRow>;
    })}{items.length === 0 && <tr><Td colSpan={7} className="py-10 text-center! text-muted">Không có môn học phù hợp với bộ lọc.</Td></tr>}</tbody>
  </TableFrame>;
}
