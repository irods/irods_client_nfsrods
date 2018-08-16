package org.irods.jargon.nfs.vfs;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JSONUtils
{
    private static final Logger log_ = LoggerFactory.getLogger(JSONUtils.class);

    private static final ObjectMapper mapper_ = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        .enable(Feature.IGNORE_UNKNOWN);

    private JSONUtils()
    {
    }

    String toJSON(Object _object) throws JsonProcessingException
    {
        return mapper_.writeValueAsString(_object);
    }

    <T> T fromJSON(String _string, Class<T> _class)
    {
        try
        {
            return mapper_.readValue(_string, _class);
        }
        catch (IOException e)
        {
            log_.error(e.getMessage());
        }

        return null;
    }

    <T> T fromJSON(String _string, TypeReference<T> _typeRef)
    {
        try
        {
            return mapper_.readValue(_string, _typeRef);
        }
        catch (IOException e)
        {
            log_.error(e.getMessage());
        }

        return null;
    }

    <T> T fromJSON(File _file, Class<T> _class)
    {
        try
        {
            return mapper_.readValue(_file, _class);
        }
        catch (IOException e)
        {
            log_.error(e.getMessage());
        }

        return null;
    }
}
