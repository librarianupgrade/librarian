package com.strange.fix.engine.slicing.visitor;

import cn.hutool.core.collection.CollUtil;
import lombok.Getter;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.*;

public class MethodLineVisitor extends ASTVisitor {

    private final List<Integer> lineNumberList;

    private final CompilationUnit compilationUnit;

    @Getter
    private final Map<MethodDeclaration, List<Integer>> methodLineNumberMap; // method declaration ---> the line number list in method declaration

    @Getter
    private final Set<Integer> lineNumberInMethodSet; // the statement set that contains statement in method declaration

    // Mapping each Method Declaration and the corresponding list of line numbers to retain
    public MethodLineVisitor( List<Integer> lineNumberList,  CompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
        this.lineNumberList = lineNumberList;
        this.methodLineNumberMap = new HashMap<>();
        this.lineNumberInMethodSet = new HashSet<>();
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        MethodDeclaration[] methodDeclarations = node.getMethods();
        for (MethodDeclaration methodDeclaration : methodDeclarations) {
            int startLine = compilationUnit.getLineNumber(methodDeclaration.getStartPosition());
            int endLine = compilationUnit.getLineNumber(methodDeclaration.getStartPosition() + methodDeclaration.getLength());
            List<Integer> methodLineNumberList = methodLineNumberMap.getOrDefault(methodDeclaration, new ArrayList<>());

            for (Integer lineNumber : lineNumberList) {
                if (lineNumber >= startLine && lineNumber <= endLine) {
                    methodLineNumberList.add(lineNumber);
                    lineNumberInMethodSet.add(lineNumber);
                }
            }
            if (CollUtil.isNotEmpty(methodLineNumberList)) {
                methodLineNumberMap.put(methodDeclaration, methodLineNumberList);
            }
        }
        return super.visit(node);
    }
}
