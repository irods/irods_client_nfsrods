/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.irods.jargon.nfs.vfs;
import java.security.Principal;
import javax.security.auth.Subject;
import org.dcache.auth.Subjects;
import org.dcache.nfs.v4.NfsIdMapping;
import org.dcache.nfs.v4.NfsLoginService;
import org.dcache.oncrpc4j.rpc.RpcLoginService;
import org.dcache.oncrpc4j.rpc.RpcTransport;
import org.ietf.jgss.GSSContext;
/**
 *
 * @author alek
 */
public class IrodsIdMap implements NfsIdMapping, RpcLoginService{
    
    private static final int NOBODY_UID = 65534;
    private static final int NOBODY_GID = 65534;

    private static final int DEFAULT_UID = 1001;
    private static final int DEFAULT_GID = 1001;

    @Override
    public int principalToGid(String principal) {
        try {
            return Integer.parseInt(principal);
        } catch (NumberFormatException e) {
        }
        return NOBODY_GID;
    }

    @Override
    public int principalToUid(String principal) {
        try {
            return Integer.parseInt(principal);
        } catch (NumberFormatException e) {
        }
        return NOBODY_UID;
    }

    @Override
    public String uidToPrincipal(int id) {
        return Integer.toString(id);
    }

    @Override
    public String gidToPrincipal(int id) {
        return Integer.toString(id);
    }
    /*
    @Override
    public Subject login(Principal principal) {
        return Subjects.of(DEFAULT_UID, DEFAULT_GID);
    }
*/

    @Override
    public Subject login(RpcTransport rt, GSSContext gssc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
