package com.strange.brokenapi.analysis.jdt.visitor.context;

import lombok.Data;

import java.util.List;

@Data
public class MethodDetailsContext {
    private String methodName;

    private String belongedClassName;

    private boolean isLambdaFunction;

    private List<String> paramTypeList;

    private Integer startLineNumber;

    private Integer endLineNumber;
}
