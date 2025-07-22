package com.strange.fix.engine.extraction.sourcecode.mapping.handler;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.mapping.RefactoringHandler;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.diff.ChangeVariableTypeRefactoring;
import org.refactoringminer.api.Refactoring;

import java.util.List;
import java.util.Objects;

public class ChangeParameterHandler implements RefactoringHandler {

    @Override
    public ApiSignature mapping(Refactoring refactoring, ApiSignature apiSignature) {
        ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) refactoring;
        UMLOperation operationAfter = (UMLOperation) changeVariableTypeRefactoring.getOperationAfter();
        ApiSignature clone = apiSignature.clone();

        List<UMLType> parameterTypeList = operationAfter.getParameterTypeList();
        List<String> newParamTypeList = parameterTypeList.stream().map(UMLType::toQualifiedString).toList();

        clone.setMethodParamList(newParamTypeList.stream().map(ClassUtil::removeGenericType).toList());
        return clone;
    }

    @Override
    public boolean preCheck(Refactoring refactoring, ApiSignature apiSignature) {
        String targetMethodName = apiSignature.getMethodName();
        List<String> targetMethodParamList = apiSignature.getMethodParamList();

        ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) refactoring;
        UMLOperation operationBefore = (UMLOperation) changeVariableTypeRefactoring.getOperationBefore();

        if (!Objects.equals(targetMethodName, operationBefore.getName())) return false;
        List<UMLType> parameterTypeList = operationBefore.getParameterTypeList();
        if (parameterTypeList.size() != targetMethodParamList.size()) return false;
        for (int i = 0; i < targetMethodParamList.size(); i++) {
            if (!isEqualSimpleType(targetMethodParamList.get(i), parameterTypeList.get(i).getClassType())) {
                return false;
            }
        }
        return true;
    }

    private boolean isEqualSimpleType(String targetTypeName, String actualTypeName) {
        if (Objects.equals(targetTypeName, "null")) return true;
        String targetSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(targetTypeName));
        String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(actualTypeName));
        return Objects.equals(targetSimpleClassName, actualSimpleClassName);
    }
}
