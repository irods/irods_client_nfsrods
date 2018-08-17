package org.irods.jargon.nfs.vfs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IRODSProxyAdminAccountConfig
{
    // @formatter:off
    @JsonProperty("username") private String username_;
    @JsonProperty("password") private String password_;
    
    IRODSProxyAdminAccountConfig() {}
    // @formatter:on

    @JsonIgnore
    public String getUsername()
    {
        return username_;
    }

    @JsonIgnore
    public String getPassword()
    {
        return password_;
    }
}
