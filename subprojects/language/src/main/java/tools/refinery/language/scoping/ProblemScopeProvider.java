/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
 * generated by Xtext 2.25.0
 */
package tools.refinery.language.scoping;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.Scopes;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.utils.ProblemDesugarer;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains custom scoping description.
 * <p>
 * See
 * <a href="https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#scoping">...</a>
 * on how and when to use it.
 */
public class ProblemScopeProvider extends AbstractProblemScopeProvider {
	@Inject
	private ProblemDesugarer desugarer;

	@Override
	public IScope getScope(EObject context, EReference reference) {
		var scope = super.getScope(context, reference);
		if (reference == ProblemPackage.Literals.NODE_ASSERTION_ARGUMENT__NODE) {
			return getNodesScope(context, scope);
		}
		if (reference == ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__VARIABLE_OR_NODE
				|| reference == ProblemPackage.Literals.NEW_ACTION__PARENT
				|| reference == ProblemPackage.Literals.DELETE_ACTION__VARIABLE_OR_NODE) {
			return getVariableScope(context, scope);
		}
		if (reference == ProblemPackage.Literals.REFERENCE_DECLARATION__OPPOSITE) {
			return getOppositeScope(context, scope);
		}
		return scope;
	}

	protected IScope getNodesScope(EObject context, IScope delegateScope) {
		var problem = EcoreUtil2.getContainerOfType(context, Problem.class);
		if (problem == null) {
			return delegateScope;
		}
		return Scopes.scopeFor(problem.getNodes(), delegateScope);
	}

	protected IScope getVariableScope(EObject context, IScope delegateScope) {
		List<Variable> variables = new ArrayList<>();
		addSingletonVariableToScope(context, variables);
		EObject currentContext = context;
		while (currentContext != null && !(currentContext instanceof ParametricDefinition)) {
			addExistentiallyQualifiedVariableToScope(currentContext, variables);
			currentContext = currentContext.eContainer();
		}
		IScope parentScope = getNodesScope(context, delegateScope);
		if (currentContext != null) {
			ParametricDefinition definition = (ParametricDefinition) currentContext;
			parentScope = Scopes.scopeFor(definition.getParameters(), parentScope);
		}
		return Scopes.scopeFor(variables, parentScope);
	}

	protected void addSingletonVariableToScope(EObject context, List<Variable> variables) {
		if (context instanceof VariableOrNodeExpr expr) {
			Variable singletonVariable = expr.getSingletonVariable();
			if (singletonVariable != null) {
				variables.add(singletonVariable);
			}
		}
	}

	protected void addExistentiallyQualifiedVariableToScope(EObject currentContext, List<Variable> variables) {
		if (currentContext instanceof ExistentialQuantifier quantifier) {
			variables.addAll(quantifier.getImplicitVariables());
		} else if (currentContext instanceof Match match) {
			variables.addAll(match.getCondition().getImplicitVariables());
		} else if (currentContext instanceof Consequent consequent) {
			for (var literal : consequent.getActions()) {
				if (literal instanceof NewAction newAction && newAction.getVariable() != null) {
					variables.add(newAction.getVariable());
				}
			}
		}
	}

	protected IScope getOppositeScope(EObject context, IScope delegateScope) {
		var referenceDeclaration = EcoreUtil2.getContainerOfType(context, ReferenceDeclaration.class);
		if (referenceDeclaration == null) {
			return delegateScope;
		}
		var relation = referenceDeclaration.getReferenceType();
		if (!(relation instanceof ClassDeclaration classDeclaration)) {
			return delegateScope;
		}
		var referenceDeclarations = desugarer.getAllReferenceDeclarations(classDeclaration);
		return Scopes.scopeFor(referenceDeclarations, delegateScope);
	}
}
