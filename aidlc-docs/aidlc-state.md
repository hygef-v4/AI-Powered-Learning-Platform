# AI-DLC State Tracking

## Project Information
- **Project Type**: Greenfield
- **Start Date**: 2026-09-12T15:04:50Z
- **Current Phase**: CONSTRUCTION
- **Current Stage**: Code Generation Part 1 complete except the paused U01 plan. Every unit has Functional Design, NFR Requirements, NFR Design, Infrastructure Design and a code generation plan; no application code generated yet.
- **Resume action**: Update and approve the paused U01 code plan (use real U02/U03 adapters); then start Code Generation Part 2 following the dependency order and critical path.

## Workspace State
- **Existing Code**: No
- **Programming Languages**: None yet (planned: Java 17 / Spring Boot 3, TypeScript / Next.js)
- **Build System**: None yet (planned: Maven, npm)
- **Reverse Engineering Needed**: No
- **Workspace Root**: `C:\Users\admin\Documents\GitHub\AI-Powered-Learning-Platform`

## Code Location Rules
- **Application Code**: Workspace root (NEVER in `aidlc-docs/`)
- **Documentation**: `aidlc-docs/` only
- **Structure Patterns**: See `construction/code-generation.md` in the AI-DLC rule details

## Extension Configuration
| Extension | Enabled | Decided At |
|---|---|---|
| Security Baseline | Yes | Requirements Analysis |
| Resiliency Baseline | Yes | Requirements Analysis |
| Property-Based Testing | No | Requirements Analysis |

**Phạm vi rule rút gọn (2026-09-24)**: Security chỉ áp dụng SECURITY-03, 04, 05, 08, 09, 12, 15; Resiliency chỉ áp dụng RESILIENCY-04, 06, 10. Các rule còn lại là N/A "ngoài phạm vi đồ án" ở mọi stage, không phải blocking finding. Xem `requirements.md` mục 12-13.

## Stage Progress

### INCEPTION
- [x] Workspace Detection
- [x] Reverse Engineering - skipped (greenfield)
- [x] Requirements Analysis
- [x] User Stories
- [x] Workflow Planning
- [x] Application Design (synchronized with unit designs 2026-09-25; global ERD removed - tables are defined per unit)
- [x] Units Generation (16 units)

### CONSTRUCTION
- [x] Functional Design - all 16 units
- [x] NFR Requirements - all 16 units
- [x] NFR Design - all 16 units
- [x] Infrastructure Design - all 16 units (+ `construction/shared-infrastructure.md`)
- [ ] Code Generation Part 1 (plans) - U02-U16 approved; U01 paused/unapproved
- [ ] Code Generation Part 2 (code) - not started
- [ ] Build and Test
- [ ] Operations (placeholder)

## Unit Progress

| Unit | Design stages | Code plan | Code |
|---|---|---|---|
| U01 Account & Access | Done | Paused, needs update | - |
| U02 Audit, Job & Event | Done | Approved | - |
| U03 File & Artifact | Done | Approved | - |
| U04 Subject, Class, Enrollment & Learning Access | Done | Approved | - |
| U05 Content, Material & RAG | Done | Approved | - |
| U06 Rubric & Question Bank | Done | Approved | - |
| U07 Payment & AI Credit | Done | Approved | - |
| U08 Assessment Core & Publication | Done | Approved | - |
| U09 Question Type Authoring | Done | Approved | - |
| U10 Template, Copy & Simulation | Done | Approved | - |
| U11 Attempt & Submission | Done | Approved | - |
| U12 Group & Allocation | Done | Approved | - |
| U13 AI & Code Execution | Done | Approved | - |
| U14 Group Document & Submission | Done | Approved | - |
| U15 Grading | Done | Approved | - |
| U16 Reporting & Notification | Done | Approved | - |

## Open Items
- MVP/Phase 2 team discussion pending: US-CNT-003, US-CNT-004, US-QBK-003, US-GRD-006..008, US-RPT-002..004 are outside current designs.
- Critical path (by plan steps): U01 → U04 → U05 → U08 → U09 → U10 → U11 → U15 → U16.
- VPS sizing suggestion: 4 vCPU / 8 GB RAM / 60 GB SSD (Judge0 included).

## History (summary)
- 2026-09-12..22: Inception completed and revised through several change requests (roles, Draw.io, group work, templates/simulation).
- 2026-09-24: Application Design corrected; unit split reworked from 17 to the current 16 units with four non-blocking waves; security/resiliency scope reduced; Construction started with U01.
- 2026-09-24..25: Per-unit design stages and code plans for U01-U16; decisions synced back to Inception (payment = AI credits, DRAWIO → DOCUMENT, no subject-wide assignments, versioning after retire, group work as a shared document, auto-grading on submit, automatic deadline reminders only).
- 2026-09-25: Inception clean-up: requirements/stories/personas/use-cases swept, application design files rewritten for 16 units, global ERD removed, dependency figure added.
- 2026-09-25: U03 simplified to avatar, material and document images (Draw.io XML lives inside documents, U09); dependency matrix and ports re-synced.
- Full chronological log: `audit.md`.
