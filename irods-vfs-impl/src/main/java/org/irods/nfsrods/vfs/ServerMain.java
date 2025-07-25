package org.irods.nfsrods.vfs;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dcache.nfs.ExportFile;
import org.dcache.nfs.v4.MDSOperationExecutor;
import org.dcache.nfs.v4.NFSServerV41;
import org.dcache.nfs.v4.xdr.nfs4_prot;
import org.dcache.nfs.vfs.VirtualFileSystem;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvc;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;
import org.irods.jargon.core.connection.ClientServerNegotiationPolicy;
import org.irods.jargon.core.connection.ClientServerNegotiationPolicy.SslNegotiationPolicy;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.packinstr.StartupPack;
import org.irods.jargon.core.connection.SettableJargonProperties;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.pool.conncache.CachedIrodsProtocolManager;
import org.irods.jargon.pool.conncache.JargonConnectionCache;
import org.irods.jargon.pool.conncache.JargonKeyedPoolConfig;
import org.irods.jargon.pool.conncache.JargonPooledObjectFactory;
import org.irods.nfsrods.config.NFSServerConfig;
import org.irods.nfsrods.config.ServerConfig;
import org.irods.nfsrods.utils.JSONUtils;

public class ServerMain
{
    // @formatter:off
    private static final String NFSRODS_CONFIG_HOME = System.getenv("NFSRODS_CONFIG_HOME");
    private static final String SERVER_CONFIG_PATH  = NFSRODS_CONFIG_HOME + "/server.json";
    private static final String EXPORTS_CONFIG_PATH = NFSRODS_CONFIG_HOME + "/exports";
    private static final String GIT_PROPERTIES      = "/git.properties";
    // @formatter:on

    private static final Logger log_ = LogManager.getLogger(ServerMain.class);
    private static final AtomicBoolean shutdownFlag = new AtomicBoolean();

    public static void main(String[] args) throws JargonException, IOException
    {
        StartupPack.setApplicationName("NFSRODS");

        {
            Properties props = new Properties();
            props.load(ServerMain.class.getResourceAsStream(GIT_PROPERTIES));

            if (printSHA(args, props))
            {
                return;
            }
                
            logSHA(props);
        }

        ServerConfig config = null;
        
        try
        {
            config = JSONUtils.fromJSON(new File(SERVER_CONFIG_PATH), ServerConfig.class);
            log_.info("main - Server config ==> {}", JSONUtils.toJSON(config));
        }
        catch (IOException e)
        {
            log_.error("main - Error reading server config." + System.lineSeparator() + e.getMessage());
            System.exit(1);
        }
        
        NFSServerConfig nfsSvrConfig = config.getNfsServerConfig();
        final IRODSFileSystem ifsys = initIRODSFileSystemWithConnectionCaching(config);

        configureJargonSslNegotiationPolicy(config, ifsys);
        configureJargonConnectionTimeout(config, ifsys);

        try (CachingProvider cachingProvider = Caching.getCachingProvider();
             CacheManager cacheManager = cachingProvider.getCacheManager();)
        {
            IRODSAccessObjectFactory ifactory = ifsys.getIRODSAccessObjectFactory();
            IRODSIdMapper idMapper = new IRODSIdMapper(config, ifactory);

            // @formatter:off
            final OncRpcSvc nfsSvc = new OncRpcSvcBuilder()
                .withPort(nfsSvrConfig.getPort())
                .withTCP()
                .withAutoPublish()
                .withWorkerThreadIoStrategy()
                .withSubjectPropagation()
                .build();
            // @formatter:on

            ExportFile exportFile = new ExportFile(new File(EXPORTS_CONFIG_PATH));
            VirtualFileSystem vfs = new IRODSVirtualFileSystem(config, ifactory, idMapper, cacheManager);

            // @formatter:off
            NFSServerV41 nfs4 = new NFSServerV41.Builder()
                .withExportTable(exportFile)
                .withVfs(vfs)
                .withOperationExecutor(new MDSOperationExecutor())
                .build();
            // @formatter:on

            nfsSvc.register(new OncRpcProgram(nfs4_prot.NFS4_PROGRAM, nfs4_prot.NFS_V4), nfs4);

            nfsSvc.start();

            log_.info("main - Press ctrl-c to shutdown.");

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run()
                {
                    log_.info("main - Shutting down server ...");
                    
                    close(nfsSvc);
                    close(ifsys);

                    log_.info("main - done.");

                    // Manually shutting down logging requires direct use of the
                    // log4j 2 API because slf4j does not implement shutdown operations.
                    LogManager.shutdown();

                    shutdownFlag.set(true);
                }
            });
            
            // Wait for shutdown signal.
            while (!shutdownFlag.get())
            {
                try { Thread.sleep(250); } catch (InterruptedException e) {}
            }
        }
        catch (JargonException | IOException e)
        {
            log_.error(e.getMessage());
            System.exit(1);
        }
    }
    
    private static boolean printSHA(String[] args, Properties _props) throws IOException
    {
        if (args.length > 0 && "sha".equals(args[0]))
        {
            System.out.println("Build Time    => " + _props.getProperty("git.build.time"));
            System.out.println("Build Version => " + _props.getProperty("git.build.version"));
            System.out.println("Build SHA     => " + _props.getProperty("git.commit.id.full"));

            return true;
        }
        
        return false;
    }
    
    private static void logSHA(Properties _props)
    {
        log_.info("Build Time    => {}", _props.getProperty("git.build.time"));
        log_.info("Build Version => {}", _props.getProperty("git.build.version"));
        log_.info("Build SHA     => {}", _props.getProperty("git.commit.id.full"));
    }
    
    private static IRODSFileSystem initIRODSFileSystemWithConnectionCaching(ServerConfig _config) throws JargonException
    {
        IRODSFileSystem ifsys = IRODSFileSystem.instance();

        JargonKeyedPoolConfig poolConfig = new JargonKeyedPoolConfig();

        JargonPooledObjectFactory poolFactory = new JargonPooledObjectFactory();
        poolFactory.setIrodsSession(ifsys.getIrodsSession());
        poolFactory.setIrodsSimpleProtocolManager(ifsys.getIrodsProtocolManager());

        JargonConnectionCache connCache = new JargonConnectionCache(poolFactory, poolConfig);

        CachedIrodsProtocolManager protocolMgr = new CachedIrodsProtocolManager();
        protocolMgr.setJargonConnectionCache(connCache);

        ifsys.getIrodsSession().setIrodsProtocolManager(protocolMgr);

        return ifsys;
    }
    
    private static void configureJargonConnectionTimeout(ServerConfig _config, IRODSFileSystem _ifsys)
    {
        IRODSSession session = _ifsys.getIrodsSession();
        SettableJargonProperties props = new SettableJargonProperties(session.getJargonProperties());
        props.setIRODSSocketTimeout(_config.getIRODSClientConfig().getConnectionTimeout());
        session.setJargonProperties(props);
    }

    private static void configureJargonSslNegotiationPolicy(ServerConfig _config, IRODSFileSystem _ifsys) throws JargonException
    {
        String policy = _config.getIRODSClientConfig().getSslNegotiationPolicy();
        SslNegotiationPolicy sslNegPolicy = ClientServerNegotiationPolicy.findSslNegotiationPolicyFromString(policy);
        log_.debug("configureClientServerNegotiationPolicy - Policy = {}", sslNegPolicy);

        IRODSSession session = _ifsys.getIrodsSession();
        SettableJargonProperties props = new SettableJargonProperties(session.getJargonProperties());
        props.setNegotiationPolicy(sslNegPolicy);
        session.setJargonProperties(props);
    }

    private static void close(Object _obj)
    {
        if (_obj == null)
        {
            return;
        }

        try
        {
            if (_obj instanceof OncRpcSvc service)
            {
                service.stop();
            }
            else if (_obj instanceof IRODSFileSystem ifsys)
            {
                IRODSSession session = ifsys.getIrodsSession();
                CachedIrodsProtocolManager pmgr = (CachedIrodsProtocolManager) session.getIrodsProtocolManager();
                pmgr.getJargonConnectionCache().close();
                ifsys.closeAndEatExceptions();
            }
        }
        catch (Exception e)
        {
            log_.error(e.getMessage());
        }
    }
}
