package org.irods.nfsrods.vfs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSGenQueryExecutor;
import org.irods.jargon.core.query.GenQueryBuilderException;
import org.irods.jargon.core.query.IRODSGenQueryBuilder;
import org.irods.jargon.core.query.IRODSGenQueryFromBuilder;
import org.irods.jargon.core.query.IRODSQueryResultSet;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.QueryConditionOperators;
import org.irods.jargon.core.query.RodsGenQueryEnum;

public class ReadWriteAclAllowlist
{
    private static final Logger log_ = LogManager.getLogger(ReadWriteAclAllowlist.class);
    
    private static final String GRANT_NFS4_SETFACL_PRIVILEGE = "irods::nfsrods::grant_nfs4_setfacl";

    private IRODSAccessObjectFactory factory_;
    private IRODSAccount adminAcct_;
    private List<NamePathPrefixPair> namePathPairs_;
    private ReadWriteLock lock_;
    private ScheduledExecutorService scheduler_;
    
    public ReadWriteAclAllowlist(IRODSAccessObjectFactory _factory,
                                 IRODSAccount _adminAcct)
    {
        factory_ = _factory;
        adminAcct_ = _adminAcct;
        namePathPairs_ = new ArrayList<>();
        lock_ = new ReentrantReadWriteLock();
        scheduler_ = Executors.newSingleThreadScheduledExecutor();

        // Periodically update the nfs4_setfacl allowlist.
        scheduler_.scheduleAtFixedRate(() -> {
            Lock lk = lock_.writeLock();
            
            try
            {
                lk.lock();
                
                namePathPairs_.clear();
                
                IRODSGenQueryBuilder builder = new IRODSGenQueryBuilder(true, null);
                
                builder.addSelectAsGenQueryValue(RodsGenQueryEnum.COL_USER_NAME)
                       .addSelectAsGenQueryValue(RodsGenQueryEnum.COL_META_USER_ATTR_VALUE)
                       .addConditionAsGenQueryField(RodsGenQueryEnum.COL_META_USER_ATTR_NAME, QueryConditionOperators.EQUAL, GRANT_NFS4_SETFACL_PRIVILEGE);
                
                IRODSGenQueryExecutor gqe = factory_.getIRODSGenQueryExecutor(adminAcct_);
                IRODSGenQueryFromBuilder query = builder.exportIRODSQueryFromBuilder(50);
                IRODSQueryResultSet resultSet = gqe.executeIRODSQueryAndCloseResultInZone(query, 0, adminAcct_.getZone());
                
                resultSet.getResults().forEach(row -> {
                    try
                    {
                        namePathPairs_.add(new NamePathPrefixPair(row.getColumn(0), row.getColumn(1)));
                    }
                    catch (JargonException e)
                    {
                        log_.error(e.getMessage());
                    }
                });
            }
            catch (GenQueryBuilderException | JargonException | JargonQueryException e)
            {
                log_.error(e.getMessage());
            }
            finally
            {
                lk.unlock();
                factory_.closeSessionAndEatExceptions();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }
    
    public boolean contains(String _name)
    {
        Lock lk = lock_.readLock();
        
        try
        {
            lk.lock();
            return namePathPairs_.parallelStream().anyMatch(e -> _name.equals(e.getName()));
        }
        finally
        {
            lk.unlock();
        }
    }
    
    public String getPathPrefix(String _name)
    {
        Lock lk = lock_.readLock();
        
        try
        {
            lk.lock();

            Optional<NamePathPrefixPair> match = namePathPairs_.parallelStream()
                .filter(e -> _name.equals(e.getName()))
                .findFirst();
            
            if (match.isPresent())
            {
                return match.get().getPathPrefix();
            }
            
            return null;
        }
        finally
        {
            lk.unlock();
        }
    }
    
    public static final class NamePathPrefixPair
    {
        private String name_;
        private String path_;
        
        private NamePathPrefixPair(String _name, String _path)
        {
            name_ = _name;
            path_ = _path;
        }
        
        public String getName() { return name_; }
        public String getPathPrefix() { return path_; }
    }
}
