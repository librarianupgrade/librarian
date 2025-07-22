package com.strange.fix.engine.extraction.sourcecode.localization.entity;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;

@Setter
@Getter
public class FieldApiUsageLocation extends ApiLocation {
    private MethodDeclaration methodDeclaration;

    private FieldDeclaration fieldDeclaration;

    private CompilationUnit compilationUnit;
}
