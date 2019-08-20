package org.carlspring.strongbox.rest.common;

import org.carlspring.strongbox.artifact.MavenArtifact;
import org.carlspring.strongbox.artifact.MavenArtifactUtils;
import org.carlspring.strongbox.artifact.MavenRepositoryArtifact;
import org.carlspring.strongbox.artifact.generator.MavenArtifactDeployer;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.providers.io.RepositoryPathResolver;
import org.carlspring.strongbox.rest.client.RestAssuredArtifactClient;
import org.carlspring.strongbox.testing.TestCaseWithMavenArtifactGenerationAndIndexing;
import org.carlspring.strongbox.testing.artifact.MavenArtifactTestUtils;
import org.carlspring.strongbox.users.domain.Privileges;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import org.apache.maven.index.artifact.Gav;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.context.WebApplicationContext;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.carlspring.strongbox.rest.client.RestAssuredArtifactClient.OK;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * General settings for the testing sub-system.
 *
 * @author Alex Oreshkevich
 */
public abstract class MavenRestAssuredBaseTest
        extends TestCaseWithMavenArtifactGenerationAndIndexing
{

    /**
     * Share logger instance across all tests.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Inject
    protected WebApplicationContext context;

    @Inject
    protected RestAssuredArtifactClient client;

    @Inject
    protected RepositoryPathResolver repositoryPathResolver;

    @Value("${strongbox.url}")
    private String contextBaseUrl;

    public void init()
            throws Exception
    {
        client.setUserAgent("Maven/*");
        client.setContextBaseUrl(contextBaseUrl);
    }

    public void shutdown()
    {
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
        return Privileges.all();
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

    protected MavenArtifactDeployer buildArtifactDeployer(Path path)
    {
        MavenArtifactDeployer deployer = new MavenArtifactDeployer(path.toString());
        deployer.setClient(client);
        return deployer;
    }

    public MavenArtifact createTimestampedSnapshotArtifact(String repositoryBasedir,
                                                           String groupId,
                                                           String artifactId,
                                                           String baseSnapshotVersion,
                                                           String packaging,
                                                           String[] classifiers,
                                                           int numberOfBuilds)
            throws NoSuchAlgorithmException, XmlPullParserException, IOException
    {
        MavenArtifact artifact = null;

        for (int i = 0; i < numberOfBuilds; i++)
        {
            String version = createSnapshotVersion(baseSnapshotVersion, i + 1);

            artifact = new MavenRepositoryArtifact(new Gav(groupId, artifactId, version));

            Path repositoryPath = Paths.get(repositoryBasedir);
            String repositoryId = repositoryPath.getFileName().toString();
            String storageId = repositoryPath.getParent().getFileName().toString();

            RepositoryPath artifactPath = repositoryPathResolver.resolve(storageId, repositoryId,
                                                                         MavenArtifactUtils.convertArtifactToPath(
                                                                                 artifact));
            artifact.setPath(artifactPath);

            generateArtifact(repositoryBasedir, artifact, packaging);

            if (classifiers != null)
            {
                for (String classifier : classifiers)
                {
                    String gavtc = groupId + ":" + artifactId + ":" + version + ":jar:" + classifier;
                    generateArtifact(repositoryBasedir, MavenArtifactTestUtils.getArtifactFromGAVTC(gavtc));
                }
            }
        }

        // Return the main artifact
        return artifact;
    }

}
