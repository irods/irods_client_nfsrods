package org.irods.jargon.nfs.vfs.unittest;

import org.irods.jargon.nfs.vfs.IrodsVirtualFileSystemTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ IrodsVirtualFileSystemTest.class })

/**
 * Suite to run all tests (except long running and functional), further refined
 * by settings in testing.properites. Some subtests may be shut off by these
 * properties.
 */
public class AllTests {

}
