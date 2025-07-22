package com.strange.fix.engine.extraction.sourcecode.mapping.handler;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.fix.engine.extraction.sourcecode.mapping.RefactoringHandler;
import gr.uom.java.xmi.diff.PackageLevelRefactoring;
import gr.uom.java.xmi.diff.RenamePackageRefactoring;
import org.refactoringminer.api.Refactoring;

import java.util.List;
import java.util.Objects;

public class RenamePackageHandler implements RefactoringHandler {
    @Override
    public ApiSignature mapping(Refactoring refactoring, ApiSignature apiSignature) {
        ApiSignature clone = apiSignature.clone();

        RenamePackageRefactoring renamePackageRefactoring = (RenamePackageRefactoring) refactoring;
        String className = apiSignature.getClassName();
        List<PackageLevelRefactoring> moveClassRefactorings = renamePackageRefactoring.getMoveClassRefactorings();
        for (PackageLevelRefactoring moveClassRefactoring : moveClassRefactorings) {
            String originalClassName = moveClassRefactoring.getOriginalClassName();
            if (Objects.equals(originalClassName, className)) {
                String movedClassName = moveClassRefactoring.getMovedClassName();
                clone.setClassName(movedClassName);
                break;
            }
        }
        return clone;
    }

    @Override
    public boolean preCheck(Refactoring refactoring, ApiSignature apiSignature) {
        RenamePackageRefactoring renamePackageRefactoring = (RenamePackageRefactoring) refactoring;
        String className = apiSignature.getClassName();
        List<PackageLevelRefactoring> moveClassRefactorings = renamePackageRefactoring.getMoveClassRefactorings();
        for (PackageLevelRefactoring moveClassRefactoring : moveClassRefactorings) {
            String originalClassName = moveClassRefactoring.getOriginalClassName();
            if (Objects.equals(originalClassName, className)) {
                return true;
            }
        }
        return false;
    }
}
