package org.irods.nfsrods.vfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.dcache.nfs.v4.NfsIdMapping;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.nfsrods.config.IRODSProxyAdminAccountConfig;
import org.irods.nfsrods.config.NFSServerConfig;
import org.irods.nfsrods.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.IntByReference;

public class IRODSIdMapper implements NfsIdMapping
{
    private static final Logger log_ = LoggerFactory.getLogger(IRODSIdMapper.class);

    private static final LibC libc_ = (LibC) Native.load("c", LibC.class);

    public static final int NOBODY_UID = 65534;
    public static final int NOBODY_GID = 65534;

    public static final String NOBODY_USER  = libc_.getpwuid(NOBODY_UID).name;
    public static final String NOBODY_GROUP = libc_.getgrgid(NOBODY_GID).name;

    private ServerConfig config_;
    private IRODSAccessObjectFactory factory_;
    private Map<String, Integer> nameToUidMap_;
    private Map<Integer, IRODSUser> uidToNameMap_;
    private ScheduledExecutorService scheduler_;
    private ReadWriteLock purgeUsersLock_;

    public IRODSIdMapper(ServerConfig _config, IRODSAccessObjectFactory _factory) throws IOException
    {
        config_ = _config;
        factory_ = _factory;
        nameToUidMap_ = new NonBlockingHashMap<>();
        uidToNameMap_ = new NonBlockingHashMap<>();
        scheduler_ = Executors.newSingleThreadScheduledExecutor();
        purgeUsersLock_ = new ReentrantReadWriteLock();

        initProxyAccount(_config);
        initSchedulerForPurgingUsers(_config);
    }

    @Override
    public int principalToGid(String _principal)
    {
        log_.debug("principalToGid - _principal = {}", _principal);

        return Integer.parseInt(_principal);
    }

    @Override
    public String gidToPrincipal(int _id)
    {
        log_.debug("gidToPrincipal - _id = {}", _id);

        return String.valueOf(_id);
    }

    @Override
    public int principalToUid(String _principal)
    {
        log_.debug("principalToUid - _principal = {}", _principal);

        return Integer.parseInt(_principal);
    }

    @Override
    public String uidToPrincipal(int _id)
    {
        log_.debug("uidToPrincipal - _id = {}", _id);

        return String.valueOf(_id);
    }

    public int getUidForUser(String _name)
    {
        if (_name == null || _name.isEmpty())
        {
            log_.error("getUidForUser - Name argument is null. Returning uid {}", NOBODY_UID);
            return NOBODY_UID;
        }

        try (AutoClosedLock l = new AutoClosedLock(purgeUsersLock_.readLock()))
        {
            if (nameToUidMap_.containsKey(_name))
            {
                return nameToUidMap_.get(_name);
            }
        }
        catch (Exception e)
        {
            log_.error(e.getMessage());
        }

        __password p = libc_.getpwnam(_name);

        if (p == null)
        {
            log_.debug("getUidForUser - User not found. Returning uid {}", NOBODY_UID);
            return NOBODY_UID;
        }

        log_.debug("getUidForUser - User found! Returning uid {}", p.uid);

        return p.uid;
    }

    public int getGidForUser(String _name)
    {
        if (_name == null || _name.isEmpty())
        {
            log_.error("getGidForUser - Name argument is null. Returning gid {}", NOBODY_GID);
            return NOBODY_UID;
        }

        try (AutoClosedLock l = new AutoClosedLock(purgeUsersLock_.readLock()))
        {
            if (nameToUidMap_.containsKey(_name))
            {
                IRODSUser user = uidToNameMap_.get(nameToUidMap_.get(_name));
                return user.getGroupID();
            }
        }
        catch (Exception e)
        {
            log_.error(e.getMessage());
        }

        __password p = libc_.getpwnam(_name);

        if (p == null)
        {
            log_.debug("getGidForUser - User not found. Returning group name {}", NOBODY_GID);
            return NOBODY_GID;
        }

        log_.debug("getGidForUser - User found! Returning group name {}", p.gid);

        return p.gid;
    }

    public IRODSUser resolveUser(int _uid) throws IOException
    {
        log_.debug("resolveUser - _userID = {}", _uid);

        IRODSUser user = null;

        try (AutoClosedLock l = new AutoClosedLock(purgeUsersLock_.readLock()))
        {
            user = uidToNameMap_.get(_uid);
        }
        catch (Exception e)
        {
            throw new IOException("Could not retrieve username for uid.");
        }

        if (user == null)
        {
            log_.debug("resolveUser - User not found in mapping. Looking up UID ...");

            __password p = libc_.getpwuid(_uid);

            if (p == null)
            {
                throw new IOException("User does not exist in the system.");
            }

            user = new IRODSUser(p.name, p.uid, p.gid, config_, factory_);

            try (AutoClosedLock l = new AutoClosedLock(purgeUsersLock_.writeLock()))
            {
                nameToUidMap_.put(p.name, p.uid);
                uidToNameMap_.put(p.uid, user);
            }
            catch (Exception e)
            {
                throw new IOException("Could not create mapping between username -> uid -> iRODS user.");
            }

            log_.debug("resolveUser - userName = {}", p.name);
        }

        return user;
    }

    private void initProxyAccount(ServerConfig _config)
    {
        IRODSProxyAdminAccountConfig proxyConfig = _config.getIRODSProxyAdminAcctConfig();
        IRODSUser user = new IRODSUser(proxyConfig.getUsername(), 0, 0, config_, factory_);

        nameToUidMap_.put(proxyConfig.getUsername(), 0);
        uidToNameMap_.put(0, user);
    }

    private void initSchedulerForPurgingUsers(ServerConfig _config) throws IOException
    {
        NFSServerConfig nfsConfig = _config.getNfsServerConfig();
        PurgeUsersRunnable runnable = new PurgeUsersRunnable(config_, nameToUidMap_, uidToNameMap_, purgeUsersLock_);
        scheduler_.scheduleAtFixedRate(runnable, 0, nfsConfig.getUserInfoRefreshTimeInMinutes(), TimeUnit.MINUTES);
    }

    private static final class AutoClosedLock implements AutoCloseable
    {
        private final Lock lock_;

        AutoClosedLock(Lock _lock)
        {
            lock_ = _lock;
            lock_.lock();
        }

        @Override
        public void close() throws Exception
        {
            lock_.unlock();
        }
    }

    @FieldOrder({"name", "passwd", "uid", "gid", "gecos", "dir", "shell"})
    public static class __password extends Structure
    {
        public String name;
        public String passwd;
        public int uid;
        public int gid;
        public String gecos;
        public String dir;
        public String shell;
    }

    @FieldOrder({"name", "passwd", "gid", "mem"})
    public static class __group extends Structure
    {
        public String name;
        public String passwd;
        public int gid;
        public Pointer mem;
    }

    private static interface LibC extends Library
    {
        __password getpwnam(String name);

        __password getpwuid(int id);

        __group getgrnam(String name);

        __group getgrgid(int id);

        int getgrouplist(String user, int gid, int[] groups, IntByReference ngroups);
    }

    private final class PurgeUsersRunnable implements Runnable
    {
        private final Map<Path, Long> lastModified_;
        private final ServerConfig config_;
        private final Map<String, Integer> princToUid_;
        private final Map<Integer, IRODSUser> uidToPrinc_;
        private final ReadWriteLock lock_;

        PurgeUsersRunnable(ServerConfig _config,
                           Map<String, Integer> _princToUid,
                           Map<Integer, IRODSUser> _uidToPrinc,
                           ReadWriteLock _lock)
            throws IOException
        {
            Path PASSWD_PATH = Paths.get("/etc/passwd");
            Path SHADOW_PATH = Paths.get("/etc/shadow");

            lastModified_ = new HashMap<>();
            lastModified_.put(PASSWD_PATH, Files.getLastModifiedTime(PASSWD_PATH, new LinkOption[] {}).toMillis());
            lastModified_.put(SHADOW_PATH, Files.getLastModifiedTime(SHADOW_PATH, new LinkOption[] {}).toMillis());

            config_ = _config;
            princToUid_ = _princToUid;
            uidToPrinc_ = _uidToPrinc;
            lock_ = _lock;
        }

        @Override
        public void run()
        {
            class BoolRef
            {
                boolean value = false;
            }

            final BoolRef update = new BoolRef();

            lastModified_.forEach((k, v) -> {
                try
                {
                    long time = Files.getLastModifiedTime(k, new LinkOption[] {}).toMillis();

                    if (lastModified_.get(k) != time)
                    {
                        update.value = true;
                        lastModified_.put(k, time);
                    }
                }
                catch (Exception e)
                {
                    log_.error(e.getMessage());
                }
            });

            if (update.value)
            {
                log_.info("scheduler - Purging users ...");

                try (AutoClosedLock l = new AutoClosedLock(lock_.writeLock()))
                {
                    princToUid_.clear();
                    uidToPrinc_.clear();

                    IRODSIdMapper.this.initProxyAccount(config_);
                }
                catch (Exception e)
                {
                    log_.error(e.getMessage());
                }

                log_.info("scheduler - Purging users ... done.");
            }
        }
    }
}
