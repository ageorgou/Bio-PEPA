package uk.ac.ed.inf.biopepa.uncertain;

import java.util.HashMap;
import java.util.Map;

public class PFParameters {

	//private double stopTime;
	private int N; // number of particles for each approximation
	private Map<String,AbstractDistribution> priors;
	private double jitter; //variance of perturbation when choosing an existing particle (assumed same for all params)
	
	private double obsNoise; //variance of the measurement error (used in acceptance probability) (assumed same for all species)
	
	public PFParameters(int N, Map<String,AbstractDistribution> priors,
			double jitter, double obsNoise) {
		//this.stopTime = stopTime;
		this.N = N;
		this.priors = new HashMap<String,AbstractDistribution>(priors);
		this.jitter = jitter;
		this.obsNoise = obsNoise;
		return;
	}


	public PFParameters() {
		
	}
	
	public void setPriors(Map<String,AbstractDistribution> priors) { this.priors = priors; }
	//public void setStopTime(double stopTime) { this.stopTime = stopTime; }
	public void setN(int N) { this.N = N; }
	public void setJitter(double jitter) { this.jitter = jitter; }
	public void setObsNoise(double obsNoise) { this.obsNoise = obsNoise; }
	
	public Map<String,AbstractDistribution> getPriors() { return priors; }
	//public double getStopTime() { return stopTime; }
	public int getN() { return N; }
	public double getJitter() { return jitter; }
	public double getObsNoise() { return obsNoise; }
	
	
}
