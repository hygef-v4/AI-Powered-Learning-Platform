import Link from "next/link";
import type { SettingGroupView } from "./types";
import { GROUP_LABELS } from "./types";
import { formatSettingValue } from "./validation";

export function SettingGroupTable({ group }: { group: SettingGroupView }) {
  return (
    <section className="mt-6">
      <h2 className="text-lg font-semibold">{GROUP_LABELS[group.group] ?? group.group}</h2>
      <table className="mt-2 w-full text-left text-sm">
        <thead className="text-slate-500">
          <tr>
            <th className="py-1 pr-4 font-medium">Mục</th>
            <th className="py-1 pr-4 font-medium">Giá trị hiện hành</th>
            <th className="py-1 pr-4 font-medium">Người sửa</th>
            <th className="py-1 font-medium">Sửa lúc</th>
          </tr>
        </thead>
        <tbody>
          {group.items.map((item) => (
            <tr key={item.key} className="border-t border-slate-200">
              <td className="py-2 pr-4">
                <Link className="text-blue-700 hover:underline" href={`/admin/settings/${encodeURIComponent(item.key)}`}>
                  {item.name}
                </Link>
              </td>
              <td className="py-2 pr-4">{formatSettingValue(item)}</td>
              <td className="py-2 pr-4">{item.updatedBy ?? "Giá trị khởi tạo"}</td>
              <td className="py-2">{item.updatedAt ? new Date(item.updatedAt).toLocaleString("vi-VN") : ""}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
