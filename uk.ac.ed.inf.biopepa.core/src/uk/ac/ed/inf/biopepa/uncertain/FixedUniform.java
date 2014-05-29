package uk.ac.ed.inf.biopepa.uncertain;

/**
 *A class to represent a uniform distribution whose range cannot be changed.
 *
 */

public class FixedUniform extends Uniform {

	public FixedUniform(double low, double high) {
		super(low, high);
	}

	@Override
	protected void updateState(double x) {
		// do nothing
	}
	
}
