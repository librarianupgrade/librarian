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

public class AmbiguousMethodHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        Integer errorLineNumber = errorResult.getErrorLineNumber();
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the class name of the method
        // arguments[1] is the method name
        // arguments[2] is the list of parameters.
        String className = ClassUtil.removeGenericType(arguments.get(0));
        String methodName = arguments.get(1);

        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
        DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(className);
        if (dependencyNode != null) {
            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setClassName(className);
            apiSignature.setMethodName(methodName);
            List<String> paramList = ClassUtil.parseParamTypeList(arguments.get(1));
            apiSignature.setMethodParamList(paramList);
            apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
            errorResult.setApiSignature(apiSignature);
            return dependencyNode;
        }

        // if the class name of the ambiguous method is wrong reported by JDT
        MethodBodyInvocationVisitor invocationVisitor = new MethodBodyInvocationVisitor();
        MethodBodyVisitor bodyVisitor = new MethodBodyVisitor(errorLineNumber, invocationVisitor);
        errorResult.getOldAST().accept(bodyVisitor);

        List<MethodInvocationContext> invocationContextList = invocationVisitor.getInvocationContextList();
        for (MethodInvocationContext methodInvocationContext : invocationContextList) {
            if (methodInvocationContext.getStartLineNumber() >= errorLineNumber && errorLineNumber <= methodInvocationContext.getEndLineNumber()
                    && methodName.equals(methodInvocationContext.getMethodName())) {
                String belongedClassName = methodInvocationContext.getBelongedClassName();

                ApiSignature apiSignature = new ApiSignature();
                apiSignature.setClassName(belongedClassName);
                apiSignature.setMethodName(methodName);
                List<String> paramList = ClassUtil.parseParamTypeList(arguments.get(1));
                apiSignature.setMethodParamList(paramList);
                apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
                errorResult.setApiSignature(apiSignature);

                return dependencyProperty.getDependencyNodeByClassName(belongedClassName);
            }
        }
        return null;
    }
}
