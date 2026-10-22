package edu.aiplatform.backup.contract;

import java.time.Instant;

/** Failed terminal runs in [fromInclusive,toExclusive); last success across all history before toExclusive. */
public record BackupDashboardSummary(Instant fromInclusive, Instant toExclusive, Instant latestSuccessfulAt,
        long failedRuns, String latestSafeError) { }
