package org.carlspring.strongbox.services.impl;

import org.carlspring.commons.encryption.EncryptionAlgorithmsEnum;
import org.carlspring.commons.io.MultipleDigestInputStream;
import org.carlspring.maven.commons.util.ArtifactUtils;
import org.carlspring.strongbox.artifact.coordinates.ArtifactCoordinates;
import org.carlspring.strongbox.client.ArtifactTransportException;
import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.providers.ProviderImplementationException;
import org.carlspring.strongbox.providers.layout.LayoutProvider;
import org.carlspring.strongbox.providers.layout.LayoutProviderRegistry;
import org.carlspring.strongbox.resource.ResourceCloser;
import org.carlspring.strongbox.services.ArtifactManagementService;
import org.carlspring.strongbox.services.ArtifactResolutionService;
import org.carlspring.strongbox.services.VersionValidatorService;
import org.carlspring.strongbox.storage.ArtifactResolutionException;
import org.carlspring.strongbox.storage.ArtifactStorageException;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.checksum.ChecksumCacheManager;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexManager;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexer;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.validation.resource.ArtifactOperationsValidator;
import org.carlspring.strongbox.storage.validation.version.VersionValidationException;
import org.carlspring.strongbox.storage.validation.version.VersionValidator;
import org.carlspring.strongbox.util.ArtifactFileUtils;
import org.carlspring.strongbox.util.MessageDigestUtils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static org.carlspring.strongbox.providers.layout.LayoutProviderRegistry.getLayoutProvider;

/**
 * @author mtodorov
 */
@Component("artifactManagementService")
public class ArtifactManagementServiceImpl
    implements ArtifactManagementService
{

    private static final Logger logger = LoggerFactory.getLogger(ArtifactManagementServiceImpl.class);

    @Autowired
    private ArtifactResolutionService artifactResolutionService;

    @Autowired
    private VersionValidatorService versionValidatorService;

    @Autowired
    private ChecksumCacheManager checksumCacheManager;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private RepositoryIndexManager repositoryIndexManager;

    @Autowired
    private ArtifactOperationsValidator artifactOperationsValidator;

    @Autowired
    private LayoutProviderRegistry layoutProviderRegistry;


    @Override
    public void store(String storageId,
                      String repositoryId,
                      String path,
                      InputStream is)
            throws IOException, ProviderImplementationException, NoSuchAlgorithmException
    {
        performRepositoryAcceptanceValidation(storageId, repositoryId, path);

        boolean fileIsChecksum = ArtifactUtils.isChecksum(path);
        MultipleDigestInputStream mdis;
        try
        {
            mdis = new MultipleDigestInputStream(is);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new ArtifactStorageException(e.getMessage(), e);
        }

        // If this is not a checksum file, store the file.
        // If this is a checksum file, keep the hash in a String.
        ByteArrayOutputStream baos = null;
        if (fileIsChecksum)
        {
            baos = new ByteArrayOutputStream();
        }

        OutputStream os = null;
        try
        {
            os = artifactResolutionService.getOutputStream(storageId, repositoryId, path);

            int readLength;
            byte[] bytes = new byte[4096];

            while ((readLength = mdis.read(bytes, 0, bytes.length)) != -1)
            {
                if (fileIsChecksum)
                {
                    // Buffer the checksum for later validation
                    baos.write(bytes, 0, readLength);
                    baos.flush();
                }

                // Write the artifact
                os.write(bytes, 0, readLength);
                os.flush();
            }

            final String artifactPath = storageId + "/" + repositoryId + "/" + path;
            if (!fileIsChecksum && os != null)
            {
                addChecksumsToCacheManager(mdis, artifactPath);
                addArtifactToIndex(storageId, repositoryId, path);
            }
            else
            {
                validateUploadedChecksumAgainstCache(baos, artifactPath);
            }
        }
        catch (ArtifactResolutionException e)
        {
            throw new ArtifactStorageException(e);
        }
        catch (IOException e)
        {
            throw new ArtifactStorageException(e.getMessage(), e);
        }
        finally
        {
            ResourceCloser.close(os, logger);
        }
    }

    private void addArtifactToIndex(String storageId, String repositoryId, String path)
            throws IOException
    {
        if (ArtifactFileUtils.isArtifactFile(path))
        {
            final RepositoryIndexer indexer = repositoryIndexManager.getRepositoryIndex(storageId + ":" + repositoryId);
            if (indexer != null)
            {
                final Artifact artifact = ArtifactUtils.convertPathToArtifact(path);
                final Storage storage = getStorage(storageId);
                final File storageBasedir = new File(storage.getBasedir());
                final File artifactFile = new File(new File(storageBasedir, repositoryId), path).getCanonicalFile();

                if (!artifactFile.getName().endsWith(".pom"))
                {
                    indexer.addArtifactToIndex(repositoryId, artifactFile, artifact);
                }
            }
        }
    }

    @Override
    public InputStream resolve(String storageId,
                               String repositoryId,
                               String path)
            throws IOException,
                   ArtifactTransportException,
                   ProviderImplementationException
    {
        InputStream is;

        try
        {
            is = artifactResolutionService.getInputStream(storageId, repositoryId, path);
            return is;
        }
        catch (IOException | NoSuchAlgorithmException e)
        {
            // This is not necessarily an error. It could simply be a check
            // whether a resource exists, before uploading/updating it.
            logger.info("The requested path does not exist: /" + storageId + "/" + repositoryId + "/" + path);
        }

        return null;
    }

    private boolean performRepositoryAcceptanceValidation(String storageId,
                                                          String repositoryId,
                                                          String path)
            throws IOException, ProviderImplementationException
    {
        artifactOperationsValidator.validate(storageId, repositoryId, path);

        final Storage storage = getStorage(storageId);
        final Repository repository = storage.getRepository(repositoryId);

        if (!path.contains("/maven-metadata.") &&
            !ArtifactUtils.isMetadata(path) && !ArtifactUtils.isChecksum(path))
        {
            LayoutProvider layoutProvider = layoutProviderRegistry.getProvider(repository.getLayout());
            ArtifactCoordinates coordinates = (ArtifactCoordinates) layoutProvider.getArtifactCoordinates(path);

            try
            {
                final Set<VersionValidator> validators = versionValidatorService.getVersionValidators();
                for (VersionValidator validator : validators)
                {
                    validator.validate(repository, coordinates);
                }
            }
            catch (VersionValidationException e)
            {
                throw new ArtifactStorageException(e);
            }

            artifactOperationsValidator.checkAllowsRedeployment(repository, coordinates);
            artifactOperationsValidator.checkAllowsDeployment(repository);
        }

        return true;
    }

    @Override
    public void delete(String storageId,
                       String repositoryId,
                       String artifactPath,
                       boolean force)
            throws IOException
    {
        artifactOperationsValidator.validate(storageId, repositoryId, artifactPath);

        final Storage storage = getStorage(storageId);
        final Repository repository = storage.getRepository(repositoryId);

        artifactOperationsValidator.checkAllowsDeletion(repository);

        try
        {
            LayoutProvider layoutProvider = getLayoutProvider(repository, layoutProviderRegistry);
            layoutProvider.delete(storageId, repositoryId, artifactPath, force);

            final RepositoryIndexer indexer = repositoryIndexManager.getRepositoryIndex(storageId + ":" + repositoryId);
            if (indexer != null)
            {
                String extension = artifactPath.substring(artifactPath.lastIndexOf('.') + 1, artifactPath.length());

                final Artifact a = ArtifactUtils.convertPathToArtifact(artifactPath);

                /* TODO: This needs to be properly fixed:
                indexer.delete(Collections.singletonList(new ArtifactInfo(repositoryId,
                                                                          a.getGroupId(),
                                                                          a.getArtifactId(),
                                                                          a.getVersion(),
                                                                          a.getClassifier(),
                                                                          extension)));
                */
            }
        }
        catch (IOException | ProviderImplementationException e)
        {
            throw new ArtifactStorageException(e.getMessage(), e);
        }
    }

    @Override
    public boolean contains(String storageId, String repositoryId, String artifactPath)
            throws IOException
    {
        return false;
    }

    @Override
    public void copy(String srcStorageId,
                     String srcRepositoryId,
                     String path,
                     String destStorageId,
                     String destRepositoryId)
            throws IOException
    {
        artifactOperationsValidator.validate(srcStorageId, srcRepositoryId, path);

        final Storage srcStorage = getStorage(srcStorageId);
        final Repository srcRepository = srcStorage.getRepository(srcRepositoryId);

        final Storage destStorage = getStorage(destStorageId);
        final Repository destRepository = destStorage.getRepository(destRepositoryId);

        File srcFile = new File(srcRepository.getBasedir(), path);
        File destFile = new File(destRepository.getBasedir(), path);

        if (srcFile.isDirectory())
        {
            FileUtils.copyDirectoryToDirectory(srcFile, destFile.getParentFile());

            // TODO: SB-377: Sort out the logic for artifact directory paths
            // TODO: SB-377: addArtifactToIndex(destStorageId, destRepositoryId, path);
        }
        else
        {
            FileUtils.copyFile(srcFile, destFile);
            addArtifactToIndex(destStorageId, destRepositoryId, path);
        }
    }

    private void validateUploadedChecksumAgainstCache(ByteArrayOutputStream baos,
                                                      String artifactPath)
    {
        logger.debug("Received checksum: " + baos.toString());

        String artifactBasePath = artifactPath.substring(0, artifactPath.lastIndexOf('.'));
        String algorithm = null;

        final String checksumExtension = artifactPath.substring(artifactPath.lastIndexOf('.') + 1,
                                                                artifactPath.length());
        if (checksumExtension.equalsIgnoreCase(EncryptionAlgorithmsEnum.MD5.getAlgorithm()))
        {
            algorithm = EncryptionAlgorithmsEnum.MD5.getAlgorithm();
        }
        else if (checksumExtension.equals("sha1"))
        {
            algorithm = EncryptionAlgorithmsEnum.SHA1.getAlgorithm();
        }
        else
        {
            // TODO: Should we be doing something about this case?
            logger.warn("Unsupported checksum type: " + checksumExtension);
        }

        final boolean matchesCachedChecksum = matchesChecksum(baos, artifactBasePath, algorithm);
        if (!matchesCachedChecksum)
        {
            // TODO: Implement event triggering that handles checksums that don't match the uploaded file.
        }

        checksumCacheManager.removeArtifactChecksum(artifactBasePath, algorithm);
    }

    private boolean matchesChecksum(ByteArrayOutputStream baos,
                                    String artifactBasePath,
                                    String algorithm)
    {
        String checksum = baos.toString();
        String cachedChecksum = checksumCacheManager.getArtifactChecksum(artifactBasePath, algorithm);

        if (cachedChecksum.equals(checksum))
        {
            logger.debug("The received " + algorithm + " checksum matches cached one! " + checksum);
            return true;
        }
        else
        {
            logger.debug("The received " + algorithm + " does not match cached one! " + checksum + "/" + cachedChecksum);
            return false;
        }
    }

    private void addChecksumsToCacheManager(MultipleDigestInputStream mdis,
                                            String artifactPath)
    {
        MessageDigest md5Digest = mdis.getMessageDigest(EncryptionAlgorithmsEnum.MD5.getAlgorithm());
        MessageDigest sha1Digest = mdis.getMessageDigest(EncryptionAlgorithmsEnum.SHA1.getAlgorithm());

        String md5 = MessageDigestUtils.convertToHexadecimalString(md5Digest);
        String sha1 = MessageDigestUtils.convertToHexadecimalString(sha1Digest);

        checksumCacheManager.addArtifactChecksum(artifactPath, EncryptionAlgorithmsEnum.MD5.getAlgorithm(), md5);
        checksumCacheManager.addArtifactChecksum(artifactPath, EncryptionAlgorithmsEnum.SHA1.getAlgorithm(), sha1);
    }

    // TODO: This should have restricted access.
    @Override
    public void deleteTrash(String storageId, String repositoryId)
            throws IOException
    {
        artifactOperationsValidator.checkStorageExists(storageId);
        artifactOperationsValidator.checkRepositoryExists(storageId, repositoryId);

        try
        {
            final Storage storage = getStorage(storageId);
            final Repository repository = storage.getRepository(repositoryId);

            artifactOperationsValidator.checkAllowsDeletion(repository);

            LayoutProvider layoutProvider = getLayoutProvider(repository, layoutProviderRegistry);
            layoutProvider.deleteTrash(storageId, repositoryId);
        }
        catch (IOException | ProviderImplementationException e)
        {
            throw new ArtifactStorageException(e.getMessage(), e);
        }
    }

    // TODO: This should have restricted access.
    @Override
    public void deleteTrash()
            throws ArtifactStorageException
    {
        try
        {
            layoutProviderRegistry.deleteTrash();
        }
        catch (IOException e)
        {
            throw new ArtifactStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void undelete(String storageId, String repositoryId, String artifactPath)
            throws IOException
    {
        artifactOperationsValidator.validate(storageId, repositoryId, artifactPath);

        final Storage storage = getStorage(storageId);
        final Repository repository = storage.getRepository(repositoryId);

        artifactOperationsValidator.checkAllowsDeletion(repository);

        try
        {
            LayoutProvider layoutProvider = getLayoutProvider(repository, layoutProviderRegistry);
            layoutProvider.undelete(storageId, repositoryId, artifactPath);

            /*
            // TODO: This will need further fixing:
            final RepositoryIndexer indexer = repositoryIndexManager.getRepositoryIndex(storageId + ":" + repositoryId);
            if (indexer != null)
            {
                final Artifact artifact = ArtifactUtils.convertPathToArtifact(artifactPath);
                final File artifactFile = new File(repository.getBasedir(), artifactPath);

                indexer.addArtifactToIndex(repositoryId, artifactFile, artifact);
            }
            */
        }
        catch (IOException | ProviderImplementationException e)
        {
            throw new ArtifactStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void undeleteTrash(String storageId, String repositoryId)
            throws IOException,
                   ProviderImplementationException
    {
        artifactOperationsValidator.checkStorageExists(storageId);
        artifactOperationsValidator.checkRepositoryExists(storageId, repositoryId);

        try
        {
            final Storage storage = getStorage(storageId);
            final Repository repository = storage.getRepository(repositoryId);

            if (repository.isTrashEnabled())
            {
                LayoutProvider layoutProvider = getLayoutProvider(repository, layoutProviderRegistry);
                layoutProvider.undeleteTrash(storageId, repositoryId);
            }
        }
        catch (IOException e)
        {
            throw new ArtifactStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void undeleteTrash()
            throws IOException,
                   ProviderImplementationException
    {
        try
        {
            layoutProviderRegistry.undeleteTrash();
        }
        catch (IOException e)
        {
            throw new ArtifactStorageException(e.getMessage(), e);
        }
    }

    @Override
    public Storage getStorage(String storageId)
    {
        return getConfiguration().getStorages().get(storageId);
    }

    @Override
    public Configuration getConfiguration()
    {
        return configurationManager.getConfiguration();
    }

}
