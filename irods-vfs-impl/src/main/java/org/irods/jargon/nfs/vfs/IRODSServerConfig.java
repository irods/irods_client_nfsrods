package org.irods.jargon.nfs.vfs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IRODSServerConfig
{
    // @formatter:off
    @JsonProperty("host")             private String host_;
    @JsonProperty("port")             private int port_;
    @JsonProperty("zone")             private String zone_;
    @JsonProperty("default_resource") private String defResc_;
    
    IRODSServerConfig() {}
    // @formatter:on

    @JsonIgnore
    public String getHost()
    {
        return host_;
    }

    @JsonIgnore
    public int getPort()
    {
        return port_;
    }

    @JsonIgnore
    public String getZone()
    {
        return zone_;
    }

    @JsonIgnore
    public String getDefaultResource()
    {
        return defResc_;
    }
}
