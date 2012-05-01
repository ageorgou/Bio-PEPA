package uk.ac.ed.inf.biopepa.ui.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;

import uk.ac.ed.inf.biopepa.ui.interfaces.ITextProvider;

/**
 * 
 * An action to save the contents of e.g. a view in a text file. The file's
 * contents must come from an ITextProvider.
 * @author ageorgou
 *
 */
public class SaveAction extends Action {
	
	ITextProvider source;
	FileDialog fd;
	String filterPath;
	
	public SaveAction(ITextProvider source) {
		super();
		this.source = source;
		
		
	}
	
	public void run() {			
		
		String text = source.asText();
		
		if (text.isEmpty())
			MessageDialog.openError(Display.getCurrent().getActiveShell(),
					"Error", "No text to save.");
		
		else try {
			
			fd = new FileDialog(Display.getDefault().getActiveShell(),SWT.SAVE) ;
			fd.setText("Save as...");
			fd.setOverwrite(true);
			//if called for the first time, get the location of the file in the editor
			if (filterPath == null) {
				IResource modelFile = (IResource) PlatformUI.getWorkbench().getActiveWorkbenchWindow(). 
					getActivePage().getActiveEditor().getEditorInput().getAdapter(IResource.class);
				filterPath = modelFile.getLocation().toFile().getParent();
			}
			fd.setFilterPath(filterPath);
			String targetFile = fd.open();
			
			//write the invariants in the target file
			if (targetFile != null) {
				Writer w = new BufferedWriter(new FileWriter(new File(targetFile)));
				w.write(source.asText());
				w.close();
				filterPath = (new File(targetFile)).getParent();
			}
		}
		catch (Exception e){
			e.printStackTrace();
			MessageDialog.openError(Display.getCurrent().getActiveShell(),
					"Error", "Error while saving to file.");
		}
	}
	
}
