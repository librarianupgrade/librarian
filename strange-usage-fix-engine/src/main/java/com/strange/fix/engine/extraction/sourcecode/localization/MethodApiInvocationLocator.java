package com.strange.fix.engine.extraction.sourcecode.localization;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.visitor.MethodApiInvocationVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.List;

public class MethodApiInvocationLocator extends ApiLocator {

    public MethodApiInvocationLocator(File projectDir, ApiSignature apiSignature) {
        super(projectDir, apiSignature);
    }

    @Override
    protected List<ApiLocation> locateInFile(File sourceFile, CompilationUnit ast, ApiSignature apiSignature) {
        MethodApiInvocationVisitor methodApiInvocationVisitor = new MethodApiInvocationVisitor(ast, sourceFile, apiSignature);
        ast.accept(methodApiInvocationVisitor);
        return methodApiInvocationVisitor.getApiLocations();
    }
}
