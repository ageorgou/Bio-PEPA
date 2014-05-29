package uk.ac.ed.inf.biopepa.uncertain;

//import java.util.Random;



public class Gaussian extends AbstractDistribution {

	private static final double Z = Math.sqrt(2*Math.PI); 
	
	private double mu;
	private double s;
	//private Random randgen;

	/**
	 * A Gaussian distribution.
	 * @param mu The mean of the distribution
	 * @param s The standard deviation of the distribution
	 */
	public Gaussian(double mu, double s) {
		this.mu = mu;
		this.s = s;
		//randgen = new Random();
	}
	
	public double pdf(double x) {
		double exp = Math.exp(-(x-mu)*(x-mu)/(2*s*s));
		return exp / (s*Z);
	}
	
	public double sample() {
		//double r = randgen.nextGaussian();
		double r = AbstractDistribution.random.nextGaussian(); 
		return mu + s*r;
	}
	
	protected void updateState(double mean) {
		mu = mean;
	}

}
