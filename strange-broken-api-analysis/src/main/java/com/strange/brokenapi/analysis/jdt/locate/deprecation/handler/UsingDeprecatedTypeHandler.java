package com.strange.brokenapi.analysis.jdt.locate.deprecation.handler;


import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.DeprecationResult;
import com.strange.brokenapi.analysis.jdt.locate.deprecation.DeprecationHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;

import java.util.List;

public class UsingDeprecatedTypeHandler implements DeprecationHandler {
    @Override
    public DependencyNode handle(DeprecationResult deprecationResult) {
        // arguments[0] is the class name
        List<String> arguments = deprecationResult.getArguments();
        String className = ClassUtil.removeGenericType(arguments.get(0));
        DependencyProperty dependencyProperty = deprecationResult.getOldTreeResolver().getDependencyProperty();
        DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(className);
        if (dependencyNode != null) {
            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setClassName(className);
            apiSignature.setBrokenApiType(ApiTypeEnum.CLASS);
            deprecationResult.setApiSignature(apiSignature);
            return dependencyNode;
        }
        return null;
    }
}
