"use client";

import { useEffect, useRef, useState } from "react";
import { PageHeader, cx } from "@/shared/ui";
import { InstitutionalKeysSection } from "./InstitutionalKeysSection";
import { QuotaPolicySection, TeacherQuotaTable } from "./QuotaSections";
import { SharedUsageTab } from "./SharedUsageTab";

type Tab = "keys" | "usage";

/**
 * Màn #10 Setting Detail "API AI của trường và hạn mức" (UC 14, 15, 33): tab Khóa và hạn mức, tab Usage. Mở từ thẻ
 * hạn mức thì cuộn tới phần hạn mức.
 */
function AiSettingsPage({ focusQuota }: { focusQuota: boolean }) {
  const [tab, setTab] = useState<Tab>("keys");
  const quota = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (focusQuota) quota.current?.scrollIntoView?.({ block: "start" });
  }, [focusQuota]);

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title="API AI của trường và hạn mức"
        backHref="/admin/settings"
        breadcrumb={[{ label: "Cài đặt", href: "/admin/settings" }, { label: focusQuota ? "Hạn mức AI giảng viên" : "API AI của trường" }]}
      />
      <div role="tablist" aria-label="Phần cài đặt AI" className="flex gap-1 border-b border-line">
        {(
          [
            ["keys", "Khóa và hạn mức"],
            ["usage", "Usage"],
          ] as const
        ).map(([key, label]) => (
          <button
            key={key}
            type="button"
            role="tab"
            aria-selected={tab === key}
            onClick={() => setTab(key)}
            className={cx(
              "-mb-px border-b-2 px-3.5 py-2 text-[13px] font-semibold",
              tab === key ? "border-primary text-primary-ink" : "border-transparent text-muted hover:text-ink",
            )}
          >
            {label}
          </button>
        ))}
      </div>
      {tab === "keys" ? (
        <>
          <InstitutionalKeysSection />
          <div ref={quota} className="flex scroll-mt-4 flex-col gap-4">
            <QuotaPolicySection />
            <TeacherQuotaTable />
          </div>
        </>
      ) : (
        <SharedUsageTab />
      )}
    </div>
  );
}

/** Đăng ký cho thẻ "API AI của trường". */
export function AiConnectionsPanel() {
  return <AiSettingsPage focusQuota={false} />;
}

/** Đăng ký cho thẻ "Hạn mức AI giảng viên": cùng trang, cuộn tới phần hạn mức. */
export function TeacherQuotaPanel() {
  return <AiSettingsPage focusQuota />;
}
