package com.strange.fix.engine.llm;

import lombok.Data;

import java.io.File;

@Data
public class LLMFixResult {
    private File fixCodeFile;

    private String llmPrompt;

    private String fixCode;

    public LLMFixResult() {

    }

    public LLMFixResult(File fixCodeFile, String llmPrompt, String fixCode) {
        this.fixCodeFile = fixCodeFile;
        this.llmPrompt = llmPrompt;
        this.fixCode = fixCode;
    }
}
