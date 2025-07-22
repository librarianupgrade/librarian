package com.strange.brokenapi.analysis.jdt.locate.deprecation.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.DeprecationResult;
import com.strange.brokenapi.analysis.jdt.locate.deprecation.DeprecationHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;

import java.util.List;

public class UsingDeprecatedMethodHandler implements DeprecationHandler {
    @Override
    public DependencyNode handle(DeprecationResult deprecationResult) {
        // arguments[0] is the class name
        // arguments[1] is the method name
        // arguments[2] is the parameter list
        List<String> arguments = deprecationResult.getArguments();
        String className = ClassUtil.removeGenericType(arguments.get(0));
        String methodName = arguments.get(1);
        List<String> paramList = ClassUtil.parseParamTypeList(arguments.get(2));

        DependencyProperty dependencyProperty = deprecationResult.getOldTreeResolver().getDependencyProperty();
        DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(className);
        if (dependencyNode != null) {
            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setClassName(className);
            apiSignature.setMethodName(methodName);
            apiSignature.setMethodParamList(paramList);
            apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
            deprecationResult.setApiSignature(apiSignature);
            return dependencyNode;
        }
        return null;
    }
}
