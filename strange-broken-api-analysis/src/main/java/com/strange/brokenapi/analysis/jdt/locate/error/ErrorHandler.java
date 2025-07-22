package com.strange.brokenapi.analysis.jdt.locate.error;


import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.jdt.ErrorResult;

public interface ErrorHandler {

    DependencyNode handle(ErrorResult errorResult);

}
