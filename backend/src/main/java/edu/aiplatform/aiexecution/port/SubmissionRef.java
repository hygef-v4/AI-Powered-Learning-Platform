package edu.aiplatform.aiexecution.port;

import java.util.UUID;

/** Bài nộp cá nhân (ATTEMPT) hoặc tài liệu chung nhóm (GROUP_DOCUMENT). */
public record SubmissionRef(String kind, UUID id) {}
