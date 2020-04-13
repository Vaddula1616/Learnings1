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
    
    public String merge(E entity)
    {
        return merge(this::g, entity);
    }

    @Override
    public String merge(Supplier<EntityTraversalSource> g,
                        E entity)
    {
        UnfoldEntityTraversal<Vertex, Vertex> unfoldTraversal = adapter().unfold(entity);
        Vertex resultVertex = start(g).saveV(unfoldTraversal.entityLabel(), entity.getUuid(),
                                             unfoldTraversal)
                                      .next();
        session.clear();

        return resultVertex.<String>property("uuid").value();
    }

    @Override
    public <R extends E> R save(R entity)
    {
        return save(this::g, entity);
    }

    @Override
    public EntityTraversal<Vertex, Vertex> start(Supplier<EntityTraversalSource> g)
    {
        return g.get().V();
    }

}
