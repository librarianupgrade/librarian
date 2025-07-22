package com.strange.fix.engine.extraction.sourcecode.mapping.handler;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.mapping.RefactoringHandler;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.diff.ChangeAttributeTypeRefactoring;
import org.refactoringminer.api.Refactoring;

import java.util.Objects;

public class ChangeAttributeTypeHandler implements RefactoringHandler {
    @Override
    public ApiSignature mapping(Refactoring refactoring, ApiSignature apiSignature) {
        ApiSignature clone = apiSignature.clone();
        ChangeAttributeTypeRefactoring changeAttributeTypeRefactoring = (ChangeAttributeTypeRefactoring) refactoring;

        UMLAttribute changedTypeAttribute = changeAttributeTypeRefactoring.getChangedTypeAttribute();
        String fieldClassType = changedTypeAttribute.getType().getClassType();
        clone.setClassName(fieldClassType);
        clone.setFieldName(changedTypeAttribute.getName());

        String fieldBelongedClassName = ClassUtil.removeGenericType(changedTypeAttribute.getClassName());
        clone.setFieldBelongedClassName(fieldBelongedClassName);

        return clone;
    }

    @Override
    public boolean preCheck(Refactoring refactoring, ApiSignature apiSignature) {
        ChangeAttributeTypeRefactoring changeAttributeTypeRefactoring = (ChangeAttributeTypeRefactoring) refactoring;

        UMLAttribute originalAttribute = changeAttributeTypeRefactoring.getOriginalAttribute();
        String fieldBelongedClassName = originalAttribute.getClassName();
        String fieldName = originalAttribute.getName();
        String exceptedFieldBelongedClassName = apiSignature.getFieldBelongedClassName();
        return Objects.equals(fieldName, apiSignature.getFieldName())
                && isEqualClassName(fieldBelongedClassName, exceptedFieldBelongedClassName)
                && isEqualClassName(originalAttribute.getType().getClassType(), apiSignature.getClassName());
    }

    private boolean isEqualClassName(String targetClassName, String exceptedClassName) {
        if (Objects.equals(exceptedClassName, targetClassName)) return true;
        exceptedClassName = ClassUtil.removeGenericType(exceptedClassName);
        targetClassName = ClassUtil.removeGenericType(targetClassName);
        if (Objects.equals(exceptedClassName, targetClassName)) return true;
        return Objects.equals(ClassUtil.getSimpleClassName(exceptedClassName), ClassUtil.getSimpleClassName(targetClassName));
    }
}
