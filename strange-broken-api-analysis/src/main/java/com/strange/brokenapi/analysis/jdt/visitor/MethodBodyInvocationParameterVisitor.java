package com.strange.brokenapi.analysis.jdt.visitor;

import com.strange.brokenapi.analysis.jdt.visitor.context.MethodInvocationContext;
import org.eclipse.jdt.core.dom.*;

public class MethodBodyInvocationParameterVisitor extends ASTVisitor {

    private final String mismatchedMethodName;

    private final Integer errorLineNumber;

    private final Integer mismatchedPosition;

    private final MethodInvocationContext invocationContext;

    public MethodBodyInvocationParameterVisitor(String mismatchedMethodName, Integer errorLineNumber, Integer mismatchedPosition) {
        this.mismatchedMethodName = mismatchedMethodName;
        this.errorLineNumber = errorLineNumber;
        this.mismatchedPosition = mismatchedPosition;
        this.invocationContext = new MethodInvocationContext();
    }

    @Override
    public boolean visit(MethodInvocation node) {
        int startPosition = node.getStartPosition();
        int length = node.getLength();
        CompilationUnit unit = (CompilationUnit) node.getRoot();
        int startLine = unit.getLineNumber(startPosition);
        int endLine = unit.getLineNumber(startPosition + length - 1);


        if (startLine <= errorLineNumber && errorLineNumber <= endLine
                && node.getName().getIdentifier().equals(mismatchedMethodName)) {
            if (mismatchedPosition < node.arguments().size()) {
                Expression targetArg = (Expression) node.arguments().get(mismatchedPosition);
                if (targetArg instanceof MethodInvocation) {
                    IMethodBinding methodBinding = ((MethodInvocation) targetArg).resolveMethodBinding();
                    if(methodBinding != null) {
                        String returnTypeClassName = methodBinding.getReturnType().getQualifiedName();
                        invocationContext.setReturnTypeClassName(returnTypeClassName);
                        invocationContext.setMethodName(methodBinding.getName());
                        invocationContext.setBelongedClassName(methodBinding.getDeclaringClass().getQualifiedName());
                        ITypeBinding[] exceptionTypes = methodBinding.getExceptionTypes();
                        if (exceptionTypes != null) {
                            for (ITypeBinding exceptionType : exceptionTypes) {
                                invocationContext.getMethodExceptions().add(exceptionType.getQualifiedName());
                            }
                        }
                    }

                }

            }

        }
        return super.visit(node);
    }

    public MethodInvocationContext getInvocationContext() {
        return invocationContext;
    }
}
