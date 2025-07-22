package com.strange.fix.engine.slicing;

import cn.hutool.core.io.file.FileReader;
import com.strange.common.utils.ClassUtil;
import com.strange.common.utils.JDTUtil;
import com.strange.fix.engine.formatter.CodeFormatter;
import com.strange.fix.engine.slicing.visitor.MethodLineVisitor;
import com.strange.fix.engine.slicing.visitor.SimpleNameMarkVisitor;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.*;

public class MethodSlicingHelper {

    public static String trimWithLineNumbers(File slicedFile, List<Integer> lineNumberList) {
        // first, get method line number map
        String sourceCode = new FileReader(slicedFile).readString();
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(sourceCode.toCharArray());
        parser.setBindingsRecovery(false);
        parser.setStatementsRecovery(false);
        parser.setResolveBindings(false);
        CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);

        // Mapping each Method Declaration and the corresponding list of line numbers to retain
        MethodLineVisitor methodLineVisitor = new MethodLineVisitor(lineNumberList, compilationUnit);
        compilationUnit.accept(methodLineVisitor);
        Map<MethodDeclaration, List<Integer>> methodLineNumberMap = methodLineVisitor.getMethodLineNumberMap();
        Set<Integer> lineNumberInMethodSet = methodLineVisitor.getLineNumberInMethodSet();

        // second, trim the java code in file
        // resolve the statement not in method declaration
        List<String> outsideStatementList = new ArrayList<>();
        for (Integer lineNumber : lineNumberList) {
            if (!lineNumberInMethodSet.contains(lineNumber)) {
                String statement = JDTUtil.getStatementByLineNumber(sourceCode, lineNumber); // TODO not prefect
                outsideStatementList.add(statement);
            }
        }

        // resolve the statement in method declaration
        // Mapping each Method Declaration and the corresponding list of line numbers to retain
        List<MethodDeclaration> savedMethodDeclarationList = new ArrayList<>();
        for (Map.Entry<MethodDeclaration, List<Integer>> entry : methodLineNumberMap.entrySet()) {
            MethodDeclaration methodDeclaration = entry.getKey();
            List<Integer> savedLineNumberList = entry.getValue();
            // trimming the method declaration
            MethodDeclaration trimmedMethod = new MethodTrimmer(methodDeclaration, savedLineNumberList).trimming();
            savedMethodDeclarationList.add(trimmedMethod);
        }
        // get the needed import statements in method declarations
        List<ImportDeclaration> savedImportDeclarationList = getImportsForMethodDeclaration(slicedFile, savedMethodDeclarationList);

        // transfer to source code
        List<String> statementList = new ArrayList<>();
        for (ImportDeclaration importDeclaration : savedImportDeclarationList) {
            statementList.add(new CodeFormatter(importDeclaration.toString()).startFormat());
        }

        statementList.addAll(outsideStatementList);

        for (MethodDeclaration methodDeclaration : savedMethodDeclarationList) {
            statementList.add(new CodeFormatter(methodDeclaration.toString()).startFormat());
        }

        return String.join("\n", statementList).replaceAll("\n", " ")
                .replaceAll("\\s+", " ").strip();
    }

    private static List<ImportDeclaration> getImportsForMethodDeclaration(File slicedFile, List<MethodDeclaration> savedMethodDeclarationList) {
        List<ImportDeclaration> importDeclarationList = JDTUtil.getImportDeclarations(slicedFile);
        List<ImportDeclaration> savedImportDeclarationList = new ArrayList<>();
        Set<String> usedTypeName = new HashSet<>();
        for (MethodDeclaration methodDeclaration : savedMethodDeclarationList) {
            SimpleNameMarkVisitor simpleNameMarkVisitor = new SimpleNameMarkVisitor();
            methodDeclaration.accept(simpleNameMarkVisitor);
            List<String> typeNameList = simpleNameMarkVisitor.getTypeNameList();
            usedTypeName.addAll(typeNameList);
        }

        for (ImportDeclaration importDeclaration : importDeclarationList) {
            if (importDeclaration.isOnDemand()) savedImportDeclarationList.add(importDeclaration);
            else {
                String fullyQualifiedName = importDeclaration.getName().getFullyQualifiedName();
                String simpleClassName = ClassUtil.getSimpleClassName(fullyQualifiedName);
                boolean isFound = false;
                for (String typeName : usedTypeName) {
                    String simpleTypeName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(typeName));
                    if (Objects.equals(simpleTypeName, simpleClassName)) {
                        isFound = true;
                        break;
                    }
                }
                if (isFound) {
                    savedImportDeclarationList.add(importDeclaration);
                }
            }
        }
        return savedImportDeclarationList;
    }
}
