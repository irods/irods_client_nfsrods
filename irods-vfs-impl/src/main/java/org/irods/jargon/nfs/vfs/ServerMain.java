package org.irods.jargon.nfs.vfs;

import java.io.File;
import java.io.IOException;
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
import org.irods.jargon.core.exception.JargonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain
{
    private static final String PREFIX = "/home/kory/dev/prog/java/github/irods_client_nfsrods/irods-vfs-impl/";

    static
    {
        PropertyConfigurator.configure(PREFIX + "config/log4j.properties");
    }

    private static final Logger log_ = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) throws JargonException, IOException, GSSException, NoSuchAlgorithmException
    {
        ServerConfig config = JSONUtils.fromJSON(new File(PREFIX + "config/server.json"), ServerConfig.class);
        
        log_.debug("main :: server config ==> {}", JSONUtils.toJSON(config));
        
        NFSServerConfig nfsSvrConfig = config.getNfsServerConfig();

        IRODSIdMap idMapper = new IRODSIdMap(config);
        GssSessionManager gssSessionMgr = new GssSessionManager(idMapper, nfsSvrConfig.getKerberosServicePrincipal(),
                                                                nfsSvrConfig.getKerberosKeytab());

        // @formatter:off
        OncRpcSvc nfsSvc = new OncRpcSvcBuilder()
            .withPort(nfsSvrConfig.getPort())
            .withTCP()
            .withAutoPublish()
            .withWorkerThreadIoStrategy()
//            .withGssSessionManager(new GssSessionManager(idMapper, "nfs/172.25.14.126@NFSRENCI.ORG", "/etc/krb5.keytab"))
            .withGssSessionManager(gssSessionMgr)
            .withSubjectPropagation()
            .build();
        // @formatter:on

        ExportFile exportFile = new ExportFile(new File(PREFIX + "config/exports"));
        VirtualFileSystem vfs = new IRODSVirtualFileSystem(idMapper);

        // @formatter:off
        NFSServerV41 nfs4 = new NFSServerV41.Builder()
            .withExportFile(exportFile)
            .withVfs(vfs)
            .withOperationFactory(new MDSOperationFactory())
            .build();
        // @formatter:on

        NfsServerV3 nfs3 = new NfsServerV3(exportFile, vfs);
        MountServer mountd = new MountServer(exportFile, vfs);

        nfsSvc.register(new OncRpcProgram(100003, 4), nfs4);
        nfsSvc.register(new OncRpcProgram(100003, 3), nfs3);
        nfsSvc.register(new OncRpcProgram(100003, 3), mountd);

        nfsSvc.start();

        log_.info("main :: press [enter] to shutdown.");
        System.in.read();

        log_.info("main :: shutting down ...");
        nfsSvc.stop();

        log_.info("main :: shutdown complete.");
    }

}
