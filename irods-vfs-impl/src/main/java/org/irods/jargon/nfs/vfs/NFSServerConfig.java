package org.irods.jargon.nfs.vfs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NFSServerConfig
{
    // @formatter:off
    @JsonProperty("port")                       private int port_;
    @JsonProperty("kerberos_service_principal") private String krb5SvcPrincipal_;
    @JsonProperty("kerberos_keytab")            private String krb5Keytab_;
    @JsonProperty("irods_mount_point")          private String iRODSMntPoint_;
    
    NFSServerConfig() {}
    // @formatter:on

    @JsonIgnore
    public int getPort()
    {
        return port_;
    }

    @JsonIgnore
    public String getKerberosServicePrincipal()
    {
        return krb5SvcPrincipal_;
    }

    @JsonIgnore
    public String getKerberosKeytab()
    {
        return krb5Keytab_;
    }

    @JsonIgnore
    public String getiRODSMountPoint()
    {
        return iRODSMntPoint_;
    }
}
