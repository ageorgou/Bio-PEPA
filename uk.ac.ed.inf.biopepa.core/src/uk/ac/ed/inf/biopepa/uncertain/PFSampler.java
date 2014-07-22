package uk.ac.ed.inf.biopepa.uncertain;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
import uk.ac.ed.inf.biopepa.core.sba.SBAModel;
import uk.ac.ed.inf.biopepa.core.sba.Solvers;
import uk.ac.ed.inf.biopepa.core.sba.Parameters.Parameter;

public class PFSampler {

	private Result observation; // the observed time-series
	// samples must be a list (at least not a set), as there may be duplicates
	//private List<List<Particle>> samples;
	private List<Particle> particles;
	int dim; // number of model parameters to learn
	String[] names; // names of the parameters (to access priors/proposals)
	private PFParameters params; // algorithm parameters
	private Model astModel;
	private SBAModel sbaModel;
	private Solver solver; // SSA solver and its associated parameters etc
	private Parameters solverParams;
	private ProgressMonitor monitor;
	private String[] components;
	private Random random;
	
	public PFSampler(Result observation, PFParameters params) {
		
		this.observation = observation;
		setParams(params);
		//this.samples = new ArrayList<List<Particle>>();
		this.particles = new ArrayList<Particle>();
		this.random = new Random();
		// we need a monitor for calling the simulation engine, so just use...
		this.monitor = new ProgressMonitor() { // a monitor that does nothing
			public void beginTask(int amount) {	}

			public void done() { }

			public boolean isCanceled() {return false;}

			public void setCanceled(boolean state) { }

			public void worked(int worked) {}
		}; // "inspired" by BioPEPACommandLine.FakeProgressMonitor
	}
	
	public String[] getNames() { return names; }
	
	public List<double[]> getSamples() {
		// return the last in the sequence of approximations
		//List<Particle> lastParticles = samples.get(samples.size() - 1);
		List<double[]> paramSamples = new ArrayList<double[]>();
		for (Particle p : particles)
			paramSamples.add(p.parameters);
		return paramSamples;		
	}
		
	
	public void runPF() {
		// create solver...
		solver = Solvers.getSolverInstance("gillespie");
		// ...and set its parameters:
		// for Gillespie, this means start time (0 by default), stop time, number of
		// replications (1) and number of points (100, which does not appear
		// to be used?). Of these, we must override the stop time, which will be different
		// at every intermediate step.
		solverParams = solver.getRequiredParameters();
		for (Parameter param : solverParams.arrayOfKeys())
			solverParams.setValue(param, param.getDefault());
		solverParams.setValue(Parameter.Components, components);
		
		solverParams.setValue(Parameter.Data_Points, 10);
		try {
			observation = solver.startTimeSeriesAnalysis(sbaModel, solverParams, monitor);
		} catch (BioPEPAException e) {
			System.out.println("Error when obtaining initial simulation (observation)...");
			e.printStackTrace();
		}
		solverParams.setValue(Parameter.Data_Points, Parameter.Data_Points.getDefault());
		
		
		//create initial set of particles
		particles = new ArrayList<Particle>();
		//all initial particles start from the first observation point (at t=0)
		double[] initState = new double[components.length];
		for (int k = 0; k < components.length; k++)
			initState[k] = observation.getTimeSeries(k)[0];
		for (int i = 0; i < params.getN(); i++) {
			// sample from prior for parameters
			double[] paramSample = new double[dim];
			for (int j = 0; j < dim; j++)
				paramSample[j] = params.getPriors().get(names[j]).sample();
			particles.add(new Particle(paramSample, initState.clone()));
			//samples.add(particles);
		}
		
		try {
			double currTime = observation.getTimePoints()[0];
			for (int n = 1; n < observation.getTimePoints().length; n++) {
				double nextTime = observation.getTimePoints()[n];
				double[] target = new double[components.length];
				for (int k = 0; k < components.length; k++)
					target[k] = observation.getTimeSeries(k)[n];
				//List<Particle> newSamples = particleFilter(particles,currTime,nextTime,target);
				//samples.add(newSamples);
				particles = particleFilter(particles,currTime,nextTime,target);
				currTime = nextTime;
			}
		} catch (BioPEPAException e) {
			System.out.println("Something went wrong when sampling!");
			e.printStackTrace();
		}
	}
	
	private List<Particle> particleFilter(List<Particle> oldParticles, double currTime, double nextTime,
			double[] target) 
		throws BioPEPAException {
		
		// update the solver's parameters for this filtering step
		solverParams.setValue(Parameter.Start_Time, currTime);
		solverParams.setValue(Parameter.Stop_Time, nextTime);
		
		//choose first Particle randomly
		Particle p = new Particle(oldParticles.get(random.nextInt(params.getN())));
		// update the model based on this particle, and simulate it
		// (updating the particle with the new state)...
		simulateForParticle(p);
		// ...and get its likelihood
		double logLikelihood = getLogLikelihood(p.state, target);
		// we don't store this initial particle yet, only after it "survives" the first comparison
		
		List<Particle> newParticles = new ArrayList<Particle>();
		for (int i = 0; i < params.getN(); i++) {
			//choose a previous particle uniformly...
			int k = random.nextInt(params.getN());
			//...and perturb it
			Particle newP = new Particle(oldParticles.get(k),params.getJitter());
			// simulate till next time, then compare likelihoods
			simulateForParticle(newP);
			double newLogLikelihood = getLogLikelihood(newP.state,target);
			double ratio = Math.exp(newLogLikelihood - logLikelihood);

			//with probability min(ratio,1) ...
			double u = random.nextDouble();
			if (u <= ratio) {
				// ...accept and store the new particle
			    newParticles.add(newP);
			    p = newP;
			    logLikelihood = newLogLikelihood;
			}
			else
				// ...otherwise store the current value of the chain
				newParticles.add(p);
		}
		
		return newParticles;
	}
	
	private void simulateForParticle(Particle p) throws BioPEPAException {
		// for every particle, we must update the model's parameters and
		// initial state accordingly:
		ExperimentLine el = p.createExperimentLine();
		el.applyToAst(astModel);
		ModelCompiler mc = BioPEPA.compile(astModel);
		mc.compile();
		sbaModel = BioPEPA.generateSBA(mc);
		
		// and simulate until the next time:
		Result r1 = solver.startTimeSeriesAnalysis(sbaModel, solverParams, monitor);
		BasicResult r = (BasicResult) r1;
		r.setComponentNames(components);
		
		// record the new state...
		// TODO is this the actual final state, or should we check?
		double[] state = new double[components.length];
		int lastIndex = r.getTimePoints().length-1;
		for (int i = 0; i < components.length; i++)
			state[i] = r.getTimeSeries(i)[lastIndex];
		p.setState(state);
	}
	
	/**	Returns the (unnormalized) log-likelihood of target given state
	 * Assumes a Gaussian observation model with noise variance params.ObsNoise
	 */
	private double getLogLikelihood(double[] state, double[] target) {
			
		double sumExp = 0;
		for (int i = 0; i < state.length; i++) {
			sumExp += Math.pow(state[i] - target[i],2);
		}
		return (-sumExp / 2 * params.getObsNoise());
	}
	
	private void setParams(PFParameters params) {
		this.params = params;
		//get the number of parameters...
		dim = params.getPriors().keySet().size();
		//...and assign an ordering to them (alphabetical is robust to multiple calls)
		int i = 0;
		names = new String[dim];
		for (String name : params.getPriors().keySet())
			names[i++] = name;
		//or simply?
		//names = (String []) params.getPriors().keySet().toArray();
		Arrays.sort(names);
	}
	
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
	
	
	private class Particle {
		
		private double[] parameters;
		private double[] state;
		
		public Particle(double[] parameters, double[] state) {
			this.parameters = parameters;
			this.state = state;
		}
		
		public Particle(Particle q) {
			this.parameters = Arrays.copyOf(q.parameters, q.parameters.length);
			this.state = Arrays.copyOf(q.state, q.state.length);
		}
		
		public Particle(Particle q, double jitter) {
			this.parameters = new double[q.parameters.length];
			for (int i = 0; i < parameters.length; i++)
				parameters[i] = q.parameters[i] + jitter * random.nextGaussian();

			//probably doesn't make sense to perturb the state too?
			this.state = Arrays.copyOf(q.state, q.state.length);
			/*
			this.state = new double[q.state.length];
			for (int i = 0; i < state.length; i++)
				state[i] = q.state[i] + jitter * random.nextGaussian();
			*/
		}
		
		public void setState(double[] state) {
			this.state = state;
		}
		
		public ExperimentLine createExperimentLine() {
			ExperimentLine el = new ExperimentLine("");
			for (int j = 0; j < dim; j++)
				el.addRateValue(names[j], parameters[j]);
			for (int j = 0; j < components.length; j++)
				el.addInitialConcentration(components[j],state[j]);
			return el;
		}
		
	}
	
}
