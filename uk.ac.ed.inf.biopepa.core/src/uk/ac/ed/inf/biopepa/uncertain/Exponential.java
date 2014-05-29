package uk.ac.ed.inf.biopepa.uncertain;

public class Exponential extends AbstractDistribution {

	private double lambda;
	
	public Exponential(double lambda) {
		this.lambda = lambda;
	}
	
	@Override
	public double pdf(double x) {
		return lambda * Math.exp(-lambda*x);
	}

	@Override
	public double sample() {
		// if u follows Uniform(0,1), then (-ln(u)/lambda) follows Exp(lambda)
		double u = AbstractDistribution.random.nextDouble();
		return (-Math.log(u) / lambda);
	}

	@Override
	protected void updateState(double x) {
		//do nothing
	}

}
