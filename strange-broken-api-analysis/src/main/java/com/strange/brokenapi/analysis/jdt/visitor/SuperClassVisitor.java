package com.strange.brokenapi.analysis.jdt.visitor;


import com.strange.brokenapi.analysis.jdt.visitor.context.FieldDetailsContext;
import com.strange.brokenapi.analysis.jdt.visitor.context.MethodDetailsContext;
import com.strange.brokenapi.analysis.jdt.visitor.context.SuperClassContext;
import com.strange.common.utils.ClassUtil;

import org.eclipse.jdt.core.dom.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SuperClassVisitor extends ASTVisitor {

    private final SuperClassContext superClassContext;

    private boolean hasVisited;

    private final String className;

    public SuperClassVisitor() {
       this("*");
    }

    public SuperClassVisitor(String className) {
        this.className = className;
        this.hasVisited = false;
        this.superClassContext = new SuperClassContext();
    }



    @Override
    public boolean visit(TypeDeclaration node) {
        if(hasVisited) return super.visit(node);
        String simpleClassName = ClassUtil.getSimpleClassName(className);
        String currentClassName = node.getName().getFullyQualifiedName();
        if(Objects.equals(simpleClassName, currentClassName) || "*".equals(simpleClassName)) {
            this.hasVisited = true;
            Type superclassType = node.getSuperclassType();
            if (superclassType != null) {
                ITypeBinding superTypeBinding = superclassType.resolveBinding();
                if (superTypeBinding != null) {
                    superClassContext.setSuperClassName(superTypeBinding.getQualifiedName());
    
                    IVariableBinding[] declaredFields = superTypeBinding.getDeclaredFields();
                    if (declaredFields != null) {
                        for (IVariableBinding declaredField : declaredFields) {
                            FieldDetailsContext fieldDetailsContext = new FieldDetailsContext();
                            fieldDetailsContext.setFieldName(declaredField.getName());
                            ITypeBinding fieldTypeBinding = declaredField.getDeclaringClass();
                            if (fieldTypeBinding != null) {
                                fieldDetailsContext.setFieldClassName(fieldTypeBinding.getQualifiedName());
                            }
                            fieldDetailsContext.setBelongedClassName(superTypeBinding.getQualifiedName());
                            superClassContext.getFieldNameList().add(fieldDetailsContext);
                        }
                    }
    
                    IMethodBinding[] declaredMethods = superTypeBinding.getDeclaredMethods();
                    if (declaredMethods != null) {
                        for (IMethodBinding declaredMethod : declaredMethods) {
                            MethodDetailsContext methodDetailsContext = new MethodDetailsContext();
                            methodDetailsContext.setMethodName(declaredMethod.getName());
    
                            ITypeBinding[] parameterTypes = declaredMethod.getParameterTypes();
                            if (parameterTypes != null) {
                                List<String> params = Arrays.stream(parameterTypes)
                                        .map(ITypeBinding::getQualifiedName)
                                        .toList();
                                methodDetailsContext.setParamTypeList(params);
                            }
                            superClassContext.getMethodNameList().add(methodDetailsContext);
                        }
                    }
    
                }
            }
        }
        return super.visit(node);
    }

    public SuperClassContext getSuperClassContext() {
        return superClassContext;
    }
}
