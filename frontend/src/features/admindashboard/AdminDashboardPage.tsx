"use client";

import Link from "next/link";
import { useEffect, useRef, type ReactNode, type CSSProperties } from "react";
import { actionLabel, shortReason } from "@/features/audit/labels";
import { Alert, PageHeader, TableFrame, Td, Th, cx } from "@/shared/ui";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { Donut, LineChart, Legend, Reveal, SegmentBar, StackedColumns, formatNumber, type Series } from "./charts";
import type { BackupRunItem, SecurityEventItem } from "./types";
import { isDenied, useLiveOverview, type Overview } from "./useLiveOverview";

const ZONE = "Asia/Ho_Chi_Minh";
const KINDS: Series[] = [
  { name: "Cá nhân", color: "var(--c-info)" },
  { name: "Nhóm", color: "var(--c-orange)" },
  { name: "Luyện tập", color: "var(--c-success)" },
];
const ROLES: (Series & { role: "STUDENT" | "TEACHER" | "SUBJECT_MANAGER" | "ADMIN" })[] = [
  { role: "STUDENT", name: "Sinh viên", color: "var(--c-info)" },
  { role: "TEACHER", name: "Giảng viên", color: "var(--c-orange)" },
  { role: "SUBJECT_MANAGER", name: "Chủ nhiệm môn", color: "var(--c-success)" },
  { role: "ADMIN", name: "Quản trị viên", color: "var(--c-accent)" },
];
const STATES = { open: { name: "Mở", color: "var(--c-info)" }, draft: { name: "Nháp", color: "var(--c-warning)" }, archived: { name: "Lưu trữ", color: "var(--c-faint)" } };
const BACKUP = {
  ok: { name: "Thành công", color: "var(--c-success)" },
  failed: { name: "Thất bại", color: "var(--c-danger)" },
  running: { name: "Đang chạy", color: "var(--c-info)" },
  none: { name: "Không chạy", color: "var(--c-line-strong)" },
};
const SECURITY_LABELS: Record<string, string> = {
  ACCESS_DENIED: "Truy cập bị chặn",
  LOGIN_FAILED: "Đăng nhập không thành",
  LOGIN_BLOCKED_LOCKED: "Đăng nhập khi đang khóa",
  ACCOUNT_TEMP_LOCKED: "Tài khoản bị khóa tạm",
  AUTH_FAILED: "Xác thực thất bại",
  ACCOUNT_DISABLED: "Vô hiệu hóa tài khoản",
  PASSWORD_RESET: "Đặt lại mật khẩu",
  MCP_TOKEN_REVEALED: "Xem token MCP",
};

/** Ngày yyyy-mm-dd theo giờ Việt Nam, lùi `back` ngày. */
function vnDay(back = 0) {
  const today = new Intl.DateTimeFormat("en-CA", { timeZone: ZONE }).format(new Date());
  const date = new Date(`${today}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() - back);
  return date.toISOString().slice(0, 10);
}
const shortDay = (day: string) => `${day.slice(8, 10)}/${day.slice(5, 7)}`;
const shortTime = (iso: string) => new Intl.DateTimeFormat("vi-VN", { timeZone: ZONE, hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit" })
  .formatToParts(new Date(iso)).reduce((acc, part) => ({ ...acc, [part.type]: part.value }), {} as Record<string, string>);

/**
 * Tổng quan hệ thống (U15) dạng bảng thống kê, tự cập nhật mỗi 30 giây khi tab đang mở (nhãn Trực tiếp; mất kết nối thì
 * giữ số cũ và đổi nhãn):
 * - bốn ô số tô màu (xanh dương, cam, xanh lá, vàng): tài khoản hoạt động (tăng/giảm hôm nay), môn đang mở, lớp đang mở,
 *   token AI hệ thống hôm nay;
 * - bài tập được giao 7 ngày gần nhất theo loại (cột chồng) cạnh lịch sao lưu 30 ngày;
 * - tài khoản theo vai trò (tròn), lớp và môn theo trạng thái (thanh), token AI 7 ngày (đường);
 * - hoạt động đáng ngờ 30 ngày gần nhất.
 * Bấm vào biểu đồ để hiện số, bấm ra ngoài thì ẩn. Nguồn nào lỗi thì chỉ thẻ đó báo không khả dụng.
 */
export function AdminDashboardPage() {
  const { data, error, live, retry } = useLiveOverview();
  return (
    <div className="flex flex-col gap-4">
      <PageHeader title={<>Tổng quan{data && <LiveBadge live={live} />}</>} />
      {!data && !error && <PageLoading />}
      {error !== null && !data && (isDenied(error)
        ? <Alert tone="danger" role="alert">Không có quyền xem tổng quan hệ thống.</Alert>
        : <PageLoadError onRetry={retry}>Không tải được tổng quan hệ thống.</PageLoadError>)}
      {data && <Board data={data} />}
    </div>
  );
}

function LiveBadge({ live }: { live: boolean }) {
  return (
    <span role="status" data-testid="dashboard-live"
      className={cx("inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 font-sans text-xs font-semibold tracking-normal",
        live ? "bg-success-soft text-success" : "bg-warning-soft text-warning")}>
      <span aria-hidden className="relative flex size-2">
        {live && <span className="absolute inline-flex size-full animate-ping rounded-full bg-success opacity-60 motion-reduce:animate-none" />}
        <span className={cx("relative inline-flex size-2 rounded-full", live ? "bg-success" : "bg-warning")} />
      </span>
      {live ? "Trực tiếp" : "Mất kết nối"}
    </span>
  );
}

function Board({ data }: { data: Overview }) {
  const { dashboard, activity, usage, backups } = data;
  const accounts = dashboard.accounts.data;
  const subjects = dashboard.subjects.data;
  const classes = dashboard.classes.data;
  const change = activity?.activeAccountChangeToday.data ?? null;
  return (
    <>
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <Kpi tone="info" label="Tài khoản hoạt động" value={accounts?.activeTotal} testId="dashboard-accounts" order={0}>
          {change !== null && (
            <span className={change > 0 ? "text-success" : change < 0 ? "text-danger" : undefined}>
              {change === 0 ? "Không đổi hôm nay" : `${change > 0 ? "+" : "−"}${formatNumber(Math.abs(change))} hôm nay`}
            </span>
          )}
        </Kpi>
        <Kpi tone="orange" label="Môn đang mở" value={subjects?.ACTIVE} testId="dashboard-subjects" order={1}>
          {subjects && <span className="text-danger">{formatNumber(subjects.ARCHIVED)} lưu trữ</span>}
        </Kpi>
        <Kpi tone="success" label="Lớp đang mở" value={classes?.OPEN} testId="dashboard-classes" order={2}>
          {classes && <><span className="text-danger">{formatNumber(classes.ARCHIVED)} lưu trữ</span> · <span className="text-warning">{formatNumber(classes.DRAFT)} nháp</span></>}
        </Kpi>
        <Kpi tone="warning" label="Token AI hệ thống" value={usage?.todayTokens} testId="dashboard-tokens" order={3}>
          {usage && (usage.globalDayTokens === null ? "Không giới hạn" : `/ ${formatNumber(usage.globalDayTokens)}`)}
        </Kpi>
      </div>

      <div className="grid gap-3 lg:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
        <Panel title="Bài tập" subtitle="Trong 7 ngày gần nhất" testId="dashboard-assignments" order={4}>
          {activity?.assignmentsByDay.data ? <>
            <Legend series={KINDS} className="mb-2" />
            <Reveal label="Số bài tập được giao mỗi ngày theo loại">
              {(shown) => <StackedColumns shown={shown} series={KINDS}
                days={activity.assignmentsByDay.data!.map((d) => ({ label: shortDay(d.day), values: [d.individual, d.group, d.practice] }))} />}
            </Reveal>
          </> : <Unavailable />}
        </Panel>
        <Panel title="Sao lưu" subtitle="Trong 30 ngày gần nhất" testId="dashboard-backups" order={5}>
          {backups ? <BackupCalendar runs={backups} /> : <Unavailable />}
        </Panel>
      </div>

      <div className="grid gap-3 md:grid-cols-3">
        <Panel title="Tài khoản" subtitle="Theo vai trò" testId="dashboard-roles" order={6}>
          {accounts ? (
            <Reveal label="Tài khoản theo vai trò" className="flex items-center gap-4">
              {(shown) => <>
                <Donut shown={shown} items={ROLES.map((r) => ({ ...r, value: Object.values(accounts.byRoleAndStatus[r.role] ?? {}).reduce((a, b) => a + b, 0) }))} />
                <Legend series={ROLES} className="flex-col" />
              </>}
            </Reveal>
          ) : <Unavailable />}
        </Panel>
        <Panel title="Lớp và môn học" subtitle="Theo trạng thái" testId="dashboard-states" order={7}>
          {classes || subjects ? (
            <Reveal label="Lớp và môn học theo trạng thái" className="flex flex-col gap-2.5">
              {(shown) => <>
                <span className="text-xs text-muted">Lớp</span>
                {classes ? <SegmentBar shown={shown} items={[{ ...STATES.open, value: classes.OPEN }, { ...STATES.draft, value: classes.DRAFT }, { ...STATES.archived, value: classes.ARCHIVED }]} /> : <Unavailable />}
                <span className="mt-1 text-xs text-muted">Môn học</span>
                {subjects ? <SegmentBar shown={shown} items={[{ ...STATES.open, value: subjects.ACTIVE }, { ...STATES.archived, value: subjects.ARCHIVED }]} /> : <Unavailable />}
                <Legend series={[STATES.open, STATES.draft, STATES.archived]} className="mt-1" />
              </>}
            </Reveal>
          ) : <Unavailable />}
        </Panel>
        <Panel title="Token AI Hệ thống" subtitle="Theo ngày" testId="dashboard-token-trend" order={8}>
          {usage ? (
            <Reveal label="Token AI hệ thống theo ngày">
              {(shown) => <LineChart shown={shown} color="var(--c-info)" points={usage.byDay.map((d) => ({ label: shortDay(d.day), value: d.tokens }))} />}
            </Reveal>
          ) : <Unavailable />}
        </Panel>
      </div>

      <Panel title="Hoạt động đáng ngờ" subtitle="Các sự việc đáng chú ý gần đây" testId="dashboard-security" order={9}
        action={<Link href="/admin/audit" className="text-[13px] font-semibold text-primary-ink hover:underline">Mở Nhật ký</Link>}>
        {activity?.securityEvents.data ? <SecurityTable events={activity.securityEvents.data} /> : <Unavailable />}
      </Panel>
    </>
  );
}

const KPI_TONES = {
  orange: "bg-orange-soft text-orange",
  info: "bg-info-soft text-info",
  success: "bg-success-soft text-success",
  warning: "bg-warning-soft text-warning",
};

/** Hiện lần lượt lúc mới vào trang: mỗi khối trễ hơn khối trước 60ms (máy bật giảm chuyển động thì hiện ngay). */
const ENTER = "animate-rise motion-reduce:animate-none";
const enterDelay = (order: number): CSSProperties => ({ animationDelay: `${order * 60}ms` });

/**
 * Số đếm lên từ 0 trong lần hiện đầu tiên (đổi giá trị trực tiếp trên nút chữ React đã tạo nên React vẫn cập nhật đúng ở các
 * lần sau); các lần tự cập nhật sau thì đổi số ngay. Không có matchMedia (môi trường test) hoặc giảm chuyển động thì không đếm.
 */
function CountUp({ value }: { value: number }) {
  const ref = useRef<HTMLSpanElement>(null);
  const done = useRef(false);
  useEffect(() => {
    const text = ref.current?.firstChild;
    if (!text || done.current) return;
    if (typeof window.matchMedia !== "function" || window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      done.current = true;
      return;
    }
    const start = performance.now();
    let frame = 0;
    const step = (now: number) => {
      const t = Math.min(1, (now - start) / 900);
      text.nodeValue = formatNumber(Math.round(value * (1 - (1 - t) ** 4)));
      if (t < 1) frame = requestAnimationFrame(step);
      else done.current = true;
    };
    frame = requestAnimationFrame(step);
    return () => {
      cancelAnimationFrame(frame);
      text.nodeValue = formatNumber(value);
    };
  }, [value]);
  return <span ref={ref}>{formatNumber(value)}</span>;
}

/** Ô số tô màu: nhãn, số lớn, một dòng phụ; chữ lấy màu của chính ô. */
function Kpi({ tone, label, value, testId, order, children }: { tone: keyof typeof KPI_TONES; label: string; value: number | undefined; testId: string; order: number; children?: ReactNode }) {
  return (
    <section aria-label={label} data-testid={testId} style={enterDelay(order)}
      className={cx("flex min-w-0 flex-col gap-1 rounded-xl px-5 py-4", ENTER, KPI_TONES[tone])}>
      <h2 className="text-[13px] font-semibold">{label}</h2>
      <p className="font-display text-[34px] leading-none font-bold tracking-[-0.02em] tabular-nums">{value === undefined ? "—" : <CountUp value={value} />}</p>
      <p className="min-h-[1.25rem] text-[13px] font-semibold">{value === undefined ? "Không khả dụng" : children}</p>
    </section>
  );
}

function Panel({ title, subtitle, action, testId, order, children }: { title: string; subtitle: string; action?: ReactNode; testId: string; order: number; children: ReactNode }) {
  return (
    <section aria-label={title} data-testid={testId} style={enterDelay(order)}
      className={cx("flex min-w-0 flex-col gap-3 rounded-xl border border-line bg-surface p-5", ENTER)}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="font-display text-[16px] font-bold text-ink">{title}</h2>
          <p className="text-xs text-muted">{subtitle}</p>
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}

function Unavailable() {
  return <p className="py-6 text-center text-[13px] text-muted">Không khả dụng</p>;
}

/** Mỗi ô một ngày trong 30 ngày gần nhất (7 ô một hàng, cũ trước), màu theo kết quả sao lưu của ngày đó. */
function BackupCalendar({ runs }: { runs: BackupRunItem[] }) {
  const byDay = new Map(runs.map((run) => [run.scheduledFor, run]));
  const days = Array.from({ length: 30 }, (_, i) => vnDay(29 - i));
  const stateOf = (day: string) => {
    const run = byDay.get(day);
    if (!run) return BACKUP.none;
    return run.state === "SUCCESS" ? BACKUP.ok : run.state === "FAILED" ? BACKUP.failed : BACKUP.running;
  };
  return (
    <div className="flex flex-col gap-3">
      <ul className="grid grid-cols-7 gap-1.5" aria-label="Kết quả sao lưu theo ngày">
        {days.map((day, i) => {
          const state = stateOf(day);
          return (
            <li key={day} title={`${shortDay(day)}: ${state.name}`} aria-label={`${shortDay(day)}: ${state.name}`}
              className="aspect-square animate-pop rounded-md transition-transform duration-150 hover:scale-110 motion-reduce:animate-none motion-reduce:transition-none"
              style={{ background: state.color, animationDelay: `${300 + i * 18}ms` }} />
          );
        })}
      </ul>
      <Legend series={[BACKUP.ok, BACKUP.failed, BACKUP.none]} />
    </div>
  );
}

function SecurityTable({ events }: { events: SecurityEventItem[] }) {
  if (events.length === 0) return <p className="py-6 text-center text-[13px] text-muted">Không có hoạt động đáng ngờ trong 30 ngày qua.</p>;
  return (
    <TableFrame filledHead className="[&_table]:min-w-[640px] [&_table]:table-fixed">
      <colgroup><col className="w-[130px]" /><col className="w-[28%]" /><col /><col className="w-[160px]" /></colgroup>
      <thead><tr><Th>Thời gian</Th><Th>Sự việc</Th><Th>Tài khoản</Th><Th>Lý do</Th></tr></thead>
      <tbody>
        {events.slice(0, 10).map((event, i) => {
          const time = shortTime(event.occurredAt);
          const label = SECURITY_LABELS[event.action] ?? `Bị từ chối: ${actionLabel(event.action)}`;
          return (
            <tr key={`${event.occurredAt}-${i}`}>
              <Td className="text-muted tabular-nums">{`${time.hour}:${time.minute} ${time.day}/${time.month}`}</Td>
              <Td className="[overflow-wrap:anywhere]">{label}</Td>
              <Td className="[overflow-wrap:anywhere]">{event.account ?? <span className="text-muted">Không rõ</span>}</Td>
              <Td className={cx("[overflow-wrap:anywhere]", event.reason ? "text-danger" : "text-muted")} title={event.reason ?? undefined}>{event.reason ? shortReason(event.reason) : "—"}</Td>
            </tr>
          );
        })}
      </tbody>
    </TableFrame>
  );
}
