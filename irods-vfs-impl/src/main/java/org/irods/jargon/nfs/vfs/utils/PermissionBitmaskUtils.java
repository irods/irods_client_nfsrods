/**
 * 
 */
package org.irods.jargon.nfs.vfs.utils;

/**
 * Utils for managing permissions and permission bitmasks
 * 
 * @author Mike Conway - NIEHS
 *
 */
public class PermissionBitmaskUtils {
	public static final int USER_READ = 0b100000000;
	public static final int USER_WRITE = 0b010000000;
	public static final int USER_EXECUTE = 0b001000000;
	public static final int GROUP_READ = 0b000100000;
	public static final int GROUP_WRITE = 0b000010000;
	public static final int GROUP_EXECUTE = 0b000001000;
	public static final int OTHERS_READ = 0b000000100;
	public static final int OTHERS_WRITE = 0b000000010;
	public static final int OTHERS_EXECUTE = 0b000000001;

}
