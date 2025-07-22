package com.strange.fix.engine.extraction.javadoc.entity;

import lombok.Data;

import java.util.List;

@Data
public class MethodDeprecation {

    private String originalClassName;

    private String originalMethodName;

    private List<String> originalMethodParameters;

    private String javaDocContent;
}
