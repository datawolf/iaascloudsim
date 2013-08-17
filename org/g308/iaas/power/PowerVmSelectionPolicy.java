package org.g308.iaas.power;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Vm;
import org.g308.iaas.MyPowerHost;
import org.g308.iaas.MyPowerVm;


public abstract class PowerVmSelectionPolicy {

	//从过载的物理主机上选择一个或多个需要迁移的虚拟机

	/**
	 * Gets the vms to migrate.
	 * 
	 * @param host the host
	 * @return the vms to migrate
	 */
	public abstract Vm getVmToMigrate(MyPowerHost host);

	/**
	 * Gets the migratable vms.
	 * 找到主机上可以迁移的虚拟机列表
	 * @param host the host
	 * @return the migratable vms
	 */
	protected List<MyPowerVm> getMigratableVms(MyPowerHost host) {
		List<MyPowerVm> migratableVms = new ArrayList<MyPowerVm>();
		for (MyPowerVm vm : host.<MyPowerVm> getVmList()) {
			if (!vm.isInMigration()) {
				migratableVms.add(vm);
			}
		}
		return migratableVms;
	}

}
