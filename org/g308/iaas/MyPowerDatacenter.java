/**
 * 
 */
package org.g308.iaas;

import java.util.List;
import java.util.Map;


import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.examples.power.Constants;


/**
 * MyPowerDatacenter is a class that enables simulation of power-aware  IAAS data centers.
 * 
 * 
 * @author wanglong
 * @since IAAS Simulation Toolkit 1.0
 */
public class MyPowerDatacenter extends Datacenter {

	/** The power. */
	private double power;
	
	/** The disable migrations. */
	private boolean disableMigrations;
	
	/** The virtual machine submited. */
	private double vitualMachineSubmitted;
	
	/** The migration count. */
	private int migrationCount;

	/**
	 * Instantiates a new MyPowerDatacenter.
	 * 
	 * @param name the name
	 * @param characteristics the res config
	 * @param schedulingInterval the scheduling interval
	 * @param vmAllocationPolicy the vm provisioner
	 * @param storageList the storage list
	 * @throws Exception the exception
	 */
	public MyPowerDatacenter(String name,
			DatacenterCharacteristics characteristics,
			VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
			double schedulingInterval) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList,
				schedulingInterval);
		
		setPower(0.0);
		setDisableMigrations(false);
		setVitualMachineSubmitted(-1);
		setMigrationCount(0);
	}
	
	/**
	 * Updates processing of each virtual machine running in this PowerDatacenter. It is necessary because
	 * Hosts and VirtualMachines are simple objects, not entities. So, they don't receive events and
	 * updating virtual machines inside them must be called from the outside.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void updateVirtualMachineProcessing() {
		if (getVitualMachineSubmitted() == -1 || getVitualMachineSubmitted() == CloudSim.clock()) {
			CloudSim.cancelAll(getId(), new PredicateType(MyCloudSimTags.MY_VM_DATACENTER_EVENT));
			schedule(getId(), getSchedulingInterval(), MyCloudSimTags.MY_VM_DATACENTER_EVENT);
			return;
		}
		double currentTime = CloudSim.clock();

		// if some time passed since last processing
		if (currentTime > getLastProcessTime()) {
			System.out.print(currentTime + " ");

			double minTime = updateVmProcessingWithoutSchedulingFutureEventsForce();

			//进行优化后，处理迁移事件
			if (!isDisableMigrations()) {
				List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(
						getVmList());

				if (migrationMap != null) {
					for (Map<String, Object> migrate : migrationMap) {
						Vm vm = (Vm) migrate.get("vm");
						MyPowerHost targetHost = (MyPowerHost) migrate.get("host");
						MyPowerHost oldHost = (MyPowerHost) vm.getHost();

						if (oldHost == null) {
							Log.formatLine(
									"%.2f: Migration of VM #%d to Host #%d is started",
									currentTime,
									vm.getId(),
									targetHost.getId());
						} else {
							Log.formatLine(
									"%.2f: Migration of VM #%d from Host #%d to Host #%d is started",
									currentTime,
									vm.getId(),
									oldHost.getId(),
									targetHost.getId());
						}

						targetHost.addMigratingInVm(vm);
						incrementMigrationCount();

						/** VM migration delay = RAM / bandwidth **/
						// we use BW / 2 to model BW available for migration purposes, the other
						// half of BW is for VM communication
						// around 16 seconds for 1024 MB using 1 Gbit/s network
						//Gbit/s / 8 = GB/s
						send(
								getId(),
								vm.getRam() / ((double) targetHost.getBw() / (2 * 8000)),
								CloudSimTags.VM_MIGRATE,
								migrate);
					}
				}
			}

			// schedules an event to the next time
			// 调度下一次事件MY_VM_DATACENTER_EVENT，确保队列中只有一个MY_VM_DATACENTER_EVENT事件。
			if (minTime != Double.MAX_VALUE) {
				CloudSim.cancelAll(getId(), new PredicateType(MyCloudSimTags.MY_VM_DATACENTER_EVENT));
				send(getId(), getSchedulingInterval(), MyCloudSimTags.MY_VM_DATACENTER_EVENT);
			}

			setLastProcessTime(currentTime);
		}
	}

	
	/**
	 * Update cloudet processing without scheduling future events.
	 * 
	 * @return the double
	 */
	protected double updateVmProcessingWithoutSchedulingFutureEvents() {
		if (CloudSim.clock() > getLastProcessTime()) {
			return updateVmProcessingWithoutSchedulingFutureEventsForce();
		}
		return 0;
	}

	/**
	 * Update vm processing without scheduling future events.
	 * 
	 * @return the double
	 */
	protected double updateVmProcessingWithoutSchedulingFutureEventsForce() {
		double currentTime = CloudSim.clock();
		double minTime = Double.MAX_VALUE;
		double timeDiff = currentTime - getLastProcessTime();
		double timeFrameDatacenterEnergy = 0.0;

		Log.printLine("\n\n--------------------------------------------------------------\n\n");
		Log.formatLine("New resource usage for the time frame starting at %.2f:", currentTime);

		for (MyPowerHost host : this.<MyPowerHost> getHostList()) {
			Log.printLine();

			double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
			if (time < minTime) {
				minTime = time;
			}

			Log.formatLine(
					"%.2f: [Host #%d] utilization is %.2f%%",
					currentTime,
					host.getId(),
					host.getUtilizationOfCpu() * 100);
		}

		if (timeDiff > 0) {
			Log.formatLine(
					"\nEnergy consumption for the last time frame from %.2f to %.2f:",
					getLastProcessTime(),
					currentTime);

			//计算每一个物理主机的能耗情况
			for (MyPowerHost host : this.<MyPowerHost> getHostList()) {
				double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
				double utilizationOfCpu = host.getUtilizationOfCpu();
				double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
						previousUtilizationOfCpu,
						utilizationOfCpu,
						timeDiff);
				timeFrameDatacenterEnergy += timeFrameHostEnergy;

				Log.printLine();
				Log.formatLine(
						"%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
						currentTime,
						host.getId(),
						getLastProcessTime(),
						previousUtilizationOfCpu * 100,
						utilizationOfCpu * 100);
				Log.formatLine(
						"%.2f: [Host #%d] energy is %.2f W*sec",
						currentTime,
						host.getId(),
						timeFrameHostEnergy);
			}

			Log.formatLine(
					"\n%.2f: Data center's energy is %.2f W*sec\n",
					currentTime,
					timeFrameDatacenterEnergy);
		}

		setPower(getPower() + timeFrameDatacenterEnergy);

		checkVirtualMachineCompletion();

		/** Remove completed VMs **/
//		for (PowerHost host : this.<PowerHost> getHostList()) {
//			for (Vm vm : host.getCompletedVms()) {
//				getVmAllocationPolicy().deallocateHostForVm(vm);
//				getVmList().remove(vm);
//				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
//			}
//		}

		Log.printLine();
//		int num_of_vm = 0;
//		for (PowerHost host : this.<PowerHost> getHostList()) {
//			num_of_vm += host.getVmList().size();
//		}
//		Log.printLine("VM Counts: " + num_of_vm);
//		
//		Log.printLine();

		setLastProcessTime(currentTime);
		return minTime;
	}

	
	/**
	 * Processes events or services that are available for this MyPowerDatacenter.
	 * 
	 * @param ev a Sim_event object
	 * @pre ev != null
	 * @post $none
	 */
	@Override
	public void processEvent(SimEvent ev) {
		int srcId = -1;

		switch (ev.getTag()) {
		// Resource characteristics inquiry
			case CloudSimTags.RESOURCE_CHARACTERISTICS:
				srcId = ((Integer) ev.getData()).intValue();
				sendNow(srcId, ev.getTag(), getCharacteristics());
				break;

			// Resource dynamic info inquiry
			case CloudSimTags.RESOURCE_DYNAMICS:
				srcId = ((Integer) ev.getData()).intValue();
				sendNow(srcId, ev.getTag(), 0);
				break;

			case CloudSimTags.RESOURCE_NUM_PE:
				srcId = ((Integer) ev.getData()).intValue();
				int numPE = getCharacteristics().getNumberOfPes();
				sendNow(srcId, ev.getTag(), numPE);
				break;

			case CloudSimTags.RESOURCE_NUM_FREE_PE:
				srcId = ((Integer) ev.getData()).intValue();
				int freePesNumber = getCharacteristics().getNumberOfFreePes();
				sendNow(srcId, ev.getTag(), freePesNumber);
				break;

			// New Cloudlet arrives
			case CloudSimTags.CLOUDLET_SUBMIT:
				processCloudletSubmit(ev, false);
				break;

			// New Cloudlet arrives, but the sender asks for an ack
			case CloudSimTags.CLOUDLET_SUBMIT_ACK:
				processCloudletSubmit(ev, true);
				break;

			// Cancels a previously submitted Cloudlet
			case CloudSimTags.CLOUDLET_CANCEL:
				processCloudlet(ev, CloudSimTags.CLOUDLET_CANCEL);
				break;

			// Pauses a previously submitted Cloudlet
			case CloudSimTags.CLOUDLET_PAUSE:
				processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE);
				break;

			// Pauses a previously submitted Cloudlet, but the sender
			// asks for an acknowledgement
			case CloudSimTags.CLOUDLET_PAUSE_ACK:
				processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE_ACK);
				break;

			// Resumes a previously submitted Cloudlet
			case CloudSimTags.CLOUDLET_RESUME:
				processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME);
				break;

			// Resumes a previously submitted Cloudlet, but the sender
			// asks for an acknowledgement
			case CloudSimTags.CLOUDLET_RESUME_ACK:
				processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME_ACK);
				break;

			// Moves a previously submitted Cloudlet to a different resource
			case CloudSimTags.CLOUDLET_MOVE:
				processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE);
				break;

			// Moves a previously submitted Cloudlet to a different resource
			case CloudSimTags.CLOUDLET_MOVE_ACK:
				processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE_ACK);
				break;

			// Checks the status of a Cloudlet
			case CloudSimTags.CLOUDLET_STATUS:
				processCloudletStatus(ev);
				break;

			// Ping packet
			case CloudSimTags.INFOPKT_SUBMIT:
				processPingRequest(ev);
				break;

			case CloudSimTags.VM_CREATE:
				processVmCreate(ev, false);
				break;

			case CloudSimTags.VM_CREATE_ACK:
				processVmCreate(ev, true);
				break;

			case CloudSimTags.VM_DESTROY:
				processVmDestroy(ev, false);
				break;

			case CloudSimTags.VM_DESTROY_ACK:
				processVmDestroy(ev, true);
				break;

			case CloudSimTags.VM_MIGRATE:
				processVmMigrate(ev, false);
				break;

			case CloudSimTags.VM_MIGRATE_ACK:
				processVmMigrate(ev, true);
				break;

			case CloudSimTags.VM_DATA_ADD:
				processDataAdd(ev, false);
				break;

			case CloudSimTags.VM_DATA_ADD_ACK:
				processDataAdd(ev, true);
				break;

			case CloudSimTags.VM_DATA_DEL:
				processDataDelete(ev, false);
				break;

			case CloudSimTags.VM_DATA_DEL_ACK:
				processDataDelete(ev, true);
				break;

			case CloudSimTags.VM_DATACENTER_EVENT:
				updateCloudletProcessing();
				checkCloudletCompletion();
				break;

			case MyCloudSimTags.MY_VM_DATACENTER_EVENT:
				updateVirtualMachineProcessing();
				checkVirtualMachineCompletion();
				break;
				
			case MyCloudSimTags.MY_VM_RETURN:
				break;
			
			// New virtual machine arrives
			case MyCloudSimTags.MY_VM_SUBMIT:
				processVirtualMachineSubmit(ev, false);
				break;

			// New virtual machine arrives, but the sender asks for an ack
			case MyCloudSimTags.MY_VM_SUBMIT_ACK:
				processVirtualMachineSubmit(ev, true);
				break;
				
			// other unknown tags are processed by this method
			default:
				processOtherEvent(ev);
				break;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.Datacenter#processVmMigrate(org.cloudbus.cloudsim.core.SimEvent,
	 * boolean)
	 */
	@Override
	protected void processVmMigrate(SimEvent ev, boolean ack) {
		updateVmProcessingWithoutSchedulingFutureEvents();
		super.processVmMigrate(ev, ack);
		SimEvent event = CloudSim.findFirstDeferred(getId(), new PredicateType(CloudSimTags.VM_MIGRATE));
		if (event == null || event.eventTime() > CloudSim.clock()) {
			updateVmProcessingWithoutSchedulingFutureEventsForce();
		}
	}
	
	/**
	 * Verifies if some virtual machine inside this PowerDatacenter already finished. If yes, send it to
	 * the User/Broker
	 * 
	 * 检查是否有vm运行结束，如果有，将其发送给broker
	 * @pre $none
	 * @post $none
	 */
	protected void checkVirtualMachineCompletion() {
		List<? extends Host> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			MyPowerHost host = (MyPowerHost) list.get(i);
			List<? extends MyPowerVm> vmList = host.getVmList();
			for (MyPowerVm vm : vmList) {
				if (vm.isFinished()){
					sendNow(vm.getUserId(), MyCloudSimTags.MY_VM_RETURN, vm);
				}
//				while (vm.getCloudletScheduler().isFinishedCloudlets()) {
//					Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
//					if (cl != null) {
//						sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
//					}
//				}
			}
		}
	}
	
	/**
	 * Checks if PowerDatacenter is in migration.
	 * 
	 * @return true, if PowerDatacenter is in migration
	 */
	protected boolean isInMigration() {
		boolean result = false;
		for (Vm vm : getVmList()) {
			if (vm.isInMigration()) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	/**
	 * Gets the power.
	 * 
	 * @return the power
	 */
	public double getPower() {
		return power;
	}

	/**
	 * Sets the power.
	 * 
	 * @param power the new power
	 */
	protected void setPower(double power) {
		this.power = power;
	}
	
	/**
	 * Checks if is disable migrations.
	 * 
	 * @return true, if is disable migrations
	 */
	public boolean isDisableMigrations() {
		return disableMigrations;
	}

	/**
	 * Sets the disable migrations.
	 * 
	 * @param disableMigrations the new disable migrations
	 */
	public void setDisableMigrations(boolean disableMigrations) {
		this.disableMigrations = disableMigrations;
	}
	
	
	/**
	 * Checks if is virtual machine submitted.
	 * 
	 * @return true, if is virtual machine submitted
	 */
	protected double getVitualMachineSubmitted() {
		return vitualMachineSubmitted;
	}

	/**
	 * Sets the virtual machine submitted.
	 * 
	 * @param cloudletSubmitted the new virtual machine submitted
	 */
	protected void setVitualMachineSubmitted(double vitualMachineSubmitted) {
		this.vitualMachineSubmitted = vitualMachineSubmitted;
	}

	/**
	 * Gets the migration count.
	 * 
	 * @return the migration count
	 */
	public int getMigrationCount() {
		return migrationCount;
	}

	/**
	 * Sets the migration count.
	 * 
	 * @param migrationCount the new migration count
	 */
	protected void setMigrationCount(int migrationCount) {
		this.migrationCount = migrationCount;
	}

	/**
	 * Increment migration count.
	 */
	protected void incrementMigrationCount() {
		setMigrationCount(getMigrationCount() + 1);
	}

	/**
	 * Processes a virtual machine submission.
	 * 
	 * @param ev a SimEvent object
	 * @param ack an acknowledgement
	 * @pre ev != null
	 * @post $none
	 */
	protected void processVirtualMachineSubmit(SimEvent ev, boolean ack) {
		updateVirtualMachineProcessing();

		try {
			// gets the vm object
			MyPowerVm vm = (MyPowerVm) ev.getData();
			vm.setHostId(vm.getHost().getId());

			// checks whether this vm has finished or not
			if (vm.isFinished()) {
				String name = CloudSim.getEntityName(vm.getUserId());
				Log.printLine(getName() + ": Warning - Vm #" + vm.getId() + " owned by " + name
						+ " is already completed/finished.");
				Log.printLine("Therefore, it is not being executed again");
				Log.printLine();

				// NOTE: If a vm has finished, then it won't be processed.
				// So, if ack is required, this method sends back a result.
				// If ack is not required, this method don't send back a result.
				// Hence, this might cause CloudSim to be hanged since waiting
				// for this vm back.
				if (ack) {
					int[] data = new int[3];
					data[0] = getId();
					data[1] = vm.getId();
					data[2] = CloudSimTags.FALSE;

					// unique tag = operation tag
					int tag = MyCloudSimTags.MY_VM_SUBMIT_ACK;
					sendNow(vm.getUserId(), tag, data);
				}

				sendNow(vm.getUserId(), MyCloudSimTags.MY_VM_RETURN, vm);

				return;
			}

//			// process this Cloudlet to this CloudResource
//			cl.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(), getCharacteristics()
//					.getCostPerBw());
//
//			int userId = cl.getUserId();
//			int vmId = cl.getVmId();
//
//			// time to transfer the files
//			double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());
//
//			Host host = getVmAllocationPolicy().getHost(vmId, userId);
//			Vm vm = host.getVm(vmId, userId);
//			CloudletScheduler scheduler = vm.getCloudletScheduler();
//			double estimatedFinishTime = scheduler.cloudletSubmit(cl, fileTransferTime);

			// if this cloudlet is in the exec queue
//			if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
//				estimatedFinishTime += fileTransferTime;
//				send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
//			}

			send(getId(), Constants.SIMULATION_LIMIT, MyCloudSimTags.MY_VM_DATACENTER_EVENT);
			
			if (ack) {
				int[] data = new int[3];
				data[0] = getId();
				data[1] = vm.getId();
				data[2] = CloudSimTags.TRUE;

				// unique tag = operation tag
				int tag = MyCloudSimTags.MY_VM_SUBMIT_ACK;
				sendNow(vm.getUserId(), tag, data);
			}
		} catch (ClassCastException c) {
			Log.printLine(getName() + ".processVirtualMachineSubmit(): " + "ClassCastException error.");
			c.printStackTrace();
		} catch (Exception e) {
			Log.printLine(getName() + ".processVirtualMachineSubmit(): " + "Exception error.");
			e.printStackTrace();
		}

		checkVirtualMachineCompletion();
		setVitualMachineSubmitted(CloudSim.clock());
	}
	
}
