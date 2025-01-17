/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John Kaplan, johnkaplantech@gmail.com - 108071 [code templates] template for body of newly created class
 *******************************************************************************/
package org.eclipse.jdt.internal.core.manipulation;

import java.util.ArrayList;

import org.eclipse.text.templates.ContextTypeRegistry;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;

/**
  */
public class CodeTemplateContextType extends TemplateContextType {

	/* context types */
	public static final String CATCHBLOCK_CONTEXTTYPE= "catchblock_context"; //$NON-NLS-1$
	public static final String METHODBODY_CONTEXTTYPE= "methodbody_context"; //$NON-NLS-1$
	public static final String CONSTRUCTORBODY_CONTEXTTYPE= "constructorbody_context"; //$NON-NLS-1$
	public static final String GETTERBODY_CONTEXTTYPE= "getterbody_context"; //$NON-NLS-1$
	public static final String SETTERBODY_CONTEXTTYPE= "setterbody_context"; //$NON-NLS-1$
	public static final String NEWTYPE_CONTEXTTYPE= "newtype_context"; //$NON-NLS-1$
	public static final String CLASSBODY_CONTEXTTYPE= "classbody_context"; //$NON-NLS-1$
	public static final String INTERFACEBODY_CONTEXTTYPE= "interfacebody_context"; //$NON-NLS-1$
	public static final String ENUMBODY_CONTEXTTYPE= "enumbody_context"; //$NON-NLS-1$
	public static final String RECORDBODY_CONTEXTTYPE= "recordbody_context"; //$NON-NLS-1$
	public static final String ANNOTATIONBODY_CONTEXTTYPE= "annotationbody_context"; //$NON-NLS-1$
	public static final String FILECOMMENT_CONTEXTTYPE= "filecomment_context"; //$NON-NLS-1$
	public static final String TYPECOMMENT_CONTEXTTYPE= "typecomment_context"; //$NON-NLS-1$
	public static final String FIELDCOMMENT_CONTEXTTYPE= "fieldcomment_context"; //$NON-NLS-1$
	public static final String METHODCOMMENT_CONTEXTTYPE= "methodcomment_context"; //$NON-NLS-1$
	public static final String CONSTRUCTORCOMMENT_CONTEXTTYPE= "constructorcomment_context"; //$NON-NLS-1$
	public static final String OVERRIDECOMMENT_CONTEXTTYPE= "overridecomment_context"; //$NON-NLS-1$
	public static final String DELEGATECOMMENT_CONTEXTTYPE= "delegatecomment_context"; //$NON-NLS-1$
	public static final String GETTERCOMMENT_CONTEXTTYPE= "gettercomment_context"; //$NON-NLS-1$
	public static final String SETTERCOMMENT_CONTEXTTYPE= "settercomment_context"; //$NON-NLS-1$
	public static final String MODULECOMMENT_CONTEXTTYPE= "modulecomment_context"; //$NON-NLS-1$

	/* templates */

	private static final String CODETEMPLATES_PREFIX= "org.eclipse.jdt.ui.text.codetemplates."; //$NON-NLS-1$
	public static final String COMMENT_SUFFIX= "comment"; //$NON-NLS-1$

	public static final String CATCHBLOCK_ID= CODETEMPLATES_PREFIX + "catchblock"; //$NON-NLS-1$
	public static final String METHODSTUB_ID= CODETEMPLATES_PREFIX + "methodbody"; //$NON-NLS-1$
	public static final String METHODSTUB_ALTERNATIVE_ID= CODETEMPLATES_PREFIX + "methodbodyalternative"; //$NON-NLS-1$
	public static final String NEWTYPE_ID= CODETEMPLATES_PREFIX + "newtype"; //$NON-NLS-1$
	public static final String CONSTRUCTORSTUB_ID= CODETEMPLATES_PREFIX + "constructorbody"; //$NON-NLS-1$
	public static final String GETTERSTUB_ID= CODETEMPLATES_PREFIX + "getterbody"; //$NON-NLS-1$
	public static final String SETTERSTUB_ID= CODETEMPLATES_PREFIX + "setterbody"; //$NON-NLS-1$
	public static final String FILECOMMENT_ID= CODETEMPLATES_PREFIX + "file" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String TYPECOMMENT_ID= CODETEMPLATES_PREFIX + "type" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String CLASSBODY_ID= CODETEMPLATES_PREFIX + "classbody"; //$NON-NLS-1$
	public static final String INTERFACEBODY_ID= CODETEMPLATES_PREFIX + "interfacebody"; //$NON-NLS-1$
	public static final String ENUMBODY_ID= CODETEMPLATES_PREFIX + "enumbody"; //$NON-NLS-1$
	public static final String RECORDBODY_ID= CODETEMPLATES_PREFIX + "recordbody"; //$NON-NLS-1$
	public static final String ANNOTATIONBODY_ID= CODETEMPLATES_PREFIX + "annotationbody"; //$NON-NLS-1$
	public static final String FIELDCOMMENT_ID= CODETEMPLATES_PREFIX + "field" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String METHODCOMMENT_ID= CODETEMPLATES_PREFIX + "method" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String CONSTRUCTORCOMMENT_ID= CODETEMPLATES_PREFIX + "constructor" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String OVERRIDECOMMENT_ID= CODETEMPLATES_PREFIX + "override" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String DELEGATECOMMENT_ID= CODETEMPLATES_PREFIX + "delegate" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String GETTERCOMMENT_ID= CODETEMPLATES_PREFIX + "getter" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String SETTERCOMMENT_ID= CODETEMPLATES_PREFIX + "setter" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String MODULECOMMENT_ID= CODETEMPLATES_PREFIX + "module" + COMMENT_SUFFIX; //$NON-NLS-1$

	/* resolver types */
	public static final String EXCEPTION_TYPE= "exception_type"; //$NON-NLS-1$
	public static final String EXCEPTION_VAR= "exception_var"; //$NON-NLS-1$
	public static final String ENCLOSING_METHOD= "enclosing_method"; //$NON-NLS-1$
	public static final String ENCLOSING_MODULE= "enclosing_module"; //$NON-NLS-1$
	public static final String ENCLOSING_TYPE= "enclosing_type"; //$NON-NLS-1$
	public static final String BODY_STATEMENT= "body_statement"; //$NON-NLS-1$
	public static final String FIELD= "field"; //$NON-NLS-1$
	public static final String FIELD_TYPE= "field_type"; //$NON-NLS-1$
	public static final String BARE_FIELD_NAME= "bare_field_name"; //$NON-NLS-1$

	public static final String PARAM= "param"; //$NON-NLS-1$
	public static final String RETURN_TYPE= "return_type"; //$NON-NLS-1$
	public static final String SEE_TO_OVERRIDDEN_TAG= "see_to_overridden"; //$NON-NLS-1$
	public static final String SEE_TO_TARGET_TAG= "see_to_target"; //$NON-NLS-1$

	public static final String TAGS= "tags"; //$NON-NLS-1$

	public static final String TYPENAME= "type_name"; //$NON-NLS-1$
	public static final String FILENAME= "file_name"; //$NON-NLS-1$
	public static final String PACKAGENAME= "package_name"; //$NON-NLS-1$
	public static final String PROJECTNAME= "project_name"; //$NON-NLS-1$

	public static final String PACKAGE_DECLARATION= "package_declaration"; //$NON-NLS-1$
	public static final String TYPE_DECLARATION= "type_declaration"; //$NON-NLS-1$
	public static final String CLASS_BODY= "classbody"; //$NON-NLS-1$
	public static final String INTERFACE_BODY= "interfacebody"; //$NON-NLS-1$
	public static final String ENUM_BODY= "enumbody"; //$NON-NLS-1$
	public static final String RECORD_BODY= "recordbody"; //$NON-NLS-1$
	public static final String ANNOTATION_BODY= "annotationbody"; //$NON-NLS-1$
	public static final String TYPE_COMMENT= "typecomment"; //$NON-NLS-1$
	public static final String FILE_COMMENT= "filecomment"; //$NON-NLS-1$


	/**
	 * Resolver that resolves to the variable defined in the context.
	 */
	public static class CodeTemplateVariableResolver extends TemplateVariableResolver {
		public CodeTemplateVariableResolver(String type, String description) {
			super(type, description);
		}

		@Override
		protected String resolve(TemplateContext context) {
			return context.getVariable(getType());
		}
	}

	/**
	 * Resolver for javadoc tags.
	 */
	public static class TagsVariableResolver extends TemplateVariableResolver {
		public TagsVariableResolver() {
			super(TAGS,  JavaManipulationMessages.CodeTemplateContextType_variable_description_tags);
		}

		@Override
		protected String resolve(TemplateContext context) {
			return "@"; //$NON-NLS-1$
		}
	}

	/**
	 * Resolver for todo tags.
	 */
	protected static class Todo extends TemplateVariableResolver {

		public Todo() {
			super("todo", JavaManipulationMessages.CodeTemplateContextType_variable_description_todo);  //$NON-NLS-1$
		}

		@Override
		protected String resolve(TemplateContext context) {
			String todoTaskTag= StubUtility.getTodoTaskTag(((CodeTemplateContext) context).getJavaProject());
			if (todoTaskTag == null)
				return "XXX"; //$NON-NLS-1$

			return todoTaskTag;
		}
	}

	private boolean fIsComment;

	public CodeTemplateContextType(String contextName) {
		super(contextName);

		fIsComment= false;

		// global
		addResolver(new GlobalTemplateVariables.Dollar());
		addResolver(new GlobalTemplateVariables.Date());
		addResolver(new GlobalTemplateVariables.Year());
		addResolver(new GlobalTemplateVariables.Time());
		addResolver(new GlobalTemplateVariables.User());
		addResolver(new Todo());

		if (contextName == null) {
			return;
		}

		switch (contextName) {
		case CATCHBLOCK_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingtype));
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingmethod));
			addResolver(new CodeTemplateVariableResolver(EXCEPTION_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_exceptiontype));
			addResolver(new CodeTemplateVariableResolver(EXCEPTION_VAR,  JavaManipulationMessages.CodeTemplateContextType_variable_description_exceptionvar));
			break;
		case METHODBODY_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingtype));
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingmethod));
			addResolver(new CodeTemplateVariableResolver(BODY_STATEMENT,  JavaManipulationMessages.CodeTemplateContextType_variable_description_bodystatement));
			break;
		case CONSTRUCTORBODY_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingtype));
			addResolver(new CodeTemplateVariableResolver(BODY_STATEMENT,  JavaManipulationMessages.CodeTemplateContextType_variable_description_bodystatement));
			break;
		case GETTERBODY_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingtype));
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingmethod));
			addResolver(new CodeTemplateVariableResolver(FIELD, JavaManipulationMessages.CodeTemplateContextType_variable_description_getterfieldname));
			break;
		case SETTERBODY_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingtype));
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingmethod));
			addResolver(new CodeTemplateVariableResolver(FIELD, JavaManipulationMessages.CodeTemplateContextType_variable_description_getterfieldname));
			addResolver(new CodeTemplateVariableResolver(PARAM, JavaManipulationMessages.CodeTemplateContextType_variable_description_param));
			break;
		case NEWTYPE_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(TYPENAME,  JavaManipulationMessages.CodeTemplateContextType_variable_description_typename));
			addResolver(new CodeTemplateVariableResolver(PACKAGE_DECLARATION,  JavaManipulationMessages.CodeTemplateContextType_variable_description_packdeclaration));
			addResolver(new CodeTemplateVariableResolver(TYPE_DECLARATION,  JavaManipulationMessages.CodeTemplateContextType_variable_description_typedeclaration));
			addResolver(new CodeTemplateVariableResolver(TYPE_COMMENT,  JavaManipulationMessages.CodeTemplateContextType_variable_description_typecomment));
			addResolver(new CodeTemplateVariableResolver(FILE_COMMENT,  JavaManipulationMessages.CodeTemplateContextType_variable_description_filecomment));
			addCompilationUnitVariables();
			break;
		case CLASSBODY_CONTEXTTYPE:
		case INTERFACEBODY_CONTEXTTYPE:
		case ENUMBODY_CONTEXTTYPE:
		case ANNOTATIONBODY_CONTEXTTYPE:
		case RECORDBODY_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(TYPENAME,  JavaManipulationMessages.CodeTemplateContextType_variable_description_typename));
			addCompilationUnitVariables();
			break;
		case TYPECOMMENT_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(TYPENAME,  JavaManipulationMessages.CodeTemplateContextType_variable_description_typename));
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingtype));
			addResolver(new TagsVariableResolver());
			addCompilationUnitVariables();
			fIsComment= true;
			break;
		case FILECOMMENT_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(TYPENAME,  JavaManipulationMessages.CodeTemplateContextType_variable_description_typename));
			addCompilationUnitVariables();
			fIsComment= true;
			break;
		case FIELDCOMMENT_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(FIELD_TYPE, JavaManipulationMessages.CodeTemplateContextType_variable_description_fieldtype));
			addResolver(new CodeTemplateVariableResolver(FIELD, JavaManipulationMessages.CodeTemplateContextType_variable_description_fieldname));
			addCompilationUnitVariables();
			fIsComment= true;
			break;
		case METHODCOMMENT_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingtype));
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingmethod));
			addResolver(new CodeTemplateVariableResolver(RETURN_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_returntype));
			addResolver(new TagsVariableResolver());
			addCompilationUnitVariables();
			fIsComment= true;
			break;
		case MODULECOMMENT_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_MODULE, JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingmodule));
			addResolver(new TagsVariableResolver());
			addCompilationUnitVariables();
			fIsComment= true;
			break;
		case OVERRIDECOMMENT_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingtype));
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingmethod));
			addResolver(new CodeTemplateVariableResolver(SEE_TO_OVERRIDDEN_TAG,  JavaManipulationMessages.CodeTemplateContextType_variable_description_see_overridden_tag));
			addResolver(new TagsVariableResolver());
			addCompilationUnitVariables();
			fIsComment= true;
			break;
		case DELEGATECOMMENT_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingtype));
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingmethod));
			addResolver(new CodeTemplateVariableResolver(SEE_TO_TARGET_TAG,  JavaManipulationMessages.CodeTemplateContextType_variable_description_see_target_tag));
			addResolver(new TagsVariableResolver());
			addCompilationUnitVariables();
			fIsComment= true;
			break;
		case CONSTRUCTORCOMMENT_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingtype));
			addResolver(new TagsVariableResolver());
			addCompilationUnitVariables();
			fIsComment= true;
			break;
		case GETTERCOMMENT_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingtype));
			addResolver(new CodeTemplateVariableResolver(FIELD_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_getterfieldtype));
			addResolver(new CodeTemplateVariableResolver(FIELD, JavaManipulationMessages.CodeTemplateContextType_variable_description_getterfieldname));
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingmethod));
			addResolver(new CodeTemplateVariableResolver(BARE_FIELD_NAME, JavaManipulationMessages.CodeTemplateContextType_variable_description_barefieldname));
			addCompilationUnitVariables();
			fIsComment= true;
			break;
		case SETTERCOMMENT_CONTEXTTYPE:
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingtype));
			addResolver(new CodeTemplateVariableResolver(FIELD_TYPE,  JavaManipulationMessages.CodeTemplateContextType_variable_description_getterfieldtype));
			addResolver(new CodeTemplateVariableResolver(FIELD, JavaManipulationMessages.CodeTemplateContextType_variable_description_getterfieldname));
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaManipulationMessages.CodeTemplateContextType_variable_description_enclosingmethod));
			addResolver(new CodeTemplateVariableResolver(PARAM, JavaManipulationMessages.CodeTemplateContextType_variable_description_param));
			addResolver(new CodeTemplateVariableResolver(BARE_FIELD_NAME, JavaManipulationMessages.CodeTemplateContextType_variable_description_barefieldname));
			addCompilationUnitVariables();
			fIsComment= true;
			break;
		default:
			break;
		}
	}

	private void addCompilationUnitVariables() {
		addResolver(new CodeTemplateVariableResolver(FILENAME, JavaManipulationMessages.CodeTemplateContextType_variable_description_filename));
		addResolver(new CodeTemplateVariableResolver(PACKAGENAME, JavaManipulationMessages.CodeTemplateContextType_variable_description_packagename));
		addResolver(new CodeTemplateVariableResolver(PROJECTNAME, JavaManipulationMessages.CodeTemplateContextType_variable_description_projectname));
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.ContextType#validateVariables(org.eclipse.jdt.internal.corext.template.TemplateVariable[])
	 */
	@Override
	protected void validateVariables(TemplateVariable[] variables) throws TemplateException {
		ArrayList<String> required=  new ArrayList<>(5);
		String contextName= getId();
		if (NEWTYPE_CONTEXTTYPE.equals(contextName)) {
			required.add(PACKAGE_DECLARATION);
			required.add(TYPE_DECLARATION);
		}
		for (TemplateVariable variable : variables) {
			String type= variable.getType();
			if (getResolver(type) == null) {
				String unknown= BasicElementLabels.getJavaElementName(type);
				throw new TemplateException(Messages.format(JavaManipulationMessages.CodeTemplateContextType_validate_unknownvariable, unknown));
			}
			required.remove(type);
		}
		if (!required.isEmpty()) {
			String missing= BasicElementLabels.getJavaElementName(required.get(0));
			throw new TemplateException(Messages.format(JavaManipulationMessages.CodeTemplateContextType_validate_missingvariable, missing));
		}
		super.validateVariables(variables);
	}

	public TemplateContext createContext() {
		return null;
	}

	public static void registerContextTypes(ContextTypeRegistry registry) {
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.CATCHBLOCK_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.METHODBODY_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.CONSTRUCTORBODY_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.GETTERBODY_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.SETTERBODY_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.NEWTYPE_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.CLASSBODY_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.INTERFACEBODY_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.ENUMBODY_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.ANNOTATIONBODY_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.RECORDBODY_CONTEXTTYPE));

		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.FILECOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.TYPECOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.FIELDCOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.METHODCOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.MODULECOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.CONSTRUCTORCOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.OVERRIDECOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.DELEGATECOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.GETTERCOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.SETTERCOMMENT_CONTEXTTYPE));
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.ContextType#validate(java.lang.String)
	 */
	@Override
	public void validate(String pattern) throws TemplateException {
		super.validate(pattern);
		if (fIsComment) {
			if (!isValidComment(pattern)) {
				throw new TemplateException(JavaManipulationMessages.CodeTemplateContextType_validate_invalidcomment);
			}
		}
	}


	private boolean isValidComment(String template) {
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(template.toCharArray());
		try {
			int next= scanner.getNextToken();
			while (TokenScanner.isComment(next)) {
				next= scanner.getNextToken();
			}
			return next == ITerminalSymbols.TokenNameEOF;
		} catch (InvalidInputException e) {
		}
		return false;
	}

}
