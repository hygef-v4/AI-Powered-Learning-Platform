"use client";
import { cx } from "./cx";
import { Icon } from "./Icon";
import { Menu } from "./Menu";

export type ClassFilterOption = { classId: string; code: string; subjectCode: string | null };

/**
 * Nhãn lớp như ô chuyển lớp trên sidebar: mã môn chữ nhỏ ngay trên mã lớp một dòng (lớp không còn tên, yêu cầu 2026-11-02);
 * không có mã môn thì chỉ một dòng.
 */
export function ClassLabel({ subjectCode, code }: { subjectCode: string | null; code: string }) {
  return (
    <span className="min-w-0 flex-1 leading-none" title={subjectCode ? `${subjectCode} · ${code}` : code}>
      {subjectCode && <span className="block overflow-hidden font-mono text-[9.5px] leading-none tracking-wide whitespace-nowrap text-muted">{subjectCode}</span>}
      <span className={cx("block truncate text-[13px] leading-[1.15] font-semibold text-ink", subjectCode && "mt-[3px]")}>{code}</span>
    </span>
  );
}

/**
 * Ô lọc theo lớp ở các trang chung (Thông báo lớp, Bài tập), cùng kiểu ô chuyển lớp trên sidebar: mã môn nhỏ trên mã lớp, Tất cả
 * lớp chỉ một dòng, dấu tích ở lớp đang chọn; danh sách cao tối đa rồi cuộn.
 */
export function ClassFilter({ classes, value, onChange, testId }: { classes: ClassFilterOption[]; value: string; onChange: (classId: string) => void; testId?: string }) {
  const current = classes.find((c) => c.classId === value);
  const option = (classId: string, subjectCode: string | null, code: string) => {
    const selected = classId === value;
    return (
      <button key={classId || "all"} type="button" role="menuitem" data-menu-item aria-current={selected ? "true" : undefined} onClick={() => onChange(classId)}
        className={cx("flex w-full items-center gap-2 rounded-lg px-2.5 py-1.5 text-left text-[13px] text-ink transition-colors hover:bg-sunken", selected && "bg-primary-soft")}>
        <ClassLabel subjectCode={subjectCode} code={code} />
        {selected && <Icon name="check" size={15} className="shrink-0 text-primary-ink" />}
      </button>
    );
  };
  return (
    <div data-testid={testId}>
      <Menu ariaLabel="Lọc theo lớp" variant="tool" align="right" className="w-60"
        summaryClassName="!h-10 w-full rounded-lg border border-line bg-surface !px-2.5 text-left hover:!bg-surface hover:border-line-strong [&>svg:last-child]:ml-auto [&>svg:last-child]:shrink-0"
        label={<ClassLabel subjectCode={current?.subjectCode ?? null} code={current?.code ?? "Tất cả lớp"} />}>
        <div className="flex max-h-72 w-[228px] flex-col gap-0.5 overflow-y-auto">
          {option("", null, "Tất cả lớp")}
          {classes.map((c) => option(c.classId, c.subjectCode, c.code))}
        </div>
      </Menu>
    </div>
  );
}
