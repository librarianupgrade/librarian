package com.strange.fix.engine.llm;

import lombok.Data;

@Data
public class ShadowFixCase {
    private String previousCode;

    private String refactoredCode;

    public ShadowFixCase(String previousCode, String refactoredCode) {
        this.previousCode = previousCode;
        this.refactoredCode = refactoredCode;
    }
}
