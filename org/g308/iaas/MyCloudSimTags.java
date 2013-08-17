/**
 * 
 */
package org.g308.iaas;

/**
 * Contains various static command tags that indicate a type of action that needs to be undertaken
 * by CloudSim entities when they receive or send events.
 * 
 * @see org.cloudbus.cloudsim.core.CloudSimTags
 * 
 * @author wanglong
 * @since IAAS Simulation Toolkit 1.0
 */
public class MyCloudSimTags{

	/** Starting constant value for cloud-related tags **/
	private static final int BASE = 0;
	
	/**
	 * Denotes an internal event generated in a MyPowerDatacenter
	 */
	public static final int MY_VM_DATACENTER_EVENT = BASE + 50;
	
	/**
	 * 
	 */
	public static final int MY_VM_RETURN = BASE + 51;
	
	/**
	 * 
	 */
	public static final int MY_VM_SUBMIT = BASE + 52;
	
	/**
	 * 
	 */
	public static final int MY_VM_SUBMIT_ACK = BASE + 53;
	
	/** Private Constructor */
	private MyCloudSimTags() {
		throw new UnsupportedOperationException("CloudSim Tags cannot be instantiated");
	}
}
