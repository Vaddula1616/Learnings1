package org.carlspring.strongbox.users.domain;

import org.carlspring.strongbox.data.domain.GenericEntity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;

/**
 * An application user
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User
        extends GenericEntity
{

    private String username;

    private String password;

    private boolean enabled;

    private Set<String> roles;

    private String securityTokenKey;

    @Embedded
    private AccessModel accessModel;

    public User()
    {
        roles = new HashSet<>();
    }

    public User(String id,
                String username,
                String password,
                boolean enabled,
                Set<String> roles)
    {
        this.objectId = id;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.roles = roles;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return enabled == user.enabled &&
               Objects.equal(username, user.username) &&
               Objects.equal(password, user.password) &&
               Objects.equal(roles, user.roles) &&
               Objects.equal(securityTokenKey, user.securityTokenKey) &&
               Objects.equal(accessModel, user.accessModel);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(username, password, enabled, roles, securityTokenKey, accessModel);
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(final String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(final String password)
    {
        this.password = password;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(final boolean enabled)
    {
        this.enabled = enabled;
    }

    public Set<String> getRoles()
    {
        return roles;
    }

    public void setRoles(final Set<String> roles)
    {
        this.roles = roles;
    }

    public String getSecurityTokenKey()
    {
        return securityTokenKey;
    }

    public void setSecurityTokenKey(String securityTokenKey)
    {
        this.securityTokenKey = securityTokenKey;
    }

    public AccessModel getAccessModel()
    {
        return accessModel;
    }

    public void setAccessModel(AccessModel accessModel)
    {
        this.accessModel = accessModel;
    }


    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("User{");
        sb.append("username='")
          .append(username)
          .append('\'');
        sb.append(", enabled=")
          .append(enabled);
        sb.append(", roles=")
          .append(roles);
        sb.append(", securityTokenKey='")
          .append(securityTokenKey)
          .append('\'');
        sb.append(", accessModel=")
          .append(accessModel);
        sb.append('}');
        return sb.toString();
    }
}
