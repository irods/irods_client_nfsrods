package org.irods.nfsrods.vfs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * Default constructor
     * 
     * @param irodsAccessObjectFactory
     *            {@link IRODSAccessObjectFactory} with hooks to the core jargon
     *            system
     * @param rootAccount
     *            {@link IRODSAccount} that can access the root node
     * @param root
     *            {@link IRODSFile} that is the root node of this file system
     */
    public IRODSVirtualFileSystem(IRODSIdMap _idMapper) throws DataNotFoundException, JargonException
    {
        if (_idMapper == null)
        {
            throw new IllegalArgumentException("null idMapper");
        }

        idMapper_ = _idMapper;
    }

    /**
     * Check access to file system object.
     * <p/>
     * This method will honor the read/write/execute bits for the user mode and
     * ignore others TODO: don't know if this even makes sense
     *
     * @param _inode
     *            inode of the object to check.
     * @param _mode
     *            a mask of permission bits to check.
     * @return an allowed subset of permissions from the given mask.
     * @throws IOException
     */
    @Override
    public int access(Inode _inode, int _mode) throws IOException
    {
        log_.debug("vfs::access");
        log_.debug("vfs::access - _mode (octal) = {}", Integer.toOctalString(_mode));
        log_.debug("vfs::access - _mode         = {}", Stat.modeToString(_mode));

        return _mode;
    }

    /**
     * Flush data in {@code dirty} state to the stable storage. Typically follows
     * {@link #write()} operation.
     *
     * @param _inode
     *            inode of the file to commit.
     * @param _offset
     *            the file position to start commit at.
     * @param _count
     *            number of bytes to commit.
     * @throws IOException
     */
    @Override
    public void commit(Inode _inode, long _offset, int _count) throws IOException
    {
        log_.debug("vfs::commit");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Create a new object in a given directory with a specific name.
     *
     * @param _parent
     *            directory where new object must be created.
     * @param _type
     *            the type of the object to be created.
     * @param _name
     *            name of the object.
     * @param _subject
     *            the owner subject of a newly created object.
     * @param _mode
     *            initial permission mask.
     * @return the inode of the newly created object.
     * @throws IOException
     */
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

        log_.debug("vfs::create - _parent  = {}", resolveInode(getInodeNumber(_parent)));
        log_.debug("vfs::create - _type    = {}", _type);
        log_.debug("vfs::create - _name    = {}", _name);
        log_.debug("vfs::create - _subject = {}", _subject);
        log_.debug("vfs::create - _mode    = {}", Stat.modeToString(_mode));

        long parentInodeNumber = getInodeNumber(_parent);
        Path parentPath = resolveInode(parentInodeNumber);
        Path newPath = parentPath.resolve(_name);

        try
        {
            IRODSAccessObjectFactory aoFactory = user.getIRODSAccessObjectFactory();
            IRODSFileFactory fileFactory = aoFactory.getIRODSFileFactory(user.getRootAccount());
            IRODSFile newFile = fileFactory.instanceIRODSFile(newPath.toString());

            log_.debug("vfs::create - Creating new file at: {}", newFile);

            newFile.createNewFile();
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
        log_.debug("vfs::directoryVerifier");
        return null;
    }

    @Override
    public nfsace4[] getAcl(Inode _inode) throws IOException
    {
        log_.debug("vfs::getAcl");
        log_.debug("vfs::getAcl - _inode = {}", resolveInode(getInodeNumber(_inode)));
        // info on nfsace4:
        // https://www.ibm.com/support/knowledgecenter/en/ssw_aix_61/com.ibm.aix.osdevice/acl_type_nfs4.htm
        return new nfsace4[0]; // TODO: this is same in local need to look at what nfsace4 is composed of
    }

    @Override
    public AclCheckable getAclCheckable()
    {
        log_.debug("vfs::getAclCheckable");
        return AclCheckable.UNDEFINED_ALL;
    }

    @Override
    public FsStat getFsStat() throws IOException
    {
        log_.debug("vfs::getFsStat");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NfsIdMapping getIdMapper()
    {
        log_.debug("vfs::getIdMapper");
        return idMapper_;
    }

    @Override
    public Inode getRootInode() throws IOException
    {
        log_.debug("vfs::getRootInode");
        return toFh(1);
    }

    /**
     * Get file system object's attributes.
     *
     * @param _inode
     *            inode of the file system object.
     * @return {@link Stat} with file's attributes.
     * @throws IOException
     */
    @Override
    public Stat getattr(Inode _inode) throws IOException
    {
        log_.debug("vfs::getattr");
        log_.debug("vfs::getattr - _inode = {}", resolveInode(getInodeNumber(_inode)));

        long inodeNumber = getInodeNumber(_inode);
        Path path = resolveInode(inodeNumber);

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
        log_.debug("vfs::hasIOLayout");
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
                .getCollectionAndDataObjectListAndSearchAO(user.getRootAccount());

            Path parentPath = resolveInode(getInodeNumber(_inode));
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
                    inodeNumber = resolvePath(filePath);
                }
                else
                {
                    inodeNumber = user.getAndIncrementFileID();
                    user.map(inodeNumber, filePath);
                }

                Stat stat = statPath(filePath, inodeNumber);
                Inode inode = toFh(inodeNumber);
                list.add(new DirectoryEntry(filePath.getFileName().toString(), inode, stat, list.size()));
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

        return new DirectoryStream(list);
    }

    @Override
    public Inode lookup(Inode _parent, String _path) throws IOException
    {
        log_.debug("vfs::lookup");
        log_.debug("vfs::lookup - _parent = {}", resolveInode(getInodeNumber(_parent)));
        log_.debug("vfs::lookup - _path   = {}", _path);

        Path parentPath = resolveInode(getInodeNumber(_parent));
        Path targetPath = parentPath.normalize().resolve(_path);

        log_.debug("vfs::lookup - looking up [{}] ...", targetPath);

        // Handle paths that include collections/files that have not been mapped yet.
        IRODSUser user = getCurrentIRODSUser();

        if (user.getPathToInodeMap().containsKey(targetPath))
        {
            return toFh(resolvePath(targetPath));
        }

        // The path has not been seen before.
        // Create a new inode number and map it to the target path if and only
        // if the target path is a valid path in iRODS.

        try
        {
            CollectionAndDataObjectListAndSearchAO lao = user.getIRODSAccessObjectFactory()
                .getCollectionAndDataObjectListAndSearchAO(user.getRootAccount());

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
            Path parentPath = resolveInode(getInodeNumber(_inode));

            IRODSUser user = getCurrentIRODSUser();
            IRODSFile irodsFile = user.getIRODSAccessObjectFactory().getIRODSFileFactory(user.getRootAccount())
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
            Path parentPath = resolveInode(getInodeNumber(_inode));
            Path destPath = resolveInode(getInodeNumber(_dest));
            String irodsParentPath = parentPath.toString() + "/" + _oldName;
            log_.debug("vfs::move - Parent path = {}", irodsParentPath);

            IRODSUser user = getCurrentIRODSUser();
            IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();
            IRODSFile pathFile = aof.getIRODSFileFactory(user.getRootAccount()).instanceIRODSFile(irodsParentPath);
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

            IRODSFile destFile = aof.getIRODSFileFactory(user.getRootAccount()).instanceIRODSFile(destPathString);
            IRODSFileSystemAO fileSystemAO = aof.getIRODSFileSystemAO(user.getRootAccount());

            log_.debug("vfs::move - is file? " + pathFile.isFile());

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

            log_.debug("vfs::move - old path     = {}" + oldPath);
            log_.debug("vfs::move - new path     = {}" + newPath);
            log_.debug("vfs::move - inode number = {}" + user.getPathToInodeMap().get(oldPath));

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
        Path path = resolveInode(getInodeNumber(_inode));
        return toFh(resolvePath(path.getParent()));
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
            Path path = resolveInode(getInodeNumber(_inode));
            IRODSFileFactory fileFactory = aoFactory.getIRODSFileFactory(user.getRootAccount());
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
        log_.debug("vfs::readlink");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(Inode _parent, String _path) throws IOException
    {
        log_.debug("vfs::remove");

        try
        {
            IRODSUser user = getCurrentIRODSUser();

            Path parentPath = resolveInode(getInodeNumber(_parent));
            Path objectPath = parentPath.resolve(_path);
            String irodsParentPath = parentPath.toString();

            log_.debug("vfs::remove - Parent path: {}", irodsParentPath);

            IRODSFile pathFile = user.getIRODSAccessObjectFactory().getIRODSFileFactory(user.getRootAccount())
                .instanceIRODSFile(irodsParentPath, _path);

            pathFile.delete();
            user.unmap(resolvePath(objectPath), objectPath);
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
        log_.debug("vfs::setAcl");
        log_.debug("vfs::setAcl - _inode = {}", resolveInode(getInodeNumber(_inode)));
        log_.debug("vfs::setAcl - _acl   = {}", Arrays.asList(_acl));
    }

    @Override
    public void setattr(Inode _inode, Stat _stat) throws IOException
    {
        log_.debug("vfs::setattr");
        log_.debug("vfs::setattr - _inode = {}", resolveInode(getInodeNumber(_inode)));
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
                IRODSAccount acct = user.getRootAccount();
                Path path = resolveInode(getInodeNumber(_inode));
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
                IRODSFileFactory ff = aof.getIRODSFileFactory(user.getRootAccount());
                Path path = resolveInode(getInodeNumber(_inode));
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
        log_.debug("vfs::symlink");
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
            Path path = resolveInode(getInodeNumber(_inode));
            IRODSFileFactory fileFactory = aoFactory.getIRODSFileFactory(user.getRootAccount());
            IRODSFile file = fileFactory.instanceIRODSFile(path.toString());

            try (IRODSFileOutputStream fos = fileFactory.instanceIRODSFileOutputStream(file))
            {
                fos.write(_data, (int) _offset, _count);
                return new WriteResult(StabilityLevel.FILE_SYNC, _count); // TODO Need to revisit this
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

    /**
     * Get a stat relating to the given file path and inode number
     * 
     * @param _path
     *            {@link Path} of the file
     * @param _inodeNumber
     *            <code>long</code> with the inode number
     * @return {@link Stat} describing the file
     * @throws IOException
     */
    private Stat statPath(Path _path, long _inodeNumber) throws IOException
    {
        log_.debug("vfs::statPath");
        log_.debug("vfs::statPath - _inodeNumber      = {}", _inodeNumber);
        log_.debug("vfs::statPath - _path             = {}", _path);

        if (_path == null)
        {
            throw new IllegalArgumentException("null path");
        }

        String path = _path.normalize().toString();
        log_.debug("vfs::statPath - path (normalized) = {}", path);

        try
        {
            IRODSUser user = getCurrentIRODSUser();
            IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();
            CollectionAndDataObjectListAndSearchAO lao = null;
            lao = aof.getCollectionAndDataObjectListAndSearchAO(user.getRootAccount());
            ObjStat objStat = lao.retrieveObjectStatForPath(path);
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

            setStatMode(path, stat, objStat.getObjectType(), user);

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

    private Inode toFh(long _inodeNumber)
    {
        return Inode.forFile(Longs.toByteArray(_inodeNumber));
    }

    /**
     * Get the iRODS absolute path given the inode number
     * 
     * @param _inodeNumber
     *            <code>long</code> with the inode number
     * @return {@link Path} that is the inode
     * @throws NoEntException
     */
    private Path resolveInode(long _inodeNumber) throws NoEntException
    {
        IRODSUser user = getCurrentIRODSUser();
        Path path = user.getInodeToPathMap().get(_inodeNumber);

        if (path == null)
        {
            throw new NoEntException("inode number = " + _inodeNumber);
        }

        return path;
    }

    private long resolvePath(Path _path) throws NoEntException
    {
        IRODSUser user = getCurrentIRODSUser();
        Long inodeNumber = user.getPathToInodeMap().get(_path);

        if (inodeNumber == null)
        {
            throw new NoEntException("Path does not exist [" + _path + "].");
        }

        return inodeNumber;
    }

    private static void setStatMode(String _path, Stat _stat, ObjectType _objType, IRODSUser _user)
        throws JargonException
    {
        IRODSAccessObjectFactory aof = _user.getIRODSAccessObjectFactory();

        switch (_objType)
        {
            case COLLECTION:
                CollectionAO coa = aof.getCollectionAO(_user.getRootAccount());
                _stat.setMode(Stat.S_IFDIR | calcMode(coa.listPermissionsForCollection(_path)));
                break;

            case DATA_OBJECT:
                DataObjectAO doa = aof.getDataObjectAO(_user.getRootAccount());
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

    private long getInodeNumber(Inode _inode)
    {
        return Longs.fromByteArray(_inode.getFileId());
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
        user.getIRODSAccessObjectFactory().closeSessionAndEatExceptions(user.getRootAccount());
    }
}
