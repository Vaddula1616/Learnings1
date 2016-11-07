package org.carlspring.strongbox.services.impl;

import org.carlspring.strongbox.client.ArtifactTransportException;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.io.ArtifactInputStream;
import org.carlspring.strongbox.providers.ProviderImplementationException;
import org.carlspring.strongbox.providers.repository.RepositoryProvider;
import org.carlspring.strongbox.providers.repository.RepositoryProviderRegistry;
import org.carlspring.strongbox.services.ArtifactResolutionService;
import org.carlspring.strongbox.storage.ArtifactResolutionException;
import org.carlspring.strongbox.storage.ArtifactStorageException;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.validation.resource.ArtifactOperationsValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

/**
 * @author mtodorov
 */
@Component
public class ArtifactResolutionServiceImpl
        implements ArtifactResolutionService
{

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private ArtifactOperationsValidator artifactOperationsValidator;

    @Autowired
    private RepositoryProviderRegistry repositoryProviderRegistry;


    @Override
    public ArtifactInputStream getInputStream(String storageId,
                                              String repositoryId,
                                              String artifactPath)
            throws IOException,
                   NoSuchAlgorithmException,
                   ArtifactTransportException,
                   ProviderImplementationException
    {
        artifactOperationsValidator.validate(storageId, repositoryId, artifactPath);

        final Repository repository = getStorage(storageId).getRepository(repositoryId);

        RepositoryProvider repositoryProvider = repositoryProviderRegistry.getProvider(repository.getType());

        ArtifactInputStream is = repositoryProvider.getInputStream(storageId, repositoryId, artifactPath);
        if (is == null) {
            throw new ArtifactResolutionException("Artifact " + artifactPath + " not found");
        }
        return is;
    }

    @Override
    public OutputStream getOutputStream(String storageId,
                                        String repositoryId,
                                        String artifactPath)
            throws IOException,
                   ProviderImplementationException
    {
        artifactOperationsValidator.validate(storageId, repositoryId, artifactPath);

        final Repository repository = getStorage(storageId).getRepository(repositoryId);

        RepositoryProvider repositoryProvider = repositoryProviderRegistry.getProvider(repository.getType());

        OutputStream os = repositoryProvider.getOutputStream(storageId, repositoryId, artifactPath);
        if (os == null)
        {
            throw new ArtifactStorageException("Artifact " + artifactPath + " cannot be stored.");
        }

        return os;
    }

    public Storage getStorage(String storageId)
    {
        return configurationManager.getConfiguration().getStorage(storageId);
    }

}
