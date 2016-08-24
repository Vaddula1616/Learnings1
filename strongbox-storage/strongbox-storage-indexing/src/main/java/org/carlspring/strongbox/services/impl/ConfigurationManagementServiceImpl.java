package org.carlspring.strongbox.services.impl;

import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.configuration.ConfigurationRepository;
import org.carlspring.strongbox.configuration.ProxyConfiguration;
import org.carlspring.strongbox.resource.ConfigurationResourceResolver;
import org.carlspring.strongbox.services.ConfigurationManagementService;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.HttpConnectionPool;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.repository.RepositoryTypeEnum;
import org.carlspring.strongbox.storage.routing.RoutingRule;
import org.carlspring.strongbox.storage.routing.RoutingRules;
import org.carlspring.strongbox.storage.routing.RuleSet;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mtodorov
 */
@Component
public class ConfigurationManagementServiceImpl
        implements ConfigurationManagementService
{

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManagementServiceImpl.class);

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    ConfigurationRepository configurationRepository;

    @Autowired
    private ConfigurationResourceResolver configurationResourceResolver;

    @Override
    public Configuration getConfiguration()
    {
        return configurationManager.getConfiguration();
    }

    @Override
    public void setConfiguration(Configuration configuration)
            throws IOException, JAXBException
    {
        //noinspection unchecked
        configurationManager.setConfiguration(configuration);
        configurationManager.store();
        configurationManager.setRepositoryStorageRelationships();
    }

    @Override
    public String getBaseUrl()
            throws IOException
    {
        return configurationManager.getConfiguration().getBaseUrl();
    }

    @Override
    public void setBaseUrl(String baseUrl)
            throws IOException, JAXBException
    {
        Configuration configuration = configurationManager.getConfiguration();
        configuration.setBaseUrl(baseUrl);
        configurationManager.setConfiguration(configuration);
        configurationManager.store();
    }

    @Override
    public int getPort()
            throws IOException
    {
        return configurationManager.getConfiguration().getPort();
    }

    @Override
    public void setPort(int port)
            throws IOException, JAXBException
    {
        Configuration configuration = configurationManager.getConfiguration();
        configuration.setPort(port);
        configurationManager.setConfiguration(configuration);
        configurationManager.store();
    }

    @Override
    public void setProxyConfiguration(String storageId,
                                      String repositoryId,
                                      ProxyConfiguration proxyConfiguration)
            throws IOException, JAXBException
    {
        Configuration configuration = configurationManager.getConfiguration();
        if (storageId != null && repositoryId != null)
        {
            configuration.getStorage(storageId)
                         .getRepository(repositoryId)
                         .setProxyConfiguration(proxyConfiguration);
        }
        else
        {
            configuration.setProxyConfiguration(proxyConfiguration);
        }

        configurationManager.setConfiguration(configuration);
        configurationManager.store();
    }

    @Override
    public ProxyConfiguration getProxyConfiguration()
            throws IOException, JAXBException
    {
        return configurationManager.getConfiguration().getProxyConfiguration();
    }

    @Override
    public void addOrUpdateStorage(Storage storage)
            throws IOException, JAXBException
    {
        Configuration configuration = configurationManager.getConfiguration();
        configuration.addStorage(storage);
        configurationManager.setConfiguration(configuration);
        configurationManager.store();
    }

    @Override
    public Storage getStorage(String storageId)
            throws IOException
    {
        return configurationManager.getConfiguration().getStorage(storageId);
    }

    @Override
    public void removeStorage(String storageId)
            throws IOException, JAXBException
    {
        Configuration configuration = configurationManager.getConfiguration();
        configuration.getStorages().remove(storageId);
        configurationManager.setConfiguration(configuration);
        configurationManager.store();
    }

    @Override
    public synchronized void addOrUpdateRepository(String storageId,
                                                   Repository repository)
            throws IOException, JAXBException
    {
        Configuration configuration = configurationManager.getConfiguration();
        configuration.getStorage(storageId).addOrUpdateRepository(repository);
        configurationManager.setConfiguration(configuration);
        configurationManager.store();
    }

    @Override
    public Repository getRepository(String storageId,
                                    String repositoryId)
            throws IOException
    {
        return configurationManager.getConfiguration().getStorage(storageId).getRepository(repositoryId);
    }

    @Override
    public List<Repository> getGroupRepositories()
    {
        List<Repository> groupRepositories = new ArrayList<>();

        for (Storage storage : configurationManager.getConfiguration().getStorages().values())
        {
            groupRepositories.addAll(storage.getRepositories().values().stream()
                                            .filter(repository -> repository.getType()
                                                                            .equals(RepositoryTypeEnum.GROUP.getType()))
                                            .collect(Collectors.toList()));
        }

        return groupRepositories;
    }

    @Override
    public List<Repository> getGroupRepositoriesContaining(String repositoryId)
    {
        List<Repository> groupRepositories = new ArrayList<>();

        for (Storage storage : configurationManager.getConfiguration().getStorages().values())
        {
            groupRepositories.addAll(storage.getRepositories().values().stream()
                                            .filter(repository -> repository.getType()
                                                                            .equals(RepositoryTypeEnum.GROUP.getType()))
                                            .filter(repository -> repository.getGroupRepositories()
                                                                            .contains(repositoryId))
                                            .collect(Collectors.toList()));
        }

        return groupRepositories;
    }

    @Override
    public void removeRepositoryFromAssociatedGroups(String repositoryId)
            throws IOException, JAXBException
    {
        List<Repository> includedInGroupRepositories = getGroupRepositoriesContaining(repositoryId);

        if (!includedInGroupRepositories.isEmpty())
        {
            Configuration configuration = configurationManager.getConfiguration();

            for (Repository repository : includedInGroupRepositories)
            {
                configuration.getStorage(repository.getStorage().getId())
                             .getRepository(repository.getId())
                             .getGroupRepositories().remove(repositoryId);

                configurationManager.setConfiguration(configuration);
            }

            configurationManager.store();
        }
    }

    @Override
    public void removeRepository(String storageId,
                                 String repositoryId)
            throws IOException, JAXBException
    {
        Configuration configuration = configurationManager.getConfiguration();
        configuration.getStorage(storageId).removeRepository(repositoryId);
        removeRepositoryFromAssociatedGroups(repositoryId);

        configurationManager.setConfiguration(configuration);
        configurationManager.store();
    }

    @Override
    public void setProxyRepositoryMaxConnections(String storageId,
                                                 String repositoryId,
                                                 int numberOfConnections)
            throws IOException, JAXBException
    {
        Repository repository = getRepository(storageId, repositoryId);
        if (repository.getHttpConnectionPool() == null)
        {
            repository.setHttpConnectionPool(new HttpConnectionPool());
        }

        repository.getHttpConnectionPool().setAllocatedConnections(numberOfConnections);
        configurationManager.store();
    }

    @Override
    public HttpConnectionPool getHttpConnectionPoolConfiguration(String storageId,
                                                                 String repositoryId)
            throws IOException, JAXBException
    {
        Repository repository = getRepository(storageId, repositoryId);
        return repository.getHttpConnectionPool();
    }

    @Override
    public boolean addOrUpdateAcceptedRuleSet(RuleSet ruleSet)
    {
        final Configuration configuration = getConfig();
        if (configuration.getRoutingRules() == null)
        {
            configuration.setRoutingRules(new RoutingRules());
        }
        configuration.getRoutingRules().addAcceptRule(ruleSet.getGroupRepository(), ruleSet);
        updateConfiguration(configuration);

        return true;
    }

    @Override
    public boolean removeAcceptedRuleSet(String groupRepository)
    {
        final Configuration configuration = getConfig();
        final Map<String, RuleSet> accepted = configuration.getRoutingRules().getAccepted();
        boolean result = false;
        if (accepted.containsKey(groupRepository))
        {
            result = true;
            accepted.remove(groupRepository);
        }
        updateConfiguration(configuration);

        return result;
    }

    @Override
    public boolean addOrUpdateAcceptedRepository(String groupRepository,
                                                 RoutingRule routingRule)
    {
        final Configuration configuration = getConfig();
        final Map<String, RuleSet> acceptedRulesMap = configuration.getRoutingRules().getAccepted();
        boolean added = false;
        if (acceptedRulesMap.containsKey(groupRepository))
        {
            for (RoutingRule rl : acceptedRulesMap.get(groupRepository).getRoutingRules())
            {
                if (routingRule.getPattern().equals(rl.getPattern()))
                {
                    added = true;
                    rl.getRepositories().addAll(routingRule.getRepositories());
                }
            }
        }
        updateConfiguration(configuration);

        return added;
    }

    @Override
    public boolean removeAcceptedRepository(String groupRepository,
                                            String pattern,
                                            String repositoryId)
    {
        final Configuration configuration = getConfig();
        final Map<String, RuleSet> acceptedRules = configuration.getRoutingRules().getAccepted();
        boolean removed = false;
        if (acceptedRules.containsKey(groupRepository))
        {
            for (RoutingRule routingRule : acceptedRules.get(groupRepository).getRoutingRules())
            {
                if (pattern.equals(routingRule.getPattern()))
                {
                    removed = true;
                    routingRule.getRepositories().remove(repositoryId);
                }
            }
        }
        updateConfiguration(configuration);

        return removed;
    }

    @Override
    public boolean overrideAcceptedRepositories(String groupRepository,
                                                RoutingRule routingRule)
    {
        final Configuration configuration = getConfig();
        boolean overridden = false;
        if (configuration.getRoutingRules().getAccepted().containsKey(groupRepository))
        {
            for (RoutingRule rule : configuration.getRoutingRules().getAccepted().get(
                    groupRepository).getRoutingRules())
            {
                if (routingRule.getPattern().equals(rule.getPattern()))
                {
                    overridden = true;
                    rule.setRepositories(routingRule.getRepositories());
                }
            }
        }
        updateConfiguration(configuration);

        return overridden;
    }

    @Override
    public RoutingRules getRoutingRules()
    {
        return getConfiguration().getRoutingRules();
    }

    private void updateConfiguration(Configuration configuration)
    {
        configurationRepository.updateConfiguration(configuration);
    }

    private Configuration getConfig()
    {
        return configurationRepository.getConfiguration();
    }

    public ConfigurationManager getConfigurationManager()
    {
        return configurationManager;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public ConfigurationResourceResolver getConfigurationResourceResolver()
    {
        return configurationResourceResolver;
    }

    public void setConfigurationResourceResolver(ConfigurationResourceResolver configurationResourceResolver)
    {
        this.configurationResourceResolver = configurationResourceResolver;
    }

}
