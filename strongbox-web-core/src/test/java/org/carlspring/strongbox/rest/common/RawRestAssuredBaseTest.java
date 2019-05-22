package org.carlspring.strongbox.rest.common;

import org.carlspring.commons.io.MultipleDigestOutputStream;
import org.carlspring.strongbox.repository.RepositoryManagementStrategyException;
import org.carlspring.strongbox.rest.client.RestAssuredArtifactClient;
import org.carlspring.strongbox.storage.repository.MutableRepository;
import org.carlspring.strongbox.storage.repository.RawRepositoryFactory;
import org.carlspring.strongbox.storage.repository.RepositoryTypeEnum;
import org.carlspring.strongbox.storage.repository.remote.MutableRemoteRepository;
import org.carlspring.strongbox.testing.TestCaseWithRepositoryManagement;
import org.carlspring.strongbox.users.domain.Roles;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.context.WebApplicationContext;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.carlspring.strongbox.rest.client.RestAssuredArtifactClient.OK;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author carlspring
 */
public class RawRestAssuredBaseTest
        extends TestCaseWithRepositoryManagement
{

    protected static final String TEST_RESOURCES = "target/test-resources";

    /**
     * Share logger instance across all tests.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Inject
    protected WebApplicationContext context;

    @Inject
    protected RestAssuredArtifactClient client;

    @Inject
    RawRepositoryFactory rawRepositoryFactory;

    @Value("${strongbox.url}")
    private String contextBaseUrl;

    public void init()
            throws Exception
    {
        client.setUserAgent("Raw/*");
        client.setContextBaseUrl(contextBaseUrl);
    }

    public String getContextBaseUrl()
    {
        return contextBaseUrl;
    }

    public void setContextBaseUrl(String contextBaseUrl)
    {
        this.contextBaseUrl = contextBaseUrl;
    }

    protected Collection<? extends GrantedAuthority> provideAuthorities()
    {
        return Roles.ADMIN.getPrivileges();
    }

    protected boolean pathExists(String url)
    {
        logger.trace("[pathExists] URL -> " + url);

        return given().header("user-agent", "Raw/*")
                      .contentType(MediaType.TEXT_PLAIN_VALUE)
                      .when()
                      .get(url)
                      .getStatusCode() == OK;
    }

    protected void assertPathExists(String url)
    {
        assertTrue(pathExists(url), "Path " + url + " doesn't exist.");
    }

    @Override
    public void createProxyRepository(String storageId,
                                      String repositoryId,
                                      String remoteRepositoryUrl)
            throws IOException,
                   JAXBException,
                   RepositoryManagementStrategyException
    {
        MutableRemoteRepository remoteRepository = new MutableRemoteRepository();
        remoteRepository.setUrl(remoteRepositoryUrl);

        MutableRepository repository = rawRepositoryFactory.createRepository(repositoryId);
        repository.setType(RepositoryTypeEnum.PROXY.getType());
        repository.setRemoteRepository(remoteRepository);

        createRepository(storageId, repository);
    }

    protected void resolveArtifact(String artifactPath)
            throws NoSuchAlgorithmException, IOException
    {
        String url = getContextBaseUrl() + artifactPath;

        logger.debug("Requesting " + url + "...");

        InputStream is = client.getResource(url);
        if (is == null)
        {
            fail("Failed to resolve " + artifactPath + "!");
        }

        File testResources = new File(TEST_RESOURCES, artifactPath);
        if (!testResources.getParentFile().exists())
        {
            //noinspection ResultOfMethodCallIgnored
            testResources.getParentFile().mkdirs();
        }

        FileOutputStream fos = new FileOutputStream(new File(TEST_RESOURCES, artifactPath));
        MultipleDigestOutputStream mdos = new MultipleDigestOutputStream(fos);

        int total = 0;
        int len;
        final int size = 1024;
        byte[] bytes = new byte[size];

        while ((len = is.read(bytes, 0, size)) != -1)
        {
            mdos.write(bytes, 0, len);
            total += len;
        }

        mdos.flush();
        mdos.close();

        assertTrue(total > 0, "Resolved a zero-length artifact!");
    }

}
