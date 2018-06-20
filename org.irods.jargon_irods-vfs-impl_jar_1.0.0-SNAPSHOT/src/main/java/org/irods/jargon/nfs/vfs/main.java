/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.irods.jargon.nfs.vfs;

import org.dcache.nfs.vfs.VirtualFileSystem;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;

/**
 *
 * @author amiecz46
 */
public class main {
    
   public static void main(String[] args) throws JargonException, Exception{
       
       IRODSAccessObjectFactory factory = new IRODSAccessObjectFactoryImpl();
       IRODSAccount root;
       //IRODSFile rootfile = IRODSFileSystem.instance().getIRODSFileFactory(root);
       
       //VirtualFileSystem vfs = new IrodsVirtualFileSystem(factory, root,  rootfile);
       
   }
    
}
