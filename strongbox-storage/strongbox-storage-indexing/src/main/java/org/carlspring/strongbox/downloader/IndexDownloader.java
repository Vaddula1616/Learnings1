package org.carlspring.strongbox.downloader;

import org.carlspring.strongbox.storage.indexing.IndexerConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.maven.index.Indexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author carlspring
 */
@Component("indexDownloader")
public class IndexDownloader
{

    private static final Logger logger = LoggerFactory.getLogger(IndexDownloader.class);

    @Inject
    private IndexerConfiguration indexerConfiguration;

    @Inject
    private IndexUpdater indexUpdater;

    private ResourceFetcher indexResourceFetcher = new IndexResourceFetcher();

    private IndexingContext indexingContext;

    public void download(IndexDownloadRequest request)
            throws IOException
    {
        // Files where local cache is (if any) and Lucene Index should be located
        File localCache = new File(request.getIndexLocalCacheDir());
        File indexDir = new File(request.getIndexDir());

        Indexer indexer = request.getIndexer();

        // Create context for repository index
        indexingContext = indexer.createIndexingContext(request.getIndexingContextId(),
                                                        request.getRepositoryId(),
                                                        localCache,
                                                        indexDir,
                                                        request.getRemoteRepositoryURL(),
                                                        null,
                                                        true,
                                                        true,
                                                        indexerConfiguration.getIndexersAsList());

        // Update the index (an incremental update will be performed,
        // if this is not the first run and the files are not deleted.

        logger.debug("Updating remote index for " + request.getRepositoryId() + "...");
        logger.debug("(This might take a while on first run, so please be patient)!");

        Date contextCurrentTimestamp = indexingContext.getTimestamp();
        IndexUpdateRequest updateRequest = new IndexUpdateRequest(indexingContext, indexResourceFetcher);
        IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);
        if (updateResult.isFullUpdate())
        {
            logger.debug("Performed a full index update for " + request.getStorageId() + ":" +
                         request.getRepositoryId() + ".");
        }
        else if (updateResult.getTimestamp()
                             .equals(contextCurrentTimestamp))
        {
            logger.debug("No update required for remote index " +
                         request.getStorageId() + ":" + request.getRepositoryId() + "," +
                         " as the index is up to date!");
        }
        else
        {
            logger.debug("Performed an incremental update, with changes covering the period between " +
                         contextCurrentTimestamp + " - " + updateResult.getTimestamp() + ".");
        }
    }

    public ResourceFetcher getIndexResourceFetcher()
    {
        return indexResourceFetcher;
    }

    public void setIndexResourceFetcher(ResourceFetcher indexResourceFetcher)
    {
        this.indexResourceFetcher = indexResourceFetcher;
    }

    public IndexingContext getIndexingContext()
    {
        return indexingContext;
    }

}
