package com.strange.fix.engine.slicing;

import lombok.NonNull;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class MethodTrimmer {

    private final MethodDeclaration methodDeclaration;

    private final List<Integer> savedStatementLineNumberList;

    private MethodDeclaration trimmedMethod;

    /**
     * Trims the body of {@code methodDecl}, keeping only the statements whose
     * starting line numbers appear in {@code linesToKeep}. The AST structure
     * remains valid.
     *
     * @param methodDeclaration            MethodDeclaration that will be modified (must belong to a parsed CompilationUnit)
     * @param savedStatementLineNumberList 1‑based line numbers to keep (usually editor line numbers)
     */
    public MethodTrimmer( MethodDeclaration methodDeclaration,  List<Integer> savedStatementLineNumberList) {
        this.methodDeclaration = methodDeclaration;
        this.savedStatementLineNumberList = savedStatementLineNumberList;
        this.trimmedMethod = null;
    }

    public MethodDeclaration trimming() {
        if (trimmedMethod == null) {
            AST ast = methodDeclaration.getAST();
            CompilationUnit compilationUnit = (CompilationUnit) methodDeclaration.getRoot();
            if (compilationUnit == null) {
                throw new IllegalArgumentException("MethodDeclaration is not rooted in a CompilationUnit.");
            }

            Block newBody = ast.newBlock();

            List<Statement> savedStatements = pickMinCoveringStatements(methodDeclaration, compilationUnit,
                    savedStatementLineNumberList);

            for (Statement stmt : savedStatements) {
                Statement copied = (Statement) ASTNode.copySubtree(ast, stmt);
                removeBody(copied); // remove the useless body content
                newBody.statements().add(copied);
            }

            methodDeclaration.setBody(newBody);

            // remove the javadoc in method declaration
            if (methodDeclaration.getJavadoc() != null) {
                methodDeclaration.setJavadoc(null);
            }

            // remove Annotation node in method declaration
            @SuppressWarnings("unchecked")
            List<IExtendedModifier> mods = methodDeclaration.modifiers();
            mods.removeIf(IExtendedModifier::isAnnotation);

            trimmedMethod = (MethodDeclaration) ASTNode.copySubtree(methodDeclaration.getAST(), methodDeclaration);
        }
        return trimmedMethod;
    }

    private void removeBody(Statement statement) {
        AST ast = statement.getAST();
        if (statement instanceof ForStatement forStatement) {
            forStatement.setBody(ast.newBlock());
        } else if (statement instanceof EnhancedForStatement enhancedForStatement) {
            enhancedForStatement.setBody(ast.newBlock());
        } else if (statement instanceof WhileStatement whileStatement) {
            whileStatement.setBody(ast.newBlock());
        }
    }


    /**
     * Given a MethodDeclaration and a list of line numbers,
     * if multiple Statements cover the same line number,
     * keep only the Statement whose start and end lines are
     * closest to that line number and whose overall span is the smallest.
     */
    List<Statement> pickMinCoveringStatements(MethodDeclaration methodDeclaration,
                                              CompilationUnit compilationUnit,
                                              List<Integer> targetLines) {
        List<StatementInfo> allStatements = new ArrayList<>();
        methodDeclaration.accept(new ASTVisitor() {
            // get the depth of the node in AST
            int getDepth(ASTNode n) {
                int d = 0;
                while ((n = n.getParent()) != null) d++;
                return d;
            }

            @Override
            public void preVisit(ASTNode node) {
                if (node instanceof Statement statement) {
                    if (node instanceof Block) return; // do not consider the block
                    int s = compilationUnit.getLineNumber(node.getStartPosition());
                    int e = compilationUnit.getLineNumber(node.getStartPosition() + node.getLength() - 1);
                    allStatements.add(new StatementInfo(s, e, getDepth(node), statement));
                }
                super.preVisit(node);
            }
        });

        Map<Integer, Statement> result = new HashMap<>();
        for (int line : targetLines) {
            StatementInfo best = null;
            // choose the best statement for the target line number
            for (StatementInfo si : allStatements) {
                if (si.start <= line && line <= si.end) {
                    if (best == null) {
                        best = si;
                        continue;
                    }
                    int lenBest = best.end - best.start;
                    int lenCur = si.end - si.start;
                    if (lenCur < lenBest ||
                            (lenCur == lenBest && si.depth > best.depth)) {
                        best = si;
                    }
                }
            }
            if (best != null) {
                result.put(line, best.statement);
            }
        }

        return new TreeMap<>(result).values()
                .stream()
                .distinct()
                .toList();
    }


    private static class StatementInfo {
        final int start, end, depth;
        final Statement statement;

        StatementInfo(int start, int end, int depth, Statement statement) {
            this.start = start;
            this.end = end;
            this.depth = depth;
            this.statement = statement;
        }
    }
}
