package com.strange.brokenapi.analysis.jdt.visitor;


import com.strange.brokenapi.analysis.jdt.visitor.context.FieldUsageContext;
import org.eclipse.jdt.core.dom.*;

public class MethodBodyAssignmentVisitor extends ASTVisitor {

    private Integer errorLineNumber;

    private FieldUsageContext fieldUsageContext;

    public MethodBodyAssignmentVisitor(Integer errorLineNumber) {
        this.errorLineNumber = errorLineNumber;
        this.fieldUsageContext = new FieldUsageContext();
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        int startPosition = node.getStartPosition();
        int length = node.getLength();
        CompilationUnit unit = (CompilationUnit) node.getRoot();
        int startLine = unit.getLineNumber(startPosition);
        int endLine = unit.getLineNumber(startPosition + length - 1);

        if (errorLineNumber >= startLine && errorLineNumber <= endLine) {
            fieldUsageContext.setStartLineNumber(startLine);
            fieldUsageContext.setEndLineNumber(endLine);
            for (Object fragment : node.fragments()) {
                VariableDeclarationFragment vdf = (VariableDeclarationFragment) fragment;
                Expression initializer = vdf.getInitializer();

                if (initializer instanceof QualifiedName) {
                    QualifiedName qualifiedName = (QualifiedName) initializer;
                    fieldUsageContext.setFieldName(qualifiedName.getName().getIdentifier());

                    ITypeBinding qualifiedTypeBinding = qualifiedName.getQualifier().resolveTypeBinding();
                    if (qualifiedTypeBinding != null) {
                        fieldUsageContext.setBelongedClassName(qualifiedTypeBinding.getQualifiedName());
                    }

                    ITypeBinding typeBinding = qualifiedName.resolveTypeBinding();
                    if (typeBinding != null) {
                        fieldUsageContext.setClassName(typeBinding.getQualifiedName());
                    }
                }
            }
        }
        return super.visit(node);
    }

    public FieldUsageContext getFieldUsageContext() {
        return fieldUsageContext;
    }
}
