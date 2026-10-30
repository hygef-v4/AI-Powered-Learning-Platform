import { ApiError } from "@/shared/api/client";
import { Alert, Button } from "@/shared/ui";

export function isSubjectAccessError(error: unknown): boolean {
  return error instanceof ApiError && (error.problem.status === 401 || error.problem.status === 403);
}

export function SubjectReadError({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  const status = error instanceof ApiError ? error.problem.status : null;
  const message = status === 401 ? "Vui lòng đăng nhập để xem môn học."
    : status === 403 ? "Bạn không có quyền quản trị môn học."
    : status === 404 ? "Không tìm thấy môn học."
    : "Không tải được dữ liệu môn học. Vui lòng thử lại.";
  return <Alert tone="danger" role="alert" action={<Button size="sm" onClick={onRetry} data-testid="subjects-read-retry">Thử lại</Button>}>{message}</Alert>;
}
