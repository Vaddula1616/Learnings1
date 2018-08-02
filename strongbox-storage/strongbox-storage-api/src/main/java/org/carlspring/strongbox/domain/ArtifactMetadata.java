package org.carlspring.strongbox.domain;

import org.carlspring.strongbox.data.domain.GenericEntity;

import java.io.Serializable;

/**
 * @author carlspring
 */
public class ArtifactMetadata extends GenericEntity<ArtifactMetadata>
        implements Serializable
{

    @Override
    public Class<ArtifactMetadata> getEntityClass()
    {
        return ArtifactMetadata.class;
    }
    
}
