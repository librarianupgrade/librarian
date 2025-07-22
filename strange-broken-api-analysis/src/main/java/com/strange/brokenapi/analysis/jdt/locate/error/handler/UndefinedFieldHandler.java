package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyClassUsageVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyFieldUsageVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.context.ClassUsageContext;
import com.strange.brokenapi.analysis.jdt.visitor.context.FieldUsageContext;
import com.strange.common.utils.ClassUtil;

import java.util.List;
import java.util.Objects;

public class UndefinedFieldHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        Integer errorLineNumber = errorResult.getErrorLineNumber();
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the name of the field
        String fieldName = arguments.get(0);

        MethodBodyFieldUsageVisitor fieldUsageVisitor = new MethodBodyFieldUsageVisitor();
        errorResult.getOldAST().accept(fieldUsageVisitor);
        List<FieldUsageContext> fieldUsageContextList = fieldUsageVisitor.getFieldUsageContextList();

        // when is an undefined field
        for (FieldUsageContext fieldUsageContext : fieldUsageContextList) {
            if (fieldUsageContext.getStartLineNumber() >= errorLineNumber
                    && fieldUsageContext.getEndLineNumber() <= errorLineNumber
                    && fieldName.equals(fieldUsageContext.getFieldName())) {
                String belongedClassName = fieldUsageContext.getBelongedClassName();
                DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
                DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(belongedClassName);
                if (dependencyNode != null) {
                    ApiSignature apiSignature = new ApiSignature();
                    apiSignature.setClassName(fieldUsageContext.getClassName());
                    apiSignature.setFieldBelongedClassName(belongedClassName);
                    apiSignature.setFieldName(fieldName);
                    apiSignature.setBrokenApiType(ApiTypeEnum.FIELD);
                    errorResult.setApiSignature(apiSignature);
                    return dependencyNode;
                }
            }
        }

        // when is an undefined inner class
        MethodBodyClassUsageVisitor methodBodyClassUsageVisitor = new MethodBodyClassUsageVisitor();
        errorResult.getOldAST().accept(methodBodyClassUsageVisitor);
        List<ClassUsageContext> classUsageContextList = methodBodyClassUsageVisitor.getClassUsageContextList();
        for (ClassUsageContext classUsageContext : classUsageContextList) {
            if (classUsageContext.getStartLineNumber() >= errorLineNumber
                    && classUsageContext.getEndLineNumber() <= errorLineNumber
                    && isEqualClassName(classUsageContext.getClassName(), fieldName)) {
                String className = ClassUtil.removeGenericType(classUsageContext.getClassName());
                DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
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

    private boolean isEqualClassName(String actualClassName, String exceptedClassName) {
        if (Objects.equals(exceptedClassName, actualClassName)) return true;
        exceptedClassName = ClassUtil.removeGenericType(exceptedClassName);
        actualClassName = ClassUtil.removeGenericType(actualClassName);
        if (Objects.equals(exceptedClassName, actualClassName)) return true;
        return Objects.equals(ClassUtil.getSimpleClassName(exceptedClassName), ClassUtil.getSimpleClassName(actualClassName));
    }
}
