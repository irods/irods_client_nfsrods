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

	/**
	 * Is the user read mode bit set
	 * 
	 * @param mode
	 *            <code>int</code> with bitmask
	 * @return <code>boolean</code>
	 */
	public static boolean isUserReadSet(final int mode) {
		return (mode & USER_READ) != 0;
	}

	/**
	 * Is the user write mode bit set
	 * 
	 * @param mode
	 *            <code>int</code> with bitmask
	 * @return <code>boolean</code>
	 */
	public static boolean isUserWriteSet(final int mode) {
		return (mode & USER_WRITE) != 0;
	}

	/**
	 * Is the user exec mode bit set
	 * 
	 * @param mode
	 *            <code>int</code> with bitmask
	 * @return <code>boolean</code>
	 */
	public static boolean isUserExecuteSet(final int mode) {
		return (mode & USER_EXECUTE) != 0;
	}

	/**
	 * Alter the given int bitmask by turning on read mode
	 * 
	 * @param mode
	 *            <code>int</code> bitmask that will have the desired mode turned on
	 */
	public static void turnOnUserRead(int mode) {
		mode |= USER_READ;
	}

	/**
	 * Alter the given int bitmask by turning on write mode
	 * 
	 * @param mode
	 *            <code>int</code> bitmask that will have the desired mode turned on
	 */
	public static void turnOnUserWrite(int mode) {
		mode |= USER_WRITE;
	}

	/**
	 * Alter the given int bitmask by turning on write mode
	 * 
	 * @param mode
	 *            <code>int</code> bitmask that will have the desired mode turned on
	 */
	public static void turnOnUserExecute(int mode) {
		mode |= USER_EXECUTE;
	}

}
