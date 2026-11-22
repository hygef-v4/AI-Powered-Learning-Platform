import { SITE_BRAND, buildSiteUrl } from "@/shared/siteMetadata";

type StructuredDataInput = { name: string; description: string; language: string };

/** JSON-LD của trang chủ: tổ chức, website và ứng dụng giáo dục, liên kết với nhau qua `@id`. */
export function buildStructuredData({ name, description, language }: StructuredDataInput) {
  const home = buildSiteUrl("/");
  const organization = `${home}#organization`;
  return {
    "@context": "https://schema.org",
    "@graph": [
      { "@type": "Organization", "@id": organization, name: SITE_BRAND, alternateName: name, url: home, logo: buildSiteUrl("/icon.svg") },
      { "@type": "WebSite", "@id": `${home}#website`, name: SITE_BRAND, alternateName: name, url: home, inLanguage: language, publisher: { "@id": organization } },
      {
        "@type": "SoftwareApplication",
        name: SITE_BRAND,
        alternateName: name,
        description,
        url: home,
        applicationCategory: "EducationalApplication",
        operatingSystem: "Web",
        inLanguage: language,
        publisher: { "@id": organization },
      },
    ],
  };
}

/** Chuỗi JSON an toàn để đặt trong thẻ `<script>`: đổi `<` để nội dung không đóng được thẻ. */
export function serializeStructuredData(data: object): string {
  return JSON.stringify(data).replace(/</g, "\\u003c");
}
