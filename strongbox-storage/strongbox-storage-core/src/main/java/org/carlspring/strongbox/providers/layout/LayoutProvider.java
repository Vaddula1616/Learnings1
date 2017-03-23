package org.carlspring.strongbox.providers.layout;

import org.carlspring.strongbox.artifact.coordinates.ArtifactCoordinates;
import org.carlspring.strongbox.client.ArtifactTransportException;
import org.carlspring.strongbox.io.ArtifactInputStream;
import org.carlspring.strongbox.io.ArtifactOutputStream;
import org.carlspring.strongbox.providers.ProviderImplementationException;
import org.carlspring.strongbox.repository.RepositoryFeatures;
import org.carlspring.strongbox.repository.RepositoryManagementStrategy;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.repository.UnknownRepositoryTypeException;

import java.io.FilenameFilter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author carlspring
 */
public interface LayoutProvider<T extends ArtifactCoordinates>
{

    void register();

    String getAlias();

    T getArtifactCoordinates(String path);

    ArtifactInputStream getInputStream(String storageId, String repositoryId, String path)
            throws IOException, NoSuchAlgorithmException, ArtifactTransportException;

    ArtifactOutputStream getOutputStream(String storageId,
                                         String repositoryId,
                                         String path)
            throws IOException, NoSuchAlgorithmException;

    boolean containsArtifact(Repository repository, ArtifactCoordinates coordinates)
            throws IOException;

    boolean contains(String storageId, String repositoryId, String path)
            throws IOException;

    boolean containsPath(Repository repository, String path)
            throws IOException;

    void copy(String srcStorageId,
              String srcRepositoryId,
              String destStorageId,
              String destRepositoryId,
              String path)
            throws IOException;

    void move(String srcStorageId,
              String srcRepositoryId,
              String destStorageId,
              String destRepositoryId,
              String path)
            throws IOException;

    void delete(String storageId, String repositoryId, String path, boolean force)
            throws IOException;

    void deleteMetadata(String storageId, String repositoryId, String metadataPath)
            throws IOException;

    void deleteTrash(String storageId, String repositoryId)
            throws IOException;

    void deleteTrash()
            throws IOException;

    void undelete(String storageId, String repositoryId, String path)
            throws IOException;

    void undeleteTrash(String storageId, String repositoryId)
            throws IOException;

    void undeleteTrash()
            throws IOException;

    boolean isExistChecksum(Repository repository,
                            String path);

    Set<String> getDigestAlgorithmSet();

    void rebuildMetadata(String storageId,
                         String repositoryId,
                         String basePath)
            throws IOException,
                   NoSuchAlgorithmException,
                   XmlPullParserException;

    void rebuildIndexes(String storageId,
                        String repositoryId,
                        String basePath,
                        boolean forceRegeneration)
            throws IOException;

    void regenerateChecksums(Repository repository,
                             List<String> versionDirectories,
                             boolean forceRegeneration)
            throws IOException,
                   NoSuchAlgorithmException,
                   ProviderImplementationException,
                   UnknownRepositoryTypeException,
                   ArtifactTransportException;

    FilenameFilter getMetadataFilenameFilter();

    RepositoryFeatures getRepositoryFeatures();

    RepositoryManagementStrategy getRepositoryManagementStrategy();

}
