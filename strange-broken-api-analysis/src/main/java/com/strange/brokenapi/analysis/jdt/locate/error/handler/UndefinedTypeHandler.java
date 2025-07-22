package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import cn.hutool.core.util.StrUtil;
import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;
import com.strange.common.utils.JDTUtil;

import java.io.File;
import java.util.List;

public class UndefinedTypeHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the class name, not the fully qualified class name, e.g., FIXSession, DockerClient.BuildParameter
        String simpleClassName = StrUtil.split(arguments.get(0), ".").get(0);

        // find the undefined type in import
        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();

        File codeFile = errorResult.getCodeFile();
        List<String> importList = JDTUtil.getImports(codeFile);
        for (String importContent : importList) {
            if (ClassUtil.isEqualUnsafe(simpleClassName, importContent)) {
                String className = importContent.replace("*", simpleClassName);
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

        String packageName = JDTUtil.getPackageName(codeFile);
        String className = packageName + "." + simpleClassName;
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
