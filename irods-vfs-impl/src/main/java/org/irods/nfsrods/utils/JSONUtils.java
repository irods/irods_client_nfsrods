package org.irods.nfsrods.utils;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JSONUtils
{
    // @formatter:off
    private static final ObjectMapper mapper_ = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(Feature.IGNORE_UNKNOWN);

    private JSONUtils() {}
    // @formatter:on

    public static String toJSON(Object _object) throws JsonProcessingException
    {
        return mapper_.writeValueAsString(_object);
    }

    public static <T> T fromJSON(String _string, Class<T> _class)
        throws JsonParseException,
        JsonMappingException,
        IOException
    {
        return mapper_.readValue(_string, _class);
    }

    public static <T> T fromJSON(String _string, TypeReference<T> _typeRef)
        throws JsonParseException,
        JsonMappingException,
        IOException
    {
        return mapper_.readValue(_string, _typeRef);
    }

    public static <T> T fromJSON(File _file, Class<T> _class)
        throws JsonParseException,
        JsonMappingException,
        IOException
    {
        return mapper_.readValue(_file, _class);
    }

    public static <T> T fromJSON(File _file, TypeReference<T> _typeRef)
        throws JsonParseException,
        JsonMappingException,
        IOException
    {
        return mapper_.readValue(_file, _typeRef);
    }
}
