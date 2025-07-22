package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyInvocationVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.context.MethodInvocationContext;

import java.util.List;
import java.util.Objects;

public class IllegalCastHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        Integer lineNumber = errorResult.getErrorLineNumber();

        MethodBodyInvocationVisitor oldInvocationVisitor = new MethodBodyInvocationVisitor();
        MethodBodyVisitor oldBodyVisitor = new MethodBodyVisitor(lineNumber, oldInvocationVisitor);
        errorResult.getOldAST().accept(oldBodyVisitor);

        MethodBodyInvocationVisitor newInvocationVisitor = new MethodBodyInvocationVisitor();
        MethodBodyVisitor newBodyVisitor = new MethodBodyVisitor(lineNumber, newInvocationVisitor);
        errorResult.getNewAST().accept(newBodyVisitor);

        List<MethodInvocationContext> oldInvocationContextList = oldInvocationVisitor.getInvocationContextList();
        List<MethodInvocationContext> newInvocationContextList = newInvocationVisitor.getInvocationContextList();

        if (oldInvocationContextList.size() == newInvocationContextList.size()) {
            for (int i = 0; i < oldInvocationContextList.size(); i++) {
                MethodInvocationContext oldInvocationContext = oldInvocationContextList.get(i);
                if (oldInvocationContext.getStartLineNumber() >= lineNumber && lineNumber <= oldInvocationContext.getEndLineNumber()) {
                    MethodInvocationContext newInvocationContext = newInvocationContextList.get(i);
                    // if the return type of the method is changed during library upgrade, then it will cause illegal cast
                    if (!Objects.equals(oldInvocationContext.getReturnTypeClassName(), newInvocationContext.getReturnTypeClassName())) {
                        String belongedClassName = oldInvocationContext.getBelongedClassName();
                        String methodName = oldInvocationContext.getMethodName();
                        List<String> parameterList = oldInvocationContext.getParameterList();

                        ApiSignature apiSignature = new ApiSignature();
                        apiSignature.setClassName(belongedClassName);
                        apiSignature.setMethodName(methodName);
                        apiSignature.setMethodParamList(parameterList);
                        apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
                        errorResult.setApiSignature(apiSignature);

                        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
                        return dependencyProperty.getDependencyNodeByClassName(belongedClassName);
                    }
                }
            }
        }

        return null;
    }
}
