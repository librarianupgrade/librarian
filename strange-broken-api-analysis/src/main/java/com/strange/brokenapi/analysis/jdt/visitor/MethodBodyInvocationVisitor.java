package com.strange.brokenapi.analysis.jdt.visitor;

import com.strange.brokenapi.analysis.jdt.visitor.context.MethodInvocationContext;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Traverse all function call information within the body of a defined method,
 * and retrieve detailed contextual information for the methods called within the method body.
 */
public class MethodBodyInvocationVisitor extends ASTVisitor {

    private final List<MethodInvocationContext> invocationContextList;

    public MethodBodyInvocationVisitor() {
        this.invocationContextList = new ArrayList<>();
    }

    @Override
    public boolean visit(MethodInvocation node) {
        IMethodBinding methodBinding = node.resolveMethodBinding();
        MethodInvocationContext context = new MethodInvocationContext();
        CompilationUnit unit = (CompilationUnit) node.getRoot();
        Expression expression = node.getExpression();
        if (expression != null) {
            context.setBaseObjectName(expression.toString());
            ITypeBinding typeBinding = expression.resolveTypeBinding();
            if (typeBinding != null) {
                context.setBaseObjectClassName(typeBinding.getQualifiedName());
            }
        }

        if (methodBinding != null) {
            String returnTypeClassName = methodBinding.getReturnType().getQualifiedName();
            context.setReturnTypeClassName(returnTypeClassName);
            context.setMethodName(methodBinding.getName());
            ITypeBinding methodDeclaringClass = methodBinding.getDeclaringClass();
            if (methodDeclaringClass != null) {
                context.setBelongedClassName(methodDeclaringClass.getQualifiedName());
            }
            ITypeBinding[] exceptionTypes = methodBinding.getExceptionTypes();
            if (exceptionTypes != null) {
                for (ITypeBinding exceptionType : exceptionTypes) {
                    context.getMethodExceptions().add(exceptionType.getQualifiedName());
                }
            }

            ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
            if (parameterTypes != null) {
                List<String> params = Arrays.stream(parameterTypes)
                        .map(ITypeBinding::getQualifiedName)
                        .toList();
                context.setParameterList(params);
            }
        }

        int startPosition = node.getStartPosition();
        int length = node.getLength();
        int startLineNumber = unit.getLineNumber(startPosition);
        int endPosition = startPosition + length;
        int endLineNumber = unit.getLineNumber(endPosition);

        context.setStartLineNumber(startLineNumber);
        context.setEndLineNumber(endLineNumber);
        invocationContextList.add(context);

        return super.visit(node);
    }

    @Override
    public boolean visit(SuperMethodInvocation node) {
        IMethodBinding methodBinding = node.resolveMethodBinding();
        MethodInvocationContext context = new MethodInvocationContext();
        CompilationUnit unit = (CompilationUnit) node.getRoot();

        context.setBaseObjectName("super");
        ITypeBinding typeBinding = node.resolveTypeBinding();
        if(typeBinding != null) {
            context.setBaseObjectClassName(typeBinding.getQualifiedName());
        }

        if (methodBinding != null) {
            String returnTypeClassName = methodBinding.getReturnType().getQualifiedName();
            context.setReturnTypeClassName(returnTypeClassName);
            context.setMethodName(methodBinding.getName());
            ITypeBinding methodDeclaringClass = methodBinding.getDeclaringClass();
            if (methodDeclaringClass != null) {
                context.setBelongedClassName(methodDeclaringClass.getQualifiedName());
            }
            ITypeBinding[] exceptionTypes = methodBinding.getExceptionTypes();
            if (exceptionTypes != null) {
                for (ITypeBinding exceptionType : exceptionTypes) {
                    context.getMethodExceptions().add(exceptionType.getQualifiedName());
                }
            }

            ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
            if (parameterTypes != null) {
                List<String> params = Arrays.stream(parameterTypes)
                        .map(ITypeBinding::getQualifiedName)
                        .toList();
                context.setParameterList(params);
            }
        }

        int startPosition = node.getStartPosition();
        int length = node.getLength();
        int startLineNumber = unit.getLineNumber(startPosition);
        int endPosition = startPosition + length;
        int endLineNumber = unit.getLineNumber(endPosition);

        context.setStartLineNumber(startLineNumber);
        context.setEndLineNumber(endLineNumber);
        invocationContextList.add(context);
        return super.visit(node);
    }

    public List<MethodInvocationContext> getInvocationContextList() {
        return invocationContextList;
    }
}
