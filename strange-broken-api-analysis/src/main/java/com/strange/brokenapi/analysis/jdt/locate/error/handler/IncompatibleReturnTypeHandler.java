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

public class IncompatibleReturnTypeHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the complete method signature,
        // e.g., org.apache.hadoop.hive.ql.io.HiveOutputFormat.getHiveRecordWriter(JobConf, Path, Class, boolean, Properties, Progressable)
        String methodSignature = arguments.get(0);


        String fullMethodName = StrUtil.subBefore(methodSignature, "(", false);

        String className = StrUtil.subBefore(fullMethodName, ".", true);
        String methodName = StrUtil.subAfter(fullMethodName, ".", true);
        String paramsStr = StrUtil.subBetween(methodSignature, "(", ")");
        List<String> paramList = ClassUtil.parseParamTypeList(paramsStr);

        ApiSignature apiSignature = new ApiSignature();
        apiSignature.setClassName(className);
        apiSignature.setMethodName(methodName);
        apiSignature.setMethodParamList(paramList);
        apiSignature.setBrokenApiType(ApiTypeEnum.ABSTRACT_METHOD);
        errorResult.setApiSignature(apiSignature);

        List<String> split = StrUtil.split(methodSignature, '.');
        split.remove(split.size() - 1); // remove the method part
        className = ClassUtil.removeGenericType(StrUtil.join(".", split));
        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
        return dependencyProperty.getDependencyNodeByClassName(className);
    }
}
