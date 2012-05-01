package uk.ac.ed.inf.biopepa.ui.interfaces;


/**
 * A simple interface for classes which can use CopyAction, SaveAction etc.
 *  
 * @author ageorgou
 *
 */
public interface ITextProvider {

	/**
	 * A method to get a textual representation of an object's contents,
	 * for copying, saving etc.
	 * 
	 * @return a String containing the object's contents in a reasonable way.
	 */
	String asText();
	
}
