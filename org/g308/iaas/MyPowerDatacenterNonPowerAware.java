package org.g308.iaas;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.power.PowerHost;

public class MyPowerDatacenterNonPowerAware extends MyPowerDatacenter {

	
	/**
	 * Instantiates a new datacenter.
	 * 
	 * @param name the name
	 * @param characteristics the res config
	 * @param schedulingInterval the scheduling interval
	 * @param utilizationBound the utilization bound
	 * @param vmAllocationPolicy the vm provisioner
	 * @param storageList the storage list
	 * 
	 * @throws Exception the exception
	 */
	public MyPowerDatacenterNonPowerAware(
			String name,
			DatacenterCharacteristics characteristics,
			VmAllocationPolicy vmAllocationPolicy, 
			List<Storage> storageList,
			double schedulingInterval) throws Exception {
		super(
				name, 
				characteristics, 
				vmAllocationPolicy, 
				storageList,
				schedulingInterval
				);
		// TODO Auto-generated constructor stub
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
		double timeframePower = 0.0;

		// if some time passed since last processing
		if (currentTime > getLastProcessTime()) {
			
			double timeDiff = currentTime - getLastProcessTime();
			double minTime = Double.MAX_VALUE;

			Log.printLine("\n");

			for (MyPowerHost host : this.<MyPowerHost> getHostList()) {
				Log.formatLine("%.2f: Host #%d", CloudSim.clock(), host.getId());

				double hostPower = 0.0;

				try {
					hostPower = host.getMaxPower() * timeDiff;
					timeframePower += hostPower;
				} catch (Exception e) {
					e.printStackTrace();
				}

				Log.formatLine(
						"%.2f: Host #%d utilization is %.2f%%",
						CloudSim.clock(),
						host.getId(),
						host.getUtilizationOfCpu() * 100);
				Log.formatLine(
						"%.2f: Host #%d energy is %.2f W*sec",
						CloudSim.clock(),
						host.getId(),
						hostPower);
			}
			
			Log.formatLine("\n%.2f: Consumed energy is %.2f W*sec\n", CloudSim.clock(), timeframePower);

			Log.printLine("\n\n--------------------------------------------------------------\n\n");

			for (MyPowerHost host : this.<MyPowerHost> getHostList()) {
				Log.formatLine("\n%.2f: Host #%d", CloudSim.clock(), host.getId());

				double time = host.updateVmsProcessing(currentTime); // inform VMs to update
																		// processing
				if (time < minTime) {
					minTime = time;
				}
			}

			//更新当前数据中心的总耗电量，单位： W*sec
			setPower(getPower() + timeframePower);
			
			checkVirtualMachineCompletion();
			
			Log.printLine();




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

	
}
