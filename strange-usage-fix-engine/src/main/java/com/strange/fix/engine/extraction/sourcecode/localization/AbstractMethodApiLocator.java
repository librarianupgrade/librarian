package com.strange.fix.engine.extraction.sourcecode.localization;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.visitor.AbstractMethodApiOverrideVisitor;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.List;

public class AbstractMethodApiLocator extends ApiLocator {

    public AbstractMethodApiLocator( File projectDir, ApiSignature apiSignature) {
        super(projectDir, apiSignature);
    }

    /**
     * asxxxxxxxxxxx
     * @param sourceFile the source code file
     * @param ast the CompilationUnit of the source code file
     * @param apiSignature the target Api Signature
     */
    @Override
    protected List<ApiLocation> locateInFile(File sourceFile, CompilationUnit ast, ApiSignature apiSignature) {
        AbstractMethodApiOverrideVisitor abstractMethodApiOverrideVisitor = new AbstractMethodApiOverrideVisitor(ast, sourceFile, apiSignature);
        ast.accept(abstractMethodApiOverrideVisitor);
        return abstractMethodApiOverrideVisitor.getApiLocations();
    }
}
