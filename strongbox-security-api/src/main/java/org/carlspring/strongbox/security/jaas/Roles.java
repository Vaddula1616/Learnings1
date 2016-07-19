package org.carlspring.strongbox.security.jaas;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author mtodorov
 */
@XmlRootElement(name = "roles")
@XmlAccessorType(XmlAccessType.FIELD)
public class Roles
{

    @XmlElement(name = "role")
    private Set<Role> roles = new LinkedHashSet<>();


    public Roles()
    {
    }

    public Roles(Set<Role> roles)
    {
        this.roles = roles;
    }

    public Set<Role> getRoles()
    {
        return roles;
    }

    public void setRoles(Set<Role> roles)
    {
        this.roles = roles;
    }

}
