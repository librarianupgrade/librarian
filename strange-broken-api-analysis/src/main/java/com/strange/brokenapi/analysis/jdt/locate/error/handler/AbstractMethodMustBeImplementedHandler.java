package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;

import java.util.List;

public class AbstractMethodMustBeImplementedHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the method name
        // arguments[1] is the method parameter list
        // arguments[2] is the class to which the abstract method belongs
        // arguments[3] is the current class name which not implement the abstract method
        String methodName = arguments.get(0);
        String className = ClassUtil.removeGenericType(arguments.get(2));
        List<String> paramList = ClassUtil.parseParamTypeList(arguments.get(1));

        ApiSignature apiSignature = new ApiSignature();
        apiSignature.setMethodName(methodName);
        apiSignature.setMethodParamList(paramList);
        apiSignature.setClassName(className);
        apiSignature.setBrokenApiType(ApiTypeEnum.ABSTRACT_METHOD);
        errorResult.setApiSignature(apiSignature);

        // if the class is in the old version
        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
        DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(className);
        if (dependencyNode != null) return dependencyNode;

        // if the class is in the new version
        DependencyProperty newDependencyProperty = errorResult.getNewTreeResolver().getDependencyProperty();
        return newDependencyProperty.getDependencyNodeByClassName(className);
    }
}
