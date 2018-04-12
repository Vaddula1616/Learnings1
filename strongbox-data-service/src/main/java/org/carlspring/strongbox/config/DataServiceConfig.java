package org.carlspring.strongbox.config;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.carlspring.strongbox.data.CacheManagerConfiguration;
import org.carlspring.strongbox.data.server.OrientDbServer;
import org.carlspring.strongbox.data.tx.OEntityUnproxyAspect;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import com.orientechnologies.orient.core.entity.OEntityManager;
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLFactory;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionFactory;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;

/**
 * Spring configuration for data service project.
 *
 * @author Alex Oreshkevich
 */
@Configuration
@Lazy(false)
@EnableTransactionManagement(proxyTargetClass = true, order = DataServiceConfig.TRANSACTIONAL_INTERCEPTOR_ORDER)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan({ "org.carlspring.strongbox.data" })
@Import(DataServicePropertiesConfig.class)
@EnableCaching(order = 105)
public class DataServiceConfig
{

    private static final Logger logger = LoggerFactory.getLogger(DataServiceConfig.class);
    
    /**
     * This must be after {@link OEntityUnproxyAspect} order.
     */
    public static final int TRANSACTIONAL_INTERCEPTOR_ORDER = 100;
    
    @Inject
    private ConnectionConfig connectionConfig;
    
    @Inject
    private ResourceLoader resourceLoader;
    
    @Inject
    private OrientDbServer server;

    @PostConstruct
    public void init() throws ClassNotFoundException, LiquibaseException
    {
        server.start();
        
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource());
        liquibase.setResourceLoader(resourceLoader);
        liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.xml");
        liquibase.afterPropertiesSet();

        new TransactionTemplate(transactionManager()).execute((s) -> {
            OEntityManager oEntityManager = oEntityManager();
            // register all domain entities
            Stream.concat(new Reflections("org.carlspring.strongbox").getTypesAnnotatedWith(Entity.class).stream(),
                          new Reflections("org.carlspring.strongbox").getTypesAnnotatedWith(Embeddable.class).stream())
                  .forEach(oEntityManager::registerEntityClass);
            
            return null;
        });
        

    }

    
    @Bean
    public PlatformTransactionManager transactionManager()
    {
        return new JpaTransactionManager(entityManagerFactory());
    }

    @Bean
    public EntityManagerFactory entityManagerFactory()
    {
        Map<String, String> jpaProperties = new HashMap<>();
        jpaProperties.put("javax.persistence.jdbc.url", connectionConfig.getUrl());
        jpaProperties.put("javax.persistence.jdbc.user", connectionConfig.getUsername());
        jpaProperties.put("javax.persistence.jdbc.password", connectionConfig.getPassword());

        LocalContainerEntityManagerFactoryBean result = new LocalContainerEntityManagerFactoryBean();
        result.setJpaPropertyMap(jpaProperties);
        result.afterPropertiesSet();
        return result.getObject();
    }

    @Bean
    public OEntityManager oEntityManager()
    {
        return OEntityManager.getEntityManagerByDatabaseURL(connectionConfig.getUrl());
    }

    @Bean
    public CacheManager cacheManager(net.sf.ehcache.CacheManager cacheManager)
    {
        EhCacheCacheManager result = new EhCacheCacheManager(cacheManager);
        result.setTransactionAware(true);
        return result;
    }

    @Bean
    public CacheManagerConfiguration cacheManagerConfiguration()
    {
        CacheManagerConfiguration cacheManagerConfiguration = new CacheManagerConfiguration();
        cacheManagerConfiguration.setCacheCacheManagerId("strongboxCacheManager");
        return cacheManagerConfiguration;
    }

    @Bean
    public EhCacheManagerFactoryBean ehCacheCacheManager(CacheManagerConfiguration cacheManagerConfiguration)
    {
        EhCacheManagerFactoryBean cmfb = new EhCacheManagerFactoryBean();
        cmfb.setConfigLocation(new ClassPathResource("ehcache.xml"));
        cmfb.setShared(false);
        cmfb.setCacheManagerName(cacheManagerConfiguration.getCacheCacheManagerId());
        return cmfb;
    }

    @Bean
    public DataSource dataSource()
        throws ClassNotFoundException
    {
        Class.forName("com.orientechnologies.orient.jdbc.OrientJdbcDriver");

        ServiceLoader.load(OSQLFunctionFactory.class);
        ServiceLoader.load(OCommandExecutorSQLFactory.class);

        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setAutoCommit(false);

        ds.setUsername(connectionConfig.getUsername());
        ds.setPassword(connectionConfig.getPassword());
        ds.setUrl(String.format("jdbc:orient:%s", connectionConfig.getUrl()));

        return ds;
    }

}
