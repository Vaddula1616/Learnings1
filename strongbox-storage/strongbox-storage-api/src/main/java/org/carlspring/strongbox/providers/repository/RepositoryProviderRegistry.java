package org.carlspring.strongbox.providers.repository;

import org.carlspring.strongbox.providers.AbstractMappedProviderRegistry;

import javax.annotation.PostConstruct;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author carlspring
 */
@Component("repositoryProviderRegistry")
public class RepositoryProviderRegistry extends AbstractMappedProviderRegistry<RepositoryProvider>
{

    private static final Logger logger = LoggerFactory.getLogger(RepositoryProviderRegistry.class);


    public RepositoryProviderRegistry()
    {
    }

    @Override
    @PostConstruct
    public void initialize()
    {
        logger.info("Initialized the repository provider registry.");
    }

    public void dump()
    {
        logger.debug("Listing repository providers:");
        for (String providerName : getProviders().keySet())
        {
            logger.debug(" provider: " + providerName);
        }
    }

    /**
     * K: String   :
     * V: Provider : The provider
     *
     * @return
     */
    @Override
    public Map<String, RepositoryProvider> getProviders()
    {
        return super.getProviders();
    }

    @Override
    public void setProviders(Map<String, RepositoryProvider> providers)
    {
        super.setProviders(providers);
    }

    @Override
    public RepositoryProvider getProvider(String alias)
    {
        return super.getProvider(alias);
    }

    @Override
    public RepositoryProvider addProvider(String alias, RepositoryProvider provider)
    {
        return super.addProvider(alias, provider);
    }

    @Override
    public void removeProvider(String alias)
    {
        super.removeProvider(alias);
    }

}
