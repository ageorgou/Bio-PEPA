package uk.ac.ed.inf.biopepa.uncertain;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.ed.inf.biopepa.core.BasicResult;
import uk.ac.ed.inf.biopepa.core.BioPEPA;
import uk.ac.ed.inf.biopepa.core.BioPEPAException;
import uk.ac.ed.inf.biopepa.core.compiler.ModelCompiler;
import uk.ac.ed.inf.biopepa.core.compiler.ProblemInfo;
import uk.ac.ed.inf.biopepa.core.dom.Model;
import uk.ac.ed.inf.biopepa.core.dom.internal.ParserException;
import uk.ac.ed.inf.biopepa.core.interfaces.ProgressMonitor;
import uk.ac.ed.inf.biopepa.core.interfaces.Result;
import uk.ac.ed.inf.biopepa.core.interfaces.Solver;
import uk.ac.ed.inf.biopepa.core.sba.ExperimentLine;
import uk.ac.ed.inf.biopepa.core.sba.Parameters;
import uk.ac.ed.inf.biopepa.core.sba.Parameters.Parameter;
import uk.ac.ed.inf.biopepa.core.sba.SBAModel;
import uk.ac.ed.inf.biopepa.core.sba.Solvers;

//(make abstract and) subclass for different distance measures/sampling schemes?
public class ABCSampler {

	
	private Result observation; // the observed time-series
	// samples must be a list (at least not a set), as there may be duplicates
	private List<double[]> samples;
	int dim; // number of model parameters to learn
	String[] names; // names of the parameters (to access priors/proposals)
	private ABCParameters params; // algorithm parameters
	private Model astModel;
	private SBAModel sbaModel;
	private Solver solver; // SSA solver and its associated parameters etc
	private Parameters solverParams;
	private ProgressMonitor monitor;
	private String[] components;
	
	public ABCSampler(Result observation, ABCParameters params) {
		this.observation = observation;
		this.samples = new ArrayList<double[]>();
		setParams(params);
		// we need a monitor for calling the simulation engine, so just use...
		this.monitor = new ProgressMonitor() { // a monitor that does nothing
			public void beginTask(int amount) {	}

			public void done() { }

			public boolean isCanceled() {return false;}

			public void setCanceled(boolean state) { }

			public void worked(int worked) {}
		}; // "inspired" by BioPEPACommandLine.FakeProgressMonitor
	}
	
	public void setParams(ABCParameters params) {
		this.params = params;
		//get the number of parameters...
		dim = params.getPriors().keySet().size();
		//...and assign an ordering to them (alphabetical is robust to multiple calls)
		int i = 0;
		names = new String[params.getPriors().keySet().size()];
		for (String name : params.getPriors().keySet())
			names[i++] = name;
		//or simply?
		//names = (String []) params.getPriors().keySet().toArray();
		Arrays.sort(names);
	}
	
	public List<double[]> getSamples() { return samples; }
	public String[] getNames() { return names; }
	public Result getObservation() { return observation; }
	
	private double distance(Result simulation) {
		//Calculate the distance between two time-series.
		//NOTE! that this changes the simulation result so that the time points
		//coincide with those of the observations.
		//If we want to preserve the original simulation result, we must
		//copy it.
		
		int nSpecies = observation.getComponentNames().length;
		double[][] dist = new double[nSpecies][];
		for (int i = 0; i < nSpecies; i++)
			dist[i] = new double[observation.getTimePoints().length];
		normaliseResult((BasicResult) simulation,observation.getTimePoints());
		
		for (int n = 0; n < nSpecies; n++) {
			double[] obsSeries = observation.getTimeSeries(n);
			double[] simSeries = simulation.getTimeSeries(n);
			for (int i = 0; i < observation.getTimePoints().length; i++)
				dist[n][i] = Math.abs(obsSeries[i] - simSeries[i]);
		}
		
		//calculate Euclidean distance (averaged over all species)
		//more general, in case we want to do something different for different species
		double[] sums = new double[nSpecies];
		
		double avgDist = 0;
		for (int n = 0; n < nSpecies; n++) {
			sums[n] = 0;
			for (int i = 0; i < observation.getTimePoints().length; i++)
				sums[n] += dist[n][i] * dist[n][i]; 
			sums[n] = Math.sqrt(sums[n]);
			avgDist += sums[n];
		}
		
		return avgDist;
	}
	
	// simply for convenience (perhaps we should get local copies
	// of the maps for performance reasons?)
	private AbstractDistribution priorOfName(String name) {
		return params.getPriors().get(name);
	}
	private AbstractDistribution proposalOfName(String name) {
		return params.getProposals().get(name);
	}
	
	private double[] nextSample(double[] currentP) throws BioPEPAException {
		//model parameter set as a dictionary (name -> value)?
		//Map<String,Double> P = new HashMap<String,Double>();
		//or just an array (since the dimension is fixed and known)
		double newP[] = new double[dim];
		ExperimentLine el = new ExperimentLine("");
		//draw samples from proposals
		for (int i = 0 ; i < dim; i++) {
			double p = params.getProposals().get(names[i]).sample(currentP[i]);
			newP[i] = p;
			el.addRateValue(names[i], p);
		}
		//update the system with new parameter values
		el.applyToAst(astModel);
		ModelCompiler mc = BioPEPA.compile(astModel);
		mc.compile();
		sbaModel = BioPEPA.generateSBA(mc);


		//simulate the new system
		Result r1 = solver.startTimeSeriesAnalysis(sbaModel, solverParams, monitor);
		BasicResult r = (BasicResult) r1;
		r.setComponentNames(components);
		
		
		//compare trace with observations: if distance is low enough,
		//accept sample with the appropriate probability, otherwise
		//stay at current parameter values
		//double[] nextP = new double[dim];
		//System.arraycopy(currentP, 0, nextP, 0, dim);
		//or:
		double nextP[] = Arrays.copyOf(currentP, dim);
		if (distance(r) < params.getTolerance()) {
			//calculate acceptance probability
			double acceptProb = 1.0;
			for (int i = 0; i < dim; i++) {
				double a = params.getPriors().get(names[i]).pdf(newP[i]) /
					params.getPriors().get(names[i]).pdf(currentP[i]);
				a *= params.getProposals().get(names[i]).pdf(newP[i],currentP[i]) /
					params.getProposals().get(names[i]).pdf(currentP[i],newP[i]);
				acceptProb *= a;
			}
			//acceptProb may be > 1, but that's not a problem here
			double alpha = Math.random(); //sample uniformly from [0,1]
			if (alpha <= acceptProb)
				nextP = Arrays.copyOf(newP,dim);
		}
		//samples.add(nextP);
		return nextP;
	}
	
	public void runABC() {
		// create solver...
		solver = Solvers.getSolverInstance("gillespie");
		// ...and set its parameters:
		// for Gillespie, this means start time (0 by default), stop time, number of
		// replications (1) and number of points (100, which does not appear
		// to be used?). Of these, we must override the stop time.
		solverParams = solver.getRequiredParameters();
		for (Parameter param : solverParams.arrayOfKeys())
			solverParams.setValue(param, param.getDefault());
		solverParams.setValue(Parameter.Stop_Time, params.getStopTime());
		solverParams.setValue(Parameter.Components, components);
		
		solverParams.setValue(Parameter.Data_Points, 10);
		try {
			observation = solver.startTimeSeriesAnalysis(sbaModel, solverParams, monitor);
		} catch (BioPEPAException e) {
			System.out.println("Error when obtaining initial simulation (observation)...");
			e.printStackTrace();
		}
		solverParams.setValue(Parameter.Data_Points, Parameter.Data_Points.getDefault());
		
		// initialize (model) parameter samples from priors...
		double[] P = new double[dim];
		for (int i = 0; i < dim; i++) {
			P[i] = params.getPriors().get(names[i]).sample();
		}
		// ...and sample repeatedly
		try {
			for (int n = 0; n < params.getN(); n++) {
				P = nextSample(P);
				samples.add(P);
			}
		} catch (BioPEPAException e) {
			System.out.println("Something went wrong when sampling!");
			e.printStackTrace();
		}
	}
	
	/*
	private String[] getComponentNames(ModelCompiler mc) {
		Set<String> compNames = new HashSet<String>();
		for (ComponentData c : mc.getComponents())
			compNames.add(c.getName());
		//mc.
		String[] compNamesArray = new String[compNames.size()];
		int i = 0;
		for (String name : compNames)
			compNamesArray[i++] = name;
		return compNamesArray;
	}
	*/
	
	public void setSource(String filename) {
		//read source file
		byte[] input = null;
		BufferedInputStream is = null;
		try {
			File f = new File(filename);
			input = new byte[(int) f.length()];
			is = new BufferedInputStream(new FileInputStream(f));
			is.read(input);
		} catch (FileNotFoundException e) {
			System.out.println("Could not open file " + filename);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Problem with reading the input file.");
			e.printStackTrace();
		}
		finally {
			try {
				is.close();
			} catch (IOException e) {
				System.out.println("Problem with closing the input file.");
				e.printStackTrace();
			}
		}
		String source = new String(input); // will throw NPE if file wasn't read (?)
		
		//make model
		try {
			astModel = BioPEPA.parse(source);
			System.out.println("Parsed input OK");
			ModelCompiler mc = BioPEPA.compile(astModel);
			ProblemInfo[] problems = mc.compile();
			for (ProblemInfo problem : problems)
				System.out.println(problem.message);
			sbaModel = BioPEPA.generateSBA(mc);
			components = sbaModel.getComponentNames();
			System.out.println("Compiled original source OK");
		} catch (ParserException e) {
			System.out.println("Something went wrong when parsing the source file!");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Something went really wrong with the parsing...");
			e.printStackTrace();
		}
		
		
	}
	
	/*
	 * Normalise these results to the new set of time points.
	 */
	private void normaliseResult (BasicResult r, double [] newTimePoints){
		/* Note we could avoid making whole new arrays if newTimePoints
		 * is the same length as the old time points array.
		 */
		double[][] newResults = new double [r.getComponentNames().length][];
		for (int index = 0; index < r.getComponentNames().length; index++){
			newResults[index] = new double [newTimePoints.length];
		}
		
		double [] newThroughput = null;
		if (r.throughputSupported()){
			newThroughput = new double [newTimePoints.length];
		} 
		
		int oldIndex = 0;
		for (int newIndex = 0; newIndex < newTimePoints.length; newIndex++){
			/*
			 * Skip past as many old results as we need to, to get to the next
			 * new time point. Note that if there are more new time points then
			 * the for-loop will execute many times without executing the while
			 * loop.
			 */
			while (r.getTimePoints()[oldIndex] < newTimePoints[newIndex] && 
					oldIndex < r.getTimePoints().length){
				oldIndex++;
			}
			/*
			 * Once we have the corresponding oldIndex we just update the new results
			 */
			for (int nameIndex = 0; nameIndex < r.getComponentNames().length; nameIndex++){
				newResults[nameIndex][newIndex] = r.getTimeSeries(nameIndex)[oldIndex];
			}
			/*
			 * Now if throughput is supported update the throughput value.
			 */
			//Anastasis: Disabled for debugging (getting Index Out of Bounds Exception)
			//if (r.throughputSupported()){
			//	newThroughput[newIndex] = r.getActionThroughput(oldIndex);
			//}
			
		}
		r.setResults(newResults);
		r.setTimePoints(newTimePoints);
		if (r.throughputSupported()){
			//this.throughputValues = newThroughput;
		}
	}
	
}
