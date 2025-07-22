package com.strange.brokenapi.analysis.jdt.visitor;

import com.strange.brokenapi.analysis.jdt.visitor.context.FieldUsageContext;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class MethodBodyFieldUsageVisitor extends ASTVisitor {

    private final List<FieldUsageContext> fieldUsageContextList;

    public MethodBodyFieldUsageVisitor() {
        this.fieldUsageContextList = new ArrayList<>();
    }

    @Override
    public boolean visit(QualifiedName node) {
        IBinding bind = node.resolveBinding();
        if (bind instanceof IVariableBinding variableBinding &&
                variableBinding.isField()) {
            int startPosition = node.getStartPosition();
            int length = node.getLength();
            CompilationUnit unit = (CompilationUnit) node.getRoot();
            int startLine = unit.getLineNumber(startPosition);
            int endLine = unit.getLineNumber(startPosition + length - 1);
            String fieldName = node.getName().getIdentifier();
            FieldUsageContext fieldUsageContext = new FieldUsageContext();
            fieldUsageContext.setStartLineNumber(startLine);
            fieldUsageContext.setEndLineNumber(endLine);
            fieldUsageContext.setFieldName(fieldName);

            ITypeBinding fieldBinding = node.getName().resolveTypeBinding();
            if (fieldBinding != null) {
                String fieldClassName = fieldBinding.getQualifiedName();
                fieldUsageContext.setClassName(fieldClassName);
            }

            ITypeBinding typeBinding = node.getQualifier().resolveTypeBinding();
            if (typeBinding != null) {
                String belongedClassName = typeBinding.getQualifiedName();
                fieldUsageContext.setBelongedClassName(belongedClassName);
            }

            fieldUsageContextList.add(fieldUsageContext);
        }
        return super.visit(node);
    }

    public List<FieldUsageContext> getFieldUsageContextList() {
        return fieldUsageContextList;
    }
}
