package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.text.Assert;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.internal.model.WorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.ProblemsLabelDecorator.ProblemsLabelChangedEvent;
import org.eclipse.jdt.ui.actions.CCPActionGroup;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.actions.MemberFilterActionGroup;
import org.eclipse.jdt.ui.actions.OpenViewActionGroup;
import org.eclipse.jdt.ui.actions.RefactorActionGroup;
import org.eclipse.jdt.ui.actions.ShowActionGroup;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.dnd.DelegatingDragAdapter;
import org.eclipse.jdt.internal.ui.dnd.DelegatingDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.LocalSelectionTransfer;
import org.eclipse.jdt.internal.ui.dnd.TransferDragSourceListener;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;
import org.eclipse.jdt.internal.ui.packageview.SelectionTransferDragAdapter;
import org.eclipse.jdt.internal.ui.packageview.SelectionTransferDropAdapter;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.StatusBarUpdater;





/**
 * The content outline page of the Java editor. The viewer implements a proprietary
 * update mechanism based on Java model deltas. It does not react on domain changes.
 * It is specified to show the content of ICompilationUnits and IClassFiles.
 * Pulishes its context menu under <code>JavaPlugin.getDefault().getPluginId() + ".outline"</code>.
 */
class JavaOutlinePage extends Page implements IContentOutlinePage {

			static Object[] NO_CHILDREN= new Object[0];
   
			/**
			 * The element change listener of the java outline viewer.
			 * @see IElementChangedListener
			 */
			class ElementChangedListener implements IElementChangedListener {
				
				public void elementChanged(final ElementChangedEvent e) {
					
					if (getControl() == null)
						return;
						
					Display d= getControl().getDisplay();
					if (d != null) {
						d.asyncExec(new Runnable() {
							public void run() {
								ICompilationUnit cu= (ICompilationUnit) fInput;
								IJavaElement base= cu;
								if (fTopLevelTypeOnly) {
									base= getMainType(cu);
									if (base == null) {
										if (fOutlineViewer != null)
											fOutlineViewer.refresh();
										return;
									}
								}
								IJavaElementDelta delta= findElement(base, e.getDelta());
								if (delta != null && fOutlineViewer != null) {
									fOutlineViewer.reconcile(delta);
								}
							}
						});
					}
				}
				
				protected IJavaElementDelta findElement(IJavaElement unit, IJavaElementDelta delta) {
					
					if (delta == null || unit == null)
						return null;
					
					IJavaElement element= delta.getElement();
					
					if (unit.equals(element))
						return delta;
					
					if (element.getElementType() > IJavaElement.CLASS_FILE)
						return null;
						
					IJavaElementDelta[] children= delta.getAffectedChildren();
					if (children == null || children.length == 0)
						return null;
						
					for (int i= 0; i < children.length; i++) {
						IJavaElementDelta d= findElement(unit, children[i]);
						if (d != null)
							return d;
					}
					
					return null;
				}
			};
         
			static class NoClassElement extends WorkbenchAdapter implements IAdaptable {
				/*
				 * @see java.lang.Object#toString()				 */
				public String toString() {
					return JavaEditorMessages.getString("JavaOutlinePage.error.NoTopLevelType"); //$NON-NLS-1$
				}
		
				/*
				 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(Class)				 */
				public Object getAdapter(Class clas) {
					if (clas == IWorkbenchAdapter.class)
						return this;
					return null;
				}
			}
			
			/**
			 * Content provider for the children of an ICompilationUnit or
			 * an IClassFile
			 * @see ITreeContentProvider
			 */
			class ChildrenProvider implements ITreeContentProvider {
            
				private Object[] NO_CLASS= new Object[] {new NoClassElement()};
				private ElementChangedListener fListener;
				
				protected boolean matches(IJavaElement element) {
					if (element.getElementType() == IJavaElement.METHOD) {
						String name= element.getElementName();
						return (name != null && name.indexOf('<') >= 0);
					}
					return false;
				}
				
				protected IJavaElement[] filter(IJavaElement[] children) {
					boolean initializers= false;
					for (int i= 0; i < children.length; i++) {
						if (matches(children[i])) {
							initializers= true;
							break;
						}
					}
							
					if (!initializers)
						return children;
						
					Vector v= new Vector();
					for (int i= 0; i < children.length; i++) {
						if (matches(children[i]))
							continue;
						v.addElement(children[i]);
					}
					
					IJavaElement[] result= new IJavaElement[v.size()];
					v.copyInto(result);
					return result;
				}
				
				public Object[] getChildren(Object parent) {
					if (parent instanceof IParent) {
						IParent c= (IParent) parent;
						try {
							return filter(c.getChildren());
						} catch (JavaModelException x) {
							JavaPlugin.log(x);
						}
					}
					return NO_CHILDREN;
				}
				
				public Object[] getElements(Object parent) {
					if (fTopLevelTypeOnly) {
						if (parent instanceof ICompilationUnit) {
							try {
								IType type= getMainType((ICompilationUnit) parent);
								return type != null ? type.getChildren() : NO_CLASS;
							} catch (JavaModelException e) {
								JavaPlugin.log(e);
							}
						} else if (parent instanceof IClassFile) {
							try {
								IType type= getMainType((IClassFile) parent);
								return type != null ? type.getChildren() : NO_CLASS;
							} catch (JavaModelException e) {
								JavaPlugin.log(e);
							}							
						}
					}
					return getChildren(parent);
				}
				
				public Object getParent(Object child) {
					if (child instanceof IJavaElement) {
						IJavaElement e= (IJavaElement) child;
						return e.getParent();
					}
					return null;
				}
				
				public boolean hasChildren(Object parent) {
					if (parent instanceof IParent) {
						IParent c= (IParent) parent;
						try {
							IJavaElement[] children= filter(c.getChildren());
							return (children != null && children.length > 0);
						} catch (JavaModelException x) {
							JavaPlugin.log(x);
						}
					}
					return false;
				}
				
				public boolean isDeleted(Object o) {
					return false;
				}
				
				public void dispose() {
					if (fListener != null) {
						JavaCore.removeElementChangedListener(fListener);
						fListener= null;
					}		
				}
				
				/*
				 * @see IContentProvider#inputChanged(Viewer, Object, Object)
				 */
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
					boolean isCU= (newInput instanceof ICompilationUnit);
									
					if (isCU && fListener == null) {
						fListener= new ElementChangedListener();
						JavaCore.addElementChangedListener(fListener);
					} else if (!isCU && fListener != null) {
						JavaCore.removeElementChangedListener(fListener);
						fListener= null;
					}
				}
			};
			
			
			class JavaOutlineViewer extends TreeViewer {
				
				/**
				 * Indicates an item which has been reused. At the point of
				 * its reuse it has been expanded. This field is used to
				 * communicate between <code>internalExpandToLevel</code> and
				 * <code>reuseTreeItem</code>.
				 */
				private Item fReusedExpandedItem;
				
				public JavaOutlineViewer(Tree tree) {
					super(tree);
					setAutoExpandLevel(ALL_LEVELS);
				}
				
				/**
				 * Investigates the given element change event and if affected incrementally
				 * updates the outline.
				 */
				public void reconcile(IJavaElementDelta delta) {
					if (getSorter() == null) {
						if (fTopLevelTypeOnly
							&& delta.getElement() instanceof IType
							&& (delta.getKind() & IJavaElementDelta.ADDED) != 0)
						{
							refresh();

						} else {
							Widget w= findItem(fInput);
							if (w != null && !w.isDisposed())
								update(w, delta);
						}
					} else {
						// just for now
						refresh();
					}
				}
				
				/*
				 * @see TreeViewer#internalExpandToLevel
				 */
				protected void internalExpandToLevel(Widget node, int level) {
					if (node instanceof Item) {
						Item i= (Item) node;
						if (i.getData() instanceof IJavaElement) {
							IJavaElement je= (IJavaElement) i.getData();
							if (je.getElementType() == IJavaElement.IMPORT_CONTAINER || isInnerType(je)) {
								if (i != fReusedExpandedItem) {
									setExpanded(i, false);
									return;
								}
							}
						}
					}
					super.internalExpandToLevel(node, level);
				}
								
				protected void reuseTreeItem(Item item, Object element) {
					
					// remove children
					Item[] c= getChildren(item);
					if (c != null && c.length > 0) {
						
						if (getExpanded(item))
							fReusedExpandedItem= item;
						
						for (int k= 0; k < c.length; k++) {
							if (c[k].getData() != null)
								disassociate(c[k]);
							c[k].dispose();
						}
					}
					
					updateItem(item, element);
					updatePlus(item, element);					
					internalExpandToLevel(item, ALL_LEVELS);
					
					fReusedExpandedItem= null;
				}
				
				protected boolean mustUpdateParent(IJavaElementDelta delta, IJavaElement element) {
					if (element instanceof IMethod) {
						if ((delta.getKind() & IJavaElementDelta.ADDED) != 0) {
							try {
								return ((IMethod)element).isMainMethod();
							} catch (JavaModelException e) {
								JavaPlugin.log(e.getStatus());
							}
						}
						return "main".equals(element.getElementName()); //$NON-NLS-1$
					}
					return false;
				}
				
				protected ISourceRange getSourceRange(IJavaElement element) throws JavaModelException {
					if (element instanceof IMember && !(element instanceof IInitializer))
						return ((IMember) element).getNameRange();
					if (element instanceof ISourceReference)
						return ((ISourceReference) element).getSourceRange();
					return null;
				}
				
				protected boolean overlaps(ISourceRange range, int start, int end) {
					return start <= (range.getOffset() + range.getLength() - 1) && range.getOffset() <= end;
				}
				
				protected boolean filtered(IJavaElement parent, IJavaElement child) {
					
					Object[] result= new Object[] { child };
					ViewerFilter[] filters= getFilters();
					for (int i= 0; i < filters.length; i++) {
						result= filters[i].filter(this, parent, result);
						if (result.length == 0)
							return true;
					}
					
					return false;
				}
				
				protected void update(Widget w, IJavaElementDelta delta) {
					
					Item item;
					
					IJavaElement parent= delta.getElement();
					IJavaElementDelta[] affected= delta.getAffectedChildren();
					Item[] children= getChildren(w);

					boolean doUpdateParent= false;
										
					Vector deletions= new Vector();
					Vector additions= new Vector();				

					for (int i= 0; i < affected.length; i++) {
					    IJavaElementDelta affectedDelta= affected[i];
						IJavaElement affectedElement= affectedDelta.getElement();
						int status= affected[i].getKind();

						// find tree item with affected element
						int j;
						for (j= 0; j < children.length; j++)
						    if (affectedElement.equals(children[j].getData()))
						    	break;
						
						if (j == children.length) {
							// addition
							if ((status & IJavaElementDelta.CHANGED) != 0 &&							
								(affectedDelta.getFlags() & IJavaElementDelta.F_MODIFIERS) != 0 &&
								!filtered(parent, affectedElement))
							{
								additions.addElement(affectedDelta);
							}
							continue;
						}

						item= children[j];

						// removed						    
						if ((status & IJavaElementDelta.REMOVED) != 0) {
							deletions.addElement(item);
							doUpdateParent= doUpdateParent || mustUpdateParent(affectedDelta, affectedElement);

						// changed						    
						} else if ((status & IJavaElementDelta.CHANGED) != 0) {
							int change= affectedDelta.getFlags();
							doUpdateParent= doUpdateParent || mustUpdateParent(affectedDelta, affectedElement);
							
							if ((change & IJavaElementDelta.F_MODIFIERS) != 0) {
								if (filtered(parent, affectedElement))
									deletions.addElement(item);
								else
									updateItem(item, affectedElement);
							}
							
							if ((change & IJavaElementDelta.F_CONTENT) != 0)
								updateItem(item, affectedElement);
								
							if ((change & IJavaElementDelta.F_CHILDREN) != 0)
								update(item, affectedDelta);															    
						}
					}
					
					// find all elements to add
					IJavaElementDelta[] add= delta.getAddedChildren();
					if (additions.size() > 0) {
						IJavaElementDelta[] tmp= new IJavaElementDelta[add.length + additions.size()];
						System.arraycopy(add, 0, tmp, 0, add.length);
						for (int i= 0; i < additions.size(); i++)
							tmp[i + add.length]= (IJavaElementDelta) additions.elementAt(i);
						add= tmp;
					}
					
					// add at the right position
					go2: for (int i= 0; i < add.length; i++) {
						
						try {
							
							IJavaElement e= add[i].getElement();
							if (filtered(parent, e))
								continue go2;
								
							doUpdateParent= doUpdateParent || mustUpdateParent(add[i], e);
							ISourceRange rng= getSourceRange(e);
							int start= rng.getOffset();
							int end= start + rng.getLength() - 1;
							
							Item last= null;
							item= null;
							children= getChildren(w);
							
							for (int j= 0; j < children.length; j++) {
								item= children[j];
								IJavaElement r= (IJavaElement) item.getData();
								
								if (r == null) {
									// parent node collapsed and not be opened before -> do nothing
									continue go2;
								}
								
									
								try {
									rng= getSourceRange(r);
									if (overlaps(rng, start, end)) {
										
										// be tolerant if the delta is not correct, or if 
										// the tree has been updated other than by a delta
										reuseTreeItem(item, e);
										continue go2;
										
									} else if (rng.getOffset() > start) {
										
										if (last != null && deletions.contains(last)) {
											// reuse item
											deletions.removeElement(last);
											reuseTreeItem(last, (Object) e);
										} else {
											// nothing to reuse
											createTreeItem(w, (Object) e, j);
										}
										continue go2;
									}
									
								} catch (JavaModelException x) {
									// stumbled over deleted element
								}
								
								last= item;
							}
						
							// add at the end of the list
							if (last != null && deletions.contains(last)) {
								// reuse item
								deletions.removeElement(last);
								reuseTreeItem(last, e);
							} else {
								// nothing to reuse
								createTreeItem(w, e, -1);
							}
						
						} catch (JavaModelException x) {
							// the element to be added is not present -> don't add it
						}
					}
					
					
					// remove items which haven't been reused
					Enumeration e= deletions.elements();
					while (e.hasMoreElements()) {
						item= (Item) e.nextElement();
						disassociate(item);
						item.dispose();
					}
					
					if (doUpdateParent)
						updateItem(w, delta.getElement());
				}
				

								
				/*
				 * @see ContentViewer#handleLabelProviderChanged(LabelProviderChangedEvent)
				 */
				protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
					Object input= getInput();
					if (event instanceof ProblemsLabelChangedEvent) {
						ProblemsLabelChangedEvent e= (ProblemsLabelChangedEvent) event;
						if (e.isMarkerChange() && input instanceof ICompilationUnit) {
							return; // marker changes can be ignored
						}
					}
					// look if the underlying resource changed
					Object[] changed= event.getElements();
					if (changed != null) {
						IResource resource= getUnderlyingResource();
						if (resource != null) {
							for (int i= 0; i < changed.length; i++) {
								if (changed[i].equals(resource)) {
									// change event to a full refresh
									event= new LabelProviderChangedEvent((IBaseLabelProvider) event.getSource());
									break;
								}
							}
						}
					}
					super.handleLabelProviderChanged(event);
				}
				
				private IResource getUnderlyingResource() {
					Object input= getInput();
					if (input instanceof ICompilationUnit) {
						ICompilationUnit cu= (ICompilationUnit) input;
						if (cu.isWorkingCopy()) {
							return cu.getOriginalElement().getResource();
						} else {
							return cu.getResource();
						}				
					} else if (input instanceof IClassFile) {
						return ((IClassFile) input).getResource();
					}
					return null;
				}				
				

			};
				
			class LexicalSortingAction extends Action {
				
				private JavaElementSorter fSorter= new JavaElementSorter();			

				public LexicalSortingAction() {
					super();
					WorkbenchHelp.setHelp(this, IJavaHelpContextIds.LEXICAL_SORTING_OUTLINE_ACTION);
					setText(JavaEditorMessages.getString("JavaOutlinePage.Sort.label")); //$NON-NLS-1$
					JavaPluginImages.setLocalImageDescriptors(this, "alphab_sort_co.gif"); //$NON-NLS-1$
					setToolTipText(JavaEditorMessages.getString("JavaOutlinePage.Sort.tooltip")); //$NON-NLS-1$
					setDescription(JavaEditorMessages.getString("JavaOutlinePage.Sort.description")); //$NON-NLS-1$
					
					boolean checked= JavaPlugin.getDefault().getPreferenceStore().getBoolean("LexicalSortingAction.isChecked"); //$NON-NLS-1$
					valueChanged(checked, false);
				}
				
				public void run() {
					valueChanged(isChecked(), true);
				}
				
				private void valueChanged(final boolean on, boolean store) {
					setChecked(on);
					BusyIndicator.showWhile(fOutlineViewer.getControl().getDisplay(), new Runnable() {
						public void run() {
							fOutlineViewer.setSorter(on ? fSorter : null);						}
					});

					if (store)
						JavaPlugin.getDefault().getPreferenceStore().setValue("LexicalSortingAction.isChecked", on); //$NON-NLS-1$
				}
			};

		class ClassOnlyAction extends Action {

			public ClassOnlyAction() {
				super();
				setText(JavaEditorMessages.getString("JavaOutlinePage.GoIntoTopLevelType.label")); //$NON-NLS-1$
				setToolTipText(JavaEditorMessages.getString("JavaOutlinePage.GoIntoTopLevelType.tooltip")); //$NON-NLS-1$
				setDescription(JavaEditorMessages.getString("JavaOutlinePage.GoIntoTopLevelType.description")); //$NON-NLS-1$
				JavaPluginImages.setLocalImageDescriptors(this, "class_obj.gif"); //$NON-NLS-1$

				IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
				boolean showclass= preferenceStore.getBoolean("GoIntoTopLevelTypeAction.isChecked"); //$NON-NLS-1$
				setTopLevelTypeOnly(showclass);
			}

			/*
			 * @see org.eclipse.jface.action.Action#run()
			 */
			public void run() {
				setTopLevelTypeOnly(!fTopLevelTypeOnly);
			}

			private void setTopLevelTypeOnly(boolean show) {
				fTopLevelTypeOnly= show;
				setChecked(show);
				fOutlineViewer.refresh();
				
				IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore(); 
				preferenceStore.setValue("GoIntoTopLevelTypeAction.isChecked", show); //$NON-NLS-1$
			}
		};

	/** A flag to show contents of top level type only */
	private boolean fTopLevelTypeOnly;
			
	private IJavaElement fInput;
	private String fContextMenuID;
	private Menu fMenu;
	private JavaOutlineViewer fOutlineViewer;
	private JavaEditor fEditor;
	
	private MemberFilterActionGroup fMemberFilterActionGroup;
		
	private ListenerList fSelectionChangedListeners= new ListenerList();
	private Hashtable fActions= new Hashtable();
	
	private TogglePresentationAction fTogglePresentation;
	private GotoErrorAction fPreviousError;
	private GotoErrorAction fNextError;
	private TextEditorAction fShowJavadoc;
	private TextOperationAction fUndo;
	private TextOperationAction fRedo;
	
	private CompositeActionGroup fActionGroups;
	private CCPActionGroup fCCPActionGroup;

	private IPropertyChangeListener fPropertyChangeListener;
	
	public JavaOutlinePage(String contextMenuID, JavaEditor editor) {
		super();
		
		Assert.isNotNull(editor);
		
		fContextMenuID= contextMenuID;
		fEditor= editor;
		
		fTogglePresentation= new TogglePresentationAction();
		fPreviousError= new GotoErrorAction("PreviousError.", false); //$NON-NLS-1$
		fPreviousError.setImageDescriptor(JavaPluginImages.DESC_TOOL_GOTO_PREV_ERROR);
		fNextError= new GotoErrorAction("NextError.", true); //$NON-NLS-1$
		fNextError.setImageDescriptor(JavaPluginImages.DESC_TOOL_GOTO_NEXT_ERROR);
		fShowJavadoc= (TextEditorAction) fEditor.getAction("ShowJavaDoc"); //$NON-NLS-1$
		fUndo= (TextOperationAction) fEditor.getAction(ITextEditorActionConstants.UNDO);
		fRedo= (TextOperationAction) fEditor.getAction(ITextEditorActionConstants.REDO);
		
		fTogglePresentation.setEditor(editor);
		fPreviousError.setEditor(editor);
		fNextError.setEditor(editor);	
		
		fPropertyChangeListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				doPropertyChange(event);
			}
		};
		JavaPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyChangeListener);
	}
   
	/**
	 * Returns the primary type of a compilation unit (has the same
	 * name as the compilation unit).
	 * 
	 * @param compilationUnit the compilation unit
	 * @return returns the primary type of the compilation unit, or
	 * <code>null</code> if is does not have one
	 */
	protected IType getMainType(ICompilationUnit compilationUnit) {
		String name= compilationUnit.getElementName();
		int index= name.indexOf('.');
		if (index != -1)
			name= name.substring(0, index);
		IType type= compilationUnit.getType(name);
		return type.exists() ? type : null;
	}

	/**
	 * Returns the primary type of a class file.
	 * 
	 * @param classFile the class file
	 * @return returns the primary type of the class file, or <code>null</code>
	 * if is does not have one
	 */
	protected IType getMainType(IClassFile classFile) {
		try {
			IType type= classFile.getType();
			return type.exists() ? type : null;
		} catch (JavaModelException e) {
			return null;	
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on Page
	 */
	public void init(IPageSite pageSite) {
		super.init(pageSite);
	}
	
	private void doPropertyChange(PropertyChangeEvent event) {
		if (fOutlineViewer != null) {
			if (PreferenceConstants.APPEARANCE_MEMBER_SORT_ORDER.equals(event.getProperty())) {
				fOutlineViewer.refresh();
			}
		}
	}	
	
	/*
	 * @see ISelectionProvider#addSelectionChangedListener(ISelectionChangedListener)
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		if (fOutlineViewer != null)
			fOutlineViewer.addPostSelectionChangedListener(listener);
		else
			fSelectionChangedListeners.add(listener);
	}
	
	/*
	 * @see ISelectionProvider#removeSelectionChangedListener(ISelectionChangedListener)
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		if (fOutlineViewer != null)
			fOutlineViewer.removePostSelectionChangedListener(listener);
		else
			fSelectionChangedListeners.remove(listener);
	}
	
	/*
	 * @see ISelectionProvider#setSelection(ISelection)
	 */
	public void setSelection(ISelection selection) {
		if (fOutlineViewer != null)
			fOutlineViewer.setSelection(selection);		
	}	
	
	/*
	 * @see ISelectionProvider#getSelection()
	 */
	public ISelection getSelection() {
		if (fOutlineViewer == null)
			return StructuredSelection.EMPTY;
		return fOutlineViewer.getSelection();
	}
	
	private void registerToolbarActions() {
		
		IToolBarManager toolBarManager= getSite().getActionBars().getToolBarManager();
		if (toolBarManager != null) {	
			toolBarManager.add(new ClassOnlyAction());		
			toolBarManager.add(new LexicalSortingAction());
			
			fMemberFilterActionGroup= new MemberFilterActionGroup(fOutlineViewer, "JavaOutlineViewer"); //$NON-NLS-1$
			fMemberFilterActionGroup.contributeToToolBar(toolBarManager);
		}
	}
	
	/*
	 * @see IPage#createControl
	 */
	public void createControl(Composite parent) {
		
		Tree tree= new Tree(parent, SWT.MULTI);

		AppearanceAwareLabelProvider lprovider= new AppearanceAwareLabelProvider(
			AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS |  JavaElementLabels.F_APP_TYPE_SIGNATURE,
			AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS
		);

		fOutlineViewer= new JavaOutlineViewer(tree);		
		fOutlineViewer.setContentProvider(new ChildrenProvider());
		fOutlineViewer.setLabelProvider(new DecoratingJavaLabelProvider(lprovider));
		
		Object[] listeners= fSelectionChangedListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			fSelectionChangedListeners.remove(listeners[i]);
			fOutlineViewer.addPostSelectionChangedListener((ISelectionChangedListener) listeners[i]);
		}
				
		MenuManager manager= new MenuManager(fContextMenuID, fContextMenuID);
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				contextMenuAboutToShow(manager);
			}
		});
		fMenu= manager.createContextMenu(tree);
		tree.setMenu(fMenu);
		
		IPageSite site= getSite();
		site.registerContextMenu(JavaPlugin.getPluginId() + ".outline", manager, fOutlineViewer); //$NON-NLS-1$
		site.setSelectionProvider(fOutlineViewer);

		// we must create the groups after we have set the selection provider to the site
		fActionGroups= new CompositeActionGroup(new ActionGroup[] {
				new OpenViewActionGroup(this), 
				new ShowActionGroup(this), 
				fCCPActionGroup= new CCPActionGroup(this),
				new RefactorActionGroup(this), 
				new GenerateActionGroup(this),
				new JavaSearchActionGroup(this)});
				
		// register global actions
		IActionBars bars= site.getActionBars();
		
		bars.setGlobalActionHandler(ITextEditorActionConstants.UNDO, fUndo);
		bars.setGlobalActionHandler(ITextEditorActionConstants.REDO, fRedo);
		bars.setGlobalActionHandler(ITextEditorActionConstants.PREVIOUS, fPreviousError);
		bars.setGlobalActionHandler(ITextEditorActionConstants.NEXT, fNextError);
		bars.setGlobalActionHandler(JdtActionConstants.SHOW_JAVA_DOC, fShowJavadoc);
		bars.setGlobalActionHandler(IJavaEditorActionConstants.TOGGLE_PRESENTATION, fTogglePresentation);
		// http://dev.eclipse.org/bugs/show_bug.cgi?id=18968
		bars.setGlobalActionHandler(IJavaEditorActionConstants.PREVIOUS_ERROR, fPreviousError);
		bars.setGlobalActionHandler(IJavaEditorActionConstants.NEXT_ERROR, fNextError);
		
		fActionGroups.fillActionBars(bars);

		IStatusLineManager statusLineManager= site.getActionBars().getStatusLineManager();
		if (statusLineManager != null) {
			StatusBarUpdater updater= new StatusBarUpdater(statusLineManager);
			fOutlineViewer.addPostSelectionChangedListener(updater);
		}
		
		registerToolbarActions();
				
		fOutlineViewer.setInput(fInput);	
		fOutlineViewer.getControl().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				handleKeyReleased(e);
			}
		});
		
		initDragAndDrop();
	}

	public void dispose() {
		
		if (fEditor == null)
			return;
			
		if (fMemberFilterActionGroup != null) {
			fMemberFilterActionGroup.dispose();
			fMemberFilterActionGroup= null;
		}
			
			
		fEditor.outlinePageClosed();
		fEditor= null;

		fSelectionChangedListeners.clear();
		fSelectionChangedListeners= null;

		if (fPropertyChangeListener != null) {
			JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(fPropertyChangeListener);
			fPropertyChangeListener= null;
		}
		
		if (fMenu != null && !fMenu.isDisposed()) {
			fMenu.dispose();
			fMenu= null;
		}
		
		if (fActionGroups != null)
			fActionGroups.dispose();
			
		fTogglePresentation.setEditor(null);
		fPreviousError.setEditor(null);
		fNextError.setEditor(null);	
		
		fOutlineViewer= null;
		
		super.dispose();
	}
	
	public Control getControl() {
		if (fOutlineViewer != null)
			return fOutlineViewer.getControl();
		return null;
	}
	
	public void setInput(IJavaElement inputElement) {
		fInput= inputElement;	
		if (fOutlineViewer != null)
			fOutlineViewer.setInput(fInput);
	}
		
	public void select(ISourceReference reference) {
		if (fOutlineViewer != null) {
			
			ISelection s= fOutlineViewer.getSelection();
			if (s instanceof IStructuredSelection) {
				IStructuredSelection ss= (IStructuredSelection) s;
				List elements= ss.toList();
				if (!elements.contains(reference)) {
					s= (reference == null ? StructuredSelection.EMPTY : new StructuredSelection(reference));
					fOutlineViewer.setSelection(s, true);
				}
			}
		}
	}
	
	public void setAction(String actionID, IAction action) {
		Assert.isNotNull(actionID);
		if (action == null)
			fActions.remove(actionID);
		else
			fActions.put(actionID, action);
	}
	
	public IAction getAction(String actionID) {
		Assert.isNotNull(actionID);
		return (IAction) fActions.get(actionID);
	}

	/**
	 * Convenience method to add the action installed under the given actionID to the
	 * specified group of the menu.
	 */
	protected void addAction(IMenuManager menu, String group, String actionID) {
		IAction action= getAction(actionID);
		if (action != null) {
			if (action instanceof IUpdate)
				((IUpdate) action).update();
				
			if (action.isEnabled()) {
		 		IMenuManager subMenu= menu.findMenuUsingPath(group);
		 		if (subMenu != null)
		 			subMenu.add(action);
		 		else
		 			menu.appendToGroup(group, action);
			}
		}
	}
	 
	protected void contextMenuAboutToShow(IMenuManager menu) {
		
		JavaPlugin.createStandardGroups(menu);
				
		IStructuredSelection selection= (IStructuredSelection)getSelection();
		fActionGroups.setContext(new ActionContext(selection));
		fActionGroups.fillContextMenu(menu);
	}
	
	/*
	 * @see Page#setFocus()
	 */
	public void setFocus() {
		if (fOutlineViewer != null)
			fOutlineViewer.getControl().setFocus();
	}
	
	/**
	 * Checkes whether a given Java element is an inner type.
	 */
	private boolean isInnerType(IJavaElement element) {
		
		if (element.getElementType() == IJavaElement.TYPE) {
			IJavaElement parent= element.getParent();
			int type= parent.getElementType();
			return (type != IJavaElement.COMPILATION_UNIT && type != IJavaElement.CLASS_FILE);
		}
		
		return false;		
	}
	
	/**
 	 * Handles key events in viewer.
 	 */
	private void handleKeyReleased(KeyEvent event) {
		
		if (event.stateMask != 0)
			return;
		
		IAction action= null;
		if (event.character == SWT.DEL) {
			action= fCCPActionGroup.getDeleteAction();
		}
			
		if (action != null && action.isEnabled())
			action.run();
	}
	
	private void initDragAndDrop() {
		int ops= DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;
		Transfer[] transfers= new Transfer[] {
			LocalSelectionTransfer.getInstance()
			};
		
		// Drop Adapter
		TransferDropTargetListener[] dropListeners= new TransferDropTargetListener[] {
			new SelectionTransferDropAdapter(fOutlineViewer)
		};
		fOutlineViewer.addDropSupport(ops | DND.DROP_DEFAULT, transfers, new DelegatingDropAdapter(dropListeners));
		
		// Drag Adapter
		Control control= fOutlineViewer.getControl();
		TransferDragSourceListener[] dragListeners= new TransferDragSourceListener[] {
			new SelectionTransferDragAdapter(fOutlineViewer)
		};
		DragSource source= new DragSource(control, ops);
		// Note, that the transfer agents are set by the delegating drag adapter itself.
		source.addDragListener(new DelegatingDragAdapter(dragListeners));
	}}