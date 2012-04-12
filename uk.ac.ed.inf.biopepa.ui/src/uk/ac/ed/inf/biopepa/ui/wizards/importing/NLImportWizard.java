package uk.ac.ed.inf.biopepa.ui.wizards.importing;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
//import org.eclipse.core.resources.IWorkspace;
//import org.eclipse.core.resources.ResourcesPlugin;
//import org.eclipse.core.runtime.IPath;
//import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard ;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.xtext.example.mydsl.MainClass;
import org.xtext.example.mydsl.NarrLangFatalException;
import org.xtext.example.mydsl.NarrLangModelException;


public class NLImportWizard extends Wizard {

	private NLImportPage firstPage ;
	//private String inputPath ;
	private String outputDir ;
	
	public NLImportWizard() {
		super();
		setWindowTitle("Export options for Bio-PEPA");
	}
	
	//Doesn't seem to work...
	public boolean isHelpAvailable() {
		return false;
	}
	
	public boolean performFinish() {

        //inputPath = firstPage.getInputPath() ;
        outputDir = firstPage.getOutputDirectory() ;
        if (outputDir == null)
        	outputDir = System.getProperty("user.dir") ;
        
        MainClass mc ;
        //boolean canContinue = false;
        
        String[] transArgs = new String[] {firstPage.getFileName(),
    			firstPage.getDirectory(), outputDir} ;
    	mc = new MainClass(transArgs) ;
        
        try {
        	//first run the validator to identify any errors in the model
        	mc.doValidation() ;
        	
        }
        catch (NarrLangFatalException e) {
        	MessageBox mb = new MessageBox(this.getShell()) ;
        	mb.setText("Translation unsuccessful") ;
        	String errors = "" ;
        	for (String error : e.getErrors())
        		errors = errors + error + "\n";
        	mb.setMessage(e.getMessage() + "\n" + errors) ;
        	//System.out.println(e.getMessage() + "\n" + errors);
        	mb.open();
        	return false;
        }
        catch (NarrLangModelException e) {
        	MessageBox mb = new MessageBox(this.getShell(),
        			SWT.YES | SWT.NO | SWT.ICON_WARNING) ;
        	mb.setText("Translation unsuccessful") ;
        	String errors = "" ;
        	for (String error : e.getErrors())
        		errors = errors + error + "\n";
        	mb.setMessage(e.getMessage() + "\n" + errors + "\n\nContinue?") ;
        	int shouldContinue = mb.open();
        	if (shouldContinue != SWT.YES)
        		return false;
        	
        }
        catch (Exception e) {
        	MessageBox mb = new MessageBox(this.getShell()) ;
        	mb.setText("Translation unsuccessful") ;
        	mb.setMessage(e.getClass().getSimpleName()) ;
        	mb.open();
        	e.printStackTrace() ;
        	return false ;
        	
        }
        

        try {
        	//run the translation and get the location of the created BioPEPA file
        	mc.doWork() ;
        	
        	String outFileName = mc.getOutFileName() ;
        	String outLoc = mc.getOutFilePath() ;
        	MessageBox mb = new MessageBox(this.getShell(), SWT.YES | SWT.NO) ;
        	mb.setText("Translation successful") ;
        	
        	mb.setMessage("Done! Translated model created at " + outLoc +
        			"\nDo you want to open the model?") ;
        	int mustOpen = mb.open() ;
        	
        	if (mustOpen == SWT.YES) {
        		
        		//use getFile() instead of getFileForLocation(), since
        		//it seems to create the resource whereas the latter doesn't:
        		//this should(??) work(?):
        		//IProject proj = workspace.getRoot().getProjects()[0] ;
        		//or ask the user for a project...
        		IProject proj = firstPage.getSelectedProject() ;
        		/*if (!proj.isOpen())*/ proj.open(null) ;
        		IFile fileHandle = proj.getFile(outFileName) ;
        		URI outURI = new File(outLoc).toURI() ;
        		fileHandle.createLink(outURI, IResource.REPLACE, null) ;
        		//DEBUG:
//        		System.out.println("Created(?) linked resource.") ;
//        		System.out.println("\n\nIFile:\n" + fileHandle) ;
//        		System.out.println(fileHandle.getFullPath()) ;
//        		System.out.println("Exists?: " + fileHandle.exists()) ;
//        		System.out.println("Is linked? " + fileHandle.isLinked()) ;
//        		IWorkspace workspace = ResourcesPlugin.getWorkspace() ;
//        		System.out.println("Valid loc?:" +
//        				workspace.validateLinkLocationURI(fileHandle,outURI)) ;
//        		System.out.println(fileHandle.getParent()) ;
//        		System.out.println(fileHandle.getParent().getName()) ;
//        		
        		IWorkbenchPage page = 
        			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() ;
        		//open the output file in the editor
        		IDE.openEditor(page, fileHandle) ;

        		return true ;
        	}
        	else
        		//if user doesn't want to open the file, exit the wizard
        		return true ;
        }
        catch (NarrLangModelException e) {
        	e.printStackTrace() ;
        	return false ;
        }
        catch (Exception e) {
        	e.printStackTrace() ;
        	return false;
        }
	}
	
	public void addPages() {
		firstPage = new NLImportPage() ;
		addPage(firstPage) ;
	}
	
	
	

	
}
