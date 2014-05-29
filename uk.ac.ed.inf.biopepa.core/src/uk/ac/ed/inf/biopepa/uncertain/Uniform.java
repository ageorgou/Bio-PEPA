package uk.ac.ed.inf.biopepa.uncertain;

public class Uniform extends AbstractDistribution {

	private double low;
	private double high;
	private double pdfFixed;
	
	public Uniform(double low, double high) {
		this.low = low;
		this.high = high;
		this.pdfFixed = 1 / (high-low);
	}
	
	public double pdf(double x) {
		if (x >= low && x <= high)
			return pdfFixed;
		else //this should never be the case (except for FixedUniform?), but let's
			//return Double.MIN_VALUE; //return this instead of 0 to avoid exceptions?
			return 0;
	}

	public double sample() {
		return low + (high-low)*AbstractDistribution.random.nextDouble();
	}

	protected void updateState(double x) {
		double shift = x - (high-low)/2 ;
		low = low + shift;
		high = high + shift;
	}

}
