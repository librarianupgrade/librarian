package com.strange.brokenapi.analysis.jdt.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TryStatement;

public class TryBlockVisitor extends ASTVisitor {
    private final Integer problemLineNumber;

    private final ASTVisitor blockVisitor;

    public TryBlockVisitor(Integer problemLineNumber, ASTVisitor blockVisitor) {
        this.problemLineNumber = problemLineNumber;
        this.blockVisitor = blockVisitor;
    }

    @Override
    public boolean visit(TryStatement node) {
        int startPosition = node.getStartPosition();
        int length = node.getLength();
        CompilationUnit unit = (CompilationUnit) node.getRoot();
        int startLine = unit.getLineNumber(startPosition);
        int endLine = unit.getLineNumber(startPosition + length - 1);

        if (problemLineNumber >= startLine && problemLineNumber <= endLine) {
            Block tryBlock = node.getBody();
            if (tryBlock != null) {
                tryBlock.accept(blockVisitor);
            }
        }

        return super.visit(node);
    }

}
