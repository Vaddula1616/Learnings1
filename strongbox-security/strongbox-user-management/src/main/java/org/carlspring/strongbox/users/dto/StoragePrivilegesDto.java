package org.carlspring.strongbox.users.dto;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.carlspring.strongbox.db.schema.Properties.STORAGE_ID;

/**
 * @author Alex Oreshkevich
 * @author Pablo Tirado
 */
public class StoragePrivilegesDto
        implements Serializable, StoragePrivileges
{

    private Set<RepositoryPrivilegesDto> repositoryPrivileges = new LinkedHashSet<>();

    @JsonProperty(value = STORAGE_ID)
    private String storageId;

    public StoragePrivilegesDto()
    {
    }

    @JsonCreator
    public StoragePrivilegesDto(@JsonProperty(value = STORAGE_ID, required = true) String storageId)
    {
        this.storageId = storageId;
    }

    public Set<RepositoryPrivilegesDto> getRepositoryPrivileges()
    {
        return repositoryPrivileges;
    }

    public String getStorageId()
    {
        return storageId;
    }

    public void setStorageId(final String storageId)
    {
        this.storageId = storageId;
    }

    public Optional<RepositoryPrivilegesDto> getRepositoryPrivileges(final String repositoryId)
    {
        return repositoryPrivileges.stream().filter(r -> r.getRepositoryId().equals(repositoryId)).findFirst();
    }
}
