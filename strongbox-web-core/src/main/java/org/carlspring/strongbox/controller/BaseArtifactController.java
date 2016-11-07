package org.carlspring.strongbox.controller;

import org.carlspring.strongbox.services.ArtifactManagementService;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.metadata.MavenMetadataManager;
import org.carlspring.strongbox.storage.repository.Repository;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

public abstract class BaseArtifactController
        extends BaseController
{

    @Autowired
    protected ArtifactManagementService artifactManagementService;

    @Autowired
    protected MavenMetadataManager mavenMetadataManager;

    // ----------------------------------------------------------------------------------------------------------------
    // Common-purpose methods

    protected synchronized <T> T read(String json,
                                      Class<T> type)
    {
        try
        {
            return objectMapper.readValue(json, type);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected synchronized ResponseEntity toResponse(Object arg)
    {
        try
        {
            return ResponseEntity.ok(objectMapper.writeValueAsString(arg));
        }
        catch (Exception e)
        {
            return toError(e);
        }
    }



    public Storage getStorage(String storageId)
    {
        return configurationManager.getConfiguration().getStorage(storageId);
    }

    public Repository getRepository(String storageId,
                                    String repositoryId)
    {
        return getStorage(storageId).getRepository(repositoryId);
    }

    public ArtifactManagementService getArtifactManagementService()
    {
        return artifactManagementService;
    }

    public void setArtifactManagementService(ArtifactManagementService artifactManagementService)
    {
        this.artifactManagementService = artifactManagementService;
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
