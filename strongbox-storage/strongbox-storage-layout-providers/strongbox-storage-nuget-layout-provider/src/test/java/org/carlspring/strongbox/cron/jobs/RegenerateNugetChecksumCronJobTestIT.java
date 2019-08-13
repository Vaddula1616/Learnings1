package org.carlspring.strongbox.cron.jobs;

import org.carlspring.strongbox.artifact.coordinates.NugetArtifactCoordinates;
import org.carlspring.strongbox.config.NugetLayoutProviderCronTasksTestConfig;
import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.providers.io.RootRepositoryPath;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.repository.RepositoryPolicyEnum;
import org.carlspring.strongbox.testing.artifact.ArtifactManagementTestExecutionListener;
import org.carlspring.strongbox.testing.artifact.NugetTestArtifact;
import org.carlspring.strongbox.testing.repository.NugetRepository;
import org.carlspring.strongbox.testing.storage.repository.RepositoryManagementTestExecutionListener;
import org.carlspring.strongbox.util.ThrowingConsumer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import static java.nio.file.Files.deleteIfExists;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Kate Novik.
 * @author Pablo Tirado
 */
@ContextConfiguration(classes = NugetLayoutProviderCronTasksTestConfig.class)
@SpringBootTest
@ActiveProfiles(profiles = "test")
@Execution(CONCURRENT)
public class RegenerateNugetChecksumCronJobTestIT
        extends BaseCronJobWithNugetIndexingTestCase
{
    private static final String STORAGE1 = "storage-nuget-rnccj";

    private static final String REPOSITORY = "repository-rnccjt";

    @Override
    @BeforeEach
    public void init(TestInfo testInfo)
            throws Exception
    {
        super.init(testInfo);
    }

    @ExtendWith({ RepositoryManagementTestExecutionListener.class,
                  ArtifactManagementTestExecutionListener.class })
    @Test
    public void testRegenerateNugetArtifactChecksum(@NugetRepository(repositoryId = REPOSITORY)
                                                    Repository repository,
                                                    @NugetTestArtifact(repositoryId = REPOSITORY,
                                                                       id = "org.carlspring.strongbox.checksum-second",
                                                                       versions = "1.0.0")
                                                    Path artifactNupkgPath)
            throws Exception
    {
        Map<String, String> additionalProperties = Maps.newLinkedHashMap();
        additionalProperties.put("basePath", "org.carlspring.strongbox.checksum-second");
        additionalProperties.put("forceRegeneration", "false");

        testRegenerateNugetChecksum(repository,
                                    artifactNupkgPath,
                                    repository.getStorage().getId(),
                                    repository.getId(),
                                    additionalProperties);
    }

    @ExtendWith({ RepositoryManagementTestExecutionListener.class,
                         ArtifactManagementTestExecutionListener.class })
    @Test
    public void testRegenerateNugetChecksumInRepository(@NugetRepository(repositoryId = REPOSITORY,
                                                                         policy = RepositoryPolicyEnum.SNAPSHOT)
                                                        Repository repository,
                                                        @NugetTestArtifact(repositoryId = REPOSITORY,
                                                                           id = "org.carlspring.strongbox.checksum-one",
                                                                           versions = "1.0.1-alpha")
                                                        Path artifactNupkgPath)
            throws Exception
    {
        Map<String, String> additionalProperties = Maps.newLinkedHashMap();
        additionalProperties.put("forceRegeneration", "false");

        testRegenerateNugetChecksum(repository,
                                    artifactNupkgPath,
                                    repository.getStorage().getId(),
                                    repository.getId(),
                                    additionalProperties);
    }

    @ExtendWith({ RepositoryManagementTestExecutionListener.class,
                  ArtifactManagementTestExecutionListener.class })
    @Test
    public void testRegenerateNugetChecksumInStorage(@NugetRepository(repositoryId = REPOSITORY)
                                                     Repository repository,
                                                     @NugetTestArtifact(repositoryId = REPOSITORY,
                                                                        id = "org.carlspring.strongbox.checksum-second",
                                                                        versions = "1.0.0")
                                                     Path artifactNupkgPath)
            throws Exception
    {
        Map<String, String> additionalProperties = Maps.newLinkedHashMap();
        additionalProperties.put("forceRegeneration", "false");

        testRegenerateNugetChecksum(repository,
                                    artifactNupkgPath,
                                    repository.getStorage().getId(),
                                    null,
                                    additionalProperties);
    }

    @ExtendWith({ RepositoryManagementTestExecutionListener.class,
                  ArtifactManagementTestExecutionListener.class })
    @Test
    public void testRegenerateNugetChecksumInStorages(@NugetRepository(storageId = STORAGE1,
                                                                       repositoryId = REPOSITORY)
                                                      Repository repository,
                                                      @NugetTestArtifact(storageId = STORAGE1,
                                                                         repositoryId = REPOSITORY,
                                                                         id = "org.carlspring.strongbox.checksum-one",
                                                                         versions = "1.0.0")
                                                      Path artifactNupkgPath)
            throws Exception
    {
        Map<String, String> additionalProperties = Maps.newLinkedHashMap();
        additionalProperties.put("forceRegeneration", "false");

        testRegenerateNugetChecksum(repository,
                                    artifactNupkgPath,
                                    null,
                                    null,
                                    additionalProperties);
    }

    private void testRegenerateNugetChecksum(Repository repository,
                                             Path artifactNupkgPath,
                                             String cronJobConfigStorageId,
                                             String cronJobConfigRepositoryId,
                                             Map<String, String> cronJobConfigProperties)
            throws Exception
    {
        final UUID jobKey = expectedJobKey;
        final String jobName = expectedJobName;

        RootRepositoryPath repositoryPath = repositoryPathResolver.resolve(repository);

        Path nupkgSha512Path = artifactNupkgPath.resolveSibling(artifactNupkgPath.getFileName() + ".sha512");
        deleteIfExists(nupkgSha512Path);
        assertTrue(Files.notExists(nupkgSha512Path),"The checksum file for nupkg artifact exist!");

        NugetArtifactCoordinates coordinates = (NugetArtifactCoordinates) RepositoryFiles.readCoordinates(
                (RepositoryPath) artifactNupkgPath.normalize());
        coordinates.setType("nuspec");
        Path nuspecSha512Path = repositoryPath.resolve(coordinates.toPath() + ".sha512");
        deleteIfExists(nuspecSha512Path);
        assertTrue(Files.notExists(nuspecSha512Path),"The checksum file for nuspec artifact exist!");

        List<Path> resultList = new ArrayList<>();
        jobManager.registerExecutionListener(jobKey.toString(), (jobKey1, statusExecuted) -> {
            if (!StringUtils.equals(jobKey1, jobKey.toString()) || !statusExecuted)
            {
                return;
            }
            resultList.add(nupkgSha512Path);
            resultList.add(nuspecSha512Path);
        });

        addCronJobConfig(jobKey,
                         jobName,
                         RegenerateChecksumCronJob.class,
                         cronJobConfigStorageId,
                         cronJobConfigRepositoryId,
                         properties -> properties.putAll(cronJobConfigProperties));

        await().atMost(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilTrue(receivedExpectedEvent());

        assertEquals(2, resultList.size());
        resultList.forEach(ThrowingConsumer.unchecked(path -> {
            assertTrue(Files.exists(path), "The checksum file " + path.toString() + " doesn't exist!");
            assertTrue(Files.size(path) > 0, "The checksum file is empty!");
        }));
    }
}
