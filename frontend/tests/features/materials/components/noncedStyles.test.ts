import { afterEach, describe, expect, it } from "vitest";
import { noncedStyles } from "@/features/materials/components/DocumentViewer";

describe("styles of rendered Word documents", () => {
  afterEach(() => document.head.querySelectorAll("script[nonce]").forEach((s) => s.remove()));

  it("copies library <style> elements into new ones carrying the page CSP nonce", () => {
    const boot = document.createElement("script");
    boot.setAttribute("nonce", "abc123");
    document.head.append(boot);
    const rendered = document.createElement("div");
    rendered.innerHTML = "<style>.docx { color: red; }</style><!-- comment --><style>p { margin: 0 }</style>";

    const styles = noncedStyles(rendered);

    expect(styles).toHaveLength(2);
    expect(styles.map((s) => s.textContent)).toEqual([".docx { color: red; }", "p { margin: 0 }"]);
    expect(styles.every((s) => s.nonce === "abc123" || s.getAttribute("nonce") === "abc123")).toBe(true);
  });
});
