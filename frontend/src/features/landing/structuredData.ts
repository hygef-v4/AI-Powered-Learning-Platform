import { SITE_BRAND_ALIASES, SITE_BRAND_FULL, buildSiteUrl } from "@/shared/siteMetadata";

type StructuredDataInput = {
  name: string;
  description: string;
  language: string;
  keywords?: string;
  features?: string[];
  faqs?: { question: string; answer: string }[];
};

/**
 * JSON-LD của trang chủ: tổ chức, website, ứng dụng giáo dục và trang hỏi đáp, liên kết với nhau qua `@id`. Tên chính là
 * "ALAPEdu" (khớp tên miền), các cách gõ khác nằm trong `alternateName`.
 */
export function buildStructuredData({ name, description, language, keywords, features, faqs }: StructuredDataInput) {
  const home = buildSiteUrl("/");
  const organization = `${home}#organization`;
  const alternateName = [...SITE_BRAND_ALIASES, name];
  const graph: object[] = [
    { "@type": "Organization", "@id": organization, name: SITE_BRAND_FULL, alternateName, url: home, logo: buildSiteUrl("/icon.svg") },
    { "@type": "WebSite", "@id": `${home}#website`, name: SITE_BRAND_FULL, alternateName, url: home, inLanguage: language, publisher: { "@id": organization } },
    {
      "@type": "SoftwareApplication",
      name: SITE_BRAND_FULL,
      alternateName,
      description,
      url: home,
      applicationCategory: "EducationalApplication",
      operatingSystem: "Web",
      inLanguage: language,
      ...(keywords ? { keywords } : {}),
      ...(features?.length ? { featureList: features } : {}),
      publisher: { "@id": organization },
    },
  ];
  if (faqs?.length) {
    graph.push({
      "@type": "FAQPage",
      "@id": `${home}#faq`,
      inLanguage: language,
      mainEntity: faqs.map(({ question, answer }) => ({
        "@type": "Question",
        name: question,
        acceptedAnswer: { "@type": "Answer", text: answer },
      })),
    });
  }
  return { "@context": "https://schema.org", "@graph": graph };
}

/** Chuỗi JSON an toàn để đặt trong thẻ `<script>`: đổi `<` để nội dung không đóng được thẻ. */
export function serializeStructuredData(data: object): string {
  return JSON.stringify(data).replace(/</g, "\\u003c");
}
