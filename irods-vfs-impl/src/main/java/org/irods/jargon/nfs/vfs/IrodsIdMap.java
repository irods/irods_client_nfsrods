/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.irods.jargon.nfs.vfs;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
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
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
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
    private static Map<String, Integer> irodsIdMap = new HashMap<String, Integer>();
    private IRODSAccessObjectFactory _irods;
    private IRODSAccount _irodsAcct;
    private String _irodsAdmin;
    
    public IrodsIdMap(IRODSAccessObjectFactory irodsFactory, IRODSAccount acct, String irodsAdmin){
        _irodsAdmin = irodsAdmin;
        _irods = irodsFactory;
        _irodsAcct = acct;
    }
    
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
                String principal =  gssc.getSrcName().toString();
                
                //if principal doesnt exist in mapping
                if(irodsIdMap.get(principal) == null){
                    
                    //get user ID of principal
                    String userID = "";
                    
                    log.debug("Substring of principal[0,4]: "+ principal.substring(0,4));
                    //if it is service
                    if(principal.substring(0,4).equals("nfs/")){
                        userID = _irods.getUserAO(_irodsAcct).findByName(_irodsAdmin).getId();
                    }
                    else{
                        //parse principal
                        String[] parts = principal.split("@");
                        userID = _irods.getUserAO(_irodsAcct).findByName(parts[0]).getId();
                    }
                    
                    //add keypairing to irodsIdMap
                    irodsIdMap.put(principal, new Integer(userID));                  
                }
                
                log.debug("IrodsIdMap Principal: " +principal +"    ID: "+ irodsIdMap.get(principal));
                //return id mapping
                return Subjects.of(irodsIdMap.get(principal), irodsIdMap.get(principal));

        } catch (GSSException ex) {
		log.debug("Login Error: " +ex);           
        } catch (JargonException ex) {
            log.debug("Jargon Exception: " +ex);  
        }
         return Subjects.of(DEFAULT_UID, DEFAULT_GID);
    }
    
    @Override
    public String toString(){
        
        
        return "IrodsIdMap{"+this.NOBODY_UID+"}";
    }
    
}
