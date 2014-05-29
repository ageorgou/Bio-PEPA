package uk.ac.ed.inf.biopepa.uncertain;

import java.util.HashMap;
import java.util.Map;

import uk.ac.ed.inf.biopepa.core.compiler.CompiledExpressionRateEvaluator;
import uk.ac.ed.inf.biopepa.core.interfaces.Result;
import uk.ac.ed.inf.biopepa.core.sba.SBAModel;
import uk.ac.ed.inf.biopepa.core.sba.SBAReaction;

public class SSESolver {

	SBAModel sbaModel;
	int nComp;
	Result observations;
	
	public SSESolver(SBAModel sbaModel, Result observations) {
		this.sbaModel = sbaModel;
		this.nComp = sbaModel.getComponentCount();
		this.observations = observations;
	}
	
	/**
	 * Get the changes to the state when a reaction occurs. This is a vector of integers,
	 * showing the jump/change for each of the components. A change is negative if a
	 * component is consumed, positive if produced, or zero if it is unaffected by the
	 * reaction (overall).
	 * @param r the SBAReaction in question
	 * @return an array of int holding the change for each component
	 */
	private int[] getUpdates(SBAReaction r) {
		int[] v = new int[nComp];
		for (int i = 0; i < nComp; i++)
			v[i] = r.netAffect(sbaModel.getComponentNames()[i]);
		
		return v;
	}
	
	private double calculateRate(SBAReaction r, double[] state) {
		String[] components = sbaModel.getComponentNames(); 
		Map<String,Number> counts = new HashMap<String,Number>();
		
		for (int i = 0; i < nComp; i++)
			counts.put(components[i], state[i]);
		
		CompiledExpressionRateEvaluator rv = //we are setting time = 0 (as it is unused)
			new CompiledExpressionRateEvaluator(sbaModel,counts,0,r);
				
		return rv.getResult();
	}
	
	private double[] getMacroRHS(double[] state) {
		double drift[] = new double[state.length];
		for (int i = 0; i < nComp; i++)
			drift[i] = 0;
		
		for (SBAReaction r : sbaModel.getReactions()) {
			double rate = calculateRate(r,state);
			int[] jump = getUpdates(r);
			for (int i = 0; i < nComp; i++) {
				drift[i] += rate * jump[i];
			}
		}
		
		return drift;
	}
	
	// Ruttor & Opper eq. 6
	private double[][] calculateD(double[] state) {
		
		double[][] D = new double[nComp][nComp];
		for (int i = 0; i < nComp; i++)
			for (int j = 0; j < nComp; j++)
			D[i][j] = 0;
		
		for (SBAReaction r : sbaModel.getReactions()) {
			double rate = calculateRate(r,state);
			int[] jump = getUpdates(r);
			for (int i = 0; i < nComp; i++)
				for (int j = 0; j < nComp; j++){
					D[i][j] += rate * jump[i] * jump[j];
			}
		}
		
		return D;
	}

}
