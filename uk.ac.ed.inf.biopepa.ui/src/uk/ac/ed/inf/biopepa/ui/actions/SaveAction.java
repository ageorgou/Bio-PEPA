package uk.ac.ed.inf.biopepa.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import uk.ac.ed.inf.biopepa.ui.interfaces.ITextProvider;

public class SaveAction extends Action {
	
	ITextProvider source;
	
	public SaveAction(ITextProvider source) {
		super();
		this.source = source;
	}
	//experimental2
	public void run() {			
		TextTransfer transfer = TextTransfer.getInstance();
		Clipboard cb = new Clipboard(Display.getCurrent());
		cb.setContents(new String[] {source.asText()}, new Transfer[] {transfer});
		cb.dispose();
	}
	
}
