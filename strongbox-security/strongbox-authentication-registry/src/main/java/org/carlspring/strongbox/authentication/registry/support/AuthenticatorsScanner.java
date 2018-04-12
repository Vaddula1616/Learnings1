package org.carlspring.strongbox.authentication.registry.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.carlspring.strongbox.authentication.api.Authenticator;
import org.carlspring.strongbox.authentication.registry.AuthenticatorsRegistry;
import org.carlspring.strongbox.resource.ConfigurationResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.Resource;

import com.google.common.base.Throwables;

/**
 * @author Przemyslaw Fusik
 */
public class AuthenticatorsScanner
{

    private static final Logger logger = LoggerFactory.getLogger(AuthenticatorsScanner.class);

    private final AuthenticatorsRegistry registry;
    
    @Inject
    private ApplicationContext parentApplicationContext;

    public AuthenticatorsScanner(AuthenticatorsRegistry registry)
    {
        this.registry = registry;
    }

    public void scanAndReloadRegistry()
    {
        final ClassLoader entryClassLoader = parentApplicationContext.getClassLoader();
        final ClassLoader requiredClassLoader = ExternalAuthenticatorsHelper.getExternalAuthenticatorsClassLoader(
                entryClassLoader);

        logger.debug("Reloading authenticators registry ...");
        final GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
        try
        {
            applicationContext.setParent(parentApplicationContext);
            applicationContext.setClassLoader(requiredClassLoader);
            applicationContext.load(getAuthenticationConfigurationResource());
            applicationContext.refresh();
        }
        catch (Exception e)
        {
            logger.error("Unable to load authenticators from configuration file.", e);
            throw Throwables.propagate(e);
        }

        final List<Authenticator> authenticators = getAuthenticators(applicationContext);
        logger.debug("Scanned authenticators: {}", authenticators.stream().map(Authenticator::getName).collect(
                Collectors.toList()));
        registry.reload(authenticators);
    }

    private List<Authenticator> getAuthenticators(ApplicationContext applicationContext)
    {
        final List<Object> authenticatorsClasses = applicationContext.getBean("authenticators", List.class);
        final List<Authenticator> authenticators = new ArrayList<>();
        for (final Object authenticator : authenticatorsClasses)
        {
            Class<?> authenticatorClass = authenticator.getClass();
            if (!Authenticator.class.isAssignableFrom(authenticatorClass))
            {
                throw new IllegalAuthenticatorException(authenticatorClass + " is not assignable from " +
                                                        Authenticator.class.getName());
            }
            authenticators.add((Authenticator) authenticator);
        }
        return authenticators;
    }

    private Resource getAuthenticationConfigurationResource()
            throws IOException
    {
        return ConfigurationResourceResolver.getConfigurationResource("strongbox.authentication.providers.xml",
                                                                      "etc/conf/strongbox-authentication-providers.xml");
    }
    
    @EventListener({ ContextRefreshedEvent.class })
    void contextRefreshedEvent(ContextRefreshedEvent e)
    {
        if (parentApplicationContext != e.getApplicationContext()) {
            return;
        }
        scanAndReloadRegistry();
    }

}
