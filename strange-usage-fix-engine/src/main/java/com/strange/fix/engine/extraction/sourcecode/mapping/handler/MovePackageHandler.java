package com.strange.fix.engine.extraction.sourcecode.mapping.handler;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.fix.engine.extraction.sourcecode.mapping.RefactoringHandler;
import org.refactoringminer.api.Refactoring;

public class MovePackageHandler implements RefactoringHandler  {
    @Override
    public ApiSignature mapping(Refactoring refactoring, ApiSignature apiSignature) {
        ApiSignature clone = apiSignature.clone();
        return clone;
    }

    @Override
    public boolean preCheck(Refactoring refactoring, ApiSignature apiSignature) {
        return false;
    }
}
