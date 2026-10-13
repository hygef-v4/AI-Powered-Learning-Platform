# SRS whole-document review — 2026-10-09

Source: [Report 3 SRS](https://docs.google.com/document/d/1ebPCxJyusasIm8aiMmm3pWiIAaEr7D7n/edit). Reviewed fresh Office DOCX modified at 2026-10-09T10:28:05.779Z. This is a review; the online document and its template/images were not modified in this task.

## Conclusion

The SRS is not synchronized. I.5 contains 51 screens and III.1–III.6 contains the corresponding 51 descriptions, but several diagrams and mockups still represent old behavior. I.1–I.4 and parts of II also conflict with the current AI-DLC and the revised III/IV/V. Matching screen counts does not establish matching functionality.

Previous edits updated prose/action lists/field tables and intentionally preserved embedded images. Consequently, text and images are now visibly inconsistent in several places. This review also identifies two errors in previously revised prose: unsupported Admin AI metrics and an overgeneralized AI grading deadline.

Authority: [current SRS contract](../current-srs-contract.md), [requirements](../../inception/requirements/requirements.md), [73 use cases](../../../docs/history/use-cases-73.md), and current unit business rules/entities. These take precedence over obsolete controls in screenshots. Preserve the template; repair content and replace affected images within their existing figure positions.

## Findings: System Functionalities and corresponding screens

| ID / priority | Exact location and observed evidence | Required correction |
|---|---|---|
| S01 / High | I.5.1.b #24/#32 and III.3.7 Figure 27 / III.4.3 Figure 43 describe viewer-requested summary. Neither mockup shows the summary action, shared result or processing state. Figure 27 still shows **Thay tệp** and editable source/module controls. | Add **Tóm tắt tài liệu** to Material Detail and Learning Material, available to scoped Student/Teacher/Subject Manager viewers; show quote, processing/shared result and retained summary on embedding failure. Source is read-only; management controls are conditional on edit scope. Replacement is a new lesson, not overwriting the existing source. |
| S02 / High | III.2.9 Figure 17, III.3.6 Figure 26 and III.5.1 Figure 60 still show scan-oriented statuses; Figures 17/26 show **Thử lại** for missing captions. I.5 #23/#24, III field tables and V BR04 explicitly prohibit manual rescan. | Remove manual retry/rescan. Distinguish extraction from requested summary/indexing, including EXTRACTED. Upload only stores/extracts; no AI/HOLD at upload. |
| S03 / High | I.5 #21 says Practice-only; III.3.4 Figure 24 includes **Kiểm tra giữa kỳ / Graded** and a mode filter. III.3.5 Figure 25 lacks the newly described review/publication and attempt controls. | Remove Graded Quiz and mode selector; show Practice-only, lesson link, lifecycle, optional timer, attempts and review/publish actions. No calendar schedule. |
| S04 / High | I.5 #25 and III.3.8 description specify subject Text/Diagram/Code assignments. Figure 28 instead selects individual classes and includes **Group Essay**, **Practice Quiz**, due dates for Quiz and a grading shortcut. | Redraw this as the Subject Manager's subject assignment list: three non-Quiz types, one schedule across OPEN classes. Grading remains under assigned teaching scope. Class Group authoring belongs to the shared form entered through Teacher Class Detail, not this subject list. |
| S05 / High | I.5 #45 / III.5.1 field table excludes Quiz from the assessment list. Figure 61 mixes **Practice Quiz** into that list with a deadline/submitted count; III.4.2 Figure 40 also lists a Practice Quiz with **30/10** in its deadline column. III.5.2 Figure 63 includes **Kiểm tra giữa kỳ / Quiz** among class submissions. | Provide the separate material-linked Practice Quiz entry; remove calendar deadlines and ordinary grading/submission-queue treatment for Quiz. Keep Practice results outside Gradebook and teacher grading. |
| S06 / High | I.5 #19/#20 and III.3.2 field table say bank questions have no rubric. Figure 22 has a **RUBRIC** column, “Có rubric” and “5 rubric” on document/group skeleton rows. It also lacks the newly documented import/AI actions. | Remove bank-owned rubrics. Keep type/version/status management; add CSV/XLSX import and authorized AI question drafting. Rubrics are created for assignment questions/leaf sections after content is used in an assignment. |
| S07 / High | I.5 #33 and III.4.4 text say no comments. Figures 44–45 visibly contain **Bình luận**, comment inputs and moderation/visibility links. | Remove comment lists, inputs and moderation controls from both mockups. Keep announcement create/update/soft-delete and creation-only notification. |
| S08 / High | I.5 #8 / III.2.3 table describes rate per minute and daily cost cap. Figure 9 instead uses **Số yêu cầu/ngày**, including 500/2000/300/1000, and **Quét và nhúng học liệu**. | Show current Settings keys/units: global switch, task models/enabled, rate/minute and USD/day cap; viewer-requested summary/embedding. Show Credit and File group detail states as applicable, rather than treating this one AI screenshot as coverage of every setting. |
| S09 / High | I.5 #6 and III.2.1 description/field table add aggregate AI usage, calls, tokens, latency and cost, absent from Figure 7. V BR25 correctly defines administrative counts only. U16 BR-U16-40/41 restricts this dashboard to counts; U13 removed `/admin/ai/usage`. | Remove unsupported AI metrics from #6 and III.2.1, rather than adding an AI metrics screenshot. Keep account role/status, subject/class status and active-enrollment counts. This corrects an error introduced during the prior prose revision. |
| S10 / Medium | I.5 #30 says Student/Teacher only, whereas III.4.1 and IV.2.1 include Subject Managers assigned to teach. I.5.1.a has Manager Dashboard and Class Dashboard but does not clearly express how a Subject Manager switches into assigned teaching scope. Shared destinations are represented by unlabeled connecting lines. | Add Subject Manager assigned teaching to #30; annotate the Manager-to-teaching entry and role guards. Explain common shell/profile/notification and wallet access without suggesting Admin owns a wallet. Do not create a separate Notifications page. |
| S11 / Medium | Figure 30 has author-configured **Giới hạn ký tự 3.000**; Figure 51 shows **512 / 3.000 ký tự**. III.3.9 and III.4.9 correctly specify no author word/line limit and a technical 1,000,000-character cap (U09 BR-U09-21). | Remove configurable 3,000-character restriction; align the answer counter with the technical cap. Figure 51 also shows a table toolbar icon; Text Essay permits basic formatting but no tables/images/diagrams (U09 BR-U09-20). |
| S12 / Medium | Figures 48–49 and 64–65 advertise **5 credit** for Practice or Teacher AI; Figure 64 estimates 90 credits for 18 submissions. Field tables now use an actual quote/settlement rule. | Replace fixed-price wording with request-specific estimated credit quote and actual-usage settlement. A sample history debit of -5 is not itself wrong; the issue is promising a universal 5-credit price. |
| S13 / Medium | III.3.12 Figure 36 lacks the newly documented lifecycle, remove-enrollment and distribution controls. Figure 37 lists ordinary Teachers only, although an eligible ACTIVE Subject Manager may be assigned to teach. | Add appropriate structural-class actions and distribution toggle/state; allow eligible ACTIVE Teacher/Subject Manager choices, never Admin. Keep structural management distinct from teaching resources. |
| S14 / Medium | III.4.11 Figure 53 depicts member claiming/completing and leader-only submit, but no clear leader allocation/reassignment/release state corresponding to the text. Figure 32 also exposes group-size/allocation settings that should be read consistently with stable class groups. | Add the leader view/state for section allocation/reassignment and release rules; distinguish DONE-only manual snapshot from automatic inclusion of saved CLAIMED drafts. Do not imply per-assignment recreation of class groups. |
| S15 / Medium | Figure 65 has no visible correction-reason/history or group-member score state, though III.5.4 specifies both; Figure 52 has import but no newly documented **Xuất DOCX**. | Supply the missing UI states within these existing screen sections or state explicitly that each screenshot shows only one state. Keep required correction reasons and manual group-contribution scoring. |
| S16 / Medium | Figures 8/66/67 display a 50-credit monthly grant; text specifies Settings, default 100. A configured amount of 50 can be valid, but the illustrative dataset is not explained. | Label values as configured sample data or use a coherent current default across Settings/wallet/package views. Do not change the policy back to fixed 50. |
| S17 / Medium | I.5 contains 51 screen rows, but its descriptions are much longer than the requested short English descriptions and differ from III in details: #24 includes order/archive as detail actions whereas III puts ordering on the list; common logout/notification is described only inside III's Profile field table. | Use one short English purpose sentence per inventory row. Describe conditional actions in III; consistently locate ordering on the list and logout/notification in the shared shell. Preserve Vietnamese UI field/button names. |

## Findings: remaining sections

| ID / priority | Exact location and observed evidence | Required correction |
|---|---|---|
| D01 / High | I.1 text and Figure 1.1 include Admin **Subjects, Classes & Enrollment**. Text grants all four roles personal credit functions and Teacher inheritance to Admin. I.4.3.5 Admin diagram directly inherits Subject Manager; I.4.4 matrix gives Admin R2/R4 and Full class/enrollment actions. | Admin inherits User/admin functionality only, cannot be assigned to manage/teach, has no wallet/AI/code or academic scope. Fix context text/image, actor descriptions, inheritance image, matrix and R1–R5 notes together. Common credit UCs08–11 belong only to Student/Teacher/Subject Manager. |
| D02 / High | I.2.2 BF-02 diagram routes upload → extract → **AI credit OK?** → **Embedding created?** → settle; its end description says indexed or awaits rescan. | Redraw extraction-only upload ending EXTRACTED, then a separate explicit viewer-requested summary/embedding branch. Add payer, shared result/deduplication and rejection without losing material viewing. No manual rescan. |
| D03 / High | I.2.6 BF-06 **GRADED** diagram begins with **Quiz or Code Lab?** and sends Quiz through official grade publication. | Only Code uses deterministic official scoring here; Quiz is Practice-only and goes to its own private result flow. |
| D04 / High | I.2.7 BF-07 diagram says **Error, credit released**; its end text says no credit charged. II UC29 and UC38 exceptions likewise release reserved credits unconditionally on failure/timeout. | Settle recorded actual usage and return unused credits; fully release only if usage is zero. Preserve saved work and permitted retry. Rejection before acceptance does not charge. |
| D05 / High | I.3 Figure 3.1 ERD includes **AI Service**, Rubric **Version**, and Account **commenting_on** Announcement. Entity tables define independently versioned rubrics, deployment-only packages, subject templates and old lesson indexing without EXTRACTED. I.3.3 still includes invitation-code enrollment, pinned rubric versions, AI Service GLOBAL rows and comment moderation. | Align conceptual entities and descriptions: Settings replaces AI Service configuration; assignment-owned non-versioned rubrics; editable Admin packages; direct subject assignments with shared schedule, no template entity; extraction/summary state separation; no comments/invitation codes. Keep referenced question versioning. |
| D06 / High | II UC43/UC44/UC38/UC39 list **Administrator** as primary actor and permit assigned-Admin teaching. UC43/44 start from obsolete **Teacher Assignment List**, **Codelab Editor**, **Diagram Essay Editor** labels. | Remove Admin. Describe Teacher R3/Subject Manager R4 class work and R2 subject authoring where applicable; use Teacher Class Detail/Assignment List → Assignment Form. UC44 automatically creates empty part rubrics; user fills them, rather than selecting/creating ACTIVE rubric versions. |
| D07 / Medium | I.4.2 UC15 and material-management descriptions omit the new viewer summary action. Student/Teacher/Subject Manager UC diagrams have no explicit summary action; Subject Manager diagram primarily relies on inherited Teacher UCs and does not clearly express subject assignment/Quiz authoring. | Add summary as a supporting action under existing material UCs, with current viewing scope and requester payer. Explicitly annotate subject versus assigned-class authoring. Keep the accepted 73-UC catalog; no new UC needed solely for this button. |
| D08 / Medium | I.2.3 BF-03 has Teacher/class-only trigger and swimlane; it does not represent Subject Manager subject assignments. I.2.8 BF-08 uses generic **User**, while the current wallet policy excludes Admin. | Add subject-authoring/common-schedule route to BF-03; qualify wallet eligibility in BF-08. BF-01's pictured Admin subject creation → Subject Manager class/enrollment division is substantially aligned and should be preserved. |
| D09 / High | III JOB09/JOB14, IV.2.2 grading deadline and V BR22 use **five minutes per processing item** without limiting that start point to batches. | Ordinary Practice/proposal request: five minutes from clicking/request acceptance. Batch: five minutes from each item's processing start (U13 BR-U13-24). Material summary retains its separate 24-hour deadline and 25-hour HOLD fallback. Prior prose generalized the batch rule incorrectly. |
| D10 / Medium | II UC24/25/26 start from **Text Essay Assignments / Codelab Assignments / Diagram Essay Assignments**; the inventory has one Student Assignments screen. | Replace with Student Assignments filtered by type → Assignment Detail → corresponding workspace. These are not three missing standalone screens. |
| D11 / Medium | Change Log latest entry says **screens #1–#55**. Current inventory and III captions have #1–#51. TOC still says JOB06 **Extract and Index** and JOB09 **Release**, and lists only 16 detailed jobs, omitting added III.7.17. | After actual synchronization, add an accurate change-log entry and refresh generated TOC/fields without rebuilding the template. Do not rewrite historical evidence as implementation completion. |

## What is already substantially aligned

- I.5.2 / III.8 both contain eight integration categories; internal REST routes are separate. III.8 material/Gemini/Drive scopes are consistent with the viewer-request revision.
- I.5.3 / III.7 contain seventeen jobs; JOB06/07 extract without AI/HOLD and JOB17 handles explicit viewer summary/embedding. TOC is stale, not the detailed job count.
- III contains all 51 inventory screen IDs and 69 mockup captions; role/type variants and popups are states of those screens, not extra standalone screens.
- III.7 material recovery and IV/V largely express the single requester HOLD, checkpoints, retained summary, bounded retries and 24/25-hour rules correctly. They do not cure obsolete BF-02/ERD/mockups.
- V expresses no Admin academic/wallet rights, no bank-owned rubric, no comments, Practice-only unscheduled Quiz and subject shared schedules correctly. I/II still contradict these.
- Login/activation/recovery/profile/password-change, basic account/package/audit screens, Practice history/taking/result and the pictured Gradebook generally match their stated main purpose. Screenshots are illustrative states; missing state controls are not automatically missing screens.

## Image inspection coverage

All 86 embedded image occurrences were displayed and visually reviewed in document order: one cover logo, 16 conceptual/flow/UC/screen-flow diagrams, and 69 screen mockups. Four-image contact sheets were used; the screen flow was additionally inspected at original resolution. Findings cite visible labels/actions rather than claiming unreadable small text was verified pixel by pixel.

| Embedded occurrences | Figures / content | Review result |
|---|---|---|
| 1 | Cover logo | Decorative; no functional requirement. |
| 2 | Context Figure 1.1 | D01; add material summary exchange. |
| 3 | BF01 | Main ownership division substantially aligned. |
| 4 | BF02 | D02: old automatic indexing flow. |
| 5 | BF03 | D08: class-only authoring. |
| 6 | BF04 | Main autosave/submission shape aligned; document attempt-specific deadlines separately. |
| 7 | BF05 | Main claim/Done/leader/automatic submission shape aligned; distinguish unfinished manual versus automatic snapshots. |
| 8 | BF06 | D03: Graded Quiz branch. |
| 9 | BF07 | D04: unconditional credit release. |
| 10 | BF08 | D08: qualify eligible buyers; verified payment/idempotency shape aligned. |
| 11 | ERD Figure 3.1 | D05: AI Service/rubric version/comments and related descriptions. |
| 12–16 | User, Student, Teacher, Subject Manager, Admin UC diagrams | D01/D07: inheritance/credit/summary/scope. |
| 17 | I.5.1.a Screen Flow | All screen names represented; S10/S17 require scope/navigation clarification. |
| 18–23 | III Figures 1–6 | Authentication/profile main purpose aligned. |
| 24–26 | Figures 7–9 | S08/S09: counts versus AI metrics and Settings units. |
| 27–33 | Figures 10–16 | Basic account/payment/subject main purpose aligned; Figure16 structural read-only class references do not themselves grant academic access. |
| 34 | Figure17 Subject Manager subject view | S02: scan/retry controls. |
| 35–38 | Figures18–21 | Package/audit/main manager purpose aligned; status sample wording still needs extraction-phase consistency. |
| 39–40 | Figures22–23 | S06: bank rubric and supporting lifecycle/import/AI states. |
| 41–44 | Figures24–27 | S01–S03: Quiz mode and material source/summary/rescan. |
| 45–50 | Figures28–33 | S04/S11/S14: assignment scope, essay limit, group states; rubric score-total concept substantially aligned. |
| 51–54 | Figures34–37 | S13: missing structural states and eligible staff selection. |
| 55–59 | Figures38–42 | Class/group access main purpose aligned; Figure40 Quiz deadline is S05. |
| 60–62 | Figures43–45 | S01/S07: missing summary and obsolete comments. |
| 63–66 | Figures46–49 | Student assignment main purpose aligned; S12 fixed AI cost wording. |
| 67–70 | Figures50–53 | S11/S14/S15: Text Essay limit/toolbar, group/diagram states. |
| 71–74 | Figures54–57 | Practice Quiz history/detail/taking/result main purpose aligned; visibility follows settings, not always full results. |
| 75–80 | Figures58–63 | Teacher class/main Gradebook aligned; S02/S05/S13 for material status, Quiz and scope. |
| 81–82 | Figures64–65 | S12/S15: fixed AI price and missing correction/group states. |
| 83–86 | Figures66–69 | Wallet/verified PayOS main purpose aligned; S16 sample monthly amount. |

## Recommended synchronization order

1. Fix authority conflicts first: I.1/I.4 rights + diagrams, I.2 BF02/BF06/BF07, I.3 model/rules and II actors/rubrics/credit exceptions.
2. Synchronize I.5 short inventory sentences, III descriptions/actions/fields and affected mockups as one change. Keep 51 screens, existing headings/tables/captions and English descriptions; keep Vietnamese visible field labels.
3. Remove unsupported Admin metrics and repair ordinary-versus-batch grading deadline in III/IV/V. Do not expand scope to make an obsolete screenshot valid.
4. Refresh TOC/change log after edits; verify figure references, 51 screen IDs, 73 UC mapping, 8 APIs and 17 jobs, then visually recheck the resulting document including images.

No new business decision is needed for the confirmed contradictions above; current AI-DLC already resolves them. No code, runtime tests, approvals, commit or push are implied by this review.

## UI-description update after this review

On 2026-10-09, the user explicitly requested: **Sửa description theo đúng UI đang hiển thị**. The online SRS Screen List and III.1–III.6 descriptions, actions and field tables now describe all 51 screens as pictured. Images and AI-DLC requirements are unchanged. Findings above that concern obsolete UI/business rules remain applicable to the later screenshot update; they are not resolved by making prose match the current images. The uploaded file was verified by identical raw Drive readback.
