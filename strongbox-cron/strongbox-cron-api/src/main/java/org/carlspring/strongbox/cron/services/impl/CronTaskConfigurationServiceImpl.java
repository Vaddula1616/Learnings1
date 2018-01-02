package org.carlspring.strongbox.cron.services.impl;

import org.carlspring.strongbox.cron.domain.CronTaskConfiguration;
import org.carlspring.strongbox.cron.domain.GroovyScriptNames;
import org.carlspring.strongbox.cron.exceptions.CronTaskException;
import org.carlspring.strongbox.cron.exceptions.CronTaskNotFoundException;
import org.carlspring.strongbox.cron.jobs.AbstractCronJob;
import org.carlspring.strongbox.cron.services.CronJobSchedulerService;
import org.carlspring.strongbox.cron.services.CronTaskConfigurationService;
import org.carlspring.strongbox.cron.services.CronTaskDataService;
import org.carlspring.strongbox.event.cron.CronTaskEventListenerRegistry;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.IteratorUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class CronTaskConfigurationServiceImpl
        implements CronTaskConfigurationService, ApplicationContextAware
{

    private final Logger logger = LoggerFactory.getLogger(CronTaskConfigurationServiceImpl.class);

    @Inject
    protected CronTaskEventListenerRegistry cronTaskEventListenerRegistry;

    private AutowireCapableBeanFactory beanFactory;

    @Inject
    private CronTaskDataService cronTaskDataService;

    @Inject
    private CronJobSchedulerService cronJobSchedulerService;

    @Override
    public void setApplicationContext(final ApplicationContext context)
    {
        beanFactory = context.getAutowireCapableBeanFactory();
    }

    public void saveConfiguration(CronTaskConfiguration configuration)
            throws Exception
    {
        logger.debug("CronTaskConfigurationService.saveConfiguration()");

        if (!configuration.contains("cronExpression"))
        {
            throw new CronTaskException("cronExpression property does not exists");
        }

        configuration = cronTaskDataService.save(configuration);

        if (configuration.contains("jobClass"))
        {
            Class c = Class.forName(configuration.getProperty("jobClass"));
            Object classInstance = c.newInstance();

            logger.debug("> " + c.getSuperclass().getCanonicalName());

            if (!(classInstance instanceof AbstractCronJob))
            {
                throw new CronTaskException(c + " does not extend " + AbstractCronJob.class);
            }

            beanFactory.autowireBean(classInstance);
            ((AbstractCronJob) classInstance).beforeScheduleCallback(configuration);

            cronJobSchedulerService.scheduleJob(configuration);

            if (configuration.shouldExecuteImmediately())
            {
                cronJobSchedulerService.executeJob(configuration);
            }
        }

        cronTaskEventListenerRegistry.dispatchCronTaskCreatedEvent(configuration.getName());
    }

    public void deleteConfiguration(CronTaskConfiguration configuration)
            throws SchedulerException,
                   CronTaskNotFoundException,
                   ClassNotFoundException
    {
        logger.debug("Deleting cron task configuration {}", configuration);

        cronTaskDataService.delete(configuration);
        cronJobSchedulerService.deleteJob(configuration);

        cronTaskEventListenerRegistry.dispatchCronTaskDeletedEvent(configuration.getName());
    }

    public List<CronTaskConfiguration> getConfiguration(String name)
    {
        return cronTaskDataService.findByName(name);
    }

    @Override
    public CronTaskConfiguration findOne(String name)
    {
        List<CronTaskConfiguration> configurations = getConfiguration(name);
        if (configurations == null || configurations.isEmpty())
        {
            return null;
        }

        return configurations.get(0);
    }

    public List<CronTaskConfiguration> getConfigurations()
    {
        logger.debug("CronTaskConfigurationService.getConfigurations()");

        Optional<List<CronTaskConfiguration>> optional = cronTaskDataService.findAll();

        return (List<CronTaskConfiguration>) (optional.isPresent() ? optional.get() :
                                              IteratorUtils.toList(optional.get().iterator()));
    }

    public GroovyScriptNames getGroovyScriptsName()
    {
        logger.debug("CronTaskConfigurationService.getGroovyScriptsName");

        return cronJobSchedulerService.getGroovyScriptsName();
    }
}
