package org.irods.nfsrods.vfs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.irods.jargon.core.pub.UserAO;
import org.irods.jargon.core.pub.domain.ObjStat;
import org.irods.jargon.core.pub.domain.User;
import org.irods.jargon.core.pub.domain.UserFilePermission;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.pub.io.IRODSFileInputStream;
import org.irods.jargon.core.pub.io.IRODSFileOutputStream;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry.ObjectType;
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

        log_.debug("vfs::create - _parent  = {}", parentPath);
        log_.debug("vfs::create - _type    = {}", _type);
        log_.debug("vfs::create - _name    = {}", _name);
        log_.debug("vfs::create - _subject = {}", _subject);
        log_.debug("vfs::create - _mode    = {}", Stat.modeToString(_mode));

        try
        {
            IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();
            IRODSFileFactory ff = aof.getIRODSFileFactory(user.getAccount());
            IRODSFile newFile = ff.instanceIRODSFile(newPath.toString());

            log_.debug("vfs::create - Creating new file at: {}", newFile);

            try (AutoClosedIRODSFile ac = new AutoClosedIRODSFile(newFile))
            {
                if (!newFile.createNewFile())
                {
                    throw new IOException("Failed to create new file in iRODS");
                }
            }

            long newInodeNumber = user.getAndIncrementFileID();
            user.map(newInodeNumber, newPath);

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

        log_.debug("vfs::getattr - _inode = {}", path);

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

        log_.debug("vfs::lookup - _path   = {}", _path);
        log_.debug("vfs::lookup - _parent = {}", parentPath);
        log_.debug("vfs::lookup - Looking up [{}] ...", targetPath);

        IRODSUser user = getCurrentIRODSUser();

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

            // If the target path is valid, then return an inode object created from
            // the user's mapped paths. Else, create a new mapping and return an
            // inode object for the new mapping.
            if (isTargetValid)
            {
                if (user.getPathToInodeMap().containsKey(targetPath))
                {
                    return toFh(getInodeNumber(targetPath));
                }

                long newInodeNumber = user.getAndIncrementFileID();
                user.map(newInodeNumber, targetPath);
                return toFh(newInodeNumber);
            }

            // If the target path is not registered in iRODS and NFSRODS has previously
            // mapped it, then unmap it. This keeps NFSRODS in sync with iRODS.
            if (user.getPathToInodeMap().containsKey(targetPath))
            {
                user.unmap(getInodeNumber(targetPath), targetPath);
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

            IRODSUser user = getCurrentIRODSUser();
            IRODSFile file = user.getIRODSAccessObjectFactory().getIRODSFileFactory(user.getAccount())
                .instanceIRODSFile(parentPath.toString(), _path);

            file.mkdir();
            file.close();

            long inodeNumber = user.getAndIncrementFileID();

            user.map(inodeNumber, file.getAbsolutePath());

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
        log_.debug("vfs::move - _inode   = {}", _inode);
        log_.debug("vfs::move - _dest    = {}", _dest);
        log_.debug("vfs::move - _oldName = {}", _oldName);
        log_.debug("vfs::move - _newName = {}", _newName);

        IRODSUser user = getCurrentIRODSUser();
        IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();

        try
        {
            Path parentPath = getPath(toInodeNumber(_inode));
            Path destPath = getPath(toInodeNumber(_dest));
            String irodsParentPath = parentPath.toString() + "/" + _oldName;

            log_.debug("vfs::move - Parent path = {}", irodsParentPath);

            IRODSFileFactory ff = aof.getIRODSFileFactory(user.getAccount());
            String destPathString = null;

            if (_newName != null && !_oldName.equals(_newName))
            {
                destPathString = destPath.toString() + "/" + _newName;
            }
            else
            {
                destPathString = destPath.toString() + "/" + _oldName;
            }

            log_.debug("vfs::move - Destination path = {}", destPathString);

            IRODSFile pathFile = ff.instanceIRODSFile(irodsParentPath);
            IRODSFile destFile = ff.instanceIRODSFile(destPathString);

            try (AutoClosedIRODSFile ac0 = new AutoClosedIRODSFile(pathFile);
                 AutoClosedIRODSFile ac1 = new AutoClosedIRODSFile(destFile))
            {
                IRODSFileSystemAO fsao = aof.getIRODSFileSystemAO(user.getAccount());

                log_.debug("vfs::move - Is file? {}", pathFile.isFile());

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

            log_.debug("vfs::move - Old path     = {}" + oldPath);
            log_.debug("vfs::move - New path     = {}" + newPath);
            log_.debug("vfs::move - Inode number = {}" + user.getPathToInodeMap().get(oldPath));

            user.remap(user.getPathToInodeMap().get(oldPath), oldPath, newPath);

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
        log_.debug("vfs::read - _offset = {}", _offset);
        log_.debug("vfs::read - _count  = {}", _count);

        IRODSUser user = getCurrentIRODSUser();
        IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();

        try
        {
            Path path = getPath(toInodeNumber(_inode));
            IRODSFileFactory ff = aof.getIRODSFileFactory(user.getAccount());
            IRODSFile file = ff.instanceIRODSFile(path.toString());

            try (AutoClosedIRODSFile ac = new AutoClosedIRODSFile(file);
                 IRODSFileInputStream fis = ff.instanceIRODSFileInputStream(file))
            {
                return fis.read(_data, (int) _offset, _count);
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
        log_.debug("vfs::remove - _parent = {}", getPath(toInodeNumber(_parent)));
        log_.debug("vfs::remove - _path   = {}", _path);

        IRODSUser user = getCurrentIRODSUser();
        IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();

        try
        {
            Path parentPath = getPath(toInodeNumber(_parent));
            Path objectPath = parentPath.resolve(_path);
            IRODSFileFactory ff = aof.getIRODSFileFactory(user.getAccount());
            IRODSFile file = ff.instanceIRODSFile(parentPath.toString(), _path);

            log_.debug("vfs::remove - Removing object ...");

            try (AutoClosedIRODSFile ac = new AutoClosedIRODSFile(file))
            {
                if (!file.delete())
                {
                    throw new IOException("Failed to delete object in iRODS");
                }
            }

            user.unmap(getInodeNumber(objectPath), objectPath);

            log_.debug("vfs::remove - Object removed.");
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

    @Override
    public void setattr(Inode _inode, Stat _stat) throws IOException
    {
        log_.debug("vfs::setattr");
        log_.debug("vfs::setattr - _inode = {}", getPath(toInodeNumber(_inode)));
        log_.debug("vfs::setattr - _stat  = {}", _stat);

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
                FilePermissionEnum perm = null;

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
                throw new IOException(e);
            }
            finally
            {
                closeCurrentConnection();
            }
        }

        if (_stat.isDefined(Stat.StatAttribute.SIZE))
        {
            log_.debug("vfs::setattr - New size = {}", _stat.getSize());

            IRODSUser user = getCurrentIRODSUser();
            IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();

            try
            {
                IRODSFileFactory ff = aof.getIRODSFileFactory(user.getAccount());
                Path path = getPath(toInodeNumber(_inode));
                IRODSFile file = ff.instanceIRODSFile(path.toString());
                
                // TODO Data should be streamed instead of copied into a buffer.
                // Very large files could break this sensitive code.
                byte[] bytes = new byte[(int) _stat.getSize()];

                try (AutoClosedIRODSFile ac = new AutoClosedIRODSFile(file))
                {
                    // Increase the size of the data object.
                    if (_stat.getSize() >= file.length())
                    {
                        try (IRODSFileOutputStream fos = ff.instanceIRODSFileOutputStream(file, OpenFlags.WRITE))
                        {
                            fos.write(bytes, 0, (int) _stat.getSize());
                        }
                    }
                    else // Truncate the size of the data object.
                    {
                        try (IRODSFileInputStream fis = ff.instanceIRODSFileInputStream(file))
                        {
                            fis.read(bytes);
                        }

                        // @formatter:off
                        try (IRODSFileOutputStream fos = ff.instanceIRODSFileOutputStream(file, OpenFlags.WRITE_TRUNCATE))
                        // @formatter:on
                        {
                            fos.write(bytes, 0, (int) _stat.getSize());
                        }
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
        log_.debug("vfs::write - _offset = {}", _offset);
        log_.debug("vfs::write - _count  = {}", _count);

        IRODSUser user = getCurrentIRODSUser();
        IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();

        try
        {
            Path path = getPath(toInodeNumber(_inode));
            IRODSFileFactory ff = aof.getIRODSFileFactory(user.getAccount());
            IRODSFile file = ff.instanceIRODSFile(path.toString());

            try (AutoClosedIRODSFile ac = new AutoClosedIRODSFile(file);
                 IRODSFileOutputStream fos = ff.instanceIRODSFileOutputStream(file))
            {
                fos.write(_data, (int) _offset, _count);
                return new WriteResult(_stabilityLevel, _count);
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
        log_.debug("vfs::statPath");
        log_.debug("vfs::statPath - _inodeNumber      = {}", _inodeNumber);
        log_.debug("vfs::statPath - _path             = {}", _path);

        IRODSUser user = getCurrentIRODSUser();

        try
        {
            IRODSAccessObjectFactory aof = user.getIRODSAccessObjectFactory();
            CollectionAndDataObjectListAndSearchAO lao = null;
            lao = aof.getCollectionAndDataObjectListAndSearchAO(user.getAccount());
            ObjStat objStat = lao.retrieveObjectStatForPath(_path.toString());
            log_.debug("vfs::statPath - iRODS stat info = {}", objStat);

            Stat stat = new Stat();

            stat.setATime(objStat.getModifiedAt().getTime());
            stat.setCTime(objStat.getCreatedAt().getTime());
            stat.setMTime(objStat.getModifiedAt().getTime());

            setStatMode(_path.toString(), stat, objStat.getObjectType(), user);

            UserAO uao = aof.getUserAO(user.getAccount());
            int ownerId = getUserID();

            log_.debug("vfs::statPath - Owner name  = {}", objStat.getOwnerName());

            if (objStat.getOwnerName() != null && !objStat.getOwnerName().isEmpty())
            {
                User iuser = uao.findByName(objStat.getOwnerName());
                ownerId = Integer.parseInt(iuser.getId());
            }

            stat.setUid(ownerId);
            stat.setGid(ownerId);
            stat.setNlink(1);
            stat.setDev(17);
            stat.setIno((int) _inodeNumber);
            // stat.setRdev(17);
            stat.setRdev(0);
            stat.setSize(objStat.getObjSize());
            stat.setFileid((int) _inodeNumber);
            stat.setGeneration(objStat.getModifiedAt().getTime());

            log_.debug("vfs::statPath - Owner ID    = {}", ownerId);
            log_.debug("vfs::statPath - Permissions = {}", Stat.modeToString(stat.getMode()));
            log_.debug("vfs::statPath - Stat        = {}", stat);

            return stat;
        }
        catch (NumberFormatException | JargonException e)
        {
            log_.error(e.getMessage());
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
            throw new NoEntException("Path does not exist for [" + _inodeNumber + "]");
        }

        return path;
    }

    private long getInodeNumber(Path _path) throws NoEntException
    {
        IRODSUser user = getCurrentIRODSUser();
        Long inodeNumber = user.getPathToInodeMap().get(_path);

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

    private static void setStatMode(String _path, Stat _stat, ObjectType _objType, IRODSUser _user)
        throws JargonException
    {
        IRODSAccessObjectFactory aof = _user.getIRODSAccessObjectFactory();

        switch (_objType)
        {
            case COLLECTION:
                CollectionAO coa = aof.getCollectionAO(_user.getAccount());
                _stat.setMode(Stat.S_IFDIR | (0055 | calcMode(coa.listPermissionsForCollection(_path))));
                break;

            case DATA_OBJECT:
                DataObjectAO doa = aof.getDataObjectAO(_user.getAccount());
                // ~0111 is needed to unset the execute bits. The parentheses aren't needed
                // really, but they help to emphasize what bits we are interested in.
                _stat.setMode(Stat.S_IFREG | (~0111 & calcMode(doa.listPermissionsForDataObject(_path))));
                break;

            // This object type comes from the Jargon library.
            // It is encountered when the user accessing iRODS is not a rodsadmin.
            case COLLECTION_HEURISTIC_STANDIN:
                _stat.setMode(Stat.S_IFDIR | 0777);
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

    private IRODSUser getCurrentIRODSUser()
    {
        return idMapper_.resolveUser(getUserID());
    }

    private void closeCurrentConnection()
    {
        IRODSUser user = getCurrentIRODSUser();
        user.getIRODSAccessObjectFactory().closeSessionAndEatExceptions(user.getAccount());
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
}
