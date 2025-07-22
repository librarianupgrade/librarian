package com.strange.brokenapi.analysis.jdt.visitor.context;

import lombok.Data;

@Data
public class FieldUsageContext {

    private String fieldName;

    private String className;

    private String belongedClassName;

    private Integer startLineNumber;

    private Integer endLineNumber;
}
