package com.strange.fix.engine.extraction.sourcecode.localization.entity;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

@Getter
@Setter
public class AbstractMethodApiLocation extends ApiLocation {

    private TypeDeclaration typeDeclaration;

    private MethodDeclaration methodDeclaration;
}
