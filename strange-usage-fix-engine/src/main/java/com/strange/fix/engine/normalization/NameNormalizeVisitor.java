package com.strange.fix.engine.normalization;

import lombok.NonNull;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.LinkedHashMap;
import java.util.Map;

class NameNormalizeVisitor extends ASTVisitor {

    private static final String PARAM_PREFIX = "param";

    private static final String VARIABLE_PREFIX = "var";

    private static final String FIELD_PREFIX = "field";

    private final AST ast;

    private final ASTRewrite astRewrite;

    private final Map<IVariableBinding, String> fieldNameMap = new LinkedHashMap<>();

    private int fieldCounter = 0;

    public NameNormalizeVisitor( AST ast,  ASTRewrite astRewrite) {
        this.ast = ast;
        this.astRewrite = astRewrite;
        this.fieldCounter = 0;
    }

    @Override
    public boolean visit(VariableDeclarationFragment frag) {
        IVariableBinding binding = frag.resolveBinding();
        if (binding != null && binding.isField()) {
            this.fieldCounter += 1;
            String newName = fieldNameMap.computeIfAbsent(binding.getVariableDeclaration(),
                    b -> FIELD_PREFIX + fieldCounter);
            astRewrite.replace(frag.getName(), ast.newSimpleName(newName), null);
        }
        return super.visit(frag);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        Map<IVariableBinding, String> paramNameMap = new LinkedHashMap<>();
        Map<IVariableBinding, String> varNameMap = new LinkedHashMap<>();
        int paramCounter = 0;
        final int[] varCounter = {0};

        for (Object o : node.parameters()) {
            SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
            IVariableBinding binding = svd.resolveBinding();
            if (binding == null) continue;
            paramCounter += 1;
            String newName = PARAM_PREFIX + paramCounter;
            paramNameMap.put(binding.getVariableDeclaration(), newName);
            astRewrite.replace(svd.getName(), ast.newSimpleName(newName), null);
        }

        node.getBody().accept(new ASTVisitor() {
            @Override
            public boolean visit(VariableDeclarationFragment frag) {
                IVariableBinding binding = frag.resolveBinding();
                if (binding == null) return super.visit(frag);
                if (binding.isField()) return super.visit(frag);
                varCounter[0] += 1;
                String newName = varNameMap.computeIfAbsent(binding.getVariableDeclaration(), b -> VARIABLE_PREFIX + varCounter[0]);
                astRewrite.replace(frag.getName(), ast.newSimpleName(newName), null);
                return super.visit(frag);
            }

            @Override
            public boolean visit(SimpleName name) {
                IBinding binding = name.resolveBinding();
                if (binding instanceof IVariableBinding vb) {
                    // Parameter
                    String replacement = paramNameMap.get(vb.getVariableDeclaration());
                    if (replacement == null) {
                        // Local var
                        replacement = varNameMap.get(vb.getVariableDeclaration());
                    }
                    if (replacement == null) {
                        // Field
                        replacement = fieldNameMap.get(vb.getVariableDeclaration());
                    }
                    if (replacement != null && !name.getIdentifier().equals(replacement)) {
                        astRewrite.replace(name, ast.newSimpleName(replacement), null);
                    }
                }
                return super.visit(name);
            }
        });
        return super.visit(node);
    }

    @Override
    public boolean visit(SimpleName name) {
        IBinding binding = name.resolveBinding();
        if (binding instanceof IVariableBinding vb && vb.isField()) {
            String replacement = fieldNameMap.get(vb.getVariableDeclaration());
            if (replacement == null) {
                fieldCounter += 1;
                replacement = fieldNameMap.computeIfAbsent(vb.getVariableDeclaration(),
                        b -> FIELD_PREFIX + fieldCounter);
            }
            if (!name.getIdentifier().equals(replacement)) {
                astRewrite.replace(name, ast.newSimpleName(replacement), null);
            }
        }
        return super.visit(name);
    }
}
