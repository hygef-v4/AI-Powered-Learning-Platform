/**
 * Tệp đề cũ chọn trong popup tạo bài, chờ trang soạn bài mở popup nhập đề (BR-U07-87). Chỉ giữ trong bộ nhớ của tab:
 * tải lại trang thì mất, người dùng chọn lại tệp trên trang soạn bài.
 */
const pending = new Map<string, File[]>();

export function savePendingImport(assignmentId: string, files: File[]) {
  if (files.length > 0) pending.set(assignmentId, files);
}

/** Tệp đang chờ của bài, không xóa (đọc được nhiều lần trong lúc dựng trang); không có thì trả mảng rỗng. */
export function peekPendingImport(assignmentId: string): File[] {
  return pending.get(assignmentId) ?? [];
}

/** Bỏ tệp đang chờ khi popup nhập đề đã mở. */
export function clearPendingImport(assignmentId: string) {
  pending.delete(assignmentId);
}
