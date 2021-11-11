package org.irods.nfsrods.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerConfig
{
    @JsonProperty("nfs_server")   private NFSServerConfig nfsServerConfig_;
    @JsonProperty("irods_client") private IRODSClientConfig iRODSClientConfig_;
    
    // @formatter:off
    @JsonCreator
    ServerConfig(@JsonProperty("nfs_server")   NFSServerConfig _nfsServerConfig,
                 @JsonProperty("irods_client") IRODSClientConfig _iRODSClientConfig)
    {
        ConfigUtils.throwIfNull(_nfsServerConfig, "nfs_server");
        ConfigUtils.throwIfNull(_iRODSClientConfig, "irods_client");

        nfsServerConfig_ = _nfsServerConfig;
        iRODSClientConfig_ = _iRODSClientConfig;
    }
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
