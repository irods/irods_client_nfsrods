package org.irods.nfsrods.vfs;

import static org.dcache.nfs.v4.xdr.nfs4_prot.ACCESS4_DELETE;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACCESS4_EXECUTE;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACCESS4_EXTEND;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACCESS4_LOOKUP;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACCESS4_MODIFY;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACCESS4_READ;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_ACCESS_ALLOWED_ACE_TYPE;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_ADD_FILE;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_ADD_SUBDIRECTORY;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_APPEND_DATA;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_DELETE;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_DELETE_CHILD;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_EXECUTE;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_GENERIC_EXECUTE;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_IDENTIFIER_GROUP;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_LIST_DIRECTORY;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_READ_ACL;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_READ_ATTRIBUTES;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_READ_DATA;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_READ_NAMED_ATTRS;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_SYNCHRONIZE;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_WRITE_ACL;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_WRITE_ATTRIBUTES;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_WRITE_DATA;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_WRITE_NAMED_ATTRS;
import static org.dcache.nfs.v4.xdr.nfs4_prot.ACE4_WRITE_OWNER;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.security.auth.Subject;

import org.dcache.auth.Subjects;
import org.dcache.nfs.ChimeraNFSException;
import org.dcache.nfs.status.ExistException;
import org.dcache.nfs.status.NoEntException;
import org.dcache.nfs.v4.NfsIdMapping;
import org.dcache.nfs.v4.xdr.aceflag4;
import org.dcache.nfs.v4.xdr.acemask4;
import org.dcache.nfs.v4.xdr.acetype4;
import org.dcache.nfs.v4.xdr.nfsace4;
import org.dcache.nfs.v4.xdr.uint32_t;
import org.dcache.nfs.v4.xdr.utf8str_mixed;
import org.dcache.nfs.vfs.AclCheckable;
import org.dcache.nfs.vfs.DirectoryEntry;
import org.dcache.nfs.vfs.DirectoryStream;
import org.dcache.nfs.vfs.FsStat;
import org.dcache.nfs.vfs.Inode;
import org.dcache.nfs.vfs.Stat;
import org.dcache.nfs.vfs.Stat.Type;
import org.dcache.nfs.vfs.VirtualFileSystem;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.packinstr.DataObjInp.OpenFlags;
import org.irods.jargon.core.protovalues.ErrorEnum;
import org.irods.jargon.core.protovalues.FilePermissionEnum;
import org.irods.jargon.core.protovalues.UserTypeEnum;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.CollectionAndDataObjectListAndSearchAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystemAO;
import org.irods.jargon.core.pub.UserAO;
import org.irods.jargon.core.pub.UserGroupAO;
import org.irods.jargon.core.pub.domain.ObjStat;
import org.irods.jargon.core.pub.domain.User;
import org.irods.jargon.core.pub.domain.UserFilePermission;
import org.irods.jargon.core.pub.domain.UserGroup;
import org.irods.jargon.core.pub.io.FileIOOperations;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.pub.io.IRODSRandomAccessFile;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry.ObjectType;
import org.irods.nfsrods.config.IRODSClientConfig;
import org.irods.nfsrods.config.IRODSProxyAdminAccountConfig;
import org.irods.nfsrods.config.NFSServerConfig;
import org.irods.nfsrods.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Longs;

public class IRODSVirtualFileSystem implements VirtualFileSystem, AclCheckable
{
    private static final Logger log_ = LoggerFactory.getLogger(IRODSVirtualFileSystem.class);

    private static final long FIXED_TIMESTAMP = System.currentTimeMillis();
    private static final FsStat FILE_SYSTEM_STAT_INFO = new FsStat(0, 0, 0, 0);

    private final IRODSAccessObjectFactory factory_;
    private final IRODSIdMapper idMapper_;
    private final InodeToPathMapper inodeToPathMapper_;
    private final IRODSAccount adminAcct_;
    private final ReadWriteAclWhitelist readWriteAclWhitelist_;
    private final boolean allowOverwriteOfExistingFiles_;
    private final boolean usingOracleDB_;

    private final MutableConfiguration<String, Stat> statObjectCacheConfig_; // Key: <username>_<path>
    private final Cache<String, Stat> statObjectCache_;                      // Key: <username>_<path>

    private final MutableConfiguration<String, Access> accessCacheConfig_; // Key: <user_id>#<access_mask>#<path>
    private final Cache<String, Access> accessCache_;                      // Key: <user_id>#<access_mask>#<path>

    private final MutableConfiguration<String, ObjectType> objectTypeCacheConfig_; // Key: <path>
    private final Cache<String, ObjectType> objectTypeCache_;                      // Key: <path>
    
    private final MutableConfiguration<String, Object> permsCacheConfig_; // Key: <path>
    private final Cache<String, Object> permsCache_;                      // Key: <path>
    
    private final MutableConfiguration<String, UserTypeEnum> userTypeCacheConfig_; // Key: <username>
    private final Cache<String, UserTypeEnum> userTypeCache_;                      // Key: <username>
    
    private final MutableConfiguration<String, Object> listOpCacheConfig_; // Key: <username>#<collection>
    private final Cache<String, Object> listOpCache_;                      // Key: <username>#<collection>
    
    // Special paths within iRODS.
    private final Path ROOT_COLLECTION;
    private final Path ZONE_COLLECTION;
    private final Path HOME_COLLECTION;
    private final Path PUBLIC_COLLECTION;
    private final Path TRASH_COLLECTION;

    public IRODSVirtualFileSystem(ServerConfig _config,
                                  IRODSAccessObjectFactory _factory,
                                  IRODSIdMapper _idMapper,
                                  CacheManager _cacheManager)
        throws DataNotFoundException, JargonException
    {
        factory_ = _factory;
        idMapper_ = _idMapper;
        inodeToPathMapper_ = new InodeToPathMapper(_config, _factory);

        IRODSClientConfig rodsSvrConfig = _config.getIRODSClientConfig();

        ROOT_COLLECTION = Paths.get("/");
        ZONE_COLLECTION = ROOT_COLLECTION.resolve(rodsSvrConfig.getZone());
        HOME_COLLECTION = ZONE_COLLECTION.resolve("home");
        PUBLIC_COLLECTION = ZONE_COLLECTION.resolve("public");
        TRASH_COLLECTION = ZONE_COLLECTION.resolve("trash");

        IRODSProxyAdminAccountConfig proxyConfig = rodsSvrConfig.getIRODSProxyAdminAcctConfig();
        Path proxyUserHomeCollection = Paths.get("/", rodsSvrConfig.getZone(), "home", proxyConfig.getUsername());
        // @formatter:off
        adminAcct_ = IRODSAccount.instance(rodsSvrConfig.getHost(),
                                           rodsSvrConfig.getPort(),
                                           proxyConfig.getUsername(),
                                           proxyConfig.getPassword(),
                                           proxyUserHomeCollection.toString(),
                                           rodsSvrConfig.getZone(),
                                           rodsSvrConfig.getDefaultResource());
        // @formatter:on

        readWriteAclWhitelist_ = new ReadWriteAclWhitelist(factory_, adminAcct_);
        
        NFSServerConfig nfsSvrConfig = _config.getNfsServerConfig();
        allowOverwriteOfExistingFiles_ = nfsSvrConfig.allowOverwriteOfExistingFiles();
        usingOracleDB_ = nfsSvrConfig.isUsingOracleDatabase();

        int expiryTime = nfsSvrConfig.getFileInfoRefreshTimeInMilliseconds();
        statObjectCacheConfig_ = newCacheConfig(expiryTime, Stat.class);
        statObjectCache_ = _cacheManager.createCache("stat_info_cache", statObjectCacheConfig_);

        expiryTime = nfsSvrConfig.getUserAccessRefreshTimeInMilliseconds();
        accessCacheConfig_ = newCacheConfig(expiryTime, Access.class);
        accessCache_ = _cacheManager.createCache("access_cache", accessCacheConfig_);

        expiryTime = nfsSvrConfig.getObjectTypeRefreshTimeInMilliseconds();
        objectTypeCacheConfig_ = newCacheConfig(expiryTime, ObjectType.class);
        objectTypeCache_ = _cacheManager.createCache("object_type_cache", objectTypeCacheConfig_);

        expiryTime = nfsSvrConfig.getUserPermissionsRefreshTimeInMilliseconds();
        permsCacheConfig_ = newCacheConfig(expiryTime, Object.class);
        permsCache_ = _cacheManager.createCache("perms_cache", permsCacheConfig_);

        expiryTime = nfsSvrConfig.getUserTypeRefreshTimeInMilliseconds();
        userTypeCacheConfig_ = newCacheConfig(expiryTime, UserTypeEnum.class);
        userTypeCache_ = _cacheManager.createCache("user_type_cache", userTypeCacheConfig_);

        expiryTime = nfsSvrConfig.getListOperationQueryResultsRefreshTimeInMilliseconds();
        listOpCacheConfig_ = newCacheConfig(expiryTime, Object.class);
        listOpCache_ = _cacheManager.createCache("list_op_cache", listOpCacheConfig_);
    }
    
    private static <V> MutableConfiguration<String, V> newCacheConfig(int _expiryTimeInMillis, Class<V> _class)
    {
        // @formatter:off
        return new MutableConfiguration<String, V>()
            .setTypes(String.class, _class)
            .setStoreByValue(false)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.MILLISECONDS, _expiryTimeInMillis)));
        // @formatter:on
    }

    @Override
    public int access(Inode _inode, int _mode) throws IOException
    {
        log_.debug("vfs::access");

        Path path = getPath(toInodeNumber(_inode));

        log_.debug("access - _inode path  = {}", path);
        log_.debug("access - _mode        = {}", _mode);
        log_.debug("access - _mode & ACCESS4_READ    = {}", (_mode & ACCESS4_READ));
        log_.debug("access - _mode & ACCESS4_LOOKUP  = {}", (_mode & ACCESS4_LOOKUP));
        log_.debug("access - _mode & ACCESS4_MODIFY  = {}", (_mode & ACCESS4_MODIFY));
        log_.debug("access - _mode & ACCESS4_EXTEND  = {}", (_mode & ACCESS4_EXTEND));
        log_.debug("access - _mode & ACCESS4_DELETE  = {}", (_mode & ACCESS4_DELETE));
        log_.debug("access - _mode & ACCESS4_EXECUTE = {}", (_mode & ACCESS4_EXECUTE));

        return _mode;
    }

    @Override
    public void commit(Inode _inode, long _offset, int _count) throws IOException
    {
        // NOP
    }

    @Override
    public Inode create(Inode _parent, Type _type, String _name, Subject _subject, int _mode) throws IOException
    {
        log_.debug("vfs::create");

        if (Type.REGULAR != _type)
        {
            throw new IllegalArgumentException("Invalid file type [" + _type + "]");
        }

        Path parentPath = getPath(toInodeNumber(_parent));
        String path = parentPath.resolve(_name).toString();

        log_.debug("create - _parent      = {}", parentPath);
        log_.debug("create - _type        = {}", _type);
        log_.debug("create - _name        = {}", _name);
        log_.debug("create - _subject     = {}", _subject);
        log_.debug("create - _subject uid = {}", Subjects.getUid(_subject));
        log_.debug("create - _subject gid = {}", Subjects.getPrimaryGid(_subject));
        log_.debug("create - _mode        = {}", Stat.modeToString(_mode));

        try
        {
            IRODSAccount acct = idMapper_.resolveUser((int) Subjects.getUid(_subject)).getAccount();
            IRODSFileFactory ff = factory_.getIRODSFileFactory(acct);
            IRODSFile newFile = ff.instanceIRODSFile(path);

            log_.debug("create - Creating new file [{}] ...", newFile);

            try (AutoClosedIRODSFile ac = new AutoClosedIRODSFile(newFile))
            {
                if (!newFile.createNewFile())
                {
                    throw new IOException("Failed to create new file in iRODS");
                }
            }
            
            long newInodeNumber = inodeToPathMapper_.getAndIncrementFileID();
            inodeToPathMapper_.map(newInodeNumber, path);

            return toFh(newInodeNumber);
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public byte[] directoryVerifier(Inode _inode) throws IOException
    {
        return DirectoryStream.ZERO_VERIFIER;
    }
    
    @Override
    public nfsace4[] getAcl(Inode _inode) throws IOException
    {
        log_.debug("vfs::getAcl");

        String path = getPath(toInodeNumber(_inode)).toString();

        log_.debug("getAcl - _inode path = {}", path);

        List<nfsace4> acl = new ArrayList<>();

        try
        {
            for (UserFilePermission p : getPermissions(path))
            {
                log_.debug("getAcl - permission = {}", p);

                nfsace4 ace = new nfsace4();

                ace.who = new utf8str_mixed(p.getUserName() + "@");
                ace.type = new acetype4(new uint32_t(ACE4_ACCESS_ALLOWED_ACE_TYPE));
                ace.flag = new aceflag4(new uint32_t(0));
                
                if (p.getUserType() == UserTypeEnum.RODS_GROUP)
                {
                    ace.flag.value.value = ACE4_IDENTIFIER_GROUP;
                }
                
                switch (p.getFilePermissionEnum())
                {
                    case OWN:
                        ace.access_mask = new acemask4(new uint32_t(ACE4_READ_DATA | ACE4_WRITE_DATA | ACE4_APPEND_DATA | ACE4_DELETE | ACE4_WRITE_OWNER));
                        break;
                        
                    case WRITE:
                        ace.access_mask = new acemask4(new uint32_t(ACE4_READ_DATA | ACE4_WRITE_DATA | ACE4_APPEND_DATA));
                        break;

                    case READ:
                        ace.access_mask = new acemask4(new uint32_t(ACE4_READ_DATA));
                        break;
                        
                    default:
                        continue;
                }
                
                acl.add(ace);
            }
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }

        return acl.toArray(new nfsace4[0]);
    }
    
    private UserFilePermission toUserFilePermission(nfsace4 _ace) throws JargonException
    {
        String who = _ace.who.toString().split("[@]")[0];

        log_.debug("toUserFilePermission - ace who         = {}", who);
        log_.debug("toUserFilePermission - ace type        = {}", _ace.type.value.value);
        log_.debug("toUserFilePermission - ace flag        = {}", _ace.flag.value.value);
        log_.debug("toUserFilePermission - ace access mask = {}", _ace.access_mask.value.value);
        
        if (ACE4_ACCESS_ALLOWED_ACE_TYPE != _ace.type.value.value)
        {
            log_.error("toUserFilePermission - Not an ALLOW ACE type, returning null ...");
            return null;
        }
        
        UserAO uao = factory_.getUserAO(adminAcct_);
        User user = uao.findByName(who);

        if (user == null)
        {
            log_.error("toUserFilePermission - User [{}] not found in iRODS, returning null ...", who);
            return null;
        }

        int accessMask = _ace.access_mask.value.value;

        log_.debug("toUserFilePermission - _accessMask & ACE4_READ_DATA         = {}", accessMask & ACE4_READ_DATA);
        log_.debug("toUserFilePermission - _accessMask & ACE4_LIST_DIRECTORY    = {}", accessMask & ACE4_LIST_DIRECTORY);
        log_.debug("toUserFilePermission - _accessMask & ACE4_WRITE_DATA        = {}", accessMask & ACE4_WRITE_DATA);
        log_.debug("toUserFilePermission - _accessMask & ACE4_ADD_FILE          = {}", accessMask & ACE4_ADD_FILE);
        log_.debug("toUserFilePermission - _accessMask & ACE4_APPEND_DATA       = {}", accessMask & ACE4_APPEND_DATA);
        log_.debug("toUserFilePermission - _accessMask & ACE4_ADD_SUBDIRECTORY  = {}", accessMask & ACE4_ADD_SUBDIRECTORY);
        log_.debug("toUserFilePermission - _accessMask & ACE4_READ_NAMED_ATTRS  = {}", accessMask & ACE4_READ_NAMED_ATTRS);
        log_.debug("toUserFilePermission - _accessMask & ACE4_WRITE_NAMED_ATTRS = {}", accessMask & ACE4_WRITE_NAMED_ATTRS);
        log_.debug("toUserFilePermission - _accessMask & ACE4_EXECUTE           = {}", accessMask & ACE4_EXECUTE);
        log_.debug("toUserFilePermission - _accessMask & ACE4_DELETE_CHILD      = {}", accessMask & ACE4_DELETE_CHILD);
        log_.debug("toUserFilePermission - _accessMask & ACE4_READ_ATTRIBUTES   = {}", accessMask & ACE4_READ_ATTRIBUTES);
        log_.debug("toUserFilePermission - _accessMask & ACE4_WRITE_ATTRIBUTES  = {}", accessMask & ACE4_WRITE_ATTRIBUTES);
        log_.debug("toUserFilePermission - _accessMask & ACE4_DELETE            = {}", accessMask & ACE4_DELETE);
        log_.debug("toUserFilePermission - _accessMask & ACE4_READ_ACL          = {}", accessMask & ACE4_READ_ACL);
        log_.debug("toUserFilePermission - _accessMask & ACE4_WRITE_ACL         = {}", accessMask & ACE4_WRITE_ACL);
        log_.debug("toUserFilePermission - _accessMask & ACE4_WRITE_OWNER       = {}", accessMask & ACE4_WRITE_OWNER);
        log_.debug("toUserFilePermission - _accessMask & ACE4_SYNCHRONIZE       = {}", accessMask & ACE4_SYNCHRONIZE);

        boolean allowReading = (accessMask & ACE4_READ_DATA) != 0;
        boolean allowWriting = (accessMask & (ACE4_WRITE_DATA | ACE4_APPEND_DATA)) != 0;
        boolean allowExecution = (accessMask & ACE4_WRITE_OWNER) != 0;
        
        UserFilePermission perm = new UserFilePermission();

        perm.setUserName(who);
        perm.setUserId(user.getId());
        perm.setUserType(user.getUserType());
        perm.setUserZone(user.getZone());
        
        if (allowExecution)
        {
            perm.setFilePermissionEnum(FilePermissionEnum.OWN);
        }
        else if (allowWriting)
        {
            perm.setFilePermissionEnum(FilePermissionEnum.WRITE);
        }
        else if (allowReading)
        {
            perm.setFilePermissionEnum(FilePermissionEnum.READ);
        }
        else
        {
            log_.error("toUserFilePermission - Cannot map ACE mask to iRODS permission, returning null ...");
            return null;
        }

        return perm;
    }
    
    private List<UserFilePermission> toUserFilePermissionList(nfsace4[] _acl) throws JargonException
    {
        List<UserFilePermission> perms = new ArrayList<>();

        for (nfsace4 ace : _acl)
        {
            UserFilePermission perm = toUserFilePermission(ace);
            
            if (perm != null)
            {
                perms.add(perm);
            }
        }
        
        return perms;
    }

    private static boolean containsPermission(List<UserFilePermission> _perms, UserFilePermission _p)
    {
        // @formatter:off
        return _perms.stream().anyMatch(perm -> _p.getUserId().equals(perm.getUserId()) &&
                                                        _p.getUserName().equals(perm.getUserName()) &&
                                                        _p.getUserZone().equals(perm.getUserZone()) &&
                                                        _p.getUserType() == perm.getUserType() &&
                                                        _p.getFilePermissionEnum() == perm.getFilePermissionEnum());
        // @formatter:on
    }

    private static final class AclDiff
    {
        public final List<UserFilePermission> added = new ArrayList<>();
        public final List<UserFilePermission> removed = new ArrayList<>();
    }

    private AclDiff diffAcl(String _path, nfsace4[] _acl) throws JargonException
    {
        List<UserFilePermission> curAcl = getPermissions(_path);
        List<UserFilePermission> newAcl = toUserFilePermissionList(_acl);
        AclDiff diff = new AclDiff();
        
        log_.debug("diffAcl - current acl = {}", curAcl);
        log_.debug("diffAcl - new acl = {}", newAcl);

        diff.added.addAll(newAcl.stream().filter(p -> !containsPermission(curAcl, p)).collect(Collectors.toList()));
        diff.removed.addAll(curAcl.stream().filter(p -> !containsPermission(newAcl, p)).collect(Collectors.toList()));

        log_.debug("diffAcl - (+) = {}", diff.added);
        log_.debug("diffAcl - (-) = {}", diff.removed);
        
        return diff;
    }
    
    private void setAccessPermissionInAdminMode(String _path, UserFilePermission _perm) throws JargonException
    {
        switch (getObjectType(_path))
        {
            case COLLECTION_HEURISTIC_STANDIN:
            case COLLECTION:
            {
                CollectionAO cao = factory_.getCollectionAO(adminAcct_);
                boolean recursive = false;

                switch (_perm.getFilePermissionEnum())
                {
                    // @formatter:off
                    case OWN:   cao.setAccessPermissionOwnAsAdmin(_perm.getUserZone(), _path, _perm.getUserName(), recursive); break;
                    case WRITE: cao.setAccessPermissionWriteAsAdmin(_perm.getUserZone(), _path, _perm.getUserName(), recursive); break;
                    case READ:  cao.setAccessPermissionReadAsAdmin(_perm.getUserZone(), _path, _perm.getUserName(), recursive); break;
                    // @formatter:on
                    default:
                }

                break;
            }

            case DATA_OBJECT:
            {
                DataObjectAO dao = factory_.getDataObjectAO(adminAcct_);

                switch (_perm.getFilePermissionEnum())
                {
                    // @formatter:off
                    case OWN:   dao.setAccessPermissionOwnInAdminMode(_perm.getUserZone(), _path, _perm.getUserName()); break;
                    case WRITE: dao.setAccessPermissionWriteInAdminMode(_perm.getUserZone(), _path, _perm.getUserName()); break;
                    case READ:  dao.setAccessPermissionReadInAdminMode(_perm.getUserZone(), _path, _perm.getUserName()); break;
                    // @formatter:on
                    default:
                }

                break;
            }
                
            default:
                log_.error("setAccessPermissionInAdminMode - Invalid object type for path [{}].", _path);
        }
    }

    @Override
    public void setAcl(Inode _inode, nfsace4[] _acl) throws IOException
    {
        log_.debug("vfs::setAcl");
        
        if (0 == _acl.length)
        {
            log_.warn("setAcl - Skipping empty ACL request.");
            return;
        }

        String path = getPath(toInodeNumber(_inode)).toString();

        log_.debug("setAcl - _inode path = {}", path);
        log_.debug("setAcl - _acl length = {}", _acl.length);
        
        try
        {
            AclDiff diff = diffAcl(path, _acl);

            log_.debug("setAcl - Updating permissions ...");
            
            if (!diff.removed.isEmpty())
            {
                log_.debug("setAcl - Removing permissions ...");
            }

            for (UserFilePermission p : diff.removed)
            {
                switch (getObjectType(path))
                {
                    case COLLECTION_HEURISTIC_STANDIN:
                    case COLLECTION:
                        CollectionAO cao = factory_.getCollectionAO(adminAcct_);
                        boolean recursive = false;
                        cao.removeAccessPermissionForUserAsAdmin(p.getUserZone(), path, p.getUserName(), recursive);
                        break;

                    case DATA_OBJECT:
                        DataObjectAO dao = factory_.getDataObjectAO(adminAcct_);
                        dao.removeAccessPermissionsForUserInAdminMode(p.getUserZone(), path, p.getUserName());
                        break;
                        
                    default:
                        log_.error("setAcl - Invalid object type for path [{}].", path);
                }
            }

            if (!diff.added.isEmpty())
            {
                log_.debug("setAcl - Adding permissions ...");
            }

            for (UserFilePermission p : diff.added)
            {
                setAccessPermissionInAdminMode(path, p);
            }
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }
    
    private boolean isAllowedToReadWriteAclForPath(String _userName, String _path) throws JargonException
    {
        if (readWriteAclWhitelist_.contains(_userName))
        {
            String prefix = readWriteAclWhitelist_.getPathPrefix(_userName);

            log_.debug("isAllowedToReadWriteAclForPath - User path prefix = {}", prefix);

            if (Paths.get(_path).startsWith(prefix))
            {
                log_.debug("isAllowedToReadWriteAclForPath - User [{}] has special " +
                           "privileges to read/write ACLs, access allowed.", _userName);
                return true;
            }
        }

        log_.debug("isAllowedToReadWriteAclForPath - User [{}] does not have special " +
                   "privileges to read/write ACLs. Checking groups ...", _userName);

        UserGroupAO ugao = factory_.getUserGroupAO(adminAcct_);
        List<UserGroup> groupsContainingUser = ugao.findUserGroupsForUser(_userName);

        for (UserGroup ug : groupsContainingUser)
        {
            String groupName = ug.getUserGroupName();

            if (readWriteAclWhitelist_.contains(groupName))
            {
                String prefix = readWriteAclWhitelist_.getPathPrefix(groupName);

                log_.debug("isAllowedToReadWriteAclForPath - Group path prefix = {}", prefix);

                if (Paths.get(_path).startsWith(prefix))
                {
                    log_.debug("isAllowedToReadWriteAclForPath - Group [{}] has special " +
                               "privileges to read/write ACLs, access allowed.", groupName);
                    return true;
                }
            }
        }

        log_.debug("isAllowedToReadWriteAclForPath - User [{}] is not a member of " +
                   "any group with special privileges.", _userName);
        
        return false;
    }

    @Override
    public Access checkAcl(Subject _subject, Inode _inode, int _accessMask) throws ChimeraNFSException, IOException
    {
        log_.debug("vfs::checkAcl");
        
        String userName = null;

        try
        {
            userName = idMapper_.resolveUser((int) Subjects.getUid(_subject)).getAccount().getUserName();
        }
        catch (Exception e)
        {
            log_.error("checkAcl - Could not resolve OS user id to an iRODS user name.");
            return Access.DENY;
        }

        String path = getPath(toInodeNumber(_inode)).toString();
        
        // Key   (String) => <user_id>#<access_mask>#<path>
        // Value (Access) => ALLOW/DENY
        // Cached stat information must be scoped to the user due to the permissions
        // possibly being changed depending on who is accessing the NFS server.
        final String cachedAccessKey = Subjects.getUid(_subject) + "#" + _accessMask + "#" + path;
        Access access = accessCache_.get(cachedAccessKey);

        if (null != access)
        {
            log_.debug("checkAcl - Returning cached access result for [{}] ...", path);
            return access;
        }

        if (isSpecialCollection(path))
        {
            log_.debug("checkAcl - Object is a special collection created by iRODS, access allowed.");
            accessCache_.put(cachedAccessKey, Access.ALLOW);
            return Access.ALLOW;
        }
        
        // @formatter:off
        log_.debug("checkAcl - _subject uid         = {}", Subjects.getUid(_subject));
        log_.debug("checkAcl - _subject primary gid = {}", Subjects.getPrimaryGid(_subject));
        log_.debug("checkAcl - _inode path          = {}", path);
        log_.debug("checkAcl - _accessMask          = {}", _accessMask);
        log_.debug("checkAcl - username             = {}", userName);

        // access mask values
        log_.debug("checkAcl - _accessMask & ACE4_READ_DATA         = {}", _accessMask & ACE4_READ_DATA);
        log_.debug("checkAcl - _accessMask & ACE4_LIST_DIRECTORY    = {}", _accessMask & ACE4_LIST_DIRECTORY);
        log_.debug("checkAcl - _accessMask & ACE4_WRITE_DATA        = {}", _accessMask & ACE4_WRITE_DATA);
        log_.debug("checkAcl - _accessMask & ACE4_ADD_FILE          = {}", _accessMask & ACE4_ADD_FILE);
        log_.debug("checkAcl - _accessMask & ACE4_APPEND_DATA       = {}", _accessMask & ACE4_APPEND_DATA);
        log_.debug("checkAcl - _accessMask & ACE4_ADD_SUBDIRECTORY  = {}", _accessMask & ACE4_ADD_SUBDIRECTORY);
        log_.debug("checkAcl - _accessMask & ACE4_READ_NAMED_ATTRS  = {}", _accessMask & ACE4_READ_NAMED_ATTRS);
        log_.debug("checkAcl - _accessMask & ACE4_WRITE_NAMED_ATTRS = {}", _accessMask & ACE4_WRITE_NAMED_ATTRS);
        log_.debug("checkAcl - _accessMask & ACE4_EXECUTE           = {}", _accessMask & ACE4_EXECUTE);
        log_.debug("checkAcl - _accessMask & ACE4_DELETE_CHILD      = {}", _accessMask & ACE4_DELETE_CHILD);
        log_.debug("checkAcl - _accessMask & ACE4_READ_ATTRIBUTES   = {}", _accessMask & ACE4_READ_ATTRIBUTES);
        log_.debug("checkAcl - _accessMask & ACE4_WRITE_ATTRIBUTES  = {}", _accessMask & ACE4_WRITE_ATTRIBUTES);
        log_.debug("checkAcl - _accessMask & ACE4_DELETE            = {}", _accessMask & ACE4_DELETE);
        log_.debug("checkAcl - _accessMask & ACE4_READ_ACL          = {}", _accessMask & ACE4_READ_ACL);
        log_.debug("checkAcl - _accessMask & ACE4_WRITE_ACL         = {}", _accessMask & ACE4_WRITE_ACL);
        log_.debug("checkAcl - _accessMask & ACE4_WRITE_OWNER       = {}", _accessMask & ACE4_WRITE_OWNER);
        log_.debug("checkAcl - _accessMask & ACE4_SYNCHRONIZE       = {}", _accessMask & ACE4_SYNCHRONIZE);
        // @formatter:on
        
        try
        {
            // Collections are always executable, so allow access.
            if ((_accessMask & ACE4_GENERIC_EXECUTE) != 0 && getObjectType(path) == ObjectType.COLLECTION)
            {
                log_.debug("checkAcl - Object is a collection, access allowed.");
                accessCache_.put(cachedAccessKey, Access.ALLOW);
                return Access.ALLOW;
            }

            // Detect if modifications to attributes or ACLs was requested (e.g. nfs4_getfacl/nfs4_setfacl).
            if ((_accessMask & (ACE4_READ_ACL | ACE4_WRITE_ACL | ACE4_READ_ATTRIBUTES | ACE4_WRITE_ATTRIBUTES)) != 0)
            {
                // Administrators are always allowed to read/write attributes and ACLs.
                if (isAdministrator(userName))
                {
                    log_.debug("checkAcl - User is an iRODS administrator, access allowed.");
                    accessCache_.put(cachedAccessKey, Access.ALLOW);
                    return Access.ALLOW;
                }
                
                // Checks the ACL whitelist to see if the user should be allowed to read/write attributes and ACLs.
                if (isAllowedToReadWriteAclForPath(userName, path))
                {
                    log_.debug("checkAcl - User [{}] has special privileges to read/write ACLs, access allowed.", userName);
                    accessCache_.put(cachedAccessKey, Access.ALLOW);
                    return Access.ALLOW;
                }
            }
            else
            {
                log_.debug("checkAcl - No attribute/ACL operations requested.");
            }

            Optional<UserFilePermission> perm = getHighestUserPermissionForPath(userName, path);
            
            if (!perm.isPresent())
            {
                log_.debug("checkAcl - User has no permission to access object, access denied.");
                accessCache_.put(cachedAccessKey, Access.DENY);
                return Access.DENY;
            }

            switch (perm.get().getFilePermissionEnum())
            {
                case OWN:
                    log_.debug("checkAcl - User is an owner, access allowed.");
                    accessCache_.put(cachedAccessKey, Access.ALLOW);
                    return Access.ALLOW;

                case WRITE:
                    if ((_accessMask & (ACE4_WRITE_DATA | ACE4_WRITE_ATTRIBUTES | ACE4_APPEND_DATA |
                                        ACE4_READ_DATA | ACE4_READ_ATTRIBUTES | ACE4_READ_ACL | ACE4_EXECUTE)) != 0)
                    {
                        log_.debug("checkAcl - User has write permission, access allowed.");
                        accessCache_.put(cachedAccessKey, Access.ALLOW);
                        return Access.ALLOW;
                    }
                    break;

                case READ:
                    if ((_accessMask & (ACE4_READ_DATA | ACE4_READ_ATTRIBUTES | ACE4_READ_ACL | ACE4_EXECUTE)) != 0)
                    {
                        log_.debug("checkAcl - User has read permission, access allowed.");
                        accessCache_.put(cachedAccessKey, Access.ALLOW);
                        return Access.ALLOW;
                    }
                    break;

                default:
                    break;
            }
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }

        log_.debug("checkAcl - Access denied.");
        accessCache_.put(cachedAccessKey, Access.DENY);

        return Access.DENY;
    }

    @Override
    public AclCheckable getAclCheckable()
    {
        return this;
    }

    @Override
    public FsStat getFsStat() throws IOException
    {
        return FILE_SYSTEM_STAT_INFO;
    }

    @Override
    public NfsIdMapping getIdMapper()
    {
        return idMapper_;
    }

    @Override
    public Inode getRootInode() throws IOException
    {
        return toFh(1);
    }

    @Override
    public Stat getattr(Inode _inode) throws IOException
    {
        log_.debug("vfs::getattr");

        long inodeNumber = toInodeNumber(_inode);
        Path path = getPath(inodeNumber);

        try
        {
            return statPath(path, inodeNumber);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public boolean hasIOLayout(Inode _inode) throws IOException
    {
        return false;
    }

    @Override
    public Inode link(Inode _parent, Inode _existing, String _target, Subject _subject) throws IOException
    {
        throw new UnsupportedOperationException("Not supported");
    }
    
    private static final class CachedListingGenQueryResult
    {
        Date collectionLastModified;
        List<CollectionAndDataObjectListingEntry> entries;
    }
    
    private List<CollectionAndDataObjectListingEntry> listDataObjectsAndCollectionsUnderPathWithPermissions(IRODSAccount _acct,
                                                                                                            String _path)
        throws JargonException
    {
        final String cachedObjectKey = _acct.getUserName() + "#" + _path;
        CachedListingGenQueryResult cachedResult = (CachedListingGenQueryResult ) listOpCache_.get(cachedObjectKey);

        CollectionAndDataObjectListAndSearchAO lao = factory_.getCollectionAndDataObjectListAndSearchAO(_acct);
        ObjStat objStat = lao.retrieveObjectStatForPath(_path);
        
        if (null != cachedResult)
        {
            // Return the cached results if the collection's contents has not changed
            // since we last saw it.
            if (objStat.getModifiedAt().equals(cachedResult.collectionLastModified))
            {
                return cachedResult.entries;
            }
            
            cachedResult.entries.clear();
        }
        else
        {
            cachedResult = new CachedListingGenQueryResult();
            cachedResult.entries = new ArrayList<>();
        }

        cachedResult.collectionLastModified = objStat.getModifiedAt();

        List<CollectionAndDataObjectListingEntry> entries = cachedResult.entries;
        int pagingIndex = 0;

        do
        {
            entries.addAll(lao.listCollectionsUnderPathWithPermissions(_path, pagingIndex, usingOracleDB_));
            pagingIndex = entries.size();
        }
        while (pagingIndex > 0 && !entries.get(pagingIndex - 1).isLastResult());

        pagingIndex = 0;

        do
        {
            entries.addAll(lao.listDataObjectsUnderPathWithPermissions(_path, pagingIndex, usingOracleDB_));
            pagingIndex = entries.size();
        }
        while (pagingIndex > 0 && !entries.get(pagingIndex - 1).isLastResult());
        
        listOpCache_.put(cachedObjectKey, cachedResult);

        return entries;
    }
    
    @Override
    public DirectoryStream list(Inode _inode, byte[] _verifier, long _cookie) throws IOException
    {
        log_.debug("vfs::list");
        log_.debug("list - _cookie = {}", _cookie);

        List<DirectoryEntry> list = new ArrayList<>();

        try
        {
            IRODSAccount acct = getCurrentIRODSUser().getAccount();

            // Get the list of groups the user is a member of.
            UserGroupAO ugao = factory_.getUserGroupAO(adminAcct_);
            List<UserGroup> groupsContainingUser = ugao.findUserGroupsForUser(acct.getUserName());
        
            Path parentPath = getPath(toInodeNumber(_inode));
            log_.debug("list - Listing contents of [{}] ...", parentPath);

            String irodsAbsPath = parentPath.toString();

            List<CollectionAndDataObjectListingEntry>
                entries = listDataObjectsAndCollectionsUnderPathWithPermissions(acct, irodsAbsPath);
            log_.debug("list - found {} entries.", entries.size());
            
            // 0, 1, and 2 are reserved cookie values.
            long dirEntryCookie = Math.max(2, _cookie);

            // "i" is used to index into the list of entries, therefore it must be greater
            // than or equal to zero. The starting value of "i" is always "dirEntryCookie - 2"
            // unless "_cookie" is zero.
            for (long i = Math.max(0, dirEntryCookie - 2); i < entries.size(); ++i)
            {
                // If the "_cookie" value is less than "dirEntryCookie", that means the path needs
                // to be added to the listing. Previously handled entries will have "dirEntryCookie"
                // values that are less than or equal to "_cookie". The following if-statement protects
                // the server from hitting the readdir/duplicate-cookie issue.
                if (++dirEntryCookie > _cookie)
                {
                    CollectionAndDataObjectListingEntry e = entries.get((int) i);

                    Path path = parentPath.resolve(e.getPathOrName());
                    log_.debug("list - Entry = {}", path);

                    Long inodeNumber = inodeToPathMapper_.getInodeNumberByPath(path);

                    if (null == inodeNumber)
                    {
                        inodeNumber = inodeToPathMapper_.getAndIncrementFileID();
                        inodeToPathMapper_.map(inodeNumber, path);
                    }
                    
                    Stat stat = statPath(path, inodeNumber, e, groupsContainingUser);
                    Inode inode = toFh(inodeNumber);
                    list.add(new DirectoryEntry(path.getFileName().toString(), inode, stat, dirEntryCookie));
                }
            }
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }

        // Uses the DirectoryStream.ZERO_VERIFIER. See READDIR operation of RFC 7530.
        return new DirectoryStream(list);
    }

    @Override
    public Inode lookup(Inode _parent, String _path) throws IOException
    {
        log_.debug("vfs::lookup");

        Path parentPath = getPath(toInodeNumber(_parent));
        Path targetPath = parentPath.resolve(_path);

        log_.debug("lookup - _path   = {}", _path);
        log_.debug("lookup - _parent = {}", parentPath);
        log_.debug("lookup - Looking up [{}] ...", targetPath);

        try
        {
            CollectionAndDataObjectListAndSearchAO lao = null;
            lao = factory_.getCollectionAndDataObjectListAndSearchAO(adminAcct_);
            boolean isTargetValid = false;

            try
            {
                isTargetValid = (lao.retrieveObjectStatForPath(targetPath.toString()) != null);
            }
            catch (Exception e)
            {
            }

            // If the target path is valid, then return an inode object created from
            // the user's mapped paths. Else, create a new mapping and return an
            // inode object for the new mapping.
            if (isTargetValid)
            {
                Long inodeNumber = inodeToPathMapper_.getInodeNumberByPath(targetPath);

                if (null != inodeNumber)
                {
                    return toFh(inodeNumber);
                }

                inodeNumber = inodeToPathMapper_.getAndIncrementFileID();
                inodeToPathMapper_.map(inodeNumber, targetPath);

                return toFh(inodeNumber);
            }

            Long inodeNumber = inodeToPathMapper_.getInodeNumberByPath(targetPath);

            // If the target path is not registered in iRODS and NFSRODS has previously
            // mapped it, then unmap it. This keeps NFSRODS in sync with iRODS.
            if (null != inodeNumber)
            {
                inodeToPathMapper_.unmap(inodeNumber, targetPath);
            }

            // It is VERY important that this exception is thrown here.
            // It affects how NFS4J continues processing the request.
            throw new NoEntException("Path does not exist");
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public Inode mkdir(Inode _inode, String _path, Subject _subject, int _mode) throws IOException
    {
        log_.debug("vfs::mkdir");

        try
        {
            Path parentPath = getPath(toInodeNumber(_inode));

            IRODSAccount acct = getCurrentIRODSUser().getAccount();
            IRODSFile file = factory_.getIRODSFileFactory(acct).instanceIRODSFile(parentPath.toString(), _path);

            file.mkdir();
            file.close();

            long inodeNumber = inodeToPathMapper_.getAndIncrementFileID();
            inodeToPathMapper_.map(inodeNumber, file.getAbsolutePath());

            return toFh(inodeNumber);
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public boolean move(Inode _srcParentInode, String _srcFilename, Inode _dstParentInode, String _dstFilename)
        throws IOException
    {
        log_.debug("vfs::move");

        Path srcParentPath = getPath(toInodeNumber(_srcParentInode));
        Path dstParentPath = getPath(toInodeNumber(_dstParentInode));

        log_.debug("move - _inode path (src) = {}", srcParentPath);
        log_.debug("move - _inode path (dst) = {}", dstParentPath);
        log_.debug("move - _srcName          = {}", _srcFilename);
        log_.debug("move - _dstName          = {}", _dstFilename);

        IRODSAccount acct = getCurrentIRODSUser().getAccount();

        try
        {
            Path srcPath = srcParentPath.resolve(_srcFilename);
            Path dstPath = dstParentPath.resolve(_dstFilename);

            log_.debug("move - Source path      = {}", srcPath);
            log_.debug("move - Destination path = {}", dstPath);

            IRODSFileFactory ff = factory_.getIRODSFileFactory(acct);
            IRODSFile srcFile = ff.instanceIRODSFile(srcPath.toString());
            IRODSFile dstFile = ff.instanceIRODSFile(dstPath.toString());
            
            // Capture the existence of the destination path. This is required to properly
            // update the mappings between inode numbers and paths.
            final boolean unmapDstPath = dstFile.exists();

            try (AutoClosedIRODSFile ac0 = new AutoClosedIRODSFile(srcFile);
                 AutoClosedIRODSFile ac1 = new AutoClosedIRODSFile(dstFile))
            {
                IRODSFileSystemAO fsao = factory_.getIRODSFileSystemAO(acct);

                if (srcFile.isFile())
                {
                    log_.debug("move - Renaming data object from [{}] to [{}] ...", srcPath, dstPath);
                    fsao.renameFile(srcFile, dstFile, allowOverwriteOfExistingFiles_);
                }
                else if (srcFile.isDirectory())
                {
                    log_.debug("move - Renaming collection from [{}] to [{}] ...", srcPath, dstPath);
                    fsao.renameDirectory(srcFile, dstFile);
                }
            }

            log_.debug("move - Updating mappings between paths and inodes ...");

            // If the destination path exists in iRODS and has been mapped by the NFSRODS
            // server, then that path must be unmapped before remapping to avoid an exception.
            if (unmapDstPath)
            {
                Long inodeNumber = inodeToPathMapper_.getInodeNumberByPath(dstPath);

                if (inodeNumber != null)
                {
                    inodeToPathMapper_.unmap(inodeNumber, dstPath);
                }
            }

            inodeToPathMapper_.remap(getInodeNumber(srcPath), srcPath, dstPath);

            return true;
        }
        catch (JargonException e)
        {
            log_.error("{} [iRODS Error Code = {}]", e.getMessage(), e.getUnderlyingIRODSExceptionCode());
            
            final ErrorEnum error = ErrorEnum.valueOf(e.getUnderlyingIRODSExceptionCode());

            if (error == ErrorEnum.CAT_NAME_EXISTS_AS_DATAOBJ)
            {
                throw new ExistException(e.getMessage());
            }

            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public Inode parentOf(Inode _inode) throws IOException
    {
        log_.debug("vfs::parentOf");
        Path path = getPath(toInodeNumber(_inode));
        return toFh(getInodeNumber(path.getParent()));
    }

    @Override
    public int read(Inode _inode, byte[] _data, long _offset, int _count) throws IOException
    {
        log_.debug("vfs::read");

        try
        {
            Path path = getPath(toInodeNumber(_inode));

            log_.debug("read - _inode path  = {}", path);
            log_.debug("read - _data.length = {}", _data.length);
            log_.debug("read - _offset      = {}", _offset);
            log_.debug("read - _count       = {}", _count);

            IRODSAccount acct = getCurrentIRODSUser().getAccount();
            IRODSFileFactory ff = factory_.getIRODSFileFactory(acct);
            IRODSRandomAccessFile file = ff.instanceIRODSRandomAccessFile(path.toString(), OpenFlags.READ);
            
            try (AutoClosedIRODSRandomAccessFile ac = new AutoClosedIRODSRandomAccessFile(file))
            {
                file.seek(_offset, FileIOOperations.SeekWhenceType.SEEK_START);
                return file.read(_data, 0, _count);
            }
        }
        catch (IOException | JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public String readlink(Inode _inode) throws IOException
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void remove(Inode _parent, String _path) throws IOException
    {
        log_.debug("vfs::remove");
        
        IRODSAccount acct = getCurrentIRODSUser().getAccount();

        try
        {
            Path parentPath = getPath(toInodeNumber(_parent));

            log_.debug("remove - _parent = {}", parentPath);
            log_.debug("remove - _path   = {}", _path);

            Path objectPath = parentPath.resolve(_path);
            IRODSFileFactory ff = factory_.getIRODSFileFactory(acct);
            IRODSFile file = ff.instanceIRODSFile(objectPath.toString());

            log_.debug("remove - Removing [{}] ...", objectPath);

            try (AutoClosedIRODSFile ac = new AutoClosedIRODSFile(file))
            {
                if (!file.delete())
                {
                    throw new IOException("Failed to delete object in iRODS");
                }
            }

            inodeToPathMapper_.unmap(getInodeNumber(objectPath), objectPath);

            log_.debug("remove - [{}] removed.", objectPath);
        }
        catch (JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }

    @Override
    public void setattr(Inode _inode, Stat _stat) throws IOException
    {
        log_.debug("vfs::setattr");
        log_.debug("setattr - _inode = {}", getPath(toInodeNumber(_inode)));
        log_.debug("setattr - _stat  = {}", _stat);

        if (_stat.isDefined(Stat.StatAttribute.MODE))
        {
            log_.warn("setattr - Adjusting the mode is not supported");
        }

        if (_stat.isDefined(Stat.StatAttribute.SIZE))
        {
            log_.warn("setattr - Adjusting the size is not supported");
        }
    }

    @Override
    public Inode symlink(Inode _parent, String _linkName, String _targetName, Subject _subject, int _mode)
        throws IOException
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public WriteResult write(Inode _inode, byte[] _data, long _offset, int _count, StabilityLevel _stabilityLevel)
        throws IOException
    {
        log_.debug("vfs::write");

        try
        {
            Path path = getPath(toInodeNumber(_inode));

            log_.debug("write - _inode path  = {}", path);
            log_.debug("write - _data.length = {}", _data.length);
            log_.debug("write - _offset      = {}", _offset);
            log_.debug("write - _count       = {}", _count);

            IRODSAccount acct = getCurrentIRODSUser().getAccount();

            // NFS will attempt to write large files in parallel by calling
            // the write operation from multiple threads. This will result in
            // an error if old stat information is used across multiple writes.
            // We remove any cached stat object for the path to avoid this.
            statObjectCache_.remove(acct.getUserName() + "_" + path);

            IRODSFileFactory ff = factory_.getIRODSFileFactory(acct);

            final boolean coordinated = true;
            IRODSRandomAccessFile file = ff.instanceIRODSRandomAccessFile(path.toString(), OpenFlags.READ_WRITE, coordinated);

            try (AutoClosedIRODSRandomAccessFile ac = new AutoClosedIRODSRandomAccessFile(file))
            {
                file.seek(_offset, FileIOOperations.SeekWhenceType.SEEK_START);
                file.write(_data, 0, _count);
                return new WriteResult(StabilityLevel.FILE_SYNC, _count);
            }
        }
        catch (IOException | JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
        finally
        {
            closeCurrentConnection();
        }
    }
    
    private ObjectType getObjectType(String _path) throws JargonException
    {
        ObjectType type = objectTypeCache_.get(_path);

        if (null != type)
        {
            log_.debug("getObjectType - Returning cached object type for [{}] ...", _path);
            return type;
        }

        CollectionAndDataObjectListAndSearchAO lao = factory_.getCollectionAndDataObjectListAndSearchAO(adminAcct_);
        ObjStat objStat = lao.retrieveObjectStatForPath(_path);
        type = objStat.getObjectType();
        objectTypeCache_.put(_path, type);

        return type;
    }

    private Stat statPath(Path _path, long _inodeNumber) throws IOException
    {
        log_.debug("statPath - _inodeNumber          = {}", _inodeNumber);
        log_.debug("statPath - _path                 = {}", _path);

        IRODSAccount acct = getCurrentIRODSUser().getAccount();
        String path = _path.toString();

        // Cached stat information must be scoped to the user due to the permissions
        // possibly being changed depending on who is accessing the NFS server.
        final String cachedStatKey = acct.getUserName() + "_" + path;
        Stat stat = statObjectCache_.get(cachedStatKey);

        if (null != stat)
        {
            log_.debug("statPath - Returning cached stat information for [{}] ...", path);
            return stat;
        }

        try
        {
            CollectionAndDataObjectListAndSearchAO lao = factory_.getCollectionAndDataObjectListAndSearchAO(adminAcct_);
            ObjStat objStat = lao.retrieveObjectStatForPath(path);

            log_.debug("statPath - iRODS stat info   = {}", objStat);

            stat = new Stat();

            setTime(stat, objStat);

            log_.debug("statPath - Secret owner name = {}", objStat.getOwnerName());

            String userName = IRODSIdMapper.getNobodyUserName();
            String groupName = IRODSIdMapper.getNobodyGroupName();
            
            if (getHighestUserPermissionForPath(acct.getUserName(), path).isPresent())
            {
                userName = acct.getUserName();
            }

            int userId = idMapper_.getUidByUserName(userName);
            int groupId = IRODSIdMapper.getNobodyGid();

            setStatMode(path, stat, objStat, acct.getUserName(), groupName);

            stat.setUid(userId);
            stat.setGid(groupId);
            stat.setNlink(1);
            stat.setDev(17);
            stat.setIno((int) _inodeNumber);
            stat.setRdev(0);
            stat.setSize(objStat.getObjSize());
            stat.setFileid((int) _inodeNumber);
            stat.setGeneration(objStat.getModifiedAt().getTime());

            log_.debug("statPath - User ID           = {}", userId);
            log_.debug("statPath - Group ID          = {}", groupId);
            log_.debug("statPath - Permissions       = {}", Stat.modeToString(stat.getMode()));
            log_.debug("statPath - Stat              = {}", stat);

            statObjectCache_.put(cachedStatKey, stat);

            return stat;
        }
        catch (NumberFormatException | JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
    }

    private Stat statPath(Path _path,
                          long _inodeNumber,
                          CollectionAndDataObjectListingEntry _entry,
                          List<UserGroup> _groupsContainingUser)
        throws IOException
    {
        log_.debug("statPath - _inodeNumber          = {}", _inodeNumber);
        log_.debug("statPath - _path                 = {}", _path);
        log_.debug("statPath - iRODS permissions     = {}", _entry.getUserFilePermission());

        IRODSAccount acct = getCurrentIRODSUser().getAccount();
        String path = _path.toString();

        // Cached stat information must be scoped to the user due to the permissions
        // possibly being changed depending on who is accessing the NFS server.
        final String cachedStatKey = acct.getUserName() + "_" + path;
        Stat stat = statObjectCache_.get(cachedStatKey);

        if (null != stat)
        {
            log_.debug("statPath - Returning cached stat information for [{}] ...", path);
            return stat;
        }
        
        // Cache the permission information.
        permsCache_.put(path, _entry.getUserFilePermission());

        try
        {
            stat = new Stat();

            setTime(stat, _entry);

            log_.debug("statPath - Secret owner name = {}", _entry.getOwnerName());

            String userName = IRODSIdMapper.getNobodyUserName();
            Optional<UserFilePermission> highestPerm = getHighestUserPermissionForPath(acct.getUserName(),
                                                                                       _entry.getUserFilePermission(),
                                                                                       _groupsContainingUser);
            if (highestPerm.isPresent())
            {
                userName = acct.getUserName();
            }

            int userId = idMapper_.getUidByUserName(userName);
            int groupId = IRODSIdMapper.getNobodyGid();
            
            setStatMode(stat, _entry, _groupsContainingUser, acct.getUserName());

            stat.setUid(userId);
            stat.setGid(groupId);
            stat.setNlink(1);
            stat.setDev(17);
            stat.setIno((int) _inodeNumber);
            stat.setRdev(0);
            stat.setSize(_entry.getDataSize());
            stat.setFileid((int) _inodeNumber);
            stat.setGeneration(_entry.getModifiedAt().getTime());

            log_.debug("statPath - User ID           = {}", userId);
            log_.debug("statPath - Group ID          = {}", groupId);
            log_.debug("statPath - Permissions       = {}", Stat.modeToString(stat.getMode()));
            log_.debug("statPath - Stat              = {}", stat);

            statObjectCache_.put(cachedStatKey, stat);

            return stat;
        }
        catch (NumberFormatException | JargonException e)
        {
            log_.error(e.getMessage());
            throw new IOException(e);
        }
    }

    private void setTime(Stat _stat, ObjStat _objStat)
    {
        if (_objStat.getObjectType() == ObjectType.COLLECTION_HEURISTIC_STANDIN)
        {
            _stat.setATime(FIXED_TIMESTAMP);
            _stat.setCTime(FIXED_TIMESTAMP);
            _stat.setMTime(FIXED_TIMESTAMP);
        }
        else
        {
            _stat.setATime(_objStat.getModifiedAt().getTime());
            _stat.setCTime(_objStat.getCreatedAt().getTime());
            _stat.setMTime(_objStat.getModifiedAt().getTime());
        }
    }
    
    private void setTime(Stat _stat, CollectionAndDataObjectListingEntry _entry)
    {
        if (_entry.getObjectType() == ObjectType.COLLECTION_HEURISTIC_STANDIN)
        {
            _stat.setATime(FIXED_TIMESTAMP);
            _stat.setCTime(FIXED_TIMESTAMP);
            _stat.setMTime(FIXED_TIMESTAMP);
        }
        else
        {
            _stat.setATime(_entry.getModifiedAt().getTime());
            _stat.setCTime(_entry.getCreatedAt().getTime());
            _stat.setMTime(_entry.getModifiedAt().getTime());
        }
    }

    private static Inode toFh(long _inodeNumber)
    {
        return Inode.forFile(Longs.toByteArray(_inodeNumber));
    }

    private Path getPath(long _inodeNumber) throws IOException
    {
        Path path = inodeToPathMapper_.getPathByInodeNumber(_inodeNumber);

        if (path == null)
        {
            throw new NoEntException("Path does not exist for [" + _inodeNumber + "]");
        }

        return path;
    }

    private long getInodeNumber(Path _path) throws IOException
    {
        Long inodeNumber = inodeToPathMapper_.getInodeNumberByPath(_path);

        if (inodeNumber == null)
        {
            throw new NoEntException("Inode number does not exist for [" + _path + "]");
        }

        return inodeNumber;
    }

    private static long toInodeNumber(Inode _inode)
    {
        return Longs.fromByteArray(_inode.getFileId());
    }

    private boolean isSpecialCollection(String _path)
    {
        List<Path> paths = new ArrayList<>();

        paths.add(ROOT_COLLECTION);
        paths.add(ZONE_COLLECTION);
        paths.add(HOME_COLLECTION);
        paths.add(PUBLIC_COLLECTION);
        paths.add(TRASH_COLLECTION);

        return paths.stream().anyMatch(p -> p.toString().equals(_path));
    }

    @SuppressWarnings("unchecked")
    private List<UserFilePermission> getPermissions(String _path) throws JargonException
    {
        List<UserFilePermission> perms = (List<UserFilePermission>) permsCache_.get(_path);

        if (perms != null)
        {
            log_.debug("getPermissions - Returning cached permissions for [{}] [perms={}] ...", _path, perms);
            return perms;
        }

        switch (getObjectType(_path))
        {
            case COLLECTION:
                CollectionAO coa = factory_.getCollectionAO(adminAcct_);
                perms = coa.listPermissionsForCollection(_path);
                break;

            case DATA_OBJECT:
                DataObjectAO doa = factory_.getDataObjectAO(adminAcct_);
                perms = doa.listPermissionsForDataObject(_path);
                break;

            default:
                perms = new ArrayList<>();
                break;
        }
        
        permsCache_.put(_path, perms);

        log_.debug("getPermissions - Returning permissions for [{}] [perms={}] ...", _path, perms);

        return perms;
    }

    private void setStatMode(String _path, Stat _stat, ObjStat _objStat, String _userName, String _groupName)
        throws JargonException
    {
        log_.debug("setStatMode - _path = {}", _path);

        switch (_objStat.getObjectType())
        {
            case COLLECTION:
            {
                if (isSpecialCollection(_path))
                {
                    _stat.setMode(Stat.S_IFDIR | 0700);
                    return;
                }

                CollectionAO coa = factory_.getCollectionAO(adminAcct_);
                List<UserFilePermission> perms = coa.listPermissionsForCollection(_path);
                _stat.setMode(Stat.S_IFDIR | calcMode(_userName, _groupName, _objStat.getObjectType(), perms));
                break;
            }

            case DATA_OBJECT:
            {
                DataObjectAO doa = factory_.getDataObjectAO(adminAcct_);
                List<UserFilePermission> perms = doa.listPermissionsForDataObject(_path);
                // @formatter:off
                _stat.setMode(Stat.S_IFREG | (~0110 & calcMode(_userName, _groupName, _objStat.getObjectType(), perms)));
                // @formatter:on
                break;
            }

            // This object type comes from the Jargon library.
            // It is encountered when the user accessing iRODS is not a rodsadmin.
            case COLLECTION_HEURISTIC_STANDIN:
                _stat.setMode(Stat.S_IFDIR);
                break;

            default:
                break;
        }
    }
    
    private void setStatMode(Stat _stat,
                             CollectionAndDataObjectListingEntry _entry,
                             List<UserGroup> _groupsContainingUser,
                             String _userName)
        throws JargonException
    {
        switch (_entry.getObjectType())
        {
            case COLLECTION:
            {
                if (isSpecialCollection(_entry.getPathOrName()))
                {
                    _stat.setMode(Stat.S_IFDIR | 0700);
                }

                int mode = calcMode(_userName, _entry.getObjectType(), _entry.getUserFilePermission(), _groupsContainingUser);
                _stat.setMode(Stat.S_IFDIR | mode);
                break;
            }

            case DATA_OBJECT:
            {
                int mode = calcMode(_userName, _entry.getObjectType(), _entry.getUserFilePermission(), _groupsContainingUser);
                _stat.setMode(Stat.S_IFREG | (~0110 & mode));
                break;
            }

            // This object type comes from the Jargon library.
            // It is encountered when the user accessing iRODS is not a rodsadmin.
            case COLLECTION_HEURISTIC_STANDIN:
                _stat.setMode(Stat.S_IFDIR);
                break;

            default:
                break;
        }
    }

    private Optional<UserFilePermission> getHighestUserPermissionForPath(String _userName, String _path)
        throws JargonException
    {
        return getHighestUserPermissionForPath(_userName, getPermissions(_path));
    }
    
    private Optional<UserFilePermission> getHighestUserPermissionForPath(String _userName, List<UserFilePermission> _perms)
        throws JargonException
    {
        UserGroupAO ugao = factory_.getUserGroupAO(adminAcct_);
        return getHighestUserPermissionForPath(_userName, _perms, ugao.findUserGroupsForUser(_userName));
    }
    
    private Optional<UserFilePermission> getHighestUserPermissionForPath(String _userName,
                                                                         List<UserFilePermission> _perms,
                                                                         List<UserGroup> _groupsContainingUser)
        throws JargonException
    {
        // @formatter:off
        // Get the highest level of permissions for the user among the groups.
        Optional<UserFilePermission> highestGroupPerm = _perms.stream()
            // Filter the incoming list "_perms" to groups the user is a member of.
            .filter(p -> _groupsContainingUser.stream().anyMatch(ug -> p.getUserName().equals(ug.getUserGroupName())))
            // Return the object holding the highest level of permissions.
            .max((lhs, rhs) -> Integer.compare(lhs.getFilePermissionEnum().ordinal(),
                                               rhs.getFilePermissionEnum().ordinal()));

        // Get the permissions for the user if they have explicit permission
        // to the object.
        List<UserFilePermission> perms = _perms.stream()
            .filter(p -> p.getUserName().equals(_userName))
            .collect(Collectors.toList());
        
        highestGroupPerm.ifPresent(p -> perms.add(p));
        
        return perms.stream()
            .max((lhs, rhs) -> Integer.compare(lhs.getFilePermissionEnum().ordinal(),
                                               rhs.getFilePermissionEnum().ordinal()));
        // @formatter:on
    }

    private int calcMode(String _userName,
                         String _groupName,
                         ObjectType _objType,
                         List<UserFilePermission> _perms)
        throws JargonException
    {
        int mode = 0100;

        if (ObjectType.DATA_OBJECT == _objType)
        {
            mode = 0;
        }
        
        Optional<UserFilePermission> perm = getHighestUserPermissionForPath(_userName, _perms);
        
        if (perm.isPresent())
        {
            UserFilePermission p = perm.get();

            log_.debug("calcMode - permission = {}", p);
            
            final int r = 0400; // Read bit
            final int w = 0200; // Write bit

            switch (p.getFilePermissionEnum())
            {
                // @formatter:off
                case OWN:   mode |= (r | w); break;
                case WRITE: mode |= (r | w); break;
                case READ:  mode |= r; break;
                default:
                // @formatter:on
            }
        }

        return mode;
    }

    private int calcMode(String _userName,
                         ObjectType _objType,
                         List<UserFilePermission> _perms,
                         List<UserGroup> _groupsContainingUser)
        throws JargonException
    {
        int mode = 0100;

        if (ObjectType.DATA_OBJECT == _objType)
        {
            mode = 0;
        }
        
        Optional<UserFilePermission> perm = getHighestUserPermissionForPath(_userName, _perms, _groupsContainingUser);
        
        if (perm.isPresent())
        {
            UserFilePermission p = perm.get();

            log_.debug("calcMode - permission = {}", p);
            
            final int r = 0400; // Read bit
            final int w = 0200; // Write bit

            switch (p.getFilePermissionEnum())
            {
                // @formatter:off
                case OWN:   mode |= (r | w); break;
                case WRITE: mode |= (r | w); break;
                case READ:  mode |= r; break;
                default:
                // @formatter:on
            }
        }

        return mode;
    }

    private static int getUserID()
    {
        Subject subject = Subject.getSubject(AccessController.getContext());
        String name = subject.getPrincipals().iterator().next().getName();
        return Integer.parseInt(name);
    }

    private IRODSUser getCurrentIRODSUser() throws IOException
    {
        return idMapper_.resolveUser(getUserID());
    }

    private boolean isAdministrator(String _userName) throws JargonException
    {
        UserTypeEnum type = userTypeCache_.get(_userName);
        
        if (null != type)
        {
            return UserTypeEnum.RODS_ADMIN == type;
        }

        UserAO uao = factory_.getUserAO(adminAcct_);
        User user = uao.findByName(_userName);
        
        if (null == user)
        {
            return false;
        }
        
        userTypeCache_.put(_userName, user.getUserType());

        return user.getUserType() == UserTypeEnum.RODS_ADMIN;
    }

    private void closeCurrentConnection() throws IOException
    {
        factory_.closeSessionAndEatExceptions();
    }

    private static class AutoClosedIRODSFile implements AutoCloseable
    {
        private final IRODSFile file_;

        AutoClosedIRODSFile(IRODSFile _file)
        {
            file_ = _file;
        }

        @Override
        public void close()
        {
            try
            {
                file_.close();
            }
            catch (JargonException e)
            {
                log_.error(e.getMessage());
            }
        }
    }

    private static class AutoClosedIRODSRandomAccessFile implements AutoCloseable
    {
        private final IRODSRandomAccessFile file_;

        AutoClosedIRODSRandomAccessFile(IRODSRandomAccessFile _file)
        {
            file_ = _file;
        }

        @Override
        public void close()
        {
            try
            {
                file_.close();
            }
            catch (IOException e)
            {
                log_.error(e.getMessage());
            }
        }
    }
}
