import {Pill} from "@/shared/ui/Pill";
import {TableFrame, Td, Th} from "@/shared/ui/Table";
import type {BackupRunItem} from "./types";

export function formatInstant(value: string | null): string {
  if (!value) return "Chưa có dữ liệu";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Chưa có dữ liệu" : new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh", day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit",
  }).format(date);
}
export function formatDay(value: string): string {
  const [year, month, day] = value.split("-");
  return `${day}/${month}/${year}`;
}
export function BackupStateLabel({run}: {run: BackupRunItem}) {
  const labels = {CREATED: "Chờ chạy", RUNNING: "Đang chạy", SUCCESS: "Thành công", FAILED: "Lỗi"};
  return <Pill tone={run.purgedAt ? "neutral" : run.state === "SUCCESS" ? "success" : run.state === "FAILED" ? "danger" : "info"}>
    {run.purgedAt ? "Đã xóa (quá 7 ngày)" : labels[run.state]}
  </Pill>;
}
export function BackupRunTable({runs}: {runs: BackupRunItem[]}) {
  return <TableFrame>
    <caption className="sr-only">Các lần backup</caption>
    <thead><tr><Th>Ngày chạy</Th><Th>Bắt đầu</Th><Th>Kết thúc</Th><Th>Dung lượng</Th><Th>Kết quả</Th><Th>Lỗi</Th></tr></thead>
    <tbody>{runs.map((run, index) => <tr key={`${run.scheduledFor}-${index}`}>
      <Td>{formatDay(run.scheduledFor)}</Td><Td>{formatInstant(run.startedAt)}</Td><Td>{formatInstant(run.completedAt)}</Td>
      <Td>{run.byteSize === null ? "—" : `${new Intl.NumberFormat("vi-VN").format(run.byteSize)} byte`}</Td>
      <Td><BackupStateLabel run={run}/></Td><Td>{run.safeError ?? "—"}</Td>
    </tr>)}</tbody>
  </TableFrame>;
}
