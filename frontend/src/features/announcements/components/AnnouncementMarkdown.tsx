"use client";
import Markdown from "react-markdown";
import rehypeSanitize from "rehype-sanitize";
import styles from "../styles/announcements.module.css";
export function safeMarkdownUrl(url: string) { return /^(https?:\/\/|\/(?!\/)|#)/i.test(url) && !/[\\\u0000-\u0020]/.test(url) ? url : ""; }
export function AnnouncementMarkdown({ body }: { body: string }) {
  return <div className={styles.markdown}><Markdown skipHtml rehypePlugins={[rehypeSanitize]} disallowedElements={["img"]} urlTransform={safeMarkdownUrl} components={{ a: ({ href, children }) => href ? <a href={href} rel="noopener noreferrer">{children}</a> : <span>{children}</span> }}>{body}</Markdown></div>;
}
