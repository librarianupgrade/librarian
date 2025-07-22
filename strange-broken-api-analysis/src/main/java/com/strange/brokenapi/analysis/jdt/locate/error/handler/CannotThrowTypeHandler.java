package com.strange.brokenapi.analysis.jdt.locate.error.handler;


import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;

public class CannotThrowTypeHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        return null;
    }
}
