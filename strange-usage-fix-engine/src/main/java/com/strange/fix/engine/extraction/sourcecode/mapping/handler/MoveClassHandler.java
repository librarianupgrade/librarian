package com.strange.fix.engine.extraction.sourcecode.mapping.handler;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.mapping.RefactoringHandler;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.diff.MoveClassRefactoring;
import org.refactoringminer.api.Refactoring;

import java.util.Objects;

public class MoveClassHandler implements RefactoringHandler {
    @Override
    public ApiSignature mapping(Refactoring refactoring, ApiSignature apiSignature) {
        ApiSignature clone = apiSignature.clone();

        MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) refactoring;
        UMLClass movedClass = moveClassRefactoring.getMovedClass();
        clone.setClassName(movedClass.getName());
        return clone;
    }

    @Override
    public boolean preCheck(Refactoring refactoring, ApiSignature apiSignature) {
        MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) refactoring;
        String originalClassName = moveClassRefactoring.getOriginalClass().getName();
        String targetClassName = ClassUtil.removeGenericType(apiSignature.getClassName());
        if (Objects.equals(originalClassName, targetClassName)) return true;
        return Objects.equals(ClassUtil.getSimpleClassName(originalClassName), ClassUtil.getSimpleClassName(targetClassName));
    }
}
