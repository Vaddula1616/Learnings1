package org.carlspring.strongbox.cron.config;

import org.carlspring.strongbox.config.DataServiceConfig;
import org.carlspring.strongbox.cron.domain.CronTaskConfiguration;

import javax.annotation.PostConstruct;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.orient.commons.repository.config.EnableOrientRepositories;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@ComponentScan({ "org.carlspring.strongbox.cron"
})
@Import({ DataServiceConfig.class
})
@EnableOrientRepositories(basePackages = "org.carlspring.strongbox.cron.repository")
@EnableWebMvc
public class CronTasksConfig
        extends WebMvcConfigurerAdapter
{

    @Autowired
    private OObjectDatabaseTx databaseTx;

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean()
    {
        return new SchedulerFactoryBean();
    }

    @PostConstruct
    @Transactional
    public void init()
    {
        // register all domain entities
        databaseTx.activateOnCurrentThread();
        databaseTx.getEntityManager().registerEntityClasses(CronTaskConfiguration.class.getPackage().getName());
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters)
    {
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
        stringConverter.setWriteAcceptCharset(false);

        converters.add(new ByteArrayHttpMessageConverter()); // if your argument is a byte[]
        converters.add(stringConverter);
        converters.add(new FormHttpMessageConverter());
        converters.add(new MappingJackson2HttpMessageConverter());
        converters.add(jaxbConverter());
        converters.add(new ResourceHttpMessageConverter());
    }

    @Bean
    public ObjectMapper objectMapper()
    {
        return new ObjectMapper();
    }

    @Bean
    public Jaxb2RootElementHttpMessageConverter jaxbConverter()
    {
        return new Jaxb2RootElementHttpMessageConverter();
    }
}
