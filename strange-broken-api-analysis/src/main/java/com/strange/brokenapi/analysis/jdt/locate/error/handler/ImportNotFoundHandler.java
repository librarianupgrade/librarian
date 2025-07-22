package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.jdt.visitor.ImportVisitor;
import com.strange.common.utils.ClassUtil;

import java.util.List;

public class ImportNotFoundHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        Integer errorLineNumber = errorResult.getErrorLineNumber();
        List<String> arguments = errorResult.getArguments();
        // arguments[0] can be a package prefix of a class name or a class name
        String className = ClassUtil.removeGenericType(arguments.get(0));

        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();

        ImportVisitor importVisitor = new ImportVisitor(errorLineNumber);
        errorResult.getOldAST().accept(importVisitor);
        String importName = importVisitor.getImportName();

        if (importName.startsWith(className)) {
            boolean staticImport = importVisitor.isStaticImport();
            boolean onDemand = importVisitor.isOnDemand();
            if (!onDemand && staticImport) {
                importName = ClassUtil.removeLastClassName(importName);
            }

            ApiSignature apiSignature = new ApiSignature();
            apiSignature.setClassName(importName);
            apiSignature.setBrokenApiType(ApiTypeEnum.IMPORT);
            errorResult.setApiSignature(apiSignature);
            return dependencyProperty.getDependencyNodeByClassName(importName);
        }
        return null;
    }
}
