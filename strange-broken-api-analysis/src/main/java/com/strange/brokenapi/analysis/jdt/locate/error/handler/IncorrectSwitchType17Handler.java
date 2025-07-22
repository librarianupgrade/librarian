package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;

import java.util.List;

public class IncorrectSwitchType17Handler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the class name in switch structure
        String className = ClassUtil.removeGenericType(arguments.get(0));

        ApiSignature apiSignature = new ApiSignature();
        apiSignature.setClassName(className);
        apiSignature.setBrokenApiType(ApiTypeEnum.CLASS);
        errorResult.setApiSignature(apiSignature);

        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
        return dependencyProperty.getDependencyNodeByClassName(className);
    }
}
