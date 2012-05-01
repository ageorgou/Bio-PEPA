/*******************************************************************************
 * Copyright (c) 2009 University of Edinburgh.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the BSD Licence, which accompanies this feature
 * and can be downloaded from http://groups.inf.ed.ac.uk/pepa/update/licence.txt
 ******************************************************************************/
package uk.ac.ed.inf.biopepa.ui.views;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import uk.ac.ed.inf.biopepa.core.sba.OutlineAnalyser;
import uk.ac.ed.inf.biopepa.core.sba.SimpleTree;
import uk.ac.ed.inf.biopepa.ui.BioPEPAEvent;
import uk.ac.ed.inf.biopepa.ui.actions.CopyAction;
import uk.ac.ed.inf.biopepa.ui.actions.SaveAction;
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
		contributeToActionBars();
	}

	private void makeActions() {
		copyAction = new CopyAction(this);
		copyAction.setText("Copy outline");
		copyAction.setToolTipText("Copy outline to clipboard");
		copyAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		
		saveAction = new SaveAction(this);
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
	
	private void contributeToActionBars() {
		IActionBars bars = getSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(copyAction);
		manager.add(saveAction);
	}
	
	private void fillContextMenu(IMenuManager manager) {
		manager.add(copyAction);
		manager.add(saveAction);
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
	
}
