package org.carlspring.strongbox.storage.repository;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author korest
 */
@XmlRootElement(name = "http-connection-pool")
@XmlAccessorType(XmlAccessType.FIELD)
public class HttpConnectionPool
{
    @XmlAttribute(name = "allocated-connections")
    private int allocatedConnections;

    public int getAllocatedConnections()
    {
        return allocatedConnections;
    }

    public void setAllocatedConnections(int allocatedConnections)
    {
        this.allocatedConnections = allocatedConnections;
    }
}
