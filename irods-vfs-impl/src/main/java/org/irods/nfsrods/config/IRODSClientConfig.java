package org.irods.nfsrods.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IRODSClientConfig
{
    // @formatter:off
    @JsonProperty("host")                                        private String host_;
    @JsonProperty("port")                                        private int port_;
    @JsonProperty("zone")                                        private String zone_;
    @JsonProperty("default_resource")                            private String defResc_;
    @JsonProperty("ssl_negotiation_policy")                      private String sslNegPolicy_;
    @JsonProperty("connection_timeout_in_seconds")               private int connTimeout_;
    @JsonProperty("hard_links_rule_engine_plugin_instance_name") private String hardLinksPluginInstName_;
    @JsonProperty("proxy_admin_account")                         private IRODSProxyAdminAccountConfig proxyAdminAcctConfig_;
    
    @JsonCreator
    IRODSClientConfig(@JsonProperty("host")                                        String _host,
                      @JsonProperty("port")                                        Integer _port,
                      @JsonProperty("zone")                                        String _zone,
                      @JsonProperty("default_resource")                            String _defaultResource,
                      @JsonProperty("ssl_negotiation_policy")                      String _sslNegotiationPolicy,
                      @JsonProperty("connection_timeout_in_seconds")               Integer _connTimeout,
                      @JsonProperty("hard_links_rule_engine_plugin_instance_name") String _hardLinksPluginInstName,
                      @JsonProperty("proxy_admin_account")                         IRODSProxyAdminAccountConfig _proxyAdminAcctConfig)
    {
        ConfigUtils.throwIfNull(_host, "host");
        ConfigUtils.throwIfNull(_port, "port");
        ConfigUtils.throwIfNull(_zone, "zone");
        ConfigUtils.throwIfNull(_defaultResource, "default_resource");
        ConfigUtils.throwIfNull(_sslNegotiationPolicy, "ssl_negotiation_policy");
        ConfigUtils.throwIfNull(_hardLinksPluginInstName, "hard_links_rule_engine_plugin_instance_name");
        ConfigUtils.throwIfNull(_proxyAdminAcctConfig, "proxy_admin_account");

        host_ = _host;
        port_ = _port;
        zone_ = _zone;
        defResc_ = _defaultResource;
        sslNegPolicy_ = _sslNegotiationPolicy;
        hardLinksPluginInstName_ = _hardLinksPluginInstName;
        proxyAdminAcctConfig_ = _proxyAdminAcctConfig;
        
        setConnectionTimeout(_connTimeout);
    }
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
    public String getSslNegotiationPolicy()
    {
        return sslNegPolicy_;
    }

    @JsonIgnore
    public int getConnectionTimeout()
    {
        return connTimeout_;
    }

    @JsonIgnore
    public String getHardLinksRuleEnginePluginInstanceName()
    {
        return hardLinksPluginInstName_;
    }

    @JsonIgnore
    public IRODSProxyAdminAccountConfig getIRODSProxyAdminAcctConfig()
    {
        return proxyAdminAcctConfig_;
    }
    
    private void setConnectionTimeout(Integer _timeout)
    {
        if (null == _timeout)
        {
            connTimeout_ = 600;
        }
        else if (_timeout < 0)
        {
            throw new IllegalArgumentException("Invalid connection timeout");
        }
        else
        {
            connTimeout_ = _timeout;
        }
    }
}
