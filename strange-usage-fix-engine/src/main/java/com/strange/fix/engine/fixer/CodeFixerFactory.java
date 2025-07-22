package com.strange.fix.engine.fixer;

import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.brokenapi.analysis.jdt.ErrorResult;

public class CodeFixerFactory {
    public static CodeFixer getCodeFixer(BrokenApiUsage brokenApiUsage) {
        ErrorResult errorResult = brokenApiUsage.getErrorResult();
        String errorType = errorResult.getErrorType();
        return switch (errorType) {
            case "MethodMustOverrideOrImplement", "AbstractMethodMustBeImplemented" -> new RuleCodeFixer();
            default -> new LLMCodeFixer();
        };
    }
}
