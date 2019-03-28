package org.irods.nfsrods.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NFSServerConfig
{
    // @formatter:off
    @JsonProperty("port")              private int port_;
    @JsonProperty("irods_mount_point") private String iRODSMntPoint_;
    
    NFSServerConfig() {}
    // @formatter:on

    @JsonIgnore
    public int getPort()
    {
        return port_;
    }

    @JsonIgnore
    public String getIRODSMountPoint()
    {
        return iRODSMntPoint_;
    }
}
