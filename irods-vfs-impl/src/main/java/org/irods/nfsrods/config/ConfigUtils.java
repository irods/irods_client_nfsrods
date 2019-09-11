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
}
