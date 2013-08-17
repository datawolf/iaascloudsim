package org.g308.iaas;

import java.util.List;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.util.MathUtil;

public class PowerHostUtilizationHistory extends MyPowerHost {

	public PowerHostUtilizationHistory(
			int id, 
			RamProvisioner ramProvisioner,
			BwProvisioner bwProvisioner, 
			long storage,
			List<? extends Pe> peList, 
			VmScheduler vmScheduler,
			PowerModel powerModel) {
		super(
				id, 
				ramProvisioner, 
				bwProvisioner, 
				storage, 
				peList, 
				vmScheduler,
				powerModel);
		// TODO Auto-generated constructor stub
		
	}
	
	/**
	 * Gets the host utilization history.
	 * 主机的历史信息，不超过20个历史数据
	 * 
	 * @return the host utilization history
	 */
	public double[] getUtilizationHistory() {
		double[] utilizationHistory = new double[MyPowerVm.HISTORY_LENGTH];
		double hostMips = getTotalMips();
		for (MyPowerVm vm : this.<MyPowerVm> getVmList()) {
			for (int i = 0; i < vm.getUtilizationHistory().size(); i++) {
				utilizationHistory[i] += vm.getUtilizationHistory().get(i) * vm.getMips() / hostMips;
			}
		}
		return MathUtil.trimZeroTail(utilizationHistory);
	}

}
