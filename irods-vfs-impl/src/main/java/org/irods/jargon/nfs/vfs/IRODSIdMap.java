package org.irods.jargon.nfs.vfs;

import java.util.Map;

import javax.security.auth.Subject;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.dcache.auth.Subjects;
import org.dcache.nfs.v4.NfsIdMapping;
import org.dcache.oncrpc4j.rpc.RpcLoginService;
import org.dcache.oncrpc4j.rpc.RpcTransport;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IRODSIdMap implements NfsIdMapping, RpcLoginService
{
    private static final Logger log_ = LoggerFactory.getLogger(IRODSIdMap.class);

    private static final int NOBODY_UID = 65534;
    private static final int NOBODY_GID = 65534;

    private static final int DEFAULT_UID = 1001;
    private static final int DEFAULT_GID = 1001;

    private static Map<String, Integer> principleUidMap_ = new NonBlockingHashMap<>();
    private static Map<Integer, IRODSUser> irodsPrincipleMap_ = new NonBlockingHashMap<>();

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
            // enable getting cred delegation
            log_.debug("GSSC Name: " + _gssCtx.getSrcName().toString());
            String principal = _gssCtx.getSrcName().toString();

            // if principal doesnt exist in mapping
            if (principleUidMap_.get(principal) == null)
            {
                // get user ID of principal
                String userName = "";

                log_.debug("Substring of principal[0,4]: " + principal.substring(0, 4));

                // if it is service
                if (principal.substring(0, 4).equals("nfs/"))
                {
                    // userID = _irods.getUserAO(_irodsAcct).findByName(_irodsAdmin).getId();
                    // TODO: Save as admin
                    userName = "rods";
                    log_.debug("[IrodsIdMap] in service if statement");
                }
                else
                {
                    // parse principal
                    String[] parts = principal.split("@");
                    userName = parts[0];
                    log_.debug("[IrodsIdMap] in user if statement");
                }

                // create User Object
                IRODSUser user = new IRODSUser(userName);

                // Save <Principle, Uid>
                principleUidMap_.put(principal, user.getUserID());
                log_.debug("principleUidMap Principal: " + principal + "    ID: " + principleUidMap_.get(principal));

                // save user Object to user ID
                irodsPrincipleMap_.put(user.getUserID(), user);
                log_.debug("irodsPrincipleMap ID: " + user.getUserID() + "    User: "
                    + irodsPrincipleMap_.get(user.getUserID()).toString());

                // createIrodsAccountInstance(new Integer(userName));
            }

            // return id mapping
            return Subjects.of(principleUidMap_.get(principal), principleUidMap_.get(principal));
        }
        catch (GSSException e)
        {
            log_.error(e.getMessage());
        }

        // if everything fails return defaults
        return Subjects.of(DEFAULT_UID, DEFAULT_GID);
    }

    public IRODSUser resolveUser(int _userID)
    {
        return irodsPrincipleMap_.get(Integer.valueOf(_userID));
    }

}