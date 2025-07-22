package com.strange.brokenapi.analysis.prioritization;

import com.strange.common.utils.ClassUtil;
import lombok.Getter;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ClassHierarchyVisitor extends ASTVisitor {
    private final List<String> classNameList;

    private final Map<String, String> superClassNameMap;

    private final Map<String, List<String>> interfaceNameListMap;

    public ClassHierarchyVisitor() {
        this.classNameList = new ArrayList<>();
        this.superClassNameMap = new HashMap<>();
        this.interfaceNameListMap = new HashMap<>();
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        // focus on the class which is the package member type
        if (node.isPackageMemberTypeDeclaration()) {
            ITypeBinding typeBinding = node.resolveBinding();
            if (typeBinding != null) {
                String currentClassName = ClassUtil.removeGenericType(typeBinding.getQualifiedName());
                this.classNameList.add(currentClassName);
                ITypeBinding[] interfaces = typeBinding.getInterfaces();
                if (interfaces != null) {
                    List<String> interfaceNameList = new ArrayList<>();
                    for (ITypeBinding interfaceBinding : interfaces) {
                        String interfaceName = ClassUtil.removeGenericType(interfaceBinding.getQualifiedName());
                        interfaceNameList.add(interfaceName);
                    }
                    this.interfaceNameListMap.put(currentClassName, interfaceNameList);
                }
                ITypeBinding superclass = typeBinding.getSuperclass();
                if (superclass != null) {
                    String superClassName = ClassUtil.removeGenericType(superclass.getQualifiedName());
                    this.superClassNameMap.put(currentClassName, superClassName);
                }
            }
        }
        return super.visit(node);
    }
}
