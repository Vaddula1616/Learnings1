package org.carlspring.strongbox.services;

import java.io.IOException;

/**
 * @author Kate Novik.
 */
public interface ArtifactIndexesService
{

    /**
     * Rebuild indexes for artifact using artifactPath (string)
     * or for all artifacts in repository, when artifactPath is null
     *
     * @param storageId    String
     * @param repositoryId String
     * @param artifactPath String
     */
    void rebuildIndexes(String storageId,
                        String repositoryId,
                        String artifactPath)
            throws IOException;

    /**
     * Rebuild indexes for all artifacts in storage
     *
     * @param storageId String
     */
    void rebuildIndexes(String storageId)
            throws IOException;

    /**
     * Rebuild indexes for artifacts in all storages
     */
    void rebuildIndexes()
            throws IOException;

}
