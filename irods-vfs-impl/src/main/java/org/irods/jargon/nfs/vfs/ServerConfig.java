package org.irods.jargon.nfs.vfs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerConfig
{
    // @formatter:off
    @JsonProperty("nfs_server")                private NFSServerConfig nfsServerConfig_;
    @JsonProperty("irods_server")              private IRODSServerConfig iRODSServerConfig_;
    @JsonProperty("irods_proxy_admin_account") private IRODSProxyAdminAccountConfig iRODSProxyAdminAcctConfig_;
    
    ServerConfig() {}
    // @formatter:on

    @JsonIgnore
    public NFSServerConfig getNfsServerConfig()
    {
        return nfsServerConfig_;
    }

    @JsonIgnore
    public IRODSServerConfig getIRODSServerConfig()
    {
        return iRODSServerConfig_;
    }

    @JsonIgnore
    public IRODSProxyAdminAccountConfig getIRODSProxyAdminAcctConfig()
    {
        return iRODSProxyAdminAcctConfig_;
    }
}
