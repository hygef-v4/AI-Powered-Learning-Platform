import { msg } from "@/i18n/msg";

type Tool = { name: string; description: string; write?: boolean };
type ToolGroup = { title: string; audience: string; tools: Tool[] };

export const MCP_TOOL_GROUPS: ToolGroup[] = [
  {
    title: msg("ai.model.mcpGuide.discoveryAndGuidance"),
    audience: msg("ai.model.mcpGuide.lecturersAndSubjectManagersGet"),
    tools: [
      { name: "get_my_capabilities", description: msg("ai.model.mcpGuide.showsTheRoleTokenPermissions") },
      { name: "read_agent_workflow", description: msg("ai.model.mcpGuide.readsTheFullGuideFrom") },
      { name: "read_authoring_requirements", description: msg("ai.model.mcpGuide.readsTheCurrentSchemaAnd") },
      { name: "list_my_classes", description: msg("ai.model.mcpGuide.findsTheClassesYouAre") },
      { name: "list_my_subjects", description: msg("ai.model.mcpGuide.findsSubjectsFromTheClasses") },
    ],
  },
  {
    title: msg("ai.model.mcpGuide.learningMaterialsAsSources"),
    audience: msg("ai.model.mcpGuide.lecturersAndSubjectManagersWithin"),
    tools: [
      { name: "list_materials", description: msg("ai.model.mcpGuide.findsMaterialsOfAClass") },
      { name: "read_authorized_material", description: msg("ai.model.mcpGuide.readsTheContentOfAllowed") },
    ],
  },
  {
    title: msg("ai.model.mcpGuide.createAssignments"),
    audience: msg("ai.model.mcpGuide.lecturersAndSubjectManagersStudents"),
    tools: [
      { name: "list_assignments", description: msg("ai.model.mcpGuide.findsAssignmentsInExactlyOne") },
      { name: "read_assignment_context", description: msg("ai.model.mcpGuide.lecturersReadTheInstructionsOutline") },
      { name: "create_assignment_draft", write: true, description: msg("ai.model.mcpGuide.createsADraftAssignmentWith") },
      { name: "save_assignment_draft", write: true, description: msg("ai.model.mcpGuide.savesAnEditProposalTo") },
    ],
  },
  {
    title: msg("ai.model.mcpGuide.quizzesAndQuestionBank"),
    audience: msg("ai.model.mcpGuide.lecturersSubjectManagersUseThe"),
    tools: [
      { name: "list_quizzes", description: msg("ai.model.mcpGuide.findsAllowedQuizzesByClass") },
      { name: "read_quiz_context", description: msg("ai.model.mcpGuide.readsAQuizYouMay") },
      { name: "create_quiz_draft", write: true, description: msg("ai.model.mcpGuide.createsADraftQuizWith") },
      { name: "save_quiz_draft", write: true, description: msg("ai.model.mcpGuide.savesQuestionProposalsForA") },
      { name: "list_bank_questions", description: msg("ai.model.mcpGuide.theSubjectManagerSearchesThe") },
      { name: "read_question_bank_context", description: msg("ai.model.mcpGuide.theSubjectManagerReadsThe") },
      { name: "save_question_bank_draft", write: true, description: msg("ai.model.mcpGuide.theSubjectManagerSavesQuestion") },
    ],
  },
  {
    title: msg("ai.model.mcpGuide.readSubmissionsAndGradingProposals"),
    audience: msg("ai.model.mcpGuide.lecturersAssignedToTheClass"),
    tools: [
      { name: "list_submissions", description: msg("ai.model.mcpGuide.listsStudentsGroupsWithSubmission") },
      { name: "read_authorized_submission", description: msg("ai.model.mcpGuide.readsTheSnapshotOfA") },
      { name: "read_grading_context", description: msg("ai.model.mcpGuide.readsExactlyTheSubmissionBrief") },
      { name: "read_grading_batch", description: msg("ai.model.mcpGuide.readsTheContextOfSeveral") },
      { name: "record_grading_proposal", write: true, description: msg("ai.model.mcpGuide.savesProposedPointsAndExplanations") },
      { name: "record_grading_proposals_batch", write: true, description: msg("ai.model.mcpGuide.savesSeveralGradingProposalsChecking") },
      { name: "list_grading_proposals", description: msg("ai.model.mcpGuide.showsTheListStatusOf") },
      { name: "read_class_learning_summary", description: msg("ai.model.mcpGuide.readsTheCurrentSubmissionGrading") },
      { name: "read_grade_distribution", description: msg("ai.model.mcpGuide.readsTheAggregatedDistributionOf") },
    ],
  },
  {
    title: msg("ai.model.mcpGuide.studentsPracticeFeedback"),
    audience: msg("ai.model.mcpGuide.onlyTheStudentsOwnSubmitted"),
    tools: [
      { name: "read_own_work", description: msg("ai.model.mcpGuide.readsYourOwnSubmittedPractice") },
      { name: "record_private_feedback", write: true, description: msg("ai.model.mcpGuide.savesPrivateFeedbackForThe") },
    ],
  },
];
