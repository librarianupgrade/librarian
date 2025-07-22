package com.strange.brokenapi.analysis.jdt.visitor.context;

import lombok.Data;

@Data
public class FieldDetailsContext {
    private String fieldName;

    private String fieldClassName;

    private String belongedClassName;

    private Integer startLineNumber;

    private Integer endLineNumber;
}
