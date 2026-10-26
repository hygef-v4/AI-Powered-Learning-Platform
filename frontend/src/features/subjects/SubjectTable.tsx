"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ClickableRow, Icon, Pill, TableFrame, Td, Th } from "@/shared/ui";
import type { SubjectListItem } from "./types";

export function SubjectTable({ items }: { items: SubjectListItem[] }) {
  const router = useRouter();
  return <TableFrame><caption className="sr-only">Danh sách môn học</caption>
    <thead><tr><Th>Mã môn</Th><Th>Tên môn</Th><Th>Chủ nhiệm môn</Th><Th className="text-center">Số lớp</Th><Th>Trạng thái</Th><Th><span className="sr-only">Chi tiết</span></Th></tr></thead>
    <tbody>{items.map((subject) => {
      const href = `/admin/subjects/${encodeURIComponent(subject.id)}`;
      return <ClickableRow key={subject.id} data-testid={`subjects-row-${subject.id}`} onClick={(event) => {
        if (!(event.target as HTMLElement).closest("a,button")) router.push(href);
      }}>
        <Td className="font-mono font-semibold">{subject.code}</Td>
        <Td><Link href={href} className="font-medium text-primary-ink hover:underline">{subject.name}</Link></Td>
        <Td>{subject.managerId === null ? <Pill>Chưa phân công</Pill> : subject.manager?.displayName ?? "Không tra được chủ nhiệm"}</Td>
        <Td className="text-center">{subject.classCount === null ? <span className="text-xs text-muted">Không khả dụng</span> : subject.classCount}</Td>
        <Td><Pill tone={subject.status === "ACTIVE" ? "success" : "neutral"}>{subject.status === "ACTIVE" ? "Hoạt động" : "Lưu trữ"}</Pill></Td>
        <Td><Link href={href} aria-label={`Chi tiết môn học ${subject.code}`} className="inline-flex rounded p-1 text-muted hover:text-primary-ink"><Icon name="chevron_right" /></Link></Td>
      </ClickableRow>;
    })}{items.length === 0 && <tr><Td colSpan={6}><p className="py-8 text-center text-muted">Không tìm thấy môn học phù hợp.</p></Td></tr>}</tbody>
  </TableFrame>;
}
