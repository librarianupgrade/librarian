package com.strange.fix.engine.extraction.sourcecode.localization.entity;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;

@Getter
@Setter
public class ConstructorApiInvocationLocation extends ApiLocation {
    private ClassInstanceCreation instanceCreationStatement;
}
