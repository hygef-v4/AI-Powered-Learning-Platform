import { msg } from "@/i18n/msg";

type Tool = { name: string; description: string; write?: boolean };
type ToolGroup = { title: string; audience: string; tools: Tool[] };

export const MCP_TOOL_GROUPS: ToolGroup[] = [
  {
    title: msg("ai.model.mcpGuide.khamPhaVaHuongDan"),
    audience: msg("ai.model.mcpGuide.giangVienVaChuNhiemMon"),
    tools: [
      { name: "get_my_capabilities", description: msg("ai.model.mcpGuide.choBietVaiTroQuyenToken") },
      { name: "read_agent_workflow", description: msg("ai.model.mcpGuide.docHuongDanDayDuTu") },
      { name: "read_authoring_requirements", description: msg("ai.model.mcpGuide.docSchemaVaGioiHanHien") },
      { name: "list_my_classes", description: msg("ai.model.mcpGuide.timLopDuocPhanCongGiang") },
      { name: "list_my_subjects", description: msg("ai.model.mcpGuide.timMonTuLopDangDay") },
    ],
  },
  {
    title: msg("ai.model.mcpGuide.hocLieuLamNguon"),
    audience: msg("ai.model.mcpGuide.giangVienVaChuNhiemMon2"),
    tools: [
      { name: "list_materials", description: msg("ai.model.mcpGuide.timHocLieuCuaLopMon") },
      { name: "read_authorized_material", description: msg("ai.model.mcpGuide.docNoiDungHocLieuDuoc") },
    ],
  },
  {
    title: msg("ai.model.mcpGuide.raDeAssignment"),
    audience: msg("ai.model.mcpGuide.giangVienVaChuNhiemMon3"),
    tools: [
      { name: "list_assignments", description: msg("ai.model.mcpGuide.timBaiTheoDungMotLop") },
      { name: "read_assignment_context", description: msg("ai.model.mcpGuide.giangVienDocHuongDanKhung") },
      { name: "create_assignment_draft", write: true, description: msg("ai.model.mcpGuide.taoBaiDraftVaDeXuat") },
      { name: "save_assignment_draft", write: true, description: msg("ai.model.mcpGuide.luuDeXuatChinhSuaVao") },
    ],
  },
  {
    title: msg("ai.model.mcpGuide.quizVaNganHangCauHoi"),
    audience: msg("ai.model.mcpGuide.giangVienChuNhiemMonDung"),
    tools: [
      { name: "list_quizzes", description: msg("ai.model.mcpGuide.timQuizDuocPhepTheoLop") },
      { name: "read_quiz_context", description: msg("ai.model.mcpGuide.docQuizCoQuyenSuaCai") },
      { name: "create_quiz_draft", write: true, description: msg("ai.model.mcpGuide.taoQuizDraftCungDeXuat") },
      { name: "save_quiz_draft", write: true, description: msg("ai.model.mcpGuide.luuDeXuatCauHoiCho") },
      { name: "list_bank_questions", description: msg("ai.model.mcpGuide.chuNhiemMonTimCauHoi") },
      { name: "read_question_bank_context", description: msg("ai.model.mcpGuide.chuNhiemMonDocNguCanh") },
      { name: "save_question_bank_draft", write: true, description: msg("ai.model.mcpGuide.chuNhiemMonLuuDeXuat") },
    ],
  },
  {
    title: msg("ai.model.mcpGuide.docBaiNopVaDeXuat"),
    audience: msg("ai.model.mcpGuide.giangVienDuocGiaoDayLop"),
    tools: [
      { name: "list_submissions", description: msg("ai.model.mcpGuide.lietKeSinhVienNhomVa") },
      { name: "read_authorized_submission", description: msg("ai.model.mcpGuide.docSnapshotBaiDaNopQua") },
      { name: "read_grading_context", description: msg("ai.model.mcpGuide.docDungBaiNopDeVa") },
      { name: "read_grading_batch", description: msg("ai.model.mcpGuide.docNguCanhNhieuBaiMoi") },
      { name: "record_grading_proposal", write: true, description: msg("ai.model.mcpGuide.luuDeXuatDiemVaGiai") },
      { name: "record_grading_proposals_batch", write: true, description: msg("ai.model.mcpGuide.luuNhieuDeXuatChamKiem") },
      { name: "list_grading_proposals", description: msg("ai.model.mcpGuide.xemDanhSachTrangThaiDe") },
      { name: "read_class_learning_summary", description: msg("ai.model.mcpGuide.docThongKeNopChamHien") },
      { name: "read_grade_distribution", description: msg("ai.model.mcpGuide.docPhanBoDiemDaCong") },
    ],
  },
  {
    title: msg("ai.model.mcpGuide.sinhVienGopYBaiLuyen"),
    audience: msg("ai.model.mcpGuide.chiBaiPracticeDaNopCua"),
    tools: [
      { name: "read_own_work", description: msg("ai.model.mcpGuide.docBaiPracticeDaNopCua") },
      { name: "record_private_feedback", write: true, description: msg("ai.model.mcpGuide.luuGopYRiengChoBai") },
    ],
  },
];
