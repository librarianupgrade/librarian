package com.strange.brokenapi.analysis;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;

import java.util.ArrayList;
import java.util.List;

public class LocateFieldVisitor extends ASTVisitor {

    private final CompilationUnit compilationUnit;

    private final List<Integer> lineNumberList;

    private final List<FieldDeclaration> targetFieldDeclarationList;

    public LocateFieldVisitor(CompilationUnit compilationUnit, List<Integer> lineNumberList) {
        this.compilationUnit = compilationUnit;
        this.lineNumberList = lineNumberList;
        this.targetFieldDeclarationList = new ArrayList<>();
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        int startLine = compilationUnit.getLineNumber(node.getStartPosition());
        int endLine = compilationUnit.getLineNumber(node.getStartPosition() + node.getLength() - 1);
        if (checkFieldContainTargetLine(startLine, endLine)) {
            targetFieldDeclarationList.add(node);
        }
        return super.visit(node);
    }

    private boolean checkFieldContainTargetLine(Integer startLineNumber, Integer endLineNumber) {
        for (Integer targetLineNumber : lineNumberList) {
            if (targetLineNumber >= startLineNumber && targetLineNumber <= endLineNumber) {
                return true;
            }
        }
        return false;
    }

    public List<FieldDeclaration> getTargetFieldDeclarationList() {
        return new ArrayList<>(targetFieldDeclarationList).stream().distinct().toList();
    }
}
