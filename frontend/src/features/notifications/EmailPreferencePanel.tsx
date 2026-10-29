"use client";
import { useEffect, useState } from "react";
import { notificationsApi } from "./api";
import { preferenceLabels, type EmailPreferences } from "./types";
import styles from "./notifications.module.css";
export function EmailPreferencePanel() {
  const [values, setValues] = useState<EmailPreferences | null>(null); const [error, setError] = useState(""); const [busy, setBusy] = useState(false); const [revision, setRevision] = useState(0);
  useEffect(() => { const controller = new AbortController(); void notificationsApi.preferences(controller.signal).then(setValues).catch(() => { if (!controller.signal.aborted) setError("Không thể tải tùy chọn email."); }); return () => controller.abort(); }, [revision]);
  async function toggle(type: string) { if (!values) return; setBusy(true); setError(""); try { setValues(await notificationsApi.savePreferences({ [type]: !values[type] })); } catch { setError("Chưa lưu được tùy chọn. Hãy thử lại."); } finally { setBusy(false); } }
  return <section className={styles.preferences}><h3>Gửi email cho tôi khi có</h3>{error && <p role="alert" className={styles.error}>{error} {!values && <button onClick={() => { setError(""); setRevision(r => r + 1); }}>Thử lại</button>}</p>}{!values ? !error && <p role="status" className={styles.empty}>Đang tải…</p> : Object.entries(preferenceLabels).map(([type, label]) => <div key={type} className={styles.preference}><span>{label}</span><button data-testid={`notification-preference-${type}`} type="button" role="switch" aria-label={label} aria-checked={values[type] ?? true} disabled={busy} className={styles.switch} onClick={() => void toggle(type)} /></div>)}<p className={styles.help}>Tắt email vẫn nhận thông báo trong ứng dụng.</p></section>;
}
