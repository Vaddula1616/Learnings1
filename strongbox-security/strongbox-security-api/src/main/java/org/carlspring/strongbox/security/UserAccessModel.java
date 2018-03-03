package org.carlspring.strongbox.security;

import org.carlspring.strongbox.data.domain.GenericEntity;

import javax.persistence.CascadeType;
import javax.persistence.OneToOne;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;

/**
 * @author Alex Oreshkevich
 */
@XmlRootElement(name = "features")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserAccessModel
        extends GenericEntity
{

    @XmlElement
    @OneToOne(cascade = CascadeType.ALL)
    private UserStorages storages;

    public UserAccessModel()
    {
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAccessModel userAccessModel = (UserAccessModel) o;
        return Objects.equal(storages, userAccessModel.storages);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(storages);
    }

    public UserStorages getStorages()
    {
        return storages;
    }

    public void setStorages(UserStorages storages)
    {
        this.storages = storages;
    }


    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("UserAccessModel{");
        sb.append("storages=")
          .append(storages);
        sb.append('}');
        return sb.toString();
    }
}
