package com.strange.fix.engine.extraction.sourcecode.localization;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.visitor.ClassApiDefinitionVisitor;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.List;

public class ClassApiDefinitionLocator extends ApiLocator {
    public ClassApiDefinitionLocator( File projectDir, ApiSignature apiSignature) {
        super(projectDir, apiSignature);
    }

    @Override
    protected List<ApiLocation> locateInFile(File sourceFile, CompilationUnit ast, ApiSignature apiSignature) {
        ClassApiDefinitionVisitor classApiDefinitionVisitor = new ClassApiDefinitionVisitor(ast, sourceFile, apiSignature);
        ast.accept(classApiDefinitionVisitor);
        return classApiDefinitionVisitor.getApiLocations();
    }
}
