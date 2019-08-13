package org.carlspring.strongbox.testing;

import org.carlspring.strongbox.artifact.locator.ArtifactDirectoryLocator;
import org.carlspring.strongbox.booters.PropertiesBooter;
import org.carlspring.strongbox.event.artifact.ArtifactEventListenerRegistry;
import org.carlspring.strongbox.locator.handlers.GenerateMavenMetadataOperation;
import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.providers.io.RootRepositoryPath;
import org.carlspring.strongbox.providers.search.MavenIndexerSearchProvider;
import org.carlspring.strongbox.providers.search.SearchException;
import org.carlspring.strongbox.repository.RepositoryManagementStrategyException;
import org.carlspring.strongbox.services.ArtifactResolutionService;
import org.carlspring.strongbox.services.ArtifactSearchService;
import org.carlspring.strongbox.services.RepositoryManagementService;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.indexing.IndexTypeEnum;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexManager;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexer;
import org.carlspring.strongbox.storage.metadata.MavenMetadataManager;
import org.carlspring.strongbox.storage.repository.*;
import org.carlspring.strongbox.storage.repository.remote.MutableRemoteRepository;
import org.carlspring.strongbox.storage.routing.MutableRoutingRule;
import org.carlspring.strongbox.storage.routing.MutableRoutingRuleRepository;
import org.carlspring.strongbox.storage.routing.RoutingRuleTypeEnum;
import org.carlspring.strongbox.storage.search.SearchRequest;
import org.carlspring.strongbox.yaml.configuration.repository.MavenRepositoryConfigurationDto;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.io.ByteStreams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.Bits;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.junit.jupiter.api.Assumptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author carlspring
 * @author Pablo Tirado
 */
public abstract class TestCaseWithMavenArtifactGenerationAndIndexing
        extends MavenTestCaseWithArtifactGeneration
{
    private static final Logger logger = LoggerFactory.getLogger(TestCaseWithMavenArtifactGenerationAndIndexing.class);

    protected static final String STORAGE0 = "storage0";
    
    @Inject
    protected Optional<RepositoryIndexManager> repositoryIndexManager;

    @Inject
    private PropertiesBooter propertiesBooter;

    @Inject
    protected RepositoryManagementService repositoryManagementService;

    @Inject
    protected ArtifactSearchService artifactSearchService;

    @Inject
    protected MavenMetadataManager mavenMetadataManager;

    @Inject
    protected ArtifactResolutionService artifactResolutionService;

    @Inject
    protected ArtifactEventListenerRegistry artifactEventListenerRegistry;

    @Inject
    private MavenRepositoryFactory mavenRepositoryFactory;


    @Override
    public void createProxyRepository(String storageId,
                                      String repositoryId,
                                      String remoteRepositoryUrl)
            throws IOException,
                   JAXBException,
                   RepositoryManagementStrategyException
    {
        MavenRepositoryConfigurationDto repositoryConfiguration = new MavenRepositoryConfigurationDto();
        repositoryConfiguration.setIndexingEnabled(true);

        MutableRemoteRepository remoteRepository = new MutableRemoteRepository();
        remoteRepository.setUrl(remoteRepositoryUrl);

        RepositoryDto repository = mavenRepositoryFactory.createRepository(repositoryId);
        repository.setRemoteRepository(remoteRepository);
        repository.setRepositoryConfiguration(repositoryConfiguration);
        repository.setType(RepositoryTypeEnum.PROXY.getType());

        createRepository(storageId, repository);
    }

    public void createAndAddRoutingRule(String groupStorageId,
                                        String groupRepositoryId,
                                        List<MutableRoutingRuleRepository> repositories,
                                        String rulePattern,
                                        RoutingRuleTypeEnum type) throws IOException
    {
        MutableRoutingRule routingRule = MutableRoutingRule.create(groupStorageId, groupRepositoryId,
                                                                   repositories, rulePattern, type);
        configurationManagementService.addRoutingRule(routingRule);
    }

    public void dumpIndex(String storageId,
                          String repositoryId)
            throws IOException
    {
        dumpIndex(storageId, repositoryId, IndexTypeEnum.LOCAL.getType());
    }

    public void dumpIndex(String storageId,
                          String repositoryId,
                          String indexType)
            throws IOException
    {
        if (!repositoryIndexManager.isPresent())
        {
            return;
        }

        String contextId = storageId + ":" + repositoryId + ":" + indexType;
        RepositoryIndexer repositoryIndexer = repositoryIndexManager.get().getRepositoryIndexer(contextId);
        if (repositoryIndexer == null)
        {
            logger.debug("Unable to find index for contextId " + contextId);
            return;
        }

        IndexingContext indexingContext = repositoryIndexer.getIndexingContext();

        final IndexSearcher searcher = indexingContext.acquireIndexSearcher();
        try
        {
            logger.debug("Dumping index for " + storageId + ":" + repositoryId + ":" + indexType + "...");

            final IndexReader ir = searcher.getIndexReader();
            Bits liveDocs = MultiFields.getLiveDocs(ir);
            for (int i = 0; i < ir.maxDoc(); i++)
            {
                if (liveDocs == null || liveDocs.get(i))
                {
                    final Document doc = ir.document(i);
                    final ArtifactInfo ai = IndexUtils.constructArtifactInfo(doc, indexingContext);
                    if (ai != null)
                    {
                        System.out.println("\t" + ai.toString());
                    }
                }
            }

            logger.debug("Index dump completed.");
        }
        finally
        {
            indexingContext.releaseIndexSearcher(searcher);
        }
    }

    protected void generateMavenMetadata(String storageId,
                                         String repositoryId)
            throws IOException
    {
        Storage storage = getConfiguration().getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);
        
        RepositoryPath repositoryPath = repositoryPathResolver.resolve(repository);

        ArtifactDirectoryLocator locator = new ArtifactDirectoryLocator();
        locator.setBasedir(repositoryPath);
        locator.setOperation(new GenerateMavenMetadataOperation(mavenMetadataManager, artifactEventListenerRegistry));
        locator.locateArtifactDirectories();
    }

    protected Path getVaultDirectoryPath()
    {
        String base = FilenameUtils.normalize(propertiesBooter.getVaultDirectory());
        if (StringUtils.isBlank(base))
        {
            throw new IllegalStateException("propertiesBooter.getVaultDirectory() resolves to '" + base +
                                            "' which is illegal base path here.");
        }
        return Paths.get(base);
    }

    protected void deleteDirectoryRelativeToVaultDirectory(String dirPathToDelete)
            throws Exception
    {
        Path basePath = getVaultDirectoryPath();
        Path fullDirPathToDelete = basePath.resolve(dirPathToDelete);
        FileUtils.deleteDirectory(fullDirPathToDelete.toFile());
    }

    protected void assertStreamNotNull(final String storageId,
                                       final String repositoryId,
                                       final String path)
            throws Exception
    {
        RepositoryPath repositoryPath = artifactResolutionService.resolvePath(storageId,
                                                                              repositoryId,
                                                                              path);

        try (final InputStream is = artifactResolutionService.getInputStream(repositoryPath))
        {
            assertNotNull(is, "Failed to resolve " + path + "!");

            if (RepositoryFiles.isMetadata(repositoryPath))
            {
                System.out.println(ByteStreams.toByteArray(is));
            }
            else
            {
                while (is.read(new byte[1024]) != -1);
            }
        }
    }

    public void assertIndexContainsArtifact(String storageId,
                                            String repositoryId,
                                            String query)
            throws SearchException
    {
        Assumptions.assumeTrue(repositoryIndexManager.isPresent());

        boolean isContained = indexContainsArtifact(storageId, repositoryId, query);

        assertTrue(isContained);
    }

    public boolean indexContainsArtifact(String storageId,
                                         String repositoryId,
                                         String query)
            throws SearchException
    {
        SearchRequest request = new SearchRequest(storageId,
                                                  repositoryId,
                                                  query,
                                                  MavenIndexerSearchProvider.ALIAS);

        return artifactSearchService.contains(request);
    }

    protected void closeIndexersForRepository(String storageId,
                                              String repositoryId)
            throws IOException
    {
        if (repositoryIndexManager.isPresent())
        {
            repositoryIndexManager.get().closeIndexersForRepository(storageId, repositoryId);
        }
    }

    public void closeIndexer(String contextId)
            throws IOException
    {
        if (repositoryIndexManager.isPresent())
        {
            repositoryIndexManager.get().closeIndexer(contextId);
        }
    }

    public RepositoryManagementService getRepositoryManagementService()
    {
        return repositoryManagementService;
    }

    @Override
    public void removeRepositories(Set<RepositoryDto> repositoriesToClean)
        throws IOException,
        JAXBException
    {
        for (RepositoryDto mutableRepository : repositoriesToClean)
        {
            RootRepositoryPath repositoryPath = repositoryPathResolver.resolve(new RepositoryData(mutableRepository));
            closeIndexersForRepository(mutableRepository.getStorage().getId(), mutableRepository.getId());

            Files.delete(repositoryPath);
        }
        
        super.removeRepositories(repositoriesToClean);
    }
    
}
