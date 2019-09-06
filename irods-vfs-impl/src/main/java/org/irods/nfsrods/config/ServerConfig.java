package org.irods.nfsrods.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerConfig
{
    // @formatter:off
    @JsonProperty("nfs_server")   private NFSServerConfig nfsServerConfig_;
    @JsonProperty("irods_client") private IRODSClientConfig iRODSClientConfig_;
    
    ServerConfig() {}
    // @formatter:on

    @JsonIgnore
    public NFSServerConfig getNfsServerConfig()
    {
        return nfsServerConfig_;
    }

    @JsonIgnore
    public IRODSClientConfig getIRODSClientConfig()
    {
        return iRODSClientConfig_;
    }
}
