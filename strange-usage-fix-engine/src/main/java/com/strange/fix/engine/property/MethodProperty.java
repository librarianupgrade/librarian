package com.strange.fix.engine.property;

import lombok.Data;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.List;

@Data
public class MethodProperty {

    private String belongedClassName;

    private String methodName;

    private String returnTypeName;

    private List<String> parameters;

    private Integer startLineNumber;

    private Integer endLineNumber;

    private MethodDeclaration methodDeclaration;

    private String sourceCode;

    private boolean constructor;
}
