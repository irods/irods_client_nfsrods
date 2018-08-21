package org.irods.jargon.nfs.vfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;

import org.dcache.nfs.status.ExistException;
import org.dcache.nfs.status.NoEntException;
import org.dcache.nfs.status.NotSuppException;
import org.dcache.nfs.status.PermException;
import org.dcache.nfs.status.ServerFaultException;
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
import org.irods.jargon.core.pub.CollectionAndDataObjectListAndSearchAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystemAO;
import org.irods.jargon.core.pub.domain.ObjStat;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.pub.io.IRODSFileReader;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.primitives.Longs;

import jline.internal.Log;

public class IRODSVirtualFileSystem implements VirtualFileSystem
{
    private static final Logger log_ = LoggerFactory.getLogger(IRODSVirtualFileSystem.class);
    private static final ObjectMapper mapper_ = new ObjectMapper();

    static
    {
        mapper_.enable(SerializationFeature.INDENT_OUTPUT);
    }

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

        Log.info("IdMapping: " + idMapper_.toString());
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

        log_.debug("_mode (int) = {}", _mode);
        log_.debug("_mode       = {}", Stat.modeToString(_mode));

        long inodeNumber = getInodeNumber(_inode);
        Path path = resolveInode(inodeNumber);

        log_.debug("path = {}", path);

        Stat stat = statPath(path, inodeNumber);

        log_.debug("stat mode (int) = {}", stat.getMode());
        log_.debug("stat mode       = {}", Stat.modeToString(stat.getMode()));

        return _mode & stat.getMode();

        /*
         * if (true) { // TODO Should 'mode' be returned? // return mode;
         * 
         * // TODO Should the mode from stat be returned instead? // Either one appears
         * to work. long inodeNumber = getInodeNumber(inode); Path path =
         * resolveInode(inodeNumber); log.debug("path = {}", path); Stat stat =
         * statPath(path, inodeNumber); log.debug("correct mode (int) = {}",
         * stat.getMode()); log.debug("correct mode       = {}",
         * Stat.modeToString(stat.getMode())); return stat.getMode(); }
         * 
         * 
         * if (inode == null) { throw new IllegalArgumentException("null inode"); }
         * 
         * long inodeNumber = getInodeNumber(inode); // Throws NoEntException if not
         * resolved... Path path = resolveInode(inodeNumber);
         * 
         * log.debug("path: {}", path);
         * 
         * int returnMode = 0; try { IRODSFile pathFile =
         * irodsAccessObjectFactory.getIRODSFileFactory(resolveIrodsAccount())
         * .instanceIRODSFile(path.toString());
         * 
         * if (PermissionBitmaskUtils.isUserExecuteSet(mode)) {
         * log.debug("check user exec"); if (pathFile.canExecute()) {
         * log.debug("determine user can execute"); returnMode =
         * PermissionBitmaskUtils.turnOnUserExecute(returnMode); } }
         * 
         * boolean canWrite = false; if (PermissionBitmaskUtils.isUserWriteSet(mode)) {
         * log.debug("checking user write"); canWrite = pathFile.canWrite(); if
         * (canWrite) { log.debug("determine user can write"); returnMode =
         * PermissionBitmaskUtils.turnOnUserWrite(returnMode); } }
         * 
         * if (PermissionBitmaskUtils.isUserReadSet(mode)) {
         * log.debug("check user read"); if (canWrite) {
         * log.debug("user already determined to have write"); returnMode =
         * PermissionBitmaskUtils.turnOnUserRead(returnMode); } else if
         * (pathFile.canRead()) { log.debug("user can read"); returnMode =
         * PermissionBitmaskUtils.turnOnUserRead(returnMode); } }
         * 
         * log.debug("finished!"); return returnMode;
         * 
         * } catch (JargonException e) {
         * log.error("exception getting access for path:{}", path, e); throw new
         * IOException(e); } finally {
         * irodsAccessObjectFactory.closeSessionAndEatExceptions(); }
         */
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
        log_.debug("commit() is right now a noop, need to think about it");
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
            throw new IllegalArgumentException("null parent");
        }

        if (_type == null)
        {
            throw new IllegalArgumentException("null type");
        }

        if (_name == null)
        {
            throw new IllegalArgumentException("null name");
        }

        if (_subject == null)
        {
            throw new IllegalArgumentException("null subjet");
        }

        IRODSUser user = resolveIRODSUser(getUserID());

        log_.debug("parent: {}", _parent);
        log_.debug("type: {}", _type);
        log_.debug("name: {}", _name);
        log_.debug("subject: {}", _subject);
        log_.debug("mode: {}", _mode);

        long parentInodeNumber = getInodeNumber(_parent);
        Path parentPath = resolveInode(parentInodeNumber);
        Path newPath = parentPath.resolve(_name);

        try
        {
            IRODSAccessObjectFactory aoFactory = user.getIRODSAccessObjectFactory();
            IRODSFileFactory fileFactory = aoFactory.getIRODSFileFactory(user.getRootAccount());
            IRODSFile newFile = fileFactory.instanceIRODSFile(newPath.toString());

            log_.debug("creating new file at: {}", newFile);

            newFile.createNewFile();

            // Set ownership and permissions.
            long newInodeNumber = user.getAndIncFileID();
            user.map(newInodeNumber, newPath);

            return toFh(newInodeNumber);
        }
        catch (JargonException e)
        {
            throw new IOException(e);
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
        //IRODSUser user = resolveIRODSUser(getUserID());
        //FileStore store = Files.getFileStore(Paths.get(user.getAbsolutePath()));
        //long total = store.getTotalSpace();
        //long free = store.getUsableSpace();
        //return new FsStat(total, Long.MAX_VALUE, total - free, user.getPathToInode().size());
        return null;
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

    private Inode toFh(long _inodeNumber)
    {
        return Inode.forFile(Longs.toByteArray(_inodeNumber));
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
        long inodeNumber = getInodeNumber(_inode);
        Path path = resolveInode(inodeNumber);
        log_.debug("vfs::getattr - inode number = {}", inodeNumber);
        log_.debug("vfs::getattr - path         = {}", path);
        return statPath(path, inodeNumber);
    }

    @Override
    public boolean hasIOLayout(Inode _inode) throws IOException
    {
        log_.debug("vfs::hasIOLayout");
        System.out.println("hasIOLayout: " + _inode.toString());
        return false;
    }

    @Override
    public Inode link(Inode _parent, Inode _existing, String _target, Subject _subject) throws IOException
    {
        log_.debug("vfs::link");

        long parentInodeNumber = getInodeNumber(_parent);
        Path parentPath = resolveInode(parentInodeNumber);

        long existingInodeNumber = getInodeNumber(_existing);
        Path existingPath = resolveInode(existingInodeNumber);

        Path targetPath = parentPath.resolve(_target);

        try
        {
            Files.createLink(targetPath, existingPath);
        }
        catch (UnsupportedOperationException e)
        {
            throw new NotSuppException("Not supported", e);
        }
        catch (FileAlreadyExistsException e)
        {
            throw new ExistException("Path exists " + _target, e);
        }
        catch (SecurityException e)
        {
            throw new PermException("Permission denied: " + e.getMessage(), e);
        }
        catch (IOException e)
        {
            throw new ServerFaultException("Failed to create: " + e.getMessage(), e);
        }

        IRODSUser user = resolveIRODSUser(getUserID());

        long newInodeNumber = user.getAndIncFileID();
        user.map(newInodeNumber, targetPath);

        return toFh(newInodeNumber);
    }

    @Override
    public DirectoryStream list(Inode _inode, byte[] _verifier, long _cookie) throws IOException
    {
        log_.debug("vfs::list");

        final List<DirectoryEntry> list = new ArrayList<>();

        try
        {
            IRODSUser user = resolveIRODSUser(getUserID());
            CollectionAndDataObjectListAndSearchAO listAO = user.getIRODSAccessObjectFactory()
                .getCollectionAndDataObjectListAndSearchAO(user.getRootAccount());

            // get collection listing from root node
            Path parentPath = resolveInode(getInodeNumber(_inode));
            log_.debug("vfs::list - list contents of [{}] ...", parentPath);

            String irodsAbsPath = parentPath.normalize().toString();

            List<CollectionAndDataObjectListingEntry> entries;
            entries = listAO.listDataObjectsAndCollectionsUnderPath(irodsAbsPath);

            for (CollectionAndDataObjectListingEntry dataObj : entries)
            {
                Path filePath = parentPath.resolve(dataObj.getPathOrName());
                long inodeNumber;

                log_.debug("vfs::list - entry = {}", filePath);

                if (user.getInodeToPath().containsValue(filePath))
                {
                    inodeNumber = resolvePath(filePath);
                }
                else
                {
                    inodeNumber = user.getAndIncFileID();
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

        return new DirectoryStream(list);
    }

    private long resolvePath(Path path) throws NoEntException
    {
        IRODSUser user = resolveIRODSUser(getUserID());
        Long inodeNumber = user.getPathToInode().get(path);

        if (inodeNumber == null)
        {
            throw new NoEntException("path " + path);
        }

        return inodeNumber;
    }

    @Override
    public Inode lookup(Inode _parent, String _path) throws IOException
    {
        log_.debug("vfs::lookup");

        Path parentPath = resolveInode(getInodeNumber(_parent));
        Path filePath = parentPath.resolve(_path);

        log_.debug("looking up [{}] ...", filePath);

        return toFh(resolvePath(filePath));
    }

    @Override
    public Inode mkdir(Inode _inode, String _path, Subject _subject, int _mode) throws IOException
    {
        log_.debug("vfs::mkdir");

        try
        {
            Path parentPath = resolveInode(getInodeNumber(_inode));

            IRODSUser user = resolveIRODSUser(getUserID());
            IRODSFile irodsFile = user.getIRODSAccessObjectFactory().getIRODSFileFactory(user.getRootAccount())
                .instanceIRODSFile(parentPath.toString(), _path);

            log_.debug("vfs::mkdir - inode map (before creating new directory) = {}", mapper_.writeValueAsString(user
                .getInodeToPath()));
            log_.debug("vfs::mkdir - new directory path = {}", irodsFile.getAbsolutePath());

            irodsFile.mkdir();

            long inodeNumber = user.getAndIncFileID();
            log_.debug("vfs::mkdir - new inode number = {}", inodeNumber);

            user.map(inodeNumber, irodsFile.getAbsolutePath());

            return toFh(inodeNumber);
        }
        catch (JargonException e)
        {
            throw new IOException(e);
        }
    }

    @Override
    public boolean move(Inode _inode, String _oldName, Inode _dest, String _newName) throws IOException
    {
        log_.debug("vfs::move");
        log_.debug("vfs::move:: OldName: " + _oldName + "node: " + _inode.toString());
        log_.debug("vfs::move:: newName: " + _newName + "node: " + _dest.toString());

        try
        {
            // get file path
            Path parentPath = resolveInode(getInodeNumber(_inode));

            // get dest path
            Path destPath = resolveInode(getInodeNumber(_dest));

            // get Irods User
            IRODSUser user = resolveIRODSUser(getUserID());

            // create IRODSFile for file to move
            String irodsParentPath = parentPath.toString() + "/" + _oldName;
            log_.debug("parent path:{}", irodsParentPath);
            IRODSFile pathFile = user.getIRODSAccessObjectFactory().getIRODSFileFactory(user.getRootAccount())
                .instanceIRODSFile(irodsParentPath);

            // create empty destination file object
            String destPathString = "";

            // make path string for destination based on if rename occurs
            if (_newName != null && !_oldName.equals(_newName))
                destPathString = destPath.toString() + "/" + _newName;
            else
                destPathString = destPath.toString() + "/" + _oldName;

            log_.debug("vfs::move:: Destination Path: " + destPathString);
            // create irods destination file object
            IRODSFile destFile = user.getIRODSAccessObjectFactory().getIRODSFileFactory(user.getRootAccount())
                .instanceIRODSFile(destPathString);

            // get file system controls
            IRODSFileSystemAO fileSystemAO = user.getIRODSAccessObjectFactory().getIRODSFileSystemAO(user
                .getRootAccount());

            log_.debug("vfs::move:: is file? " + pathFile.isFile());

            // check if file or directory and run appropriate commands
            if (pathFile.isFile())
            {
                fileSystemAO.renameFile(pathFile, destFile);
            }
            else
            {
                fileSystemAO.renameDirectory(pathFile, destFile);
            }

            // TODO: Is remap needed when moving files iRODS side, or will list be regened
            // via list() call?
            Path oldPath = Paths.get(irodsParentPath);
            Path newPath = Paths.get(destPathString);
            log_.debug("VFS::Move: Old Path: " + oldPath);
            log_.debug("VFS::Move: new Path: " + newPath);
            log_.debug("VFS::Move: Inode #: " + user.getPathToInode().get(oldPath));
            user.remap(user.getPathToInode().get(oldPath), oldPath, newPath);

            return true;
        }
        catch (JargonException e)
        {
            log_.error("error during move", e);
            throw new IOException(e);
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

        long inodeNumber = getInodeNumber(_inode);
        Path path = resolveInode(inodeNumber);
        IRODSUser user = resolveIRODSUser(getUserID());
        IRODSAccessObjectFactory aoFactory = user.getIRODSAccessObjectFactory();

        try
        {
            IRODSFileFactory fileFactory = aoFactory.getIRODSFileFactory(user.getRootAccount());
            IRODSFileReader rdr = fileFactory.instanceIRODSFileReader(path.toString());

            char[] chars = new char[_count];
            while (rdr.read(chars) > -1);
            
            rdr.close();

            CharBuffer cBuf = CharBuffer.wrap(chars);
            ByteBuffer bBuf = Charset.forName("UTF-8").encode(cBuf);
            bBuf.get(_data, 0, _count);
        }
        catch (JargonException e)
        {
            throw new IOException(e);
        }

        //ByteBuffer destBuffer = ByteBuffer.wrap(data, 0, count);

        return -1;
    }

    @Override
    public String readlink(Inode _inode) throws IOException
    {
        log_.debug("vfs::readlink");

        // recursion woo, no idea what to do with this one.
        readlink(_inode);

        System.out.println("readlink: " + _inode.toString());
        return null;
    }

    @Override
    public void remove(Inode _parent, String _path) throws IOException
    {
        log_.debug("vfs::remove");

        try
        {
            // get Irods User
            IRODSUser user = resolveIRODSUser(getUserID());

            Path parentPath = resolveInode(getInodeNumber(_parent));
            Path objectPath = parentPath.resolve(_path);
            String irodsParentPath = parentPath.toString();

            log_.debug("parent path: {}", irodsParentPath);

            IRODSFile pathFile = user.getIRODSAccessObjectFactory().getIRODSFileFactory(user.getRootAccount())
                .instanceIRODSFile(irodsParentPath, _path);

            pathFile.delete();
            user.unmap(resolvePath(objectPath), objectPath);
        }
        catch (JargonException e)
        {
            log_.error("error during remove", e);
            throw new IOException(e);
        }
    }

    @Override
    public void setAcl(Inode _inode, nfsace4[] _acl) throws IOException
    {
        log_.debug("vfs::setAcl");
        System.out.println("setacl: " + _inode.toString());
    }

    @Override
    public void setattr(Inode _inode, Stat _stat) throws IOException
    {
        log_.debug("vfs::setattr");
        /*
         * long inodeNumber = getInodeNumber(inode); Path path =
         * resolveInode(inodeNumber); //PosixFileAttributeView attributeView =
         * Files.getFileAttributeView(path, PosixFileAttributeView.class,
         * NOFOLLOW_LINKS); if (stat.isDefined(Stat.StatAttribute.OWNER)) { try { String
         * uid = String.valueOf(stat.getUid()); UserPrincipal user =
         * _lookupService.lookupPrincipalByName(uid); attributeView.setOwner(user); }
         * catch (IOException e) { throw new
         * UnsupportedOperationException("set uid failed: " + e.getMessage()); } } if
         * (stat.isDefined(Stat.StatAttribute.GROUP)) { try { String gid =
         * String.valueOf(stat.getGid()); GroupPrincipal group =
         * _lookupService.lookupPrincipalByGroupName(gid);
         * attributeView.setGroup(group); } catch (IOException e) { throw new
         * UnsupportedOperationException("set gid failed: " + e.getMessage()); } } if
         * (stat.isDefined(Stat.StatAttribute.MODE)) { try { Files.setAttribute(path,
         * "unix:mode", stat.getMode(), NOFOLLOW_LINKS); } catch (IOException e) { throw
         * new UnsupportedOperationException("set mode unsupported: " + e.getMessage());
         * } } if (stat.isDefined(Stat.StatAttribute.SIZE)) { //little known fact -
         * truncate() returns the original channel //noinspection EmptyTryBlock try
         * (FileChannel ignored = FileChannel.open(path,
         * StandardOpenOption.WRITE).truncate(stat.getSize())) {} } if
         * (stat.isDefined(Stat.StatAttribute.ATIME)) { try { FileTime time =
         * FileTime.fromMillis(stat.getCTime()); Files.setAttribute(path,
         * "unix:lastAccessTime", time, NOFOLLOW_LINKS); } catch (IOException e) { throw
         * new UnsupportedOperationException("set atime failed: " + e.getMessage()); } }
         * if (stat.isDefined(Stat.StatAttribute.MTIME)) { try { FileTime time =
         * FileTime.fromMillis(stat.getMTime()); Files.setAttribute(path,
         * "unix:lastModifiedTime", time, NOFOLLOW_LINKS); } catch (IOException e) {
         * throw new UnsupportedOperationException("set mtime failed: " +
         * e.getMessage()); } } if (stat.isDefined(Stat.StatAttribute.CTIME)) { try {
         * FileTime time = FileTime.fromMillis(stat.getCTime());
         * Files.setAttribute(path, "unix:ctime", time, NOFOLLOW_LINKS); } catch
         * (IOException e) { throw new
         * UnsupportedOperationException("set ctime failed: " + e.getMessage()); } }
         */
    }

    @Override
    public Inode symlink(Inode _parent, String _linkName, String _targetName, Subject _subject, int _mode) throws IOException
    {
        log_.debug("vfs::symlink");
        return null;
    }

    @Override
    public WriteResult write(Inode _inode, byte[] _data, long _offset, int _count, StabilityLevel _stabilityLevel)
        throws IOException
    {
        log_.debug("vfs::write");
        return null;
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
        IRODSUser user = resolveIRODSUser(getUserID());
        Path path = user.getInodeToPath().get(_inodeNumber);

        if (path == null)
        {
            throw new NoEntException("inode #" + _inodeNumber);
        }

        return path;
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
        // get Irods User
        IRODSUser user = resolveIRODSUser(getUserID());
        log_.debug("vfs::statPath");
        log_.debug("vfs::statPath - inode number = {}", _inodeNumber);
        log_.debug("vfs::statPath - path         = {}", _path);

        if (_path == null)
        {
            throw new IllegalArgumentException("null path");
        }

        String irodsAbsPath = _path.normalize().toString();
        log_.debug("vfs::statPath - absolute path =  {}", irodsAbsPath);

        try
        {
            CollectionAndDataObjectListAndSearchAO listAO = user.getIRODSAccessObjectFactory()
                .getCollectionAndDataObjectListAndSearchAO(user.getRootAccount());
            ObjStat objStat = listAO.retrieveObjectStatForPath(irodsAbsPath);
            log_.debug("vfs::statPath - objStat = {}", objStat);

            Stat stat = new Stat();

            stat.setATime(objStat.getModifiedAt().getTime());
            stat.setCTime(objStat.getCreatedAt().getTime());
            stat.setMTime(objStat.getModifiedAt().getTime());

            // UserAO userAO = user.getIrodsAccessObjectFactory().getUserAO(user.getRootAccount());
            StringBuilder sb = new StringBuilder();
            sb.append(objStat.getOwnerName());
            sb.append("#");
            sb.append(objStat.getOwnerZone());

            // Set User stats
            stat.setUid(getUserID());
            stat.setGid(getUserID()); // iRODS does not have a gid
            log_.debug("vfs::statPath - user id = {}", getUserID());

            // TODO right now don't have soft link or mode support
            // stat.setMode(PermissionBitmaskUtils.USER_READ |
            // PermissionBitmaskUtils.USER_WRITE);

            // Set file type and permissions.
            switch (objStat.getObjectType())
            {
                case COLLECTION:
                    stat.setMode(Stat.S_IFDIR | 0777);
                    break;

                case DATA_OBJECT:
                    stat.setMode(Stat.S_IFREG | 0666);
                    break;

                // TODO What is this?
                case COLLECTION_HEURISTIC_STANDIN:
                    stat.setMode(Stat.S_IFDIR | 0777);
                    break;

                case LOCAL_DIR:
                    break;

                case LOCAL_FILE:
                    break;

                case NO_INPUT:
                    break;

                case UNKNOWN:
                    break;

                case UNKNOWN_FILE:
                    break;

                default:
                    break;
            }

            log_.debug("vfs::statPath - permissions = {}", Stat.modeToString(stat.getMode()));

            stat.setNlink(1);
            stat.setDev(17);
            stat.setIno((int) _inodeNumber);
            stat.setRdev(17);
            stat.setSize(objStat.getObjSize());
            stat.setFileid((int) _inodeNumber);
            stat.setGeneration(objStat.getModifiedAt().getTime());

            log_.debug("vfs::statPath - stat = {}", stat);

            return stat;
        }
        catch (NumberFormatException | JargonException e)
        {
            log_.error("exception getting stat for path: {}", _path, e);
            throw new IOException(e);
        }
    }

    private long getInodeNumber(Inode _inode)
    {
        return Longs.fromByteArray(_inode.getFileId());
    }

    /*
     * 
     * Irods file/collection is owned by first person who makes it - unless
     * inheritance is on. in this case, all permissions transfer to data inside it
     */
    /**
     * Stand-in for a method to return the current user or proxy as a given
     * user...not sure yet how the principal is resolved
     * 
     * @return
     */
    private IRODSUser resolveIRODSUser(int _userID)
    {
        return idMapper_.resolveUser(_userID);
    }

    private int getUserID()
    {
        Subject subject = Subject.getSubject(AccessController.getContext());
        String name = subject.getPrincipals().iterator().next().getName();
        return Integer.parseInt(name);
    }

}
