package com.strange.fix.engine.extraction.sourcecode.localization.entity;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

@Getter
@Setter
public class ClassApiDefinitionLocation extends ApiLocation {
    private TypeDeclaration typeDeclaration;

    private CompilationUnit compilationUnit;
}
