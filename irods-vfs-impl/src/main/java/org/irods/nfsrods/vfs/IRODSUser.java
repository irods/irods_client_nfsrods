package org.irods.nfsrods.vfs;

import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.nfsrods.config.IRODSClientConfig;
import org.irods.nfsrods.config.IRODSProxyAdminAccountConfig;
import org.irods.nfsrods.config.NFSServerConfig;
import org.irods.nfsrods.config.ServerConfig;

public class IRODSUser
{
    private static final Logger log_ = LogManager.getLogger(IRODSIdMapper.class);

    private IRODSAccount proxiedAcct_;
    private int userID_;
    private int groupID_;

    public IRODSUser(String _username, int _uid, int _gid, ServerConfig _config, IRODSAccessObjectFactory _factory)
    {
        NFSServerConfig nfsSvrConfig = _config.getNfsServerConfig();
        IRODSClientConfig rodsSvrConfig = _config.getIRODSClientConfig();
        IRODSProxyAdminAccountConfig proxyConfig = rodsSvrConfig.getIRODSProxyAdminAcctConfig();

        String adminAcct = proxyConfig.getUsername();
        String adminPw = proxyConfig.getPassword();
        String zone = rodsSvrConfig.getZone();

        String rootPath = Paths.get(nfsSvrConfig.getIRODSMountPoint()).toString();
        log_.debug("IRODSUser - iRODS mount point = {}", rootPath);
        log_.debug("IRODSUser - Creating proxy for username [{}] ...", _username);

        userID_ = _uid;
        groupID_ = _gid;

        // @formatter:off
        proxiedAcct_ = IRODSAccount.instanceWithProxy(rodsSvrConfig.getHost(), rodsSvrConfig.getPort(), _username,
                                                      adminPw, rootPath, zone, rodsSvrConfig.getDefaultResource(),
                                                      adminAcct, zone);
        // @formatter:on
    }

    public int getUserID()
    {
        return this.userID_;
    }

    public int getGroupID()
    {
        return this.groupID_;
    }

    public IRODSAccount getAccount()
    {
        return proxiedAcct_;
    }
}
