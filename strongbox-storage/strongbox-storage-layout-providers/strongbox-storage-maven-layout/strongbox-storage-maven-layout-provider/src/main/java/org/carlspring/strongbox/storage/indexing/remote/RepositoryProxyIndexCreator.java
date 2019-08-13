package org.carlspring.strongbox.storage.indexing.remote;

import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.providers.io.RepositoryPathResolver;
import org.carlspring.strongbox.service.ProxyRepositoryConnectionPoolConfigurationService;
import org.carlspring.strongbox.storage.indexing.*;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexDirectoryPathResolver.RepositoryIndexDirectoryPathResolverQualifier;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexCreator.RepositoryIndexCreatorQualifier;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexingContextFactory.RepositoryIndexingContextFactoryQualifier;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.repository.RepositoryTypeEnum;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;

import org.apache.maven.index.incremental.DefaultIncrementalHandler;
import org.apache.maven.index.updater.DefaultIndexUpdater;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author carlspring
 * @author Przemyslaw Fusik
 */
@Component
@RepositoryIndexCreatorQualifier(RepositoryTypeEnum.PROXY)
public class RepositoryProxyIndexCreator
        extends AbstractRepositoryIndexCreator
{

    private static final Logger logger = LoggerFactory.getLogger(RepositoryProxyIndexCreator.class);

    private final IndexUpdater indexUpdater = new DefaultIndexUpdater(new DefaultIncrementalHandler(), null);

    @Inject
    private ProxyRepositoryConnectionPoolConfigurationService proxyRepositoryConnectionPoolConfigurationService;

    @Inject
    private ResourceFetcherFactory resourceFetcherFactory;

    @Inject
    private RepositoryPathResolver repositoryPathResolver;

    @Inject
    @RepositoryIndexingContextFactoryQualifier(IndexTypeEnum.REMOTE)
    private RepositoryIndexingContextFactory indexingContextFactory;

    @Inject
    @RepositoryIndexDirectoryPathResolverQualifier(IndexTypeEnum.REMOTE)
    private RepositoryIndexDirectoryPathResolver indexDirectoryPathResolver;

    @Override
    protected void onIndexingContextCreated(final RepositoryPath repositoryIndexDirectoryPath,
                                            final RepositoryCloseableIndexingContext indexingContext)
            throws IOException
    {
        final Repository repository = indexingContext.getRepositoryRaw();

        final String storageId = repository.getStorage().getId();
        final String repositoryId = repository.getId();

        final IndexUpdateResult updateResult = fetchIndex(indexingContext, repository, storageId, repositoryId);

        final Date contextCurrentTimestamp = indexingContext.getTimestamp();
        if (Objects.equals(updateResult.getTimestamp(), contextCurrentTimestamp))
        {
            logger.debug(String.format("No update required for remote index %s:%s, as the index is up to date!",
                                       storageId, repositoryId));
            if (!IndexPacker.packageExists(repositoryIndexDirectoryPath))
            {
                IndexPacker.pack(repositoryIndexDirectoryPath, indexingContext);
            }
            return;
        }
        if (updateResult.isFullUpdate())
        {
            logger.debug(String.format("Performed a full index update for %s:%s.", storageId, repositoryId));

        }
        else
        {
            logger.debug(
                    String.format(
                            "Performed an incremental update, with changes covering the period between %s - %s.",
                            contextCurrentTimestamp, updateResult.getTimestamp()));
        }
        IndexPacker.pack(repositoryIndexDirectoryPath, indexingContext);
    }

    private IndexUpdateResult fetchIndex(final RepositoryCloseableIndexingContext indexingContext,
                                         final Repository repository,
                                         final String storageId,
                                         final String repositoryId)
            throws IOException
    {
        logger.debug(String.format("Downloading remote index for %s:%s ...", storageId, repositoryId));

        final IndexUpdateRequest updateRequest = new IndexUpdateRequest(indexingContext,
                                                                        resourceFetcherFactory.createIndexResourceFetcher(
                                                                                indexingContext.getRepositoryUrl(),
                                                                                proxyRepositoryConnectionPoolConfigurationService.getHttpClient()));

        updateRequest.setIndexTempDir(
                RepositoryFiles.temporary(repositoryPathResolver.resolve(repository)).toFile());

        return indexUpdater.fetchAndUpdateIndex(updateRequest);
    }

    @Override
    protected RepositoryIndexingContextFactory getRepositoryIndexingContextFactory()
    {
        return indexingContextFactory;
    }

    @Override
    protected RepositoryIndexDirectoryPathResolver getRepositoryIndexDirectoryPathResolver()
    {
        return indexDirectoryPathResolver;
    }
}
