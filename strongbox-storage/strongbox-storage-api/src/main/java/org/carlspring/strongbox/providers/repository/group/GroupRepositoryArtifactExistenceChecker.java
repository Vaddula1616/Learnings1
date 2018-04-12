package org.carlspring.strongbox.providers.repository.group;

import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.providers.layout.LayoutProvider;
import org.carlspring.strongbox.providers.layout.LayoutProviderRegistry;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.springframework.stereotype.Component;

/**
 * @author Przemyslaw Fusik
 */
@Component
public class GroupRepositoryArtifactExistenceChecker
{

    @Inject
    private ConfigurationManager configurationManager;

    @Inject
    private LayoutProviderRegistry layoutProviderRegistry;

    public boolean artifactExistsInTheGroupRepositorySubTree(final Repository groupRepository,
                                                             final String artifactPath)
            throws IOException
    {
        return artifactExistsInTheGroupRepositorySubTree(groupRepository, artifactPath, new HashMap<>());
    }

    public boolean artifactExistsInTheGroupRepositorySubTree(final Repository groupRepository,
                                                             final String artifactPath,
                                                             final Map<String, MutableBoolean> repositoryArtifactExistence)
            throws IOException
    {
        for (final String maybeStorageAndRepositoryId : groupRepository.getGroupRepositories().keySet())
        {
            final String subStorageId = getStorageId(groupRepository, maybeStorageAndRepositoryId);
            final String subRepositoryId = getRepositoryId(maybeStorageAndRepositoryId);
            final Repository subRepository = getRepository(subStorageId, subRepositoryId);

            final String storageAndRepositoryId = subStorageId + ":" + subRepositoryId;
            repositoryArtifactExistence.putIfAbsent(storageAndRepositoryId, new MutableBoolean());
            if (repositoryArtifactExistence.get(storageAndRepositoryId).isTrue())
            {
                return true;
            }

            if (subRepository.isGroupRepository())
            {
                boolean artifactExistence = artifactExistsInTheGroupRepositorySubTree(subRepository,
                                                                                      artifactPath,
                                                                                      repositoryArtifactExistence);
                if (artifactExistence)
                {
                    repositoryArtifactExistence.get(storageAndRepositoryId).setTrue();
                    return true;
                }
            }
            else
            {
                final LayoutProvider layoutProvider = layoutProviderRegistry.getProvider(subRepository.getLayout());
                if (layoutProvider.containsPath(subRepository, artifactPath))
                {
                    repositoryArtifactExistence.get(storageAndRepositoryId).setTrue();
                    return true;
                }
            }
        }
        return false;
    }

    private Repository getRepository(final String subStorageId,
                                     final String subRepositoryId)
    {
        return configurationManager.getConfiguration()
                                   .getStorage(subStorageId)
                                   .getRepository(subRepositoryId);
    }

    private String getRepositoryId(final String maybeStorageAndRepositoryId)
    {
        return configurationManager.getRepositoryId(maybeStorageAndRepositoryId);
    }

    private String getStorageId(final Repository groupRepository,
                                final String maybeStorageAndRepositoryId)
    {
        return configurationManager.getStorageId(groupRepository.getStorage(),
                                                 maybeStorageAndRepositoryId);
    }

}
