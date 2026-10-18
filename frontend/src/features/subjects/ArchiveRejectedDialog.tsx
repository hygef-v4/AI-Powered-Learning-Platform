import { Button, Dialog } from "@/shared/ui";
import { SubjectClassesTable } from "./SubjectClassesTable";
import type { SubjectApiProblem, SubjectClassSummary } from "./types";

export function ArchiveRejectedDialog({ problem, onClose }: { problem: SubjectApiProblem; onClose: () => void }) {
  const count = typeof problem.unarchivedCount === "number" && Number.isSafeInteger(problem.unarchivedCount) && problem.unarchivedCount >= 0 ? problem.unarchivedCount : null;
  const items: SubjectClassSummary[] = Array.isArray(problem.blockingClasses) ? problem.blockingClasses.filter((item) => item && typeof item.classId === "string"
    && typeof item.code === "string" && typeof item.name === "string" && typeof item.term === "string" && (item.status === "OPEN" || item.status === "DRAFT"))
    .map((item) => ({ ...item, teacherName: typeof item.teacherName === "string" ? item.teacherName : null })) : [];
  return <Dialog open title="Chưa thể lưu trữ môn" tone="warning" size="lg" onClose={onClose}
    footer={<Button onClick={onClose} data-testid="subject-archive-rejected-close">Đã hiểu</Button>}>
    <p role="alert">{count === null ? "Môn còn lớp chưa lưu trữ." : `Môn còn ${count} lớp chưa lưu trữ.`} Cần lưu trữ tất cả lớp Nháp và Đang mở trước khi lưu trữ môn.</p>
    {items.length > 0 ? <SubjectClassesTable items={items} /> : <p className="text-sm text-muted">Chưa có chi tiết lớp để hiển thị. Đây không phải xác nhận môn có 0 lớp.</p>}
    {count !== null && items.length < count && items.length > 0 && <p className="text-xs text-muted">Đang hiển thị {items.length} / {count} lớp chưa lưu trữ.</p>}
    <p className="text-xs text-muted">Danh sách chỉ xem; việc quản lý lớp thuộc màn hình của người phụ trách lớp.</p>
  </Dialog>;
}
