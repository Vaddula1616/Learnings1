package org.carlspring.strongbox.providers.layout;

import org.carlspring.strongbox.artifact.coordinates.NugetArtifactCoordinates;
import org.carlspring.strongbox.providers.header.HeaderMappingRegistry;
import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.repository.NugetRepositoryFeatures;
import org.carlspring.strongbox.repository.NugetRepositoryManagementStrategy;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
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
@DependsOn("layoutProviderRegistry")
public class NugetLayoutProvider
        extends AbstractLayoutProvider<NugetArtifactCoordinates>
{
    private static final Logger logger = LoggerFactory.getLogger(NugetLayoutProvider.class);

    public static final String ALIAS = "NuGet";

    public static final String USER_AGENT_PREFIX = "NuGet";

    @Inject
    private HeaderMappingRegistry headerMappingRegistry;

    @Inject
    private NugetRepositoryManagementStrategy nugetRepositoryManagementStrategy;

    @Inject
    private NugetRepositoryFeatures nugetRepositoryFeatures;


    @PostConstruct
    public void register()
    {
        layoutProviderRegistry.addProvider(ALIAS, this);
        headerMappingRegistry.register(ALIAS, USER_AGENT_PREFIX);

        logger.info("Registered layout provider '" + getClass().getCanonicalName() + "' with alias '" + ALIAS + "'.");
    }

    protected NugetArtifactCoordinates getArtifactCoordinates(RepositoryPath path) throws IOException
    {
        return NugetArtifactCoordinates.parse(RepositoryFiles.relativizePath(path));
    }

    protected boolean isArtifactMetadata(RepositoryPath path)
    {
        return path.getFileName().toString().endsWith(".nuspec");
    }

    @Override
    public void deleteMetadata(RepositoryPath repositoryPath)
    {
    }

    protected String toBase64(byte[] digest)
    {
        byte[] encoded = Base64.getEncoder()
                               .encode(digest);
        return new String(encoded, StandardCharsets.UTF_8);
    }

    @Override
    public Set<String> getDefaultArtifactCoordinateValidators()
    {
        return nugetRepositoryFeatures.getDefaultArtifactCoordinateValidators();
    }

    @Override
    protected Set<String> getDigestAlgorithmSet()
    {
        return Stream.of(MessageDigestAlgorithms.SHA_512)
                     .collect(Collectors.toSet());
    }

    @Override
    public NugetRepositoryManagementStrategy getRepositoryManagementStrategy()
    {
        return nugetRepositoryManagementStrategy;
    }

}
