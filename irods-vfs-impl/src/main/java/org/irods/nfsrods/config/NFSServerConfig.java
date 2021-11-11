package org.irods.nfsrods.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NFSServerConfig
{
    @JsonProperty("port")                                                      private int port_;
    @JsonProperty("irods_mount_point")                                         private String iRODSMntPoint_;
    @JsonProperty("user_information_refresh_time_in_milliseconds")             private int userInfoRefreshTimeInMillis_;
    @JsonProperty("file_information_refresh_time_in_milliseconds")             private int fileInfoRefreshTimeInMillis_;
    @JsonProperty("user_access_refresh_time_in_milliseconds")                  private int userAccessRefreshTimeInMillis_;
    @JsonProperty("object_type_refresh_time_in_milliseconds")                  private int objectTypeRefreshTimeInMillis_;
    @JsonProperty("user_permissions_refresh_time_in_milliseconds")             private int userPermsRefreshTimeInMillis_;
    @JsonProperty("user_type_refresh_time_in_milliseconds")                    private int userTypeRefreshTimeInMillis_;
    @JsonProperty("list_operation_query_results_refresh_time_in_milliseconds") private int listOpQueryResultsRefreshTimeInMillis_;
    @JsonProperty("allow_overwrite_of_existing_files")                         private boolean allowOverwriteOfExistingFiles_;
    @JsonProperty("using_oracle_database")                                     private boolean usingOracleDB_;
    
    // @formatter:off
    @JsonCreator
    NFSServerConfig(@JsonProperty("port")                                                      Integer _port,
                    @JsonProperty("irods_mount_point")                                         String _iRODSMountPoint,
                    @JsonProperty("user_information_refresh_time_in_milliseconds")             Integer _userInfoRefreshTimeInMillis,
                    @JsonProperty("file_information_refresh_time_in_milliseconds")             Integer _fileInfoRefreshTimeInMillis,
                    @JsonProperty("user_access_refresh_time_in_milliseconds")                  Integer _userAccessRefreshTimeInMillis,
                    @JsonProperty("object_type_refresh_time_in_milliseconds")                  Integer _objectTypeRefreshTimeInMillis,
                    @JsonProperty("user_permissions_refresh_time_in_milliseconds")             Integer _userPermsRefreshTimeInMillis,
                    @JsonProperty("user_type_refresh_time_in_milliseconds")                    Integer _userTypeRefreshTimeInMillis,
                    @JsonProperty("list_operation_query_results_refresh_time_in_milliseconds") Integer _listOpQueryResultsRefreshTimeInMillis,
                    @JsonProperty("allow_overwrite_of_existing_files")                         Boolean _allowOverwriteOfExistingFiles,
                    @JsonProperty("using_oracle_database")                                     Boolean _usingOracleDB)
    {
        ConfigUtils.throwIfNull(_port, "port");
        ConfigUtils.throwIfNull(_iRODSMountPoint, "irods_mount_point");

        port_ = _port;
        iRODSMntPoint_ = _iRODSMountPoint;
        userInfoRefreshTimeInMillis_ = ConfigUtils.withDefault(_userInfoRefreshTimeInMillis, 3600000);
        fileInfoRefreshTimeInMillis_ = ConfigUtils.withDefault(_fileInfoRefreshTimeInMillis, 1000);
        userAccessRefreshTimeInMillis_ = ConfigUtils.withDefault(_userAccessRefreshTimeInMillis, 1000);
        objectTypeRefreshTimeInMillis_ = ConfigUtils.withDefault(_objectTypeRefreshTimeInMillis, 300000);
        userPermsRefreshTimeInMillis_ = ConfigUtils.withDefault(_userPermsRefreshTimeInMillis, 300000);
        userTypeRefreshTimeInMillis_ = ConfigUtils.withDefault(_userTypeRefreshTimeInMillis, 300000);
        listOpQueryResultsRefreshTimeInMillis_ = ConfigUtils.withDefault(_listOpQueryResultsRefreshTimeInMillis, 30000);
        allowOverwriteOfExistingFiles_ = ConfigUtils.withDefault(_allowOverwriteOfExistingFiles, true);
        usingOracleDB_ = ConfigUtils.withDefault(_usingOracleDB, false);
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
    public int getObjectTypeRefreshTimeInMilliseconds()
    {
        return objectTypeRefreshTimeInMillis_;
    }

    @JsonIgnore
    public int getUserPermissionsRefreshTimeInMilliseconds()
    {
        return userPermsRefreshTimeInMillis_;
    }

    @JsonIgnore
    public int getUserTypeRefreshTimeInMilliseconds()
    {
        return userTypeRefreshTimeInMillis_;
    }

    @JsonIgnore
    public int getListOperationQueryResultsRefreshTimeInMilliseconds()
    {
        return listOpQueryResultsRefreshTimeInMillis_;
    }

    @JsonIgnore
    public boolean allowOverwriteOfExistingFiles()
    {
        return allowOverwriteOfExistingFiles_;
    }
    
    @JsonIgnore
    public boolean isUsingOracleDatabase()
    {
        return usingOracleDB_;
    }
}
