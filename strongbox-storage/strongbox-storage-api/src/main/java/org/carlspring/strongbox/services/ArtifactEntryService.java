package org.carlspring.strongbox.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.carlspring.strongbox.artifact.coordinates.ArtifactCoordinates;
import org.carlspring.strongbox.data.service.CrudService;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD service for managing {@link ArtifactEntry} entities.
 *
 * @author Alex Oreshkevich
 */
@Transactional
public interface ArtifactEntryService
        extends CrudService<ArtifactEntry, String>
{

    /**
     * Returns list of artifacts that matches search query defined as {@link ArtifactCoordinates} fields.
     * By default all fields are optional and combined using logical AND operator.
     * If all coordinates aren't present this query will delegate request to {@link #findAll()}
     * (because in that case every ArtifactEntry will match the query).
     *
     * @param coordinates search query defined as a set of coordinates (id ,version, groupID etc.)
     * @return list of artifacts or empty list if nothing was found
     */
    List<ArtifactEntry> findByCoordinates(String storageId,
                                          String repositoryId,
                                          ArtifactCoordinates coordinates);
    
    List<ArtifactEntry> findByCoordinates(String storageId,
                                          String repositoryId,
                                          Map<String, String> coordinates);
    
    List<ArtifactEntry> findByCoordinates(String storageId,
                                          String repositoryId,
                                          Map<String, String> coordinates,
                                          int skip,
                                          int limit,
                                          String orderBy,
                                          boolean strict);
    
    Long countByCoordinates(String storageId,
                            String repositoryId,
                            Map<String, String> coordinates,
                            boolean strict);
    
    boolean exists(String storageId, String repositoryId, String path);
    
    Optional<ArtifactEntry> findOne(String storageId, String repositoryId, String path);
}
