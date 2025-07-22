package com.strange.fix.engine.property;

import lombok.Data;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;

import java.util.ArrayList;
import java.util.List;

@Data
public class ClassProperty {
    private String className;

    private String superClassName;

    private List<String> interfaces;

    private AbstractTypeDeclaration classDeclaration;

    private String sourceCode;

    private List<AnnotationProperty> annotationPropertyList;

    private List<FieldProperty> fieldPropertyList;

    private List<MethodProperty> methodPropertyList;

    public ClassProperty() {
        this.fieldPropertyList = new ArrayList<>();
        this.methodPropertyList = new ArrayList<>();
        this.annotationPropertyList = new ArrayList<>();
    }

    public void addMethodProperty(MethodProperty methodProperty) {
        this.methodPropertyList.add(methodProperty);
    }

    public void addFieldProperty(FieldProperty fieldProperty) {
        this.fieldPropertyList.add(fieldProperty);
    }

    public void addAnnotationProperty(AnnotationProperty annotationProperty) {
        this.annotationPropertyList.add(annotationProperty);
    }
}
