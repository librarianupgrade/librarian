package com.strange.fix.engine.property;

import lombok.Data;
import org.eclipse.jdt.core.dom.FieldDeclaration;

@Data
public class FieldProperty {
    private String belongedClassName;

    private String fieldType;

    private String fieldName;

    private FieldDeclaration fieldDeclaration;

    private String sourceCode;
}
