import styles from "../styles/academics.module.css";

/** Ô tròn ghi hai chữ cái đầu của tên người (hai từ cuối của họ tên). */
export function Avatar({ name }: { name: string }) {
  const initials = name.split(/\s+/).filter(Boolean).slice(-2).map((word) => word[0]).join("");
  return <span className={styles.avatar} aria-hidden="true">{initials}</span>;
}
