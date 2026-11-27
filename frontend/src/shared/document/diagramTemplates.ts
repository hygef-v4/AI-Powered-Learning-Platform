import type { DiagramType } from "./types";

/**
 * Mẫu sơ đồ chèn từ thanh công cụ (yêu cầu 2026-11-27): mỗi mẫu là vài hình UML tối thiểu. Một mô tả hình sinh ra cả XML
 * Draw.io (để sửa tiếp) lẫn SVG xem trước (để khung sơ đồ hiện hình ngay, chưa cần mở Draw.io). Lưu trong Draw.io thì
 * ảnh xem trước được Draw.io xuất lại.
 */
type Shape =
  | { kind: "rect" | "round" | "ellipse" | "diamond" | "component"; id: string; x: number; y: number; w: number; h: number; label: string }
  | { kind: "actor"; id: string; x: number; y: number; w: number; h: number; label: string }
  | { kind: "start" | "end"; id: string; x: number; y: number; w: number; h: number }
  | { kind: "frame"; id: string; x: number; y: number; w: number; h: number; label: string }
  | { kind: "classBox"; id: string; x: number; y: number; w: number; h: number; label: string; attributes: string[]; operations: string[] }
  | { kind: "lifeline"; id: string; x: number; y: number; w: number; h: number; label: string };

/** Cạnh nối hai hình; {@code points} là đoạn vẽ trong SVG xem trước (Draw.io tự định tuyến theo nguồn, đích). */
type Edge = { id: string; source: string; target: string; points: [number, number, number, number]; label?: string; dashed?: boolean;
  arrow?: "open" | "block" | "none" };

type Template = { width: number; height: number; shapes: Shape[]; edges: Edge[] };

const STROKE = "#1f2937";
const FILL = "#ffffff";
const FONT = "font-family=\"Helvetica, Arial, sans-serif\" font-size=\"12\" fill=\"#111827\"";

const TEMPLATES: Record<Exclude<DiagramType, "OTHER">, Template> = {
  USE_CASE: {
    width: 440, height: 220,
    shapes: [
      { kind: "actor", id: "actor", x: 20, y: 70, w: 30, h: 60, label: "Actor" },
      { kind: "frame", id: "system", x: 130, y: 10, w: 290, h: 200, label: "System" },
      { kind: "ellipse", id: "uc1", x: 200, y: 50, w: 150, h: 50, label: "Use case 1" },
      { kind: "ellipse", id: "uc2", x: 200, y: 130, w: 150, h: 50, label: "Use case 2" },
    ],
    edges: [
      { id: "e1", source: "actor", target: "uc1", points: [55, 95, 200, 75], arrow: "none" },
      { id: "e2", source: "actor", target: "uc2", points: [55, 105, 200, 155], arrow: "none" },
    ],
  },
  CLASS: {
    width: 440, height: 150,
    shapes: [
      { kind: "classBox", id: "a", x: 10, y: 10, w: 160, h: 120, label: "ClassA", attributes: ["- id: int", "- name: String"], operations: ["+ getName(): String"] },
      { kind: "classBox", id: "b", x: 270, y: 10, w: 160, h: 120, label: "ClassB", attributes: ["- id: int"], operations: ["+ update(): void"] },
    ],
    edges: [{ id: "e1", source: "a", target: "b", points: [170, 70, 270, 70], label: "1..*", arrow: "open" }],
  },
  SEQUENCE: {
    width: 400, height: 230,
    shapes: [
      { kind: "lifeline", id: "l1", x: 20, y: 10, w: 120, h: 210, label: ":ObjectA" },
      { kind: "lifeline", id: "l2", x: 260, y: 10, w: 120, h: 210, label: ":ObjectB" },
    ],
    edges: [
      { id: "m1", source: "l1", target: "l2", points: [80, 90, 320, 90], label: "1: request()", arrow: "block" },
      { id: "m2", source: "l2", target: "l1", points: [320, 150, 80, 150], label: "2: response", dashed: true, arrow: "open" },
    ],
  },
  ACTIVITY: {
    width: 220, height: 330,
    shapes: [
      { kind: "start", id: "s", x: 95, y: 10, w: 30, h: 30 },
      { kind: "round", id: "a1", x: 50, y: 70, w: 120, h: 40, label: "Action 1" },
      { kind: "diamond", id: "d", x: 85, y: 140, w: 50, h: 50, label: "" },
      { kind: "round", id: "a2", x: 50, y: 220, w: 120, h: 40, label: "Action 2" },
      { kind: "end", id: "f", x: 95, y: 290, w: 30, h: 30 },
    ],
    edges: [
      { id: "e1", source: "s", target: "a1", points: [110, 40, 110, 70], arrow: "open" },
      { id: "e2", source: "a1", target: "d", points: [110, 110, 110, 140], arrow: "open" },
      { id: "e3", source: "d", target: "a2", points: [110, 190, 110, 220], label: "[ok]", arrow: "open" },
      { id: "e4", source: "a2", target: "f", points: [110, 260, 110, 290], arrow: "open" },
    ],
  },
  ER: {
    width: 440, height: 110,
    shapes: [
      { kind: "rect", id: "e1", x: 10, y: 30, w: 110, h: 50, label: "Entity A" },
      { kind: "diamond", id: "r", x: 170, y: 15, w: 100, h: 80, label: "Relates" },
      { kind: "rect", id: "e2", x: 320, y: 30, w: 110, h: 50, label: "Entity B" },
    ],
    edges: [
      { id: "l1", source: "e1", target: "r", points: [120, 55, 170, 55], label: "1", arrow: "none" },
      { id: "l2", source: "r", target: "e2", points: [270, 55, 320, 55], label: "N", arrow: "none" },
    ],
  },
  COMPONENT: {
    width: 440, height: 110,
    shapes: [
      { kind: "component", id: "c1", x: 10, y: 25, w: 150, h: 60, label: "Component A" },
      { kind: "component", id: "c2", x: 280, y: 25, w: 150, h: 60, label: "Component B" },
    ],
    edges: [{ id: "e1", source: "c1", target: "c2", points: [160, 55, 280, 55], label: "«use»", dashed: true, arrow: "open" }],
  },
  STATE: {
    width: 460, height: 90,
    shapes: [
      { kind: "start", id: "s", x: 10, y: 30, w: 30, h: 30 },
      { kind: "round", id: "st1", x: 80, y: 20, w: 120, h: 50, label: "State 1" },
      { kind: "round", id: "st2", x: 260, y: 20, w: 120, h: 50, label: "State 2" },
      { kind: "end", id: "f", x: 420, y: 30, w: 30, h: 30 },
    ],
    edges: [
      { id: "e1", source: "s", target: "st1", points: [40, 45, 80, 45], arrow: "open" },
      { id: "e2", source: "st1", target: "st2", points: [200, 45, 260, 45], label: "event", arrow: "open" },
      { id: "e3", source: "st2", target: "f", points: [380, 45, 420, 45], arrow: "open" },
    ],
  },
};

export const DIAGRAM_TEMPLATE_TYPES = Object.keys(TEMPLATES) as Exclude<DiagramType, "OTHER">[];

const escapeXml = (value: string) => value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");

function cell(id: string, value: string, style: string, x: number, y: number, w: number, h: number, parent = "1"): string {
  return `<mxCell id="${id}" value="${escapeXml(value)}" style="${style}" vertex="1" parent="${parent}">`
    + `<mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`;
}

function shapeXml(shape: Shape): string {
  const { id, x, y, w, h } = shape;
  switch (shape.kind) {
    case "rect": return cell(id, shape.label, "rounded=0;whiteSpace=wrap;html=1;", x, y, w, h);
    case "round": return cell(id, shape.label, "rounded=1;whiteSpace=wrap;html=1;arcSize=30;", x, y, w, h);
    case "ellipse": return cell(id, shape.label, "ellipse;whiteSpace=wrap;html=1;", x, y, w, h);
    case "diamond": return cell(id, shape.label, "rhombus;whiteSpace=wrap;html=1;", x, y, w, h);
    case "component": return cell(id, shape.label, "shape=component;align=left;spacingLeft=36;whiteSpace=wrap;html=1;", x, y, w, h);
    case "actor": return cell(id, shape.label, "shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;", x, y, w, h);
    case "frame": return cell(id, shape.label, "rounded=0;whiteSpace=wrap;html=1;fillColor=none;verticalAlign=top;fontStyle=1;", x, y, w, h);
    case "start": return cell(id, "", "ellipse;fillColor=#000000;strokeColor=#000000;html=1;", x, y, w, h);
    case "end": return cell(id, "", "ellipse;html=1;shape=endState;fillColor=#000000;strokeColor=#000000;", x, y, w, h);
    case "lifeline": return cell(id, shape.label, "shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=1;collapsible=0;size=40;", x, y, w, h);
    case "classBox": {
      const header = 26;
      const row = 20;
      const attributesHeight = shape.attributes.length * row;
      const children = [
        ...shape.attributes.map((text, i) => cell(`${id}-a${i}`, text, "text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=4;html=1;",
          0, header + i * row, w, row, id)),
        cell(`${id}-line`, "", "line;strokeWidth=1;fillColor=none;align=left;verticalAlign=middle;html=1;", 0, header + attributesHeight, w, 8, id),
        ...shape.operations.map((text, i) => cell(`${id}-o${i}`, text, "text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=4;html=1;",
          0, header + attributesHeight + 8 + i * row, w, row, id)),
      ];
      return cell(id, shape.label, "swimlane;fontStyle=1;align=center;verticalAlign=top;childLayout=stackLayout;horizontal=1;startSize=26;horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=1;marginBottom=0;html=1;",
        x, y, w, h) + children.join("");
    }
  }
}

function edgeXml(edge: Edge): string {
  const end = edge.arrow === "none" ? "endArrow=none;" : edge.arrow === "block" ? "endArrow=block;endFill=1;" : "endArrow=open;endFill=0;";
  const style = `html=1;${end}${edge.dashed ? "dashed=1;" : ""}`;
  return `<mxCell id="${edge.id}" value="${escapeXml(edge.label ?? "")}" style="${style}" edge="1" parent="1" source="${edge.source}" target="${edge.target}">`
    + `<mxGeometry relative="1" as="geometry"/></mxCell>`;
}

/** XML Draw.io của mẫu (mxfile không nén). */
export function buildTemplateXml(type: Exclude<DiagramType, "OTHER">): string {
  const template = TEMPLATES[type];
  const cells = [...template.shapes.map(shapeXml), ...template.edges.map(edgeXml)].join("");
  return `<mxfile><diagram id="page-1" name="Trang 1"><mxGraphModel><root><mxCell id="0"/><mxCell id="1" parent="0"/>${cells}</root></mxGraphModel></diagram></mxfile>`;
}

function text(x: number, y: number, value: string, anchor = "middle", weight = "normal"): string {
  return `<text x="${x}" y="${y}" text-anchor="${anchor}" dominant-baseline="middle" font-weight="${weight}" ${FONT}>${escapeXml(value)}</text>`;
}

function shapeSvg(shape: Shape): string {
  const { x, y, w, h } = shape;
  const box = `fill="${FILL}" stroke="${STROKE}" stroke-width="1"`;
  const cx = x + w / 2;
  const cy = y + h / 2;
  switch (shape.kind) {
    case "rect": return `<rect x="${x}" y="${y}" width="${w}" height="${h}" ${box}/>${text(cx, cy, shape.label)}`;
    case "round": return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${Math.min(12, h / 3)}" ${box}/>${text(cx, cy, shape.label)}`;
    case "ellipse": return `<ellipse cx="${cx}" cy="${cy}" rx="${w / 2}" ry="${h / 2}" ${box}/>${text(cx, cy, shape.label)}`;
    case "diamond": return `<polygon points="${cx},${y} ${x + w},${cy} ${cx},${y + h} ${x},${cy}" ${box}/>${shape.label ? text(cx, cy, shape.label) : ""}`;
    case "component": return `<rect x="${x}" y="${y}" width="${w}" height="${h}" ${box}/>`
      + `<rect x="${x - 6}" y="${y + 12}" width="12" height="8" ${box}/><rect x="${x - 6}" y="${y + h - 20}" width="12" height="8" ${box}/>`
      + text(cx + 6, cy, shape.label);
    case "actor": {
      const head = w / 3;
      const neck = y + head * 2;
      return `<circle cx="${cx}" cy="${y + head}" r="${head}" ${box}/>`
        + `<path d="M${cx} ${neck} V${y + h * 0.65} M${x} ${y + h * 0.42} H${x + w} M${cx} ${y + h * 0.65} L${x} ${y + h} M${cx} ${y + h * 0.65} L${x + w} ${y + h}" fill="none" stroke="${STROKE}"/>`
        + text(cx, y + h + 12, shape.label);
    }
    case "frame": return `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="none" stroke="${STROKE}"/>${text(cx, y + 14, shape.label, "middle", "bold")}`;
    case "start": return `<circle cx="${cx}" cy="${cy}" r="${w / 2}" fill="#000000"/>`;
    case "end": return `<circle cx="${cx}" cy="${cy}" r="${w / 2}" fill="${FILL}" stroke="#000000"/><circle cx="${cx}" cy="${cy}" r="${w / 2 - 5}" fill="#000000"/>`;
    case "lifeline": return `<rect x="${x}" y="${y}" width="${w}" height="40" ${box}/>${text(cx, y + 20, shape.label)}`
      + `<line x1="${cx}" y1="${y + 40}" x2="${cx}" y2="${y + h}" stroke="${STROKE}" stroke-dasharray="4 3"/>`;
    case "classBox": {
      const header = 26;
      const row = 20;
      const split = y + header + shape.attributes.length * row + 4;
      return `<rect x="${x}" y="${y}" width="${w}" height="${h}" ${box}/>`
        + `<line x1="${x}" y1="${y + header}" x2="${x + w}" y2="${y + header}" stroke="${STROKE}"/>`
        + text(cx, y + header / 2, shape.label, "middle", "bold")
        + shape.attributes.map((value, i) => text(x + 6, y + header + i * row + row / 2, value, "start")).join("")
        + `<line x1="${x}" y1="${split}" x2="${x + w}" y2="${split}" stroke="${STROKE}"/>`
        + shape.operations.map((value, i) => text(x + 6, split + 4 + i * row + row / 2, value, "start")).join("");
    }
  }
}

function edgeSvg(edge: Edge): string {
  const [x1, y1, x2, y2] = edge.points;
  const marker = edge.arrow === "none" ? "" : ` marker-end="url(#${edge.arrow === "block" ? "arrow-block" : "arrow-open"})"`;
  // Nhãn nằm trên đoạn ngang, bên phải đoạn dọc.
  const label = !edge.label ? ""
    : x1 === x2 ? text(x1 + 6, (y1 + y2) / 2, edge.label, "start")
      : text((x1 + x2) / 2, (y1 + y2) / 2 - 9, edge.label);
  return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${STROKE}"${edge.dashed ? " stroke-dasharray=\"5 4\"" : ""}${marker}/>${label}`;
}

/** SVG xem trước của mẫu, cùng bố cục với XML. */
export function buildTemplateSvg(type: Exclude<DiagramType, "OTHER">): string {
  const template = TEMPLATES[type];
  const defs = `<defs><marker id="arrow-open" markerWidth="10" markerHeight="10" refX="9" refY="5" orient="auto">`
    + `<path d="M0 0 L9 5 L0 10" fill="none" stroke="${STROKE}"/></marker>`
    + `<marker id="arrow-block" markerWidth="10" markerHeight="10" refX="9" refY="5" orient="auto">`
    + `<path d="M0 0 L9 5 L0 10 Z" fill="${STROKE}"/></marker></defs>`;
  const body = [...template.shapes.map(shapeSvg), ...template.edges.map(edgeSvg)].join("");
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${template.width}" height="${template.height}" viewBox="0 0 ${template.width} ${template.height}">`
    + `<rect width="100%" height="100%" fill="#ffffff"/>${defs}${body}</svg>`;
}

/**
 * Ảnh PNG của mẫu (yêu cầu 2026-11-27: tải DOCX ra đúng hình sơ đồ ngay cả khi chưa mở Draw.io), vẽ SVG xem trước lên
 * canvas ở tỉ lệ 2x. SVG của mẫu không có foreignObject nên canvas không bị khóa; trình duyệt không vẽ được thì null.
 */
export async function buildTemplatePng(type: Exclude<DiagramType, "OTHER">): Promise<string | null> {
  if (typeof document === "undefined" || typeof Image === "undefined") return null;
  const template = TEMPLATES[type];
  const scale = 2;
  try {
    const image = new Image();
    image.src = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(buildTemplateSvg(type))}`;
    await image.decode();
    const canvas = document.createElement("canvas");
    canvas.width = template.width * scale;
    canvas.height = template.height * scale;
    const context = canvas.getContext("2d");
    if (!context) return null;
    context.scale(scale, scale);
    context.drawImage(image, 0, 0, template.width, template.height);
    const png = canvas.toDataURL("image/png");
    return png.startsWith("data:image/png;base64,") ? png : null;
  } catch {
    return null;
  }
}
