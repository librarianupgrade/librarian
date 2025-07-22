package com.strange.fix.engine.extraction.sourcecode.localization.entity;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

@Getter
@Setter
public class MethodApiDefinitionLocation extends ApiLocation {
    private CompilationUnit compilationUnit;

    private MethodDeclaration methodDeclaration;
}
