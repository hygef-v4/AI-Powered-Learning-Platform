"use client";

import type { ReactNode } from "react";
import { ButtonLink, Card, PageHeader } from "@/shared/ui";
import { McpClientSetup } from "../components/SetupGuides";
import { MCP_TOOL_GROUPS } from "../model/mcpGuide";
import { msg } from "@/i18n/msg";

const CONTENTS = [
  ["tong-quan", msg("ai.pages.McpGuidePage.whatIsMcpFor")],
  ["ket-noi", msg("ai.pages.McpGuidePage.connectAnApp")],
  ["su-dung", msg("ai.pages.McpGuidePage.howToUseIt")],
  ["cong-cu", msg("ai.pages.McpGuidePage.toolList")],
  ["xu-ly-loi", msg("ai.pages.McpGuidePage.troubleshooting")],
  ["tham-khao", msg("ai.pages.McpGuidePage.officialDocumentation")],
] as const;

export function McpGuidePage() {
  return <div className="flex min-w-0 flex-col gap-8">
    <PageHeader title={msg("ai.pages.McpGuidePage.mcpDocumentation")} backHref="/ai/connection?tab=mcp"
      subtitle={msg("ai.pages.McpGuidePage.connectYourAgentToThe")}
      actions={<ButtonLink href="/ai/connection?tab=mcp" size="sm">{msg("ai.pages.McpGuidePage.manageMcpConnections")}</ButtonLink>} />
    <div className="grid min-w-0 gap-8 lg:grid-cols-[200px_minmax(0,1fr)] lg:gap-12">
      <GuideContents />
      <article aria-label={msg("ai.pages.McpGuidePage.mcpGuide")} className="min-w-0 space-y-9 text-[15px] leading-7 text-body">
        <GuideSection id="tong-quan" title={msg("ai.pages.McpGuidePage.whatIsMcpFor")}>
          <p>{msg("ai.pages.McpGuidePage.mcpLetsYouChatIn")}</p>
          <p>{msg("ai.pages.McpGuidePage.youStateYourRequestAnd")}</p>
          <Card as="dl" padding="lg" className="grid gap-4 sm:grid-cols-2">
            <div><dt className="font-semibold text-ink">{msg("ai.pages.McpGuidePage.readOnly")}</dt><dd className="mt-1 text-sm">{msg("ai.pages.McpGuidePage.viewTheDataYourRole")}</dd></div>
            <div><dt className="font-semibold text-ink">{msg("ai.pages.McpGuidePage.readWrite")}</dt><dd className="mt-1 text-sm">{msg("ai.pages.McpGuidePage.addsTheWriteToolsYour")}</dd></div>
          </Card>
          <ul className="list-disc space-y-2 pl-5">
            <li>{msg("ai.pages.McpGuidePage.lecturersWorkInAssignedClasses")}</li>
            <li>{msg("ai.pages.McpGuidePage.studentsAskForFeedbackOn")}</li>
            <li>{msg("ai.pages.McpGuidePage.adminsCannotConnectMcpThe")}</li>
          </ul>
        </GuideSection>

        <GuideSection id="ket-noi" title={msg("ai.pages.McpGuidePage.connectAnApp")}>
          <p>{msg("ai.pages.McpGuidePage.inManageMcpConnectionsClick")}<strong>{msg("ai.pages.McpGuidePage.connectNewAgent")}</strong>{msg("ai.pages.McpGuidePage.giveItAMemorableLabel")}</p>
          <Card className="min-w-0 sm:p-5"><McpClientSetup /></Card>
          <p className="text-sm text-muted">{msg("ai.pages.McpGuidePage.theGuideDoesNotRequire")}</p>
          <p className="text-sm">{msg("ai.pages.McpGuidePage.keepTheTokenInYour")}</p>
          <p className="text-sm">{msg("ai.pages.McpGuidePage.toDisconnectDisableOrRemove")}</p>
        </GuideSection>

        <GuideSection id="su-dung" title={msg("ai.pages.McpGuidePage.howToUseIt")}>
          <div className="space-y-5">
            <div>
              <h3 className="font-semibold text-ink">{msg("ai.pages.McpGuidePage.n1CreateOrEditAn")}</h3>
              <ol className="mt-2 list-decimal space-y-2 pl-5">
                <li>{msg("ai.pages.McpGuidePage.stateTheClassSubjectTopic")}</li>
                <li>{msg("ai.pages.McpGuidePage.theAgentReadsItsPermissions")}</li>
                <li>{msg("ai.pages.McpGuidePage.theAgentCreatesADraft")}</li>
                <li>{msg("ai.pages.McpGuidePage.openTheLinkReviewThe")}<strong>{msg("ai.pages.McpGuidePage.applyProposal")}</strong>{msg("ai.pages.McpGuidePage.editAndSaveInThe")}</li>
              </ol>
            </div>
            <div>
              <h3 className="font-semibold text-ink">{msg("ai.pages.McpGuidePage.n2RubricBasedGradingProposals")}</h3>
              <ol className="mt-2 list-decimal space-y-2 pl-5">
                <li>{msg("ai.pages.McpGuidePage.tellTheAgentTheClass")}</li>
                <li>{msg("ai.pages.McpGuidePage.theAgentReadsTheGrading")}</li>
                <li>{msg("ai.pages.McpGuidePage.askForPointsPerItem")}</li>
                <li>{msg("ai.pages.McpGuidePage.openTheReviewLinkOn")}</li>
              </ol>
              <p className="mt-3 rounded-lg border border-line bg-sunken p-4 text-sm">{msg("ai.pages.McpGuidePage.mcpDoesNotGuaranteeThe")}</p>
            </div>
            <div>
              <h3 className="font-semibold text-ink">{msg("ai.pages.McpGuidePage.n3QuizzesQuestionBankAnd")}</h3>
              <p className="mt-2">{msg("ai.pages.McpGuidePage.quizzesUseTheirOwnQuestion")}</p>
            </div>
          </div>
          <Card as="details" className="text-sm">
            <summary className="cursor-pointer font-semibold text-ink">{msg("ai.pages.McpGuidePage.samplePromptsToGetStarted")}</summary>
            <div className="mt-4 space-y-4">
              <div><p className="font-semibold">{msg("ai.pages.McpGuidePage.createAssignment")}</p><pre className="mt-2 whitespace-pre-wrap break-words rounded-lg bg-sunken p-3 font-mono text-xs leading-6">{`Create a graded individual assignment for class SE1933 on software architecture: 3 UML questions, 10 points total, medium difficulty. Confirm the exact class, ask about any missing requirements, and draft questions with a matching rubric. Use the server's AUTHOR_ASSIGNMENT workflow. Return the review link; do not publish.`}</pre></div>
              <div><p className="font-semibold">{msg("ai.pages.McpGuidePage.grading")}</p><pre className="mt-2 whitespace-pre-wrap break-words rounded-lg bg-sunken p-3 font-mono text-xs leading-6">{`List the submitted work for the class and assignment I specify. Use GRADE_WITH_RUBRIC, read every current context page, and propose per-item scores with evidence and explanations. Flag uncertainty for teacher review. Save proposals only and return review links; do not save, finalize or publish official grades.`}</pre></div>
            </div>
          </Card>
          <p className="text-sm text-muted">{msg("ai.pages.McpGuidePage.noNeedToDownloadA")}</p>
        </GuideSection>

        <GuideSection id="cong-cu" title={msg("ai.pages.McpGuidePage.toolList")}>
          <p>{msg("ai.pages.McpGuidePage.toolsAreActionsTheAgent")}<strong>{msg("ai.pages.McpGuidePage.readOnly")}</strong> {msg("ai.pages.McpGuidePage.doNotCreateBusinessRecords")}<strong>{msg("ai.pages.McpGuidePage.writeDraftsFeedback")}</strong> {msg("ai.pages.McpGuidePage.requireAReadWriteToken")}</p>
          <div className="space-y-8">
            {MCP_TOOL_GROUPS.map((group) => <section key={group.title} aria-label={group.title}>
              <h3 className="font-semibold text-ink">{group.title}</h3>
              <p className="mt-1 text-sm text-muted">{group.audience}</p>
              <Card as="dl" padding="none" className="mt-3 divide-y divide-line px-4 sm:px-5">
                {group.tools.map((tool) => <div key={tool.name} className="grid min-w-0 gap-2 py-4 xl:grid-cols-[minmax(0,0.9fr)_minmax(0,1.4fr)] xl:gap-5">
                  <dt className="min-w-0"><code className="break-words font-mono text-[13px] font-semibold text-ink">{tool.name}</code><span className="mt-1 block text-xs text-muted">{tool.write ? msg("ai.pages.McpGuidePage.writeDraftsFeedback") : msg("ai.pages.McpGuidePage.readOnly")}</span></dt>
                  <dd className="min-w-0 text-sm leading-6">{tool.description}</dd>
                </div>)}
              </Card>
            </section>)}
          </div>
        </GuideSection>

        <GuideSection id="xu-ly-loi" title={msg("ai.pages.McpGuidePage.troubleshooting")}>
          <dl className="space-y-4 text-sm">
            <div><dt className="font-semibold text-ink">{msg("ai.pages.McpGuidePage.n401OrCannotAuthenticate")}</dt><dd>{msg("ai.pages.McpGuidePage.checkThatTheTokenHas")}</dd></div>
            <div><dt className="font-semibold text-ink">{msg("ai.pages.McpGuidePage.toolsMissingOrPermissionDenied")}</dt><dd>{msg("ai.pages.McpGuidePage.readTheCapabilitiesAgainA")}</dd></div>
            <div><dt className="font-semibold text-ink">{msg("ai.pages.McpGuidePage.cannotReachTheServerAddress")}</dt><dd>{msg("ai.pages.McpGuidePage.localhostIsTheMachineRunning")}</dd></div>
            <div><dt className="font-semibold text-ink">{msg("ai.pages.McpGuidePage.contextNotFullyReadOr")}</dt><dd>{msg("ai.pages.McpGuidePage.forMcpContextReadRequired")}</dd></div>
            <div><dt className="font-semibold text-ink">{msg("ai.pages.McpGuidePage.timeoutWhileCreatingADraft")}</dt><dd>{msg("ai.pages.McpGuidePage.checkWhetherTheActionAlready")}</dd></div>
            <div><dt className="font-semibold text-ink">{msg("ai.pages.McpGuidePage.createdButTheAssignmentIs")}</dt><dd>{msg("ai.pages.McpGuidePage.openTheExactReviewurlThe")}</dd></div>
          </dl>
        </GuideSection>

        <GuideSection id="tham-khao" title={msg("ai.pages.McpGuidePage.officialDocumentation")}>
          <p className="text-sm">{msg("ai.pages.McpGuidePage.menusAndConfigurationLocationsMay")}</p>
          <ul className="list-disc space-y-2 pl-5 text-sm">
            {[
              [msg("ai.pages.McpGuidePage.codexMcpConnection"), "https://developers.openai.com/codex/mcp"],
              [msg("ai.pages.McpGuidePage.claudeCodeMcpAndConfiguration"), "https://code.claude.com/docs/en/mcp"],
              [msg("ai.pages.McpGuidePage.opencodeMcpServersAndAuthentication"), "https://opencode.ai/docs/mcp-servers/"],
              [msg("ai.pages.McpGuidePage.antigravitySettingUpMcpIn"), "https://codelabs.developers.google.com/getting-started-google-antigravity#6"],
            ].map(([label, href]) => <li key={href}><a href={href} target="_blank" rel="noreferrer" className="text-primary underline decoration-primary/40 underline-offset-4 hover:decoration-primary focus-visible:outline-2 focus-visible:outline-focus">{label}</a></li>)}
          </ul>
        </GuideSection>
      </article>
    </div>
  </div>;
}

/** Thẻ mục lục dính bên trái trang tài liệu. */
function GuideContents() {
  return (
    <Card as="nav" className="self-start lg:sticky lg:top-24" aria-label={msg("ai.pages.McpGuidePage.mcpDocumentationContents")}>
      <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-muted">{msg("ai.pages.McpGuidePage.inThisDocument")}</p>
      <ol className="grid gap-1 sm:grid-cols-2 lg:grid-cols-1">
        {CONTENTS.map(([id, label], index) => <li key={id}>
          <a href={`#${id}`} className="flex min-w-0 gap-3 rounded-lg px-2 py-2 text-[13px] text-body transition-colors hover:bg-sunken hover:text-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus">
            <span className="text-faint" aria-hidden="true">{index + 1}</span>{label}
          </a>
        </li>)}
      </ol>
    </Card>
  );
}

function GuideSection({ id, title, children }: { id: string; title: string; children: ReactNode }) {
  return <section id={id} aria-labelledby={`${id}-title`} className="min-w-0 scroll-mt-24 border-t border-line pt-7 first:border-0 first:pt-0">
    <h2 id={`${id}-title`} className="mb-4 text-xl font-semibold leading-7 text-ink">{title}</h2>
    <div className="space-y-4">{children}</div>
  </section>;
}
