package com.strange.fix.engine.extraction.sourcecode.localization;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.visitor.FieldApiUsageVisitor;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.List;

public class FieldApiUsageLocator extends ApiLocator {
    public FieldApiUsageLocator( File projectDir, ApiSignature apiSignature) {
        super(projectDir, apiSignature);
    }

    @Override
    protected List<ApiLocation> locateInFile(File sourceFile, CompilationUnit ast, ApiSignature apiSignature) {
        FieldApiUsageVisitor fieldApiUsageVisitor = new FieldApiUsageVisitor(ast, sourceFile, apiSignature);
        ast.accept(fieldApiUsageVisitor);
        return fieldApiUsageVisitor.getApiLocations();
    }
}
