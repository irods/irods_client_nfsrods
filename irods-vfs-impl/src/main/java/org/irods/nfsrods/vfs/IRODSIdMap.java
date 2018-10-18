package org.irods.nfsrods.vfs;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.dcache.auth.Subjects;
import org.dcache.nfs.v4.NfsIdMapping;
import org.dcache.oncrpc4j.rpc.RpcLoginService;
import org.dcache.oncrpc4j.rpc.RpcTransport;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.nfsrods.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IRODSIdMap implements NfsIdMapping, RpcLoginService
{
    private static final Logger log_ = LoggerFactory.getLogger(IRODSIdMap.class);

    private static final int NOBODY_UID = 65534;
    private static final int NOBODY_GID = 65534;

    private final ServerConfig config_;
    private final IRODSAccessObjectFactory factory_;
    private Map<String, Integer> principleUidMap_;
    private Map<Integer, IRODSUser> irodsPrincipleMap_;

    public IRODSIdMap(ServerConfig _config, IRODSAccessObjectFactory _factory)
    {
        config_ = _config;
        factory_ = _factory;
        principleUidMap_ = new NonBlockingHashMap<>();
        irodsPrincipleMap_ = new NonBlockingHashMap<>();
    }

    @Override
    public int principalToGid(String _principal)
    {
        try
        {
            log_.debug("principalToGid :: _principal = {}", Integer.parseInt(_principal));
            return Integer.parseInt(_principal);
        }
        catch (NumberFormatException e)
        {
            log_.error(e.getMessage());
        }

        return NOBODY_GID;
    }

    @Override
    public int principalToUid(String _principal)
    {
        try
        {
            log_.debug("principalToUid :: _principal = {}", Integer.parseInt(_principal));
            return Integer.parseInt(_principal);
        }
        catch (NumberFormatException e)
        {
            log_.error(e.getMessage());
        }

        return NOBODY_UID;
    }

    @Override
    public String uidToPrincipal(int _id)
    {
        log_.debug("uidToPrincipal :: _id = {}", Integer.toString(_id));
        return Integer.toString(_id);
    }

    @Override
    public String gidToPrincipal(int _id)
    {
        log_.debug("gidToPrincipal :: _id = {}", Integer.toString(_id));
        return Integer.toString(_id);
    }

    @Override
    public Subject login(RpcTransport _rpcTransport, GSSContext _gssCtx)
    {
        try
        {
            String principal = _gssCtx.getSrcName().toString();
            Integer rodsUserID = principleUidMap_.get(principal);

            // printPrincipalType(principal);

            if (rodsUserID == null)
            {
                String userName = null;

                // If the principal represents a service.
                if (principal != null && principal.startsWith("nfs/"))
                {
                    userName = config_.getIRODSProxyAdminAcctConfig().getUsername();
                }
                else
                {
                    // KerberosPrincipal kp = new KerberosPrincipal(principal);
                    // userName = kp.getName();
                    userName = principal.substring(0, principal.indexOf('@'));
                    log_.debug("IRODSIdMap :: userName = {}", userName);
                }

                IRODSUser user = new IRODSUser(userName, config_, factory_);
                rodsUserID = user.getUserID();
                principleUidMap_.put(principal, rodsUserID);
                irodsPrincipleMap_.put(rodsUserID, user);
            }

            return Subjects.of(rodsUserID, rodsUserID);
        }
        catch (GSSException e)
        {
            log_.error(e.getMessage());
        }

        return Subjects.of(NOBODY_UID, NOBODY_GID);
    }

    public IRODSUser resolveUser(int _userID)
    {
        return irodsPrincipleMap_.get(Integer.valueOf(_userID));
    }

    @SuppressWarnings("unused")
    private void printPrincipalType(String _principal)
    {
        try
        {
            KerberosPrincipal kp = new KerberosPrincipal(_principal);

            switch (kp.getNameType())
            {
                case KerberosPrincipal.KRB_NT_PRINCIPAL:
                    log_.debug("Principal Type for [{}] = KRB_NT_PRINCIPAL", _principal);
                    break;

                case KerberosPrincipal.KRB_NT_SRV_HST:
                    log_.debug("Principal Type for [{}] = KRB_NT_SRV_HST", _principal);
                    break;

                case KerberosPrincipal.KRB_NT_SRV_INST:
                    log_.debug("Principal Type for [{}] = KRB_NT_SRV_INST", _principal);
                    break;

                case KerberosPrincipal.KRB_NT_SRV_XHST:
                    log_.debug("Principal Type for [{}] = KRB_NT_SRV_XHST", _principal);
                    break;

                case KerberosPrincipal.KRB_NT_UID:
                    log_.debug("Principal Type for [{}] = KRB_NT_UID", _principal);
                    break;

                case KerberosPrincipal.KRB_NT_UNKNOWN:
                    log_.debug("Principal Type for [{}] = KRB_NT_UNKNOWN", _principal);
                    break;
            }
        }
        catch (IllegalArgumentException e)
        {
            log_.error(e.getMessage());
        }
    }
}
