package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyInvocationParameterVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyInvocationVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.context.MethodInvocationContext;
import com.strange.common.utils.ClassUtil;

import java.util.List;
import java.util.Objects;

public class ParameterMismatchHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        Integer errorLineNumber = errorResult.getErrorLineNumber();
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the class to which the method belongs,
        // arguments[1] is the method name,
        // arguments[2] is the excepted list of parameters,
        // arguments[3] is the actual list of input parameters.
        String className = ClassUtil.removeGenericType(arguments.get(0));
        String methodName = arguments.get(1);
        List<String> exceptedParamList = ClassUtil.parseParamTypeList(arguments.get(2));
        List<String> actualParamList = ClassUtil.parseParamTypeList(arguments.get(3));

        ApiSignature apiSignature = new ApiSignature();
        apiSignature.setClassName(className);
        apiSignature.setMethodName(methodName);
        apiSignature.setMethodParamList(actualParamList);
        apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
        errorResult.setApiSignature(apiSignature);

        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
        DependencyProperty newDependencyProperty = errorResult.getNewTreeResolver().getDependencyProperty();
        // if is the changed return value of the method cause the mismatched parameters
        if (exceptedParamList.size() == actualParamList.size()) {
            int mismatchedPosition = -1;
            int diffCount = 0;
            for (int i = 0; i < exceptedParamList.size(); i++) {
                if (!Objects.equals(exceptedParamList.get(i), actualParamList.get(i))) {
                    mismatchedPosition = i;
                    diffCount += 1;
                }
            }
            if (diffCount == 1) {
                DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(actualParamList.get(mismatchedPosition));
                if (dependencyNode != null) return dependencyNode;

                MethodBodyInvocationParameterVisitor invocationParameterVisitor = new MethodBodyInvocationParameterVisitor(methodName, errorLineNumber, mismatchedPosition);
                errorResult.getOldAST().accept(new MethodBodyVisitor(errorLineNumber, invocationParameterVisitor));
                MethodInvocationContext invocationContext = invocationParameterVisitor.getInvocationContext();
                String belongedClassName = invocationContext.getBelongedClassName();
                dependencyNode = dependencyProperty.getDependencyNodeByClassName(belongedClassName);
                if (dependencyNode != null) return dependencyNode;
            }
        }

        // to check whether the method parameter list changed
        MethodBodyInvocationVisitor oldInvocationVisitor = new MethodBodyInvocationVisitor();
        MethodBodyInvocationVisitor newInvocationVisitor = new MethodBodyInvocationVisitor();
        errorResult.getOldAST().accept(new MethodBodyVisitor(errorLineNumber, oldInvocationVisitor));
        errorResult.getNewAST().accept(new MethodBodyVisitor(errorLineNumber, newInvocationVisitor));

        MethodInvocationContext oldInvocationContext = null;
        for (MethodInvocationContext invocationContext : oldInvocationVisitor.getInvocationContextList()) {
            if (invocationContext.getStartLineNumber() <= errorLineNumber && invocationContext.getEndLineNumber() >= errorLineNumber
                    && methodName.equals(invocationContext.getMethodName())) {
                oldInvocationContext = invocationContext;
                break;
            }
        }

        MethodInvocationContext newInvocationContext = null;
        for (MethodInvocationContext invocationContext : newInvocationVisitor.getInvocationContextList()) {
            if (invocationContext.getStartLineNumber() <= errorLineNumber && invocationContext.getEndLineNumber() >= errorLineNumber
                    && methodName.equals(invocationContext.getMethodName())) {
                newInvocationContext = invocationContext;
                break;
            }
        }

        boolean paramListChanged = false;
        if (oldInvocationContext != null && newInvocationContext != null) {
            paramListChanged = isParamListChanged(oldInvocationContext.getParameterList(),
                    newInvocationContext.getParameterList());
        }

        if (paramListChanged) {
            // if method parameter list change and the method is in the old version
            DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(className);
            if (dependencyNode != null) return dependencyNode;

            // if method parameter list change and the method is in the new version
            dependencyNode = newDependencyProperty.getDependencyNodeByClassName(className);
            if (dependencyNode != null) return dependencyNode;
        } else {
            // Ambiguous Type Occur
            if (exceptedParamList.size() == actualParamList.size()) {
                int diffCount = 0;
                int diffIdx = -1;
                for (int i = 0; i < exceptedParamList.size(); i++) {
                    if (!Objects.equals(exceptedParamList.get(i), actualParamList.get(i))) {
                        diffCount += 1;
                        diffIdx = i;
                    }
                }

                if (diffCount == 1) {
                    String ambiguousClassName = actualParamList.get(diffIdx);
                    DependencyNode dependencyNode = newDependencyProperty.getDependencyNodeByClassName(ambiguousClassName);
                    if (dependencyNode != null) return dependencyNode;
                }
            }
        }
        return null;
    }


    private boolean isParamListChanged(List<String> oldParamList, List<String> newParamList) {
        if (oldParamList.size() == newParamList.size()) {
            for (int i = 0; i < oldParamList.size(); i++) {
                if (!Objects.equals(oldParamList.get(i), newParamList.get(i))) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }
}
