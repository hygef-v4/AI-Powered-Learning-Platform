"use client";
import { useEffect, useState } from "react";
import { notificationsApi } from "../api";
import { preferenceLabels, type EmailPreferences } from "../model/types";
import styles from "../styles/notifications.module.css";
export function EmailPreferencePanel() {
  const [values, setValues] = useState<EmailPreferences | null>(null);
  const [error, setError] = useState("");
  const [pending, setPending] = useState<Record<string, boolean>>({});
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    void notificationsApi
      .preferences(controller.signal)
      .then(setValues)
      .catch(() => {
        if (!controller.signal.aborted) setError("Không thể tải tùy chọn email.");
      });
    return () => controller.abort();
  }, [revision]);
  async function toggle(type: string) {
    if (!values || type in pending || type === "BACKUP_FAILED") return;
    const value = !values[type];
    setPending((previous) => ({ ...previous, [type]: value }));
    setError("");
    try {
      const saved = await notificationsApi.savePreferences({ [type]: value });
      setValues((previous) =>
        previous ? { ...previous, [type]: saved[type] ?? value } : previous,
      );
    } catch {
      setError("Chưa lưu được tùy chọn. Hãy thử lại.");
    } finally {
      setPending((previous) => {
        const next = { ...previous };
        delete next[type];
        return next;
      });
    }
  }
  return (
    <section className={styles.preferences}>
      <h3>Gửi email cho tôi khi có</h3>
      {error && (
        <p role="alert" className={styles.error}>
          {error}{" "}
          {!values && (
            <button
              onClick={() => {
                setError("");
                setRevision((r) => r + 1);
              }}
            >
              Thử lại
            </button>
          )}
        </p>
      )}
      {!values
        ? !error && (
            <p role="status" className={styles.empty}>
              Đang tải…
            </p>
          )
        : Object.entries(values).map(([type, enabled]) => (
            <div key={type} className={styles.preference}>
              <span>{preferenceLabels[type] ?? type}</span>
              <button
                data-testid={`notification-preference-${type}`}
                type="button"
                role="switch"
                aria-label={preferenceLabels[type] ?? type}
                aria-checked={type in pending ? pending[type] : enabled}
                aria-busy={type in pending || undefined}
                disabled={type in pending || type === "BACKUP_FAILED"}
                className={styles.switch}
                onClick={() => void toggle(type)}
              />
            </div>
          ))}
      {values?.BACKUP_FAILED && (
        <p className={styles.help}>Email sao lưu thất bại là cảnh báo bắt buộc, không thể tắt.</p>
      )}
      <p className={styles.help}>Tắt email vẫn nhận thông báo trong ứng dụng.</p>
    </section>
  );
}
