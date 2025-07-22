package com.strange.fix.engine.extraction.sourcecode.localization;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.visitor.ClassApiUsageVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.List;

public class ClassApiUsageLocator extends ApiLocator {

    public ClassApiUsageLocator(File projectDir, ApiSignature apiSignature) {
        super(projectDir, apiSignature);
    }


    @Override
    protected List<ApiLocation> locateInFile(File sourceFile, CompilationUnit ast, ApiSignature apiSignature) {
        ClassApiUsageVisitor classApiUsageVisitor = new ClassApiUsageVisitor(ast, sourceFile, apiSignature);
        ast.accept(classApiUsageVisitor);
        return classApiUsageVisitor.getApiLocations();
    }
}
