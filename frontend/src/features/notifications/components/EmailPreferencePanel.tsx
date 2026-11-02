"use client";
import { useEffect, useState } from "react";
import { notificationsApi } from "../api";
import { preferenceLabels, type EmailPreferences } from "../model/types";
import styles from "../styles/notifications.module.css";
const ORDER = Object.keys(preferenceLabels);
const byLabelOrder = (a: string, b: string) => (ORDER.indexOf(a) + 1 || 99) - (ORDER.indexOf(b) + 1 || 99);

export function EmailPreferencePanel() {
  const [values, setValues] = useState<EmailPreferences | null>(null); const [error, setError] = useState(""); const [pending, setPending] = useState<Record<string, boolean>>({}); const [revision, setRevision] = useState(0);
  useEffect(() => { const controller = new AbortController(); void notificationsApi.preferences(controller.signal).then(setValues).catch(() => { if (!controller.signal.aborted) setError("Không thể tải tùy chọn email."); }); return () => controller.abort(); }, [revision]);
  // Gạt ngay sang giá trị mới; chỉ công tắc đó khóa tới khi server trả về (các công tắc khác vẫn bấm được), lỗi thì về giá trị
  // cũ. Mỗi lần lưu chỉ nhận lại đúng mục vừa đổi, để các lần lưu song song không ghi đè nhau.
  async function toggle(type: string) { if (!values || type in pending) return; const value = !(values[type] ?? true); setPending((p) => ({ ...p, [type]: value })); setError(""); try { const saved = await notificationsApi.savePreferences({ [type]: value }); setValues((v) => v && { ...v, [type]: saved[type] ?? value }); } catch { setError("Chưa lưu được tùy chọn. Hãy thử lại."); } finally { setPending((p) => { const next = { ...p }; delete next[type]; return next; }); } }
  return <section className={styles.preferences}><h3>Gửi email cho tôi khi có</h3>{error && <p role="alert" className={styles.error}>{error} {!values && <button onClick={() => { setError(""); setRevision(r => r + 1); }}>Thử lại</button>}</p>}{!values ? !error && <p role="status" className={styles.empty}>Đang tải…</p> : Object.keys(values).sort(byLabelOrder).map((type) => [type, preferenceLabels[type] ?? type] as const).map(([type, label]) => <div key={type} className={styles.preference}><span>{label}</span><button data-testid={`notification-preference-${type}`} type="button" role="switch" aria-label={label} aria-checked={type in pending ? pending[type] : values[type] ?? true} aria-busy={type in pending || undefined} disabled={type in pending} className={styles.switch} onClick={() => void toggle(type)} /></div>)}<p className={styles.help}>Tắt email vẫn nhận thông báo trong ứng dụng.</p></section>;
}
