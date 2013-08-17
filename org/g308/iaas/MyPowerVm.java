/**
 * 
 */
package org.g308.iaas;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.ResCloudlet;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * @author wanglong
 *
 */
public class MyPowerVm extends Vm {

	/** The Constant HISTORY_LENGTH. */
	public static final int HISTORY_LENGTH = 30;
	
	/** The utilization history. */
	private final List<Double> utilizationHistory = new LinkedList<Double>();
	
	/** The previous time. */
	private double previousTime;
	
	/** The index. */
	private int index;
	
	/** The scheduling interval. */
	private double schedulingInterval;
	
	/** The records the transaction history for this virtual machine. */
	private final boolean record;

	/** The newline. */
	private String newline;

	/** The history. */
	private StringBuffer history;
	
	/** The cache previous time. */
	private double cachePreviousTime;

	/** The cache current requested mips. */
	private List<Double> cacheCurrentRequestedMips;
	
	/**
	 * Start time of executing this virtual machine. With new functionalities, such as CANCEL, PAUSED and
	 * RESUMED, this attribute only stores the latest execution time. Previous execution time are
	 * ignored.
	 */
	private double execStartTime;
	
	/** The total mips. */
	private double totalMips;
	
	/** The start time. */
	private double startTime;
	
	/** The end time. */
	private double endTime;
	
	/** The status of this virtual machine. */
	private int status;
	
	// //////////////////////////////////////////
	// Below are CONSTANTS attributes
	/** The Cloudlet has been created and added to the CloudletList object. */
	public static final int CREATED = 0;

	/** The Cloudlet has been assigned to a CloudResource object as planned. */
	public static final int READY = 1;

	/** The Cloudlet has moved to a Cloud node. */
	public static final int QUEUED = 2;

	/** The Cloudlet is in execution in a Cloud node. */
	public static final int INEXEC = 3;

	/** The Cloudlet has been executed successfully. */
	public static final int SUCCESS = 4;

	/** The Cloudlet is failed. */
	public static final int FAILED = 5;

	/** The Cloudlet has been canceled. */
	public static final int CANCELED = 6;

	/**
	 * The Cloudlet has been paused. It can be resumed by changing the status into <tt>RESUMED</tt>.
	 */
	public static final int PAUSED = 7;

	/** The Cloudlet has been resumed from <tt>PAUSED</tt> state. */
	public static final int RESUMED = 8;

	/** The cloudlet has failed due to a resource failure. */
	public static final int FAILED_RESOURCE_UNAVAILABLE = 9;
	
	/** The vm id. */
	protected int hostId;
	
	/** The broker id. */
	protected int userId;

	/** The cost per bw. */
	protected double costPerBw;

	/** The accumulated bw cost. */
	protected double accumulatedBwCost;
	
	// Utilization

	/** The utilization of cpu model. */
	private UtilizationModel utilizationModelCpu;

	/** The utilization of memory model. */
	private UtilizationModel utilizationModelRam;

	/** The utilization of bw model. */
	private UtilizationModel utilizationModelBw;


	/**
	 * @param id
	 * @param userId
	 * @param mips
	 * @param numberOfPes
	 * @param ram
	 * @param bw
	 * @param size
	 * @param vmm
	 * @param cloudletScheduler
	 * @param previoustime
	 * @param schedulingInterval
	 * @param record
	 * @param newline
	 * @param history
	 * @param execStartTime
	 * @param startTime
	 * @param endTime
	 * @param status
	 * @param utilizationModelCpu
	 * @param utilizationModelRam
	 * @param utilizationModelBw
	 */
	public MyPowerVm(int id, 
			int userId,
			double mips, 
			int numberOfPes, 
			int ram,
			long bw, 
			long size, 
			String vmm,
			double startTime,
			double endTime, 
			UtilizationModel utilizationModelCpu,
			UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw,
			double schedulingInterval) {
			this(
					id,
					userId, 
					mips, 
					numberOfPes, 
					ram, 
					bw, 
					size, 
					vmm,
					startTime,
					endTime,
					utilizationModelCpu,
					utilizationModelRam,
					utilizationModelBw,
					schedulingInterval,
					false);
			
			this.hostId = -1;
			this.accumulatedBwCost = 0.0;
			this.costPerBw = 0.0;
	}


	public MyPowerVm(int id, 
			int userId, 
			double mips, 
			int numberOfPes, 
			int ram,
			long bw, 
			long size, 
			String vmm, 
			double startTime, 
			double endTime,
			UtilizationModel utilizationModelCpu,
			UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw, 
			double schedulingInterval,
			boolean record) {
			
			super(id, userId, mips, numberOfPes, ram, bw,
					size, vmm, null);
				userId  = -1;
			
			status = CREATED;
			this.record = record;
			this.setStartTime(startTime);
			this.setEndTime(endTime);
			this.setUtilizationModelBw(utilizationModelBw);
			this.setUtilizationModelCpu(utilizationModelCpu);
			this.setUtilizationModelRam(utilizationModelRam);
			this.setSchedulingInterval(schedulingInterval);
			this.setTotalMips(getNumberOfPes() * getMips());
			this.setCachePreviousTime(-1);
			index = -1;
			
			this.hostId = -1;
			this.accumulatedBwCost = 0.0;
			this.costPerBw = 0.0;
	}
	

	public List<Double> getUtilizationHistory() {
		return utilizationHistory;
	}


	public double getPreviousTime() {
		return previousTime;
	}


	public double getSchedulingInterval() {
		return schedulingInterval;
	}

	/**
	 * Sets the total mips.
	 * 
	 * @param mips the new total mips
	 */
	public void setTotalMips(double mips) {
		totalMips = mips;
	}

	/**
	 * Gets the total mips.
	 * 
	 * @return the total mips
	 */
	public double getTotalMips() {
		return totalMips;
	}

	public boolean isRecord() {
		return record;
	}


	public StringBuffer getHistory() {
		return history;
	}


	public double getExecStartTime() {
		return execStartTime;
	}


	public double getStartTime() {
		return startTime;
	}


	public double getEndTime() {
		return endTime;
	}


	public int getStatus() {
		return status;
	}

	public void setPreviousTime(double previousTime) {
		this.previousTime = previousTime;
	}


	public void setSchedulingInterval(double schedulingInterval) {
		this.schedulingInterval = schedulingInterval;
	}


	public void setHistory(StringBuffer history) {
		this.history = history;
	}


	public void setExecStartTime(double execStartTime) {
		this.execStartTime = execStartTime;
	}


	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}


	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}


	public void setStatus(int status) {
		this.status = status;
	}
	
	/**
	 * Gets the ID of the broker
	 * 
	 * @return user id
	 */
	public int getUserId() {
		return userId;
	}
	
	/**
	 * Sets the ID of the broker
	 * 
	 * @param userId the broker id
	 * @pre id >= 0
	 * @post none
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}
	
	/**
	 * Gets the ID of the Host that will run this virtual machine
	 * 
	 * @return host id, -1 if the virtual machines was not assigned to a virtual machine
	 */
	public int getHostId() {
		return hostId;
	}


	/**
	 * Sets the ID of the host that will run this virtual machine
	 * 
	 * @param hostId the host id
	 * @pre id >= 0
	 * @post $none
	 */
	public void setHostId(int hostId) {
		this.hostId = hostId;
	}
	
	/**
	 * Gets the cache previous time.
	 * 
	 * @return the cache previous time
	 */
	protected double getCachePreviousTime() {
		return cachePreviousTime;
	}

	/**
	 * Sets the cache previous time.
	 * 
	 * @param cachePreviousTime the new cache previous time
	 */
	protected void setCachePreviousTime(double cachePreviousTime) {
		this.cachePreviousTime = cachePreviousTime;
	}

	/**
	 * Gets the cache current requested mips.
	 * 
	 * @return the cache current requested mips
	 */
	protected List<Double> getCacheCurrentRequestedMips() {
		return cacheCurrentRequestedMips;
	}

	/**
	 * Sets the cache current requested mips.
	 * 
	 * @param cacheCurrentRequestedMips the new cache current requested mips
	 */
	protected void setCacheCurrentRequestedMips(List<Double> cacheCurrentRequestedMips) {
		this.cacheCurrentRequestedMips = cacheCurrentRequestedMips;
	}
	
	/**
	 * Sets the utilization model cpu.
	 * 
	 * @param utilizationModelCpu the new utilization model cpu
	 */
	public void setUtilizationModelCpu(final UtilizationModel utilizationModelCpu) {
		this.utilizationModelCpu = utilizationModelCpu;
	}

	/**
	 * Gets the utilization model of cpu
	 * 
	 * @return the utilization model of cpu
	 */
	public UtilizationModel getUtilizationModelCpu() {
		return utilizationModelCpu;
	}
	
	
	/**
	 * Gets the utilization of cpu
	 * 
	 * @param time the time
	 * @return the utilization of cpu
	 */
	public double getUtilizationOfCpu(final double time){
		return this.getUtilizationModelCpu().getUtilization(time);
	}
	


	/**
	 * Sets the utilization model ram.
	 * 
	 * @param utilizationModelRam the new utilization model ram
	 */
	public void setUtilizationModelRam(final UtilizationModel utilizationModelRam) {
		this.utilizationModelRam = utilizationModelRam;
	}


	/**
	 * Gets the utilization model of ram
	 * 
	 * @return the utilization model of ram
	 */
	public UtilizationModel getUtilizationModelRam() {
		return utilizationModelRam;
	}
	
	/**
	 * Gets the utilization of memory
	 * 
	 * @param time the time
	 * @return the utilization of memory
	 */
	public double getUtilizationOfRam(final double time){
		return this.getUtilizationModelRam().getUtilization(time);
	}


	/**
	 * Sets the utilization model bw
	 * @param utilizationModelBw the new utilization model bw
	 */
	public void setUtilizationModelBw(final UtilizationModel utilizationModelBw) {
		this.utilizationModelBw = utilizationModelBw;
	}


	/**
	 * Gets the utilization model of bw
	 * 
	 * @return the utilization model of bw
	 */
	public UtilizationModel getUtilizationModelBw() {
		return utilizationModelBw;
	}
	
	/**
	 * Gets the utilization of bw.
	 * 
	 * @param time the time
	 * @return the utilization of bw
	 */
	public double getUtilizationOfBw(final double time){
		return this.getUtilizationModelBw().getUtilization(time);
	}
			
	/**
	 * Gets the current requested mips.
	 * 
	 * @return the current requested mips
	 */
	public List<Double> getCurrentRequestedMips() {
		
		if (getCachePreviousTime() == getPreviousTime()) {
			return getCacheCurrentRequestedMips();
		}
		
		List<Double> currentMips = new ArrayList<Double>();
		double totalMips = getTotalUtilizationOfCpu(getPreviousTime()) * getTotalMips();
		double mipsForPe = totalMips / getNumberOfPes();

		for (int i = 0; i < getNumberOfPes(); i++) {
			currentMips.add(mipsForPe);
		}

		setCachePreviousTime(getPreviousTime());
		setCacheCurrentRequestedMips(currentMips);

		return currentMips;
		
//		List<Double> currentRequestedMips = getCloudletScheduler().getCurrentRequestedMips();
//		
//		if (isBeingInstantiated()) {
//			currentRequestedMips = new ArrayList<Double>();
//			for (int i = 0; i < getNumberOfPes(); i++) {
//				currentRequestedMips.add(getMips());
//			}
//		}
//		return currentRequestedMips;
	}
	
	/**
	 * Get utilization created by all clouddlets running on this VM.
	 * 
	 * @param time the time
	 * @return total utilization
	 */
	public double getTotalUtilizationOfCpu(double time) {
//		return getCloudletScheduler().getTotalUtilizationOfCpu(time);
		
		return getUtilizationOfCpu(time);
//		double totalUtilization = 0;
//		for (ResCloudlet rcl : getCloudletExecList()) {
//			totalUtilization += rcl.getCloudlet().getUtilizationOfCpu(time);
//		}
//		return totalUtilization;
	}
	
	public double updateVmProcessing(double currentTime, List<Double> mipsShare) {
		if (mipsShare != null) {
//			return getCloudletScheduler().updateVmProcessing(currentTime, mipsShare);

//			double timeSpan = currentTime - getPreviousTime();
			double nextEvent = getEndTime();
			
			
			return nextEvent;
		}
		return 0.0;
	}
	
	/**
	 * Checks whether this virtual machine has finished execution or not.
	 * 
	 * @return <tt>true</tt> if this vm has finished execution, <tt>false</tt> otherwise
	 * @pre $none
	 * @post $none
	 */
	public boolean isFinished() {
		if (index == -1) {
			return false;
		}
		boolean completed = false;
		
		final double finish = getEndTime();
		final double now = CloudSim.clock(); 

		final double result = finish - now;

		if (result >= 0.0) {
			completed = true;
		}
		return completed;
	}
	
	/**
	 * Gets the current requested bw.
	 * 
	 * @return the current requested bw
	 */
	public long getCurrentRequestedBw() {
		if (isBeingInstantiated()) {
			return getBw();
		}
		return (long) (this.getUtilizationOfBw(CloudSim.clock()) * getBw());
	}

	/**
	 * Gets the current requested ram.
	 * 
	 * @return the current requested ram
	 */
	public int getCurrentRequestedRam() {
		if (isBeingInstantiated()) {
			return getRam();
		}
		return (int) (this.getUtilizationOfRam(CloudSim.clock()) * getRam());
	}
}
