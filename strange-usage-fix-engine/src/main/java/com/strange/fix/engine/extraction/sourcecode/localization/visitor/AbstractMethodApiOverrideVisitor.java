package com.strange.fix.engine.extraction.sourcecode.localization.visitor;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.AbstractMethodApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class AbstractMethodApiOverrideVisitor extends ApiLocationVisitor {

    private final List<AbstractMethodApiLocation> abstractMethodApiLocations;

    public AbstractMethodApiOverrideVisitor( CompilationUnit compilationUnit,  File codeFile, ApiSignature apiSignature) {
        super(compilationUnit, codeFile, apiSignature);
        this.abstractMethodApiLocations = new ArrayList<>();
    }

    @Override
    public List<ApiLocation> getApiLocations() {
        return new ArrayList<>(abstractMethodApiLocations);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        ITypeBinding typeBinding = node.resolveBinding();
        if (typeBinding != null) {
            if (isTargetTypeBinding(typeBinding, apiSignature)) {
                MethodDeclaration[] methodDeclarations = node.getMethods();
                for (MethodDeclaration methodDeclaration : methodDeclarations) {
                    IMethodBinding methodBinding = methodDeclaration.resolveBinding();
                    if (methodBinding != null) {
                        if (isTargetMethodBinding(methodBinding, apiSignature)) {
                            AbstractMethodApiLocation abstractMethodApiLocation = new AbstractMethodApiLocation();
                            abstractMethodApiLocation.setMethodDeclaration(methodDeclaration);
                            abstractMethodApiLocation.setTypeDeclaration(node);
                            compilationUnit.getLineNumber(methodDeclaration.getStartPosition());
                            int startOffset = methodDeclaration.getStartPosition();
                            int endOffset = startOffset + node.getLength() - 1;
                            abstractMethodApiLocation.setStartLineNumber(compilationUnit.getLineNumber(startOffset));
                            abstractMethodApiLocation.setEndLineNumber(compilationUnit.getLineNumber(endOffset));
                            abstractMethodApiLocation.setLocatedFile(codeFile);
                            abstractMethodApiLocation.setTargetApiSignature(apiSignature);
                            this.abstractMethodApiLocations.add(abstractMethodApiLocation);
                        }
                    }
                }
            }
        }
        return super.visit(node);
    }


    private boolean isTargetTypeBinding(ITypeBinding typeBinding, ApiSignature targetApiSignature) {
        String className = ClassUtil.removeGenericType(targetApiSignature.getClassName());
        ITypeBinding superclass = typeBinding.getSuperclass();
        if (superclass != null) {
            String qualifiedName = superclass.getQualifiedName();
            String actualClassName = ClassUtil.removeGenericType(qualifiedName);
            if (Objects.equals(actualClassName, className)) {
                return true;
            }
        }

        ITypeBinding[] interfaces = typeBinding.getInterfaces();
        if (interfaces != null) {
            for (ITypeBinding interfaceType : interfaces) {
                String qualifiedName = interfaceType.getQualifiedName();
                String actualClassName = ClassUtil.removeGenericType(qualifiedName);
                if (Objects.equals(actualClassName, className)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTargetMethodBinding(IMethodBinding methodBinding, ApiSignature targetApiSignature) {
        String actualMethodName = methodBinding.getName();
        if (!Objects.equals(actualMethodName, targetApiSignature.getMethodName())) return false;

        ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
        if (parameterTypes == null) return false;

        List<String> targetMethodParamList = targetApiSignature.getMethodParamList();
        if (targetMethodParamList == null) return false;

        if (parameterTypes.length != targetMethodParamList.size()) return false;

        for (int i = 0; i < parameterTypes.length; i++) {
            ITypeBinding parameterType = parameterTypes[i];
            String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(parameterType.getQualifiedName()));
            String targetSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(targetMethodParamList.get(i)));
            if (!Objects.equals(actualSimpleClassName, targetSimpleClassName)) return false;
        }
        return true;
    }
}
