import type { DocumentBlock, DocumentModel } from "@/shared/document/types";

type ImageBlock = Extract<DocumentBlock, {type: "IMAGE"}>;
export type SubmissionDocumentDto = {
  blocks: (Exclude<DocumentBlock, ImageBlock> | (Omit<ImageBlock, "content"> & {
    content: ImageBlock["content"] & {fileId?: string};
  }))[];
};

/** Submitted image IDs are resolved through the evaluation's authorized image endpoint. */
export function toEditorDocument(document: SubmissionDocumentDto): DocumentModel {
  return {
    blocks: document.blocks.map((block) => block.type === "IMAGE"
      ? {...block, content: {...block.content, hasFile: typeof block.content.fileId === "string" && block.content.fileId !== ""}}
      : block),
  };
}
