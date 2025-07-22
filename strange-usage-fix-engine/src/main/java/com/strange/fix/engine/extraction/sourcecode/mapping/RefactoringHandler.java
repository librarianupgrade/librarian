package com.strange.fix.engine.extraction.sourcecode.mapping;

import com.strange.brokenapi.analysis.ApiSignature;
import org.refactoringminer.api.Refactoring;

public interface RefactoringHandler {
    default ApiSignature handle(Refactoring refactoring, ApiSignature apiSignature) {
        if (preCheck(refactoring, apiSignature)) {
            return mapping(refactoring, apiSignature);
        } else {
            return null;
        }
    }

    ApiSignature mapping(Refactoring refactoring, ApiSignature apiSignature);

    // Verifies if the refactoring corresponds to the provided API signature.
    boolean preCheck(Refactoring refactoring, ApiSignature apiSignature);

}
