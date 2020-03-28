package org.carlspring.strongbox.data.domain;

import org.neo4j.ogm.annotation.Id;

/**
 * @author sbespalov
 *
 */
public class DomainEntity implements DomainObject
{

    private Long id;
    @Id
    private String uuid;

    public Long getNativeId()
    {
        return id;
    }

    public void setNativeId(Long id)
    {
        this.id = id;
    }

    @Override
    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        if (this.uuid != null && !this.uuid.equals(uuid))
        {
            throw new IllegalStateException(String.format("Can't change the uuid, [%s]->[%s].", this.uuid, uuid));
        }

        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        else if (!(obj instanceof DomainEntity))
        {
            return false;
        }

        DomainEntity that = (DomainEntity) obj;
        if (this.uuid == null)
        {
            return false;
        }

        return this.uuid.equals(that.uuid);
    }

    @Override
    public int hashCode()
    {
        if (uuid == null)
        {
            return super.hashCode();
        }
        return uuid.hashCode();
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder(this.getClass().getSimpleName());
        sb.append("{");
        sb.append(", uuid='").append(uuid).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
