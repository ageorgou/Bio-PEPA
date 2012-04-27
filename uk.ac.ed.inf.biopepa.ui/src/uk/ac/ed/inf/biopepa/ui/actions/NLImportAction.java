package uk.ac.ed.inf.biopepa.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import uk.ac.ed.inf.biopepa.ui.wizards.importing.NLImportWizard;

public class NLImportAction extends AbstractAction {

	public void run(IAction action) {
		WizardDialog dialog = null;
		try {
			NLImportWizard wizard = new NLImportWizard();
			dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
			dialog.open();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}