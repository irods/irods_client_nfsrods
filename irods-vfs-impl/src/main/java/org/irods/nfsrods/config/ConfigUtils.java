package org.irods.nfsrods.config;

class ConfigUtils
{
    static void throwIfNull(Object _object, String _message)
    {
        if (null == _object)
        {
            throw new IllegalArgumentException("Missing server configuration option: " + _message);
        }
    }
    
    static <T> T withDefault(T _targetValue, T _default)
    {
        return (null != _targetValue) ? _targetValue : _default;
    }
}
