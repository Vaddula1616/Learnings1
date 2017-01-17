package org.carlspring.strongbox.services.impl;

import org.carlspring.maven.artifact.downloader.IndexDownloader;
import org.carlspring.strongbox.artifact.locator.ArtifactDirectoryLocator;
import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.handlers.ArtifactLocationGenerateMavenIndexOperation;
import org.carlspring.strongbox.services.ArtifactIndexesService;
import org.carlspring.strongbox.services.RepositoryManagementService;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexManager;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Kate Novik.
 */
@Component("artifactIndexesService")
public class ArtifactIndexesServiceImpl
        implements ArtifactIndexesService
{

    private static final Logger logger = LoggerFactory.getLogger(ArtifactIndexesServiceImpl.class);

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private RepositoryIndexManager repositoryIndexManager;

    @Autowired
    private RepositoryManagementService repositoryManagementService;

    @Inject
    private IndexDownloader indexDownloader;

    @Override
    public void rebuildIndexes(String storageId,
                               String repositoryId,
                               String artifactPath)
            throws IOException
    {
        Storage storage = getConfiguration().getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);

        ArtifactLocationGenerateMavenIndexOperation operation = new ArtifactLocationGenerateMavenIndexOperation(repositoryIndexManager);

        operation.setStorage(storage);
        operation.setRepository(repository);
        operation.setBasePath(artifactPath);

        ArtifactDirectoryLocator locator = new ArtifactDirectoryLocator();
        locator.setOperation(operation);
        locator.locateArtifactDirectories();

        if (artifactPath == null)
        {
            repositoryManagementService.pack(storageId, repositoryId);
        }
    }

    @Override
    public void downloadRemoteIndex(String storageId,
                                    String repositoryId)
            throws PlexusContainerException, ComponentLookupException, IOException
    {

        Storage storage = getConfiguration().getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);
        String repositoryBasedir = repository.getBasedir();

        indexDownloader.setIndexingContextId(repositoryId + "/ctx");
        indexDownloader.setRepositoryId(repositoryId);
        indexDownloader.setRepositoryURL(repository.getRemoteRepository()
                                                   .getUrl());
        indexDownloader.setIndexLocalCacheDir(repositoryBasedir);
        indexDownloader.setIndexDir(new File(repositoryBasedir, ".index")
                                       .toString());
        indexDownloader.download();
    }

    @Override
    public void rebuildIndexes(String storageId)
            throws IOException
    {
        Map<String, Repository> repositories = getRepositories(storageId);

        for (String repository : repositories.keySet())
        {
            rebuildIndexes(storageId, repository, null);
        }
    }

    @Override
    public void rebuildIndexes()
            throws IOException
    {
        Map<String, Storage> storages = getStorages();
        for (String storageId : storages.keySet())
        {
            rebuildIndexes(storageId);
        }
    }

    private Configuration getConfiguration()
    {
        return configurationManager.getConfiguration();
    }

    private Map<String, Storage> getStorages()
    {
        return getConfiguration().getStorages();
    }

    private Map<String, Repository> getRepositories(String storageId)
    {
        return getStorages().get(storageId).getRepositories();
    }
}
