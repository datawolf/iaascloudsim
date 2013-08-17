package org.g308.iaas.examples.planetlab;


public class Dvfs {

	/**
	 * The main method
	 * @param args  the argument
	 */
	public static void main(String[] args) {
		boolean enableOutput = true;
		boolean outputToFile = true;
		String inputFolder = Dvfs.class.getClassLoader().getResource("workload/planetlab").getPath();
		String outputFolder = "output";
		String workload = "20110303"; // PlanetLab workload
		String vmAllocationPolicy = "dvfs"; // DVFS policy without VM migrations
		String vmSelectionPolicy = "";
		String parameter = "";

		new PlanetLabRunner(
				enableOutput,
				outputToFile,
				inputFolder,
				outputFolder,
				workload,
				vmAllocationPolicy,
				vmSelectionPolicy,
				parameter);
	}
}
