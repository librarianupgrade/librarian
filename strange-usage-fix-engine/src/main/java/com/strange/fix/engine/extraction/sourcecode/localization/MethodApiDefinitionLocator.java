package com.strange.fix.engine.extraction.sourcecode.localization;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.visitor.MethodApiDefinitionVisitor;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.List;

public class MethodApiDefinitionLocator extends ApiLocator {

    public MethodApiDefinitionLocator( File projectDir, ApiSignature apiSignature) {
        super(projectDir, apiSignature);
    }

    @Override
    protected List<ApiLocation> locateInFile(File sourceFile, CompilationUnit ast, ApiSignature apiSignature) {
        MethodApiDefinitionVisitor methodApiDefinitionVisitor = new MethodApiDefinitionVisitor(ast, sourceFile, apiSignature);
        ast.accept(methodApiDefinitionVisitor);
        return methodApiDefinitionVisitor.getApiLocations();
    }
}
