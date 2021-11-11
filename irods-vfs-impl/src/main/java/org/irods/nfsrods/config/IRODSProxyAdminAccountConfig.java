package org.irods.nfsrods.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IRODSProxyAdminAccountConfig
{
    @JsonProperty("username") private String username_;
    @JsonProperty("password") private String password_;
    
    // @formatter:off
    @JsonCreator
    IRODSProxyAdminAccountConfig(@JsonProperty("username") String _username,
                                 @JsonProperty("password") String _password)
    {
        ConfigUtils.throwIfNull(_username, "username");
        ConfigUtils.throwIfNull(_password, "password");

        username_ = _username;
        password_ = _password;
    }
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
