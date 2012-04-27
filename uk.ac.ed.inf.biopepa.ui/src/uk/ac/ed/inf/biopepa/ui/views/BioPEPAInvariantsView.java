package uk.ac.ed.inf.biopepa.ui.views;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;

import uk.ac.ed.inf.biopepa.ui.interfaces.ITextProvider;


/**
 * 
 * This is based on the sample class for creating
 * a view. You can tell this where I've left in comments
 * code for creating "useless" actions when you double click
 * an item for example (or the context menu or the fill up menu).
 * I've left the code for this in comments since I may wish to
 * add in actions to, for example perform common sub-expression
 * elimination. 
 */

public class BioPEPAInvariantsView extends ViewPart implements ITextProvider {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "uk.ac.ed.inf.biopepa.ui.views.BioPEPAInvariantsView";

	private TableViewer viewer;
	private Action saveAction;
	private Action copyAction;
	// private Action action2;
	// private Action doubleClickAction;

	// The shared instance
	private static BioPEPAInvariantsView invview;
	
	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			return new String[0]; // { "One", "Two", "Three" };
		}
	}
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}
		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}
		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().
					getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}
	
	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public BioPEPAInvariantsView() {
		invview = this;
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				BioPEPAInvariantsView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		/*
		manager.add(action1);
		manager.add(new Separator());
		manager.add(action2);
		*/
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(copyAction);
		manager.add(saveAction);
		
		/*
		manager.add(action1);
		manager.add(action2);
		// Other plug-ins can contribute there actions here
		 * 
		 */
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(copyAction);
		manager.add(saveAction);
		/*
		manager.add(action1);
		manager.add(action2);
		*/
	}

	private void makeActions() {
		copyAction = new CopyInvariantsAction();
		copyAction.setText("Copy all");
		copyAction.setToolTipText("Copy invariants to clipboard");
		copyAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		
		saveAction = new SaveInvariantsAction();
		saveAction.setText("Save...");
		saveAction.setToolTipText("Save invariants to file");
		saveAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT));
	}
	
	/*
	private void makeActions() {
		action1 = new Action() {
			public void run() {
				showMessage("Action 1 executed");
			}
		};
		action1.setText("Action 1");
		action1.setToolTipText("Action 1 tooltip");
		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		action2 = new Action() {
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				showMessage("Double-click detected on "+obj.toString());
			}
		};
	}
	*/

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				// doubleClickAction.run();
			}
		});
	}
	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Invariants",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static BioPEPAInvariantsView getDefault() {
		return invview;
	}
	
	public void clearInvariants(){
		viewer.getTable().removeAll();
	}
	
	public void addInvariant(String invariant){
		viewer.add(invariant);
	}
	
	private String[] getInvariants() {
		TableItem[] items = viewer.getTable().getItems();
		String[] invariants = new String[items.length];
		for (int i = 0; i < items.length; i++)
			invariants[i] = items[i].getText();
		
		return invariants;
		
	}
	
	public String asText() {
		String text = "";
		String[] invs = getInvariants();
		for (int i = 0; i < invs.length; i++)
			text += invs[i] + "\n";
		return text;
	}
	
	private class CopyInvariantsAction extends Action {
		
		public void run() {			
			TextTransfer transfer = TextTransfer.getInstance();
			Clipboard cb = new Clipboard(Display.getCurrent());
			cb.setContents(new String[] {asText()}, new Transfer[] {transfer});
			cb.dispose();
			
		}
				
	}
	
	private class SaveInvariantsAction extends Action {
		
		public void run() {
			
			if (getInvariants().length == 0)
				showMessage("You must first infer the invariants.");
			
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
//					for (String inv : invariants) {
//						w.write(inv + "\n");
//					}
					w.write(asText());
					w.close();
				}
				
				
			}
			catch (Exception e){
				e.printStackTrace();
				showMessage("Error while saving invariants.");
			}
		}
		
	}
}