package org.irods.nfsrods.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NFSServerConfig
{
    @JsonProperty("port")                                          private int port_;
    @JsonProperty("irods_mount_point")                             private String iRODSMntPoint_;
    @JsonProperty("user_information_refresh_time_in_milliseconds") private int userInfoRefreshTimeInMillis_;
    @JsonProperty("file_information_refresh_time_in_milliseconds") private int fileInfoRefreshTimeInMillis_;
    @JsonProperty("user_access_refresh_time_in_milliseconds")      private int userAccessRefreshTimeInMillis_;
    @JsonProperty("allow_overwrite_of_existing_files")             private boolean allowOverwriteOfExistingFiles_;
    
    // @formatter:off
    @JsonCreator
    NFSServerConfig(@JsonProperty("port")                                          Integer _port,
                    @JsonProperty("irods_mount_point")                             String _iRODSMountPoint,
                    @JsonProperty("user_information_refresh_time_in_milliseconds") Integer _userInfoRefreshTimeInMillis,
                    @JsonProperty("file_information_refresh_time_in_milliseconds") Integer _fileInfoRefreshTimeInMillis,
                    @JsonProperty("user_access_refresh_time_in_milliseconds")      Integer _userAccessRefreshTimeInMillis,
                    @JsonProperty("allow_overwrite_of_existing_files")             Boolean _allowOverwriteOfExistingFiles)
    {
        ConfigUtils.throwIfNull(_port, "port");
        ConfigUtils.throwIfNull(_iRODSMountPoint, "irods_mount_point");
        ConfigUtils.throwIfNull(_userInfoRefreshTimeInMillis, "user_information_refresh_time_in_milliseconds");
        ConfigUtils.throwIfNull(_fileInfoRefreshTimeInMillis, "file_information_refresh_time_in_milliseconds");
        ConfigUtils.throwIfNull(_userAccessRefreshTimeInMillis, "user_access_refresh_time_in_milliseconds");

        port_ = _port;
        iRODSMntPoint_ = _iRODSMountPoint;
        userInfoRefreshTimeInMillis_ = _userInfoRefreshTimeInMillis;
        fileInfoRefreshTimeInMillis_ = _fileInfoRefreshTimeInMillis;
        userAccessRefreshTimeInMillis_ = _userAccessRefreshTimeInMillis;
        allowOverwriteOfExistingFiles_ = ConfigUtils.withDefault(_allowOverwriteOfExistingFiles, true);
    }
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
    
    @JsonIgnore
    public int getUserInfoRefreshTimeInMilliseconds()
    {
        return userInfoRefreshTimeInMillis_;
    }
    
    @JsonIgnore
    public int getFileInfoRefreshTimeInMilliseconds()
    {
        return fileInfoRefreshTimeInMillis_;
    }

    @JsonIgnore
    public int getUserAccessRefreshTimeInMilliseconds()
    {
        return userAccessRefreshTimeInMillis_;
    }

    @JsonIgnore
    public boolean allowOverwriteOfExistingFiles()
    {
        return allowOverwriteOfExistingFiles_;
    }
}
