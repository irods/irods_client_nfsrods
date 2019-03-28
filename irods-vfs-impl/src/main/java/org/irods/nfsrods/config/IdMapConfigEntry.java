package org.irods.nfsrods.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IdMapConfigEntry
{
    // @formatter:off
    @JsonProperty("name") private String name_;
    @JsonProperty("uid")  private int uid_;
    @JsonProperty("gid")  private int gid_;
    
    private IdMapConfigEntry() {}
    // @formatter:on
    
    @JsonIgnore
    public String getName()
    {
        return name_;
    }

    @JsonIgnore
    public int getUserId()
    {
        return uid_;
    }

    @JsonIgnore
    public int getGroupId()
    {
        return gid_;
    }
}
