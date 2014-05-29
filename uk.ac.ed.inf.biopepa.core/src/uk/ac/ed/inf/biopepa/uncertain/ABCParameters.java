package uk.ac.ed.inf.biopepa.uncertain;

import java.util.HashMap;
import java.util.Map;

public class ABCParameters {

	private double tolerance;
	private double stopTime;
	private int N; // number of samples to take
	private Map<String,AbstractDistribution> priors;
	private Map<String,AbstractDistribution> proposals;
	
	public ABCParameters(double tolerance, double stopTime, int N, Map<String,AbstractDistribution> priors,
			Map<String,AbstractDistribution> proposals) {
		this.tolerance = tolerance;
		this.stopTime = stopTime;
		this.N = N;
		this.priors = new HashMap<String,AbstractDistribution>(priors);
		this.proposals = new HashMap<String,AbstractDistribution>(proposals);
		return;
	}
	
	public ABCParameters() {
		
	}
	
	public void setPriors(Map<String,AbstractDistribution> priors) { this.priors = priors; }
	public void setProposals(Map<String,AbstractDistribution> proposals) { this.proposals = proposals; }
	public void setTolerance(double tolerance) { this.tolerance = tolerance; }
	public void setStopTime(double stopTime) { this.stopTime = stopTime; }
	public void setN(int N) { this.N = N; }
	
	public Map<String,AbstractDistribution> getPriors() { return priors; }
	public Map<String,AbstractDistribution> getProposals() { return proposals; }
	public double getTolerance() { return tolerance; }
	public double getStopTime() { return stopTime; }
	public int getN() { return N; }
	
}
