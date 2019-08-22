package org.carlspring.strongbox.providers.repository;

import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.providers.io.RepositoryPathResolver;
import org.carlspring.strongbox.providers.repository.proxied.LocalStorageProxyRepositoryExpiredArtifactsCleaner;
import org.carlspring.strongbox.services.ArtifactEntryService;
import org.carlspring.strongbox.services.ConfigurationManagementService;
import org.carlspring.strongbox.storage.repository.remote.heartbeat.RemoteRepositoryAlivenessCacheManager;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.InputStream;
import java.util.Optional;

import org.apache.commons.lang.time.DateUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Przemyslaw Fusik
 * @author Pablo Tirado
 */
abstract class BaseLocalStorageProxyRepositoryExpiredArtifactsCleanerTest
{
    protected static final String STORAGE_ID = "storage-common-proxies";

    @Inject
    protected ProxyRepositoryProvider proxyRepositoryProvider;

    @Inject
    protected ArtifactEntryService artifactEntryService;

    @Inject
    protected LocalStorageProxyRepositoryExpiredArtifactsCleaner localStorageProxyRepositoryExpiredArtifactsCleaner;

    @Inject
    @Named("mockedRemoteRepositoryAlivenessCacheManager")
    protected RemoteRepositoryAlivenessCacheManager remoteRepositoryAlivenessCacheManager;

    @Inject
    protected RepositoryPathResolver repositoryPathResolver;

    @Inject
    protected ConfigurationManagementService configurationManagementService;

    protected abstract String getRepositoryId();

    protected abstract String getPath();

    protected String getGroupId()
    {
        return "org.carlspring.maven";
    }

    protected String getArtifactId()
    {
        return "maven-commons";
    }

    protected abstract String getVersion();

    protected ArtifactEntry downloadAndSaveArtifactEntry()
            throws Exception
    {
        Optional<ArtifactEntry> artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(STORAGE_ID,
                                                                                                                 getRepositoryId(),
                                                                                                                 getPath()));
        assertThat(artifactEntryOptional, CoreMatchers.equalTo(Optional.empty()));

        RepositoryPath repositoryPath = proxyRepositoryProvider.fetchPath(repositoryPathResolver.resolve(STORAGE_ID,
                                                                                                         getRepositoryId(),
                                                                                                         getPath()));
        try (final InputStream ignored = proxyRepositoryProvider.getInputStream(repositoryPath))
        {
            assertNotNull(ignored, "Failed to resolve " + repositoryPath + "!");
        }

        artifactEntryOptional = Optional.ofNullable(artifactEntryService.findOneArtifact(STORAGE_ID,
                                                                                         getRepositoryId(),
                                                                                         getPath()));
        ArtifactEntry artifactEntry = artifactEntryOptional.orElse(null);
        assertThat(artifactEntry, CoreMatchers.notNullValue());
        assertThat(artifactEntry.getLastUpdated(), CoreMatchers.notNullValue());
        assertThat(artifactEntry.getLastUsed(), CoreMatchers.notNullValue());
        assertThat(artifactEntry.getSizeInBytes(), CoreMatchers.notNullValue());
        assertThat(artifactEntry.getSizeInBytes(), Matchers.greaterThan(0L));

        artifactEntry.setLastUsed(DateUtils.addDays(artifactEntry.getLastUsed(), -10));

        return artifactEntryService.save(artifactEntry);
    }

    protected Configuration getConfiguration()
    {
        return configurationManagementService.getConfiguration();
    }

}
