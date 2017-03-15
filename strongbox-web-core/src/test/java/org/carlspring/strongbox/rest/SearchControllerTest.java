package org.carlspring.strongbox.rest;

import org.carlspring.strongbox.artifact.generator.MavenArtifactDeployer;
import org.carlspring.strongbox.booters.StorageBooter;
import org.carlspring.strongbox.resource.ConfigurationResourceResolver;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;
import org.carlspring.strongbox.rest.context.IntegrationTest;
import org.carlspring.strongbox.services.RepositoryManagementService;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexManager;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexer;

import javax.inject.Inject;
import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.carlspring.maven.commons.util.ArtifactUtils.getArtifactFromGAVTC;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Oreshkevich
 * @author Martin Todorov
 */
@IntegrationTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SearchControllerTest
        extends RestAssuredBaseTest
{

    @Inject
    StorageBooter storageBooter;

    @Inject
    RepositoryIndexManager repositoryIndexManager;

    @Inject
    RepositoryManagementService repositoryManagementService;


    @Override
    public void init()
            throws Exception
    {
        super.init();

        prepareMockData();
    }

    private void prepareMockData()
    {
        File repositoryBasedir = new File(ConfigurationResourceResolver.getVaultDirectory() +
                                          "/storages/storage0/releases");
        removeDir(new File(repositoryBasedir, "org/carlspring/strongbox/searches/test-project").getAbsolutePath());

        try
        {
            File strongboxBaseDir = new File(ConfigurationResourceResolver.getVaultDirectory() + "/tmp");
            String[] classifiers = new String[]{ "javadoc", "tests" };

            Artifact artifact1 = getArtifactFromGAVTC("org.carlspring.strongbox.searches:test-project:1.0.11.3");
            Artifact artifact2 = getArtifactFromGAVTC("org.carlspring.strongbox.searches:test-project:1.0.11.3.1");
            Artifact artifact3 = getArtifactFromGAVTC("org.carlspring.strongbox.searches:test-project:1.0.11.3.2");

            MavenArtifactDeployer artifactDeployer = buildArtifactDeployer(strongboxBaseDir);

            artifactDeployer.generateAndDeployArtifact(artifact1, classifiers, "storage0", "releases", "jar");
            artifactDeployer.generateAndDeployArtifact(artifact2, classifiers, "storage0", "releases", "jar");
            artifactDeployer.generateAndDeployArtifact(artifact3, classifiers, "storage0", "releases", "jar");

            final RepositoryIndexer repositoryIndexer = repositoryIndexManager.getRepositoryIndexer("storage0:releases:local");

            assertNotNull(repositoryIndexer);

            repositoryManagementService.reIndex("storage0", "releases", "org/carlspring/strongbox/searches");
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to prepare mock data", e);
        }
    }

    @Test
    public void doSearchTests()
            throws Exception
    {
        final String q = "g:org.carlspring.strongbox.searches a:test-project";

        // testSearchPlainText
        String response = client.search(q, MediaType.TEXT_PLAIN_VALUE);

        assertTrue("Received unexpected response! \n" + response + "\n",
                   response.contains("org.carlspring.strongbox.searches:test-project:1.0.11.3:jar") &&
                   response.contains("org.carlspring.strongbox.searches:test-project:1.0.11.3.1:jar"));

        // testSearchJSON

        response = client.search(q, MediaType.APPLICATION_JSON_VALUE);

        assertTrue("Received unexpected response! \n" + response + "\n",
                   response.contains("\"version\" : \"1.0.11.3\"") &&
                   response.contains("\"version\" : \"1.0.11.3.1\""));

        // testSearchXML
        response = client.search(q, MediaType.APPLICATION_XML_VALUE);

        assertTrue("Received unexpected response! \n" + response + "\n",
                   response.contains(">1.0.11.3<") && response.contains(">1.0.11.3.1<"));
    }

}
