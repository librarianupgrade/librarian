package com.strange.fix.engine.extraction.sourcecode.localization;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.visitor.FieldApiDefinitionVisitor;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.List;

public class FieldApiDefinitionLocator extends ApiLocator {
    public FieldApiDefinitionLocator( File projectDir, ApiSignature apiSignature) {
        super(projectDir, apiSignature);
    }

    @Override
    protected List<ApiLocation> locateInFile(File sourceFile, CompilationUnit ast, ApiSignature apiSignature) {
        FieldApiDefinitionVisitor fieldApiDefinitionVisitor = new FieldApiDefinitionVisitor(ast, sourceFile, apiSignature);
        ast.accept(fieldApiDefinitionVisitor);
        return fieldApiDefinitionVisitor.getApiLocations();
    }
}
