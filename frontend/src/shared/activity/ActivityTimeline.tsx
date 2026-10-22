import { Alert, Pill } from "@/shared/ui";

export type ActivityInterval = { start: string; end: string | null; certainty: "KNOWN" | "UNKNOWN" };
export type ActivitySummary = {
  policyState: "DISABLED" | "NO_DATA" | "AVAILABLE" | "EXPIRED";
  awayCount: number;
  knownAwayDuration: string | number;
  intervals: ActivityInterval[];
  unknownGaps: number;
  reliabilityNote: string;
};

/** Thời lượng ISO-8601 (PT1M5S) hoặc số giây thành "1 phút 5 giây". */
export function formatDuration(value: string | number): string {
  let seconds = 0;
  if (typeof value === "number") {
    seconds = Math.round(value);
  } else {
    const match = /^PT(?:(\d+)H)?(?:(\d+)M)?(?:([\d.]+)S)?$/.exec(value);
    if (match) seconds = Number(match[1] ?? 0) * 3600 + Number(match[2] ?? 0) * 60 + Math.round(Number(match[3] ?? 0));
  }
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = seconds % 60;
  return [hours && `${hours} giờ`, minutes && `${minutes} phút`, (rest || (!hours && !minutes)) && `${rest} giây`]
    .filter(Boolean).join(" ");
}

function time(value: string | null) {
  return value ? new Date(value).toLocaleTimeString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" }) : "?";
}

/** Dòng thời gian rời trang cho giảng viên (U09, U11): chỉ là dấu hiệu, không kết luận gian lận (BR-U08-53). */
export function ActivityTimeline({ summary }: { summary: ActivitySummary }) {
  if (summary.policyState === "DISABLED") return <p className="text-xs text-muted">Bài không bật ghi nhận hoạt động.</p>;
  if (summary.policyState === "EXPIRED") return <p className="text-xs text-muted">Dữ liệu đã quá thời hạn lưu.</p>;
  if (summary.policyState === "NO_DATA") return <p className="text-xs text-muted">Chưa có dữ liệu hoạt động.</p>;
  return (
    <div className="flex flex-col gap-2">
      <p className="text-[13px] text-ink">
        Rời trang {summary.awayCount} lần · tổng thời gian đã biết {formatDuration(summary.knownAwayDuration)}
      </p>
      <ul className="flex flex-col gap-1 text-xs">
        {summary.intervals.map((interval, index) => (
          <li key={index} className="flex items-center gap-2">
            <Pill tone={interval.certainty === "KNOWN" ? "warning" : "neutral"}>{interval.certainty === "KNOWN" ? "Rời trang" : "Không rõ"}</Pill>
            {time(interval.start)} – {time(interval.end)}
          </li>
        ))}
      </ul>
      <Alert tone="info">{summary.reliabilityNote}</Alert>
    </div>
  );
}
