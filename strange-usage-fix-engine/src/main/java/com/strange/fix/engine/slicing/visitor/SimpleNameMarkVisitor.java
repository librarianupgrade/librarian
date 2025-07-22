package com.strange.fix.engine.slicing.visitor;

import cn.hutool.core.util.StrUtil;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class SimpleNameMarkVisitor extends ASTVisitor {

    private final List<String> typeNameList;

    public SimpleNameMarkVisitor() {
        this.typeNameList = new ArrayList<>();
    }

    @Override
    public boolean visit(SimpleType node) {
        typeNameList.add(node.getName().getFullyQualifiedName());
        return super.visit(node);
    }

    @Override
    public boolean visit(QualifiedType node) {
        typeNameList.add(node.getName().getIdentifier());
        return super.visit(node);
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        if (node.getType().isSimpleType()) {
            SimpleType st = (SimpleType) node.getType();
            typeNameList.add(st.getName().getFullyQualifiedName());
        }
        return true;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        if (node.getExpression() == null)
            typeNameList.add(node.getName().getIdentifier());
        return true;
    }

    @Override
    public boolean visit(FieldAccess node) {
        if (node.getExpression() == null)
            typeNameList.add(node.getName().getIdentifier());
        return true;
    }

    public List<String> getTypeNameList() {
        List<String> typeNames = new ArrayList<>();
        for (String typeName : typeNameList) {
            List<String> split = StrUtil.split(typeName, '.');
            typeNames.addAll(split);
        }
        return typeNames;
    }
}
