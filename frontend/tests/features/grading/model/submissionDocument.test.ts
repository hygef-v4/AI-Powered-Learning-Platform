import { expect, it } from "vitest";
import { newBlock, type BlockType } from "@/shared/document/types";
import { toEditorDocument, type SubmissionDocumentDto } from "@/features/grading/model/submissionDocument";

it("preserves every submitted block kind, origin and formatting without mutating the DTO", () => {
  const kinds: BlockType[] = ["HEADING", "PARAGRAPH", "LIST", "TABLE", "IMAGE", "DIAGRAM"];
  const document: SubmissionDocumentDto = { blocks: kinds.map((kind) => newBlock(kind, "STUDENT")) };
  const table = document.blocks.find((block) => block.type === "TABLE")!;
  table.content.rows[0]![0]!.added = true;
  const image = document.blocks.find((block) => block.type === "IMAGE")!;
  image.content = {fileId: "image-1", alt: "Answer", width: 480, hasFile: false};
  const before = structuredClone(document);
  const adapted = toEditorDocument(document);
  expect(document).toEqual(before);
  expect(adapted.blocks.filter((block) => block.type !== "IMAGE")).toEqual(document.blocks.filter((block) => block.type !== "IMAGE"));
  expect(adapted.blocks.find((block) => block.type === "IMAGE")).toEqual({...image, content: {...image.content, hasFile: true}});
  expect(toEditorDocument({blocks: []})).toEqual({blocks: []});
});

it.each([undefined, ""])("marks an image without a fileId (%s) as unavailable", (fileId) => {
  expect(toEditorDocument({blocks: [{id: "i", origin: "TEACHER", type: "IMAGE", content: {fileId, alt: "image"}}]}).blocks[0])
    .toEqual({id: "i", origin: "TEACHER", type: "IMAGE", content: {fileId, alt: "image", hasFile: false}});
});
