package com.strange.fix.engine.extraction.sourcecode.mapping.handler;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.mapping.RefactoringHandler;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import org.refactoringminer.api.Refactoring;

import java.util.Objects;

public class RenameClassHandler implements RefactoringHandler {
    @Override
    public ApiSignature mapping(Refactoring refactoring, ApiSignature apiSignature) {
        ApiSignature clone = apiSignature.clone();

        RenameClassRefactoring renameClassRefactoring = (RenameClassRefactoring) refactoring;
        UMLClass renamedClass = renameClassRefactoring.getRenamedClass();
        clone.setClassName(renamedClass.getName());
        return clone;
    }

    @Override
    public boolean preCheck(Refactoring refactoring, ApiSignature apiSignature) {
        RenameClassRefactoring renameClassRefactoring = (RenameClassRefactoring) refactoring;

        String originalClassName = renameClassRefactoring.getOriginalClass().getName();
        String targetClassName = ClassUtil.removeGenericType(apiSignature.getClassName());
        if (Objects.equals(originalClassName, targetClassName)) return true;
        return Objects.equals(ClassUtil.getSimpleClassName(originalClassName), ClassUtil.getSimpleClassName(targetClassName));
    }
}
