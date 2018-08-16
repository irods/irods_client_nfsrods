package org.irods.jargon.nfs.vfs;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    private NonBlockingHashMap<BigInteger, Path> inodeToPath_; // Used to be NonBlockingHashMapLong<Path>
    private NonBlockingHashMap<Path, BigInteger> pathToInode_; // Used to be NonBlockingHashMap<Path, Long>
    private BigInteger fileID_; // Used to be AtomicLong
    private IRODSAccessObjectFactory factory_;
    private IRODSAccount proxiedAcct_;
    private IRODSFile rootFile_;
    private int userID_;

    public IRODSUser(String _username)
    {
        inodeToPath_ = new NonBlockingHashMap<>();
        pathToInode_ = new NonBlockingHashMap<>();
        fileID_ = BigInteger.ONE; // numbering starts at 1

        // TODO: Export rods config data to config.java file
        String adminAcct = "rods";
        String adminPw = "rods";
        String userzone = "tempZone";

        String zonePath = "/" + userzone + "/home/" + _username;
        log_.debug("IRODSUser :: Creating proxy for username [{}] ...", _username);

        try
        {
            // Create Irods Account instance for user and bind to associated globals
            proxiedAcct_ = IRODSAccount.instanceWithProxy("localhost", 1247, _username, adminPw, zonePath, userzone, "demoResc", adminAcct, userzone);
            IRODSFileSystem fs = IRODSFileSystem.instance();
            factory_ = IRODSAccessObjectFactoryImpl.instance(fs.getIrodsSession());
            rootFile_ = factory_.getIRODSFileFactory(proxiedAcct_).instanceIRODSFile(zonePath);

            // faster to save userID once then contact jargon on repeat getUserID() calls
//            User user = irodsAOFactory_.getUserAO(proxiedAcct_).findByName(proxiedAcct_.getUserName());
            User user = factory_.getUserAO(proxiedAcct_).findByName(_username);
            userID_ = Integer.parseInt(user.getId());

        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
        }

        establishRoot();
    }

    public int getUserID()
    {
        return this.userID_;
    }

    public String getAbsolutePath()
    {
        return rootFile_.getAbsolutePath();
    }

    public NonBlockingHashMap<BigInteger, Path> getInodeToPath()
    {
        return inodeToPath_;
    }

//    public void setInodeToPath(NonBlockingHashMapLong<Path> inodeToPath)
//    {
//        this.inodeToPath_ = inodeToPath;
//    }

    public NonBlockingHashMap<Path, BigInteger> getPathToInode()
    {
        return pathToInode_;
    }

//    public void setPathToInode(NonBlockingHashMap<Path, Long> pathToInode)
//    {
//        this.pathToInode_ = pathToInode;
//    }

//    public Long getAndIncrementFileId()
//    {
//        return fileID_.getAndIncrement();
//    }
    
    public BigInteger getAndIncFileID()
    {
        BigInteger oldID = fileID_;
        fileID_ = fileID_.add(BigInteger.ONE);
        return oldID;
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
//                    Logger.getLogger(IRODSUser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            log_.debug("mapping root...");

            //map(fileID_.getAndIncrement(), rootFile_.getAbsolutePath()); // so root is always inode #1
            map(getAndIncFileID(), rootFile_.getAbsolutePath()); // so root is always inode #1
        }
        finally
        {
            factory_.closeSessionAndEatExceptions();
        }
    }

    /** Mapping **/

    public void map(BigInteger inodeNumber, String irodsPath)
    {
        map(inodeNumber, Paths.get(irodsPath));
    }

    public void map(BigInteger inodeNumber, Path path)
    {
        if (inodeToPath_.putIfAbsent(inodeNumber, path) != null)
        {
            throw new IllegalStateException();
        }

        BigInteger otherInodeNumber = pathToInode_.putIfAbsent(path, inodeNumber);

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

    public void unmap(BigInteger inodeNumber, Path path)
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

    public void remap(BigInteger inodeNumber, Path oldPath, Path newPath)
    {
        unmap(inodeNumber, oldPath);
        map(inodeNumber, newPath);
    }

    @Override
    public String toString()
    {
        return "IRODSUser{" + "rootAccount=" + proxiedAcct_ + ", userID=" + userID_ + '}';
    }

}
