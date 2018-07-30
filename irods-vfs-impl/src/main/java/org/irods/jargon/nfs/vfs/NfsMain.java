package org.irods.jargon.nfs.vfs;

import java.io.File;
import java.io.IOException;
import javax.crypto.Cipher;

import java.security.NoSuchAlgorithmException;
import org.apache.log4j.PropertyConfigurator;
import org.dcache.nfs.ExportFile;
import org.dcache.nfs.v3.MountServer;
import org.dcache.nfs.v3.NfsServerV3;
import org.dcache.nfs.v4.MDSOperationFactory;
import org.dcache.nfs.v4.NFSServerV41;
import org.dcache.nfs.vfs.VirtualFileSystem;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvc;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;
import org.dcache.oncrpc4j.rpc.gss.GssSessionManager;
import org.ietf.jgss.GSSException;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.irods.jargon.nfs.vfs.IrodsIdMap;

public class NfsMain {
	
    private static final String PREFIX = "/home/alek/jargon-nfs4j-irodsvfs/irods-vfs-impl/";
//    private static final String PREFIX = "/home/kory/dev/prog/java/github/jargon-nfs4j-irodsvfs/irods-vfs-impl/";
	
	static
	{
		PropertyConfigurator.configure(PREFIX + "config/log4j.properties");
	}
    
    private static final Logger log = LoggerFactory.getLogger(NfsMain.class);

	public static void main(String[] args) throws JargonException, IOException, GSSException, NoSuchAlgorithmException
	{
	
	int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
   	log.debug("Max Key Length AES: " + maxKeyLen);
                
                IrodsIdMap _idMapper = new IrodsIdMap();
		OncRpcSvc nfsSvc = new OncRpcSvcBuilder()
			.withPort(2049)
			.withTCP()
			.withAutoPublish()
			.withWorkerThreadIoStrategy()
                        .withGssSessionManager(new GssSessionManager(_idMapper,"nfs/172.25.14.126@NFSRENCI.ORG","/etc/krb5.keytab"))
                        .withSubjectPropagation()
			.build();
		
		ExportFile exportFile = new ExportFile(new File(PREFIX + "config/exports"));
		
		IRODSAccount acct = IRODSAccount.instance("localhost", 1247, "rods", "rods", "/tempZone/home/rods", "tempZone", "demoResc");
		IRODSFileSystem fs = IRODSFileSystem.instance();
		IRODSAccessObjectFactory factory = IRODSAccessObjectFactoryImpl.instance(fs.getIrodsSession());
		IRODSFile rootFile = factory.getIRODSFileFactory(acct).instanceIRODSFile("/tempZone/home/rods");
		VirtualFileSystem vfs = new IrodsVirtualFileSystem(factory, acct, rootFile, _idMapper);
			
		NFSServerV41 nfs4 = new NFSServerV41.Builder()
		    .withExportFile(exportFile)
		    .withVfs(vfs)
		    .withOperationFactory(new MDSOperationFactory())
		    .build();
		
		NfsServerV3 nfs3 = new NfsServerV3(exportFile, vfs);
		MountServer mountd = new MountServer(exportFile, vfs);
		
		nfsSvc.register(new OncRpcProgram(100003, 4), nfs4);
		nfsSvc.register(new OncRpcProgram(100003, 3), nfs3);
		nfsSvc.register(new OncRpcProgram(100003, 3), mountd);
		
		nfsSvc.start();
		
		log.info("press [enter] to shutdown.");
		System.in.read();
		
		log.info("shutting down ...");
		nfsSvc.stop();

		log.info("shutdown complete.");
	}

}
