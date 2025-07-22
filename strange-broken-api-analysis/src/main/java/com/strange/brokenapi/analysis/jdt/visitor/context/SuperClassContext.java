package com.strange.brokenapi.analysis.jdt.visitor.context;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SuperClassContext {
    private String superClassName;

    private List<FieldDetailsContext> fieldNameList;

    private List<MethodDetailsContext> methodNameList;

    public SuperClassContext() {
        this.fieldNameList = new ArrayList<>();
        this.methodNameList = new ArrayList<>();
    }
}
