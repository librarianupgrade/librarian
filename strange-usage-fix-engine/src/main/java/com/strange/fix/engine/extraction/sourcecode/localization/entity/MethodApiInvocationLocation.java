package com.strange.fix.engine.extraction.sourcecode.localization.entity;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.MethodInvocation;

@Setter
@Getter
public class MethodApiInvocationLocation extends ApiLocation {

    private MethodInvocation invocationNode;
}
