package org.g308.iaas.power;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.util.MathUtil;
import org.g308.iaas.MyPowerHost;
import org.g308.iaas.PowerHostUtilizationHistory;

public class PowerVmAllocationPolicyMigrationLocalRegression extends PowerVmAllocationPolicyMigrationAbstract {

	/** The scheduling interval. */
	private double schedulingInterval;

	/** The safety parameter. */
	private double safetyParameter;

	/** The fallback vm allocation policy. */
	private PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy;

	/**
	 * Instantiates a new power vm allocation policy migration local regression.
	 * 
	 * @param hostList the host list
	 * @param vmSelectionPolicy the vm selection policy
	 * @param schedulingInterval the scheduling interval
	 * @param fallbackVmAllocationPolicy the fallback vm allocation policy
	 * @param utilizationThreshold the utilization threshold
	 */
	public PowerVmAllocationPolicyMigrationLocalRegression(
			List<? extends Host> hostList,
			PowerVmSelectionPolicy vmSelectionPolicy,
			double safetyParameter,
			double schedulingInterval,
			PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy,
			double utilizationThreshold) {//没有用到这个
		super(hostList, vmSelectionPolicy);
		setSafetyParameter(safetyParameter);
		setSchedulingInterval(schedulingInterval);
		setFallbackVmAllocationPolicy(fallbackVmAllocationPolicy);
	}

	/**
	 * Instantiates a new power vm allocation policy migration local regression.
	 * 
	 * @param hostList the host list
	 * @param vmSelectionPolicy the vm selection policy
	 * @param schedulingInterval the scheduling interval
	 * @param fallbackVmAllocationPolicy the fallback vm allocation policy
	 */
	public PowerVmAllocationPolicyMigrationLocalRegression(
			List<? extends Host> hostList,
			PowerVmSelectionPolicy vmSelectionPolicy,
			double safetyParameter,
			double schedulingInterval,
			PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
		super(hostList, vmSelectionPolicy);
		setSafetyParameter(safetyParameter);
		setSchedulingInterval(schedulingInterval);
		setFallbackVmAllocationPolicy(fallbackVmAllocationPolicy);
	}

	/**
	 * Checks if is host over utilized.
	 * 
	 * @param host the host
	 * @return true, if is host over utilized
	 */
	@Override
	protected boolean isHostOverUtilized(MyPowerHost host) {
		PowerHostUtilizationHistory _host = (PowerHostUtilizationHistory) host;
		double[] utilizationHistory = _host.getUtilizationHistory();
		
		//使用了最近的10个历史数据，当数据量不足10个时，使用fallbackVmAllocationPolicy终端判断过载的策略
		int length = 10; // we use 10 to make the regression responsive enough to latest values
		if (utilizationHistory.length < length) {
			return getFallbackVmAllocationPolicy().isHostOverUtilized(host);
		}
		
		
		//寻找最近的10个数据
		double[] utilizationHistoryReversed = new double[length];
		for (int i = 0; i < length; i++) {
			utilizationHistoryReversed[i] = utilizationHistory[length - i - 1];
		}
		
		double[] estimates = null;
		try {
			//获得估计的值
			estimates = getParameterEstimates(utilizationHistoryReversed);
		} catch (IllegalArgumentException e) {
			return getFallbackVmAllocationPolicy().isHostOverUtilized(host);
		}
		
		
		//这部分应该看看论文里面的描述
		double migrationIntervals = Math.ceil(getMaximumVmMigrationTime(_host) / getSchedulingInterval());
		double predictedUtilization = estimates[0] + estimates[1] * (length + migrationIntervals);
		predictedUtilization *= getSafetyParameter();

		addHistoryEntry(host, predictedUtilization);

		return predictedUtilization >= 1;
	}

	/**
	 * Gets the parameter estimates.
	 * 获得新的 cpu上限阈值
	 * @param utilizationHistoryReversed the utilization history reversed
	 * @return the parameter estimates
	 */
	protected double[] getParameterEstimates(double[] utilizationHistoryReversed) {
		return MathUtil.getLoessParameterEstimates(utilizationHistoryReversed);
	}

	/**
	 * Gets the maximum vm migration time.
	 * 
	 * @param host the host
	 * @return the maximum vm migration time
	 */
	protected double getMaximumVmMigrationTime(MyPowerHost host) {
		int maxRam = Integer.MIN_VALUE;
		for (Vm vm : host.getVmList()) {
			int ram = vm.getRam();
			if (ram > maxRam) {
				maxRam = ram;
			}
		}
		//其实就是找内存最大的虚拟机，迁移时间为什么还要除以 2 * 8000
		return maxRam / ((double) host.getBw() / (2 * 8000));
	}

	/**
	 * Sets the scheduling interval.
	 * 
	 * @param schedulingInterval the new scheduling interval
	 */
	protected void setSchedulingInterval(double schedulingInterval) {
		this.schedulingInterval = schedulingInterval;
	}

	/**
	 * Gets the scheduling interval.
	 * 
	 * @return the scheduling interval
	 */
	protected double getSchedulingInterval() {
		return schedulingInterval;
	}

	/**
	 * Sets the fallback vm allocation policy.
	 * 
	 * @param fallbackVmAllocationPolicy the new fallback vm allocation policy
	 */
	public void setFallbackVmAllocationPolicy(
			PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
		this.fallbackVmAllocationPolicy = fallbackVmAllocationPolicy;
	}

	/**
	 * Gets the fallback vm allocation policy.
	 * 
	 * @return the fallback vm allocation policy
	 */
	public PowerVmAllocationPolicyMigrationAbstract getFallbackVmAllocationPolicy() {
		return fallbackVmAllocationPolicy;
	}

	public double getSafetyParameter() {
		return safetyParameter;
	}

	public void setSafetyParameter(double safetyParameter) {
		this.safetyParameter = safetyParameter;
	}

	
}
