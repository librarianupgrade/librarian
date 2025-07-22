package com.strange.brokenapi.analysis.jdt.visitor.context;

import lombok.Data;

@Data
public class ClassUsageContext {
    private String className;

    private Integer startLineNumber;

    private Integer endLineNumber;
}
