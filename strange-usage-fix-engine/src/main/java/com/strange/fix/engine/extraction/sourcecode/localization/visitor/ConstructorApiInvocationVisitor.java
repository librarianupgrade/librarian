package com.strange.fix.engine.extraction.sourcecode.localization.visitor;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ConstructorApiInvocationLocation;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.*;

public class ConstructorApiInvocationVisitor extends ApiLocationVisitor {

    private final List<ConstructorApiInvocationLocation> constructorApiInvocationLocationList;

    public ConstructorApiInvocationVisitor( CompilationUnit compilationUnit,  File codeFile,  ApiSignature apiSignature) {
        super(compilationUnit, codeFile, apiSignature);
        this.constructorApiInvocationLocationList = new ArrayList<>();
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        return super.visit(node);
    }

    @Override
    public void endVisit(TypeDeclaration node) {

    }

    @Override
    public boolean visit(MethodDeclaration node) {

        return super.visit(node);
    }

    @Override
    public void endVisit(MethodDeclaration node) {

    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        return super.visit(node);
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        return super.visit(node);
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        IMethodBinding constructorBinding = node.resolveConstructorBinding();
        if (constructorBinding != null) {
            if (isEqualConstructor(constructorBinding)) {
                MethodDeclaration ancestorMethodDeclaration = findAncestorMethodDeclaration(node);
                if (ancestorMethodDeclaration != null) {
                    IMethodBinding methodBinding = ancestorMethodDeclaration.resolveBinding();
                    if (methodBinding != null) {
                        ConstructorApiInvocationLocation constructorApiInvocationLocation = new ConstructorApiInvocationLocation();
                        ApiSignature locatedApiSignature = nodeToApiSignatureDef(ancestorMethodDeclaration);
                        constructorApiInvocationLocation.setLocatedApiSignature(locatedApiSignature);

                        compilationUnit.getLineNumber(node.getStartPosition());
                        int startOffset = node.getStartPosition();
                        int endOffset = startOffset + node.getLength() - 1;
                        constructorApiInvocationLocation.setApiType(ApiTypeEnum.METHOD);
                        constructorApiInvocationLocation.setInstanceCreationStatement(node);
                        constructorApiInvocationLocation.setStartLineNumber(compilationUnit.getLineNumber(startOffset));
                        constructorApiInvocationLocation.setEndLineNumber(compilationUnit.getLineNumber(endOffset));
                        constructorApiInvocationLocation.setTargetApiSignature(apiSignature);
                        constructorApiInvocationLocation.setLocatedFile(codeFile);
                        constructorApiInvocationLocationList.add(constructorApiInvocationLocation);
                    }
                }
            }
        }
        return super.visit(node);
    }

    private boolean isEqualConstructor(IMethodBinding constructorBinding) {
        ITypeBinding declaringClass = constructorBinding.getDeclaringClass();
        String constructedClassName = ClassUtil.removeGenericType(declaringClass.getQualifiedName());
        if (!Objects.equals(constructedClassName, apiSignature.getClassName())) return false;

        ITypeBinding[] parameterTypes = constructorBinding.getParameterTypes();
        if (parameterTypes == null) return false;
        List<String> constructorParamList = apiSignature.getMethodParamList();
        if (parameterTypes.length != constructorParamList.size()) return false;

        for (int i = 0; i < parameterTypes.length; i++) {
            ITypeBinding parameterType = parameterTypes[i];
            String targetSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(parameterType.getQualifiedName()));
            String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(constructorParamList.get(i)));
            if (!isEqualType(targetSimpleClassName, actualSimpleClassName)) {
                return false;
            }
        }
        return true;
    }


    private boolean isEqualType(String targetSimpleClassName, String actualSimpleClassName) {
        if (Objects.equals(actualSimpleClassName, "null")) return true;
        return Objects.equals(actualSimpleClassName, targetSimpleClassName);
    }

    @Override
    public List<ApiLocation> getApiLocations() {
        return new ArrayList<>(constructorApiInvocationLocationList);
    }


}
