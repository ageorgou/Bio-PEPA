/*******************************************************************************
 * Copyright (c) 2009 University of Edinburgh.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the BSD Licence, which accompanies this feature
 * and can be downloaded from http://groups.inf.ed.ac.uk/pepa/update/licence.txt
 ******************************************************************************/
package uk.ac.ed.inf.biopepa.ui.views;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import uk.ac.ed.inf.biopepa.core.sba.OutlineAnalyser;
import uk.ac.ed.inf.biopepa.core.sba.SimpleTree;
import uk.ac.ed.inf.biopepa.ui.BioPEPAEvent;
import uk.ac.ed.inf.biopepa.ui.interfaces.BioPEPAListener;
import uk.ac.ed.inf.biopepa.ui.interfaces.BioPEPAModel;
import uk.ac.ed.inf.biopepa.ui.interfaces.ITextProvider;

public class BioPEPAOutline extends ContentOutlinePage implements
		BioPEPAListener, ITextProvider {
	
	Action copyAction;
	Action saveAction;
	
	private Runnable runnable = new Runnable() {
		public void run() {
			TreeViewer tv = getTreeViewer();
			tv.setInput(bt);
			if(expanded != null)
				tv.setExpandedElements(expanded);
		}		
	};

	public BioPEPAOutline(BioPEPAModel model) {
		this.model = model;
	}


	private class OutlineContentProvider extends ArrayContentProvider implements
			ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {
			return ((SimpleTree) parentElement).getChildren();
		}

		public Object getParent(Object element) {
			return ((SimpleTree) element).getParent();
		}

		public boolean hasChildren(Object element) {
			return ((SimpleTree) element).getChildren().length != 0;
		}

	}

	private class OutlineLabelProvider extends LabelProvider {
		public String getText(Object element) {
			return ((SimpleTree) element).getName();
		}
	}

	private BioPEPAModel model;
	private SimpleTree[] bt;
	private Object[] expanded;

	public void modelChanged(BioPEPAEvent event) {
		if (event.getEvent().equals(BioPEPAEvent.Event.PARSED)
		   || event.getEvent().equals(BioPEPAEvent.Event.MODIFIED)) {
			refreshTree();
		} else if (event.getEvent().equals(BioPEPAEvent.Event.EXCEPTION))
			refreshTree();
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		getTreeViewer().setContentProvider(new OutlineContentProvider());
		getTreeViewer().setLabelProvider(new OutlineLabelProvider());
		refreshTree();
		makeActions();
		hookContextMenu();
		//add for toolbar/menu items, if desired
	}

	private void makeActions() {
		copyAction = new CopyInvariantsAction();
		copyAction.setText("Copy all");
		copyAction.setToolTipText("Copy outline to clipboard");
		copyAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		
		saveAction = new SaveInvariantsAction();
		saveAction.setText("Save...");
		saveAction.setToolTipText("Save outline to file");
		saveAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT));
	}
	
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				BioPEPAOutline.this.fillContextMenu(manager);
			}
		});
		TreeViewer tv = getTreeViewer();
		Menu menu = menuMgr.createContextMenu(tv.getControl());
		tv.getControl().setMenu(menu);
		getSite().registerContextMenu("uk.ac.ed.inf.biopepa.ui.outlineMenu",
				menuMgr, tv);
	}
	
	private void fillContextMenu(IMenuManager manager) {
		manager.add(copyAction);
		manager.add(saveAction);
		/*
		manager.add(copyAction);
		manager.add(saveAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		*/
	}
	
	private void refreshTree() {
		OutlineAnalyser outlineanalyser = new OutlineAnalyser();
		this.expanded = null;
		SimpleTree[] newtree = 
				outlineanalyser.createOutlineTree(model.getSBAModel());
		
		/* If the current tree is not null, then we may have some
		 * elements of the tree which are expanded. So it is good
		 * to have them remain expanded even if the tree has been
		 * updated. TODO: please check that this actually works?
		 */
		if(bt != null) {
			// getTreeViewer is only accessible from the UI thread
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					TreeViewer tv = getTreeViewer();
					expanded = tv.getExpandedElements();
				}					
			});
		}
		
	    bt = newtree;
		
		Display.getDefault().asyncExec(runnable);
	}
	
	public String asText() {
		StringBuilder sb = new StringBuilder();
		for (SimpleTree t : bt) {
			sb.append(t.printTree());
			sb.append("\n");
		}
		return sb.toString();
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
			String text = asText();

			if (text == null || text.isEmpty())
				MessageDialog.openError(Display.getCurrent().getActiveShell(),
						"Outline", "No outline found.");

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
					w.write(asText());
					w.close();
				}


			}
			catch (Exception e){
				e.printStackTrace();
				MessageDialog.openError(Display.getCurrent().getActiveShell(),
						"Outline", "Error while saving outline.");
			}
		}

	}
}
