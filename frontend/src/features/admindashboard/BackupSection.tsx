import {Section} from "@/shared/ui/Section";
import {Button, ButtonLink} from "@/shared/ui/Button";
import {BackupRunTable, BackupStateLabel, formatDay, formatInstant} from "./BackupRunTable";
import type {BackupSectionView} from "./types";

export function BackupSection({value, onHistory}: {value: BackupSectionView; onHistory: () => void}) {
  const latest = value.latest.data;
  const schedule = value.schedule.data;
  return <Section title="Sao lưu dữ liệu"
    actions={<><Button onClick={onHistory} data-testid="backup-history-open">Lịch sử backup</Button><ButtonLink href="/admin/settings">Cài đặt backup</ButtonLink></>}>
    <div className="grid gap-4 md:grid-cols-2">
      <div><h3 className="mb-2 font-semibold">Lần chạy gần nhất</h3>
        {value.latest.availability === "UNAVAILABLE" ? <p>Không khả dụng</p> : !latest ? <p>Chưa có lần backup nào.</p> : <>
          <p>{formatDay(latest.scheduledFor)} · <BackupStateLabel run={latest}/></p>
          <dl className="mt-2 grid grid-cols-2 gap-2 text-xs">
            <dt>Bắt đầu</dt><dd>{formatInstant(latest.startedAt)}</dd><dt>Kết thúc</dt><dd>{formatInstant(latest.completedAt)}</dd>
            <dt>Thời điểm snapshot</dt><dd>{formatInstant(latest.snapshotAt)}</dd>
            <dt>Dung lượng</dt><dd>{latest.byteSize === null ? "—" : `${latest.byteSize.toLocaleString("vi-VN")} byte`}</dd>
            {latest.safeError && <><dt>Lỗi</dt><dd>{latest.safeError}</dd></>}
          </dl>
        </>}
      </div>
      <div data-testid="backup-schedule"><h3 className="mb-2 font-semibold">Lịch backup</h3>
        {!schedule ? <p>Không khả dụng</p> : !schedule.enabled ? <p>Chưa bật</p> : <>
          <p>Hằng ngày lúc {schedule.dailyAt?.slice(0, 5)} · giờ Việt Nam</p>
          <p className="mt-1 text-xs text-muted">Lần tiếp theo dự kiến theo cấu hình: {formatInstant(schedule.nextPlannedAt)}</p>
        </>}
      </div>
      <div data-testid="backup-latest-success"><h3 className="font-semibold">Lần thành công gần nhất</h3>
        <p>{value.latestSuccessfulAt.availability === "UNAVAILABLE" ? "Không khả dụng" : value.latestSuccessfulAt.data === null ? "Chưa có backup thành công." : formatInstant(value.latestSuccessfulAt.data)}</p>
      </div>
      <div data-testid="backup-failures"><h3 className="font-semibold">Lần backup thất bại trong 7 ngày</h3>
        <p>{value.failuresLast7Days.availability === "AVAILABLE" ? value.failuresLast7Days.data.count : "Không khả dụng"}</p>
        {value.failuresLast7Days.data?.latestSafeError && <p>{value.failuresLast7Days.data.latestSafeError}</p>}
      </div>
    </div>
    <p className="text-xs text-muted">Chỉ sao lưu cơ sở dữ liệu. Giữ các bản thành công trong 7 ngày; luôn giữ bản thành công gần nhất, kể cả khi đã quá 7 ngày.</p>
    <h3 className="font-semibold">Ba lần gần nhất</h3>
    {value.recentRuns.availability === "UNAVAILABLE" ? <p>Không khả dụng</p> : value.recentRuns.data.length === 0 ? <p>Chưa có lịch sử backup.</p> : <BackupRunTable runs={value.recentRuns.data}/>}
  </Section>;
}
