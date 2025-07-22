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
import com.strange.common.utils.ClassUtil;

import java.util.List;

public class UndefinedMethodHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        Integer errorLineNumber = errorResult.getErrorLineNumber();
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is a class name
        // arguments[1] is the method name
        // arguments[2] is the method's parameter list
        String className = ClassUtil.removeGenericType(arguments.get(0));
        String methodName = arguments.get(1);
        List<String> paramList = ClassUtil.parseParamTypeList(arguments.get(2));

        // if the method is existed in old version
        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
        DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(className);
        if (dependencyNode != null) {
            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setClassName(className);
            apiSignature.setMethodName(methodName);
            apiSignature.setMethodParamList(paramList);
            apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
            errorResult.setApiSignature(apiSignature);
            return dependencyNode;
        }

        // if the method is existed in new version
        DependencyProperty newDependencyProperty = errorResult.getNewTreeResolver().getDependencyProperty();
        dependencyNode = newDependencyProperty.getDependencyNodeByClassName(className);
        if (dependencyNode != null) {
            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setClassName(className);
            apiSignature.setMethodName(methodName);
            apiSignature.setMethodParamList(paramList);
            apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
            errorResult.setApiSignature(apiSignature);
            return dependencyNode;
        }

        // if the class name of the ambiguous method is wrong reported by JDT
        MethodBodyInvocationVisitor invocationVisitor = new MethodBodyInvocationVisitor();
        errorResult.getOldAST().accept(new MethodBodyVisitor(errorLineNumber, invocationVisitor));
        List<MethodInvocationContext> invocationContextList = invocationVisitor.getInvocationContextList();
        for (MethodInvocationContext invocationContext : invocationContextList) {
            if (invocationContext.getStartLineNumber() <= errorLineNumber && invocationContext.getEndLineNumber() >= errorLineNumber
                    && methodName.equals(invocationContext.getMethodName())) {
                String belongedClassName = invocationContext.getBelongedClassName();
                dependencyNode = dependencyProperty.getDependencyNodeByClassName(belongedClassName);
                if (dependencyNode != null) {
                    ApiSignature apiSignature = new ApiSignature();
                    apiSignature.setClassName(className);
                    apiSignature.setMethodName(methodName);
                    apiSignature.setMethodParamList(paramList);
                    apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
                    errorResult.setApiSignature(apiSignature);
                    return dependencyNode;
                }
            }
        }
        return null;
    }
}
