package com.strange.brokenapi.analysis.jdt.visitor;

import com.strange.brokenapi.analysis.jdt.visitor.context.ClassUsageContext;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class MethodBodyClassUsageVisitor extends ASTVisitor {

    private final List<ClassUsageContext> classUsageContextList;

    public MethodBodyClassUsageVisitor() {
        this.classUsageContextList = new ArrayList<>();
    }

    @Override
    public boolean visit(SimpleName node) {
        IBinding binding = node.resolveBinding();
        if (binding != null) {
            if (binding instanceof ITypeBinding typeBinding) {
                String qualifiedName = typeBinding.getQualifiedName();

                ClassUsageContext classUsageContext = new ClassUsageContext();
                classUsageContext.setClassName(qualifiedName);

                int startPosition = node.getStartPosition();
                int length = node.getLength();
                CompilationUnit unit = (CompilationUnit) node.getRoot();
                int startLine = unit.getLineNumber(startPosition);
                int endLine = unit.getLineNumber(startPosition + length - 1);
                classUsageContext.setStartLineNumber(startLine);
                classUsageContext.setEndLineNumber(endLine);
                classUsageContextList.add(classUsageContext);

            }
        }
        return super.visit(node);
    }

    public List<ClassUsageContext> getClassUsageContextList() {
        return classUsageContextList;
    }
}
