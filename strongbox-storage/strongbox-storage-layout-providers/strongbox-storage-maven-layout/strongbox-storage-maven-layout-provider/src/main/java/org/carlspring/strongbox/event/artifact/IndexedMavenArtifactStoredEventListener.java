package org.carlspring.strongbox.event.artifact;

import java.io.IOException;

import javax.inject.Inject;

import org.carlspring.strongbox.config.MavenIndexerEnabledCondition;
import org.carlspring.strongbox.event.AsyncEventListener;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.providers.layout.Maven2LayoutProvider;
import org.carlspring.strongbox.repository.group.index.MavenIndexGroupRepositoryComponent;
import org.carlspring.strongbox.storage.repository.Repository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * @author Przemyslaw Fusik
 */
@Component
@Conditional(MavenIndexerEnabledCondition.class)
public class IndexedMavenArtifactStoredEventListener
        extends BaseMavenArtifactEventListener
{

    @Inject
    private MavenIndexGroupRepositoryComponent mavenIndexGroupRepositoryComponent;

    @AsyncEventListener
    public void handle(final ArtifactEvent<RepositoryPath> event)
    {
        final Repository repository = getRepository(event);

        if (!Maven2LayoutProvider.ALIAS.equals(repository.getLayout()))
        {
            return;
        }

        if (event.getType() != ArtifactEventTypeEnum.EVENT_ARTIFACT_FILE_STORED.getType())
        {
            return;
        }

        try
        {
            mavenIndexGroupRepositoryComponent.updateGroupsContaining(event.getPath());
        }
        catch (final IOException e)
        {
            logger.error("Unable to update parent group repositories indexes of file " + event.getPath(), e);
        }
    }
}
