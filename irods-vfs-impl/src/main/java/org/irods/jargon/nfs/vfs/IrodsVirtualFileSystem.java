/**
 * 
 */
package org.irods.jargon.nfs.vfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.security.auth.Subject;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.dcache.nfs.v4.NfsIdMapping;
import org.dcache.nfs.v4.SimpleIdMap;
import org.dcache.nfs.v4.xdr.nfsace4;
import org.dcache.nfs.vfs.AclCheckable;
import org.dcache.nfs.vfs.DirectoryEntry;
import org.dcache.nfs.vfs.FsStat;
import org.dcache.nfs.vfs.Inode;
import org.dcache.nfs.vfs.Stat;
import org.dcache.nfs.vfs.Stat.Type;
import org.dcache.nfs.vfs.VirtualFileSystem;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * iRODS implmentation of the nfs4j virtual file system
 * 
 * @author Mike Conway - NIEHS
 *
 */
public class IrodsVirtualFileSystem implements VirtualFileSystem {

	private static final Logger log = LoggerFactory.getLogger(IrodsVirtualFileSystem.class);

	private final IRODSAccessObjectFactory irodsAccessObjectFactory;
	private final IRODSAccount rootAccount;
	private final IRODSFile root;
	private final NonBlockingHashMapLong<Path> inodeToPath = new NonBlockingHashMapLong<>();
	private final NonBlockingHashMap<Path, Long> pathToInode = new NonBlockingHashMap<>();
	private final AtomicLong fileId = new AtomicLong(1); // numbering starts at 1
	private final NfsIdMapping _idMapper = new SimpleIdMap();

	@Override
	public int access(Inode arg0, int arg1) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void commit(Inode arg0, long arg1, int arg2) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public Inode create(Inode arg0, Type arg1, String arg2, Subject arg3, int arg4) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public nfsace4[] getAcl(Inode arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AclCheckable getAclCheckable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FsStat getFsStat() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NfsIdMapping getIdMapper() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Inode getRootInode() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stat getattr(Inode arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasIOLayout(Inode arg0) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Inode link(Inode arg0, Inode arg1, String arg2, Subject arg3) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DirectoryEntry> list(Inode arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Inode lookup(Inode arg0, String arg1) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Inode mkdir(Inode arg0, String arg1, Subject arg2, int arg3) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean move(Inode arg0, String arg1, Inode arg2, String arg3) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Inode parentOf(Inode arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int read(Inode arg0, byte[] arg1, long arg2, int arg3) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String readlink(Inode arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(Inode arg0, String arg1) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setAcl(Inode arg0, nfsace4[] arg1) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setattr(Inode arg0, Stat arg1) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public Inode symlink(Inode arg0, String arg1, String arg2, Subject arg3, int arg4) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WriteResult write(Inode arg0, byte[] arg1, long arg2, int arg3, StabilityLevel arg4) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	private void establishRoot() throws DataNotFoundException, JargonException {
		log.info("establishRoot at:{}", root);
		try {
			if (root.exists() && root.canRead()) {
				log.info("root is valid");
			} else {
				throw new DataNotFoundException("cannot establish root at:" + root);
			}
		} finally {
			irodsAccessObjectFactory.closeSessionAndEatExceptions();
		}

	}

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
	public IrodsVirtualFileSystem(IRODSAccessObjectFactory irodsAccessObjectFactory, IRODSAccount rootAccount,
			IRODSFile root) throws DataNotFoundException, JargonException {
		super();

		if (irodsAccessObjectFactory == null) {
			throw new IllegalArgumentException("null irodsAccessObjectFactory");
		}

		if (rootAccount == null) {
			throw new IllegalArgumentException("null rootAccount");
		}

		if (root == null) {
			throw new IllegalArgumentException("null root");
		}

		this.irodsAccessObjectFactory = irodsAccessObjectFactory;
		this.rootAccount = rootAccount;
		this.root = root;
		establishRoot();
	}

}
