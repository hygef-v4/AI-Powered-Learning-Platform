package edu.aiplatform.ai.contract;

import java.util.UUID;

/** versionNo is absent for uploaded prompt sources which are not versioned U13 materials. */
public record MaterialText(UUID materialId, String title, String text, boolean truncated,
                           Integer versionNo) {
}
