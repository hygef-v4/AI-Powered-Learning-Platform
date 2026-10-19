import Link from "next/link";

// Trang 404 chung: màn chưa có (thuộc unit khác chưa merge) hoặc gõ sai đường dẫn. "/" đưa người đã đăng nhập về
// trang theo role, chưa đăng nhập thì về User Login (U01 F15).
export default function NotFound() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-3 bg-shell p-8 text-center">
      <h1 className="text-xl font-bold text-ink">Không tìm thấy trang</h1>
      <p className="text-sm text-muted">Trang này chưa có hoặc đường dẫn không đúng.</p>
      <Link href="/" className="text-sm font-semibold text-primary hover:underline">
        Về trang chủ
      </Link>
    </main>
  );
}
