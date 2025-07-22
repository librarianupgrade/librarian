package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import cn.hutool.core.util.StrUtil;
import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.jdt.visitor.SuperClassVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.context.FieldDetailsContext;
import com.strange.brokenapi.analysis.jdt.visitor.context.SuperClassContext;
import com.strange.common.utils.ClassUtil;
import com.strange.common.utils.JDTUtil;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class UnresolvedVariableHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        Integer errorLineNumber = errorResult.getErrorLineNumber();
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the name of unresolved variable name
        String variableName = arguments.get(0);
        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();

        // if the unresolved variable is existing in super class
        SuperClassVisitor superClassVisitor = new SuperClassVisitor();
        errorResult.getOldAST().accept(superClassVisitor);
        SuperClassContext superClassContext = superClassVisitor.getSuperClassContext();
        List<FieldDetailsContext> fieldDetailsContextList = superClassContext.getFieldNameList();
        for (FieldDetailsContext fieldDetailsContext : fieldDetailsContextList) {
            if (Objects.equals(variableName, fieldDetailsContext.getFieldName())) {
                String superClassName = ClassUtil.removeGenericType(superClassContext.getSuperClassName());
                DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(superClassName);
                if (dependencyNode != null) {
                    ApiSignature apiSignature = new ApiSignature();
                    apiSignature.setClassName(superClassName);
                    apiSignature.setFieldName(variableName);
                    apiSignature.setBrokenApiType(ApiTypeEnum.FIELD);
                    errorResult.setApiSignature(apiSignature);
                    return dependencyNode;
                }
            }
        }

        // if the unresolved variable is existing in import
        String simpleClassName = StrUtil.split(variableName, ".").get(0);
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
