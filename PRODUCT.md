# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Primary: Giảng viên (teacher) and Chủ nhiệm môn (subject manager).** They author assignments and quizzes, grade submissions against rubrics, and run their classes day to day. The interface is optimized for their repeated work first.
- **Secondary: Sinh viên (student).** Does assignments and quizzes, works in group documents, reads feedback and grades, often on a phone.
- **Secondary: Quản trị viên (admin).** Manages accounts, subjects, settings, audit log and backups.

A subject manager is also a teacher: their teaching area is identical to a teacher's, with management areas (subject content, managed classes) added on top.

## Product Purpose

An AI-assisted learning platform for an FPT university setting: classes, assignments, quizzes, materials, announcements, groups and grading in one place. Success means a teacher can author and grade quickly with AI help while keeping the final decision, and a student can understand at a glance what to do next.

## Positioning

- Assignments are documents: authoring, doing and grading happen in a Google Docs–style page, with rubrics attached beside each part like margin comments.
- Draw.io diagrams live directly inside assignments, fitting software analysis and design subjects (UML).
- AI drafts and grading proposals are suggestions; the teacher accepts or edits them.

## Operating Context

- Business rules, roles and flows follow the AI-DLC docs in `aidlc-docs/`; UI work must not invent screens or features absent from that spec (no "Today" dashboards or similar).
- Existing navigation per role: Student: Lớp của tôi, Bài tập, Luyện tập Quiz, Thông báo lớp. Teacher: Lớp được phân công, Thông báo lớp. Subject manager: Tổng quan, Môn học (Thông tin môn, Câu hỏi, Quiz, Bài tập, Học liệu), Quản lý (Lớp của môn), Giảng dạy (same as teacher). Admin: Tổng quan, Tài khoản, Môn học, Cài đặt, Nhật ký audit.
- Assignment parts always keep one empty line at the end of each part so authors and students can keep typing.

## Capabilities and Constraints

- Stack: Next.js (App Router) + React + Tailwind v4 in `frontend/`; Spring backend; strict CSP with nonces; self-hosted fonts.
- Vietnamese UI throughout; school emails on @fpt.edu.vn.
- Light and dark themes are both required. Until the user picks one, the theme follows the operating system setting (confirmed 2026-11-10).
- Short popups stay centered dialogs; long forms are made as compact as possible and become their own page if they still don't fit one screen. No redundant buttons (no Cancel beside a close X) and no hint text explaining what a field does.
- Near-identical create forms are merged into one form with extra fields rather than split by function.

## Brand Commitments

User-confirmed (binding):
- Name: "Nền tảng học tập". Logo: hanko seal with the kanji 学.
- Palette trial updated 2026-11-01 from the user's white-background reference: light canvas #FAFAFA, white sidebar/top bar/cards, neutral gray borders and text. Muted blue #466A96 is reserved for controls and selected states rather than tinting large surfaces. This replaces the cooler Slate Blue light surfaces the user found tiring. Dark theme retains slate surfaces with pale blue accents. Semantic success/warning/error colors remain distinct. Remove the oversized shell watermark and soften shadows; retain the small brush logo's brand colors.
- Font: Plus Jakarta Sans is being trialed at the user's request (2026-11-01); Be Vietnam Pro was the previous approved choice.
- Shell: left sidebar holds only page links; notifications, the theme switch and the account menu (profile, AI connection, sign out) sit in a top bar. There is no global search (confirmed 2026-11-10).
- Style: modern minimalist, "less is more": show only what the user needs, neatly arranged, understandable on one screen, with small interaction effects.

## Evidence on Hand

- AI-DLC requirements, stories and unit designs in `aidlc-docs/`.
- No testimonials, metrics or customer claims exist; do not fabricate any.

## Product Principles

1. Show only what the current task needs; everything else is one step away.
2. One screen, one main action; never repeat a control that already exists on the screen.
3. The teacher decides; AI proposes.
4. Assignments read and behave like documents.
5. Shared screens across roles; a role only adds its own parts.

## Accessibility & Inclusion

- WCAG 2.2 AA: contrast, keyboard access, visible focus, screen reader labels, reduced motion.
- Must work well on phones (students do work and check grades on mobile).
