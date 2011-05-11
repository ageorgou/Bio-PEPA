package uk.ac.ed.inf.biopepa.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import uk.ac.ed.inf.biopepa.ui.wizards.export.PrismExportWizard;

public class PrismExportAction extends AbstractAction {

	public void run(IAction action) {
		WizardDialog dialog = null;
		try {
			PrismExportWizard wizard = new PrismExportWizard(model);
			dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
			dialog.open();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
