package org.irods.nfsrods.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IRODSClientConfig
{
    // @formatter:off
    @JsonProperty("host")                private String host_;
    @JsonProperty("port")                private int port_;
    @JsonProperty("zone")                private String zone_;
    @JsonProperty("default_resource")    private String defResc_;
    @JsonProperty("proxy_admin_account") private IRODSProxyAdminAccountConfig iRODSProxyAdminAcctConfig_;
    
    IRODSClientConfig() {}
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

    @JsonIgnore
    public IRODSProxyAdminAccountConfig getIRODSProxyAdminAcctConfig()
    {
        return iRODSProxyAdminAcctConfig_;
    }
}
