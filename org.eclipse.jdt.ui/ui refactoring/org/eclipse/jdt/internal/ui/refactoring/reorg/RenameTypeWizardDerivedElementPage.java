/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameCompilationUnitProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
 * 
 * Wizard page for displaying a tree of derived elements renamed along with a
 * type.
 * 
 * @since 3.2
 * 
 */
class RenameTypeWizardDerivedElementPage extends UserInputWizardPage {

	public static class EditElementDialog extends StatusDialog implements IDialogFieldListener {

		private StringDialogField fNameField;
		private IJavaElement fElementToEdit;

		public EditElementDialog(Shell parent, IJavaElement elementToEdit, String newName) {
			super(parent);
			setTitle(RefactoringMessages.RenameTypeWizardDerivedElementPage_change_element_name);
			setShellStyle(getShellStyle() | SWT.RESIZE);

			fElementToEdit= elementToEdit;

			fNameField= new StringDialogField();
			fNameField.setDialogFieldListener(this);
			fNameField.setLabelText(RefactoringMessages.RenameTypeWizardDerivedElementPage_enter_new_name);

			fNameField.setText(newName);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
		 */
		protected Control createDialogArea(Composite parent) {
			final Composite composite= (Composite) super.createDialogArea(parent);
			LayoutUtil.doDefaultLayout(composite, new DialogField[] { fNameField }, true, SWT.DEFAULT, SWT.DEFAULT);
			fNameField.postSetFocusOnDialogField(parent.getDisplay());
			
			LayoutUtil.setWidthHint(fNameField.getLabelControl(null), convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH));
			Text text= fNameField.getTextControl(null);
			LayoutUtil.setHorizontalGrabbing(text);
			TextFieldNavigationHandler.install(text);

			Dialog.applyDialogFont(composite);
			return composite;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			if (field == fNameField)
				updateStatus(validateSettings());
		}

		private IStatus validateSettings() {
			final String name= fNameField.getText();
			if (name.length() == 0) {
				return new StatusInfo(IStatus.ERROR, RefactoringMessages.RenameTypeWizardDerivedElementPage_name_empty);
			}
			IStatus status= JavaConventions.validateFieldName(name);
			if (status.matches(IStatus.ERROR))
				return status;
			if (!Checks.startsWithLowerCase(name))
				return new StatusInfo(IStatus.WARNING, RefactoringMessages.RenameTypeWizardDerivedElementPage_name_should_start_lowercase);

			if (fElementToEdit instanceof IMember && ((IMember) fElementToEdit).getDeclaringType() != null) {
				IType type= ((IMember) fElementToEdit).getDeclaringType();
				if (fElementToEdit instanceof IField) {
					final IField f= type.getField(name);
					if (f.exists())
						return new StatusInfo(IStatus.ERROR, RefactoringMessages.RenameTypeWizardDerivedElementPage_field_exists);
				}
				if (fElementToEdit instanceof IMethod) {
					final IMethod m= type.getMethod(name, ((IMethod) fElementToEdit).getParameterTypes());
					if (m.exists())
						return new StatusInfo(IStatus.ERROR, RefactoringMessages.RenameTypeWizardDerivedElementPage_method_exists);
				}
			}

			return StatusInfo.OK_STATUS;
		}

		public String getNewName() {
			return fNameField.getText();
		}
	}

	private static class DerivedElementTreeContentProvider implements ITreeContentProvider {

		private Map/* <IJavaElement,Set<IJavaElement>> */fTreeElementMap;
		private Set/* <ICompilationUnit> */fTopLevelElements;

		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ICompilationUnit || parentElement instanceof IType)
				return ((Set) fTreeElementMap.get(parentElement)).toArray();
			else
				return new Object[0];
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IMember) {
				final IType declaring= ((IMember) element).getDeclaringType();
				if (declaring != null)
					return declaring;
				else
					return ((IMember) element).getCompilationUnit();
			}
			if (element instanceof ICompilationUnit)
				return null;
			Assert.isTrue(false, "Should not get here"); //$NON-NLS-1$
			return null;
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			return (!isDerivedElement(element));
		}

		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			Assert.isTrue(inputElement == null || inputElement instanceof Map);
			return fTopLevelElements.toArray();
		}

		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
			fTreeElementMap.clear();
			fTreeElementMap= null;
			fTopLevelElements= null;
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			Assert.isTrue(newInput == null || newInput instanceof Map);
			if (newInput == null)
				return;
			final Map derivedElementsMap= (Map) newInput;
			final IJavaElement[] derivedElements= (IJavaElement[]) derivedElementsMap.keySet().toArray(new IJavaElement[0]);
			fTreeElementMap= new HashMap();
			fTopLevelElements= new HashSet();
			for (int i= 0; i < derivedElements.length; i++) {
				final IType declaring;
				if (derivedElements[i] instanceof IMember)
					declaring= ((IMember) derivedElements[i]).getDeclaringType();
				else {
					final IJavaElement parent= derivedElements[i].getParent();
					if (parent instanceof IMember)
						addToMap(parent, derivedElements[i]);
					declaring= (IType) derivedElements[i].getAncestor(IJavaElement.TYPE);
				}
				addToMap(declaring, derivedElements[i]);
				handleDeclaring(declaring);
			}
		}

		private void handleDeclaring(final IType someType) {
			if (someType.getDeclaringType() == null) {
				fTopLevelElements.add(someType.getCompilationUnit());
				addToMap(someType.getCompilationUnit(), someType);
				return;
			}

			final IType enclosing= someType.getDeclaringType();
			addToMap(enclosing, someType);
			handleDeclaring(enclosing);
		}

		private void addToMap(final IJavaElement key, final IJavaElement element) {
			Set elements= (Set) fTreeElementMap.get(key);
			if (elements == null) {
				elements= new HashSet();
				fTreeElementMap.put(key, elements);
			}
			elements.add(element);
		}

	}

	private static class DerivedLabelProvider extends JavaElementLabelProvider {

		private Map fDescriptorImageMap= new HashMap();
		private Map fElementToNewName;

		public DerivedLabelProvider() {
			super(JavaElementLabelProvider.SHOW_DEFAULT);
		}

		public void initialize(Map elementToNewName) {
			this.fElementToNewName= elementToNewName;
		}

		public void dispose() {
			for (Iterator iter= fDescriptorImageMap.values().iterator(); iter.hasNext();) {
				Image image= (Image) iter.next();
				image.dispose();
			}
			super.dispose();
		}

		private Image manageImageDescriptor(ImageDescriptor descriptor) {
			Image image= (Image) fDescriptorImageMap.get(descriptor);
			if (image == null) {
				image= descriptor.createImage();
				fDescriptorImageMap.put(descriptor, image);
			}
			return image;
		}

		public Image getImage(Object element) {
			if (isDerivedElement(element))
				return manageImageDescriptor(JavaPluginImages.DESC_OBJS_DEFAULT_CHANGE);
			return super.getImage(element);
		}

		public Image getJavaImage(Object element) {
			return super.getImage(element);
		}

		public String getText(Object element) {
			if (isDerivedElement(element)) {
				return Messages.format(RefactoringMessages.RenameTypeWizardDerivedElementPage_rename_to, new String[] { super.getText(element), (String)fElementToNewName.get(element) } ); 
			}
			return super.getText(element);
		}

	}

	public static final String PAGE_NAME= "DerivedElementSelectionPage"; //$NON-NLS-1$

	private final long LABEL_FLAGS= JavaElementLabels.DEFAULT_QUALIFIED | JavaElementLabels.ROOT_POST_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH | JavaElementLabels.M_PARAMETER_TYPES
			| JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_APP_RETURNTYPE | JavaElementLabels.M_EXCEPTIONS | JavaElementLabels.F_APP_TYPE_SIGNATURE | JavaElementLabels.T_TYPE_PARAMETERS;

	private Label fDerivedCountLabel;
	private SourceViewer fSourceViewer;
	private ContainerCheckedTreeViewer fTreeViewer;
	private DerivedLabelProvider fTreeViewerLabelProvider;
	private Map fDerivedElementsToNewName;
	private Button fEditElementButton;
	private boolean fWasInitialized;
	private Label fCurrentElementLabel;

	public RenameTypeWizardDerivedElementPage() {
		super(PAGE_NAME);
		setMessage(RefactoringMessages.RenameTypeWizardDerivedElementPage_select_derived_elements);
	}

	// --- UI creation

	public void createControl(Composite parent) {

		ViewForm viewForm= new ViewForm(parent, SWT.BORDER | SWT.FLAT);

		Composite inner= new Composite(viewForm, SWT.NULL);
		GridLayout layout= new GridLayout();
		inner.setLayout(layout);

		createTreeAndSourceViewer(inner);
		createButtonComposite(inner);
		viewForm.setContent(inner);

		setControl(viewForm);

		Dialog.applyDialogFont(viewForm);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.RENAME_TYPE_WIZARD_PAGE);
	}

	private void createTreeAndSourceViewer(Composite superComposite) {
		SashForm composite= new SashForm(superComposite, SWT.HORIZONTAL);
		initializeDialogUnits(superComposite);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(20);
		gd.widthHint= convertWidthInCharsToPixels(10);
		composite.setLayoutData(gd);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);

		createDerivedElementTreeComposite(composite);
		createSourceViewerComposite(composite);
		composite.setWeights(new int[] { 50, 50 });
	}

	private void createDerivedElementTreeComposite(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);

		createTypeHierarchyLabel(composite);
		createTreeViewer(composite);
	}

	private void createTypeHierarchyLabel(Composite composite) {
		fDerivedCountLabel= new Label(composite, SWT.WRAP);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint= JavaElementImageProvider.SMALL_SIZE.x;
		fDerivedCountLabel.setLayoutData(gd);
		fDerivedCountLabel.setText(RefactoringMessages.RenameTypeWizardDerivedElementPage_review_derived_elements);
	}

	private void createTreeViewer(Composite composite) {
		Tree tree= new Tree(composite, SWT.CHECK | SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		fTreeViewer= new ContainerCheckedTreeViewer(tree);
		fTreeViewer.setUseHashlookup(true);
		fTreeViewer.setSorter(new JavaElementSorter());
		fTreeViewer.setContentProvider(new DerivedElementTreeContentProvider());
		fTreeViewerLabelProvider= new DerivedLabelProvider();
		fTreeViewer.setLabelProvider(fTreeViewerLabelProvider);
		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				RenameTypeWizardDerivedElementPage.this.treeViewerSelectionChanged(event);
			}
		});
		fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				RenameTypeWizardDerivedElementPage.this.editCurrentElement();
			}
		});
	}

	private void createSourceViewerComposite(Composite parent) {
		Composite c= new Composite(parent, SWT.NONE);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		c.setLayout(layout);

		createSourceViewerLabel(c);
		createSourceViewer(c);
	}

	private void createSourceViewerLabel(Composite c) {
		fCurrentElementLabel= new Label(c, SWT.NONE);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint= JavaElementImageProvider.SMALL_SIZE.x;
		fCurrentElementLabel.setText(RefactoringMessages.RenameTypeWizardDerivedElementPage_select_element_to_view_source);
		fCurrentElementLabel.setLayoutData(gd);
	}

	private void createSourceViewer(Composite c) {
		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		fSourceViewer= new JavaSourceViewer(c, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION, store);
		fSourceViewer.configure(new JavaSourceViewerConfiguration(getJavaTextTools().getColorManager(), store, null, null));
		fSourceViewer.setEditable(false);
		fSourceViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		fSourceViewer.getControl().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
	}

	private static JavaTextTools getJavaTextTools() {
		return JavaPlugin.getDefault().getJavaTextTools();
	}

	private void createButtonComposite(Composite superComposite) {
		Composite buttonComposite= new Composite(superComposite, SWT.NONE);
		buttonComposite.setLayoutData(new GridData());
		GridLayout bcl= new GridLayout();
		bcl.numColumns= 2;
		buttonComposite.setLayout(bcl);

		Button returnToDefaults= new Button(buttonComposite, SWT.PUSH);
		returnToDefaults.setText(RefactoringMessages.RenameTypeWizardDerivedElementPage_restore_defaults);
		returnToDefaults.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(returnToDefaults);
		returnToDefaults.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				RenameTypeWizardDerivedElementPage.this.resetDataInRefAndUI();
			}
		});
		fEditElementButton= new Button(buttonComposite, SWT.PUSH);
		fEditElementButton.setText(RefactoringMessages.RenameTypeWizardDerivedElementPage_change_name);
		fEditElementButton.setLayoutData(new GridData());
		fEditElementButton.setEnabled(false);
		SWTUtil.setButtonDimensionHint(fEditElementButton);
		fEditElementButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				RenameTypeWizardDerivedElementPage.this.editCurrentElement();
			}
		});
	}


	// ---------------------- Acting

	public void setVisible(boolean visible) {
		if (visible) {
			initializeUIFromRef();
		}
		super.setVisible(visible);
	}

	private void initializeUIFromRef() {
		// Get data from the refactoring
		final Map elementsToNewNames= getRenameTypeProcessor().getDerivedElementsToNewNames();
		// To prevent flickering, only change the input if the list is brand-new
		if (fDerivedElementsToNewName == null || elementsToNewNames != fDerivedElementsToNewName) {
			fDerivedElementsToNewName= elementsToNewNames;
			fTreeViewerLabelProvider.initialize(fDerivedElementsToNewName);
			fTreeViewer.setInput(fDerivedElementsToNewName);
		}
		restoreSelectionAndNames(getRenameTypeProcessor().getDerivedElementsToSelection());
		fTreeViewer.expandAll();
		fWasInitialized= true;
	}

	private void initializeRefFromUI() {
		IJavaElement[] selected= getCheckedDerivedElements();
		Map selection= getRenameTypeProcessor().getDerivedElementsToSelection();
		for (Iterator iter= selection.keySet().iterator(); iter.hasNext();) {
			IJavaElement element= (IJavaElement) iter.next();
			selection.put(element, Boolean.FALSE);
		}
		for (int i= 0; i < selected.length; i++)
			selection.put(selected[i], Boolean.TRUE);

	}

	private void resetDataInRefAndUI() {
		getRenameTypeProcessor().resetSelectedDerivedElements();
		restoreSelectionAndNames(getRenameTypeProcessor().getDerivedElementsToSelection());
	}

	protected void editCurrentElement() {
		IStructuredSelection selection= (IStructuredSelection) fTreeViewer.getSelection();
		if ( (selection != null) && isDerivedElement(selection.getFirstElement())) {
			IJavaElement element= (IJavaElement) selection.getFirstElement();
			String newName= (String) fDerivedElementsToNewName.get(element);
			if (newName == null)
				return;
			EditElementDialog dialog= new EditElementDialog(getShell(), element, newName);
			if (dialog.open() == Window.OK) {
				String changedName= dialog.getNewName();
				if (!changedName.equals(newName)) {
					fDerivedElementsToNewName.put(element, changedName);
					fTreeViewer.update(element, null);
				}
			}
		}
	}

	private void restoreSelectionAndNames(final Map selection) {
		final Map selectedElements= selection;
		for (Iterator iter= selectedElements.keySet().iterator(); iter.hasNext();) {
			IJavaElement element= (IJavaElement) iter.next();
			boolean isSelected= ((Boolean) selectedElements.get(element)).booleanValue();
			fTreeViewer.setChecked(element, isSelected);
			fTreeViewer.update(element, null);
		}
	}

	// ------------ Navigation

	/*
	 * @see IWizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		if (fWasInitialized)
			initializeRefFromUI();
		// computes the input successor page
		// (=create changes)
		IWizardPage nextPage= super.getNextPage();
		nextPage.setPreviousPage(this);
		return nextPage;
	}


	/*
	 * @see IWizardPage#getPreviousPage()
	 */
	public IWizardPage getPreviousPage() {
		if (fWasInitialized)
			initializeRefFromUI();
		return super.getPreviousPage();
	}

	/*
	 * @see RefactoringWizardPage#performFinish()
	 */
	protected boolean performFinish() {
		initializeRefFromUI();
		return super.performFinish();
	}

	// ------------ Helper

	private static boolean isDerivedElement(Object element) {
		return ( (element instanceof IMethod) || (element instanceof IField) || (element instanceof ILocalVariable));
	}

	private void treeViewerSelectionChanged(SelectionChangedEvent event) {
		try {
			final IJavaElement selection= getFirstSelectedSourceReference(event);
			setSourceViewerContents(selection);
			fEditElementButton.setEnabled(selection != null && (isDerivedElement(selection)));
			fCurrentElementLabel.setText(selection != null ? JavaElementLabels.getElementLabel(selection, LABEL_FLAGS) : RefactoringMessages.RenameTypeWizardDerivedElementPage_select_element_to_view_source);
			fCurrentElementLabel.setImage(selection != null ? fTreeViewerLabelProvider.getJavaImage(selection) : null);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.RenameTypeWizard_defaultPageTitle, RefactoringMessages.RenameTypeWizard_unexpected_exception);
		}
	}

	private IJavaElement getFirstSelectedSourceReference(SelectionChangedEvent event) {
		ISelection s= event.getSelection();
		if (! (s instanceof IStructuredSelection))
			return null;
		IStructuredSelection strSel= (IStructuredSelection) s;
		if (strSel.size() != 1)
			return null;
		Object first= strSel.getFirstElement();
		if (! (first instanceof IJavaElement))
			return null;
		return (IJavaElement) first;
	}

	private void setSourceViewerContents(IJavaElement el) throws JavaModelException {
		if (el == null) {
			fSourceViewer.setDocument(null);
			return;
		}
		ICompilationUnit element= (ICompilationUnit) el.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (element == null) {
			fSourceViewer.setDocument(null);
			return;
		}
		String contents= element.getSource();
		IDocument document= (contents == null) ? new Document() : new Document(contents);
		getJavaTextTools().setupJavaDocumentPartitioner(document);
		fSourceViewer.setDocument(document);

		ISourceRange sr= getNameRange(el);
		if (sr != null) {
			TextSelection textSelect= new TextSelection(getNameRange(el).getOffset(), getNameRange(el).getLength());
			fSourceViewer.setSelection(textSelect, true);
		}
	}

	private ISourceRange getNameRange(IJavaElement element) throws JavaModelException {
		if (element instanceof IMember)
			return ((IMember) element).getNameRange();
		else if (element instanceof ILocalVariable)
			return ((ILocalVariable) element).getNameRange();
		else
			return null;
	}

	private IJavaElement[] getCheckedDerivedElements() {
		Object[] checked= fTreeViewer.getCheckedElements();
		List elements= new ArrayList(checked.length);
		for (int i= 0; i < checked.length; i++) {
			if (isDerivedElement(checked[i]))
				elements.add(checked[i]);
		}
		return (IJavaElement[]) elements.toArray(new IJavaElement[elements.size()]);
	}

	public RenameTypeProcessor getRenameTypeProcessor() {
		RefactoringProcessor proc= ((RenameRefactoring) getRefactoring()).getProcessor();
		if (proc instanceof RenameTypeProcessor)
			return (RenameTypeProcessor) proc;
		else if (proc instanceof RenameCompilationUnitProcessor) {
			RenameCompilationUnitProcessor rcu= (RenameCompilationUnitProcessor) proc;
			return rcu.getRenameTypeProcessor();
		}
		Assert.isTrue(false); // Should never get here
		return null;
	}
}
