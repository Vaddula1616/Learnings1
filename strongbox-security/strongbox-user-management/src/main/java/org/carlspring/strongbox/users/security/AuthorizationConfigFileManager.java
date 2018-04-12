package org.carlspring.strongbox.users.security;

import org.carlspring.strongbox.resource.ConfigurationResourceResolver;
import org.carlspring.strongbox.xml.parsers.GenericParser;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * @author Przemyslaw Fusik
 */
@Component
public class AuthorizationConfigFileManager
{
    
    private final GenericParser<AuthorizationConfig> parser = new GenericParser<>(AuthorizationConfig.class);

    private Resource getConfigurationResource()
            throws IOException
    {
        return ConfigurationResourceResolver.getConfigurationResource("strongbox.authorization.config.xml",
                                                                      "etc/conf/strongbox-authorization.xml");
    }

    public void store(final AuthorizationConfig config)
    {
        try
        {
            parser.store(config, getConfigurationResource());
        }
        catch (JAXBException | IOException e)
        {
            throw new AuthorizationConfigReadException(e);
        }
        
    }

    public AuthorizationConfig read()
    {
        try(InputStream inputStream = getConfigurationResource().getInputStream())
        {
            return parser.parse(inputStream);
        }
        catch (JAXBException | IOException e)
        {
            throw new AuthorizationConfigReadException(e);
        }
    }

}
