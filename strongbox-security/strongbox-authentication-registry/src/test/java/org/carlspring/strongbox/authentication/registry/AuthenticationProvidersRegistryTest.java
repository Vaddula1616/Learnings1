package org.carlspring.strongbox.authentication.registry;

import org.carlspring.strongbox.authentication.TestConfig;
import org.carlspring.strongbox.authentication.api.impl.xml.PasswordAuthenticationProvider;
import org.carlspring.strongbox.authentication.registry.AuthenticationProvidersRegistry;
import org.carlspring.strongbox.config.hazelcast.HazelcastConfiguration;
import org.carlspring.strongbox.config.hazelcast.HazelcastInstanceId;

import javax.inject.Inject;
import java.util.Collection;

import com.google.common.collect.Lists;
import org.hamcrest.CoreMatchers;
import org.hamcrest.CustomMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Przemyslaw Fusik
 */
@SpringBootTest
@ActiveProfiles({ "test", "AuthenticationProvidersRegistryTestConfig" })
@TestPropertySource(properties = { "strongbox.config.file.authentication.providers=classpath:aprt-strongbox-authentication-providers.xml",
                                   "strongbox.authentication.providers.yaml=classpath:/etc/conf/aprt-strongbox-authentication-providers.yaml" })
@ContextConfiguration(classes = TestConfig.class)
public class AuthenticationProvidersRegistryTest
{

    @Inject
    AuthenticationProvidersRegistry authenticationProvidersRegistry;

    @Test
    public void registryShouldNotBeNull()
    {
        assertThat(getAuthenticationProviderList(), CoreMatchers.notNullValue());
    }

    private Collection<AuthenticationProvider> getAuthenticationProviderList()
    {
        return authenticationProvidersRegistry.getAuthenticationProviderMap().values();
    }

    @Test
    public void registryShouldContainStrongboxBuiltinAuthenticationProvider()
    {
        assertThat(getAuthenticationProviderList(), CoreMatchers.hasItem(
                new CustomMatcher<AuthenticationProvider>("registryShouldContainStrongboxBuiltinAuthenticationProvider")
                {
                    @Override
                    public boolean matches(Object o)
                    {
                        return ((AuthenticationProvider) o).getClass().getName()
                                                  .equals(PasswordAuthenticationProvider.class.getName());
                    }
                }));
    }

    @Test
    public void registryShouldContainEmptyAuthenticationProvider()
    {
        assertThat(Lists.newArrayList(getAuthenticationProviderList()),
                   CoreMatchers.hasItem(new CustomMatcher<AuthenticationProvider>("registryShouldContainEmptyAuthenticationProvider")
                   {
                       @Override
                       public boolean matches(Object o)
                       {
                           return ((AuthenticationProvider) o).getClass().getName()
                                                     .equals("org.carlspring.strongbox.authentication.impl.example.EmptyAuthenticationProvider");
                       }
                   }));
    }
    
    @Profile("AuthenticationProvidersRegistryTestConfig")
    @Import(HazelcastConfiguration.class)
    @Configuration
    public static class AuthenticationProvidersRegistryTestConfig
    {

        @Primary
        @Bean
        public HazelcastInstanceId hazelcastInstanceIdAcctit()
        {
            return new HazelcastInstanceId("AuthenticationProvidersRegistryTest-hazelcast-instance");
        }

    }


}
