/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.ui.JavaUIMessages;

public class MultiElementListSelectionDialog extends AbstractElementListSelectionDialog {
		
	private Object[][] fElements;
	private int[][] fSelectedIndices;
	
	private boolean[] fPagesOKStates;
		
	private int fCurrentPage;
	private int fNumberOfPages;
	
	private Button fFinishButton;
	private Button fBackButton;
	private Button fNextButton;
	
	private Label fPageInfoLabel;
	private String fPageInfoMessage= JavaUIMessages.getString("MultiElementListSelectionDialog.pageInfoMessage"); //$NON-NLS-1$;
	
	/**
	 * Constructs a list selection dialog.
	 * @param renderer The label renderer used
	 * @param ignoreCase Decides if the match string ignores lower/upppr case
	 * @param multipleSelection Allow multiple selection	 
	 */
	public MultiElementListSelectionDialog(Shell parent, ILabelProvider renderer) {
		super(parent, renderer);
	}
	
	/**
	 * Sets message shown in right top corner. Use {0} and {1} as placeholders
	 * for the current and the total number of pages.
	 */
	public void setPageInfoMessage(String message) {
		fPageInfoMessage= message;
	}
	
	/**
	 * Sets the elements to be shown in the dialog.
	 */
	public void setElements(Object[][] elements) {
		fElements= elements;
		fNumberOfPages= elements.length;			
		fPagesOKStates= new boolean[fNumberOfPages]; // all initialized with false
		fSelectedIndices= new int[fNumberOfPages][]; // all initialized with null
		initializeResult(fNumberOfPages);
	}

	public int open() {
		List selection= getInitialSelections();
		if (selection == null) {
			setInitialSelections(new String[fNumberOfPages]);
			selection= getInitialSelections();
		}
			
		Assert.isTrue(selection.size() == fNumberOfPages);
		
		return super.open();
	}
		 
	/**
	 * @private
	 */	
	protected Control createDialogArea(Composite parent) {
		Composite contents= (Composite) super.createDialogArea(parent);

		createMessageArea(contents);
		createFilterText(contents);
		createFilteredList(contents);

		initFilteredList();
		initFilterText();		
				
		fCurrentPage= 0;
		setPageData();
		
		return contents;
	}

	/**
	 * @private
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		fBackButton= createButton(parent, IDialogConstants.BACK_ID, IDialogConstants.BACK_LABEL, false);
		fNextButton= createButton(parent, IDialogConstants.NEXT_ID, IDialogConstants.NEXT_LABEL, true);
		fFinishButton= createButton(parent, IDialogConstants.OK_ID, IDialogConstants.FINISH_LABEL, false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	/**
	 * @private
	 */
	protected Label createMessageArea(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);

		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.horizontalSpacing= 5;
		layout.numColumns= 2;
		composite.setLayout(layout);
		
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		composite.setLayoutData(data);
		
		Label messageLabel= super.createMessageArea(composite);
		
		fPageInfoLabel= new Label(composite, SWT.NULL);
		fPageInfoLabel.setText(getPageInfoMessage());
		
		data= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalAlignment= data.END;
		fPageInfoLabel.setLayoutData(data);
		
		return messageLabel;
	}
	
	/**
	 * @private
	 */
	protected void computeResult() {
		setResult(fCurrentPage, getWidgetSelection());
	}
		
	/**
	 * @private
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.BACK_ID) {
			turnPage(false);
		} else if (buttonId == IDialogConstants.NEXT_ID) {
			turnPage(true);
		} else {
			super.buttonPressed(buttonId);
		}			
	}
	
	/**
	 *
	 */
	protected void handleDefaultSelected() {
		if (verifyCurrentSelection()) {
			if (fCurrentPage == fNumberOfPages - 1) {
				buttonPressed(IDialogConstants.OK_ID);
			} else {
				buttonPressed(IDialogConstants.NEXT_ID);
			}
		}
	}
	
	/**
	 * @private
	 * @see AbstractElementListSelectionDialog#updateButtonsEnableState
	 */
	protected void updateButtonsEnableState(IStatus status) {
		boolean isOK= !status.matches(IStatus.ERROR);
		fPagesOKStates[fCurrentPage]= isOK;

		fNextButton.setEnabled(isOK && (fCurrentPage < fNumberOfPages - 1));
		fBackButton.setEnabled(fCurrentPage != 0);
		
		boolean isAllOK= isOK;
		int i= 0;
		while (isAllOK && i < fNumberOfPages) {
			isAllOK &= fPagesOKStates[i++];
		}
		
		fFinishButton.setEnabled(isAllOK);
	}

	protected void setInitialSelection(int position, Object element) {
		List list= getInitialSelections();
		list.set(position, element);
	}
	
	private void turnPage(boolean toNextPage) {
		setResult(fCurrentPage, getWidgetSelection());
		setInitialSelection(fCurrentPage, getFilter());
		fSelectedIndices[fCurrentPage]= getSelectionIndices();
		
		if (toNextPage) {
			if (fCurrentPage < fNumberOfPages - 1) {
				fCurrentPage++;
			} else {
				return;
			}
		} else {
			if (fCurrentPage > 0) {
				fCurrentPage--;
			} else {
				return;
			}
		}
		
		if (fPageInfoLabel != null && !fPageInfoLabel.isDisposed())
			fPageInfoLabel.setText(getPageInfoMessage());
		
		setPageData();		
		
		verifyCurrentSelection();
	}
	
	private void setPageData() {
		setSelectionListElements(fElements[fCurrentPage]);

		String filter= (String) getInitialSelections().get(fCurrentPage);
		if (filter == null)
			filter= ""; //$NON-NLS-1$
		setFilter(filter);
			
		int[] selectedIndex= fSelectedIndices[fCurrentPage];
		if (selectedIndex != null)
			setSelection(selectedIndex);
	}
	
	private String getPageInfoMessage() {
		if (fPageInfoMessage == null)
			return ""; //$NON-NLS-1$
			
		String[] args= new String[] { Integer.toString(fCurrentPage + 1), Integer.toString(fNumberOfPages) };	
		return MessageFormat.format(fPageInfoMessage, args);
	}
		
	private void initializeResult(int length) {
		List result= new ArrayList(length);
		for (int i= 0; i != length; i++)
			result.add(null);

		setResult(result);
	}
	
}