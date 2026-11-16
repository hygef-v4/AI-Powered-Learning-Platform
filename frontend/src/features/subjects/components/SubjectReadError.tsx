import { ApiError } from "@/shared/api/client";
import { Alert, Button } from "@/shared/ui";
import { msg } from "@/i18n/msg";

export function isSubjectAccessError(error: unknown): boolean {
  return (
    error instanceof ApiError && (error.problem.status === 401 || error.problem.status === 403)
  );
}

export function SubjectReadError({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  const status = error instanceof ApiError ? error.problem.status : null;
  const message =
    status === 401
      ? msg("subjects.components.SubjectReadError.vuiLongDangNhapDeXem")
      : status === 403
        ? msg("subjects.components.SubjectReadError.banKhongCoQuyenQuanTri")
        : status === 404
          ? msg("subjects.components.SubjectReadError.khongTimThayMonHoc")
          : msg("subjects.components.SubjectReadError.khongTaiDuocDuLieuMon");
  return (
    <Alert
      tone="danger"
      role="alert"
      action={
        <Button size="sm" onClick={onRetry} data-testid="subjects-read-retry">
          {msg("subjects.components.SubjectReadError.thuLai")}</Button>
      }
    >
      {message}
    </Alert>
  );
}
