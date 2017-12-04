package org.carlspring.strongbox.configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.IOUtils;
import org.carlspring.strongbox.resource.ConfigurationResourceResolver;
import org.carlspring.strongbox.services.ServerConfigurationService;
import org.carlspring.strongbox.xml.parsers.GenericParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.orientechnologies.orient.core.entity.OEntityManager;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;

@Component("configurationRepository")
@Transactional
public class ConfigurationRepository
{

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationRepository.class);

    @Inject
    ServerConfigurationService serverConfigurationService;

    @Inject
    ConfigurationCache configurationCache;

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private OEntityManager oEntityManager;
    
    @Inject
    private ConfigurationManager configurationManager;

    @Inject
    private TransactionTemplate transactionTemplate;
    
    private String currentDatabaseId;


    public ConfigurationRepository()
    {
    }

    private OObjectDatabaseTx getDatabase()
    {
        return (OObjectDatabaseTx) entityManager.getDelegate();
    }

    @PostConstruct
    public synchronized void init()
            throws IOException
    {
        logger.debug("ConfigurationRepository.init()");

        oEntityManager.registerEntityClass(BinaryConfiguration.class, true);

        transactionTemplate.execute((s) -> {
            if (!schemaExists() || getConfiguration() == null)
            {
                try
                {
                    createSettings();
                }
                catch (IOException e)
                {
                    throw new BeanInitializationException(String.format("Failed to init: msg-[%s]", e.getMessage()), e);
                }
            }
            return null;

        });
        
    }

    private synchronized boolean schemaExists()
    {
        OObjectDatabaseTx db = getDatabase();
        return db != null && db.getMetadata().getSchema().existsClass(BinaryConfiguration.class.getSimpleName());
    }

    private synchronized void createSettings()
            throws IOException
    {
        // skip configuration initialization if config is already in place
        if (currentDatabaseId != null)
        {
            logger.debug("Skip config initialization: already in place.");
            return;
        }

        Configuration configuration = loadConfigurationFromFileSystem();

        // Create configuration in database and put it to cache.
        updateConfiguration(configuration);
    }

    private Configuration loadConfigurationFromFileSystem()
            throws IOException
    {
        logger.debug("Loading configuration from XML file...");

        Configuration configuration = null;

        InputStream is = getConfigurationResource().getInputStream();

        try
        {
            byte[] bytes = IOUtils.toByteArray(is);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

            GenericParser<Configuration> parser = new GenericParser<>(Configuration.class);

            configuration = parser.parse(bais);

            bais.close();
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }

        return configuration;
    }

    public synchronized Configuration getConfiguration()
    {
        Optional<Configuration> optionalConfig = configurationCache.getConfiguration(currentDatabaseId);
        if (optionalConfig.isPresent())
        {
            return optionalConfig.get();
        }

        return null;
    }

    @Transactional
    public synchronized Optional<Configuration> updateConfiguration(Configuration configuration)
    {
        if (configuration == null)
        {
            throw new NullPointerException("The configuration is null.");
        }

        try
        {
            final String data = configurationCache.getParser().serialize(configuration);
            final String configurationId = configuration.getObjectId();

            // update existing configuration with new data (if possible)
            if (configurationId != null)
            {
                serverConfigurationService.findOne(configurationId)
                                          .ifPresent(binaryConfiguration -> doSave(binaryConfiguration, data));
            }
            else
            {
                doSave(new BinaryConfiguration(), data);
            }

            if (currentDatabaseId == null)
            {
                throw new NullPointerException("The currentDatabaseId is null.");
            }

            configuration.setObjectId(currentDatabaseId);
            configurationCache.save(configuration);

            logger.debug("Configuration updated under ID " + currentDatabaseId);
        }
        catch (Exception e)
        {
            logger.error("Unable to save configuration\n\n" + configuration, e);

            return Optional.empty();
        }

        return Optional.of(configuration);
    }

    @Transactional
    private synchronized void doSave(BinaryConfiguration binaryConfiguration,
                                     String data)
    {
        binaryConfiguration.setData(data);
        binaryConfiguration = serverConfigurationService.save(binaryConfiguration);
        currentDatabaseId = binaryConfiguration.getObjectId();
    }

    public Resource getConfigurationResource()
            throws IOException
    {
        return ConfigurationResourceResolver.getConfigurationResource("strongbox.config.xml",
                                                                      "etc/conf/strongbox.xml");
    }

}
