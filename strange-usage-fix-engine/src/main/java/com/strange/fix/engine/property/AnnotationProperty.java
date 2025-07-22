package com.strange.fix.engine.property;

import lombok.Data;
import org.eclipse.jdt.core.dom.Annotation;

@Data
public class AnnotationProperty {
    private Annotation annotation;

    private String belongedClassName;
}
