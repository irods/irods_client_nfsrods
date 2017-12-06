/**
 * 
 */
package org.irods.jargon.nfs.vfs;

import java.io.IOException;
import java.util.List;

import javax.security.auth.Subject;

import org.dcache.nfs.v4.NfsIdMapping;
import org.dcache.nfs.v4.xdr.nfsace4;
import org.dcache.nfs.vfs.AclCheckable;
import org.dcache.nfs.vfs.DirectoryEntry;
import org.dcache.nfs.vfs.FsStat;
import org.dcache.nfs.vfs.Inode;
import org.dcache.nfs.vfs.Stat;
import org.dcache.nfs.vfs.Stat.Type;
import org.dcache.nfs.vfs.VirtualFileSystem;

/**
 * iRODS implmentation of the nfs4j virtual file system
 * 
 * @author Mike Conway - NIEHS
 *
 */
public class IrodsVirtualFileSystem implements VirtualFileSystem {

	/**
	 * 
	 */
	public IrodsVirtualFileSystem() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#access(org.dcache.nfs.vfs.Inode,
	 * int)
	 */
	@Override
	public int access(Inode arg0, int arg1) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#commit(org.dcache.nfs.vfs.Inode,
	 * long, int)
	 */
	@Override
	public void commit(Inode arg0, long arg1, int arg2) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#create(org.dcache.nfs.vfs.Inode,
	 * org.dcache.nfs.vfs.Stat.Type, java.lang.String, javax.security.auth.Subject,
	 * int)
	 */
	@Override
	public Inode create(Inode arg0, Type arg1, String arg2, Subject arg3, int arg4) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#getAcl(org.dcache.nfs.vfs.Inode)
	 */
	@Override
	public nfsace4[] getAcl(Inode arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#getAclCheckable()
	 */
	@Override
	public AclCheckable getAclCheckable() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#getFsStat()
	 */
	@Override
	public FsStat getFsStat() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#getIdMapper()
	 */
	@Override
	public NfsIdMapping getIdMapper() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#getRootInode()
	 */
	@Override
	public Inode getRootInode() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#getattr(org.dcache.nfs.vfs.Inode)
	 */
	@Override
	public Stat getattr(Inode arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dcache.nfs.vfs.VirtualFileSystem#hasIOLayout(org.dcache.nfs.vfs.Inode)
	 */
	@Override
	public boolean hasIOLayout(Inode arg0) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#link(org.dcache.nfs.vfs.Inode,
	 * org.dcache.nfs.vfs.Inode, java.lang.String, javax.security.auth.Subject)
	 */
	@Override
	public Inode link(Inode arg0, Inode arg1, String arg2, Subject arg3) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#list(org.dcache.nfs.vfs.Inode)
	 */
	@Override
	public List<DirectoryEntry> list(Inode arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#lookup(org.dcache.nfs.vfs.Inode,
	 * java.lang.String)
	 */
	@Override
	public Inode lookup(Inode arg0, String arg1) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#mkdir(org.dcache.nfs.vfs.Inode,
	 * java.lang.String, javax.security.auth.Subject, int)
	 */
	@Override
	public Inode mkdir(Inode arg0, String arg1, Subject arg2, int arg3) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#move(org.dcache.nfs.vfs.Inode,
	 * java.lang.String, org.dcache.nfs.vfs.Inode, java.lang.String)
	 */
	@Override
	public boolean move(Inode arg0, String arg1, Inode arg2, String arg3) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#parentOf(org.dcache.nfs.vfs.Inode)
	 */
	@Override
	public Inode parentOf(Inode arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#read(org.dcache.nfs.vfs.Inode,
	 * byte[], long, int)
	 */
	@Override
	public int read(Inode arg0, byte[] arg1, long arg2, int arg3) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#readlink(org.dcache.nfs.vfs.Inode)
	 */
	@Override
	public String readlink(Inode arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#remove(org.dcache.nfs.vfs.Inode,
	 * java.lang.String)
	 */
	@Override
	public void remove(Inode arg0, String arg1) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#setAcl(org.dcache.nfs.vfs.Inode,
	 * org.dcache.nfs.v4.xdr.nfsace4[])
	 */
	@Override
	public void setAcl(Inode arg0, nfsace4[] arg1) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#setattr(org.dcache.nfs.vfs.Inode,
	 * org.dcache.nfs.vfs.Stat)
	 */
	@Override
	public void setattr(Inode arg0, Stat arg1) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#symlink(org.dcache.nfs.vfs.Inode,
	 * java.lang.String, java.lang.String, javax.security.auth.Subject, int)
	 */
	@Override
	public Inode symlink(Inode arg0, String arg1, String arg2, Subject arg3, int arg4) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dcache.nfs.vfs.VirtualFileSystem#write(org.dcache.nfs.vfs.Inode,
	 * byte[], long, int, org.dcache.nfs.vfs.VirtualFileSystem.StabilityLevel)
	 */
	@Override
	public WriteResult write(Inode arg0, byte[] arg1, long arg2, int arg3, StabilityLevel arg4) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
