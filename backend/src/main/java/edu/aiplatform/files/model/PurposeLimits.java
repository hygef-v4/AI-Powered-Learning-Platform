package edu.aiplatform.files.model;

import edu.aiplatform.files.contract.ArtifactPurpose;
import java.util.Set;

/** Trần dung lượng và loại tệp cho phép hiện hành của một {@code purpose}. */
public record PurposeLimits(ArtifactPurpose purpose, int maxSizeMb, Set<FileType> allowedTypes) {

    public long maxBytes() {
        return maxSizeMb * 1024L * 1024L;
    }
}
