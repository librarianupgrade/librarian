package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;

import java.util.List;

public class NonGenericTypeHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is a class name
        // arguments[1] is a list of generics
        String className = ClassUtil.removeGenericType(arguments.get(0));
        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
        DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(className);

        if (dependencyNode != null) {
            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setClassName(className);
            apiSignature.setBrokenApiType(ApiTypeEnum.CLASS);
            errorResult.setApiSignature(apiSignature);
            return dependencyNode;
        }
        return null;
    }
}
