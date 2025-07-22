package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyReturnVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.MethodDetailsVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.context.MethodDetailsContext;
import com.strange.brokenapi.analysis.jdt.visitor.context.MethodInvocationContext;

import java.util.List;

public class ReturnTypeMismatchHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        Integer errorLineNumber = errorResult.getErrorLineNumber();

        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the class name of actual return value
        // arguments[1] is the class name of expected return value
        MethodDetailsVisitor methodDetailsVisitor = new MethodDetailsVisitor(errorLineNumber);
        errorResult.getNewAST().accept(methodDetailsVisitor);
        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();

        // if is the problem of the method itself
        List<MethodDetailsContext> detailsContextList = methodDetailsVisitor.getDetailsContextList();
        for (MethodDetailsContext detailsContext : detailsContextList) {
            String belongedClassName = detailsContext.getBelongedClassName();
            DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(belongedClassName);
            if (dependencyNode != null) {
                ApiSignature apiSignature = new ApiSignature();
                apiSignature.setClassName(belongedClassName);
                apiSignature.setMethodName(detailsContext.getMethodName());
                apiSignature.setMethodParamList(detailsContext.getParamTypeList());
                apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
                errorResult.setApiSignature(apiSignature);
                return dependencyNode;
            }
        }

        // if is the problem of the return value TODO
        MethodBodyReturnVisitor methodBodyReturnVisitor = new MethodBodyReturnVisitor();
        errorResult.getNewAST().accept(new MethodBodyVisitor(errorLineNumber, methodBodyReturnVisitor));
        MethodInvocationContext invocationContext = methodBodyReturnVisitor.getInvocationContext();
        String belongedClassName = invocationContext.getBelongedClassName();
        DependencyProperty newDependencyProperty = errorResult.getNewTreeResolver().getDependencyProperty();
        DependencyNode dependencyNode = newDependencyProperty.getDependencyNodeByClassName(belongedClassName);
        if (dependencyNode != null) {
            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setClassName(belongedClassName);
            apiSignature.setMethodName(invocationContext.getMethodName());
            apiSignature.setMethodParamList(invocationContext.getParameterList());
            apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
            errorResult.setApiSignature(apiSignature);
            return dependencyNode;
        }
        return null;
    }
}
