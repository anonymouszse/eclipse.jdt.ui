/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class ExtensionManager {
	
	private String fName;
	
	private String fProcessorID;
	private static final int MAX_ENTRIES= 20;
	private LinkedList fLRUProcessors= new LinkedList();
	private List fProcessors= new ArrayList(5);
	
	private String fParticipantID;
	private List fParticipants= new ArrayList(20);
	
	//---- debuging----------------------------------------
	private static final boolean EXIST_TRACING;
	static {
		String value= Platform.getDebugOption("org.eclipse.jdt.ui/processor/existTracing"); //$NON-NLS-1$
		EXIST_TRACING= value != null && value.equalsIgnoreCase("true"); //$NON-NLS-1$
	}
	
	public ExtensionManager(String name, String processorId, String participantId) {
		Assert.isNotNull(name);
		Assert.isNotNull(processorId);
		Assert.isNotNull(participantId);
		fName= name;
		fProcessorID= processorId;
		fParticipantID= participantId;
		init();
	}
	
	public boolean hasProcessor(Object[] elements) throws CoreException {
		// check last recently used processors
		long start= 0;
		if (EXIST_TRACING)
			start= System.currentTimeMillis();
		for (Iterator iter= fLRUProcessors.iterator(); iter.hasNext();) {
			ProcessorDescriptor descriptor= (ProcessorDescriptor)iter.next();
			if (descriptor.matches(elements)) {
				if (fLRUProcessors.getFirst() != descriptor) {
					iter.remove();
					fLRUProcessors.addFirst(descriptor);
				}
				if (EXIST_TRACING)
					printTime(start);
				return true;
			}
		}
		// now check normal list of processors
		for (Iterator iter= fProcessors.iterator(); iter.hasNext();) {
			ProcessorDescriptor descriptor= (ProcessorDescriptor)iter.next();
			if (descriptor.matches(elements)) {
				if (fLRUProcessors.size() >= MAX_ENTRIES) {
					fLRUProcessors.removeLast();
				}
				fLRUProcessors.addFirst(descriptor);
				if (EXIST_TRACING)
					printTime(start);
				return true;
			}
		}
		if (EXIST_TRACING)
			printTime(start);
		return false;
	}
	
	private void printTime(long start) {
		System.out.println("[" + fName +  //$NON-NLS-1$
			" extension manager] - existing test: " +  //$NON-NLS-1$
			(System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$
	}
	
	public IRefactoringProcessor getProcessor(Object[] elements) throws CoreException {
		List selected= new ArrayList();
		for (Iterator p= fProcessors.iterator(); p.hasNext();) {
			ProcessorDescriptor ce= (ProcessorDescriptor)p.next();
			if (ce.matches(elements)) {
				selected.add(ce);
			}
		}
		if (selected.size() == 0)
			return null;
			
		if (selected.size() == 1) {
			return createProcessor((ProcessorDescriptor)selected.get(0), elements);
		} else {
			Comparator sorter= new Comparator() {
				public int compare(Object o1, Object o2) {
					ProcessorDescriptor d1= (ProcessorDescriptor)o1;
					ProcessorDescriptor d2= (ProcessorDescriptor)o2;
				
					String oid1= d1.getOverrideId();
					String oid2= d2.getOverrideId();
				
					if (oid1 != null && oid2 == null) {
						if (oid1.equals(d2.getId()))
							return 1;
					} else if (oid1 == null && oid2 != null) {
						if (oid2.equals(d1.getId()))
							return -1;
					}
					return 0;
				}
				public boolean equals(Object obj) {
					return super.equals(obj);
				}
			};
		
			Collections.sort(selected, sorter);
			for (int i= selected.size() - 1; i >= 0; i--) {
				IRefactoringProcessor result= createProcessor((ProcessorDescriptor)selected.get(i), elements);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	
	public IRefactoringParticipant[] getParticipants(IRefactoringProcessor processor, Object[] elements, SharableParticipants shared) throws CoreException {
		List result= new ArrayList();
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			for (Iterator iter= fParticipants.iterator(); iter.hasNext();) {
				ParticipantDescriptor descriptor= (ParticipantDescriptor)iter.next();
				if (descriptor.matches(processor, element)) {
					IRefactoringParticipant participant= (IRefactoringParticipant)shared.get(descriptor);
					if (participant != null) {
						((ISharableParticipant)participant).addElement(element);
					} else {
						participant= descriptor.createParticipant();
						participant.initialize(processor, element);
						if (participant.isAvailable()) {
							result.add(participant);
							if (participant instanceof ISharableParticipant)
								shared.put(descriptor, participant);
						}
					}
				}
			}
		}
		
		return (IRefactoringParticipant[])result.toArray(new IRefactoringParticipant[result.size()]);
	}

	private void init() {
		IPluginRegistry registry= Platform.getPluginRegistry();
		IConfigurationElement[] ces= registry.getConfigurationElementsFor(
			JavaPlugin.getPluginId(), 
			fProcessorID);
		for (int i= 0; i < ces.length; i++) {
			fProcessors.add(new ProcessorDescriptor(ces[i]));
		}
		
		ces= registry.getConfigurationElementsFor(
			JavaPlugin.getPluginId(), 
			fParticipantID);
		for (int i= 0; i < ces.length; i++) {
			ParticipantDescriptor descriptor= new ParticipantDescriptor(ces[i]);
			IStatus status= descriptor.checkSyntax();
			switch (status.getSeverity()) {
				case IStatus.ERROR:
					JavaPlugin.log(status);
					break;
				case IStatus.WARNING:
				case IStatus.INFO:
					JavaPlugin.log(status);
					// fall through
				default:
					fParticipants.add(descriptor);
			}
		}
	}
	
	private IRefactoringProcessor createProcessor(ProcessorDescriptor processor, Object[] elements) throws CoreException {
		IRefactoringProcessor result= processor.createProcessor();
		result.initialize(elements);
		if (result.isAvailable())
			return result;
		return null;
	}
}
