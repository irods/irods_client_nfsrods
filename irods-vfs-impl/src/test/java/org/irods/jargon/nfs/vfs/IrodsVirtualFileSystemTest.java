package org.irods.jargon.nfs.vfs;

import java.util.Properties;

import org.dcache.nfs.vfs.Inode;
import org.dcache.nfs.vfs.Stat;
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

}
