package com.strange.brokenapi.analysis.jdt.locate.deprecation;


import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.jdt.DeprecationResult;

public interface DeprecationHandler {
    DependencyNode handle(DeprecationResult deprecationResult);
}
