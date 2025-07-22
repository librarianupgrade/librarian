package com.strange.fix.engine.extraction.sourcecode;

import cn.hutool.core.io.file.FileReader;
import com.strange.common.utils.JDTUtil;
import com.strange.common.utils.StringSimilarityUtil;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.FieldApiDefinitionLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.MethodApiDefinitionLocation;
import com.strange.fix.engine.formatter.CodeFormatter;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StatementSimilarityMapper {

    private static final double SIMILARITY_THRESHOLD = 0.55;

    private static final Integer LENGTH = 20;

    private final ApiLocation newApiDefLocation;

    private final List<String> targetStatementList;

    public StatementSimilarityMapper( ApiLocation newApiDefLocation,  List<String> targetStatementList) {
        this.newApiDefLocation = newApiDefLocation;
        this.targetStatementList = targetStatementList;
    }

    public List<Integer> getSimilarStatements() {
        if (newApiDefLocation instanceof MethodApiDefinitionLocation methodApiDefinitionLocation) {
            return resolveMethodApiDefinitionLocation(methodApiDefinitionLocation);
        } else if (newApiDefLocation instanceof FieldApiDefinitionLocation fieldApiDefinitionLocation) {
            return resolveFieldApiDefinitionLocation(fieldApiDefinitionLocation);
        } else {
            throw new RuntimeException("logic failure");
        }
    }

    private List<Integer> resolveMethodApiDefinitionLocation(MethodApiDefinitionLocation methodApiDefinitionLocation) {
        List<Integer> similarLineNumberList = new ArrayList<>();
        MethodDeclaration methodDeclaration = methodApiDefinitionLocation.getMethodDeclaration();
        CompilationUnit compilationUnit = methodApiDefinitionLocation.getCompilationUnit();
        BasicStatementVisitor basicStatementVisitor = new BasicStatementVisitor();
        methodDeclaration.accept(basicStatementVisitor);
        List<ASTNode> basicStatementList = basicStatementVisitor.getBasicStatementList();
        for (ASTNode statement : basicStatementList) {
            if (isSimilar(statement, targetStatementList)) {
                similarLineNumberList.add(compilationUnit.getLineNumber(statement.getStartPosition()));
            }
        }
        return similarLineNumberList;
    }

    private List<Integer> resolveFieldApiDefinitionLocation(FieldApiDefinitionLocation fieldApiDefinitionLocation) {
        List<Integer> similarLineNumberList = new ArrayList<>();
        File locatedFile = fieldApiDefinitionLocation.getLocatedFile();
        String sourceCode = new FileReader(locatedFile).readString();
        FieldDeclaration fieldDeclaration = fieldApiDefinitionLocation.getFieldDeclaration();
        CompilationUnit compilationUnit = fieldApiDefinitionLocation.getCompilationUnit();

        String actualSourceCode = JDTUtil.getRawFieldDeclaration(sourceCode, fieldDeclaration);
        if (isSimilar(actualSourceCode, targetStatementList)) {
            int fieldStartLineNumber = compilationUnit.getLineNumber(fieldDeclaration.getStartPosition());
            int fieldEndLineNumber = compilationUnit.getLineNumber(fieldDeclaration.getStartPosition() + fieldDeclaration.getLength() - 1);
            for (int i = fieldStartLineNumber; i <= fieldEndLineNumber; i++) {
                similarLineNumberList.add(i);
            }
        }
        return similarLineNumberList;
    }


    private boolean isSimilar(ASTNode statement, List<String> targetStatementList) {
        String actualStatement = new CodeFormatter(statement.toString()).startFormat();
        return isSimilar(actualStatement, targetStatementList);
    }

    private boolean isSimilar(String statement, List<String> targetStatementList) {
        for (String exceptedCode : targetStatementList) {

            String formatedActualCode = statement.replaceAll("\\s+", "");
            String formatedExceptedCode = exceptedCode.replaceAll("\\s+", "");
            double score = StringSimilarityUtil.levenshteinSimilarity(formatedActualCode, formatedExceptedCode);
            if (score >= SIMILARITY_THRESHOLD) {
                return true;
            }

            String shorterString = formatedActualCode.length() <= formatedExceptedCode.length() ? formatedActualCode : formatedExceptedCode;
            String longerString = formatedActualCode.length() <= formatedExceptedCode.length() ? formatedExceptedCode : formatedActualCode;
            int len = Math.min(shorterString.length(), LENGTH);
            for (int i = 0; i + len - 1 < shorterString.length(); i++) {
                String subString1 = shorterString.substring(i, i + len);
                for (int j = 0; j + len - 1 < longerString.length(); j++) {
                    String subString2 = longerString.substring(j, j + len);
                    score = StringSimilarityUtil.levenshteinSimilarity(subString1, subString2);
                    if (score >= SIMILARITY_THRESHOLD) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    @Getter
    static class BasicStatementVisitor extends ASTVisitor {
        private final List<ASTNode> basicStatementList = new ArrayList<>();

        private boolean isBasic(Statement s) {
            return s instanceof ExpressionStatement
                    || s instanceof ReturnStatement
                    || s instanceof VariableDeclarationStatement
                    || s instanceof ThrowStatement
                    || s instanceof BreakStatement
                    || s instanceof ContinueStatement
                    || s instanceof EmptyStatement;
        }

        @Override
        public void preVisit(ASTNode node) {
            if (node instanceof Statement stmt) {
                if (isBasic(stmt)) {
                    basicStatementList.add(stmt);
                } else if (stmt instanceof IfStatement ifStatement) {
                    Expression expression = ifStatement.getExpression();
                    basicStatementList.add(expression);
                } else if (stmt instanceof ForStatement forStatement) {
                    Expression expression = forStatement.getExpression();
                    basicStatementList.add(expression);
                } else if (stmt instanceof EnhancedForStatement enhancedForStatement) {
                    Expression expression = enhancedForStatement.getExpression();
                    basicStatementList.add(expression);
                } else if (stmt instanceof WhileStatement whileStatement) {
                    Expression expression = whileStatement.getExpression();
                    basicStatementList.add(expression);
                }
            }
        }
    }
}
