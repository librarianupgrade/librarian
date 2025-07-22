package com.strange.brokenapi.analysis.jdt.visitor;

import lombok.NonNull;
import org.eclipse.jdt.core.dom.*;

/**
 * Find the specific definition of the method body based on the line number of the Problem,
 * and pass a `Visitor` to perform a custom traversal of the method body.
 */
public class MethodBodyVisitor extends ASTVisitor {

    private final Integer problemLineNumber;

    private final ASTVisitor bodyVisitor;

    public MethodBodyVisitor( Integer problemLineNumber,  ASTVisitor bodyVisitor) {
        this.problemLineNumber = problemLineNumber;
        this.bodyVisitor = bodyVisitor;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        int startPosition = node.getStartPosition();
        int length = node.getLength();
        CompilationUnit unit = (CompilationUnit) node.getRoot();
        int startLine = unit.getLineNumber(startPosition);
        int endLine = unit.getLineNumber(startPosition + length - 1);
        if (problemLineNumber >= startLine && problemLineNumber <= endLine) {
            // find the method which exist problem according the problem line number
            Block methodBody = node.getBody();
            if (methodBody != null) {
                methodBody.accept(bodyVisitor);
            }
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(LambdaExpression node) {
        int startPosition = node.getStartPosition();
        int length = node.getLength();
        CompilationUnit unit = (CompilationUnit) node.getRoot();
        int startLine = unit.getLineNumber(startPosition);
        int endLine = unit.getLineNumber(startPosition + length - 1);
        if (problemLineNumber >= startLine && problemLineNumber <= endLine) {
            // find the lambda method which exist problem according the problem line number
            ASTNode methodBody = node.getBody();
            if (methodBody != null) {
                methodBody.accept(bodyVisitor);
            }
        }
        return super.visit(node);
    }
}
