"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ClickableRow, Icon, Pill, TableFrame, Td, Th } from "@/shared/ui";
import type { SubjectListItem } from "../model/types";
import { msg } from "@/i18n/msg";

/** Bảng môn học cùng kiểu bảng Tài khoản: tên đậm bấm được, mũi tên mở chi tiết; cột cố định, tên môn rộng nhất. */
export function SubjectTable({ items }: { items: SubjectListItem[]; offset?: number }) {
  const router = useRouter();
  return (
    <TableFrame filledHead className="[&_table]:min-w-[680px] [&_table]:table-fixed">
      <caption className="sr-only">{msg("subjects.components.SubjectTable.subjectList")}</caption>
      <colgroup>
        <col className="w-[130px]" />
        <col />
        <col className="w-[28%]" />
        <col className="w-[120px]" />
        <col className="w-[48px]" />
      </colgroup>
      <thead>
        <tr>
          <Th>{msg("subjects.components.SubjectTable.subjectCode")}</Th>
          <Th>{msg("subjects.components.SubjectTable.subjectName")}</Th>
          <Th>{msg("subjects.components.SubjectTable.subjectManager")}</Th>
          <Th>{msg("subjects.components.SubjectTable.status")}</Th>
          <Th>
            <span className="sr-only">{msg("subjects.components.SubjectTable.open")}</span>
          </Th>
        </tr>
      </thead>
      <tbody>
        {items.map((subject) => {
          const href = `/admin/subjects/${encodeURIComponent(subject.id)}`;
          return (
            <ClickableRow
              key={subject.id}
              data-testid={`subjects-row-${subject.id}`}
              onClick={(event) => {
                if (!(event.target as HTMLElement).closest("a,button")) router.push(href);
              }}
            >
              <Td className="truncate font-mono font-semibold" title={subject.code}>
                {subject.code}
              </Td>
              <Td>
                <Link
                  href={href}
                  className="block truncate font-semibold text-ink hover:text-primary-ink"
                  title={subject.name}
                >
                  {subject.name}
                </Link>
              </Td>
              <Td className="truncate">
                {subject.managerId === null ? (
                  <Pill>{msg("subjects.components.SubjectTable.notAssigned")}</Pill>
                ) : (
                  (subject.manager?.displayName ?? msg("subjects.components.SubjectTable.couldNotLookUpThe"))
                )}
              </Td>
              <Td>
                <Pill tone={subject.status === "ACTIVE" ? "success" : "danger"}>
                  {subject.status === "ACTIVE" ? msg("subjects.components.SubjectTable.active") : msg("subjects.components.SubjectTable.archived")}
                </Pill>
              </Td>
              <Td className="text-faint">
                <Link
                  href={href}
                  aria-label={msg("subjects.components.SubjectTable.subjectDetails", { code: String(subject.code) })}
                  className="inline-flex"
                >
                  <Icon name="chevron_right" size={18} />
                </Link>
              </Td>
            </ClickableRow>
          );
        })}
        {items.length === 0 && (
          <tr>
            <Td colSpan={5} className="py-10 text-center! text-muted">
              {msg("subjects.components.SubjectTable.noMatchingSubjects")}</Td>
          </tr>
        )}
      </tbody>
    </TableFrame>
  );
}
