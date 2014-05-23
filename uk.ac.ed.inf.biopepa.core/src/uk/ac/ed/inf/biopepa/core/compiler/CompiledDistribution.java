package uk.ac.ed.inf.biopepa.core.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import uk.ac.ed.inf.biopepa.core.BioPEPAException;
import uk.ac.ed.inf.biopepa.core.dom.AST;
import uk.ac.ed.inf.biopepa.core.dom.Expression;
import uk.ac.ed.inf.biopepa.core.dom.FunctionCall;
import uk.ac.ed.inf.biopepa.uncertain.AbstractDistribution;
import uk.ac.ed.inf.biopepa.uncertain.Exponential;
import uk.ac.ed.inf.biopepa.uncertain.Gaussian;
import uk.ac.ed.inf.biopepa.uncertain.Uniform;

public class CompiledDistribution extends CompiledExpression {

	
	public enum Distribution {
		GAUSSIAN(AST.Literals.GAUSSIAN.getToken(),2),
		UNIFORM(AST.Literals.UNIFORM.getToken(),2),
		EXPONENTIAL(AST.Literals.EXPONENTIAL.getToken(),1);
		
		String name;
		int arg;
		
		Distribution(String name, int arg) {
			this.arg = arg;
			this.name = name;
		}
		
		public String getID() { return name; }
		public int args() { return arg; } 
	}

	Distribution distribution = null;
	List<CompiledExpression> arguments = new ArrayList<CompiledExpression>(3);
	
	public static Distribution getDistribution(String dist) {
		for (Distribution d : Distribution.values())
			if (d.name.equals(dist))
				return d;
		
		return null;
	}
	
	public Distribution getDistribution() { return distribution; }
	
	public void setDistribution(Distribution distribution) {
		//do we need this check?
		if (arguments.size() > distribution.arg)
			throw new IllegalStateException();
		this.distribution = distribution;
	}
	
	public List<CompiledExpression> getArguments(){
		return Collections.unmodifiableList(arguments);
	}

	void setArgument(int index, CompiledExpression argument) {
		if (distribution != null && distribution.arg < index)
			throw new IllegalArgumentException();
		for (int i = arguments.size(); i <= index; i++)
			arguments.add(null);
		arguments.set(index, argument);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(distribution.name).append("(");
		for (CompiledExpression en : arguments)
			sb.append(en.toString()).append(", ");
		sb.replace(sb.length() - 2, sb.length(), ")");
		return sb.toString();
	}
	
	/**
	 * Get a Distribution referred to in a given FunctionCall, checking for problems at the same time.
	 * @param compiler
	 * @param call
	 * @return
	 * @throws CompilerException if the distribution contained in the call does not exist,
	 * or if the number of arguments supplied to it is incorrect.
	 */
	public static Distribution checkDistribution(ModelCompiler compiler, FunctionCall call)
			throws CompilerException {
		
		Distribution d = getDistribution(call.getName().getIdentifier());
		if (d == null) {
			compiler.problemRequestor.accept(ProblemKind.UNSUPPORTED_FUNCTION_USED, call);
			throw new CompilerException();
		}
		if (call.arguments().size() != d.arg) {
			compiler.problemRequestor.accept(ProblemKind.INVALID_NUMBER_OF_ARGUMENTS, call);
			throw new CompilerException();
		}
		return d;
	}
	
	public static AbstractDistribution getImplementation(FunctionCall call, ModelCompiler mc)
			throws BioPEPAException {
		
		Distribution d = checkDistribution(mc, call);
		
		//calculate the value of the parameters of the distribution		
		List<Double> distArgs = new ArrayList<Double>();
		for (Expression e : call.arguments()) {
			ExpressionEvaluatorVisitor v  = new ExpressionEvaluatorVisitor(mc);
			e.accept(v);
			if (! (v.node instanceof CompiledNumber))
				throw new CompilerException("Could not retrieve arguments of distribution");
			distArgs.add(((CompiledNumber) v.node).getNumber().doubleValue());
		}
		
		switch(d) {
		case UNIFORM :	return new Uniform(distArgs.get(0),distArgs.get(1));
		
		case GAUSSIAN : return new Gaussian(distArgs.get(0),distArgs.get(1));
		
		case EXPONENTIAL : return new Exponential(distArgs.get(0));
		
		default : throw new CompilerException("No such distribution");
		}
		
		
	}
		

	@Override
	public boolean accept(CompiledExpressionVisitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public CompiledDistribution clone() {
	CompiledDistribution cd = new CompiledDistribution();
	cd.distribution = distribution;
	for (CompiledExpression ce : arguments) {
		if (ce != null)
			cd.arguments.add(ce.clone());
		else
			cd.arguments.add(null);
	}
	if (expandedForm != null)
		cd.expandedForm = expandedForm.clone();
	return cd;
}


	@Override
	public boolean isDynamic() {
		//this needs some more thought - what happens to the result of the call?
		return true;
	}

}
