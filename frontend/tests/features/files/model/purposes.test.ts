import { describe, expect, it } from "vitest";
import { acceptAttribute, checkFile, type PurposePolicy } from "@/features/files/model/purposes";

const material: PurposePolicy = {
  purpose: "MATERIAL",
  maxSizeMb: 10,
  allowedTypes: ["PDF", "DOCX"],
  extensions: ["pdf", "docx"],
  mediaTypes: ["application/pdf"],
};

describe("checkFile", () => {
  it("nhận tệp đúng đuôi trong giới hạn", () => {
    expect(checkFile({ name: "De Thi.PDF", size: 1024 }, material)).toBeNull();
  });

  it("từ chối sai đuôi hoặc vượt dung lượng", () => {
    expect(checkFile({ name: "anh.png", size: 1 }, material)).toBe("Chỉ nhận tệp PDF, DOCX");
    expect(checkFile({ name: "khong-duoi", size: 1 }, material)).not.toBeNull();
    expect(checkFile({ name: "a.pdf", size: 10 * 1024 * 1024 + 1 }, material)).toBe("Tệp vượt 10 MB");
  });

  it("dựng accept từ danh sách đuôi", () => {
    expect(acceptAttribute(material)).toBe(".pdf,.docx");
  });
});
