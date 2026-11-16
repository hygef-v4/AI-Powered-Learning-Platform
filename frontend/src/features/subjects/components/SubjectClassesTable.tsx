import { Pill, TableFrame, Td, Th } from "@/shared/ui";
import type { SubjectClassSummary } from "../model/types";
import { msg } from "@/i18n/msg";

export function SubjectClassesTable({ items }: { items: SubjectClassSummary[] }) {
  return (
    <TableFrame filledHead className="[&_table]:min-w-[560px] [&_table]:table-fixed">
      <caption className="sr-only">{msg("subjects.components.SubjectClassesTable.lopCuaMonHocChiXem")}</caption>
      <colgroup>
        <col className="w-[28%]" />
        <col className="w-[110px]" />
        <col />
        <col className="w-[130px]" />
      </colgroup>
      <thead>
        <tr>
          <Th>{msg("subjects.components.SubjectClassesTable.maLop")}</Th>
          <Th>{msg("subjects.components.SubjectClassesTable.hocKy")}</Th>
          <Th>{msg("subjects.components.SubjectClassesTable.giangVien")}</Th>
          <Th>{msg("subjects.components.SubjectClassesTable.trangThai")}</Th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <tr key={item.classId}>
            <Td className="font-mono font-semibold">{item.code}</Td>
            <Td className="font-mono text-body">{item.term}</Td>
            <Td>{item.teacherName ?? <Pill>{msg("subjects.components.SubjectClassesTable.chuaPhanCong")}</Pill>}</Td>
            <Td>
              <Pill
                tone={
                  item.status === "OPEN"
                    ? "success"
                    : item.status === "DRAFT"
                      ? "warning"
                      : "danger"
                }
              >
                {item.status === "OPEN" ? msg("subjects.components.SubjectClassesTable.dangMo") : item.status === "DRAFT" ? msg("subjects.components.SubjectClassesTable.nhap") : msg("subjects.components.SubjectClassesTable.luuTru")}
              </Pill>
            </Td>
          </tr>
        ))}
        {items.length === 0 && (
          <tr>
            <Td colSpan={4}>
              <p className="py-8 text-center text-muted">{msg("subjects.components.SubjectClassesTable.khongCoLopPhuHop")}</p>
            </Td>
          </tr>
        )}
      </tbody>
    </TableFrame>
  );
}
