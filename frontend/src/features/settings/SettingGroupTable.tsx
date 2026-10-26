"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Icon } from "@/shared/ui/Icon";
import { Pill } from "@/shared/ui/Pill";
import { Section } from "@/shared/ui/Section";
import { ClickableRow, TableFrame, Td, Th } from "@/shared/ui/Table";
import type { SettingGroupView, SettingItem } from "./types";
import { groupMeta } from "./types";
import { formatSettingValue, formatShortDateTime, isEmptyValue } from "./validation";

function ValueCell({ item }: { item: SettingItem }) {
  if (isEmptyValue(item.value)) return <span className="text-muted">Chưa cấu hình</span>;
  if (item.type === "BOOLEAN") return <Pill tone={item.value ? "success" : "neutral"}>{item.value ? "Bật" : "Tắt"}</Pill>;
  return <>{formatSettingValue(item)}</>;
}

/** Một nhóm cài đặt khóa–giá trị trên Setting List; bấm dòng để mở Setting Detail. */
export function SettingGroupTable({ group }: { group: SettingGroupView }) {
  const router = useRouter();
  const meta = groupMeta(group.group);
  return (
    <Section title={meta.label} description={meta.description} icon={meta.icon} tone={meta.tone}>
      <TableFrame>
        <thead>
          <tr>
            <Th className="w-[34%]">Mục</Th>
            <Th className="w-[28%]">Giá trị hiện hành</Th>
            <Th className="w-[16%]">Người sửa</Th>
            <Th className="w-[18%]">Thời điểm sửa</Th>
            <Th className="w-[4%]">
              <span className="sr-only">Mở</span>
            </Th>
          </tr>
        </thead>
        <tbody>
          {group.items.map((item) => {
            const href = `/admin/settings/${encodeURIComponent(item.key)}`;
            return (
              <ClickableRow key={item.key} onClick={() => router.push(href)}>
                <Td className="font-bold">
                  <Link href={href} className="hover:text-primary-ink" onClick={(e) => e.stopPropagation()}>
                    {item.name}
                  </Link>
                </Td>
                <Td>
                  <ValueCell item={item} />
                </Td>
                <Td>{item.updatedBy ?? "—"}</Td>
                <Td className="whitespace-nowrap">{item.updatedAt ? formatShortDateTime(item.updatedAt) : "—"}</Td>
                <Td className="text-muted">
                  <Icon name="chevron_right" size={18} />
                </Td>
              </ClickableRow>
            );
          })}
        </tbody>
      </TableFrame>
    </Section>
  );
}
