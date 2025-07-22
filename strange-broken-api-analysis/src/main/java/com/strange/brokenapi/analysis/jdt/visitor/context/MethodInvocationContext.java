package com.strange.brokenapi.analysis.jdt.visitor.context;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MethodInvocationContext {
    private Integer startLineNumber;

    private Integer endLineNumber;

    private String methodName;

    private String belongedClassName;

    private String returnTypeClassName;

    private List<String> parameterList = new ArrayList<>();

    private String baseObjectName;

    private String baseObjectClassName;

    private List<String> methodExceptions = new ArrayList<>();
}
