package com.strange.brokenapi.analysis;

import lombok.Getter;
import org.eclipse.jdt.core.dom.*;

public class LocateClassVisitor extends ASTVisitor {

    private final CompilationUnit compilationUnit;

    private final Integer lineNumber;

    private boolean anonymousClass;

    @Getter
    private AbstractTypeDeclaration targetTypeDeclaration;

    public LocateClassVisitor(CompilationUnit compilationUnit, Integer lineNumber) {
        this.compilationUnit = compilationUnit;
        this.lineNumber = lineNumber;
        this.targetTypeDeclaration = null;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        int startLine = compilationUnit.getLineNumber(node.getStartPosition());
        int endLine = compilationUnit.getLineNumber(node.getStartPosition() + node.getLength() - 1);
        if (startLine <= lineNumber && lineNumber <= endLine) {
            this.targetTypeDeclaration = node;
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        int startLine = compilationUnit.getLineNumber(node.getStartPosition());
        int endLine = compilationUnit.getLineNumber(node.getStartPosition() + node.getLength() - 1);
        if (startLine <= lineNumber && lineNumber <= endLine) {
            this.targetTypeDeclaration = node;
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(RecordDeclaration node) {
        int startLine = compilationUnit.getLineNumber(node.getStartPosition());
        int endLine = compilationUnit.getLineNumber(node.getStartPosition() + node.getLength() - 1);
        if (startLine <= lineNumber && lineNumber <= endLine) {
            this.targetTypeDeclaration = node;
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        // The compatibility issue occurs in anonymous classes, e.g. new InStream { @Override ...}
        AnonymousClassDeclaration anonymousClassDeclaration = node.getAnonymousClassDeclaration();
        if (anonymousClassDeclaration != null) {
            int startLine = compilationUnit.getLineNumber(anonymousClassDeclaration.getStartPosition());
            int endLine = compilationUnit.getLineNumber(anonymousClassDeclaration.getStartPosition()
                    + anonymousClassDeclaration.getLength() - 1);
            if (startLine <= lineNumber && lineNumber <= endLine) {
                TypeDeclaration parentTypeDeclaration = getParentTypeDeclaration(node);
                if (parentTypeDeclaration != null) {
                    this.anonymousClass = true;
                    this.targetTypeDeclaration = parentTypeDeclaration;
                }
            }

        }
        return super.visit(node);
    }

    private TypeDeclaration getParentTypeDeclaration(ClassInstanceCreation node) {
        ASTNode currentNode = node;
        while (currentNode != null) {
            ASTNode parentNode = currentNode.getParent();
            if (parentNode instanceof TypeDeclaration) {
                return (TypeDeclaration) parentNode;
            }
            currentNode = parentNode;
        }
        return null;
    }
}
