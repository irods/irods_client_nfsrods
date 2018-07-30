/**
 * 
 */
package org.irods.jargon.nfs.vfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.security.auth.Subject;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.dcache.nfs.status.ExistException;
import org.dcache.nfs.status.NoEntException;
import org.dcache.nfs.status.NotSuppException;
import org.dcache.nfs.status.PermException;
import org.dcache.nfs.status.ServerFaultException;
import org.dcache.nfs.v4.NfsIdMapping;
import org.dcache.nfs.v4.SimpleIdMap;
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
import org.irods.jargon.core.pub.UserAO;
import org.irods.jargon.core.pub.domain.ObjStat;
import org.irods.jargon.core.pub.domain.User;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.irods.jargon.nfs.vfs.utils.PermissionBitmaskUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.primitives.Longs;
import java.security.AccessController;
import jline.internal.Log;
import org.irods.jargon.core.pub.domain.Collection;
import org.irods.jargon.core.pub.domain.DataObject;
import org.irods.jargon.core.pub.domain.UserFilePermission;

/**
 * iRODS implmentation of the nfs4j virtual file system
 * 
 * @author Mike Conway - NIEHS
 *
 */
public class IrodsVirtualFileSystem implements VirtualFileSystem
{
    private static final Logger log = LoggerFactory.getLogger(IrodsVirtualFileSystem.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    static
    {
    	mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private final IRODSAccessObjectFactory irodsAccessObjectFactory;
    private final IRODSAccount rootAccount;
    private final IRODSFile root;
    private final NonBlockingHashMapLong<Path> inodeToPath = new NonBlockingHashMapLong<>();
    private final NonBlockingHashMap<Path, Long> pathToInode = new NonBlockingHashMap<>();
    private final AtomicLong fileId = new AtomicLong(1); // numbering starts at 1
    private final NfsIdMapping _idMapper;

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
    public IrodsVirtualFileSystem(IRODSAccessObjectFactory _irodsAccessObjectFactory,
                                  IRODSAccount _rootAccount,
                                  IRODSFile _root, IrodsIdMap idMapper) throws DataNotFoundException, JargonException
    {
        super(); // This is probably not needed.

        if (_irodsAccessObjectFactory == null)
        {
            throw new IllegalArgumentException("null irodsAccessObjectFactory");
        }

        if (_rootAccount == null)
        {
            throw new IllegalArgumentException("null rootAccount");
        }

        if (_root == null)
        {
            throw new IllegalArgumentException("null root");
        }
        
        
        _idMapper = idMapper;
        irodsAccessObjectFactory = _irodsAccessObjectFactory;
        rootAccount = _rootAccount;
        root = _root;
        
        Log.info("IdMapping: " + _idMapper.toString());
        
        establishRoot();
    }

    private void establishRoot() throws DataNotFoundException, JargonException
    {
        log.debug("establish root at: {}", root);

        try
        {
            if (root.exists() && root.canRead())
            {
                log.debug("root is valid");
            }
            else
            {
                throw new DataNotFoundException("cannot establish root at:" + root);
            }

            log.debug("mapping root...");
            map(fileId.getAndIncrement(), root.getAbsolutePath()); // so root is always inode #1
        }
        finally
        {
            irodsAccessObjectFactory.closeSessionAndEatExceptions();
        }
    }

    /**
     * Check access to file system object.
     * <p/>
     * This method will honor the read/write/execute bits for the user mode and
     * ignore others TODO: don't know if this even makes sense
     *
     * @param inode
     *            inode of the object to check.
     * @param mode
     *            a mask of permission bits to check.
     * @return an allowed subset of permissions from the given mask.
     * @throws IOException
     */
    @Override
    public int access(Inode inode, int mode) throws IOException
    {
        log.debug("vfs::access");
        
        log.debug("mode (int) = {}", mode);
        log.debug("mode       = {}", Stat.modeToString(mode));
        
        if (true)
        {
        	// TODO Should 'mode' be returned?
//        	return mode;
        	
        	// TODO Should the mode from stat be returned instead?
        	// Either one appears to work.
            long inodeNumber = getInodeNumber(inode);
            Path path = resolveInode(inodeNumber);
            log.debug("path = {}", path);
            Stat stat = statPath(path, inodeNumber);
            log.debug("correct mode (int) = {}", stat.getMode());
            log.debug("correct mode       = {}", Stat.modeToString(stat.getMode()));
            return stat.getMode();
        }

        if (inode == null)
        {
            throw new IllegalArgumentException("null inode");
        }

        long inodeNumber = getInodeNumber(inode);
        // Throws NoEntException if not resolved...
        Path path = resolveInode(inodeNumber);

        log.debug("path: {}", path);

        int returnMode = 0;
        try
        {
            IRODSFile pathFile = irodsAccessObjectFactory.getIRODSFileFactory(resolveIrodsAccount())
                .instanceIRODSFile(path.toString());

            if (PermissionBitmaskUtils.isUserExecuteSet(mode))
            {
                log.debug("check user exec");
                if (pathFile.canExecute())
                {
                    log.debug("determine user can execute");
                    returnMode = PermissionBitmaskUtils.turnOnUserExecute(returnMode);
                }
            }

            boolean canWrite = false;
            if (PermissionBitmaskUtils.isUserWriteSet(mode))
            {
                log.debug("checking user write");
                canWrite = pathFile.canWrite();
                if (canWrite)
                {
                    log.debug("determine user can write");
                    returnMode = PermissionBitmaskUtils.turnOnUserWrite(returnMode);
                }
            }

            if (PermissionBitmaskUtils.isUserReadSet(mode))
            {
                log.debug("check user read");
                if (canWrite)
                {
                    log.debug("user already determined to have write");
                    returnMode = PermissionBitmaskUtils.turnOnUserRead(returnMode);
                }
                else if (pathFile.canRead())
                {
                    log.debug("user can read");
                    returnMode = PermissionBitmaskUtils.turnOnUserRead(returnMode);
                }
            }

            log.debug("finished!");
            return returnMode;

        }
        catch (JargonException e)
        {
            log.error("exception getting access for path:{}", path, e);
            throw new IOException(e);
        }
        finally
        {
            irodsAccessObjectFactory.closeSessionAndEatExceptions();
        }
    }

    /**
     * Flush data in {@code dirty} state to the stable storage. Typically follows
     * {@link #write()} operation.
     *
     * @param inode
     *            inode of the file to commit.
     * @param offset
     *            the file position to start commit at.
     * @param count
     *            number of bytes to commit.
     * @throws IOException
     */
    @Override
    public void commit(Inode inode, long offset, int count) throws IOException
    {
        log.debug("vfs::commit");
        log.debug("commit() is right now a noop, need to think about it");
    }

    /**
     * Create a new object in a given directory with a specific name.
     *
     * @param parent
     *            directory where new object must be created.
     * @param type
     *            the type of the object to be created.
     * @param name
     *            name of the object.
     * @param subject
     *            the owner subject of a newly created object.
     * @param mode
     *            initial permission mask.
     * @return the inode of the newly created object.
     * @throws IOException
     */
    @Override
    public Inode create(Inode parent, Type type, String name, Subject subject, int mode) throws IOException
    {
        log.debug("vfs::create");

        if (parent == null)
        {
            throw new IllegalArgumentException("null parent");
        }

        if (type == null)
        {
            throw new IllegalArgumentException("null type");
        }

        if (name == null)
        {
            throw new IllegalArgumentException("null name");
        }

        if (subject == null)
        {
            throw new IllegalArgumentException("null subjet");
        }

        log.debug("parent: {}", parent);
        log.debug("type: {}", type);
        log.debug("name: {}", name);
        log.debug("subject: {}", subject);
        log.debug("mode: {}", mode);

        long parentInodeNumber = getInodeNumber(parent);
        Path parentPath = resolveInode(parentInodeNumber);
        Path newPath = parentPath.resolve(name);

        try
        {
            IRODSFileFactory irodsFileFactory = irodsAccessObjectFactory
                    .getIRODSFileFactory(resolveIrodsAccount());
            IRODSFile newFile = irodsFileFactory.instanceIRODSFile(newPath.toString());
            log.debug("creating new file at: {}", newFile);
            newFile.createNewFile();
            long newInodeNumber = fileId.getAndIncrement();
            map(newInodeNumber, newPath);
//            setOwnershipAndMode(newPath, subject, mode);
            return toFh(newInodeNumber);

        }
        catch (JargonException e)
        {
            log.error("error creating new file at path: {}", newPath, e);
            throw new IOException("exception creating new file", e);
        }
    }

    @Override
    public byte[] directoryVerifier(Inode inode) throws IOException
    {
        log.debug("vfs::directoryVerifier");
        return null;
    }

//    private void setOwnershipAndMode(Path newPath, Subject subject, int mode)
//    {
//        log.debug("setOwnershipAndMode()"); // TODO: right now a noop
//    }

    @Override
    public nfsace4[] getAcl(Inode inode) throws IOException
    {
        log.debug("vfs::getAcl");
        // info on nfsace4:
        // https://www.ibm.com/support/knowledgecenter/en/ssw_aix_61/com.ibm.aix.osdevice/acl_type_nfs4.htm
        return new nfsace4[0]; // TODO: this is same in local need to look at what nfsace4 is composed of
    }

    @Override
    public AclCheckable getAclCheckable()
    {
        log.debug("vfs::getAclCheckable");
        return AclCheckable.UNDEFINED_ALL;
    }

    @Override
    public FsStat getFsStat() throws IOException
    {
        log.debug("vfs::getFsStat");
        FileStore store = Files.getFileStore(Paths.get(root.getAbsolutePath()));
        long total = store.getTotalSpace();
        long free = store.getUsableSpace();
        return new FsStat(total, Long.MAX_VALUE, total - free, pathToInode.size());
    }

    @Override
    public NfsIdMapping getIdMapper()
    {
        log.debug("vfs::getIdMapper");
        return _idMapper;
    }

    @Override
    public Inode getRootInode() throws IOException
    {
        log.debug("vfs::getRootInode");
        return toFh(1); // always #1 (see constructor)
    }

    private Inode toFh(long inodeNumber)
    {
        return Inode.forFile(Longs.toByteArray(inodeNumber));
    }

    /**
     * Get file system object's attributes.
     *
     * @param inode
     *            inode of the file system object.
     * @return {@link Stat} with file's attributes.
     * @throws IOException
     */
    @Override
    public Stat getattr(Inode inode) throws IOException
    {
        log.debug("vfs::getattr");
        long inodeNumber = getInodeNumber(inode);
        Path path = resolveInode(inodeNumber);
        log.debug("vfs::getattr - inode number = {}", inodeNumber);
        log.debug("vfs::getattr - path         = {}", path);
        return statPath(path, inodeNumber);
    }

    @Override
    public boolean hasIOLayout(Inode inode) throws IOException
    {
        log.debug("vfs::hasIOLayout");
        System.out.println("hasIOLayout: " + inode.toString());
        return false;
    }

    @Override
    public Inode link(Inode parent, Inode existing, String target, Subject subject) throws IOException
    {
        log.debug("vfs::link");

        long parentInodeNumber = getInodeNumber(parent);
        Path parentPath = resolveInode(parentInodeNumber);

        long existingInodeNumber = getInodeNumber(existing);
        Path existingPath = resolveInode(existingInodeNumber);

        Path targetPath = parentPath.resolve(target);

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
            throw new ExistException("Path exists " + target, e);
        }
        catch (SecurityException e)
        {
            throw new PermException("Permission denied: " + e.getMessage(), e);
        }
        catch (IOException e)
        {
            throw new ServerFaultException("Failed to create: " + e.getMessage(), e);
        }

        long newInodeNumber = fileId.getAndIncrement();
        map(newInodeNumber, targetPath);
        return toFh(newInodeNumber);
    }

    @Override
    public DirectoryStream list(Inode _inode, byte[] _verifier, long _cookie) throws IOException
    {
        log.debug("vfs::list");

        final List<DirectoryEntry> list = new ArrayList<>();

        try
        {
            CollectionAndDataObjectListAndSearchAO listAO = irodsAccessObjectFactory
                .getCollectionAndDataObjectListAndSearchAO(resolveIrodsAccount());

            // get collection listing from root node
            Path parentPath = resolveInode(getInodeNumber(_inode));
            log.debug("vfs::list - list contents of [{}] ...", parentPath);

            String irodsAbsPath = parentPath.normalize().toString();

            List<CollectionAndDataObjectListingEntry> entries = listAO
                .listDataObjectsAndCollectionsUnderPath(irodsAbsPath);

            for (final CollectionAndDataObjectListingEntry dataObj : entries)
            {
                Path filePath = parentPath.resolve(dataObj.getPathOrName());
                long inodeNumber = -1;
                
                log.debug("vfs::list - entry = {}", filePath);

                if (inodeToPath.containsValue(filePath))
                {
                    inodeNumber = resolvePath(filePath);
                }
                else
                {
                    inodeNumber = fileId.getAndIncrement();
                    map(inodeNumber, filePath);
                }

                Stat stat = statPath(filePath, inodeNumber);
                list.add(new DirectoryEntry(filePath.getFileName().toString(), toFh(inodeNumber), stat, inodeNumber));
            }
        }
        catch (JargonException e)
        {
            log.error("error during list", e);
            throw new IOException(e);
        }
        finally
        {
            irodsAccessObjectFactory.closeSessionAndEatExceptions();
        }

        return new DirectoryStream(list);
    }

    private long resolvePath(Path path) throws NoEntException
    {
        Long inodeNumber = pathToInode.get(path);
        if (inodeNumber == null)
        {
            throw new NoEntException("path " + path);
        }
        return inodeNumber;
    }

    @Override
    public Inode lookup(Inode _parent, String _path) throws IOException
    {
        log.debug("vfs::lookup");
        
        Path parentPath = resolveInode(getInodeNumber(_parent));
        Path filePath = parentPath.resolve(_path);
        
        log.debug("looking up [{}] ...", filePath);
        
        return toFh(resolvePath(filePath));
    }

    @Override
    public Inode mkdir(Inode _inode, String _path, Subject _subject, int _mode) throws IOException
    {
        log.debug("vfs::mkdir");

        try
        {
            Path parentPath = resolveInode(getInodeNumber(_inode));

            IRODSFileFactory fileFactory = irodsAccessObjectFactory.getIRODSFileFactory(resolveIrodsAccount());
            IRODSFile irodsFile = fileFactory.instanceIRODSFile(parentPath.toString(), _path);

            log.debug("vfs::mkdir - inode map (before creating new directory) = {}", mapper.writeValueAsString(inodeToPath));
            log.debug("vfs::mkdir - new directory path = {}", irodsFile.getAbsolutePath());
            irodsFile.mkdir();

            long inodeNumber = fileId.getAndIncrement();
            log.debug("vfs::mkdir - new inode number = {}", inodeNumber);

            map(inodeNumber, irodsFile.getAbsolutePath());

            return toFh(inodeNumber);
        }
        catch (JargonException e)
        {
            log.error("exception making directory", e);
            throw new IOException(e);
        }
        finally
        {
            irodsAccessObjectFactory.closeSessionAndEatExceptions();
        }
    }

    @Override
    public boolean move(Inode inode, String oldName, Inode dest, String newName) throws IOException
    {
        log.debug("vfs::move");
        log.debug("vfs::move:: OldName: "+ oldName + "node: " + inode.toString());
        log.debug("vfs::move:: newName: "+ newName + "node: " + dest.toString());

        try
        {
            // get file path
            Path parentPath = resolveInode(getInodeNumber(inode));

            // get dest path
            Path destPath = resolveInode(getInodeNumber(dest));

            // create IRODSFile for file to move
            String irodsParentPath = parentPath.toString()+"/"+oldName;
            log.debug("parent path:{}", irodsParentPath);
            IRODSFile pathFile = irodsAccessObjectFactory.getIRODSFileFactory(resolveIrodsAccount())
                .instanceIRODSFile(irodsParentPath);

            // create empty destination file object
            String destPathString = "";

            // make path string for destination based on if rename occurs
            if (newName != null && !oldName.equals(newName))
                destPathString = destPath.toString() + "/" + newName;
            else
                destPathString = destPath.toString() + "/" + oldName;
            
            log.debug("vfs::move:: Destination Path: "+ destPathString);
            // create irods destination file object
            IRODSFile destFile = irodsAccessObjectFactory.getIRODSFileFactory(resolveIrodsAccount())
                    .instanceIRODSFile(destPathString);

            // get file system controls
            IRODSFileSystemAO fileSystemAO = irodsAccessObjectFactory.getIRODSFileSystemAO(rootAccount);
            
            log.debug("vfs::move:: is file? "+ pathFile.isFile());

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
            log.debug("VFS::Move: Old Path: "+ oldPath);
            log.debug("VFS::Move: new Path: "+ newPath);
            log.debug("VFS::Move: Inode #: "+ pathToInode.get(oldPath));
            remap(pathToInode.get(oldPath), oldPath, newPath);

            return true;
        }
        catch (JargonException e)
        {
            log.error("error during move", e);
            throw new IOException(e);
        }
        finally
        {
            irodsAccessObjectFactory.closeSessionAndEatExceptions();
        }
    }

    @Override
    public Inode parentOf(Inode _inode) throws IOException
    {
        log.debug("vfs::parentOf");
        Path path = resolveInode(getInodeNumber(_inode));
        return toFh(resolvePath(path.getParent()));
    }

    @Override
    public int read(Inode inode, byte[] data, long offset, int count) throws IOException
    {
        log.debug("vfs::read");
        long inodeNumber = getInodeNumber(inode);
        Path path = resolveInode(inodeNumber);
        ByteBuffer destBuffer = ByteBuffer.wrap(data, 0, count);
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ))
        {
            return channel.read(destBuffer, offset);
        }
    }

    @Override
    public String readlink(Inode inode) throws IOException
    {
        log.debug("vfs::readlink");

        // recursion woo, no idea what to do with this one.
        readlink(inode);

        System.out.println("readlink: " + inode.toString());
        return null;
    }

    @Override
    public void remove(Inode parent, String path) throws IOException
    {
        log.debug("vfs::remove");

        try
        {
            Path parentPath = resolveInode(getInodeNumber(parent));
            Path objectPath = parentPath.resolve(path);
            String irodsParentPath = parentPath.toString();

            log.debug("parent path: {}", irodsParentPath);

            IRODSFile pathFile = irodsAccessObjectFactory.getIRODSFileFactory(resolveIrodsAccount())
                .instanceIRODSFile(irodsParentPath, path);

            pathFile.delete();
            unmap(resolvePath(objectPath), objectPath);
        }
        catch (JargonException e)
        {
            log.error("error during remove", e);
            throw new IOException(e);
        }
        finally
        {
            irodsAccessObjectFactory.closeSessionAndEatExceptions();
        }
    }

    @Override
    public void setAcl(Inode inode, nfsace4[] acl) throws IOException
    {
        log.debug("vfs::setAcl");
        System.out.println("setacl: " + inode.toString());
    }

    @Override
    public void setattr(Inode inode, Stat stat) throws IOException
    {
        log.debug("vfs::setattr");
        /*
         * long inodeNumber = getInodeNumber(inode);
         * Path path = resolveInode(inodeNumber);
         * //PosixFileAttributeView attributeView = Files.getFileAttributeView(path,
         * PosixFileAttributeView.class, NOFOLLOW_LINKS);
         * if (stat.isDefined(Stat.StatAttribute.OWNER)) {
         * try {
         * String uid = String.valueOf(stat.getUid());
         * UserPrincipal user = _lookupService.lookupPrincipalByName(uid);
         * attributeView.setOwner(user);
         * } catch (IOException e) {
         * throw new UnsupportedOperationException("set uid failed: " + e.getMessage());
         * }
         * }
         * if (stat.isDefined(Stat.StatAttribute.GROUP)) {
         * try {
         * String gid = String.valueOf(stat.getGid());
         * GroupPrincipal group = _lookupService.lookupPrincipalByGroupName(gid);
         * attributeView.setGroup(group);
         * } catch (IOException e) {
         * throw new UnsupportedOperationException("set gid failed: " + e.getMessage());
         * }
         * }
         * if (stat.isDefined(Stat.StatAttribute.MODE)) {
         * try {
         * Files.setAttribute(path, "unix:mode", stat.getMode(), NOFOLLOW_LINKS);
         * } catch (IOException e) {
         * throw new UnsupportedOperationException("set mode unsupported: " +
         * e.getMessage());
         * }
         * }
         * if (stat.isDefined(Stat.StatAttribute.SIZE)) {
         * //little known fact - truncate() returns the original channel
         * //noinspection EmptyTryBlock
         * try (FileChannel ignored = FileChannel.open(path,
         * StandardOpenOption.WRITE).truncate(stat.getSize())) {}
         * }
         * if (stat.isDefined(Stat.StatAttribute.ATIME)) {
         * try {
         * FileTime time = FileTime.fromMillis(stat.getCTime());
         * Files.setAttribute(path, "unix:lastAccessTime", time, NOFOLLOW_LINKS);
         * } catch (IOException e) {
         * throw new UnsupportedOperationException("set atime failed: " +
         * e.getMessage());
         * }
         * }
         * if (stat.isDefined(Stat.StatAttribute.MTIME)) {
         * try {
         * FileTime time = FileTime.fromMillis(stat.getMTime());
         * Files.setAttribute(path, "unix:lastModifiedTime", time, NOFOLLOW_LINKS);
         * } catch (IOException e) {
         * throw new UnsupportedOperationException("set mtime failed: " +
         * e.getMessage());
         * }
         * }
         * if (stat.isDefined(Stat.StatAttribute.CTIME)) {
         * try {
         * FileTime time = FileTime.fromMillis(stat.getCTime());
         * Files.setAttribute(path, "unix:ctime", time, NOFOLLOW_LINKS);
         * } catch (IOException e) {
         * throw new UnsupportedOperationException("set ctime failed: " +
         * e.getMessage());
         * }
         * }
         */

    }

    @Override
    public Inode symlink(Inode parent, String linkName, String targetName, Subject subject, int mode) throws IOException
    {
        log.debug("vfs::symlink");
        System.out.println("symlink: " + parent.toString());
        return null;
    }

    @Override
    public WriteResult write(Inode inode, byte[] data, long offset, int count, StabilityLevel stabilityLevel)
        throws IOException
    {
        log.debug("vfs::write");
        System.out.println("write: " + inode.toString());
        return null;
    }

    

    /**
     * Get the iRODS absolute path given the inode number
     * 
     * @param inodeNumber
     *            <code>long</code> with the inode number
     * @return {@link Path} that is the inode
     * @throws NoEntException
     */
    private Path resolveInode(long inodeNumber) throws NoEntException
    {
        Path path = inodeToPath.get(inodeNumber);
        if (path == null)
        {
            throw new NoEntException("inode #" + inodeNumber);
        }
        return path;
    }

    /**
     * Get a stat relating to the given file path and inode number
     * 
     * @param path
     *            {@link Path} of the file
     * @param inodeNumber
     *            <code>long</code> with the inode number
     * @return {@link Stat} describing the file
     * @throws IOException
     */
    private Stat statPath(Path path, long inodeNumber) throws IOException
    {
        log.debug("vfs::statPath");
        log.debug("vfs::statPath - inode number = {}", inodeNumber);
        log.debug("vfs::statPath - path         = {}", path);

        if (path == null)
        {
            throw new IllegalArgumentException("null path");
        }

        String irodsAbsPath = path.normalize().toString();
        log.debug("vfs::statPath - absolute path =  {}", irodsAbsPath);

        try
        {
            CollectionAndDataObjectListAndSearchAO listAO = irodsAccessObjectFactory
                .getCollectionAndDataObjectListAndSearchAO(resolveIrodsAccount());
            ObjStat objStat = listAO.retrieveObjectStatForPath(irodsAbsPath);
            log.debug("vfs::statPath - objStat = {}", objStat);

            Stat stat = new Stat();

            stat.setATime(objStat.getModifiedAt().getTime());
            stat.setCTime(objStat.getCreatedAt().getTime());
            stat.setMTime(objStat.getModifiedAt().getTime());

            UserAO userAO = irodsAccessObjectFactory.getUserAO(resolveIrodsAccount());
            StringBuilder sb = new StringBuilder();
            sb.append(objStat.getOwnerName());
            sb.append("#");
            sb.append(objStat.getOwnerZone());
            User user = userAO.findByName(sb.toString());

            stat.setGid(0); // iRODS does not have a gid
            Subject subject = Subject.getSubject(AccessController.getContext());
            log.debug("Subject: " + subject.toString());
            subject.getPrincipals().forEach(e -> log.debug(e.toString()));
            stat.setUid(Integer.parseInt(user.getId()));
            
            log.debug("vfs::statPath - user id = {}", user.getId());

            // TODO right now don't have soft link or mode support
            //stat.setMode(PermissionBitmaskUtils.USER_READ | PermissionBitmaskUtils.USER_WRITE);
            
            //get file permission
            
            
            log.debug("vfs::statPath - object type = {}", objStat.getObjectType());
            
            if (objStat.getObjectType() == CollectionAndDataObjectListingEntry.ObjectType.COLLECTION)
            {
                stat.setMode(Stat.S_IFDIR | 0777);
            }
            else if (objStat.getObjectType() == CollectionAndDataObjectListingEntry.ObjectType.DATA_OBJECT)
            {
                stat.setMode(Stat.S_IFREG | 0666);
            }
            
            log.debug("vfs::statPath - permissions = {}", Stat.modeToString(stat.getMode()));

            stat.setNlink(1);
            stat.setDev(17);
            stat.setIno((int) inodeNumber);
            stat.setRdev(17);
            stat.setSize(objStat.getObjSize());
            stat.setFileid((int) inodeNumber);
            stat.setGeneration(objStat.getModifiedAt().getTime());

            log.debug("vfs::statPath - stat = {}", stat);

            return stat;
        }
        catch (NumberFormatException | JargonException e)
        {
            log.error("exception getting stat for path: {}", path, e);
            throw new IOException(e);
        }
        finally
        {
            irodsAccessObjectFactory.closeSessionAndEatExceptions();
        }
    }

    private long getInodeNumber(Inode inode)
    {
        return Longs.fromByteArray(inode.getFileId());
    }

    /*
     * 
     * Irods file/collection is owned by first person who makes it
     * - unless inheritance is on. in this case, all permissions transfer to data
     * inside it
     */
    /**
     * Stand-in for a method to return the current user or proxy as a given
     * user...not sure yet how the principal is resolved
     * 
     * @return
     */
    private IRODSAccount resolveIrodsAccount()
    {
        return rootAccount;
    }
    
    /**Mapping**/
    
    private void map(long inodeNumber, String irodsPath)
    {
        map(inodeNumber, Paths.get(irodsPath));
    }

    private void map(long inodeNumber, Path path)
    {
        if (inodeToPath.putIfAbsent(inodeNumber, path) != null)
        {
            throw new IllegalStateException();
        }
        Long otherInodeNumber = pathToInode.putIfAbsent(path, inodeNumber);
        if (otherInodeNumber != null)
        {
            // try rollback
            if (inodeToPath.remove(inodeNumber) != path)
            {
                throw new IllegalStateException("cant map, rollback failed");
            }
            throw new IllegalStateException("path ");
        }
    }
    
    private void unmap(long inodeNumber, Path path)
    {
        Path removedPath = inodeToPath.remove(inodeNumber);
        log.debug("VFS::unmap: Path: " + path +" removedPath: "+removedPath);
        if (!path.equals(removedPath))
        {
            throw new IllegalStateException();
        }
        if (pathToInode.remove(path) != inodeNumber)
        {
            throw new IllegalStateException();
        }
    }

    private void remap(long inodeNumber, Path oldPath, Path newPath)
    {
        // TODO - attempt rollback?
        unmap(inodeNumber, oldPath);
        map(inodeNumber, newPath);
    }
}
