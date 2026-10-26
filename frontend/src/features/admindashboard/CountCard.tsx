import { StatTile } from "@/shared/ui/StatTile";

export const formatCount = (value: number) => new Intl.NumberFormat("vi-VN").format(value);

/** Ô số liệu trên Tổng quan hệ thống: số lớn chữ đậm, nhãn nhỏ, một dòng chi tiết. */
export function CountCard({title, value, detail, testId, href}: {title: string; value: number | null; detail?: string; testId: string; href?: string}) {
  return <StatTile testId={testId} label={title} value={value === null ? "—" : formatCount(value)}
    meta={value === null ? "Không khả dụng" : detail} href={href}/>;
}
