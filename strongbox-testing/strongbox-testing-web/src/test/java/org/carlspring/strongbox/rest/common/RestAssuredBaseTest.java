package org.carlspring.strongbox.rest.common;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.carlspring.strongbox.rest.client.RestAssuredArtifactClient.OK;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import javax.inject.Inject;

import org.carlspring.strongbox.rest.client.RestAssuredArtifactClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.config.ObjectMapperConfig;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;

/**
 * General settings for the testing sub-system.
 *
 * @author Alex Oreshkevich
 */
public abstract class RestAssuredBaseTest
{

    public final static int DEFAULT_PORT = 48080;

    public final static String DEFAULT_HOST = "localhost";

    protected static final String STORAGE0 = "storage0";
    
    /**
     * Share logger instance across all tests.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Inject
    protected WebApplicationContext context;

    @Inject
    protected ObjectMapper objectMapper;

    @Inject
    protected RestAssuredArtifactClient client;

    @Value("${strongbox.url}")
    private String contextBaseUrl;

    private RestAssuredMockMvcConfig restAssuredMockMvcConfig;

    public void init()
            throws Exception
    {
        logger.debug("Initializing RestAssured...");

        client.setContextBaseUrl(contextBaseUrl);
        
        restAssuredMockMvcConfig = RestAssuredMockMvcConfig.config().objectMapperConfig(
                new ObjectMapperConfig().jackson2ObjectMapperFactory((aClass, s) -> objectMapper));
    }

    public String getContextBaseUrl()
    {
        return contextBaseUrl;
    }

    protected MockMvcRequestSpecification givenCustom()
    {
        return given().config(restAssuredMockMvcConfig);
    }

    public void setContextBaseUrl(String contextBaseUrl)
    {
        this.contextBaseUrl = contextBaseUrl;
    }
    
    public static void removeDir(String path)
    {
        removeDir(new File(path));
    }

    /**
     * Recursively removes directory or file #file and all it's content.
     *
     * @param file directory or file to be removed
     */
    public static void removeDir(File file)
    {
        if (file == null || !file.exists())
        {
            return;
        }

        if (file.isDirectory())
        {
            File[] files = file.listFiles();
            if (files != null)
            {
                for (File f : files)
                {
                    removeDir(f);
                }
            }
        }

        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    protected boolean pathExists(String url)
    {
        logger.trace("[pathExists] URL -> " + url);

        return given().header("user-agent", "Maven/*")
                      .contentType(MediaType.TEXT_PLAIN_VALUE)
                      .when()
                      .get(url)
                      .getStatusCode() == OK;
    }

    protected void assertPathExists(String url)
    {
        assertTrue(pathExists(url), "Path " + url + " doesn't exist.");
    }

}
