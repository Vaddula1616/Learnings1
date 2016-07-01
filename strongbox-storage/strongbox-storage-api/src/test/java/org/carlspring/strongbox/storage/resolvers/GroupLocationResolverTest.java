package org.carlspring.strongbox.storage.resolvers;

import com.carmatechnologies.commons.testing.logging.ExpectedLogs;
import com.carmatechnologies.commons.testing.logging.api.LogLevel;
<<<<<<< HEAD
import org.carlspring.strongbox.BaseStorageApiTest;
import org.carlspring.strongbox.configuration.Configuration;
=======
import org.carlspring.strongbox.CommonConfig;
import org.carlspring.strongbox.StorageApiConfig;
import org.carlspring.strongbox.client.ArtifactTransportException;
>>>>>>> upstream/master
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.configuration.ConfigurationRepository;
import org.carlspring.strongbox.resource.ConfigurationResourceResolver;
import org.carlspring.strongbox.resource.ResourceCloser;
import org.carlspring.strongbox.storage.repository.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author mtodorov
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class GroupLocationResolverTest
        extends BaseStorageApiTest
{

    private static final File STORAGE_BASEDIR = new File(ConfigurationResourceResolver.getVaultDirectory() + "/storages/storage0");

    private static final File REPOSITORY_BASEDIR_RELEASES_BY_JUAN = new File(STORAGE_BASEDIR, "releases-by-juan");
    
    private static final File REPOSITORY_BASEDIR_RELEASES = new File(STORAGE_BASEDIR, "releases");

    private static final File REPOSITORY_BASEDIR_RELEASES_WITH_TRASH = new File(STORAGE_BASEDIR, "releases-with-trash");

    private static final File REPOSITORY_BASEDIR_RELEASES_IN_MEMORY = new File(STORAGE_BASEDIR, "releases-in-memory");

    @Autowired
    private GroupLocationResolver groupLocationResolver;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Rule
    public final ExpectedLogs logs = new ExpectedLogs()
    {{
        captureFor(GroupLocationResolver.class, LogLevel.DEBUG);
    }};

    public static boolean INITIALIZED;


    @Before
    public void setUp()
            throws Exception
    {
        if (!INITIALIZED)
        {
            generateArtifact(REPOSITORY_BASEDIR_RELEASES_WITH_TRASH.getAbsolutePath(), "com.artifacts.in.releases.with.trash:foo:1.2.3");
            generateArtifact(REPOSITORY_BASEDIR_RELEASES.getAbsolutePath(), "com.artifacts.in.releases:foo:1.2.4");
            generateArtifact(REPOSITORY_BASEDIR_RELEASES_IN_MEMORY.getAbsolutePath(), "com.artifacts.denied.in.memory:foo:1.2.5");
            generateArtifact(REPOSITORY_BASEDIR_RELEASES.getAbsolutePath(), "com.artifacts.denied.by.wildcard:foo:1.2.6");

            generateArtifact(REPOSITORY_BASEDIR_RELEASES_BY_JUAN.getAbsolutePath(), "org.carlspring.metadata.by.juan:juancho:1.2.64");
 
            INITIALIZED = true;
        }
    }

    @After
    public void tearDown()
            throws Exception
    {
        Configuration configuration = configurationManager.getConfiguration();
        Repository repository = configuration.getStorage("storage0").getRepository("releases");
        if (!repository.isInService())
        {
            repository.putInService();
            configurationRepository.updateConfiguration(configuration);
        }
    }

    @Test
    public void testGroupIncludes()
            throws IOException, NoSuchAlgorithmException, ArtifactTransportException
    {
        System.out.println("# Testing group includes...");

        InputStream is = groupLocationResolver.getInputStream("storage0",
                                                              "group-releases",
                                                              "com/artifacts/in/releases/with/trash/foo/1.2.3/foo-1.2.3.jar");

        assertNotNull(is);

        ResourceCloser.close(is, null);
    }

    @Test
    public void testGroupIncludesWithOutOfServiceRepository()
            throws IOException, NoSuchAlgorithmException, ArtifactTransportException
    {
        System.out.println("# Testing group includes with out of service repository...");

        /*Repository repository = configurationManager.getConfiguration().getStorage("storage0")

                .getRepository("releases");
        repository.putOutOfService();

        */
        Configuration configuration = configurationManager.getConfiguration();
        configuration.getStorage("storage0").getRepository("releases").putOutOfService();

        configurationRepository.updateConfiguration(configuration);

        InputStream is = groupLocationResolver.getInputStream("storage0",
                                                              "group-releases",
                                                              "com/artifacts/in/releases/foo/1.2.4/foo-1.2.4.jar");

        assertNull(is);

        ResourceCloser.close(is, null);
    }

    @Test
    public void testGroupIncludesWildcardRule()
            throws IOException, NoSuchAlgorithmException, ArtifactTransportException
    {
        System.out.println("# Testing group includes with wildcard...");

        InputStream is = groupLocationResolver.getInputStream("storage0",
                                                              "group-releases",
                                                              "com/artifacts/in/releases/foo/1.2.4/foo-1.2.4.jar");

        assertNotNull(is);

        ResourceCloser.close(is, null);
    }

    @Test
    public void testGroupIncludesWildcardRuleAgainstNestedRepository()
            throws IOException, NoSuchAlgorithmException, ArtifactTransportException
    {
        System.out.println("# Testing group includes with wildcard against nested repositories...");

        InputStream is = groupLocationResolver.getInputStream("storage0",
                                                              "group-releases-nested",
                                                              "com/artifacts/in/releases/foo/1.2.4/foo-1.2.4.jar");

        assertNotNull(is);

        ResourceCloser.close(is, null);
    }
    
    @Test
    public void testGroupAgainstNestedRepository()
            throws IOException, NoSuchAlgorithmException, ArtifactTransportException
    {
        System.out.println("# Testing group includes with wildcard against nested repositories...");

        InputStream is = groupLocationResolver.getInputStream("storage0",
                                                              "group-releases-nested-deep-1",
                                                              "org/carlspring/metadata/by/juan/juancho/1.2.64/juancho-1.2.64.jar");

        assertNotNull(is);

        ResourceCloser.close(is, null);
    }

    @Test
    public void testGroupExcludes()
            throws IOException, NoSuchAlgorithmException, ArtifactTransportException
    {
        System.out.println("# Testing group excludes...");

        InputStream is = groupLocationResolver.getInputStream("storage0",
                                                              "group-releases",
                                                              "com/artifacts/denied/in/memory/foo/1.2.5/foo-1.2.5.jar");


        assertNull(is);
    }

    @Test
    public void testGroupExcludesWildcardRule()
            throws IOException, NoSuchAlgorithmException, ArtifactTransportException
    {
        System.out.println("# Testing group excludes with wildcard...");

        InputStream is = groupLocationResolver.getInputStream("storage0",
                                                              "group-releases",
                                                              "com/artifacts/denied/by/wildcard/foo/1.2.6/foo-1.2.6.jar");


        assertNull(is);
    }

}
