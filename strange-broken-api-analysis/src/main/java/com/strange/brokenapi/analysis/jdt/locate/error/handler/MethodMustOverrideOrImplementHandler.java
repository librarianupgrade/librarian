package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.maven.MavenDependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.jdt.visitor.SuperClassVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.context.SuperClassContext;
import com.strange.common.utils.ClassUtil;

import java.util.List;

public class MethodMustOverrideOrImplementHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the method name
        // arguments[1] is the method's parameter list
        // arguments[2] is the class name of the method
        String methodName = arguments.get(0);
        List<String> paramList = ClassUtil.parseParamTypeList(arguments.get(1));
        String className = ClassUtil.removeGenericType(arguments.get(2));

        // if the class name is the class of abstracted method which belonged to
        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
        DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(className);
        if (dependencyNode != null && dependencyNode != MavenDependencyNode.SELF) {
            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setClassName(className);
            apiSignature.setMethodName(methodName);
            apiSignature.setMethodParamList(paramList);
            apiSignature.setBrokenApiType(ApiTypeEnum.ABSTRACT_METHOD);
            errorResult.setApiSignature(apiSignature);
            return dependencyNode;
        }

        // if the class name is not the class of abstracted method which belonged to, then we direct search the super class
        SuperClassVisitor superClassVisitor = new SuperClassVisitor(className);
        errorResult.getOldAST().accept(superClassVisitor);
        SuperClassContext superClassContext = superClassVisitor.getSuperClassContext();
        String superClassName = superClassContext.getSuperClassName();
        if (superClassName != null) {
            superClassName = ClassUtil.removeGenericType(superClassContext.getSuperClassName());
            dependencyNode = dependencyProperty.getDependencyNodeByClassName(superClassName);
            if (dependencyNode != null) {
                ApiSignature apiSignature = new ApiSignature();
                apiSignature.setClassName(superClassName);
                apiSignature.setMethodName(methodName);
                apiSignature.setMethodParamList(paramList);
                apiSignature.setBrokenApiType(ApiTypeEnum.ABSTRACT_METHOD);
                errorResult.setApiSignature(apiSignature);
                return dependencyNode;
            }
        }
        return null;
    }
}
