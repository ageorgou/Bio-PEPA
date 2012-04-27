package uk.ac.ed.inf.biopepa.ui.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;

import uk.ac.ed.inf.biopepa.ui.interfaces.ITextProvider;

public class SaveAction extends Action {
	
	ITextProvider source;
	
	public SaveAction(ITextProvider source) {
		super();
		this.source = source;
	}
	//experimental2
	public void run() {			
		
		String text = source.asText();
		
		if (text.isEmpty())
			MessageDialog.openError(Display.getCurrent().getActiveShell(),
					"Error", "No text to save.");
		
		else try {
			//get the location of the file in the editor
			IResource modelFile = (IResource) PlatformUI.getWorkbench().getActiveWorkbenchWindow(). 
				getActivePage().getActiveEditor().getEditorInput().getAdapter(IResource.class);
			String modelPath = modelFile.getLocation().removeLastSegments(1).toString();
							
			FileDialog fd =
				new FileDialog(Display.getDefault().getActiveShell(),SWT.SAVE) ;
			fd.setOverwrite(true);
			fd.setFilterPath(modelPath);
			String targetFile = fd.open();
			
			//write the invariants in the target file
			if (targetFile != null) {
				Writer w = new BufferedWriter(new FileWriter(new File(targetFile)));
				w.write(source.asText());
				w.close();
			}
			
			
		}
		catch (Exception e){
			e.printStackTrace();
			MessageDialog.openError(Display.getCurrent().getActiveShell(),
					"Error", "Error while saving to file.");
		}
	}
	
}
