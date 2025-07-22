package com.strange.brokenapi.analysis.jdt.visitor;

import com.strange.brokenapi.analysis.jdt.visitor.context.MethodInvocationContext;
import com.strange.common.utils.ClassUtil;
import org.eclipse.jdt.core.dom.*;

public class MethodBodyReturnVisitor extends ASTVisitor {

    private final MethodInvocationContext invocationContext;

    public MethodBodyReturnVisitor() {
        this.invocationContext = new MethodInvocationContext();
    }

    @Override
    public boolean visit(ReturnStatement node) {
        Expression expression = node.getExpression();
        if (expression instanceof MethodInvocation) {
            MethodInvocation methodInvocation = (MethodInvocation) expression;
            IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
            if (methodBinding != null) {
                String belongedClassName = ClassUtil.removeGenericType(methodBinding.getDeclaringClass().getQualifiedName());
                invocationContext.setMethodName(methodBinding.getName());
                invocationContext.setBelongedClassName(belongedClassName);
                ITypeBinding[] exceptionTypes = methodBinding.getExceptionTypes();
                if (exceptionTypes != null) {
                    for (ITypeBinding exceptionType : exceptionTypes) {
                        invocationContext.getMethodExceptions().add(exceptionType.getQualifiedName());
                    }
                }
            }

            CompilationUnit unit = (CompilationUnit) node.getRoot();
            int startPosition = node.getStartPosition();
            int length = node.getLength();
            int startLineNumber = unit.getLineNumber(startPosition);
            int endPosition = startPosition + length;
            int endLineNumber = unit.getLineNumber(endPosition);

            invocationContext.setStartLineNumber(startLineNumber);
            invocationContext.setEndLineNumber(endLineNumber);
        }
        return true;
    }

    public MethodInvocationContext getInvocationContext() {
        return invocationContext;
    }
}
