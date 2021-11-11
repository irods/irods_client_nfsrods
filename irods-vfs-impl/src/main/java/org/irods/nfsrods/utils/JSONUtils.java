package org.irods.nfsrods.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

public class JSONUtils
{
    // @formatter:off
    private static final ObjectMapper mapper_ = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(Feature.IGNORE_UNKNOWN)
        .setSerializerFactory(BeanSerializerFactory.instance.withSerializerModifier(new BeanSerializerConfigModifier()));

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
    
    @SuppressWarnings("serial")
    static class MaskedPasswordWriter extends BeanPropertyWriter
    {
        MaskedPasswordWriter(BeanPropertyWriter _writer)
        {
            super(_writer);
        }
        
        @Override
        public void serializeAsField(Object _bean, JsonGenerator _jgen, SerializerProvider _provider)
            throws IOException
        {
            _jgen.writeStringField("password", "*************");
        }
    }
    
    static class BeanSerializerConfigModifier extends BeanSerializerModifier
    {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig _config,
                                                         BeanDescription _beanDesc,
                                                         List<BeanPropertyWriter> _beanProperties)
        {
            for (int i = 0; i < _beanProperties.size(); ++i)
            {
                BeanPropertyWriter writer = _beanProperties.get(i);

                if ("password".equals(writer.getName()))
                {
                    _beanProperties.set(i, new MaskedPasswordWriter(writer));
                }
            }

            return _beanProperties;
        }
    }
}
