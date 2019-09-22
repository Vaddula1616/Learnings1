package org.carlspring.strongbox.services.impl;

import org.carlspring.strongbox.StorageApiTestConfig;
import org.carlspring.strongbox.artifact.coordinates.AbstractArtifactCoordinates;
import org.carlspring.strongbox.artifact.coordinates.ArtifactCoordinates;
import org.carlspring.strongbox.artifact.coordinates.RawArtifactCoordinates;
import org.carlspring.strongbox.data.CacheManagerTestExecutionListener;
import org.carlspring.strongbox.data.service.support.search.PagingCriteria;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.services.ArtifactEntryService;

import javax.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.util.CollectionUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.carlspring.strongbox.services.support.ArtifactEntrySearchCriteria.Builder.anArtifactEntrySearchCriteria;

/**
 * Functional test and usage example scenarios for {@link ArtifactEntryService}.
 *
 * @author Alex Oreshkevich
 * @author Pablo Tirado
 * @see https://dev.carlspring.org/youtrack/issue/SB-711
 */
@SpringBootTest
@ActiveProfiles(profiles = "test")
@ContextConfiguration(classes = StorageApiTestConfig.class)
@TestExecutionListeners(listeners = { CacheManagerTestExecutionListener.class },
                        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ArtifactEntryServiceTest
{

    private static final Logger logger = LoggerFactory.getLogger(ArtifactEntryServiceTest.class);

    private final String STORAGE_ID = "storage0";

    private final String REPOSITORY_ID = "aest";

    private final String GROUP_ID = "org.carlspring.strongbox.aest";

    private final String ARTIFACT_ID = "coordinates-test";

    private final String LOG_PATTERN = "###################### [%s] ######################";

    @Inject
    private ArtifactEntryService artifactEntryService;

    @Inject
    private ArtifactCoordinatesService artifactCoordinatesService;

    @BeforeEach
    public void setup(TestInfo testInfo)
    {
        displayAllEntries(testInfo, "setup before");

        final String repositoryId = getRepositoryId(testInfo);
        final String groupId = getGroupId(testInfo);

        createArtifacts(groupId,
                        ARTIFACT_ID,
                        STORAGE_ID,
                        repositoryId);

        displayAllEntries(testInfo, "setup after");
    }

    private String getRepositoryId(TestInfo testInfo)
    {
        String methodName = getMethodName(testInfo);
        return REPOSITORY_ID + "-" + methodName;
    }

    private String getGroupId(TestInfo testInfo)
    {
        String methodName = getMethodName(testInfo);
        return GROUP_ID + "." + methodName;
    }

    private String getMethodName(TestInfo testInfo)
    {
        Assumptions.assumeTrue(testInfo.getTestMethod().isPresent());
        return testInfo.getTestMethod().get().getName();
    }

    private void createArtifacts(String groupId,
                                 String artifactId,
                                 String storageId,
                                 String repositoryId)
    {
        // create 3 artifacts, one will have coordinates that matches our query, one - not
        ArtifactCoordinates coordinates1 = createArtifactCoordinates(groupId, artifactId + "123", "1.2.3", "jar");
        ArtifactCoordinates coordinates2 = createArtifactCoordinates(groupId, artifactId, "1.2.3", "jar");
        ArtifactCoordinates coordinates3 = createArtifactCoordinates(groupId + ".myId", artifactId + "321", "1.2.3",
                                                                     "jar");

        createArtifactEntry(coordinates1, storageId, repositoryId);
        createArtifactEntry(coordinates2, storageId, repositoryId);
        createArtifactEntry(coordinates3, storageId, repositoryId);
    }

    private ArtifactCoordinates createArtifactCoordinates(final String groupId,
                                                          final String artifactId,
                                                          final String version,
                                                          final String extension)
    {

        return new RawArtifactCoordinates(String.format("%s/%s/%s/%s", groupId, artifactId, version, extension));
    }

    private void createArtifactEntry(ArtifactCoordinates coordinates,
                                     String storageId,
                                     String repositoryId)
    {
        ArtifactEntry artifactEntry = new ArtifactEntry();
        artifactEntry.setArtifactCoordinates(coordinates);
        artifactEntry.setStorageId(storageId);
        artifactEntry.setRepositoryId(repositoryId);

        save(artifactEntry);
    }

    @AfterEach
    public void cleanup(TestInfo testInfo)
    {
        displayAllEntries(testInfo, "cleanup before");

        List<ArtifactEntry> artifactEntries = findAll(testInfo);
        List<AbstractArtifactCoordinates> artifactCoordinates = artifactEntries.stream()
                                                                               .map(e -> (AbstractArtifactCoordinates) e.getArtifactCoordinates())
                                                                               .collect(Collectors.toList());
        artifactEntryService.delete(artifactEntries);
        artifactCoordinatesService.delete(artifactCoordinates);

        displayAllEntries(testInfo, "cleanup after");
    }

    private List<ArtifactEntry> findAll(TestInfo testInfo)
    {
        final String repositoryId = getRepositoryId(testInfo);
        final String groupId = getGroupId(testInfo);

        HashMap<String, String> coordinates = new HashMap<>();
        coordinates.put("path", groupId);
        return artifactEntryService.findArtifactList(null, repositoryId, coordinates, false);
    }

    private void displayAllEntries(TestInfo testInfo,
                                   String when)
    {
        List<ArtifactEntry> result = findAll(testInfo);
        if (CollectionUtils.isEmpty(result))
        {
            logger.info("{} Artifact repository is empty", when);
        }
        else
        {
            result.forEach(artifactEntry -> logger.info(when + " Found artifact [{}] - {}",
                                                        artifactEntry.getArtifactCoordinates().getId(),
                                                        artifactEntry));
        }
    }

    @Test
    public void saveEntityShouldWork(TestInfo testInfo)
    {
        final String repositoryId = getRepositoryId(testInfo);
        final String groupId = getGroupId(testInfo);

        ArtifactEntry artifactEntry = new ArtifactEntry();
        artifactEntry.setStorageId(STORAGE_ID);
        artifactEntry.setRepositoryId(repositoryId);
        artifactEntry.setArtifactCoordinates(createArtifactCoordinates(groupId,
                                                                       ARTIFACT_ID + "1234",
                                                                       "1.2.3",
                                                                       "jar"));

        assertThat(artifactEntry.getCreated()).isNull();

        artifactEntry = save(artifactEntry);

        assertThat(artifactEntry.getCreated()).isNotNull();

        Date creationDate = artifactEntry.getCreated();
        //Updating artifact entry in order to ensure that creationDate is not updated
        artifactEntry.setDownloadCount(1);
        artifactEntry = save(artifactEntry);

        assertThat(artifactEntry.getCreated()).isEqualTo(creationDate);
    }

    @Test
    public void cascadeUpdateShouldWork(TestInfo testInfo)
    {
        final String repositoryId = getRepositoryId(testInfo);
        final String groupId = getGroupId(testInfo);

        ArtifactCoordinates jarCoordinates = createArtifactCoordinates(groupId, ARTIFACT_ID + "123", "1.2.3", "jar");
        ArtifactCoordinates pomCoordinates = createArtifactCoordinates(groupId, ARTIFACT_ID + "123", "1.2.3", "pom");

        Optional<ArtifactEntry> artifactEntryOptional = Optional.ofNullable(
                artifactEntryService.findOneArtifact(STORAGE_ID,
                                                     repositoryId,
                                                     jarCoordinates.toPath()));

        assertThat(artifactEntryOptional).isPresent();

        ArtifactEntry artifactEntry = artifactEntryOptional.get();
        assertThat(artifactEntry).isNotNull();
        assertThat(artifactEntry.getArtifactCoordinates()).isNotNull();
        assertThat(artifactEntry.getArtifactCoordinates().toPath()).isEqualTo(jarCoordinates.toPath());

        //Simple field update
        artifactEntry.setRepositoryId(repositoryId + "abc");
        artifactEntry = save(artifactEntry);

        artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(STORAGE_ID,
                                                                                         repositoryId,
                                                                                         jarCoordinates.toPath()));
        assertThat(artifactEntryOptional).isNotPresent();

        artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(STORAGE_ID,
                                                                                         repositoryId + "abc",
                                                                                         jarCoordinates.toPath()));
        assertThat(artifactEntryOptional).isPresent();

        //Cascade field update
        RawArtifactCoordinates RawArtifactCoordinates = (RawArtifactCoordinates) artifactEntry.getArtifactCoordinates();
        RawArtifactCoordinates.setId(pomCoordinates.toPath());
        save(artifactEntry);

        artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(STORAGE_ID,
                                                                                         repositoryId + "abc",
                                                                                         jarCoordinates.toPath()));
        assertThat(artifactEntryOptional).isNotPresent();

        artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(STORAGE_ID,
                                                                                         repositoryId + "abc",
                                                                                         pomCoordinates.toPath()));
        assertThat(artifactEntryOptional).isPresent();
    }

    private ArtifactEntry save(ArtifactEntry artifactEntry)
    {
        return artifactEntryService.save(artifactEntry);
    }

    @Test
    public void searchBySizeShouldWork(TestInfo testInfo)
    {
        final String repositoryId = getRepositoryId(testInfo);

        List<ArtifactEntry> allArtifactEntries = findAll(testInfo);
        int all = allArtifactEntries.size();
        updateArtifactAttributes(testInfo);

        List<ArtifactEntry> entries = artifactEntryService.findMatching(anArtifactEntrySearchCriteria()
                                                                                .withMinSizeInBytes(500L)
                                                                                .build(),
                                                                        PagingCriteria.ALL)
                                                          .stream()
                                                          .filter(e -> e.getRepositoryId().equals(repositoryId))
                                                          .collect(Collectors.toList());

        entries.forEach(entry -> logger.debug("Found artifact after search: [{}] - {}",
                                              entry.getArtifactCoordinates().getId(),
                                              entry));

        assertThat(entries).hasSize(all - 1);
    }

    @Test
    public void searchByLastUsedShouldWork(TestInfo testInfo)
    {
        final String repositoryId = getRepositoryId(testInfo);

        List<ArtifactEntry> allArtifactEntries = findAll(testInfo);
        int all = allArtifactEntries.size();
        updateArtifactAttributes(testInfo);

        List<ArtifactEntry> entries = artifactEntryService.findMatching(anArtifactEntrySearchCriteria()
                                                                                .withLastAccessedTimeInDays(5)
                                                                                .build(),
                                                                        PagingCriteria.ALL)
                                                          .stream()
                                                          .filter(e -> e.getRepositoryId().equals(repositoryId))
                                                          .collect(Collectors.toList());

        entries.forEach(entry -> logger.debug("Found artifact after search: [{}] - {}",
                                              entry.getArtifactCoordinates().getId(),
                                              entry));

        assertThat(entries).hasSize(all - 1);
    }

    @Test
    public void deleteAllShouldWork(TestInfo testInfo)
    {
        displayAllEntries(testInfo, "deleteAllShouldWork before");

        List<ArtifactEntry> artifactEntries = findAll(testInfo);
        int all = artifactEntries.size();
        assertThat(all).isEqualTo(3);

        int removed = artifactEntryService.delete(artifactEntries);
        assertThat(removed).isEqualTo(all);

        List<ArtifactEntry> artifactEntriesLeft = findAll(testInfo);
        assertThat(artifactEntriesLeft).isEmpty();

        displayAllEntries(testInfo, "deleteAllShouldWork after");
    }

    @Test
    public void deleteButNotAllShouldWork(TestInfo testInfo)
    {
        List<ArtifactEntry> artifactEntries = findAll(testInfo);
        int all = artifactEntries.size();
        assertThat(all).isEqualTo(3);

        artifactEntries.remove(0);
        int removed = artifactEntryService.delete(artifactEntries);
        assertThat(removed).isEqualTo(all - 1);

        List<ArtifactEntry> artifactEntriesLeft = findAll(testInfo);
        int left = artifactEntriesLeft.size();
        assertThat(left).isEqualTo(1);
    }

    @Test
    public void searchByLastUsedAndBySizeShouldWork(TestInfo testInfo)
    {
        displayAllEntries(testInfo, "searchByLastUsedAndBySizeShouldWork before");

        final String repositoryId = getRepositoryId(testInfo);

        logger.info(String.format(LOG_PATTERN, String.format("repositoryId [%s]", repositoryId)));

        List<ArtifactEntry> allArtifactEntries = findAll(testInfo);
        allArtifactEntries.forEach(a -> logger.info(String.format(LOG_PATTERN, String.format("forEach allArtifactEntries [%s]", a))));

        int all = allArtifactEntries.size();
        logger.info(String.format(LOG_PATTERN, String.format("allArtifactEntries.size() [%d]", all)));

        updateArtifactAttributes(testInfo);

        List<ArtifactEntry> entries = artifactEntryService.findMatching(anArtifactEntrySearchCriteria()
                                                                                .withMinSizeInBytes(500L)
                                                                                .withLastAccessedTimeInDays(5)
                                                                                .build(),
                                                                        PagingCriteria.ALL)
                                                          .stream()
                                                          .filter(e -> e.getRepositoryId().equals(repositoryId))
                                                          .collect(Collectors.toList());

        entries.forEach(a -> logger.info(String.format(LOG_PATTERN, String.format("forEach findMatching [%s]", a))));
        logger.info(String.format(LOG_PATTERN, String.format("findMatching.size() [%d]", entries.size())));

        entries.forEach(entry -> logger.debug("Found artifact after search: [{}] - {}",
                                              entry.getArtifactCoordinates().getId(),
                                              entry));

        logger.info(String.format(LOG_PATTERN, String.format("assertThat [%d] [%d]", entries.size(), all - 1)));
        assertThat(entries).hasSize(all - 1);

        displayAllEntries(testInfo, "searchByLastUsedAndBySizeShouldWork after");
    }

    /**
     * Make sure that we are able to search artifacts by single coordinate.
     */
    @Test
    public void searchBySingleCoordinate(TestInfo testInfo)
    {
        final String repositoryId = getRepositoryId(testInfo);
        final String groupId = getGroupId(testInfo);

        List<ArtifactEntry> allArtifactEntries = findAll(testInfo);
        logger.debug("There are a total of {} artifacts.", allArtifactEntries.size());

        // prepare search query key (coordinates)
        RawArtifactCoordinates coordinates = new RawArtifactCoordinates(groupId + "/");

        List<ArtifactEntry> artifactEntries = artifactEntryService.findArtifactList(STORAGE_ID,
                                                                                    repositoryId,
                                                                                    coordinates.getCoordinates(),
                                                                                    false);

        assertThat(artifactEntries).isNotNull();
        assertThat(artifactEntries).isNotEmpty();
        assertThat(artifactEntries).hasSize(2);

        artifactEntries.forEach(artifactEntry ->
                                {
                                    logger.debug("Found artifact {}", artifactEntry);
                                    assertThat(
                                            (
                                            (RawArtifactCoordinates) artifactEntry.getArtifactCoordinates()).getPath().startsWith(
                                                    groupId + "/")).isTrue();
                                });
    }

    /**
     * Make sure that we are able to search artifacts by two coordinates that need to be joined with logical AND operator.
     */
    @Test
    public void searchByTwoCoordinate(TestInfo testInfo)
    {
        final String repositoryId = getRepositoryId(testInfo);
        final String groupId = getGroupId(testInfo);

        List<ArtifactEntry> allArtifactEntries = findAll(testInfo);
        logger.debug("There are a total of {} artifacts.", allArtifactEntries.size());

        // prepare search query key (coordinates)
        RawArtifactCoordinates c1 = new RawArtifactCoordinates(groupId + "/" + ARTIFACT_ID + "/");

        List<ArtifactEntry> result = artifactEntryService.findArtifactList(STORAGE_ID,
                                                                           repositoryId,
                                                                           c1.getCoordinates(),
                                                                           false);
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);

        result.forEach(artifactEntry ->
                       {
                           logger.debug("Found artifact {}", artifactEntry);
                           assertThat(
                                   (
                                   (RawArtifactCoordinates) artifactEntry.getArtifactCoordinates()).getPath().startsWith(
                                           groupId + "/" + ARTIFACT_ID)).isTrue();
                       });

        Long c = artifactEntryService.countArtifacts(STORAGE_ID,
                                                     repositoryId,
                                                     c1.getCoordinates(),
                                                     false);
        assertThat(c).isEqualTo(Long.valueOf(1));
    }

    private void updateArtifactAttributes(TestInfo testInfo)
    {
        List<ArtifactEntry> artifactEntries = findAll(testInfo);
        artifactEntries.forEach(a -> logger.info(String.format(LOG_PATTERN, String.format("forEach updateArtifactAttributes [%s]", a))));
        logger.info(String.format(LOG_PATTERN, String.format("updateArtifactAttributes.size() [%d]", artifactEntries.size())));

        for (int i = 0; i < artifactEntries.size(); i++)
        {
            final ArtifactEntry artifactEntry = artifactEntries.get(i);
            if (i == 0)
            {
                artifactEntry.setLastUsed(new Date());
                artifactEntry.setLastUpdated(new Date());
                artifactEntry.setSizeInBytes(1L);
            }
            else
            {
                artifactEntry.setLastUsed(DateUtils.addDays(new Date(), -10));
                artifactEntry.setLastUpdated(DateUtils.addDays(new Date(), -10));
                artifactEntry.setSizeInBytes(100000L);
            }

            save(artifactEntry);
        }
    }

}
