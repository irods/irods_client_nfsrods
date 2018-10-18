package org.irods.nfsrods.vfs;

import java.io.File;
import java.io.IOException;

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
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.nfsrods.config.NFSServerConfig;
import org.irods.nfsrods.config.ServerConfig;
import org.irods.nfsrods.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain
{
    // @formatter:off
    private static final String NFSRODS_HOME        = System.getenv("NFSRODS_HOME");
    private static final String LOGGER_CONFIG_PATH  = NFSRODS_HOME + "/config/log4j.properties";
    private static final String SERVER_CONFIG_PATH  = NFSRODS_HOME + "/config/server.json";
    private static final String EXPORTS_CONFIG_PATH = NFSRODS_HOME + "/config/exports";
    // @formatter:on

    static
    {
        PropertyConfigurator.configure(LOGGER_CONFIG_PATH);
    }

    private static final Logger log_ = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) throws JargonException
    {
        ServerConfig config = null;

        try
        {
            config = JSONUtils.fromJSON(new File(SERVER_CONFIG_PATH), ServerConfig.class);
            log_.debug("main :: Server config ==> {}", JSONUtils.toJSON(config));
        }
        catch (IOException e)
        {
            log_.error("main :: Error reading server config." + System.lineSeparator() + e.getMessage());
            System.exit(1);
        }

        NFSServerConfig nfsSvrConfig = config.getNfsServerConfig();
        IRODSFileSystem ifsys = IRODSFileSystem.instance();
        OncRpcSvc nfsSvc = null;

        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHandler<>(ifsys, "Closing iRODS connections")));

        try
        {
            IRODSAccessObjectFactory ifactory = ifsys.getIRODSAccessObjectFactory();
            IRODSIdMap idMapper = new IRODSIdMap(config, ifactory);

            // @formatter:off
            GssSessionManager gssSessionMgr = new GssSessionManager(idMapper,
                                                                    nfsSvrConfig.getKerberosServicePrincipal(),
                                                                    nfsSvrConfig.getKerberosKeytab());

            nfsSvc = new OncRpcSvcBuilder()
                .withPort(nfsSvrConfig.getPort())
                .withTCP()
                .withAutoPublish()
                .withWorkerThreadIoStrategy()
                .withGssSessionManager(gssSessionMgr)
                .withSubjectPropagation()
                .build();

            Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHandler<>(nfsSvc, "Shutting down NFS services")));
            // @formatter:on

            ExportFile exportFile = new ExportFile(new File(EXPORTS_CONFIG_PATH));
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

            log_.info("main :: Press [ctrl-c] to shutdown.");

            Thread.currentThread().join();
        }
        catch (JargonException | IOException | GSSException | InterruptedException e)
        {
            log_.error(e.getMessage());
        }
    }

    private static void close(Object _obj)
    {
        if (_obj == null)
        {
            return;
        }

        try
        {
            // @formatter:off
            if      (_obj instanceof OncRpcSvc)       { ((OncRpcSvc) _obj).stop(); }
            else if (_obj instanceof IRODSFileSystem) { ((IRODSFileSystem) _obj).closeAndEatExceptions(); }
            // @formatter:on
        }
        catch (Exception e)
        {
            log_.error(e.getMessage());
        }
    }

    private static final class ShutdownHandler<T> implements Runnable
    {
        private T object_;
        private String msg_;

        ShutdownHandler(T _object, String _msg)
        {
            object_ = _object;
            msg_ = _msg;
        }

        @Override
        public void run()
        {
            log_.info("main :: {} ...", msg_);

            close(object_);

            log_.info("main :: {} ... done.", msg_);
        }
    }
}
