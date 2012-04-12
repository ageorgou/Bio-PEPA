package uk.ac.ed.inf.biopepa.ui.wizards.importing;


//import org.eclipse.jface.dialogs.MessageDialog;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.widgets.Combo;

public class NLImportPage extends WizardPage {
	private Text inputText;
	private Text outputText;
	
	private FileDialog fd ;
	private DirectoryDialog dd ;
	
	private String fileName ;
	private String directory ;
	private String inputPath ;
	private String outputDirectory ;
	private IProject selectedProject ;

	private boolean inputFileComplete ;
	private boolean projectComplete ;
	
	/**
	 * Create the wizard.
	 */
	public NLImportPage() {
		super("wizardPage");
		setTitle("Import Narrative Language model");
		setDescription("Choose a Narrative Language file to import");
		fd = new FileDialog(Display.getDefault().getActiveShell(),SWT.OPEN) ;
		dd = new DirectoryDialog(Display.getDefault().getActiveShell(),SWT.OPEN) ;
		inputFileComplete = false ;
		projectComplete = false ;
		setPageComplete(false) ;
	}

	/**
	 * Create contents of the wizard.
	 * @param parent
	 */
	public void createControl(Composite parent) {
		
		
		Composite container = new Composite(parent, SWT.NULL);

		setControl(container);
		
		inputText = new Text(container, SWT.BORDER);
		inputText.setEditable(false);
		inputText.setBounds(143, 60, 220, 21);
		
		Label lblFilePath = new Label(container, SWT.NONE);
		lblFilePath.setBounds(30, 63, 88, 15);
		lblFilePath.setText("Input file:");
		
		Button btnBrowse = new Button(container, SWT.NONE);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				fd.open() ;
				//TODO: change to something more elegant/portable?
				directory = fd.getFilterPath() ;
				fileName = fd.getFileName() ;
				inputPath = (new File(directory,fileName)).getPath();
				
				if (inputPath != null && !inputPath.equals("")) {
					inputFileComplete = true ;
					setPageComplete(projectComplete) ;
					inputText.setText(inputPath) ;
					outputText.setText(directory) ;
					setErrorMessage(null) ;
				}
				else {
					inputFileComplete = false ;
					setPageComplete(false) ;
					setErrorMessage("Please select an input file.") ;
				}

			}
		});
		btnBrowse.setBounds(402, 60, 75, 25);
		btnBrowse.setText("Browse...");
		
		Label lblOutputLocation = new Label(container, SWT.NONE);
		lblOutputLocation.setBounds(30, 223, 87, 15);
		lblOutputLocation.setText("Output location:");
		
		outputText = new Text(container, SWT.BORDER);
		outputText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				outputDirectory = outputText.getText() ;
			}
		});
		outputText.setEditable(false);
		outputText.setBounds(143, 217, 220, 21);
		
		Button btnBrowse_1 = new Button(container, SWT.NONE);
		btnBrowse_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dd.setFilterPath(directory) ;
				dd.open();
				//note: if input path hasn't been selected, directory == null
				//(from default initialization) so dd will (prob) start at cwd
				outputText.setText(dd.getFilterPath()) ;
			}
		});
		btnBrowse_1.setBounds(402, 213, 75, 25);
		btnBrowse_1.setText("Browse...");
		
		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setBounds(30, 115, 281, 21);
		lblNewLabel.setText("Choose a project in which to include the output file:");
		
		final Combo combo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int selectedIndex = combo.getSelectionIndex() ;
				if (selectedIndex != -1) {
					String selectedName = combo.getItem(selectedIndex) ;
					selectedProject = ResourcesPlugin.getWorkspace().getRoot().
						getProject(selectedName) ;
					projectComplete = true ;
					setPageComplete(inputFileComplete) ;
					setErrorMessage(null) ;
				}
				else {
					setErrorMessage("You must select a project");
					projectComplete = false ;
					setPageComplete(false) ;
				}
			}
		});
		combo.setBounds(402, 112, 123, 23);
		//populate combo with projects
		//Set<String> projectNames = new HashSet<String>() ;
		IWorkspace ws = ResourcesPlugin.getWorkspace() ;
		for (IProject proj : ws.getRoot().getProjects()) {
			//projectNames.add(proj.getName()) ;
			combo.add(proj.getName()) ;
		}
		//combo.setItems(projectNames.toArray(new String[] {})) ;
		
		
	}
	
	public String getInputPath() {
		return inputPath ;
	}
	
	public String getDirectory() {
		return directory ;
	}
	
	public String getFileName() {
		return fileName ;
	}
	
	public String getOutputDirectory() {
		return outputDirectory ;
	}
	
	public IProject getSelectedProject() {
		return selectedProject ;
	}
	
	public void performHelp() {
		MessageBox mb = new MessageBox(this.getShell()) ;
    	mb.setText("You asked for help!") ;
    	mb.setMessage("But you won't be getting any for the time being...\n" + 
    			"I'm also not sure how to disable this button.") ;
    	mb.open();
    	return;
	}
}
