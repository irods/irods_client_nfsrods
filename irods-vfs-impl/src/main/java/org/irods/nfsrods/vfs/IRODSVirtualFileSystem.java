package org.irods.nfsrods.vfs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.security.auth.Subject;

import org.dcache.nfs.status.NoEntException;
import org.dcache.nfs.v4.NfsIdMapping;
import org.dcache.nfs.v4.xdr.nfsace4;
import org.dcache.nfs.vfs.AclCheckable;
import org.dcache.nfs.vfs.DirectoryEntry;
import org.dcache.nfs.vfs.DirectoryStream;
import org.dcache.nfs.vfs.FsStat;
import org.dcache.nfs.vfs.Inode;
import org.dcache.nfs.vfs.Stat;
import org.dcache.nfs.vfs.Stat.Type;
import org.dcache.nfs.vfs.VirtualFileSystem;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.packinstr.DataObjInp.OpenFlags;
import org.irods.jargon.core.protovalues.FilePermissionEnum;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.CollectionAndDataObjectListAndSearchAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystemAO;
import org.irods.jargon.core.pub.domain.ObjStat;
import org.irods.jargon.core.pub.domain.UserFilePermission;
import org.irods.jargon.core.pub.io.FileIOOperations;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.pub.io.IRODSFileInputStream;
import org.irods.jargon.core.pub.io.IRODSFileOutputStream;
import org.irods.jargon.core.pub.io.IRODSRandomAccessFile;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry.ObjectType;
import org.irods.nfsrods.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Longs;

public class IRODSVirtualFileSystem implements VirtualFileSystem
{
    private static final Logger log_ = LoggerFactory.getLogger(IRODSVirtualFileSystem.class);

    private final IRODSAccessObjectFactory factory_;
    private final IRODSIdMapper idMapper_;
    private final InodeToPathMapper inodeToPathMapper_;
    private final MutableConfiguration<Path, Stat> cacheConfig_;
    private final Cache<Path, Stat> statObjectCache_;
    
    private static final long FIXED_TIMESTAMP = System.currentTimeMillis();

    public IRODSVirtualFileSystem(ServerConfig _config,
                                  IRODSAccessObjectFactory _factory,
                                  IRODSIdMapper _idMapper,
                                  CacheManager _cacheManager)
        throws DataNotFoundException,
        JargonException
    {
        if (_factory == null)
        {
            throw new IllegalArgumentException("Null IRODSAccessObjectFactory");
        }

        if (_config == null)
        {
            throw new IllegalArgumentException("Null ServerConfig");
        }

        if (_idMapper == null)
        {
            throw new IllegalArgumentException("Null IRODSIdMap");
        }

        if (_cacheManager == null)
        {
            throw new IllegalArgumentException("Null CacheManager");
        }

        factory_ = _factory;
        idMapper_ = _idMapper;
        inodeToPathMapper_ = new InodeToPathMapper(_config, _factory);
        
        int file_info_refresh_time = _config.getNfsServerConfig().getFileInfoRefreshTimeInSeconds();
        
        cacheConfig_ = new MutableConfiguration<Path, Stat>()
            .setTypes(Path.class, Stat.class)
            .setStoreByValue(false)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, file_info_refresh_time)));
        
        statObjectCache_ = _cacheManager.createCache("stat_info_cache", cacheConfig_);
    }

    @Override
    public int access(Inode _inode, int _mode) throws IOException
    {
        return _mode;
    }

    @Override
    public void commit(Inode _inode, long _offset, int _count) throws IOException
    {
        // NOP
    }

    @Override
    public Inode create(Inode _parent, Type _type, String _name, Subject _subject, int _mode) throws IOException
    {
        log_.debug("vfs::create");

        if (_parent == null)
        {
            throw new IllegalArgumentException("Null parent");
        }

        if (_type == null)
        {
            throw new IllegalArgumentException("Null type");
        }

        if (_name == null)
        {
            throw new IllegalArgumentException("Null name");
        }

        if (_subject == null)
        {
            throw new IllegalArgumentException("Null subjet");
        }

        if (Type.REGULAR != _type)
        {
            throw new IllegalArgumentException("Invalid file type [" + _type + "]");
        }

        IRODSUser user = getCurrentIRODSUser();

        Path parentPath = getPath(toInodeNumber(_parent));
        Path newPath = parentPath.resolve(_name);

        log_.debug("create - _parent  = {}", parentPath);
        log_.debug("create - _type    = {}", _type);
        log_.debug("create - _name    = {}", _name);
        log_.debug("create - _subject = {}", _subject);
        log_.debug("create - _mode    = {}", Stat.modeToString(_mode));

        try
        {
            IRODSFileFactory ff = factory_.getIRODSFileFactory(user.getAccount());
            IRODSFile newFile = ff.instanceIRODSFile(newPath.toString());

            log_.debug("create - Creating new file at: {}", newFile);

            try (AutoClosedIRODSFile ac = new AutoClosedIRODSFile(newFile))
            {
                if (!newFile.createNewFile())
                {
                    throw new IOException("Failed to create new file in iRODS");
                }
            }

            long newInodeNumber = inodeToPathMapper_.getAndIncrementFileID();
            inodeToPathMapper_.map(newInodeNumber, newPath);

            return toFh(newInodeNumber);
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public byte[] directoryVerifier(Inode _inode) throws IOException
    {
        return DirectoryStream.ZERO_VERIFIER;
    }

    @Override
    public nfsace4[] getAcl(Inode _inode) throws IOException
    {
        return new nfsace4[0];
    }

    @Override
    public AclCheckable getAclCheckable()
    {
        return AclCheckable.UNDEFINED_ALL;
    }

    @Override
    public FsStat getFsStat() throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public NfsIdMapping getIdMapper()
    {
        return idMapper_;
    }

    @Override
    public Inode getRootInode() throws IOException
    {
        return toFh(1);
    }

    @Override
    public Stat getattr(Inode _inode) throws IOException
    {
        log_.debug("vfs::getattr");

        long inodeNumber = toInodeNumber(_inode);
        Path path = getPath(inodeNumber);

        try
        {
            return statPath(path, inodeNumber);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public boolean hasIOLayout(Inode _inode) throws IOException
    {
        return false;
    }

    @Override
    public Inode link(Inode _parent, Inode _existing, String _target, Subject _subject) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DirectoryStream list(Inode _inode, byte[] _verifier, long _cookie) throws IOException
    {
        log_.debug("vfs::list");

        List<DirectoryEntry> list = new ArrayList<>();

        try
        {
            IRODSAccount acct = getCurrentIRODSUser().getAccount();
            CollectionAndDataObjectListAndSearchAO listAO = factory_.getCollectionAndDataObjectListAndSearchAO(acct);

            Path parentPath = getPath(toInodeNumber(_inode));
            log_.debug("list - listing contents of [{}] ...", parentPath);

            String irodsAbsPath = parentPath.normalize().toString();

            List<CollectionAndDataObjectListingEntry> entries;
            entries = listAO.listDataObjectsAndCollectionsUnderPath(irodsAbsPath);

            for (CollectionAndDataObjectListingEntry dataObj : entries)
            {
                Path filePath = parentPath.resolve(dataObj.getPathOrName());
                log_.debug("list - entry = {}", filePath);

                long inodeNumber;

                if (inodeToPathMapper_.getInodeToPathMap().containsValue(filePath))
                {
                    inodeNumber = getInodeNumber(filePath);
                }
                else
                {
                    inodeNumber = inodeToPathMapper_.getAndIncrementFileID();
                    inodeToPathMapper_.map(inodeNumber, filePath);
                }

                Stat stat = statPath(filePath, inodeNumber);
                Inode inode = toFh(inodeNumber);
                list.add(new DirectoryEntry(filePath.getFileName().toString(), inode, stat, inodeNumber));
            }
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }

        return new DirectoryStream(DirectoryStream.ZERO_VERIFIER, list);
    }

    @Override
    public Inode lookup(Inode _parent, String _path) throws IOException
    {
        log_.debug("vfs::lookup");

        Path parentPath = getPath(toInodeNumber(_parent));
        Path targetPath = parentPath.resolve(_path);

        log_.debug("lookup - _path   = {}", _path);
        log_.debug("lookup - _parent = {}", parentPath);
        log_.debug("lookup - Looking up [{}] ...", targetPath);

        try
        {
            IRODSAccount acct = getCurrentIRODSUser().getAccount();
            CollectionAndDataObjectListAndSearchAO lao = factory_.getCollectionAndDataObjectListAndSearchAO(acct);

            // @formatter:off
            boolean isTargetValid = lao.listDataObjectsAndCollectionsUnderPath(parentPath.toString())
                .stream().anyMatch(obj -> {
                    if (obj.isCollection())
                    {
                        return targetPath.toString().equals(obj.getPathOrName());
                    }

                    return _path.equals(obj.getPathOrName());
                });
            // @formatter:on

            // If the target path is valid, then return an inode object created from
            // the user's mapped paths. Else, create a new mapping and return an
            // inode object for the new mapping.
            if (isTargetValid)
            {
                if (inodeToPathMapper_.getPathToInodeMap().containsKey(targetPath))
                {
                    return toFh(getInodeNumber(targetPath));
                }

                long newInodeNumber = inodeToPathMapper_.getAndIncrementFileID();
                inodeToPathMapper_.map(newInodeNumber, targetPath);
                return toFh(newInodeNumber);
            }

            // If the target path is not registered in iRODS and NFSRODS has previously
            // mapped it, then unmap it. This keeps NFSRODS in sync with iRODS.
            if (inodeToPathMapper_.getPathToInodeMap().containsKey(targetPath))
            {
                inodeToPathMapper_.unmap(getInodeNumber(targetPath), targetPath);
            }

            // It is VERY important that this exception is thrown here.
            // It affects how NFS4J continues processing the request.
            throw new NoEntException("Path does not exist");
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public Inode mkdir(Inode _inode, String _path, Subject _subject, int _mode) throws IOException
    {
        log_.debug("vfs::mkdir");

        try
        {
            Path parentPath = getPath(toInodeNumber(_inode));

            IRODSAccount acct = getCurrentIRODSUser().getAccount();
            IRODSFile file = factory_.getIRODSFileFactory(acct).instanceIRODSFile(parentPath.toString(), _path);

            file.mkdir();
            file.close();

            long inodeNumber = inodeToPathMapper_.getAndIncrementFileID();
            inodeToPathMapper_.map(inodeNumber, file.getAbsolutePath());

            return toFh(inodeNumber);
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public boolean move(Inode _inode, String _oldName, Inode _dest, String _newName) throws IOException
    {
        log_.debug("vfs::move");
        log_.debug("move - _inode   = {}", _inode);
        log_.debug("move - _dest    = {}", _dest);
        log_.debug("move - _oldName = {}", _oldName);
        log_.debug("move - _newName = {}", _newName);

        IRODSUser user = getCurrentIRODSUser();

        try
        {
            Path parentPath = getPath(toInodeNumber(_inode));
            Path destPath = getPath(toInodeNumber(_dest));
            String irodsParentPath = parentPath.toString() + "/" + _oldName;

            log_.debug("move - Parent path = {}", irodsParentPath);

            IRODSFileFactory ff = factory_.getIRODSFileFactory(user.getAccount());
            String destPathString = null;

            if (_newName != null && !_oldName.equals(_newName))
            {
                destPathString = destPath.toString() + "/" + _newName;
            }
            else
            {
                destPathString = destPath.toString() + "/" + _oldName;
            }

            log_.debug("move - Destination path = {}", destPathString);

            IRODSFile pathFile = ff.instanceIRODSFile(irodsParentPath);
            IRODSFile destFile = ff.instanceIRODSFile(destPathString);

            try (AutoClosedIRODSFile ac0 = new AutoClosedIRODSFile(pathFile);
                 AutoClosedIRODSFile ac1 = new AutoClosedIRODSFile(destFile))
            {
                IRODSFileSystemAO fsao = factory_.getIRODSFileSystemAO(user.getAccount());

                log_.debug("move - Is file? {}", pathFile.isFile());

                if (pathFile.isFile())
                {
                    fsao.renameFile(pathFile, destFile);
                }
                else
                {
                    fsao.renameDirectory(pathFile, destFile);
                }
            }

            // Remap the inode number to the correct path.

            Path oldPath = Paths.get(irodsParentPath);
            Path newPath = Paths.get(destPathString);

            log_.debug("move - Old path     = {}" + oldPath);
            log_.debug("move - New path     = {}" + newPath);
            log_.debug("move - Inode number = {}" + inodeToPathMapper_.getPathToInodeMap().get(oldPath));

            inodeToPathMapper_.remap(inodeToPathMapper_.getPathToInodeMap().get(oldPath), oldPath, newPath);

            return true;
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public Inode parentOf(Inode _inode) throws IOException
    {
        log_.debug("vfs::parentOf");
        Path path = getPath(toInodeNumber(_inode));
        return toFh(getInodeNumber(path.getParent()));
    }

    @Override
    public int read(Inode _inode, byte[] _data, long _offset, int _count) throws IOException
    {
        log_.debug("vfs::read");
        log_.debug("read - _data.length = {}", _data.length);
        log_.debug("read - _offset      = {}", _offset);
        log_.debug("read - _count       = {}", _count);

        IRODSAccount acct = getCurrentIRODSUser().getAccount();

        try
        {
            Path path = getPath(toInodeNumber(_inode));
            IRODSFileFactory ff = factory_.getIRODSFileFactory(acct);
            IRODSRandomAccessFile file = ff.instanceIRODSRandomAccessFile(path.toString());
            
            try (AutoClosedIRODSRandomAccessFile ac = new AutoClosedIRODSRandomAccessFile(file))
            {
                file.seek(_offset, FileIOOperations.SeekWhenceType.SEEK_START);
                return file.read(_data, 0, _count);
            }
        }
        catch (IOException | JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public String readlink(Inode _inode) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public void remove(Inode _parent, String _path) throws IOException
    {
        log_.debug("vfs::remove");
        log_.debug("remove - _parent = {}", getPath(toInodeNumber(_parent)));
        log_.debug("remove - _path   = {}", _path);

        IRODSAccount acct = getCurrentIRODSUser().getAccount();

        try
        {
            Path parentPath = getPath(toInodeNumber(_parent));
            Path objectPath = parentPath.resolve(_path);
            IRODSFileFactory ff = factory_.getIRODSFileFactory(acct);
            IRODSFile file = ff.instanceIRODSFile(parentPath.toString(), _path);

            log_.debug("remove - Removing object ...");

            try (AutoClosedIRODSFile ac = new AutoClosedIRODSFile(file))
            {
                if (!file.delete())
                {
                    throw new IOException("Failed to delete object in iRODS");
                }
            }

            inodeToPathMapper_.unmap(getInodeNumber(objectPath), objectPath);

            log_.debug("remove - Object removed.");
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public void setAcl(Inode _inode, nfsace4[] _acl) throws IOException
    {
        // NOP
    }

    private void setPermissions(Inode _inode) throws IOException
    {
        long inodeNumber = toInodeNumber(_inode);
        Path path = getPath(inodeNumber);
        Stat latestStat = statPath(path, inodeNumber);

        log_.debug("setPermissions - New mode = {}", Integer.toOctalString(latestStat.getMode()));

        IRODSAccount acct = getCurrentIRODSUser().getAccount();

        try
        {
            DataObjectAO dao = factory_.getDataObjectAO(acct);
            FilePermissionEnum perm = null;

            // @formatter:off
            switch (latestStat.getMode() & 0700)
            {
                case 0700:
                case 0600: perm = FilePermissionEnum.OWN; break;
                case 0400: perm = FilePermissionEnum.READ; break;
                case 0200: perm = FilePermissionEnum.WRITE; break;
                default:   perm = FilePermissionEnum.NULL; break;
            }
            // @formatter:on

            dao.setAccessPermission(acct.getZone(), path.toString(), acct.getUserName(), perm);
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }
    
    private void truncate(Inode _inode) throws IOException
    {
        long inodeNumber = toInodeNumber(_inode);
        Path path = getPath(inodeNumber);
        Stat latestStat = statPath(path, inodeNumber);

        log_.debug("truncate - New size  = {}", latestStat.getSize());

        IRODSAccount acct = getCurrentIRODSUser().getAccount();

        try
        {
            IRODSFileFactory ff = factory_.getIRODSFileFactory(acct);
            IRODSFile file = ff.instanceIRODSFile(path.toString());
            
            try (AutoClosedIRODSFile ac = new AutoClosedIRODSFile(file))
            {
                // Delete everything in the data object.
                // TODO See if there is a better way to do this.
                if (latestStat.getSize() == 0)
                {
                    log_.trace("truncate - Erasing the contents of the data object ...");
                    file.delete();
                    file.createNewFile();
                    return;
                }

                final int CHUNK_SIZE = 4096;
                final long size_diff = latestStat.getSize() - file.length();
                final long full_chunks = Math.abs(size_diff) / CHUNK_SIZE;
                final long remaining_bytes = Math.abs(size_diff) % CHUNK_SIZE;

                // Increase the size of the data object.
                if (size_diff > 0)
                {
                    log_.trace("truncate - Increasing the size of the data object by {} ...", size_diff);

                    byte[] chunk = new byte[CHUNK_SIZE];

                    // Open the data object in append mode.
                    try (IRODSFileOutputStream fos = ff.instanceIRODSFileOutputStream(file, OpenFlags.READ_WRITE))
                    {
                        for (int i = 0; i < full_chunks; ++i)
                        {
                            fos.write(chunk);
                        }
                        
                        if (remaining_bytes > 0)
                        {
                            fos.write(chunk, 0, (int) remaining_bytes);
                        }
                    }
                }
                else if (size_diff < 0) // Truncate the size of the data object.
                {
                    log_.trace("truncate - Decreasing the size of the data object by {} ...", size_diff);

                    String newExt = ".temp_" + toInodeNumber(_inode);
                    IRODSFile tempFile = ff.instanceIRODSFile(path.toString() + newExt);

                    byte[] chunk = new byte[CHUNK_SIZE];

                    // @formatter:off
                    try (AutoClosedIRODSFile ac1 = new AutoClosedIRODSFile(tempFile);
                         IRODSFileInputStream fis = ff.instanceIRODSFileInputStream(file);
                         IRODSFileOutputStream fos = ff.instanceIRODSFileOutputStream(tempFile))
                    // @formatter:on
                    {
                        for (int i = 0; i < full_chunks; ++i)
                        {
                            fis.read(chunk);
                            fos.write(chunk);
                        }

                        if (remaining_bytes > 0)
                        {
                            fis.read(chunk, 0, (int) remaining_bytes);
                            fos.write(chunk, 0, (int) remaining_bytes);
                        }
                    }
                    
                    // Rename data objects.
                    file.delete();
                    tempFile.renameTo(file);
                }
            }
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public void setattr(Inode _inode, Stat _stat) throws IOException
    {
        log_.debug("vfs::setattr");
        log_.debug("setattr - _inode = {}", getPath(toInodeNumber(_inode)));
        log_.debug("setattr - _stat  = {}", _stat);
        
        if (_stat.isDefined(Stat.StatAttribute.MODE))
        {
            setPermissions(_inode);
        }

        if (_stat.isDefined(Stat.StatAttribute.SIZE))
        {
            truncate(_inode);
        }
    }

    @Override
    public Inode symlink(Inode _parent, String _linkName, String _targetName, Subject _subject, int _mode)
        throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet");
    }
    
    @Override
    public WriteResult write(Inode _inode, byte[] _data, long _offset, int _count, StabilityLevel _stabilityLevel)
        throws IOException
    {
        log_.debug("vfs::write");
        log_.debug("write - _data.length = {}", _data.length);
        log_.debug("write - _offset      = {}", _offset);
        log_.debug("write - _count       = {}", _count);

        IRODSAccount acct = getCurrentIRODSUser().getAccount();

        try
        {
            Path path = getPath(toInodeNumber(_inode));
            IRODSFileFactory ff = factory_.getIRODSFileFactory(acct);
            IRODSRandomAccessFile file = ff.instanceIRODSRandomAccessFile(path.toString());
            
            try (AutoClosedIRODSRandomAccessFile ac = new AutoClosedIRODSRandomAccessFile(file))
            {
                file.seek(_offset, FileIOOperations.SeekWhenceType.SEEK_START);
                file.write(_data, 0, _count);
                return new WriteResult(StabilityLevel.FILE_SYNC, _count);
            }
        }
        catch (IOException | JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    private Stat statPath(Path _path, long _inodeNumber) throws IOException
    {
        log_.debug("statPath - _inodeNumber        = {}", _inodeNumber);
        log_.debug("statPath - _path               = {}", _path);
        
        if (statObjectCache_.containsKey(_path))
        {
            log_.debug("statPath - Returning cached stat information ...", _path);
            return statObjectCache_.get(_path);
        }

        IRODSAccount acct = getCurrentIRODSUser().getAccount();

        try
        {
            CollectionAndDataObjectListAndSearchAO lao = null;
            lao = factory_.getCollectionAndDataObjectListAndSearchAO(acct);
            ObjStat objStat = lao.retrieveObjectStatForPath(_path.toString());

            log_.debug("statPath - iRODS stat info = {}", objStat);

            Stat stat = new Stat();

            setTime(stat, objStat);
            setStatMode(_path.toString(), stat, objStat.getObjectType(), acct);

            log_.debug("statPath - Owner name      = {}", objStat.getOwnerName());

            int ownerId = idMapper_.getUidForUser(objStat.getOwnerName());
            int groupId = idMapper_.getGidForUser(objStat.getOwnerName());
            
            stat.setUid(ownerId);
            stat.setGid(groupId);
            stat.setNlink(1);
            stat.setDev(17);
            stat.setIno((int) _inodeNumber);
            stat.setRdev(17);
            stat.setRdev(0);
            stat.setSize(objStat.getObjSize());
            stat.setFileid((int) _inodeNumber);
            stat.setGeneration(objStat.getModifiedAt().getTime());

            log_.debug("statPath - Owner ID        = {}", ownerId);
            log_.debug("statPath - Group ID        = {}", groupId);
            log_.debug("statPath - Permissions     = {}", Stat.modeToString(stat.getMode()));
            log_.debug("statPath - Stat            = {}", stat);
            
            statObjectCache_.put(_path, stat);

            return stat;
        }
        catch (NumberFormatException | JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
    }

    private void setTime(Stat _stat, ObjStat _objStat)
    {
        if (_objStat.getObjectType() == ObjectType.COLLECTION_HEURISTIC_STANDIN)
        {
            _stat.setATime(FIXED_TIMESTAMP);
            _stat.setCTime(FIXED_TIMESTAMP);
            _stat.setMTime(FIXED_TIMESTAMP);
        }
        else
        {
            _stat.setATime(_objStat.getModifiedAt().getTime());
            _stat.setCTime(_objStat.getCreatedAt().getTime());
            _stat.setMTime(_objStat.getModifiedAt().getTime());
        }
    }

    private static Inode toFh(long _inodeNumber)
    {
        return Inode.forFile(Longs.toByteArray(_inodeNumber));
    }

    private Path getPath(long _inodeNumber) throws IOException
    {
        Path path = inodeToPathMapper_.getInodeToPathMap().get(_inodeNumber);

        if (path == null)
        {
            throw new NoEntException("Path does not exist for [" + _inodeNumber + "]");
        }

        return path;
    }

    private long getInodeNumber(Path _path) throws IOException
    {
        Long inodeNumber = inodeToPathMapper_.getPathToInodeMap().get(_path);

        if (inodeNumber == null)
        {
            throw new NoEntException("Inode number does not exist for [" + _path + "]");
        }

        return inodeNumber;
    }

    private static long toInodeNumber(Inode _inode)
    {
        return Longs.fromByteArray(_inode.getFileId());
    }

//    private static int getGroupIdFromMetaData(IRODSUser _user, String _path)
//    {
//        IRODSAccessObjectFactory aof = _user.getIRODSAccessObjectFactory();
//        
//        try
//        {
//            DataObjectAO dao = aof.getDataObjectAO(_user.getAccount());
//            List<MetaDataAndDomainData> metadata = dao.findMetadataValuesForDataObject(_path);
//
//            if (!metadata.isEmpty())
//            {
//                for (MetaDataAndDomainData md : metadata)
//                {
//                    if ("filesystem::gid".equals(md.getAvuAttribute()))
//                    {
//                        return Integer.parseInt(md.getAvuValue());
//                    }
//                }
//            }
//        }
//        catch (JargonException e)
//        {
//            log_.error(e.getMessage());
//        }
//        
//        return -1;
//    }

    private void setStatMode(String _path, Stat _stat, ObjectType _objType, IRODSAccount _account)
        throws JargonException
    {
        switch (_objType)
        {
            case COLLECTION:
                CollectionAO coa = factory_.getCollectionAO(_account);
                _stat.setMode(Stat.S_IFDIR | (0055 | calcMode(coa.listPermissionsForCollection(_path))));
                break;

            case DATA_OBJECT:
                DataObjectAO doa = factory_.getDataObjectAO(_account);
                // ~0111 is needed to unset the execute bits. The parentheses aren't needed
                // really, but they help to emphasize what bits we are interested in.
                _stat.setMode(Stat.S_IFREG | (~0111 & calcMode(doa.listPermissionsForDataObject(_path))));
                break;

            // This object type comes from the Jargon library.
            // It is encountered when the user accessing iRODS is not a rodsadmin.
            case COLLECTION_HEURISTIC_STANDIN:
                _stat.setMode(Stat.S_IFDIR | 0755);
                break;

            case LOCAL_DIR:
            case LOCAL_FILE:
            case NO_INPUT:
            case UNKNOWN:
            case UNKNOWN_FILE:
            default:
                break;
        }
    }

    private static int calcMode(List<UserFilePermission> _perms)
    {
        // Permissions are only set for the user.
        // TODO Groups will need investigation.
        final int r = 0400; // Read bit
        final int w = 0200; // Write bit
        final int x = 0100; // Execute bit

        int mode = 0;

        for (UserFilePermission perm : _perms)
        {
            switch (perm.getFilePermissionEnum())
            {
                // @formatter:off
                case OWN:   mode |= (r | w | x); break;
                case READ:  mode |= r; break;
                case WRITE: mode |= w; break;
                default:
                // @formatter:on
            }
        }

        return mode;
    }

    private static int getUserID()
    {
        Subject subject = Subject.getSubject(AccessController.getContext());
        String name = subject.getPrincipals().iterator().next().getName();
        return Integer.parseInt(name);
    }

    private IRODSUser getCurrentIRODSUser() throws IOException
    {
        return idMapper_.resolveUser(getUserID());
    }

    private void closeCurrentConnection() throws IOException
    {
        factory_.closeSessionAndEatExceptions(getCurrentIRODSUser().getAccount());
    }

    private static class AutoClosedIRODSFile implements AutoCloseable
    {
        private final IRODSFile file_;

        AutoClosedIRODSFile(IRODSFile _file)
        {
            file_ = _file;
        }

        @Override
        public void close()
        {
            try
            {
                file_.close();
            }
            catch (JargonException e)
            {
                log_.error(e.getMessage());
            }
        }
    }

    private static class AutoClosedIRODSRandomAccessFile implements AutoCloseable
    {
        private final IRODSRandomAccessFile file_;

        AutoClosedIRODSRandomAccessFile(IRODSRandomAccessFile _file)
        {
            file_ = _file;
        }

        @Override
        public void close()
        {
            try
            {
                file_.close();
            }
            catch (IOException e)
            {
                log_.error(e.getMessage());
            }
        }
    }
}
