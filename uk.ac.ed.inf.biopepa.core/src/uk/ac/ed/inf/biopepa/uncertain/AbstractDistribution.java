package uk.ac.ed.inf.biopepa.uncertain;

import java.util.Random;


public abstract class AbstractDistribution {

	// A random number generator, to be used by subclasses as needed
	protected final static Random random = new Random(); 
	
	public abstract double pdf(double x);
	
	public abstract double sample();
	
	protected abstract void updateState(double x);
	
	public double sample(double x) {
		updateState(x);
		return sample();
	}
	
	public double pdf(double x, double y) {
		updateState(x);
		return pdf(y);
	}
}
