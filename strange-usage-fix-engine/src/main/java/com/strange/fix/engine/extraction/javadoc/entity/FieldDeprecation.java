package com.strange.fix.engine.extraction.javadoc.entity;

import lombok.Data;

@Data
public class FieldDeprecation {

    private String originalClassName;

    private String originalFieldName;

    private String javaDocContent;
}
