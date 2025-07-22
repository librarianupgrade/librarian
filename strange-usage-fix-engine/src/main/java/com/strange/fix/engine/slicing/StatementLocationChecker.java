package com.strange.fix.engine.slicing;

import cn.hutool.core.io.file.FileReader;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StatementLocationChecker {
    /**
     * Checks if the specified 1-based line number is inside
     * the body of any method in the given source.
     *
     * @param sourceCodeFile Java source code File
     * @param targetLine     The line number to check (1-based)
     * @return true if the line is inside a method body; false otherwise
     */
    public static boolean isLineInMethodBody(File sourceCodeFile, int targetLine) {
        String sourceCode = new FileReader(sourceCodeFile).readString();

        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(sourceCode.toCharArray());
        parser.setResolveBindings(false);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<MethodDeclaration> methodList = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                methodList.add(node);
                return super.visit(node);
            }
        });

        for (MethodDeclaration method : methodList) {
            Block body = method.getBody();
            if (body == null) {
                continue;
            }

            int bodyStartOffset = body.getStartPosition();
            int bodyLength = body.getLength();

            int startLine = cu.getLineNumber(bodyStartOffset);
            int endLine = cu.getLineNumber(bodyStartOffset + bodyLength);

            // If targetLine falls within [startLine, endLine], it’s inside this method
            if (targetLine >= startLine && targetLine <= endLine) {
                return true;
            }
        }

        return false;
    }
}
