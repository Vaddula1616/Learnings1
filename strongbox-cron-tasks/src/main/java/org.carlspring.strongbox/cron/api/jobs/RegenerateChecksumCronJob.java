package org.carlspring.strongbox.cron.api.jobs;

import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.cron.config.JobManager;
import org.carlspring.strongbox.cron.domain.CronTaskConfiguration;
import org.carlspring.strongbox.services.ChecksumService;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kate Novik.
 */
public class RegenerateChecksumCronJob
        extends JavaCronJob
{

    private final Logger logger = LoggerFactory.getLogger(RegenerateChecksumCronJob.class);

    @Autowired
    private ChecksumService checksumService;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private JobManager manager;


    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext)
            throws JobExecutionException
    {
        logger.debug("Executed RegenerateChecksumCronJob.");

        CronTaskConfiguration config = (CronTaskConfiguration) jobExecutionContext.getMergedJobDataMap()
                                                                                  .get("config");
        try
        {
            String storageId = config.getProperty("storageId");
            String repositoryId = config.getProperty("repositoryId");
            String basePath = config.getProperty("basePath");

            /**Values of forceRegeneration are true - to re-write existing checksum and to regenerate missing checksum,
             false - to regenerate missing checksum only*/
            boolean forceRegeneration = Boolean.valueOf(config.getProperty("forceRegeneration"));

            if (storageId == null)
            {
                Map<String, Storage> storages = getStorages();
                for (String storage : storages.keySet())
                {
                    regenerateRepositoriesChecksum(storage, forceRegeneration);
                }
            }
            else if (repositoryId == null)
            {
                regenerateRepositoriesChecksum(storageId, forceRegeneration);
            }
            else
            {
                checksumService.regenerateChecksum(storageId, repositoryId, basePath, forceRegeneration);
            }
        }
        catch (IOException | XmlPullParserException | NoSuchAlgorithmException e)
        {
            logger.error(e.getMessage(), e);
        }

        manager.addExecutedJob(config.getName(), true);
    }

    /**
     * To regenerate artifact's checksum in repositories
     *
     * @param storageId         path of storage
     * @param forceRegeneration true - to re-write existing checksum and to regenerate missing checksum,
     *                          false - to regenerate missing checksum only
     * @throws NoSuchAlgorithmException
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void regenerateRepositoriesChecksum(String storageId,
                                                boolean forceRegeneration)
            throws NoSuchAlgorithmException, XmlPullParserException, IOException
    {
        Map<String, Repository> repositories = getRepositories(storageId);

        for (String repository : repositories.keySet())
        {
            checksumService.regenerateChecksum(storageId, repository, null, forceRegeneration);
        }
    }

    private Map<String, Storage> getStorages()
    {
        return configurationManager.getConfiguration()
                                   .getStorages();
    }

    private Map<String, Repository> getRepositories(String storageId)
    {
        return getStorages().get(storageId)
                            .getRepositories();
    }


}