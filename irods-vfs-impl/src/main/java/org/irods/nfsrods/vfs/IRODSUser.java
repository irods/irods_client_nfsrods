package org.irods.nfsrods.vfs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.domain.User;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.nfsrods.config.IRODSProxyAdminAccountConfig;
import org.irods.nfsrods.config.IRODSServerConfig;
import org.irods.nfsrods.config.NFSServerConfig;
import org.irods.nfsrods.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IRODSUser
{
    private static final Logger log_ = LoggerFactory.getLogger(IRODSIdMap.class);

    private NonBlockingHashMap<Long, Path> inodeToPath_;
    private NonBlockingHashMap<Path, Long> pathToInode_;
    private AtomicLong fileID_;
    private IRODSAccessObjectFactory factory_;
    private IRODSAccount proxiedAcct_;
    private IRODSFile rootFile_;
    private int userID_;

    public IRODSUser(String _username, ServerConfig _config, IRODSAccessObjectFactory _factory)
    {
        inodeToPath_ = new NonBlockingHashMap<>();
        pathToInode_ = new NonBlockingHashMap<>();
        fileID_ = new AtomicLong(1); // Inode numbers start at 1

        NFSServerConfig nfsSvrConfig = _config.getNfsServerConfig();
        IRODSProxyAdminAccountConfig proxyConfig = _config.getIRODSProxyAdminAcctConfig();
        IRODSServerConfig rodsSvrConfig = _config.getIRODSServerConfig();

        String adminAcct = proxyConfig.getUsername();
        String adminPw = proxyConfig.getPassword();
        String zone = rodsSvrConfig.getZone();

        String rootPath = Paths.get(nfsSvrConfig.getIRODSMountPoint()).toString();
        log_.debug("IRODSUser :: iRODS mount point = {}", rootPath);
        log_.debug("IRODSUser :: Creating proxy for username [{}] ...", _username);

        try
        {
            proxiedAcct_ = IRODSAccount.instanceWithProxy(rodsSvrConfig.getHost(), rodsSvrConfig.getPort(), _username,
                                                          adminPw, rootPath, zone, rodsSvrConfig.getDefaultResource(),
                                                          adminAcct, zone);
            factory_ = _factory;
            rootFile_ = factory_.getIRODSFileFactory(proxiedAcct_).instanceIRODSFile(rootPath);

            User user = factory_.getUserAO(proxiedAcct_).findByName(_username);
            userID_ = Integer.parseInt(user.getId());

            establishRoot();
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
        }
    }

    public int getUserID()
    {
        return this.userID_;
    }

    public String getAbsolutePath()
    {
        return rootFile_.getAbsolutePath();
    }

    public NonBlockingHashMap<Long, Path> getInodeToPathMap()
    {
        return inodeToPath_;
    }

    public NonBlockingHashMap<Path, Long> getPathToInodeMap()
    {
        return pathToInode_;
    }

    public Long getAndIncrementFileID()
    {
        return fileID_.getAndIncrement();
    }

    public IRODSAccessObjectFactory getIRODSAccessObjectFactory()
    {
        return factory_;
    }

    public IRODSAccount getRootAccount()
    {
        return proxiedAcct_;
    }

    public IRODSFile getRoot()
    {
        return rootFile_;
    }

    private void establishRoot()
    {
//        try
//        {
            if (!rootFile_.exists())// || !rootFile_.canRead())
            {
                log_.error("Root file does not exist or it cannot be read");

                try
                {
                    throw new DataNotFoundException("Cannot establish root at [" + rootFile_ + "].");
                }
                catch (DataNotFoundException e)
                {
                    log_.error(e.getMessage());
                }
                
                return;
            }

            log_.debug("establishRoot :: Mapping root to [{}] ...", rootFile_);

            map(getAndIncrementFileID(), rootFile_.getAbsolutePath());

            log_.debug("establishRoot :: Mapping successful.");
//        }
//        finally
//        {
//            factory_.closeSessionAndEatExceptions();
//        }
    }

    public void map(Long inodeNumber, String irodsPath)
    {
        map(inodeNumber, Paths.get(irodsPath));
    }

    public void map(Long inodeNumber, Path path)
    {
        if (inodeToPath_.putIfAbsent(inodeNumber, path) != null)
        {
            // FIXME Add message.
            throw new IllegalStateException();
        }

        Long otherInodeNumber = pathToInode_.putIfAbsent(path, inodeNumber);

        if (otherInodeNumber != null)
        {
            // try rollback
            if (inodeToPath_.remove(inodeNumber) != path)
            {
                // FIXME Add message.
                throw new IllegalStateException("Cannot remove mapping, rollback failed.");
            }

            // FIXME Add message.
            throw new IllegalStateException("path ");
        }
    }

    public void unmap(Long inodeNumber, Path path)
    {
        Path removedPath = inodeToPath_.remove(inodeNumber);

        if (!path.equals(removedPath))
        {
            // FIXME Add message.
            throw new IllegalStateException();
        }

        if (pathToInode_.remove(path) != inodeNumber)
        {
            // FIXME Add message.
            throw new IllegalStateException();
        }
    }

    public void remap(Long inodeNumber, Path oldPath, Path newPath)
    {
        unmap(inodeNumber, oldPath);
        map(inodeNumber, newPath);
    }

    @Override
    public String toString()
    {
        return "IRODSUser{rootAccount=" + proxiedAcct_ + ", userID=" + userID_ + '}';
    }

}
