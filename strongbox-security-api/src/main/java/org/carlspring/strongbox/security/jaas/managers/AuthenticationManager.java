package org.carlspring.strongbox.security.jaas.managers;

import org.carlspring.strongbox.common.resource.ConfigurationResourceResolver;
import org.carlspring.strongbox.common.xml.parsers.GenericParser;
import org.carlspring.strongbox.configuration.AnonymousAccessConfiguration;
import org.carlspring.strongbox.configuration.AuthenticationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;

/**
 * @author mtodorov
 */
@Component
@Lazy
@Scope("singleton")
public class AuthenticationManager
        implements AuthenticationConfigurationManager
{

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationManager.class);

    @Autowired
    private ConfigurationResourceResolver configurationResourceResolver;

    private AuthenticationConfiguration configuration;

    private GenericParser<AuthenticationConfiguration> parser = new GenericParser<AuthenticationConfiguration>(AuthenticationConfiguration.class);


    @PostConstruct
    @Override
    public void load()
            throws IOException, JAXBException
    {
        Resource resource = configurationResourceResolver.getConfigurationResource("security.authentication.xml",
                                                                                   "etc/conf/security-authentication.xml");

        logger.info("Loading Strongbox configuration from " + resource.toString() + "...");

        configuration = parser.parse(resource.getInputStream());
    }

    @Override
    public void store()
            throws IOException, JAXBException
    {
        Resource resource = configurationResourceResolver.getConfigurationResource("security.authentication.xml",
                                                                                   "etc/conf/security-authentication.xml");

        parser.store(configuration, resource.getFile().getAbsoluteFile());
    }

    public List<String> getRealms()
    {
        return configuration.getRealms();
    }

    public boolean removeRealm(String realm)
    {
        return configuration.removeRealm(realm);
    }

    public boolean addRealm(String realm)
    {
        return configuration.addRealm(realm);
    }

    public void setRealms(List<String> realms)
    {
        configuration.setRealms(realms);
    }

    public boolean containsRealm(String realm)
    {
        return configuration.containsRealm(realm);
    }

    public void setAnonymousAccessConfiguration(AnonymousAccessConfiguration anonymousAccessConfiguration)
    {
        configuration.setAnonymousAccessConfiguration(anonymousAccessConfiguration);
    }

    public AnonymousAccessConfiguration getAnonymousAccessConfiguration()
    {
        return configuration.getAnonymousAccessConfiguration();
    }

    public ConfigurationResourceResolver getConfigurationResourceResolver()
    {
        return configurationResourceResolver;
    }

    public void setConfigurationResourceResolver(ConfigurationResourceResolver configurationResourceResolver)
    {
        this.configurationResourceResolver = configurationResourceResolver;
    }

    public AuthenticationConfiguration getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration(AuthenticationConfiguration configuration)
    {
        this.configuration = configuration;
    }

}
