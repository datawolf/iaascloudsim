/**
 * 
 */
package org.g308.iaas;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.examples.power.Constants;
import org.cloudbus.cloudsim.lists.VmList;

/**
 * A broker for the power package
 * 
 * @author wanglong
 * @since IAAS Simulation Toolkit 1.0
 */
public class MyPowerDatacenterBroker extends DatacenterBroker {
	
	/** The virtual machine list. */
	protected List<? extends Vm> vmReceivedList;

	/** The virtual machine submitted list. */
	protected List<? extends Vm> vmSubmittedList;

	/** The virtual machine submitted. */
	protected int vmsSubmitted;
	
	/**
	 * Instantiates a new power datacenter broker.
	 * 
	 * @param name the name
	 * @throws Exception the exception
	 */
	public MyPowerDatacenterBroker(String name) throws Exception {
		super(name);
		
		
		setVmSubmittedList(new ArrayList<Vm>());
		setVmReceivedList(new ArrayList<Vm>());

		vmsSubmitted = 0;
	}
	
	
	/**
	 * Sets the virtual machine received list.
	 * 
	 * @param <T> the generic type
	 * @param vmReceivedList  the new virtual machine received list
	 */
	protected <T extends Vm> void setVmReceivedList(List<T> vmReceivedList) {
		this.vmReceivedList = vmReceivedList;	
	}

	/**
	 * Gets the virtual machine received list.
	 * 
	 * @param <T> the generic type
	 * @return the virtual machine received list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Vm> List<T> getVmReceivedList() {
		return (List<T>) vmReceivedList;
	}
	
	
	/**
	 * Sets the virtual machine submitted list.
	 * 
	 * @param <T> the generic type
	 * @param vmSubmittedList the new virtual machine submitted list
	 */
	protected <T extends Vm> void setVmSubmittedList(List<T> vmSubmittedList) {
		this.vmSubmittedList = vmSubmittedList;	
	}

	/**
	 * Gets the virtual machine submitted list.
	 * 
	 * @param <T> the generic type
	 * @return the virtual machine submitted list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Vm> List<T> getVmSubmittedList() {
		return (List<T>) vmSubmittedList;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.cloudbus.cloudsim.DatacenterBroker#processVmCreate(org.cloudbus.cloudsim.core.SimEvent)
	 */
	@Override
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int result = data[2];

		if (result != CloudSimTags.TRUE) {
			int datacenterId = data[0];
			int vmId = data[1];
			System.out.println(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId
					+ " failed in Datacenter #" + datacenterId);
			System.exit(0);
		}
		processVmCreate2(ev);
	}
	
	
	/**
	 * Submit cloudlets to the created VMs.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void submitCloudlets() {
		int vmIndex = 0;
		for (Cloudlet cloudlet : getCloudletList()) {
			Vm vm;
			// if user didn't bind this cloudlet and it has not been executed yet
			if (cloudlet.getVmId() == -1) {
				vm = getVmsCreatedList().get(vmIndex);
			} else { // submit to the specific vm
				vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
				if (vm == null) { // vm was not created
					Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
							+ cloudlet.getCloudletId() + ": bount VM not available");
					continue;
				}
			}

			Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
					+ cloudlet.getCloudletId() + " to VM #" + vm.getId());
			cloudlet.setVmId(vm.getId());
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
			cloudletsSubmitted++;
			vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
			getCloudletSubmittedList().add(cloudlet);
		}

		// remove submitted cloudlets from waiting list
		for (Cloudlet cloudlet : getCloudletSubmittedList()) {
			getCloudletList().remove(cloudlet);
		}
	}
	
	/**
	 * Process the ack received due to a request for VM creation.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	protected void processVmCreate2(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int datacenterId = data[0];
		int vmId = data[1];
		int result = data[2];

		if (result == CloudSimTags.TRUE) {
			getVmsToDatacentersMap().put(vmId, datacenterId);
			getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
			Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vmId
					+ " has been created in Datacenter #" + datacenterId + ", Host #"
					+ VmList.getById(getVmsCreatedList(), vmId).getHost().getId());
		} else {
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId
					+ " failed in Datacenter #" + datacenterId);
		}

		incrementVmsAcks();

		// all the requested VMs have been created
		//判断是否所有的vm都创建成功了。如果是，就开始提交cloudlet。
		if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {
			submitVirtualMachines();
//			send(datacenterId, CloudSim.clock() + CloudSim.getMinTimeBetweenEvents(), MyCloudSimTags.MY_VM_DATACENTER_EVENT);
		} else {
			// all the acks received, but some VMs were not created
			if (getVmsRequested() == getVmsAcks()) {
				// find id of the next datacenter that has not been tried
				for (int nextDatacenterId : getDatacenterIdsList()) {
					if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
						createVmsInDatacenter(nextDatacenterId);
						return;
					}
				}

				// all datacenters already queried
				if (getVmsCreatedList().size() > 0) { // if some vm were created
					submitCloudlets();
				} else { // no vms created. abort
					Log.printLine(CloudSim.clock() + ": " + getName()
							+ ": none of the required VMs could be created. Aborting");
					finishExecution();
				}
			}
		}
	}

	/**
	 * Submit virtual machines to the created
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void submitVirtualMachines() {
//		int vmIndex = 0;
		
		List<? extends MyPowerVm> vmList = getVmList();
		for (MyPowerVm vm : vmList){
			vm.setIndex(0);
			getVmSubmittedList().add(vm);
			vmsSubmitted++;
			sendNow(getVmsToDatacentersMap().get(vm.getId()), MyCloudSimTags.MY_VM_SUBMIT, vm);
			Log.printLine(CloudSim.clock() + ": " + getName() + ": submitting vm #" + vm.getId());
		}
		
//		for (Cloudlet cloudlet : getCloudletList()) {
//			Vm vm;
//			// if user didn't bind this cloudlet and it has not been executed yet
//			if (cloudlet.getVmId() == -1) {
//				vm = getVmsCreatedList().get(vmIndex);
//			} else { // submit to the specific vm
//				vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
//				if (vm == null) { // vm was not created
//					Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
//							+ cloudlet.getCloudletId() + ": bount VM not available");
//					continue;
//				}
//			}
//
//			Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
//					+ cloudlet.getCloudletId() + " to VM #" + vm.getId());
//			cloudlet.setVmId(vm.getId());
//			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
//			cloudletsSubmitted++;
//			vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
//			getCloudletSubmittedList().add(cloudlet);
//		}

		// remove submitted virtual machines from waiting list
//		vmList = getVmSubmittedList();
//		for (MyPowerVm vm : vmList) {
//			getVmSubmittedList().remove(vm);
//		}
	}

}
