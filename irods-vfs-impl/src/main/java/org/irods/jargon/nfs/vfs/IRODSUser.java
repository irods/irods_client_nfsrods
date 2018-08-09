/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.irods.jargon.nfs.vfs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alek
 */
public class IRODSUser {
    
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IrodsIdMap.class);
    private  NonBlockingHashMapLong<Path> inodeToPath = new NonBlockingHashMapLong<>();
    private  NonBlockingHashMap<Path, Long> pathToInode = new NonBlockingHashMap<>();
    private  AtomicLong fileId = new AtomicLong(1); // numbering starts at 1
    private  IRODSAccessObjectFactory irodsAccessObjectFactory;
    private  IRODSAccount rootAccount;
    private  IRODSFile root;
    public int userID;
    
    public IRODSUser(String username){
        
        //TODO: Export rods config data to config.java file
        String adminAcct = "rods";
        String adminPw = "rods";
        String userzone = "tempZone";
        
        String zonePath = "/"+userzone+"/home/"+username;
        log.debug("[IRODSUser] Creating User");
        try {
            //Create Irods Account instance for user and bind to associated globals
            this.rootAccount = IRODSAccount.instanceWithProxy("localhost", 1247, username, adminPw, zonePath, userzone, "demoResc", adminAcct, userzone);
            IRODSFileSystem fs = IRODSFileSystem.instance();
            this.irodsAccessObjectFactory = IRODSAccessObjectFactoryImpl.instance(fs.getIrodsSession());
            this.root = this.irodsAccessObjectFactory.getIRODSFileFactory(this.rootAccount).instanceIRODSFile(zonePath);
            
            //faster to save userID once then contact jargon on repeat getUserID() calls
            this.userID = Integer.parseInt(irodsAccessObjectFactory.getUserAO(rootAccount).findByName(rootAccount.getUserName()).getId());
            
        } catch (JargonException ex) {
            log.error("Error creating IRODSUser Object: " + ex);
        }
        
        //establish root inode
        establishRoot();
	
    }
    
    public int getUserID(){
        return this.userID;
    }
    
    public String getAbsolutePath(){
        return root.getAbsolutePath();
    }

    public NonBlockingHashMapLong<Path> getInodeToPath() {
        return inodeToPath;
    }

    public void setInodeToPath(NonBlockingHashMapLong<Path> inodeToPath) {
        this.inodeToPath = inodeToPath;
    }

    public NonBlockingHashMap<Path, Long> getPathToInode() {
        return pathToInode;
    }

    public void setPathToInode(NonBlockingHashMap<Path, Long> pathToInode) {
        this.pathToInode = pathToInode;
    }

    public Long getAndIncrementFileId() {
        return fileId.getAndIncrement();
    }


    public IRODSAccessObjectFactory getIrodsAccessObjectFactory() {
        return irodsAccessObjectFactory;
    }

    public IRODSAccount getRootAccount() {
        return rootAccount;
    }

    public IRODSFile getRoot() {
        return root;
    }
    
    /**
     * Establishes root inode based off the home dir of the IRODSFile rootFile 
     */
    private void establishRoot()
    {
        log.debug("[IRODSUser] establish root at: {}", this.root);

        try
        {
            if (this.root.exists() && this.root.canRead())
            {
                log.debug("root is valid");
            }
            else
            {
                try {
                    throw new DataNotFoundException("cannot establish root at:" + this.root);
                } catch (DataNotFoundException ex) {
                    Logger.getLogger(IRODSUser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            log.debug("mapping root...");
            this.map(this.fileId.getAndIncrement(), this.root.getAbsolutePath()); // so root is always inode #1
        }
        finally
        {
            irodsAccessObjectFactory.closeSessionAndEatExceptions();
        }
    }
    
    
    /**Mapping**/
    
    public void map(long inodeNumber, String irodsPath)
    {
        map(inodeNumber, Paths.get(irodsPath));
    }

    public void map(long inodeNumber, Path path)
    {
        if (this.inodeToPath.putIfAbsent(inodeNumber, path) != null)
        {
            throw new IllegalStateException();
        }
        Long otherInodeNumber = this.pathToInode.putIfAbsent(path, inodeNumber);
        if (otherInodeNumber != null)
        {
            // try rollback
            if (this.inodeToPath.remove(inodeNumber) != path)
            {
                throw new IllegalStateException("cant map, rollback failed");
            }
            throw new IllegalStateException("path ");
        }
    }
    
    public void unmap(long inodeNumber, Path path)
    {
        Path removedPath = this.inodeToPath.remove(inodeNumber);
        log.debug("VFS::unmap: Path: " + path +" removedPath: "+removedPath);
        if (!path.equals(removedPath))
        {
            throw new IllegalStateException();
        }
        if (this.pathToInode.remove(path) != inodeNumber)
        {
            throw new IllegalStateException();
        }
    }

    public void remap(long inodeNumber, Path oldPath, Path newPath)
    {
        // TODO - attempt rollback?
        unmap(inodeNumber, oldPath);
        map(inodeNumber, newPath);
    }


    
    
}
