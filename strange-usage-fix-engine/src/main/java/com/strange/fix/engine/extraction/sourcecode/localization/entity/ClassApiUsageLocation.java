package com.strange.fix.engine.extraction.sourcecode.localization.entity;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;

@Getter
@Setter
public class ClassApiUsageLocation extends ApiLocation {

    private Statement statement;

    private MethodDeclaration methodDeclaration;

    private FieldDeclaration fieldDeclaration;
}
