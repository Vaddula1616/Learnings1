package org.carlspring.strongbox.locator.handlers;

import org.carlspring.maven.commons.io.filters.PomFilenameFilter;
import org.carlspring.strongbox.artifact.locator.handlers.AbstractArtifactLocationHandler;
import org.carlspring.strongbox.providers.ProviderImplementationException;
import org.carlspring.strongbox.storage.metadata.MavenMetadataManager;
import org.carlspring.strongbox.storage.metadata.VersionCollectionRequest;
import org.carlspring.strongbox.storage.metadata.VersionCollector;
import org.carlspring.strongbox.storage.repository.UnknownRepositoryTypeException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mtodorov
 */
public class GenerateMavenMetadataOperation
        extends AbstractArtifactLocationHandler
{

    private static final Logger logger = LoggerFactory.getLogger(GenerateMavenMetadataOperation.class);

    private MavenMetadataManager mavenMetadataManager;

    private String previousPath;


    public GenerateMavenMetadataOperation()
    {
    }

    public GenerateMavenMetadataOperation(MavenMetadataManager mavenMetadataManager)
    {
        this.mavenMetadataManager = mavenMetadataManager;
    }

    public void execute(Path path)
    {
        File f = path.toAbsolutePath().toFile();

        String[] list = f.list(new PomFilenameFilter());
        List<String> filePaths = list != null ? Arrays.asList(list) : new ArrayList<>();

        String parentPath = path.getParent().toAbsolutePath().toString();

        if (!filePaths.isEmpty())
        {
            // Don't enter visited paths (i.e. version directories such as 1.2, 1.3, 1.4...)
            if (!getVisitedRootPaths().isEmpty() && getVisitedRootPaths().containsKey(parentPath))
            {
                List<File> visitedVersionPaths = getVisitedRootPaths().get(parentPath);

                if (visitedVersionPaths.contains(f))
                {
                    return;
                }
            }

            if (logger.isDebugEnabled())
            {
                // We're using System.out.println() here for clarity and due to the length of the lines
                System.out.println(parentPath);
            }

            // The current directory is out of the tree
            if (previousPath != null && !parentPath.startsWith(previousPath))
            {
                getVisitedRootPaths().remove(previousPath);
                previousPath = parentPath;
            }

            if (previousPath == null)
            {
                previousPath = parentPath;
            }

            List<File> versionDirectories = getVersionDirectories(Paths.get(parentPath));
            if (versionDirectories != null)
            {
                getVisitedRootPaths().put(parentPath, versionDirectories);

                VersionCollector versionCollector = new VersionCollector();
                VersionCollectionRequest request = versionCollector.collectVersions(path.getParent().toAbsolutePath());

                if (logger.isDebugEnabled())
                {
                    for (File directory : versionDirectories)
                    {
                        // We're using System.out.println() here for clarity and due to the length of the lines
                        System.out.println(" " + directory.getAbsolutePath());
                    }
                }

                String artifactPath = parentPath.substring(getRepository().getBasedir().length() + 1, parentPath.length());

                try
                {
                    mavenMetadataManager.generateMetadata(getRepository(), artifactPath, request);
                }
                catch (IOException |
                       XmlPullParserException |
                       NoSuchAlgorithmException |
                       ProviderImplementationException |
                       UnknownRepositoryTypeException e)
                {
                    logger.error("Failed to generate metadata for " + artifactPath, e);
                }
            }
        }
    }

    public MavenMetadataManager getMavenMetadataManager()
    {
        return mavenMetadataManager;
    }

    public void setMavenMetadataManager(MavenMetadataManager mavenMetadataManager)
    {
        this.mavenMetadataManager = mavenMetadataManager;
    }

}
