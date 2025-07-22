package com.strange.fix.engine.extraction.sourcecode.localization.visitor;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.MethodApiDefinitionLocation;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MethodApiDefinitionVisitor extends ApiLocationVisitor {

    private final List<MethodApiDefinitionLocation> methodApiDefinitionLocationList;

    public MethodApiDefinitionVisitor( CompilationUnit compilationUnit,  File codeFile,  ApiSignature apiSignature) {
        super(compilationUnit, codeFile, apiSignature);
        this.methodApiDefinitionLocationList = new ArrayList<>();
    }


    @Override
    public boolean visit(MethodDeclaration node) {
        IMethodBinding methodBinding = node.resolveBinding();
        if (methodBinding != null) {
            if (isEqualMethod(methodBinding)) {
                MethodApiDefinitionLocation methodApiDefinitionLocation = new MethodApiDefinitionLocation();
                compilationUnit.getLineNumber(node.getStartPosition());
                int startOffset = node.getStartPosition();
                int endOffset = startOffset + node.getLength() - 1;
                methodApiDefinitionLocation.setApiType(ApiTypeEnum.METHOD_DEF);
                methodApiDefinitionLocation.setMethodDeclaration(node);
                methodApiDefinitionLocation.setCompilationUnit(compilationUnit);
                methodApiDefinitionLocation.setStartLineNumber(compilationUnit.getLineNumber(startOffset));
                methodApiDefinitionLocation.setEndLineNumber(compilationUnit.getLineNumber(endOffset));
                methodApiDefinitionLocation.setTargetApiSignature(apiSignature);
                methodApiDefinitionLocation.setLocatedFile(codeFile);
                methodApiDefinitionLocationList.add(methodApiDefinitionLocation);
            }
        }
        return super.visit(node);
    }


    private boolean isEqualMethod( IMethodBinding methodBinding) {
        if (!Objects.equals(methodBinding.getName(), apiSignature.getMethodName())) {
            return false;
        }

        ITypeBinding declaringClass = methodBinding.getDeclaringClass();
        if (declaringClass == null) return false;
        String methodBelongedClassName = ClassUtil.removeGenericType(declaringClass.getQualifiedName());
        if (!Objects.equals(methodBelongedClassName, apiSignature.getClassName())) return false;

        ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
        if (parameterTypes == null) return false;
        List<String> methodParamList = apiSignature.getMethodParamList();
        if (parameterTypes.length != methodParamList.size()) return false;

        for (int i = 0; i < parameterTypes.length; i++) {
            ITypeBinding parameterType = parameterTypes[i];
            String targetSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(parameterType.getQualifiedName()));
            String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(methodParamList.get(i)));
            if (!isEqualType(targetSimpleClassName, actualSimpleClassName)) {
                return false;
            }
        }

        return true;
    }


    private boolean isEqualType(String targetSimpleClassName, String actualSimpleClassName) {
        if (Objects.equals(actualSimpleClassName, "null")) return true;
        if (Objects.equals(targetSimpleClassName, "Object")) return true;
        if (Objects.equals(targetSimpleClassName, "Class")) return true;
        return Objects.equals(actualSimpleClassName, targetSimpleClassName);
    }

    @Override
    public List<ApiLocation> getApiLocations() {
        return new ArrayList<>(methodApiDefinitionLocationList);
    }
}
