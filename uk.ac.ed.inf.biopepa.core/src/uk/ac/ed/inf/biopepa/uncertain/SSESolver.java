package uk.ac.ed.inf.biopepa.uncertain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ode.ContinuousOutputModel;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.events.EventHandler;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;

import uk.ac.ed.inf.biopepa.core.compiler.CompiledExpressionRateEvaluator;
import uk.ac.ed.inf.biopepa.core.interfaces.Result;
import uk.ac.ed.inf.biopepa.core.sba.SBAModel;
import uk.ac.ed.inf.biopepa.core.sba.SBAReaction;

public class SSESolver {

	SBAModel sbaModel;
	int nComp;
	int nReact;
	Result observations;
	double obsNoise;
	Map<SBAReaction,int[]> updates;
	private ContinuousOutputModel output;
	private DiffProvider dp;
		
	public SSESolver(SBAModel sbaModel, Result observations, double obsNoise) {
		this.sbaModel = sbaModel;
		this.nComp = sbaModel.getComponentCount();
		this.nReact = sbaModel.getReactions().length;
		this.observations = observations;
		this.obsNoise = obsNoise;
		
		//store the updates for each reaction (to avoid recalculating them every time)
		updates = new HashMap<SBAReaction,int[]>();
		for (SBAReaction r : sbaModel.getReactions()) {
			int[] v = new int[nComp];
			for (int i = 0; i < nComp; i++)
				v[i] = r.netAffect(sbaModel.getComponentNames()[i]);
			updates.put(r, v);
		}
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
		return updates.get(r);
	}
	
	/**
	 * Get the rate of a reaction, given the state of the system.
	 * @param r the SBAReaction in question
	 * @param state an array holding the counts of each component
	 * @return the rate of the reaction as a double
	 */
	private double calculateRate(SBAReaction r, double[] state) {
		String[] components = sbaModel.getComponentNames(); 
		Map<String,Number> counts = new HashMap<String,Number>();
		
		for (int i = 0; i < nComp; i++)
			counts.put(components[i], state[i]);
		
		CompiledExpressionRateEvaluator rv = //we are setting time = 0 (as it is unused)
			new CompiledExpressionRateEvaluator(sbaModel,counts,0,r);
		r.getRate().accept(rv);		
		return rv.getResult();
	}
	
	// Ruttor & Opper eq. 5, 7
	private double[] getMacroRHS(double[] state) {

		if (state.length != nComp) {
			throw new IllegalArgumentException("The size of the state does not match the number of components.");
		}
		
		double drift[] = new double[state.length];
		Arrays.fill(drift, 0);
				
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
			Arrays.fill(D[i], 0);
		
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
	
	// Ruttor & Opper eq. 10
	private double[][] getVarRHS(double[] mean, double[][] var, boolean forward) {
		
		double[][] arrayA = new double[nComp][nComp];
		for (int i = 0; i < nComp; i++)
			Arrays.fill(arrayA[i], 0);
		dp.setCounts(state2map(mean));
		for (SBAReaction r : sbaModel.getReactions()) {
			int[] jump = getUpdates(r);
			for (int i = 0; i < nComp; i++) {
				int j = 0;
				for (String comp_j : sbaModel.getComponentNames()) {
					arrayA[i][j] += jump[i] * dp.getRate(r.getName(), comp_j); 
					j++;
				}
			}
		}
		RealMatrix A = new Array2DRowRealMatrix(arrayA);
		RealMatrix S = new Array2DRowRealMatrix(var);
		RealMatrix D = new Array2DRowRealMatrix(calculateD(mean));
		RealMatrix RHS;
		if (forward)
			RHS = A.multiply(S).add(S.multiply(A.transpose())).add(D);
		else
			RHS = A.multiply(S).add(S.multiply(A.transpose())).subtract(D);
		//RHS = RHS.add(diagonalMatrix(nComp,10e-6));
		//from RealMatrix back to array...
		double[][] rhsArray = RHS.getData();
		return rhsArray;
	}
	
	private double[] getInitialState() {
		double[] state = new double[nComp];
		int i = 0;
		for (String comp : sbaModel.getComponentNames())
			state[i++] = sbaModel.getNamedComponentCount(comp);
		return state;
	}

	private Map<String,Number> state2map(double[] state) {
		Map<String,Number> map = new HashMap<String,Number>();
		String[] compNames = sbaModel.getComponentNames();
		
		for (int i = 0; i < nComp; i++)
			map.put(compNames[i], state[i]);
		return map;
	}
	
	public void solveMacroForward(double tFinal) {
		MacroODE ode = new MacroODE(true);
		FirstOrderIntegrator solv = new ClassicalRungeKuttaIntegrator(0.001);
		// add output model
		output = new ContinuousOutputModel();
		solv.addStepHandler(output);

		// add event handlers for each observation point (beyond the first)
		if (observations != null)
			for (int i = 1; i < observations.getTimePoints().length; i++) {
				double[] observedState = new double[nComp];
				for (int n = 0; n < nComp; n++)
					observedState[n] = observations.getTimeSeries(n)[i];
				ForwardUpdate u =
					new ForwardUpdate(observations.getTimePoints()[i],observedState);
				solv.addEventHandler(u, solv.getCurrentSignedStepsize(), 0.001, 20);
			}
		double[] y = new double[nComp + nComp*nComp];
		
		//initialize the "provider"
		Map<String,Number> initCounts = new HashMap<String,Number>();
		for (String name : sbaModel.getComponentNames())
			initCounts.put(name, sbaModel.getNamedComponentCount(name));
		dp = new DiffProvider(sbaModel,initCounts);
		
		//build initial conditions (mean to true state, variance to 0)
		double[] initConditions = new double[nComp + nComp*nComp];
		Arrays.fill(initConditions, 0);
		System.arraycopy(getInitialState(), 0, initConditions, 0, nComp);
		// TEST set initial variance to diagonal noise
		for (int i = 0; i < nComp; i++)
			initConditions[nComp + (nComp+1)*i] = obsNoise;
		
		solv.integrate(ode, 0, initConditions, tFinal, y);
	}

	//version with handlers commented out
	/*
	public void solveMacroBackward(double tFinal) {
		MacroODE ode = new MacroODE(false);
		FirstOrderIntegrator solv = new ClassicalRungeKuttaIntegrator(0.001);
		// add output model
		output = new ContinuousOutputModel();
		solv.addStepHandler(output);

		// add event handlers for each observation point (beyond the first)
		if (observations != null)
			for (int i = 1; i < observations.getTimePoints().length; i++) {
				double[] observedState = new double[nComp];
				for (int n = 0; n < nComp; n++)
					observedState[n] = observations.getTimeSeries(n)[i];
				BackwardUpdate u =
					new BackwardUpdate(observations.getTimePoints()[i],observedState);
				solv.addEventHandler(u, solv.getCurrentSignedStepsize(), 0.001, 20);
			}
		double[] y = new double[nComp + nComp*nComp];
		
		//initialize the "provider"
		int nObs = observations.getTimePoints().length;
		double[] lastObservation = new double[nComp];
		for (int i = 0; i < nComp; i++)
			lastObservation[i] = observations.getTimeSeries(i)[nObs-1];		
		Map<String,Number> finalCounts = state2map(lastObservation);
		dp = new ScaledDiffProvider(sbaModel,finalCounts);
		
		//build initial conditions (mean = last observation, variance = noise)
		double[] finalConditions = new double[nComp + nComp*nComp];
		Arrays.fill(finalConditions, 0);
		System.arraycopy(lastObservation, 0, finalConditions, 0, nComp);
		for (int i = 0; i < nComp; i++) //only diagonal elements
			finalConditions[nComp + (nComp+1)*i] = obsNoise;
		
		solv.integrate(ode, tFinal, finalConditions, 0, y);
	}
	*/
	
	//solve backward equation in steps 
	public void solveMacroBackward(double tFinal) {
		MacroODE ode = new MacroODE(false);
		FirstOrderIntegrator solv = new ClassicalRungeKuttaIntegrator(0.0001);
		// (re)set output model
		output = new ContinuousOutputModel();
		
		double[] y = new double[nComp + nComp*nComp];
		
		//initialize the "provider"
		int nObs = observations.getTimePoints().length;
		double[] lastObservation = new double[nComp];
		for (int i = 0; i < nComp; i++)
			lastObservation[i] = observations.getTimeSeries(i)[nObs-1];		
		dp = new ScaledDiffProvider(sbaModel,state2map(lastObservation));
		
		//build initial conditions (mean = last observation, variance = noise)
		double[] finalConditions = new double[nComp + nComp*nComp];
		Arrays.fill(finalConditions, 0);
		System.arraycopy(lastObservation, 0, finalConditions, 0, nComp);
		for (int i = 0; i < nComp; i++) //only diagonal elements
			finalConditions[nComp + (nComp+1)*i] = obsNoise;

		
		double tStart = tFinal; //start from final time
		for (int i = nObs-1; i > 0; i--) {
			double tTarget = observations.getTimePoints()[i-1];
			// add a new output model
			ContinuousOutputModel newOutput = new ContinuousOutputModel();
			solv.clearStepHandlers();
			solv.addStepHandler(newOutput);
			//solve the equation backwards until the previous observation point
			solv.integrate(ode, tStart, finalConditions, tTarget, y);
			//update solution by combining with observation
			double[] observation = new double[nComp];
			for (int j = 0; j < nComp; j++)
				observation[j] = observations.getTimeSeries(j)[i-1];	
			combine(y,observation);
			//combine results so far and set up the next interval
			output.append(newOutput);
			tStart = tTarget;
			System.arraycopy(y, 0, finalConditions, 0, nComp + nComp*nComp);
		}
	}
	
	public void combine(double[] solution, double[] observation) {
		
		double[] oldMean = Arrays.copyOf(solution, nComp);
		double[][] oldVarArray = make2D(solution,nComp,nComp);

		RealMatrix oldVar, oldVarInv, noise, noiseInv, newVar;
		oldVar = new Array2DRowRealMatrix(oldVarArray);
		noise = diagonalMatrix(nComp,obsNoise);
		oldVarInv = new LUDecomposition(oldVar).getSolver().getInverse();
		noiseInv = new LUDecomposition(noise).getSolver().getInverse();
		newVar = new LUDecomposition(oldVarInv.add(noiseInv)).getSolver().getInverse();
		//using the matrix inversion lemma:
		//inv(inv(A) + inv(B)) = B - B*inv(A+B)*B
		//so perhaps this can be simplified

		double[] newMean, term1, term2;
		term1 = oldVarInv.preMultiply(oldMean);
		term2 = noiseInv.preMultiply(observation);
		for (int i = 0; i < nComp; i++)
			term2[i] += term1[i];
		newMean = newVar.preMultiply(term2);

		// "unpack" newMean and newVar back into solution
		for (int k = 0; k < nComp; k++)
			solution[k] = newMean[k];

		double[][] newVarArray = newVar.getData();
		int k = nComp; //offset
		for (int i = 0; i < nComp; i++)
			for (int j = 0; j < nComp; j++)
				solution[k++] = newVarArray[i][j];
		
		return;

	}
	
	public double[] getSolutionAtTime(double t) {
		output.setInterpolatedTime(t);
		return output.getInterpolatedState();
	}
	
	/**
	 * Create a square matrix with the specified value in its diagonal and zero
	 * elements otherwise (i.e. a multiple of the identity matrix).
	 * @param dim the number of rows (equally, the number of columns) of the matrix
	 * @param diag the value on the diagonal
	 * @return a RealMatrix with 0 everywhere except the diagonal.
	 */
	private RealMatrix diagonalMatrix(int dim, double diag) {
		double[][] array = new double[dim][dim];
		for (int i = 0; i < dim; i++)
			for (int j = 0; j < dim; j++)
				if (i != j)
					array[i][j] = 0;
				else
					array[i][j] = diag;
		
		return new Array2DRowRealMatrix(array);
	}
	
	private double[][] make2D(double[] array, int dim) {
		return make2D(array,0,dim);
	}
	
	private double[][] make2D(double[] array, int offset, int dim) {
		if (array.length - offset != dim * dim)
			throw new IllegalArgumentException("Dimension mismatch");
		
		double[][] array2D = new double[dim][dim];
		for (int i = 0, n = offset; i < dim; i++)
			for (int j = 0; j < dim; j++)
				array2D[i][j] = array[n++];
//		int i = 0, j = 0;
//		for (double d : Arrays.copyOfRange(array,offset,array.length)) {
//			array2D[i][j++] = d;
//			if (j == dim) {	j = 0; i++; }
//		}
		return array2D;
	}
	
	private class MacroODE implements FirstOrderDifferentialEquations {
		
		private boolean forward;
		
		public int getDimension() { return nComp + nComp*nComp; }
		
		public MacroODE(boolean forward) {
			this.forward = forward;
		}
		
		public void computeDerivatives(double t, double[] y, double[] ydot)
				throws MaxCountExceededException, DimensionMismatchException {
			//split variable vector into mean and variance
			double[] mean = Arrays.copyOf(y, nComp);
			double[][] var = new double[nComp][nComp];
			for (int i = 0; i < nComp; i++)
				for (int j = 0; j < nComp; j++)
					var[i][j] = y[nComp + (i*nComp) + j];
			
			double[] meanRHS = getMacroRHS(mean);
			for (int k = 0; k < nComp; k++)
				ydot[k] = meanRHS[k];
								
			double[][] varRHS = getVarRHS(mean,var,forward);
			int k = nComp; //offset
			for (int i = 0; i < nComp; i++)
				for (int j = 0; j < nComp; j++)
					ydot[k++] = varRHS[i][j];

			return;
		}
		
		
	}
	
	private class ForwardUpdate implements EventHandler {
		private double observationTime;
		private double[] observation;
		int nComp;
		
		public ForwardUpdate(double observationTime, double[] observation) {
			this.observationTime = observationTime;
			this.observation = observation;
			this.nComp = observation.length;
		}
		public void init(double t0, double[] y0, double t) {
			//do nothing
		}

		public Action eventOccurred(double t, double[] y, boolean ydot) {
			return Action.RESET_STATE;
		}

		public double g(double t, double[] y) {
			return t - observationTime;
		}

		public void resetState(double t, double[] y) {
			// TODO update the mean and variance after the observation...
			
			double[] oldMean = Arrays.copyOf(y, nComp);
			double[][] oldVarArray = make2D(y,nComp,nComp);
			
			RealMatrix oldVar, oldVarInv, noise, noiseInv, newVar;
			oldVar = new Array2DRowRealMatrix(oldVarArray);
			noise = diagonalMatrix(nComp,obsNoise);
			oldVarInv = new LUDecomposition(oldVar).getSolver().getInverse();
			noiseInv = new LUDecomposition(noise).getSolver().getInverse();
			newVar = new LUDecomposition(oldVarInv.add(noiseInv)).getSolver().getInverse();
			//using the matrix inversion lemma:
			//inv(inv(A) + inv(B)) = B - B*inv(A+B)*B
			//so perhaps this can be simplified
			
			double[] newMean, term1, term2;
			term1 = oldVarInv.preMultiply(oldMean);
			term2 = noiseInv.preMultiply(observation);
			for (int i = 0; i < nComp; i++)
				term2[i] += term1[i];
			newMean = newVar.preMultiply(term2);
			
			// "unpack" newMean and newVar back into arg1
			for (int k = 0; k < nComp; k++)
				y[k] = newMean[k];
								
			double[][] newVarArray = newVar.getData();
			int k = nComp; //offset
			for (int i = 0; i < nComp; i++)
				for (int j = 0; j < nComp; j++)
					y[k++] = newVarArray[i][j];

			return;

		}

	}
	
	private class BackwardUpdate implements EventHandler {
		private double observationTime;
		private double[] observation;
		int nComp;

		public BackwardUpdate(double observationTime, double[] observation) {
			this.observationTime = observationTime;
			this.observation = observation;
			this.nComp = observation.length;
		}
		public void init(double t0, double[] y0, double t) {
			//do nothing
		}

		public Action eventOccurred(double t, double[] y, boolean ydot) {
			return Action.RESET_STATE;
		}

		public double g(double t, double[] y) {
			return t - observationTime;
		}

		public void resetState(double t, double[] y) {

			double[] oldMean = Arrays.copyOf(y, nComp);
			double[][] oldVarArray = make2D(y,nComp,nComp);

			RealMatrix oldVar, oldVarInv, noise, noiseInv, newVar;
			oldVar = new Array2DRowRealMatrix(oldVarArray);
			noise = diagonalMatrix(nComp,obsNoise);
			oldVarInv = new LUDecomposition(oldVar).getSolver().getInverse();
			noiseInv = new LUDecomposition(noise).getSolver().getInverse();
			newVar = new LUDecomposition(oldVarInv.add(noiseInv)).getSolver().getInverse();
			//using the matrix inversion lemma:
			//inv(inv(A) + inv(B)) = B - B*inv(A+B)*B
			//so perhaps this can be simplified

			double[] newMean, term1, term2;
			term1 = oldVarInv.preMultiply(oldMean);
			term2 = noiseInv.preMultiply(observation);
			for (int i = 0; i < nComp; i++)
				term2[i] += term1[i];
			newMean = newVar.preMultiply(term2);

			// "unpack" newMean and newVar back into arg1
			for (int k = 0; k < nComp; k++)
				y[k] = newMean[k];

			double[][] newVarArray = newVar.getData();
			int k = nComp; //offset
			for (int i = 0; i < nComp; i++)
				for (int j = 0; j < nComp; j++)
					y[k++] = newVarArray[i][j];
			
			return;

		}

	}
	
}
