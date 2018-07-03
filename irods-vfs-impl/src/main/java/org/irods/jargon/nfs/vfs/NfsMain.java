package org.irods.jargon.nfs.vfs;

import java.io.File;
import java.io.IOException;

import org.dcache.nfs.ExportFile;
import org.dcache.nfs.v3.MountServer;
import org.dcache.nfs.v3.NfsServerV3;
import org.dcache.nfs.v4.DeviceManager;
import org.dcache.nfs.v4.MDSOperationFactory;
import org.dcache.nfs.v4.NFSServerV41;
import org.dcache.nfs.vfs.VirtualFileSystem;
import org.dcache.xdr.OncRpcProgram;
import org.dcache.xdr.OncRpcSvc;
import org.dcache.xdr.OncRpcSvcBuilder;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;

public class NfsMain {

	public static void main(String[] args) throws JargonException, IOException {
		OncRpcSvc nfsSvc = new OncRpcSvcBuilder()
//			.withBindAddress("0.0.0.0")
			.withPort(2049)
			.withTCP()
			.withAutoPublish()
			.withWorkerThreadIoStrategy()
			.build();
		
		ExportFile exportFile = new ExportFile(new File("config/exports"));
		
		IRODSAccount acct = IRODSAccount.instance("kdd-ws", 1247, "rods", "rods", "/tempZone/home/rods", "tempZone", "demoResc");
		IRODSFileSystem fs = IRODSFileSystem.instance();
		IRODSAccessObjectFactory factory = IRODSAccessObjectFactoryImpl.instance(fs.getIrodsSession());
		IRODSFile rootFile = factory.getIRODSFileFactory(acct).instanceIRODSFile("/tempZone/home/rods");
		VirtualFileSystem vfs = new IrodsVirtualFileSystem(factory, acct, rootFile);
			
		NFSServerV41 nfs4 = new NFSServerV41(new MDSOperationFactory(),
											 new DeviceManager(),
											 vfs,
											 exportFile);
		
		NfsServerV3 nfs3 = new NfsServerV3(exportFile, vfs);
		MountServer mountd = new MountServer(exportFile, vfs);
		
		nfsSvc.register(new OncRpcProgram(100003, 4), nfs4);
		nfsSvc.register(new OncRpcProgram(100003, 3), nfs3);
		nfsSvc.register(new OncRpcProgram(100003, 3), mountd);
		
		nfsSvc.start();
		
		System.in.read();
	}

}
