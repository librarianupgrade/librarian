package com.strange.brokenapi.analysis.jdt.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;

public class ImportVisitor extends ASTVisitor {

    private final Integer errorLineNumber;

    private String importName;

    private boolean onDemand;

    private boolean staticImport;

    public ImportVisitor(Integer errorLineNumber) {
        this.errorLineNumber = errorLineNumber;
    }

    @Override
    public boolean visit(ImportDeclaration node) {
        int startPosition = node.getStartPosition();
        int length = node.getLength();
        CompilationUnit unit = (CompilationUnit) node.getRoot();
        int startLine = unit.getLineNumber(startPosition);
        int endLine = unit.getLineNumber(startPosition + length - 1);
        if (errorLineNumber >= startLine && errorLineNumber <= endLine) {
            this.importName = node.getName().getFullyQualifiedName();
            this.staticImport = node.isStatic();
            this.onDemand = node.isOnDemand();
        }
        return true;
    }

    public String getImportName() {
        return importName;
    }

    public boolean isOnDemand() {
        return onDemand;
    }

    public boolean isStaticImport() {
        return staticImport;
    }
}
