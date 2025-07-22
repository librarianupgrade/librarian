package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;

import java.util.List;

public class MethodNameClashHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the method name causing the conflict
        // arguments[1] is the current method's actual parameter list
        // arguments[2] is the class name where the overridden method is located
        // arguments[3] is the parameter list of the method that conflicts with the current method
        // arguments[4] is the class name where the method originally defined
        String className = ClassUtil.removeGenericType(arguments.get(4));
        String methodName = arguments.get(0);
        List<String> paramList = ClassUtil.parseParamTypeList(arguments.get(3));

        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
        DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(className);
        if (dependencyNode != null) {
            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setMethodName(methodName);
            apiSignature.setClassName(className);
            apiSignature.setMethodParamList(paramList);
            apiSignature.setBrokenApiType(ApiTypeEnum.ABSTRACT_METHOD);
            errorResult.setApiSignature(apiSignature);
            return dependencyNode;
        }
        return null;
    }
}
