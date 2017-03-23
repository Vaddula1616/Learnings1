package org.carlspring.strongbox.providers.layout;

import org.carlspring.strongbox.artifact.coordinates.NugetHierarchicalArtifactCoordinates;
import org.carlspring.strongbox.client.ArtifactTransportException;
import org.carlspring.strongbox.io.ArtifactOutputStream;
import org.carlspring.strongbox.io.RepositoryPath;
import org.carlspring.strongbox.io.filters.NuspecFilenameFilter;
import org.carlspring.strongbox.providers.ProviderImplementationException;
import org.carlspring.strongbox.repository.NugetRepositoryFeatures;
import org.carlspring.strongbox.repository.NugetRepositoryManagementStrategy;
import org.carlspring.strongbox.repository.RepositoryFeatures;
import org.carlspring.strongbox.repository.RepositoryManagementStrategy;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Layout provider for Nuget package repository.<br>
 * It provides hierarchical directory layout like follows: <br>
 * &lt;packageID&gt;<br>
 * └─&lt;version&gt;<br>
 * &emsp;├─&lt;packageID&gt;.&lt;version&gt;.nupkg<br>
 * &emsp;├─&lt;packageID&gt;.&lt;version&gt;.nupkg.sha512<br>
 * &emsp;└─&lt;packageID&gt;.nuspec<br>
 * 
 * 
 * @author Sergey Bespalov
 *
 */
@Component
public class NugetHierarchicalLayoutProvider extends AbstractLayoutProvider<NugetHierarchicalArtifactCoordinates,
                                                                            NugetRepositoryFeatures,
                                                                            NugetRepositoryManagementStrategy>
{
    private static final Logger logger = LoggerFactory.getLogger(NugetHierarchicalLayoutProvider.class);

    public static final String ALIAS = "Nuget Hierarchical";

    @Inject
    private NugetRepositoryFeatures nugetRepositoryFeatures;

    @Inject
    private NugetRepositoryManagementStrategy nugetRepositoryManagementStrategy;


    @Override
    @PostConstruct
    public void register()
    {
        layoutProviderRegistry.addProvider(ALIAS, this);

        logger.info("Registered layout provider '" + getClass().getCanonicalName() + "' with alias '" + ALIAS + "'.");
    }

    @Override
    public String getAlias()
    {
        return ALIAS;
    }

    @Override
    public NugetHierarchicalArtifactCoordinates getArtifactCoordinates(String path)
    {
        return new NugetHierarchicalArtifactCoordinates(path);
    }

    @Override
    protected boolean isMetadata(String path)
    {
        return path.endsWith("nuspec");
    }

    @Override
    public void deleteMetadata(String storageId,
                               String repositoryId,
                               String metadataPath)
        throws IOException
    {

    }

    @Override
    protected void doDeletePath(RepositoryPath repositoryPath,
                                boolean force,
                                boolean deleteChecksum)
        throws IOException
    {
        RepositoryPath sha512Path = repositoryPath.resolveSibling(repositoryPath.getFileName() + ".sha512");
        super.doDeletePath(repositoryPath, force, deleteChecksum);
        if (deleteChecksum)
        {
            super.doDeletePath(sha512Path, force, deleteChecksum);
        }
    }

    @Override
    protected ArtifactOutputStream decorateStream(String path,
                                                  OutputStream os,
                                                  NugetHierarchicalArtifactCoordinates c)
            throws NoSuchAlgorithmException
    {
        ArtifactOutputStream result = super.decorateStream(path, os, c);
        result.setDigestStringifier(this::toBase64);
        return result;
    }

    private String toBase64(byte[] digest)
    {
        byte[] encoded = Base64.getEncoder()
                               .encode(digest);
        return new String(encoded);
    }

    @Override
    public Set<String> getDigestAlgorithmSet()
    {
        return Stream.of(MessageDigestAlgorithms.SHA_512)
                     .collect(Collectors.toSet());
    }

    @Override
    public void rebuildMetadata(String storageId,
                                String repositoryId,
                                String basePath)
            throws IOException,
                   NoSuchAlgorithmException,
                   XmlPullParserException
    {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void rebuildIndexes(String storageId,
                               String repositoryId,
                               String basePath,
                               boolean forceRegeneration)
            throws IOException
    {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void regenerateChecksums(Repository repository,
                                    List<String> versionDirectories,
                                    boolean forceRegeneration)
            throws IOException
    {

        if (!versionDirectories.isEmpty())
            {
                RepositoryPath basePath = resolve(repository, versionDirectories.get(0)).getParent();

                logger.debug("Artifact checksum generation triggered for " + basePath + " in '" +
                             repository.getStorage()
                                       .getId() + ":" +
                             repository.getId() + "'" + " [policy: " + repository.getPolicy() + "].");

                versionDirectories.forEach(path ->
                                           {
                                               try
                                               {
                                                   storeChecksum(repository, resolve(repository, path),
                                                                 forceRegeneration);
                                               }
                                               catch (IOException |
                                                              NoSuchAlgorithmException |
                                                              ArtifactTransportException |
                                                              ProviderImplementationException e)
                                               {
                                                   logger.error(e.getMessage(), e);
                                               }

                                               logger.debug("Generated Nuget checksum for " + path + ".");
                                           });
            }
        else
        {
            logger.error("Artifact checksum generation failed.");
        }
    }

    @Override
    public FilenameFilter getMetadataFilenameFilter()
    {
        return new NuspecFilenameFilter();
    }

    @Override
    public NugetRepositoryFeatures getRepositoryFeatures()
    {
        return nugetRepositoryFeatures;
    }

    @Override
    public NugetRepositoryManagementStrategy getRepositoryManagementStrategy()
    {
        return nugetRepositoryManagementStrategy;
    }

}