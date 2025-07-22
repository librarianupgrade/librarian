package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;

import java.util.List;

public class NotVisibleFieldHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the field name
        // arguments[1] is the class name to which the field belongs
        String fieldName = arguments.get(0);
        String className = ClassUtil.removeGenericType(arguments.get(1));

        // if the class name existed in old version
        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
        DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(className);
        if (dependencyNode != null) {
            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setClassName(className);
            apiSignature.setFieldName(fieldName);
            apiSignature.setBrokenApiType(ApiTypeEnum.FIELD);
            errorResult.setApiSignature(apiSignature);
            return dependencyNode;
        }

        // if the class name existed in new version
        DependencyProperty newDependencyProperty = errorResult.getNewTreeResolver().getDependencyProperty();
        dependencyNode = newDependencyProperty.getDependencyNodeByClassName(className);
        if (dependencyNode != null) {
            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setClassName(className);
            apiSignature.setFieldName(fieldName);
            apiSignature.setBrokenApiType(ApiTypeEnum.FIELD);
            errorResult.setApiSignature(apiSignature);
            return dependencyNode;
        }

        return null;
    }
}
