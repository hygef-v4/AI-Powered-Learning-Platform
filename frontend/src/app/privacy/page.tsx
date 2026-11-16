import type { Metadata } from "next";
import Link from "next/link";
import { serverMsg, serverRichTranslator, serverTranslator } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.privacy.page.chinhSachQuyenRiengTu") };
}

/** Trang công khai, không cần đăng nhập: chính sách quyền riêng tư (Google yêu cầu khi publish OAuth app). */
export default async function PrivacyPage() {
  const t = await serverTranslator();
  // Câu có phần in đậm: <b>…</b> trong tệp ngôn ngữ thành <strong>.
  const rich = await serverRichTranslator();
  const b = (key: string) => rich(key, { b: (chunks) => <strong className="font-semibold text-ink">{chunks}</strong> });
  const siteName = t("shared.siteMetadata.nenTangHocTap");
  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-3xl flex-col gap-6 px-4 py-12">
      <header className="flex flex-col gap-2">
        {/* Chân trang chỉ có ở màn đăng nhập nên nút quay lại về trang đăng nhập (yêu cầu 2026-11-16). */}
        <Link href="/login" className="text-[13px] font-semibold text-primary-ink underline underline-offset-4">
          ← {t("identity.otp.components.OtpFlow.quayLaiDangNhap")}
        </Link>
        <h1 className="font-display text-[28px] leading-tight text-ink">{t("app.privacy.page.chinhSachQuyenRiengTu")}</h1>
        <p className="text-[13px] text-muted">{t("app.privacy.page.capNhatLanCuoi1511")}</p>
      </header>

      <article className="flex flex-col gap-5 text-[14.5px] leading-7 text-body">
        <p>
          {siteName} {b("app.privacy.page.laHeThongPhucVuDay")}</p>

        <section className="flex flex-col gap-2">
          <h2 className="text-[17px] font-bold text-ink">{t("app.privacy.page.quyenTruyCapGoogleDrive")}</h2>
          <p>
            {t("app.privacy.page.tichHopGoogleCuaHeThong")}<code className="rounded bg-sunken px-1 py-0.5 font-mono text-[13px]">drive.file</code>{t("app.privacy.page.heThongChiNhinThayTao")}<strong>{t("app.privacy.page.dungCacTepVaThuMuc")}</strong>{b("app.privacy.page.trongMyDriveCuaTaiKhoan")}
          </p>
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[17px] font-bold text-ink">{t("app.privacy.page.duLieuHeThongLuu")}</h2>
          <ul className="list-disc space-y-1.5 pl-5">
            <li>{b("app.privacy.page.tepNguoiDungTaiLenHoc")}</li>
            <li>{b("app.privacy.page.thongTinTaiKhoanCanCho")}</li>
          </ul>
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[17px] font-bold text-ink">{t("app.privacy.page.nhungDieuHeThongKhongLam")}</h2>
          <ul className="list-disc space-y-1.5 pl-5">
            <li>{b("app.privacy.page.khongBanHayChiaSeDu")}</li>
            <li>{b("app.privacy.page.khongDungDuLieuGoogleCua")}</li>
            <li>{b("app.privacy.page.thongTinXacThucGoogleOauth")}</li>
          </ul>
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-[17px] font-bold text-ink">{t("app.privacy.page.lienHe")}</h2>
          <p>
            {b("app.privacy.page.thacMacVeChinhSachNay")}{" "}
            <a href="https://github.com/hygef-v4/AI-Powered-Learning-Platform" target="_blank" rel="noreferrer"
              className="text-primary-ink underline underline-offset-4">
              {t("app.privacy.page.khoMaNguon")}</a>.
          </p>
        </section>
      </article>
    </main>
  );
}
