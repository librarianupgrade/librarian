package com.strange.brokenapi.analysis.jdt.visitor;

import com.strange.brokenapi.analysis.jdt.visitor.context.MethodDetailsContext;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Finds the method that caused an error by specifying the problem line number,
 * and retrieves relevant information about the method (e.g., method name,
 * containing class, parameter list, etc.).
 */
public class MethodDetailsVisitor extends ASTVisitor {
    private final Integer problemLineNumber;

    private final List<MethodDetailsContext> detailsContextList = new ArrayList<>();

    public MethodDetailsVisitor(Integer problemLineNumber) {
        this.problemLineNumber = problemLineNumber;
    }

    @Override
    public boolean visit(LambdaExpression node) {
        int startPosition = node.getStartPosition();
        int length = node.getLength();
        CompilationUnit unit = (CompilationUnit) node.getRoot();
        int startLine = unit.getLineNumber(startPosition);
        int endLine = unit.getLineNumber(startPosition + length - 1);

        if (problemLineNumber >= startLine && problemLineNumber <= endLine) {
            MethodDetailsContext detailsContext = new MethodDetailsContext();
            detailsContext.setLambdaFunction(true);
            detailsContext.setStartLineNumber(startLine);
            detailsContext.setEndLineNumber(endLine);

            IMethodBinding methodBinding = node.resolveMethodBinding();
            if (methodBinding != null) {
                detailsContext.setMethodName(methodBinding.getName());
                detailsContext.setBelongedClassName(methodBinding.getDeclaringClass().getQualifiedName());
                ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
                if (parameterTypes != null) {
                    List<String> params = Arrays.stream(parameterTypes)
                            .map(ITypeBinding::getQualifiedName)
                            .toList();
                    detailsContext.setParamTypeList(params);
                }
            }
            detailsContextList.add(detailsContext);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        int startPosition = node.getStartPosition();
        int length = node.getLength();
        CompilationUnit unit = (CompilationUnit) node.getRoot();
        int startLine = unit.getLineNumber(startPosition);
        int endLine = unit.getLineNumber(startPosition + length - 1);

        if (problemLineNumber >= startLine && problemLineNumber <= endLine) {
            MethodDetailsContext detailsContext = new MethodDetailsContext();
            detailsContext.setLambdaFunction(false);
            detailsContext.setStartLineNumber(startLine);
            detailsContext.setEndLineNumber(endLine);

            IMethodBinding methodBinding = node.resolveBinding();
            if (methodBinding != null) {
                detailsContext.setMethodName(methodBinding.getName());
                detailsContext.setBelongedClassName(methodBinding.getDeclaringClass().getQualifiedName());
                ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
                if (parameterTypes != null) {
                    List<String> params = Arrays.stream(parameterTypes)
                            .map(ITypeBinding::getQualifiedName)
                            .toList();
                    detailsContext.setParamTypeList(params);
                }
            }
            detailsContextList.add(detailsContext);
        }

        return super.visit(node);
    }

    public List<MethodDetailsContext> getDetailsContextList() {
        return detailsContextList;
    }
}
