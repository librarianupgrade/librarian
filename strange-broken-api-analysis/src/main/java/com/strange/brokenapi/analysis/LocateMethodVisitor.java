package com.strange.brokenapi.analysis;

import com.strange.common.utils.ClassUtil;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LocateMethodVisitor extends ASTVisitor {

    private final CompilationUnit compilationUnit;

    private final List<Integer> lineNumberList;

    // The method to be located must be declared within the corresponding type
    private final AbstractTypeDeclaration parentTypeDeclaration;

    private final List<MethodDeclaration> targetMethodDeclarationList;

    public LocateMethodVisitor(CompilationUnit compilationUnit, List<Integer> lineNumberList, AbstractTypeDeclaration parentTypeDeclaration) {
        this.compilationUnit = compilationUnit;
        this.lineNumberList = lineNumberList;
        this.parentTypeDeclaration = parentTypeDeclaration;
        this.targetMethodDeclarationList = new ArrayList<>();
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        int startLine = compilationUnit.getLineNumber(node.getStartPosition());
        int endLine = compilationUnit.getLineNumber(node.getStartPosition() + node.getLength() - 1);

        if (checkMethodContainTargetLine(startLine, endLine)) {
            MethodDeclaration methodDeclaration = locateCorrectMethodDeclaration(node);
            if (methodDeclaration != null) {
                this.targetMethodDeclarationList.add(methodDeclaration);
            }

        }
        return super.visit(node);
    }

    private boolean checkMethodContainTargetLine(Integer startLineNumber, Integer endLineNumber) {
        for (Integer targetLineNumber : lineNumberList) {
            if (targetLineNumber >= startLineNumber && targetLineNumber <= endLineNumber) {
                return true;
            }
        }
        return false;
    }

    private MethodDeclaration locateCorrectMethodDeclaration(MethodDeclaration methodDeclaration) {
        MethodDeclaration neededMethodDeclaration = methodDeclaration;
        ASTNode currentNode = methodDeclaration.getParent();

        while (currentNode != null) {
            if (currentNode instanceof TypeDeclaration typeDeclaration) {
                String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(typeDeclaration.getName().getIdentifier()));
                String targetSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(parentTypeDeclaration.getName().getIdentifier()));
                if (Objects.equals(actualSimpleClassName, targetSimpleClassName)) {
                    break;
                }
            } else if (currentNode instanceof MethodDeclaration parentMethodDeclaration) {
                neededMethodDeclaration = parentMethodDeclaration;
            }
            currentNode = currentNode.getParent();
        }
        return neededMethodDeclaration;
    }

    public List<MethodDeclaration> getTargetMethodDeclarationList() {
        return new ArrayList<>(targetMethodDeclarationList).stream().distinct()
                .toList();
    }
}
