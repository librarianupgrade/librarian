package com.strange.fix.engine.extraction.javadoc.entity;

import lombok.Data;

@Data
public class ClassDeprecation {

    private String originalClassName;

    private String javaDocContent;
}
