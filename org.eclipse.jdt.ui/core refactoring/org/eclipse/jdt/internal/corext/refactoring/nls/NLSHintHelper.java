/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

public class NLSHintHelper {

	private NLSHintHelper() {
	}

	/**
	 * Returns the accessor binding info or <code>null</code> if this element is not a nls'ed entry
	 */
	public static AccessorClassReference getAccessorClassReference(CompilationUnit astRoot, NLSElement nlsElement) {
		IRegion region= nlsElement.getPosition();
		return getAccessorClassReference(astRoot, region);
	}
	
	/**
	 * Returns the accessor binding info or <code>null</code> if this element is not a nls'ed entry
	 */
	public static AccessorClassReference getAccessorClassReference(CompilationUnit astRoot, IRegion region) {
		ASTNode nlsStringLiteral= NodeFinder.perform(astRoot, region.getOffset(), region.getLength());
		if (nlsStringLiteral == null) {
			return null; // not found
		}
		ASTNode parent= nlsStringLiteral.getParent();
		if (!(parent instanceof MethodInvocation)) {
			return null;
		}
		
		MethodInvocation methodInvocation= (MethodInvocation) parent;
		List args= methodInvocation.arguments();
		if (args.indexOf(nlsStringLiteral) != 0) {
			return null; // must be first argument in lookup method
		}
			
		IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
		if (methodBinding == null || !Modifier.isStatic(methodBinding.getModifiers())) {
			return null; // only static methods qualify
		}

		ITypeBinding accessorBinding= methodBinding.getDeclaringClass();
		if (isAccessorCandidate(accessorBinding)) {
			return new AccessorClassReference(accessorBinding, new Region(parent.getStartPosition(), parent.getLength()));
		}
		return null;
	}

	private static boolean isAccessorCandidate(ITypeBinding binding) {
		IVariableBinding[] fields= binding.getDeclaredFields();
		for (int i= 0; i < fields.length; i++) {
			if (isBundleField(fields[i]))
				return true;
		}
		return false;
	}
	
	private static boolean isBundleField(IVariableBinding field) {
		if (field == null)
			return false;
		
		String name= field.getName();
		return name.equals("BUNDLE_NAME") || name.equals("RESOURCE_BUNDLE") || name.equals("bundleName"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static IPackageFragment getPackageOfAccessorClass(IJavaProject javaProject, ITypeBinding accessorBinding) throws JavaModelException {
		if (accessorBinding != null) {
			ICompilationUnit unit= Bindings.findCompilationUnit(accessorBinding, javaProject);
			if (unit != null) {
				return (IPackageFragment) unit.getParent();
			}
		}
		return null;
	}

	public static String getResourceBundleName(IJavaProject javaProject, ITypeBinding accessorClassBinding) throws JavaModelException {
		IJavaElement je= accessorClassBinding.getJavaElement();
		if (je == null)
			return null;
		
		IOpenable openable= je.getOpenable();
		IJavaElement container= null;
		if (openable instanceof ICompilationUnit)
			container= (ICompilationUnit)openable;
		else if (openable instanceof IClassFile)
			container= (IClassFile)openable;
		else
			Assert.isLegal(false);
		
		CompilationUnit astRoot= JavaPlugin.getDefault().getASTProvider().getAST(container, ASTProvider.WAIT_YES, null);
		
		IVariableBinding[] fields= accessorClassBinding.getDeclaredFields();
		for (int i= 0; i < fields.length; i++) {
			if (isBundleField(fields[i])) {
				VariableDeclarationFragment node= (VariableDeclarationFragment) astRoot.findDeclaringNode(fields[i].getKey());
				if (node != null) {
					Expression initializer= node.getInitializer();
					if (initializer instanceof StringLiteral) {
						return ((StringLiteral) initializer).getLiteralValue();
					} else if (initializer instanceof MethodInvocation) {
						MethodInvocation methInvocation= (MethodInvocation) initializer;
						Expression exp= methInvocation.getExpression();
						if ((exp != null) && (exp instanceof TypeLiteral)) {
							SimpleType simple= (SimpleType) ((TypeLiteral) exp).getType();
							ITypeBinding typeBinding= simple.resolveBinding();
							if (typeBinding != null) {
								return Bindings.getRawQualifiedName(typeBinding);
							}
						}
					}
				}
			}
		}
		return null;
	}

	public static IPackageFragment getResourceBundlePackage(IJavaProject javaProject, String packageName, String resourceName) throws JavaModelException {
		IPackageFragmentRoot[] allRoots= javaProject.getAllPackageFragmentRoots();
		for (int i= 0; i < allRoots.length; i++) {
			IPackageFragmentRoot root= allRoots[i];
			if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
				IPackageFragment packageFragment= root.getPackageFragment(packageName);
				if (packageFragment.exists()) {
					Object[] resources= packageFragment.isDefaultPackage() ? root.getNonJavaResources() : packageFragment.getNonJavaResources();
					for (int j= 0; j < resources.length; j++) {
						Object object= resources[j];
						if (object instanceof IFile) {
							IFile file= (IFile) object;
							if (file.getName().equals(resourceName)) {
								return packageFragment;
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	public static IStorage getResourceBundle(IJavaProject javaProject, String packageName, String resourceName) throws JavaModelException {
		IPackageFragmentRoot[] allRoots= javaProject.getAllPackageFragmentRoots();
		for (int i= 0; i < allRoots.length; i++) {
			IPackageFragmentRoot root= allRoots[i];
			if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
				IStorage storage= getResourceBundle(root, packageName, resourceName);
				if (storage != null)
					return storage;
			}
		}
		return null;
	}
	
	public static IStorage getResourceBundle(IPackageFragmentRoot root, String packageName, String resourceName) throws JavaModelException {
		IPackageFragment packageFragment= root.getPackageFragment(packageName);
		if (packageFragment.exists()) {
			Object[] resources= packageFragment.isDefaultPackage() ? root.getNonJavaResources() : packageFragment.getNonJavaResources();
			for (int j= 0; j < resources.length; j++) {
				Object object= resources[j];
				if (object instanceof IStorage) {
					IStorage storage= (IStorage)object;
					if (storage.getName().equals(resourceName)) {
						return storage;
					}
				}
			}
		}
		return null;
	}

	public static IStorage getResourceBundle(IJavaProject javaProject, ITypeBinding accessorClass) throws JavaModelException {
		String resourceBundle= getResourceBundleName(javaProject, accessorClass);
		if (resourceBundle == null) {
			return null;
		}
		
		String resourceName= Signature.getSimpleName(resourceBundle) + NLSRefactoring.PROPERTY_FILE_EXT;
		String packName= Signature.getQualifier(resourceBundle);
		
		if (accessorClass.isFromSource())
			return getResourceBundle(javaProject, packName, resourceName);
		else if (accessorClass.getJavaElement() != null)
			return getResourceBundle((IPackageFragmentRoot)accessorClass.getJavaElement().getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT), packName, resourceName);
		
		return null;
	}
	
	/**
	 * Reads the properties from the given storage and
	 * returns it.
	 * 
	 * @param javaProject the Java project
	 * @param accessorBinding the accessor binding
	 * @return the properties or <code>null</code> if it was not successfully read
	 */
	public static Properties getProperties(IJavaProject javaProject, ITypeBinding accessorBinding) {
		try {
			IStorage storage= NLSHintHelper.getResourceBundle(javaProject, accessorBinding);
			return getProperties(storage);
		} catch (JavaModelException ex) {
			// sorry no properties
			return null;
		}
	}
	
	/**
	 * Reads the properties from the given storage and
	 * returns it.
	 * 
	 * @param storage the storage
	 * @return the properties or <code>null</code> if it was not successfully read
	 */
	public static Properties getProperties(IStorage storage) {
		if (storage == null)
			return null;

		Properties props= new Properties();
		InputStream is= null;
		
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		try {
			if (manager != null) {
				ITextFileBuffer buffer= manager.getTextFileBuffer(storage.getFullPath());
				if (buffer != null) {
					IDocument document= buffer.getDocument();
					is= new ByteArrayInputStream(document.get().getBytes());
				}
			}
			
			// Fallback: read from storage
			if (is == null)
				is= storage.getContents();
			
			props.load(is);
			
		} catch (IOException e) {
			// sorry no properties
			return null;
		} catch (CoreException e) {
			// sorry no properties
			return null;
		} finally {
			if (is != null) try {
				is.close();
			} catch (IOException e) {
				// return properties anyway but log
				JavaPlugin.log(e);
			}
		}
		return props;
	}

}
