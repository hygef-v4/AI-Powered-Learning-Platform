import { Pill, TableFrame, Td, Th } from "@/shared/ui";
import type { SubjectClassSummary } from "../model/types";

export function SubjectClassesTable({ items }: { items: SubjectClassSummary[] }) {
  return <TableFrame filledHead className="[&_table]:min-w-[640px] [&_table]:table-fixed"><caption className="sr-only">Lớp của môn học, chỉ xem</caption>
    <colgroup><col className="w-[130px]" /><col /><col className="w-[110px]" /><col className="w-[26%]" /><col className="w-[130px]" /></colgroup><thead><tr><Th>Mã lớp</Th><Th>Tên lớp</Th><Th>Học kỳ</Th><Th>Giảng viên</Th><Th>Trạng thái</Th></tr></thead>
    <tbody>{items.map((item) => <tr key={item.classId}>
      <Td className="font-mono font-semibold">{item.code}</Td><Td>{item.name}</Td><Td>{item.term}</Td><Td>{item.teacherName ?? <Pill>Chưa phân công</Pill>}</Td>
      <Td><Pill tone={item.status === "OPEN" ? "success" : item.status === "DRAFT" ? "warning" : "danger"}>{item.status === "OPEN" ? "Đang mở" : item.status === "DRAFT" ? "Nháp" : "Lưu trữ"}</Pill></Td>
    </tr>)}{items.length === 0 && <tr><Td colSpan={5}><p className="py-8 text-center text-muted">Không có lớp phù hợp.</p></Td></tr>}</tbody>
  </TableFrame>;
}
