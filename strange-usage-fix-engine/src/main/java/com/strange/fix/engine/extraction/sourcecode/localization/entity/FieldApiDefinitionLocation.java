package com.strange.fix.engine.extraction.sourcecode.localization.entity;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;

@Getter
@Setter
public class FieldApiDefinitionLocation extends ApiLocation {
    private FieldDeclaration fieldDeclaration;

    private CompilationUnit compilationUnit;
}
