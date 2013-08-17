package org.cloudbus.cloudsim.distributions;

public class MyTestForRandom {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		指数分布
//		ExponentialDistr d = new ExponentialDistr(1);
//		伽玛分布
//		GammaDistr d = new GammaDistr(1, 2.0);
//		对数正态分布
//		LognormalDistr d = new LognormalDistr(2.0, 2.0);
//		均匀分布
//		UniformDistr d = new UniformDistr(2.0, 8.0);
//		帕累托
//		ParetoDistr d = new ParetoDistr(2.0, 2.0);
//		洛马克斯分布
//		LomaxDistribution d = new LomaxDistribution(2.0, 2.0, 1.0);
//		威布尔
//		WeibullDistr d = new WeibullDistr(2.0, 2.0);
//		齐普夫
		ZipfDistr d = new ZipfDistr(2.0, 2);
		int		i = 10;
		double	rd;
		while (i-- > 0)
		{
			rd = d.sample();
			System.out.println(rd);
		}
	}

}
