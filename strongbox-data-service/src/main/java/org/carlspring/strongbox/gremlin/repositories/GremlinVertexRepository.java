package org.carlspring.strongbox.gremlin.repositories;

import java.util.function.Supplier;

import javax.transaction.Transactional;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.carlspring.strongbox.data.domain.DomainObject;
import org.carlspring.strongbox.gremlin.adapters.UnfoldEntityTraversal;
import org.carlspring.strongbox.gremlin.dsl.EntityTraversal;
import org.carlspring.strongbox.gremlin.dsl.EntityTraversalSource;

@Transactional
public abstract class GremlinVertexRepository<E extends DomainObject> extends GremlinRepository<Vertex, E>
{

    @Override
    public <R extends E> R save(R entity)
    {
        UnfoldEntityTraversal<Vertex, Vertex> unfoldTraversal = adapter().unfold(entity);
        Vertex resultVertex = start(this::g).saveV(unfoldTraversal.entityLabel(), entity.getUuid(),
                                                   unfoldTraversal)
                                            .next();
        session.clear();
        
        return (R) findById(resultVertex.<String>property("uuid").value()).get();
    }

    @Override
    public EntityTraversal<Vertex, Vertex> start(Supplier<EntityTraversalSource> g)
    {
        return g.get().V();
    }

}
