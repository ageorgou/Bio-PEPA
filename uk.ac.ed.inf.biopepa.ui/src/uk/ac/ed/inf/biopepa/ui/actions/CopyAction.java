package uk.ac.ed.inf.biopepa.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import uk.ac.ed.inf.biopepa.ui.interfaces.ITextProvider;

/**
 * An action to copy the contents of e.g. a view to the clipboard.
 * The text must come from an ITextProvider.
 * 
 * @author ageorgou
 */

public class CopyAction extends Action {
	
	ITextProvider source;
	
	public CopyAction(ITextProvider source) {
		super();
		this.source = source;
	}
	
	public void run() {			
		TextTransfer transfer = TextTransfer.getInstance();
		Clipboard cb = new Clipboard(Display.getCurrent());
		cb.setContents(new String[] {source.asText()}, new Transfer[] {transfer});
		cb.dispose();
	}
	
}
