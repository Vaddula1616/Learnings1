package org.carlspring.strongbox.services.impl;

import static org.carlspring.strongbox.services.support.ArtifactEntrySearchCriteria.Builder.anArtifactEntrySearchCriteria;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.time.DateUtils;
import org.carlspring.strongbox.StorageApiTestConfig;
import org.carlspring.strongbox.artifact.coordinates.AbstractArtifactCoordinates;
import org.carlspring.strongbox.artifact.coordinates.ArtifactCoordinates;
import org.carlspring.strongbox.artifact.coordinates.NullArtifactCoordinates;
import org.carlspring.strongbox.data.CacheManagerTestExecutionListener;
import org.carlspring.strongbox.data.service.support.search.PagingCriteria;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.services.ArtifactEntryService;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Functional test and usage example scenarios for {@link ArtifactEntryService}.
 *
 * @author Alex Oreshkevich
 * @see https://dev.carlspring.org/youtrack/issue/SB-711
 */
@SpringBootTest
@ActiveProfiles(profiles = "test")
@ContextConfiguration(classes = StorageApiTestConfig.class)
@TestExecutionListeners(listeners = { CacheManagerTestExecutionListener.class }, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Execution(ExecutionMode.SAME_THREAD)
public class ArtifactEntryServiceTest
{

    private static final Logger logger = LoggerFactory.getLogger(ArtifactEntryServiceTest.class);

    final String storageId = "storage0";

    final String repositoryId = "aest";

    final String groupId = "org.carlspring.strongbox.aest";

    final String artifactId = "coordinates-test";

    @Inject
    ArtifactEntryService artifactEntryService;

    @Inject
    ArtifactCoordinatesService artifactCoordinatesService;

    @BeforeEach
    public void setup() {
        createArtifacts(groupId, artifactId, storageId, repositoryId);
        
        displayAllEntries();
    }
    
    @AfterEach
    public void cleanup() {
        List<ArtifactEntry> artifactEntries = findAll();
        artifactEntries.forEach(e -> e.getCreated());
        List<AbstractArtifactCoordinates> artifactCoordinates = artifactEntries.stream()
                                                                               .map(e -> (AbstractArtifactCoordinates) e.getArtifactCoordinates())
                                                                               .collect(Collectors.toList());
        artifactEntryService.delete(artifactEntries);
        artifactCoordinatesService.delete(artifactCoordinates);
        
        displayAllEntries();
    }

    protected List<ArtifactEntry> findAll()
    {
        HashMap<String, String> coordinates = new HashMap<>();
        coordinates.put("path", String.format("%s", groupId));
        List<ArtifactEntry> artifactEntries = new ArrayList<ArtifactEntry>(artifactEntryService.findArtifactList(null, null, coordinates, false));
        return artifactEntries;
    }
    
    protected int count() {
        return findAll().size();
    }
    
    @Test
    public void saveEntityShouldWork()
    {
        ArtifactEntry artifactEntry = new ArtifactEntry();
        artifactEntry.setStorageId(storageId);
        artifactEntry.setRepositoryId(repositoryId);
        artifactEntry.setArtifactCoordinates(new NullArtifactCoordinates(String.format("%s/%s/%s/%s",
                                                                                       groupId,
                                                                                       artifactId + "1234",
                                                                                       "1.2.3",
                                                                                       "jar")));

        assertThat(artifactEntry.getCreated(), CoreMatchers.nullValue());

        artifactEntry = save(artifactEntry);

        assertThat(artifactEntry.getCreated(), CoreMatchers.notNullValue());

        Date creationDate = artifactEntry.getCreated();
        //Updating artifact entry in order to ensure that creationDate is not updated
        artifactEntry.setDownloadCount(1);
        artifactEntry = save(artifactEntry);

        assertEquals(artifactEntry.getCreated(), creationDate);
    }

    @Test
    public void cascadeUpdateShouldWork()
    {
        Optional<ArtifactEntry> artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(storageId,
                                                                                                                 repositoryId,
                                                                                                                 "org.carlspring.strongbox.aest/coordinates-test123/1.2.3/jar"));

        assertTrue(artifactEntryOptional.isPresent());

        ArtifactEntry artifactEntry = artifactEntryOptional.get();
        assertThat(artifactEntry.getArtifactCoordinates(), CoreMatchers.notNullValue());
        assertEquals("org.carlspring.strongbox.aest/coordinates-test123/1.2.3/jar",
                     artifactEntry.getArtifactCoordinates().toPath());

        //Simple field update
        artifactEntry.setRepositoryId(repositoryId + "abc");
        artifactEntry = save(artifactEntry);

        artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(storageId, repositoryId,
                                                                                         "org.carlspring.strongbox.aest/coordinates-test123/1.2.3/jar"));
        assertFalse(artifactEntryOptional.isPresent());

        artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(storageId,
                                                                                         repositoryId + "abc",
                                                                                         "org.carlspring.strongbox.aest/coordinates-test123/1.2.3/jar"));
        assertTrue(artifactEntryOptional.isPresent());

        //Cascade field update
        NullArtifactCoordinates nullArtifactCoordinates = (NullArtifactCoordinates)artifactEntry.getArtifactCoordinates();
        nullArtifactCoordinates.setId("org.carlspring.strongbox.aest/coordinates-test123/1.2.3/pom");
        save(artifactEntry);

        artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(storageId,
                                                                                         repositoryId + "abc",
                                                                                         "org.carlspring.strongbox.aest/coordinates-test123/1.2.3/jar"));
        assertFalse(artifactEntryOptional.isPresent());

        artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(storageId,
                                                                                         repositoryId + "abc",
                                                                                         "org.carlspring.strongbox.aest/coordinates-test123/1.2.3/pom"));
        assertTrue(artifactEntryOptional.isPresent());

    }

    private ArtifactEntry save(ArtifactEntry artifactEntry)
    {
        ArtifactEntry result = artifactEntryService.save(artifactEntry);
        
        return result;
    }
    
    @Test
    public void searchBySizeShouldWork()
            throws Exception
    {
        int all = count();
        updateArtifactAttributes();

        List<ArtifactEntry> entries = artifactEntryService.findMatching(anArtifactEntrySearchCriteria()
                                                                                                       .withMinSizeInBytes(500l)
                                                                                                       .build(),
                                                                        PagingCriteria.ALL)
                                                          .stream()
                                                          .filter(e -> e.getRepositoryId().equals(repositoryId))
                                                          .collect(Collectors.toList());

        assertThat(entries.size(), CoreMatchers.equalTo(all - 1));
    }

    @Test
    public void searchByLastUsedShouldWork()
            throws Exception
    {
        int all = count();
        updateArtifactAttributes();

        List<ArtifactEntry> entries = artifactEntryService.findMatching(anArtifactEntrySearchCriteria()
                                                                                                       .withLastAccessedTimeInDays(5)
                                                                                                       .build(),
                                                                        PagingCriteria.ALL)
                                                          .stream()
                                                          .filter(e -> e.getRepositoryId().equals(repositoryId))
                                                          .collect(Collectors.toList());

        assertThat(entries.size(), CoreMatchers.equalTo(all - 1));
    }

    @Test
    public void deleteAllShouldWork()
            throws Exception
    {
        int all = count();
        assertThat(all, CoreMatchers.equalTo(3));

        List<ArtifactEntry> artifactEntries = findAll();
        int removed = artifactEntryService.delete(artifactEntries);
        assertThat(removed, CoreMatchers.equalTo(all));

        int left = count();
        assertThat(left, CoreMatchers.equalTo(0));
        assertTrue(findAll().isEmpty());
    }

    @Test
    public void deleteButNotAllShouldWork()
            throws Exception
    {
        int all = count();
        assertThat(all, CoreMatchers.equalTo(3));

        List<ArtifactEntry> artifactEntries = findAll();
        artifactEntries.remove(0);
        int removed = artifactEntryService.delete(artifactEntries);
        assertThat(removed, CoreMatchers.equalTo(all - 1));

        int left = count();
        assertThat(left, CoreMatchers.equalTo(1));
    }

    @Test
    public void searchByLastUsedAndBySizeShouldWork()
            throws Exception
    {
        int all = count();
        updateArtifactAttributes();

        List<ArtifactEntry> entries = artifactEntryService.findMatching(anArtifactEntrySearchCriteria()
                                                                                                       .withMinSizeInBytes(500l)
                                                                                                       .withLastAccessedTimeInDays(5)
                                                                                                       .build(),
                                                                        PagingCriteria.ALL)
                                                          .stream()
                                                          .filter(e -> e.getRepositoryId().equals(repositoryId))
                                                          .collect(Collectors.toList());

        assertThat(entries.size(), CoreMatchers.equalTo(all - 1));
    }

    /**
     * Make sure that we are able to search artifacts by single coordinate.
     *
     * @throws Exception
     */
    @Test
    public void searchBySingleCoordinate()
            throws Exception
    {
        logger.debug("There are a total of " + count() + " artifacts.");

        // prepare search query key (coordinates)
        NullArtifactCoordinates coordinates = new NullArtifactCoordinates(groupId + "/");

        List<ArtifactEntry> artifactEntries = artifactEntryService.findArtifactList(storageId, repositoryId, coordinates.getCoordinates(), false);

        assertNotNull(artifactEntries);
        assertFalse(artifactEntries.isEmpty());
        assertEquals(2, artifactEntries.size());

        artifactEntries.forEach(artifactEntry ->
                                {
                                    logger.debug("Found artifact " + artifactEntry);
                                    assertTrue(((NullArtifactCoordinates)artifactEntry.getArtifactCoordinates()).getPath().startsWith(groupId + "/"));
                                });
    }

    /**
     * Make sure that we are able to search artifacts by two coordinates that need to be joined with logical AND operator.
     */
    @Test
    public void searchByTwoCoordinate()
            throws Exception
    {
        logger.debug("There are a total of " + count() + " artifacts.");

        // prepare search query key (coordinates)
        NullArtifactCoordinates c1 = new NullArtifactCoordinates(groupId + "/" + artifactId + "/");

        List<ArtifactEntry> result = artifactEntryService.findArtifactList(storageId, repositoryId, c1.getCoordinates(), false);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        assertEquals(1, result.size());

        result.forEach(artifactEntry ->
                       {
                           logger.debug("Found artifact " + artifactEntry);
                           assertTrue(((NullArtifactCoordinates)artifactEntry.getArtifactCoordinates()).getPath().startsWith(groupId + "/" + artifactId));
                       });

        Long c = artifactEntryService.countArtifacts(storageId, repositoryId, c1.getCoordinates(), false);
        assertEquals(Long.valueOf(1), c);
    }

    public void displayAllEntries()
    {
        List<ArtifactEntry> result = findAll();
        if (result == null || result.isEmpty())
        {
            logger.debug("Artifact repository is empty");
        }

        result.forEach(artifactEntry -> logger.debug("Found artifact " + "["
                + artifactEntry.getArtifactCoordinates().getId() + "]" + artifactEntry));
    }

    public void createArtifacts(String groupId,
                                String artifactId,
                                String storageId,
                                String repositoryId)
    {
        // create 3 artifacts, one will have coordinates that matches our query, one - not
        ArtifactCoordinates coordinates1 = new NullArtifactCoordinates(String.format("%s/%s/%s/%s", groupId, artifactId + "123", "1.2.3", "jar"));
        ArtifactCoordinates coordinates2 = new NullArtifactCoordinates(String.format("%s/%s/%s/%s", groupId, artifactId, "1.2.3", "jar"));
        ArtifactCoordinates coordinates3 = new NullArtifactCoordinates(String.format("%s/%s/%s/%s", groupId  + "myId", artifactId + "321", "1.2.3", "jar"));

        createArtifactEntry(coordinates1, storageId, repositoryId);
        createArtifactEntry(coordinates2, storageId, repositoryId);
        createArtifactEntry(coordinates3, storageId, repositoryId);
    }

    public ArtifactEntry createArtifactEntry(ArtifactCoordinates coordinates,
                                             String storageId,
                                             String repositoryId)
    {
        ArtifactEntry artifactEntry = new ArtifactEntry();
        artifactEntry.setArtifactCoordinates(coordinates);
        artifactEntry.setStorageId(storageId);
        artifactEntry.setRepositoryId(repositoryId);

        return save(artifactEntry);
    }

    public ArtifactCoordinates createMavenArtifactCoordinates()
    {

        return new NullArtifactCoordinates(String.format("%s/%s/%s/%s", "org.carlspring.strongbox.aest.another.package", "coordinates-test-super-test", "1.2.3", "jar"));
    }

    private void updateArtifactAttributes()
    {
        List<ArtifactEntry> artifactEntries = findAll();
        for (int i = 0; i < artifactEntries.size(); i++)
        {
            final ArtifactEntry artifactEntry = artifactEntries.get(i);
            if (i == 0)
            {
                artifactEntry.setLastUsed(new Date());
                artifactEntry.setLastUpdated(new Date());
                artifactEntry.setSizeInBytes(1l);
            }
            else
            {
                artifactEntry.setLastUsed(DateUtils.addDays(new Date(), -10));
                artifactEntry.setLastUpdated(DateUtils.addDays(new Date(), -10));
                artifactEntry.setSizeInBytes(100000l);
            }

            save(artifactEntry);
        }
    }

}
