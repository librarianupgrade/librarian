package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import cn.hutool.core.util.StrUtil;
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
import com.strange.common.utils.JDTUtil;

import java.io.File;
import java.util.List;

public class UndefinedNameHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        Integer errorLineNumber = errorResult.getErrorLineNumber();
        List<String> arguments = errorResult.getArguments();
        // the name of undefined symbol
        String undefinedName = arguments.get(0);

        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();

        // if the undefined name is in method invocation (Field Name) TODO
        MethodBodyInvocationVisitor invocationVisitor = new MethodBodyInvocationVisitor();
        errorResult.getOldAST().accept(new MethodBodyVisitor(errorLineNumber, invocationVisitor));
        List<MethodInvocationContext> invocationContextList = invocationVisitor.getInvocationContextList();
        for (MethodInvocationContext methodInvocationContext : invocationContextList) {
            if (methodInvocationContext.getStartLineNumber() <= errorLineNumber
                    && methodInvocationContext.getEndLineNumber() >= errorLineNumber && undefinedName.equals(methodInvocationContext.getBaseObjectName())) {
                String belongedClassName = methodInvocationContext.getBelongedClassName();
                DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(belongedClassName);
                if (dependencyNode != null) {
                    ApiSignature apiSignature = new ApiSignature();
                    apiSignature.setClassName(belongedClassName);
                    apiSignature.setFieldName(undefinedName);
                    apiSignature.setBrokenApiType(ApiTypeEnum.FIELD);
                    errorResult.setApiSignature(apiSignature);
                    return dependencyNode;
                }
            }
        }

        // if the undefined name is in import
        String simpleClassName = StrUtil.split(undefinedName, ".").get(0);
        File codeFile = errorResult.getCodeFile();
        List<String> importList = JDTUtil.getImports(codeFile);
        for (String importContent : importList) {
            if (ClassUtil.isEqualUnsafe(simpleClassName, importContent)) {
                String className = importContent.replace("*", simpleClassName);
                DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(className);
                if (dependencyNode != null) {
                    ApiSignature apiSignature = new ApiSignature();
                    apiSignature.setClassName(className);
                    apiSignature.setBrokenApiType(ApiTypeEnum.CLASS);
                    errorResult.setApiSignature(apiSignature);
                    return dependencyNode;
                }
            }
        }
        return null;
    }
}
