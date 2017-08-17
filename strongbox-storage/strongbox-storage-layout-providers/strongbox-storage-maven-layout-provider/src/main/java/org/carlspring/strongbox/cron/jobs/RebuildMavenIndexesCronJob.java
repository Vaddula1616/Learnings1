package org.carlspring.strongbox.cron.jobs;

import org.carlspring.strongbox.cron.config.JobManager;
import org.carlspring.strongbox.cron.domain.CronTaskConfiguration;
import org.carlspring.strongbox.services.ArtifactIndexesService;

import javax.inject.Inject;
import java.io.IOException;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kate Novik.
 */
public class RebuildMavenIndexesCronJob
        extends JavaCronJob
{

    private final Logger logger = LoggerFactory.getLogger(RebuildMavenIndexesCronJob.class);

    @Inject
    private ArtifactIndexesService artifactIndexesService;

    @Inject
    private JobManager manager;


    @Override
    public void executeTask(JobExecutionContext jobExecutionContext)
            throws JobExecutionException
    {
        logger.debug("Executed RebuildMavenIndexesCronJob.");

        CronTaskConfiguration config = (CronTaskConfiguration) jobExecutionContext.getMergedJobDataMap().get("config");
        try
        {
            String storageId = config.getProperty("storageId");
            String repositoryId = config.getProperty("repositoryId");
            String basePath = config.getProperty("basePath");

            if (storageId == null)
            {
                artifactIndexesService.rebuildIndexes();
            }
            else if (repositoryId == null)
            {
                artifactIndexesService.rebuildIndexes(storageId);
            }
            else
            {
                artifactIndexesService.rebuildIndex(storageId, repositoryId, basePath);
            }
        }
        catch (IOException e)
        {
            logger.error(e.getMessage(), e);
            manager.addExecutedJob(config.getName(), true);
        }

        manager.addExecutedJob(config.getName(), true);
    }

}
