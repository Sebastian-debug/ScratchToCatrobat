/*
 * ScratchToCatrobat: A tool for converting Scratch projects into Catrobat programs.
 * Copyright (C) 2013-2017 The Catrobat Team
 * (http://developer.catrobat.org/credits)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package sourcecodefilter.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.internal.resources.refresh.win32.Convert;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import sourcecodefilter.ConverterRelevantCatroidSource;
import sourcecodefilter.SourceCodeFilter;

public class MethodInvocationFilter extends ASTVisitor {

	final private ConverterRelevantCatroidSource catroidSource;
	final private Set<String> methodInvocationsToBeRemoved;
	final private Set<String> methodInvocationsWithParameterToBeRemoved;
	
	public MethodInvocationFilter(ConverterRelevantCatroidSource catroidSource,
			Set<String> methodInvocationsToBeRemoved,
			Set<String> methodInvocationsWithParameterToBeRemoved) {
		this.catroidSource = catroidSource;
		this.methodInvocationsToBeRemoved = methodInvocationsToBeRemoved;
		this.methodInvocationsWithParameterToBeRemoved = methodInvocationsWithParameterToBeRemoved;
	}

    @SuppressWarnings("unchecked")
	public void removeUnallowedMethodInvocations() {
        CompilationUnit cu = catroidSource.getSourceAst();
        final List<AbstractTypeDeclaration> types = cu.types();
        assert types.size() > 0;
        if (catroidSource.getQualifiedClassName().equals("org.catrobat.catroid.content.Scene")) {
        	return;
        }
        for (AbstractTypeDeclaration abstractTypeDecl : types) {
            for (BodyDeclaration bodyDecl : new ArrayList<BodyDeclaration>(abstractTypeDecl.bodyDeclarations())) {
                if (bodyDecl.getNodeType() != ASTNode.METHOD_DECLARATION) {
                	continue;
                }

                MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDecl;
                Block body = methodDeclaration.getBody();
                if ((body == null) || (body.statements().size() == 0)) {
                	continue;
                }
                try {
                	methodDeclaration.accept(this);
                } catch (java.lang.IllegalArgumentException ex) {
                	System.err.println("Unable to filter methods in: " + this.catroidSource.getQualifiedClassName());
                }
            }
        }
    }

	public boolean visit(MethodInvocation methodInvocation) {
		final String[] temp = methodInvocation.toString().split("\\(", 2);
		assert(temp.length > 0);
		final String fullName = temp[0];

		for (String unallowedMethodInvocation : methodInvocationsToBeRemoved) {
			boolean isMatching;
			if (unallowedMethodInvocation.contains("*")) {
				unallowedMethodInvocation = unallowedMethodInvocation.replace("*", "");
				isMatching = fullName.startsWith(unallowedMethodInvocation)
						|| fullName.endsWith(unallowedMethodInvocation);
			} else {
				isMatching = fullName.equals(unallowedMethodInvocation);
			}

            if (isMatching) {
            	methodInvocation.getParent().delete();
            	return false;
            }
		}

		for (String expectedMethodInvocation : methodInvocationsWithParameterToBeRemoved) {
			if (methodInvocation.toString().equals(expectedMethodInvocation)) {
            	methodInvocation.getParent().delete();
			}
		}
		
		if (SourceCodeFilter.removeMethodParameterAtPosition.containsKey(fullName) ) {
			Set<String> indicesToRemove = SourceCodeFilter.removeMethodParameterAtPosition.get(fullName);
			for (String indToRemove : indicesToRemove)
			{
				methodInvocation.arguments().remove(Integer.parseInt(indToRemove));
			}
		}
		return false;
	}
}
