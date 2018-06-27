package org.irods.jargon.nfs.vfs;

import java.util.Properties;
import javax.security.auth.Subject;
import org.dcache.nfs.vfs.FsStat;

import org.dcache.nfs.vfs.Inode;
import org.dcache.nfs.vfs.Stat;
import org.dcache.utils.UnixUtils;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.utils.MiscIRODSUtils;
import org.irods.jargon.nfs.vfs.utils.PermissionBitmaskUtils;
import org.irods.jargon.testutils.AssertionHelper;
import org.irods.jargon.testutils.IRODSTestSetupUtilities;
import org.irods.jargon.testutils.TestingPropertiesHelper;
import org.irods.jargon.testutils.filemanip.ScratchFileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class IrodsVirtualFileSystemTest {

	private static Properties testingProperties = new Properties();
	private static TestingPropertiesHelper testingPropertiesHelper = new TestingPropertiesHelper();
	private static ScratchFileUtils scratchFileUtils = null;
	public static final String IRODS_TEST_SUBDIR_PATH = "IrodsVirtualFileSystemTest";
	private static IRODSTestSetupUtilities irodsTestSetupUtilities = null;
	private static AssertionHelper assertionHelper = null;
	private static IRODSFileSystem irodsFileSystem;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestingPropertiesHelper testingPropertiesLoader = new TestingPropertiesHelper();
		testingProperties = testingPropertiesLoader.getTestProperties();
		scratchFileUtils = new ScratchFileUtils(testingProperties);
		scratchFileUtils.clearAndReinitializeScratchDirectory(IRODS_TEST_SUBDIR_PATH);
		irodsTestSetupUtilities = new IRODSTestSetupUtilities();
		irodsTestSetupUtilities.initializeIrodsScratchDirectory();
		irodsTestSetupUtilities.initializeDirectoryForTest(IRODS_TEST_SUBDIR_PATH);
		assertionHelper = new AssertionHelper();
		irodsFileSystem = IRODSFileSystem.instance();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		irodsFileSystem.closeAndEatExceptions();
	}

	@After
	public void afterEach() throws Exception {
		irodsFileSystem.closeAndEatExceptions();
	}

	@Test
	public void testCreateNewDFile() throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
		String homeDir = MiscIRODSUtils.buildIRODSUserHomeForAccountUsingDefaultScheme(irodsAccount);
		IRODSFile rootFile = accessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(homeDir);
		IrodsVirtualFileSystem vfs = new IrodsVirtualFileSystem(accessObjectFactory, irodsAccount, rootFile);
		Inode rootNode = vfs.getRootInode();
		int readBitmask = 0 | PermissionBitmaskUtils.USER_READ;
		int access = vfs.access(rootNode, readBitmask);
		Assert.assertEquals("did not get expected user read acess", readBitmask, access);

	}

	@Test
	public void testGetAttrRoot() throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
		String homeDir = MiscIRODSUtils.buildIRODSUserHomeForAccountUsingDefaultScheme(irodsAccount);
		IRODSFile rootFile = accessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(homeDir);
		IrodsVirtualFileSystem vfs = new IrodsVirtualFileSystem(accessObjectFactory, irodsAccount, rootFile);
		Inode rootNode = vfs.getRootInode();
		Stat stat = vfs.getattr(rootNode);
		Assert.assertNotNull("null stat", stat);
	}

	@Test
	public void testGetReadAttrAtRoot() throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
		String homeDir = MiscIRODSUtils.buildIRODSUserHomeForAccountUsingDefaultScheme(irodsAccount);
		IRODSFile rootFile = accessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(homeDir);
		IrodsVirtualFileSystem vfs = new IrodsVirtualFileSystem(accessObjectFactory, irodsAccount, rootFile);
		Inode rootNode = vfs.getRootInode();
		int readBitmask = 0 | PermissionBitmaskUtils.USER_READ;
		int access = vfs.access(rootNode, readBitmask);
		Assert.assertEquals("did not get expected user read acess", readBitmask, access);

	}

	@Test
	public void testGetReadWriteAttrAtRoot() throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
		String homeDir = MiscIRODSUtils.buildIRODSUserHomeForAccountUsingDefaultScheme(irodsAccount);
		IRODSFile rootFile = accessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(homeDir);
		IrodsVirtualFileSystem vfs = new IrodsVirtualFileSystem(accessObjectFactory, irodsAccount, rootFile);
		Inode rootNode = vfs.getRootInode();
		int readBitmask = 0 | PermissionBitmaskUtils.USER_READ | PermissionBitmaskUtils.USER_WRITE;
		int access = vfs.access(rootNode, readBitmask);
		Assert.assertEquals("did not get expected user read/write acess", readBitmask, access);

	}

	@Test
	public void testIrodsVirtualFileSystem() throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
		String homeDir = MiscIRODSUtils.buildIRODSUserHomeForAccountUsingDefaultScheme(irodsAccount);
		IRODSFile rootFile = accessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(homeDir);
		IrodsVirtualFileSystem vfs = new IrodsVirtualFileSystem(accessObjectFactory, irodsAccount, rootFile);
		Assert.assertNotNull("no vfs created", vfs);
		Inode actual = vfs.getRootInode();
		Assert.assertNotNull("no root inode", actual);

	}
        
        @Test
        public void testAccess() throws Exception{
            
            //setup acct and file stuff
            IRODSAccount irodsAccount = testingPropertiesHelper.buildIRODSAccountFromTestProperties(testingProperties);
            IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
            String homeDir = MiscIRODSUtils.buildIRODSUserHomeForAccountUsingDefaultScheme(irodsAccount);
            IRODSFile rootFile = accessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(homeDir);
            IrodsVirtualFileSystem vfs = new IrodsVirtualFileSystem(accessObjectFactory, irodsAccount, rootFile);
            //get inode
            Inode rootNode = vfs.getRootInode();
            //check if null
            Assert.assertNotNull("Inode is null", rootNode);
            //read test
            Assert.assertSame("User can read", vfs.access(rootNode,  PermissionBitmaskUtils.USER_READ), 0);
            
            //write test
            Assert.assertSame("User can write", vfs.access(rootNode,  PermissionBitmaskUtils.USER_WRITE), 0);
            
            //execute test
            Assert.assertSame("User can execute", vfs.access(rootNode,  PermissionBitmaskUtils.USER_EXECUTE), 0);
            
        }
        
        @Test
        public void testCommit() throws Exception{
            //noop rn
        }
        
        @Test
        public void testCreate() throws Exception{
            //get Inode
            Inode rootNode = getVFS().getRootInode();
            //get name
            String name = "test";
            //mode
            int mode = PermissionBitmaskUtils.USER_WRITE;
            //subject
            
            //rotate through types to test
            
            
            
        }
        
        @Test
        public void testGetAcl() throws Exception{
            
        }
        
        @Test
        public void testGetAclCheckable() throws Exception{
            
        }
        
        @Test
        public void testGetFsStat() throws Exception{
            //get fsstat
            FsStat test = getVFS().getFsStat();
            
            //null
            Assert.assertNotNull("FsStat is not null", test);
            
            //total space
            Assert.assertNotNull("No total space value", test.getTotalSpace());
            
            //used space
            Assert.assertNotNull("No used space value", test.getUsedSpace());
            
            //total files
            Assert.assertNotNull("No total files value", test.getTotalFiles());
                    
            //used files
            Assert.assertNotNull("No used files value", test.getUsedFiles() );
        }   
        
        @Test
        public void testGetIdMapper() throws Exception{
            Assert.assertNotNull("Cannot get ID Mapper", getVFS().getIdMapper());
        }
        
        @Test
        public void testGetRootInode() throws Exception{
            Assert.assertNotNull("Inode is null", getVFS().getRootInode());
        }
        
        @Test
        public void testToFh() throws Exception{
            
        }
        
        @Test
        public void testGetattr() throws Exception{
            
        }
        
        @Test
        public void testHasIOLayout() throws Exception{
            Assert.assertFalse("Does not have IO Layout", getVFS().hasIOLayout(getVFS().getRootInode()));
        }
        
        @Test
        public void testLink() throws Exception{
            
        }
        
        @Test
        public void testList() throws Exception{
            
        }
        
        @Test
        public void testLookup() throws Exception{
            
        }
        
        @Test
        public void testMkdir() throws Exception{
            
            //get irods acct stuff ready
            IRODSAccount irodsAccount = testingPropertiesHelper.buildIRODSAccountFromTestProperties(testingProperties);
            IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
            String homeDir = MiscIRODSUtils.buildIRODSUserHomeForAccountUsingDefaultScheme(irodsAccount);
            IRODSFile rootFile = accessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(homeDir);
            
            //create VFS
            IrodsVirtualFileSystem vfs = new IrodsVirtualFileSystem(accessObjectFactory, irodsAccount, rootFile);
            
            //create Test Dir string
            String path = IRODS_TEST_SUBDIR_PATH;
            
            //get subject for mkdir()
            Subject currentUser = UnixUtils.getCurrentUser();
            
            //call mkdir()
            vfs.mkdir(vfs.getRootInode(), path, currentUser, 0);
            
            //get irods directory as irods file
            IRODSFile file = irodsFileSystem.getIRODSFileFactory(irodsAccount).instanceIRODSFile(path);
            
            try{ //try deleting dir
                irodsFileSystem.getIRODSAccessObjectFactory().getIRODSFileSystemAO(irodsAccount).directoryDeleteForce(file);
            }
            catch (Exception e){//if it fails theres issues
                System.out.println("Error Deleting Directory: " + e);
            }
            
            
            
        }
        
        @Test
        public void testMove() throws Exception{
            
        }
        
        @Test
        public void testParentOf() throws Exception{
            
        }
        
        @Test
        public void testRead() throws Exception{
            
        }
        
        @Test
        public void testReadlink() throws Exception{
            
        }
        
        @Test
        public void testRemove() throws Exception{
            
        }
        
        @Test
        public void testSetAcl() throws Exception{
            
        }
        
        @Test
        public void testSetattr() throws Exception{
            
        }
        
        @Test
        public void testSymlink() throws Exception{
            
        }
        
        private IrodsVirtualFileSystem getVFS() throws Exception{
            IRODSAccount irodsAccount = testingPropertiesHelper.buildIRODSAccountFromTestProperties(testingProperties);
            IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
            String homeDir = MiscIRODSUtils.buildIRODSUserHomeForAccountUsingDefaultScheme(irodsAccount);
            IRODSFile rootFile = accessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(homeDir);
            return new IrodsVirtualFileSystem(accessObjectFactory, irodsAccount, rootFile);
        }
        

}
