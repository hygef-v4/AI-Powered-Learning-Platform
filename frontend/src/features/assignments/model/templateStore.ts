/**
 * Đề mẫu đã tải lúc tạo bài (yêu cầu 2026-11-05): giữ lại tệp theo mã bài để popup Tạo nháp bằng AI mặc định dùng làm Mẫu đề.
 * Lưu trong IndexedDB của trình duyệt (còn sau khi tải lại trang); trình duyệt không có IndexedDB thì chỉ giữ trong phiên.
 * Bài tạo bằng cách copy từ bài có sẵn không có đề mẫu.
 */
const DB = "assignment-templates";
const STORE = "templates";
const memory = new Map<string, File>();

function open(): Promise<IDBDatabase | null> {
  if (typeof indexedDB === "undefined") return Promise.resolve(null);
  return new Promise((resolve) => {
    try {
      const request = indexedDB.open(DB, 1);
      request.onupgradeneeded = () => request.result.createObjectStore(STORE);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => resolve(null);
    } catch {
      resolve(null);
    }
  });
}

export async function rememberTemplate(assignmentId: string, file: File): Promise<void> {
  memory.set(assignmentId, file);
  const db = await open();
  if (!db) return;
  await new Promise<void>((resolve) => {
    try {
      const tx = db.transaction(STORE, "readwrite");
      tx.objectStore(STORE).put(file, assignmentId);
      tx.oncomplete = tx.onerror = tx.onabort = () => resolve();
    } catch {
      resolve();
    }
  });
  db.close();
}

export async function templateFor(assignmentId: string): Promise<File | null> {
  const cached = memory.get(assignmentId);
  if (cached) return cached;
  const db = await open();
  if (!db) return null;
  const file = await new Promise<File | null>((resolve) => {
    try {
      const request = db.transaction(STORE, "readonly").objectStore(STORE).get(assignmentId);
      request.onsuccess = () => resolve(request.result instanceof Blob ? toFile(request.result) : null);
      request.onerror = () => resolve(null);
    } catch {
      resolve(null);
    }
  });
  db.close();
  if (file) memory.set(assignmentId, file);
  return file;
}

function toFile(blob: Blob): File {
  return blob instanceof File ? blob : new File([blob], "de-mau.docx", { type: blob.type });
}
