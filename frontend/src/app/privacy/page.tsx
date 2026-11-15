import type { Metadata } from "next";
import Link from "next/link";
import { SITE_NAME } from "@/shared/siteMetadata";

export const metadata: Metadata = { title: "Chính sách quyền riêng tư" };

/** Trang công khai, không cần đăng nhập: chính sách quyền riêng tư (Google yêu cầu khi publish OAuth app). */
export default function PrivacyPage() {
  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-3xl flex-col gap-6 px-4 py-12">
      <header className="flex flex-col gap-2">
        <Link href="/" className="text-[13px] font-semibold text-primary-ink underline underline-offset-4">
          ← {SITE_NAME}
        </Link>
        <h1 className="font-display text-[28px] leading-tight text-ink">Chính sách quyền riêng tư</h1>
        <p className="text-[13px] text-muted">Cập nhật lần cuối: 15/11/2026</p>
      </header>

      <article className="flex flex-col gap-5 text-[14.5px] leading-7 text-body">
        <p>
          {SITE_NAME} là hệ thống phục vụ dạy và học trong phạm vi môn học, dùng nội bộ cho giảng viên,
          chủ nhiệm môn và sinh viên của lớp.
        </p>

        <section className="flex flex-col gap-2">
          <h2 className="text-[17px] font-bold text-ink">Quyền truy cập Google Drive</h2>
          <p>
            Tích hợp Google của hệ thống chỉ xin phạm vi <code className="rounded bg-sunken px-1 py-0.5 font-mono text-[13px]">drive.file</code>:
            hệ thống chỉ nhìn thấy, tạo và quản lý <strong>đúng các tệp và thư mục do chính hệ thống tạo ra</strong> trong
            My Drive của tài khoản được kết nối (thư mục &ldquo;AI Learning Platform&rdquo; chứa học liệu, ảnh trong tài liệu
            và bản sao lưu cơ sở dữ liệu đã mã hóa). Hệ thống không đọc được bất kỳ tệp nào khác trong tài khoản.
          </p>
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[17px] font-bold text-ink">Dữ liệu hệ thống lưu</h2>
          <ul className="list-disc space-y-1.5 pl-5">
            <li>Tệp người dùng tải lên (học liệu, ảnh trong đề và bài làm), lưu trên Google Drive đã kết nối hoặc máy chủ.</li>
            <li>Thông tin tài khoản cần cho việc vận hành (họ tên, email trường cấp, vai trò, mã số sinh viên) và dữ liệu
              học tập (bài tập, bài nộp, điểm) trong cơ sở dữ liệu của hệ thống.</li>
          </ul>
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[17px] font-bold text-ink">Những điều hệ thống không làm</h2>
          <ul className="list-disc space-y-1.5 pl-5">
            <li>Không bán hay chia sẻ dữ liệu người dùng cho bên thứ ba.</li>
            <li>Không dùng dữ liệu Google của người dùng cho quảng cáo hay lập hồ sơ.</li>
            <li>Thông tin xác thực Google (OAuth client, refresh token) chỉ nằm trong cấu hình máy chủ, không lộ ra
              phía người dùng.</li>
          </ul>
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[17px] font-bold text-ink">Liên hệ</h2>
          <p>
            Thắc mắc về chính sách này: liên hệ quản trị viên hệ thống của lớp, hoặc mở issue trên{" "}
            <a href="https://github.com/hygef-v4/AI-Powered-Learning-Platform" target="_blank" rel="noreferrer"
              className="text-primary-ink underline underline-offset-4">
              kho mã nguồn
            </a>.
          </p>
        </section>

        <section className="flex flex-col gap-2 border-t border-line pt-4 text-[13px] text-muted">
          <p>
            English summary: this platform&apos;s Google integration requests only the <code>drive.file</code> scope, so it can
            access only files it creates itself (course materials, document images and encrypted backups in the connected
            account&apos;s My Drive). User data is never sold, shared with third parties, or used for advertising.
          </p>
        </section>
      </article>
    </main>
  );
}
