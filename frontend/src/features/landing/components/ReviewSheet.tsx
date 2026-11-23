import type { CSSProperties } from "react";
import { INK_ORANGE, INK_TEAL } from "@/shared/ui/Logo";

type Translate = (key: string) => string;

// Chữ số trên nền tối: cam gốc đủ tương phản, xanh lơ gốc hơi tối nên dùng bản sáng hơn cho số của giảng viên.
const LECTURER_TEXT = "#6ccad9";

const CRITERIA = [
  { key: "classes", ai: 3, lecturer: 3 },
  { key: "inheritance", ai: 2, lecturer: 2.5 },
  { key: "methods", ai: 4, lecturer: 4 },
] as const;

const delay = (seconds: number): CSSProperties => ({ animationDelay: `${seconds}s` });

/**
 * Hình chính của landing: sơ đồ lớp UML trên bảng vẽ và phiếu rubric bên cạnh. Cột cam là điểm AI đề xuất, cột xanh lơ là
 * điểm giảng viên chốt (tiêu chí kế thừa được giảng viên nâng điểm). Khi tải trang, các lớp hiện ra, đường kế thừa được
 * vẽ, rồi tới điểm AI và cuối cùng điểm giảng viên. Rê chuột lên một tiêu chí thì phần sơ đồ tương ứng sáng lên (CSS
 * `:has`, không cần JavaScript). Tên lớp trong sơ đồ là mã mẫu, không dịch.
 */
export function ReviewSheet({ tr }: { tr: Translate }) {
  const formatScore = new Intl.NumberFormat(tr("reviewSheet.numberLocale"), {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  });
  const aiTotal = CRITERIA.reduce((sum, criterion) => sum + criterion.ai, 0);
  const lecturerTotal = CRITERIA.reduce((sum, criterion) => sum + criterion.lecturer, 0);

  return (
    <div className="review-sheet relative mx-auto w-full max-w-[560px] lg:max-w-none">
      <ClassDiagram className="h-auto w-full sm:w-[90%]" />

      <div
        className="animate-pop relative -mt-2 ml-auto w-full max-w-[340px] rounded-xl border border-line-strong bg-surface p-4 shadow-[var(--shadow-float)] sm:-mt-6"
        style={delay(0.95)}
      >
        <table className="w-full border-collapse text-left text-[13px]">
          <caption className="pb-3 text-left text-[14px] font-semibold text-ink">{tr("reviewSheet.title")}</caption>
          <thead>
            <tr className="text-[11px] text-muted">
              <th scope="col" className="pb-2 font-medium">
                <span className="sr-only">{tr("reviewSheet.criterion")}</span>
              </th>
              <th scope="col" className="w-[72px] pb-2 text-right font-medium">
                <span className="inline-flex items-center gap-1.5">
                  <i className="size-2 rounded-full" style={{ background: INK_ORANGE }} aria-hidden="true" />
                  {tr("reviewSheet.aiColumn")}
                </span>
              </th>
              <th scope="col" className="w-[84px] pb-2 text-right font-medium">
                <span className="inline-flex items-center gap-1.5">
                  <i className="size-2 rounded-full" style={{ background: INK_TEAL }} aria-hidden="true" />
                  {tr("reviewSheet.lecturerColumn")}
                </span>
              </th>
            </tr>
          </thead>
          <tbody>
            {CRITERIA.map(({ key, ai, lecturer }, index) => (
              <tr key={key} data-criterion={key} className="review-row border-t border-line">
                <th scope="row" className="py-2.5 pr-2 font-medium text-body">
                  {tr(`reviewSheet.criteria.${key}`)}
                  {ai !== lecturer && (
                    <span className="mt-0.5 block text-[11px] font-normal" style={{ color: LECTURER_TEXT }}>
                      {tr("reviewSheet.adjusted")}
                    </span>
                  )}
                </th>
                <td className="animate-fade py-2.5 text-right font-mono font-semibold" style={{ ...delay(1.15 + index * 0.12), color: INK_ORANGE }}>
                  {formatScore.format(ai)}
                </td>
                <td className="animate-pop py-2.5 text-right font-mono font-semibold" style={{ ...delay(1.7 + index * 0.12), color: LECTURER_TEXT }}>
                  {formatScore.format(lecturer)}
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="border-t-2 border-line-strong">
              <th scope="row" className="pt-3 font-semibold text-ink">{tr("reviewSheet.total")}</th>
              <td className="pt-3 text-right font-mono font-semibold" style={{ color: INK_ORANGE }}>
                {formatScore.format(aiTotal)}
              </td>
              <td className="animate-pop pt-3 text-right font-mono text-[20px] leading-none font-extrabold" style={{ ...delay(2.15), color: LECTURER_TEXT }}>
                {formatScore.format(lecturerTotal)}
                <span className="text-[12px] font-semibold text-muted">/10</span>
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
}

/** Ba lớp: `Person` là lớp cha, `Lecturer` và `Student` kế thừa qua mũi tên tổng quát hóa (tam giác rỗng). */
function ClassDiagram({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 440 300" aria-hidden="true" className={className} fontFamily="var(--font-mono)">
      <g className="animate-pop" style={delay(0.1)}>
        <UmlClass x={145} y={14} name="Person" fields={["- name: String", "- email: String"]} method="+ login(): void" />
      </g>

      <path
        data-part="inheritance"
        className="uml-part animate-draw"
        d="M110 196 V166 H330 V196 M220 166 V130"
        pathLength={1}
        fill="none"
        stroke="var(--c-body)"
        strokeWidth="2"
        style={{ ...delay(0.55), strokeDasharray: 1 }}
      />
      <path
        data-part="inheritance"
        className="uml-part animate-pop"
        d="M220 113 L208 130 H232 Z"
        fill="var(--c-bg)"
        stroke="var(--c-body)"
        strokeWidth="2"
        strokeLinejoin="round"
        style={delay(1.05)}
      />

      <g className="animate-pop" style={delay(0.25)}>
        <UmlClass x={35} y={196} name="Lecturer" fields={["- staffId: String"]} method="+ grade(): Score" />
      </g>
      <g className="animate-pop" style={delay(0.4)}>
        <UmlClass x={255} y={196} name="Student" fields={["- studentId: String"]} method="+ submit(): Diagram" graded />
      </g>
    </svg>
  );
}

function UmlClass({
  x,
  y,
  name,
  fields,
  method,
  graded = false,
}: {
  x: number;
  y: number;
  name: string;
  fields: string[];
  method: string;
  graded?: boolean;
}) {
  // Ba ngăn của lớp UML: tên, thuộc tính, phương thức.
  const methodsTop = 38 + fields.length * 17;
  const height = methodsTop + 26;
  const stroke = graded ? INK_ORANGE : "var(--c-line-strong)";
  return (
    <g transform={`translate(${x} ${y})`}>
      <rect
        data-part="classes"
        className="uml-part"
        width="150"
        height={height}
        rx="6"
        fill="var(--c-surface)"
        stroke={stroke}
        strokeWidth={graded ? 2.5 : 2}
      />
      <line x1="0" y1="30" x2="150" y2="30" stroke={stroke} strokeWidth="2" />
      <text x="75" y="20" textAnchor="middle" fontSize="13" fontWeight="700" fill="var(--c-ink)">{name}</text>
      {fields.map((field, index) => (
        <text key={field} x="12" y={48 + index * 17} fontSize="11" fill="var(--c-body)">{field}</text>
      ))}
      <line x1="0" y1={methodsTop} x2="150" y2={methodsTop} stroke={stroke} strokeWidth="1.5" />
      <text data-part="methods" className="uml-part" x="12" y={methodsTop + 17} fontSize="11" fill="var(--c-muted)">{method}</text>
    </g>
  );
}
