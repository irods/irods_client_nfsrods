/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.irods.jargon.nfs.vfs;
import java.security.Principal;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import org.dcache.auth.Origin;
import org.dcache.auth.Subjects;
import org.dcache.nfs.v4.NfsIdMapping;
import org.dcache.nfs.v4.NfsLoginService;
import org.dcache.oncrpc4j.rpc.RpcLoginService;
import org.dcache.oncrpc4j.rpc.RpcTransport;
import org.ietf.jgss.GSSContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ietf.jgss.GSSException;
/**
 *
 * @author alek
 */
public class IrodsIdMap implements NfsIdMapping, RpcLoginService{
       
    private static final Logger log = LoggerFactory.getLogger(IrodsIdMap.class);
    
    private static final int NOBODY_UID = 65534;
    private static final int NOBODY_GID = 65534;

    private static final int DEFAULT_UID = 1001;
    private static final int DEFAULT_GID = 1001;

    @Override
    public int principalToGid(String principal) {
        try {
            log.debug("PrincipalToGid: "+ Integer.parseInt(principal));
            return Integer.parseInt(principal);
        } catch (NumberFormatException e) {
        }
        
        return NOBODY_GID;
    }

    @Override
    public int principalToUid(String principal) {
        try {
            log.debug("PrincipalToUid: "+ Integer.parseInt(principal));
            return Integer.parseInt(principal);
        } catch (NumberFormatException e) {
		log.debug("[IrodsIdMapper] PtoUid Exception: " + e);
        }
        log.debug("PrincipalToGid");
        return NOBODY_UID;
    }

    @Override
    public String uidToPrincipal(int id) {
        log.debug("uidToPRincipal: "+ Integer.toString(id));
        return Integer.toString(id);
    }

    @Override
    public String gidToPrincipal(int id) {
        log.debug("gidToPrincipal: "+ Integer.toString(id));
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
        try {	        
		//enable getting cred delegation
		log.debug("GSSC Name: "+ gssc.getSrcName().toString());
		log.debug("GSSC Target Name: " + gssc.getTargName().toString());
		
		log.debug("Route: " +rt.getRemoteSocketAddress().getAddress());
		log.debug("Rt hostname: " +rt.getRemoteSocketAddress().getHostString());
                log.debug("Rt hoststring: " +rt.getRemoteSocketAddress().getHostName());

        } catch (GSSException ex) {
		log.debug("Login Error: " +ex);           
        }
         return Subjects.of(DEFAULT_UID, DEFAULT_GID);
    }
    
    @Override
    public String toString(){
        
        
        return "IrodsIdMap{"+this.NOBODY_UID+"}";
    }
    
}
