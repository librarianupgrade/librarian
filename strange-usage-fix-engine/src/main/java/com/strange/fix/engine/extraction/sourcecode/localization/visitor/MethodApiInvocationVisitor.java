package com.strange.fix.engine.extraction.sourcecode.localization.visitor;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.MethodApiInvocationLocation;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MethodApiInvocationVisitor extends ApiLocationVisitor {

    private final List<MethodApiInvocationLocation> methodApiInvocationLocations;

    public MethodApiInvocationVisitor( CompilationUnit compilationUnit,  File codeFile,  ApiSignature apiSignature) {
        super(compilationUnit, codeFile, apiSignature);
        this.methodApiInvocationLocations = new ArrayList<>();
    }

    @Override
    public List<ApiLocation> getApiLocations() {
        return new ArrayList<>(methodApiInvocationLocations);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        IMethodBinding methodBinding = node.resolveMethodBinding();
        if (methodBinding != null) {
            if (isEqualMethod(methodBinding)) {
                // if method invocation in method declaration
                MethodDeclaration ancestorMethodDeclaration = findAncestorMethodDeclaration(node);
                if (ancestorMethodDeclaration != null) {
                    IMethodBinding locatedMethodBinding = ancestorMethodDeclaration.resolveBinding();
                    if (locatedMethodBinding != null) {
                        MethodApiInvocationLocation methodApiInvocationLocation = new MethodApiInvocationLocation();
                        ApiSignature locatedApiSignature = nodeToApiSignatureDef(ancestorMethodDeclaration);
                        methodApiInvocationLocation.setLocatedApiSignature(locatedApiSignature);

                        compilationUnit.getLineNumber(node.getStartPosition());
                        int startOffset = node.getStartPosition();
                        int endOffset = startOffset + node.getLength() - 1;
                        methodApiInvocationLocation.setApiType(ApiTypeEnum.METHOD);
                        methodApiInvocationLocation.setInvocationNode(node);
                        methodApiInvocationLocation.setStartLineNumber(compilationUnit.getLineNumber(startOffset));
                        methodApiInvocationLocation.setEndLineNumber(compilationUnit.getLineNumber(endOffset));
                        methodApiInvocationLocation.setTargetApiSignature(apiSignature);
                        methodApiInvocationLocation.setLocatedFile(codeFile);
                        methodApiInvocationLocations.add(methodApiInvocationLocation);
                    }
                }

                // if method invocation in field declaration
                FieldDeclaration ancestorFieldDeclaration = findAncestorFieldDeclaration(node);
                if (ancestorFieldDeclaration != null) {
                    MethodApiInvocationLocation methodApiInvocationLocation = new MethodApiInvocationLocation();
                    ApiSignature locatedApiSignature = nodeToApiSignatureDef(ancestorFieldDeclaration);
                    methodApiInvocationLocation.setLocatedApiSignature(locatedApiSignature);

                    compilationUnit.getLineNumber(node.getStartPosition());
                    int startOffset = node.getStartPosition();
                    int endOffset = startOffset + node.getLength() - 1;
                    methodApiInvocationLocation.setApiType(ApiTypeEnum.METHOD);
                    methodApiInvocationLocation.setInvocationNode(node);
                    methodApiInvocationLocation.setStartLineNumber(compilationUnit.getLineNumber(startOffset));
                    methodApiInvocationLocation.setEndLineNumber(compilationUnit.getLineNumber(endOffset));
                    methodApiInvocationLocation.setTargetApiSignature(apiSignature);
                    methodApiInvocationLocation.setLocatedFile(codeFile);
                    methodApiInvocationLocations.add(methodApiInvocationLocation);
                }
            }
        }
        return super.visit(node);
    }

    private boolean isEqualMethod( IMethodBinding methodBinding) {
//        don't need to compare the class name of the method
//        ITypeBinding declaringClass = methodBinding.getDeclaringClass();
//        if (declaringClass == null) return false;
//        if (!Objects.equals(declaringClass.getQualifiedName(), apiSignature.getClassName())) {
//            return false;
//        }
        if (!Objects.equals(methodBinding.getName(), apiSignature.getMethodName())) {
            return false;
        }

        ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
        if (parameterTypes == null) return false;
        List<String> methodParamList = apiSignature.getMethodParamList();
        if (parameterTypes.length != methodParamList.size()) return false;

        for (int i = 0; i < parameterTypes.length; i++) {
            ITypeBinding parameterType = parameterTypes[i];
            String targetSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(parameterType.getQualifiedName()));
            String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(methodParamList.get(i)));
            if (Objects.equals(targetSimpleClassName, "Object")) {
                continue;
            }

            if (Objects.equals(targetSimpleClassName, "Class")) {
                continue;
            }

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
}
