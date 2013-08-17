package org.g308.iaas.examples.planetlab;

import java.util.Calendar;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.g308.iaas.examples.power.Helper;
import org.g308.iaas.examples.power.RunnerAbstract;

public class PlanetLabRunner extends RunnerAbstract {

	/**
	 * Instantiates a new planet lab runner.
	 * 
	 * @param enableOutput the enable output
	 * @param outputToFile the output file
	 * @param inputFolder the input folder
	 * @param outputFolder the output folder
	 * @param workload the workload
	 * @param vmAllocationPolicy the virtual machine allocation policy
	 * @param vmSelectionPolicy the virtual machine selection policy
	 * @param parameter
	 */
	public PlanetLabRunner(
			boolean enableOutput, 
			boolean outputToFile,
			String inputFolder, 
			String outputFolder, 
			String workload,
			String vmAllocationPolicy, 
			String vmSelectionPolicy,
			String parameter) {
		super(
				enableOutput, 
				outputToFile, 
				inputFolder, 
				outputFolder,
				workload,
				vmAllocationPolicy, 
				vmSelectionPolicy, parameter);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.g308.iaas.examples.power.RunnerAbstract#inits(java.lang.String)
	 */
	@Override
	protected void init(String inputFolder) {
		try {
			CloudSim.init(1, Calendar.getInstance(), true);
//			CloudSim.terminateSimulation(3600*12);

			broker = Helper.createBroker();
			int brokerId = broker.getId();

//			cloudletList = PlanetLabHelper.createCloudletListPlanetLab(brokerId, inputFolder);
//			vmList = Helper.createVmList(brokerId, cloudletList.size());
			vmList = Helper.createVmPlanetLab(brokerId, inputFolder);
//			vmList = Helper.createVmList(brokerId, 600);
			hostList = Helper.createHostList(PlanetLabConstants.NUMBER_OF_HOSTS);
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
			System.exit(0);
		}
		
	}
	
	

}
