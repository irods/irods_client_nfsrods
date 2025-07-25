package org.irods.nfsrods.vfs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.CollectionAndDataObjectListAndSearchAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.domain.ObjStat;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.nfsrods.config.IRODSClientConfig;
import org.irods.nfsrods.config.IRODSProxyAdminAccountConfig;
import org.irods.nfsrods.config.NFSServerConfig;
import org.irods.nfsrods.config.ServerConfig;

class InodeToPathMapper
{
    private static final Logger log_ = LogManager.getLogger(IRODSIdMapper.class);

    private Map<Long, Path> inodeToPath_;
    private Map<Path, Long> pathToInode_;
    private Set<Long> availableInodeNumbers_;
    private ReadWriteLock lock_;

    public InodeToPathMapper(ServerConfig _config, IRODSAccessObjectFactory _factory) throws JargonException
    {
        inodeToPath_ = new HashMap<>();
        pathToInode_ = new HashMap<>();
        availableInodeNumbers_ = new HashSet<>();
        lock_ = new ReentrantReadWriteLock();
        
        NFSServerConfig nfsSvrConfig = _config.getNfsServerConfig();
        IRODSClientConfig rodsSvrConfig = _config.getIRODSClientConfig();
        IRODSProxyAdminAccountConfig proxyConfig = rodsSvrConfig.getIRODSProxyAdminAcctConfig();

        String adminAcct = proxyConfig.getUsername();
        String adminPw = proxyConfig.getPassword();
        String zone = rodsSvrConfig.getZone();

        String rootPath = Paths.get(nfsSvrConfig.getIRODSMountPoint()).toString();
        log_.debug("InodeToPathMapper - iRODS mount point = {}", rootPath);

        IRODSAccount acct = IRODSAccount.instanceWithProxy(rodsSvrConfig.getHost(), rodsSvrConfig.getPort(), adminAcct,
                                                           adminPw, rootPath, zone, rodsSvrConfig.getDefaultResource(),
                                                           adminAcct, zone);

        try
        {
            establishRoot(_factory.getIRODSFileFactory(acct).instanceIRODSFile(rootPath), _factory, acct);
        }
        finally
        {
            _factory.closeSessionAndEatExceptions();
        }
    }

    private void establishRoot(IRODSFile _irodsMountPoint, IRODSAccessObjectFactory _factory, IRODSAccount _adminAcct) throws DataNotFoundException, JargonException
    {
        if (!_irodsMountPoint.exists())
        {
            throw new DataNotFoundException("Cannot establish root at [" + _irodsMountPoint + "]");
        }


        CollectionAndDataObjectListAndSearchAO lao = _factory.getCollectionAndDataObjectListAndSearchAO(_adminAcct);
        ObjStat objStat = lao.retrieveObjectStatForPath(_irodsMountPoint.getAbsolutePath());
        long newInodeNumber = objStat.getDataId();

        log_.debug("establishRoot - Mapping root to [{}] ...", _irodsMountPoint);

        map((long) newInodeNumber, _irodsMountPoint.getAbsolutePath());
        

        log_.debug("establishRoot - Mapping successful.");
    }
    
    public Long getInodeNumberByPath(String _path)
    {
        return getInodeNumberByPath(Paths.get(_path));
    }

    public Long getInodeNumberByPath(Path _path)
    {
        final Wrapper<Long> ref = new Wrapper<>();
        new AtomicRead(() -> ref.value = pathToInode_.get(_path));
        return ref.value;
    }

    public Path getPathByInodeNumber(Long _inodeNumber)
    {
        final Wrapper<Path> ref = new Wrapper<>();
        new AtomicRead(() -> ref.value = inodeToPath_.get(_inodeNumber));
        return ref.value;
    }

    public boolean isMapped(Long _inodeNumber)
    {
        final Wrapper<Boolean> ref = new Wrapper<>(false);
        new AtomicRead(() -> ref.value = inodeToPath_.containsKey(_inodeNumber));
        return ref.value;
    }

    public boolean isMapped(String _path)
    {
        return isMapped(Paths.get(_path));
    }

    public boolean isMapped(Path _path)
    {
        final Wrapper<Boolean> ref = new Wrapper<>(false);
        new AtomicRead(() -> ref.value = pathToInode_.containsKey(_path));
        return ref.value;
    }

    public void map(Long _inodeNumber, String _path)
    {
        map(_inodeNumber, Paths.get(_path));
    }

    public void map(Long _inodeNumber, Path _path)
    {
        new AtomicWrite(() -> {
            log_.debug("map - mapping inode number to path [{} => {}] ...", _inodeNumber, _path);

            Path otherPath = inodeToPath_.putIfAbsent(_inodeNumber, _path);

            log_.debug("map - previously mapped path [{}]", otherPath);

            if (otherPath != null)
            {
                throw new IllegalStateException("Inode number is already mapped to existing path");
            }

            Long otherInodeNumber = pathToInode_.putIfAbsent(_path, _inodeNumber);

            if (otherInodeNumber != null)
            {
                if (inodeToPath_.remove(_inodeNumber) != _path)
                {
                    throw new IllegalStateException("Failed to rollback mapping");
                }

                throw new IllegalStateException("Path is already mapped to existing inode number");
            }
        });
    }

    public void unmap(Long _inodeNumber, Path _path)
    {
        final boolean storeInAvailableInodeNumbersSet = true;
        unmap(_inodeNumber, _path, storeInAvailableInodeNumbersSet);
    }

    private void unmap(Long _inodeNumber, Path _path, boolean _storeInAvailableInodeNumbersSet)
    {
        log_.debug("unmap - unmapping inode number and path [{} => {}] ...", _inodeNumber, _path);

        new AtomicWrite(() -> {
            Path mappedPath = inodeToPath_.remove(_inodeNumber);

            if (!_path.equals(mappedPath))
            {
                throw new IllegalStateException("Invalid mapping: mismatch paths " +
                                                "[mapped path => " + mappedPath + "]");
            }

            long mappedInodeNumber = pathToInode_.remove(_path);

            if (mappedInodeNumber != _inodeNumber)
            {
                throw new IllegalStateException("Invalid mapping: mismatch inode numbers " +
                                                "[mapped inode number => " + mappedInodeNumber + "]");
            }

            if (_storeInAvailableInodeNumbersSet)
            {
                availableInodeNumbers_.add(_inodeNumber);
            }
        });
    }

    public void remap(Long _inodeNumber, Path _oldPath, Path _newPath)
    {
        // This block must NOT be wrapped in an AtomicWrite.
        // Doing so would cause a deadlock since unmap/map try to acquire the lock.
        final boolean storeInAvailableInodeNumbersSet = false;
        unmap(_inodeNumber, _oldPath, storeInAvailableInodeNumbersSet);
        map(_inodeNumber, _newPath);
    }
    
    private interface Function
    {
        void execute();
    }
    
    private final class AtomicRead
    {
        AtomicRead(Function _func)
        {
            Lock l = lock_.readLock();
            l.lock();
            try { _func.execute(); } finally { l.unlock(); }
        }
    }

    private final class AtomicWrite
    {
        AtomicWrite(Function _func)
        {
            Lock l = lock_.writeLock();
            l.lock();
            try { _func.execute(); } finally { l.unlock(); }
        }
    }
    
    private final class Wrapper<T>
    {
        T value;
        Wrapper() {}
        Wrapper(T _initialValue) { value = _initialValue; }
    }
}
