package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import cn.hutool.core.util.StrUtil;
import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;

import java.util.List;

public class NotVisibleConstructorHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the class name of constructor
        // arguments[1] is the constructor's parameter list
        String className = ClassUtil.removeGenericType(arguments.get(0));
        List<String> paramList = ClassUtil.parseParamTypeList(arguments.get(1));

        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
        DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(className);

        if (dependencyNode != null) {
            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setClassName(className);
            String simpleName = StrUtil.subAfter(className, ".", true);
            apiSignature.setMethodName(simpleName);
            apiSignature.setMethodParamList(paramList);
            apiSignature.setBrokenApiType(ApiTypeEnum.CONSTRUCTOR);
            errorResult.setApiSignature(apiSignature);
            return dependencyNode;
        }
        return null;
    }
}
