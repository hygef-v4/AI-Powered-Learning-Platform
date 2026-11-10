package edu.aiplatform.files.contract;

import edu.aiplatform.identity.contract.Actor;

/** Owner rechecks current access when a previously issued download grant is redeemed. */
public interface IArtifactDownloadPolicy {
    ArtifactPurpose purpose();

    void requireDownload(Actor actor, String fileId);
}
