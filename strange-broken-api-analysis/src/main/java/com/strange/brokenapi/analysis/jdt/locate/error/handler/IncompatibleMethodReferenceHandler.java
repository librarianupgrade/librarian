package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;

import java.util.List;

public class IncompatibleMethodReferenceHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the method name, arguments[1] is the method's parameter list,
        // arguments[2] is the class name of the method, arguments[3] is the class name of expected return value of the method,
        // arguments[4] is the class name of actual return value of the method
        String className = ClassUtil.removeGenericType(arguments.get(2));
        String methodName = arguments.get(0);
        List<String> paramList = ClassUtil.parseParamTypeList(arguments.get(1));

        ApiSignature apiSignature = new ApiSignature();
        apiSignature.setClassName(className);
        apiSignature.setMethodName(methodName);
        apiSignature.setMethodParamList(paramList);
        apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
        errorResult.setApiSignature(apiSignature);

        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
        return dependencyProperty.getDependencyNodeByClassName(className);
    }
}
