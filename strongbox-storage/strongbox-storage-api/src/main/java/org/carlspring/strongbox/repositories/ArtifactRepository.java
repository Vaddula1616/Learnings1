package org.carlspring.strongbox.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.carlspring.strongbox.db.schema.Edges;
import org.carlspring.strongbox.db.schema.Vertices;
import org.carlspring.strongbox.domain.Artifact;
import org.carlspring.strongbox.gremlin.adapters.ArtifactHierarchyAdapter;
import org.carlspring.strongbox.gremlin.adapters.EntityTraversalUtils;
import org.carlspring.strongbox.gremlin.dsl.EntityTraversal;
import org.carlspring.strongbox.gremlin.repositories.GremlinVertexRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public class ArtifactRepository extends GremlinVertexRepository<Artifact> implements ArtifactEntityQueries
{

    @Inject
    ArtifactHierarchyAdapter artifactAdapter;
    @Inject
    ArtifactEntityQueries queries;

    @Override
    protected ArtifactHierarchyAdapter adapter()
    {
        return artifactAdapter;
    }

    public List<Artifact> findByPathLike(String storageId,
                                         String repositoryId,
                                         String path)
    {
        return EntityTraversalUtils.reduceHierarchy(queries.findByPathLike(storageId, repositoryId, path));
    }

    public Page<Artifact> findMatching(Integer lastAccessedTimeInDays,
                                       Long minSizeInBytes,
                                       Pageable pagination)
    {
        LocalDateTime date = Optional.ofNullable(lastAccessedTimeInDays)
                                     .map(v -> LocalDateTime.now().minusDays(lastAccessedTimeInDays))
                                     .orElse(null);
        return findMatching(date, minSizeInBytes, pagination);
    }

    public Page<Artifact> findMatching(LocalDateTime lastAccessedDate,
                                       Long minSizeInBytes,
                                       Pageable pagination)
    {
        Page<Artifact> result = queries.findMatching(lastAccessedDate, minSizeInBytes, pagination);

        return new PageImpl<>(EntityTraversalUtils.reduceHierarchy(result.toList()), pagination, result.getTotalElements());
    }

    public Boolean artifactEntityExists(String storageId,
                                        String repositoryId,
                                        String path)
    {
        return Optional.ofNullable(queries.artifactEntityExists(storageId, repositoryId, path)).orElse(Boolean.FALSE);
    }

    public Boolean artifactExists(String storageId,
                                            String repositoryId,
                                            String path)
    {
        EntityTraversal<Vertex, Vertex> t = g().V()
                                               .hasLabel(Vertices.GENERIC_ARTIFACT_COORDINATES)
                                               .has("uuid", path)
                                               .inE(Edges.ARTIFACT_HAS_ARTIFACT_COORDINATES)
                                               .otherV()
                                               .hasLabel(Vertices.ARTIFACT)
                                               .has("storageId", storageId)
                                               .has("repositoryId", repositoryId)
                                               .optional(__.inE(Edges.REMOTE_ARTIFACT_INHERIT_ARTIFACT).otherV())
                                               .choose(__.hasLabel(Vertices.REMOTE_ARTIFACT),
                                                       __.has("cached", true),
                                                       __.identity());
        return t.hasNext();
    }

    public Artifact findOneArtifact(String storageId,
                                    String repositoryId,
                                    String path)
    {
        EntityTraversal<Vertex, Artifact> t = g().V()
                                                 .hasLabel(Vertices.GENERIC_ARTIFACT_COORDINATES)
                                                 .has("uuid", path)
                                                 .inE(Edges.ARTIFACT_HAS_ARTIFACT_COORDINATES)
                                                 .otherV()
                                                 .hasLabel(Vertices.ARTIFACT)
                                                 .has("storageId", storageId)
                                                 .has("repositoryId", repositoryId)
                                                 .optional(__.inE(Edges.REMOTE_ARTIFACT_INHERIT_ARTIFACT).otherV())
                                                 .map(artifactAdapter.fold());
        if (!t.hasNext()) {
            return null;
        }
            
        return t.next();
    }

}

@Repository
interface ArtifactEntityQueries extends org.springframework.data.repository.Repository<Artifact, String>
{

    @Query("MATCH (genericCoordinates:GenericArtifactCoordinates)<-[r1]-(artifact:Artifact) " +
           "WHERE genericCoordinates.uuid STARTS WITH $path and artifact.storageId=$storageId and artifact.repositoryId=$repositoryId " +
           "WITH artifact, r1, genericCoordinates " +
           "OPTIONAL MATCH (artifact)-[r4]->(tag:ArtifactTag) " +
           "WITH artifact, r1, genericCoordinates, r4, tag " +
           "MATCH (genericCoordinates)<-[r2]-(layoutCoordinates) " +
           "WITH artifact, r1, genericCoordinates, r2, layoutCoordinates, r4, tag " +
           "RETURN artifact, r1, genericCoordinates, r2, layoutCoordinates, r4, tag")
    List<Artifact> findByPathLike(@Param("storageId") String storageId,
                                  @Param("repositoryId") String repositoryId,
                                  @Param("path") String path);

    @Query(value = "MATCH (genericCoordinates:GenericArtifactCoordinates)<-[r1]-(artifact:Artifact) " +
                   "WHERE artifact.lastUsed <= coalesce($lastAccessedDate, artifact.lastUsed) and artifact.sizeInBytes >=  coalesce($minSizeInBytes, artifact.sizeInBytes) " +
                   "WITH artifact, r1, genericCoordinates " +
                   "OPTIONAL MATCH (artifact)-[r4]->(tag:ArtifactTag) " +
                   "WITH artifact, r1, genericCoordinates, r4, tag " +
                   "MATCH (genericCoordinates)<-[r2]-(layoutCoordinates) " +
                   "WITH artifact, r1, genericCoordinates, r2, layoutCoordinates, r4, tag " +
                   "RETURN artifact, r1, genericCoordinates, r2, layoutCoordinates, r4, tag",
           countQuery = "MATCH (artifact:Artifact) " +
                        "WHERE artifact.lastUsed <= coalesce($lastAccessedDate, artifact.lastUsed) and artifact.sizeInBytes >=  coalesce($minSizeInBytes, artifact.sizeInBytes) " +
                        "RETURN count(artifact)")
    Page<Artifact> findMatching(@Param("lastAccessedDate") LocalDateTime lastAccessedDate,
                                @Param("minSizeInBytes") Long minSizeInBytes,
                                Pageable page);

    @Query("MATCH (genericCoordinates:GenericArtifactCoordinates)<-[r1]-(artifact:Artifact) " +
           "WHERE genericCoordinates.uuid=$path and artifact.storageId=$storageId and artifact.repositoryId=$repositoryId " +
           "RETURN EXISTS(artifact.uuid)")
    Boolean artifactEntityExists(@Param("storageId") String storageId,
                                 @Param("repositoryId") String repositoryId,
                                 @Param("path") String path);

}
