package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyInvocationVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.TryBlockVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.context.MethodInvocationContext;

import java.util.List;

public class UnreachableCatchHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        Integer lineNumber = errorResult.getErrorLineNumber();
        List<String> arguments = errorResult.getArguments();

        // the class name of exception
        String exceptionClassName = arguments.get(0);

        MethodBodyInvocationVisitor oldInvocationVisitor = new MethodBodyInvocationVisitor();
        errorResult.getOldAST().accept(new TryBlockVisitor(lineNumber, oldInvocationVisitor));

        MethodBodyInvocationVisitor newInvocationVisitor = new MethodBodyInvocationVisitor();
        errorResult.getNewAST().accept(new TryBlockVisitor(lineNumber, newInvocationVisitor));

        List<MethodInvocationContext> oldInvocationContextList = oldInvocationVisitor.getInvocationContextList();
        List<MethodInvocationContext> newInvocationContextList = newInvocationVisitor.getInvocationContextList();

        if (oldInvocationContextList.size() == newInvocationContextList.size()) {
            for (int i = 0; i < oldInvocationContextList.size(); i++) {
                MethodInvocationContext oldInvocationContext = oldInvocationContextList.get(i);
                MethodInvocationContext newInvocationContext = newInvocationContextList.get(i);
                if (isRemoveTheException(oldInvocationContext.getMethodExceptions(), newInvocationContext.getMethodExceptions(), exceptionClassName)) {
                    String belongedClassName = oldInvocationContext.getBelongedClassName();
                    DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
                    DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(belongedClassName);
                    if (dependencyNode != null) {
                        ApiSignature apiSignature = new ApiSignature();
                        apiSignature.setClassName(oldInvocationContext.getBelongedClassName());
                        apiSignature.setMethodName(oldInvocationContext.getMethodName());
                        apiSignature.setMethodParamList(oldInvocationContext.getParameterList());
                        apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
                        errorResult.setApiSignature(apiSignature);
                        return dependencyNode;
                    }
                }
            }
        }
        return null;
    }

    private boolean isRemoveTheException(List<String> previousExceptionList, List<String> currentExceptionList, String exceptionName) {
        return previousExceptionList.contains(exceptionName) && !currentExceptionList.contains(exceptionName);
    }
}
