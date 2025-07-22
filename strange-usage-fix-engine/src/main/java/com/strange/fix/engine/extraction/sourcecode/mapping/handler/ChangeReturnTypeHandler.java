package com.strange.fix.engine.extraction.sourcecode.mapping.handler;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.mapping.RefactoringHandler;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.diff.ChangeReturnTypeRefactoring;
import org.refactoringminer.api.Refactoring;

import java.util.List;
import java.util.Objects;

public class ChangeReturnTypeHandler implements RefactoringHandler {
    @Override
    public ApiSignature mapping(Refactoring refactoring, ApiSignature apiSignature) {
        ApiSignature clone = apiSignature.clone();
        ChangeReturnTypeRefactoring changeReturnTypeRefactoring = (ChangeReturnTypeRefactoring) refactoring;
        UMLType changedType = changeReturnTypeRefactoring.getChangedType();
        clone.setMethodReturnType(changedType.toString());
        return clone;
    }

    @Override
    public boolean preCheck(Refactoring refactoring, ApiSignature apiSignature) {
        String targetMethodName = apiSignature.getMethodName();
        List<String> targetMethodParamList = apiSignature.getMethodParamList();
        ChangeReturnTypeRefactoring changeReturnTypeRefactoring = (ChangeReturnTypeRefactoring) refactoring;

        UMLOperation originalOperation = changeReturnTypeRefactoring.getOperationBefore();
        String originalMethodName = originalOperation.getName();
        List<UMLType> originalParameterTypeList = originalOperation.getParameterTypeList();

        if (!Objects.equals(targetMethodName, originalMethodName)) return false;
        if (targetMethodParamList.size() != originalParameterTypeList.size()) return false;
        for (int i = 0; i < targetMethodParamList.size(); i++) {
            if (!isEqualSimpleType(targetMethodParamList.get(i), originalParameterTypeList.get(i).getClassType())) {
                return false;
            }
        }
        UMLType originalType = changeReturnTypeRefactoring.getOriginalType();
        apiSignature.setMethodReturnType(originalType.toString());
        return true;
    }

    private boolean isEqualSimpleType(String targetTypeName, String actualTypeName) {
        if (Objects.equals(targetTypeName, "null")) return true;
        String targetSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(targetTypeName));
        String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(actualTypeName));
        return Objects.equals(targetSimpleClassName, actualSimpleClassName);
    }
}
