package org.carlspring.strongbox.testing;

import org.apache.maven.artifact.Artifact;
import org.carlspring.maven.commons.util.ArtifactUtils;
import org.carlspring.strongbox.config.CommonConfig;
import org.carlspring.strongbox.config.StorageApiConfig;
import org.carlspring.strongbox.StorageIndexingConfig;
import org.carlspring.strongbox.config.ClientConfig;
import org.carlspring.strongbox.config.DataServiceConfig;
import org.carlspring.strongbox.services.RepositoryManagementService;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexManager;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * @author carlspring
 */
@ContextConfiguration
public class TestCaseWithArtifactGenerationWithIndexing
        extends TestCaseWithArtifactGeneration
{

    @Configuration
    @Import({
            StorageIndexingConfig.class,
            StorageApiConfig.class,
            CommonConfig.class,
            ClientConfig.class,
            DataServiceConfig.class
    })
    public static class SpringConfig { }

    @Autowired
    private RepositoryIndexManager repositoryIndexManager;

    @Autowired
    private RepositoryManagementService repositoryManagementService;


    public void addArtifactToIndex(File repositoryBasedir,
                                   String storageId,
                                   String repositoryId,
                                   String artifactPath)
            throws IOException
    {
        File artifactFile = new File(repositoryBasedir, artifactPath);

        Artifact artifact = ArtifactUtils.getArtifactFromGAV("org.carlspring.strongbox:strongbox-utils:6.2.2:jar");

        RepositoryIndexer indexer = repositoryIndexManager.getRepositoryIndex(storageId + ":" + repositoryId);

        indexer.addArtifactToIndex(repositoryId, artifactFile, artifact);
    }

    public RepositoryIndexManager getRepositoryIndexManager()
    {
        return repositoryIndexManager;
    }

    public RepositoryManagementService getRepositoryManagementService()
    {
        return repositoryManagementService;
    }

}
