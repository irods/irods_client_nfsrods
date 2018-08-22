package org.irods.jargon.nfs.vfs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.domain.User;
import org.irods.jargon.core.pub.io.IRODSFile;
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

    public IRODSUser(String _username, ServerConfig _config)
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
            IRODSFileSystem fs = IRODSFileSystem.instance();
            factory_ = IRODSAccessObjectFactoryImpl.instance(fs.getIrodsSession());
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
        log_.debug("establish root at: {}", rootFile_);

        try
        {
            if (rootFile_.exists() && rootFile_.canRead())
            {
                log_.debug("root is valid");
            }
            else
            {
                try
                {
                    throw new DataNotFoundException("cannot establish root at:" + rootFile_);
                }
                catch (DataNotFoundException ex)
                {
                    // FIXME Replace line below.
                    // Logger.getLogger(IRODSUser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            log_.debug("mapping root...");

            map(getAndIncrementFileID(), rootFile_.getAbsolutePath());
        }
        finally
        {
            factory_.closeSessionAndEatExceptions();
        }
    }

    public void map(Long inodeNumber, String irodsPath)
    {
        map(inodeNumber, Paths.get(irodsPath));
    }

    public void map(Long inodeNumber, Path path)
    {
        if (inodeToPath_.putIfAbsent(inodeNumber, path) != null)
        {
            throw new IllegalStateException();
        }

        Long otherInodeNumber = pathToInode_.putIfAbsent(path, inodeNumber);

        if (otherInodeNumber != null)
        {
            // try rollback
            if (inodeToPath_.remove(inodeNumber) != path)
            {
                throw new IllegalStateException("cant map, rollback failed");
            }

            throw new IllegalStateException("path ");
        }
    }

    public void unmap(Long inodeNumber, Path path)
    {
        Path removedPath = inodeToPath_.remove(inodeNumber);

        if (!path.equals(removedPath))
        {
            throw new IllegalStateException();
        }

        if (pathToInode_.remove(path) != inodeNumber)
        {
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
