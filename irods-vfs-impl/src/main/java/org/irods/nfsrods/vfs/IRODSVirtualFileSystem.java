package org.irods.nfsrods.vfs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;

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
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.pub.io.IRODSFileInputStream;
import org.irods.jargon.core.pub.io.IRODSFileOutputStream;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry.ObjectType;
import org.irods.nfsrods.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Longs;

public class IRODSVirtualFileSystem implements VirtualFileSystem
{
    private static final Logger log_ = LoggerFactory.getLogger(IRODSVirtualFileSystem.class);

    private final IRODSIdMap idMapper_;

    public IRODSVirtualFileSystem(IRODSIdMap _idMapper) throws DataNotFoundException, JargonException
    {
        if (_idMapper == null)
        {
            throw new IllegalArgumentException("null idMapper");
        }

        idMapper_ = _idMapper;
    }

    @Override
    public int access(Inode _inode, int _mode) throws IOException
    {
        log_.debug("vfs::access");
        log_.debug("vfs::access - _mode (octal) = {}", Integer.toOctalString(_mode));
        log_.debug("vfs::access - _mode         = {}", Stat.modeToString(_mode));
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
            throw new IllegalArgumentException("Invalid file type [" + _type + "].");
        }

        IRODSUser user = getCurrentIRODSUser();

        log_.debug("vfs::create - _parent  = {}", getPath(toInodeNumber(_parent)));
        log_.debug("vfs::create - _type    = {}", _type);
        log_.debug("vfs::create - _name    = {}", _name);
        log_.debug("vfs::create - _subject = {}", _subject);
        log_.debug("vfs::create - _mode    = {}", Stat.modeToString(_mode));

        long parentInodeNumber = toInodeNumber(_parent);
        Path parentPath = getPath(parentInodeNumber);
        Path newPath = parentPath.resolve(_name);
        
        // TODO Should be check if the file exists before creating it?

        try
        {
            IRODSAccessObjectFactory aoFactory = user.getIRODSAccessObjectFactory();
            IRODSFileFactory fileFactory = aoFactory.getIRODSFileFactory(user.getAccount());
            IRODSFile newFile = fileFactory.instanceIRODSFile(newPath.toString());

            log_.debug("vfs::create - Creating new file at: {}", newFile);

            if (!newFile.createNewFile())
            {
                throw new IOException("Failed to create new file in iRODS.");
            }
                
            long newInodeNumber = user.getAndIncrementFileID();
            user.map(newInodeNumber, newPath);
            newFile.close();

            return toFh(newInodeNumber);
        }
        catch (JargonException e)
        {
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
        throw new UnsupportedOperationException("Not supported yet.");
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
        log_.debug("vfs::getattr - _inode = {}", getPath(toInodeNumber(_inode)));

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
        log_.debug("vfs::link");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DirectoryStream list(Inode _inode, byte[] _verifier, long _cookie) throws IOException
    {
        log_.debug("vfs::list");
        
        List<DirectoryEntry> list = new ArrayList<>();

        try
        {
            IRODSUser user = getCurrentIRODSUser();
            CollectionAndDataObjectListAndSearchAO listAO = user.getIRODSAccessObjectFactory()
                .getCollectionAndDataObjectListAndSearchAO(user.getAccount());

            Path parentPath = getPath(toInodeNumber(_inode));
            log_.debug("vfs::list - listing contents of [{}] ...", parentPath);

            String irodsAbsPath = parentPath.normalize().toString();

            List<CollectionAndDataObjectListingEntry> entries;
            entries = listAO.listDataObjectsAndCollectionsUnderPath(irodsAbsPath);
            
            for (CollectionAndDataObjectListingEntry dataObj : entries)
            {
                Path filePath = parentPath.resolve(dataObj.getPathOrName());
                log_.debug("vfs::list - entry = {}", filePath);

                long inodeNumber;

                if (user.getInodeToPathMap().containsValue(filePath))
                {
                    inodeNumber = getInodeNumber(filePath);
                }
                else
                {
                    inodeNumber = user.getAndIncrementFileID();
                    user.map(inodeNumber, filePath);
                }

                Stat stat = statPath(filePath, inodeNumber);
                Inode inode = toFh(inodeNumber);
                list.add(new DirectoryEntry(filePath.getFileName().toString(), inode, stat, inodeNumber));
            }
        }
        catch (JargonException e)
        {
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
        log_.debug("vfs::lookup - _parent = {}", getPath(toInodeNumber(_parent)));
        log_.debug("vfs::lookup - _path   = {}", _path);

        Path parentPath = getPath(toInodeNumber(_parent));
        Path targetPath = parentPath.normalize().resolve(_path);

        log_.debug("vfs::lookup - Looking up [{}] ...", targetPath);

        // Handle paths that include collections/files that have not been mapped yet.
        IRODSUser user = getCurrentIRODSUser();

        if (user.getPathToInodeMap().containsKey(targetPath))
        {
            return toFh(getInodeNumber(targetPath));
        }

        // The path has not been seen before.
        // Create a new inode number and map it to the target path if and only
        // if the target path is a valid path in iRODS.

        try
        {
            CollectionAndDataObjectListAndSearchAO lao = user.getIRODSAccessObjectFactory()
                .getCollectionAndDataObjectListAndSearchAO(user.getAccount());

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

            if (isTargetValid)
            {
                long newInodeNumber = user.getAndIncrementFileID();
                user.map(newInodeNumber, targetPath);
                return toFh(newInodeNumber);
            }

            // It is VERY important that this exception is thrown here.
            // It affects how NFS4J continues processing the request.
            throw new NoEntException("Path does not exist.");
        }
        catch (JargonException e)
        {
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

            IRODSUser user = getCurrentIRODSUser();
            IRODSFile irodsFile = user.getIRODSAccessObjectFactory().getIRODSFileFactory(user.getAccount())
                .instanceIRODSFile(parentPath.toString(), _path);

            String json = JSONUtils.toJSON(user.getInodeToPathMap());
            log_.debug("vfs::mkdir - Inode map (before creating new directory) = {}", json);
            log_.debug("vfs::mkdir - New directory path = {}", irodsFile.getAbsolutePath());

            irodsFile.mkdir();

            long inodeNumber = user.getAndIncrementFileID();
            log_.debug("vfs::mkdir - New inode number = {}", inodeNumber);

            user.map(inodeNumber, irodsFile.getAbsolutePath());

            return toFh(inodeNumber);
        }
        catch (JargonException e)
        {
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
        log_.debug("vfs::move - _inode   = {}", _inode);
        log_.debug("vfs::move - _dest    = {}", _dest);
        log_.debug("vfs::move - _oldName = {}", _oldName);
        log_.debug("vfs::move - _newName = {}", _newName);

        try
        {
            Path parentPath = getPath(toInodeNumber(_inode));
            Path destPath = getPath(toInodeNumber(_dest));
            String irodsParentPath = parentPath.toString() + "/" + _oldName;
            log_.debug("vfs::move - Parent path = {}", irodsParentPath);

            IRODSUser user = getCurrentIRODSUser();
            IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();
            IRODSFile pathFile = aof.getIRODSFileFactory(user.getAccount()).instanceIRODSFile(irodsParentPath);
            String destPathString;

            if (_newName != null && !_oldName.equals(_newName))
            {
                destPathString = destPath.toString() + "/" + _newName;
            }
            else
            {
                destPathString = destPath.toString() + "/" + _oldName;
            }

            log_.debug("vfs::move - Destination path = {}", destPathString);

            IRODSFile destFile = aof.getIRODSFileFactory(user.getAccount()).instanceIRODSFile(destPathString);
            IRODSFileSystemAO fileSystemAO = aof.getIRODSFileSystemAO(user.getAccount());

            log_.debug("vfs::move - Is file? {}", pathFile.isFile());

            if (pathFile.isFile())
            {
                fileSystemAO.renameFile(pathFile, destFile);
            }
            else
            {
                fileSystemAO.renameDirectory(pathFile, destFile);
            }

            // TODO Is remap needed when moving files iRODS side, or will list be regened
            // via list() call?
            Path oldPath = Paths.get(irodsParentPath);
            Path newPath = Paths.get(destPathString);

            log_.debug("vfs::move - Old path     = {}" + oldPath);
            log_.debug("vfs::move - New path     = {}" + newPath);
            log_.debug("vfs::move - Inode number = {}" + user.getPathToInodeMap().get(oldPath));

            user.remap(user.getPathToInodeMap().get(oldPath), oldPath, newPath);

            return true;
        }
        catch (JargonException e)
        {
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
        log_.debug("vfs::read - _offset = {}", _offset);
        log_.debug("vfs::read - _count  = {}", _count);

        IRODSUser user = getCurrentIRODSUser();
        IRODSAccessObjectFactory aoFactory = user.getIRODSAccessObjectFactory();

        try
        {
            Path path = getPath(toInodeNumber(_inode));
            IRODSFileFactory fileFactory = aoFactory.getIRODSFileFactory(user.getAccount());
            IRODSFile file = fileFactory.instanceIRODSFile(path.toString());

            try (IRODSFileInputStream fis = fileFactory.instanceIRODSFileInputStream(file))
            {
                return fis.read(_data, (int) _offset, _count);
            }
        }
        catch (IOException | JargonException e)
        {
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(Inode _parent, String _path) throws IOException
    {
        log_.debug("vfs::remove");
        log_.debug("vfs::remove - _parent = {}", getPath(toInodeNumber(_parent)));
        log_.debug("vfs::remove - _path   = {}", _path);

        try
        {
            IRODSUser user = getCurrentIRODSUser();

            Path parentPath = getPath(toInodeNumber(_parent));
            Path objectPath = parentPath.resolve(_path);

            IRODSFile file = user.getIRODSAccessObjectFactory().getIRODSFileFactory(user.getAccount())
                .instanceIRODSFile(parentPath.toString(), _path);

            log_.debug("vfs::remove - Removing data object ...");

            if (!file.delete())
            {
                throw new IOException("Failed to delete data object in iRODS.");
            }

            user.unmap(getInodeNumber(objectPath), objectPath);

            log_.debug("vfs::remove - Data object removed.");
        }
        catch (JargonException e)
        {
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

    @Override
    public void setattr(Inode _inode, Stat _stat) throws IOException
    {
        log_.debug("vfs::setattr");
        log_.debug("vfs::setattr - _inode = {}", getPath(toInodeNumber(_inode)));
        log_.debug("vfs::setattr - _stat  = {}", _stat);
        
        if (_stat.isDefined(Stat.StatAttribute.OWNER))
        {
            log_.debug("vfs::setattr - New owner id = {}", _stat.getUid());
        }

        if (_stat.isDefined(Stat.StatAttribute.GROUP))
        {
            log_.debug("vfs::setattr - New group id = {}", _stat.getGid());
        }

        if (_stat.isDefined(Stat.StatAttribute.MODE))
        {
            log_.debug("vfs::setattr - New mode = {}", Integer.toOctalString(_stat.getMode()));

            IRODSUser user = getCurrentIRODSUser();
            IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();

            try
            {
                IRODSAccount acct = user.getAccount();
                Path path = getPath(toInodeNumber(_inode));
                DataObjectAO dao = aof.getDataObjectAO(acct);
                FilePermissionEnum perm;
                
                // @formatter:off
                switch (_stat.getMode() & 0700)
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
            }
            finally
            {
                closeCurrentConnection();
            }
        }

        if (_stat.isDefined(Stat.StatAttribute.SIZE))
        {
            IRODSUser user = getCurrentIRODSUser();
            IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();

            try
            {
                IRODSFileFactory ff = aof.getIRODSFileFactory(user.getAccount());
                Path path = getPath(toInodeNumber(_inode));
                IRODSFile file = ff.instanceIRODSFile(path.toString());
                
                // Only allow shrinking.
                if (_stat.getSize() >= file.length())
                {
                    // TODO Should this be an exception?
                    log_.debug("vfs::setattr - Increasing the size of the file is prohibited!");
                    return;
                }
                
                byte[] bytes = new byte[(int) _stat.getSize()];
                int bytesRead = 0;
                
                try (IRODSFileInputStream fis = ff.instanceIRODSFileInputStream(file))
                {
                    bytesRead = fis.read(bytes);
                }

                log_.debug("vfs::setattr - Bytes read = {}", bytesRead);
                
                try (IRODSFileOutputStream fos = ff.instanceIRODSFileOutputStream(file, OpenFlags.WRITE_TRUNCATE))
                {
                    fos.write(bytes, 0, bytesRead);
                }

                log_.debug("vfs::setattr - New size = {}", _stat.getSize());

                file.close();
            }
            catch (JargonException e)
            {
                log_.error(e.getMessage());
            }
            finally
            {
                closeCurrentConnection();
            }
        }

        if (_stat.isDefined(Stat.StatAttribute.ATIME))
        {
            log_.debug("vfs::setattr - New access time = {}", FileTime.fromMillis(_stat.getATime()).toInstant());
        }

        if (_stat.isDefined(Stat.StatAttribute.MTIME))
        {
            log_.debug("vfs::setattr - New modify time = {}", FileTime.fromMillis(_stat.getMTime()).toInstant());

            // FIXME IRODSFile::setLastModified is not implemented.
//            IRODSUser user = getCurrentIRODSUser();
//            IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();
//            
//            try
//            {
//                IRODSFileFactory ff = aof.getIRODSFileFactory(user.getRootAccount());
//                Path path = resolveInode(getInodeNumber(_inode));
//                IRODSFile file = ff.instanceIRODSFile(path.toString());
//
//                log_.debug("vfs::setattr - New modify time = {}", FileTime.fromMillis(_stat.getMTime()).toInstant());
//
//                file.setLastModified(_stat.getMTime());
//                file.close();
//            }
//            catch (JargonException e)
//            {
//                log_.error(e.getMessage());
//            }
//            finally
//            {
//                closeCurrentConnection();
//            }
        }

        if (_stat.isDefined(Stat.StatAttribute.CTIME))
        {
            log_.debug("vfs::setattr - New create time = {}", FileTime.fromMillis(_stat.getCTime()).toInstant());
        }
    }

    @Override
    public Inode symlink(Inode _parent, String _linkName, String _targetName, Subject _subject, int _mode)
        throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WriteResult write(Inode _inode, byte[] _data, long _offset, int _count, StabilityLevel _stabilityLevel)
        throws IOException
    {
        log_.debug("vfs::write");
        log_.debug("vfs::write - _offset = {}", _offset);
        log_.debug("vfs::write - _count  = {}", _count);

        IRODSUser user = getCurrentIRODSUser();
        IRODSAccessObjectFactory aoFactory = user.getIRODSAccessObjectFactory();

        try
        {
            Path path = getPath(toInodeNumber(_inode));
            IRODSFileFactory fileFactory = aoFactory.getIRODSFileFactory(user.getAccount());
            IRODSFile file = fileFactory.instanceIRODSFile(path.toString());

            try (IRODSFileOutputStream fos = fileFactory.instanceIRODSFileOutputStream(file))
            {
                fos.write(_data, (int) _offset, _count);
                return new WriteResult(_stabilityLevel, _count); // TODO Need to revisit this
            }
        }
        catch (IOException | JargonException e)
        {
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    private Stat statPath(Path _path, long _inodeNumber) throws IOException
    {
        log_.debug("vfs::statPath");
        log_.debug("vfs::statPath - _inodeNumber      = {}", _inodeNumber);
        log_.debug("vfs::statPath - _path             = {}", _path);

//        if (_path == null)
//        {
//            throw new IllegalArgumentException("null path");
//        }

//        String path = _path.normalize().toString();
//        log_.debug("vfs::statPath - Normalized path   = {}", path);

        try
        {
            IRODSUser user = getCurrentIRODSUser();
            IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();
            CollectionAndDataObjectListAndSearchAO lao = null;
            lao = aof.getCollectionAndDataObjectListAndSearchAO(user.getAccount());
            ObjStat objStat = lao.retrieveObjectStatForPath(_path.toString());
            log_.debug("vfs::statPath - iRODS stat info = {}", objStat);

            Stat stat = new Stat();

            stat.setATime(objStat.getModifiedAt().getTime());
            stat.setCTime(objStat.getCreatedAt().getTime());
            stat.setMTime(objStat.getModifiedAt().getTime());

            // UserAO userAO = user.getIrodsAccessObjectFactory().getUserAO(user.getRootAccount());
            // StringBuilder sb = new StringBuilder();
            // sb.append(objStat.getOwnerName());
            // sb.append("#");
            // sb.append(objStat.getOwnerZone());

            setStatMode(_path.toString(), stat, objStat.getObjectType(), user);

            stat.setUid(getUserID());
            stat.setGid(getUserID()); // TODO Investigate groups (Jargon has ACL support!!!).
            stat.setNlink(1);
            stat.setDev(17);
            stat.setIno((int) _inodeNumber);
            stat.setRdev(17);
            stat.setSize(objStat.getObjSize());
            stat.setFileid((int) _inodeNumber);
            stat.setGeneration(objStat.getModifiedAt().getTime());

            log_.debug("vfs::statPath - User id     = {}", getUserID());
            log_.debug("vfs::statPath - Permissions = {}", Stat.modeToString(stat.getMode()));
            log_.debug("vfs::statPath - Stat        = {}", stat);

            return stat;
        }
        catch (NumberFormatException | JargonException e)
        {
            throw new IOException(e);
        }
    }

    private static Inode toFh(long _inodeNumber)
    {
        return Inode.forFile(Longs.toByteArray(_inodeNumber));
    }

    private Path getPath(long _inodeNumber) throws NoEntException
    {
        IRODSUser user = getCurrentIRODSUser();
        Path path = user.getInodeToPathMap().get(_inodeNumber);

        if (path == null)
        {
            throw new NoEntException("Path does not exist for [" + _inodeNumber + "].");
        }

        return path;
    }

    private long getInodeNumber(Path _path) throws NoEntException
    {
        IRODSUser user = getCurrentIRODSUser();
        Long inodeNumber = user.getPathToInodeMap().get(_path);

        if (inodeNumber == null)
        {
            throw new NoEntException("Inode number does not exist for [" + _path + "].");
        }

        return inodeNumber;
    }

    private static long toInodeNumber(Inode _inode)
    {
        return Longs.fromByteArray(_inode.getFileId());
    }

    private static void setStatMode(String _path, Stat _stat, ObjectType _objType, IRODSUser _user)
        throws JargonException
    {
        IRODSAccessObjectFactory aof = _user.getIRODSAccessObjectFactory();

        switch (_objType)
        {
            case COLLECTION:
                CollectionAO coa = aof.getCollectionAO(_user.getAccount());
                _stat.setMode(Stat.S_IFDIR | calcMode(coa.listPermissionsForCollection(_path)));
                break;

            case DATA_OBJECT:
                DataObjectAO doa = aof.getDataObjectAO(_user.getAccount());
                // ~0111 is needed to unset the execute bits. The parentheses aren't needed
                // really, but they help to emphasize what bits we are interested in.
                _stat.setMode(Stat.S_IFREG | (~0111 & calcMode(doa.listPermissionsForDataObject(_path))));
                break;

            // This is required when the iRODS mount point is NOT set to the client's
            // home directory.
            //
            // TODO This appears to attached to collections automatically made
            // by iRODS (e.g. /tempZone, /tempZone/{home,public}, etc.).
            case COLLECTION_HEURISTIC_STANDIN:
                _stat.setMode(Stat.S_IFDIR | 0700);
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
        // Permissions are only set for the user and world.
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
                case OWN:     mode |= (r | w | x); break;
                case READ:    mode |= r; break;
                case WRITE:   mode |= w; break;
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

    private IRODSUser getCurrentIRODSUser()
    {
        return idMapper_.resolveUser(getUserID());
    }
    
    private void closeCurrentConnection()
    {
        IRODSUser user = getCurrentIRODSUser();
        user.getIRODSAccessObjectFactory().closeSessionAndEatExceptions(user.getAccount());
    }
}
