/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;


public class GotoErrorAction extends TextEditorAction {
		
	private boolean fForward;
	
	public GotoErrorAction(String prefix, boolean forward) {
		super(JavaEditorMessages.getResourceBundle(), prefix, null);
		fForward= forward;
	}
	
	public void run() {
		JavaEditor e= (JavaEditor) getTextEditor();
		e.gotoError(fForward);
	}
	
	public void setEditor(ITextEditor editor) {
		if (editor instanceof JavaEditor) 
			super.setEditor(editor);
		update();
	}
	
	public void update() {
		setEnabled(getTextEditor() instanceof JavaEditor);
	}
}