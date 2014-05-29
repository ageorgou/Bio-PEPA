package uk.ac.ed.inf.biopepa.uncertain;

import java.util.List;

/**
 * A class for a distribution represented as a set of samples. This can be
 * used, for example, for the posterior as obtained from a sampling algorithm.
 * Note that this class is for one-dimensional variables/samples.
 * (Experimental!)
 * 
 * @author s1050238
 *
 */

public class Empirical extends AbstractDistribution {

	private List<Double> samples;
	
	public Empirical(List<Double> samples) {
		this.samples = samples;
	}
	
	@Override
	public double pdf(double x) {
		// TODO Auto-generated method stub
		// perhaps some sort of kernel density estimator?
		// this will be the tricky part of implementing this
		return 0;
	}

	@Override
	public double sample() {
		// Just choose one of the samples uniformly 
		int i = AbstractDistribution.random.nextInt(samples.size());
		return samples.get(i);
	}

	@Override
	protected void updateState(double x) {
		// do nothing

	}

}
