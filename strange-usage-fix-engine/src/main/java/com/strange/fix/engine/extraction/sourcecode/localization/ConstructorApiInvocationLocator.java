package com.strange.fix.engine.extraction.sourcecode.localization;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.visitor.ConstructorApiInvocationVisitor;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.List;

public class ConstructorApiInvocationLocator extends ApiLocator {
    public ConstructorApiInvocationLocator( File projectDir, ApiSignature apiSignature) {
        super(projectDir, apiSignature);
    }

    @Override
    protected List<ApiLocation> locateInFile(File sourceFile, CompilationUnit ast, ApiSignature apiSignature) {
        ConstructorApiInvocationVisitor constructorApiInvocationVisitor = new ConstructorApiInvocationVisitor(ast, sourceFile, apiSignature);
        ast.accept(constructorApiInvocationVisitor);
        return constructorApiInvocationVisitor.getApiLocations();
    }
}
