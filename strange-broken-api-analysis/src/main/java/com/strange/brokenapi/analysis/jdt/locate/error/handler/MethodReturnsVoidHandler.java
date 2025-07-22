package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.jdt.visitor.MethodDetailsVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.context.MethodDetailsContext;

import java.util.List;

public class MethodReturnsVoidHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        Integer lineNumber = errorResult.getErrorLineNumber();
        List<String> arguments = errorResult.getArguments();
        // arguments is a empty list

        MethodDetailsVisitor methodDetailsVisitor = new MethodDetailsVisitor(lineNumber);
        errorResult.getNewAST().accept(methodDetailsVisitor);

        List<MethodDetailsContext> detailsContextList = methodDetailsVisitor.getDetailsContextList();
        for (MethodDetailsContext detailsContext : detailsContextList) {
            String belongedClassName = detailsContext.getBelongedClassName();
            DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();
            DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(belongedClassName);
            if (dependencyNode != null) {
                ApiSignature apiSignature = new ApiSignature();
                apiSignature.setClassName(belongedClassName);
                apiSignature.setMethodName(detailsContext.getMethodName());
                apiSignature.setMethodParamList(detailsContext.getParamTypeList());
                apiSignature.setBrokenApiType(ApiTypeEnum.ABSTRACT_METHOD);
                errorResult.setApiSignature(apiSignature);
                return dependencyNode;
            }
        }
        return null;
    }
}
